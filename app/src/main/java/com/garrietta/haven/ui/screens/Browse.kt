package com.garrietta.haven.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.SectionHeader
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.stateText

const val UNASSIGNED_AREA = "__unassigned__"

@Composable
fun AreasScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val areas by vm.areas.collectAsState()
    val states by vm.states.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val unassignedCount = states.values.count { entityAreas[it.entityId] == null }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Areas", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }
        items(areas) { area ->
            val count = states.values.count { entityAreas[it.entityId] == area.id }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().clickable { navigate(Screen.AreaDetail(area.id)) },
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(area.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("$count entities", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (unassignedCount > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().clickable { navigate(Screen.AreaDetail(UNASSIGNED_AREA)) },
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Unassigned", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("$unassignedCount entities", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun AreaDetailScreen(vm: HavenViewModel, areaId: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val areas by vm.areas.collectAsState()
    val states by vm.states.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val name = if (areaId == UNASSIGNED_AREA) "Unassigned" else areas.firstOrNull { it.id == areaId }?.name ?: areaId
    val entities = states.values
        .filter { if (areaId == UNASSIGNED_AREA) entityAreas[it.entityId] == null else entityAreas[it.entityId] == areaId }
        .sortedWith(compareBy({ it.domain }, { it.friendlyName }))
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { DetailTopBar(name, onBack) }
        var lastDomain: String? = null
        for (entity in entities) {
            if (entity.domain != lastDomain) {
                lastDomain = entity.domain
                item {
                    SectionHeader(entity.domain.replace('_', ' ').replaceFirstChar { it.uppercase() })
                }
            }
            item {
                EntityControlCard(entity, vm) { navigate(Screen.EntityDetail(it)) }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (entities.isEmpty()) {
            item { Text("Nothing here yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun SearchScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    var query by remember { mutableStateOf("") }
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val results = if (query.isBlank()) {
        emptyList()
    } else {
        val q = query.lowercase()
        states.values.filter { entity ->
            entity.friendlyName.lowercase().contains(q) ||
                entity.entityId.lowercase().contains(q) ||
                entity.domain.lowercase() == q ||
                (entityAreas[entity.entityId]?.let { areaId -> areas.firstOrNull { it.id == areaId }?.name?.lowercase()?.contains(q) } == true)
        }.sortedBy { it.friendlyName }.take(60)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Search", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Name, entity id, area, or domain") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        if (query.isBlank()) {
            Text("Search your home by name, entity id, area, or domain.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (results.isEmpty()) {
            Text("Nothing matches \"$query\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(results) { entity ->
                    EntityControlCard(entity, vm) { navigate(Screen.EntityDetail(it)) }
                    Spacer(Modifier.height(12.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun EntityDetailScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { DetailTopBar(entity?.friendlyName ?: entityId, onBack) }
        if (entity == null) {
            item { Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                Text(entity.stateText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accentFor(entityId))
                Spacer(Modifier.height(4.dp))
                Text(entityId, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                // Full control, header taps disabled here (already on the detail screen).
                EntityControlCard(entity, vm) { }
                Spacer(Modifier.height(16.dp))
                Row {
                    OutlinedButton(onClick = { vm.turnOn(entityId) }) { Text("Turn on") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.turnOff(entityId) }) { Text("Turn off") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.toggle(entityId) }) { Text("Toggle") }
                }
                Spacer(Modifier.height(24.dp))
                Text("Attributes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            val attrs = entity.attributes.filterKeys { it != "friendly_name" }.toList().sortedBy { it.first }
            for ((key, value) in attrs) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(key, fontSize = 13.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(valueToString(value), fontSize = 13.sp)
                    }
                }
            }
            if (entity.state.toDoubleOrNull() != null) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Last 24 hours", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    HistorySparkline(vm, entityId)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun valueToString(value: Any?): String = when (value) {
    null -> "—"
    is List<*> -> value.joinToString(", ")
    is Map<*, *> -> value.entries.joinToString(", ") { "${it.key}: ${it.value}" }
    else -> value.toString()
}

@Composable
private fun HistorySparkline(vm: HavenViewModel, entityId: String) {
    var points by remember { mutableStateOf<List<com.garrietta.haven.data.HistoryPoint>>(emptyList()) }
    LaunchedEffect(entityId) {
        points = vm.client.fetchHistory(entityId, 24)
    }
    if (points.size >= 2) {
        val min = points.minOf { it.value }
        val max = points.maxOf { it.value }
        Column {
            Sparkline(points, accentFor(entityId), Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(4.dp))
            Text("Low $min · High $max", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Text("No history yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
