package com.garrietta.haven.ui.controls

import com.garrietta.haven.data.HaEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DialogHeaderStateTest {

    @Test
    fun weatherAppendsWholeTemperature() {
        val weather = HaEntity(
            "weather.forecast_home", "sunny",
            mapOf("friendly_name" to "Forecast Home", "temperature" to 72.0),
        )
        assertEquals("Sunny · 72°", weather.dialogHeaderState())
    }

    @Test
    fun weatherFormatsConditionAndDecimalTemperature() {
        val weather = HaEntity(
            "weather.forecast_home", "partlycloudy",
            mapOf("friendly_name" to "Forecast Home", "temperature" to 68.5),
        )
        assertEquals("Partly Cloudy · 68.5°", weather.dialogHeaderState())
    }

    @Test
    fun weatherWithoutTemperatureIsJustCondition() {
        val weather = HaEntity(
            "weather.forecast_home", "sunny",
            mapOf("friendly_name" to "Forecast Home"),
        )
        assertEquals("Sunny", weather.dialogHeaderState())
    }

    @Test
    fun nonWeatherIsExactlyStateText() {
        val sensor = HaEntity(
            "sensor.thermostat_current_temperature", "66.2",
            mapOf("friendly_name" to "Thermostat Current Temperature", "unit_of_measurement" to "°F"),
        )
        assertEquals("66.2 °F", sensor.dialogHeaderState())
        val climate = HaEntity(
            "climate.thermostat", "heat_cool",
            mapOf("friendly_name" to "Ecobee Thermostat"),
        )
        assertEquals("Heat cool", climate.dialogHeaderState())
    }
}
