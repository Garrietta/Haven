package com.garrietta.haven.ui.components

import com.garrietta.haven.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The tab-slide rule: a tab to the right slides in from the right. */
class TabSlideTest {

    private val bar = listOf(Screen.Today, Screen.Security, Screen.Climate, Screen.Settings)

    @Test
    fun sameTabYieldsNull() {
        assertNull(tabSlideDirection(bar, Screen.Today, Screen.Today))
    }

    @Test
    fun forwardOffsetsClearContentAndRail() {
        // Unfolded: 1000px content + 84px rail — the outgoing page
        // must travel 1084px left, clearing the left screen edge,
        // not just the 1000px content boundary.
        assertEquals(1000 to -1084, tabSlideOffsets(1, 1000, 84))
    }

    @Test
    fun backwardOffsetsMirrorForward() {
        assertEquals(-1084 to 1000, tabSlideOffsets(-1, 1000, 84))
    }

    @Test
    fun foldedOffsetsHaveNoRailClearance() {
        assertEquals(1000 to -1000, tabSlideOffsets(1, 1000, 0))
        assertEquals(-1000 to 1000, tabSlideOffsets(-1, 1000, 0))
    }

    @Test
    fun nullDirectionBehavesAsForward() {
        assertEquals(1000 to -1084, tabSlideOffsets(null, 1000, 84))
    }
}
