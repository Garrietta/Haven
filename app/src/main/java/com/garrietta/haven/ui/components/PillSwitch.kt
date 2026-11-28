package com.garrietta.haven.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.garrietta.haven.data.CardSwitchColors
import kotlin.math.roundToInt

/**
 * UI rewrite round §3: the dashboard card's M3 pill switch — 52dp
 * wide × 30dp tall, CircleShape track, 22dp circular thumb, haptic
 * tick on toggle. Colors come from the matrix ([CardSwitchColors]):
 * the track follows the entity's state row, the checked thumb uses
 * the checked state's card hex, the unchecked thumb is muted. The
 * thumb travel is 52 − 22 − 2×4 = 22dp, animated with progress
 * clamped to [0,1] before geometry math (never negative offset).
 * Detail panels keep the stock M3 Switch; this is the card switch.
 */
@Composable
fun PillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: CardSwitchColors,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.8f),
        label = "pillSwitchThumb",
    )
    val p = progress.coerceIn(0f, 1f)
    val track = if (checked) colors.trackChecked else colors.trackUnchecked
    val thumb = if (checked) colors.thumbChecked else colors.thumbUnchecked
    Box(
        modifier
            .size(52.dp, 30.dp)
            .background(track, CircleShape)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset {
                    with(density) { IntOffset((4.dp + 22.dp * p).toPx().roundToInt(), 0) }
                }
                .size(22.dp)
                .background(thumb, CircleShape),
        )
    }
}
