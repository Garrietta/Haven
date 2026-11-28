package com.garrietta.haven.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.garrietta.haven.core.model.HaEntity

/** A Material icon for an entity, from domain + device_class + state (HA's mdi icons aren't bundled). */
fun entityIcon(e: HaEntity): ImageVector {
    val on = e.state == "on"
    return when (e.domain) {
        "light" -> if (on) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb
        "switch", "input_boolean" -> when (e.deviceClass) {
            "outlet" -> Icons.Filled.Power
            else -> if (on) Icons.Filled.ToggleOn else Icons.Filled.ToggleOff
        }
        "fan" -> Icons.Filled.Air
        "cover" -> when (e.deviceClass) {
            "garage" -> Icons.Filled.Garage
            "door", "gate" -> Icons.Filled.DoorFront
            "blind", "shade", "curtain", "shutter" -> Icons.Filled.Blinds
            else -> if (e.state == "open") Icons.Filled.SensorDoor else Icons.Outlined.SensorDoor
        }
        "climate" -> Icons.Filled.Thermostat
        "water_heater" -> Icons.Filled.WaterDrop
        "humidifier" -> Icons.Filled.WaterDrop
        "lock" -> if (e.state == "locked") Icons.Filled.Lock else Icons.Filled.LockOpen
        "media_player" -> when (e.deviceClass) {
            "speaker" -> Icons.Filled.Speaker
            "receiver" -> Icons.Filled.SurroundSound
            else -> Icons.Filled.Tv
        }
        "vacuum" -> Icons.Filled.CleaningServices
        "lawn_mower" -> Icons.Filled.Grass
        "camera" -> Icons.Filled.Videocam
        "scene" -> Icons.Filled.Palette
        "script" -> Icons.Filled.PlayArrow
        "button", "input_button" -> Icons.Filled.TouchApp
        "automation" -> Icons.Filled.AutoMode
        "alarm_control_panel" -> if (e.state == "disarmed") Icons.Outlined.Shield else Icons.Filled.Shield
        "person" -> Icons.Filled.Person
        "weather" -> weatherIcon(e.state)
        "siren" -> Icons.Filled.Campaign
        "valve" -> Icons.Filled.WaterDrop
        "timer" -> Icons.Filled.Timer
        "todo" -> Icons.Filled.Checklist
        "input_number", "number" -> Icons.Filled.Tune
        "input_select", "select" -> Icons.Filled.List
        "binary_sensor" -> when (e.deviceClass) {
            "door", "garage_door", "opening" -> if (on) Icons.Filled.MeetingRoom else Icons.Filled.DoorFront
            "window" -> Icons.Filled.Window
            "motion", "occupancy", "presence" -> if (on) Icons.Filled.DirectionsWalk else Icons.Outlined.DirectionsWalk
            "moisture" -> Icons.Filled.WaterDrop
            "smoke", "gas", "carbon_monoxide" -> Icons.Filled.Warning
            "battery" -> Icons.Filled.BatteryAlert
            "connectivity" -> Icons.Filled.Wifi
            "problem" -> Icons.Filled.ReportProblem
            "lock" -> if (on) Icons.Filled.LockOpen else Icons.Filled.Lock
            else -> Icons.Filled.Sensors
        }
        "sensor" -> when (e.deviceClass) {
            "temperature" -> Icons.Filled.Thermostat
            "humidity", "moisture" -> Icons.Filled.WaterDrop
            "battery" -> Icons.Filled.BatteryStd
            "power", "energy", "current", "voltage" -> Icons.Filled.Bolt
            "illuminance" -> Icons.Filled.WbSunny
            "pressure" -> Icons.Filled.Speed
            "signal_strength" -> Icons.Filled.SignalCellularAlt
            "timestamp" -> Icons.Filled.Schedule
            "carbon_dioxide", "pm25", "pm10", "aqi", "volatile_organic_compounds" -> Icons.Filled.Air
            else -> Icons.Filled.Sensors
        }
        else -> Icons.Filled.DevicesOther
    }
}

fun weatherIcon(state: String): ImageVector = when (state) {
    "sunny" -> Icons.Filled.WbSunny
    "clear-night" -> Icons.Filled.NightsStay
    "partlycloudy" -> Icons.Filled.WbCloudy
    "cloudy", "fog" -> Icons.Filled.Cloud
    "rainy", "pouring" -> Icons.Filled.Umbrella
    "lightning", "lightning-rainy" -> Icons.Filled.Thunderstorm
    "snowy", "snowy-rainy", "hail" -> Icons.Filled.AcUnit
    "windy", "windy-variant" -> Icons.Filled.Air
    else -> Icons.Filled.Cloud
}
