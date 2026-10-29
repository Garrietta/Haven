package com.garrietta.haven.data

import android.content.Context
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
 * close() only tears down locally (peer connection, factory, EGL);
 * the HA-side session expires on its own.
 *
 * Callbacks fire on the thread the WebRTC stack calls them on; the
 * caller posts to main where UI state is touched (same pattern as
 * requestHlsUrl). onFailed fires at most once.
 */
class RingWebRtcSession(
    private val appContext: Context,
    private val sendOffer: (offerSdp: String, onAnswer: (WebRtcAnswer) -> Unit) -> Unit,
    private val sendCandidate: (sessionId: String, candidateSdp: String) -> Unit,
) {
    private var egl: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var sessionId: String? = null
    private var failed = false
    private var closed = false

    private fun fail(stage: String, onFailed: (String) -> Unit) {
        if (failed || closed) return
        failed = true
        onFailed(stage)
    }

    /**
     * Starts the session: renders remote video into [renderer] and
     * calls [onFirstFrame] on the first decoded frame (the "Live"
     * badge gates on this, never on negotiation alone). [onFailed]
     * carries the stage that failed, for the error UI.
     */
    fun start(
        renderer: SurfaceViewRenderer,
        onFirstFrame: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        synchronized(Companion) {
            if (!factoryInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .createInitializationOptions(),
                )
                factoryInitialized = true
            }
        }
        val eglBase = EglBase.create()
        egl = eglBase
        renderer.init(eglBase.eglBaseContext, null)
        val factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        this.factory = factory

        var firstFrameFired = false
        val sink = VideoSink { frame: VideoFrame ->
            if (!firstFrameFired) {
                firstFrameFired = true
                onFirstFrame()
            }
            renderer.onFrame(frame)
        }
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

    /** Tears down locally: peer connection, factory, EGL, renderer. */
    fun close(renderer: SurfaceViewRenderer) {
        if (closed) return
        closed = true
        try {
            pc?.close()
        } catch (_: Exception) {
        }
        pc = null
        try {
            factory?.dispose()
        } catch (_: Exception) {
        }
        factory = null
        try {
            renderer.release()
        } catch (_: Exception) {
        }
        try {
            egl?.release()
        } catch (_: Exception) {
        }
        egl = null
    }

    companion object {
        private var factoryInitialized = false
    }
}
