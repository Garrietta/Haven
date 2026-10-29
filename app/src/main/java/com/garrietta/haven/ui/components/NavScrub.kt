package com.garrietta.haven.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs

/**
 * Drag along the pill bar to pick a tab, the way a thumb slides across a
 * segmented control — ported from Koda's NavBarScrub. The highlight follows
 * the finger ([hoveredTab]) and the destination only changes on release, so
 * crossing three tabs on the way to a fourth composes one screen rather than
 * four. A tap is untouched: the drag detector only claims the pointer past
 * touch slop, which is also what cancels the item's own press.
 *
 * Items register their bounds with [pillScrubItem]; the bar that hosts them
 * takes [pillScrub].
 */
class PillScrubState {
    private var bar: LayoutCoordinates? = null
    private val items = mutableMapOf<Int, LayoutCoordinates>()

    /** The tab indices currently drawn, so a hidden destination is never landed on. */
    internal var tabIds: List<Int> = emptyList()

    // Refreshed on every composition rather than used as pointerInput keys: a
    // new lambda each recomposition would restart the detector, and hovering
    // recomposes the bar, so the scrub would cancel itself on the first tick.
    internal var onHoverChange: () -> Unit = {}
    internal var onCommit: (Int) -> Unit = {}

    /** The tab under the finger while a scrub is in progress, else null. */
    var hoveredTab by mutableStateOf<Int?>(null)
        private set

    internal fun registerBar(coordinates: LayoutCoordinates) {
        bar = coordinates
    }

    internal fun registerItem(tabId: Int, coordinates: LayoutCoordinates) {
        items[tabId] = coordinates
    }

    /**
     * The item whose centre is nearest [position] along the bar's axis, in the
     * bar's coordinates. Nearest rather than contains, so the gaps between
     * pills and the bar's own end padding still resolve to something.
     */
    internal fun tabAt(position: Float): Int? {
        val bar = bar?.takeIf { it.isAttached } ?: return null
        return tabIds.mapNotNull { id ->
            val item = items[id]?.takeIf { it.isAttached } ?: return@mapNotNull null
            val centre = bar.localBoundingBoxOf(item).center
            id to abs(centre.x - position)
        }.minByOrNull { it.second }?.first
    }

    internal fun hover(tabId: Int?) {
        hoveredTab = tabId
    }
}

/**
 * [onHoverChange] fires each time the finger crosses onto another item (a
 * haptic tick belongs there); [onCommit] fires once on release with the item
 * under the finger. A cancelled drag commits nothing.
 */
@Composable
fun rememberPillScrubState(
    tabIds: List<Int>,
    onHoverChange: () -> Unit,
    onCommit: (Int) -> Unit,
): PillScrubState = remember { PillScrubState() }.also {
    it.tabIds = tabIds
    it.onHoverChange = onHoverChange
    it.onCommit = onCommit
}

fun Modifier.pillScrubItem(state: PillScrubState, tabId: Int): Modifier =
    onGloballyPositioned { state.registerItem(tabId, it) }

/**
 * Scroll-away visibility for the pill bar (production-upgrade round).
 * Pure transition rule, unit-tested: a downward scroll past the
 * threshold hides the bar, an upward scroll past it shows it again,
 * anything smaller leaves the state untouched (no twitching on
 * sub-threshold jitter). Thresholds are in pixels; callers convert
 * from dp (the spec sample's 8dp).
 */
fun scrollVisibilityAfter(isVisible: Boolean, deltaYPx: Float, thresholdPx: Float): Boolean =
    when {
        deltaYPx < -thresholdPx -> false
        deltaYPx > thresholdPx -> true
        else -> isVisible
    }

/**
 * Nested-scroll observer that drives [isVisible] from list scrolling.
 * Attach to the scaffold content only (never to sheet/detail
 * overlays, whose inner scrolls must not move the bar). It observes
 * without consuming (returns Offset.Zero), so scrolling behavior is
 * unchanged. Combined with the modal rule in HavenApp, the bar is
 * visible exactly when no modal is open AND scrolling has not hidden
 * it.
 */
class ScrollVisibilityState(private val thresholdPx: Float) {
    var isVisible by mutableStateOf(true)
        private set

    /** A fresh screen always starts with the pill visible (tab switches reset). */
    fun reset() {
        isVisible = true
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            isVisible = scrollVisibilityAfter(isVisible, available.y, thresholdPx)
            return Offset.Zero
        }
    }
}

fun Modifier.pillScrub(state: PillScrubState): Modifier = this
    .onGloballyPositioned { state.registerBar(it) }
    .pointerInput(state) {
        detectHorizontalDragGestures(
            onDragStart = { offset -> state.hover(state.tabAt(offset.x)) },
            onDragEnd = {
                val target = state.hoveredTab
                state.hover(null)
                if (target != null) state.onCommit(target)
            },
            onDragCancel = { state.hover(null) },
            onHorizontalDrag = { change, _ ->
                change.consume()
                val target = state.tabAt(change.position.x)
                if (target != null && target != state.hoveredTab) {
                    state.hover(target)
                    state.onHoverChange()
                }
            },
        )
    }
