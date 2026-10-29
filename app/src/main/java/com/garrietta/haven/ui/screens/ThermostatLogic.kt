package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.HaEntity

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
fun snapToDegree(value: Double): Double = kotlin.math.roundToInt(value).toDouble()

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
