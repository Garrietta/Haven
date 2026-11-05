package com.garrietta.haven.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment

/** Centered target rect for the thermostat pop-out: the measured panel
 * size, centered in the overlay on both axes, clamped inside the
 * overlay bounds so an oversized panel never extends past the screen.
 * Pure, unit-tested. */
fun centerTargetRect(overlayOrigin: Offset, overlaySize: IntSize, panelSize: IntSize): Rect {
    val left = overlayOrigin.x + ((overlaySize.width - panelSize.width) / 2f).coerceAtLeast(0f)
    val top = overlayOrigin.y + ((overlaySize.height - panelSize.height) / 2f).coerceAtLeast(0f)
    val right = minOf(left + panelSize.width, overlayOrigin.x + overlaySize.width)
    val bottom = minOf(top + panelSize.height, overlayOrigin.y + overlaySize.height)
    return Rect(left, top, right, bottom)
}

/**
 * Explicit single-surface morph machinery (2026-10-29 architectural
 * overhaul). REPLACES the framework sharedBounds morph from #41/#49.
 *
 * Why sharedBounds was replaced: during a sharedBounds transition the
 * framework keeps BOTH endpoints' content visible (it crossfades between
 * the source tile and the destination panel/page). When the two contents
 * share large identical subtrees — the weather card's arc on Home and
 * the weather sheet's arc, the thermostat tile and its page — the
 * crossfade reads as doubled, ghosted content (2026-10-29 verdict video:
 * weather sheet ~0s/~2s, thermostat detail ~4s/~6s, sensor detail ~8s,
 * garage sheet ~11s/~15s, lamp detail ~17s/~26s). Tuning timing cannot
 * fix a mechanism that renders both sides at once.
 *
 * The replacement mechanism, per the overhaul spec:
 * 1. ROOT COORDINATES: every morph source reports its boundsInRoot into
 *    [LocalCardBounds]. The morph surface interpolates ONE rect from
 *    that captured rect to its target rect, in root coordinates, with
 *    the overlay's own root origin subtracted — so the path is a
 *    straight line from the card to its target and exactly reversed on
 *    dismiss. The exit path is anchored to the SAME rect captured at
 *    open (never re-resolved at dismiss time), which is what kills the
 *    diagonal fly-outs (the garage sheet dismissing toward the top-left
 *    corner): the old exit morph re-resolved its target through the
 *    framework at dismiss time instead of returning to the card.
 * 2. SINGLE SURFACE, OPAQUE COVER (container transform, Round 5 —
 *    supersedes the 2026-10-29 source-fading rule AND Round 4's
 *    exit curve): while a morph is active, the source card stays
 *    rendered at full alpha (the overlay root provides
 *    [LocalMorphCardAlpha] = 1f; the [morphCardAlpha] /
 *    [morphExitCardAlpha] curves below are retained as pure
 *    functions but no longer drive the card), and the panel's
 *    opaque surface covers it — background alpha 1 from frame 0
 *    on enter, through p >= 0.20 on close, fading only over
 *    [0.20, 0] via [morphCloseBackgroundAlpha] — while its
 *    content fades in late via [morphEnterContentAlpha] on enter
 *    and out early via [morphCloseContentAlpha] on close. Start
 *    content fades out early, end content fades in late,
 *    symmetric on exit: the card and the surface read as ONE
 *    element transforming, never a blank solid panel flashing
 *    in at open, never both contents at full opacity at once (that
 *    was #49's ghosting), never a staged reappearance, never a snap
 *    back. Exit is the smooth reverse of enter on the same bounds
 *    spring — the #62 synchronous removal read on device as "a sudden
 *    smack back to the previous state" and is gone.
 *    Round 21, item 8 SUPERSEDES the full-alpha half of this
 *    rule: the source card now fades via [morphCardAlpha] (gone
 *    by progress 0.4, back over the last 40% of exit), so the
 *    tile BECOMES the popup and leaves a blank slot in the grid;
 *    the surface-cover half is unchanged.
 * 3. NO SCALING (Round 6 change — see [MorphSurface]): content is
 *    laid out once at its target size and position, and the surface
 *    now SCALES via graphicsLayer (translation + scale about the
 *    top-left origin) instead of being clipped to the growing rect.
 *    The measurement frame is GONE: the live panel's own header
 *    reports the travel target from its untransformed layout bounds
 *    (PanelOverlay provides LocalHeroTarget/LocalArcTarget around
 *    the live content), so no panel is ever composed twice.
 *    Content still fades in late per the reconciled curves, so no
 *    full-size content ever appears at full opacity inside a
 *    still-small rect.
 * 4. SHEET GEOMETRY UNCHANGED (stated deviation from the spec sample):
 *    the sample's centered floating card (16dp/48dp/20dp margins,
 *    24→32dp corners) is NOT adopted — Haven's sheets stay
 *    bottom-anchored panels and detail pages stay full pages, as
 *    approved in earlier rounds. Only the mechanism is adopted.
 */

/** Morph-source bounds by entity id, in root coordinates. Written by cards, read at morph open. */
val LocalCardBounds = staticCompositionLocalOf<MutableMap<String, Rect>> { mutableMapOf() }

/**
 * Feedback round 11, item 2: morph-source OUTLINE shapes by entity
 * id. Written by cards alongside [LocalCardBounds] (every site that
 * reports bounds reports the exact outline it renders — override,
 * global, or index-cycled default), read by the morphing surface so
 * the exit morph lands on the tile's own corners instead of a
 * hardcoded 28dp. A missing entry (a source that never reported)
 * falls back to the store resolution at the morph call site, then
 * to the old 28dp behavior — never a crash, never a guess.
 */
val LocalCardShapes = staticCompositionLocalOf<MutableMap<String, Shape>> { mutableMapOf() }

/**
 * Feedback round 16, item 4a: morph-source CONTAINER colors by
 * entity id. Written by cards alongside [LocalCardBounds] (every
 * site that reports bounds reports the exact container color it
 * renders — tile tint, preset, or default), read at morph open so
 * the morphing surface starts at the tile's own color instead of
 * the panel color. The folded surface already lerped from a
 * source color over the first 35% of the morph, but every call
 * site passed the same constant (surfaceContainerHighest) — for a
 * yellow lamp tile or a purple media tile the exit's final frames
 * showed a pale rect over the hidden colored tile: the white
 * flash. The unfolded popup had no lerp at all (full panel cream
 * from frame 0). With the tile's true color captured at open,
 * both surfaces land on the tile with its color.
 */
val LocalCardColors = staticCompositionLocalOf<MutableMap<String, Color>> { mutableMapOf() }

/** Entity id of the card currently hidden as a morph source, or null. */
val LocalHiddenCardId = staticCompositionLocalOf<String?> { null }

/**
 * Header bounds by entity id, in root coordinates: the card header row
 * (EntityControlCard) or the tile badge (HomeRoomCard). Written by cards,
 * read by the traveling header at morph time. Separate from
 * [LocalCardBounds]: the traveling header needs the header's own rect,
 * not the whole card's.
 */
val LocalHeroBounds = staticCompositionLocalOf<MutableMap<String, Rect>> { mutableMapOf() }

/** Entity id whose card header is currently hidden because its traveling header is on screen, or null. */
val LocalHiddenHeroId = staticCompositionLocalOf<String?> { null }

/**
 * Reporter for a pop-up's own header target rect, in root coordinates.
 * Provided ONLY around a morph's measurement frame (never around the
 * morph surface content): the measurement frame sits at the pop-up's
 * final geometry, so its header rect IS the travel target, while the
 * surface content's header sits at the interpolated rect mid-morph
 * and must never overwrite the target. Null outside measurement
 * frames: reporting sites no-op there.
 */
val LocalHeroTarget = staticCompositionLocalOf<((Rect) -> Unit)?> { null }

/**
 * Weather dial-panel bounds by entity id, in root coordinates: the
 * condition-tinted box around WeatherControl's Dial. Written by every
 * WeatherControl, read by the traveling arc at morph time. Separate
 * from [LocalCardBounds]: the traveling arc needs the panel's own
 * rect, not the whole card's.
 */
val LocalArcBounds = staticCompositionLocalOf<MutableMap<String, Rect>> { mutableMapOf() }

/**
 * Reporter for a pop-up's own dial-panel target rect, in root
 * coordinates. Provided ONLY around a morph's measurement frame,
 * mirroring [LocalHeroTarget]: the measurement frame's panel rect IS
 * the arc travel target.
 */
val LocalArcTarget = staticCompositionLocalOf<((Rect) -> Unit)?> { null }

/**
 * The active morph surface's progress READER, when the content is
 * rendered inside one. Null everywhere else (cards, ordinary pages)
 * — destination content that must dissolve in sync with a traveling
 * element reads this instead of assuming a morph is running.
 * Provided by [MorphSurface] from its [progress] parameter.
 *
 * Round 6: this is a lambda, not a Float, on purpose. Callers must
 * invoke it ONLY inside a graphicsLayer or draw block, never during
 * composition: reading the Animatable's value during composition
 * recomposes the whole panel subtree every frame, which is exactly
 * the per-frame Measure/Layout cost the 120Hz pipeline removes.
 * Inside graphicsLayer/draw blocks the read invalidates only the
 * layer, and the morph runs on the RenderThread.
 */
val LocalMorphProgress = staticCompositionLocalOf<(() -> Float)?> { null }

/**
 * Visual-fidelity round §5: whether a morph is currently CLOSING
 * (an animated dismiss is running). The overlay root provides this
 * from its dismiss flags; panels read it through their own
 * isClosing parameters, while source cards — which have no
 * isClosing parameter — read it here to pick the close branch of
 * [hideHeroWhenMorphSource]. Defaults to false (enter / rest).
 */
val LocalMorphClosing = staticCompositionLocalOf { false }

