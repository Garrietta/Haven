package com.garrietta.haven.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.ThermostatSetpoints
import com.garrietta.haven.data.fmtDegrees
import com.garrietta.haven.data.movedRange
import com.garrietta.haven.data.setpointStep
import com.garrietta.haven.data.thermostatActivity
import com.garrietta.haven.data.thermostatSetpoints
import com.garrietta.haven.ui.components.DialogTitleText
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.LabeledChoiceRow
import com.garrietta.haven.ui.components.LocalHeroTarget
import com.garrietta.haven.ui.components.LocalPopupDragHandle
import com.garrietta.haven.ui.components.LocalPopupDragRangePx
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.chartLineColor
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.morphHeaderBadgeAlpha
import com.garrietta.haven.ui.components.popupDragDetector
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.entityIcon
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.theme.FamilyColors
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.glow
import com.garrietta.haven.ui.theme.grainGlow
import com.garrietta.haven.ui.theme.leafCard

/**
 * A thermostat's popup: the same header every device popup has, then
 * [ThermostatControl], its last 24 hours and the rows every popup ends
 * with. The Climate tab shows the same panel, so a thermostat looks and
 * works the same wherever it is opened.
 */
@Composable
fun ThermostatPopout(entity: HaEntity, vm: HavenViewModel, onClose: () -> Unit, navigate: ((Screen) -> Unit)? = null) {
    val custom = rememberCardCustom(vm)
    val nameOf = rememberEntityNamer(vm)
    val badge = tileBadgePair(
        custom.tilePresets, custom.domainTilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
        MaterialTheme.colorScheme, accentFor(entity.entityId),
    )
    // Dragging the header pulls the popup down to close it; null outside popups.
    val dragHandle = LocalPopupDragHandle.current
    val dragRangePx = LocalPopupDragRangePx.current
    // The header reports where it lands so the badge travelling from the tile lands on it.
    val heroTarget = LocalHeroTarget.current
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) }
                .then(
                    if (dragHandle != null) {
                        Modifier.popupDragDetector(
                            dragHandle.state,
                            { dy -> dragHandle.onDelta(dy, dragRangePx()) },
                            { v -> dragHandle.onRelease(v, dragRangePx()) },
                        )
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DomainBadge(
                entity,
                entityIcon(custom.iconKeys, entity),
                accentFor(entity.entityId),
                modifier = Modifier.size(48.dp).morphHeaderBadgeAlpha().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClose() },
                badgeOverride = badge,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                DialogTitleText(nameOf(entity), Modifier.fillMaxWidth())
                Text(entity.dialogHeaderState(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        ThermostatControl(entity, vm, showName = false)
        val states by vm.states.collectAsState()
        ThermostatHistorySection(entity, vm, states)
        Spacer(Modifier.height(16.dp))
        EntityPopupMenuRows(vm, entity, onClose = onClose, navigate = navigate, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * The family a thermostat is drawn in: heating or cooling while it is
 * doing so (or set to only that), on while it holds a range, quiet
 * when it is off or gone.
 */
internal fun thermostatFamily(t: HaEntity?): StateFamily = when {
    t == null || t.state == "off" || t.state == "unavailable" -> StateFamily.Quiet
    t.attrString("hvac_action") == "heating" -> StateFamily.Heat
    t.attrString("hvac_action") == "cooling" -> StateFamily.Cool
    t.state == "heat" -> StateFamily.Heat
    t.state == "cool" -> StateFamily.Cool
    else -> StateFamily.On
}

/**
 * A thermostat, controlled, as one panel in the color of what it is
 * doing: its name and activity with the humidity, the temperature large
 * with − and + for its set point (or for heat and cool), then Mode,
 * Comfort and Fan as choice rows, each only where the thermostat reports
 * it. While it heats or cools the panel glows with grain from the
 * corner; at rest it is flat.
 *
 * The popup and the Climate tab both draw this, so a thermostat has one
 * design. Where [onOpen] is given, a tap anywhere on the panel that is
 * not one of its controls opens the popup, and [onLongPress] the tile's
 * options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThermostatControl(
    entity: HaEntity,
    vm: HavenViewModel,
    modifier: Modifier = Modifier,
    shapeIndex: Int = 0,
    showName: Boolean = true,
    onOpen: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val dark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val family = thermostatFamily(entity)
    val fam = family.colors(dark)
    val working = entity.attrString("hvac_action") in setOf("heating", "cooling")
    val shape = leafCard(shapeIndex)
    val nameOf = rememberEntityNamer(vm)
    val name = nameOf(entity)
    val step = setpointStep(entity)
    val current = entity.attrDouble("current_temperature")
    val humidity = entity.attrDouble("current_humidity")
    val ground = if (working) {
        Modifier.grainGlow(hot = family.glow(dark), ground = fam.container, from = Offset(0.88f, 0.12f), reach = 1.2f)
    } else {
        Modifier.background(fam.container)
    }
    val touch = if (onOpen != null || onLongPress != null) {
        Modifier.combinedClickable(
            onClickLabel = "Open $name",
            onLongClickLabel = onLongPress?.let { "Options for $name" },
            onLongClick = onLongPress,
            onClick = { onOpen?.invoke() },
        )
    } else {
        Modifier
    }
    Box(modifier.fillMaxWidth().clip(shape).then(ground).then(touch)) {
        CompositionLocalProvider(LocalContentColor provides fam.onContainer) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (showName) {
                            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(thermostatActivity(entity), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (humidity != null) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${Math.round(humidity)}%", style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        current?.let { "${fmtDegrees(it)}°" } ?: "—",
                        style = MaterialTheme.typography.displayLarge,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    when (val set = thermostatSetpoints(entity)) {
                        ThermostatSetpoints.None -> Unit
                        is ThermostatSetpoints.Single ->
                            SetpointStepper("Set to", set.value, step, fam) { vm.setClimateTemperature(entity.entityId, it) }
                        is ThermostatSetpoints.Range -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SetpointStepper("Heat to", set.low, step, fam) {
                                val (low, high) = movedRange(set.low, set.high, step, newLow = it)
                                vm.setClimateTemperatureRange(entity.entityId, low, high)
                            }
                            SetpointStepper("Cool to", set.high, step, fam) {
                                val (low, high) = movedRange(set.low, set.high, step, newHigh = it)
                                vm.setClimateTemperatureRange(entity.entityId, low, high)
                            }
                        }
                    }
                }
                // The choices sit on the panel: unchosen pills are a wash of
                // the panel's own ink, the chosen one the style's capsule.
                val idle = fam.onContainer.copy(alpha = 0.10f) to fam.onContainer
                val modes = entity.attrList("hvac_modes")?.mapNotNull { it as? String }.orEmpty()
                if (modes.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    LabeledChoiceRow(
                        "Mode", orderedHvacModes(modes).map { it to formatHvacMode(it) }, entity.state,
                        { vm.setClimateHvacMode(entity.entityId, it) },
                        idleColors = idle, labelColor = fam.onContainer,
                    )
                }
                val presets = entity.attrList("preset_modes")?.mapNotNull { it as? String }.orEmpty()
                if (presets.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LabeledChoiceRow(
                        "Comfort", presets.map { it to choiceLabel(it) }, entity.attrString("preset_mode"),
                        { vm.setClimatePresetMode(entity.entityId, it) },
                        idleColors = idle, labelColor = fam.onContainer,
                    )
                }
                val fanModes = entity.attrList("fan_modes")?.mapNotNull { it as? String }.orEmpty()
                if (fanModes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LabeledChoiceRow(
                        "Fan", orderedFanModes(fanModes).map { it to choiceLabel(it) }, entity.attrString("fan_mode"),
                        { vm.setClimateFanMode(entity.entityId, it) },
                        idleColors = idle, labelColor = fam.onContainer,
                    )
                }
            }
        }
    }
}

/**
 * A set point with − and + beside it, in the panel's accent. Each tap
 * moves it one step on screen at once and sends it; the number Home
 * Assistant reports back replaces it.
 */
@Composable
private fun SetpointStepper(label: String, value: Double, step: Double, fam: FamilyColors, onChange: (Double) -> Unit) {
    var shown by remember(value) { mutableStateOf(value) }
    val colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = fam.accent, contentColor = fam.onAccent)
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { shown -= step; onChange(shown) }, colors = colors, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "$label lower")
        }
        Column(Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("${fmtDegrees(shown)}°", style = MaterialTheme.typography.titleLarge)
        }
        FilledTonalIconButton(onClick = { shown += step; onChange(shown) }, colors = colors, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "$label higher")
        }
    }
}

