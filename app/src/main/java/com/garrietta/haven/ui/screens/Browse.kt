package com.garrietta.haven.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.SectionHeader
import com.garrietta.haven.ui.controls.EntityControlCard

const val UNASSIGNED_AREA = "__unassigned__"

@Composable
fun AreasScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val areas by vm.areas.collectAsState()
    val states by vm.states.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val unassignedCount = states.values.count { entityAreas[it.entityId] == null }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
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
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
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
                EntityControlCard(entity, vm) { vm.openPopup(it) }
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
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
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
                    EntityControlCard(entity, vm) { vm.openPopup(it) }
                    Spacer(Modifier.height(12.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

