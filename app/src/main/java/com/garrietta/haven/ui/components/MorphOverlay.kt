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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt

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
 * 2. SINGLE SURFACE, SOURCE HIDDEN: while a morph is active, the source
 *    card renders with alpha 0 ([hideWhenMorphSource]) — layout
 *    unchanged, so nothing reflows — and the morph surface is the ONLY
 *    rendering of the content. No doubling at any frame, entering or
 *    exiting.
 * 3. NO SCALING: content is laid out once at its target size and
 *    clipped to the interpolated rect, pinned top-left (the header is
 *    visible from the first frame and descends with the surface as it
 *    grows) — revealed as the surface grows, never stretched, per the
 *    recorded Koda language. Sheet target sizes come from a single
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
 * Renders this layout invisible (alpha 0, layout unchanged) while it is
 * the active morph source. Alpha, not removal: removing the card would
 * reflow the grid and invalidate the captured source rect mid-morph.
 */
fun Modifier.hideWhenMorphSource(id: String): Modifier = composed {
    val hiddenId = LocalHiddenCardId.current
    graphicsLayer {
        alpha = if (id == hiddenId) 0f else 1f
    }
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
 * The single morphing surface. [rect] is the interpolated rect in root
 * pixels; [overlayOrigin] is the overlay root's own root-coordinate
 * origin, subtracted so the surface lands correctly regardless of where
 * the overlay Box sits. [targetSize] is the content's laid-out size in
 * pixels; the content is placed top-left inside the surface at exactly
 * that size and clipped to the surface (never scaled, never reflowed).
 * [cornerTop] is the surface's top corner radius (sheets: constant 28dp;
 * detail pages: 28dp → 0dp over the morph). [dragOffsetY] shifts the
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
    onPlaced: ((Rect) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { rect.width.toDp() }
    val heightDp = with(density) { rect.height.toDp() }
    val targetWidthDp = with(density) { targetSize.width.toDp() }
    val targetHeightDp = with(density) { targetSize.height.toDp() }
    Box(
        modifier
            .offset {
                IntOffset(
                    (rect.left - overlayOrigin.x).roundToInt(),
                    (rect.top - overlayOrigin.y + dragOffsetY).roundToInt(),
                )
            }
            .size(widthDp, heightDp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = cornerTop, topEnd = cornerTop))
            .background(background)
            .then(
                if (onPlaced != null) Modifier.onGloballyPositioned { onPlaced(it.boundsInWindow()) }
                else Modifier,
            ),
    ) {
        Box(
            Modifier.size(targetWidthDp, targetHeightDp),
            contentAlignment = Alignment.TopStart,
        ) {
            content()
        }
    }
}
