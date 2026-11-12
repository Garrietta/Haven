package com.garrietta.haven.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.garrietta.haven.DetailPopupTitleHeader
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.HeadlineAction
import com.garrietta.haven.data.HeadlineClause
import com.garrietta.haven.data.HomeHeadline
import com.garrietta.haven.data.HomeSubline
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.data.groupEntitiesByRoom
import com.garrietta.haven.data.isHomeRoomEntity
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.data.roomDigest
import com.garrietta.haven.data.switchIsActive
import com.garrietta.haven.data.tapToggles
import com.garrietta.haven.ui.components.CardTapRoute
import com.garrietta.haven.ui.components.cardTapRoute
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.leafCard

/*
 * Home as rooms: a sentence about the house, then one card per room.
 *
 * A room with something happening is a card in that state's color, with
 * a line saying what and the room's devices as round switches. A room
 * with nothing happening is a single quiet line, so the screen is as
 * tall as the house is busy. The words and the colors are decided in
 * data/RoomDigest.kt; this file only draws them.
 */

private const val STATE_PILL = "state"

/**
 * The sentence at the top of Home, a line for each thing the house is
 * doing. The last word of a line is its state, drawn as a pill in that
 * state's color, and the pill is a button: it puts that thing back to
 * rest. "on" turns those lights off, "open" closes the door. The line
 * under it gives the temperatures, and the inside one opens the
 * thermostat.
 */
@Composable
internal fun HomeHeadlineText(
    headline: HomeHeadline,
    subline: HomeSubline?,
    onAction: (HeadlineAction) -> Unit,
    onThermostat: (String) -> Unit,
) {
    val style = MaterialTheme.typography.headlineLarge
    val scheme = MaterialTheme.colorScheme
    headline.resting?.let { Text(it, style = style, color = scheme.onBackground) }
    for (clause in headline.clauses) HeadlineLine(clause, onAction)
    if (subline != null) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            subline.inside?.let { inside ->
                val id = subline.thermostatId
                Text(
                    inside,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textDecoration = if (id != null) TextDecoration.Underline else null,
                    modifier = if (id != null) {
                        Modifier.clickable(onClickLabel = "Open the thermostat", role = Role.Button) { onThermostat(id) }
                    } else {
                        Modifier
                    },
                )
            }
            subline.outside?.let { outside ->
                Text(
                    if (subline.inside != null) " · $outside" else outside,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeadlineLine(clause: HeadlineClause, onAction: (HeadlineAction) -> Unit) {
    val style = MaterialTheme.typography.headlineLarge
    val scheme = MaterialTheme.colorScheme
    val colors = clause.family.colors(isDarkBackdrop(scheme.background))
    // A calm state is a quiet pill; anything that is on, open or running
    // is drawn in the strong tone, so it reads as something to act on.
    val calm = clause.family == StateFamily.Secure
    val fill = if (calm) colors.container else colors.accent
    val ink = if (calm) colors.onContainer else colors.onAccent
    // The pill is as wide as its word plus a little air on each side.
    val measurer = rememberTextMeasurer()
    val wordWidth = remember(clause.state, style) { measurer.measure(clause.state, style, maxLines = 1).size.width }
    val width = with(LocalDensity.current) { (wordWidth.toDp() + 24.dp).toSp() }
    val text = buildAnnotatedString {
        append(clause.text)
        append(" ")
        appendInlineContent(STATE_PILL, clause.state)
        append(".")
    }
    val inline = mapOf(
        STATE_PILL to InlineTextContent(Placeholder(width, 1.2.em, PlaceholderVerticalAlign.TextCenter)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(fill)
                    .clickable(role = Role.Button) { onAction(clause.action) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    clause.state,
                    style = style,
                    color = ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.wrapContentHeight(unbounded = true),
                )
            }
        },
    )
    Text(text, style = style, color = scheme.onBackground, inlineContent = inline)
}

/**
 * One room on Home. [index] picks which corner of the card is the tight
 * one, so a stack of rooms does not read as a stack of identical boxes.
 * [resting] forces the single quiet line, for the "No room" entry, whose
 * contents are whatever has not been given a room rather than a place.
 */
@Composable
internal fun RoomCard(
    vm: HavenViewModel,
    room: RoomSection,
    states: Map<String, HaEntity>,
    index: Int,
    navigate: (Screen) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    resting: Boolean = false,
) {
    val members = room.members.mapNotNull(states::get)
    val digest = remember(room.name, members) { roomDigest(room.name, members) }
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkBackdrop(scheme.background)

    if (resting || digest.quiet) {
        val quiet = StateFamily.Quiet.colors(isDark)
        Row(
            modifier
                .fillMaxWidth()
                .reportCardBounds(roomPanelSourceId(room.areaId))
                .hideWhenMorphSource(roomPanelSourceId(room.areaId))
                .clip(MaterialTheme.shapes.extraLarge)
                .background(quiet.container)
                .clickable(onClickLabel = "Open ${room.name}", onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (resting) "Devices without a room" else digest.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = quiet.onContainer,
                )
            }
            digest.reading?.takeUnless { resting }?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = quiet.onContainer)
                Spacer(Modifier.width(4.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = quiet.onContainer)
        }
        return
    }

    val colors = digest.family.colors(isDark)
    Column(
        modifier
            .fillMaxWidth()
            .reportCardBounds(roomPanelSourceId(room.areaId))
            .hideWhenMorphSource(roomPanelSourceId(room.areaId))
            .clip(leafCard(index))
            .background(colors.container)
            .clickable(onClickLabel = "Open ${room.name}", onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                room.name,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            digest.reading?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = colors.onContainer)
            }
        }
        Text(
            digest.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (id in digest.switches) {
                val entity = states[id] ?: continue
                RoomSwitch(vm, entity, colors.onContainer, isDark, navigate)
            }
            if (digest.more > 0) {
                Box(
                    Modifier
                        .size(SWITCH_SIZE)
                        .clip(CircleShape)
                        .background(colors.onContainer.copy(alpha = RESTING_SWITCH_ALPHA))
                        .clickable(onClickLabel = "Show all of ${room.name}", onClick = onOpen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+${digest.more}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onContainer,
                    )
                }
            }
        }
    }
}

