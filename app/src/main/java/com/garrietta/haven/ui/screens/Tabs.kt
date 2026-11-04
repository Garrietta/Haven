package com.garrietta.haven.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.garrietta.haven.ui.controls.DialogActionButtons
import com.garrietta.haven.ui.components.MAX_PINNED_TABS
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.NavStyle
import com.garrietta.haven.ui.components.TAB_ICON_OPTIONS
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
            Text("This tab was deleted.", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { navigate(Screen.TabManager) }) { Text("Manage tabs") }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Explicit onSurface + single-line ellipsis: a bare Text
            // outside a Surface resolves its color to black, which
            // reads as a "clipped" title in dark theme (the T
            // vanishing into the background — the 2026-10-29
            // verdict's "hermostat" frame). Titles stay fully
            // rendered through the whole morph and at rest.
            Text(tab.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
 * One list of cards that flows across the full width on wide
 * (unfolded) layouts (Round 6: GridCells.Adaptive at 220dp minimum,
 * replacing the old fixed 2-column / 3-on-Expanded grid, which
 * capped dashboard category screens to a narrow strip and left a
 * stranded single tile on 5-card tabs). Boards and tab screens
 * share it so wide mode is consistent; the per-card renderer
 * (including broken-YAML handling) is the caller's.
 */
@Composable
fun <T> AdaptiveCardList(items: List<T>, render: @Composable (T) -> Unit) {
    if (LocalWideLayout.current) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items.size) { i -> render(items[i]) }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
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
            shape = generalCardShape(0),
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
/**
 * UI rewrite round: the custom-tabs browse list (Settings -> Browse ->
 * User Tabs). The bar shows Home, Settings, and (unless hidden)
 * Scenes/Energy, plus up to two pinned custom tabs (feedback round
 * 15, item 3); every VISIBLE tab also lives here as a row (title +
 * card count), tap opens the tab. Hidden tabs stay in Manage Tabs
 * with a Show button; the empty state points at Manage Tabs to
 * create one.
 */
fun UserTabsScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val visibleTabs = vm.userTabs.filter { !it.hidden }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Text("User tabs", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text("Your custom tabs. Hidden tabs live in Manage tabs.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
        if (visibleTabs.isEmpty()) {
            item {
                Text("No custom tabs yet.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navigate(Screen.TabManager) }) { Text("Manage tabs") }
            }
        } else {
            items(visibleTabs) { tab ->
                Row(
                    Modifier.fillMaxWidth().clickable { navigate(Screen.UserTab(tab.id)) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tab.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${tab.cards.size} card" + if (tab.cards.size == 1) "" else "s",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { navigate(Screen.TabManager) }) { Text("Manage tabs") }
        }
    }
}

@Composable
fun TabManagerScreen(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    var newTabTitle by remember { mutableStateOf("") }
    // (tabId, cardIndex, yaml) of the card currently in the YAML editor, if any.
    var editingCard by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            DetailTopBar("Manage tabs", onBack)
            Text(
                "Home is always first and Settings is always last — everything else here is yours. " +
                    "Reorder tabs to change their order in User Tabs.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            // Stage C: navigation customization lives here (not a new
            // settings section — Stage G owns the settings IA reorg).
            // Hidden tabs stay listed below with a Show button, so a
            // hidden tab can always come back; Settings can never be
            // hidden or removed, by construction in pillBarItems.
            Text("Navigation", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            // Feedback round 15, item 3: Scenes and Energy are
            // hideable from the bar/rail; Home and Settings are
            // locked and have no switch. A hidden built-in tab
            // stays reachable from Settings -> Browse.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show Scenes on bar", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(checked = vm.navShowScenes, onCheckedChange = { vm.navShowScenes = it })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show Energy on bar", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(checked = vm.navShowEnergy, onCheckedChange = { vm.navShowEnergy = it })
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Up to two custom tabs can be pinned to the bar (Pin below each tab). Pinned tabs sit " +
                    "between Energy and Settings; Home and Settings always stay on the bar.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pill style", fontSize = 14.sp, modifier = Modifier.weight(1f))
                var styleExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { styleExpanded = true }) {
                        Text(if (vm.navStyle == NavStyle.COMPACT) "Compact" else "Floating")
                    }
                    DropdownMenu(
                        expanded = styleExpanded,
                        onDismissRequest = { styleExpanded = false },
                    ) {
                        DropdownMenuItem(text = { Text("Floating") }, onClick = {
                            styleExpanded = false
                            vm.navStyle = NavStyle.FLOATING
                        })
                        DropdownMenuItem(text = { Text("Compact") }, onClick = {
                            styleExpanded = false
                            vm.navStyle = NavStyle.COMPACT
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reset navigation restores the floating pill, automatic icons, shows all tabs, " +
                        "puts Scenes and Energy back on the bar, and clears all pins. " +
                        "Your tabs are never deleted by a reset.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { vm.resetNavigation() }) { Text("Reset") }
            }
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
    // Customization round, item 3: device names for the picker.
    val pickerDevices by vm.entityDevices.collectAsState()
    val pickerDeviceInfo by vm.deviceInfo.collectAsState()
    val pickerDeviceNames = pickerDevices.mapValues { (_, deviceId) -> pickerDeviceInfo[deviceId]?.name ?: "" }
    var title by remember(tab.id, tab.title) { mutableStateOf(tab.title) }
    var newCardType by remember { mutableStateOf("tile") }
    var newCardTypeExpanded by remember { mutableStateOf(false) }
    var newCardEntity by remember { mutableStateOf("") }
    var typeMenuFor by remember { mutableStateOf<Int?>(null) }
    Card(
        shape = generalCardShape(0),
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
            // Stage C: per-tab icon choice + hide/show. The icon picker
            // writes the stored key (null = automatic); the pill icon
            // updates through the same bar model as everything else.
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                var iconExpanded by remember { mutableStateOf(false) }
                Box {
                    val currentLabel = TAB_ICON_OPTIONS.firstOrNull { it.key == tab.icon }?.label ?: "Automatic"
                    OutlinedButton(onClick = { iconExpanded = true }) { Text("Icon: $currentLabel") }
                    DropdownMenu(
                        expanded = iconExpanded,
                        onDismissRequest = { iconExpanded = false },
                    ) {
                        DropdownMenuItem(text = { Text("Automatic") }, onClick = {
                            iconExpanded = false
                            vm.setTabIcon(tab.id, null)
                        })
                        for (option in TAB_ICON_OPTIONS) {
                            DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                iconExpanded = false
                                vm.setTabIcon(tab.id, option.key)
                            })
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { vm.setTabHidden(tab.id, !tab.hidden) }) {
                    Text(if (tab.hidden) "Show" else "Hide")
                }
                // Feedback round 15, item 3: pin the tab onto the
                // bar (at most two). The button disables at the
                // cap for unpinned tabs; the ViewModel setter is
                // the backstop for the same cap.
                Spacer(Modifier.width(8.dp))
                val pinnedCount = vm.userTabs.count { it.pinned }
                OutlinedButton(
                    enabled = tab.pinned || pinnedCount < MAX_PINNED_TABS,
                    onClick = { vm.setTabPinned(tab.id, !tab.pinned) },
                ) {
                    Text(if (tab.pinned) "Unpin" else "Pin")
                }
                if (tab.pinned) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pinned to bar",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (tab.hidden) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Hidden from User Tabs",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                        deviceNames = pickerDeviceNames,
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
    // Customization round, item 3: device names also match, so a
    // device name in the picker finds its entities.
    deviceNames: Map<String, String> = emptyMap(),
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value, deviceNames) {
        if (value.isBlank()) {
            emptyList()
        } else {
            states.values.filter {
                it.entityId.contains(value, ignoreCase = true) ||
                    it.friendlyName.contains(value, ignoreCase = true) ||
                    (deviceNames[it.entityId]?.contains(value, ignoreCase = true) == true)
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
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
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
                // Expressive-menus round: large 56dp pills (Cancel
                // tonal, Save filled) — same taps, same save-first
                // validation; broken YAML still never overwrites.
                DialogActionButtons(
                    onCancel = onDismiss,
                    onConfirm = {
                        try {
                            parseCardYaml(text)
                            onSave(text)
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't read this YAML."
                        }
                    },
                    confirmLabel = "Save",
                    modifier = Modifier.fillMaxWidth(),
                )
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
        shape = generalCardShape(0),
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
