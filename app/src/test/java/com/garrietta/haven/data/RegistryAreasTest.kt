package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistryAreasTest {

    @Test
    fun entityAreaWinsOverDeviceArea() {
        val resolved = resolveEntityAreas(
            entityAreas = mapOf("light.lamp" to "bedroom"),
            entityDevices = mapOf("light.lamp" to "dev1"),
            deviceAreas = mapOf("dev1" to "living_room"),
        )
        assertEquals("bedroom", resolved["light.lamp"])
    }

    @Test
    fun deviceAreaIsFallbackWhenEntityAreaAbsent() {
        val resolved = resolveEntityAreas(
            entityAreas = mapOf("light.lamp" to null),
            entityDevices = mapOf("light.lamp" to "dev1"),
            deviceAreas = mapOf("dev1" to "living_room"),
        )
        assertEquals("living_room", resolved["light.lamp"])
    }

    @Test
    fun entityWithOnlyDeviceLinkStillResolves() {
        val resolved = resolveEntityAreas(
            entityAreas = emptyMap(),
            entityDevices = mapOf("sensor.temp" to "dev2"),
            deviceAreas = mapOf("dev2" to "kitchen"),
        )
        assertEquals("kitchen", resolved["sensor.temp"])
    }

    @Test
    fun unmappedEntityStaysAbsentNeverBlank() {
        val resolved = resolveEntityAreas(
            entityAreas = mapOf("light.orphan" to null),
            entityDevices = mapOf("light.orphan" to "dev9"),
            deviceAreas = emptyMap(),
        )
        assertFalse(resolved.containsKey("light.orphan"))
    }

    @Test
    fun blankAreasAreIgnored() {
        val resolved = resolveEntityAreas(
            entityAreas = mapOf("light.lamp" to ""),
            entityDevices = mapOf("light.lamp" to "dev1"),
            deviceAreas = mapOf("dev1" to ""),
        )
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun homeRoomFilterKeepsRealDevicesOnly() {
        for (domain in listOf("light", "switch", "sensor", "climate", "cover", "fan", "lock", "media_player", "input_boolean", "binary_sensor")) {
            assertTrue(domain, isHomeRoomEntity(HaEntity("$domain.x", "on", emptyMap())))
        }
        for (domain in listOf("camera", "weather", "person", "device_tracker", "zone", "sun", "update")) {
            assertFalse(domain, isHomeRoomEntity(HaEntity("$domain.x", "on", emptyMap())))
        }
    }
}
