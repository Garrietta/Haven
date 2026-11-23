package com.garrietta.haven.screenshots

import com.garrietta.haven.data.HaArea
import com.garrietta.haven.data.HaDeviceInfo
import com.garrietta.haven.data.HaEntity

/**
 * A home the way Home Assistant really sends one, for Home's screenshots:
 * areas only half filled in, a plug that reaches it as a light and a
 * switch, a TV added by two integrations, a vacuum twice, cameras with
 * their settings switches, a garage door, a greenhouse with its heater,
 * fan and sprinklers, an alarm, a thermostat holding a range, and two
 * people home. The names are made up.
 */
object LivedInHome {
    val areas = listOf(
        HaArea("living_room", "Living Room"),
        HaArea("bedroom", "Bedroom"),
        HaArea("greenhouse", "Greenhouse"),
    )

    private fun e(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
        HaEntity(id, state, mapOf("friendly_name" to name) + attrs.toMap())

    private val list = listOf(
        e("light.lamp_socket_1", "on", "Lamp", "brightness" to 204, "supported_color_modes" to listOf("onoff")),
        e("switch.smart_plug_socket_1", "on", "Lamp Socket 1"),
        e("light.standing_lamp_socket_1", "off", "Standing Lamp", "supported_color_modes" to listOf("onoff")),
        e("light.garage_opener_dnd", "on", "Smart Garage Door Opener Dnd"),
        e("cover.garage_door", "closed", "Garage Door", "device_class" to "garage"),
        e("camera.front_door_live_view", "idle", "Front Door"),
        e("camera.outdoor_camera_hd", "idle", "Outdoor camera"),
        e("camera.living_room_camera", "idle", "Living Room Camera"),
        e("camera.garage_camera", "idle", "Garage Camera"),
        e("switch.garage_camera", "on", "Garage Camera"),
        e("switch.garage_camera_motion_detection", "on", "Garage Camera Motion detection"),
        e("climate.thermostat", "heat_cool", "Thermostat", "current_temperature" to 68.0, "current_humidity" to 53.0,
            "target_temp_low" to 66.0, "target_temp_high" to 72.0, "hvac_action" to "idle",
            "hvac_modes" to listOf("off", "heat", "cool", "heat_cool"), "min_temp" to 45, "max_temp" to 92),
        e("sensor.thermostat_current_temperature", "68.0", "Thermostat Current Temperature",
            "device_class" to "temperature", "unit_of_measurement" to "°F"),
        e("vacuum.robot", "docked", "Robot Vacuum", "battery_level" to 100),
        e("vacuum.robot_homekit", "docked", "Robot Vacuum Homekit"),
        e("switch.robot_child_lock", "off", "Robot Vacuum Child Lock"),
        e("media_player.family_room_tv", "unavailable", "TV"),
        e("media_player.tv", "unavailable", "TV"),
        e("media_player.bedroom_tv", "playing", "Bedroom TV", "media_title" to "Nature Sounds"),
        e("switch.greenhouse_heater", "off", "Greenhouse Heater"),
        e("switch.greenhouse_fan", "on", "Greenhouse Fan"),
        e("switch.outlet_led", "on", "Dual outlet LED"),
        e("switch.sprinklers", "off", "Sprinklers"),
        e("light.outdoor_floodlight", "off", "Outdoor camera Floodlight", "supported_color_modes" to listOf("onoff")),
        e("alarm_control_panel.home_mode", "disarmed", "Home Mode"),
        e("device_tracker.alex_phone", "home", "Alex's Phone"),
        e("device_tracker.sam_phone", "home", "Sam’s Phone"),
        e("weather.forecast", "cloudy", "Forecast", "temperature" to 45.0, "temperature_unit" to "°F"),
    )

    val states: Map<String, HaEntity> = list.associateBy { it.entityId }

    val entityAreas = mapOf(
        "light.lamp_socket_1" to "living_room",
        "light.standing_lamp_socket_1" to "living_room",
        "vacuum.robot" to "living_room",
        "media_player.family_room_tv" to "living_room",
        "media_player.bedroom_tv" to "bedroom",
        "switch.greenhouse_heater" to "greenhouse",
        "switch.greenhouse_fan" to "greenhouse",
        "switch.sprinklers" to "greenhouse",
    )

    val entityDevices = mapOf(
        "light.lamp_socket_1" to "d_lamp", "switch.smart_plug_socket_1" to "d_lamp",
        "light.standing_lamp_socket_1" to "d_stand",
        "light.garage_opener_dnd" to "d_opener", "cover.garage_door" to "d_opener",
        "camera.garage_camera" to "d_gcam", "switch.garage_camera" to "d_gcam", "switch.garage_camera_motion_detection" to "d_gcam",
        "camera.living_room_camera" to "d_lcam",
        "camera.outdoor_camera_hd" to "d_out", "light.outdoor_floodlight" to "d_out",
        "camera.front_door_live_view" to "d_ring",
        "climate.thermostat" to "d_thermo", "sensor.thermostat_current_temperature" to "d_thermo",
        "vacuum.robot" to "d_vac", "vacuum.robot_homekit" to "d_vac", "switch.robot_child_lock" to "d_vac",
        "media_player.family_room_tv" to "d_tv1", "media_player.tv" to "d_tv2",
        "media_player.bedroom_tv" to "d_tv3",
        "switch.greenhouse_heater" to "d_outlet", "switch.greenhouse_fan" to "d_outlet", "switch.outlet_led" to "d_outlet",
        "switch.sprinklers" to "d_spr",
    )

    val deviceInfo = mapOf(
        "d_lamp" to HaDeviceInfo("Lamp", "Smart plug", "Tuya"),
        "d_stand" to HaDeviceInfo("Standing lamp", "Smart plug", "Tuya"),
        "d_opener" to HaDeviceInfo("Smart Garage Door Opener", "msg100", "Meross"),
        "d_gcam" to HaDeviceInfo("Garage Camera", "C110", "TP-Link"),
        "d_lcam" to HaDeviceInfo("Living Room Camera", "C110", "TP-Link"),
        "d_out" to HaDeviceInfo("Outdoor camera", "C402", "TP-Link"),
        "d_ring" to HaDeviceInfo("Front Door", "Doorbell", "Ring"),
        "d_thermo" to HaDeviceInfo("Thermostat", "ECB501", "ecobee"),
        "d_vac" to HaDeviceInfo("Robot Vacuum", "T2292", "Eufy"),
        "d_tv1" to HaDeviceInfo("TV", "Vizio V655M-K03", "Vizio"),
        "d_tv2" to HaDeviceInfo("TV", "VIZIO V655M-K03", "VIZIO"),
        "d_tv3" to HaDeviceInfo("Bedroom TV", "Chromecast", "Google"),
        "d_outlet" to HaDeviceInfo("Dual outlet", "P400M", "TP-Link"),
        "d_spr" to HaDeviceInfo("Smart Sprinkler Timer", "mst100", "Meross"),
    )

    val auxiliary = setOf("switch.outlet_led")
}
