package com.garrietta.haven.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun morphCloseContentAlpha_fadesOutEarlyOnClose() {
        // At rest the panel content is fully visible; it is gone by
        // progress 0.85, while the container is still most of the way
        // to the card — so no expanded content ever shrinks over the
        // grid at full opacity.
        assertEquals(1f, morphCloseContentAlpha(1f), 0.001f)
        assertEquals(0f, morphCloseContentAlpha(0.85f), 0.001f)
        assertEquals(0f, morphCloseContentAlpha(0f), 0.001f)
    }

    @Test
    fun morphCloseContentAlpha_clamps() {
        assertEquals(1f, morphCloseContentAlpha(2f), 0.001f)
        assertEquals(0f, morphCloseContentAlpha(-0.5f), 0.001f)
    }

    @Test
    fun morphEnterContentAlpha_symmetricEnterCurve() {
        // Round 5: content invisible through p = 0.15 (nothing at
        // full opacity over the card on the first frame), fully in
        // by p = 0.50, and clamped outside [0, 1].
        assertEquals(0f, morphEnterContentAlpha(0f), 0.001f)
        assertEquals(0f, morphEnterContentAlpha(0.15f), 0.001f)
        assertEquals(1f, morphEnterContentAlpha(0.5f), 0.001f)
        assertEquals(1f, morphEnterContentAlpha(1f), 0.001f)
        assertEquals(0f, morphEnterContentAlpha(-0.5f), 0.001f)
        assertEquals(1f, morphEnterContentAlpha(2f), 0.001f)
    }

    @Test
    fun morphCloseBackgroundAlpha_opaqueThroughEightyPercent() {
        // Round 5: the surface stays opaque from rest (p = 1) down
        // to p = 0.20, then fades linearly to transparent at p = 0,
        // so the stationary card is revealed smoothly, never popped.
        assertEquals(1f, morphCloseBackgroundAlpha(1f), 0.001f)
        assertEquals(1f, morphCloseBackgroundAlpha(0.5f), 0.001f)
        assertEquals(1f, morphCloseBackgroundAlpha(0.20f), 0.001f)
        assertEquals(0.5f, morphCloseBackgroundAlpha(0.10f), 0.001f)
        assertEquals(0f, morphCloseBackgroundAlpha(0f), 0.001f)
        assertEquals(1f, morphCloseBackgroundAlpha(2f), 0.001f)
        assertEquals(0f, morphCloseBackgroundAlpha(-0.5f), 0.001f)
    }

    @Test
    fun morphPanelContainerAlphaOpen_fadesInOverFirstFifteenPercent() {
        // Round 6 reconciled open curve: invisible at p = 0 (the
        // stationary card shows through), opaque from p = 0.15.
        assertEquals(0f, morphPanelContainerAlphaOpen(0f), 0.001f)
        assertEquals(0.5f, morphPanelContainerAlphaOpen(0.075f), 0.001f)
        assertEquals(1f, morphPanelContainerAlphaOpen(0.15f), 0.001f)
        assertEquals(1f, morphPanelContainerAlphaOpen(1f), 0.001f)
        assertEquals(0f, morphPanelContainerAlphaOpen(-0.5f), 0.001f)
        assertEquals(1f, morphPanelContainerAlphaOpen(2f), 0.001f)
    }

    @Test
    fun morphPanelContainerAlphaClose_fadesOutOverFinalTwentyFivePercent() {
        // Round 6 reconciled close curve: opaque until p = 0.25,
        // then out to transparent at p = 0 (supersedes Round 5's
        // [0.20, 0] window).
        assertEquals(1f, morphPanelContainerAlphaClose(1f), 0.001f)
        assertEquals(1f, morphPanelContainerAlphaClose(0.25f), 0.001f)
        assertEquals(0.5f, morphPanelContainerAlphaClose(0.125f), 0.001f)
        assertEquals(0f, morphPanelContainerAlphaClose(0f), 0.001f)
        assertEquals(1f, morphPanelContainerAlphaClose(2f), 0.001f)
        assertEquals(0f, morphPanelContainerAlphaClose(-0.5f), 0.001f)
    }

    @Test
    fun morphPanelBackgroundBlend_reachesPanelColorAtThirtyFivePercent() {
        // Round 6 color lerp: source card color at p = 0, panel
        // background from p = 0.35 (smoothstep, so the midpoint is
        // exactly 0.5 at p = 0.175).
        assertEquals(0f, morphPanelBackgroundBlend(0f), 0.001f)
        assertEquals(0.5f, morphPanelBackgroundBlend(0.175f), 0.001f)
        assertEquals(1f, morphPanelBackgroundBlend(0.35f), 0.001f)
        assertEquals(1f, morphPanelBackgroundBlend(1f), 0.001f)
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

    @Test
    fun morphBackgroundBlend_endpoints() {
        // Card color at morph start, destination color once the first
        // 30% is done — the blend exists to remove the hard color step
        // at morph start that read as a white flash in light mode
        // (2026-10-29 verdict videos on run #60).
        assertEquals(0f, morphBackgroundBlend(0f), 0.001f)
        assertEquals(1f, morphBackgroundBlend(0.3f), 0.001f)
        assertEquals(1f, morphBackgroundBlend(1f), 0.001f)
    }

    @Test
    fun morphBackgroundBlend_midpoint() {
        // Smoothstep midpoint of [0, 0.3] is 0.15 -> 0.5.
        assertEquals(0.5f, morphBackgroundBlend(0.15f), 0.001f)
    }

    @Test
    fun morphCardAlpha_fadesOutOverFirstFortyPercent() {
        // Container-transform card handoff: visible at morph start,
        // gone once the surface is opaque; reversed automatically on
        // exit (the card fades back in over the last 40% of the
        // return, whole — never staged, never a snap).
        assertEquals(1f, morphCardAlpha(0f), 0.001f)
        assertEquals(0.5f, morphCardAlpha(0.2f), 0.001f)
        assertEquals(0f, morphCardAlpha(0.4f), 0.001f)
        assertEquals(0f, morphCardAlpha(1f), 0.001f)
    }

    @Test
    fun morphCardAlpha_clampsAndOvershoot() {
        assertEquals(1f, morphCardAlpha(-1f), 0.001f)
        assertEquals(0f, morphCardAlpha(1.5f), 0.001f)
        assertEquals(0f, morphCardAlpha(1.00493f), 0.001f)
    }

    @Test
    fun morphBackgroundAlpha_rampsInOverFirstThirtyFivePercent() {
        // Surface background: transparent at the card, opaque once
        // grown — no solid rect ever replaces the card in one frame.
        assertEquals(0f, morphBackgroundAlpha(0f), 0.001f)
        assertEquals(0.5f, morphBackgroundAlpha(0.175f), 0.001f)
        assertEquals(1f, morphBackgroundAlpha(0.35f), 0.001f)
        assertEquals(1f, morphBackgroundAlpha(1f), 0.001f)
    }

    @Test
    fun morphBackgroundAlpha_clampsAndOvershoot() {
        assertEquals(0f, morphBackgroundAlpha(-1f), 0.001f)
        assertEquals(1f, morphBackgroundAlpha(1.5f), 0.001f)
        assertEquals(1f, morphBackgroundAlpha(1.00493f), 0.001f)
    }

    @Test
    fun popupTargetRect_floatingShelfMargins() {
        // 2026-10-29 pop-up round: the shelf sits 16dp in from each
        // side and 20dp above the overlay bottom — never full-span.
        // (1080x2400 overlay, 40px side margin, 50px bottom margin,
        // 1000x1200 panel.)
        val target = popupTargetRect(Offset(0f, 0f), IntSize(1080, 2400), IntSize(1000, 1200), 40f, 50f)
        assertEquals(Rect(40f, 1150f, 1040f, 2350f), target)
    }

    @Test
    fun popupTargetRect_respectsOverlayOrigin() {
        val target = popupTargetRect(Offset(10f, 20f), IntSize(1080, 2400), IntSize(1000, 600), 40f, 50f)
        assertEquals(Rect(50f, 1770f, 1050f, 2370f), target)
    }

    @Test
    fun popupTargetRect_oversizedPanelClampsTop() {
        // A panel taller than the available shelf space never extends
        // past the overlay top; it clips at the top edge instead.
        val target = popupTargetRect(Offset(0f, 0f), IntSize(1080, 2400), IntSize(1000, 3000), 40f, 50f)
        assertEquals(0f, target.top, 0.001f)
        assertEquals(2350f, target.bottom, 0.001f)
    }

    @Test
    fun morphPopupCorner_transitionsContinuously() {
        // 2026-10-29 pop-up round: the window's corner radius morphs
        // 28dp -> 32dp across the WHOLE animation — no jump, no snap.
        assertEquals(28.dp, morphPopupCorner(0f))
        assertEquals(30.dp, morphPopupCorner(0.5f))
        assertEquals(32.dp, morphPopupCorner(1f))
    }

    @Test
    fun morphPopupCorner_clampsAndOvershoot() {
        // Same crash-proofing as morphCornerTop: spring overshoot
        // (the observed 1.00493) lands exactly on the shelf radius,
        // never past it, never negative.
        assertEquals(28.dp, morphPopupCorner(-1f))
        assertEquals(32.dp, morphPopupCorner(1.5f))
        assertEquals(32.dp, morphPopupCorner(1.00493f))
    }

    @Test
    fun heroHeaderAlpha_visibleUntilFinalTwoPercent() {
        // The traveling header is fully visible through the morph
        // and hands off to the pop-up's own header only over
        // [0.98, 1.0] (2026-10-30: was [0.9, 1.0]) — traveler and
        // destination coincide at the handoff, so no duplicate
        // copies are ever visible mid-flight.
        assertEquals(1f, heroHeaderAlpha(0f), 0.001f)
        assertEquals(1f, heroHeaderAlpha(0.5f), 0.001f)
        assertEquals(1f, heroHeaderAlpha(0.9f), 0.001f)
        assertEquals(1f, heroHeaderAlpha(0.95f), 0.001f)
        assertEquals(1f, heroHeaderAlpha(0.98f), 0.001f)
        assertEquals(0.5f, heroHeaderAlpha(0.99f), 0.001f)
        assertEquals(0f, heroHeaderAlpha(1f), 0.001f)
    }

    @Test
    fun heroHeaderAlpha_clampsAndOvershoot() {
        assertEquals(1f, heroHeaderAlpha(-1f), 0.001f)
        assertEquals(0f, heroHeaderAlpha(1.5f), 0.001f)
        assertEquals(0f, heroHeaderAlpha(1.00493f), 0.001f)
    }

    @Test
    fun badgeRectInRow_leadingSquareVerticallyCentered() {
        assertEquals(Rect(0f, 8f, 48f, 56f), badgeRectInRow(Rect(0f, 0f, 100f, 64f), 48f))
        assertEquals(Rect(10f, 28f, 58f, 76f), badgeRectInRow(Rect(10f, 20f, 110f, 84f), 48f))
    }

    @Test
    fun badgeRectInRow_rowShorterThanBadgeFloorsAtRowTop() {
        assertEquals(Rect(0f, 0f, 48f, 48f), badgeRectInRow(Rect(0f, 0f, 100f, 40f), 48f))
    }

    // Round 4 exit flash: the card is hidden exactly while panel
    // content is visible (progress > 0.85) and fades in smoothly
    // over [0.85 -> 0] — no blank slot, no one-frame pop.
    @Test
    fun morphExitCardAlpha_endpoints() {
        assertEquals(0f, morphExitCardAlpha(1f), 0.001f)
        assertEquals(1f, morphExitCardAlpha(0f), 0.001f)
    }

    @Test
    fun morphExitCardAlpha_hiddenThroughContentWindow() {
        assertEquals(0f, morphExitCardAlpha(0.95f), 0.001f)
        assertEquals(0f, morphExitCardAlpha(0.85f), 0.001f)
    }

    @Test
    fun morphExitCardAlpha_risesMonotonically() {
        val mid = morphExitCardAlpha(0.5f)
        assertTrue(mid > 0f && mid < 1f)
        assertTrue(morphExitCardAlpha(0.25f) > mid)
    }

    @Test
    fun sourceBadgeAlphaOnClose_isTravelerInverseAndLandsVisible() {
        // At rest (progress 1) the badge value is 1 (moot: the card
        // is covered by the opaque panel) — the curve matters over
        // the container fade window, where it must mirror the
        // traveler's fade exactly (badge + traveler sum to 1).
        for (p in listOf(0.25f, 0.125f, 0.05f)) {
            val badge = morphSourceBadgeAlphaOnClose(p)
            val traveler = heroHeaderAlpha(p) * morphPanelContainerAlphaClose(p)
            assertEquals(1f, badge + traveler, 0.001f)
        }
        // At landing the badge is fully visible BEFORE any state
        // clear — the exit-tail gap is closed by construction.
        assertEquals(1f, morphSourceBadgeAlphaOnClose(0f), 0.001f)
        // Mid-close the badge is hidden while the traveler flies.
        assertEquals(0f, morphSourceBadgeAlphaOnClose(0.9f), 0.001f)
    }
}
