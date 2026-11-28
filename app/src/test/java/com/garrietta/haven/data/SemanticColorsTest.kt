package com.garrietta.haven.data

import com.garrietta.haven.ui.components.SemanticState
import com.garrietta.haven.ui.components.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage E (redesign): the centralized semantic mapping. Every slot's
 * text pair must clear 4.5:1 in BOTH themes (glyphs use onContainer, so
 * the 3:1 icon floor rides along), and the resolver must never invent
 * a reassuring state from an unknown one.
 */
class SemanticColorsTest {

    private fun entity(
        id: String,
        state: String,
        deviceClass: String? = null,
        hvacAction: String? = null,
    ): HaEntity {
        val attrs = mutableMapOf<String, Any?>()
        if (deviceClass != null) attrs["device_class"] = deviceClass
        if (hvacAction != null) attrs["hvac_action"] = hvacAction
        return HaEntity(id, state, attrs)
    }

    @Test
    fun everySlotClearsFourPointFiveInBothThemes() {
        for (state in SemanticState.entries) {
            for (isDark in listOf(false, true)) {
                val visual = semanticVisual(state, isDark)
                val ratio = contrastRatio(visual.onContainer, visual.container)
                assertTrue(
                    "$state dark=$isDark contrast $ratio below 4.5:1",
                    ratio >= 4.5f,
                )
            }
        }
    }

    @Test
    fun lightAndDarkVariantsDiffer() {
        // A mapping with identical light/dark pairs would be a bug:
        // dark pastels on black wash out, and vice versa.
        for (state in SemanticState.entries) {
            assertNotEquals(
                "$state must differ between themes",
                semanticVisual(state, false),
                semanticVisual(state, true),
            )
        }
    }

    @Test
    fun resolverAvailabilityFirst() {
        assertEquals(
            SemanticState.Unavailable,
            semanticStateFor(entity("light.lamp", "unavailable")),
        )
        assertEquals(
            SemanticState.Unknown,
            semanticStateFor(entity("light.lamp", "unknown")),
        )
        assertEquals(
            SemanticState.Unknown,
            semanticStateFor(entity("sensor.temp", "unknown", "temperature")),
        )
    }

    @Test
    fun resolverSwitchLikeDomains() {
        assertEquals(SemanticState.DeviceOn, semanticStateFor(entity("light.lamp", "on")))
        assertEquals(SemanticState.DeviceOff, semanticStateFor(entity("light.lamp", "off")))
        assertEquals(SemanticState.DeviceOn, semanticStateFor(entity("switch.plug", "on")))
        assertEquals(SemanticState.DeviceOff, semanticStateFor(entity("fan.bedroom", "off")))
    }

    @Test
    fun resolverLocks() {
        assertEquals(SemanticState.LockLocked, semanticStateFor(entity("lock.front", "locked")))
        assertEquals(SemanticState.LockUnlocked, semanticStateFor(entity("lock.front", "unlocked")))
        assertEquals(SemanticState.LockUnlocked, semanticStateFor(entity("lock.front", "opening")))
        assertEquals(SemanticState.LockFault, semanticStateFor(entity("lock.front", "jammed")))
        assertEquals(SemanticState.Unknown, semanticStateFor(entity("lock.front", "weird")))
    }

    @Test
    fun resolverBinarySensors() {
        assertEquals(
            SemanticState.MotionActive,
            semanticStateFor(entity("binary_sensor.motion", "on", "motion")),
        )
        assertEquals(
            SemanticState.MotionClear,
            semanticStateFor(entity("binary_sensor.motion", "off", "motion")),
        )
        assertEquals(
            SemanticState.OpenAlert,
            semanticStateFor(entity("binary_sensor.door", "on", "door")),
        )
        assertEquals(
            SemanticState.ClosedNeutral,
            semanticStateFor(entity("binary_sensor.door", "off", "door")),
        )
        assertEquals(
            SemanticState.AlarmFault,
            semanticStateFor(entity("binary_sensor.smoke", "on", "smoke")),
        )
        assertEquals(
            SemanticState.OpenAlert,
            semanticStateFor(entity("binary_sensor.generic", "on")),
        )
    }

