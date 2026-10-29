package com.garrietta.haven.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.coolSetpointContainer
import com.garrietta.haven.ui.components.coolSetpointOnContainer
import com.garrietta.haven.ui.components.heatSetpointContainer
import com.garrietta.haven.ui.components.heatSetpointOnContainer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The spec's thermostat dial. It reworks run #38's dial under the standing
 * 1-degree rule (Garrett's 2026-10-29 decision): the finger drags
 * continuously around a 240-degree sweep, but the APPLIED value always
 * snaps to whole degrees; steppers move ±1; an inside-face tap still
 * toggles the heating↔cooling selector; only drags that start on a thumb
 * change values.
 *
 * - 240-degree sweep with a 120-degree gap centered on the bottom (start
 *   150 degrees, end 390 = 30 degrees). The shared [Dial] stays at 270
 *   degrees for the sheet dial and the weather dial; its tests are
 *   untouched.
 * - Dual floating pill thumbs on the same track in heat_cool mode, with
 *   the range between them shaded via the dynamic container blend
 *   (tertiaryContainer -> primaryContainer). Single modes get one thumb.
 * - Mode arcs: Off = dashed track at 0.38 opacity, monochromatic caps;
 *   Heating = saturated amber-orange -> deep crimson gradient with an
 *   outward bloom behind the thumb; Cooling = cyan -> cobalt gradient.
 * - Needle spring (0.72 / StiffnessLow) on every thumb angle, so snapped
 *   values still travel smoothly.
 * - Rubber-banding: pushing the finger into the bottom gap strains the
 *   grabbed thumb past the nearer end (overshoot damped to 35%) instead
 *   of jumping; the applied value never moves in the gap.
 * - Gap-proofing holds for drawn thumbs too: a drawn heat thumb never
 *   crosses the cool thumb, and vice versa.
 *
 * Containers use the token pipeline (heat thumb: errorContainer in
 * single-heat mode, transitioning smoothly to tertiaryContainer in
 * dual-band mode; cool thumb: primaryContainer). The arc gradients and
 * the hero numbers keep the spec-named temperature hues and run #38's
 * Google-Home orange/blue — container pastels are unreadable as dial
 * text and thumbs, so the tokens live where containers exist.
 */
private val HEAT_ARC_START = Color(0xFFFF8F00)
private val HEAT_ARC_END = Color(0xFFC62828)
private val COOL_ARC_START = Color(0xFF00BCD4)
private val COOL_ARC_END = Color(0xFF1565C0)

@Composable
fun ThermostatDial(
    heat: Float,
    cool: Float,
    single: Float,
    dual: Boolean,
    current: Float?,
    currentLabel: String?,
    range: ClosedFloatingPointRange<Float>,
    mode: String,
    selector: SetpointSelector,
    onHeatChange: (Float) -> Unit,
    onCoolChange: (Float) -> Unit,
    onSingleChange: (Float) -> Unit,
    onFinished: (SetpointSelector, Float) -> Unit,
    onInsideTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    diameter: Dp = 300.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    // Animated heat-thumb container: errorContainer in single-heat mode,
    // transitioning smoothly to tertiaryContainer in dual-band mode.
    val heatContainer by animateColorAsState(
        heatSetpointContainer(scheme, dual),
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
        label = "heatThumbContainer",
    )
    val heatOnContainer = heatSetpointOnContainer(scheme, dual)
    val coolContainer = coolSetpointContainer(scheme)
    val coolOnContainer = coolSetpointOnContainer(scheme)
    val needleSpec = if (reduceMotion) spring<Float>(stiffness = 10_000f, dampingRatio = 1f)
        else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)

    // Latest values/callbacks for the gesture handlers. The pointerInput
    // keys must NOT include values that change mid-drag, or the drag
    // restarts on every update.
    val heatState = rememberUpdatedState(heat)
    val coolState = rememberUpdatedState(cool)
    val singleState = rememberUpdatedState(single)
    val onHeatChangeState = rememberUpdatedState(onHeatChange)
    val onCoolChangeState = rememberUpdatedState(onCoolChange)
    val onSingleChangeState = rememberUpdatedState(onSingleChange)
    val onFinishedState = rememberUpdatedState(onFinished)
    val onInsideTapState = rememberUpdatedState(onInsideTap)

    var grabbed by remember { mutableStateOf<SetpointSelector?>(null) }
    var strain by remember { mutableStateOf<Float?>(null) }
    var lastApplied by remember { mutableStateOf(0f) }

    // Drawn values: the strained value while the finger pushes into the
    // bottom gap, else the applied value — gap-proofed so a drawn heat
    // thumb never crosses the cool thumb, and vice versa.
    val drawnHeat = ((if (grabbed == SetpointSelector.HEATING) strain else null) ?: heat)
        .let { if (dual) minOf(it, cool) else it }
    val drawnCool = ((if (grabbed == SetpointSelector.COOLING) strain else null) ?: cool)
        .let { if (dual) maxOf(it, heat) else it }
    val drawnSingle = (if (grabbed == SetpointSelector.HEATING && !dual) strain else null) ?: single
    val animatedHeatAngle by animateFloatAsState(dial240AngleForValue(drawnHeat, range), needleSpec, label = "heatAngle")
    val animatedCoolAngle by animateFloatAsState(dial240AngleForValue(drawnCool, range), needleSpec, label = "coolAngle")
    val animatedSingleAngle by animateFloatAsState(dial240AngleForValue(drawnSingle, range), needleSpec, label = "singleAngle")

    fun trackRadiusPx(sizePx: Float): Float = sizePx / 2f - with(density) { 22.dp.toPx() }

    fun thumbCenter(angleDeg: Float, radius: Float, cx: Float, cy: Float): Offset {
        val a = Math.toRadians(angleDeg.toDouble()).toFloat()
        return Offset(cx + radius * cos(a), cy + radius * sin(a))
    }

    fun DrawScope.drawThumb(center: Offset, fill: Color, dot: Color, active: Boolean) {
        val w = 30.dp.toPx()
        val h = 20.dp.toPx()
        // Outward bloom behind the thumb.
        drawCircle(color = fill.copy(alpha = 0.25f), radius = 24.dp.toPx(), center = center)
        drawRoundRect(
            color = fill.copy(alpha = if (active) 1f else 0.55f),
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = if (active) 0.9f else 0.5f),
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(color = dot.copy(alpha = if (active) 1f else 0.55f), radius = 4.dp.toPx(), center = center)
    }

    Canvas(
        modifier
            .size(diameter)
            .pointerInput(dual, range, mode) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val trackRadius = trackRadiusPx(size.minDimension)
                    if (dialTapZone(offset.x, offset.y, cx, cy, trackRadius, 30.dp.toPx()) == DialTapZone.INSIDE) {
                        onInsideTapState.value?.invoke()
                    }
                }
            }
            .pointerInput(dual, range, mode) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val trackRadius = trackRadiusPx(size.minDimension)
                        val grabR = 32.dp.toPx()
                        fun distTo(angleDeg: Float): Float {
                            val c = thumbCenter(angleDeg, trackRadius, cx, cy)
                            val dx = offset.x - c.x
                            val dy = offset.y - c.y
                            return sqrt(dx * dx + dy * dy)
                        }
                        grabbed = if (dual) {
                            val dh = distTo(dial240AngleForValue(heatState.value, range))
                            val dc = distTo(dial240AngleForValue(coolState.value, range))
                            when {
                                dh <= grabR && dc <= grabR ->
                                    if (dh <= dc) SetpointSelector.HEATING else SetpointSelector.COOLING
                                dh <= grabR -> SetpointSelector.HEATING
                                dc <= grabR -> SetpointSelector.COOLING
                                else -> null
                            }
                        } else {
                            if (distTo(dial240AngleForValue(singleState.value, range)) <= grabR) {
                                SetpointSelector.HEATING
                            } else {
                                null
                            }
                        }
                        grabbed?.let { g ->
                            lastApplied = if (dual) {
                                if (g == SetpointSelector.HEATING) heatState.value else coolState.value
                            } else {
                                singleState.value
                            }
                            strain = null
                        }
                    },
                    onDragEnd = {
                        grabbed?.let { g -> onFinishedState.value(g, lastApplied) }
                        grabbed = null
                        strain = null
                    },
                    onDragCancel = {
                        grabbed = null
                        strain = null
                    },
                    onDrag = { change, _ ->
                        val g = grabbed ?: return@detectDragGestures
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val angleDeg = (Math.toDegrees(
                            atan2(
                                (change.position.y - cy).toDouble(),
                                (change.position.x - cx).toDouble(),
                            ),
                        ).toFloat() + 360f) % 360f
                        val rel = (angleDeg - DIAL240_START_DEG + 360f) % 360f
                        val span = range.endInclusive - range.start
                        if (rel <= DIAL240_SWEEP_DEG) {
                            strain = null
                            val raw = range.start + (rel / DIAL240_SWEEP_DEG) * span
                            val snapped = snapToDegree(raw.toDouble()).toFloat()
                            val applied = if (dual) {
                                if (g == SetpointSelector.HEATING) {
                                    clampHeatSetpoint(snapped.toDouble(), range.start.toDouble(), coolState.value.toDouble()).toFloat()
                                } else {
                                    clampCoolSetpoint(snapped.toDouble(), heatState.value.toDouble(), range.endInclusive.toDouble()).toFloat()
                                }
                            } else {
                                snapped.coerceIn(range)
                            }
                            lastApplied = applied
                            if (dual) {
                                if (g == SetpointSelector.HEATING) onHeatChangeState.value(applied)
                                else onCoolChangeState.value(applied)
                            } else {
                                onSingleChangeState.value(applied)
                            }
                        } else {
                            // Bottom gap: strain past the nearer end instead
                            // of jumping. The gap spans 120 degrees from the
                            // max end (30) around the bottom to the min end
                            // (150); its midpoint (90) splits the sides.
                            val gapAngle = (angleDeg - 30f + 360f) % 360f
                            val overshoot = if (gapAngle <= 60f) {
                                gapAngle / DIAL240_SWEEP_DEG * span
                            } else {
                                -(120f - gapAngle) / DIAL240_SWEEP_DEG * span
                            }
                            val base = if (overshoot >= 0f) range.endInclusive else range.start
                            strain = rubberBandedValue(base + overshoot, range)
                        }
                    },
                )
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val trackRadius = trackRadiusPx(size.minDimension)
        val stroke = 12.dp.toPx()
        val topLeft = Offset(cx - trackRadius, cy - trackRadius)
        val arcSize = Size(trackRadius * 2f, trackRadius * 2f)
        when (mode) {
            "off" -> {
                drawArc(
                    color = scheme.onSurfaceVariant.copy(alpha = 0.38f),
                    startAngle = DIAL240_START_DEG,
                    sweepAngle = DIAL240_SWEEP_DEG,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Butt,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                    ),
                )
            }
            "heat" -> {
                val steps = 60
                for (i in 0 until steps) {
                    drawArc(
                        color = lerp(HEAT_ARC_START, HEAT_ARC_END, i / (steps - 1).toFloat()),
                        startAngle = DIAL240_START_DEG + i * (DIAL240_SWEEP_DEG / steps),
                        sweepAngle = DIAL240_SWEEP_DEG / steps + 0.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            "cool" -> {
                val steps = 60
                for (i in 0 until steps) {
                    drawArc(
                        color = lerp(COOL_ARC_START, COOL_ARC_END, i / (steps - 1).toFloat()),
                        startAngle = DIAL240_START_DEG + i * (DIAL240_SWEEP_DEG / steps),
                        sweepAngle = DIAL240_SWEEP_DEG / steps + 0.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            "heat_cool" -> {
                drawArc(
                    color = scheme.surfaceVariant,
                    startAngle = DIAL240_START_DEG,
                    sweepAngle = DIAL240_SWEEP_DEG,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // The shaded range between the thumbs, blended across the
                // dynamic containers (tertiary -> primary).
                val from = animatedHeatAngle
                val to = animatedCoolAngle
                val steps = 40
                for (i in 0 until steps) {
                    drawArc(
                        color = lerp(heatContainer, coolContainer, i / (steps - 1).toFloat()),
                        startAngle = from + (to - from) * i / steps,
                        sweepAngle = (to - from) / steps + 0.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke + 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            else -> {
                drawArc(
                    color = scheme.surfaceVariant,
                    startAngle = DIAL240_START_DEG,
                    sweepAngle = DIAL240_SWEEP_DEG,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        // Current-temperature dot on the track, and its label at the top
        // of the ring (Google-Home style, as run #38 had it).
        if (current != null) {
            val cPos = thumbCenter(dial240AngleForValue(current, range), trackRadius, cx, cy)
            drawCircle(color = scheme.onSurfaceVariant, radius = 5.dp.toPx(), center = cPos)
        }
        if (currentLabel != null) {
            val layout = textMeasurer.measure(
                currentLabel,
                TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant),
            )
            drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - trackRadius + 28.dp.toPx()))
        }
        // Pill thumbs on the track.
        if (dual) {
            drawThumb(
                thumbCenter(animatedHeatAngle, trackRadius, cx, cy),
                heatContainer,
                heatOnContainer,
                selector == SetpointSelector.HEATING,
            )
            drawThumb(
                thumbCenter(animatedCoolAngle, trackRadius, cx, cy),
                coolContainer,
                coolOnContainer,
                selector == SetpointSelector.COOLING,
            )
        } else {
            val (fill, dot) = when (mode) {
                "heat" -> heatSetpointContainer(scheme, false) to heatSetpointOnContainer(scheme, false)
                "cool" -> coolSetpointContainer(scheme) to coolSetpointOnContainer(scheme)
                else -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
            }
            drawThumb(thumbCenter(animatedSingleAngle, trackRadius, cx, cy), fill, dot, true)
        }
    }
}
