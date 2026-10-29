package com.garrietta.haven.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

/**
 * Page transitions that replicate the recorded Koda mini-player expansion.
 *
 * What the recording shows (frame-by-frame): tapping Koda's bottom mini
 * bar does NOT scale the page out of the bar's bounds. Instead a sheet
 * rises from the bottom edge — its top edge starts low on the screen with
 * rounded top corners, and it grows upward until it fills the screen and
 * the corners flatten to zero. The page underneath stays perfectly static
 * (no scale, no fade) while the sheet covers it from the bottom up, and
 * the sheet's own content is laid out at full size and revealed as the
 * sheet grows — the art first, then title, progress, and controls — never
 * stretched or scaled. Collapse reverses it exactly: the sheet sinks back
 * down and the page underneath is revealed. The whole rise takes about
 * half a second with a soft settle, not a bouncy overshoot.
 *
 * That is what this wrapper does, and it deliberately replaces the old
 * RisingPageContent (scale-from-tapped-bounds + fade), which read as a
 * generic zoom/slide and did not feel like Koda at all:
 *
 * - The page starts shifted down so its visible top edge sits at the
 *   tapped item's bottom edge ([origin].bottom in window coordinates),
 *   exactly where Koda's sheet starts at its mini bar. With no origin it
 *   starts fully below the screen (a pure bottom rise).
 * - As [progress] goes 0 → 1 the shift eases to zero, the top corner
 *   radius flattens 28dp → 0, and the content fades gently 0.55 → 1.
 *   The content itself is never scaled.
 * - Back runs the same mapping 1 → 0: the page sinks back toward the
 *   tapped item and the origin page is revealed, static, underneath.
 * - At progress 1 (or with no origin) the transform is the identity, so
 *   swapping between the animated wrapper and plain content is seamless.
 *   Motion follows [LocalReduceMotion]: callers pass progress already
 *   snapped when reduced motion is on.
 *
 * No Navigation-Compose migration is needed for this: the recording is a
 * bottom-anchored sheet rise, not a shared-element morph between two
 * NavHost destinations, and HavenApp's own backstack plus this wrapper
 * reproduces it directly. A NavHost migration would be a larger change
 * for a worse match.
 */
@Composable
fun SheetPageContent(
    origin: Rect?,
    progress: Float,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    var pageBounds by remember { mutableStateOf<Rect?>(null) }
    val bounds = pageBounds
    // How far the page must shift down so its top edge lands on the tapped
    // item's bottom edge (or fully below the screen with no origin).
    // Coerced to the page height: an origin below the page (e.g. a sheet's
    // bounds) degrades to a pure bottom rise instead of overshooting.
    val startShift = if (origin != null && bounds != null) {
        (origin.bottom - bounds.top).coerceIn(0f, bounds.height)
    } else if (bounds != null) {
        bounds.height
    } else {
        0f
    }
    val active = bounds != null && progress < 1f
    val shift = startShift * (1f - progress)
    val radius = 28.dp * (1f - progress)
    val contentAlpha = 0.55f + 0.45f * progress
    Box(
        modifier
            .onGloballyPositioned { pageBounds = it.boundsInWindow() }
            .graphicsLayer {
                this.translationY = if (active) shift else 0f
            }
            .clip(RoundedCornerShape(topStart = if (active) radius else 0.dp, topEnd = if (active) radius else 0.dp))
            .background(background),
    ) {
        Box(
            Modifier.graphicsLayer {
                this.alpha = if (active) contentAlpha else 1f
            },
        ) {
            content()
        }
    }
}
