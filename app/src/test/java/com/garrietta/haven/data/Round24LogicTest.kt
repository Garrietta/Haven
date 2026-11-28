package com.garrietta.haven.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.components.dragRangeForCenteredPopup
import com.garrietta.haven.ui.components.dragRangeForPanelOverlay
import com.garrietta.haven.ui.components.morphSwapContainerAlpha
import com.garrietta.haven.ui.components.morphTileAlpha
import com.garrietta.haven.ui.components.resizePreviewScale
import com.garrietta.haven.ui.screens.tabTileGridIndices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Round 24: shape-index stability (item 4), the drag-range
 * minimum + binary tile/container handoff (item 3), Tall height
 * math + resize preview + reorder persistence (item 6). All
 * pure functions — no Android runtime. The device checks (no
 * flash on open/close cycles, Tall visibly doubling, a dragged
 * reorder surviving a restart) are covered by the compile +
 * device checks.
 */
class Round24LogicTest {

    // ---- Item 4: shape-index stability ----

    @Test
    fun shapeIndex_stableOrder() {
        val cards = listOf(
            "type: entities\nentity: light.lamp",
            "type: entities\nentities:\n  - light.a\n  - light.b",
            "type: entities\nentity: climate.thermostat",
        )
        val indices = tabTileGridIndices(cards)
        assertEquals(0, indices["light.lamp"])
        assertEquals(1, indices["light.a"])
        assertEquals(2, indices["light.b"])
        assertEquals(3, indices["climate.thermostat"])
    }

    @Test
    fun shapeIndex_nonEntityCardsContributeNothing() {
        val cards = listOf(
            "type: gauge\nentity: sensor.temp",
            "type: button\nentity: light.x",
            "just a string",
            "type: entities\nentity: light.lamp",
        )
        assertEquals(mapOf("light.lamp" to 0), tabTileGridIndices(cards))
    }

    @Test
    fun shapeIndex_duplicateKeepsFirstIndex() {
        val cards = listOf(
            "type: entities\nentity: light.lamp",
            "type: entities\nentity: light.lamp",
            "type: entities\nentity: light.other",
        )
        val indices = tabTileGridIndices(cards)
        assertEquals(0, indices["light.lamp"])
        // The duplicate consumed index 1 without moving the
        // first card's index, so the next card lands at 2.
        assertEquals(2, indices["light.other"])
    }

    @Test
    fun shapeIndex_missingEntityStillConsumesAnIndex() {
        // An id whose entity is missing from states still emits
        // a MissingEntityTile and still consumes an index, so
        // its neighbors never shift when the entity appears.
        val cards = listOf(
            "type: entities\nentity: light.gone",
            "type: entities\nentity: light.lamp",
        )
        val indices = tabTileGridIndices(cards)
        assertEquals(0, indices["light.gone"])
        assertEquals(1, indices["light.lamp"])
    }

    // ---- Item 3: drag-range minimum ----

    @Test
    fun dragRange_centeredPopup_minimum() {
        // Standing Lamp: placed and tile centers ~coincident
        // (10px apart) — without the floor a small finger
        // movement scrubbed the whole morph 3–4x too fast.
        // 950px = 380dp at a representative 2.5x density.
        assertEquals(950f, dragRangeForCenteredPopup(1000f, 1010f, 800f, minDragRangePx = 950f), 0.001f)
        // A genuinely long range passes through unchanged.
        assertEquals(1200f, dragRangeForCenteredPopup(1000f, 2200f, 800f, minDragRangePx = 950f), 0.001f)
        // Default (0) preserves the old behavior exactly.
        assertEquals(10f, dragRangeForCenteredPopup(1000f, 1010f, 800f), 0.001f)
        // Unknown centers fall back to the placed height,
        // floored the same way.
        assertEquals(950f, dragRangeForCenteredPopup(null, null, 800f, minDragRangePx = 950f), 0.001f)
    }

    @Test
    fun dragRange_panelOverlay_minimum() {
        assertEquals(950f, dragRangeForPanelOverlay(100f, 90f, 800f, minDragRangePx = 950f), 0.001f)
        assertEquals(1200f, dragRangeForPanelOverlay(1300f, 100f, 800f, minDragRangePx = 950f), 0.001f)
        assertEquals(10f, dragRangeForPanelOverlay(100f, 90f, 800f), 0.001f)
        // Null source falls back to the target height.
        assertEquals(950f, dragRangeForPanelOverlay(null, 100f, 800f, minDragRangePx = 950f), 0.001f)
    }

    // ---- Item 3: binary tile/container handoff ----

