package com.garrietta.haven.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Canonical window size classes (developer.android.com adaptive
 * layouts): Compact < 600dp, Medium 600–839dp, Expanded ≥ 840dp.
 * Haven classifies from its own BoxWithConstraints window width —
 * no material3-adaptive dependency is added (the adaptive library's
 * NavigationSuiteScaffold would auto-switch bar↔rail, and the pill
 * bar IS Haven's navigation by standing overrule: a rail would
 * displace it, so the pill wins and no rail ships).
 *
 * BEHAVIOR MAP (the Fold8 tension, resolved, not ignored): the Fold8
 * unfolded is ~690dp portrait / ~830dp landscape, so it lands in
 * MEDIUM, not Expanded — an 840dp gate would miss unfolded landscape
 * entirely. Two-pane behavior therefore starts at MEDIUM (the
 * existing 600dp threshold, preserved exactly as LocalWideLayout);
 * EXPANDED adds further optimization on top (3-column grids, a
 * centered max-width content cap), never a behavior change for the
 * Fold8. Foldable posture is handled through window-size changes:
 * folding/unfolding changes maxWidth, which reclassifies the window,
 * and the ViewModel holds state so continuity survives recreation.
 */
enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

/** Pure classification, unit-tested. Canonical breakpoints. */
fun windowSizeClassFor(width: Dp): WindowSizeClass = when {
    width < 600.dp -> WindowSizeClass.COMPACT
    width < 840.dp -> WindowSizeClass.MEDIUM
    else -> WindowSizeClass.EXPANDED
}

/** Provided by HavenApp above the Scaffold, next to LocalWideLayout. */
val LocalWindowSizeClass = compositionLocalOf { WindowSizeClass.COMPACT }
