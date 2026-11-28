package com.garrietta.haven.ui.screens

import androidx.compose.ui.geometry.Rect
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.frontDoorResolverStates
import com.garrietta.haven.data.resolveFrontDoorCameraSource
import com.garrietta.haven.ui.components.rectDiffersByMoreThan1px
import com.garrietta.haven.ui.components.shouldNavigateOnTabTap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feedback round 18: the round's new pure logic, pinned by tests. */
class Round18LogicTest {

    @Test
    fun useDetailTwoColumnLayout_numericOnly() {
        assertTrue(useDetailTwoColumnLayout("72.5"))
        assertTrue(useDetailTwoColumnLayout("0"))
        assertFalse(useDetailTwoColumnLayout("on"))
        assertFalse(useDetailTwoColumnLayout("open"))
        assertFalse(useDetailTwoColumnLayout("idle"))
        assertFalse(useDetailTwoColumnLayout(null))
        assertFalse(useDetailTwoColumnLayout(""))
    }

    @Test
    fun rectDiffersByMoreThan1px_firstCaptureAlwaysLands() {
        assertTrue(rectDiffersByMoreThan1px(null, Rect(0f, 0f, 100f, 100f)))
    }

    @Test
    fun rectDiffersByMoreThan1px_subPixelJitterDoesNotWrite() {
        val stored = Rect(10f, 20f, 110f, 120f)
        assertFalse(rectDiffersByMoreThan1px(stored, Rect(10.5f, 20f, 110f, 120f)))
        assertFalse(rectDiffersByMoreThan1px(stored, Rect(10f, 20f, 110f, 121f)))
        assertFalse(rectDiffersByMoreThan1px(stored, stored))
    }

    @Test
    fun rectDiffersByMoreThan1px_largerMovesDoWrite() {
        val stored = Rect(10f, 20f, 110f, 120f)
        assertTrue(rectDiffersByMoreThan1px(stored, Rect(11.01f, 20f, 110f, 120f)))
        assertTrue(rectDiffersByMoreThan1px(stored, Rect(10f, 20f, 110f, 118.5f)))
        // Exactly 1f is NOT more than 1f: no write (boundary pinned).
        assertFalse(rectDiffersByMoreThan1px(stored, Rect(11f, 20f, 110f, 120f)))
    }

    private fun entity(entityId: String, state: String) =
        HaEntity(entityId, state, mapOf("friendly_name" to entityId))

    @Test
    fun frontDoorResolverStates_feedsSharedResolver() {
        // 69a1 available -> leads, even with live_view present.
        val bothUp = frontDoorResolverStates(
            entity("camera.front_door_69a1", "streaming"),
            entity("camera.front_door_live_view", "idle"),
        )
        assertEquals("camera.front_door_69a1", resolveFrontDoorCameraSource(bothUp))
        // 69a1 down -> live_view leads; both down -> null.
        val fallback = frontDoorResolverStates(
            entity("camera.front_door_69a1", "unavailable"),
            entity("camera.front_door_live_view", "idle"),
        )
        assertEquals("camera.front_door_live_view", resolveFrontDoorCameraSource(fallback))
        val bothDown = frontDoorResolverStates(
            entity("camera.front_door_69a1", "unavailable"),
            entity("camera.front_door_live_view", "unavailable"),
        )
        assertEquals(null, resolveFrontDoorCameraSource(bothDown))
        // Absent entries behave like unavailable ones.
        assertEquals(null, resolveFrontDoorCameraSource(frontDoorResolverStates(null, null)))
        assertEquals(
            "camera.front_door_live_view",
            resolveFrontDoorCameraSource(frontDoorResolverStates(null, entity("camera.front_door_live_view", "idle"))),
        )
    }
}
