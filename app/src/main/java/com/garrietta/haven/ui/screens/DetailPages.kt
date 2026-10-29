package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.HistoryPoint
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.weatherConditionColor
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.FanControl
import com.garrietta.haven.ui.controls.LightControl
import com.garrietta.haven.ui.controls.LockControl
import com.garrietta.haven.ui.controls.ServiceSlider
import com.garrietta.haven.ui.controls.SwitchControl
import com.garrietta.haven.ui.controls.VacuumControl
import com.garrietta.haven.ui.controls.WeatherControl
import com.garrietta.haven.ui.controls.SensorTile
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.stateText
import java.util.Locale
import org.json.JSONObject

/**
 * Full pages, one per entity, in the Google Home / SmartThings manner:
 * a consistent header (back, domain badge, name), a hero state, the
 * domain's primary control, then real attributes. Tap a tile opens its
 * page; long-press still opens the quick sheet.
 *
 * Honesty rule, same as the thermostat page: a section renders only when
 * backing state exists. No invented readings, scenes, schedules, or
 * history — a history strip appears only for numeric states, through the
 * existing history endpoint, and sections with no state are omitted.
 * Climate keeps its own thermostat page; cameras open the live fullscreen
 * viewer (a page of its own, landscape-locked) instead of a static page.
 */
@Composable
fun DetailPageScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    if (entity == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            DetailTopBar(entityId, onBack)
            Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    // Climate's full control lives on its thermostat page, not here.
    if (entity.domain == "climate") {
        ThermostatPageScreen(vm, entityId, onBack)
        return
    }
    // Weather's full page is an expressive page (condition hero +
    // expanded forecast), not the generic attribute-dump detail.
    if (entity.domain == "weather") {
        WeatherPageScreen(entity, vm, onBack)
        return
    }
    if (entity.domain == "camera") {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            DetailTopBar(entity.friendlyName, onBack)
            Spacer(Modifier.height(16.dp))
            Text(entity.stateText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accentFor(entityId))
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                vm.openCameraFullscreen(entityId)
                onBack()
            }) {
                // Ring serves its last recording, not live video — the
                // button must not promise live for it.
                Text(if (isRingCamera(entity)) "View last recording" else "Open live view")
            }
        }
        return
    }
    DetailPageContent(vm, entity, onBack)
}

/**
 * The weather full page: tap on the Home weather card lands here
 * (long-press still opens the sheet). A condition-colored expressive
 * hero — the dial + humidity on a large condition-tinted panel — then
 * an expanded hourly forecast (24 hours of condition-colored shape
 * chips, not the sheet's 6). No raw entity id, no attribute dump:
 * this page is a designed surface, not the generic detail.
 */
