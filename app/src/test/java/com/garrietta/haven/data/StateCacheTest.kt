package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The cold-start cache keeps the device registry Home sorts by. */
class StateCacheTest {
    @Test
    fun theRegistryPartRoundTrips() {
        val states = mapOf("light.lamp" to HaEntity("light.lamp", "on", mapOf("friendly_name" to "Lamp")))
        val text = serializeStateCache(
            states, emptyList(), emptyMap(),
            entityDevices = mapOf("light.lamp" to "d1", "switch.theme" to "d2"),
            deviceInfo = mapOf("d1" to HaDeviceInfo("Lamp", "Plug", "Tuya"), "d2" to HaDeviceInfo("Theme", null, null, service = true)),
            auxiliary = setOf("switch.lamp_led"),
        )
        val back = parseStateCache(text)!!
        assertEquals(mapOf("light.lamp" to "d1", "switch.theme" to "d2"), back.entityDevices)
        assertEquals("Lamp", back.deviceInfo.getValue("d1").name)
        assertEquals("Plug", back.deviceInfo.getValue("d1").model)
        assertTrue(back.deviceInfo.getValue("d2").service)
        assertEquals(setOf("switch.lamp_led"), back.auxiliary)
    }

    @Test
    fun anOlderCacheWithoutTheRegistryStillLoads() {
        val old = """{"states":[{"entity_id":"light.lamp","state":"on","attributes":{}}],"areas":[],"entityAreas":{}}"""
        val back = parseStateCache(old)!!
        assertEquals(1, back.states.size)
        assertTrue(back.entityDevices.isEmpty() && back.deviceInfo.isEmpty() && back.auxiliary.isEmpty())
    }
}
