package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.HaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermostatLogicTest {

    private fun entity(entityId: String, state: String, attrs: Map<String, Any?> = emptyMap()) =
        HaEntity(entityId, state, attrs)

    // Feedback round 5, item 5: the dual-number gap is computed
    // from dial geometry — it spreads the pair to fill the inner
    // width with the edge clearance, floored at the ample minimum.
    @Test
    fun dualNumberGapSpreadsToFillInnerWidth() {
        // 300dp hero dial: inner width 244, two 76px labels,
        // 8px clearance per side -> 244 - 152 - 16 = 76px gap.
        assertEquals(76f, dualNumberGapPx(76f, 76f, 244f, 32f, 8f), 0.001f)
    }

    @Test
    fun dualNumberGapFloorsAtMinimumWhenDialIsNarrow() {
        // 240dp popout dial: inner width 184, two 78px labels,
        // 8px clearance -> fitting gap 12px, below the 32px floor.
        assertEquals(32f, dualNumberGapPx(78f, 78f, 184f, 32f, 8f), 0.001f)
        // 3-digit labels (100°) widen further; the floor still wins
        // so the numbers never touch each other.
        assertEquals(32f, dualNumberGapPx(102f, 78f, 184f, 32f, 8f), 0.001f)
        // Even absurdly wide labels never push the gap below the floor.
        assertEquals(32f, dualNumberGapPx(200f, 200f, 184f, 32f, 8f), 0.001f)
    }

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
        // Round 6: 270-degree sweep, start 135, end 405 = 45.
        val range = 50f..90f
        assertEquals(50f, dial240ValueFromAngle(135f, range)!!, 0.001f)
        assertEquals(70f, dial240ValueFromAngle(270f, range)!!, 0.001f)
        assertEquals(90f, dial240ValueFromAngle(45f, range)!!, 0.001f)
        assertEquals(90f, dial240ValueFromAngle(405f, range)!!, 0.001f)
        // The 90-degree bottom gap (45 -> 135 through 90) maps to nothing.
        assertNull(dial240ValueFromAngle(90f, range))
        assertNull(dial240ValueFromAngle(60f, range))
        assertNull(dial240ValueFromAngle(120f, range))
        // Just past the start is back on the arc (140 was in the old
        // 120-degree gap; the round-6 sweep reaches further down).
        assertEquals(50.74f, dial240ValueFromAngle(140f, range)!!, 0.01f)
    }

    @Test
    fun dial240AngleForValueRoundTrips() {
        val range = 50f..90f
        assertEquals(135f, dial240AngleForValue(50f, range), 0.001f)
        assertEquals(270f, dial240AngleForValue(70f, range), 0.001f)
        assertEquals(405f, dial240AngleForValue(90f, range), 0.001f)
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
        // still holds heat <= cool. Round 6 angle for 67.4 on the
        // 270-degree sweep: 135 + (17.4 / 40) * 270 = 252.45.
        val range = 50f..90f
        val raw = dial240ValueFromAngle(252.45f, range)!!
        assertEquals(67.4f, raw, 0.01f)
        assertEquals(67.0, snapToDegree(raw.toDouble()), 0.001)
        assertEquals(67.0, clampHeatSetpoint(67.0, 50.0, 70.0), 0.001)
        assertEquals(70.0, clampHeatSetpoint(72.0, 50.0, 70.0), 0.001)
    }

    @Test
    fun dualThumbRadiiFarApartStayOnTrack() {
        val (heat, cool) = dualThumbRadii(150f, 270f, 283.5f, 78.75f, 52.5f, 10.5f, 31.5f)
        assertEquals(283.5f, heat, 0.001f)
        assertEquals(283.5f, cool, 0.001f)
    }

    @Test
    fun dualThumbRadiiCloseStaggerFully() {
        // The verdict's 66/68 case on a 260dp dial (track 108dp).
        val (heat, cool) = dualThumbRadii(263.45f, 272.18f, 283.5f, 78.75f, 52.5f, 10.5f, 31.5f)
        assertEquals(252f, heat, 0.5f)
        assertEquals(315f, cool, 0.5f)
        assertTrue(cool - heat >= 52.5f)
    }

    @Test
    fun dualThumbRadiiIdenticalValuesStagger() {
        val (heat, cool) = dualThumbRadii(200f, 200f, 283.5f, 78.75f, 52.5f, 10.5f, 31.5f)
        assertEquals(252f, heat, 0.001f)
        assertEquals(315f, cool, 0.001f)
    }

    @Test
    fun dualThumbRadiiRampIsContinuous() {
        // At needed + ramp the offset is 0; at needed it is full.
        val needed = 78.75f + 10.5f
        val ramp = 31.5f
        val track = 283.5f
        fun offsetFor(arcPx: Float): Float {
            val deg = arcPx / track * 180f / Math.PI.toFloat()
            val (_, cool) = dualThumbRadii(200f, 200f + deg, track, 78.75f, 52.5f, 10.5f, ramp)
            return cool - track
        }
        assertEquals(0f, offsetFor(needed + ramp), 0.01f)
        assertEquals(31.5f, offsetFor(needed), 0.01f)
        val mid = offsetFor(needed + ramp / 2f)
        assertTrue(mid > 0f && mid < 31.5f)
    }

    // Round 6: the display range follows the DECADE formula —
    // lowAxis = max(40, floor(low/10)*10 - 10), highAxis =
    // min(100, ceil(high/10)*10 + 10) — SUPERSEDING the round-2
    // flat 10°F bracket (67/69 used to read 57–79; it now reads
    // 50–80). The 40/100 clamps are hard, HA ranges entirely
    // outside [40, 100] fall back untouched, and setpoint values
    // are never rewritten (only the display range).
    @Test
    fun thermostatDisplayRange_bracketsSetpointsByDecade() {
        assertEquals(50f..80f, thermostatDisplayRange(45f, 95f, listOf(67f, 69f)))
        assertEquals(50f..80f, thermostatDisplayRange(40f, 100f, listOf(67f, 69f)))
        // Single setpoint brackets the same way: 70 → 60–80.
        assertEquals(60f..80f, thermostatDisplayRange(40f, 100f, listOf(70f)))
    }

    @Test
    fun thermostatDisplayRange_round6Acceptance_66_67Reads50_80() {
        assertEquals(50f..80f, thermostatDisplayRange(45f, 95f, listOf(66f, 67f)))
        assertEquals(50f..80f, thermostatDisplayRange(40f, 100f, listOf(66f, 67f)))
    }

    @Test
    fun thermostatDisplayRange_clampsTo40_100() {
        // High setpoints: the top clamps at 100 (95 → 80–100:
        // floor(95/10)*10 - 10 = 80, ceil to 100 capped at 100).
        assertEquals(80f..100f, thermostatDisplayRange(40f, 100f, listOf(95f)))
        // Low setpoints: the bottom clamps at 40 (50/52 → 40–70).
        assertEquals(40f..70f, thermostatDisplayRange(30f, 100f, listOf(50f, 52f)))
        // No setpoints reported: HA range intersected with [40, 100].
        assertEquals(45f..95f, thermostatDisplayRange(45f, 95f, emptyList()))
    }

    @Test
    fun thermostatDisplayRange_clampsAt40ForLowSetpoint() {
        // 41°: floor(41/10)*10 - 10 = 30, clamped up to the hard 40.
        assertEquals(40f..60f, thermostatDisplayRange(40f, 100f, listOf(41f)))
    }

    @Test
    fun thermostatDisplayRange_clampsAt100ForHighSetpoint() {
        // 99°: ceil(99/10)*10 + 10 = 110, clamped down to the hard 100.
        assertEquals(80f..100f, thermostatDisplayRange(40f, 100f, listOf(99f)))
    }

    @Test
    fun thermostatDisplayRange_decadeBoundaries() {
        // Setpoints exactly on decades still get a full decade of
        // headroom on each side: 60/70 → 50–80.
        assertEquals(50f..80f, thermostatDisplayRange(40f, 100f, listOf(60f, 70f)))
    }

    @Test
    fun thermostatDisplayRange_fallsBackWhenNoOverlap() {
        // A Celsius thermostat (e.g. 10-30C) sits entirely outside
        // [40, 100], so the HA range is used as-is.
        assertEquals(10f..30f, thermostatDisplayRange(10f, 30f, listOf(21f)))
        assertEquals(10f..30f, thermostatDisplayRange(10f, 30f, emptyList()))
    }

    // Round 6: the Settings override, resolved by the ONE shared
    // function every surface uses (popout, sheet, detail).

    @Test
    fun resolveThermostatDisplayRange_overridePinsAxisRegardlessOfSetpoints() {
        // Explicit 50/80 pins the axis even for far-away setpoints.
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), 50f, 80f),
        )
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(70f, 72f), 50f, 80f),
        )
    }

    @Test
    fun resolveThermostatDisplayRange_clearReturnsToFormula() {
        // Both unset → the decade formula governs.
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), null, null),
        )
    }

    @Test
    fun resolveThermostatDisplayRange_partialOverrideFallsBackToFormula() {
        // Either value unset → the formula governs, never a
        // half-override.
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), 50f, null),
        )
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), null, 80f),
        )
    }

    @Test
    fun resolveThermostatDisplayRange_invertedOverrideFallsBackToFormula() {
        // Min above max → never inverted, never a crash: formula.
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), 90f, 50f),
        )
        // Equal ends are equally invalid.
        assertEquals(
            50f..80f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), 70f, 70f),
        )
    }

    @Test
    fun resolveThermostatDisplayRange_overrideClampsTo40_100() {
        // Out-of-band override ends clamp into [40, 100].
        assertEquals(
            40f..100f,
            resolveThermostatDisplayRange(40f, 100f, listOf(66f, 67f), 30f, 110f),
        )
    }

    @Test
    fun thermostatDisplayRange_degenerate() {
        assertEquals(70f..70f, thermostatDisplayRange(70f, 70f, listOf(70f)))
    }

    @Test
    fun dial240AngleForValue_clampsOutOfRange() {
        val range = 55f..85f
        assertEquals(dial240AngleForValue(55f, range), dial240AngleForValue(40f, range), 0.001f)
        assertEquals(dial240AngleForValue(85f, range), dial240AngleForValue(100f, range), 0.001f)
        // In-range round-trip still holds.
        assertTrue(dial240AngleForValue(67f, range) < dial240AngleForValue(69f, range))
    }

    @Test
    fun separatedDualAngles_passesThroughWhenAlreadySeparated() {
        val range = 55f..85f
        val heat = dial240AngleForValue(60f, range)
        val cool = dial240AngleForValue(75f, range)
        assertEquals(heat to cool, separatedDualAngles(heat, cool))
    }

    @Test
    fun separatedDualAngles_spreadsClosePairTo18Degrees() {
        val range = 55f..85f
        // Round 6: on the 270-degree sweep 1°F = 9° of arc, so
        // 67/68 sits 9 degrees apart, under the 18-degree floor.
        val heat = dial240AngleForValue(67f, range)
        val cool = dial240AngleForValue(68f, range)
        val (h, c) = separatedDualAngles(heat, cool)
        assertEquals(18f, c - h, 0.001f)
        // Symmetric about the pair midpoint, values untouched.
        assertEquals((heat + cool) / 2f, (h + c) / 2f, 0.001f)
        assertTrue(h < heat)
        assertTrue(c > cool)
    }

    @Test
    fun separatedDualAngles_67_69NowExactly18PassesThrough() {
        // Round 6: 2°F = 18° on the 270-degree sweep, exactly the
        // floor — the old reported overlap case needs no spreading.
        val range = 55f..85f
        val heat = dial240AngleForValue(67f, range)
        val cool = dial240AngleForValue(69f, range)
        assertEquals(18f, cool - heat, 0.001f)
        assertEquals(heat to cool, separatedDualAngles(heat, cool))
    }

    @Test
    fun separatedDualAngles_clampsIntoArcAtTheEnds() {
        // A pair hard against the arc start still ends separated
        // and inside [135, 405] (round-6 sweep).
        val (h, c) = separatedDualAngles(135f, 137f)
        assertEquals(18f, c - h, 0.001f)
        assertTrue(h >= 135f)
        assertTrue(c <= 405f)
        val (h2, c2) = separatedDualAngles(403f, 405f)
        assertEquals(18f, c2 - h2, 0.001f)
        assertTrue(h2 >= 135f)
        assertTrue(c2 <= 405f)
    }

    @Test
    fun separatedDualAngles_identicalAnglesSpreadSymmetrically() {
        val (h, c) = separatedDualAngles(300f, 300f)
        assertEquals(18f, c - h, 0.001f)
        assertEquals(300f, (h + c) / 2f, 0.001f)
    }
}
