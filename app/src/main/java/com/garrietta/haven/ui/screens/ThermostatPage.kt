package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.controls.applyClimateTarget
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.stateText
import org.json.JSONObject

private val HEAT_COLOR = Color(0xFFD84315)
private val COOL_COLOR = Color(0xFF1565C0)

/**
 * The thermostat as a full page, not a sheet: a hero dial showing current
 * and target together, a gradient heat/cool range bar with the live
 * operating state, and Mode / Fan / Hold / Sensors / Schedule / Indoor
 * cards below. Every card reads only what Home Assistant exposes — a card
 * with no backing state hides instead of inventing one.
 *
 * The dial logic is the prior round's, unchanged: Fahrenheit values from
 * HA, the midpoint of high/low for heat/cool thermostats, drag clamped to
 * the real setpoint range with the bottom gap ignored, and a dial move
 * shifts both setpoints while preserving their gap.
 */
@Composable
fun ThermostatPageScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    val accent = accentFor(entityId)
    // Target state is hoisted unconditionally (hooks must not sit behind
    // the null check below); it re-initializes whenever HA reports a new
    // setpoint, same as the sheet's dial did.
    val highInit = entity?.attrDouble("target_temp_high")
    val lowInit = entity?.attrDouble("target_temp_low")
    val targetInit = entity?.attrDouble("temperature")
        ?: if (highInit != null && lowInit != null) (highInit + lowInit) / 2.0 else 21.0
    val min = entity?.attrDouble("min_temp")?.toFloat() ?: 10f
    val max = entity?.attrDouble("max_temp")?.toFloat() ?: 30f
    var target by remember(targetInit) { mutableStateOf(targetInit.toFloat().coerceIn(min, max)) }
    val modeColor = when (entity?.state) {
        "heat" -> HEAT_COLOR
        "cool" -> COOL_COLOR
        else -> accent
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        DetailTopBar(entity?.friendlyName ?: entityId, onBack)
        if (entity == null) {
            Text(
                "This entity is no longer available.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (LocalWideLayout.current) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ThermostatHero(entity, vm, target, min, max, modeColor) { target = it }
                    Spacer(Modifier.height(16.dp))
                    HeatCoolRangeBar(entity, min, max)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp)) {
                    ThermostatCards(entity, vm, states)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThermostatHero(entity, vm, target, min, max, modeColor) { target = it }
                Spacer(Modifier.height(16.dp))
                HeatCoolRangeBar(entity, min, max)
                Spacer(Modifier.height(16.dp))
                ThermostatCards(entity, vm, states)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Hero dial: current (gray) + target (large, mode-colored) with −/+ steppers. */
@Composable
private fun ThermostatHero(
    entity: HaEntity,
    vm: HavenViewModel,
    target: Float,
    min: Float,
    max: Float,
    modeColor: Color,
    onTargetChange: (Float) -> Unit,
) {
    val current = entity.attrDouble("current_temperature")
    val step = entity.attrDouble("target_temp_step")?.toFloat() ?: 1f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Dial(
            value = target,
            range = min..max,
            onValueChange = onTargetChange,
            onFinished = { applyClimateTarget(vm, entity, it.toDouble()) },
            centerText = "",
            centerSub = "",
            accent = modeColor,
            diameter = 300.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current != null) {
                    Text(
                        current.formatTemp(),
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    target.toDouble().formatTemp(),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = modeColor,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = {
                val next = (target - step).coerceIn(min, max)
                onTargetChange(next)
                applyClimateTarget(vm, entity, next.toDouble())
            }) { Text("−") }
            OutlinedButton(onClick = {
                val next = (target + step).coerceIn(min, max)
                onTargetChange(next)
                applyClimateTarget(vm, entity, next.toDouble())
            }) { Text("+") }
        }
    }
}

/**
 * Gradient heat ↔ cool bar with setpoint markers, a current-temperature
 * marker, and the live operating state. Single-setpoint thermostats get a
 * neutral track with one setpoint marker instead of a heat/cool gradient.
 */
@Composable
private fun HeatCoolRangeBar(entity: HaEntity, min: Float, max: Float) {
    val low = entity.attrDouble("target_temp_low")
    val high = entity.attrDouble("target_temp_high")
    val single = entity.attrDouble("temperature")
    val current = entity.attrDouble("current_temperature")
    val stateLabel = hvacActionLabel(entity.attrString("hvac_action")) ?: entity.stateText()
    val heatCool = low != null && high != null
    val minD = min.toDouble()
    val maxD = max.toDouble()
    Column(Modifier.fillMaxWidth()) {
        val barBackground = if (heatCool) {
            Modifier.background(Brush.horizontalGradient(listOf(HEAT_COLOR, COOL_COLOR)))
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(50)).then(barBackground),
        ) {
            if (low != null) RangeMarker(rangeFraction(low, minD, maxD), HEAT_COLOR, maxWidth)
            if (high != null) RangeMarker(rangeFraction(high, minD, maxD), COOL_COLOR, maxWidth)
            if (single != null) RangeMarker(rangeFraction(single, minD, maxD), COOL_COLOR, maxWidth)
            if (current != null) RangeMarker(rangeFraction(current, minD, maxD), Color.White, maxWidth)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (low != null) "Heat ${low.formatTemp()}"
                else if (single != null) "Set ${single.formatTemp()}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stateLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (high != null) "Cool ${high.formatTemp()}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A dot on the range bar at [fraction] of the bar width (positive padding only). */
@Composable
private fun RangeMarker(fraction: Float, color: Color, barWidth: Dp) {
    val x = (barWidth * fraction - 6.dp).coerceAtLeast(0.dp)
    Box(
        Modifier.padding(start = x, top = 8.dp).size(12.dp)
            .background(color, RoundedCornerShape(50)),
    )
}

@Composable
private fun ThermostatCard(label: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in options) {
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = {
                    Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() })
                },
            )
        }
    }
}

