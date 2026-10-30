package com.garrietta.haven.ui.controls
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import com.garrietta.haven.ui.components.formatCondition

import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.PillToggle
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.dissolveIntoTraveler
import com.garrietta.haven.ui.components.reportArcBounds
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.weatherConditionColor
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.polygonShape
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Second half of the domain dispatch (called from EntityControlCard). */
@Composable
fun MoreControl(entity: HaEntity, vm: HavenViewModel, showForecast: Boolean = true) {
    when (entity.domain) {
        "sensor", "binary_sensor" -> SensorTile(entity, vm)
        "weather" -> WeatherControl(entity, vm, showForecast = showForecast)
        "camera" -> CameraControl(entity, vm)
        "scene" -> FilledTonalButton(onClick = { vm.turnOn(entity.entityId) }) { Text("Activate scene") }
        "script" -> FilledTonalButton(onClick = { vm.turnOn(entity.entityId) }) { Text("Run script") }
        "automation" -> AutomationControl(entity, vm)
        "todo" -> TodoControl(entity, vm)
        "input_boolean" -> Row(verticalAlignment = Alignment.CenterVertically) {
            PillToggle(entity.state == "on", { vm.toggle(entity.entityId) }, accentFor(entity.entityId))
            Spacer(Modifier.width(12.dp))
            Text(if (entity.state == "on") "On" else "Off", fontWeight = FontWeight.SemiBold)
        }
        "input_number" -> InputNumberControl(entity, vm)
        "input_select" -> InputSelectControl(entity, vm)
        "input_text" -> InputTextControl(entity, vm)
        "input_datetime" -> InputDatetimeControl(entity, vm)
        "timer" -> TimerControl(entity, vm)
        "counter" -> CounterControl(entity, vm)
        "alarm_control_panel" -> AlarmControl(entity, vm)
        "person", "device_tracker" -> Text(
            if (entity.state == "home") "Home" else entity.stateText(),
            fontWeight = FontWeight.SemiBold,
        )
        else -> Text(entity.stateText(), fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Sensor card, rebuilt human-readable (spec item 7): polygon state badge
 * + current value + smoothed cubic-bezier sparkline with gradient alpha
 * area fill + Min/Current/Max row. NEVER a raw entity id or a raw ISO UTC
 * timestamp on the card face — those live only in detail/attribute views.
 * History comes from the existing fetchHistory path; non-numeric sensors
 * (and binary sensors) show badge + value only.
 */
@Composable
fun SensorTile(entity: HaEntity, vm: HavenViewModel, showHeader: Boolean = true, showChart: Boolean = true) {
    val accent = accentFor(entity.entityId)
    val numeric = entity.state.toDoubleOrNull()
    val unit = entity.attrString("unit_of_measurement")
    // Cached plot (same cache as the detail strip): re-entry shows
    // the last plot immediately, refreshed silently in the background.
    val history = vm.historyFor(entity.entityId) ?: emptyList()
    LaunchedEffect(entity.entityId) {
        if (numeric != null) vm.refreshHistory(entity.entityId)
    }
    Column {
        // Detail pop-ups pass showHeader = false: the pop-up header
        // already shows icon + name + value once (2026-10-29
        // verdict: no repeated icon+value rows below the header), so
        // the detail shows only the sparkline + Min/Current/Max.
        // Cards and sheets keep the header row.
        if (showHeader) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 48dp badge (up from 40dp): the shape background clears
            // the value text beside it even while pulsing/rotating.
            DomainBadge(entity, domainIcon(entity), accent, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entity.stateText(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val deviceClass = entity.attrString("device_class")
                if (deviceClass != null) {
                    Text(deviceClass.replace('_', ' '), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        }
        // Smooth chart reveal (2026-10-30 verdict: no empty-box
        // snap): the chart fades in once history lands instead of
        // popping. Detail passes showChart = false — its labeled
        // strip is the single chart (see DetailHistoryStrip).
        val chartReady = showChart && numeric != null && history.size >= 2
        val chartAlpha by animateFloatAsState(
            if (chartReady) 1f else 0f,
            animationSpec = tween(300),
            label = "sensorChartReveal",
        )
        if (chartReady) {
            Column(Modifier.alpha(chartAlpha)) {
                Spacer(Modifier.height(12.dp))
                Sparkline(history, accent, Modifier.fillMaxWidth().height(64.dp))
                val mm = sparklineMinCurrentMax(history)
                if (mm != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Min ${fmtSensor(mm.first, unit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Current ${fmtSensor(mm.second, unit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Max ${fmtSensor(mm.third, unit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Whole numbers print whole; the rest get one decimal; unit suffix when present. */
private fun fmtSensor(v: Double, unit: String?): String {
    val num = if (v == v.toInt().toDouble()) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
    return if (unit != null) "$num $unit" else num
}

/**
 * Shared hourly-forecast fetch: the `forecast` attribute when present,
 * else WS `weather/get_forecasts` (hourly, return_response). Used by
 * both WeatherControl (rows) and ForecastTimeline (timeline) so the
 * two presentations never fetch twice or diverge. When [enabled] is
 * false nothing is fetched (Home hides the forecast).
 */
@Composable
private fun rememberForecast(entity: HaEntity, vm: HavenViewModel, enabled: Boolean): List<Map<String, Any?>> {
    var forecast by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    LaunchedEffect(entity.entityId, enabled) {
        if (!enabled) return@LaunchedEffect
        val attr = entity.attrList("forecast")?.mapNotNull { it as? Map<String, Any?> }
        if (!attr.isNullOrEmpty()) {
            forecast = attr
        } else {
            vm.client.sendCommand(
                "call_service",
                build = {
                    it.put("domain", "weather")
                    it.put("service", "get_forecasts")
                    it.put("target", JSONObject().put("entity_id", entity.entityId))
                    it.put("service_data", JSONObject().put("type", "hourly"))
                    it.put("return_response", true)
                },
                onResult = { msg ->
                    val response = msg.optJSONObject("result")?.optJSONObject("response")
                    val arr: JSONArray? = when {
                        response == null -> null
                        response.has(entity.entityId) -> response.optJSONObject(entity.entityId)?.optJSONArray("forecast")
                        response.has("forecast") -> response.optJSONArray("forecast")
                        else -> null
                    }
                    if (arr != null) {
                        val list = mutableListOf<Map<String, Any?>>()
                        for (i in 0 until arr.length()) {
                            val item = HaEntity.jsonToAny(arr.optJSONObject(i)) as? Map<String, Any?>
                            if (item != null) list.add(item)
                        }
                        Handler(Looper.getMainLooper()).post { forecast = list }
                    }
                },
            )
        }
    }
    return forecast
}

@Composable
fun WeatherControl(entity: HaEntity, vm: HavenViewModel, showForecast: Boolean = true, showDial: Boolean = true, forecastLimit: Int = 6) {
    val temp = entity.attrDouble("temperature") ?: 0.0
    val humidity = entity.attrDouble("humidity")
    val forecast = rememberForecast(entity, vm, showForecast)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showDial) {
            // The dial sits on a condition-tinted panel: the condition
            // color is the hero, not an accent line — the panel behind the
            // dial reads instantly as "sunny amber" or "rainy blue".
            // Arc travel (2026-10-29 verdict: "weather panel all
            // elements should transition up to the popup panel"):
            // this panel reports its rect as the traveling arc's
            // source (cards) and target (measurement frames), and
            // dissolves into the traveling arc while inside a morph
            // surface — so the card's arc visibly moves into the
            // pop-up instead of vanishing and fading back in as a
            // second copy. The humidity line below is destination
            // layout and fades in late with the rest of the content.
            val conditionColor = weatherConditionColor(entity.state)
            Box(
                Modifier.reportArcBounds(entity.entityId).dissolveIntoTraveler()
                    .background(conditionColor.copy(alpha = 0.14f), MaterialTheme.shapes.extraLarge).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Dial(
                    value = temp.toFloat(),
                    range = -10f..40f,
                    onValueChange = {},
                    centerText = temp.formatTemp(),
                    centerSub = formatCondition(entity.state),
                    accent = conditionColor,
                )
            }
            if (humidity != null) {
                Text("Humidity ${humidity.toInt()}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Home hides the forecast rows (showForecast = false): the dial,
        // condition, and humidity above are the whole Weather section there.
        // Sheets, Boards, and detail surfaces keep the rows.
        if (showForecast) {
            Spacer(Modifier.height(8.dp))
            for (item in forecast.take(forecastLimit)) {
            val whenRaw = (item["datetime"] as? String) ?: ""
            val whenLabel = friendlyHourLabel(whenRaw)
            val fTemp = (item["temperature"] as? Number)?.toDouble()
            val condition = (item["condition"] as? String) ?: ""
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Expressive chip: the hour's condition as a small
                    // condition-colored polygon badge (sun / wave /
                    // cloud by condition family) with a white glyph —
                    // not a bare monochrome icon. Tight 8dp spacing.
                    val chipColor = weatherConditionColor(condition)
                    Box(
                        Modifier.size(28.dp).background(chipColor, polygonShape(forecastChipKind(condition))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            conditionIcon(condition),
                            contentDescription = condition,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(whenLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(formatCondition(condition), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (fTemp != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(fTemp.formatTemp(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Hourly forecast as a scrollable TIMELINE (2026-10-29 verdict: the
 * 24-row plain list "just doesn't look very good — a forecast could
 * be a timeline"). One node per hour: hour label, condition-colored
 * polygon badge on a continuous track, temperature, condition word.
 * The track is drawn per node (each node's 2dp segment spans its full
 * 72dp width, nodes abut with zero spacing) so the line runs unbroken
 * across hours and changes color with the condition. Same forecast
 * source and condition coloring as WeatherControl's rows; only the
 * full weather page uses the timeline — sheets, Boards, and detail
 * surfaces keep their rows.
 */
@Composable
fun ForecastTimeline(entity: HaEntity, vm: HavenViewModel) {
    val forecast = rememberForecast(entity, vm, true).take(24)
    if (forecast.isEmpty()) return
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(forecast) { item ->
            val whenRaw = (item["datetime"] as? String) ?: ""
            // Timeline nodes show the HOUR only (2026-10-29
            // observation, not a user directive): friendlyHourLabel's
            // "Tomorrow 10 PM" truncates to "Tomorro..." in the node,
            // repeated for every node. Rows elsewhere keep the
            // day-qualified label. Nodes widened 72dp -> 88dp and the
            // condition text dropped to 10sp (2026-10-30 verdict:
            // clipped temps / truncated descriptions at the edge).
            val whenLabel = timelineHourLabel(whenRaw)
            val fTemp = (item["temperature"] as? Number)?.toDouble()
            val condition = (item["condition"] as? String) ?: ""
            val chipColor = weatherConditionColor(condition)
            Column(
                Modifier.width(88.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(whenLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.fillMaxWidth().height(2.dp).background(chipColor.copy(alpha = 0.35f)))
                    Box(
                        Modifier.size(40.dp).background(chipColor, polygonShape(forecastChipKind(condition))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            conditionIcon(condition),
                            contentDescription = condition,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (fTemp != null) {
                    Text(fTemp.formatTemp(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(formatCondition(condition), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Forecast chip polygon by condition family: sun for clear, wave for wet, cloud otherwise. */
private fun forecastChipKind(condition: String): ExpressiveKind = when (condition.lowercase()) {
    "sunny", "clear", "clear-night" -> ExpressiveKind.SUNNY
    "rainy", "pouring", "lightning", "lightning-rainy", "hail", "snowy", "snowy-rainy" -> ExpressiveKind.WAVE
    else -> ExpressiveKind.CLOUD
}

/** "2026-10-28T23:00:00" -> "11 PM", hour only, for timeline nodes (see ForecastTimeline). Falls back to the raw string. */
private fun timelineHourLabel(raw: String): String {
    val parsed = parseWeatherTime(raw) ?: return raw.replace('T', ' ').take(16)
    return SimpleDateFormat("h a", Locale.US).format(parsed)
}

/** "2026-10-28T23:00:00" -> "11 PM", "Tomorrow 11 PM", or "Thu 11 PM". Falls back to the raw string. */
private fun friendlyHourLabel(raw: String): String {
    val parsed = parseWeatherTime(raw) ?: return raw.replace('T', ' ').take(16)
    val hour = SimpleDateFormat("h a", Locale.US).format(parsed)
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = parsed }
    val sameDay = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return hour
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrow.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
        tomorrow.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    if (isTomorrow) return "Tomorrow $hour"
    return "${SimpleDateFormat("EEE", Locale.US).format(parsed)} $hour"
}

private fun parseWeatherTime(raw: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
    )
    for (pattern in patterns) {
        try {
            return SimpleDateFormat(pattern, Locale.US).parse(raw)
        } catch (e: Exception) {
            // Try the next pattern.
        }
    }
    return null
}

private fun conditionIcon(condition: String): ImageVector = when (condition) {
    "sunny", "clear" -> Icons.Filled.WbSunny
    "clear-night" -> Icons.Filled.Nightlight
    "partlycloudy" -> Icons.Filled.WbCloudy
    "cloudy" -> Icons.Filled.Cloud
    "rainy", "pouring", "hail" -> Icons.Filled.Umbrella
    "snowy", "snowy-rainy" -> Icons.Filled.AcUnit
    "lightning", "lightning-rainy" -> Icons.Filled.Thunderstorm
    "windy", "windy-variant" -> Icons.Filled.Air
    "fog" -> Icons.Filled.Cloud
    else -> Icons.Filled.WbSunny
}

@Composable
/**
 * Camera card: snapshot plus refresh. Tapping the snapshot opens the
 * app-level fullscreen viewer (vm-level state, landscape-locked) instead
 * of a local Dialog — the old Dialog could not rotate the screen, so a
 * portrait phone showed a sideways letterboxed feed.
 */
fun CameraControl(entity: HaEntity, vm: HavenViewModel) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(entity.entityId, refreshKey) {
        bitmap = vm.client.fetchCameraBitmap(entity.entityId)
    }
    Column {
        if (bitmap != null) {
            Box(
                Modifier.fillMaxWidth().height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                    .clickable { vm.openCameraFullscreen(entity.entityId) },
            ) {
                Image(
                    bitmap!!.asImageBitmap(),
                    contentDescription = entity.friendlyName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            // Same 180dp as the loaded image: the sheet panel's target
            // bounds must not change when the snapshot arrives, or the
            // panel jumps mid-morph (the old 120dp placeholder grew to
            // 180dp on load — one of the jumpy recalculations in the
            // 2026-10-29 evidence video).
            Box(
                Modifier.fillMaxWidth().height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Text("No snapshot yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { refreshKey++ }) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Refresh snapshot")
        }
    }
}

@Composable
fun AutomationControl(entity: HaEntity, vm: HavenViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillToggle(entity.state == "on", { vm.toggle(entity.entityId) }, accentFor(entity.entityId))
        Spacer(Modifier.weight(1f))
        FilledTonalButton(onClick = { vm.callService("automation", "trigger", entity.entityId) }) { Text("Run") }
    }
}

@Composable
fun TodoControl(entity: HaEntity, vm: HavenViewModel) {
    var items by remember { mutableStateOf<JSONArray?>(null) }
    var newItem by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(entity.entityId, refreshKey) {
        vm.client.fetchTodoItems(entity.entityId) { arr ->
            Handler(Looper.getMainLooper()).post { items = arr }
        }
    }
    Column {
        val arr = items
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val uid = item.optString("uid")
                val summary = item.optString("summary")
                val done = item.optString("status") == "completed"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = done,
                        onCheckedChange = { checked ->
                            vm.client.todoUpdate(entity.entityId, uid, if (checked) "completed" else "needs_action")
                            refreshKey++
                        },
                    )
                    Text(summary, modifier = Modifier.weight(1f), fontSize = 15.sp)
                    IconButton(onClick = {
                        vm.client.todoDelete(entity.entityId, uid)
                        refreshKey++
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else {
            Text("Loading tasks…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                label = { Text("New task") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newItem.isNotBlank()) {
                    vm.client.todoCreate(entity.entityId, newItem.trim())
                    newItem = ""
                    refreshKey++
                }
            }) { Text("Add") }
        }
    }
}

@Composable
fun InputNumberControl(entity: HaEntity, vm: HavenViewModel) {
    val value = entity.state.toDoubleOrNull() ?: 0.0
    val min = entity.attrDouble("min")?.toFloat() ?: 0f
    val max = entity.attrDouble("max")?.toFloat() ?: 100f
    val step = entity.attrDouble("step")?.toFloat() ?: 1f
    Column {
        Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        ServiceSlider(value.toFloat(), min..max, {
            vm.callService("input_number", "set_value", entity.entityId, JSONObject().put("value", it.toDouble()))
        }, accentFor(entity.entityId))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                vm.callService("input_number", "set_value", entity.entityId, JSONObject().put("value", (value - step).coerceAtLeast(min.toDouble())))
            }) { Text("−") }
            OutlinedButton(onClick = {
                vm.callService("input_number", "set_value", entity.entityId, JSONObject().put("value", (value + step).coerceAtMost(max.toDouble())))
            }) { Text("+") }
        }
    }
}

@Composable
fun InputSelectControl(entity: HaEntity, vm: HavenViewModel) {
    val options = entity.attrList("options")?.mapNotNull { it as? String } ?: emptyList()
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(entity.state) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        vm.callService("input_select", "select_option", entity.entityId, JSONObject().put("option", option))
                    },
                )
            }
        }
    }
}

@Composable
fun InputTextControl(entity: HaEntity, vm: HavenViewModel) {
    var text by remember(entity.state) { mutableStateOf(entity.state) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            vm.callService("input_text", "set_value", entity.entityId, JSONObject().put("value", text))
        }) { Text("Set") }
    }
}

@Composable
fun InputDatetimeControl(entity: HaEntity, vm: HavenViewModel) {
    var text by remember(entity.state) { mutableStateOf(entity.state) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            vm.callService("input_datetime", "set_datetime", entity.entityId, JSONObject().put("datetime", text))
        }) { Text("Set") }
    }
}

@Composable
fun TimerControl(entity: HaEntity, vm: HavenViewModel) {
    val remaining = entity.attrString("remaining") ?: entity.state
    Column {
        Text(remaining, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(entity.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.callService("timer", "start", entity.entityId) }) { Text("Start") }
            FilledTonalButton(onClick = { vm.callService("timer", "pause", entity.entityId) }) { Text("Pause") }
            OutlinedButton(onClick = { vm.callService("timer", "cancel", entity.entityId) }) { Text("Cancel") }
        }
    }
}

@Composable
fun CounterControl(entity: HaEntity, vm: HavenViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entity.state, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { vm.callService("counter", "decrement", entity.entityId) }) { Text("−") }
        OutlinedButton(onClick = { vm.callService("counter", "increment", entity.entityId) }) { Text("+") }
        OutlinedButton(onClick = { vm.callService("counter", "reset", entity.entityId) }) { Text("Reset") }
    }
}

@Composable
fun AlarmControl(entity: HaEntity, vm: HavenViewModel) {
    var code by remember { mutableStateOf("") }
    Column {
        Text(entity.stateText(), fontWeight = FontWeight.SemiBold)
        Text("Code: ${"•".repeat(code.length)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
        for (row in digits.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (digit in row) {
                    FilledTonalButton(
                        onClick = {
                            when (digit) {
                                "C" -> code = ""
                                "⌫" -> code = code.dropLast(1)
                                else -> code += digit
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(digit) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val codeData = if (code.isNotEmpty()) JSONObject().put("code", code) else null
            Button(onClick = { vm.callService("alarm_control_panel", "alarm_arm_home", entity.entityId, codeData) }) { Text("Arm home") }
            Button(onClick = { vm.callService("alarm_control_panel", "alarm_arm_away", entity.entityId, codeData) }) { Text("Arm away") }
            OutlinedButton(onClick = { vm.callService("alarm_control_panel", "alarm_disarm", entity.entityId, codeData) }) { Text("Disarm") }
        }
    }
}
