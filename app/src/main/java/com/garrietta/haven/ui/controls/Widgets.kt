package com.garrietta.haven.ui.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.core.model.HistoryPoint
import com.garrietta.haven.ui.theme.pressBounce
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Local-value holder for continuous controls: follows the finger while
 * dragging, holds the committed value for [holdMs] after release (so an
 * intermediate server update can't snap it back), then re-syncs to [server].
 */
@Composable
fun rememberHeldValue(server: Float, holdMs: Long = 2_500): HeldValue {
    val held = remember { HeldValue(server) }
    LaunchedEffect(server, held.holdUntil) {
        val wait = held.holdUntil - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        if (!held.dragging) held.value = server
    }
    held.holdMs = holdMs
    return held
}

class HeldValue(initial: Float) {
    var value by mutableFloatStateOf(initial)
    var dragging by mutableStateOf(false)
    var holdUntil by mutableLongStateOf(0L)
    var holdMs = 2_500L
    fun release() { dragging = false; holdUntil = System.currentTimeMillis() + holdMs }
}

/**
 * Big pill slider (Haven's control language): a 64dp tonal track with a
 * filled portion and the value printed inside. Commits on release.
 */
@Composable
fun PillSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    fill: Color,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    label: (Float) -> String,
    icon: ImageVector? = null,
    height: Dp = 64.dp,
    steps: Float = 1f,
    trackBrush: Brush? = null,
    onCommit: (Float) -> Unit,
) {
    val held = rememberHeldValue(value)
    val haptics = LocalHapticFeedback.current
    val span = range.endInclusive - range.start
    val frac = ((held.value - range.start) / span).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(frac, label = "sliderFill")
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2))
            .then(if (trackBrush != null) Modifier.background(trackBrush) else Modifier.background(track))
            .pointerInput(range) {
                fun valueAt(x: Float): Float {
                    val raw = range.start + (x / size.width).coerceIn(0f, 1f) * span
                    return ((raw / steps).roundToInt() * steps).coerceIn(range.start, range.endInclusive)
                }
                detectDragGestures(
                    onDragStart = { held.dragging = true; held.value = valueAt(it.x) },
                    onDragEnd = { held.release(); haptics.performHapticFeedback(HapticFeedbackType.ContextClick); onCommit(held.value) },
                    onDragCancel = { held.release() },
                ) { change, _ ->
                    val v = valueAt(change.position.x)
                    if (v != held.value && (v == range.start || v == range.endInclusive)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    held.value = v
                }
            }
            .pointerInput(range) {
                detectTapGestures { pos ->
                    val raw = range.start + (pos.x / size.width).coerceIn(0f, 1f) * span
                    held.value = ((raw / steps).roundToInt() * steps).coerceIn(range.start, range.endInclusive)
                    held.release()
                    onCommit(held.value)
                }
            },
    ) {
        if (trackBrush == null) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(animFrac.coerceAtLeast(0.001f)).clip(RoundedCornerShape(height / 2)).background(fill))
        } else {
            // Gradient tracks (color temperature) get a thumb ring instead of a fill.
            val x = maxWidth * animFrac
            Box(Modifier.padding(start = (x - 6.dp).coerceIn(4.dp, maxWidth - 16.dp)).align(Alignment.CenterStart).size(12.dp, height - 16.dp).clip(RoundedCornerShape(6.dp)).background(Color.White))
        }
        Row(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
            Box(Modifier.weight(1f))
            Text(label(held.value), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Pressable tonal pill button (transport, steppers, cover controls). */
@Composable
fun RowScope.PillButton(
    icon: ImageVector? = null,
    text: String? = null,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    weight: Float = 1f,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, pressBounce(), label = "pillBtn")
    val bg by animateColorAsState(if (enabled) container else container.copy(alpha = 0.4f), label = "pillBtnBg")
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier.weight(weight).height(height).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(height / 2)).background(bg)
            .clickable(interaction, indication = null, enabled = enabled) { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, text, Modifier.size(24.dp), tint = content.copy(alpha = if (enabled) 1f else 0.5f))
            if (icon != null && text != null) Box(Modifier.size(8.dp))
            if (text != null) Text(text, fontWeight = FontWeight.SemiBold, color = content.copy(alpha = if (enabled) 1f else 0.5f))
        }
    }
}

