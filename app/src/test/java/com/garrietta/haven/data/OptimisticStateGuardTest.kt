package com.garrietta.haven.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests: the guard is pure Kotlin with no Android types. */
class OptimisticStateGuardTest {

    @Test
    fun contradictingEventIsDroppedWhileExpectationLives() {
        val guard = OptimisticStateGuard()
        guard.expect("light.lamp", "off", nowMs = 1000L)
        assertTrue(guard.shouldDrop("light.lamp", "on", nowMs = 1500L))
        // Still live after a drop: the next contradicting event drops too.
        assertTrue(guard.shouldDrop("light.lamp", "on", nowMs = 2000L))
    }

    @Test
    fun matchingEventAppliesAndClears() {
        val guard = OptimisticStateGuard()
        guard.expect("light.lamp", "off", nowMs = 1000L)
        assertFalse(guard.shouldDrop("light.lamp", "off", nowMs = 1500L))
        // Cleared: later events apply normally.
        assertFalse(guard.shouldDrop("light.lamp", "on", nowMs = 1600L))
    }

    @Test
    fun expiredExpectationStopsFiltering() {
        val guard = OptimisticStateGuard(windowMs = 4_000L)
        guard.expect("light.lamp", "off", nowMs = 1000L)
        assertFalse(guard.shouldDrop("light.lamp", "on", nowMs = 1000L + 4_001L))
    }

    @Test
    fun clearLetsLaterEventsThrough() {
        val guard = OptimisticStateGuard()
        guard.expect("light.lamp", "off", nowMs = 1000L)
        guard.clear("light.lamp")
        assertFalse(guard.shouldDrop("light.lamp", "on", nowMs = 1500L))
    }

    @Test
    fun unknownEntityNeverDrops() {
        val guard = OptimisticStateGuard()
        assertFalse(guard.shouldDrop("light.other", "on", nowMs = 1000L))
    }

    @Test
    fun newestExpectationWinsOnRapidRetoggle() {
        val guard = OptimisticStateGuard()
        guard.expect("light.lamp", "off", nowMs = 1000L)
        guard.expect("light.lamp", "on", nowMs = 1200L)
        // The newest expectation governs: "off" now contradicts it.
        assertTrue(guard.shouldDrop("light.lamp", "off", nowMs = 1500L))
        // And "on" confirms it.
        assertFalse(guard.shouldDrop("light.lamp", "on", nowMs = 1600L))
    }
}
