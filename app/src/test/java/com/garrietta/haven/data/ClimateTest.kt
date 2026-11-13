package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimateTest {

    private fun e(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
        HaEntity(id, state, mapOf<String, Any?>("friendly_name" to name) + attrs)

    private fun temp(id: String, v: String, name: String) =
        e(id, v, name, "device_class" to "temperature", "unit_of_measurement" to "°F")

    private fun hum(id: String, v: String, name: String) =
        e(id, v, name, "device_class" to "humidity", "unit_of_measurement" to "%")

    @Test
    fun aDevicesTemperatureAndHumidityAreOneCard() {
        val states = listOf(
            temp("sensor.bedroom_temperature", "66", "Bedroom Temperature"),
            hum("sensor.bedroom_humidity", "44", "Bedroom Humidity"),
            temp("sensor.attic_temperature", "80", "Attic Temperature"),
        ).associateBy { it.entityId }
        val readings = climateReadings(
            states,
            entityDevices = mapOf("sensor.bedroom_temperature" to "d1", "sensor.bedroom_humidity" to "d1"),
            deviceNames = mapOf("d1" to "Bedroom sensor"),
            entityAreas = mapOf("sensor.bedroom_temperature" to "bedroom"),
        )
        assertEquals(listOf("Attic", "Bedroom sensor"), readings.map { it.name })
        val bedroom = readings[1]
        assertEquals(66.0, bedroom.temperatureValue!!, 0.0)
        assertEquals(44.0, bedroom.humidityValue!!, 0.0)
        assertEquals("bedroom", bedroom.areaId)
        assertNull(readings[0].humidity)
    }

    @Test
    fun outdoorReadingsGoLastAndSkippedOnesStayOut() {
        val states = listOf(
            temp("sensor.outdoor_temperature", "40", "Outdoor Temperature"),
            temp("sensor.kitchen_temperature", "70", "Kitchen Temperature"),
            temp("sensor.plug_internal_temperature", "95", "Plug Internal Temperature"),
            e("sensor.plug_signal", "-60", "Plug Signal", "device_class" to "signal_strength"),
        ).associateBy { it.entityId }
        val readings = climateReadings(states, emptyMap(), emptyMap(), emptyMap(), skip = setOf("sensor.plug_internal_temperature"))
        assertEquals(listOf("Kitchen", "Outdoor"), readings.map { it.name })
        assertTrue(readings.last().outdoor)
        assertFalse(readings.first().outdoor)
    }

    @Test
    fun aSensorNamedOnlyForWhatItMeasuresTakesItsRoomsName() {
        val states = mapOf("sensor.t" to temp("sensor.t", "67", "Temperature"))
        val readings = climateReadings(states, emptyMap(), emptyMap(), mapOf("sensor.t" to "lr"), areaNames = mapOf("lr" to "Living Room"))
        assertEquals("Living Room", readings.single().name)
    }

    @Test
    fun theSummaryAveragesInsideAndReadsOutside() {
        val readings = climateReadings(
            listOf(
                temp("sensor.a_temperature", "68", "A Temperature"),
                temp("sensor.b_temperature", "72", "B Temperature"),
                hum("sensor.a_humidity", "40", "A Humidity"),
                temp("sensor.outdoor_temperature", "35", "Outdoor Temperature"),
            ).associateBy { it.entityId },
            emptyMap(), emptyMap(), emptyMap(),
        )
        val summary = climateSummary(readings, emptyList(), weather = null)
        assertEquals(70.0, summary.inside!!, 0.0)
        assertEquals(40.0, summary.humidity!!, 0.0)
        assertEquals(35.0, summary.outside!!, 0.0)
        assertEquals("F", summary.unit)
    }

    @Test
    fun aThermostatIsWhatInsideReadsAndWeatherStandsInOutside() {
        val thermostat = e("climate.t", "heat", "T", "current_temperature" to 67, "current_humidity" to 41)
        val weather = e("weather.home", "sunny", "Home", "temperature" to 58, "temperature_unit" to "°F")
        val greenhouse = climateReadings(
            mapOf("sensor.g" to temp("sensor.g", "85", "Greenhouse Temperature")), emptyMap(), emptyMap(), emptyMap(),
        )
        val summary = climateSummary(greenhouse, listOf(thermostat), weather)
        assertEquals(67.0, summary.inside!!, 0.0)
        assertEquals(41.0, summary.humidity!!, 0.0)
        assertEquals(58.0, summary.outside!!, 0.0)
    }

    @Test
    fun aThermostatSaysWhatItIsDoing() {
        assertEquals("Heating to 70°", thermostatActivity(e("climate.t", "heat", "T", "hvac_action" to "heating", "temperature" to 70)))
        assertEquals("Idle · set to 68–75°", thermostatActivity(e("climate.t", "heat_cool", "T", "hvac_action" to "idle", "target_temp_low" to 68, "target_temp_high" to 75)))
        assertEquals("Off", thermostatActivity(e("climate.t", "off", "T")))
        assertEquals("Cooling to 72.5°", thermostatActivity(e("climate.t", "cool", "T", "hvac_action" to "cooling", "temperature" to 72.5)))
    }

    @Test
    fun comfortRunsColdToHot() {
        assertEquals(0f, comfortPosition(50.0, "F"), 0f)
        assertEquals(0.5f, comfortPosition(70.0, "F"), 0.001f)
        assertEquals(1f, comfortPosition(30.0, "C"), 0f)
    }

    @Test
    fun choicesReadAsWords() {
        assertEquals("Heat & cool", hvacModeLabel("heat_cool"))
        assertEquals("Away indefinitely", presetLabel("away_indefinitely"))
        assertEquals(0.5, setpointStep(e("climate.t", "heat", "T", "target_temp_step" to 0.5)), 0.0)
        assertEquals(1.0, setpointStep(e("climate.t", "heat", "T")), 0.0)
    }
}