/** "fan_only" reads "Fan only", "home" reads "Home". */
private fun choiceLabel(value: String): String = value.replace('_', ' ').replaceFirstChar { it.uppercase() }

/** Modes in a fixed order (Off, Heat, Cool, Heat and cool, then the rest as reported), so the row reads the same on every thermostat. */
private fun orderedHvacModes(modes: List<String>): List<String> {
    val canonical = listOf("off", "heat", "cool", "heat_cool", "auto", "dry", "fan_only")
    return canonical.filter { it in modes } + modes.filter { it !in canonical }
}

/** Fan modes with Auto and On first, then the rest as reported. */
private fun orderedFanModes(modes: List<String>): List<String> {
    val canonical = listOf("auto", "on")
    return canonical.filter { it in modes } + modes.filter { it !in canonical }
}

/**
 * The thermostat's last 24 hours under its panel in the popup: the
 * temperature and humidity sensors that belong to it ([findLinkedSensors]),
 * each a line with its low and high. A sensor whose history has not
 * arrived shows a shimmer, never an invented line.
 */
@Composable
private fun ThermostatHistorySection(entity: HaEntity, vm: HavenViewModel, states: Map<String, HaEntity>) {
    val sensors = findLinkedSensors(states, entity)
    val temp = sensors.firstOrNull { it.entityId.contains("temperature") }
    val hum = sensors.firstOrNull { it.entityId.contains("humidity") }
    if (temp == null && hum == null) return
    Spacer(Modifier.height(16.dp))
    SectionTitle("Last 24 hours")
    Spacer(Modifier.height(8.dp))
    if (temp != null) ThermostatSensorHistory(temp, vm)
    if (hum != null) ThermostatSensorHistory(hum, vm)
}

