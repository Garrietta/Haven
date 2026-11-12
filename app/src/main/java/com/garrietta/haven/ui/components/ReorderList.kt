package com.garrietta.haven.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.garrietta.haven.ui.theme.settleSpring

/*
 * Drag-to-reorder for lists of rows: a handle on each row, held and
 * dragged up or down. The row lifts and follows the finger, and its
 * neighbours step aside as its middle crosses theirs. The new order is
 * handed back once, when the finger lifts, so a drag is one write.
 *
 * Two shapes of list use it. Rows that are items of a LazyColumn (rooms,
 * tabs) go through [LazyListReorder], which reads where each row really
 * is from the list's layout, so rows of different heights reorder
 * correctly and the list scrolls when the row is held near an edge. Rows
 * of equal height inside one item (a tab's cards) go through
 * [ColumnReorder], which counts rows from the distance dragged.
 */

/** One row's place in the list: its key, top and height, in the list's pixels. */
data class RowSpan(val key: Any, val top: Int, val size: Int)

/**
 * The row the dragged row should take the place of: the first other
 * row, among [candidates], whose span holds the dragged row's middle.
 * [dragTop] is where the dragged row is drawn now. Null while the
 * middle is still over the row's own slot or over no reorderable row.
 */
fun reorderTarget(draggedKey: Any, dragTop: Float, dragSize: Int, candidates: List<RowSpan>): Any? {
    val middle = dragTop + dragSize / 2f
    return candidates.firstOrNull { it.key != draggedKey && middle >= it.top && middle < it.top + it.size }?.key
}

/** [list] with the element keyed [from] moved to where [to] is. Unchanged when either is missing. */
fun <T> moveKeyed(list: List<T>, from: T, to: T): List<T> {
    val i = list.indexOf(from)
    val j = list.indexOf(to)
    if (i < 0 || j < 0 || i == j) return list
    return list.toMutableList().apply { add(j, removeAt(i)) }
}

/**
 * How fast to scroll a list while a row is held near its edge, in px
 * per frame: zero inside the list, growing with how far the row has
 * gone past an edge, and capped so a flick does not race away.
 */
fun edgeScrollSpeed(rowTop: Float, rowBottom: Float, viewStart: Int, viewEnd: Int, maxPerFrame: Float): Float = when {
    rowBottom > viewEnd -> ((rowBottom - viewEnd) / 4f).coerceAtMost(maxPerFrame)
    rowTop < viewStart -> ((rowTop - viewStart) / 4f).coerceAtLeast(-maxPerFrame)
    else -> 0f
}

/**
 * The drag state of one LazyColumn's reorderable rows. [canSwap] says
 * which keys the dragged row may trade places with (rooms only with
 * rooms); [onMove] moves the row in the caller's working order as it
 * goes; [onDrop] commits that order when the finger lifts.
 */
