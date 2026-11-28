package com.garrietta.haven.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack

/**
 * Performance round (2026-11-28): live camera video over WebRTC, the way
 * Home Assistant's own frontend does it — which is why HA's cameras open
 * in well under a second while Haven's MJPEG proxy took several.
 *
 * Protocol (HA 2024.11+, go2rtc built in; also used by Ring, Nest, …):
 *  1. `camera/webrtc/get_client_config` → ICE servers (+ whether the
 *     provider wants all candidates in the offer).
 *  2. Create a recv-only peer connection (video + audio, audio muted),
 *     create the offer and send it AT ONCE via the
 *     `camera/webrtc/offer` subscription (trickle ICE). The old session
 *     waited for ICE gathering to complete first — seconds of dead time
 *     on every open.
 *  3. Subscription events: `session` (session id), `answer` (SDP),
 *     `candidate` (RTCIceCandidateInit object), `error`.
 *  4. Local candidates go out via `camera/webrtc/candidate` as an
 *     RTCIceCandidateInit object (the old code sent a bare string, which
 *     current HA rejects) — queued until the session id arrives.
 *     Remote candidates are queued until the answer is applied.
 *  5. Close = unsubscribe, which makes HA close the provider session.
 *
 * Threading: every WebRTC callback hops to the main thread before it
 * touches session state, so nothing here races; callers receive their
 * callbacks on the main thread. onFailed fires at most once.
 */
class HaWebRtcSession(
    private val appContext: Context,
    private val client: HaClient,
    private val entityId: String,
) {
    private val main = Handler(Looper.getMainLooper())
    private var pc: PeerConnection? = null
    private var subscriptionId: Int? = null
    private var sessionId: String? = null
    private var remoteSet = false
    private var candidatesUpfront = false
    private val pendingLocal = mutableListOf<JSONObject>()
    private val pendingRemote = mutableListOf<IceCandidate>()
    private var videoTrack: VideoTrack? = null
    private var sink: VideoSink? = null
    private var started = false
    private var closed = false
    private var failed = false
    private var onFailedCb: ((String) -> Unit)? = null

    private fun fail(stage: String) {
        if (failed || closed) return
        failed = true
        Log.w(TAG, "webrtc $entityId failed: $stage")
        WebRtcCrashGuard.attemptEnded(appContext)
        val cb = onFailedCb
        main.post { cb?.invoke(stage) }
    }

    /**
     * Starts negotiation and renders into [renderer]. [onFirstFrame]
     * fires on the first decoded frame; [onFailed] names the stage that
     * failed so the caller can fall back to HLS / MJPEG.
     */
    fun start(renderer: SurfaceViewRenderer, onFirstFrame: () -> Unit, onFailed: (String) -> Unit) {
        if (started) return
        started = true
        onFailedCb = onFailed
        WebRtcCrashGuard.attemptStarted(appContext)
        client.webRtcClientConfig(entityId) { config ->
            if (closed) return@webRtcClientConfig
            try {
                startWithConfig(renderer, config, onFirstFrame)
            } catch (e: Exception) {
                fail("init: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun startWithConfig(renderer: SurfaceViewRenderer, config: JSONObject?, onFirstFrame: () -> Unit) {
        val (eglBase, factory) = WebRtcEngine.shared(appContext)
        WebRtcEngine.initRenderer(renderer, eglBase)

        candidatesUpfront = config?.optBoolean("getCandidatesUpfront", false) == true
        val iceServers = parseIceServers(config?.optJSONObject("configuration")?.optJSONArray("iceServers"))
            .ifEmpty { listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()) }
        val rtc = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        }

        var firstFrame = false
        val videoSink = VideoSink { frame: VideoFrame ->
            if (!firstFrame) {
                firstFrame = true
                main.post {
                    if (!closed) {
                        WebRtcCrashGuard.attemptEnded(appContext)
                        onFirstFrame()
                    }
                }
            }
            renderer.onFrame(frame)
        }
        sink = videoSink

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                main.post { onLocalCandidate(candidate) }
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                if (state == PeerConnection.IceGatheringState.COMPLETE && candidatesUpfront) {
                    main.post { sendOfferIfReady() }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {
                when (val track = receiver.track()) {
                    is VideoTrack -> main.post {
                        if (closed) return@post
                        videoTrack = track
                        track.addSink(videoSink)
                    }
                    // Muted by default, like HA's camera cards.
                    is AudioTrack -> track.setEnabled(false)
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                if (state == PeerConnection.IceConnectionState.FAILED) main.post { fail("ICE failed") }
            }

            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                if (state == PeerConnection.PeerConnectionState.FAILED) main.post { fail("connection failed") }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel) {}
            override fun onRenegotiationNeeded() {}
        }

        val connection = factory.createPeerConnection(rtc, observer) ?: run {
            fail("peer connection create failed")
            return
        }
        pc = connection
        val recvOnly = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        connection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, recvOnly)
        connection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, recvOnly)

        connection.createOffer(object : SimpleSdp() {
            override fun onCreateSuccess(desc: SessionDescription) {
                connection.setLocalDescription(object : SimpleSdp() {
                    override fun onSetSuccess() {
                        main.post { if (!candidatesUpfront) sendOfferIfReady() }
                    }
                    override fun onSetFailure(error: String) { main.post { fail("local description: $error") } }
                }, desc)
            }
            override fun onCreateFailure(error: String) { main.post { fail("offer: $error") } }
        }, MediaConstraints())
    }

    private var offerSent = false

    private fun sendOfferIfReady() {
        if (closed || failed || offerSent) return
        val local = pc?.localDescription ?: return
        offerSent = true
        subscriptionId = client.subscribe(
            type = "camera/webrtc/offer",
            build = { it.put("entity_id", entityId).put("offer", local.description) },
            onEvent = { event -> main.post { onSignal(event) } },
            onResult = { ok, error -> if (!ok) main.post { fail("offer rejected: ${error ?: "unknown"}") } },
            onClosed = { main.post { fail("connection to Home Assistant dropped") } },
        )
    }

    private fun onSignal(event: JSONObject) {
        if (closed || failed) return
        when (event.optString("type")) {
            "session" -> {
                sessionId = event.optString("session_id").ifEmpty { null }
                val sid = sessionId ?: return
                pendingLocal.forEach { client.sendWebRtcCandidate(entityId, sid, it) }
                pendingLocal.clear()
            }
            "answer" -> {
                val sdp = event.optString("answer").ifEmpty { null } ?: return fail("answer missing SDP")
                pc?.setRemoteDescription(object : SimpleSdp() {
                    override fun onSetSuccess() {
                        main.post {
                            if (closed) return@post
                            remoteSet = true
                            pendingRemote.forEach { pc?.addIceCandidate(it) }
                            pendingRemote.clear()
                        }
                    }
                    override fun onSetFailure(error: String) { main.post { fail("answer: $error") } }
                }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
            }
            "candidate" -> {
                val candidate = parseRemoteCandidate(event) ?: return
                if (remoteSet) pc?.addIceCandidate(candidate) else pendingRemote += candidate
            }
            "error" -> fail(event.optString("message").ifEmpty { event.optString("code").ifEmpty { "provider error" } })
        }
    }

    private fun onLocalCandidate(candidate: IceCandidate) {
        if (closed || candidatesUpfront) return // upfront: already inside the offer SDP
        val init = JSONObject()
            .put("candidate", candidate.sdp)
            .put("sdpMid", candidate.sdpMid)
            .put("sdpMLineIndex", candidate.sdpMLineIndex)
        val sid = sessionId
        if (sid != null) client.sendWebRtcCandidate(entityId, sid, init) else pendingLocal += init
    }

    /** Tears down on the main thread: unsubscribe, detach sink, release renderer, dispose the peer connection. */
    fun close(renderer: SurfaceViewRenderer) {
        if (closed) return
        closed = true
        WebRtcCrashGuard.attemptEnded(appContext)
        val run = Runnable {
            subscriptionId?.let { client.unsubscribe(it) }
            subscriptionId = null
            sink?.let { s -> try { videoTrack?.removeSink(s) } catch (_: Exception) {} }
            videoTrack = null
            sink = null
            try { renderer.clearImage() } catch (_: Exception) {}
            WebRtcEngine.releaseRenderer(renderer)
            try { pc?.close() } catch (_: Exception) {}
            try { pc?.dispose() } catch (_: Exception) {}
            pc = null
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else main.post(run)
    }

    private open class SimpleSdp : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) {}
        override fun onSetFailure(error: String) {}
    }

    companion object {
        private const val TAG = "Haven"
    }
}

