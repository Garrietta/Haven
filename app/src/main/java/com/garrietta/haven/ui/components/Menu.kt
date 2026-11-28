package com.garrietta.haven.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.FamilyColors
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.polygonShape

/*
 * Menus: Settings, and the option rows at the foot of a device popup.
 * A menu is a group of rows on one card, each row led by a polygon
 * badge. Tiles are the live things in the app and wear the strong half
 * of a family (deep badge, pastel icon); menu rows are the quiet things
 * and wear the other half (pastel badge, deep icon).
 */

/** The families a menu badge is drawn from. Alert and Quiet are left out: a settings row is neither. */
private val MENU_FAMILIES = listOf(
    StateFamily.On, StateFamily.Cool, StateFamily.Media, StateFamily.Caution,
    StateFamily.Water, StateFamily.Heat, StateFamily.Secure, StateFamily.Power,
)

/** A row's badge colors: one family per key, the same one every launch. */
fun menuBadgeColors(badgeKey: String, isDark: Boolean): FamilyColors =
    MENU_FAMILIES[Math.floorMod(badgeKey.hashCode(), MENU_FAMILIES.size)].colors(isDark)

/** The heading over a group of menu rows. */
@Composable
fun MenuSection(title: String, modifier: Modifier = Modifier) {
    SectionTitle(title, modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp))
}

/**
 * One menu row: badge, title, optional line under it, and either a
 * control ([trailing]) or a chevron when the row goes somewhere.
 * [flat] rows have no background of their own, for use in a [MenuGroup].
 */
@Composable
fun MenuRow(
    badgeKey: String,
    badgeKind: ExpressiveKind,
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    flat: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val badge = menuBadgeColors(badgeKey, isDarkBackdrop(scheme.background))
    val row = Modifier
        .fillMaxWidth()
        .then(if (flat) Modifier else Modifier.background(scheme.surfaceContainer, MaterialTheme.shapes.extraLarge))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 14.dp)
    Row(row.then(modifier), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).background(badge.container, polygonShape(badgeKind)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = badge.accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        val end: (@Composable () -> Unit)? = trailing ?: if (onClick != null) {
            { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant) }
        } else {
            null
        }
        TextWithTrailing(
            modifier = Modifier.weight(1f),
            text = {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            },
            trailing = end,
        )
    }
}

/**
 * Text with a control at its end, which moves under the text when the
 * two cannot share a line. A wide control (a stepper with a value in the
 * middle) on a narrow window would otherwise leave the title a column a
 * few characters wide; below [MIN_TEXT_WIDTH] of room it drops to its
 * own line, aligned to the end.
 */
@Composable
private fun TextWithTrailing(
    modifier: Modifier,
    text: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    Layout(
        modifier = modifier,
        content = {
            Box { text() }
            Box { trailing?.invoke() }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val end = measurables[1].measure(loose)
        val gap = if (end.width > 0) 12.dp.roundToPx() else 0
        val beside = constraints.maxWidth - end.width - gap
        if (end.width == 0 || beside >= MIN_TEXT_WIDTH.roundToPx()) {
            val body = measurables[0].measure(loose.copy(maxWidth = beside.coerceAtLeast(0)))
            val height = maxOf(body.height, end.height)
            layout(constraints.maxWidth, height) {
                body.placeRelative(0, (height - body.height) / 2)
                end.placeRelative(constraints.maxWidth - end.width, (height - end.height) / 2)
            }
        } else {
            val body = measurables[0].measure(loose)
            val spacing = 8.dp.roundToPx()
            layout(constraints.maxWidth, body.height + spacing + end.height) {
                body.placeRelative(0, 0)
                end.placeRelative(constraints.maxWidth - end.width, body.height + spacing)
            }
        }
    }
}

private val MIN_TEXT_WIDTH = 132.dp

/** A card holding flat [MenuRow]s, separated by [MenuDivider]s. */
@Composable
fun MenuGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = generalCardShape(0),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(content = content)
    }
}

/** A hairline between rows, starting where the row's text starts (padding 12 + badge 48 + gap 12). */
@Composable
fun MenuDivider(indent: Dp = 72.dp, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = indent),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
