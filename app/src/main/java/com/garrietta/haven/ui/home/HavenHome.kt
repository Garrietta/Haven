package com.garrietta.haven.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.DeviceFacts
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.HomePlace
import com.garrietta.haven.data.SECURITY_CAMERAS
import com.garrietta.haven.data.StatusCard
import com.garrietta.haven.data.StatusKind
import com.garrietta.haven.data.GROUP_GREENHOUSE
import com.garrietta.haven.data.isLeftover
import com.garrietta.haven.data.climateLine
import com.garrietta.haven.data.curated
import com.garrietta.haven.data.homeGroups
import com.garrietta.haven.data.homeStatus
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.data.isWideTile
import com.garrietta.haven.data.peopleHome
import com.garrietta.haven.data.placeLine
import com.garrietta.haven.data.shortName
import com.garrietta.haven.data.tapToggles
import com.garrietta.haven.data.tileActive
import com.garrietta.haven.data.tileState
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.HavenLockup
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.formatCondition
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.controls.entityIcon
import com.garrietta.haven.ui.screens.CameraQuadrant
import com.garrietta.haven.ui.screens.rememberCardCustom
import com.garrietta.haven.ui.theme.Corner
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.leafCard
import com.garrietta.haven.ui.theme.pressBounce
import org.json.JSONObject

/*
 * Home (the 2026-11 redesign). It is organized the way the house is
 * lived in, top to bottom:
 *
 *  1. The house at a glance: the brand, the weather and who is home.
 *  2. The status deck: one card per kind of thing the house has
 *     (climate, security, lights, the garage, the greenhouse, the
 *     vacuum, TV and audio), each saying how it is in a word or two and
 *     colored only when something is running or wants a look. A card
 *     opens its page.
 *  3. The cameras, as a carousel of their pictures.
 *  4. The devices, in groups by what they are (climate, lights, doors,
 *     the greenhouse, plugs, the vacuum, TV and audio), each group a
 *     heading (with what is on) over its devices as tiles. A tile is the device: a tap does
 *     what a tap can safely do (lights and plugs flip; a garage door, a
 *     lock or a thermostat opens its control), a long press opens the
 *     device. The thermostat's tile is wide and takes − and + directly.
 *
 * Unfolded, the overview (1-3) takes the left pane and the places the
 * right. What is shown and how it is grouped is decided in
 * data/HomeLayout.kt; this file only draws it.
 */