/**
 * Feedback round 18, item 5: the drag handle + range for popup
 * header drag-to-dismiss. CenteredPopup and PanelOverlay provide
 * these; popup header rows (DetailPopupHeader, the sheet header,
 * the thermostat popout header) attach [popupDragDetector] to
 * them so dragging down on a header scrubs the morph with the
 * finger. Null handle = no drag (plain detail pages, which sit
 * outside any popup). The range is a lambda because
 * CenteredPopup's range depends on the placed rect, which only
 * exists after first placement.
 */
val LocalPopupDragHandle = staticCompositionLocalOf<PopupDragHandle?> { null }
val LocalPopupDragRangePx = staticCompositionLocalOf<() -> Float> { { 0f } }

/**
 * Feedback round 4, item 4: whether the traveling header is
 * suppressed for this detail — true when the detail opened from
 * a sub-panel (Lights grid, Updates, vacuum/media/security/
 * greenhouse panels) or another detail, i.e. when the source
 * card lives in an overlay that is replaced by this detail,
 * not on the root dashboard. Round 18, item 4 retired the
 * traveler: header badges now always render in place and fade
 * with the content curves (see morphHeaderBadgeAlpha), so this
 * flag is retained dormant (providers still set it; no badge
 * reads it anymore). Defaults to false.
 */
val LocalSuppressTraveler = staticCompositionLocalOf { false }

/**
 * Alpha for the active morph source card (see
 * [hideWhenMorphSource]). Round 5: the overlay root provided 1f
 * always — the card never faded; the opaque panel surface covered
 * it. Round 21, item 8 (SUPERSEDES that rule): the root provides
 * a lambda reading the active morph's progress through
 * [morphCardAlpha], so the tile fades out by progress 0.4 on
 * enter and back in over the last 40% of exit. A lambda, not a
 * Float, so cards read it inside graphicsLayer with no per-frame
 * recomposition — and MorphSurface does NOT shadow this local
 * (it shadows only [LocalMorphProgress]), so panel-internal
 * cards read the ROOT progress for a stacked panel detail too,
 * instead of the underlying panel's rest progress.
 */
val LocalMorphCardAlpha = staticCompositionLocalOf<() -> Float> { { 1f } }

/**
 * Reports this layout's boundsInRoot into [LocalCardBounds] under [id].
 * Writes only when the rect actually changed, so the post-layout
 * callback cannot schedule a recomposition loop.
 */
/**
 * Feedback round 18, item 7: whether a newly measured [Rect]
 * differs from the stored one by more than 1f pixel on any edge.
 * A null stored rect (first capture) always counts as different,
 * so the first capture always lands; sub-pixel layout jitter
 * afterwards never schedules a recomposition. Pure, unit-tested.
 */
fun rectDiffersByMoreThan1px(stored: Rect?, rect: Rect): Boolean =
    stored == null ||
        kotlin.math.abs(stored.left - rect.left) > 1f ||
        kotlin.math.abs(stored.top - rect.top) > 1f ||
        kotlin.math.abs(stored.right - rect.right) > 1f ||
        kotlin.math.abs(stored.bottom - rect.bottom) > 1f

fun Modifier.reportCardBounds(id: String): Modifier = composed {
    val bounds = LocalCardBounds.current
    onGloballyPositioned { coords: LayoutCoordinates ->
        val rect = coords.boundsInRoot()
        if (rectDiffersByMoreThan1px(bounds[id], rect)) bounds[id] = rect
    }
}

/**
 * Feedback round 11, item 2: reports this layout's outline shape
 * into [LocalCardShapes] under [id]. Call sites pair this with
 * [reportCardBounds] and pass the exact shape the layout renders,
 * so the morphing surface can land on the tile's own corners.
 * Writes only when the shape actually changed (same rule as bounds,
 * so the write cannot schedule a recomposition loop — Shape
 * equality here is the outline's own equals).
 */
fun Modifier.reportCardShape(id: String, shape: Shape): Modifier = composed {
    val shapes = LocalCardShapes.current
    onGloballyPositioned {
        if (shapes[id] != shape) shapes[id] = shape
    }
}

/**
 * Feedback round 16, item 4a: reports this layout's container
 * color into [LocalCardColors] under [id]. Same write-once-changed
 * rule as bounds/shapes. Call sites pair this with
 * [reportCardBounds] and pass the exact color the layout renders.
 */
fun Modifier.reportCardColor(id: String, color: Color): Modifier = composed {
    val colors = LocalCardColors.current
    onGloballyPositioned {
        if (colors[id] != color) colors[id] = color
    }
}

/**
 * Reports this layout's header boundsInRoot into [LocalHeroBounds]
 * under [id] (see [reportCardBounds] for the write-once-changed rule).
 * EntityControlCard reports its header row; HomeRoomCard reports its
 * badge (the tile's name/status sit below the badge, so the badge
 * rect anchors the traveling header's position).
 */
fun Modifier.reportHeroBounds(id: String): Modifier = composed {
    val bounds = LocalHeroBounds.current
    onGloballyPositioned { coords: LayoutCoordinates ->
        val rect = coords.boundsInRoot()
        if (rectDiffersByMoreThan1px(bounds[id], rect)) bounds[id] = rect
    }
}

/**
 * Reports this layout's boundsInRoot for arc travel, split by
 * context exactly like the hero split: a measurement frame
 * (LocalArcTarget provided) reports ONLY the target; morph surface
 * content (LocalMorphProgress provided) reports NEITHER — its panel
 * sits at the interpolated rect mid-morph and must never overwrite
 * the card-side source; ordinary card content (neither provided)
 * writes the source map. Without this split, the surface's own
 * panel would drag the travel source along mid-morph and the exit
 * would land on the target rect instead of the card.
 */
fun Modifier.reportArcBounds(id: String): Modifier = composed {
    val bounds = LocalArcBounds.current
    val target = LocalArcTarget.current
    val inSurface = LocalMorphProgress.current != null
    onGloballyPositioned { coords: LayoutCoordinates ->
        val rect = coords.boundsInRoot()
        when {
            target != null -> target(rect)
            !inSurface && bounds[id] != rect -> bounds[id] = rect
        }
    }
}

/**
 * Dissolves this layout into its traveling counterpart: while inside
 * a morph surface, alpha follows 1 − [heroHeaderAlpha] of the
 * surface's progress (invisible while the traveling element carries
 * the content, materializing only in the final 2% handoff window
 * as the traveler arrives — the same curve as the traveling header).
 * Outside a morph surface, alpha is 1: cards and ordinary pages are
 * unaffected, and measurement frames (which sit outside the surface)
 * report at full alpha.
 */
fun Modifier.dissolveIntoTraveler(): Modifier = composed {
    val progress = LocalMorphProgress.current
    graphicsLayer {
        alpha = if (progress != null) 1f - heroHeaderAlpha(progress()) else 1f
    }
}

/**
 * Source-badge alpha on a morphing surface CLOSE at [progress]
 * (1 = panel at rest, 0 = landed back on the card) — the
 * visual-fidelity round §5 fix for the exit tail gap. The old rule
 * hid the source badge binarily for the whole morph: on close, the
 * traveling badge fades with the container over the final 25%
 * (see [morphPanelContainerAlphaClose]) while the source badge
 * stayed hidden until the dismiss coroutine cleared state frames
 * after progress reached 0 — a gap where no badge showed at all.
 * This curve is the traveler's exact inverse: while the panel
 * surface is opaque the badge value is moot (the card is covered),
 * and over the container's fade window the badge crossfades in
 * against the coincident traveler, reaching full opacity exactly
 * at landing — before any state clear — so no frame lacks a badge
 * and no doubling forms (the two coincide to sub-pixel accuracy).
 * Pure, unit-tested.
 */
fun morphSourceBadgeAlphaOnClose(progress: Float): Float =
    1f - heroHeaderAlpha(progress) * morphPanelContainerAlphaClose(progress)

/**
 * Hides this layout's header while it is the active morph source's
 * traveling header (layout unchanged, alpha only). Round 5: the
 * overlay root provides a null hidden id, so this never hides in
 * practice — the opaque panel covers the stationary card header
 * (the interpolated panel rect always contains the source rect),
 * and the traveler is badge-only above the panel, so no doubling
 * forms. The mechanism is retained: if a hidden id is ever
 * provided again, the header hides from frame 0 exactly as before
 * (the doubling read in every 2026-10-29 verdict video).
 *
 * Visual-fidelity round §5: the badge variant below
 * ([hideHeroWhenMorphSource]) is closing-aware — on enter it
 * still hides binarily (the traveler owns the badge for the whole
 * flight), but on close it crossfades back in via
 * [morphSourceBadgeAlphaOnClose] instead of staying hidden until
 * state clears. This header variant is badge-text-agnostic and
 * keeps the binary rule: headers (unlike badges) are never the
 * traveling element, so no exit-tail gap applies to them.
 */
fun Modifier.hideHeroWhenMorphSource(id: String): Modifier = composed {
    val hiddenId = LocalHiddenHeroId.current
    // Visual-fidelity round §5: closing-aware. On enter the badge
    // hides binarily (the traveler owns the badge for the whole
    // flight — any earlier return would double it). On close the
    // badge crossfades back in via morphSourceBadgeAlphaOnClose
    // (the traveler's exact inverse), reaching full opacity at
    // landing — before the dismiss coroutine clears state — so
    // the exit tail never has a badge-less frame. Progress is read
    // ONLY inside graphicsLayer (the overlay root provides the
    // active morph's progress lambda); a null progress (no morph)
    // falls back to fully visible, which is also the rest state.
    val closing = LocalMorphClosing.current
    val progress = LocalMorphProgress.current
    graphicsLayer {
        alpha = when {
            id != hiddenId -> 1f
            !closing -> 0f
            else -> morphSourceBadgeAlphaOnClose(progress?.invoke() ?: 0f)
        }
    }
}

