package com.garrietta.haven.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.data.CardSize
import com.garrietta.haven.data.snapCardSize
import com.garrietta.haven.ui.theme.settleSpring
import androidx.compose.animation.animateContentSize

/**
 * Feedback round 20, item 5: the drag handle for resizable
 * cards. Rendered ONLY in a page's Arrange mode, never in
 * normal browsing, so it cannot hijack taps, morphs, or
 * popup drags. It sits at the card's bottom-end corner.
 *
 * Gesture ownership (the dial-thumb discipline): the grip
 * consumes every drag change, so the parent scroll never also
 * moves while resizing; taps on the grip are swallowed by a
 * no-op clickable so they never open the card's panel. The
 * drag snaps on release by vector (right = wide, left =
 * normal, down = tall, up = normal) with a live preview label
 * while dragging; the size change itself settles on the
 * established settle spring.
 */
@Composable
fun CardResizeHandle(
    current: CardSize,
    onSizeChange: (CardSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var drag by remember(current) { mutableStateOf<Offset?>(null) }
    val pending = drag?.let { snapCardSize(current, it.x, it.y) }
    Box(modifier.padding(4.dp)) {
        Column(horizontalAlignment = Alignment.End) {
            if (pending != null && pending != current) {
                Text(
                    when (pending) {
                        CardSize.WIDE -> "Wide"
                        CardSize.TALL -> "Tall"
                        CardSize.NORMAL -> "Normal"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { /* grip taps never open the panel */ }
                    .pointerInput(current) {
                        detectDragGestures(
                            onDragStart = { drag = Offset.Zero },
                            onDragEnd = {
                                val target = drag?.let { snapCardSize(current, it.x, it.y) } ?: current
                                drag = null
                                if (target != current) onSizeChange(target)
                            },
                            onDragCancel = { drag = null },
                        ) { change, dragAmount ->
                            change.consume()
                            drag = (drag ?: Offset.Zero) + dragAmount
                        }
                    },
            ) {
                val dot = MaterialTheme.colorScheme.onSurfaceVariant
                Canvas(Modifier.size(16.dp)) {
                    for (row in 0..1) {
                        for (col in 0..1) {
                            drawCircle(
                                dot,
                                radius = 2.dp.toPx(),
                                center = Offset(
                                    size.width * if (col == 0) 0.25f else 0.75f,
                                    size.height * if (row == 0) 0.25f else 0.75f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** animateContentSize with the resize settle, for card wrappers. */
fun Modifier.resizeSettle(): Modifier = animateContentSize(settleSpring())
