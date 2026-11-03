package com.garrietta.haven.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.WeakHashMap
import org.json.JSONObject
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack

/**
 * Result of a `camera/webrtc/offer` round-trip: the SDP answer and the
 * session id, or an error stage when HA could not start a session.
 * The answer field is accepted as `answer` (current HA) or `sdp`
 * (older variants) — the shape is parsed, never assumed.
 */
data class WebRtcAnswer(val answerSdp: String?, val sessionId: String?, val error: String?)

/**
 * Pure parse of a `camera/webrtc/offer` subscription EVENT message
 * (unit-tested, Round 5 Bug B): HA answers the offer command as an
 * event subscription, not as a result — the answer arrives later as
 * a `type: "event"` message whose `event` object carries
 * `type: "answer"` and the answer SDP string (or `sdp`), plus the
 * session id. Returns the answer for an answer event, null for any
 * other event (candidates, unrelated events) — the caller keeps
 * the subscription open across candidates and closes it on the
 * answer. A missing/blank answer SDP yields an error result, never
 * a silent null. [knownSessionId] (from the command's result, when
 * it carried one) fills in when the event itself carries none.
 */
fun parseWebRtcEventAnswer(msg: JSONObject, knownSessionId: String? = null): WebRtcAnswer? {
    if (msg.optString("type") != "event") return null
    val event = msg.optJSONObject("event") ?: return null
    if (event.optString("type") != "answer") return null
    val answer = event.optString("answer").ifEmpty { null }
        ?: event.optString("sdp").ifEmpty { null }
    val sessionId = event.optString("session_id").ifEmpty { null } ?: knownSessionId
    if (answer == null) return WebRtcAnswer(null, sessionId, "answer event missing SDP")
    return WebRtcAnswer(answer, sessionId, null)
}

/** Pure parse of a `camera/webrtc/offer` result message (unit-tested). */
fun parseWebRtcAnswer(msg: JSONObject): WebRtcAnswer {
    if (!msg.optBoolean("success", false)) {
        val err = msg.optJSONObject("error")
        return WebRtcAnswer(null, null, err?.optString("message")?.ifEmpty { null } ?: "offer rejected")
    }
    val result = msg.optJSONObject("result") ?: return WebRtcAnswer(null, null, "empty answer")
    val answer = result.optString("answer").ifEmpty { null }
        ?: result.optString("sdp").ifEmpty { null }
    val sessionId = result.optString("session_id").ifEmpty { null }
    if (answer == null) return WebRtcAnswer(null, null, "answer missing SDP")
    return WebRtcAnswer(answer, sessionId, null)
}

/**
 * Ring live video over WebRTC, following HA's own routing: HA's
 * frontend plays Ring live via WebRTC (WS `camera/webrtc/offer`), not
 * via the MJPEG proxy (which serves a transcoded recording) and not
 * via HLS-through-stream. This session mirrors that path:
 *
 * 1. Create a recv-only peer connection (video transceiver, no local
 *    media — Haven never captures).
 * 2. Create an offer, wait for ICE gathering to COMPLETE, then send
 *    the complete offer (all gathered candidates inline). Waiting for
 *    gathering complete is deliberate: it removes any dependency on
 *    server-side trickle candidates, whose WS delivery shape could not
 *    be confirmed from public HA sources. If HA's Ring path requires
 *    trickle (an answer without usable candidates), that surfaces as
 *    an ICE failure stage, not a silent black screen.
 * 3. Set the returned answer as the remote description; the remote
 *    video track renders into the supplied SurfaceViewRenderer.
 * 4. Local candidates gathered AFTER the answer (rare — gathering
 *    already completed) are trickle-sent best-effort via
 *    `camera/webrtc/candidate`; a failed send is ignored, never fatal.
 *
 * ICE servers: public STUN only (stun.l.google.com:19302), the same
 * fallback HA's frontend uses when HA supplies none. No TURN is
 * configured: if Ring's cloud side requires relay, ICE fails and the
 * failure stage names it — the caller then falls through to the
 * HLS-live path, never to the recording.
 *
 * There is no confirmed `camera/webrtc/close` WS command in HA, so
 * close() only tears down locally (sink, renderer, peer connection);
 * the shared factory/EGL base outlive every session and the HA-side
 * session expires on its own.
 *
 * Callbacks are posted to the main thread inside the session, so the
 * caller can touch Compose state directly. onFailed fires at most once.
 */
