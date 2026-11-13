package com.garrietta.haven.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The handoff between a popup and the tile it came from. The surface
 * must never be empty: at every progress either the popup's content, the
 * tile's recording, or both are visible, and the tile and the surface
 * swap on the same frame.
 */
class MorphGhostTest {

    @Test
    fun ghostIsWholeOnTheTileAndGoneOnceThePopupIsOpen() {
        assertEquals(1f, morphGhostAlpha(0f), 0.001f)
        assertEquals(1f, morphGhostAlpha(0.05f), 0.001f)
        assertEquals(0f, morphGhostAlpha(0.40f), 0.001f)
        assertEquals(0f, morphGhostAlpha(1f), 0.001f)
        // Overshoot from a bouncy spring stays in range.
        assertEquals(1f, morphGhostAlpha(-0.2f), 0.001f)
        assertEquals(0f, morphGhostAlpha(1.3f), 0.001f)
    }

    @Test
    fun theSurfaceIsNeverEmptyAtAnyProgress() {
        var p = 0.011f
        while (p <= 1f) {
            val visible = morphGhostAlpha(p) + morphEnterContentAlpha(p)
            assertTrue("nothing to see at progress $p", visible >= 0.5f)
            p += 0.01f
        }
    }

    @Test
    fun tileAndSurfaceSwapOnTheSameFrame() {
        // Exactly one of them is drawn at every progress, in both
        // directions: no frame with both, no frame with neither.
        for (p in listOf(0f, 0.005f, 0.01f, 0.011f, 0.02f, 0.5f, 1f)) {
            for (closing in listOf(false, true)) {
                assertEquals("progress $p", 1f, morphTileAlpha(p, closing) + morphSwapContainerAlpha(p), 0.001f)
            }
        }
    }

    @Test
    fun aRecordingIsOnlyUsedForTheTileItIsOf() {
        val source = Rect(40f, 300f, 380f, 572f) // 340 x 272
        assertTrue(ghostMatchesSource(IntSize(340, 272), source))
        assertTrue(ghostMatchesSource(IntSize(341, 271), source))
        // The same entity's wider favourite tile, or a taller one.
        assertFalse(ghostMatchesSource(IntSize(704, 272), source))
        assertFalse(ghostMatchesSource(IntSize(340, 568), source))
    }
}
