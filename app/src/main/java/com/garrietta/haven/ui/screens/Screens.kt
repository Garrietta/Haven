package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.AppGraph
import com.garrietta.haven.core.model.Area
import com.garrietta.haven.core.model.EntityEntry
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.store.HomeStore
import com.garrietta.haven.ui.DOMAIN_GROUPS
import com.garrietta.haven.ui.USER_DOMAINS
import com.garrietta.haven.ui.camera.CameraCard
import com.garrietta.haven.ui.components.EntityTile
import com.garrietta.haven.ui.components.HavenCard
import com.garrietta.haven.ui.components.HavenChip
import com.garrietta.haven.ui.components.SectionHeader
import com.garrietta.haven.ui.components.tileColors
import com.garrietta.haven.ui.fmtNum
import com.garrietta.haven.ui.greeting
import com.garrietta.haven.ui.groupOf
import com.garrietta.haven.ui.stateText
import com.garrietta.haven.ui.weatherIcon
import com.garrietta.haven.ui.weatherText

/** UI-level actions every screen needs. */
class Actions(
    val openControls: (String) -> Unit,
    val openCamera: (String) -> Unit,
    val openRoom: (String) -> Unit,
)

/** Pure: entities a person should see — user domains, not hidden, not config/diagnostic. Unit tested. */
fun visibleEntities(states: Map<String, HaEntity>, entries: Map<String, EntityEntry>): List<HaEntity> =
    states.values.filter { e ->
        val entry = entries[e.entityId]
        e.domain in USER_DOMAINS && entry?.hidden != true && entry?.category == null
    }.sortedBy { it.friendlyName.lowercase() }

/** Tap behaviour: toggle things that toggle, open everything else. */
fun primaryAction(graph: AppGraph, e: HaEntity, actions: Actions) {
    when (e.domain) {
        "light", "switch", "input_boolean", "fan", "scene", "script", "button", "input_button", "automation", "siren" -> graph.store.toggle(e)
        "camera" -> actions.openCamera(e.entityId)
        else -> actions.openControls(e.entityId)
    }
}

/** Adaptive tile grid: 2 columns folded, 3–5 unfolded / tablet. */
@Composable
fun TileGrid(
    contentPadding: PaddingValues,
    minTile: Dp = 160.dp,
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minTile),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