/** The id a room's card reports its place under, so its popup can grow out of it. */
internal fun roomPanelSourceId(areaId: String?): String = "room:${areaId ?: "unassigned"}"

/**
 * A room, opened from its card: its name, the line that says what is
 * happening in it, and every device it holds as a tile. A tile opens
 * its control on top of this popup, so closing the control comes back
 * here and closing this comes back to Home.
 */
@Composable
fun RoomPanelScreen(
    vm: HavenViewModel,
    areaId: String?,
    navigate: (Screen) -> Unit,
    onPanelDetail: ((String) -> Unit)? = null,
) {
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val lightOverrides = vm.settingsStore.homeLightOverrides()
    // The same members, in the same order, as the room has on Home.
    val members = remember(states.keys, entityAreas, lightOverrides, areaId) {
        groupEntitiesByRoom(vm.states.value.values.filter { isHomeRoomEntity(it) }, entityAreas)[areaId].orEmpty()
            .filter { it.domain != "light" || isLightShownOnHome(it, lightOverrides) }
            .sortedBy { it.friendlyName }
            .map { it.entityId }
    }
    val name = when (areaId) {
        null -> "No room"
        else -> vm.settingsStore.roomDisplayNames()[areaId] ?: areas.firstOrNull { it.id == areaId }?.name ?: "Room"
    }
    val digest = roomDigest(name, members.mapNotNull(states::get))
    Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        DetailPopupTitleHeader(name)
        Text(
            if (areaId == null) "Devices without a room" else digest.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        // Rows in a Column, not straight in the Box: a Box would stack them.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 12.dp) / 2
            Column {
                for ((rowIndex, pair) in members.chunked(2).withIndex()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for ((colIndex, id) in pair.withIndex()) {
                            key(id) {
                                Box(Modifier.width(cardWidth)) {
                                    HomeRoomCard(vm, id, navigate, rowIndex * 2 + colIndex) {
                                        if (onPanelDetail != null) onPanelDetail(id) else navigate(Screen.EntityDetail(id))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private val SWITCH_SIZE = 52.dp
private const val RESTING_SWITCH_ALPHA = 0.10f

/**
 * A device as a round switch on its room's card. Lit, it wears its own
 * state's color, so a playing TV is violet on a green living room; at
 * rest it is a tint of the card. A tap flips what a tap can safely flip
 * (lights, plugs, fans) and opens the control for everything else; a
 * long press always opens the control, which grows out of the switch.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomSwitch(
    vm: HavenViewModel,
    entity: HaEntity,
    onContainer: Color,
    isDark: Boolean,
    navigate: (Screen) -> Unit,
) {
    val lit = if (switchIsActive(entity)) domainCardColors(entity, isDark) else null
    val open: () -> Unit = {
        when (cardTapRoute(entity.domain, entity.entityId)) {
            CardTapRoute.CLIMATE_POPOUT -> vm.openClimatePopout(entity.entityId)
            CardTapRoute.VACUUM_PANEL -> navigate(Screen.VacuumPanel)
            CardTapRoute.ENTITY_DETAIL -> navigate(Screen.EntityDetail(entity.entityId))
        }
    }
    Box(
        Modifier
            .size(SWITCH_SIZE)
            .reportCardBounds(entity.entityId)
            .hideWhenMorphSource(entity.entityId)
            .clip(CircleShape)
            .background(lit?.badgeBg ?: onContainer.copy(alpha = RESTING_SWITCH_ALPHA))
            .combinedClickable(
                role = if (tapToggles(entity)) Role.Switch else Role.Button,
                onLongClickLabel = "Open controls",
                onLongClick = open,
                onClick = { if (tapToggles(entity)) vm.toggle(entity.entityId) else open() },
            )
            .semantics { contentDescription = "${entity.friendlyName}, ${entity.state}" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            domainIcon(entity),
            contentDescription = null,
            tint = lit?.badgeGlyph ?: onContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}