/** RTCConfiguration.iceServers JSON (urls string|array, username, credential) → WebRTC ICE servers. Pure-ish. */
fun parseIceServers(arr: JSONArray?): List<PeerConnection.IceServer> {
    if (arr == null) return emptyList()
    val out = mutableListOf<PeerConnection.IceServer>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val urls = parseIceUrls(o)
        if (urls.isEmpty()) continue
        val b = PeerConnection.IceServer.builder(urls)
        o.optString("username").takeIf { it.isNotEmpty() }?.let { b.setUsername(it) }
        o.optString("credential").takeIf { it.isNotEmpty() }?.let { b.setPassword(it) }
        out += b.createIceServer()
    }
    return out
}

/** The `urls` of one RTCIceServer entry: a string or an array of strings. Pure, unit tested. */
fun parseIceUrls(server: JSONObject): List<String> {
    val arr = server.optJSONArray("urls")
    if (arr != null) return (0 until arr.length()).mapNotNull { arr.optString(it).ifEmpty { null } }
    return listOfNotNull(server.optString("urls").ifEmpty { null })
}

/**
 * A `candidate` subscription event → IceCandidate. Accepts HA's
 * RTCIceCandidateInit object shape and the older bare-string shape.
 * Blank candidates (end-of-candidates markers) yield null.
 */
