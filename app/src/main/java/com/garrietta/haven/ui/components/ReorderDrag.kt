package com.garrietta.haven.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.garrietta.haven.ui.theme.settleSpring
import com.garrietta.haven.ui.theme.springSpatial
import kotlinx.coroutines.launch

/**
 * Round 24, item 6 (reworked round 25, item 7): drag-to-reorder
 * for Arrange mode, Android home-screen style. Attach to a card
 * cell's wrapper (inside its key(entityId) block, so the drag
 * state follows the entity across swaps). While [enabled] is false
 * the modifier is a no-op — normal browsing, taps, morphs, and the
 * resize handle are untouched.
 *
 * Gesture: a drag on the card body moves the card under the finger
 * (translation + a 4% lift, drawn above its neighbors at zIndex 10
 * with a 16dp shadow) while a subtle rounded placeholder outline
 * marks its current target slot. Every time the finger crosses half
 * a cell horizontally or half a row vertically, [onMove] fires once
 * with the step counts (±1 per axis per crossing) and the offset
 * re-bases by a full cell, so a long drag walks the card across the
 * grid one slot at a time and the grid re-lays-out live under the
 * finger. The caller converts steps to a list delta (horizontal =
 * ±1 slot, vertical = ±columns slots) and persists it through the
 * ViewModel move function, so the reorder survives a restart. On
 * release the card springs into its slot ([settleSpring]); the
 * remaining cards animate into their new slots with the spatial
 * spring via [slotSpringOffset]. The drag state is reported through
 * [onDragStateChange] so the wrapper can pause the slot animation
 * for the dragged card (its position is owned by the drag offset).
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
data class ReorderSteps(val dxSteps: Int, val dySteps: Int, val remaining: Offset)

/**
 * Round 25, item 7: the center-crossing step rule, extracted pure
 * so it is unit-tested. A step fires each time the drag offset
 * crosses half a cell horizontally or half a row vertically — that
 * is the dragged card's center crossing the neighboring slot's
 * center — and the offset re-bases by a full cell, so a long drag
 * walks the card one slot at a time. [stepX]/[stepY] are the half
 * steps (half a cell / half a row); the returned [ReorderSteps]
 * carries the fired steps plus the leftover offset after re-basing.
 */
fun reorderSteps(ox: Float, oy: Float, stepX: Float, stepY: Float): ReorderSteps {
    var x = ox
    var y = oy
    var dx = 0
    var dy = 0
    val sx = stepX.coerceAtLeast(1f)
    val sy = stepY.coerceAtLeast(1f)
    while (x >= sx) {
        dx += 1
        x -= sx * 2f
    }
    while (x <= -sx) {
        dx -= 1
        x += sx * 2f
    }
    while (y >= sy) {
        dy += 1
        y -= sy * 2f
    }
    while (y <= -sy) {
        dy -= 1
        y += sy * 2f
    }
    return ReorderSteps(dx, dy, Offset(x, y))
}