/**
 * Hides this layout's BODY while it is the active morph source
 * (layout unchanged, alpha only) — the 2026-10-29 verdict-fix round's
 * answer to the weather pop-up still doubling on #67. The header hide
 * above covers only the header row: the source card's arc, humidity
 * line, sensor chart, and lamp hero stayed visible while the pop-up's
 * duplicate content faded in over them, so two copies of the same
 * content shared the screen. Callers gate this on their morph-source
 * flag, exactly like [hideWhenMorphSource]: nested/destination cards
 * sharing the entity id must never hide.
 *
 * 2026-10-30 centered-dialog round: the hide is progress-driven
 * ([LocalMorphCardAlpha], same curve as [hideWhenMorphSource]),
 * NOT binary. The binary hide caused the exit flash the verdict
 * reports (grid flickering blank tiles on dismiss): at progress 0
 * the dialog is already invisible, but the body only returned when
 * the dismiss coroutine cleared state frames later — blank cards
 * with no dialog. Progress-driven, the body fades back over the
 * last 40% of exit and is fully visible at progress 0, before any
 * state clear — and on enter it is gone by progress 0.4, before
 * the dialog content fades in at 0.7, so no doubling returns.
 */
fun Modifier.hideBodyWhenMorphSource(id: String): Modifier = composed {
    val hiddenId = LocalHiddenCardId.current
    val cardAlpha = LocalMorphCardAlpha.current
    if (id == hiddenId) {
        // Feedback round 22, item 3: the progress read happens
        // HERE, during composition — not inside graphicsLayer.
        // The graphicsLayer-only read never re-evaluated on
        // device (#132: the source tile stayed fully visible
        // behind the open popup at rest), so the alpha now comes
        // from a composition-observed read: this card — and only
        // this card — recomposes per animation frame while it is
        // the hidden source, and the alpha follows morphCardAlpha
        // exactly (gone by progress 0.4, back over the last 40%
        // of exit, fully visible at progress 0 before any state
        // clears). Non-source cards never read the progress, so
        // they pay no per-frame recomposition cost.
        val alpha = cardAlpha()
        graphicsLayer { this.alpha = alpha }
    } else {
        graphicsLayer { alpha = 1f }
    }
}

/**
 * Renders this layout with the morph source's progress-driven alpha
 * while it is the active morph source (layout unchanged). The alpha
 * comes from [LocalMorphCardAlpha]: 1 at morph start (the card is
 * fully visible under the still-transparent surface), fading to 0
 * once the surface is opaque, and fading back in over the last 40%
 * of an animated exit — so the card never vanishes in a single frame
 * and never pops back in stages. Alpha, not removal: removing the
 * card would reflow the grid and invalidate the captured source
 * rect mid-morph.
 */
fun Modifier.hideWhenMorphSource(id: String): Modifier = composed {
    val hiddenId = LocalHiddenCardId.current
    val cardAlpha = LocalMorphCardAlpha.current
    if (id == hiddenId) {
        // Feedback round 22, item 3: the progress read happens
        // HERE, during composition — not inside graphicsLayer.
        // The graphicsLayer-only read never re-evaluated on
        // device (#132: the source tile stayed fully visible
        // behind the open popup at rest), so the alpha now comes
        // from a composition-observed read: this card — and only
        // this card — recomposes per animation frame while it is
        // the hidden source, and the alpha follows morphCardAlpha
        // exactly (gone by progress 0.4, back over the last 40%
        // of exit, fully visible at progress 0 before any state
        // clears). Non-source cards never read the progress, so
        // they pay no per-frame recomposition cost.
        val alpha = cardAlpha()
        graphicsLayer { this.alpha = alpha }
    } else {
        graphicsLayer { alpha = 1f }
    }
}

/**
 * Smoothstep interpolation between [edge0] and [edge1], clamped to
 * [0, 1] — the single shared easing for every morph curve below
 * (see the spring-clamp skill: one shared pure clamping function,
 * never ad-hoc clamps per call site). Progress is clamped before
 * computing, so spring overshoot can never push any curve out of
 * range.
 */
fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x.coerceIn(0f, 1f) - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Linear interpolation between two rects; [t] clamped to [0, 1]. */
fun lerpRect(from: Rect, to: Rect, t: Float): Rect {
    val c = t.coerceIn(0f, 1f)
    return Rect(
        left = from.left + (to.left - from.left) * c,
        top = from.top + (to.top - from.top) * c,
        right = from.right + (to.right - from.right) * c,
        bottom = from.bottom + (to.bottom - from.bottom) * c,
    )
}

/**
 * Feedback round 7: per-surface drag state for the Koda now-playing
 * morph port. Plain snapshot state on the finger — NOT an Animatable
 * scrubbed mid-gesture (Koda's MutatorMutex cancels per-event snapTo
 * calls, which is why Koda's video minimize keeps dragProgress as a
 * plain float). One instance per popup surface, hoisted in HavenApp
 * scope near the progress animatables so dismiss functions can read
 * it. [pulling] is the nested-scroll latch: it lives here, not in the
 * connection object, so recomposition creating a fresh connection
 * cannot lose a mid-gesture latch. [settling] marks a dismiss that was
 * initiated from a drag, so the surface keeps the OPEN fade curves
 * through the settle (the popup content rides down visibly on the
 * settle spring, instead of vanishing on the close curve).
 * Feedback round 11, item 1: [dragProgress] is gesture-travel
 * bookkeeping ONLY (the release rule compares it against
 * [dragStartProgress]). It is NEVER rendered: the popup stays fully
 * open under the finger, and only the release commits the spring
 * down into the tile. Round 7's finger-following scrub is deleted
 * — the user corrected that Koda does not do that, and neither
 * does Haven.
 */
class PopupDragState {
    var isDragging by mutableStateOf(false)
    var dragProgress by mutableFloatStateOf(1f)
    var dragStartProgress by mutableFloatStateOf(1f)
    var pulling by mutableStateOf(false)
    var settling by mutableStateOf(false)

    fun reset() {
        isDragging = false
        dragProgress = 1f
        dragStartProgress = 1f
        pulling = false
        settling = false
    }
}

/**
 * Feedback round 7: the drag handle a popup wrapper receives. The
 * wrapper computes its own drag range (it owns the placed rect) and
 * passes it per event; the controller curries everything else
 * (animatable, exiting gate, snapshot, dismiss). Pure carrier.
 */
class PopupDragHandle(
    val state: PopupDragState,
    val onDelta: (dy: Float, rangePx: Float) -> Unit,
    val onRelease: (velocityY: Float, rangePx: Float) -> Unit,
)

/**
 * Feedback round 7: Koda's video-minimize release rule, pure.
 * Velocity momentum first (±120 px/s: downward drift commits, upward
 * drift always restores), else travel: the finger must have pulled
 * more than [commitTravelPx] (40dp in px) of the drag range. Progress
 * units: [dragStartProgress] minus [releasedProgress] is the fraction
 * of the range travelled, so the travel threshold in progress units
 * is commitTravelPx / range.
 */
fun shouldDismissDrag(
    velocityY: Float,
    dragStartProgress: Float,
    releasedProgress: Float,
    dragRangePx: Float,
    commitTravelPx: Float,
): Boolean {
    if (velocityY > 120f) return true
    if (velocityY < -120f) return false
    return (dragStartProgress - releasedProgress) >
        (commitTravelPx / dragRangePx.coerceAtLeast(1f))
}

/**
 * Feedback round 7: Koda's finger formula, pure. Dragging down (positive
 * dy) decreases progress; the denominator is the exact top-edge travel
 * of the container. Feedback round 11, item 1: the result feeds ONLY
 * the release rule ([shouldDismissDrag] travel) — it is never
 * rendered, so the popup does not track the finger. Floor 0 — Haven
 * has no curtain path, so no 0.25 floor.
 */
fun dragProgressAfterDelta(current: Float, dy: Float, dragRangePx: Float): Float =
    (current - dy / dragRangePx.coerceAtLeast(1f)).coerceIn(0f, 1f)

/**
 * Feedback round 7: drag range for a rect-morphing PanelOverlay — the
 * exact travel of the container's top edge from its target position to
 * the source tile's top. Falls back to the target height when the
 * source is null or the range is degenerate (tile above target, which
 * cannot happen for a below-screen panel but is guarded anyway).
 * Round 24, item 3: [minDragRangePx] floors the range — a tile
 * whose top sits near the target top would otherwise scrub the
 * whole morph in a few pixels of finger travel. Call sites pass
 * 380.dp in px. Pure, unit-tested.
 */
fun dragRangeForPanelOverlay(
    sourceTop: Float?,
    targetTop: Float,
    targetHeight: Float,
    minDragRangePx: Float = 0f,
): Float {
    val range = if (sourceTop != null) sourceTop - targetTop else targetHeight
    val ranged = if (range > 0f) range else targetHeight.coerceAtLeast(1f)
    return maxOf(ranged, minDragRangePx).coerceAtLeast(1f)
}

/**
 * Feedback round 7: drag range for a CenteredPopup — the vertical
 * travel of the surface center from its placed position to the tile
 * center. Falls back to the placed height when either center is
 * unknown or the range is degenerate (tile centered on the popup).
 * Round 24, item 3: [minDragRangePx] floors the range — a
 * vertically-centered tile (Standing Lamp) has a near-zero center
 * distance, so without the floor a small finger movement scrubbed
 * the whole morph 3–4x too fast ("very fast, incredibly rapid").
 * Call sites pass 380.dp in px. Pure, unit-tested.
 */
fun dragRangeForCenteredPopup(
    placedCenterY: Float?,
    tileCenterY: Float?,
    placedHeight: Float,
    minDragRangePx: Float = 0f,
): Float {
    val range = if (placedCenterY != null && tileCenterY != null) {
        kotlin.math.abs(placedCenterY - tileCenterY)
    } else {
        placedHeight
    }
    return maxOf(range.coerceAtLeast(1f), minDragRangePx)
}

