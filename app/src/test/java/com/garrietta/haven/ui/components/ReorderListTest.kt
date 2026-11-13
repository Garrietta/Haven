package com.garrietta.haven.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReorderListTest {

    private val rows = listOf(RowSpan("a", 0, 100), RowSpan("b", 100, 300), RowSpan("c", 400, 100))

    @Test
    fun aRowTakesTheSlotItsMiddleIsOver() {
        // "a" dragged down until its middle (top + 50) is inside "b".
        assertEquals("b", reorderTarget("a", dragTop = 60f, dragSize = 100, candidates = rows))
        // Rows of different heights: "c" dragged up into the tall "b".
        assertEquals("b", reorderTarget("c", dragTop = 300f, dragSize = 100, candidates = rows))
    }

    @Test
    fun aRowOverItsOwnSlotOrNothingStaysPut() {
        assertNull(reorderTarget("a", dragTop = 20f, dragSize = 100, candidates = rows))
        assertNull(reorderTarget("c", dragTop = 900f, dragSize = 100, candidates = rows))
    }

    @Test
    fun movingByKeyKeepsEverythingElseInOrder() {
        assertEquals(listOf("b", "c", "a"), moveKeyed(listOf("a", "b", "c"), "a", "c"))
        assertEquals(listOf("c", "a", "b"), moveKeyed(listOf("a", "b", "c"), "c", "a"))
        assertEquals(listOf("a", "b", "c"), moveKeyed(listOf("a", "b", "c"), "a", "missing"))
    }

    @Test
    fun theListScrollsOnlyWhileTheRowIsPastAnEdge() {
        assertEquals(0f, edgeScrollSpeed(100f, 200f, 0, 1000, 24f), 0.001f)
        assertEquals(10f, edgeScrollSpeed(940f, 1040f, 0, 1000, 24f), 0.001f)
        assertEquals(-5f, edgeScrollSpeed(-20f, 80f, 0, 1000, 24f), 0.001f)
        // A row flung far past the edge does not race the list away.
        assertEquals(24f, edgeScrollSpeed(2000f, 2100f, 0, 1000, 24f), 0.001f)
    }

    @Test
    fun equalRowsMoveOneSlotPerRowDragged() {
        assertEquals(1, columnTarget(start = 1, draggedPx = 40f, rowHeight = 100, count = 4))
        assertEquals(2, columnTarget(start = 1, draggedPx = 60f, rowHeight = 100, count = 4))
        assertEquals(0, columnTarget(start = 1, draggedPx = -160f, rowHeight = 100, count = 4))
        // Held inside the list at both ends.
        assertEquals(3, columnTarget(start = 1, draggedPx = 900f, rowHeight = 100, count = 4))
        assertEquals(0, columnTarget(start = 1, draggedPx = -900f, rowHeight = 100, count = 4))
        assertEquals(2, columnTarget(start = 2, draggedPx = 300f, rowHeight = 0, count = 4))
    }

    @Test
    fun movingAmongTheShownLeavesTheRestInTheirSlots() {
        // "rooms" has no row; the shown sections move around it.
        val full = listOf("cameras", "weather", "rooms", "status", "favorites")
        val shown = listOf("cameras", "weather", "status", "favorites")
        assertEquals(listOf("weather", "status", "rooms", "cameras", "favorites"), reorderSubset(full, shown, 0, 2))
        assertEquals(full, reorderSubset(full, shown, 1, 1))
        assertEquals(full, reorderSubset(full, shown, 0, 9))
    }
}
