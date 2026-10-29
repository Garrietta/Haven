package com.garrietta.haven.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.data.HistoryPoint
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.MorphShape
import com.garrietta.haven.ui.theme.havenSpring
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Provided by HavenApp from the user's settings. */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * True when the window is wide enough for two-pane layouts (unfolded
 * Fold8). The threshold is 600dp, not Material's 840dp "expanded" bucket:
 * the Fold8 unfolded is ~690dp portrait / ~830dp landscape, so 840dp
 * would miss unfolded landscape entirely. Folded (~360-400dp) stays
 * single-column.
 */
val LocalWideLayout = compositionLocalOf { false }

/** Stable per-entity accent color, hashed from the entity id (same idea as Relay's palettes). */
private val ACCENTS = listOf(
    Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF8B5CF6),
    Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF84CC16), Color(0xFF3B82F6),
    Color(0xFFEAB308), Color(0xFF14B8A6), Color(0xFFF43F5E), Color(0xFFA855F7),
)

fun accentFor(entityId: String): Color {
    val hash = entityId.hashCode()
    val idx = ((hash % ACCENTS.size) + ACCENTS.size) % ACCENTS.size
    return ACCENTS[idx]
}

/**
 * A big round dial: drag around the ring (or tap it) to set a value.
 * The ring sweeps 270 degrees, leaving the gap at the bottom.
 *
 * Two drag fixes: a touch in the bottom gap is ignored (it used to snap
 * the value to min or max as the finger crossed it, which reads as the
 * dial jumping), and every computed value is clamped to [range] so a
 * stray pointer position can never push the dial outside the thermostat's
 * real setpoint range. The handle sits exactly on the track's centerline:
 * the canvas is inset by the handle radius, so neither the track's round
 * caps nor the handle are clipped at the edges.
 */
@Composable
fun Dial(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: (Float) -> Unit = {},
    centerText: String,
    centerSub: String,
    accent: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,
    // When set, replaces the default center text with custom content
    // (the thermostat page uses it for its dual current + target numbers).
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    // Null in the bottom gap: the caller ignores the touch there.
    val valueFor: (Offset, IntSize) -> Float? = { pos, sz ->
        val cx = sz.width / 2f
        val cy = sz.height / 2f
        var deg = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
        if (deg < 0) deg += 360f
        val rel = (deg - 135f + 360f) % 360f
        if (rel > 270f) {
            null
        } else {
            (range.start + (rel / 270f) * (range.endInclusive - range.start)).coerceIn(range)
        }
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    // Last value the user actually set: onFinished reports this, not the
    // composable's value prop, so a tap applies the tapped value (not the
    // previous one) and a drag-end applies the final position even if the
    // parent has not recomposed yet.
    var lastSet by remember { mutableStateOf(value) }
    Box(
        modifier
            .size(diameter)
            .pointerInput(range) {
                detectTapGestures { offset ->
                    valueFor(offset, size)?.let {
                        lastSet = it
                        onValueChange(it)
                        onFinished(it)
                    }
                }
            }
            .pointerInput(range) {
                detectDragGestures(
                    onDragEnd = { onFinished(lastSet) },
                ) { change, _ ->
                    valueFor(change.position, size)?.let {
                        lastSet = it
                        onValueChange(it)
                    }
                }
            },
    ) {
        // Inset by the handle radius: the arc's centerline then sits at
        // size.minDimension / 2 of the inset canvas, and the handle drawn
        // at that same radius lands exactly on the track instead of 10dp
        // inside it (the old (minDimension - stroke) / 2 math).
        Canvas(Modifier.size(diameter).padding(13.dp)) {
            val stroke = 20.dp.toPx()
            drawArc(track, 135f, 270f, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            drawArc(accent, 135f, 270f * fraction, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            val angle = Math.toRadians((135.0 + 270.0 * fraction))
            val radius = size.minDimension / 2f
            drawCircle(
                accent,
                radius = 13.dp.toPx(),
                center = Offset(
                    size.width / 2f + radius * cos(angle).toFloat(),
                    size.height / 2f + radius * sin(angle).toFloat(),
                ),
            )
        }
        if (centerContent != null) {
            Box(Modifier.align(Alignment.Center)) { centerContent() }
        } else {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(centerText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(centerSub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A small history sparkline for sensor detail screens. */
@Composable
fun Sparkline(points: List<HistoryPoint>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.minOf { it.value }
        val max = points.maxOf { it.value }
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val t0 = points.first().timeMs
        val t1 = points.last().timeMs
        val tspan = (t1 - t0).takeIf { it > 0L } ?: 1L
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = (p.timeMs - t0).toFloat() / tspan * size.width
            val y = size.height - ((p.value - min) / span * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * A big pill toggle: a sliding knob on a spring, with a morphing shape behind it.
 * Used for switches and input_booleans where a plain Switch would look like a web form.
 */
@Composable
fun PillToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    onColor: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = havenSpring(),
        label = "pill",
    )
    val progress = if (reduceMotion) (if (checked) 1f else 0f) else animatedProgress
    val track = if (checked) onColor else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(width = 96.dp, height = 56.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable { onToggle() },
    ) {
        MorphShape(
            from = ExpressiveKind.BUN,
            to = ExpressiveKind.SUNNY,
            progress = progress,
            color = if (checked) Color.White.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.size(56.dp).align(Alignment.Center),
        )
        Box(
            Modifier
                .padding(start = 6.dp + 40.dp * progress, top = 6.dp)
                .size(44.dp)
                .background(Color.White, RoundedCornerShape(50)),
        )
    }
}
