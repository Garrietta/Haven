package com.garrietta.haven.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.stateText
import org.json.JSONArray
import org.json.JSONObject

private fun cardEntity(card: JSONObject): String? = card.optString("entity").ifEmpty { null }

private fun cardEntities(card: JSONObject): List<String> {
    val arr = card.optJSONArray("entities") ?: return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        when (val item = arr.get(i)) {
            is String -> out.add(item)
            is JSONObject -> item.optString("entity").ifEmpty { null }?.let { out.add(it) }
        }
    }
    return out
}

private fun stackCards(card: JSONObject): List<JSONObject> {
    val arr = card.optJSONArray("cards") ?: return emptyList()
    val out = mutableListOf<JSONObject>()
    for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { out.add(it) }
    return out
}

@Composable
fun DashboardsScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val config by vm.lovelace.collectAsState()
    val lovelaceError by vm.lovelaceError.collectAsState()
    val states by vm.states.collectAsState()
    var selected by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Boards", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.client.refreshLovelace() }) { Text("Refresh") }
            Spacer(Modifier.width(8.dp))
            if (config != null) {
                Button(onClick = {
                    vm.startEditing(config!!)
                    navigate(Screen.DashboardEdit)
                }) { Text("Edit") }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Dashboard picker: the instance — not Haven — decides which
        // dashboards exist, so list every one it reports and let him switch.
        // The "Default" chip loads with no url_path (the YAML-default key).
        val dashboards by vm.dashboards.collectAsState()
        val selectedDashboard by vm.selectedDashboard.collectAsState()
        if (dashboards.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentPath = vm.client.lovelaceUrlPath
                FilterChip(
                    selected = currentPath == null,
                    onClick = { vm.selectDashboard(null) },
                    label = { Text("Default") },
                )
                for (d in dashboards) {
                    FilterChip(
                        selected = d.urlPath != null && d.urlPath == currentPath,
                        onClick = { vm.selectDashboard(d.urlPath) },
                        label = { Text(d.title ?: d.urlPath ?: "Dashboard") },
                    )
                }
            }
            if (selectedDashboard?.mode == "yaml") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "This dashboard is stored as a YAML file on your instance — Haven shows it, but edits must be made in the file itself.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        if (config == null) {
            if (lovelaceError != null) {
                Text(lovelaceError!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.client.refreshLovelace() }) { Text("Retry") }
            } else {
                Text("Loading your dashboards…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        val views = config!!.views
        if (views.isEmpty()) {
            Text("This instance has no Lovelace views yet. Create one in Edit mode.")
            return@Column
        }
        val safeSelected = selected.coerceIn(views.indices)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, view) in views.withIndex()) {
                FilterChip(
                    selected = i == safeSelected,
                    onClick = { selected = i },
                    label = { Text(view.title.ifEmpty { "View ${i + 1}" }) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        AdaptiveCardList(views[safeSelected].cards) { card ->
            LovelaceCardView(card, states, vm, navigate)
        }
    }
}

/** Renders one Lovelace card natively. Unknown types get an honest placeholder. */
@Composable
fun LovelaceCardView(
    card: JSONObject,
    states: Map<String, HaEntity>,
    vm: HavenViewModel,
    navigate: (Screen) -> Unit,
) {
    val type = card.optString("type")
    val entityAreas by vm.entityAreas.collectAsState()
    when (type) {
        "entities" -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (id in cardEntities(card)) {
                    val entity = states[id]
                    if (entity != null) EntityControlCard(entity, vm) { vm.openPopup(it) }
                    else MissingEntityRow(id)
                }
            }
        }
        "tile", "light", "thermostat", "sensor", "weather", "weather-forecast", "media", "media-control", "camera", "picture-entity" -> {
            val entity = cardEntity(card)?.let { states[it] }
            if (entity != null) EntityControlCard(entity, vm) { vm.openPopup(it) }
            else MissingEntityRow(cardEntity(card) ?: "?")
        }
        "gauge" -> {
            val entity = cardEntity(card)?.let { states[it] }
            if (entity != null) GaugeCard(card, entity)
            else MissingEntityRow(cardEntity(card) ?: "?")
        }
        "entity" -> {
            val entity = cardEntity(card)?.let { states[it] }
            if (entity != null) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().clickable { vm.openPopup(entity.entityId) },
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entity.friendlyName, fontWeight = FontWeight.SemiBold)
                            Text(entity.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else MissingEntityRow(cardEntity(card) ?: "?")
        }
        "button" -> {
            val entity = cardEntity(card)?.let { states[it] }
            val name = card.optString("name").ifEmpty { entity?.friendlyName ?: "Button" }
            FilledTonalButton(
                onClick = {
                    val tap = card.optJSONObject("tap_action")
                    when (tap?.optString("action")) {
                        "toggle" -> entity?.let { vm.toggle(it.entityId) }
                        "call-service" -> {
                            val service = tap.optString("service")
                            val domain = service.substringBefore(".")
                            val svc = service.substringAfter(".", "")
                            if (svc.isNotEmpty()) {
                                vm.callService(domain, svc, tap.optString("target").ifEmpty { null } ?: entity?.entityId, tap.optJSONObject("service_data"))
                            }
                        }
                        else -> entity?.let { vm.openPopup(it.entityId) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("$name" + if (entity != null) " · ${entity.stateText()}" else "")
            }
        }
        "glance" -> {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (id in cardEntities(card)) {
                        val entity = states[id]
                        if (entity != null) {
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.openPopup(id) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(entity.friendlyName, fontSize = 14.sp)
                                Text(entity.stateText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        "area" -> {
            val areaId = card.optString("area")
            val areas by vm.areas.collectAsState()
            val area = areas.firstOrNull { it.id == areaId }
            val count = states.values.count { entityAreas[it.entityId] == areaId }
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().clickable { navigate(Screen.AreaDetail(areaId)) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(area?.name ?: areaId, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("$count entities", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        "vertical-stack", "stack" -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (child in stackCards(card)) LovelaceCardView(child, states, vm, navigate)
            }
        }
        "horizontal-stack" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (child in stackCards(card)) {
                    Column(Modifier.weight(1f)) { LovelaceCardView(child, states, vm, navigate) }
                }
            }
        }
        "grid" -> {
            val children = stackCards(card)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in children.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (child in row) {
                            Column(Modifier.weight(1f)) { LovelaceCardView(child, states, vm, navigate) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        "conditional" -> {
            val conditions = card.optJSONArray("conditions")
            var ok = true
            if (conditions != null) {
                for (i in 0 until conditions.length()) {
                    val cond = conditions.optJSONObject(i) ?: continue
                    val condType = cond.optString("condition", "state")
                    if (condType == "state") {
                        val entity = states[cond.optString("entity")]
                        val want = cond.optString("state")
                        if (entity == null || entity.state != want) ok = false
                    }
                }
            }
            if (ok) {
                card.optJSONObject("card")?.let { LovelaceCardView(it, states, vm, navigate) }
            }
        }
        "markdown" -> {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(card.optString("content"), Modifier.padding(16.dp), fontSize = 15.sp)
            }
        }
        else -> {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Not rendered natively yet", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Card type '$type' doesn't have a native Haven renderer yet — it shows here as a placeholder rather than a web page.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingEntityRow(id: String) {
    Text("Entity $id is unavailable", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun DashboardEditScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val views = vm.editingViews
    var selected by remember { mutableStateOf(0) }
    var newViewTitle by remember { mutableStateOf("") }
    var newCardType by remember { mutableStateOf("entities") }
    var newCardEntity by remember { mutableStateOf("") }
    var newCardTypeExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val editSelectedDashboard by vm.selectedDashboard.collectAsState()
    val cardTypes = listOf("entities", "tile", "button", "light", "thermostat", "sensor", "weather-forecast", "media-control", "glance", "markdown")
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        DetailTopBar("Edit dashboard", onBack)
        if (views.isEmpty()) {
            Text("No dashboard loaded. Go back and open Boards first.")
            return@Column
        }
        val safeSelected = selected.coerceIn(views.indices)
        Text("Views", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, view) in views.withIndex()) {
                FilterChip(
                    selected = i == safeSelected,
                    onClick = { selected = i },
                    label = { Text(view.optString("title").ifEmpty { "View ${i + 1}" }) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newViewTitle,
                onValueChange = { newViewTitle = it },
                label = { Text("New view title") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newViewTitle.isNotBlank()) {
                    vm.addView(newViewTitle.trim())
                    newViewTitle = ""
                }
            }) { Text("Add view") }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = {
            vm.removeView(safeSelected)
            selected = 0
        }) { Text("Remove this view") }
        Spacer(Modifier.height(16.dp))
        Text("Cards in this view", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        val cards = views[safeSelected].optJSONArray("cards") ?: JSONArray()
        LazyColumn(Modifier.weight(1f)) {
            for (i in 0 until cards.length()) {
                val card = cards.optJSONObject(i) ?: continue
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(card.optString("type"), fontWeight = FontWeight.SemiBold)
                            val summary = card.optString("entity").ifEmpty { card.optString("content").take(40) }
                            if (summary.isNotEmpty()) Text(summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.moveCard(safeSelected, i, i - 1) }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                        }
                        IconButton(onClick = { vm.moveCard(safeSelected, i, i + 1) }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                        }
                        IconButton(onClick = { vm.removeCard(safeSelected, i) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text("Add a card", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        OutlinedButton(onClick = { newCardTypeExpanded = true }) { Text(newCardType) }
                        DropdownMenu(expanded = newCardTypeExpanded, onDismissRequest = { newCardTypeExpanded = false }) {
                            for (t in cardTypes) {
                                DropdownMenuItem(text = { Text(t) }, onClick = {
                                    newCardType = t
                                    newCardTypeExpanded = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newCardEntity,
                        onValueChange = { newCardEntity = it },
                        label = { Text("Entity id (optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val card = JSONObject().put("type", newCardType)
                    if (newCardEntity.isNotBlank()) {
                        if (newCardType == "entities" || newCardType == "glance") {
                            card.put("entities", JSONArray().put(newCardEntity.trim()))
                        } else {
                            card.put("entity", newCardEntity.trim())
                        }
                    }
                    vm.addCard(safeSelected, card)
                    newCardEntity = ""
                }) { Text("Add card") }
                Spacer(Modifier.height(16.dp))
                // YAML-mode dashboards have no storage save path: HA's
                // lovelace/config/save only writes storage dashboards, so
                // offering Save here would fail confusingly. Display stays
                // fully available; only the write is gated, with the reason.
                val editYamlMode = editSelectedDashboard?.mode == "yaml"
                if (editYamlMode) {
                    Text(
                        "This dashboard is stored as a YAML file on your instance, so Haven can't save edits to it — edit the YAML file directly.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        vm.saveEditing { ok ->
                            status = if (ok) "Saved to your instance." else "Save failed — check the connection and try again."
                            if (ok) vm.client.refreshLovelace()
                        }
                    },
                    enabled = !editYamlMode,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save dashboard") }
                status?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
