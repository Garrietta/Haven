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
