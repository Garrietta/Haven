package com.garrietta.haven.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailPopupHeader
import com.garrietta.haven.DetailPopupTitleHeader
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.RecordingFullscreen
import com.garrietta.haven.data.HaClient
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.findRecordingSibling
import com.garrietta.haven.data.formatHaTimestamp
import com.garrietta.haven.data.isRingCamera
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.garrietta.haven.ui.components.DomainBadge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.weatherConditionColor
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.ForecastTimeline
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
 * Pop-ups, one per entity (2026-10-29 pop-up round — every detail
 * destination is a bottom-anchored floating shelf now, never a
 * full-span page): a consistent header (back, domain badge, name), a
 * hero state, the domain's primary control, then the history chart.
 * No attribute dump: value, state, controls, chart — nothing raw.
 * Tap a tile opens its pop-up; long-press still opens the quick sheet.
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
            DetailPopupTitleHeader("Unavailable")
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
            DetailPopupHeader(entity, onBack)
            Spacer(Modifier.height(16.dp))
            Text(entity.stateText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accentFor(entityId))
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                vm.openCameraFullscreen(entityId)
                onBack()
            }) {
                // Fullscreen is live-only now (Ring via WebRTC, others
                // via MJPEG); recordings live on the long-press
                // timeline, so the button promises live for every
                // camera alike.
                Text("Open live view")
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
        DetailPopupHeader(entity, onBack)
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
        // Timeline, not a list (2026-10-29 verdict: the 24-row plain
        // list "just doesn't look very good — a forecast could be a
        // timeline"). The hero above and the condition coloring are
        // unchanged; only the hourly presentation changes.
        ForecastTimeline(entity, vm)
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
                    Text(entity.stateText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    // History only (2026-10-29 pop-up round): the raw attribute dump
    // is gone — no attribute rows, no Last updated / Last changed
    // timestamps, no state class, no measurement or unit rows, no raw
    // ids. An intuitive home app shows the value, the state, the
    // controls, and the chart — never the HA attribute table. The
    // 24-hour history strip stays: it is a chart, not a dump.
    val history: @Composable () -> Unit = {
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
            DetailPopupHeader(entity, onBack)
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(end = 8.dp)) {
                    hero()
                    control()
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp)) {
                    history()
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            item { DetailPopupHeader(entity, onBack) }
            item { hero() }
            item { control() }
            item { history() }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/**
 * Fraction of the garage-door frame the door fills, 1 = fully closed
 * (door down, frame full), 0 = fully open (door up, frame empty).
 * Position is ground truth when reported; without it, open/closed
 * states map to the ends and any in-between state (opening, closing,
 * unknown) maps to mid-travel — a neutral placeholder, never an
 * invented position. Pure, unit-tested.
 */
fun garageDoorFillFraction(position: Int?, state: String): Float {
    if (position != null) return (100 - position.coerceIn(0, 100)) / 100f
    return when (state) {
        "open" -> 0f
        "closed" -> 1f
        else -> 0.5f
    }
}

/** Cover page: open/close/stop always, position slider only when the cover reports one. */
@Composable
private fun CoverPageControl(entity: HaEntity, vm: HavenViewModel) {
    val position = entity.attrInt("current_position")
    val accent = accentFor(entity.entityId)
    val isGarage = entity.attrString("device_class") == "garage"
    if (isGarage) {
        GarageDoorMenu(entity, vm, position, accent)
        return
    }
    Column {
        Text(entity.stateText(), fontWeight = FontWeight.SemiBold)
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
 * Garage door expressive menu (2026-10-29 verdict: "some kind of
 * expressive UI menu — I don't know how you can incorporate
 * expressive shapes to kind of represent a garage door, but that
 * would be nice"). The expressive element IS a garage door: a
 * segmented door-panel shape in a frame, filled from the top by
 * exactly the closed fraction — closed fills the frame, open empties
 * it from the bottom (the door slides up), partial positions fill
 * partway, and the fill animates as the door moves. Segment divider
 * lines sit at the quarter heights of the frame wherever the fill
 * covers them, so the door reads as panels, not a flat block. Open /
 * Close / Stop are pill controls in the #49 pill language, and the
 * position slider stays where a position is reported. Attributes stay
 * reachable below this control (the page's attributes section is
 * untouched) — the menu replaces only the plain control row.
 */
@Composable
private fun GarageDoorMenu(entity: HaEntity, vm: HavenViewModel, position: Int?, accent: Color) {
    val fill = garageDoorFillFraction(position, entity.state)
    val animatedFill by animateFloatAsState(fill, tween(600), label = "garageDoorFill")
    val frameHeight = 200.dp
    val panelBg = MaterialTheme.colorScheme.surfaceContainer
    Column {
        Text("Garage door · ${entity.stateText()}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (position != null) {
            Text("$position% open", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(frameHeight)
                .background(panelBg, RoundedCornerShape(20.dp)),
        ) {
            // The door itself: top-aligned fill, height = closed
            // fraction of the frame. Rounded top corners only — the
            // bottom edge is the door's own straight edge.
            Box(
                Modifier.fillMaxWidth()
                    .height(frameHeight * animatedFill)
                    .align(Alignment.TopCenter)
                    .background(
                        accent,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ),
            )
            // Segment dividers at the quarter heights, only where
            // the fill reaches them.
            for (quarter in 1..3) {
                val dividerY = frameHeight * (quarter / 4f)
                if (animatedFill * frameHeight.value > dividerY.value) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = dividerY - 1.dp)
                            .background(panelBg),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Pills with generous internal padding and spacing (see
        // PressablePillButton.contentPadding): the door shape above
        // is untouched — only these controls change, from
        // tightly-spaced text to proper spaced pills.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PressablePillButton(
                onClick = { vm.callService("cover", "open_cover", entity.entityId) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Open", fontWeight = FontWeight.SemiBold)
            }
            PressablePillButton(
                onClick = { vm.callService("cover", "close_cover", entity.entityId) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
            PressablePillButton(
                onClick = { vm.callService("cover", "stop_cover", entity.entityId) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Stop", fontWeight = FontWeight.SemiBold)
            }
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

/**
 * Real 24-hour history through the existing endpoint; numeric states
 * only. Loading and empty are distinct states: while the fetch is in
 * flight the strip shows a shimmer, and "No history yet" appears only
 * after a completed load returns too few points. The old code showed
 * "No history yet" during every load, so history visibly arrived late
 * behind a false empty state (2026-10-29 evidence video).
 *
 * CACHED (2026-10-29 verdict: "the plot should be cached"): the
 * strip reads the ViewModel history cache first and refreshes
 * silently in the background. On re-entry the cached plot shows
 * immediately — no shimmer, no reload — and the refresh never blanks
 * it: the cache keeps the last plottable series across a failed
 * refresh, and only updates when new data lands.
 */
@Composable
private fun DetailHistoryStrip(vm: HavenViewModel, entity: HaEntity) {
    val cached = vm.historyFor(entity.entityId)
    LaunchedEffect(entity.entityId) {
        vm.refreshHistory(entity.entityId)
    }
    if (cached == null) {
        ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
    } else if (cached.size >= 2) {
        val points = cached
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

/**
 * Scrollable recorded-clips timeline for a camera (long-press on its
 * tile). Content, honestly sourced:
 * - the Ring last-recording sibling entity (same id prefix +
 *   "recording"), with its HA timestamp, played as MJPEG in the
 *   RecordingFullscreen (labeled Recording, never Live);
 * - media-source clips found by browsing candidate roots (generic
 *   camera root, Ring root) to depth 2. A playable node is collected
 *   only if it names this camera or comes from the Ring root for a
 *   Ring camera — under-collection is safe, misattribution is not.
 * Media-source browse responses carry no timestamps, so clips are
 * listed without invented dates; the dated recording (when present)
 * leads. Where HA exposes nothing for a camera, the empty state says
 * so plainly instead of faking a list.
 */
@Composable
fun CameraClipsScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    val name = entity?.friendlyName ?: entityId
    var loading by remember(entityId) { mutableStateOf(true) }
    var clips by remember(entityId) { mutableStateOf<List<HaClient.MediaChild>>(emptyList()) }
    var playing by remember(entityId) { mutableStateOf<HaClient.MediaChild?>(null) }
    var playingUrl by remember(entityId) { mutableStateOf<Pair<String, String>?>(null) }
    var resolving by remember(entityId) { mutableStateOf(false) }
    var recordingId by remember(entityId) { mutableStateOf<String?>(null) }
    val sibling = remember(entityId, states) { findRecordingSibling(entityId, states.values) }
    val isRing = entity?.let { isRingCamera(it) } == true
    val objId = entityId.substringAfter(".")

    LaunchedEffect(entityId) {
        val found = withTimeoutOrNull(20_000) {
            suspend fun browse(id: String, type: String): List<HaClient.MediaChild>? =
                suspendCancellableCoroutine { cont ->
                    vm.client.browseMedia(id, type) { cont.resumeWith(Result.success(it)) }
                }
            fun belongs(child: HaClient.MediaChild) =
                child.title.contains(name, ignoreCase = true) ||
                    child.mediaContentId.contains(objId, ignoreCase = true)
            val out = mutableListOf<HaClient.MediaChild>()
            val ringRoot = "media-source://ring"
            val roots = listOf("" to "", "media-source://camera" to "media_source", ringRoot to "media_source")
            for ((rootId, rootType) in roots) {
                val children = browse(rootId, rootType) ?: continue
                for (child in children) {
                    if (child.canPlay) {
                        if (belongs(child) || (rootId == ringRoot && isRing)) out.add(child)
                        continue
                    }
                    if (!child.canExpand) continue
                    val generic = child.title.equals("Camera", true) ||
                        child.title.equals("Cameras", true) ||
                        (isRing && child.title.equals("Ring", true))
                    if (!belongs(child) && !generic) continue
                    val grandchildren = browse(child.mediaContentId, child.mediaContentType) ?: continue
                    for (gc in grandchildren) {
                        if (gc.canPlay && (belongs(gc) || (rootId == ringRoot && isRing))) out.add(gc)
                    }
                }
            }
            out.distinctBy { it.mediaContentId }
        } ?: emptyList()
        clips = found
        loading = false
    }

    // Resolve a tapped clip, then play it fullscreen.
    LaunchedEffect(playing) {
        val clip = playing ?: return@LaunchedEffect
        resolving = true
        playingUrl = null
        playingUrl = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Pair<String, String>?> { cont ->
                vm.client.resolveMedia(clip.mediaContentId, clip.mediaContentType) { cont.resumeWith(Result.success(it)) }
            }
        }
        resolving = false
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(title = "$name clips", onBack = onBack)
            when {
                loading -> {
                    repeat(4) {
                        ShimmerBox(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                sibling == null && clips.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("No recorded clips", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Home Assistant exposes no recorded-clip list for this camera.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isRing) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "If your Ring integration has a last-recording entity, enabling it adds it here.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (sibling != null) {
                            item { TimelineSectionHeader("Last recording") }
                            item {
                                RecordingRow(vm, sibling) { recordingId = sibling.entityId }
                            }
                        }
                        if (clips.isNotEmpty()) {
                            item { TimelineSectionHeader("Recorded clips") }
                            items(clips, key = { it.mediaContentId }) { clip ->
                                ClipRow(vm, clip) { playing = clip }
                            }
                        }
                    }
                }
            }
        }
        if (recordingId != null) {
            androidx.activity.compose.BackHandler { recordingId = null }
            RecordingFullscreen(vm, recordingId!!, onClose = { recordingId = null }, enterProgress = 1f)
        }
        if (playing != null) {
            androidx.activity.compose.BackHandler { playing = null }
            ClipPlayerScreen(vm, playing!!, playingUrl, resolving, onClose = { playing = null })
        }
    }
}

@Composable
private fun TimelineSectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RecordingRow(vm: HavenViewModel, sibling: HaEntity, onTap: () -> Unit) {
    // Cached snapshot: re-entering the timeline shows the last
    // recording thumbnail immediately, refreshed in the background.
    val bitmap = vm.snapshotFor(sibling.entityId)
    LaunchedEffect(sibling.entityId) {
        vm.refreshSnapshot(sibling.entityId)
    }
    Row(
        Modifier.fillMaxWidth().clickable { onTap() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(96.dp, 64.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sibling.friendlyName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            val stamp = formatHaTimestamp(sibling.lastChanged ?: sibling.lastUpdated)
            Text(
                stamp ?: "Last recording",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ClipRow(vm: HavenViewModel, clip: HaClient.MediaChild, onTap: () -> Unit) {
    // Cached thumbnail: re-entering the timeline shows the last
    // thumbnail immediately, refreshed in the background.
    val thumb = clip.thumbnail
    val bitmap = thumb?.let { vm.imageFor(it) }
    LaunchedEffect(clip.mediaContentId) {
        if (thumb != null) vm.refreshImage(thumb)
    }
    Row(
        Modifier.fillMaxWidth().clickable { onTap() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(96.dp, 64.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(clip.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "Tap to play",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Fullscreen playback of one resolved media-source clip (scrubbable). */
@Composable
private fun ClipPlayerScreen(
    vm: HavenViewModel,
    clip: HaClient.MediaChild,
    resolved: Pair<String, String>?,
    resolving: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(resolved) {
        if (resolved == null) return@DisposableEffect onDispose {}
        val p = vm.client.createMediaPlayer(context, resolved.first, resolved.second)
        player = p
        onDispose {
            player = null
            p.release()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            resolving -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading clip…", color = Color.White, fontSize = 16.sp)
                }
            }
            resolved == null -> {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Clip unavailable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Home Assistant could not resolve this clip.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onClose) { Text("Close") }
                }
            }
            player != null -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    // Re-bind on recomposition: without this the
                    // factory-captured player goes stale when the
                    // resolved clip changes, and the view can show a
                    // released player (same staleness class as the
                    // HLS fullscreen fix in this round).
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
        ) { Text("Close", color = Color.White) }
    }
}
