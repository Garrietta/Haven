package com.garrietta.haven.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.store.HomeStore
import com.garrietta.haven.ui.entityIcon
import com.garrietta.haven.ui.stateText
import com.garrietta.haven.ui.theme.MorphPolygon
import com.garrietta.haven.ui.theme.havenSpring
import com.garrietta.haven.ui.theme.popupMorphSpring
import com.garrietta.haven.ui.theme.pressBounce
import com.garrietta.haven.ui.theme.stateShapePolygon

val LocalIsDark = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Tile colors for an entity: the domain palette when active, the quiet surface when not. */
@Composable
fun tileColors(e: HaEntity?): DomainCardColors {
    val dark = LocalIsDark.current
    val active = e != null && !e.isUnavailable && HomeStore.isActive(e)
    return (if (active) domainCardColors(e!!, dark) else null) ?: inactiveCardColors(dark)
}

/** The state badge: a polygon that morphs idle ↔ active on Haven's spring, with the glyph on top. */
@Composable
fun StateBadge(e: HaEntity, size: Dp = 40.dp, colors: DomainCardColors = tileColors(e)) {
    val progress by animateFloatAsState(
        badgeMorphProgress(e.domain, e.state, e.attrString("hvac_action")),
        havenSpring(), label = "badgeMorph",
    )
    val from = remember(e.domain) { stateShapePolygon(badgeIdleShape(e.domain)) }
    val to = remember(e.domain, e.state) { stateShapePolygon(badgeActiveShape(e.domain, e.state)) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        MorphPolygon(from, to, progress, colors.badgeBg, Modifier.fillMaxSize())
        Icon(entityIcon(e), null, tint = colors.badgeGlyph, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * The one tile style used everywhere. Tap = the entity's primary action
 * (toggle / run / open), long-press = its control popup. Colors cross-fade
 * with state; the tile presses in with Haven's bounce.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityTile(
    e: HaEntity,
    name: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    tempUnit: String = "°",
    subtitle: String? = null,
) {
    val colors = tileColors(e)
    val container by animateColorAsState(colors.container, label = "tileBg")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, pressBounce(), label = "tilePress")
    val haptics = LocalHapticFeedback.current
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); onTap() },
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
            )
            .padding(14.dp)
            .heightIn(min = 104.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        StateBadge(e, 40.dp, colors)
        Spacer(Modifier.size(10.dp))
        Text(
            name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            subtitle ?: stateText(e, tempUnit), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** A tonal pill chip (summary chips, filters). */
@Composable
fun HavenChip(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    container: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(container ?: if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh, label = "chip")
    Row(
        Modifier.clip(CircleShape).background(bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant)
            Spacer(Modifier.size(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) scheme.onPrimaryContainer else scheme.onSurface)
    }
}

/**
 * Centered popup card (not a bottom sheet): scrim + spring scale-in, tap the
 * scrim or drag the card down to dismiss. The body scrolls inside a card
 * capped at 460dp wide so it reads the same folded and unfolded.
 */
@Composable
fun PopupCard(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                .clickable(remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        )
    }
    AnimatedVisibility(
        visible,
        enter = scaleIn(popupMorphSpring(), initialScale = 0.88f) + fadeIn(),
        exit = scaleOut(targetScale = 0.92f) + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        var drag by remember { mutableFloatStateOf(0f) }
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()
                    .graphicsLayer { translationY = drag.coerceAtLeast(0f); alpha = 1f - (drag / 900f).coerceIn(0f, 0.5f) }
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { if (drag > 220f) onDismiss(); drag = 0f },
                            onDragCancel = { drag = 0f },
                        ) { _, dy -> drag = (drag + dy).coerceAtLeast(0f) }
                    },
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) { content() }
            }
        }
    }
}

/** A rounded tonal surface used for cards on Home / rooms. */
@Composable
fun HavenCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.clip(RoundedCornerShape(28.dp)).background(color)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        content = content,
    )
}