    @Test
    fun swapContainerAlpha_binary() {
        assertEquals(0f, morphSwapContainerAlpha(0f), 0.001f)
        assertEquals(0f, morphSwapContainerAlpha(0.01f), 0.001f)
        assertEquals(1f, morphSwapContainerAlpha(0.02f), 0.001f)
        assertEquals(1f, morphSwapContainerAlpha(0.5f), 0.001f)
        assertEquals(1f, morphSwapContainerAlpha(1f), 0.001f)
    }

    @Test
    fun tileAlpha_dragSnapCasesStayHidden() {
        // The Round 23 miss: a drag-release snap entered the
        // exit curve mid-way (progress 0.3 / 0.1) and the tile
        // jumped to morphExitCardAlpha(progress) in one frame.
        // The binary rule keeps it hidden at any progress
        // above 0.01, on both directions.
        assertEquals(0f, morphTileAlpha(0.3f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.1f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.5f, closing = false), 0.001f)
        assertEquals(1f, morphTileAlpha(0.01f, closing = true), 0.001f)
        assertEquals(1f, morphTileAlpha(0f, closing = false), 0.001f)
    }

    @Test
    fun tileAndContainerNeverOverlap() {
        // At every progress exactly one of tile / container is
        // visible: no frame shows both, no frame shows neither.
        for (p in listOf(0f, 0.005f, 0.01f, 0.02f, 0.3f, 0.85f, 1f)) {
            assertEquals(1f, morphTileAlpha(p, closing = true) + morphSwapContainerAlpha(p), 0.001f)
            assertEquals(1f, morphTileAlpha(p, closing = false) + morphSwapContainerAlpha(p), 0.001f)
        }
    }

    // ---- Item 6: Tall height math ----

    @Test
    fun tallHeight_isTwoNormalsPlusSpacing() {
        assertEquals(284.dp, tallCardHeightDp())
        assertEquals(136.dp * 2 + 12.dp, tallCardHeightDp())
    }

    // ---- Item 6: reorder persistence helper ----

    @Test
    fun movedByDelta_movesAndClamps() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", "c", "b", "d"), movedByDelta(list, "b", 1))
        assertEquals(listOf("a", "c", "d", "b"), movedByDelta(list, "b", 5))
        assertEquals(listOf("c", "a", "b", "d"), movedByDelta(list, "c", -2))
        assertEquals(listOf("b", "a", "c", "d"), movedByDelta(list, "b", -5))
    }

    @Test
    fun movedByDelta_noOpsReturnSameList() {
        val list = listOf("a", "b", "c", "d")
        assertSame(list, movedByDelta(list, "b", 0))
        assertSame(list, movedByDelta(list, "x", 1))
        // Moving the first card up or the last card down changes
        // nothing (clamped to the same slot).
        assertSame(list, movedByDelta(list, "a", -1))
        assertSame(list, movedByDelta(list, "d", 1))
    }

    // ---- Item 6: live resize preview ----

    @Test
    fun resizePreviewScale_stretchesTowardPendingSize() {
        // Full drag (96px+) stretches 1.8x on the pending axis.
        assertEquals(1.8f to 1f, resizePreviewScale(CardSize.NORMAL, Offset(100f, 0f)))
        assertEquals(1f to 1.8f, resizePreviewScale(CardSize.NORMAL, Offset(0f, 100f)))
        assertEquals(1f to 1.8f, resizePreviewScale(CardSize.WIDE, Offset(0f, 100f)))
        assertEquals(1.8f to 1f, resizePreviewScale(CardSize.TALL, Offset(100f, 0f)))
        // Half drag stretches halfway (1.4x).
        assertEquals(1.4f to 1f, resizePreviewScale(CardSize.NORMAL, Offset(48f, 0f)))
        assertEquals(1f to 1.4f, resizePreviewScale(CardSize.NORMAL, Offset(0f, 48f)))
    }

    @Test
    fun resizePreviewScale_noStretchForNormalOrNoDrag() {
        assertEquals(1f to 1f, resizePreviewScale(CardSize.NORMAL, null))
        assertEquals(1f to 1f, resizePreviewScale(CardSize.WIDE, null))
        // Pending Normal (dragging back toward Normal) reads as
        // no stretch: the card is already at its current size.
        assertEquals(1f to 1f, resizePreviewScale(CardSize.WIDE, Offset(-100f, 0f)))
        assertEquals(1f to 1f, resizePreviewScale(CardSize.TALL, Offset(0f, -100f)))
        // Below the 24px snap threshold the size is unchanged.
        assertEquals(1f to 1f, resizePreviewScale(CardSize.NORMAL, Offset(10f, 0f)))
    }
}
