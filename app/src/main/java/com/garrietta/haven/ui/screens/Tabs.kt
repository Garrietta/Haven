package com.garrietta.haven.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.UserTab
import com.garrietta.haven.data.cardType
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.pillClearanceBottom
import org.json.JSONObject

/**
 * User tabs: a title plus cards, each card stored as YAML text. Tab pages
 * render Apple Home-style tiles (see TabTileGrid); cards that can't tile
 * fall back to the same native LovelaceCardView as Boards cards, so
 * nothing a user built is ever lost by the tile layout.
 */
@Composable
fun UserTabScreen(vm: HavenViewModel, tabId: String, navigate: (Screen) -> Unit) {
    val tab = vm.userTabs.firstOrNull { it.id == tabId }
    val states by vm.states.collectAsState()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        if (tab == null) {
            Spacer(Modifier.height(16.dp))
            Text("This tab was deleted.", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { navigate(Screen.TabManager) }) { Text("Manage tabs") }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tab.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { navigate(Screen.TabManager) }) { Text("Edit") }
        }
        Spacer(Modifier.height(12.dp))
        if (tab.cards.isEmpty()) {
            Text(
                "No cards yet. Tap Edit, then add an entity as a card.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Tab pages render Apple Home-style tiles (tap = full page,
            // long-press = detail sheet); cards that can't tile
            // keep their card rendering below.
            TabTileGrid(vm, tab.cards, states, navigate) { yaml ->
                TabCardView(vm, yaml, states, navigate)
            }
        }
    }
}

/**
 * One list of cards that becomes a 2-column grid on wide (unfolded)
 * layouts. Boards and tab screens share it so wide mode is consistent;
 * the per-card renderer (including broken-YAML handling) is the caller's.
 */