    @Test
    fun resolverClimateMediaAlarmVacuum() {
        assertEquals(
            SemanticState.HeatingActive,
            semanticStateFor(entity("climate.thermostat", "heat", hvacAction = "heating")),
        )
        assertEquals(
            SemanticState.CoolingActive,
            semanticStateFor(entity("climate.thermostat", "cool", hvacAction = "cooling")),
        )
        assertEquals(
            SemanticState.ClimateIdle,
            semanticStateFor(entity("climate.thermostat", "heat", hvacAction = "idle")),
        )
        assertEquals(
            SemanticState.MediaPlaying,
            semanticStateFor(entity("media_player.tv", "playing")),
        )
        assertEquals(
            SemanticState.MediaIdle,
            semanticStateFor(entity("media_player.tv", "paused")),
        )
        assertEquals(
            SemanticState.AlarmFault,
            semanticStateFor(entity("alarm_control_panel.home", "triggered")),
        )
        assertEquals(
            SemanticState.ClosedNeutral,
            semanticStateFor(entity("alarm_control_panel.home", "disarmed")),
        )
        // Armed is secure (blue), not a fault.
        assertEquals(
            SemanticState.LockLocked,
            semanticStateFor(entity("alarm_control_panel.home", "armed_away")),
        )
        assertEquals(
            SemanticState.DeviceOn,
            semanticStateFor(entity("vacuum.t2292", "cleaning")),
        )
        assertEquals(
            SemanticState.AlarmFault,
            semanticStateFor(entity("vacuum.t2292", "error")),
        )
    }

    @Test
    fun resolverPowerThresholds() {
        assertEquals(
            SemanticState.EnergyLow,
            semanticStateFor(entity("sensor.plug", "499", "power")),
        )
        assertEquals(
            SemanticState.EnergyHigh,
            semanticStateFor(entity("sensor.plug", "500", "power")),
        )
        assertEquals(
            SemanticState.EnergyCritical,
            semanticStateFor(entity("sensor.plug", "2000", "power")),
        )
        // Custom thresholds move the boundaries.
        assertEquals(
            SemanticState.EnergyHigh,
            semanticStateFor(entity("sensor.plug", "100", "power"), energyWarnWatts = 50.0),
        )
        // Non-power sensors and non-numeric states have no energy slot.
        assertEquals(
            SemanticState.Unknown,
            semanticStateFor(entity("sensor.temp", "72", "temperature")),
        )
        assertEquals(
            SemanticState.Unknown,
            semanticStateFor(entity("sensor.plug", "off", "power")),
        )
        // Unmapped domains resolve neutral, never guessed.
        assertEquals(
            SemanticState.Unknown,
            semanticStateFor(entity("scene.movie", "2026-11-02T00:00:00")),
        )
    }

    @Test
    fun energySlotForWattsMatchesThresholds() {
        assertEquals(SemanticState.EnergyLow, energySlotForWatts(0.0))
        assertEquals(SemanticState.EnergyLow, energySlotForWatts(499.9))
        assertEquals(SemanticState.EnergyHigh, energySlotForWatts(500.0))
        assertEquals(SemanticState.EnergyHigh, energySlotForWatts(1999.9))
        assertEquals(SemanticState.EnergyCritical, energySlotForWatts(2000.0))
    }

    @Test
    fun importantAlertIsNarrow() {
        assertTrue(hasImportantAlert(listOf(entity("lock.front", "unlocked"))))
        assertTrue(hasImportantAlert(listOf(entity("binary_sensor.door", "on", "door"))))
        assertTrue(hasImportantAlert(listOf(entity("cover.garage", "open"))))
        assertTrue(hasImportantAlert(listOf(entity("alarm_control_panel.home", "triggered"))))
        // Armed is secure, motion is transient, lights/energy routine.
        assertFalse(hasImportantAlert(listOf(entity("alarm_control_panel.home", "armed_away"))))
        assertFalse(hasImportantAlert(listOf(entity("binary_sensor.motion", "on", "motion"))))
        assertFalse(hasImportantAlert(listOf(entity("light.lamp", "on"))))
        assertFalse(hasImportantAlert(listOf(entity("lock.front", "locked"))))
        assertFalse(hasImportantAlert(listOf(entity("binary_sensor.door", "off", "door"))))
        assertFalse(hasImportantAlert(emptyList()))
    }

    @Test
    fun alertDotDiffersByTheme() {
        assertNotEquals(alertDotColor(false), alertDotColor(true))
    }
}
