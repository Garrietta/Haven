package com.garrietta.haven.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.mutableStateMapOf
import com.garrietta.haven.core.net.HaConnection
import com.garrietta.haven.core.net.HaRest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** How a camera can be played, best first — HA's own order. */
enum class Transport { WEBRTC, HLS, MJPEG }

/** Pure: HA frontend_stream_types → transports to try, in order. Unit tested. */
fun transportsFor(streamTypes: List<String>, webRtcAllowed: Boolean): List<Transport> = buildList {
    if (webRtcAllowed && "web_rtc" in streamTypes) add(Transport.WEBRTC)
    if ("hls" in streamTypes) add(Transport.HLS)
    add(Transport.MJPEG)
}

/**
 * Camera plumbing shared by every camera surface: stream-type discovery
 * (cached), WebRTC signalling over the live socket, HLS playlist URLs,
 * snapshots (memory + disk, so tiles paint instantly) and MJPEG streaming.
 */
class CameraService(
    private val context: Context,
    private val scope: CoroutineScope,
    private val conn: HaConnection,
    val rest: HaRest,
) : CameraSignaling {

    val webRtcAllowed: Boolean = WebRtcCrashGuard.checkOnLaunch(context)

    private val streamTypes = ConcurrentHashMap<String, List<String>>()

    suspend fun streamTypes(entityId: String): List<String> {
        streamTypes[entityId]?.let { return it }
        val types = try {
            val r = conn.call("camera/capabilities", 5_000) { put("entity_id", entityId) } as? JSONObject
            r?.optJSONArray("frontend_stream_types")?.let { a -> (0 until a.length()).map { a.optString(it) } }
        } catch (_: Exception) {
            null
        }
        if (types != null) streamTypes[entityId] = types
        return types ?: emptyList()
    }

    suspend fun hlsUrl(entityId: String): String? = try {
        (conn.call("camera/stream", 10_000) { put("entity_id", entityId) } as? JSONObject)?.optString("url")?.ifEmpty { null }
    } catch (_: Exception) {
        null
    }

    // ---- CameraSignaling ----------------------------------------------------

    override fun clientConfig(entityId: String, onResult: (JSONObject?) -> Unit) {
        scope.launch {
            val r = try { conn.call("camera/webrtc/get_client_config", 5_000) { put("entity_id", entityId) } as? JSONObject } catch (_: Exception) { null }
            onResult(r)
        }
    }

    override fun offer(
        entityId: String,
        sdp: String,
        onSubscribed: (Int) -> Unit,
        onEvent: (JSONObject) -> Unit,
        onError: (String) -> Unit,
        onClosed: () -> Unit,
    ) {
        scope.launch {
            try {
                val id = conn.subscribe(
                    "camera/webrtc/offer",
                    { put("entity_id", entityId).put("offer", sdp) },
                    onClosed = onClosed,
                    onEvent = onEvent,
                )
                onSubscribed(id)
            } catch (e: Exception) {
                onError(e.message ?: "offer failed")
            }
        }
    }

    override fun candidate(entityId: String, sessionId: String, candidate: JSONObject) =
        conn.fire("camera/webrtc/candidate") {
            put("entity_id", entityId).put("session_id", sessionId).put("candidate", candidate)
        }

    override fun unsubscribe(subscriptionId: Int) = conn.unsubscribe(subscriptionId)

    // ---- Snapshots ------------------------------------------------------------

    /** Latest still per camera, observable by Compose. */
    val snapshots = mutableStateMapOf<String, Bitmap>()
    private val memory = LruCache<String, Bitmap>(12)
    private val dir = File(context.cacheDir, "snapshots").apply { mkdirs() }
    private val inFlight = ConcurrentHashMap<String, Job>()

    fun cachedSnapshot(entityId: String): Bitmap? {
        snapshots[entityId]?.let { return it }
        memory.get(entityId)?.let { snapshots[entityId] = it; return it }
        val f = File(dir, entityId.replace('.', '_') + ".jpg")
        if (!f.isFile) return null
        return BitmapFactory.decodeFile(f.absolutePath)?.also { memory.put(entityId, it); snapshots[entityId] = it }
    }

    /** Fetches a fresh still (deduplicated per camera). */
    fun refreshSnapshot(entityId: String, widthPx: Int = 960) {
        if (inFlight[entityId]?.isActive == true) return
        inFlight[entityId] = scope.launch(Dispatchers.IO) {
            val bytes = rest.cameraSnapshot(entityId, widthPx) ?: return@launch
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
            memory.put(entityId, bmp)
            withContext(Dispatchers.Main) { snapshots[entityId] = bmp }
            try { File(dir, entityId.replace('.', '_') + ".jpg").writeBytes(bytes) } catch (_: Exception) {}
        }
    }

    /** Remember the newest live frame as the poster for next time. */
    fun rememberFrame(entityId: String, frame: Bitmap) {
        memory.put(entityId, frame)
        snapshots[entityId] = frame
    }

    // ---- MJPEG ------------------------------------------------------------------

    /**
     * Streams `/api/camera_proxy_stream`, decoding only the newest frame of
     * each read so playback never lags behind real time. Calls [onFrame] on
     * the main thread; [onError] once if it fails or stalls. Cancel the job to stop.
     */
    fun streamMjpeg(entityId: String, onFrame: (Bitmap) -> Unit, onError: (String) -> Unit): Job = scope.launch(Dispatchers.IO) {
        val base = rest.baseUrl ?: return@launch withContext(Dispatchers.Main) { onError("Not connected") }
        val req: Request = rest.request("$base/api/camera_proxy_stream/$entityId") ?: return@launch withContext(Dispatchers.Main) { onError("Signed out") }
        val call = rest.streamHttp.newCall(req)
        coroutineContext[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) { withContext(Dispatchers.Main) { onError("HTTP ${resp.code}") }; return@launch }
                val boundary = Regex("boundary=([^;\\s]+)").find(resp.header("Content-Type") ?: "")?.groupValues?.get(1)?.trim('"')
                    ?: run { withContext(Dispatchers.Main) { onError("Not an MJPEG stream") }; return@launch }
                val body = resp.body?.byteStream() ?: return@launch
                val parser = MjpegParser(boundary)
                val buf = ByteArray(64 * 1024)
                var lastFrame = System.currentTimeMillis()
                val watchdog = launch {
                    while (isActive) {
                        delay(2_000)
                        if (System.currentTimeMillis() - lastFrame > 20_000) { call.cancel(); break }
                    }
                }
                try {
                    while (isActive) {
                        val n = body.read(buf)
                        if (n < 0) break
                        val frames = parser.feed(buf, 0, n)
                        if (frames.isEmpty()) continue
                        val jpeg = frames.last()
                        val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: continue
                        lastFrame = System.currentTimeMillis()
                        withContext(Dispatchers.Main) { onFrame(bmp) }
                    }
                } finally {
                    watchdog.cancel()
                }
            }
            if (isActive) withContext(Dispatchers.Main) { onError("Stream ended") }
        } catch (e: Exception) {
            if (isActive) withContext(Dispatchers.Main) { onError(e.message ?: "Stream failed") }
        }
    }

    /** An ExoPlayer for HA's (LL-)HLS playlist, Bearer-authed, close to the live edge. Caller releases. */
    suspend fun hlsPlayer(playlistPath: String): androidx.media3.exoplayer.ExoPlayer? {
        val url = rest.resolve(playlistPath) ?: return null
        val token = rest.token() ?: return null
        return withContext(Dispatchers.Main) {
            val ds = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(rest.http)
                .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            val item = androidx.media3.common.MediaItem.Builder().setUri(url)
                .setLiveConfiguration(
                    androidx.media3.common.MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(1_500).setMinPlaybackSpeed(0.97f).setMaxPlaybackSpeed(1.04f).build(),
                ).build()
            val source = androidx.media3.exoplayer.hls.HlsMediaSource.Factory(ds).setAllowChunklessPreparation(true).createMediaSource(item)
            androidx.media3.exoplayer.ExoPlayer.Builder(context)
                .setLoadControl(androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(2_000, 10_000, 500, 1_000).build())
                .build().apply {
                    volume = 0f
                    setMediaSource(source)
                    prepare()
                    playWhenReady = true
                }
        }
    }

    /** Warms whatever the viewer will use (called when a finger lands on a camera tile). */
    fun prewarm(entityId: String) {
        scope.launch {
            val t = transportsFor(streamTypes(entityId), webRtcAllowed)
            if (t.first() == Transport.HLS) hlsUrl(entityId)
        }
    }

    @Suppress("unused")
    private fun JSONArray.strings() = (0 until length()).map { optString(it) }
}
