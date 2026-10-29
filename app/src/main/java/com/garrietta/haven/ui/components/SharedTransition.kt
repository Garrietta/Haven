package com.garrietta.haven.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import com.garrietta.haven.ui.theme.springBounds

/**
 * Shared-element plumbing for card -> sheet opens (spec item 6) and,
 * since the visual-bugs round, card -> detail-page opens too.
 *
 * A Dialog window breaks shared-element coordinate spaces, so the sheet
 * renders inline (see EntityPopupSheet) and both sides of the morph live
 * in one SharedTransitionLayout in HavenApp. Tiles and cards read these
 * locals and declare `sharedBounds` with key "card-<entityId>"; the sheet
 * panel and the detail-page overlay root declare the same key in their
 * own scopes. If either scope is absent (a card rendered outside the app
 * shell), the modifier is a no-op and the destination still appears with
 * its fade — graceful degradation, never a crash.
 *
 * ONE OWNER PER KEY. The transition system cannot morph one key to two
 * targets: the 2026-10-29 evidence video showed the sheet panel AND the
 * control card nested inside it both declaring "card-<id>" in the same
 * scope, rendering as overlapping ghost copies mid-morph and settling
 * off-center. Only the transition-owned container declares a key — the
 * source tile/card, the sheet panel, or the detail overlay root. Nested
 * cards never do (see EntityControlCard's shareBounds flag).
 *
 * The bounds spring is the spec's container spring (0.8/MediumLow), not
 * the older 0.72/320 morph spring: one bounds language for sheets and
 * pages. Under reduce-motion the bounds snap instead of springing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Declares shared bounds for a card key when both scopes are present, else a no-op. */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedCardBounds(key: String): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalSharedVisibilityScope.current
    val reduceMotion = LocalReduceMotion.current
    if (sharedScope != null && visibilityScope != null) {
        with(sharedScope) {
            sharedBounds(
                rememberSharedContentState(key),
                visibilityScope,
                boundsTransform = { _, _ ->
                    if (reduceMotion) {
                        androidx.compose.animation.core.spring(
                            stiffness = 10_000f,
                            dampingRatio = 1f,
                        )
                    } else {
                        springBounds()
                    }
                },
            )
        }
    } else {
        this
    }
}