/**
 * Feedback round 7: Koda's pullToMinimize as a NestedScrollConnection,
 * attached to the popup scroll container (never the outer box — Koda's
 * comments-panel lesson: a nested scrollable inside the popup must
 * keep its own dismiss). Protocol order, not pointer racing, decides:
 * parent pre-scroll runs before the child consumes, so no
 * consume-order proof is needed.
 * - onPreScroll: once [PopupDragState.pulling] latched, keeps the
 *   gesture to the end and feeds every delta to the drag, consuming
 *   all of it, UserInput only.
 * - onPostScroll: leftover downward drag (available.y > 0 — the scroll
 *   container is already at top, or the content is shorter than the
 *   viewport so nothing scrolls) latches pulling, feeds the drag,
 *   consumes all.
 * - onPostFling: only if this connection started the pull, unlatches
 *   and releases with the fling velocity, consuming it. A fling that
 *   merely runs out of list at the top is left alone.
 * The controller's onDelta/onRelease gate on reduce-motion, exiting,
 * and at-rest internally; this connection only routes.
 */
class PopupDismissConnection(
    private val drag: PopupDragState,
    private val onDelta: (Float) -> Unit,
    private val onRelease: (Float) -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || !drag.pulling) return Offset.Zero
        onDelta(available.y)
        return available
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (available.y <= 0f) return Offset.Zero
        drag.pulling = true
        onDelta(available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!drag.pulling) return available
        drag.pulling = false
        onRelease(available.y)
        return available
    }
}

/**
 * Feedback round 7: direct vertical-drag detector for the non-scroll
 * camera branch (PanelOverlay scrollContent = false), where no scroll
 * competition exists. Child controls that consume their own drags
 * cancel this detector (innermost-first arbitration); drags on the
 * bare video surface reach it. Velocity from the real pointer
 * positions; cancel releases with zero velocity so the travel rule
 * decides.
 */
fun Modifier.popupDragDetector(
    drag: PopupDragState,
    onDelta: (Float) -> Unit,
    onRelease: (Float) -> Unit,
): Modifier = pointerInput(drag, onDelta, onRelease) {
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = { tracker.resetTracking() },
        onVerticalDrag = { change, dragAmount ->
            tracker.addPosition(change.uptimeMillis, change.position)
            onDelta(dragAmount)
        },
        onDragEnd = { onRelease(tracker.calculateVelocity().y) },
        onDragCancel = { onRelease(0f) },
    )
}

/**
 * Bottom-anchored sheet target: full overlay width, the measured panel
 * height, bottom edge exactly at the overlay bottom. All coordinates in
 * root pixels.
 */
fun sheetTargetRect(overlayOrigin: Offset, overlaySize: IntSize, panelSize: IntSize): Rect {
    val left = overlayOrigin.x
    val top = overlayOrigin.y + overlaySize.height - panelSize.height
    return Rect(left, top, left + overlaySize.width, top + panelSize.height)
}

/** Full-page target: exactly the overlay rect, in root pixels. */
fun detailTargetRect(overlayOrigin: Offset, overlaySize: IntSize): Rect =
    Rect(
        overlayOrigin.x,
        overlayOrigin.y,
        overlayOrigin.x + overlaySize.width,
        overlayOrigin.y + overlaySize.height,
    )

/**
 * Floating-shelf target (2026-10-29 pop-up round): the bottom-anchored
 * floating window every detail destination now opens as — NOT a
 * full-span page, NOT a swipe-up sheet. The panel sits [sideMarginPx]
 * in from each side and [bottomMarginPx] above the overlay bottom
 * (pill clearance + navigation inset), so the dimmed page stays
 * visible around it on all sides. The top is clamped to the overlay
 * top so an oversized panel can never extend past the screen.
 * Pure, unit-tested.
 */
fun popupTargetRect(
    overlayOrigin: Offset,
    overlaySize: IntSize,
    panelSize: IntSize,
    sideMarginPx: Float,
    bottomMarginPx: Float,
): Rect {
    val left = overlayOrigin.x + sideMarginPx
    val right = overlayOrigin.x + overlaySize.width - sideMarginPx
    val bottom = overlayOrigin.y + overlaySize.height - bottomMarginPx
    val top = (bottom - panelSize.height).coerceAtLeast(overlayOrigin.y)
    return Rect(left, top, right, bottom)
}

/**
 * Graceful morph origin when the source card never reported bounds
 * (e.g. a board/Lovelace card, which does not report): the target rect
 * shifted down by [offsetPx], so the surface rises into place instead
 * of appearing instantly. Supersedes #51's instant-appearance fallback:
 * a morph is always possible from this origin, so there is never a
 * reason to pop in or to crash on missing bounds. The caller lerps
 * from this rect to the target exactly like a sourced morph; on exit
 * the same interpolation runs in reverse.
 */
fun fallbackFromRect(target: Rect, offsetPx: Float): Rect =
    Rect(target.left, target.top + offsetPx, target.right, target.bottom + offsetPx)

/**
 * Top corner radius for a morphing surface at [progress] (0 = card,
 * 1 = full page): 28dp at the card, 0dp at the page. Progress is
 * clamped to [0, 1] BEFORE the radius is computed, and the result is
 * floored at 0.dp.
 *
 * Why both clamps: the bounds spring (0.8 damping) and the sheet
 * spring (0.85) both overshoot past 1, and the old formula
 * `28.dp * (1f - progress)` went a hair negative at the overshoot
 * peak. A negative corner throws IllegalArgumentException inside
 * CornerBasedShape.createOutline during the layer update — not
 * during draw — so one bad frame kills the app. Six such crashes on
 * 2026-10-29 (topStart = topEnd about -0.36..-0.67px, bottom corners
 * 0), all on detail opens, which is exactly this formula's
 * signature. MorphSurface ALSO coerces at its clip site, so even a
 * future caller passing a raw negative value cannot throw.
 */
fun morphCornerTop(progress: Float): Dp =
    (28.dp * (1f - progress.coerceIn(0f, 1f))).coerceAtLeast(0.dp)

/**
 * Corner radius for a morphing pop-up shelf at [progress] (0 = card,
 * 1 = shelf): the card's 28dp lerps continuously to the shelf's
 * 32dp across the WHOLE animation — no jump, no snap. The 2026-10-29
 * verdict flagged the radius as not smoothly transitioning: the old
 * pop-out drew fixed 32dp panel corners inside a square-cornered
 * surface, so the radius appeared in one frame instead of morphing.
 * Progress is clamped before computing and the result floored at
 * 0.dp, same crash-proofing as [morphCornerTop] (see the spring-clamp
 * skill). Pure, unit-tested.
 */
fun morphPopupCorner(progress: Float, from: Dp = 28.dp, to: Dp = 32.dp): Dp {
    val p = progress.coerceIn(0f, 1f)
    return (from + (to - from) * p).coerceAtLeast(0.dp)
}

/**
 * Feedback round 11, item 2: the four corners of a tile outline,
 * for morph landing. Pure, unit-tested.
 */
data class CardCorners(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
)

/**
 * The four corners of a [RoundedCornerShape] outline, or null for
 * a polygon/unknown outline. CornerSize implementations are
 * internal in this Compose version, so corners are read through
 * the public [CornerSize.toPx] API at unit density (where px ==
 * dp numerically) instead of a cast — every tile outline in-tree
 * is Dp-based, so the read is exact.
 */
fun roundedLandingCorners(shape: Shape?): CardCorners? {
    if (shape !is RoundedCornerShape) return null
    val density = Density(1f)
    fun corner(size: CornerSize): Dp = size.toPx(Size.Zero, density).dp
    return CardCorners(
        corner(shape.topStart),
        corner(shape.topEnd),
        corner(shape.bottomEnd),
        corner(shape.bottomStart),
    )
}

/**
 * Feedback round 11, item 2: the full-screen morph surface's corners
 * at [progress] (0 = tile, 1 = full page). The corners lerp from the
 * tile's own corners to square exactly in step with the rect lerp, so
 * the shrinking outline always reads as a scaled tile — never a sharp
 * rectangle floating mid-screen — and lands on the tile's corners
 * exactly. Null landing (unknown outline) keeps the old 28dp rule.
 * Pure, unit-tested.
 */
fun morphSurfaceCorners(progress: Float, landing: CardCorners?): CardCorners {
    val t = (1f - progress.coerceIn(0f, 1f))
    if (landing == null) {
        val c = morphCornerTop(progress)
        return CardCorners(c, c, c, c)
    }
    return CardCorners(
        topStart = landing.topStart * t,
        topEnd = landing.topEnd * t,
        bottomEnd = landing.bottomEnd * t,
        bottomStart = landing.bottomStart * t,
    )
}

/**
 * Feedback round 11, item 2: the centered popup's corners at
 * [progress] (0 = tile, 1 = popup). The popup's 28dp corners lerp to
 * the tile's own corners, so the exit ends on the tile's shape, not
 * on 28dp. Null landing keeps 28dp. Pure, unit-tested.
 */
fun morphPopupCorners(progress: Float, landing: CardCorners?): CardCorners {
    val t = (1f - progress.coerceIn(0f, 1f))
    if (landing == null) return CardCorners(28.dp, 28.dp, 28.dp, 28.dp)
    return CardCorners(
        topStart = 28.dp + (landing.topStart - 28.dp) * t,
        topEnd = 28.dp + (landing.topEnd - 28.dp) * t,
        bottomEnd = 28.dp + (landing.bottomEnd - 28.dp) * t,
        bottomStart = 28.dp + (landing.bottomStart - 28.dp) * t,
    )
}

private fun CardCorners.toShape(): RoundedCornerShape =
    RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

