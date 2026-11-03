package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entity(id: String, state: String, attrs: Map<String, Any?> = emptyMap()) =
    HaEntity(id, state, attrs + mapOf("friendly_name" to id))

class ControlsMappingTest {

    @Test
    fun lightWithBrightnessIsToggleRange() {
        val spec = mapEntityToControl(entity("light.lamp", "on", mapOf("brightness" to 127)))!!
        assertEquals(ControlDeviceType.LIGHT, spec.deviceType)
        assertEquals(ControlKind.TOGGLE_RANGE, spec.kind)
        assertEquals(true, spec.booleanValue)
        assertEquals(127 * 100f / 255f, spec.rangeValue!!, 0.01f)
    }

    @Test
    fun outletSwitchMapsToOutletType() {
        val spec = mapEntityToControl(entity("switch.heater", "off", mapOf("device_class" to "outlet")))!!
        assertEquals(ControlDeviceType.OUTLET, spec.deviceType)
        assertEquals(ControlKind.TOGGLE, spec.kind)
    }

    @Test
    fun garageCoverWithPositionIsRange() {
        val spec = mapEntityToControl(
            entity("cover.garage", "open", mapOf("device_class" to "garage", "current_position" to 100)),
        )!!
        assertEquals(ControlDeviceType.GARAGE, spec.deviceType)
        assertEquals(ControlKind.RANGE, spec.kind)
        assertEquals(100f, spec.rangeValue!!, 0.001f)
    }

    @Test
    fun excludedDomainsMapToNull() {
        assertNull(mapEntityToControl(entity("lock.front", "locked")))
        assertNull(mapEntityToControl(entity("media_player.tv", "playing")))
        assertNull(mapEntityToControl(entity("vacuum.t2292", "docked")))
        assertNull(mapEntityToControl(entity("camera.front", "idle")))
        assertNull(mapEntityToControl(entity("sensor.temp", "72")))
        assertNull(mapEntityToControl(entity("binary_sensor.motion", "off")))
    }

    @Test
    fun climateBandShiftMovesWholeBandAndClamps() {
        // Band 68-72, new midpoint 70 -> delta 0? old midpoint is 70, so use 72 -> +2.
        val shifted = shiftClimateBand(72f, 68f, 72f, 50f, 90f)!!
        assertEquals(70f, shifted.first, 0.001f)
        assertEquals(74f, shifted.second, 0.001f)
        // Clamped by max: band 88-90, new midpoint 95 -> delta capped at 0 (max-high).
        val clamped = shiftClimateBand(90f, 88f, 95f, 50f, 90f)!!
        assertEquals(88f, clamped.first, 0.001f)
        assertEquals(90f, clamped.second, 0.001f)
        assertNull(shiftClimateBand(null, 68f, 72f, 50f, 90f))
    }

    @Test
    fun climateBooleanModePrefersHeat() {
        assertEquals("heat", climateBooleanMode(listOf("off", "cool", "heat"), true))
        assertEquals("off", climateBooleanMode(listOf("off", "heat"), false))
        assertNull(climateBooleanMode(listOf("heat"), false))
    }

    @Test
    fun defaultControlsAreFavoritesFirstThenNameSorted() {
        val entities = listOf(
            entity("light.zebra", "on"),
            entity("light.apple", "on"),
            entity("switch.mango", "on"),
        )
        val specs = defaultControls(entities, listOf("switch.mango", "light.zebra"))
        assertEquals(listOf("switch.mango", "light.zebra", "light.apple"), specs.map { it.entityId })
        assertTrue(specs.all { it.kind != ControlKind.TEMPERATURE || it.deviceType == ControlDeviceType.THERMOSTAT })
    }
}
