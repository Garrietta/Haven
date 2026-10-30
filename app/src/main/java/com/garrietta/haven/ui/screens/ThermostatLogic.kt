package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.HaEntity
import kotlin.math.roundToInt

/**
 * Pure helpers for the thermostat full page. Kept Compose-free so they are
 * unit-testable, and so the page never invents state: every rule below
 * reads only what Home Assistant actually exposes.
 */

/** Fraction of [value] within [min]..[max], clamped to 0..1; a zero span yields 0. */
fun rangeFraction(value: Double, min: Double, max: Double): Float {
    val span = max - min
    if (span <= 0.0) return 0f
    return ((value - min) / span).toFloat().coerceIn(0f, 1f)
}

/** Human label for an HA hvac_action; null when the action is absent or blank. */
fun hvacActionLabel(action: String?): String? = when (action) {
    "heating" -> "Heating"
    "cooling" -> "Cooling"
    "idle" -> "Idle"
    "fan" -> "Fan only"
    "drying" -> "Drying"
    "preheating" -> "Preheating"
    null, "" -> null
    else -> action.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * The preset to show on the Schedule card: the climate entity's own
 * preset_mode, else a select entity named "<climate object>_current_mode"
 * (Ecobee's comfort profile). HA exposes no next-schedule transition for
 * these thermostats, so the current preset is shown instead of an invented
 * schedule line. Null when neither exists.
 */
fun findPreset(states: Map<String, HaEntity>, climate: HaEntity): String? {
    climate.attrString("preset_mode")?.takeIf { it.isNotBlank() }?.let { return it }
    val objectId = climate.entityId.substringAfter(".")
    return states["select.${objectId}_current_mode"]?.state
        ?.takeIf { it.isNotBlank() && it != "unknown" && it != "unavailable" }
}

/**
 * A button that clears a hold (Ecobee exposes button.<device>_clear_hold).
 * Prefers the button sharing the climate entity's object prefix, else any
 * *_clear_hold button, else null. The Hold card only renders when one is found.
 */
fun findClearHoldButton(states: Map<String, HaEntity>, climate: HaEntity): HaEntity? {
    val candidates = states.values.filter {
        it.domain == "button" && it.entityId.substringAfter(".").endsWith("_clear_hold")
    }
    if (candidates.isEmpty()) return null
    val objectId = climate.entityId.substringAfter(".")
    return candidates.firstOrNull { it.entityId.substringAfter(".").startsWith(objectId + "_") }
        ?: candidates.sortedBy { it.entityId }.first()
}

/**
 * Sensors linked to a climate entity: the tab-seeding ids first
 * (sensor.<object>_current_temperature / _current_humidity), else sensor
 * entities sharing the climate object prefix with a numeric state. Empty
 * means the Sensors card hides — no invented linkage.
 */
fun findLinkedSensors(states: Map<String, HaEntity>, climate: HaEntity): List<HaEntity> {
    val objectId = climate.entityId.substringAfter(".")
    val seeded = listOf(
        "sensor.${objectId}_current_temperature",
        "sensor.${objectId}_current_humidity",
    ).mapNotNull { states[it] }
    if (seeded.isNotEmpty()) return seeded
    return states.values.filter { e ->
        e.domain == "sensor" &&
            e.entityId.substringAfter(".").startsWith(objectId + "_") &&
            e.state.toDoubleOrNull() != null
    }.sortedBy { it.entityId }
}

/**
 * Which setpoint the thermostat dial edits in heat/cool mode. The dial
 * shows the active one large; an inside tap toggles between them.
 */
enum class SetpointSelector { HEATING, COOLING }

/** The other selector: an inside tap flips heating ↔ cooling. */
fun toggleSelector(selector: SetpointSelector): SetpointSelector =
    if (selector == SetpointSelector.HEATING) SetpointSelector.COOLING else SetpointSelector.HEATING

/**
 * The selector a heat/cool dial starts on: the cooling selector only when
 * the thermostat is currently cooling, otherwise heating (covers heat,
 * heat_cool, off, and unknown modes).
 */
fun defaultSelector(hvacMode: String?): SetpointSelector =
    if (hvacMode == "cool") SetpointSelector.COOLING else SetpointSelector.HEATING

/** All thermostat changes happen in whole degrees: round to the nearest. */
fun snapToDegree(value: Double): Double = value.roundToInt().toDouble()

/**
 * Clamp a heating setpoint to whole degrees inside [min]..[high]: it can
 * never pass the cooling setpoint (gap-proofing) or leave the real
 * setpoint range. Null bounds are skipped, never invented.
 */
fun clampHeatSetpoint(value: Double, min: Double?, high: Double): Double {
    var v = snapToDegree(value).coerceAtMost(high)
    if (min != null) v = v.coerceAtLeast(min)
    return v
}

/** Mirror of [clampHeatSetpoint] for the cooling setpoint ([low]..[max]). */
fun clampCoolSetpoint(value: Double, low: Double, max: Double?): Double {
    var v = snapToDegree(value).coerceAtLeast(low)
    if (max != null) v = v.coerceAtMost(max)
    return v
}

/**
 * The dial's 270-degree sweep maps an angle (degrees, 0 = east, clockwise
 * positive in screen coordinates after the atan2 normalization the Dial
 * performs) to a value: the sweep starts at 135 degrees and runs 270
 * degrees, leaving the gap at the bottom. Null inside the gap — the caller
 * ignores the touch there instead of snapping to an edge.
 */
fun dialValueFromAngle(deg: Float, range: ClosedFloatingPointRange<Float>): Float? {
    val rel = (deg - 135f + 360f) % 360f
    if (rel > 270f) return null
    return (range.start + (rel / 270f) * (range.endInclusive - range.start))
        .coerceIn(range)
}

// ---------------------------------------------------------------------------
// The spec's 240-degree dial (ThermostatDial). The shared Dial above stays
// at 270 degrees — its tests are untouched — while this dial carries the
// spec: a 240-degree sweep with a 120-degree gap centered on the bottom
// (start 150 degrees, end 390 degrees = 30 degrees), dual pill thumbs,
// rubber-banding past the ends, and 1-degree snapping underneath.
// ---------------------------------------------------------------------------

/** Start angle of the 240-degree sweep, in screen degrees (0 = east, clockwise). */
const val DIAL240_START_DEG = 150f

/** Sweep of the 240-degree dial; the remaining 120 degrees is the bottom gap. */
const val DIAL240_SWEEP_DEG = 240f

/**
 * Maps a screen angle (degrees, 0 = east, clockwise, y down) to a dial value
 * on the 240-degree sweep, or null when the angle falls in the bottom gap.
 * Unlike the 270-degree dial, this does NOT clamp: a gap angle is a gap
 * angle, and the caller keeps the previous value.
 */
fun dial240ValueFromAngle(deg: Float, range: ClosedFloatingPointRange<Float>): Float? {
    val rel = (deg - DIAL240_START_DEG + 360f) % 360f
    if (rel > DIAL240_SWEEP_DEG) return null
    return range.start + (rel / DIAL240_SWEEP_DEG) * (range.endInclusive - range.start)
}

/** The screen angle for a dial value on the 240-degree sweep. */
fun dial240AngleForValue(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    return DIAL240_START_DEG + fraction * DIAL240_SWEEP_DEG
}

/**
 * Staggered radii for the dual dial thumbs (2026-10-30 verdict:
 * the heat + cool pills collide at the top right of the ring when
 * setpoints are 2 degrees apart, e.g. 66/68). The pills are
 * axis-aligned 30x20dp rects, so when their centers' track distance
 * drops below the pill width + gap they would overlap: the heat
 * thumb moves inward and the cool thumb outward by [offset], which
 * ramps smoothly from 0 (at [needed] + [rampPx] of track distance)
 * to the full half-height stagger (at [needed] or closer) — no pop
 * as a drag crosses the threshold, and at the reported top-of-ring
 * position the 24dp radial separation clears the 20dp pill height
 * outright. Values, grab targets, and the dial model are untouched:
 * only the drawn radii move, and the grab test uses these same
 * radii, so taps still land on the thumb they see. Pure.
 */
fun dualThumbRadii(
    heatAngleDeg: Float,
    coolAngleDeg: Float,
    trackRadiusPx: Float,
    thumbWidthPx: Float,
    thumbHeightPx: Float,
    gapPx: Float,
    rampPx: Float,
): Pair<Float, Float> {
    val diffDeg = kotlin.math.abs(heatAngleDeg - coolAngleDeg).coerceAtMost(DIAL240_SWEEP_DEG)
    val arcDist = trackRadiusPx * Math.toRadians(diffDeg.toDouble()).toFloat()
    val needed = thumbWidthPx + gapPx
    val fullOffset = (thumbHeightPx + gapPx) / 2f
    val offset = when {
        arcDist >= needed + rampPx -> 0f
        arcDist <= needed -> fullOffset
        else -> fullOffset * (needed + rampPx - arcDist) / rampPx
    }
    return (trackRadiusPx - offset) to (trackRadiusPx + offset)
}

/**
 * Rubber-banding past the dial ends: values inside the range pass through
 * unchanged; overshoot past either end is damped to 35% of the excess, so
 * the thumb resists instead of pinning hard. The APPLIED value is still
 * snapped to whole degrees and clamped (the 1-degree rule stands); only the
 * drawn thumb uses the rubber-banded value, so the needle visibly strains
 * against the ends while the finger goes past them.
 */
fun rubberBandedValue(raw: Float, range: ClosedFloatingPointRange<Float>): Float =
    when {
        raw < range.start -> range.start + (raw - range.start) * 0.35f
        raw > range.endInclusive -> range.endInclusive + (raw - range.endInclusive) * 0.35f
        else -> raw
    }

/** Where a dial tap landed, in pure pixel geometry (no Compose types). */
enum class DialTapZone { INSIDE, RING, OUTSIDE }

/**
 * Classify a dial tap by its distance from the dial center: inside the
 * face (clear of the ring by [ringTolerancePx]) is INSIDE and toggles the
 * selector; on the ring (within tolerance of the track radius) is RING
 * and does nothing; beyond the ring is OUTSIDE and does nothing. Only
 * INSIDE ever fires a tap action — taps never change temperature.
 */
fun dialTapZone(
    touchX: Float,
    touchY: Float,
    centerX: Float,
    centerY: Float,
    trackRadiusPx: Float,
    ringTolerancePx: Float,
): DialTapZone {
    val dx = touchX - centerX
    val dy = touchY - centerY
    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
    return when {
        dist < trackRadiusPx - ringTolerancePx -> DialTapZone.INSIDE
        dist <= trackRadiusPx + ringTolerancePx -> DialTapZone.RING
        else -> DialTapZone.OUTSIDE
    }
}

/** True when a drag start is close enough to the handle center to grab it. */
fun isHandleGrab(
    touchX: Float,
    touchY: Float,
    handleX: Float,
    handleY: Float,
    grabRadiusPx: Float,
): Boolean {
    val dx = touchX - handleX
    val dy = touchY - handleY
    return kotlin.math.sqrt(dx * dx + dy * dy) <= grabRadiusPx
}

/**
 * Google Home-style mode label for the Mode pill: "heat_cool" reads
 * "Heat • Cool" (the bullet matches Google Home's "Heat • Cool" pill),
 * everything else capitalizes the same way [hvacActionLabel] does.
 */
fun formatHvacMode(mode: String): String = when (mode) {
    "heat_cool" -> "Heat • Cool"
    "heat" -> "Heat"
    "cool" -> "Cool"
    "off" -> "Off"
    "auto" -> "Auto"
    "dry" -> "Dry"
    "fan_only" -> "Fan only"
    else -> mode.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * The outdoor temperature for the Google Home-style Indoor/Outdoor rows,
 * from real HA state only — never invented. Precedence: (1) the seeded
 * sensor "<climate object>_outdoor_temperature"; (2) any sensor whose id
 * or friendly name mentions "outdoor" and whose state is a temperature
 * reading (see [parseSensorTemp] — an outdoor humidity sensor never
 * qualifies); (3) the temperature attribute of a lone weather entity.
 * Null when nothing qualifies, and then the Outdoor row hides.
 */
fun findOutdoorTemperature(states: Map<String, HaEntity>, climate: HaEntity): Double? {
    val objectId = climate.entityId.substringAfter(".")
    states["sensor.${objectId}_outdoor_temperature"]?.let { parseSensorTemp(it) }?.let { return it }
    val outdoorSensors = states.values.filter { e ->
        e.domain == "sensor" &&
            (e.entityId.contains("outdoor", ignoreCase = true) ||
                e.friendlyName.contains("outdoor", ignoreCase = true))
    }.sortedBy { it.entityId }
    for (sensor in outdoorSensors) {
        parseSensorTemp(sensor)?.let { return it }
    }
    val weather = states.values.filter { it.domain == "weather" }
    if (weather.size == 1) {
        weather.first().attrDouble("temperature")?.let { return it }
    }
    return null
}

/**
 * A sensor's state as a Fahrenheit temperature, or null when it is not a
 * temperature reading: the state must parse as a number, and the sensor
 * must look like a temperature source (device_class temperature, a °F/°C
 * unit, or "temperature" in its id or name). A °C reading is converted to
 * °F; weather attributes are already in the system unit and bypass this.
 */
private fun parseSensorTemp(sensor: HaEntity): Double? {
    val value = sensor.state.toDoubleOrNull() ?: return null
    val deviceClass = sensor.attrString("device_class")
    val unit = sensor.attrString("unit_of_measurement")
    val looksTemperature = deviceClass == "temperature" ||
        unit == "°F" || unit == "°C" || unit == "F" || unit == "C" ||
        sensor.entityId.contains("temperature", ignoreCase = true) ||
        sensor.friendlyName.contains("temperature", ignoreCase = true)
    if (!looksTemperature) return null
    return if (unit == "°C" || unit == "C") value * 9.0 / 5.0 + 32.0 else value
}
