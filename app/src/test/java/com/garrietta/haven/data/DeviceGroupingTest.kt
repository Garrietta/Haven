package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceGroupingTest {

    private fun entity(id: String, category: String? = null, originalName: String? = null, disabled: String? = null) =
        RegistryEntity(id, null, originalName, null, null, "dev", disabled, null, "tapo", category)

    private fun state(id: String, name: String) = HaEntity(id, "on", mapOf("friendly_name" to name))

    @Test
    fun jsonNullReadsAsEmpty() {
        // Android's optString turns a JSON null into the text "null"; text() must not.
        val o = JSONObject("""{"area_id": null, "name": "Heater"}""")
        assertEquals("", o.text("area_id"))
        assertEquals("", o.text("missing"))
        assertEquals("Heater", o.text("name"))
        assertEquals("", JSONObject("""{"area_id": "null"}""").text("area_id"))
    }

    @Test
    fun entitiesFallIntoHomeAssistantsGroups() {
        assertEquals(EntityGroup.CONTROLS, entityGroup(entity("switch.heater")))
        assertEquals(EntityGroup.SENSORS, entityGroup(entity("sensor.heater_power")))
        assertEquals(EntityGroup.CONFIGURATION, entityGroup(entity("switch.heater_auto_off", category = "config")))
        assertEquals(EntityGroup.DIAGNOSTIC, entityGroup(entity("sensor.heater_signal", category = "diagnostic")))
    }

    @Test
    fun mainControlIsTheDevicesOwnSwitch() {
        val main = mainControl(
            listOf(
                entity("switch.heater_auto_off", category = "config", originalName = "Auto-off"),
                entity("switch.heater_led", originalName = "LED"),
                entity("switch.heater"),
                entity("sensor.heater_power", originalName = "Power"),
            ),
        )
        assertEquals("switch.heater", main?.entityId)
    }

    @Test
    fun noMainControlWithoutAnEnabledOnOff() {
        assertNull(mainControl(listOf(entity("sensor.temp"), entity("switch.off", disabled = "user"))))
    }

    @Test
    fun namesDropTheDevicePrefix() {
        assertEquals("Auto-off at", nameUnderDevice("Heater auto-off at", "Heater"))
        assertEquals("Heater", nameUnderDevice("Heater", "Heater"))
        assertEquals("Fan speed", nameUnderDevice("Fan speed", "Heater"))
    }

    @Test
    fun anAreaListsDevicesNotTheirEntities() {
        val members = areaMembers(
            entities = listOf(
                state("switch.heater", "Heater"),
                state("sensor.heater_power", "Heater power"),
                state("switch.heater_led", "Heater LED"),
                state("fan.fan", "Fan"),
                state("input_boolean.vents", "Vents open"),
            ),
            entityDevices = mapOf("switch.heater" to "d1", "sensor.heater_power" to "d1", "switch.heater_led" to "d1", "fan.fan" to "d2"),
            deviceNames = mapOf("d1" to "Greenhouse heater"),
        )
        assertEquals(listOf("Fan", "Greenhouse heater", "Vents open"), members.map { it.name })
        assertEquals(3, (members[1] as AreaMember.Device).entityCount)
        assertEquals("Heater, Heater LED, Heater power", (members[1] as AreaMember.Device).contents)
        assertEquals("input_boolean.vents", (members[2] as AreaMember.Lone).entity.entityId)
    }
}