/**
 * Feedback round 11, item 2: the full-screen morph surface's clip
 * shape at [progress] for a tile outline of [landingShape]. Corner
 * outlines lerp per [morphSurfaceCorners]; polygon outlines cannot
 * lerp corners, so the surface keeps the soft rounded outline while
 * opaque and switches to the exact polygon in the final fade window
 * (progress < 0.1), where the container alpha is already fading and
 * the never-hidden source tile shows through beneath — the switch
 * crossfades onto the coincident tile instead of popping. At
 * progress 0 the shape IS the tile's shape in both cases.
 *
 * Feedback round 17, item 1: SCALE-AWARE. The graphicsLayer
 * transform multiplies the clip's local radii by the layer scale,
 * so the old absolute-dp corners rendered far too sharp mid-flight
 * (a 28dp tile corner showed ~12dp at p = 0.25 on close, and ~6dp
 * at p = 0 — the "tall sharp rectangle" on the 2026-11-04 Ecobee
 * close, never landing on the squircle). The local radii now
 * divide out the layer scale ([scaleX]/[scaleY], from the same
 * rect lerp the transform uses), so the VISIBLE radius follows
 * the landing × (1 − p) ramp exactly on both axes (elliptical
 * [CornerRadius]: x divides by scaleX, y by scaleY). Dp → px
 * happens in createOutline, where density is available. Polygon
 * landings are proportional paths, so they were never affected
 * and keep the early return above.
 */
/** The four elliptical corner radii of [morphSurfaceShape], in px. */
data class SurfaceCornerRadii(
    val topStart: CornerRadius,
    val topEnd: CornerRadius,
    val bottomEnd: CornerRadius,
    val bottomStart: CornerRadius,
)

/**
 * Feedback round 17, item 1: the scale-aware corner radii for
 * [morphSurfaceShape], extracted as a pure function so unit
 * tests can pin the division math without constructing an
 * Outline (whose RoundRect constructor builds an Android Path,
 * unmocked on the JVM — elliptical radii in particular trip it).
 * Null exactly when [morphSurfaceShape] returns the landing
 * shape itself (polygon landing inside the fade window).
 */
fun morphSurfaceRadii(
    progress: Float,
    landingShape: Shape?,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    density: Density,
): SurfaceCornerRadii? {
    val p = progress.coerceIn(0f, 1f)
    if (landingShape != null && roundedLandingCorners(landingShape) == null && p < 0.1f) {
        return null
    }
    val corners = morphSurfaceCorners(p, roundedLandingCorners(landingShape))
    val sx = if (scaleX > 0f) scaleX else 1f
    val sy = if (scaleY > 0f) scaleY else 1f
    with(density) {
        fun rx(c: Dp): Float = (c.toPx() / sx).coerceAtLeast(0f)
        fun ry(c: Dp): Float = (c.toPx() / sy).coerceAtLeast(0f)
        return SurfaceCornerRadii(
            topStart = CornerRadius(rx(corners.topStart), ry(corners.topStart)),
            topEnd = CornerRadius(rx(corners.topEnd), ry(corners.topEnd)),
            bottomEnd = CornerRadius(rx(corners.bottomEnd), ry(corners.bottomEnd)),
            bottomStart = CornerRadius(rx(corners.bottomStart), ry(corners.bottomStart)),
        )
    }
}

fun morphSurfaceShape(progress: Float, landingShape: Shape?, scaleX: Float = 1f, scaleY: Float = 1f): Shape {
    val p = progress.coerceIn(0f, 1f)
    if (landingShape != null && roundedLandingCorners(landingShape) == null && p < 0.1f) {
        return landingShape
    }
    return object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val r = morphSurfaceRadii(progress, landingShape, scaleX, scaleY, density)!!
            return Outline.Rounded(
                RoundRect(
                    left = 0f, top = 0f, right = size.width, bottom = size.height,
                    topLeftCornerRadius = r.topStart,
                    topRightCornerRadius = r.topEnd,
                    bottomRightCornerRadius = r.bottomEnd,
                    bottomLeftCornerRadius = r.bottomStart,
                ),
            )
        }
    }
}

/**
 * Feedback round 20, item 1: the morph exit landing shape. Bounds and
 * container color were already captured at open (round 16); the shape
 * was the one input read live from the global entity-keyed map at
 * close time, so any intervening write — another rendering of the
 * same entity on a different page (a Home card's grid index differs
 * from a tab tile's), or the prefs fallback when the entry is absent
 * — landed the exit on a different shape than the source tile
 * renders, and the never-hidden tile snapped to its own shape when
 * the surface was removed: the tab-card shape change on every popup
 * exit. The captured-at-open shape wins; the live map and prefs
 * are fallbacks only (e.g. a source that never reported a shape).
 * Pure, unit-tested.
 */
fun resolveLandingShape(captured: Shape?, reported: Shape?, prefs: Shape?): Shape? =
    captured ?: reported ?: prefs

/**
 * Feedback round 11, item 2: the centered popup's clip shape at
 * [progress] for a tile outline of [landingShape]. Corner outlines
 * lerp per [morphPopupCorners]; polygon outlines keep the popup's
 * soft 28dp (the centered popup never lands on the tile rect — it
 * scale-fades — so no landing mismatch can show, and its outline is
 * never sharp).
 */
fun morphPopupShape(progress: Float, landingShape: Shape?): Shape {
    val p = progress.coerceIn(0f, 1f)
    return morphPopupCorners(p, roundedLandingCorners(landingShape)).toShape()
}

/**
 * Feedback round 23, item 2: the four elliptical corner radii of
 * the centered popup's clip with a layer scale, in px — the popup
 * twin of [morphSurfaceRadii] (round 17, item 1). The centered
 * popup's graphicsLayer scales the whole layer (including its
 * clip) by the tileMorphTransform scale, so the old scale-blind
 * clip rendered the landing corners multiplied by the scale: a
 * 28dp tile corner showed ~17-21dp at landing for a lamp tile
 * (scale ~0.6-0.75), and a wide tile (Climate Ecobee: scaleX > 1,
 * scaleY ~0.3) landed stretched in x and squashed in y — then the
 * tile appeared with its true corners and the shape visibly
 * snapped on every exit. The local radii divide out the layer
 * scale, so the VISIBLE corners follow the morphPopupCorners ramp
 * exactly (28dp at rest down to the tile's own corners at
 * landing), on both axes. Polygon/null landings keep the popup's
 * soft 28dp (same rule as [morphPopupCorners]); the popup never
 * switches to a polygon outline. Progress is clamped first; a
 * non-positive scale reads as 1, so a degenerate rect can never
 * divide by zero or flip a radius negative. Pure, unit-tested.
 */
fun morphPopupRadii(
    progress: Float,
    landingShape: Shape?,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    density: Density,
): SurfaceCornerRadii {
    val p = progress.coerceIn(0f, 1f)
    val corners = morphPopupCorners(p, roundedLandingCorners(landingShape))
    val sx = if (scaleX > 0f) scaleX else 1f
    val sy = if (scaleY > 0f) scaleY else 1f
    with(density) {
        fun rx(c: Dp): Float = (c.toPx() / sx).coerceAtLeast(0f)
        fun ry(c: Dp): Float = (c.toPx() / sy).coerceAtLeast(0f)
        return SurfaceCornerRadii(
            topStart = CornerRadius(rx(corners.topStart), ry(corners.topStart)),
            topEnd = CornerRadius(rx(corners.topEnd), ry(corners.topEnd)),
            bottomEnd = CornerRadius(rx(corners.bottomEnd), ry(corners.bottomEnd)),
            bottomStart = CornerRadius(rx(corners.bottomStart), ry(corners.bottomStart)),
        )
    }
}

/**
 * Feedback round 23, item 2: the centered popup's clip shape at
 * [progress] for a tile outline of [landingShape], with the layer
 * scale ([scaleX]/[scaleY], from the same tileMorphTransform the
 * graphicsLayer uses) divided out — see [morphPopupRadii]. The
 * two-argument overload is the scale-1 case (sourceless popups,
 * which fade + scale in place); the sourced CenteredPopup branch
 * must use this overload with the morph's own scales, or the
 * landing corners render multiplied by the scale and the tile
 * snaps to its true corners when the surface is removed.
 */
fun morphPopupShape(progress: Float, landingShape: Shape?, scaleX: Float, scaleY: Float): Shape {
    val p = progress.coerceIn(0f, 1f)
    return object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val r = morphPopupRadii(p, landingShape, scaleX, scaleY, density)
            return Outline.Rounded(
                RoundRect(
                    left = 0f, top = 0f, right = size.width, bottom = size.height,
                    topLeftCornerRadius = r.topStart,
                    topRightCornerRadius = r.topEnd,
                    bottomRightCornerRadius = r.bottomEnd,
                    bottomLeftCornerRadius = r.bottomStart,
                ),
            )
        }
    }
}

/**
 * Alpha for the traveling header at [progress]: fully visible
 * through the morph, handing off to the pop-up's own header only
 * over the last 2% ([0.98, 1.0]) — the 2026-10-30 verdict round's
 * answer to "don't recreate elements, physically move them".
 *
 * Why 2% and not the old 10%: with a 10% dissolve window the
 * traveler and the destination header were BOTH substantially
 * visible for a tenth of the flight, at slightly different rects
 * (the traveler still lerping) — the doubling, the color shift,
 * and the "text pops back up" the verdict reports (the weather
 * 70° arc doubling side-by-side, the lamp "On" redrawing, the
 * "Heat • Cool" state flashing under the "Ecobee" title). With a
 * 2% window the two copies coincide to sub-pixel accuracy at the
 * handoff: the traveler IS the header for the whole flight, and
 * the destination header only materializes as the traveler
 * arrives. Round 18, item 4 retired the traveler that consumed
 * this curve (header badges now fade in place instead); the
 * curve itself is retained unit-tested. Progress is clamped
 * first, so spring overshoot lands exactly on 0. Pure,
 * unit-tested.
 */
