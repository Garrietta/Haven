package com.garrietta.haven.ui

import com.garrietta.haven.core.model.HaEntity
import kotlin.math.roundToInt

/** Human state text for any entity. Pure, unit tested. */
fun stateText(e: HaEntity, tempUnit: String = "°"): String {
    if (e.state == "unavailable") return "Unavailable"
    if (e.state == "unknown") return "Unknown"
    return when (e.domain) {
        "light" -> if (e.state == "on") e.attrInt("brightness")?.let { "${(it * 100 + 127) / 255}%" } ?: "On" else "Off"
        "climate" -> {
            val action = e.attrString("hvac_action")?.let { pretty(it) }
            val target = e.attrDouble("temperature")?.let { "${fmtNum(it)}°" }
                ?: run {
                    val lo = e.attrDouble("target_temp_low"); val hi = e.attrDouble("target_temp_high")
                    if (lo != null && hi != null) "${fmtNum(lo)}–${fmtNum(hi)}°" else null
                }
            when {
                e.state == "off" -> "Off"
                action != null && action != "Idle" && action != "Off" -> listOfNotNull(action, target?.let { "to $it" }).joinToString(" ")
                else -> listOfNotNull(pretty(e.state), target).joinToString(" · ")
            }
        }
        "cover" -> e.attrInt("current_position")?.takeIf { e.state == "open" && it in 1..99 }?.let { "Open $it%" } ?: pretty(e.state)
        "fan" -> if (e.state == "on") e.attrInt("percentage")?.let { "$it%" } ?: "On" else "Off"
        "media_player" -> when (e.state) {
            "playing", "paused" -> e.attrString("media_title") ?: pretty(e.state)
            else -> pretty(e.state)
        }
        "sensor", "number", "input_number" -> {
            val n = e.state.toDoubleOrNull()
            val unit = e.unit
            if (n != null) fmtNum(n) + when {
                unit == null -> ""
                unit == "%" || unit.startsWith("°") -> unit
                else -> " $unit"
            } else pretty(e.state)
        }
        "binary_sensor" -> binaryText(e)
        "person", "device_tracker" -> when (e.state) { "home" -> "Home"; "not_home" -> "Away"; else -> pretty(e.state) }
        "lock" -> pretty(e.state)
        "vacuum" -> pretty(e.state)
        "alarm_control_panel" -> pretty(e.state.replace("armed_", "Armed "))
        "weather" -> e.attrDouble("temperature")?.let { "${fmtNum(it)}° · ${weatherText(e.state)}" } ?: weatherText(e.state)
        "scene", "script", "button", "input_button" -> if (e.domain == "script" && e.state == "on") "Running" else "Tap to run"
        "automation" -> if (e.state == "on") "Enabled" else "Disabled"
        "camera" -> pretty(e.state)
        "timer" -> pretty(e.state)
        else -> pretty(e.state)
    }
}

fun binaryText(e: HaEntity): String {
    val on = e.state == "on"
    return when (e.deviceClass) {
        "door", "garage_door", "window", "opening" -> if (on) "Open" else "Closed"
        "motion", "occupancy", "presence" -> if (on) "Detected" else "Clear"
        "moisture" -> if (on) "Wet" else "Dry"
        "lock" -> if (on) "Unlocked" else "Locked"
        "battery" -> if (on) "Low" else "Normal"
        "connectivity" -> if (on) "Connected" else "Disconnected"
        "plug", "power" -> if (on) "Powered" else "Off"
        "problem" -> if (on) "Problem" else "OK"
        "smoke", "gas", "carbon_monoxide", "safety" -> if (on) "Detected" else "Clear"
        else -> if (on) "On" else "Off"
    }
}

fun weatherText(state: String): String = when (state) {
    "clear-night" -> "Clear"
    "partlycloudy" -> "Partly cloudy"
    "lightning-rainy" -> "Storms"
    "snowy-rainy" -> "Sleet"
    "windy-variant" -> "Windy"
    "exceptional" -> "Unusual"
    else -> pretty(state)
}

fun pretty(s: String): String = s.replace('_', ' ').replaceFirstChar { it.uppercase() }

fun fmtNum(v: Double): String =
    if (kotlin.math.abs(v - v.roundToInt()) < 0.05 || kotlin.math.abs(v) >= 100) v.roundToInt().toString()
    else String.format(java.util.Locale.US, "%.1f", v)

/** Domains a person controls or reads — everything else (update, sun, zone, tts…) stays out of the UI. */
val USER_DOMAINS = setOf(
    "light", "switch", "fan", "cover", "climate", "lock", "media_player", "vacuum", "camera", "sensor",
    "binary_sensor", "scene", "script", "input_boolean", "input_number", "input_select", "input_button",
    "alarm_control_panel", "humidifier", "water_heater", "valve", "siren", "select", "number", "button",
    "automation", "person", "weather", "lawn_mower", "timer", "todo",
)

/** Controls first, then readouts — the order rooms and the device list group by. */
val DOMAIN_GROUPS: List<Pair<String, Set<String>>> = listOf(
    "Lights" to setOf("light"),
    "Climate" to setOf("climate", "fan", "humidifier", "water_heater"),
    "Cameras" to setOf("camera"),
    "Media" to setOf("media_player"),
    "Security" to setOf("lock", "alarm_control_panel", "cover", "valve", "siren"),
    "Switches" to setOf("switch", "input_boolean"),
    "Cleaning" to setOf("vacuum", "lawn_mower"),
    "Scenes & scripts" to setOf("scene", "script", "button", "input_button"),
    "Settings" to setOf("input_number", "input_select", "number", "select", "timer"),
    "Sensors" to setOf("sensor", "binary_sensor", "weather"),
    "Automations" to setOf("automation"),
    "People" to setOf("person"),
)

fun groupOf(domain: String): String = DOMAIN_GROUPS.firstOrNull { domain in it.second }?.first ?: "Other"

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
