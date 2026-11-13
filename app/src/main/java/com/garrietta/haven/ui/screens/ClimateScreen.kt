package com.garrietta.haven.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ClimateReading
import com.garrietta.haven.data.ClimateSummary
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.climateReadings
import com.garrietta.haven.data.climateSummary
import com.garrietta.haven.data.climateThermostats
import com.garrietta.haven.data.comfortPosition
import com.garrietta.haven.data.fmtDegrees
import com.garrietta.haven.data.hvacModeLabel
import com.garrietta.haven.data.presetLabel
import com.garrietta.haven.data.setpointStep
import com.garrietta.haven.data.thermostatActivity
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.ScreenTitle
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.SensorKind
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.chartLineColor
import com.garrietta.haven.ui.components.choiceChipColors
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.theme.FamilyColors
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.leafCard

/*
 * The Climate tab: how warm and how damp the house is, room by room,
 * and the thermostats that keep it so.
 *
 * At the top, the house in one card: the average inside, the humidity,
 * the outside, and what the thermostat is doing, in the color of what
 * it is doing (heating ember, cooling frost). Under it each thermostat
 * with its set point a tap from changing, its modes, its comfort
 * settings (an ecobee's Home, Away, Sleep) and its fan. Then every
 * temperature and humidity reading, one card a device, each with where
 * it sits between cold and hot and its last day as a line. Outdoor
 * readings and the weather stand apart at the end.
 */
