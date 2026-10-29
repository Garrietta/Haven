package com.garrietta.haven.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Scroll-away visibility rule (production-upgrade round, 2026-10-29):
 * the pill slides out on a downward scroll past the threshold and
 * returns on an upward scroll past it; sub-threshold jitter never
 * flips the state, so the bar cannot twitch on tiny scrolls.
 * Sign convention: negative deltaY = scrolling down (content moves
 * up) = hide; positive deltaY = scrolling up = show.
 */
class PillScrollTest {

    private val threshold = 24f

    @Test
    fun downPastThreshold_hides() {
        assertEquals(false, scrollVisibilityAfter(true, -25f, threshold))
        // Already hidden stays hidden.
        assertEquals(false, scrollVisibilityAfter(false, -100f, threshold))
    }

    @Test
    fun upPastThreshold_shows() {
        assertEquals(true, scrollVisibilityAfter(false, 25f, threshold))
        // Already visible stays visible.
        assertEquals(true, scrollVisibilityAfter(true, 100f, threshold))
    }

    @Test
    fun subThreshold_unchanged() {
        assertEquals(true, scrollVisibilityAfter(true, -10f, threshold))
        assertEquals(false, scrollVisibilityAfter(false, 10f, threshold))
        assertEquals(true, scrollVisibilityAfter(true, 0f, threshold))
        assertEquals(false, scrollVisibilityAfter(false, 0f, threshold))
    }

    @Test
    fun exactlyAtThreshold_unchanged() {
        // The threshold is exclusive: exactly-at must not flip, or a
        // steady threshold-exact scroll would strobe the bar.
        assertEquals(true, scrollVisibilityAfter(true, -24f, threshold))
        assertEquals(false, scrollVisibilityAfter(false, 24f, threshold))
    }

    @Test
    fun reset_restoresVisible() {
        // Tab switches reset the latched state: a pill hidden by
        // scrolling on one tab must not stay hidden on the next tab
        // until the user scrolls up there (2026-10-29 verdict: the
        // Thermostat tab opened with no pill and no scroll happening).
        val state = ScrollVisibilityState(threshold)
        assertEquals(true, state.isVisible)
        state.nestedScrollConnection.onPreScroll(
            Offset(0f, -100f),
            NestedScrollSource.UserInput,
        )
        assertEquals(false, state.isVisible)
        state.reset()
        assertEquals(true, state.isVisible)
    }
}
