package com.garrietta.haven.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stream 4 (design-system sprint): lit light tiles show "On · N%",
 * converting HA's 0–255 brightness to a percentage.
 */
class LightBrightnessPercentTest {

    @Test
    fun conversionEndpointsAndMidpoint() {
        assertEquals(0, lightBrightnessPercent(0))
        assertEquals(50, lightBrightnessPercent(128))
        assertEquals(100, lightBrightnessPercent(255))
    }

    @Test
    fun typicalDimAndBrightValues() {
        assertEquals(10, lightBrightnessPercent(26))
        assertEquals(75, lightBrightnessPercent(191))
    }
}
