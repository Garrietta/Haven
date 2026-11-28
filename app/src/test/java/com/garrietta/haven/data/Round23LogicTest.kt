package com.garrietta.haven.data

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.components.morphCloseContentAlpha
import com.garrietta.haven.ui.components.morphPopupRadii
import com.garrietta.haven.ui.components.morphPopupShape
import com.garrietta.haven.ui.components.morphTileAlpha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feedback round 23: the exit flash (item 1) and the shape that
 * changes on every tap/exit (item 2). All pure functions — no
 * Android runtime. The device checks (no solid-yellow frame on
 * close, tile corners pixel-identical across open/close cycles)
 * are covered by the compile + device checks.
 *
 * Round 24, item 3 SUPERSEDES the item-1 fade: the tile-alpha
 * tests below now pin the binary handoff (invisible above
 * progress 0.01, visible at or below it), not the round-23
 * exit fade. The morphPopupRadii tests are unchanged.
 */
class Round23LogicTest {

    // Round 24, item 3 — SUPERSEDES the fade below: the round-23
    // exit fade did NOT close the flash on device. The tile alpha
    // reads the animatable while the surfaces render the
    // finger-tracked drag progress, so a drag-release snap jumps
    // the tile 0 -> morphExitCardAlpha(dragProgress) in one frame.
    // The handoff is now binary on BOTH directions: invisible
    // above progress 0.01, visible at or below it — the container
    // (morphSwapContainerAlpha) is the single visible card until
    // landing. The old fade assertions are kept as history in
    // the comment, not as tests.
    @Test
    fun tileAlpha_binaryOnOpen() {
        // Visible only at landing; invisible for the whole growth.
        assertEquals(1f, morphTileAlpha(0f, closing = false), 0.001f)
        assertEquals(1f, morphTileAlpha(0.01f, closing = false), 0.001f)
        assertEquals(0f, morphTileAlpha(0.011f, closing = false), 0.001f)
        assertEquals(0f, morphTileAlpha(0.4f, closing = false), 0.001f)
        assertEquals(0f, morphTileAlpha(1f, closing = false), 0.001f)
    }

    @Test
    fun tileAlpha_binaryOnClose() {
        // The drag-snap case that round 23 missed: at the released
        // drag progress (0.3, 0.1) the tile must STAY invisible —
        // the old exit fade put it at 0.65 / 0.96 there, the pop.
        assertEquals(0f, morphTileAlpha(1f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.85f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.3f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.1f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.011f, closing = true), 0.001f)
        // Fully visible at landing, before any dismiss clears state.
        assertEquals(1f, morphTileAlpha(0.01f, closing = true), 0.001f)
        assertEquals(1f, morphTileAlpha(0f, closing = true), 0.001f)
    }

    @Test
    fun tileAlpha_directionIndependent() {
        // The binary rule is the same on enter and exit — no
        // direction can disagree with the container.
        for (p in listOf(0f, 0.01f, 0.05f, 0.5f, 1f)) {
            assertEquals(morphTileAlpha(p, closing = false), morphTileAlpha(p, closing = true), 0.001f)
        }
    }

    @Test
    fun tileAlpha_closeHandoffIsExact() {
        // At p = 0.85 the panel content is exactly gone and the
        // tile starts exactly then: no frame has both (no
        // doubling), no frame has neither (no blank slot).
        assertEquals(0f, morphCloseContentAlpha(0.85f), 0.001f)
        assertEquals(0f, morphTileAlpha(0.85f, closing = true), 0.001f)
        // Above 0.85 the content is visible and the tile is hidden.
        assertTrue(morphCloseContentAlpha(0.9f) > 0f)
        assertEquals(0f, morphTileAlpha(0.9f, closing = true), 0.001f)
        // Below 0.85 the content is gone and the tile stays hidden
        // until the 0.01 landing (round 24: the container, not the
        // tile, is the visible card through the return).
        assertEquals(0f, morphCloseContentAlpha(0.5f), 0.001f)
        assertEquals(0f, morphTileAlpha(0.5f, closing = true), 0.001f)
    }

    @Test
    fun tileAlpha_closeHasNoDeadZone() {
        // The old bug: at p = 0.6 on close, morphCardAlpha is 0
        // (tile invisible) while the content is already gone —
        // a bare solid-color surface for 45% of the close. Round
        // 24: the tile stays invisible there BY DESIGN (the
        // opaque container is the visible card), so the assertion
        // is now that tile + container never leave a gap: the
        // container is opaque wherever the tile is hidden.
        assertEquals(0f, morphTileAlpha(0.6f, closing = true), 0.001f)
        assertEquals(0f, morphTileAlpha(0.7f, closing = true), 0.001f)
    }

