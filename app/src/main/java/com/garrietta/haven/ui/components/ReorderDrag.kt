package com.garrietta.haven.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.garrietta.haven.ui.theme.settleSpring

/**
 * Round 24, item 6: drag-to-reorder for Arrange mode. Attach to a
 * card cell's wrapper (inside its key(entityId) block, so the drag
 * state follows the entity across swaps). While [enabled] is false
 * the modifier is a no-op — normal browsing, taps, morphs, and the
 * resize handle are untouched.
 *
 * Gesture: a drag on the card body moves the card under the finger
 * (translation + a 4% lift, drawn above its neighbors). Every time
 * the finger crosses half a cell horizontally or half a row
 * vertically, [onMove] fires once with the step counts (±1 per
 * axis per crossing) and the offset re-bases by a full cell, so a
 * long drag walks the card across the grid one slot at a time and
 * the grid re-lays-out live under the finger. The caller converts
 * steps to a list delta (horizontal = ±1 slot, vertical = ±columns
 * slots) and persists it through the ViewModel move function, so
 * the reorder survives a restart. On release the card springs
 * into its slot ([settleSpring]).
 *
 * Ownership: the drag consumes its own deltas, so the parent
 * scroll never also moves while reordering. The resize handle is
 * a child element with its own detector — innermost-first
 * arbitration means handle drags never start a reorder. The
 * card's long-press still toggles Arrange mode (round 21): a
 * press-and-hold-then-drag can exit Arrange mid-drag, so
 * reorder drags are prompt motions, not holds.
 *
 * [rowHeightPx] is one full row height including spacing
 * (136dp + spacing), so a vertical crossing is one visual row.
 * The horizontal step is half the cell's own measured width.
 */
fun Modifier.reorderDrag(
    enabled: Boolean,
    rowHeightPx: Float,
    onMove: (dxSteps: Int, dySteps: Int) -> Unit,
): Modifier = composed {
    var cellSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val latestOnMove by rememberUpdatedState(onMove)
    // The displayed offset chases the finger instantly while
    // dragging (snap) and springs back into the slot on release.
    val displayOffset by animateOffsetAsState(
        targetValue = dragOffset,
        animationSpec = if (dragging) snap() else settleSpring(),
        label = "reorderOffset",
    )
    Modifier
        .onSizeChanged { cellSize = it }
        .graphicsLayer {
            translationX = displayOffset.x
            translationY = displayOffset.y
            val lift = if (dragging) 1.04f else 1f
            scaleX = lift
            scaleY = lift
        }
        .zIndex(if (dragging) 1f else 0f)
        .then(
            if (!enabled) Modifier
            else Modifier.pointerInput(enabled, rowHeightPx) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        dragOffset = Offset.Zero
                    },
                    onDragEnd = {
                        dragging = false
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffset = Offset.Zero
                    },
                ) { change, dragAmount ->
                    change.consume()
                    var ox = dragOffset.x + dragAmount.x
                    var oy = dragOffset.y + dragAmount.y
                    val stepX = (cellSize.width / 2f).coerceAtLeast(1f)
                    val stepY = (rowHeightPx / 2f).coerceAtLeast(1f)
                    var dx = 0
                    var dy = 0
                    while (ox >= stepX) {
                        dx += 1
                        ox -= stepX * 2f
                    }
                    while (ox <= -stepX) {
                        dx -= 1
                        ox += stepX * 2f
                    }
                    while (oy >= stepY) {
                        dy += 1
                        oy -= stepY * 2f
                    }
                    while (oy <= -stepY) {
                        dy -= 1
                        oy += stepY * 2f
                    }
                    dragOffset = Offset(ox, oy)
                    if (dx != 0 || dy != 0) latestOnMove(dx, dy)
                }
            },
        )
}