fun parseRemoteCandidate(event: JSONObject): IceCandidate? {
    val obj = event.optJSONObject("candidate")
    val sdp = obj?.optString("candidate") ?: event.optString("candidate")
    if (sdp.isNullOrBlank()) return null
    val mid = obj?.optString("sdpMid")?.ifEmpty { null } ?: "0"
    val index = obj?.optInt("sdpMLineIndex", 0) ?: 0
    return IceCandidate(mid, index, sdp)
}

/**
 * Application-scoped WebRTC engine: one PeerConnectionFactory + EGL base
 * for the life of the app (disposing them under a live native peer
 * connection is a native-abort vector — see the Round 6 notes in
 * RingWebRtc.kt), plus the renderer init-once guard.
 */
object WebRtcEngine {
    private var initialized = false
    private var egl: org.webrtc.EglBase? = null
    private var factory: org.webrtc.PeerConnectionFactory? = null
    private val renderers = java.util.Collections.newSetFromMap(java.util.WeakHashMap<SurfaceViewRenderer, Boolean>())

    @Synchronized
    fun shared(appContext: Context): Pair<org.webrtc.EglBase, org.webrtc.PeerConnectionFactory> {
        val f = factory
        val e = egl
        if (f != null && e != null) return e to f
        if (!initialized) {
            org.webrtc.PeerConnectionFactory.initialize(
                org.webrtc.PeerConnectionFactory.InitializationOptions.builder(appContext.applicationContext)
                    .createInitializationOptions(),
            )
            initialized = true
        }
        val eglBase = org.webrtc.EglBase.create()
        val adm = org.webrtc.audio.JavaAudioDeviceModule.builder(appContext.applicationContext)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .createAudioDeviceModule()
        val created = org.webrtc.PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(org.webrtc.DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(org.webrtc.DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        egl = eglBase
        factory = created
        return eglBase to created
    }

    @Synchronized
    fun initRenderer(renderer: SurfaceViewRenderer, eglBase: org.webrtc.EglBase) {
        if (renderer in renderers) return
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        renderer.setEnableHardwareScaler(true)
        renderers += renderer
    }

    @Synchronized
    fun releaseRenderer(renderer: SurfaceViewRenderer) {
        if (renderer !in renderers) return
        try { renderer.release() } catch (_: Exception) {}
        renderers -= renderer
    }
}

/**
 * Native-crash safety net. A libwebrtc SIGSEGV cannot be caught from
 * Kotlin (that is what gated WebRTC off in Round 7), so instead of
 * guessing, the app records "attempt in flight" before negotiating and
 * clears it when the attempt ends (first frame, failure, or close). If
 * the process died mid-attempt, the flag is still set on the next
 * launch: [checkOnLaunch] counts it, and after two such deaths on the
 * same app version WebRTC is disabled for that version — cameras then
 * open over HLS (still far faster than the MJPEG proxy). A new app
 * version re-enables it.
 */
object WebRtcCrashGuard {
    // A new name drops the counts kept under the earlier rule, which took
    // any process end mid-attempt for a crash.
    private const val PREFS = "haven_webrtc_guard_v2"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The build the counts belong to: the version code and the CI build number, since test builds share a version code. */
    private fun versionOf(ctx: Context): String = try {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        "$code.${com.garrietta.haven.BuildConfig.BUILD_NUMBER}"
    } catch (_: Exception) { "0" }

    /** Call once per process start. Returns true when WebRTC may be used. */
    fun checkOnLaunch(ctx: Context): Boolean {
        val p = prefs(ctx)
        val version = versionOf(ctx)
        if (p.getString("build", null) != version) {
            p.edit().clear().putString("build", version).apply()
        }
        if (p.getBoolean("inflight", false)) {
            // Only a crash counts. The process also ends, perfectly normally,
            // when Android reclaims a backgrounded app that had a camera
            // still connecting; counting those switched WebRTC off for
            // cameras (the doorbell) that have no other live path.
            if (lastExitWasCrash(ctx)) {
                val deaths = p.getInt("deaths", 0) + 1
                p.edit().putBoolean("inflight", false).putInt("deaths", deaths).apply()
                Log.w("Haven", "WebRTC: previous process crashed mid-negotiation ($deaths)")
            } else {
                p.edit().putBoolean("inflight", false).apply()
            }
        }
        return enabled(ctx)
    }

    fun enabled(ctx: Context): Boolean = prefs(ctx).getInt("deaths", 0) < 2

    /** Whether Android recorded the app's last exit as a crash (API 30+; earlier versions cannot tell, so no). */
    private fun lastExitWasCrash(ctx: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 30) return false
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val last = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 1).firstOrNull() ?: return false
            exitReasonIsCrash(last.reason)
        } catch (_: Exception) {
            false
        }
    }

    fun attemptStarted(ctx: Context) { prefs(ctx).edit().putBoolean("inflight", true).commit() }

    fun attemptEnded(ctx: Context) { prefs(ctx).edit().putBoolean("inflight", false).apply() }
}

/** Whether an ApplicationExitInfo reason is a crash (a native one, or an uncaught Java exception), not an ordinary end. */
fun exitReasonIsCrash(reason: Int): Boolean =
    reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE || reason == android.app.ApplicationExitInfo.REASON_CRASH
