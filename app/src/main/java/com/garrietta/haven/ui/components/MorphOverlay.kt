package com.garrietta.haven.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt

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
 * 2. SINGLE SURFACE, SOURCE FADING (container transform, 2026-10-29
 *    natural-animations round — supersedes the #62 instant return
 *    AND the binary hide): while a morph is active, the source card
 *    renders with the progress-driven alpha from [morphCardAlpha]
 *    (visible at morph start, gone once the surface is opaque, fading
 *    back in whole over the last 40% of an animated exit), and the
 *    surface background fades in over [0, 0.35] via
 *    [morphBackgroundAlpha] while its content fades in late via
 *    [morphContentAlpha]. Start content fades out early, end content
 *    fades in late, symmetric on exit: the card and the surface read
 *    as ONE element transforming, never a blank solid panel flashing
 *    in at open, never both contents at full opacity at once (that
 *    was #49's ghosting), never a staged reappearance, never a snap
 *    back. Exit is the smooth reverse of enter on the same bounds
 *    spring — the #62 synchronous removal read on device as "a sudden
 *    smack back to the previous state" and is gone.
 * 3. NO SCALING: content is laid out once at its target size and
 *    clipped to the interpolated rect, pinned top-left (revealed as
 *    the surface grows, never stretched, per the recorded Koda
 *    language — with the content itself fading in late per
 *    [morphContentAlpha], so no full-size content ever appears inside
 *    a still-small rect). Sheet target sizes come from a single
 *    invisible measurement frame (alpha 0, below the scrim, so it can
 *    neither flash nor take touches); detail targets are the overlay
 *    size and need no measurement.
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
 * Progress-driven alpha for the active morph source card (see
 * [hideWhenMorphSource]): 1 when no morph is active. Provided by the
 * overlay root from the active morph's progress via
 * [morphCardAlpha].
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
 * Hides this layout's header while it is the active morph source's
 * traveling header (layout unchanged, alpha only). The card's header
 * must vanish the moment the traveling header appears at the same
 * rect — otherwise the morph opens with two copies of the badge,
 * name, and state (the doubling read in every 2026-10-29 verdict
 * video). The rest of the card still fades on [morphCardAlpha]; only
 * the header is hidden from frame 0, because the traveling header
 * replaces it exactly.
 */
fun Modifier.hideHeroWhenMorphSource(id: String): Modifier = composed {
    val hiddenId = LocalHiddenHeroId.current
    graphicsLayer {
        alpha = if (id == hiddenId) 0f else 1f
    }
}

/**
 * Hides this layout's BODY while it is the active morph source
 * (layout unchanged, alpha only) — the 2026-10-29 verdict-fix round's
 * answer to the weather pop-up still doubling on #67. The header hide
 * above covers only the header row: the source card's arc, humidity
 * line, sensor chart, and lamp hero stayed visible (fading only on
 * [morphCardAlpha]) while the pop-up's duplicate content faded in
 * over them, so two copies of the same content shared the screen.
 * With the body hidden from frame 0 — same mechanism as the header
 * hide, keyed on the morph-source id — the pop-up's content is the
 * only copy on screen at any frame, and the card restores atomically
 * on dismiss exactly as the header does (the morph surface sits
 * exactly on the card rect at progress 0 with a transparent
 * background, so the restore is invisible). Callers gate this on
 * their morph-source flag, exactly like [hideWhenMorphSource]:
 * nested/destination cards sharing the entity id must never hide.
 */
fun Modifier.hideBodyWhenMorphSource(id: String): Modifier = composed {
    val hiddenId = LocalHiddenCardId.current
    graphicsLayer {
        alpha = if (id == hiddenId) 0f else 1f
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
 * through the morph, dissolving into the pop-up's own header over
 * the last 10% ([0.9, 1.0]) — late enough that the header has
 * arrived at its target rect, short enough that the two headers are
 * never both readable. On exit the same curve runs in reverse, so
 * the header reappears at its target and travels back to the card.
 * Progress is clamped first, so spring overshoot lands exactly on 0.
 * Pure, unit-tested.
 */
fun heroHeaderAlpha(progress: Float): Float =
    1f - smoothstep(0.9f, 1.0f, progress)

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
 * The single morphing surface. [rect] is the interpolated rect in root
 * pixels; [overlayOrigin] is the overlay root's own root-coordinate
 * origin, subtracted so the surface lands correctly regardless of where
 * the overlay Box sits. [targetSize] is the content's laid-out size in
 * pixels; the content is placed top-left inside the surface at exactly
 * that size and clipped to the surface (never scaled, never reflowed).
 * [cornerTop] is the surface's top corner radius (sheets: constant 28dp;
 * detail pages: 28dp → 0dp over the morph). It is coerced to >= 0.dp
 * at the clip site: no caller can ever hand this surface a negative
 * corner again (see [morphCornerTop]). [cornerAll], when non-null,
 * replaces the top-only clip with a uniform all-corner radius —
 * the pop-up shelves clip all four corners, and the radius itself
 * morphs via [morphPopupCorner], so the window's corners transition
 * smoothly across the whole animation instead of snapping in.
 * [contentAlpha] fades the
 * surface's content (1 = fully visible); every morph passes
 * [morphContentAlpha] of its progress, so content materializes only
 * once the panel is nearly full size instead of popping to full size
 * inside a still-small rect (see [morphContentAlpha]).
 * [fromBackground] + [backgroundBlend] blend the surface's background
 * from the source card's color to [background] over the first 30% of
 * the morph (callers pass [morphBackgroundBlend] of their progress),
 * so the growing panel reads as the card growing rather than a new
 * surface flashing in — the enter white flash from the 2026-10-29
 * verdict videos. When [fromBackground] is null the surface uses
 * [background] directly (backward-compatible default). [backgroundAlpha]
 * fades the surface's background itself (callers pass
 * [morphBackgroundAlpha] of their progress): transparent at the card,
 * opaque once grown — so no solid rect ever replaces the card in a
 * single frame at open, and the surface dissolves onto the card on
 * exit instead of vanishing from under it. [dragOffsetY] shifts the
 * surface down for the sheet drag gesture without touching the rect
 * math. [onPlaced] reports the surface's window bounds (the sheet's
 * Details action uses them as the camera rise origin, as before).
 */
