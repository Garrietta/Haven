package com.garrietta.haven.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/*
 * The hand on a tile. One detector reads a whole touch and decides what
 * it was, the way a phone's home screen does:
 *
 *   a tap                      does what a tap on that tile does
 *   a slide across it          sets a light's brightness or a fan's speed
 *   press, hold, let go        the tile's options
 *   press, hold, then move     picks the tile up to move it within its group
 *
 * A touch that moves up or down before the hold is a scroll, and is left
 * to the list. Nothing here needs a handle, an arrow or an edit mode.
 */

/** What a touch on a tile turned out to be; the tile decides what each does. */
internal class TileHand(
    val tap: () -> Unit,
    val hold: () -> Unit,
    /** Null where the tile has no level to slide. */
    val slide: TileSlide?,
    val pickUp: () -> Unit,
    val move: (Offset) -> Unit,
    val drop: () -> Unit,
)

/** A level a finger slides across a tile: [start] when the slide begins, [by] as it moves (a fraction of the tile's width), [end] on release. */
internal class TileSlide(val start: () -> Unit, val by: (Float) -> Unit, val end: () -> Unit)

internal fun Modifier.tileGestures(key: Any, interaction: MutableInteractionSource, hand: () -> TileHand): Modifier =
    pointerInput(key) {
        val slop = viewConfiguration.touchSlop
        val holdTime = viewConfiguration.longPressTimeoutMillis
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val h = hand()
            val press = PressInteraction.Press(down.position)
            interaction.tryEmit(press)
            var released = false
            var sliding = false
            var travelled = Offset.Zero
            // Until the hold: a release is a tap, a sideways move on a tile
            // with a level is a slide, any other move is the list's scroll.
            val decided = withTimeoutOrNull(holdTime) {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                    if (!change.pressed) {
                        released = true
                        return@withTimeoutOrNull true
                    }
                    travelled += change.positionChange()
                    if (travelled.getDistance() > slop) {
                        sliding = h.slide != null && abs(travelled.x) > abs(travelled.y) * 1.5f
                        return@withTimeoutOrNull true
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                true
            } ?: false
            when {
                released -> {
                    interaction.tryEmit(PressInteraction.Release(press))
                    h.tap()
                }
                sliding -> {
                    interaction.tryEmit(PressInteraction.Cancel(press))
                    val slide = h.slide!!
                    slide.start()
                    slide.by(travelled.x / size.width)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dx = change.positionChange().x
                        change.consume()
                        slide.by(dx / size.width)
                    }
                    slide.end()
                }
                decided -> interaction.tryEmit(PressInteraction.Cancel(press))
                else -> {
                    // Held. Letting go without moving is the options; moving
                    // picks the tile up and carries it under the finger.
                    var moving = false
                    var moved = Offset.Zero
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        change.consume()
                        moved += delta
                        if (!moving && moved.getDistance() > slop) {
                            moving = true
                            interaction.tryEmit(PressInteraction.Cancel(press))
                            h.pickUp()
                            h.move(moved)
                        } else if (moving) {
                            h.move(delta)
                        }
                    }
                    if (moving) {
                        h.drop()
                    } else {
                        interaction.tryEmit(PressInteraction.Release(press))
                        h.hold()
                    }
                }
            }
        }
    }

/**
 * A tile being carried to a new place in its group: which one, how far
 * it has been carried, and the tile it is over, found from where every
 * tile in the group was last laid out.
 */
internal class TileCarry {
    var carried by mutableStateOf<String?>(null)
        private set
    var group: String? = null
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var over by mutableStateOf<String?>(null)
        private set
    private val bounds = mutableStateMapOf<String, Pair<String, Rect>>()

    fun place(id: String, group: String, rect: Rect) {
        if (id != carried) bounds[id] = group to rect
    }

    fun pickUp(id: String, group: String) {
        carried = id
        this.group = group
        offset = Offset.Zero
        over = null
    }

    fun move(delta: Offset) {
        val id = carried ?: return
        offset += delta
        val center = (bounds[id]?.second ?: return).center + offset
        over = bounds.entries.firstOrNull { (other, at) -> other != id && at.first == group && at.second.contains(center) }?.key
    }

    /** Puts the tile down: the carried id and the one it landed on, or null if it landed on none. */
    fun drop(): Pair<String, String>? {
        val id = carried
        val target = over
        carried = null
        over = null
        offset = Offset.Zero
        return if (id != null && target != null) id to target else null
    }
}