class LazyListReorder internal constructor(
    val listState: LazyListState,
    private val haptics: HapticFeedback,
) {
    internal var canSwap: (Any, Any) -> Boolean = { _, _ -> false }
    internal var onMove: (Any, Any) -> Unit = { _, _ -> }
    internal var onDrop: () -> Unit = {}

    /** The key of the row being dragged, or null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    private var dragged by mutableFloatStateOf(0f)
    private var startTop by mutableIntStateOf(0)

    // The list lays out a move on the next frame. Until it has, its
    // layout still shows the row in its old slot, and reading it would
    // swap the row straight back; this holds that old index.
    private var awaitingLayoutAt: Int? = null

    private fun info(key: Any?) = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** How far the dragged row is drawn from where the list has laid it out. */
    fun offsetOf(key: Any): Float {
        if (key != draggingKey) return 0f
        val item = info(key) ?: return 0f
        return startTop + dragged - item.offset
    }

    internal fun start(key: Any) {
        val item = info(key) ?: return
        draggingKey = key
        startTop = item.offset
        dragged = 0f
        awaitingLayoutAt = null
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    internal fun drag(dy: Float) {
        dragged += dy
        swapIfCrossed()
    }

    /** Scrolls by the edge speed for this frame; true while there is somewhere to go. */
    internal suspend fun edgeScroll(): Boolean {
        val key = draggingKey ?: return false
        val item = info(key) ?: return false
        val top = item.offset + offsetOf(key)
        val layout = listState.layoutInfo
        val speed = edgeScrollSpeed(top, top + item.size, layout.viewportStartOffset, layout.viewportEndOffset, 24f)
        if (speed == 0f) return false
        // The row is drawn at its start plus the drag, in the list's
        // window, so it stays under a still finger while the rows scroll
        // past it and it takes their places as its middle crosses them.
        val moved = listState.scrollBy(speed)
        swapIfCrossed()
        return moved != 0f
    }

    private fun swapIfCrossed() {
        val key = draggingKey ?: return
        val item = info(key) ?: return
        if (awaitingLayoutAt == item.index) return
        awaitingLayoutAt = null
        val spans = listState.layoutInfo.visibleItemsInfo
            .filter { it.key == key || canSwap(key, it.key) }
            .map { RowSpan(it.key, it.offset, it.size) }
        val target = reorderTarget(key, item.offset + offsetOf(key), item.size, spans) ?: return
        awaitingLayoutAt = item.index
        onMove(key, target)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    internal fun end() {
        if (draggingKey == null) return
        draggingKey = null
        dragged = 0f
        onDrop()
    }
}

@Composable
fun rememberLazyListReorder(
    listState: LazyListState,
    canSwap: (Any, Any) -> Boolean,
    onMove: (Any, Any) -> Unit,
    onDrop: () -> Unit,
): LazyListReorder {
    val haptics = LocalHapticFeedback.current
    val reorder = remember(listState) { LazyListReorder(listState, haptics) }
    reorder.canSwap = canSwap
    reorder.onMove = onMove
    reorder.onDrop = onDrop
    // While a row is held near an edge the list keeps scrolling, a frame
    // at a time, without waiting for the finger to move.
    LaunchedEffect(reorder.draggingKey) {
        if (reorder.draggingKey == null) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            reorder.edgeScroll()
        }
    }
    return reorder
}

/**
 * The row being reordered: lifted and drawn under the finger while it
 * is dragged. The caller adds `animateItem()` for rows that are not
 * being dragged, so they step aside on the list's own placement motion.
 */
fun Modifier.reorderRow(reorder: LazyListReorder, key: Any): Modifier {
    val dragging = reorder.draggingKey == key
    return this
        .zIndex(if (dragging) 10f else 0f)
        .graphicsLayer {
            translationY = reorder.offsetOf(key)
            val lift = if (dragging) 1.02f else 1f
            scaleX = lift
            scaleY = lift
            shadowElevation = if (dragging) 12.dp.toPx() else 0f
        }
}

/**
 * The grip a row is dragged by. [label] names the row for TalkBack,
 * which gets Move up and Move down actions in place of the drag.
 */
@Composable
fun ReorderHandle(
    reorder: LazyListReorder,
    key: Any,
    label: String,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestKey by rememberUpdatedState(key)
    Icon(
        Icons.Filled.DragIndicator,
        contentDescription = "Reorder $label",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .pointerInput(reorder) {
                detectVerticalDragGestures(
                    onDragStart = { reorder.start(latestKey) },
                    onDragEnd = { reorder.end() },
                    onDragCancel = { reorder.end() },
                ) { change, dy ->
                    change.consume()
                    reorder.drag(dy)
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Move up") { onStep(-1); true },
                    CustomAccessibilityAction("Move down") { onStep(1); true },
                )
            },
    )
}

/**
 * Reordering for rows of equal height laid out in a plain Column: the
 * row index changes by one each time the finger crosses half a row,
 * and [onDrop] receives the from and to indices when it lifts.
 */
class ColumnReorder internal constructor(private val haptics: HapticFeedback) {
    internal var onDrop: (Int, Int) -> Unit = { _, _ -> }

    /** The index the dragged row started at, or null. */
    var from by mutableStateOf<Int?>(null)
        private set

    /** The index the dragged row would land on now. */
    var target by mutableIntStateOf(0)
        private set
    private var dragged by mutableFloatStateOf(0f)
    internal var rowHeight = 0
    internal var count = 0

    internal fun start(index: Int) {
        from = index
        target = index
        dragged = 0f
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    internal fun drag(dy: Float) {
        val start = from ?: return
        dragged += dy
        val next = columnTarget(start, dragged, rowHeight, count)
        if (next != target) {
            target = next
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    internal fun end() {
        val start = from ?: return
        from = null
        dragged = 0f
        if (target != start) onDrop(start, target)
    }

    /**
     * How far row [index] is drawn from its slot: the dragged row under
     * the finger, the rows it has passed one row the other way.
     */
    fun offsetOf(index: Int): Float {
        val start = from ?: return 0f
        if (index == start) return dragged
        return when {
            start < target && index in (start + 1)..target -> -rowHeight.toFloat()
            start > target && index in target until start -> rowHeight.toFloat()
            else -> 0f
        }
    }
}

/** The index a row that started at [start] lands on after a drag of [draggedPx], rows [rowHeight] tall, [count] of them. */
fun columnTarget(start: Int, draggedPx: Float, rowHeight: Int, count: Int): Int {
    if (rowHeight <= 0 || count <= 0) return start
    val rows = kotlin.math.round(draggedPx / rowHeight).toInt()
    return (start + rows).coerceIn(0, count - 1)
}

@Composable
fun rememberColumnReorder(count: Int, onDrop: (Int, Int) -> Unit): ColumnReorder {
    val haptics = LocalHapticFeedback.current
    val reorder = remember { ColumnReorder(haptics) }
    reorder.onDrop = onDrop
    reorder.count = count
    return reorder
}

/** A row of a [ColumnReorder]: measured, lifted while dragged, stepping aside while passed. */
@Composable
fun Modifier.columnReorderRow(reorder: ColumnReorder, index: Int): Modifier {
    val dragging = reorder.from == index
    // The rows being passed glide; the dragged row follows the finger exactly.
    val glide by animateFloatAsState(
        if (dragging) 0f else reorder.offsetOf(index),
        settleSpring(),
        label = "row step",
    )
    return this
        .onSizeChanged { reorder.rowHeight = it.height }
        .zIndex(if (dragging) 10f else 0f)
        .graphicsLayer {
            translationY = if (dragging) reorder.offsetOf(index) else glide
            shadowElevation = if (dragging) 8.dp.toPx() else 0f
        }
}

/** The grip of a [ColumnReorder] row. */
@Composable
fun ColumnReorderHandle(
    reorder: ColumnReorder,
    index: Int,
    label: String,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestIndex by rememberUpdatedState(index)
    Icon(
        Icons.Filled.DragIndicator,
        contentDescription = "Reorder $label",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .pointerInput(reorder) {
                detectVerticalDragGestures(
                    onDragStart = { reorder.start(latestIndex) },
                    onDragEnd = { reorder.end() },
                    onDragCancel = { reorder.end() },
                ) { change, dy ->
                    change.consume()
                    reorder.drag(dy)
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Move up") { onStep(-1); true },
                    CustomAccessibilityAction("Move down") { onStep(1); true },
                )
            },
    )
}
