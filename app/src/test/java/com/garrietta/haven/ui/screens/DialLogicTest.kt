package com.garrietta.haven.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialLogicTest {

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
}