fun heroHeaderAlpha(progress: Float): Float =
    1f - smoothstep(0.98f, 1.0f, progress)

/**
 * Feedback round, item 3: the unfolded centered popup's enter
 * transform, pure so unit tests pin it. The popup enters FROM the
 * pressed tile's direction: at progress 0 it sits [distancePx]
 * along the unit vector from the popup center toward the source
 * tile's center, at 0.92 scale and 0 alpha; at progress 1 it is
 * centered, full scale, opaque — so the close (the same lifecycle
 * running in reverse) exits back toward the tile automatically.
 * Progress is clamped first (spring overshoot must never push the
 * offset past the tile or the scale past 1). A null source (no
 * reported card rect) fades + scales in place, with no offset.
 */
data class PopupEnter(val offset: Offset, val scale: Float, val alpha: Float)

/**
 * Feedback round 2, item 6: which side of the overlay a popup
 * anchors to, from the source tile's center x. The overlay spans
 * [overlayLeft, overlayLeft + overlayWidth] in root coords; the
 * source center's relative position splits it into thirds — left
 * third -> START, right third -> END, middle (or a null source,
 * i.e. no reported card rect) -> CENTER. Pure, unit-tested.
 */
enum class PopupSideAnchor { START, CENTER, END }

fun popupSideAnchor(sourceCenterX: Float?, overlayLeft: Float, overlayWidth: Float): PopupSideAnchor {
    if (sourceCenterX == null || overlayWidth <= 0f) return PopupSideAnchor.CENTER
    val rel = (sourceCenterX - overlayLeft) / overlayWidth
    return when {
        rel < 1f / 3f -> PopupSideAnchor.START
        rel > 2f / 3f -> PopupSideAnchor.END
        else -> PopupSideAnchor.CENTER
    }
}

/**
 * Feedback round 15, item 2: the unfolded popup's morph-from-tile
 * transform, pure so unit tests pin it. SUPERSEDES the old rule
 * that unfolded popups "enter from the pressed tile's direction"
 * ([centeredPopupEnter]'s side slide + fade): the surface now
 * interpolates its own rect from the source tile's rect to its
 * placed popup rect — the same container transform the folded
 * morph and Koda's player use — so the panel grows out of the
 * tapped tile and shrinks back onto it on exit. At progress 0 the
 * rect IS the tile rect; at progress 1 it IS the placed rect.
 * Progress is clamped first (spring overshoot must never push the
 * surface past the tile or past full size, and never divide by a
 * degenerate target). A null source keeps the old
 * [centeredPopupEnter] fallback at the call site; this helper is
 * only called with a real tile rect.
 */
data class TileMorph(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
)

fun tileMorphTransform(source: Rect, target: Rect, progress: Float): TileMorph {
    val p = progress.coerceIn(0f, 1f)
    val rect = lerpRect(source, target, p)
    return TileMorph(
        translationX = rect.left - target.left,
        translationY = rect.top - target.top,
        scaleX = if (target.width > 0f) rect.width / target.width else 1f,
        scaleY = if (target.height > 0f) rect.height / target.height else 1f,
    )
}

/**
 * Feedback round, item 3 (SUPERSEDED for sourced popups by round
 * 15's [tileMorphTransform], KEPT as the sourceless fallback): the
 * unfolded centered popup's enter transform. The popup enters FROM
 * the pressed tile's direction: at progress 0 it sits [distancePx]
 * along the unit vector from the popup center toward the source
 * tile's center, at 0.92 scale and 0 alpha; at progress 1 it is
 * centered, full scale, opaque.
 */
fun centeredPopupEnter(
    progress: Float,
    sourceCenter: Offset?,
    popupCenter: Offset,
    distancePx: Float,
): PopupEnter {
    val p = progress.coerceIn(0f, 1f)
    val direction = if (sourceCenter == null) {
        Offset.Zero
    } else {
        val d = sourceCenter - popupCenter
        val len = d.getDistance()
        if (len < 1f) Offset.Zero else d / len
    }
    val remaining = 1f - p
    return PopupEnter(
        offset = direction * (remaining * distancePx),
        scale = 0.92f + 0.08f * p,
        alpha = p,
    )
}

/**
 * Feedback round 18, item 4 (SUPERSEDES the round-17 traveling
 * badge): popup header badges now render IN PLACE inside their
 * header row and fade with the same content curves the morph
 * surface uses — [morphEnterContentAlpha] on open,
 * [morphCloseContentAlpha] on close ([LocalMorphClosing] picks
 * the branch). At rest (progress 1, not closing) this is exactly
 * 1, so headers render fully from frame 0 with no traveler
 * handoff and no scaled-badge flash on folded morphs.
 * Measurement frames read no progress (null) and are unaffected.
 */
fun Modifier.morphHeaderBadgeAlpha(): Modifier = composed {
    val progress = LocalMorphProgress.current
    val closing = LocalMorphClosing.current
    graphicsLayer {
        val p = progress?.invoke()?.coerceIn(0f, 1f) ?: 1f
        alpha = if (closing) morphCloseContentAlpha(p) else morphEnterContentAlpha(p)
    }
}

/**
 * The badge rect inside a header row rect: a [badgeSizePx] square
 * at the row's start, vertically centered. Cards report their
 * header ROW rect (badge + name + state) while tiles report
 * their badge rect directly, so the traveler derives both ends
 * of the badge path from row rects with this one rule: the badge
 * is always the row's leading square, centered vertically, so a
 * wrapped-name dialog row (taller than its badge) still yields
 * the exact badge position. Pure, unit-tested.
 */
fun badgeRectInRow(row: Rect, badgeSizePx: Float): Rect {
    val top = row.top + ((row.height - badgeSizePx) / 2f).coerceAtLeast(0f)
    return Rect(row.left, top, row.left + badgeSizePx, top + badgeSizePx)
}

/**
 * Feedback round 17, item 1: a morph source's badge rect. Tile
 * sources report their square badge rect directly; card sources
 * report their wide header row, so the badge is the row's leading
 * square ([badgeRectInRow]) — the same near-square test the old
 * TravelingHeader used for its tileSource split, now shared so
 * the surface and the traveler agree about the source badge.
 * Pure, unit-tested.
 */
fun morphSourceBadge(source: Rect, badgeSizePx: Float): Rect =
    if (source.width < source.height * 1.5f) source
    else badgeRectInRow(source, badgeSizePx)

/**
 * Feedback round 17, item 1: the traveler blend factor. The
 * traveler starts exactly on the source badge (k = 0 at p = 0 —
 * no frame-0 pop) and converges onto the morphing surface's
 * header slot by p = 0.3 (k = 1), riding the slot for the rest
 * of the flight in both directions. On close the close-tail fade
 * (container alpha out over the final 25%) hides the last
 * stretch, so the return to the linear path below p = 0.3 is
 * never visible as a landing pop. Pure, unit-tested.
 * Round 18, item 4: the traveler rendering that consumed this
 * is gone; the math is retained unit-tested.
 */
fun travelerBlend(progress: Float): Float =
    smoothstep(0f, 0.3f, progress.coerceIn(0f, 1f))

/**
 * Feedback round 17, item 1: the traveling badge's rect in the
 * morph surface's LOCAL coordinates (target space), at
 * [progress] (0 = card, 1 = target). The old TravelingHeader
 * lerped badge-to-badge in window space, which cut across the
 * panel mid-flight while the surface's own header slot hugged
 * the shrinking rect's top-left — the badge "floating over the
 * center of the shrinking panel" on the 2026-11-04 runs. This
 * rect instead rides the surface: its window-mapped position
 * blends from the linear badge path to the surface's header-slot
 * path ([travelerBlend]), and its size stays the linear badge
 * size (badges must not squash under the surface's non-uniform
 * scale). The caller lays the traveler out at the target slot
 * and transforms it onto this rect per frame (see MorphSurface),
 * so the net window rect is exactly: position = blend of linear
 * and slot paths, size = linear badge size, at every frame.
 * At p = 0 the rect maps back to the source badge exactly; at
 * p = 1 it is the target slot exactly. Pure, unit-tested.
 * Round 18, item 4: retained unit-tested (see travelerBlend).
 */
fun travelerLocalRect(
    sourceBadge: Rect,
    targetBadge: Rect,
    source: Rect,
    target: Rect,
    progress: Float,
): Rect {
    val p = progress.coerceIn(0f, 1f)
    val rect = lerpRect(source, target, p)
    val scaleX = if (target.width > 0f) rect.width / target.width else 1f
    val scaleY = if (target.height > 0f) rect.height / target.height else 1f
    val sx = if (scaleX > 0f) scaleX else 1f
    val sy = if (scaleY > 0f) scaleY else 1f
    // Linear badge path (window space): the old behavior, exact
    // at both ends.
    val linLeft = sourceBadge.left + (targetBadge.left - sourceBadge.left) * p
    val linTop = sourceBadge.top + (targetBadge.top - sourceBadge.top) * p
    val linWidth = sourceBadge.width + (targetBadge.width - sourceBadge.width) * p
    val linHeight = sourceBadge.height + (targetBadge.height - sourceBadge.height) * p
    // Surface header-slot path (window space): the slot's fixed
    // target-local offset, carried by the surface's own rect lerp.
    val slotLocalLeft = targetBadge.left - target.left
    val slotLocalTop = targetBadge.top - target.top
    val slotLeft = rect.left + slotLocalLeft * sx
    val slotTop = rect.top + slotLocalTop * sy
    // Blend position; size stays linear (no squash).
    val k = travelerBlend(p)
    val winLeft = linLeft + (slotLeft - linLeft) * k
    val winTop = linTop + (slotTop - linTop) * k
    // Inverse-map the desired window rect into surface-local
    // coordinates: the surface transform maps it back exactly.
    val localLeft = (winLeft - rect.left) / sx
    val localTop = (winTop - rect.top) / sy
    val localWidth = linWidth / sx
    val localHeight = linHeight / sy
    return Rect(localLeft, localTop, localLeft + localWidth, localTop + localHeight)
}

