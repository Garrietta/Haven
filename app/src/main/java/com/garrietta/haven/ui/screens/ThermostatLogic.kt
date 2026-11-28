package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.HaEntity

/*
 * Pure helpers for the thermostat's popup, kept free of Compose so they
 * are unit-tested. They read only what Home Assistant reports.
 */

/**
 * The sensors that belong to a thermostat, for its history: the ids
 * Home Assistant gives an Ecobee (sensor.<object>_current_temperature
 * and _current_humidity) first, else numeric sensors sharing the
 * thermostat's object id as a prefix. Empty when there are none; no
 * link is guessed beyond that.
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

/** A mode as the Mode row labels it: "heat_cool" reads "Heat • Cool", others are capitalized. */
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
