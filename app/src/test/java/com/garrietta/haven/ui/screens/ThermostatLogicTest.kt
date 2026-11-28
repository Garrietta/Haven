package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.HaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermostatLogicTest {

    private fun entity(entityId: String, state: String, attrs: Map<String, Any?> = emptyMap()) =
        HaEntity(entityId, state, attrs)

    @Test
    fun linkedSensorsPreferSeededIdsThenPrefix() {
        val climate = entity("climate.thermostat", "heat_cool")
        val states = mapOf(
            "sensor.thermostat_current_temperature" to entity("sensor.thermostat_current_temperature", "67.2"),
            "sensor.thermostat_current_humidity" to entity("sensor.thermostat_current_humidity", "61"),
            "sensor.thermostat_battery" to entity("sensor.thermostat_battery", "100"),
        )
        assertEquals(
            listOf("sensor.thermostat_current_temperature", "sensor.thermostat_current_humidity"),
            findLinkedSensors(states, climate).map { it.entityId },
        )
        val unseeded = mapOf(
            "sensor.thermostat_battery" to entity("sensor.thermostat_battery", "100"),
            "sensor.thermostat_signal" to entity("sensor.thermostat_signal", "ok"),
            "sensor.other_temp" to entity("sensor.other_temp", "70"),
        )
        assertEquals(
            listOf("sensor.thermostat_battery"),
            findLinkedSensors(unseeded, climate).map { it.entityId },
        )
        assertTrue(findLinkedSensors(emptyMap(), climate).isEmpty())
    }

    @Test
    fun formatHvacModeReadsHeatCoolAsBulletPair() {
        assertEquals("Heat • Cool", formatHvacMode("heat_cool"))
        assertEquals("Heat", formatHvacMode("heat"))
        assertEquals("Cool", formatHvacMode("cool"))
        assertEquals("Off", formatHvacMode("off"))
        assertEquals("Fan only", formatHvacMode("fan_only"))
        assertEquals("Heat pump", formatHvacMode("heat_pump"))
    }

    @Test
    fun thermostatFamilyFollowsWhatItIsDoing() {
        assertEquals(com.garrietta.haven.ui.theme.StateFamily.Quiet, thermostatFamily(null))
        assertEquals(com.garrietta.haven.ui.theme.StateFamily.Quiet, thermostatFamily(entity("climate.t", "off")))
        assertEquals(com.garrietta.haven.ui.theme.StateFamily.Heat, thermostatFamily(entity("climate.t", "heat_cool", mapOf("hvac_action" to "heating"))))
        assertEquals(com.garrietta.haven.ui.theme.StateFamily.Cool, thermostatFamily(entity("climate.t", "cool")))
        assertEquals(com.garrietta.haven.ui.theme.StateFamily.On, thermostatFamily(entity("climate.t", "heat_cool", mapOf("hvac_action" to "idle"))))
    }
}
