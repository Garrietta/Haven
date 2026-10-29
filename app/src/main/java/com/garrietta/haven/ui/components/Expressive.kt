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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.garrietta.haven.ui.screens.DialTapZone
import com.garrietta.haven.ui.screens.dialTapZone
import com.garrietta.haven.ui.screens.dialValueFromAngle
import com.garrietta.haven.ui.screens.isHandleGrab
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.MorphShape
import com.garrietta.haven.ui.theme.havenSpring
import com.garrietta.haven.ui.theme.polygonShape
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
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
 * One expressive shape family per domain, shared by tiles, detail pages,
 * sheets, and badges so the whole app reads as one set: lights bloom
 * (SUNNY), climate squishes (BUN), cameras flower (FLOWER), covers burst
 * (BURST), media and fans ripple (WAVE), weather and sensors cloud
 * (CLOUD). Everything else falls back to BUN.
 */
fun shapeForDomain(domain: String): ExpressiveKind = when (domain) {
    "light" -> ExpressiveKind.SUNNY
    "climate" -> ExpressiveKind.BUN
    "camera" -> ExpressiveKind.FLOWER
    "cover" -> ExpressiveKind.BURST
    "switch", "lock", "input_boolean" -> ExpressiveKind.BURST
    "media_player", "fan" -> ExpressiveKind.WAVE
    "weather", "sensor", "binary_sensor" -> ExpressiveKind.CLOUD
    else -> ExpressiveKind.BUN
}

/** Domain badge: the domain's shape in its accent, with its icon centered. */
@Composable
fun DomainBadge(domain: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.background(accent.copy(alpha = 0.22f), polygonShape(shapeForDomain(domain))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * Koda Settings menu DNA, adapted to Haven's dynamic colors (the layout /
 * surface / badge / header pattern, never Koda's hardcoded palette):
 * near-black screen background (the dynamic dark scheme already is),
 * dark rounded-rectangle rows, shaped icon badges in muted tones,
 * ALL-CAPS muted section headers, bold title + muted subtitle rows, and
 * a right chevron on rows that navigate. Used by Settings and by menu
 * rows in sheets, so every menu in the app reads as one set.
 */
@Composable
fun KodaMenuSection(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun KodaMenuRow(
    badgeKey: String,
    badgeKind: ExpressiveKind,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val accent = accentFor(badgeKey)
    val row = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 12.dp)
    Row(row.then(modifier), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.background(accent.copy(alpha = 0.20f), polygonShape(badgeKind)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A big round dial: the value changes ONLY by grabbing the radial handle
 * and dragging it around the ring. Taps never change the value — a tap
 * inside the dial face (the center, clear of the ring) reports through
 * [onInsideTap] instead (the thermostat uses it to toggle its heating /
 * cooling selector), and taps on the ring, the handle, or the corners do
 * nothing. Drags that do not start on the handle are ignored, so a stray
 * swipe across the dial cannot move the value.
 *
 * The ring sweeps 270 degrees, leaving the gap at the bottom. Two drag
 * fixes carry over: a touch in the bottom gap is ignored (it used to snap
 * the value to min or max as the finger crossed it), and every computed
 * value is clamped to [range]. When [snapToWhole] is set, drag values
 * snap to whole units (the thermostat's 1-degree intervals). The handle
 * sits exactly on the track's centerline: the canvas is inset by the
 * handle radius, so neither the track's round caps nor the handle are
 * clipped at the edges.
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
    // Fired by a tap inside the dial face; null means inside taps do
    // nothing. Never changes the value — that is handle-drag only.
    onInsideTap: (() -> Unit)? = null,
    // Snap drag values to whole units (thermostat degrees).
    snapToWhole: Boolean = false,
    // When set, replaces the default center text with custom content
    // (the thermostat page uses it for its dual current + target numbers).
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    // Read live inside the pointer-input blocks: the blocks only restart
    // when their keys change, so a plain captured fraction would go stale
    // as the value moves.
    val fractionState = rememberUpdatedState(fraction)
    // Last value the user actually set: onFinished reports this, not the
    // composable's value prop, so a drag-end applies the final position
    // even if the parent has not recomposed yet.
    var lastSet by remember { mutableStateOf(value) }
    // Whether the current drag started on the handle; reset on every drag
    // start, so only handle grabs move the value.
    var grabbing by remember { mutableStateOf(false) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(diameter)
            .pointerInput(range, snapToWhole) {
                val handleRadiusPx = 13.dp.toPx()
                val ringTolerancePx = 23.dp.toPx()
                val grabRadiusPx = handleRadiusPx + 15.dp.toPx()
                detectTapGestures { offset ->
                    val zone = dialTapZone(
                        offset.x, offset.y,
                        size.width / 2f, size.height / 2f,
                        minOf(size.width, size.height) / 2f - handleRadiusPx,
                        ringTolerancePx,
                    )
                    if (zone == DialTapZone.INSIDE) onInsideTap?.invoke()
                }
            }
            .pointerInput(range, snapToWhole) {
                val handleRadiusPx = 13.dp.toPx()
                val grabRadiusPx = handleRadiusPx + 15.dp.toPx()
                detectDragGestures(
                    onDragStart = { down ->
                        val trackRadius = minOf(size.width, size.height) / 2f - handleRadiusPx
                        val angle = Math.toRadians(135.0 + 270.0 * fractionState.value)
                        val hx = size.width / 2f + trackRadius * cos(angle).toFloat()
                        val hy = size.height / 2f + trackRadius * sin(angle).toFloat()
                        grabbing = isHandleGrab(down.x, down.y, hx, hy, grabRadiusPx)
                    },
                    onDragEnd = { if (grabbing) onFinished(lastSet) },
                    onDragCancel = { grabbing = false },
                ) { change, _ ->
                    if (!grabbing) return@detectDragGestures
                    val pos = change.position
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var deg = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
                    if (deg < 0) deg += 360f
                    dialValueFromAngle(deg, range)?.let { raw ->
                        val next = if (snapToWhole) raw.roundToInt().toFloat() else raw
                        lastSet = next
                        onValueChange(next)
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