private fun LazyGridScope.full(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Tile(graph: AppGraph, e: HaEntity, actions: Actions, unit: String) {
    EntityTile(e, e.friendlyName, onTap = { primaryAction(graph, e, actions) }, onLongPress = { actions.openControls(e.entityId) }, tempUnit = unit)
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(graph: AppGraph, actions: Actions, padding: PaddingValues) {
    val states by graph.store.entities.collectAsState()
    val entries by graph.store.entries.collectAsState()
    val areas by graph.store.areas.collectAsState()
    val config by graph.store.config.collectAsState()
    val favorites by graph.prefs.favorites.collectAsState()
    val visible = remember(states, entries) { visibleEntities(states, entries) }

    val weather = visible.firstOrNull { it.domain == "weather" }
    val climate = visible.firstOrNull { it.domain == "climate" }
    val cameras = visible.filter { it.domain == "camera" }
    val lightsOn = visible.filter { it.domain == "light" && it.state == "on" }
    val people = visible.filter { it.domain == "person" }
    val alerts = visible.filter { isAlert(it) }
    val favs = (favorites ?: defaultFavorites(visible)).mapNotNull { states[it] }

    TileGrid(padding) {
        full {
            Column(Modifier.padding(top = 8.dp, start = 4.dp)) {
                Text(greeting(java.time.LocalTime.now().hour), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(config.locationName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        full {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                people.forEach { p -> HavenChip(p.friendlyName + if (p.state == "home") " · Home" else " · Away", Icons.Filled.Person, selected = p.state == "home") { actions.openControls(p.entityId) } }
                HavenChip(if (lightsOn.isEmpty()) "Lights off" else "${lightsOn.size} lights on", Icons.Filled.Lightbulb, selected = lightsOn.isNotEmpty()) {
                    lightsOn.forEach { graph.store.toggle(it) }
                }
                alerts.forEach { a -> HavenChip("${a.friendlyName} · ${stateText(a)}", Icons.Filled.Warning, container = MaterialTheme.colorScheme.errorContainer) { actions.openControls(a.entityId) } }
            }
        }
        if (weather != null || climate != null) {
            full {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    weather?.let { w ->
                        HavenCard(Modifier.weight(1f).height(124.dp), onClick = { actions.openControls(w.entityId) }) {
                            Column(Modifier.padding(18.dp)) {
                                Icon(weatherIcon(w.state), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))
                                Text("${w.attrDouble("temperature")?.let { fmtNum(it) } ?: "--"}°", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                Text(weatherText(w.state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    climate?.let { c ->
                        val colors = tileColors(c)
                        HavenCard(Modifier.weight(1f).height(124.dp), color = colors.container, onClick = { actions.openControls(c.entityId) }) {
                            Column(Modifier.padding(18.dp)) {
                                Icon(Icons.Filled.Thermostat, null, Modifier.size(30.dp), tint = colors.badgeBg)
                                Spacer(Modifier.weight(1f))
                                Text("${c.attrDouble("current_temperature")?.let { fmtNum(it) } ?: "--"}°", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                Text(stateText(c, config.temperatureUnit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        if (cameras.isNotEmpty()) {
            full { SectionHeader("Cameras") }
            full {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    cameras.forEach { cam -> CameraCard(graph, cam.entityId, Modifier.width(260.dp).aspectRatio(16f / 10f)) { actions.openCamera(cam.entityId) } }
                }
            }
        }
        if (favs.isNotEmpty()) {
            full { SectionHeader("Favorites") }
            items(favs, key = { it.entityId }) { Tile(graph, it, actions, config.temperatureUnit) }
        }
        if (areas.isNotEmpty()) {
            full { SectionHeader("Rooms") }
            full {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    areas.forEach { a -> HavenChip(a.name, Icons.Filled.MeetingRoom) { actions.openRoom(a.id) } }
                }
            }
        }
        full { Spacer(Modifier.height(24.dp)) }
    }
}

/** Something worth surfacing on Home. Pure, unit tested. */
fun isAlert(e: HaEntity): Boolean = when (e.domain) {
    "lock" -> e.state == "unlocked" || e.state == "jammed" || e.state == "open"
    "alarm_control_panel" -> e.state == "triggered"
    "binary_sensor" -> e.state == "on" && e.deviceClass in setOf("door", "garage_door", "window", "opening", "smoke", "gas", "moisture", "carbon_monoxide", "problem", "safety")
    "cover" -> e.deviceClass in setOf("garage", "door", "gate") && e.state == "open"
    else -> false
}

/** Until the user stars things: lights, climate, covers, locks, media — up to 8. */
fun defaultFavorites(visible: List<HaEntity>): List<String> {
    val order = listOf("light", "climate", "cover", "lock", "media_player", "fan", "switch", "vacuum")
    return visible.filter { it.domain in order }.sortedBy { order.indexOf(it.domain) }.take(8).map { it.entityId }
}

// ---------------------------------------------------------------- Rooms

@Composable
fun RoomsScreen(graph: AppGraph, actions: Actions, padding: PaddingValues) {
    val states by graph.store.entities.collectAsState()
    val entries by graph.store.entries.collectAsState()
    val devices by graph.store.devices.collectAsState()
    val areas by graph.store.areas.collectAsState()
    val visible = remember(states, entries) { visibleEntities(states, entries) }
    val byArea = remember(visible, entries, devices) { visible.groupBy { graph.store.areaOf(it.entityId, entries, devices) } }

    TileGrid(padding, minTile = 170.dp) {
        full { ScreenTitle("Rooms") }
        items(areas, key = { it.id }) { area ->
            RoomCard(area, byArea[area.id].orEmpty(), states, onOpen = { actions.openRoom(area.id) }, onLightsOff = {
                byArea[area.id].orEmpty().filter { it.domain == "light" && it.state == "on" }.forEach { graph.store.toggle(it) }
            })
        }
        val unassigned = byArea[null].orEmpty().filter { it.domain !in setOf("person", "weather", "automation") }
        if (unassigned.isNotEmpty()) {
            full { HavenChip("${unassigned.size} devices without a room", Icons.Filled.DevicesOther) { actions.openRoom(NO_ROOM) } }
        }
        full { Spacer(Modifier.height(24.dp)) }
    }
}

const val NO_ROOM = "__none__"

@Composable
private fun RoomCard(area: Area, entities: List<HaEntity>, states: Map<String, HaEntity>, onOpen: () -> Unit, onLightsOff: () -> Unit) {
    val lightsOn = entities.count { it.domain == "light" && it.state == "on" }
    val temp = (area.temperatureEntityId?.let { states[it] } ?: entities.firstOrNull { it.domain == "sensor" && it.deviceClass == "temperature" })
        ?.state?.toDoubleOrNull()
    val humidity = (area.humidityEntityId?.let { states[it] } ?: entities.firstOrNull { it.domain == "sensor" && it.deviceClass == "humidity" })
        ?.state?.toDoubleOrNull()
    val active = entities.count { HomeStore.isActive(it) && it.domain !in setOf("sensor", "binary_sensor", "person") }
    HavenCard(Modifier.fillMaxWidth().height(150.dp), color = if (lightsOn > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, onClick = onOpen) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MeetingRoom, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                if (lightsOn > 0) {
                    Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { onLightsOff() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("$lightsOn on", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(area.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val facts = listOfNotNull(temp?.let { "${fmtNum(it)}°" }, humidity?.let { "${fmtNum(it)}%" }, if (active > 0) "$active active" else "${entities.size} devices")
            Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RoomScreen(graph: AppGraph, areaId: String, actions: Actions, padding: PaddingValues, onBack: () -> Unit) {
    val states by graph.store.entities.collectAsState()
    val entries by graph.store.entries.collectAsState()
    val devices by graph.store.devices.collectAsState()
    val areas by graph.store.areas.collectAsState()
    val unit = graph.store.config.collectAsState().value.temperatureUnit
    val name = if (areaId == NO_ROOM) "No room" else areas.firstOrNull { it.id == areaId }?.name ?: "Room"
    val inRoom = remember(states, entries, devices, areaId) {
        visibleEntities(states, entries).filter { (graph.store.areaOf(it.entityId, entries, devices) ?: NO_ROOM) == areaId }
            .filter { areaId != NO_ROOM || it.domain !in setOf("person", "weather", "automation") }
    }
    val groups = DOMAIN_GROUPS.mapNotNull { (title, domains) -> inRoom.filter { it.domain in domains }.takeIf { it.isNotEmpty() }?.let { title to it } }
    TileGrid(padding) {
        full {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        groups.forEach { (title, list) ->
            full { SectionHeader(title) }
            if (title == "Cameras") {
                list.forEach { cam -> full { CameraCard(graph, cam.entityId, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { actions.openCamera(cam.entityId) } } }
            } else {
                items(list, key = { it.entityId }) { Tile(graph, it, actions, unit) }
            }
        }
        if (groups.isEmpty()) full { EmptyNote("Nothing in this room yet.") }
        full { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- Cameras

@Composable
fun CamerasScreen(graph: AppGraph, actions: Actions, padding: PaddingValues) {
    val states by graph.store.entities.collectAsState()
    val entries by graph.store.entries.collectAsState()
    val cams = remember(states, entries) { visibleEntities(states, entries).filter { it.domain == "camera" } }
    TileGrid(padding, minTile = 300.dp) {
        full { ScreenTitle("Cameras") }
        items(cams, key = { it.entityId }) { cam ->
            CameraCard(graph, cam.entityId, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { actions.openCamera(cam.entityId) }
        }
        if (cams.isEmpty()) full { EmptyNote("No cameras found.") }
        full { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- Devices

@Composable
fun DevicesScreen(graph: AppGraph, actions: Actions, padding: PaddingValues) {
    val states by graph.store.entities.collectAsState()
    val entries by graph.store.entries.collectAsState()
    val devices by graph.store.devices.collectAsState()
    val areas by graph.store.areas.collectAsState()
    val unit = graph.store.config.collectAsState().value.temperatureUnit
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    val all = remember(states, entries) { visibleEntities(states, entries) }
    val areaNames = remember(areas) { areas.associate { it.id to it.name } }
    val q = query.trim().lowercase()
    val filtered = all.filter { e ->
        (group == null || groupOf(e.domain) == group) &&
            (q.isEmpty() || e.friendlyName.lowercase().contains(q) || e.entityId.contains(q) ||
                areaNames[graph.store.areaOf(e.entityId, entries, devices)]?.lowercase()?.contains(q) == true)
    }
    val presentGroups = DOMAIN_GROUPS.map { it.first }.filter { g -> all.any { groupOf(it.domain) == g } }
    TileGrid(padding) {
        full { ScreenTitle("Devices") }
        full {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search devices, rooms…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Clear") } },
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        }
        full {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HavenChip("All", selected = group == null) { group = null }
                presentGroups.forEach { g -> HavenChip(g, selected = group == g) { group = if (group == g) null else g } }
            }
        }
        full { Text("${filtered.size} devices", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp)) }
        items(filtered, key = { it.entityId }) { Tile(graph, it, actions, unit) }
        full { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun ScreenTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
}

@Composable
fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
