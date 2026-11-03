package com.garrietta.haven.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.energySlotForWatts
import com.garrietta.haven.data.semanticVisual
import com.garrietta.haven.ui.components.isDarkBackdrop

/**
 * Stage F (redesign): the two first-class destinations from brief §4 —
 * Scenes (§10) and Energy (§11) — as full screens on the pill bar, not
 * sub-pages of Settings. Both render ONLY real Home Assistant states:
 * no sample scenes, no fake charts, honest empty states.
 */

private fun formatWatts(watts: Double): String =
    if (watts == watts.toInt().toDouble()) watts.toInt().toString() else "%.1f".format(watts)

private fun formatReading(value: Double, unit: String): String =
    if (value == value.toInt().toDouble()) "${value.toInt()} $unit" else "%.1f $unit".format(value)

/** Activation feedback per scene: the call's own result, never "active". */
private enum class SceneSendState { IDLE, SENDING, SENT, FAILED }

@Composable
fun ScenesScreen(vm: HavenViewModel) {
    val states by vm.states.collectAsState()
    val scenes = states.values
        .filter { it.domain == "scene" }
        .sortedBy { it.friendlyName }
    var sendStates by remember { mutableStateOf<Map<String, SceneSendState>>(emptyMap()) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text("Scenes", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scenes from your Home Assistant instance. Activating a scene sets your devices to its saved states.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
        if (scenes.isEmpty()) {
            item {
                Text(
                    "No scenes found on your Home Assistant instance. Scenes you create there appear here automatically.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (scene in scenes) {
            item {
                val sendState = sendStates[scene.entityId] ?: SceneSendState.IDLE
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            scene.friendlyName,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        when (sendState) {
                            SceneSendState.IDLE -> Button(onClick = {
                                sendStates = sendStates + (scene.entityId to SceneSendState.SENDING)
                                vm.activateScene(scene.entityId) { ok ->
                                    sendStates = sendStates + (scene.entityId to if (ok) SceneSendState.SENT else SceneSendState.FAILED)
                                }
                            }) { Text("Activate") }
                            SceneSendState.SENDING -> OutlinedButton(onClick = {}, enabled = false) { Text("Sending…") }
                            SceneSendState.SENT -> OutlinedButton(onClick = {}, enabled = false) { Text("Sent") }
                            SceneSendState.FAILED -> Button(onClick = {
                                sendStates = sendStates + (scene.entityId to SceneSendState.SENDING)
                                vm.activateScene(scene.entityId) { ok ->
                                    sendStates = sendStates + (scene.entityId to if (ok) SceneSendState.SENT else SceneSendState.FAILED)
                                }
                            }) { Text("Failed — retry") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun powerSensors(states: Map<String, HaEntity>): List<HaEntity> =
    states.values.filter {
        it.domain == "sensor" && it.attrString("device_class") == "power" && it.state.toDoubleOrNull() != null
    }.sortedByDescending { it.state.toDouble() }

private fun energySensors(states: Map<String, HaEntity>): List<HaEntity> =
    states.values.filter {
        it.domain == "sensor" && it.attrString("device_class") == "energy" && it.state.toDoubleOrNull() != null
    }.sortedBy { it.friendlyName }

@Composable
fun EnergyScreen(vm: HavenViewModel) {
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val areaName: (String) -> String? = { entityId ->
        entityAreas[entityId]?.let { areaId -> areas.firstOrNull { it.id == areaId }?.name }
    }
    val power = powerSensors(states)
    val energy = energySensors(states)
    val totalWatts = power.sumOf { it.state.toDouble() }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text("Energy", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                "Live readings from your Home Assistant sensors — current draw, per-device breakdown, and cumulative totals.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
        if (power.isEmpty() && energy.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No energy data", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No power or energy sensors were found in your Home Assistant states. " +
                                "Add power-monitoring plugs or an energy integration and their readings will appear here. " +
                                "Nothing is estimated or charted from missing data.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (power.isNotEmpty()) {
            item {
                // The total wears its semantic slot (low/high/critical
                // by the same documented thresholds as the resolver).
                val visual = semanticVisual(
                    energySlotForWatts(totalWatts),
                    isDarkBackdrop(MaterialTheme.colorScheme.background),
                )
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = visual.container),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Power now", fontWeight = FontWeight.SemiBold, color = visual.onContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatWatts(totalWatts)} W across ${power.size} sensor" + if (power.size == 1) "" else "s",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = visual.onContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Devices drawing power", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            for (sensor in power) {
                item {
                    EnergyRow(
                        name = sensor.friendlyName,
                        value = formatReading(sensor.state.toDouble(), sensor.attrString("unit_of_measurement") ?: "W"),
                        area = areaName(sensor.entityId),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        if (energy.isNotEmpty()) {
            item {
                Text("Cumulative energy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            for (sensor in energy) {
                item {
                    EnergyRow(
                        name = sensor.friendlyName,
                        value = formatReading(sensor.state.toDouble(), sensor.attrString("unit_of_measurement") ?: "kWh"),
                        area = areaName(sensor.entityId),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EnergyRow(name: String, value: String, area: String?) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (area != null) {
                    Text(area, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
