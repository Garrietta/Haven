package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entity(id: String, name: String) = HaEntity(id, "on", mapOf("friendly_name" to name))

class DeviceSearchTest {

    @Test
    fun groupsByDeviceAndDropsEntityOnlyRows() {
        val entities = listOf(
            entity("switch.heater_auto_off", "Heater Auto Off"),
            entity("sensor.heater_temp", "Heater Temp"),
            entity("light.lamp", "Lamp"),
        )
        val groups = groupEntitiesByDevice(
            entities,
            mapOf("switch.heater_auto_off" to "dev1", "sensor.heater_temp" to "dev1"),
        )
        assertEquals(1, groups.size)
        assertEquals("dev1", groups[0].deviceId)
        assertEquals(listOf("sensor.heater_temp", "switch.heater_auto_off"), groups[0].entityIds)
    }

    @Test
    fun deviceMatchesByRegistryNameModelOrEntityName() {
        val group = DeviceGroup(
            "dev1",
            HaDeviceInfo("TP-Link P400M", "P400M", "TP-Link"),
            listOf("switch.heater_auto_off"),
        )
        val byId = mapOf("switch.heater_auto_off" to entity("switch.heater_auto_off", "Heater Auto Off"))
        assertTrue(deviceMatchesQuery("", group, byId))
        assertTrue(deviceMatchesQuery("p400m", group, byId))
        assertTrue(deviceMatchesQuery("tp-link", group, byId))
        assertTrue(deviceMatchesQuery("heater auto", group, byId))
        assertTrue(deviceMatchesQuery("switch.heater", group, byId))
        assertFalse(deviceMatchesQuery("lamp", group, byId))
    }

    @Test
    fun displayNameFallsBackToDeviceId() {
        assertEquals("Device dev9", deviceDisplayName(DeviceGroup("dev9", null, emptyList())))
        assertEquals("Heater", deviceDisplayName(DeviceGroup("dev1", HaDeviceInfo("Heater", null, null), emptyList())))
    }

    @Test
    fun literalNullAndBlankDeviceIdsAreDropped() {
        val entities = listOf(
            entity("light.a", "A"),
            entity("light.b", "B"),
            entity("light.c", "C"),
        )
        val groups = groupEntitiesByDevice(
            entities,
            mapOf("light.a" to "null", "light.b" to "NULL", "light.c" to "dev1"),
        )
        assertEquals(1, groups.size)
        assertEquals("dev1", groups[0].deviceId)
        assertEquals(listOf("light.c"), groups[0].entityIds)
    }

    @Test
    fun displayNameNeverRendersDeviceNull() {
        assertEquals("Unknown device", deviceDisplayName(DeviceGroup("null", null, emptyList())))
        assertEquals("Unknown device", deviceDisplayName(DeviceGroup("", null, emptyList())))
        assertFalse(deviceDisplayName(DeviceGroup("null", null, emptyList())).contains("null", ignoreCase = true))
        // A registry name still wins over any id weirdness.
        assertEquals("Heater", deviceDisplayName(DeviceGroup("null", HaDeviceInfo("Heater", null, null), emptyList())))
    }
}