@Composable
fun MorphSurface(
    rect: Rect,
    overlayOrigin: Offset,
    targetSize: IntSize,
    cornerTop: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    dragOffsetY: Float = 0f,
    contentAlpha: Float = 1f,
    fromBackground: Color? = null,
    backgroundBlend: Float = 1f,
    backgroundAlpha: Float = 1f,
    cornerAll: Dp? = null,
    onPlaced: ((Rect) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { rect.width.toDp() }
    val heightDp = with(density) { rect.height.toDp() }
    val targetWidthDp = with(density) { targetSize.width.toDp() }
    val targetHeightDp = with(density) { targetSize.height.toDp() }
    val blendedBackground = if (fromBackground != null) {
        lerp(fromBackground, background, backgroundBlend.coerceIn(0f, 1f))
    } else {
        background
    }
    // Multiply, never replace: a Transparent surface (the pop-out)
    // must stay transparent at every alpha, and an opaque surface
    // ramps 0 -> 1 with the morph.
    val effectiveBackground = blendedBackground.copy(
        alpha = blendedBackground.alpha * backgroundAlpha.coerceIn(0f, 1f),
    )
    Box(
        modifier
            .offset {
                IntOffset(
                    (rect.left - overlayOrigin.x).roundToInt(),
                    (rect.top - overlayOrigin.y + dragOffsetY).roundToInt(),
                )
            }
            .size(widthDp, heightDp)
            .clip(
                if (cornerAll != null) androidx.compose.foundation.shape.RoundedCornerShape(cornerAll.coerceAtLeast(0.dp))
                else androidx.compose.foundation.shape.RoundedCornerShape(topStart = cornerTop.coerceAtLeast(0.dp), topEnd = cornerTop.coerceAtLeast(0.dp)),
            )
            .background(effectiveBackground)
            .then(
                if (onPlaced != null) Modifier.onGloballyPositioned { onPlaced(it.boundsInWindow()) }
                else Modifier,
            ),
    ) {
        Box(
            Modifier.size(targetWidthDp, targetHeightDp)
                .graphicsLayer { alpha = contentAlpha },
            contentAlignment = Alignment.TopStart,
        ) {
            content()
        }
    }
}
