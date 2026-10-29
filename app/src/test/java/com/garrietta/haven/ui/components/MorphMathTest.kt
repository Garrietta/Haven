package com.garrietta.haven.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

    @Test
    fun fallbackFromRect_shiftsDownOnly() {
        // The graceful sourceless origin is the target rect translated
        // straight down: same left/right/width/height, never scaled.
        val target = Rect(0f, 1200f, 1080f, 2400f)
        assertEquals(Rect(0f, 1280f, 1080f, 2480f), fallbackFromRect(target, 80f))
        assertEquals(target, fallbackFromRect(target, 0f))
    }

    @Test
    fun fallbackFromRect_morphStaysInTargetAtEnd() {
        // Lerping from the fallback to the target ends exactly on the
        // target at every edge, so no snap on arrival.
        val target = Rect(10f, 20f, 1090f, 2420f)
        assertEquals(target, lerpRect(fallbackFromRect(target, 80f), target, 1f))
    }

    @Test
    fun morphCornerTop_endpoints() {
        assertEquals(28.dp, morphCornerTop(0f))
        assertEquals(0.dp, morphCornerTop(1f))
    }

    @Test
    fun morphCornerTop_midpoint() {
        assertEquals(14.dp, morphCornerTop(0.5f))
    }

    @Test
    fun morphCornerTop_neverNegative() {
        // 2026-10-29 crash round: the bounds spring (0.8 damping)
        // overshoots past 1, and the old formula 28.dp * (1f - progress)
        // went negative at the peak (six crashes, topStart = topEnd
        // about -0.36..-0.67px), throwing in createOutline during the
        // layer update. No progress value may ever yield a negative
        // corner again — including the exact observed overshoot.
        for (p in listOf(-1f, -0.5f, 1.00493f, 1.5f, 2f)) {
            assertEquals(true, morphCornerTop(p) >= 0.dp)
        }
        assertEquals(0.dp, morphCornerTop(1.00493f))
    }

    @Test
    fun morphContentAlpha_hiddenWhilePanelGrows() {
        // The steep fade: content is fully invisible while the panel
        // is still growing, so no frame ever shows content clipped by
        // the rect edge (the 2026-10-29 "clipping" verdict: sheets AND
        // detail pages, where destination content duplicates the card).
        assertEquals(0f, morphContentAlpha(0f), 0.001f)
        assertEquals(0f, morphContentAlpha(0.5f), 0.001f)
        assertEquals(0f, morphContentAlpha(0.7f), 0.001f)
    }

    @Test
    fun morphContentAlpha_fadesInOverLastThird() {
        assertEquals(0.5f, morphContentAlpha(0.85f), 0.001f)
        assertEquals(1f, morphContentAlpha(1f), 0.001f)
    }

    @Test
    fun morphContentAlpha_clampsAndIsMonotonic() {
        assertEquals(0f, morphContentAlpha(-1f), 0.001f)
        assertEquals(1f, morphContentAlpha(1.5f), 0.001f)
        assertEquals(1f, morphContentAlpha(1.00493f), 0.001f)
        var prev = 0f
        for (p in listOf(0f, 0.5f, 0.7f, 0.8f, 0.9f, 1f)) {
            val a = morphContentAlpha(p)
            assertEquals(true, a >= prev)
            prev = a
        }
    }

    @Test
    fun centerTargetRect_centersPanel() {
        // Panel smaller than the overlay: centered on both axes, with
        // the overlay origin respected (left = 10 + (1080-800)/2, top =
        // 20 + (2400-1560)/2).
        assertEquals(
            Rect(150f, 440f, 950f, 2000f),
            centerTargetRect(Offset(10f, 20f), IntSize(1080, 2400), IntSize(800, 1560)),
        )
    }

    @Test
    fun centerTargetRect_clampsOversizedPanel() {
        // Panel larger than the overlay: clamped inside the overlay,
        // never extending past the screen.
        assertEquals(
            Rect(10f, 20f, 1090f, 2420f),
            centerTargetRect(Offset(10f, 20f), IntSize(1080, 2400), IntSize(2000, 3000)),
        )
    }
}
