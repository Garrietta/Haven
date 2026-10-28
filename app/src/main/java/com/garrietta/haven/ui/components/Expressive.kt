package com.garrietta.haven.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
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
 */
@Composable
fun Dial(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit = {},
    centerText: String,
    centerSub: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val valueFor: (Offset, IntSize) -> Float = { pos, sz ->
        val cx = sz.width / 2f
        val cy = sz.height / 2f
        var deg = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
        if (deg < 0) deg += 360f
        val rel = (deg - 135f + 360f) % 360f
        val f = if (rel <= 270f) rel / 270f else if (rel < 315f) 0f else 1f
        range.start + f * (range.endInclusive - range.start)
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(220.dp)
            .pointerInput(range) {
                detectTapGestures { offset ->
                    onValueChange(valueFor(offset, size))
                    onFinished()
                }
            }
            .pointerInput(range) {
                detectDragGestures(
                    onDragEnd = { onFinished() },
                ) { change, _ ->
                    onValueChange(valueFor(change.position, size))
                }
            },
    ) {
        Canvas(Modifier.size(220.dp)) {
            val stroke = 20.dp.toPx()
            drawArc(track, 135f, 270f, false, style = Stroke(stroke, StrokeCap.Round))
            drawArc(accent, 135f, 270f * fraction, false, style = Stroke(stroke, StrokeCap.Round))
            val angle = Math.toRadians((135.0 + 270.0 * fraction))
            val radius = (size.minDimension - stroke) / 2f
            drawCircle(
                accent,
                radius = 13.dp.toPx(),
                center = Offset(
                    size.width / 2f + radius * cos(angle).toFloat(),
                    size.height / 2f + radius * sin(angle).toFloat(),
                ),
            )
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(centerText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(centerSub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        drawPath(path, color, style = Stroke(3.dp.toPx(), StrokeCap.Round, StrokeJoin.Round))
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
