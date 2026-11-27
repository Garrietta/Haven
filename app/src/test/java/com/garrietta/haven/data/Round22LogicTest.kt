package com.garrietta.haven.data

import com.garrietta.haven.Screen
import com.garrietta.haven.ui.components.railSelectedIndexWithFallback
import com.garrietta.haven.ui.components.scrollVisibilityAfter
import com.garrietta.haven.ui.components.topMorphSourceId
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.stateText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feedback round 22: the unavailable/unknown state formatting
 * (item 7), the rail-indicator fallback (item 6), the morph-source
 * camera fallback (item 3), and the scroll-visibility leftover
 * rule (item 5). All pure functions — no Android runtime.
 * Items 1, 2, and 4 are structural (overlay routing, popup top
 * buffers, card keying) with no pure logic to unit-test; they are
 * covered by the compile + device checks.
 */
class Round22LogicTest {

    // Item 7: unavailable/unknown states render clean, no unit suffix.
    @Test
    fun unavailableSensorHasNoUnitSuffix() {
        val sensor = HaEntity(
            "sensor.5179_2551_temperature", "unavailable",
            mapOf("unit_of_measurement" to "°F"),
        )
        assertEquals("Unavailable", sensor.stateText())
        assertEquals("Unavailable", sensor.dialogHeaderState())
    }

    @Test
    fun unknownSensorHasNoUnitSuffix() {
        val sensor = HaEntity(
            "sensor.temperature", "unknown",
            mapOf("unit_of_measurement" to "°F"),
        )
        assertEquals("Unknown", sensor.stateText())
    }

    @Test
    fun unavailableIsCaseInsensitive() {
        val sensor = HaEntity(
            "sensor.temperature", "UNAVAILABLE",
            mapOf("unit_of_measurement" to "°F"),
        )
        assertEquals("Unavailable", sensor.stateText())
    }

    @Test
    fun healthySensorKeepsItsUnit() {
        val sensor = HaEntity(
            "sensor.5179_2551_temperature", "56.48",
            mapOf("unit_of_measurement" to "°F"),
        )
        assertEquals("56.48 °F", sensor.stateText())
    }

    @Test
    fun unavailableNonSensorStaysClean() {
        val lamp = HaEntity("light.lamp_socket_1", "unavailable", emptyMap())
        assertEquals("Unavailable", lamp.stateText())
        assertEquals("Unavailable", lamp.dialogHeaderState())
    }

    // Item 3: the camera dialog is the last morph-source fallback.
    @Test
    fun topMorphSourceIdCameraFallback() {
        assertEquals("camera.x", topMorphSourceId(null, null, "camera.x"))
        assertEquals("panel", topMorphSourceId("panel", null, "camera.x"))
        assertEquals("detail", topMorphSourceId(null, "detail", "camera.x"))
        assertNull(topMorphSourceId(null, null, null))
        assertNull(topMorphSourceId(null, null))
    }

    // Item 5: the at-top leftover (onPostScroll) follows the same
    // threshold rule as the pre-scroll delta.
    @Test
    fun scrollLeftoverDownwardShowsBar() {
        assertTrue(scrollVisibilityAfter(false, 20f, 8f))
    }

    @Test
    fun scrollLeftoverUpwardHidesBar() {
        assertFalse(scrollVisibilityAfter(true, -20f, 8f))
    }

    @Test
    fun scrollLeftoverZeroLeavesStateUntouched() {
        assertFalse(scrollVisibilityAfter(false, 0f, 8f))
        assertTrue(scrollVisibilityAfter(true, 0f, 8f))
    }

    @Test
    fun scrollLeftoverSubThresholdLeavesStateUntouched() {
        assertFalse(scrollVisibilityAfter(false, 5f, 8f))
        assertTrue(scrollVisibilityAfter(true, -5f, 8f))
    }
}
