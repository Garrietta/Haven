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

    // ---- Dial interaction model: 1-degree steps, tap-toggle, handle-drag ----

    @Test
    fun snapToDegreeRoundsToWhole() {
        assertEquals(69.0, snapToDegree(68.5), 0.0)
        assertEquals(68.0, snapToDegree(68.4), 0.0)
        assertEquals(67.0, snapToDegree(67.0), 0.0)
        assertEquals(70.0, snapToDegree(70.2), 0.0)
    }

    @Test
    fun selectorTogglesAndDefaultsByMode() {
        assertEquals(SetpointSelector.COOLING, toggleSelector(SetpointSelector.HEATING))
        assertEquals(SetpointSelector.HEATING, toggleSelector(SetpointSelector.COOLING))
        assertEquals(SetpointSelector.COOLING, defaultSelector("cool"))
        assertEquals(SetpointSelector.HEATING, defaultSelector("heat"))
        assertEquals(SetpointSelector.HEATING, defaultSelector("heat_cool"))
        assertEquals(SetpointSelector.HEATING, defaultSelector("off"))
        assertEquals(SetpointSelector.HEATING, defaultSelector(null))
    }

    @Test
    fun setpointClampsKeepHeatBelowCool() {
        // Heating can never pass cooling, and stays inside the range.
        assertEquals(70.0, clampHeatSetpoint(75.0, 50.0, 70.0), 0.0)
        assertEquals(60.0, clampHeatSetpoint(60.0, 50.0, 70.0), 0.0)
        assertEquals(50.0, clampHeatSetpoint(40.0, 50.0, 70.0), 0.0)
        // Cooling can never drop below heating, and stays inside the range.
        assertEquals(67.0, clampCoolSetpoint(60.0, 67.0, 90.0), 0.0)
        assertEquals(80.0, clampCoolSetpoint(80.0, 67.0, 90.0), 0.0)
        assertEquals(90.0, clampCoolSetpoint(95.0, 67.0, 90.0), 0.0)
        // Snapping happens before clamping: 67.5 heat against a 67 cool
        // becomes 68, then clamps back to 67.
        assertEquals(67.0, clampHeatSetpoint(67.5, 50.0, 67.0), 0.0)
    }

    @Test
    fun dialAngleMapsSweepAndNullsInGap() {
        val range = 50f..90f
        assertEquals(50f, dialValueFromAngle(135f, range))
        assertEquals(70f, dialValueFromAngle(270f, range))
        assertEquals(90f, dialValueFromAngle(45f, range))
        // Bottom gap: 90 and 100 degrees sit past the 270-degree sweep.
        assertNull(dialValueFromAngle(90f, range))
        assertNull(dialValueFromAngle(100f, range))
    }

    @Test
    fun dialTapZonesSeparateInsideFromRing() {
        // Track radius 100px, 23px ring tolerance: center is inside, the
        // ring itself is ring, the corners are outside. Only inside taps
        // toggle the selector; ring and outside taps change nothing.
        assertEquals(DialTapZone.INSIDE, dialTapZone(0f, 0f, 0f, 0f, 100f, 23f))
        assertEquals(DialTapZone.INSIDE, dialTapZone(50f, 0f, 0f, 0f, 100f, 23f))
        assertEquals(DialTapZone.RING, dialTapZone(100f, 0f, 0f, 0f, 100f, 23f))
        assertEquals(DialTapZone.RING, dialTapZone(120f, 0f, 0f, 0f, 100f, 23f))
        assertEquals(DialTapZone.OUTSIDE, dialTapZone(124f, 0f, 0f, 0f, 100f, 23f))
        assertEquals(DialTapZone.OUTSIDE, dialTapZone(100f, 100f, 0f, 0f, 100f, 23f))
    }

    @Test
    fun handleGrabNeedsProximity() {
        assertTrue(isHandleGrab(10f, 10f, 10f, 10f, 28f))
        assertTrue(isHandleGrab(38f, 10f, 10f, 10f, 28f))
        assertTrue(!isHandleGrab(39f, 10f, 10f, 10f, 28f))
        assertTrue(!isHandleGrab(100f, 100f, 10f, 10f, 28f))
    }

    // ---- Google Home display: mode labels + outdoor temperature ----

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
    fun outdoorTemperaturePrefersSeededThenOutdoorSensorThenWeather() {
        val climate = entity("climate.thermostat", "heat_cool")
        val seeded = mapOf(
            "sensor.thermostat_outdoor_temperature" to
                entity("sensor.thermostat_outdoor_temperature", "52"),
        )
        assertEquals(52.0, findOutdoorTemperature(seeded, climate)!!, 0.0)
        val outdoorNamed = mapOf(
            "sensor.outdoor_temperature" to entity(
                "sensor.outdoor_temperature", "48",
                mapOf("device_class" to "temperature", "unit_of_measurement" to "°F"),
            ),
        )
        assertEquals(48.0, findOutdoorTemperature(outdoorNamed, climate)!!, 0.0)
        val weatherOnly = mapOf(
            "weather.forecast_home" to entity(
                "weather.forecast_home", "sunny",
                mapOf("temperature" to 52.5),
            ),
        )
        assertEquals(52.5, findOutdoorTemperature(weatherOnly, climate)!!, 0.0)
        // Seeded beats an outdoor-named sensor beats weather.
        assertEquals(
            52.0,
            findOutdoorTemperature(seeded + outdoorNamed + weatherOnly, climate)!!,
            0.0,
        )
        assertEquals(
            48.0,
            findOutdoorTemperature(outdoorNamed + weatherOnly, climate)!!,
            0.0,
        )
    }

    @Test
    fun outdoorTemperatureRejectsHumidityAndAmbiguousWeather() {
        val climate = entity("climate.thermostat", "heat_cool")
        // An outdoor humidity sensor never qualifies, even though its id
        // mentions "outdoor".
        val humidityOnly = mapOf(
            "sensor.outdoor_humidity" to entity(
                "sensor.outdoor_humidity", "61",
                mapOf("device_class" to "humidity", "unit_of_measurement" to "%"),
            ),
        )
        assertNull(findOutdoorTemperature(humidityOnly, climate))
        assertNull(findOutdoorTemperature(emptyMap(), climate))
        // Two weather entities: ambiguous, so no row rather than a guess.
        val twoWeather = mapOf(
            "weather.home" to entity("weather.home", "sunny", mapOf("temperature" to 50.0)),
            "weather.cabin" to entity("weather.cabin", "cloudy", mapOf("temperature" to 40.0)),
        )
        assertNull(findOutdoorTemperature(twoWeather, climate))
    }

    @Test
    fun outdoorTemperatureConvertsCelsiusSensors() {
        val climate = entity("climate.thermostat", "heat_cool")
        val celsius = mapOf(
            "sensor.outdoor_temperature" to entity(
                "sensor.outdoor_temperature", "10",
                mapOf("device_class" to "temperature", "unit_of_measurement" to "°C"),
            ),
        )
        assertEquals(50.0, findOutdoorTemperature(celsius, climate)!!, 0.0)
    }

    // -- The spec's 240-degree dial (ThermostatDial). The 270-degree tests
    // above are untouched; these cover only the new dial. --

    @Test
    fun dial240MapsEndpointsMidpointAndGap() {
        val range = 50f..90f
        assertEquals(50f, dial240ValueFromAngle(150f, range)!!, 0.001f)
        assertEquals(70f, dial240ValueFromAngle(270f, range)!!, 0.001f)
        assertEquals(90f, dial240ValueFromAngle(30f, range)!!, 0.001f)
        assertEquals(90f, dial240ValueFromAngle(390f, range)!!, 0.001f)
        // The 120-degree bottom gap (30 -> 150 through 90) maps to nothing.
        assertNull(dial240ValueFromAngle(90f, range))
        assertNull(dial240ValueFromAngle(100f, range))
        assertNull(dial240ValueFromAngle(140f, range))
    }

    @Test
    fun dial240AngleForValueRoundTrips() {
        val range = 50f..90f
        assertEquals(150f, dial240AngleForValue(50f, range), 0.001f)
        assertEquals(270f, dial240AngleForValue(70f, range), 0.001f)
        assertEquals(390f, dial240AngleForValue(90f, range), 0.001f)
        assertEquals(70f, dial240ValueFromAngle(dial240AngleForValue(70f, range), range)!!, 0.001f)
    }

    @Test
    fun rubberBandPassesInsideValuesAndDampsOvershoot() {
        val range = 50f..90f
        assertEquals(70f, rubberBandedValue(70f, range), 0.001f)
        assertEquals(50f, rubberBandedValue(50f, range), 0.001f)
        // 10 degrees past the top end damps to 3.5 past it.
        assertEquals(93.5f, rubberBandedValue(100f, range), 0.001f)
        // 10 degrees below the bottom end damps to 3.5 below it.
        assertEquals(46.5f, rubberBandedValue(40f, range), 0.001f)
    }

    @Test
    fun dial240SnapsToWholeDegreesUnderneath() {
        // The dial drags continuously, but the applied value snaps (the 1-degree
        // rule): a raw 67.4 from the angle snaps to 67, and the clamp pair
        // still holds heat <= cool.
        val range = 50f..90f
        val raw = dial240ValueFromAngle(254.4f, range)!!
        assertEquals(67.4f, raw, 0.01f)
        assertEquals(67.0, snapToDegree(raw.toDouble()), 0.001)
        assertEquals(67.0, clampHeatSetpoint(67.0, 50.0, 70.0), 0.001)
        assertEquals(70.0, clampHeatSetpoint(72.0, 50.0, 70.0), 0.001)
    }
}
