package com.garrietta.haven.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.garrietta.haven.data.CLIMATE_READINGS
import com.garrietta.haven.data.CLIMATE_THERMOSTATS
import com.garrietta.haven.data.addedReading
import com.garrietta.haven.data.climateSectionFor
import com.garrietta.haven.data.takenOff
import com.garrietta.haven.data.curated
import com.garrietta.haven.data.ClimateSummary
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.climateReadings
import com.garrietta.haven.data.climateSummary
import com.garrietta.haven.data.climateThermostats
import com.garrietta.haven.data.comfortPosition
import com.garrietta.haven.data.fmtDegrees
import com.garrietta.haven.data.thermostatActivity
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.SensorKind
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.chartLineColor
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.controls.stateText
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
    val page = climatePage(vm)
    var editing by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var addQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    if (editing) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(key = "head") {
                EditableTitle("Climate", editing) { editing = !editing; addQuery = "" }
                Spacer(Modifier.height(12.dp))
            }
            climateEditor(vm, page, addQuery) { addQuery = it }
        }
        return
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(bottom = 16.dp),
    ) {
        EditableTitle("Climate", editing) { editing = !editing; addQuery = "" }
        Spacer(Modifier.height(12.dp))
        ClimateBody(vm, page, navigate, wide = LocalWideLayout.current)
    }
}

/**
 * The Climate page as a popup, opened from Home's Climate card: the same
 * cards as the tab, one column wide, so a tap never has to leave Home.
 */
@Composable
fun ClimatePopupScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val page = climatePage(vm)
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        PagePopupHeader("Climate")
        Spacer(Modifier.height(12.dp))
        ClimateBody(vm, page, navigate, wide = false)
    }
}

/** What the Climate page shows, worked out once for the page, its popup and its editor. */
private class ClimatePage(
    val states: Map<String, HaEntity>,
    val nameOf: (HaEntity) -> String,
    val areaNames: Map<String, String>,
    val thermostatDefaults: List<String>,
    val thermostats: List<HaEntity>,
    val readingDefaults: List<String>,
    val byKey: Map<String, ClimateReading>,
    val readings: List<ClimateReading>,
    val weather: HaEntity?,
) {
    val indoor get() = readings.filterNot { it.outdoor }
    val outdoor get() = readings.filter { it.outdoor }
}

