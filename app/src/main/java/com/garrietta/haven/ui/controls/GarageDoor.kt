package com.garrietta.haven.ui.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.theme.havenMotion

/**
 * Garage-door-shaped vertical control (customization round, item 5).
 *
 * The door is a slatted panel inside a frame. Tapping it toggles the
 * cover (vm.toggleCover: open if closed/closing, close if open/
 * opening — the same optimistic final-state rule as every other
 * cover control). The animation is a genuine upward transition:
 * the slatted panel translates UP out of the frame as the door
 * opens (open fraction 0 -> 1), and back down as it closes. The
 * fraction comes only from the reported position/state — never
 * invented motion. All progress is clamped to [0, 1] before any
 * geometry math (the standing no-negative-padding rule, applied to
 * offsets too).
 *
 * The Open/Close/Stop pills and the position slider stay alongside
 * this visual wherever it is used (panel, sheet, card) — the visual
 * replaces only the old abstract track/bar, never the behavior law:
 * Open/Close disabled+dimmed in the invalid direction, Stop always
 * enabled.
 */
@Composable
fun GarageDoorVisual(
    openFraction: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 150.dp,
    doorColor: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = LocalReduceMotion.current
    val animatedOpen by animateFloatAsState(
        openFraction.coerceIn(0f, 1f),
        animationSpec = havenMotion(reduceMotion),
        label = "garageDoorRise",
    )
    val open = animatedOpen.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier
            .size(width, height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onToggle() },
    ) {
        val frameHeight = maxHeight
        // Inner content is twice the frame: the slatted door (top
        // half) plus empty opening (bottom half). Sliding it up by
        // open * frameHeight makes the door rise out of the frame
        // and the opening grow from below — the garage-door motion.
        Column(
            Modifier
                .fillMaxWidth()
                .height(frameHeight * 2f)
                .offset(y = -(open * frameHeight.value).dp),
        ) {
            Column(Modifier.fillMaxWidth().height(frameHeight)) {
                repeat(6) {
                    Box(
                        Modifier.fillMaxWidth().weight(1f)
                            .background(doorColor),
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
            // Empty opening below the risen door.
            Spacer(Modifier.fillMaxWidth().height(frameHeight))
        }
        // Frame outline, drawn over the sliding content.
        // Feedback round 17, item 2: the frame is REAL now —
        // the old stub was an empty transparent Box, so at
        // openFraction 1f the risen door left only the flat
        // container background (the 2026-11-04 "empty dark blue
        // box"). The 2dp outline and the top header track stay
        // visible at every open fraction, so the control never
        // renders as an empty box when open.
        Box(
            Modifier.fillMaxWidth().height(frameHeight)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier.fillMaxWidth().height(10.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(5.dp)),
            )
        }
    }
}

/** Convenience: the door visual wired to a cover entity's reported position/state. */
@Composable
fun GarageDoorForEntity(
    entityId: String,
    openFraction: Float,
    vm: HavenViewModel,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 150.dp,
) {
    GarageDoorVisual(
        openFraction = openFraction,
        onToggle = { vm.toggleCover(entityId) },
        modifier = modifier,
        width = width,
        height = height,
    )
}
