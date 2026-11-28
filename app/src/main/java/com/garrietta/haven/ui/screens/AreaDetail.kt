package com.garrietta.haven.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.garrietta.haven.data.groupEntitiesByDevice
import com.garrietta.haven.data.deviceMatchesQuery
import com.garrietta.haven.data.deviceDisplayName
import com.garrietta.haven.data.attachDeviceInfo
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.ScreenTitle

const val UNASSIGNED_AREA = "__unassigned__"

@Composable
fun AreaDetailScreen(vm: HavenViewModel, areaId: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val areas by vm.areas.collectAsState()
    val states by vm.states.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val name = if (areaId == UNASSIGNED_AREA) "Unassigned" else areas.firstOrNull { it.id == areaId }?.name ?: areaId
    val entities = states.values
        .filter { if (areaId == UNASSIGNED_AREA) entityAreas[it.entityId] == null else entityAreas[it.entityId] == areaId }
        .sortedWith(compareBy({ it.domain }, { it.friendlyName }))
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
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
