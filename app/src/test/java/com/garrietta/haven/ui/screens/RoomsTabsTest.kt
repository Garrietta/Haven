package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.Placement
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomsTabsTest {

    @Test
    fun aStepMovesOnePlaceAndStopsAtTheEnds() {
        val ids = listOf("kitchen", "living", "bedroom")
        assertEquals(listOf("living", "kitchen", "bedroom"), stepped(ids, "living", -1))
        assertEquals(listOf("kitchen", "bedroom", "living"), stepped(ids, "living", 1))
        assertEquals(ids, stepped(ids, "kitchen", -1))
        assertEquals(ids, stepped(ids, "bedroom", 1))
        assertEquals(ids, stepped(ids, "garage", 1))
    }

    @Test
    fun rowsSayWhatTheyHoldAndWhereTheyArePinned() {
        assertEquals("No cards", cardCountLabel(0))
        assertEquals("1 card", cardCountLabel(1))
        assertEquals("4 cards", cardCountLabel(4))
        assertEquals("Room chip", placementLabel(Placement.ROOM_CHIP))
        assertEquals("On the bar", placementLabel(Placement.NAV_TAB))
        assertEquals("Hidden", placementLabel(Placement.HIDDEN))
    }
}