fun Modifier.reorderDrag(
    enabled: Boolean,
    rowHeightPx: Float,
    onDragStateChange: (Boolean) -> Unit = {},
    onMove: (dxSteps: Int, dySteps: Int) -> Unit,
): Modifier = composed {
    var cellSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val latestOnMove by rememberUpdatedState(onMove)
    val latestOnDragStateChange by rememberUpdatedState(onDragStateChange)
    // The displayed offset chases the finger instantly while
    // dragging (snap) and springs back into the slot on release.
    val displayOffset by animateOffsetAsState(
        targetValue = dragOffset,
        animationSpec = if (dragging) snap() else settleSpring(),
        label = "reorderOffset",
    )
    // Round 25, item 7: the placeholder outline — a subtle rounded
    // rect drawn UNDER the card in its current target slot while it
    // floats above it (zIndex 10, 1.04 lift, 16dp shadow). The
    // drawBehind sits OUTSIDE the graphicsLayer on purpose: the
    // drag translation must move the card, never the placeholder.
    val placeholderColor = MaterialTheme.colorScheme.outlineVariant
    Modifier
        .onSizeChanged { cellSize = it }
        .graphicsLayer {
            translationX = displayOffset.x
            translationY = displayOffset.y
            val lift = if (dragging) 1.04f else 1f
            scaleX = lift
            scaleY = lift
            shadowElevation = if (dragging) 16.dp.toPx() else 0f
        }
        .drawBehind {
            if (dragging) {
                drawRoundRect(
                    color = placeholderColor,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            }
        }
        .zIndex(if (dragging) 10f else 0f)
        .then(
            if (!enabled) Modifier
            else Modifier.pointerInput(enabled, rowHeightPx) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        latestOnDragStateChange(true)
                        dragOffset = Offset.Zero
                    },
                    onDragEnd = {
                        dragging = false
                        latestOnDragStateChange(false)
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        dragging = false
                        latestOnDragStateChange(false)
                        dragOffset = Offset.Zero
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val steps = reorderSteps(
                        dragOffset.x + dragAmount.x,
                        dragOffset.y + dragAmount.y,
                        cellSize.width / 2f,
                        rowHeightPx / 2f,
                    )
                    dragOffset = steps.remaining
                    if (steps.dxSteps != 0 || steps.dySteps != 0) latestOnMove(steps.dxSteps, steps.dySteps)
                }
            },
        )
}

/** Holder for the slot-spring's last observed position + slot key (plain vars, not snapshot state — onPlaced runs during layout). */
private class SlotSpringState {
    var lastPosition: Offset? = null
    var lastSlotKey: Any? = null
}

/**
 * Round 25, item 7: launcher-style slot animation for the cards
 * that are NOT being dragged. When a card's [slotKey] (its display
 * position) changes, its layout position jumps to the new slot;
 * this modifier animates it in from the old slot with the spatial
 * spring instead of letting it teleport, keeping the relative order
 * of all other cards intact. Scrolling never triggers it (the slot
 * key is unchanged), and the dragged card is excluded ([enabled] is
 * false while it is dragged — its position is owned by the drag
 * offset, not the slot animation). A null [slotKey] disables the
 * modifier entirely. The lazy tab grid uses it too (its slot key
 * is the card's index in `cards`); lazy item compositions are
 * keyed by entity id, so recycling disposes the state instead of
 * animating a stale position.
 */
fun Modifier.slotSpringOffset(slotKey: Any?, enabled: Boolean): Modifier = composed {
    if (slotKey == null) return@composed this
    val scope = rememberCoroutineScope()
    // Offset.VectorConverter does not exist in this Compose
    // version (1.8.2) — the converter is spelled out explicitly.
    val offsetAnim = remember {
        Animatable(
            Offset.Zero,
            TwoWayConverter({ AnimationVector2D(it.x, it.y) }, { Offset(it.v1, it.v2) }),
        )
    }
    val state = remember { SlotSpringState() }
    this
        .onPlaced { coords ->
            // localToRoot(Zero) is the 1.8.2 spelling — there is no
            // positionInRoot() on LayoutCoordinates in this version.
            val pos = coords.localToRoot(Offset.Zero)
            val prev = state.lastPosition
            val slotChanged = state.lastSlotKey != slotKey
            state.lastPosition = pos
            state.lastSlotKey = slotKey
            if (prev != null && prev != pos && slotChanged && enabled) {
                val from = prev - pos
                scope.launch {
                    offsetAnim.snapTo(from)
                    offsetAnim.animateTo(Offset.Zero, springSpatial())
                }
            }
            // No else branch on purpose: when the slot key is
            // unchanged (scrolling), the animatable is left alone —
            // snapping it to zero here would kill an in-flight slot
            // animation the moment the grid scrolls.
        }
        .graphicsLayer {
            translationX = offsetAnim.value.x
            translationY = offsetAnim.value.y
        }
}
