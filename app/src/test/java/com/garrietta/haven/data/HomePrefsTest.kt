package com.garrietta.haven.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePrefsTest {

    private fun light(entityId: String, friendlyName: String, state: String = "on") =
        HaEntity(entityId, state, mapOf("friendly_name" to friendlyName))

    @Test
    fun dndInEntityIdIsIndicator() {
        assertTrue(isIndicatorLight(light("light.smart_garage_door_24090408838301610702c4e7ae0ae280_dnd", "Garage Door DND")))
        assertTrue(isIndicatorLight(light("light.something_DND", "Something")))
    }

    @Test
    fun friendlyNameEndingInDndIsIndicator() {
        assertTrue(isIndicatorLight(light("light.hub_indicator", "Smart Hub Dnd")))
        assertTrue(isIndicatorLight(light("light.hub_indicator", "Smart Hub DND")))
    }

    @Test
    fun ordinaryLightsAreNotIndicators() {
        assertFalse(isIndicatorLight(light("light.lamp_socket_1", "Lamp")))
        assertFalse(isIndicatorLight(light("light.standing_lamp_socket_1", "Standing Lamp")))
        // "dnd" elsewhere in the name is not an ending match, and the id is clean.
        assertFalse(isIndicatorLight(light("light.bedroom", "Dnd Lamp")))
    }

    @Test
    fun defaultRuleHidesIndicatorsAndShowsRoomLights() {
        val overrides = emptyMap<String, Boolean>()
        assertFalse(isLightShownOnHome(light("light.smart_hub_25111733115125620801c4e7ae24d9ea_dnd", "Smart Hub DND"), overrides))
        assertTrue(isLightShownOnHome(light("light.lamp_socket_1", "Lamp"), overrides))
    }

    @Test
    fun explicitOnOverridesDefaultExclusion() {
        val dnd = light("light.smart_garage_door_24090408838301610702c4e7ae0ae280_dnd", "Garage Door DND")
        assertTrue(isLightShownOnHome(dnd, mapOf(dnd.entityId to true)))
    }

    @Test
    fun explicitOffHidesARoomLight() {
        val lamp = light("light.lamp_socket_1", "Lamp")
        assertFalse(isLightShownOnHome(lamp, mapOf(lamp.entityId to false)))
    }
}
