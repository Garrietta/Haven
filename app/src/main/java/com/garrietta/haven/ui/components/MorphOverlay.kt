package com.garrietta.haven.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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

/** Entity id of the card currently hidden as a morph source, or null. */
val LocalHiddenCardId = staticCompositionLocalOf<String?> { null }

/**
 * Header bounds by entity id, in root coordinates: the card header row
 * (EntityControlCard) or the tile badge (EntityTile). Written by cards,
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
 * Alpha for the active morph source card (see
 * [hideWhenMorphSource]). Round 5: the overlay root provides 1f
 * always — the card never fades; the opaque panel surface covers
 * it. (Historically this was progress-driven via [morphCardAlpha];
 * that curve is retained below but no longer wired in.)
 */
val LocalMorphCardAlpha = staticCompositionLocalOf { 1f }

/**
 * Reports this layout's boundsInRoot into [LocalCardBounds] under [id].
 * Writes only when the rect actually changed, so the post-layout
 * callback cannot schedule a recomposition loop.
 */
fun Modifier.reportCardBounds(id: String): Modifier = composed {
    val bounds = LocalCardBounds.current
    onGloballyPositioned { coords: LayoutCoordinates ->
        val rect = coords.boundsInRoot()
        if (bounds[id] != rect) bounds[id] = rect
    }
}

/**
 * Reports this layout's header boundsInRoot into [LocalHeroBounds]
 * under [id] (see [reportCardBounds] for the write-once-changed rule).
 * EntityControlCard reports its header row; EntityTile reports its
 * badge (the tile's name/status sit below the badge, so the badge
 * rect anchors the traveling header's position).
 */
fun Modifier.reportHeroBounds(id: String): Modifier = composed {
    val bounds = LocalHeroBounds.current
    onGloballyPositioned { coords: LayoutCoordinates ->
        val rect = coords.boundsInRoot()
        if (bounds[id] != rect) bounds[id] = rect
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
    graphicsLayer {
        alpha = if (id == hiddenId) cardAlpha else 1f
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
    graphicsLayer {
        alpha = if (id == hiddenId) cardAlpha else 1f
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
 * the destination header (gated by [hideWhileTraveling], and the
 * arc panel by [dissolveIntoTraveler] = 1 − this curve) only
 * materializes as the traveler arrives. On exit the same curve
 * runs in reverse: the destination header hands back to the
 * traveler in the first 2% of the return, and the traveler
 * carries it home. Progress is clamped first, so spring
 * overshoot lands exactly on 0. Pure, unit-tested.
 */
fun heroHeaderAlpha(progress: Float): Float =
    1f - smoothstep(0.98f, 1.0f, progress)

/**
 * Hides a destination header while its traveling header is in
 * flight: alpha 0 for the whole morph except the final 2%
 * handoff window (1 − [heroHeaderAlpha]), 1 at rest and outside
 * any morph surface. Dialog headers (sheet, detail, thermostat
 * pop-out) wear this so the destination header can never compete
 * with its traveler — the header the user sees mid-flight is
 * always the traveling one, and the destination header appears
 * exactly as the traveler arrives at its rect. Measurement frames
 * read no progress (null) and are unaffected.
 */
fun Modifier.hideWhileTraveling(): Modifier = composed {
    val progress = LocalMorphProgress.current
    graphicsLayer {
        alpha = if (progress == null) 1f else 1f - heroHeaderAlpha(progress())
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(morphCornerTop(p))
                clip = true
            }
            .drawBehind {
                val p = progress?.invoke()?.coerceIn(0f, 1f) ?: 1f
                val blended = if (fromBackground != null) {
                    lerp(fromBackground, background, morphPanelBackgroundBlend(p))
                } else {
                    background
                }
                val containerAlpha = if (isClosing) {
                    morphPanelContainerAlphaClose(p)
                } else {
                    morphPanelContainerAlphaOpen(p)
                }
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
