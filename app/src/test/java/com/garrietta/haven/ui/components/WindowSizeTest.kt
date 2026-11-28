package com.garrietta.haven.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stream 3 (design-system sprint): canonical window size classes.
 * Breakpoints are Compact < 600dp, Medium 600–839dp, Expanded ≥ 840dp.
 * The Fold8 unfolded (~690dp portrait / ~830dp landscape) lands in
 * Medium — two-pane behavior starts at Medium, Expanded only adds
 * optimization on top.
 */
class WindowSizeTest {

    @Test
    fun canonicalBreakpoints() {
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassFor(599.dp))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassFor(600.dp))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassFor(839.dp))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassFor(840.dp))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassFor(1400.dp))
    }

    @Test
    fun fold8PosturesLandInMedium() {
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassFor(360.dp))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassFor(690.dp))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassFor(830.dp))
    }
}
