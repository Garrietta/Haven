package com.garrietta.haven.ui.controls

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.AppGraph
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.model.HistoryPoint
import com.garrietta.haven.core.store.HomeStore
import com.garrietta.haven.ui.components.HavenChip
import com.garrietta.haven.ui.components.StateBadge
import com.garrietta.haven.ui.components.tileColors
import com.garrietta.haven.ui.fmtNum
import com.garrietta.haven.ui.pretty
import com.garrietta.haven.ui.stateText
import com.garrietta.haven.ui.weatherIcon
import com.garrietta.haven.ui.weatherText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * The popup body for one entity: header (morphing badge, name, room, state,
 * favorite star) and the control designed for its domain.
 */
@Composable
fun EntityControls(graph: AppGraph, entityId: String, onOpenCamera: (String) -> Unit, onClose: () -> Unit) {
    val e = graph.store.entity(entityId).collectAsState().value ?: return
    val areas by graph.store.areas.collectAsState()
    val favorites by graph.prefs.favorites.collectAsState()
    val unit = graph.store.config.collectAsState().value.temperatureUnit
    val area = graph.store.areaOf(entityId)?.let { id -> areas.firstOrNull { it.id == id }?.name }
    val isFav = favorites?.contains(entityId) == true
    val store = graph.store

    Row(verticalAlignment = Alignment.CenterVertically) {
        StateBadge(e, 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (area != null) Text(area, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(e.friendlyName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stateText(e, unit), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { graph.prefs.toggleFavorite(entityId, favorites ?: emptyList()) }) {
            Icon(if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favorite", tint = if (isFav) Color(0xFFE8B931) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(20.dp))
    if (e.isUnavailable) {
        Text("This device isn't responding right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val accent = tileColors(e).badgeBg.takeIf { HomeStore.isActive(e) } ?: MaterialTheme.colorScheme.primary
    when (e.domain) {
        "light" -> LightControls(store, e, accent)
        "switch", "input_boolean", "siren", "automation" -> ToggleControls(store, e)
        "fan" -> FanControls(store, e, accent)
        "cover" -> CoverControls(store, e, accent)
        "climate" -> ClimateControls(store, e, unit)
        "lock" -> LockControls(store, e)
        "media_player" -> MediaControls(graph, e, accent)
        "vacuum" -> VacuumControls(store, e)
        "alarm_control_panel" -> AlarmControls(store, e)
        "camera" -> PillRow { PillButton(Icons.Filled.Videocam, "Open live view") { onClose(); onOpenCamera(entityId) } }
        "scene", "script", "button", "input_button" -> PillRow {
            PillButton(Icons.Filled.PlayArrow, if (e.domain == "scene") "Activate" else "Run", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) { store.toggle(e) }
        }
        "input_select", "select" -> OptionControls(store, e)
        "input_number", "number" -> NumberControls(store, e, accent)
        "weather" -> WeatherControls(graph, e)
        "sensor" -> SensorControls(graph, e, accent)
        "binary_sensor", "person", "device_tracker" -> HistoryStrip(graph, e)
        else -> Text(pretty(e.state), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun PillRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)

@Composable
private fun ToggleControls(store: HomeStore, e: HaEntity) {
    val on = HomeStore.isActive(e)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { store.toggle(e) }.padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (on) "On" else "Off", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(on, { store.toggle(e) }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun LightControls(store: HomeStore, e: HaEntity, accent: Color) {
    val modes = e.attrStrings("supported_color_modes")
    val dimmable = modes.any { it != "onoff" } || e.attributes.containsKey("brightness")
    if (dimmable) {
        val pct = if (e.state == "on") ((e.attrInt("brightness") ?: 255) * 100 + 127) / 255f else 0f
        PillSlider(pct, 0f..100f, accent, label = { if (it <= 0f) "Off" else "${it.toInt()}%" }, icon = Icons.Filled.Lightbulb) {
            store.setBrightness(e, it.toInt())
        }
    } else {
        ToggleControls(store, e)
        return
    }
    Spacer(Modifier.height(12.dp))
    PillRow {
        PillButton(Icons.Filled.PowerSettingsNew, if (e.state == "on") "Turn off" else "Turn on") { store.toggle(e) }
    }
    if ("color_temp" in modes) {
        Spacer(Modifier.height(12.dp))
        val minK = (e.attrInt("min_color_temp_kelvin") ?: 2000).toFloat()
        val maxK = (e.attrInt("max_color_temp_kelvin") ?: 6500).toFloat()
        val k = (e.attrInt("color_temp_kelvin") ?: ((minK + maxK) / 2).toInt()).toFloat()
        PillSlider(
            k, minK..maxK, fill = accent, label = { "${it.toInt()}K" }, steps = 50f,
            trackBrush = Brush.horizontalGradient(listOf(Color(0xFFFFB46B), Color(0xFFFFF1DC), Color(0xFFCFE3FF))),
        ) { store.setColorTemp(e, it.toInt()) }
    }
    if (modes.any { it in setOf("hs", "rgb", "rgbw", "rgbww", "xy") }) {
        Spacer(Modifier.height(14.dp))
        val swatches = listOf(0f to 0f, 30f to 90f, 50f to 80f, 120f to 70f, 180f to 70f, 220f to 80f, 280f to 70f, 330f to 70f)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { (h, s) ->
                val c = Color.hsv(h, s / 100f, 1f)
                Box(Modifier.size(44.dp).clip(CircleShape).background(c).clickable { store.setHs(e, h, s) })
            }
        }
    }
}

@Composable
private fun FanControls(store: HomeStore, e: HaEntity, accent: Color) {
    if (e.supports(1) || e.attributes.containsKey("percentage")) {
        PillSlider((e.attrInt("percentage") ?: 0).toFloat(), 0f..100f, accent, label = { if (it <= 0) "Off" else "${it.toInt()}%" }, icon = Icons.Filled.Air,
            steps = (e.attrDouble("percentage_step") ?: 1.0).toFloat()) { store.setFanPercentage(e, it.toInt()) }
        Spacer(Modifier.height(12.dp))
    }
    ToggleControls(store, e)
    val presets = e.attrStrings("preset_modes")
    if (presets.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        ChipRow(presets, e.attrString("preset_mode")) { store.fire("fan", "set_preset_mode", e.entityId, JSONObject().put("preset_mode", it)) }
    }
}

@Composable
private fun CoverControls(store: HomeStore, e: HaEntity, accent: Color) {
    val supportsPosition = e.supports(4) || e.attributes.containsKey("current_position")
    if (supportsPosition) {
        PillSlider((e.attrInt("current_position") ?: 0).toFloat(), 0f..100f, accent, label = { if (it <= 0) "Closed" else "${it.toInt()}% open" }) {
            store.setCoverPosition(e, it.toInt())
        }
        Spacer(Modifier.height(12.dp))
    }
    PillRow {
        PillButton(Icons.Filled.KeyboardArrowUp, "Open", enabled = e.state != "open") { store.setCoverOpen(e, true) }
        if (e.supports(8) || e.domain == "cover") PillButton(Icons.Filled.Stop, null, weight = 0.6f) { store.stopCover(e) }
        PillButton(Icons.Filled.KeyboardArrowDown, "Close", enabled = e.state != "closed") { store.setCoverOpen(e, false) }
    }
}

@Composable
private fun ClimateControls(store: HomeStore, e: HaEntity, unit: String) {
    val min = e.attrDouble("min_temp") ?: 45.0
    val max = e.attrDouble("max_temp") ?: 95.0
    val step = e.attrDouble("target_temp_step") ?: if (unit.contains("F")) 1.0 else 0.5
    val current = e.attrDouble("current_temperature")
    val heat = Color(0xFFFF8A50)
    val cool = Color(0xFF64B5F6)
    val accent = when (e.attrString("hvac_action") ?: e.state) { "heating", "heat" -> heat; "cooling", "cool" -> cool; else -> MaterialTheme.colorScheme.primary }
    val target = e.attrDouble("temperature")
    val lo = e.attrDouble("target_temp_low")
    val hi = e.attrDouble("target_temp_high")
    when {
        target != null -> ThermostatDial(current, target, min, max, step, accent, unit, Modifier.fillMaxWidth()) { store.setTemperature(e, it) }
        lo != null && hi != null -> {
            Text(current?.let { "Now ${fmtNum(it)}$unit" } ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Stepper("Heat to", lo, step, heat) { store.setTemperatureRange(e, it.coerceAtMost(hi - step), hi) }
            Spacer(Modifier.height(10.dp))
            Stepper("Cool to", hi, step, cool) { store.setTemperatureRange(e, lo, it.coerceAtLeast(lo + step)) }
        }
        else -> Text(current?.let { "Now ${fmtNum(it)}$unit" } ?: pretty(e.state), style = MaterialTheme.typography.headlineMedium)
    }
    val modes = e.attrStrings("hvac_modes")
    if (modes.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        ChipRow(modes, e.state, label = { pretty(it.replace("heat_cool", "Heat/Cool")) }) { store.setHvacMode(e, it) }
    }
    val fans = e.attrStrings("fan_modes")
    if (fans.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        ChipRow(fans, e.attrString("fan_mode"), icon = Icons.Filled.Air) { store.setClimateAttr(e, "set_fan_mode", "fan_mode", it) }
    }
    val presets = e.attrStrings("preset_modes")
    if (presets.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        ChipRow(presets, e.attrString("preset_mode")) { store.setClimateAttr(e, "set_preset_mode", "preset_mode", it) }
    }
}

@Composable
private fun Stepper(label: String, value: Double, step: Double, color: Color, onCommit: (Double) -> Unit) {
    var local by remember(value) { mutableStateOf(value) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton(Icons.Filled.Remove, null, weight = 0.7f) { local -= step; onCommit(local) }
        Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text("${fmtNum(local)}°", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        PillButton(Icons.Filled.Add, null, weight = 0.7f) { local += step; onCommit(local) }
    }
}

@Composable
private fun LockControls(store: HomeStore, e: HaEntity) {
    var confirm by remember { mutableStateOf(false) }
    val locked = e.state == "locked"
    PillRow {
        if (locked) {
            PillButton(Icons.Filled.LockOpen, if (confirm) "Tap again to unlock" else "Unlock", if (confirm) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
                if (confirm) { store.setLocked(e, false); confirm = false } else confirm = true
            }
        } else {
            PillButton(Icons.Filled.Lock, "Lock", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) { store.setLocked(e, true) }
        }
    }
}

@Composable
private fun MediaControls(graph: AppGraph, e: HaEntity, accent: Color) {
    val store = graph.store
    val picture = e.attrString("entity_picture")
    val art by produceState<Bitmap?>(null, picture) { value = picture?.let { graph.rest.image(it, 600) } }
    if (e.state in setOf("playing", "paused", "buffering")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            art?.let {
                Image(it.asImageBitmap(), null, Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(e.attrString("media_title") ?: pretty(e.state), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                (e.attrString("media_artist") ?: e.attrString("app_name"))?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    PillRow {
        PillButton(Icons.Filled.SkipPrevious, null) { store.media(e, "media_previous_track") }
        PillButton(
            if (e.state == "playing") Icons.Filled.Pause else Icons.Filled.PlayArrow, null,
            MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, weight = 1.4f,
        ) { store.media(e, "media_play_pause") }
        PillButton(Icons.Filled.SkipNext, null) { store.media(e, "media_next_track") }
    }
    if (e.attributes.containsKey("volume_level")) {
        Spacer(Modifier.height(12.dp))
        PillSlider(((e.attrDouble("volume_level") ?: 0.0) * 100).toFloat(), 0f..100f, accent, label = { "${it.toInt()}%" }, icon = Icons.Filled.VolumeUp) {
            store.setVolume(e, it / 100f)
        }
    }
    Spacer(Modifier.height(12.dp))
    PillRow {
        PillButton(Icons.Filled.PowerSettingsNew, if (e.state == "off") "Turn on" else "Turn off") { store.toggle(e) }
        if (e.attributes.containsKey("is_volume_muted")) {
            PillButton(if (e.attrBool("is_volume_muted") == true) Icons.Filled.VolumeOff else Icons.Filled.VolumeMute, null, weight = 0.6f) {
                store.fire("media_player", "volume_mute", e.entityId, JSONObject().put("is_volume_muted", e.attrBool("is_volume_muted") != true))
            }
        }
    }
    val sources = e.attrStrings("source_list")
    if (sources.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        ChipRow(sources, e.attrString("source")) { store.fire("media_player", "select_source", e.entityId, JSONObject().put("source", it)) }
    }
}

@Composable
private fun VacuumControls(store: HomeStore, e: HaEntity) {
    val battery = e.attrInt("battery_level")
    if (battery != null) {
        Text("Battery $battery%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
    }
    PillRow {
        val cleaning = e.state == "cleaning"
        PillButton(if (cleaning) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (cleaning) "Pause" else "Start",
            MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) { store.vacuum(e, if (cleaning) "pause" else "start") }
        PillButton(Icons.Filled.Home, "Dock") { store.vacuum(e, "return_to_base") }
    }
    Spacer(Modifier.height(10.dp))
    PillRow {
        PillButton(Icons.Filled.Stop, "Stop") { store.vacuum(e, "stop") }
        PillButton(Icons.Filled.LocationSearching, "Locate") { store.vacuum(e, "locate") }
    }
    val speeds = e.attrStrings("fan_speed_list")
    if (speeds.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        ChipRow(speeds, e.attrString("fan_speed")) { store.fire("vacuum", "set_fan_speed", e.entityId, JSONObject().put("fan_speed", it)) }
    }
}

@Composable
private fun AlarmControls(store: HomeStore, e: HaEntity) {
    var code by remember { mutableStateOf("") }
    val needsCode = e.attrString("code_format") != null
    if (needsCode) {
        Text(if (code.isEmpty()) "Enter code" else "•".repeat(code.length), fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "C")).forEach { row ->
            PillRow { row.forEach { k -> PillButton(text = k, height = 52.dp) { code = when (k) { "⌫" -> code.dropLast(1); "C" -> ""; else -> code + k } } } }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(6.dp))
    }
    if (e.state != "disarmed") {
        PillRow { PillButton(Icons.Filled.LockOpen, "Disarm", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) { store.alarm(e, "alarm_disarm", code); code = "" } }
    } else {
        PillRow {
            PillButton(Icons.Filled.Home, "Home") { store.alarm(e, "alarm_arm_home", code); code = "" }
            PillButton(Icons.Filled.DirectionsWalk, "Away") { store.alarm(e, "alarm_arm_away", code); code = "" }
            PillButton(Icons.Filled.NightsStay, "Night") { store.alarm(e, "alarm_arm_night", code); code = "" }
        }
    }
}

@Composable
private fun OptionControls(store: HomeStore, e: HaEntity) {
    val options = e.attrStrings("options")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val sel = opt == e.state
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { store.selectOption(e, opt) }.padding(horizontal = 18.dp, vertical = 14.dp),
            ) { Text(opt, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun NumberControls(store: HomeStore, e: HaEntity, accent: Color) {
    val min = (e.attrDouble("min") ?: 0.0).toFloat()
    val max = (e.attrDouble("max") ?: 100.0).toFloat()
    val step = (e.attrDouble("step") ?: 1.0).toFloat()
    PillSlider(e.state.toFloatOrNull() ?: min, min..max, accent, label = { fmtNum(it.toDouble()) + (e.unit?.let { u -> " $u" } ?: "") }, steps = step) {
        store.setNumber(e, it.toDouble())
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, label: (String) -> String = { pretty(it) }, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { HavenChip(label(it), icon.takeIf { _ -> it == selected }, selected = it == selected) { onPick(it) } }
    }
}

@Composable
private fun SensorControls(graph: AppGraph, e: HaEntity, accent: Color) {
    if (e.state.toDoubleOrNull() == null) { HistoryStrip(graph, e); return }
    var hours by remember { mutableStateOf(24) }
    val points by produceState<List<HistoryPoint>>(emptyList(), e.entityId, hours) { value = graph.rest.history(e.entityId, hours) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(6, 24, 168).forEach { h -> HavenChip(if (h == 168) "7d" else "${h}h", selected = hours == h) { hours = h } }
    }
    Spacer(Modifier.height(12.dp))
    HistoryChart(points, accent, Modifier.fillMaxWidth().height(160.dp))
    if (points.size > 1) {
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Low ${fmtNum(points.minOf { it.value })}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("High ${fmtNum(points.maxOf { it.value })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryStrip(graph: AppGraph, e: HaEntity) {
    val ago = remember(e.lastChanged) {
        val mins = ((System.currentTimeMillis() / 1000.0 - e.lastChanged) / 60).toLong()
        when { mins < 1 -> "just now"; mins < 60 -> "$mins min ago"; mins < 1440 -> "${mins / 60} h ago"; else -> "${mins / 1440} d ago" }
    }
    Text("Changed $ago", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (e.domain == "person") {
        e.attrDouble("gps_accuracy")?.let { Text("GPS accuracy ${it.toInt()} m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun WeatherControls(graph: AppGraph, e: HaEntity) {
    val forecast = remember { MutableStateFlow<List<JSONObject>>(emptyList()) }
    val days by forecast.collectAsState()
    DisposableEffect(e.entityId) {
        var subId: Int? = null
        val job = graph.scope.launchSub {
            subId = graph.connection.subscribe("weather/subscribe_forecast", { put("entity_id", e.entityId).put("forecast_type", "daily") }) { ev ->
                val arr: JSONArray = ev.optJSONArray("forecast") ?: return@subscribe
                forecast.value = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            }
        }
        onDispose { job.cancel(); subId?.let { graph.connection.unsubscribe(it) } }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(weatherIcon(e.state), null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text("${e.attrDouble("temperature")?.let { fmtNum(it) } ?: "--"}°", fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(weatherText(e.state), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        days.take(7).forEach { d ->
            val day = runCatching { java.time.OffsetDateTime.parse(d.optString("datetime")).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }.getOrDefault("")
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(day, style = MaterialTheme.typography.labelMedium)
                Icon(weatherIcon(d.optString("condition")), null, Modifier.size(26.dp).padding(vertical = 2.dp))
                Text("${fmtNum(d.optDouble("temperature"))}°", fontWeight = FontWeight.Bold)
                if (d.has("templow")) Text("${fmtNum(d.optDouble("templow"))}°", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val humidity = e.attrDouble("humidity")
    val wind = e.attrDouble("wind_speed")
    if (humidity != null || wind != null) {
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            humidity?.let { HavenChip("Humidity ${fmtNum(it)}%", Icons.Filled.WaterDrop) }
            wind?.let { HavenChip("Wind ${fmtNum(it)} ${e.attrString("wind_speed_unit") ?: ""}", Icons.Filled.Air) }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSub(block: suspend () -> Unit) =
    launch { try { block() } catch (_: Exception) {} }