@Composable
private fun WeatherPageScreen(entity: HaEntity, vm: HavenViewModel, onBack: () -> Unit) {
    val conditionColor = weatherConditionColor(entity.state)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        DetailTopBar(entity.friendlyName, onBack)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().background(conditionColor.copy(alpha = 0.16f), RoundedCornerShape(32.dp)).padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            WeatherControl(entity, vm, showForecast = false)
        }
        Spacer(Modifier.height(16.dp))
        Text("Hourly forecast", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        WeatherControl(entity, vm, showDial = false, forecastLimit = 24)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailPageContent(vm: HavenViewModel, entity: HaEntity, onBack: () -> Unit) {
    val accent = accentFor(entity.entityId)
    val hero: @Composable () -> Unit = {
        // Lights get a brightness-tinted hero panel (kelvin + brightness
        // scaled, same token as the tiles): the page's color is
        // unmissable, not just the badge glyph.
        val heroRow: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DomainBadge(
                    entity,
                    domainIcon(entity),
                    accent,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entity.stateText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text(entity.entityId, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (entity.domain == "light") {
            Box(
                Modifier.fillMaxWidth().background(
                    lightBrightnessContainer(
                        MaterialTheme.colorScheme,
                        entity.attrInt("color_temp_kelvin"),
                        entity.attrInt("brightness")?.let { it * 100 / 255 },
                        entity.state == "on",
                    ),
                    RoundedCornerShape(28.dp),
                ).padding(16.dp),
            ) {
                heroRow()
            }
        } else {
            heroRow()
        }
        Spacer(Modifier.height(16.dp))
    }
    val control: @Composable () -> Unit = {
        when (entity.domain) {
            "light" -> LightControl(entity, vm)
            "switch", "input_boolean" -> SwitchControl(entity, vm)
            "cover" -> CoverPageControl(entity, vm)
            "lock" -> LockControl(entity, vm)
            "sensor", "binary_sensor" -> SensorTile(entity, vm)
            "media_player" -> MediaPageControl(entity, vm)
            "vacuum" -> VacuumControl(entity, vm)
            "fan" -> FanControl(entity, vm)
            "weather" -> WeatherControl(entity, vm, showForecast = true)
            // This inner card is page content, never a morph source: it
            // must not report bounds or hide (the page overlay itself
            // is the morph surface).
            else -> EntityControlCard(entity, vm, onDetail = { }, showHeader = false, morphSource = false)
        }
        Spacer(Modifier.height(16.dp))
        // Generic on/off actions, only where they make sense: an entity
        // whose state is on/off and which is not a read-only domain.
        if (entity.state in listOf("on", "off") &&
            entity.domain !in listOf("binary_sensor", "sensor", "weather", "camera", "person", "device_tracker")
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.turnOn(entity.entityId) }) { Text("Turn on") }
                OutlinedButton(onClick = { vm.turnOff(entity.entityId) }) { Text("Turn off") }
                OutlinedButton(onClick = { vm.toggle(entity.entityId) }) { Text("Toggle") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    val attributes: @Composable () -> Unit = {
        Text("Attributes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        val attrs = entity.attributes.filterKeys { it != "friendly_name" }.toList().sortedBy { it.first }
        for ((key, value) in attrs) {
            DetailAttributeRow(key, detailValueToString(value))
        }
        // Sensor pages surface last-updated / last-changed from the real
        // state stream; other domains keep them out of the way.
        if (entity.domain == "sensor" || entity.domain == "binary_sensor") {
            entity.lastUpdated?.let { DetailAttributeRow("Last updated", it) }
            entity.lastChanged?.let { DetailAttributeRow("Last changed", it) }
        }
        if (entity.state.toDoubleOrNull() != null) {
            Spacer(Modifier.height(24.dp))
            Text("Last 24 hours", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            DetailHistoryStrip(vm, entity)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (LocalWideLayout.current) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            DetailTopBar(entity.friendlyName, onBack)
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(end = 8.dp)) {
                    hero()
                    control()
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp)) {
                    attributes()
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            item { DetailTopBar(entity.friendlyName, onBack) }
            item { hero() }
            item { control() }
            item { attributes() }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** Cover page: open/close/stop always, position slider only when the cover reports one. */
@Composable
private fun CoverPageControl(entity: HaEntity, vm: HavenViewModel) {
    val position = entity.attrInt("current_position")
    val accent = accentFor(entity.entityId)
    val isGarage = entity.attrString("device_class") == "garage"
    Column {
        Text(
            if (isGarage) "Garage door · ${entity.stateText()}" else entity.stateText(),
            fontWeight = FontWeight.SemiBold,
        )
        if (position != null) {
            Text("$position% open", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.callService("cover", "open_cover", entity.entityId) }) { Text("Open") }
            FilledTonalButton(onClick = { vm.callService("cover", "close_cover", entity.entityId) }) { Text("Close") }
            OutlinedButton(onClick = { vm.callService("cover", "stop_cover", entity.entityId) }) { Text("Stop") }
        }
        if (position != null) {
            Spacer(Modifier.height(8.dp))
            ServiceSlider(position.toFloat(), 0f..100f, { vm.setCoverPosition(entity.entityId, it.toInt()) }, accent)
        }
    }
}

/**
 * Media page: transport only for players in a transportable state, volume
 * only when the player reports a level, source only when it lists sources.
 * Anything HA would reject never renders as a button.
 */
@Composable
private fun MediaPageControl(entity: HaEntity, vm: HavenViewModel) {
    val title = entity.attrString("media_title") ?: "Nothing playing"
    val artist = entity.attrString("media_artist")
    val volume = entity.attrDouble("volume_level")
    val playing = entity.state == "playing"
    val transportOk = entity.state in listOf("playing", "paused", "idle", "buffering")
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (artist != null) Text(artist, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        if (transportOk) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressablePillButton(
                    onClick = { vm.callService("media_player", "media_previous_track", entity.entityId) },
                    modifier = Modifier.width(56.dp),
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                PressablePillButton(
                    onClick = { vm.callService("media_player", "media_play_pause", entity.entityId) },
                    modifier = Modifier.width(88.dp),
                    height = 56.dp,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (playing) "Pause" else "Play", fontWeight = FontWeight.SemiBold)
                }
                PressablePillButton(
                    onClick = { vm.callService("media_player", "media_next_track", entity.entityId) },
                    modifier = Modifier.width(56.dp),
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (volume != null) {
            Text("Volume", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ServiceSlider(
                (volume * 100).toFloat(), 0f..100f,
                { vm.callService("media_player", "volume_set", entity.entityId, JSONObject().put("volume_level", it / 100f)) },
                accentFor(entity.entityId),
            )
        }
        val sources = entity.attrList("source_list")?.mapNotNull { it as? String } ?: emptyList()
        if (sources.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val currentSource = entity.attrString("source") ?: "Source"
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(currentSource) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    for (source in sources) {
                        DropdownMenuItem(
                            text = { Text(source) },
                            onClick = {
                                expanded = false
                                vm.callService("media_player", "select_source", entity.entityId, JSONObject().put("source", source))
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Stacked label + full-width value; long values truncate with tap-to-copy (same as Browse). */
@Composable
private fun DetailAttributeRow(key: String, value: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(key.replace('_', ' '), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (value.length > 200) {
            Text(value.take(200) + "…", fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(value))
                    copied = true
                }) {
                    Text(if (copied) "Copied" else "Copy full value")
                }
                if (copied) {
                    Text("Copied to clipboard", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text(value, fontSize = 13.sp)
        }
    }
}

private fun detailValueToString(value: Any?): String = when (value) {
    null -> "—"
    is List<*> -> value.joinToString(", ")
    is Map<*, *> -> value.entries.joinToString(", ") { "${it.key}: ${it.value}" }
    else -> value.toString()
}

/**
 * Real 24-hour history through the existing endpoint; numeric states
 * only. Loading and empty are distinct states: while the fetch is in
 * flight the strip shows a shimmer, and "No history yet" appears only
 * after a completed load returns too few points. The old code showed
 * "No history yet" during every load, so history visibly arrived late
 * behind a false empty state (2026-10-29 evidence video).
 */
@Composable
private fun DetailHistoryStrip(vm: HavenViewModel, entity: HaEntity) {
    var points by remember { mutableStateOf<List<HistoryPoint>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(entity.entityId) {
        loaded = false
        points = vm.client.fetchHistory(entity.entityId, 24)
        loaded = true
    }
    if (!loaded) {
        ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
    } else if (points.size >= 2) {
        val mm = sparklineMinCurrentMax(points)
        val unit = entity.attrString("unit_of_measurement")
        Column {
            Sparkline(points, accentFor(entity.entityId), Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(4.dp))
            if (mm != null) {
                Text(
                    "Min ${fmtDetailHistory(mm.first, unit)} · Current ${fmtDetailHistory(mm.second, unit)} · Max ${fmtDetailHistory(mm.third, unit)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        Text("No history yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Whole numbers print whole; the rest get one decimal; unit suffix when present. */
private fun fmtDetailHistory(v: Double, unit: String?): String {
    val num = if (v == v.toInt().toDouble()) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
    return if (unit != null) "$num $unit" else num
}
