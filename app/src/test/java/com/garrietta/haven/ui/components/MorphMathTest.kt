package com.garrietta.haven.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class MorphMathTest {

    private val from = Rect(100f, 800f, 500f, 1000f)
    private val to = Rect(0f, 1200f, 1080f, 2400f)

    @Test
    fun lerpRect_endpoints() {
        assertEquals(from, lerpRect(from, to, 0f))
        assertEquals(to, lerpRect(from, to, 1f))
    }

    @Test
    fun lerpRect_midpoint() {
        assertEquals(Rect(50f, 1000f, 790f, 1700f), lerpRect(from, to, 0.5f))
    }

    @Test
    fun lerpRect_clamps() {
        assertEquals(from, lerpRect(from, to, -0.5f))
        assertEquals(to, lerpRect(from, to, 2f))
    }

    @Test
    fun sheetTargetRect_bottomAnchoredFullWidth() {
        val target = sheetTargetRect(Offset(0f, 0f), IntSize(1080, 2400), IntSize(1080, 1200))
        assertEquals(Rect(0f, 1200f, 1080f, 2400f), target)
    }

    @Test
    fun sheetTargetRect_respectsOverlayOrigin() {
        val target = sheetTargetRect(Offset(10f, 20f), IntSize(1080, 2400), IntSize(1080, 600))
        assertEquals(Rect(10f, 1820f, 1090f, 2420f), target)
    }

    @Test
    fun detailTargetRect_isOverlayRect() {
        val target = detailTargetRect(Offset(10f, 20f), IntSize(1080, 2400))
        assertEquals(Rect(10f, 20f, 1090f, 2420f), target)
    }

    @Test
    fun lerpRect_detailMorphAlwaysCoversSource() {
        // A card -> fullscreen morph must cover the source rect at every
        // progress value, so no blank card-shaped hole ever appears.
        val overlay = detailTargetRect(Offset.Zero, IntSize(1080, 2400))
        for (p in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val r = lerpRect(from, overlay, p)
            assertEquals(true, r.left <= from.left)
            assertEquals(true, r.top <= from.top)
            assertEquals(true, r.right >= from.right)
            assertEquals(true, r.bottom >= from.bottom)
        }
    }
}
