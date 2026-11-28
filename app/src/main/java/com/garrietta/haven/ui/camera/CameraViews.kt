package com.garrietta.haven.ui.camera

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.garrietta.haven.AppGraph
import com.garrietta.haven.core.camera.HaWebRtcSession
import com.garrietta.haven.core.camera.Transport
import com.garrietta.haven.core.camera.transportsFor
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

/**
 * Live camera video. Tries HA's transports in its own order (WebRTC → HLS →
 * MJPEG), showing the last snapshot at full brightness until the first live
 * frame lands. [onLive] reports which transport is playing.
 */
@Composable
fun LiveCameraView(graph: AppGraph, entityId: String, modifier: Modifier = Modifier, onLive: (Transport?) -> Unit = {}) {
    val cameras = graph.cameras
    var plan by remember(entityId) { mutableStateOf<List<Transport>?>(null) }
    var index by remember(entityId) { mutableIntStateOf(0) }
    var live by remember(entityId) { mutableStateOf<Transport?>(null) }
    LaunchedEffect(entityId) {
        cameras.cachedSnapshot(entityId)
        cameras.refreshSnapshot(entityId)
        plan = transportsFor(cameras.streamTypes(entityId), cameras.webRtcAllowed)
    }
    val transports = plan
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        val current = transports?.getOrNull(index)
        if (current != null) {
            key(entityId, index) {
                val fail: () -> Unit = { live = null; onLive(null); index++ }
                val ok: () -> Unit = { live = current; onLive(current) }
                when (current) {
                    Transport.WEBRTC -> WebRtcVideo(graph, entityId, ok, fail)
                    Transport.HLS -> HlsVideo(graph, entityId, ok, fail)
                    Transport.MJPEG -> MjpegVideo(graph, entityId, ok, fail)
                }
            }
        }
        if (live == null) Poster(cameras.snapshots[entityId], failed = transports != null && index >= transports.size)
    }
}

@Composable
private fun Poster(bitmap: Bitmap?, failed: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Row(
            Modifier.align(if (bitmap != null) Alignment.BottomCenter else Alignment.Center).padding(bottom = if (bitmap != null) 28.dp else 0.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!failed) {
                CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (failed) "Camera unavailable" else "Connecting…", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun WebRtcVideo(graph: AppGraph, entityId: String, onFirstFrame: () -> Unit, onFail: () -> Unit) {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    val holder = remember { object { var renderer: SurfaceViewRenderer? = null; var done = false } }
    var rendered by remember { mutableStateOf(false) }
    val failOnce = { if (!holder.done) { holder.done = true; main.post { onFail() } } }
    AndroidView(factory = { SurfaceViewRenderer(it).also { r -> holder.renderer = r } }, modifier = Modifier.fillMaxSize())
    DisposableEffect(entityId) {
        val renderer = holder.renderer
        if (renderer == null) {
            onDispose { }
        } else {
            val session = HaWebRtcSession(context.applicationContext, graph.cameras, entityId)
            session.start(renderer, onFirstFrame = { rendered = true; onFirstFrame() }, onFailed = { failOnce() })
            onDispose { session.close(renderer) }
        }
    }
    LaunchedEffect(entityId) { delay(12_000); if (!rendered) failOnce() }
}

@Composable
private fun HlsVideo(graph: AppGraph, entityId: String, onFirstFrame: () -> Unit, onFail: () -> Unit) {
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var aspect by remember { mutableStateOf<Float?>(null) }
    var rendered by remember { mutableStateOf(false) }
    LaunchedEffect(entityId) {
        val url = graph.cameras.hlsUrl(entityId) ?: return@LaunchedEffect onFail()
        val p = graph.cameras.hlsPlayer(url) ?: return@LaunchedEffect onFail()
        p.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) aspect = size.width * size.pixelWidthHeightRatio / size.height
            }
            override fun onRenderedFirstFrame() { rendered = true; onFirstFrame() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { onFail() }
        })
        player = p
        delay(15_000)
        if (!rendered) onFail()
    }
    DisposableEffect(entityId) { onDispose { player?.release() } }
    val p = player ?: return
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val a = aspect
        val m = when {
            a == null -> Modifier.fillMaxSize()
            maxWidth / maxHeight < a -> Modifier.fillMaxWidth().aspectRatio(a)
            else -> Modifier.aspectRatio(a).fillMaxSize()
        }
        AndroidView(factory = { SurfaceView(it).also { v -> p.setVideoSurfaceView(v) } }, modifier = m)
    }
}

@Composable
private fun MjpegVideo(graph: AppGraph, entityId: String, onFirstFrame: () -> Unit, onFail: () -> Unit) {
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    DisposableEffect(entityId) {
        var first = true
        val job = graph.cameras.streamMjpeg(entityId, onFrame = {
            frame = it
            graph.cameras.rememberFrame(entityId, it)
            if (first) { first = false; onFirstFrame() }
        }, onError = { if (first) onFail() })
        onDispose { job.cancel() }
    }
    frame?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
}

/** Full-screen camera viewer. */
@Composable
fun CameraViewer(graph: AppGraph, entityId: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val e = graph.store.entity(entityId).collectAsState().value
    var transport by remember { mutableStateOf<Transport?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LiveCameraView(graph, entityId, Modifier.fillMaxSize()) { transport = it }
        Row(Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text(e?.friendlyName ?: "Camera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            val label = when (transport) {
                Transport.WEBRTC, Transport.HLS -> "LIVE"
                Transport.MJPEG -> "STREAM"
                null -> null
            }
            if (label != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(if (label == "LIVE") Color(0xFFD32F2F) else Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(5.dp))
                    Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

/** A camera card: live-refreshing snapshot (every 10s while visible), name overlay. */
@Composable
fun CameraCard(graph: AppGraph, entityId: String, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val e = graph.store.entity(entityId).collectAsState().value
    val snap = graph.cameras.snapshots[entityId]
    LaunchedEffect(entityId) {
        graph.cameras.cachedSnapshot(entityId)
        while (true) {
            graph.cameras.refreshSnapshot(entityId, 640)
            delay(10_000)
        }
    }
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { graph.cameras.prewarm(entityId); onOpen() },
    ) {
        if (snap != null) Image(snap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(e?.friendlyName ?: entityId, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            if (e?.isUnavailable == true) Text("Unavailable", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

