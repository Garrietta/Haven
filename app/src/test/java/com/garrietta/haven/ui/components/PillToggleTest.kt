package com.garrietta.haven.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 4 lamp-crash tests: the toggle knob's padding must stay
 * non-negative across the FULL on/off animation range, including
 * underdamped spring overshoot past either end. Regression test
 * for the 2026-11-02 07:45 trace — "Padding must be
 * non-negative" at VerticalPillToggle (Expressive.kt:927) — where
 * the unclamped spring value fed `4.dp + 84.dp * (1f - progress)`
 * and any frame with progress > 1 threw.
 */
class PillToggleTest {

    @Test
    fun verticalKnobPadding_endpoints() {
        // On = knob at the top inset, off = knob at the bottom inset.
        assertEquals(4.dp, verticalPillKnobTopPadding(1f))
        assertEquals(88.dp, verticalPillKnobTopPadding(0f))
    }

    @Test
    fun verticalKnobPadding_midpoint() {
        assertEquals(46.dp, verticalPillKnobTopPadding(0.5f))
    }

    @Test
    fun verticalKnobPadding_neverNegativeOnOvershoot() {
        // The exact fatal case: the ON swing overshooting past 1
        // used to compute 4 + 84 * (1 - 1.08) = -2.72dp. Now the
        // knob pins at the top inset instead of throwing.
        assertEquals(4.dp, verticalPillKnobTopPadding(1.08f))
        assertEquals(4.dp, verticalPillKnobTopPadding(1.5f))
        assertEquals(4.dp, verticalPillKnobTopPadding(2f))
        // The OFF swing overshooting below 0 pins at the bottom
        // inset instead of travelling past the track's edge.
        assertEquals(88.dp, verticalPillKnobTopPadding(-0.08f))
        assertEquals(88.dp, verticalPillKnobTopPadding(-0.5f))
        assertEquals(88.dp, verticalPillKnobTopPadding(-1f))
    }

    @Test
    fun verticalKnobPadding_sweepStaysInsideTrack() {
        // Full animation range plus wide overshoot on both sides:
        // every value lands inside [4, 88]dp, never negative.
        for (i in -100..200) {
            val pad = verticalPillKnobTopPadding(i / 100f)
            assertTrue("padding $pad at progress ${i / 100f} must be >= 0", pad >= 0.dp)
            assertTrue("padding $pad at progress ${i / 100f} must be <= 88dp", pad <= 88.dp)
        }
    }

    @Test
    fun verticalKnobPadding_monotonicInTravel() {
        // Within the travel range the knob moves smoothly from
        // bottom (off) to top (on) with no jumps.
        var prev = verticalPillKnobTopPadding(0f)
        for (i in 1..100) {
            val pad = verticalPillKnobTopPadding(i / 100f)
            assertTrue("padding must not increase as progress rises", pad <= prev)
            prev = pad
        }
    }

    @Test
    fun horizontalKnobPadding_endpointsAndOvershoot() {
        assertEquals(6.dp, pillKnobStartPadding(0f))
        assertEquals(46.dp, pillKnobStartPadding(1f))
        assertEquals(46.dp, pillKnobStartPadding(1.08f))
        assertEquals(6.dp, pillKnobStartPadding(-0.08f))
        for (i in -100..200) {
            val pad = pillKnobStartPadding(i / 100f)
            assertTrue("padding $pad at progress ${i / 100f} must be >= 0", pad >= 0.dp)
            assertTrue("padding $pad at progress ${i / 100f} must be <= 46dp", pad <= 46.dp)
        }
    }
}
