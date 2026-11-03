package com.garrietta.haven.ui.components

import com.garrietta.haven.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Follow-up B (2026-11-03): Koda's directional tab-slide rule.
 * Bar order is Home, Scenes, Energy, Settings.
 */
class TabSlideTest {

    private val bar = listOf(Screen.Today, Screen.Scenes, Screen.Energy, Screen.Settings)

    @Test
    fun forwardToHigherRank() {
        assertEquals(1, tabSlideDirection(bar, Screen.Today, Screen.Scenes))
        assertEquals(1, tabSlideDirection(bar, Screen.Today, Screen.Settings))
        assertEquals(1, tabSlideDirection(bar, Screen.Scenes, Screen.Energy))
    }

    @Test
    fun backwardToLowerRank() {
        assertEquals(-1, tabSlideDirection(bar, Screen.Scenes, Screen.Today))
        assertEquals(-1, tabSlideDirection(bar, Screen.Settings, Screen.Today))
        assertEquals(-1, tabSlideDirection(bar, Screen.Energy, Screen.Scenes))
    }

    @Test
    fun nonTabScreensYieldNull() {
        assertNull(tabSlideDirection(bar, Screen.Today, Screen.HomeStatusPanel("lights_on")))
        assertNull(tabSlideDirection(bar, Screen.HomeStatusPanel("lights_on"), Screen.Settings))
        assertNull(tabSlideDirection(bar, Screen.EntityDetail("light.lamp"), Screen.Today))
    }

    @Test
    fun sameTabYieldsNull() {
        assertNull(tabSlideDirection(bar, Screen.Today, Screen.Today))
    }
}
