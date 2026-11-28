package com.garrietta.haven.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.ui.theme.kodaBarSpec

data class PillItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
    val badged: Boolean = false,
)

/**
 * Haven's pill bar: an opaque floating island ([PillBarContainer]) whose
 * selected destination is a lime capsule ([PillBarActiveCapsule]) holding
 * the filled icon + bold label; unselected destinations are outlined 22dp
 * glyphs. Selection animates Koda-style — item padding 12↔20dp and the
 * label expanding/fading on [kodaBarSpec], colors cross-fading on tween 200.
 * Dragging along the bar scrubs the highlight with a haptic tick per tab
 * and commits on release.
 */
@Composable
fun PillBar(
    items: List<PillItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val selectedIndex = items.indexOfFirst { it.key == selectedKey }
    val scrub = rememberPillScrubState(
        tabIds = items.indices.toList(),
        onHoverChange = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
        onCommit = { target ->
            if (target != selectedIndex && target in items.indices) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onSelect(items[target].key)
            }
        },
    )
    Surface(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(32.dp),
        color = PillBarContainer,
        shadowElevation = 6.dp,
    ) {
        val content: @Composable () -> Unit = {
            items.forEachIndexed { index, item ->
                val highlighted = (scrub.hoveredTab ?: selectedIndex) == index
                PillBarItem(
                    item = item,
                    highlighted = highlighted,
                    showLabel = highlighted && !vertical,
                    modifier = Modifier.pillScrubItem(scrub, index),
                    onTap = {
                        if (item.key != selectedKey) {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onSelect(item.key)
                        }
                    },
                )
            }
        }
        if (vertical) {
            Column(
                Modifier.padding(horizontal = 10.dp, vertical = 14.dp).pillScrubVertical(scrub),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { content() }
        } else {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp).pillScrub(scrub),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { content() }
        }
    }
}

@Composable
private fun PillBarItem(
    item: PillItem,
    highlighted: Boolean,
    showLabel: Boolean,
    modifier: Modifier,
    onTap: () -> Unit,
    height: Dp = 44.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val padding by animateDpAsState(if (highlighted) 20.dp else 12.dp, kodaBarSpec(), label = "pillPad")
    val capsule by animateColorAsState(if (highlighted) PillBarActiveCapsule else Color.Transparent, tween(200), label = "pillCapsule")
    val glyph by animateColorAsState(
        if (highlighted) PillBarActiveGlyph else pillUnselectedGlyph(scheme, isDarkBackdrop(scheme.background)),
        tween(200), label = "pillGlyph",
    )
    Row(
        modifier
            .height(height)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onTap() }
            .background(capsule, CircleShape)
            .padding(horizontal = padding)
            .animateContentSize(kodaBarSpec()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Icon(if (highlighted) item.icon else item.outlinedIcon, item.label, tint = glyph, modifier = Modifier.size(22.dp))
            if (item.badged) {
                Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(alertDotColor(!highlighted), CircleShape))
            }
        }
        AnimatedVisibility(
            visible = showLabel,
            enter = fadeIn() + expandHorizontally(kodaBarSpec()),
            exit = fadeOut() + shrinkHorizontally(kodaBarSpec()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    item.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PillBarActiveGlyph,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