@Composable
private fun ThermostatCards(entity: HaEntity, vm: HavenViewModel, states: Map<String, HaEntity>) {
    val modes = entity.attrList("hvac_modes")?.mapNotNull { it as? String } ?: emptyList()
    if (modes.isNotEmpty()) {
        ThermostatCard("Mode") {
            ChipRow(modes, entity.state) { vm.setClimateHvacMode(entity.entityId, it) }
        }
        Spacer(Modifier.height(12.dp))
    }
    val fanModes = entity.attrList("fan_modes")?.mapNotNull { it as? String } ?: emptyList()
    if (fanModes.isNotEmpty()) {
        ThermostatCard("Fan") {
            ChipRow(fanModes, entity.attrString("fan_mode")) { vm.setClimateFanMode(entity.entityId, it) }
        }
        Spacer(Modifier.height(12.dp))
    }
    val clearHold = findClearHoldButton(states, entity)
    if (clearHold != null) {
        ThermostatCard("Hold") {
            Text(
                "Manual changes hold until the next scheduled change.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.callService("button", "press", clearHold.entityId) }) {
                Text("Resume schedule")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val sensors = findLinkedSensors(states, entity)
    if (sensors.isNotEmpty()) {
        ThermostatCard("Sensors") {
            for (sensor in sensors) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sensor.friendlyName, fontSize = 14.sp)
                    Text(
                        sensor.stateText(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val preset = findPreset(states, entity)
    val presetModes = entity.attrList("preset_modes")?.mapNotNull { it as? String } ?: emptyList()
    if (preset != null || presetModes.isNotEmpty()) {
        ThermostatCard("Schedule") {
            // HA exposes no next-schedule transition for these thermostats,
            // so the current preset stands in for a schedule line — never
            // an invented transition time.
            if (preset != null) {
                Text(
                    "Current preset: ${preset.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                    fontSize = 14.sp,
                )
            }
            if (presetModes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ChipRow(presetModes, entity.attrString("preset_mode")) {
                    vm.callService(
                        "climate",
                        "set_preset_mode",
                        entity.entityId,
                        JSONObject().put("preset_mode", it),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val current = entity.attrDouble("current_temperature")
    val humidity = entity.attrDouble("humidity")
    if (current != null || humidity != null) {
        ThermostatCard("Indoor") {
            if (current != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temperature", fontSize = 14.sp)
                    Text(
                        current.formatTemp(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (humidity != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Humidity", fontSize = 14.sp)
                    Text(
                        "${humidity.toInt()}%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
