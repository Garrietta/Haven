package com.garrietta.haven.ui.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Rect
import com.garrietta.haven.ui.theme.settleSpring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feedback round 7: the Koda now-playing morph port's pure drag
 * math. Velocity momentum first, else 40dp travel; the finger
 * formula tracks 1:1 over the exact top-edge travel; the centered
 * popup lerps its placed rect toward the tile rect.
 */
class PopupDragTest {

    @Test
    fun settleSpringIsKodasCalmSpec() {
        val spec = settleSpring<Float>() as SpringSpec<Float>
        assertEquals(0.95f, spec.dampingRatio, 0.001f)
        assertEquals(350f, spec.stiffness, 0.001f)
    }

    @Test
    fun shouldDismissDragVelocityMomentumFirst() {
        // Downward drift commits regardless of travel.
        assertTrue(shouldDismissDrag(200f, 1f, 0.99f, 1000f, 40f))
        // Upward drift always restores, regardless of travel.
        assertFalse(shouldDismissDrag(-200f, 1f, 0.2f, 1000f, 40f))
        // Exactly at the boundary falls through to the travel rule.
        assertFalse(shouldDismissDrag(120f, 1f, 0.99f, 1000f, 40f))
        assertTrue(shouldDismissDrag(-120f, 1f, 0.2f, 1000f, 40f))
    }

    @Test
    fun shouldDismissDragTravelRule() {
        // Travel 0.10 of range > 40/1000 = 0.04: commits.
        assertTrue(shouldDismissDrag(0f, 1f, 0.90f, 1000f, 40f))
        // Travel 0.02 < 0.04: restores.
        assertFalse(shouldDismissDrag(0f, 1f, 0.98f, 1000f, 40f))
        // Exactly at the threshold does not commit (strict >).
        // Exact float boundary: travel 1.0 vs threshold 40/40 = 1.0
        // (a 0.96f-style boundary is not exact in float32: 1f - 0.96f
        // lands a hair above 0.04f and would commit).
        assertFalse(shouldDismissDrag(0f, 1f, 0f, 40f, 40f))
        // A slow upward drift restores even with no travel.
        assertFalse(shouldDismissDrag(-50f, 1f, 1f, 1000f, 40f))
    }

    @Test
    fun dragProgressAfterDeltaTracksOneToOne() {
        assertEquals(0.75f, dragProgressAfterDelta(1f, 250f, 1000f), 0.001f)
        // Dragging up restores progress.
        assertEquals(0.75f, dragProgressAfterDelta(0.5f, -250f, 1000f), 0.001f)
        // Clamped at both ends, never negative, never above 1.
        assertEquals(0f, dragProgressAfterDelta(0.1f, 500f, 1000f), 0.001f)
        assertEquals(1f, dragProgressAfterDelta(0.9f, -500f, 1000f), 0.001f)
        // Degenerate range coerces to 1px rather than dividing by zero.
        assertEquals(0f, dragProgressAfterDelta(1f, 2f, 0f), 0.001f)
    }

    @Test
    fun dragRangeForPanelOverlayIsTopEdgeTravel() {
        assertEquals(700f, dragRangeForPanelOverlay(800f, 100f, 1000f), 0.001f)
        // No source: the target height stands in.
        assertEquals(1000f, dragRangeForPanelOverlay(null, 100f, 1000f), 0.001f)
        // Degenerate (tile above target): the target height stands in.
        assertEquals(1000f, dragRangeForPanelOverlay(50f, 100f, 1000f), 0.001f)
    }

    @Test
    fun dragRangeForCenteredPopupIsCenterTravel() {
        assertEquals(300f, dragRangeForCenteredPopup(500f, 200f, 800f), 0.001f)
        assertEquals(800f, dragRangeForCenteredPopup(null, 200f, 800f), 0.001f)
        assertEquals(800f, dragRangeForCenteredPopup(500f, null, 800f), 0.001f)
        // Coincident centers: coerced to at least 1px.
        assertEquals(1f, dragRangeForCenteredPopup(500f, 500f, 800f), 0.001f)
    }

    @Test
    fun centeredPopupDragTransformLerpsRect() {
        val placed = Rect(0f, 0f, 400f, 800f)
        val tile = Rect(100f, 1400f, 300f, 1600f)
        // At rest the transform is identity.
        val rest = centeredPopupDragTransform(placed, tile, 1f)
        assertEquals(0f, rest.translation.x, 0.001f)
        assertEquals(0f, rest.translation.y, 0.001f)
        assertEquals(1f, rest.scaleX, 0.001f)
        assertEquals(1f, rest.scaleY, 0.001f)
        // Fully dragged: center lands on the tile center, size on the tile size.
        val landed = centeredPopupDragTransform(placed, tile, 0f)
        assertEquals(0f, landed.translation.x, 0.001f)
        assertEquals(1100f, landed.translation.y, 0.001f)
        assertEquals(0.5f, landed.scaleX, 0.001f)
        assertEquals(0.25f, landed.scaleY, 0.001f)
        // Halfway: halfway on both.
        val half = centeredPopupDragTransform(placed, tile, 0.5f)
        assertEquals(550f, half.translation.y, 0.001f)
        assertEquals(0.75f, half.scaleX, 0.001f)
        assertEquals(0.625f, half.scaleY, 0.001f)
    }

    @Test
    fun popupDragStateResetClearsAllFlags() {
        val state = PopupDragState()
        state.isDragging = true
        state.dragProgress = 0.3f
        state.dragStartProgress = 0.8f
        state.pulling = true
        state.settling = true
        state.reset()
        assertFalse(state.isDragging)
        assertEquals(1f, state.dragProgress, 0.001f)
        assertEquals(1f, state.dragStartProgress, 0.001f)
        assertFalse(state.pulling)
        assertFalse(state.settling)
    }
}