@Composable
fun ClimateScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val auxiliary by vm.auxiliaryEntities.collectAsState()
    val areas by vm.areas.collectAsState()
    val areaNames = remember(areas) { areas.associate { it.id to it.name } }
    val thermostats = climateThermostats(states)
    val readings = climateReadings(states, entityDevices, deviceInfo.mapValues { it.value.name }, entityAreas, auxiliary, areaNames)
    val indoor = readings.filterNot { it.outdoor }
    val outdoor = readings.filter { it.outdoor }
    val weather = states.values.firstOrNull { it.domain == "weather" }
    val summary = climateSummary(readings, thermostats, weather)
    val dark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val wide = LocalWideLayout.current
    val columns = if (wide) 3 else 2

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "head") {
            ScreenTitle("Climate")
            Spacer(Modifier.height(12.dp))
            // Unfolded, the house and the first thermostat sit side by side
            // rather than each stretching across the whole width.
            val first = thermostats.firstOrNull()
            if (wide && first != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HouseCard(summary, first, dark, Modifier.weight(1f))
                    ThermostatCard(first, vm, dark, 0, onOpen = { navigate(Screen.EntityDetail(first.entityId)) }, Modifier.weight(1.3f))
                }
            } else {
                HouseCard(summary, first, dark)
            }
            Spacer(Modifier.height(20.dp))
        }
        if (thermostats.isEmpty() && readings.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No thermostats or temperature sensors in Home Assistant yet. Those you add there appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        thermostats.forEachIndexed { index, thermostat ->
            if (wide && index == 0) return@forEachIndexed
            item(key = "thermostat-" + thermostat.entityId) {
                ThermostatCard(thermostat, vm, dark, index, onOpen = { navigate(Screen.EntityDetail(thermostat.entityId)) })
                Spacer(Modifier.height(12.dp))
            }
        }
        if (indoor.isNotEmpty()) {
            item(key = "rooms-title") {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Rooms")
                Spacer(Modifier.height(8.dp))
            }
            readingRows(indoor, columns, "rooms", vm, areaNames, dark, navigate)
        }
        if (outdoor.isNotEmpty() || weather != null) {
            item(key = "outside-title") {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Outside")
                Spacer(Modifier.height(8.dp))
            }
            if (weather != null) {
                item(key = "weather") {
                    WeatherCard(weather, dark, onOpen = { navigate(Screen.EntityDetail(weather.entityId)) })
                    Spacer(Modifier.height(8.dp))
                }
            }
            readingRows(outdoor, columns, "outside", vm, areaNames, dark, navigate)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.readingRows(
    readings: List<ClimateReading>,
    columns: Int,
    keyPrefix: String,
    vm: HavenViewModel,
    areaNames: Map<String, String>,
    dark: Boolean,
    navigate: (Screen) -> Unit,
) {
    readings.chunked(columns).forEachIndexed { row, group ->
        item(key = "$keyPrefix-${group.first().key}") {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEachIndexed { index, reading ->
                    ReadingCard(
                        reading,
                        areaName = reading.areaId?.let(areaNames::get),
                        vm = vm,
                        dark = dark,
                        shapeIndex = row * columns + index,
                        onOpen = {
                            val id = reading.temperature?.entityId ?: reading.humidity?.entityId
                            if (id != null) navigate(Screen.EntityDetail(id))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The color of what a thermostat is doing: ember heating, frost cooling, quiet at rest or off. */
private fun thermostatFamily(t: HaEntity?): StateFamily = when {
    t == null || t.state == "off" || t.state == "unavailable" -> StateFamily.Quiet
    t.attrString("hvac_action") == "heating" -> StateFamily.Heat
    t.attrString("hvac_action") == "cooling" -> StateFamily.Cool
    t.state == "heat" -> StateFamily.Heat
    t.state == "cool" -> StateFamily.Cool
    else -> StateFamily.On
}

/** The house in one card: inside, humidity, outside, and what the thermostat is doing. */
@Composable
private fun HouseCard(summary: ClimateSummary, thermostat: HaEntity?, dark: Boolean, modifier: Modifier = Modifier) {
    val fam = thermostatFamily(thermostat).colors(dark)
    Surface(color = fam.container, contentColor = fam.onContainer, shape = leafCard(0), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Inside", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    summary.inside?.let { "${fmtDegrees(Math.round(it * 2) / 2.0)}°" } ?: "—",
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.padding(bottom = 10.dp)) {
                    if (summary.humidity != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${Math.round(summary.humidity)}% humidity", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    if (summary.outside != null) {
                        Text("${fmtDegrees(Math.round(summary.outside).toDouble())}° outside", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            if (thermostat != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(fam.accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Thermostat, contentDescription = null, tint = fam.onAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(thermostatActivity(thermostat), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * One thermostat: what it reads and what it is doing, its set point (or
 * its heat and cool pair) with − and + beside it, and its modes, comfort
 * settings and fan as choices. The dial and its history are a tap away.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThermostatCard(t: HaEntity, vm: HavenViewModel, dark: Boolean, index: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val fam = thermostatFamily(t).colors(dark)
    val step = setpointStep(t)
    val current = t.attrDouble("current_temperature")
    val humidity = t.attrDouble("current_humidity")
    Surface(color = fam.container, contentColor = fam.onContainer, shape = leafCard(index + 1), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.friendlyName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(thermostatActivity(t), style = MaterialTheme.typography.bodyMedium)
                }
                if (humidity != null) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${Math.round(humidity)}%", style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    current?.let { "${fmtDegrees(it)}°" } ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clickable(onClick = onOpen),
                )
                Spacer(Modifier.weight(1f))
                val low = t.attrDouble("target_temp_low")
                val high = t.attrDouble("target_temp_high")
                val target = t.attrDouble("temperature")
                when {
                    t.state == "off" -> Unit
                    target != null -> SetpointStepper("Set to", target, step, fam) { vm.setClimateTemperature(t.entityId, it) }
                    low != null && high != null -> Column(horizontalAlignment = Alignment.End) {
                        SetpointStepper("Heat to", low, step, fam) { vm.setClimateTemperatureRange(t.entityId, it.coerceAtMost(high - step), high) }
                        Spacer(Modifier.height(6.dp))
                        SetpointStepper("Cool to", high, step, fam) { vm.setClimateTemperatureRange(t.entityId, low, it.coerceAtLeast(low + step)) }
                    }
                }
            }
            ChoiceRow(
                label = "Mode",
                options = t.attrList("hvac_modes").orEmpty().mapNotNull { it as? String },
                selected = t.state,
                labelOf = ::hvacModeLabel,
            ) { vm.setClimateHvacMode(t.entityId, it) }
            ChoiceRow(
                label = "Comfort",
                options = t.attrList("preset_modes").orEmpty().mapNotNull { it as? String },
                selected = t.attrString("preset_mode"),
                labelOf = ::presetLabel,
            ) { vm.setClimatePresetMode(t.entityId, it) }
            ChoiceRow(
                label = "Fan",
                options = t.attrList("fan_modes").orEmpty().mapNotNull { it as? String },
                selected = t.attrString("fan_mode"),
                labelOf = ::presetLabel,
            ) { vm.setClimateFanMode(t.entityId, it) }
            TextButton(onClick = onOpen, modifier = Modifier.padding(top = 4.dp)) { Text("Dial and history") }
        }
    }
}

/**
 * A set point with − and + beside it. Each tap moves it one step at
 * once on screen and sends it; the number Home Assistant reports back
 * replaces it.
 */
@Composable
private fun SetpointStepper(label: String, value: Double, step: Double, fam: FamilyColors, onChange: (Double) -> Unit) {
    var shown by remember(value) { mutableStateOf(value) }
    val colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = fam.accent, contentColor = fam.onAccent)
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { shown -= step; onChange(shown) }, colors = colors) {
            Icon(Icons.Filled.Remove, contentDescription = "$label lower")
        }
        Column(Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("${fmtDegrees(shown)}°", style = MaterialTheme.typography.titleLarge)
        }
        FilledTonalIconButton(onClick = { shown += step; onChange(shown) }, colors = colors) {
            Icon(Icons.Filled.Add, contentDescription = "$label higher")
        }
    }
}

/** One of a thermostat's sets of choices, as chips, the chosen one the lime capsule. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: String?, labelOf: (String) -> String, onPick: (String) -> Unit) {
    if (options.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text(label, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in options) {
            FilterChip(
                selected = option == selected,
                onClick = { onPick(option) },
                label = { Text(labelOf(option)) },
                colors = choiceChipColors(),
            )
        }
    }
}

/**
 * One place's air: its name and room, the temperature large with the
 * humidity beside it, a bar placing the temperature between cold and
 * hot, and the temperature's last day as a line.
 */
@Composable
private fun ReadingCard(
    reading: ClimateReading,
    areaName: String?,
    vm: HavenViewModel,
    dark: Boolean,
    shapeIndex: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unit = reading.temperature?.attrString("unit_of_measurement")?.removePrefix("°") ?: "F"
    val cool = StateFamily.Cool.colors(dark)
    val heat = StateFamily.Heat.colors(dark)
    val water = StateFamily.Water.colors(dark)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = leafCard(shapeIndex),
        modifier = modifier.clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(reading.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The room, unless the card is already named for it.
            Text(
                areaName?.takeUnless { it.equals(reading.name, ignoreCase = true) } ?: " ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    reading.temperatureValue?.let { "${fmtDegrees(it)}°" } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.weight(1f))
                reading.humidityValue?.let { h ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = "Humidity", tint = water.accent, modifier = Modifier.size(14.dp))
                        Text("${Math.round(h)}%", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            reading.temperatureValue?.let { value ->
                Spacer(Modifier.height(8.dp))
                ComfortBar(comfortPosition(value, unit), cool.accent, heat.accent)
            }
            val sensor = reading.temperature ?: reading.humidity
            if (sensor != null) {
                LaunchedEffect(sensor.entityId) { vm.refreshHistory(sensor.entityId) }
                val history = vm.historyFor(sensor.entityId)
                Spacer(Modifier.height(8.dp))
                when {
                    history == null -> ShimmerBox(Modifier.fillMaxWidth().height(36.dp))
                    history.size >= 2 -> Sparkline(
                        history,
                        chartLineColor(if (reading.temperature != null) SensorKind.TEMPERATURE else SensorKind.HUMIDITY, dark),
                        Modifier.fillMaxWidth().height(36.dp),
                    )
                    else -> Spacer(Modifier.height(36.dp))
                }
            }
        }
    }
}

/** Where a temperature sits between cold and hot: a cool-to-warm track with a dot. */
@Composable
private fun ComfortBar(position: Float, cold: Color, hot: Color) {
    val ring = MaterialTheme.colorScheme.surfaceContainer
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val track = size.height * 0.5f
        val y = size.height / 2
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(cold.copy(alpha = 0.45f), hot.copy(alpha = 0.45f))),
            topLeft = Offset(0f, y - track / 2),
            size = androidx.compose.ui.geometry.Size(size.width, track),
            cornerRadius = CornerRadius(track / 2),
        )
        val x = (size.width - size.height) * position + size.height / 2
        drawCircle(ring, radius = size.height / 2, center = Offset(x, y))
        drawCircle(lerp(cold, hot, position), radius = size.height / 2 - 2.dp.toPx(), center = Offset(x, y))
    }
}

/** The weather outside: the temperature, what it is doing, the humidity and the wind. */
@Composable
private fun WeatherCard(weather: HaEntity, dark: Boolean, onOpen: () -> Unit) {
    val fam = StateFamily.Cool.colors(dark)
    Surface(
        color = fam.container,
        contentColor = fam.onContainer,
        shape = leafCard(2),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(weather.stateText(), style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(
                    weather.attrDouble("humidity")?.let { "${Math.round(it)}% humidity" },
                    weather.attrDouble("wind_speed")?.let { speed ->
                        "Wind ${fmtDegrees(Math.round(speed).toDouble())} ${weather.attrString("wind_speed_unit") ?: ""}".trim()
                    },
                )
                if (details.isNotEmpty()) Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.Air, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                weather.attrDouble("temperature")?.let { "${fmtDegrees(Math.round(it).toDouble())}°" } ?: "—",
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }
}
