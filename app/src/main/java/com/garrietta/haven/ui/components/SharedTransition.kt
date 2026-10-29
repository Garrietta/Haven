package com.garrietta.haven.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.composed
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.garrietta.haven.ui.theme.springMorph

/**
 * Shared-element plumbing for card -> sheet opens (spec item 6).
 *
 * A Dialog window breaks shared-element coordinate spaces, so the sheet
 * renders inline (see EntityPopupSheet) and both sides of the morph live
 * in one SharedTransitionLayout in HavenApp. Tiles and cards read these
 * locals and declare `sharedBounds` with key "card-<entityId>"; the sheet
 * panel declares the same key in its own visibility scope. If either
 * scope is absent (a card rendered outside the app shell), the modifier
 * is a no-op and the sheet still appears with its fade — graceful
 * degradation, never a crash.
 *
 * PAGES keep the recorded bottom-sheet rise and do not use this.
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
    if (sharedScope != null && visibilityScope != null) {
        with(sharedScope) {
            sharedBounds(
                rememberSharedContentState(key),
                visibilityScope,
                boundsTransform = { _, _ -> springMorph() },
            )
        }
    } else {
        this
    }
}