/**
 * Content alpha for a morphing surface at [progress] (0 = card,
 * 1 = target). The content stays INVISIBLE while the panel is still
 * growing and fades in over the last 30% of the morph (smoothstep over
 * [0.7, 1.0]); on exit the same curve dissolves it before the panel
 * shrinks back to the card.
 *
 * Why a steep fade, and why for EVERY morph (superseding the #53
 * weather-only linear fade): the Koda reveal rule — content laid out
 * once at target size, clipped to the growing rect — reads as
 * clipping/doubling whenever the destination content duplicates its
 * source card (the weather arc, the thermostat numbers, the lamp
 * tile), because a full-size arc or value appears inside a
 * still-small rect. The 2026-10-29 verdict video shows it on sheets
 * AND detail pages, so scoping the fade to one sheet cannot fix it.
 * With this curve no frame ever shows content meaningfully clipped
 * by the rect edge: while the rect is mid-size the panel is still
 * fading in over the card (its background ramping via
 * [morphBackgroundAlpha], the card fading out via [morphCardAlpha]),
 * and the content materializes only once the panel is nearly full
 * size. The panel motion itself stays fully visible — only the
 * content is delayed.
 * Progress is clamped to [0, 1] first, so spring overshoot can never
 * push the alpha out of range (see the spring-clamp skill: one shared
 * pure clamping function, never ad-hoc clamps per call site).
 */
fun morphContentAlpha(progress: Float): Float =
    smoothstep(0.7f, 1.0f, progress)

/**
 * Background blend for a morphing surface at [progress] (0 = card,
 * 1 = target): the panel's background color interpolates from the
 * source card's color to the destination color over the FIRST 30% of
 * the morph (smoothstep over [0, 0.3]), then stays at the destination
 * color. Progress is clamped to [0, 1] first, so spring overshoot
 * (the observed 1.00493) lands exactly on 1.
 *
 * Why this exists (2026-10-29 verdict videos on run #60): the steep
 * content fade fixed doubling, but the panel itself switched from
 * the card's color to the destination color in a single frame at
 * morph start — in light mode that step reads as a white flash, and
 * on exit the same step plus the dissolve left the card to pop back
 * in stages. Blending the background over the first 30% removes the
 * hard color step at morph start: the growing panel reads as the
 * card itself growing, not as a new white surface appearing. The
 * content still fades in late ([morphContentAlpha]), so doubling
 * protection is untouched — only the background transition is
 * softened. Callers pass the card color as [MorphSurface]
 * fromBackground and this curve as its backgroundBlend.
 */
fun morphBackgroundBlend(progress: Float): Float =
    smoothstep(0f, 0.3f, progress)

/**
 * Source-card alpha for a morphing surface at [progress] (0 = card,
 * 1 = target): the card stays fully visible at morph start and
 * fades out over the first 40% of the morph, as the surface
 * background fades in beneath it. On exit the same curve runs in
 * reverse, so the card fades back in whole over the last 40% of the
 * return — never vanishing in one frame, never popping back in
 * stages (badge first, text later). This is the container-transform
 * content handoff (2026-10-29 natural-animations round): the old
 * binary hide (alpha 0 for the whole morph) is what made the card
 * vanish at open and pop back at close; the progress-driven fade
 * makes the card and the surface one continuous element.
 */
fun morphCardAlpha(progress: Float): Float =
    1f - smoothstep(0f, 0.4f, progress)

/**
 * Round 21, item 8: which morph source card hides — the TOPMOST
 * surface wins. A panel detail stacked over a detail panel hides
 * its own source card, not the underlying panel's; a detail panel
 * beats the thermostat popout. Pure, so the priority is
 * unit-tested (see Round21LogicTest).
 */
fun topMorphSourceId(panelDetailId: String?, detailId: String?, popoutId: String?, cameraDialogId: String? = null): String? =
    panelDetailId ?: detailId ?: popoutId ?: cameraDialogId

/**
 * Round 21, item 8: the topmost morph's progress, same priority
 * as [topMorphSourceId]. Nulls are "that surface is not open";
 * 1f is rest (no morph — the card reads fully visible).
 */
fun topMorphProgress(panelDetail: Float?, detail: Float?, popout: Float?): Float =
    panelDetail ?: detail ?: popout ?: 1f

/**
 * Content alpha for a morphing surface CLOSE at [progress] (1 = panel
 * at rest, 0 = landed back on the card). The expanded panel's inner
 * content fades out over the FIRST part of the close — fully gone by
 * progress 0.85 (smoothstep over [0.85, 1.0]) — while the surface
 * container itself keeps lerping back to the card rect. Without this,
 * the close showed the expanded content (e.g. the thermostat's
 * scalloped dial and large dual readout) at full opacity while
 * shrinking over the already-visible grid tiles — the dial
 * bleed-through of the 2026-11-01 verdict round. The enter direction
 * deliberately does NOT use this curve (PanelOverlay passes 1f on
 * enter, so content is present from the panel's first frame, per the
 * panels round); only the close fades early. Progress is clamped
 * first, so spring overshoot can never push the alpha out of range.
 */
fun morphCloseContentAlpha(progress: Float): Float =
    smoothstep(0.85f, 1.0f, progress)

/**
 * Source-card alpha on a morphing surface CLOSE at [progress]
 * (1 = panel at rest, 0 = landed back on the card) — the Round 4
 * exit-flash fix. The old exit rule hid the card outright until
 * progress reached 0.02, while the panel content was already fully
 * gone at 0.85 ([morphCloseContentAlpha]): the slot sat blank for
 * most of the close, then the card popped back in one frame —
 * the blank-then-redraw flash. This curve keeps the card hidden
 * exactly while panel content is still visible (progress above
 * 0.85), then fades the card in smoothly over [0.85, 0] — no
 * frame has both at full opacity (no doubling), and no frame
 * has neither (no blank slot, no pop). Progress is clamped
 * first, like every other morph curve here.
 */
fun morphExitCardAlpha(progress: Float): Float =
    1f - smoothstep(0f, 0.85f, progress)

/**
 * Feedback round 23, item 1: the source tile's alpha for the
 * topmost morph at [progress], with the close direction wired to
 * the curve that was written for it. On OPEN ([closing] false)
 * the tile follows [morphCardAlpha] (gone by 0.4, as the surface
 * grows). On CLOSE ([closing] true) it follows
 * [morphExitCardAlpha] (hidden exactly while panel content is
 * still visible, fading in over [0.85, 0] as the surface shrinks
 * back onto the slot). The old provider used [morphCardAlpha] for
 * both directions, so on close the tile stayed invisible until
 * progress 0.4 while the content was already gone by 0.85
 * ([morphCloseContentAlpha]) and the container stayed fully opaque
 * — a bare solid-color shape (solid yellow for the lamps) for 45%
 * of the close, then the tile popping back in one frame.
 * [morphExitCardAlpha] was written in round 4 for exactly this
 * handoff and had zero call sites; this selector is its first use.
 * The caller is the root LocalMorphCardAlpha provider
 * (HavenApp.kt), which reads the same closing flags as
 * LocalMorphClosing. Pure, unit-tested.
 *
 * Round 24, item 3 — SUPERSEDES the fade above: the round-23
 * fade did NOT close the flash on device (diagnosed in code,
 * not re-tuned). The tile alpha reads the ANIMATABLE
 * (rootMorphProgress reads detailProgress.value etc.), but the
 * SURFACES render with the finger-tracked drag progress
 * (dragProgressFor). During a drag the tile sits at 0
 * (animatable still 1f, closing false) — consistent by luck.
 * On release-with-commit, dismissDetail snaps the animatable
 * to the released drag progress (e.g. 0.3) and flips exiting
 * BEFORE the settle runs, so the tile alpha JUMPS 0 ->
 * morphExitCardAlpha(0.3) = 0.65 in one frame (0.96 at drag
 * progress 0.1) while the container is still a small solid
 * shape mid-flight — the pop Garrett still sees. Round 23
 * assumed every close starts at p = 1 (the tap-outside/Back
 * path); the drag path enters the exit curve mid-way, at high
 * alpha. A second mismatch: during the drag-settle, isClosing
 * is false (settling), so the container follows the OPEN fade
 * (out over [0.15, 0]) while the tile follows the EXIT fade —
 * both partially visible at once. Any fade curve keyed on the
 * animatable has this jump, so the handoff is now BINARY: the
 * tile is completely invisible whenever progress > 0.01 on
 * BOTH enter and exit, and the morphing container (see
 * [morphSwapContainerAlpha]) is the single visible card until
 * the exact frame it lands (progress <= 0.01), when the
 * stationary card swaps in with zero overlap. The [closing]
 * parameter is retained for call-site stability; the rule is
 * direction-independent by design.
 */
fun morphTileAlpha(progress: Float, closing: Boolean): Float =
    if (progress.coerceIn(0f, 1f) <= 0.01f) 1f else 0f

/**
 * Round 24, item 3: the morphing popup container's alpha for
 * the tile-swap handoff (see [morphTileAlpha]). The container
 * stays fully opaque all the way down to progress 0.01 — on
 * BOTH enter and exit, and through the drag-settle (where
 * isClosing is false and the old code followed the open fade,
 * so the container dissolved while the tile faded in) — while
 * its background lerps to fromBackground (the source card's
 * exact container color) over [0, 0.35]. At progress just
 * above 0.01 the container sits at the tile's rect in the
 * tile's color with no content yet, so it IS the tile
 * visually; at 0.01 and below it is gone and the stationary
 * card (alpha 1) shows. No frame has both, no frame has
 * neither, on any dismiss path. Sourceless popups do NOT use
 * this — they keep the centeredPopupEnter fade + scale, since
 * there is no tile to swap with. Pure, unit-tested
 * (Round24LogicTest).
 */