    // Item 2: the centered popup's clip divides out the layer
    // scale, so the VISIBLE corners follow the 28dp -> tile ramp
    // exactly instead of the landing corners times the scale.
    @Test
    fun popupRadii_scaleOneMatchesCornerLerp() {
        val landing = RoundedCornerShape(8.dp, 28.dp, 36.dp, 12.dp)
        val atTile = morphPopupRadii(0f, landing, density = Density(1f))
        assertEquals(8f, atTile.topStart.x, 0.001f)
        assertEquals(28f, atTile.topEnd.x, 0.001f)
        assertEquals(36f, atTile.bottomEnd.x, 0.001f)
        assertEquals(12f, atTile.bottomStart.x, 0.001f)
        val atRest = morphPopupRadii(1f, landing, density = Density(1f))
        assertEquals(28f, atRest.topStart.x, 0.001f)
        assertEquals(28f, atRest.topEnd.x, 0.001f)
        val mid = morphPopupRadii(0.5f, landing, density = Density(1f))
        assertEquals(18f, mid.topStart.x, 0.001f)
        assertEquals(32f, mid.bottomEnd.x, 0.001f)
    }

    @Test
    fun popupRadii_uniformScaleDividesOut() {
        // A lamp tile at scale 0.5: the local radii double, so the
        // visible radii (local x scale) land exactly on the tile.
        val landing = RoundedCornerShape(28.dp)
        val radii = morphPopupRadii(0f, landing, scaleX = 0.5f, scaleY = 0.5f, density = Density(1f))
        assertEquals(56f, radii.topStart.x, 0.001f)
        assertEquals(56f, radii.topStart.y, 0.001f)
        // Visible = local x scale: 56 x 0.5 = 28dp, the tile corner.
        assertEquals(28f, radii.topStart.x * 0.5f, 0.001f)
    }

    @Test
    fun popupRadii_wideTileSplitsAxes() {
        // The wide Climate Ecobee card: scaleX > 1, scaleY ~0.3.
        // Without the division the landing corners render
        // stretched in x and squashed in y; with it each axis
        // divides out its own scale.
        val landing = RoundedCornerShape(28.dp)
        val radii = morphPopupRadii(0f, landing, scaleX = 1.4f, scaleY = 0.3f, density = Density(1f))
        assertEquals(20f, radii.topStart.x, 0.001f)
        assertEquals(93.333f, radii.topStart.y, 0.01f)
        assertEquals(28f, radii.topStart.x * 1.4f, 0.001f)
        assertEquals(28f, radii.topStart.y * 0.3f, 0.01f)
    }

    @Test
    fun popupRadii_polygonAndNullKeepSoftCorners() {
        // Polygon/null landings keep the popup's soft 28dp, divided
        // by the scale like every other corner.
        val poly = morphPopupRadii(0f, RectangleShape, scaleX = 0.5f, scaleY = 0.5f, density = Density(1f))
        assertEquals(56f, poly.topStart.x, 0.001f)
        val none = morphPopupRadii(0f, null, scaleX = 0.5f, scaleY = 0.5f, density = Density(1f))
        assertEquals(56f, none.topStart.x, 0.001f)
    }

    @Test
    fun popupRadii_clampsAndGuardsScale() {
        val landing = RoundedCornerShape(8.dp, 28.dp, 36.dp, 12.dp)
        // Spring overshoot clamps: never past the tile, never past 1.
        val under = morphPopupRadii(-0.5f, landing, density = Density(1f))
        assertEquals(8f, under.topStart.x, 0.001f)
        val over = morphPopupRadii(1.5f, landing, density = Density(1f))
        assertEquals(28f, over.topStart.x, 0.001f)
        // A non-positive scale reads as 1 (no divide by zero, no
        // flipped radius).
        val zero = morphPopupRadii(0f, landing, scaleX = 0f, scaleY = -1f, density = Density(1f))
        assertEquals(8f, zero.topStart.x, 0.001f)
        assertEquals(28f, zero.topEnd.x, 0.001f)
    }

    @Test
    fun popupShape_scaledOverloadBuilds() {
        // The Outline itself is unmocked on the JVM (same rule as
        // morphSurfaceShape); this pins that the scale-aware
        // overload resolves and returns a shape.
        val landing = RoundedCornerShape(8.dp, 28.dp, 36.dp, 12.dp)
        assertNotNull(morphPopupShape(0.5f, landing, 0.5f, 0.5f))
    }
}
