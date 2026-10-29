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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

/**
 * Container-transform page transitions, bounds-anchored.
 *
 * Why bounds-anchored instead of Navigation-Compose shared elements:
 * HavenApp navigates on its own backstack, not a NavHost, so the
 * navigation library's shared-element support isn't available without a
 * full navigation migration. Instead the tapped tile, card, or sheet saves
 * its on-screen rect (see [HavenViewModel.pageOrigin]) and the destination
 * page morphs out of exactly that rect — same effect, no new dependency.
 *
 * The caller drives [progress] 0 → 1 on the house spring for enter and
 * 1 → 0 for back; this wrapper only maps progress to scale, fade, and
 * corner radius. At progress 1 (or with no origin) it is the identity, so
 * swapping between the animated wrapper and plain content is seamless.
 * Motion follows [LocalReduceMotion]: callers pass progress already
 * snapped to 1 when reduced motion is on.
 */
@Composable
fun RisingPageContent(
    origin: Rect?,
    progress: Float,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    var pageBounds by remember { mutableStateOf<Rect?>(null) }
    val bounds = pageBounds
    val active = origin != null && bounds != null && progress < 1f
    // At the endpoints the transform is the identity; only interpolate mid-flight.
    val startScaleX = if (origin != null && bounds != null) {
        (origin.width / bounds.width).coerceIn(0.2f, 1f)
    } else {
        1f
    }
    val startScaleY = if (origin != null && bounds != null) {
        (origin.height / bounds.height).coerceIn(0.2f, 1f)
    } else {
        1f
    }
    val originCenterX = if (origin != null && bounds != null) {
        ((origin.center.x - bounds.left) / bounds.width).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val originCenterY = if (origin != null && bounds != null) {
        ((origin.center.y - bounds.top) / bounds.height).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val scaleX = startScaleX + (1f - startScaleX) * progress
    val scaleY = startScaleY + (1f - startScaleY) * progress
    val alpha = 0.45f + 0.55f * progress
    val radius = 28.dp * (1f - progress)
    Box(
        modifier
            .onGloballyPositioned { pageBounds = it.boundsInWindow() }
            .graphicsLayer {
                this.scaleX = if (active) scaleX else 1f
                this.scaleY = if (active) scaleY else 1f
                this.alpha = if (active) alpha else 1f
                this.transformOrigin = TransformOrigin(originCenterX, originCenterY)
            }
            .clip(RoundedCornerShape(if (active) radius else 0.dp))
            .background(background),
    ) {
        content()
    }
}