@Composable
private fun ThermostatSensorHistory(sensor: HaEntity, vm: HavenViewModel) {
    val cached = vm.historyFor(sensor.entityId)
    LaunchedEffect(sensor.entityId) { vm.refreshHistory(sensor.entityId) }
    val unit = sensor.attrString("unit_of_measurement")
    Column(Modifier.fillMaxWidth()) {
        Text(sensorDisplayName(sensor), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(sensor.stateText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val chartHeight = com.garrietta.haven.ui.components.historyChartHeight(maxWidth) * 0.7f
            if (cached == null) {
                ShimmerBox(Modifier.fillMaxWidth().height(chartHeight))
            } else if (cached.size >= 2) {
                Column {
                    Sparkline(cached, chartLineColor(sensorKind(sensor.entityId, sensor.attrString("device_class"), sensor.friendlyName), isDarkBackdrop(MaterialTheme.colorScheme.background)), Modifier.fillMaxWidth().height(chartHeight))
                    val mm = sparklineMinCurrentMax(cached)
                    if (mm != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Low ${fmtThermostatHistory(mm.first, unit)} · High ${fmtThermostatHistory(mm.third, unit)} over 24 hours",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (cached != null && cached.size < 2) {
            Text("No history yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun fmtThermostatHistory(v: Double, unit: String?): String = com.garrietta.haven.data.formatReading(v.toString(), unit, null)

