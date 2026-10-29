package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.HaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermostatLogicTest {

    private fun entity(entityId: String, state: String, attrs: Map<String, Any?> = emptyMap()) =
        HaEntity(entityId, state, attrs)

    @Test
    fun rangeFractionClampsAndGuardsZeroSpan() {
        assertEquals(0f, rangeFraction(0.0, 50.0, 90.0), 0.001f)
        assertEquals(0.5f, rangeFraction(70.0, 50.0, 90.0), 0.001f)
        assertEquals(1f, rangeFraction(200.0, 50.0, 90.0), 0.001f)
        assertEquals(0f, rangeFraction(70.0, 70.0, 70.0), 0.001f)
    }

    @Test
    fun hvacActionLabels() {
        assertEquals("Heating", hvacActionLabel("heating"))
        assertEquals("Cooling", hvacActionLabel("cooling"))
        assertEquals("Idle", hvacActionLabel("idle"))
        assertEquals("Fan only", hvacActionLabel("fan"))
        assertNull(hvacActionLabel(null))
        assertNull(hvacActionLabel(""))
    }

    @Test
    fun presetPrefersClimateAttrThenCurrentModeSelect() {
        val climate = entity(
            "climate.thermostat", "heat_cool",
            mapOf("preset_mode" to "home"),
        )
        val states = mapOf(
            "select.thermostat_current_mode" to entity("select.thermostat_current_mode", "sleep"),
        )
        assertEquals("home", findPreset(states, climate))
        val noAttr = entity("climate.thermostat", "heat_cool")
        assertEquals("sleep", findPreset(states, noAttr))
        assertNull(findPreset(emptyMap(), noAttr))
        val unknown = mapOf(
            "select.thermostat_current_mode" to entity("select.thermostat_current_mode", "unknown"),
        )
        assertNull(findPreset(unknown, noAttr))
    }

    @Test
    fun clearHoldPrefersSharedPrefixThenAny() {
        val climate = entity("climate.thermostat", "heat_cool")
        val states = mapOf(
            "button.other_clear_hold" to entity("button.other_clear_hold", "unknown"),
            "button.thermostat_clear_hold" to entity("button.thermostat_clear_hold", "unknown"),
        )
        assertEquals(
            "button.thermostat_clear_hold",
            findClearHoldButton(states, climate)?.entityId,
        )
        val onlyOther = mapOf(
            "button.other_clear_hold" to entity("button.other_clear_hold", "unknown"),
        )
        assertEquals("button.other_clear_hold", findClearHoldButton(onlyOther, climate)?.entityId)
        assertNull(findClearHoldButton(emptyMap(), climate))
    }

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
}