@Composable
fun <T> AdaptiveCardList(items: List<T>, render: @Composable (T) -> Unit) {
    if (LocalWideLayout.current) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = pillClearanceBottom()),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items.size) { i -> render(items[i]) }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = pillClearanceBottom()),
        ) {
            for (item in items) {
                item {
                    render(item)
                    Spacer(Modifier.height(12.dp))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TabCardView(
    vm: HavenViewModel,
    yaml: String,
    states: Map<String, HaEntity>,
    navigate: (Screen) -> Unit,
) {
    val card = try {
        parseCardYaml(yaml)
    } catch (e: Exception) {
        null
    }
    if (card == null) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("This card's YAML won't parse", fontWeight = FontWeight.SemiBold)
                Text(
                    "It stays hidden until fixed — the rest of the tab still works.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { navigate(Screen.TabManager) }) { Text("Fix in Manage tabs") }
            }
        }
    } else {
        LovelaceCardView(card, states, vm, navigate)
    }
}

/** Card types offered in the tab editor (renderer-supported, plus the new gauge/camera aliases). */
private val TAB_CARD_TYPES = listOf(
    "tile", "button", "light", "thermostat", "gauge", "sensor", "weather",
    "weather-forecast", "media", "media-control", "camera", "picture-entity",
    "entities", "glance", "markdown", "area",
)

@Composable
fun TabManagerScreen(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    var newTabTitle by remember { mutableStateOf("") }
    // (tabId, cardIndex, yaml) of the card currently in the YAML editor, if any.
    var editingCard by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        item {
            DetailTopBar("Manage tabs", onBack)
            Text(
                "Home is always first and Settings is always last — everything else here is yours. " +
                    "Reorder tabs to move them along the pill bar.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
        for (tab in vm.userTabs) {
            item {
                TabEditorCard(vm, tab, states, onEditYaml = { index, yaml ->
                    editingCard = Triple(tab.id, index, yaml)
                })
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            Text("Add a tab", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTabTitle,
                    onValueChange = { newTabTitle = it },
                    label = { Text("Tab title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val id = vm.addTab(newTabTitle)
                    newTabTitle = ""
                    navigate(Screen.UserTab(id))
                }) { Text("Add") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    editingCard?.let { (tabId, index, yaml) ->
        CardYamlEditor(
            initialYaml = yaml,
            onDismiss = { editingCard = null },
            onSave = { newYaml ->
                vm.updateCardYaml(tabId, index, newYaml)
                editingCard = null
            },
        )
    }
}

@Composable
private fun TabEditorCard(
    vm: HavenViewModel,
    tab: UserTab,
    states: Map<String, HaEntity>,
    onEditYaml: (Int, String) -> Unit,
) {
    var title by remember(tab.id, tab.title) { mutableStateOf(tab.title) }
    var newCardType by remember { mutableStateOf("tile") }
    var newCardTypeExpanded by remember { mutableStateOf(false) }
    var newCardEntity by remember { mutableStateOf("") }
    var typeMenuFor by remember { mutableStateOf<Int?>(null) }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        vm.renameTab(tab.id, it)
                    },
                    label = { Text("Tab title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.moveTab(tab.id, -1) }) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move tab left")
                }
                IconButton(onClick = { vm.moveTab(tab.id, 1) }) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move tab right")
                }
                IconButton(onClick = { vm.deleteTab(tab.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete tab")
                }
            }
            Spacer(Modifier.height(12.dp))
            for ((index, yaml) in tab.cards.withIndex()) {
                val type = try {
                    cardType(parseCardYaml(yaml))
                } catch (e: Exception) {
                    "?"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        OutlinedButton(onClick = { typeMenuFor = index }) { Text(type) }
                        DropdownMenu(
                            expanded = typeMenuFor == index,
                            onDismissRequest = { typeMenuFor = null },
                        ) {
                            for (t in TAB_CARD_TYPES) {
                                DropdownMenuItem(text = { Text(t) }, onClick = {
                                    typeMenuFor = null
                                    if (!vm.changeTabCardType(tab.id, index, t)) {
                                        // Unparseable YAML can't change type cleanly — open it for repair.
                                        onEditYaml(index, yaml)
                                    }
                                })
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        yaml.lineSequence().firstOrNull { it.contains("entity:") }?.trim() ?: "card ${index + 1}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Entity ids are single tokens: they must ellipse,
                        // never wrap mid-word across lines (2026-10-29
                        // evidence video: light.lamp_socket_1 and
                        // sensor.thermostat_current_temperature broke
                        // mid-token in this row).
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onEditYaml(index, yaml) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit card YAML")
                    }
                    IconButton(onClick = { vm.moveCardInTab(tab.id, index, index - 1) }) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move card up")
                    }
                    IconButton(onClick = { vm.moveCardInTab(tab.id, index, index + 1) }) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move card down")
                    }
                    IconButton(onClick = { vm.removeCardFromTab(tab.id, index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove card")
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Add a card", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { newCardTypeExpanded = true }) { Text(newCardType) }
                    DropdownMenu(expanded = newCardTypeExpanded, onDismissRequest = { newCardTypeExpanded = false }) {
                        for (t in TAB_CARD_TYPES) {
                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                newCardType = t
                                newCardTypeExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    EntityPickerField(
                        states = states,
                        value = newCardEntity,
                        onValueChange = { newCardEntity = it },
                        onSelect = { newCardEntity = it },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val yaml = buildString {
                    append("type: ").append(newCardType).append("\n")
                    if (newCardEntity.isNotBlank()) {
                        if (newCardType == "entities" || newCardType == "glance") {
                            append("entities:\n  - ").append(newCardEntity.trim()).append("\n")
                        } else {
                            append("entity: ").append(newCardEntity.trim()).append("\n")
                        }
                    }
                }
                vm.addCardToTab(tab.id, yaml)
                newCardEntity = ""
            }) { Text("Add card") }
        }
    }
}

/**
 * Entity picker: type to filter by name or id, tap a match to fill it in.
 * The dropdown caps at 8 matches so a broad query never covers the screen.
 */
@Composable
private fun EntityPickerField(
    states: Map<String, HaEntity>,
    value: String,
    onValueChange: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value) {
        if (value.isBlank()) {
            emptyList()
        } else {
            states.values.filter {
                it.entityId.contains(value, ignoreCase = true) ||
                    it.friendlyName.contains(value, ignoreCase = true)
            }.take(8)
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
            expanded = true
        },
        label = { Text("Entity (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    DropdownMenu(expanded = expanded && matches.isNotEmpty(), onDismissRequest = { expanded = false }) {
        for (entity in matches) {
            DropdownMenuItem(
                text = { Text("${entity.friendlyName} (${entity.entityId})") },
                onClick = {
                    onSelect(entity.entityId)
                    expanded = false
                },
            )
        }
    }
}

/**
 * HAOS-style YAML editor for one card. Save parses first: broken YAML shows
 * its error inline and never overwrites the stored card.
 */
@Composable
private fun CardYamlEditor(initialYaml: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initialYaml) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp)) {
                Text("Card YAML", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        try {
                            parseCardYaml(text)
                            onSave(text)
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't read this YAML."
                        }
                    }) { Text("Save") }
                }
            }
        }
    }
}

/**
 * Gauge card (new renderer type): a 270-degree arc plus a big number. Bounds
 * come from the card (min/max) or the entity's attributes, defaulting to
 * 0–100; a non-numeric entity gets an honest note instead of a broken arc.
 */
@Composable
fun GaugeCard(card: JSONObject, entity: HaEntity) {
    val name = card.optString("name").ifEmpty { entity.friendlyName }
    val value = entity.state.toDoubleOrNull()
    val min = if (card.has("min")) card.optDouble("min") else entity.attrDouble("min") ?: 0.0
    val max = if (card.has("max")) card.optDouble("max") else entity.attrDouble("max") ?: 100.0
    val unit = card.optString("unit").ifEmpty { entity.attrString("unit_of_measurement") ?: "" }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            if (value == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gauge needs a numeric entity — this one reports \"${entity.state}\".",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val frac = if (max > min) ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat() else 0f
                val accent = accentFor(entity.entityId)
                val track = MaterialTheme.colorScheme.surfaceVariant
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.size(160.dp)) {
                    val stroke = 14.dp.toPx()
                    drawArc(
                        color = track,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = accent,
                        startAngle = 135f,
                        sweepAngle = 270f * frac,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Text(
                    "$value $unit".trim(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