@Composable
private fun climatePage(vm: HavenViewModel): ClimatePage {
    val states by vm.states.collectAsState()
    val nameOf = rememberEntityNamer(vm)
    val entityDevices by vm.entityDevices.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val auxiliary by vm.auxiliaryEntities.collectAsState()
    val areas by vm.areas.collectAsState()
    val areaNames = remember(areas) { areas.associate { it.id to it.name } }
    // Both sections show what Home Assistant has, arranged by hand
    // (PageCuration): taken off, added to and put in order in Edit.
    val exists: (String) -> Boolean = { states[it] != null }
    val thermostatDefaults = climateThermostats(states).map { it.entityId }
    val thermostats = curated(thermostatDefaults, vm.curation(CLIMATE_THERMOSTATS), exists).mapNotNull(states::get)
    val found = climateReadings(
        states, entityDevices, deviceInfo.mapValues { it.value.name }, entityAreas, auxiliary, areaNames,
        serviceDevices = deviceInfo.filterValues { it.service }.keys,
    )
    val readingCuration = vm.curation(CLIMATE_READINGS)
    val covered = found.flatMap { listOfNotNull(it.temperature?.entityId, it.humidity?.entityId) }.toSet()
    val byKey = found.associateBy { it.key } +
        readingCuration.added.filter { it !in covered }.mapNotNull(states::get).associate { it.entityId to addedReading(it, entityAreas) }
    val readingDefaults = found.map { it.key }
    val readings = curated(readingDefaults, readingCuration) { it in byKey }.mapNotNull(byKey::get)
    return ClimatePage(
        states, nameOf, areaNames, thermostatDefaults, thermostats, readingDefaults, byKey, readings,
        weather = states.values.firstOrNull { it.domain == "weather" },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.climateEditor(
    vm: HavenViewModel,
    page: ClimatePage,
    query: String,
    onQuery: (String) -> Unit,
) {
    val states = page.states
    val nameOf = page.nameOf
    val readingCuration = vm.curation(CLIMATE_READINGS)
    pageEditor(
        vm,
        sections = listOf(
            EditorSection(
                CLIMATE_THERMOSTATS, "Thermostats",
                page.thermostats.map { editorItem(it, it.entityId, nameOf(it)) },
                takenOff(page.thermostatDefaults, vm.curation(CLIMATE_THERMOSTATS)).map { editorItem(states[it], it, states[it]?.let(nameOf)) },
            ),
            EditorSection(
                CLIMATE_READINGS, "Readings",
                page.readings.map { EditorItem(it.key, it.name, readingSummary(it, it.areaId?.let(page.areaNames::get))) },
                takenOff(page.readingDefaults, readingCuration).mapNotNull { key ->
                    page.byKey[key]?.let { EditorItem(key, it.name, readingSummary(it, it.areaId?.let(page.areaNames::get))) }
                },
            ),
        ),
        candidates = { q ->
            val onPage = (page.thermostats.map { it.entityId } + page.readings.flatMap { listOfNotNull(it.key, it.temperature?.entityId, it.humidity?.entityId, it.other?.entityId) }).toSet()
            if (q.isEmpty()) emptyList() else states.values
                .filter { it.entityId !in onPage && (it.domain == "climate" || it.domain == "sensor") }
                .filter { it.friendlyName.lowercase().contains(q) || it.entityId.contains(q) }
                .sortedBy { it.friendlyName.lowercase() }
                .take(25)
        },
        query = query,
        onQuery = onQuery,
        onAdd = { entity -> vm.updateCuration(climateSectionFor(entity.entityId)) { it.add(entity.entityId) } },
    )
}

/**
 * The Climate page's cards, for the tab and its popup: the house, each
 * thermostat's panel, each room's air, then outside. [wide] sets the
 * house beside the first thermostat and three readings to a row.
 */
@Composable
private fun ClimateBody(vm: HavenViewModel, page: ClimatePage, navigate: (Screen) -> Unit, wide: Boolean) {
    val thermostats = page.thermostats
    val summary = climateSummary(page.readings, thermostats, page.weather)
    val dark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val columns = if (wide) 3 else 2
    // Unfolded, the house and the first thermostat sit side by side
    // rather than each stretching across the whole width.
    val first = thermostats.firstOrNull()
    if (wide && first != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HouseCard(summary, first, dark, Modifier.weight(1f))
            ThermostatSection(first, vm, 0, navigate, Modifier.weight(1.3f))
        }
    } else {
        HouseCard(summary, first, dark)
    }
    Spacer(Modifier.height(20.dp))
    if (thermostats.isEmpty() && page.readings.isEmpty()) {
        Text(
            "No thermostats or temperature sensors in Home Assistant yet. Those you add there appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    thermostats.forEachIndexed { index, thermostat ->
        if (wide && index == 0) return@forEachIndexed
        ThermostatSection(thermostat, vm, index, navigate)
        Spacer(Modifier.height(12.dp))
    }
    if (page.indoor.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        SectionTitle("Rooms")
        Spacer(Modifier.height(8.dp))
        ReadingRows(page.indoor, columns, vm, page.areaNames, dark, navigate)
    }
    val weather = page.weather
    if (page.outdoor.isNotEmpty() || weather != null) {
        Spacer(Modifier.height(8.dp))
        SectionTitle("Outside")
        Spacer(Modifier.height(8.dp))
        if (weather != null) {
            WeatherCard(weather, dark, onOpen = { navigate(Screen.EntityDetail(weather.entityId)) })
            Spacer(Modifier.height(8.dp))
        }
        ReadingRows(page.outdoor, columns, vm, page.areaNames, dark, navigate)
    }
}

@Composable
private fun ReadingRows(
    readings: List<ClimateReading>,
    columns: Int,
    vm: HavenViewModel,
    areaNames: Map<String, String>,
    dark: Boolean,
    navigate: (Screen) -> Unit,
) {
    readings.chunked(columns).forEachIndexed { row, group ->
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
            // A card alone in its row takes the row rather than
            // sitting beside empty space.
            if (group.size > 1) repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
        }
    }
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
 * A thermostat on the Climate tab: its panel ([ThermostatControl]), the
 * same one its popup shows. A tap on the panel away from its controls
 * opens the popup; holding it opens the tile's options.
 */
@Composable
private fun ThermostatSection(t: HaEntity, vm: HavenViewModel, index: Int, navigate: (Screen) -> Unit, modifier: Modifier = Modifier) {
    ThermostatControl(
        t, vm, modifier,
        shapeIndex = index + 1,
        onOpen = { navigate(Screen.EntityDetail(t.entityId)) },
        onLongPress = { vm.openCardSettings(t.entityId) },
    )
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
                    reading.temperatureValue?.let { "${fmtDegrees(it)}°" } ?: reading.other?.stateText() ?: "—",
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
            val sensor = reading.temperature ?: reading.humidity ?: reading.other
            if (sensor != null) {
                LaunchedEffect(sensor.entityId) { vm.refreshHistory(sensor.entityId) }
                val history = vm.historyFor(sensor.entityId)
                Spacer(Modifier.height(8.dp))
                // The sparkline grows with the card: a wider card gets a taller line.
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val sparkHeight = (maxWidth / 4.5f).coerceIn(36.dp, 80.dp)
                    when {
                        history == null -> ShimmerBox(Modifier.fillMaxWidth().height(sparkHeight))
                        history.size >= 2 -> Sparkline(
                            history,
                            chartLineColor(if (reading.temperature != null) SensorKind.TEMPERATURE else SensorKind.HUMIDITY, dark),
                            Modifier.fillMaxWidth().height(sparkHeight),
                        )
                        else -> Spacer(Modifier.height(sparkHeight))
                    }
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


/** A reading as its editor row says it: what it reads, and its room. */
private fun readingSummary(reading: ClimateReading, areaName: String?): String = listOfNotNull(
    reading.temperatureValue?.let { "${fmtDegrees(it)}°" },
    reading.humidityValue?.let { "${Math.round(it)}%" },
    reading.other?.stateText(),
    areaName?.takeUnless { it.equals(reading.name, ignoreCase = true) },
).joinToString(" · ").ifBlank { reading.key }