fun morphSwapContainerAlpha(progress: Float): Float =
    if (progress.coerceIn(0f, 1f) <= 0.01f) 0f else 1f

/**
 * Surface-background alpha for a morphing surface at [progress]:
 * transparent at the card, opaque once the surface has grown a
 * little (smoothstep over [0, 0.35]). Together with
 * [morphCardAlpha] this removes the solid-background flash on open:
 * at progress 0 the surface is invisible and the card shows through
 * it — no blank solid rect ever replaces the card in a single frame.
 * On exit the same curve runs in reverse, so the surface dissolves
 * onto the card instead of vanishing from under it.
 */
fun morphBackgroundAlpha(progress: Float): Float =
    smoothstep(0f, 0.35f, progress)

/**
 * Content alpha for a morphing surface OPEN at [progress] (0 = card,
 * 1 = target) — the Round 5 symmetric enter curve (PanelOverlay).
 * Content stays invisible for the first 15% of the open (the opaque
 * surface alone covers the stationary card, so nothing can appear
 * at 100% opacity over the card on the first frame — the giant
 * "Disarmed" text bug), then fades in over [0.15, 0.5]. The close
 * direction keeps [morphCloseContentAlpha]; together they are
 * symmetric: late-in on enter, early-out on close. Progress is
 * clamped first, so spring overshoot can never push the alpha out
 * of range. Pure, unit-tested.
 */
fun morphEnterContentAlpha(progress: Float): Float =
    ((progress.coerceIn(0f, 1f) - 0.15f) / 0.35f).coerceIn(0f, 1f)

/**
 * Surface-background alpha for a morphing surface CLOSE at
 * [progress] (1 = panel at rest, 0 = landed back on the card) —
 * the Round 5 opaque-cover rule (PanelOverlay). The surface stays
 * fully opaque through the first 80% of the close (progress 1 down
 * to 0.20), so the shrinking panel cleanly covers the stationary
 * card while open and through the return, and only fades out over
 * [0.20, 0] to reveal the card smoothly underneath — no 1-frame
 * pop, no blank slot. On enter the background is opaque from
 * frame 0 (PanelOverlay passes 1f), so this curve is close-only.
 * Progress is clamped first. Pure, unit-tested.
 */
fun morphCloseBackgroundAlpha(progress: Float): Float =
    (progress.coerceIn(0f, 1f) / 0.20f).coerceIn(0f, 1f)

/**
 * Round 6 reconciled panel-container curves (the SINGLE curve set
 * for items 2 + 3 of the round — this supersedes Round 5's
 * [morphCloseBackgroundAlpha] window on close and Round 5's
 * frame-0-opaque rule on open; the old function is retained as a
 * pure function for its unit tests, but no panel code uses it
 * anymore):
 * - OPEN: the container fades in over the first 15% of the open,
 *   (p / 0.15) clamped. At p = 0 the panel is invisible and the
 *   stationary source card shows through it — no black box can
 *   cut across adjacent cards on the first frame, because there
 *   is no opaque surface yet, only the card itself.
 * - CLOSE: the container stays opaque until p = 0.25, then fades
 *   out over the final 25% of the close, (p / 0.25) clamped. This
 *   replaces Round 5's [0.20, 0] window: the wider window is what
 *   item 3 specifies, and the traveler close-tail crossfade
 *   follows this same curve so traveler and container land
 *   together on the source badge.
 * - CONTENT (unchanged from Round 5, kept so the set is complete):
 *   enter [morphEnterContentAlpha] — invisible through p = 0.15,
 *   in by p = 0.50; close [morphCloseContentAlpha] — out early,
 *   gone by p = 0.85, long before the container starts fading.
 *   Traveler handoff [heroHeaderAlpha] — final 2% — is untouched.
 * - COLOR: the panel background lerps from the source card's
 *   color to the panel background over the first 35% of the morph
 *   ([morphPanelBackgroundBlend], smoothstep over [0, 0.35]),
 *   replacing Round 5's [morphBackgroundBlend] [0, 0.30] window.
 *   Together with the alpha ramp, the growing surface reads as
 *   the card itself growing, never as a new surface flashing in.
 * Pure, unit-tested.
 */
fun morphPanelContainerAlphaOpen(progress: Float): Float =
    (progress.coerceIn(0f, 1f) / 0.15f).coerceIn(0f, 1f)

fun morphPanelContainerAlphaClose(progress: Float): Float =
    (progress.coerceIn(0f, 1f) / 0.25f).coerceIn(0f, 1f)

fun morphPanelBackgroundBlend(progress: Float): Float =
    smoothstep(0f, 0.35f, progress)

/**
 * The single morphing surface, Round 6 (120Hz pipeline).
 *
 * The content is laid out ONCE at the target size and position
 * (the panel's full-screen geometry). ALL per-frame motion lives
 * strictly inside [graphicsLayer] / draw blocks driven by the
 * [progress] lambda: the interpolated rect becomes translationX /
 * translationY / scaleX / scaleY about the top-left origin, the
 * corner clip becomes the graphicsLayer shape, the background
 * color + container alpha are drawn in [drawBehind], and the
 * content alpha is a graphicsLayer alpha. No Modifier.size or
 * Modifier.offset animates in composition anymore, so a morph
 * frame costs no Compose Measure/Layout pass — the RenderThread
 * carries it. The one deliberate visual change vs Round 5: the
 * content now SCALES with the surface (graphicsLayer scale) instead
 * of being clipped to the growing rect; the end states (full-size
 * panel at rest, card-sized surface at p = 0) are identical.
 *
 * [source] is the captured source card rect (or the graceful
 * fallback rect); [target] is the panel's full-screen rect in the
 * same root coordinates. [fromBackground] is the source card's
 * container color (PanelOverlay's sourceCardColor): the surface
 * background lerps from it to [background] over the first 35% of
 * the morph, and its alpha follows the reconciled container curves
 * (open: first 15% in; close: final 25% out). When [fromBackground]
 * is null the surface uses [background] directly. [progress] is a
 * lambda reading the caller's Animatable; it is invoked ONLY inside
 * graphicsLayer/draw blocks (see [LocalMorphProgress]). [onPlaced]
 * reports the surface's layout bounds (the target rect, since the
 * layout itself sits at target geometry) for the camera rise
 * origin; layout bounds are transform-independent, so this fires
 * correctly from the first frame.
 */
@Composable
fun MorphSurface(
    source: Rect,
    target: Rect,
    overlayOrigin: Offset,
    background: Color,
    modifier: Modifier = Modifier,
    fromBackground: Color? = null,
    progress: (() -> Float)? = null,
    isClosing: Boolean = false,
    onPlaced: ((Rect) -> Unit)? = null,
    // Feedback round 11, item 2: the source tile's outline (from
    // [LocalCardShapes], resolved by the caller). The clip lerps
    // into it so the exit lands on the tile's own corners.
    landingShape: Shape? = null,
    // Feedback round 18, item 4: the round-17 traveling badge
    // is GONE — header badges render in place and fade with the
    // content curves (see morphHeaderBadgeAlpha), so this surface
    // carries no traveler slot anymore.
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val targetWidthDp = with(density) { target.width.toDp() }
    val targetHeightDp = with(density) { target.height.toDp() }
    // NOTE: overlayOrigin is retained in the signature for callers
    // that already pass it; the transform below is target-relative,
    // so the origin cancels (target.left IS the overlay origin for
    // full-screen panels) and no origin arithmetic is needed here.
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress?.invoke()?.coerceIn(0f, 1f) ?: 1f
                val rect = lerpRect(source, target, p)
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = rect.left - target.left
                translationY = rect.top - target.top
                scaleX = if (target.width > 0f) rect.width / target.width else 1f
                scaleY = if (target.height > 0f) rect.height / target.height else 1f
                // Round 17: the clip divides out the layer scale,
                // so visible corners follow the landing ramp.
                shape = morphSurfaceShape(p, landingShape, scaleX, scaleY)
                clip = true
            }
            .drawBehind {
                val p = progress?.invoke()?.coerceIn(0f, 1f) ?: 1f
                val blended = if (fromBackground != null) {
                    lerp(fromBackground, background, morphPanelBackgroundBlend(p))
                } else {
                    background
                }
                // Feedback round 22, item 3: on close the container
                // does NOT fade to transparent (the old
                // morphPanelContainerAlphaClose ramped it to 0 by
                // p = 0.25, so the shrinking surface vanished
                // mid-flight and the tile popped back as a
                // white/cream flash). The fromBackground lerp above
                // already cross-fades the surface into the source
                // tile's exact container color as p -> 0, so the
                // surface lands on the tile seamlessly; the alpha
                // stays 1 through the close and the lerp does the
                // handoff. Round 24, item 3 — SUPERSEDES the open
                // fade too (the sentence above is history): the
                // container is the single visible card until
                // landing (see morphSwapContainerAlpha) — opaque
                // down to p = 0.01 on enter, exit, and the
                // drag-settle alike, so no fade window can
                // disagree with the tile's binary handoff. At
                // p <= 0.01 the tile (alpha 1) shows instead.
                val containerAlpha = morphSwapContainerAlpha(p)
                drawRect(blended.copy(alpha = blended.alpha * containerAlpha))
            }
            .then(
                if (onPlaced != null) Modifier.onGloballyPositioned { onPlaced(it.boundsInWindow()) }
                else Modifier,
            ),
    ) {
        Box(
            Modifier.size(targetWidthDp, targetHeightDp)
                .graphicsLayer {
                    alpha = when {
                        progress == null -> 1f
                        isClosing -> morphCloseContentAlpha(progress())
                        else -> morphEnterContentAlpha(progress())
                    }
                },
            contentAlignment = Alignment.TopStart,
        ) {
            if (progress != null) {
                CompositionLocalProvider(LocalMorphProgress provides progress) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}