class RingWebRtcSession(
    private val appContext: Context,
    private val sendOffer: (offerSdp: String, onAnswer: (WebRtcAnswer) -> Unit) -> Unit,
    private val sendCandidate: (sessionId: String, candidateSdp: String) -> Unit,
) {
    // Round 6: NO per-session EGL/factory fields. The PeerConnectionFactory
    // and its EglBase are application-scoped singletons owned by the
    // Companion (see sharedFactory): a panel close must never dispose
    // them, because the native PeerConnection still references the
    // factory's threads while a session tears down — disposing the
    // factory under a live native peer connection is the native-abort
    // vector (SIGABRT/SIGSEGV in libjingle, uncatchable from Kotlin).
    private var pc: PeerConnection? = null
    private var sessionId: String? = null
    private var failed = false
    private var closed = false
    private var started = false
    private var videoTrack: VideoTrack? = null
    private var sink: VideoSink? = null

    // WebRTC invokes callbacks on its own signaling/decoder threads;
    // Compose snapshot state must only be written on the main thread.
    // Every callback into the caller is posted to main here (and in
    // the sink below), so a cross-thread write can never corrupt the
    // snapshot or crash the tap path.
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun fail(stage: String, onFailed: (String) -> Unit) {
        if (failed || closed) return
        failed = true
        mainHandler.post { onFailed(stage) }
    }

    /**
     * Starts the session: renders remote video into [renderer] and
     * calls [onFirstFrame] on the first decoded frame (the "Live"
     * badge gates on this, never on negotiation alone). [onFailed]
     * carries the stage that failed, for the error UI.
     *
     * Hardening (2026-10-29 camera-tap crash, cause unproven by audit
     * — no bug report was attached, so the tap path was audited, not
     * reproduced): start is idempotent (a double start from an
     * effect re-run or a Retry racing the old session can never
     * double-init the native stack), and any synchronous throw during
     * native init (EGL context, factory, renderer) becomes a named
     * "init:" failure for the error UI + HLS-live fallback, never a
     * crash dialog. If the crash persists on device after this build,
     * a bug report would pinpoint it — the audit could not.
     */
    fun start(
        renderer: SurfaceViewRenderer,
        onFirstFrame: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        if (started) return
        started = true
        try {
            startInternal(renderer, onFirstFrame, onFailed)
        } catch (e: Exception) {
            fail("init: ${e.message ?: e.javaClass.simpleName}", onFailed)
        }
    }

    private fun startInternal(
        renderer: SurfaceViewRenderer,
        onFirstFrame: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val (eglBase, factory) = sharedFactory(appContext)
        // Round 6 init guard: renderer.init must never run twice on
        // the same SurfaceViewRenderer instance (a second init on an
        // already-initialized native renderer is a second native-abort
        // vector). A fresh renderer per attempt still inits normally;
        // a reused view (effect re-run, Retry racing the old session)
        // skips the second init and renders into the existing context.
        // The guard is cleared in close() after renderer.release(), so
        // a genuinely released view can init again on a later session.
        if (!isRendererInitialized(renderer)) {
            renderer.init(eglBase.eglBaseContext, null)
            markRendererInitialized(renderer)
        }

        var firstFrameFired = false
        val sink = VideoSink { frame: VideoFrame ->
            if (!firstFrameFired) {
                firstFrameFired = true
                mainHandler.post { onFirstFrame() }
            }
            renderer.onFrame(frame)
        }
        this.sink = sink
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                // Only after the answer (sessionId known); best-effort.
                sessionId?.let { sendCandidate(it, candidate.sdp) }
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                if (state == PeerConnection.IceGatheringState.COMPLETE) {
                    val local = pc?.localDescription ?: return
                    sendOffer(local.description) { answer ->
                        if (closed) return@sendOffer
                        if (answer.error != null || answer.answerSdp == null) {
                            fail("offer: ${answer.error ?: "no answer"}", onFailed)
                            return@sendOffer
                        }
                        sessionId = answer.sessionId
                        pc?.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() {}
                            override fun onSetFailure(error: String) {
                                fail("answer: $error", onFailed)
                            }
                            override fun onCreateSuccess(desc: SessionDescription) {}
                            override fun onCreateFailure(error: String) {}
                        }, SessionDescription(SessionDescription.Type.ANSWER, answer.answerSdp))
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {
                val track = receiver.track() as? VideoTrack ?: return
                videoTrack = track
                track.addSink(sink)
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                if (state == PeerConnection.IceConnectionState.FAILED) {
                    fail("ICE connection failed", onFailed)
                }
            }

            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                if (state == PeerConnection.PeerConnectionState.FAILED) {
                    fail("peer connection failed", onFailed)
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddStream(stream: MediaStream) {}
        }
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()),
        )
        val connection = factory.createPeerConnection(rtcConfig, observer)
        if (connection == null) {
            fail("peer connection create failed", onFailed)
            return
        }
        pc = connection
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
        )
        connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                connection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String) {
                        fail("offer: $error", onFailed)
                    }
                    override fun onCreateSuccess(desc: SessionDescription) {}
                    override fun onCreateFailure(error: String) {}
                }, desc)
            }

            override fun onCreateFailure(error: String) {
                fail("offer: $error", onFailed)
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, MediaConstraints())
    }

    /**
     * Tears down locally, on the main thread, in strict order.
     * Round 6 (Front Door process death ~3s after engaging live, on
     * the WebRTC handshake/fallback path): the #101 close() called
     * pc.close() then factory.dispose() with NO pc.dispose(), on
     * whatever thread the caller ran on, while the panel's
     * DisposableEffect.onDispose fired concurrently with the
     * SurfaceViewRenderer unmounting. That is the native-abort
     * shape: the native PeerConnection still referencing factory
     * threads when the factory is disposed, and the EGL context
     * released mid-detach — no Kotlin try/catch can catch it.
     *
     * The new order, all on the main handler: (1) remove the sink
     * from the video track, drop the track; (2) renderer.clearImage()
     * + renderer.release(); (3) pc.close() AND pc.dispose();
     * (4) the shared factory is NEVER disposed here; (5) the shared
     * EGL base is NEVER released here. Both live for the life of
     * the app (see sharedFactory). If close() is called off the main
     * thread (a signaling-thread observer path), the teardown is
     * posted to the main handler instead of running inline, so the
     * observer callback finishes before anything is torn down.
     */
    fun close(renderer: SurfaceViewRenderer) {
        if (closed) return
        closed = true
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { closeOnMain(renderer) }
            return
        }
        closeOnMain(renderer)
    }

    private fun closeOnMain(renderer: SurfaceViewRenderer) {
        // (1) Detach the sink FIRST: a decoded frame landing on a
        // released renderer after close is a use-after-release
        // crash vector.
        val detachedSink = sink
        if (detachedSink != null) {
            try {
                videoTrack?.removeSink(detachedSink)
            } catch (_: Exception) {
            }
        }
        videoTrack = null
        sink = null
        // (2) Clear the last frame, then release the renderer.
        try {
            renderer.clearImage()
        } catch (_: Exception) {
        }
        try {
            renderer.release()
        } catch (_: Exception) {
        }
        markRendererReleased(renderer)
        // (3) Close AND dispose the peer connection. The missing
        // dispose() in #101 left the native PeerConnection alive
        // while its factory was disposed underneath it.
        try {
            pc?.close()
        } catch (_: Exception) {
        }
        try {
            pc?.dispose()
        } catch (_: Exception) {
        }
        pc = null
        // (4)(5) Deliberately absent: sharedFactory/sharedEgl are
        // application-scoped and are never disposed or released on
        // a panel close. Disposing them here was the crash vector.
    }

    companion object {
        private var factoryInitialized = false
        private var sharedEgl: EglBase? = null
        private var sharedFactory: PeerConnectionFactory? = null

        /**
         * One lazy application-scoped factory + EGL base, shared by
         * every session for the life of the app. The encoder/decoder
         * factories are built against the shared EGL context, so
         * every session's renderer inits against the same context
         * and no session teardown can pull the context out from
         * under another session's native peer connection.
         */
        private fun sharedFactory(appContext: Context): Pair<EglBase, PeerConnectionFactory> {
            synchronized(Companion) {
                val existingFactory = sharedFactory
                val existingEgl = sharedEgl
                if (existingFactory != null && existingEgl != null) {
                    return existingEgl to existingFactory
                }
                if (!factoryInitialized) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(appContext)
                            .createInitializationOptions(),
                    )
                    factoryInitialized = true
                }
                val eglBase = EglBase.create()
                val factory = PeerConnectionFactory.builder()
                    .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
                    .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
                    .createPeerConnectionFactory()
                sharedEgl = eglBase
                sharedFactory = factory
                return eglBase to factory
            }
        }

        // Renderers whose native init has already run. A WeakHashMap
        // set: entries vanish with their view, so this can never pin
        // a dead renderer — and it is the init-twice guard, not a
        // lifetime manager.
        private val initializedRenderers =
            Collections.newSetFromMap(WeakHashMap<SurfaceViewRenderer, Boolean>())

        private fun isRendererInitialized(renderer: SurfaceViewRenderer): Boolean =
            synchronized(Companion) { renderer in initializedRenderers }

        private fun markRendererInitialized(renderer: SurfaceViewRenderer) {
            synchronized(Companion) { initializedRenderers.add(renderer) }
        }

        private fun markRendererReleased(renderer: SurfaceViewRenderer) {
            synchronized(Companion) { initializedRenderers.remove(renderer) }
        }
    }
}
