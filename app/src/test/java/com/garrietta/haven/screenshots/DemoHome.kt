package com.garrietta.haven.screenshots

import com.garrietta.haven.data.HaArea
import com.garrietta.haven.data.HaEntity

/**
 * A small, believable home for screenshot tests: four rooms and one of
 * every surface the Home screen draws (lights on and off, a heating
 * thermostat, a lock, a garage door, playing media, a docked vacuum,
 * temperature / humidity / power sensors, weather, presence). It goes in
 * through the same seam the cold-start disk cache uses
 * (HaClient.restoreCachedStates), so the screens render exactly as they
 * do on a phone with last-known data.
 */
object DemoHome {
    val areas = listOf(
        HaArea("living_room", "Living Room"),
        HaArea("kitchen", "Kitchen"),
        HaArea("bedroom", "Bedroom"),
        HaArea("garage", "Garage"),
    )

    private fun entity(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
        HaEntity(id, state, mapOf("friendly_name" to name) + attrs.toMap())

    private val list = listOf(
        entity("light.living_room_lamp", "on", "Floor Lamp", "brightness" to 204, "color_temp_kelvin" to 2700,
            "supported_color_modes" to listOf("color_temp")),
        entity("light.living_room_ceiling", "off", "Ceiling", "supported_color_modes" to listOf("brightness")),
        entity("media_player.living_room_tv", "playing", "Living Room TV", "media_title" to "Blue in Green",
            "media_artist" to "Miles Davis", "volume_level" to 0.4),
        entity("climate.thermostat", "heat", "Thermostat", "hvac_action" to "heating", "temperature" to 70,
            "current_temperature" to 67, "min_temp" to 45, "max_temp" to 95,
            "hvac_modes" to listOf("off", "heat", "cool", "heat_cool")),
        entity("sensor.living_room_temperature", "67.4", "Temperature", "device_class" to "temperature",
            "unit_of_measurement" to "°F"),
        entity("sensor.living_room_humidity", "41", "Humidity", "device_class" to "humidity",
            "unit_of_measurement" to "%"),

        entity("light.kitchen_pendants", "on", "Pendants", "brightness" to 255, "color_temp_kelvin" to 4000,
            "supported_color_modes" to listOf("color_temp")),
        entity("light.kitchen_under_cabinet", "on", "Under Cabinet", "brightness" to 128,
            "supported_color_modes" to listOf("brightness")),
        entity("switch.coffee_maker", "off", "Coffee Maker"),
        entity("sensor.dishwasher_power", "742", "Dishwasher", "device_class" to "power",
            "unit_of_measurement" to "W"),

        entity("light.bedroom_lamp", "off", "Bedside Lamp", "supported_color_modes" to listOf("brightness")),
        entity("fan.bedroom_fan", "on", "Ceiling Fan", "percentage" to 50),
        entity("cover.bedroom_blinds", "closed", "Blinds", "current_position" to 0, "device_class" to "blind"),

        entity("cover.garage_door", "closed", "Garage Door", "device_class" to "garage"),
        entity("lock.side_door", "locked", "Side Door"),
        entity("vacuum.robot", "docked", "Robot Vacuum", "battery_level" to 96),
        entity("binary_sensor.garage_motion", "off", "Garage Motion", "device_class" to "motion"),

        entity("weather.home", "partlycloudy", "Home", "temperature" to 58, "temperature_unit" to "°F",
            "humidity" to 62, "wind_speed" to 7.5),
        entity("person.alex", "home", "Alex"),
        entity("device_tracker.alex_phone", "home", "Alex's Phone"),
    )

    val states: Map<String, HaEntity> = list.associateBy { it.entityId }

    val entityAreas: Map<String, String> = mapOf(
        "light.living_room_lamp" to "living_room",
        "light.living_room_ceiling" to "living_room",
        "media_player.living_room_tv" to "living_room",
        "climate.thermostat" to "living_room",
        "sensor.living_room_temperature" to "living_room",
        "sensor.living_room_humidity" to "living_room",
        "light.kitchen_pendants" to "kitchen",
        "light.kitchen_under_cabinet" to "kitchen",
        "switch.coffee_maker" to "kitchen",
        "sensor.dishwasher_power" to "kitchen",
        "light.bedroom_lamp" to "bedroom",
        "fan.bedroom_fan" to "bedroom",
        "cover.bedroom_blinds" to "bedroom",
        "cover.garage_door" to "garage",
        "lock.side_door" to "garage",
        "vacuum.robot" to "garage",
        "binary_sensor.garage_motion" to "garage",
    )

    /**
     * What the Security tab needs beside the rooms: cameras, the switches
     * that turn their detection on and off, a motion sensor, an alarm and
     * a siren. Kept apart so the other screenshots are unchanged.
     */
    val security: Map<String, HaEntity> = listOf(
        entity("camera.front_door_live_view", "idle", "Front Door"),
        entity("camera.tapo_2_live_view", "idle", "Garage"),
        entity("camera.backyard", "idle", "Backyard"),
        entity("switch.front_door_motion_detection", "on", "Front Door Motion detection"),
        entity("switch.tapo_2_motion_detection", "on", "Garage Motion detection"),
        entity("switch.tapo_2_person_detection", "off", "Garage Person detection"),
        entity("binary_sensor.front_door_motion", "off", "Front Door Motion", "device_class" to "motion"),
        entity("alarm_control_panel.home", "armed_home", "Home Alarm"),
        entity("siren.outdoor_camera_siren", "off", "Outdoor siren"),
    ).associateBy { it.entityId }

    val securityDevices: Map<String, String> = mapOf(
        "camera.front_door_live_view" to "ring",
        "switch.front_door_motion_detection" to "ring",
        "binary_sensor.front_door_motion" to "ring",
        "camera.tapo_2_live_view" to "tapo",
        "switch.tapo_2_motion_detection" to "tapo",
        "switch.tapo_2_person_detection" to "tapo",
    )
}