/** Line chart with a soft gradient fill. */
@Composable
fun HistoryChart(points: List<HistoryPoint>, color: Color, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No history yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val minV = points.minOf { it.value }
    val maxV = points.maxOf { it.value }
    val t0 = points.first().timeSec
    val t1 = points.last().timeSec
    Canvas(modifier) {
        val vSpan = (maxV - minV).takeIf { it > 1e-9 } ?: 1.0
        val tSpan = (t1 - t0).takeIf { it > 0 } ?: 1.0
        fun pt(p: HistoryPoint) = Offset(
            ((p.timeSec - t0) / tSpan * size.width).toFloat(),
            (size.height - (p.value - minV) / vSpan * size.height * 0.85 - size.height * 0.075).toFloat(),
        )
        val line = Path()
        points.forEachIndexed { i, p -> val o = pt(p); if (i == 0) line.moveTo(o.x, o.y) else line.lineTo(o.x, o.y) }
        val area = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f))))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** 270° thermostat arc with a draggable setpoint. Returns the new value on release. */
@Composable
fun ThermostatDial(
    current: Double?,
    target: Double,
    min: Double,
    max: Double,
    step: Double,
    accent: Color,
    unit: String,
    modifier: Modifier = Modifier,
    onCommit: (Double) -> Unit,
) {
    val held = rememberHeldValue(target.toFloat())
    val haptics = LocalHapticFeedback.current
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurface
    fun snap(v: Double) = ((v / step).roundToInt() * step).coerceIn(min, max)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxWidth().height(260.dp).pointerInput(min, max) {
                detectDragGestures(
                    onDragStart = { held.dragging = true },
                    onDragEnd = { held.release(); onCommit(held.value.toDouble()) },
                    onDragCancel = { held.release() },
                ) { change, _ ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val d = change.position - c
                    var ang = Math.toDegrees(kotlin.math.atan2(d.y.toDouble(), d.x.toDouble())) // -180..180, 0 = east
                    // Arc runs from 135° (bottom-left) clockwise through 270 sweep.
                    var rel = (ang - 135.0 + 360.0) % 360.0
                    if (rel > 270) rel = if (rel > 315) 0.0 else 270.0
                    val v = snap(min + rel / 270.0 * (max - min))
                    if (v.toFloat() != held.value) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    held.value = v.toFloat()
                }
            },
        ) {
            val stroke = 22.dp.toPx()
            val r = kotlin.math.min(size.width, size.height) / 2f - stroke
            val topLeft = Offset(size.width / 2f - r, size.height / 2f - r)
            val arc = Size(r * 2, r * 2)
            drawArc(track, 135f, 270f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            val frac = ((held.value - min) / (max - min)).toFloat().coerceIn(0f, 1f)
            drawArc(accent, 135f, 270f * frac, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            val a = Math.toRadians(135.0 + 270.0 * frac)
            val knob = Offset(size.width / 2f + (r * kotlin.math.cos(a)).toFloat(), size.height / 2f + (r * kotlin.math.sin(a)).toFloat())
            drawCircle(Color.White, stroke * 0.62f, knob)
            drawCircle(accent, stroke * 0.36f, knob)
            if (current != null) {
                val cf = ((current - min) / (max - min)).toFloat().coerceIn(0f, 1f)
                val ca = Math.toRadians(135.0 + 270.0 * cf)
                val p = Offset(size.width / 2f + (r * kotlin.math.cos(ca)).toFloat(), size.height / 2f + (r * kotlin.math.sin(ca)).toFloat())
                drawCircle(onSurface, 4.dp.toPx(), p)
            }
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                com.garrietta.haven.ui.fmtNum(held.value.toDouble()) + "°",
                fontSize = 56.sp, fontWeight = FontWeight.Bold, color = onSurface,
            )
            if (current != null) Text("Now ${com.garrietta.haven.ui.fmtNum(current)}$unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