@Composable
fun HavenHome(vm: HavenViewModel, navigate: (Screen) -> Unit, openTab: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val auxiliary by vm.auxiliaryEntities.collectAsState()
    val lightOverrides = vm.settingsStore.homeLightOverrides()
    // Home goes by the devices alone (not Home Assistant's areas), and
    // leaves out leftovers: what has not answered for a week.
    val now = System.currentTimeMillis()
    val places = remember(states, entityDevices, deviceInfo, auxiliary, lightOverrides) {
        homeGroups(
            states, entityDevices,
            deviceInfo.mapValues { (_, d) -> DeviceFacts(d.name, d.model, d.service) },
            auxiliary,
            nowMillis = now,
            shownOnHome = { com.garrietta.haven.data.isShownOnHome(it, lightOverrides) },
        )
    }
    val nameOf = com.garrietta.haven.ui.screens.rememberEntityNamer(vm)
    val status = remember(states, places, nameOf) { homeStatus(states, places, now, nameOf) }
    val cameraIds = curated(vm.securityCameraIds(states), vm.curation(SECURITY_CAMERAS)) { id ->
        states[id]?.let { !isLeftover(it, now) } == true
    }
    val weather = states.values.firstOrNull { it.domain == "weather" }
    val people = remember(states) { peopleHome(states.values.filter { it.domain == "device_tracker" }) }
    val connection by vm.connection.collectAsState()
    val connectionLine: @Composable () -> Unit = {
        com.garrietta.haven.ui.screens.ConnectionLine(connection, states.isNotEmpty()) { vm.reconnectNow() }
    }
    var sheet by rememberSaveable { mutableStateOf<StatusKind?>(null) }

    val onStatus: (StatusCard) -> Unit = { card ->
        when (card.kind) {
            StatusKind.Climate -> openTab(Screen.Climate)
            StatusKind.Security -> openTab(Screen.Security)
            StatusKind.Garage -> card.entityIds.singleOrNull()?.let { navigate(Screen.EntityDetail(it)) } ?: run { sheet = card.kind }
            StatusKind.Greenhouse -> navigate(Screen.GreenhousePanel)
            StatusKind.Vacuum -> navigate(Screen.VacuumPanel)
            StatusKind.Lights, StatusKind.Media -> sheet = card.kind
        }
    }
    val actions = TileActions(vm, navigate, nameOf)

    if (LocalWideLayout.current) {
        Row(Modifier.fillMaxSize().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            Column(
                Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(end = 20.dp),
            ) {
                HomeHeader(weather, people)
                connectionLine()
                Spacer(Modifier.height(20.dp))
                StatusGrid(status, onStatus)
                if (cameraIds.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Cameras")
                    Spacer(Modifier.height(8.dp))
                    for (id in cameraIds) {
                        CameraQuadrant(vm, id, Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(bottom = 8.dp), RoundedCornerShape(Corner.lg))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(0.58f).fillMaxHeight()) {
                // As many columns as keep a tile at least 180dp wide.
                val columns = ((maxWidth + 8.dp) / (180.dp + 8.dp)).toInt().coerceIn(2, 4)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    places(places, states, columns = columns, actions = actions, navigate = navigate)
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 112.dp)) {
            item(key = "header") {
                HomeHeader(weather, people, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
                Box(Modifier.padding(horizontal = 20.dp)) { connectionLine() }
                Spacer(Modifier.height(16.dp))
            }
            item(key = "status") {
                StatusRow(status, onStatus)
            }
            if (cameraIds.isNotEmpty()) {
                item(key = "cameras") {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Cameras", Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(8.dp))
                    CameraCarousel(vm, cameraIds)
                }
            }
            places(places, states, columns = 2, actions = actions, navigate = navigate, horizontal = 16.dp)
        }
    }

    sheet?.let { kind ->
        val ids = status.firstOrNull { it.kind == kind }?.entityIds.orEmpty()
        CategorySheet(
            title = status.firstOrNull { it.kind == kind }?.title.orEmpty(),
            ids = ids,
            states = states,
            actions = actions,
            onDismiss = { sheet = null },
        )
    }
}

/** What a tile does when tapped, held, or nudged. */
private class TileActions(val vm: HavenViewModel, val navigate: (Screen) -> Unit, val nameOf: (HaEntity) -> String) {
    fun tap(entity: HaEntity) = when {
        tapToggles(entity) -> vm.toggle(entity.entityId)
        entity.domain == "vacuum" -> navigate(Screen.VacuumPanel)
        entity.domain == "media_player" -> navigate(Screen.MediaPanel(entity.entityId))
        else -> navigate(Screen.EntityDetail(entity.entityId))
    }

    fun open(entity: HaEntity) = navigate(Screen.EntityDetail(entity.entityId))

    /** Press and hold: the tile's options (color, icon, name). */
    fun options(entity: HaEntity) = vm.openCardSettings(entity.entityId)

    /** Moves a thermostat's set point by [delta] degrees, or both ends of its range in heat-and-cool. */
    fun nudge(entity: HaEntity, delta: Int) {
        val data = JSONObject()
        val low = entity.attrDouble("target_temp_low")
        val high = entity.attrDouble("target_temp_high")
        val single = entity.attrDouble("temperature")
        when {
            single != null -> data.put("temperature", single + delta)
            low != null && high != null -> data.put("target_temp_low", low + delta).put("target_temp_high", high + delta)
            else -> return
        }
        vm.callService("climate", "set_temperature", entity.entityId, data)
    }
}

// ---- The house at a glance ----

@Composable
private fun HomeHeader(weather: HaEntity?, people: List<String>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HavenLockup(Modifier.weight(1f), markSize = 30.dp)
            if (weather != null) {
                val temp = weather.attrDouble("temperature")?.let { "${Math.round(it)}°" }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = if (people.isEmpty()) 0.dp else 12.dp)) {
                    Text(temp ?: "", style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
                    Text(formatCondition(weather.state), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
            // Who is home: an initial each, overlapping like a little group.
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                people.take(4).forEachIndexed { index, name ->
                    // People are not a state: their initials take the color style's own accents.
                    val (fill, ink) = if (index % 2 == 0) scheme.primary to scheme.onPrimary else scheme.tertiary to scheme.onTertiary
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(scheme.background).padding(2.dp)
                            .clip(CircleShape).background(fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.take(1), style = MaterialTheme.typography.labelLarge, color = ink)
                    }
                }
            }
        }
        if (people.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                when (people.size) {
                    1 -> "${people[0]} is home"
                    2 -> "${people[0]} and ${people[1]} are home"
                    else -> "${people.size} people are home"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

// ---- The status deck ----

private fun statusIcon(kind: StatusKind): ImageVector = when (kind) {
    StatusKind.Climate -> Icons.Rounded.Thermostat
    StatusKind.Security -> Icons.Rounded.Security
    StatusKind.Lights -> Icons.Rounded.Lightbulb
    StatusKind.Garage -> Icons.Rounded.Garage
    StatusKind.Greenhouse -> Icons.Rounded.Grass
    StatusKind.Vacuum -> Icons.Rounded.CleaningServices
    StatusKind.Media -> Icons.Rounded.Tv
}

@Composable
private fun StatusRow(cards: List<StatusCard>, onCard: (StatusCard) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(cards, key = { it.kind }) { card -> StatusCardView(card, Modifier.width(136.dp)) { onCard(card) } }
    }
}

@Composable
private fun StatusGrid(cards: List<StatusCard>, onCard: (StatusCard) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in cards.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (card in row) StatusCardView(card, Modifier.weight(1f)) { onCard(card) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * A status card: its icon in a badge, what it is about, and how it is, the
 * words under the badge so a narrow card still says them whole. Quiet at
 * rest, its family's color when something is on or wants a look.
 */
@Composable
private fun StatusCardView(card: StatusCard, modifier: Modifier, onClick: () -> Unit) {
    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val colors = card.family.colors(isDark)
    val container by animateColorAsState(colors.container, label = "statusContainer")
    val ink = colors.onContainer
    Column(
        modifier
            .height(124.dp)
            .clip(RoundedCornerShape(Corner.xl))
            .background(container)
            .clickable(onClickLabel = card.title, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // A badge is a shape, never a box: each kind of card wears its own.
        Box(
            Modifier.size(40.dp).clip(statusBadgeShape(card.kind)).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(statusIcon(card.kind), contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(card.title, style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.8f), maxLines = 1)
            Text(card.value, style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Each kind of status card's badge, one of Material's expressive shapes. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun statusBadgeShape(kind: StatusKind): androidx.compose.ui.graphics.Shape {
    val polygon = when (kind) {
        StatusKind.Climate -> androidx.compose.material3.MaterialShapes.Cookie9Sided
        StatusKind.Security -> androidx.compose.material3.MaterialShapes.Gem
        StatusKind.Lights -> androidx.compose.material3.MaterialShapes.Sunny
        StatusKind.Garage -> androidx.compose.material3.MaterialShapes.Square
        StatusKind.Greenhouse -> androidx.compose.material3.MaterialShapes.Clover4Leaf
        StatusKind.Vacuum -> androidx.compose.material3.MaterialShapes.Circle
        StatusKind.Media -> androidx.compose.material3.MaterialShapes.Cookie4Sided
    }
    return polygon.toShape()
}

// ---- Cameras ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraCarousel(vm: HavenViewModel, ids: List<String>) {
    val state = rememberCarouselState { ids.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 260.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(170.dp),
    ) { index ->
        CameraQuadrant(
            vm, ids[index], Modifier.fillMaxSize().maskClip(RoundedCornerShape(Corner.lg)),
            visibleWindow = { carouselItemDrawInfo.maskRect },
        )
    }
}

// ---- Places ----

private fun LazyListScope.places(
    places: List<HomePlace>,
    states: Map<String, HaEntity>,
    columns: Int,
    actions: TileActions,
    navigate: (Screen) -> Unit,
    horizontal: Dp = 0.dp,
) {
    for (place in places) {
        item(key = "place-" + place.id) {
            PlaceHeading(place, states, navigate, actions.nameOf, Modifier.padding(horizontal = horizontal + 4.dp).padding(top = 24.dp, bottom = 10.dp))
        }
        // Rows of tiles; a wide tile takes a row of its own.
        val rows = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (id in place.entityIds) {
            val entity = states[id] ?: continue
            if (isWideTile(entity)) {
                if (current.isNotEmpty()) { rows += current; current = mutableListOf() }
                rows += listOf(id)
            } else {
                current += id
                if (current.size == columns) { rows += current; current = mutableListOf() }
            }
        }
        if (current.isNotEmpty()) rows += current
        rows.forEachIndexed { r, row ->
            item(key = "row-" + place.id + "-" + row.first()) {
                // A row takes its tallest tile's height, so a name on two
                // lines grows the row instead of pushing the state off the tile.
                Row(
                    Modifier.padding(horizontal = horizontal).padding(bottom = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for ((c, id) in row.withIndex()) {
                        val entity = states[id] ?: continue
                        if (isWideTile(entity)) {
                            ThermostatTile(entity, actions, Modifier.weight(1f), r * columns + c)
                        } else {
                            DeviceTile(entity, place, actions, Modifier.weight(1f), r * columns + c)
                        }
                    }
                    val wide = row.size == 1 && states[row[0]]?.let(::isWideTile) == true
                    if (!wide) repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PlaceHeading(place: HomePlace, states: Map<String, HaEntity>, navigate: (Screen) -> Unit, nameOf: (HaEntity) -> String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    // The greenhouse has a page of its own (automations, watering).
    val opens: (() -> Unit)? = when (place.name) {
        GROUP_GREENHOUSE -> ({ navigate(Screen.GreenhousePanel) })
        com.garrietta.haven.data.GROUP_VACUUM -> ({ navigate(Screen.VacuumPanel) })
        else -> null
    }
    Row(
        modifier.fillMaxWidth().then(if (opens != null) Modifier.clickable(onClickLabel = "Open ${place.name}", onClick = opens) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleLarge, color = scheme.onBackground)
            val line = placeLine(place, states, nameOf)
            if (line.isNotEmpty()) {
                Text(line, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (opens != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

/**
 * A device as a tile: its badge, its name, what it is doing. At rest it
 * sits quietly on the page; switched on, open or playing it takes its
 * family's container, or the color its owner gave it. A tap does what a
 * tap can safely do, and a light or plug's tile has a small button in the
 * corner for its full controls; press and hold for the tile's options
 * (color, icon, name). A press springs it down a little.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceTile(entity: HaEntity, place: HomePlace, actions: TileActions, modifier: Modifier, index: Int) {
    val vm = actions.vm
    val scheme = MaterialTheme.colorScheme
    val custom = rememberCardCustom(vm)
    val isDark = isDarkBackdrop(scheme.background)
    val haptics = LocalHapticFeedback.current
    val active = tileActive(entity)
    val preset = com.garrietta.haven.data.tilePresetFor(custom.tilePresets, entity.entityId, custom.domainTilePresets, entity.domain)
    val family = when {
        !active -> StateFamily.Quiet
        preset != null -> preset.family
        else -> com.garrietta.haven.data.domainFamily(entity) ?: StateFamily.On
    }
    val colors = family.colors(isDark)
    val container by animateColorAsState(if (active) colors.container else scheme.surfaceContainer, label = "tileContainer")
    val ink = if (active) colors.onContainer else scheme.onSurface
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, pressBounce(), label = "tilePress")
    val name = custom.titles[entity.entityId]?.takeIf { it.isNotBlank() } ?: shortName(actions.nameOf(entity), place.name)
    val accent = accentFor(entity.entityId)
    val badge = com.garrietta.haven.ui.components.tileBadgePair(custom.tilePresets, custom.domainTilePresets, entity, isDark, scheme, accent)
    Box(
        modifier
            .heightIn(min = 124.dp).fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(leafCard(index))
            .background(container)
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClickLabel = name,
                onLongClickLabel = "Options for $name",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    actions.options(entity)
                },
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    actions.tap(entity)
                },
            ),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            DomainBadge(entity, entityIcon(custom.iconKeys, entity), accent, Modifier.size(42.dp), badgeOverride = badge)
            Column {
                Text(name, style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(tileState(entity), style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (tapToggles(entity)) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Open $name", role = Role.Button) { actions.open(entity) }
                    .semantics { contentDescription = "Open $name" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * The thermostat, two tiles wide: the temperature inside in large type,
 * what it is doing, and − and + that move the set point (both ends of the
 * range in heat-and-cool). A tap anywhere else opens the full control.
 */
@Composable
private fun ThermostatTile(entity: HaEntity, actions: TileActions, modifier: Modifier, index: Int) {
    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val family = when (entity.attrString("hvac_action")) {
        "heating" -> StateFamily.Heat
        "cooling" -> StateFamily.Cool
        else -> StateFamily.Quiet
    }
    val colors = family.colors(isDark)
    val container by animateColorAsState(if (family == StateFamily.Quiet) MaterialTheme.colorScheme.surfaceContainer else colors.container, label = "thermostat")
    val ink = if (family == StateFamily.Quiet) MaterialTheme.colorScheme.onSurface else colors.onContainer
    val inside = entity.attrDouble("current_temperature")?.let { "${Math.round(it)}°" } ?: "—"
    val humidity = entity.attrDouble("current_humidity")?.let { " · ${Math.round(it)}%" }.orEmpty()
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .height(124.dp)
            .clip(leafCard(index))
            .background(container)
            .clickable(onClickLabel = "Open ${actions.nameOf(entity)}", role = Role.Button) { actions.open(entity) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(actions.nameOf(entity), style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(inside, style = MaterialTheme.typography.displaySmall, color = ink)
            Text(climateLine(entity) + humidity, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val canNudge = entity.attrDouble("temperature") != null ||
            (entity.attrDouble("target_temp_low") != null && entity.attrDouble("target_temp_high") != null)
        if (canNudge) {
            val tonal = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (family == StateFamily.Quiet) MaterialTheme.colorScheme.surfaceContainerHighest else colors.accent,
                contentColor = if (family == StateFamily.Quiet) MaterialTheme.colorScheme.onSurface else colors.onAccent,
            )
            FilledTonalIconButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); actions.nudge(entity, -1) },
                colors = tonal,
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Remove, contentDescription = "Lower") }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); actions.nudge(entity, 1) },
                colors = tonal,
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = "Raise") }
        }
    }
}

// ---- A category's devices (lights, TV and audio) ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    title: String,
    ids: List<String>,
    states: Map<String, HaEntity>,
    actions: TileActions,
    onDismiss: () -> Unit,
) {
    // Opens straight to its full height: a half-open sheet would cut the tiles off mid-row.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            val members = ids.mapNotNull(states::get)
            for ((r, row) in members.chunked(2).withIndex()) {
                Row(Modifier.padding(bottom = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((c, entity) in row.withIndex()) {
                        DeviceTile(entity, HomePlace("sheet", "", null, emptyList()), actions, Modifier.weight(1f), r * 2 + c)
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
