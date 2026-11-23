package com.garrietta.haven.data

import com.garrietta.haven.ui.theme.StateFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Home's organization, on a home shaped like a real one: areas only half
 * filled in, a plug that reaches Home Assistant as a light and a switch,
 * a TV added by two integrations, a vacuum twice, cameras with their
 * settings switches, and a greenhouse.
 */
class HomeLayoutTest {

    private fun e(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
        HaEntity(id, state, mapOf("friendly_name" to name) + attrs.toMap())

    private val areas = listOf(
        HaArea("living_room", "Living Room"),
        HaArea("bedroom", "Bedroom"),
        HaArea("greenhouse", "Greenhouse"),
    )

    private val states = listOf(
        e("light.lamp_socket_1", "on", "Lamp", "brightness" to 255),
        e("switch.smart_plug_3_socket_1", "on", "Lamp Socket 1"),
        e("light.standing_lamp", "off", "Standing Lamp"),
        e("light.garage_opener_dnd", "on", "Smart Garage Door Opener Dnd"),
        e("cover.garage_door", "closed", "Garage Door", "device_class" to "garage"),
        e("switch.garage_opener_buzzer", "off", "Smart Garage Door Opener buzzerEnable"),
        e("camera.garage_live_view", "idle", "Garage Camera"),
        e("switch.garage_camera", "on", "Garage Camera"),
        e("switch.garage_camera_motion_detection", "on", "Garage Camera Motion detection"),
        e("climate.thermostat", "heat_cool", "Thermostat", "current_temperature" to 68.0,
            "target_temp_low" to 66.0, "target_temp_high" to 72.0, "hvac_action" to "idle"),
        e("sensor.thermostat_current_temperature", "68.0", "Thermostat Current Temperature", "device_class" to "temperature"),
        e("vacuum.robot", "docked", "Robot Vacuum", "battery_level" to 100),
        e("vacuum.robot_homekit", "docked", "Robot Vacuum Homekit"),
        e("switch.robot_child_lock", "off", "Robot Vacuum Child Lock"),
        e("media_player.family_room_tv", "unavailable", "Vizio TV"),
        e("media_player.tv", "unavailable", "TV"),
        e("media_player.bedroom_tv", "unavailable", "Bedroom TV"),
        e("switch.greenhouse_heater", "off", "Greenhouse Heater"),
        e("switch.greenhouse_fan", "on", "Greenhouse Fan"),
        e("switch.outlet_led", "on", "Dual outlet LED"),
        e("switch.sprinklers", "off", "Sprinklers"),
        e("switch.sprinkler_togglex", "unavailable", "Sprinkler Timer Togglex"),
        e("light.outdoor_floodlight", "off", "Outdoor camera Floodlight"),
        e("camera.outdoor_hd", "idle", "Outdoor camera HD Stream"),
        e("alarm_control_panel.home_mode", "disarmed", "Home Mode"),
        e("device_tracker.alex_phone", "home", "Alex's Phone"),
        e("device_tracker.sam_iphone", "home", "Sam’s iPhone"),
        e("device_tracker.alex_tablet", "home", "Alex's Tablet"),
        e("device_tracker.ipad_1234", "home", "iPad (1234)"),
        e("weather.forecast", "cloudy", "Forecast", "temperature" to 45.0),
    ).associateBy { it.entityId }

    private val entityAreas = mapOf(
        "light.lamp_socket_1" to "living_room",
        "light.standing_lamp" to "living_room",
        "vacuum.robot" to "living_room",
        "media_player.family_room_tv" to "living_room",
        "media_player.bedroom_tv" to "bedroom",
        "switch.greenhouse_heater" to "greenhouse",
        "switch.greenhouse_fan" to "greenhouse",
        "switch.sprinklers" to "greenhouse",
    )

    private val entityDevices = mapOf(
        "light.lamp_socket_1" to "d_lamp", "switch.smart_plug_3_socket_1" to "d_lamp",
        "light.standing_lamp" to "d_stand",
        "light.garage_opener_dnd" to "d_opener", "cover.garage_door" to "d_opener", "switch.garage_opener_buzzer" to "d_opener",
        "camera.garage_live_view" to "d_gcam", "switch.garage_camera" to "d_gcam", "switch.garage_camera_motion_detection" to "d_gcam",
        "climate.thermostat" to "d_thermo", "sensor.thermostat_current_temperature" to "d_thermo",
        "vacuum.robot" to "d_vac", "vacuum.robot_homekit" to "d_vac", "switch.robot_child_lock" to "d_vac",
        "media_player.family_room_tv" to "d_tv1", "media_player.tv" to "d_tv2",
        "media_player.bedroom_tv" to "d_tv3",
        "switch.greenhouse_heater" to "d_outlet", "switch.greenhouse_fan" to "d_outlet", "switch.outlet_led" to "d_outlet",
        "switch.sprinklers" to "d_spr", "switch.sprinkler_togglex" to "d_spr",
        "light.outdoor_floodlight" to "d_out", "camera.outdoor_hd" to "d_out",
    )

    private val devices = mapOf(
        "d_lamp" to DeviceFacts("Lamp", "Smart plug"),
        "d_stand" to DeviceFacts("Standing lamp", "Smart plug"),
        "d_opener" to DeviceFacts("Smart Garage Door Opener", "msg100"),
        "d_gcam" to DeviceFacts("Garage Camera", "C110"),
        "d_thermo" to DeviceFacts("Thermostat", "ECB501"),
        "d_vac" to DeviceFacts("Robot Vacuum", "T2292"),
        "d_tv1" to DeviceFacts("TV", "Vizio V655M-K03"),
        "d_tv2" to DeviceFacts("TV", "VIZIO V655M-K03"),
        "d_tv3" to DeviceFacts("Bedroom TV", "Chromecast"),
        "d_outlet" to DeviceFacts("Dual outlet", "P400M"),
        "d_spr" to DeviceFacts("Smart Sprinkler Timer", "mst100"),
        "d_out" to DeviceFacts("Outdoor camera", "C402"),
    )

    private val places = homePlaces(states, areas, entityAreas, entityDevices, devices, auxiliary = setOf("switch.outlet_led"))
    private fun place(name: String) = places.first { it.name == name }

    @Test
    fun theHouseFallsIntoPlacesInOrder() {
        assertEquals(
            listOf(WHOLE_HOME, "Living Room", "Bedroom", "Garage", "Outside", "Greenhouse"),
            places.map { it.name },
        )
    }

    @Test
    fun aPlaceWithNoAreaIsReadOffTheDeviceName() {
        assertEquals(listOf("cover.garage_door"), place("Garage").entityIds)
        assertEquals("place:garage", place("Garage").id)
        assertEquals(null, place("Garage").areaId)
        assertEquals(listOf("light.outdoor_floodlight"), place("Outside").entityIds)
        // A name matching an area joins the area.
        assertEquals("greenhouse", place("Greenhouse").areaId)
    }

    @Test
    fun aPlugShownAsALightIsOneTile() {
        val living = place("Living Room").entityIds
        assertTrue("light.lamp_socket_1" in living)
        assertFalse(places.any { "switch.smart_plug_3_socket_1" in it.entityIds })
    }

    @Test
    fun aDeviceInTheRegistryTwiceIsOneTile() {
        val all = places.flatMap { it.entityIds }
        assertTrue("media_player.family_room_tv" in all)
        assertFalse("media_player.tv" in all)
        assertTrue("vacuum.robot" in all)
        assertFalse("vacuum.robot_homekit" in all)
    }

    @Test
    fun settingsIndicatorsAndDeadSwitchesStayOff() {
        val all = places.flatMap { it.entityIds }.toSet()
        for (id in listOf(
            "light.garage_opener_dnd", "switch.garage_opener_buzzer", "switch.garage_camera",
            "switch.garage_camera_motion_detection", "switch.robot_child_lock", "switch.outlet_led",
            "switch.sprinkler_togglex", "camera.garage_live_view", "alarm_control_panel.home_mode",
        )) assertFalse(id, id in all)
    }

    @Test
    fun withinAPlaceLightsComeBeforeTheVacuumAndTheTv() {
        assertEquals(
            listOf("light.lamp_socket_1", "light.standing_lamp", "vacuum.robot", "media_player.family_room_tv"),
            place("Living Room").entityIds,
        )
    }

    @Test
    fun theWholeHomeHasTheThermostatAndItsReading() {
        assertEquals(listOf("climate.thermostat"), place(WHOLE_HOME).entityIds)
        assertEquals(listOf("sensor.thermostat_current_temperature"), place(WHOLE_HOME).readingIds)
    }

    @Test
    fun placesAreNamedByAreasFirstThenCommonWords() {
        val names = listOf("Living Room", "Greenhouse")
        assertEquals("Living Room", placeNamed("Living Room Camera", names))
        assertEquals("Greenhouse", placeNamed("Greenhouse Heater", names))
        assertEquals("Garage", placeNamed("Garage Door", names))
        assertEquals("Front Door", placeNamed("Ring Doorbell", names))
        assertEquals("Outside", placeNamed("Outdoor camera Floodlight", names))
        assertEquals("Living Room", placeNamed("Family Room TV", names))
        assertEquals(null, placeNamed("Thermostat", names))
        // Whole words only: "yard" is not found in "Lanyard".
        assertEquals(null, placeNamed("Lanyard charger", names))
    }

    @Test
    fun theStatusDeckSaysHowEachThingIs() {
        val deck = homeStatus(states, places)
        assertEquals(
            listOf(StatusKind.Climate, StatusKind.Security, StatusKind.Lights, StatusKind.Garage, StatusKind.Greenhouse, StatusKind.Vacuum, StatusKind.Media),
            deck.map { it.kind },
        )
        val byKind = deck.associateBy { it.kind }
        assertEquals("68° inside", byKind.getValue(StatusKind.Climate).value)
        assertEquals("Disarmed", byKind.getValue(StatusKind.Security).value)
        assertEquals("1 on", byKind.getValue(StatusKind.Lights).value)
        assertEquals(StateFamily.Light, byKind.getValue(StatusKind.Lights).family)
        assertEquals("Closed", byKind.getValue(StatusKind.Garage).value)
        assertEquals("Fan on", byKind.getValue(StatusKind.Greenhouse).value)
        assertEquals("Docked · 100%", byKind.getValue(StatusKind.Vacuum).value)
        assertEquals("All off", byKind.getValue(StatusKind.Media).value)
    }

    @Test
    fun anOpenGarageTurnsSecurityAndTheGarageToAlert() {
        val open = states + ("cover.garage_door" to e("cover.garage_door", "open", "Garage Door", "device_class" to "garage"))
        val deck = homeStatus(open, places).associateBy { it.kind }
        assertEquals(StateFamily.Alert, deck.getValue(StatusKind.Security).family)
        assertEquals("Garage Door open", deck.getValue(StatusKind.Security).value)
        assertEquals(StateFamily.Alert, deck.getValue(StatusKind.Garage).family)
    }

    @Test
    fun tilesSayWhatTheDeviceIsDoing() {
        assertEquals("100%", tileState(states.getValue("light.lamp_socket_1")))
        assertEquals("Off", tileState(states.getValue("media_player.bedroom_tv")))
        assertEquals("Holding 66–72°", tileState(states.getValue("climate.thermostat")))
        assertEquals("Closed", tileState(states.getValue("cover.garage_door")))
        assertEquals("Heating to 70°", climateLine(e("climate.x", "heat", "T", "hvac_action" to "heating", "temperature" to 70.0)))
    }

    @Test
    fun aPlaceHeadingCarriesItsReadingsAndWhatIsOn() {
        // The thermostat's tile shows the temperature; its heading need not.
        assertEquals("", placeLine(place(WHOLE_HOME), states))
        assertEquals("68°", placeLine(place(WHOLE_HOME).copy(entityIds = emptyList()), states))
        assertEquals("Lamp is on", placeLine(place("Living Room"), states))
        assertEquals("Greenhouse Fan is on", placeLine(place("Greenhouse"), states))
    }

    @Test
    fun peopleAreTheNamesOnTheirDevices() {
        assertEquals(listOf("Alex", "Sam"), peopleHome(states.values.toList()))
    }

    @Test
    fun aDeviceNameLosesItsPlace() {
        assertEquals("Heater", shortName("Greenhouse Heater", "Greenhouse"))
        assertEquals("Lamp", shortName("Lamp", "Living Room"))
        assertEquals("Greenhouse", shortName("Greenhouse", "Greenhouse"))
    }
}
