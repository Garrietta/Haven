package com.garrietta.haven.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun tileMorphTransform_coversSourceAtZeroAndTargetAtOne() {
        // Feedback round 15, item 2: at p=0 the surface rect IS the
        // tile rect (translation + scale map target back onto the
        // tile); at p=1 it IS the placed rect (identity).
        val atZero = tileMorphTransform(from, to, 0f)
        assertEquals(100f, atZero.translationX, 0.001f)
        assertEquals(-400f, atZero.translationY, 0.001f)
        assertEquals(400f / 1080f, atZero.scaleX, 0.001f)
        assertEquals(200f / 1200f, atZero.scaleY, 0.001f)
        val atOne = tileMorphTransform(from, to, 1f)
        assertEquals(0f, atOne.translationX, 0.001f)
        assertEquals(0f, atOne.translationY, 0.001f)
        assertEquals(1f, atOne.scaleX, 0.001f)
        assertEquals(1f, atOne.scaleY, 0.001f)
        // Midpoint: rect halfway between tile and target.
        val mid = tileMorphTransform(from, to, 0.5f)
        assertEquals(50f, mid.translationX, 0.001f)
        assertEquals(-200f, mid.translationY, 0.001f)
        assertEquals(740f / 1080f, mid.scaleX, 0.001f)
        assertEquals(700f / 1200f, mid.scaleY, 0.001f)
    }

    @Test
    fun tileMorphTransform_clampsOvershoot() {
        // Spring overshoot past 0 or 1 must never push the surface
        // past the tile or past full size (never negative padding
        // territory): progress clamps to [0, 1].
        val below = tileMorphTransform(from, to, -0.5f)
        assertEquals(100f, below.translationX, 0.001f)
        val above = tileMorphTransform(from, to, 1.5f)
        assertEquals(0f, above.translationX, 0.001f)
        assertEquals(1f, above.scaleX, 0.001f)
    }

    @Test
    fun centeredPopupEnter_comesFromTileDirection() {
        // Tile left of center → offset points left at p=0, gone at p=1.
        val left = centeredPopupEnter(0f, Offset(100f, 500f), Offset(500f, 500f), 96f)
        assertEquals(-96f, left.offset.x, 0.001f)
        assertEquals(0f, left.offset.y, 0.001f)
        assertEquals(0.92f, left.scale, 0.001f)
        assertEquals(0f, left.alpha, 0.001f)
        // Tile below-right → diagonal offset toward it.
        val diag = centeredPopupEnter(0f, Offset(600f, 600f), Offset(500f, 500f), 96f)
        assertTrue(diag.offset.x > 0f && diag.offset.y > 0f)
        // Halfway: half distance, mid scale/alpha.
        val mid = centeredPopupEnter(0.5f, Offset(100f, 500f), Offset(500f, 500f), 96f)
        assertEquals(-48f, mid.offset.x, 0.001f)
        assertEquals(0.96f, mid.scale, 0.001f)
        assertEquals(0.5f, mid.alpha, 0.001f)
        // At rest: centered, full scale, opaque (component-wise:
        // Offset packs -0.0 distinctly from 0.0, so never compare
        // whole offsets after a multiply-by-zero).
        val rest = centeredPopupEnter(1f, Offset(100f, 500f), Offset(500f, 500f), 96f)
        assertEquals(0f, rest.offset.x, 0.001f)
        assertEquals(0f, rest.offset.y, 0.001f)
        assertEquals(1f, rest.scale, 0.001f)
        assertEquals(1f, rest.alpha, 0.001f)
        // Null source: fade + scale in place, no offset.
        val sourceless = centeredPopupEnter(0f, null, Offset(500f, 500f), 96f)
        assertEquals(0f, sourceless.offset.x, 0.001f)
        assertEquals(0f, sourceless.offset.y, 0.001f)
        // Spring overshoot clamps: never past the tile, never past 1.
        val over = centeredPopupEnter(1.5f, Offset(100f, 500f), Offset(500f, 500f), 96f)
        assertEquals(0f, over.offset.x, 0.001f)
        assertEquals(0f, over.offset.y, 0.001f)
        assertEquals(1f, over.scale, 0.001f)
        val under = centeredPopupEnter(-0.5f, Offset(100f, 500f), Offset(500f, 500f), 96f)
        assertEquals(-96f, under.offset.x, 0.001f)
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

    // Feedback round 11, item 2: the morph landing reads the
    // tile's own corners, never a hardcoded default.
    @Test
    fun roundedLandingCorners_readsRoundedShape() {
        // generalCardShape(0): three 28dp corners, 12dp bottom-right.
        assertEquals(CardCorners(28.dp, 28.dp, 12.dp, 28.dp), roundedLandingCorners(generalCardShape(0)))
        // Sharp 8dp all corners.
        assertEquals(CardCorners(8.dp, 8.dp, 8.dp, 8.dp), roundedLandingCorners(generalCardShape(4)))
        // Non-rounded outlines (and unknown shapes) read as null.
        assertEquals(null, roundedLandingCorners(androidx.compose.ui.graphics.RectangleShape))
        assertEquals(null, roundedLandingCorners(null))
    }

    @Test
    fun morphSurfaceCorners_landsOnTileCorners() {
        val landing = CardCorners(8.dp, 28.dp, 36.dp, 12.dp)
        // At the tile: exactly the tile's corners.
        assertEquals(landing, morphSurfaceCorners(0f, landing))
        // At full page: square, as before.
        assertEquals(CardCorners(0.dp, 0.dp, 0.dp, 0.dp), morphSurfaceCorners(1f, landing))
        // Mid-morph: corners scale with the rect lerp.
        assertEquals(CardCorners(4.dp, 14.dp, 18.dp, 6.dp), morphSurfaceCorners(0.5f, landing))
        // Null landing keeps the old 28dp rule.
        assertEquals(CardCorners(28.dp, 28.dp, 28.dp, 28.dp), morphSurfaceCorners(0f, null))
        assertEquals(CardCorners(0.dp, 0.dp, 0.dp, 0.dp), morphSurfaceCorners(1f, null))
        // Spring overshoot clamps: never past the tile, never past 1.
        assertEquals(landing, morphSurfaceCorners(-0.5f, landing))
        assertEquals(CardCorners(0.dp, 0.dp, 0.dp, 0.dp), morphSurfaceCorners(1.5f, landing))
    }

    @Test
    fun morphPopupCorners_lerpsPopupToTile() {
        val landing = CardCorners(8.dp, 28.dp, 36.dp, 12.dp)
        // At the tile: exactly the tile's corners.
        assertEquals(landing, morphPopupCorners(0f, landing))
        // At rest: the popup's own 28dp.
        assertEquals(CardCorners(28.dp, 28.dp, 28.dp, 28.dp), morphPopupCorners(1f, landing))
        // Mid-morph: halfway between popup and tile corners.
        assertEquals(CardCorners(18.dp, 28.dp, 32.dp, 20.dp), morphPopupCorners(0.5f, landing))
        // Null landing keeps 28dp throughout.
        assertEquals(CardCorners(28.dp, 28.dp, 28.dp, 28.dp), morphPopupCorners(0f, null))
    }

    @Test
    fun morphSurfaceShape_polygonLandingSwitchesInFadeWindow() {
        val poly = androidx.compose.ui.graphics.RectangleShape
        // In the fade window the shape IS the tile's outline.
        assertTrue(morphSurfaceShape(0.05f, poly) === poly)
        assertNull(morphSurfaceRadii(0.05f, poly, density = Density(1f)))
        // While opaque the outline stays soft and rounded — read
        // via the radii function (round 17: the shape divides
        // radii by the surface scale, so past the fade window it
        // is no longer a RoundedCornerShape, and Outline
        // construction itself is unmocked on the JVM).
        val mid = morphSurfaceRadii(0.5f, poly, density = Density(1f))!!
        assertEquals(14f, mid.topStart.x, 0.001f)
        assertEquals(14f, mid.topEnd.x, 0.001f)
        assertEquals(14f, mid.bottomEnd.x, 0.001f)
        assertEquals(14f, mid.bottomStart.x, 0.001f)
        // A corner landing resolves through the corner lerp.
        val tile = morphSurfaceRadii(0f, generalCardShape(4), density = Density(1f))!!
        assertEquals(8f, tile.topStart.x, 0.001f)
        assertEquals(8f, tile.topStart.y, 0.001f)
        // A popup over a polygon tile never goes sharp.
        assertEquals(CardCorners(28.dp, 28.dp, 28.dp, 28.dp), roundedLandingCorners(morphPopupShape(0f, poly)))
    }

    @Test
    fun morphSurfaceShape_scaleAwareCornersLandOnTileCorners() {
        // Feedback round 17, item 1: the local radii are divided
        // by the surface scale, so the VISIBLE corners — local
        // radius times scale — interpolate 0 -> landing exactly.
        // Mid-morph, 28dp landing, scaleX 0.5 / scaleY 0.25,
        // density 2: local rx 28dp (56px), local ry 56dp (112px);
        // visible rx 56px * 0.5 = 28px = 14dp, visible ry
        // 112px * 0.25 = 28px = 14dp — the 0.5-lerp corner.
        val scaled = morphSurfaceRadii(
            0.5f, androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            scaleX = 0.5f, scaleY = 0.25f, density = Density(2f),
        )!!
        assertEquals(56f, scaled.topStart.x, 0.001f)
        assertEquals(112f, scaled.topStart.y, 0.001f)
        assertEquals(28f, scaled.topStart.x * 0.5f, 0.001f)
        assertEquals(28f, scaled.topStart.y * 0.25f, 0.001f)
        // At rest the radii are zero at any scale.
        val rest = morphSurfaceRadii(
            1f, androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            scaleX = 0.5f, scaleY = 0.25f, density = Density(2f),
        )!!
        assertEquals(0f, rest.topStart.x, 0.001f)
        assertEquals(0f, rest.topStart.y, 0.001f)
        // Scale 1 is byte-identical to the old behavior.
        val unit = morphSurfaceRadii(
            0.5f, androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            density = Density(1f),
        )!!
        assertEquals(14f, unit.topStart.x, 0.001f)
        assertEquals(14f, unit.topStart.y, 0.001f)
    }

    // Feedback round 17, item 1: the traveler blend factor.

    @Test
    fun travelerBlend_startsOnSourceAndConvergesByPointThree() {
        assertEquals(0f, travelerBlend(0f), 0.001f)
        assertEquals(0.5f, travelerBlend(0.15f), 0.001f)
        assertEquals(1f, travelerBlend(0.3f), 0.001f)
        assertEquals(1f, travelerBlend(1f), 0.001f)
        assertEquals(0f, travelerBlend(-0.5f), 0.001f)
        assertEquals(1f, travelerBlend(1.5f), 0.001f)
    }

    @Test
    fun morphSourceBadge_tileReportsItselfRowReportsLeadingSquare() {
        // Near-square tile: the badge rect itself.
        val tile = Rect(100f, 800f, 300f, 1000f)
        assertEquals(tile, morphSourceBadge(tile, 56f))
        // Wide header row: the leading square, vertically centered.
        val row = Rect(100f, 800f, 900f, 900f)
        assertEquals(Rect(100f, 822f, 156f, 878f), morphSourceBadge(row, 56f))
    }

    @Test
    fun travelerLocalRect_mapsBackToSourceTargetAndSlot() {
        // Tile 400x200, panel 1080x1200; both badges 48x48.
        val source = Rect(100f, 800f, 500f, 1000f)
        val target = Rect(0f, 1200f, 1080f, 2400f)
        val sourceBadge = Rect(112f, 812f, 160f, 860f)
        val targetBadge = Rect(48f, 1248f, 96f, 1296f)
        // Surface-map a local rect back to window space, exactly
        // as MorphSurface's outer transform does: window =
        // rect.left + local * (rect.size / target.size).
        fun windowMapped(p: Float): Rect {
            val local = travelerLocalRect(sourceBadge, targetBadge, source, target, p)
            val rect = lerpRect(source, target, p)
            val sx = rect.width / target.width
            val sy = rect.height / target.height
            return Rect(
                rect.left + local.left * sx, rect.top + local.top * sy,
                rect.left + local.right * sx, rect.top + local.bottom * sy,
            )
        }
        // At p = 0 the traveler sits exactly on the source badge
        // (no frame-0 pop); at p = 1 exactly on the target slot.
        val atZero = windowMapped(0f)
        assertEquals(sourceBadge.left, atZero.left, 0.01f)
        assertEquals(sourceBadge.top, atZero.top, 0.01f)
        assertEquals(sourceBadge.right, atZero.right, 0.01f)
        assertEquals(sourceBadge.bottom, atZero.bottom, 0.01f)
        val atOne = windowMapped(1f)
        assertEquals(targetBadge.left, atOne.left, 0.01f)
        assertEquals(targetBadge.top, atOne.top, 0.01f)
        assertEquals(targetBadge.right, atOne.right, 0.01f)
        assertEquals(targetBadge.bottom, atOne.bottom, 0.01f)
        // Mid-flight the blend has converged (k = 1 past 0.3),
        // so the window rect is the surface's header-slot path
        // with the LINEAR badge size — never the linear path
        // (80, 1030) and never squashed by the surface scale.
        val mid = windowMapped(0.5f)
        val rect = lerpRect(source, target, 0.5f)
        val slotLeft = rect.left + (targetBadge.left - target.left) * (rect.width / target.width)
        val slotTop = rect.top + (targetBadge.top - target.top) * (rect.height / target.height)
        assertEquals(slotLeft, mid.left, 0.01f)
        assertEquals(slotTop, mid.top, 0.01f)
        assertEquals(48f, mid.width, 0.01f)
        assertEquals(48f, mid.height, 0.01f)
    }

    // Feedback round 2, item 6: popup side-anchoring buckets the
    // source tile's center into overlay thirds.
    @Test
    fun popupSideAnchor_bucketsByThirds() {
        // Overlay 0..900: left third -> START, right third -> END.
        assertEquals(PopupSideAnchor.START, popupSideAnchor(100f, 0f, 900f))
        assertEquals(PopupSideAnchor.START, popupSideAnchor(299f, 0f, 900f))
        assertEquals(PopupSideAnchor.CENTER, popupSideAnchor(300f, 0f, 900f))
        assertEquals(PopupSideAnchor.CENTER, popupSideAnchor(450f, 0f, 900f))
        assertEquals(PopupSideAnchor.CENTER, popupSideAnchor(600f, 0f, 900f))
        assertEquals(PopupSideAnchor.END, popupSideAnchor(601f, 0f, 900f))
        assertEquals(PopupSideAnchor.END, popupSideAnchor(850f, 0f, 900f))
        // Non-zero overlay origin shifts the thirds with it.
        assertEquals(PopupSideAnchor.START, popupSideAnchor(150f, 100f, 900f))
        assertEquals(PopupSideAnchor.END, popupSideAnchor(950f, 100f, 900f))
        // No reported rect (or a degenerate overlay) -> CENTER.
        assertEquals(PopupSideAnchor.CENTER, popupSideAnchor(null, 0f, 900f))
        assertEquals(PopupSideAnchor.CENTER, popupSideAnchor(100f, 0f, 0f))
    }
}
