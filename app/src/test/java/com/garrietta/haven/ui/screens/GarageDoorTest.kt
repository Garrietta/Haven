package com.garrietta.haven.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class GarageDoorTest {

    @Test
    fun garageDoorFillFraction_positionEndpoints() {
        // 0% open = door down, frame full; 100% open = door up, empty.
        assertEquals(1f, garageDoorFillFraction(0, "closed"), 0.001f)
        assertEquals(0f, garageDoorFillFraction(100, "open"), 0.001f)
    }

    @Test
    fun garageDoorFillFraction_positionMidpoint() {
        // 45% open fills the frame from the top by 55%.
        assertEquals(0.55f, garageDoorFillFraction(45, "open"), 0.001f)
    }

    @Test
    fun garageDoorFillFraction_positionClamps() {
        // Out-of-range positions (bad sensor data) clamp, never
        // produce a fill outside [0, 1].
        assertEquals(1f, garageDoorFillFraction(-10, "closed"), 0.001f)
        assertEquals(0f, garageDoorFillFraction(150, "open"), 0.001f)
    }

    @Test
    fun garageDoorFillFraction_stateFallbacks() {
        // No position reported: open/closed map to the ends.
        assertEquals(0f, garageDoorFillFraction(null, "open"), 0.001f)
        assertEquals(1f, garageDoorFillFraction(null, "closed"), 0.001f)
    }

    @Test
    fun garageDoorFillFraction_inBetweenStatesAreMidTravel() {
        // Opening/closing/unknown without a position: neutral
        // mid-travel, never an invented position.
        assertEquals(0.5f, garageDoorFillFraction(null, "opening"), 0.001f)
        assertEquals(0.5f, garageDoorFillFraction(null, "closing"), 0.001f)
        assertEquals(0.5f, garageDoorFillFraction(null, "unavailable"), 0.001f)
    }

    @Test
    fun garageDoorFillFraction_positionBeatsState() {
        // Position is ground truth even when it disagrees with the
        // state string (a stale state during travel).
        assertEquals(0.7f, garageDoorFillFraction(30, "closed"), 0.001f)
        assertEquals(0.2f, garageDoorFillFraction(80, "open"), 0.001f)
    }
}
