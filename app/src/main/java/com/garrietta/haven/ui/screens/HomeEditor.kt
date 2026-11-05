package com.garrietta.haven.ui.screens
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HOME_SECTIONS
import com.garrietta.haven.data.migrateRound3Hidden
import com.garrietta.haven.data.migrateRound3SectionOrder
import com.garrietta.haven.data.HOME_SECTION_ORDER_DEFAULT
import com.garrietta.haven.data.HomeStatusCategory
import com.garrietta.haven.data.isIndicatorLight
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.data.resolveOrder
import com.garrietta.haven.ui.components.TAB_ICON_OPTIONS
import com.garrietta.haven.ui.components.tabIconPairForKey

/**
 * Home page editor, following the Settings -> Manage tabs pattern: reached
 * from Settings -> Manage home page, saves immediately as each control
 * changes (no Save button, like the theme and motion switches), and Home
 * renders the saved selections on every load.
 *
 * - Welcome message: empty = the automatic time-based greeting; anything
 *   else shows verbatim on Home in place of the greeting.
 * - Section toggles: one per section the Today screen renders.
 * - Per-light toggles: every light-domain entity, by friendly name. A light
 *   hidden by the default indicator rule can be switched back on here, and
 *   that explicit choice is stored as an override, so a later states
 *   refresh never re-excludes it.
 */
@Composable
fun HomeEditorScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    var welcome by remember { mutableStateOf(vm.settingsStore.homeWelcomeMessage) }
    var hidden by remember { mutableStateOf(migrateRound3Hidden(vm.settingsStore.homeHiddenSections)) }
    var overrides by remember { mutableStateOf(vm.settingsStore.homeLightOverrides()) }
    // Stage G: arrangement + room presentation state. Rendered orders
    // resolve against the canonical lists at render time, so areas
    // that arrive after first composition still slot in correctly.
    var sectionOrder by remember { mutableStateOf(migrateRound3SectionOrder(vm.settingsStore.homeSectionOrder())) }
    var displayNames by remember { mutableStateOf(vm.settingsStore.roomDisplayNames()) }
    var roomIcons by remember { mutableStateOf(vm.settingsStore.roomIconKeys()) }
    var roomOrder by remember { mutableStateOf(vm.settingsStore.roomOrder()) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    val lights = states.values.filter { it.domain == "light" }.sortedBy { it.friendlyName }
    val resolvedSectionOrder = resolveOrder(sectionOrder, HOME_SECTION_ORDER_DEFAULT)
    val resolvedRoomOrder = resolveOrder(roomOrder, areas.map { it.id })
    // Swap helper: moves [key] one slot toward the start/end of the
    // stored order (rooms included as swappable neighbours even
    // though rooms have no toggle row of their own).
    fun moveSection(key: String, up: Boolean) {
        val order = resolvedSectionOrder.toMutableList()
        val i = order.indexOf(key)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j < 0 || j >= order.size) return
        order[i] = order[j].also { order[j] = order[i] }
        sectionOrder = order
        vm.settingsStore.setHomeSectionOrder(order)
        vm.bumpHomePrefs()
    }
    fun moveRoom(areaId: String, up: Boolean) {
        val order = resolvedRoomOrder.toMutableList()
        val i = order.indexOf(areaId)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j < 0 || j >= order.size) return
        order[i] = order[j].also { order[j] = order[i] }
        roomOrder = order
        vm.settingsStore.setRoomOrder(order)
        vm.bumpHomePrefs()
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)
            // Feedback round 18, item 6: opaque root so sub-screen
            // transitions never overlap transparent text.
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            DetailTopBar("Manage home page", onBack)
            Text(
                "Home shows your welcome message, then the camera hero and room sections below. Changes apply as soon as you make them.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("Welcome message", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = welcome,
                onValueChange = {
                    welcome = it
                    vm.settingsStore.homeWelcomeMessage = it
                    vm.bumpHomePrefs()
                },
                label = { Text("Welcome message") },
                placeholder = { Text("Leave empty for the automatic greeting") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Empty means Home greets you by the time of day, as it does today. Anything else shows exactly as typed.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text("Sections", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        item {
            // Grouped M3 surface container (2026-11-01 round 1):
            // the section toggle rows sit in one surfaceContainerLow
            // container (24dp outer corners, 16dp internal horizontal
            // padding, each row clipped to 4dp) instead of floating
            // uncontained on the raw background.
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column {
                    // Stage G: the greeting is the pinned header —
                    // toggleable, never orderable — so it gets its own
                    // row with no move buttons. Every other block
                    // renders in stored order with up/down buttons
                    // that swap it with its neighbour (rooms included
                    // as a swappable neighbour, so rooms can move up
                    // when the block above them moves down).
                    val toggleable = HOME_SECTIONS.toMap()
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Greeting", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        // Feedback round 16, item 3: default switch–text buffer.
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = "greeting" !in hidden,
                            onCheckedChange = { shown ->
                                hidden = if (shown) hidden - "greeting" else hidden + "greeting"
                                vm.settingsStore.homeHiddenSections = hidden
                                vm.bumpHomePrefs()
                            },
                        )
                    }
                    for (key in resolvedSectionOrder) {
                        val label = toggleable[key] ?: continue
                        if (key == "greeting") continue
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            // Feedback round 16, item 3: default switch–text buffer.
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = { moveSection(key, true) }) {
                                Icon(Icons.Filled.ArrowUpward, "Move up", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { moveSection(key, false) }) {
                                Icon(Icons.Filled.ArrowDownward, "Move down", modifier = Modifier.size(20.dp))
                            }
                            Switch(
                                checked = key !in hidden,
                                onCheckedChange = { shown ->
                                    hidden = if (shown) hidden - key else hidden + key
                                    vm.settingsStore.homeHiddenSections = hidden
                                    vm.bumpHomePrefs()
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Favorites", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Favorites live on Home in the order you arrange them. Manage them here even when the Favorites section itself is hidden above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showFavoritesSheet = true }) {
                Text("Manage favorites")
            }
            if (showFavoritesSheet) {
                FavoritesSheet(vm, states, onDismiss = { showFavoritesSheet = false })
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Home Status items", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Which flagged conditions the Home Status section shows. Hiding a category hides only that row — the section itself is toggled above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            // Same grouped container as the sections above.
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column {
                    for (category in HomeStatusCategory.entries) {
                        val key = "status:${category.key}"
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(category.label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Switch(
                                checked = key !in hidden,
                                onCheckedChange = { shown ->
                                    hidden = if (shown) hidden - key else hidden + key
                                    vm.settingsStore.homeHiddenSections = hidden
                                    vm.bumpHomePrefs()
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Rooms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Home is organized by room. Hide a room here to drop its whole section; create, rename, delete, and assign rooms in Settings → Rooms & areas (changes carry over to Home Assistant itself).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            // Same grouped container as the sections above.
            // Stage G: each room row is a presentation editor —
            // display name (empty = the HA name), icon picker, order
            // buttons, hide switch. Nothing here touches HA area IDs:
            // these are display overrides only, so renaming a room
            // here never renames it in Home Assistant.
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column {
                    for (areaId in resolvedRoomOrder) {
                        val area = areas.firstOrNull { it.id == areaId } ?: continue
                        val key = "room:${area.id}"
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).padding(vertical = 4.dp)) {
                            // Feedback round 17, item 4: the room
                            // title above the field, so the row is
                            // labeled even when the field is empty
                            // and unfocused (the 2026-11-04 runs
                            // showed bare "Display name" fields
                            // with no room identity); the label
                            // carries the room name too.
                            Text(area.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = displayNames[area.id] ?: "",
                                    onValueChange = { value ->
                                        vm.settingsStore.setRoomDisplayName(area.id, value.ifBlank { null })
                                        displayNames = vm.settingsStore.roomDisplayNames()
                                        vm.bumpHomePrefs()
                                    },
                                    label = { Text("Display name (${area.name})") },
                                    placeholder = { Text(area.name) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = key !in hidden,
                                    onCheckedChange = { shown ->
                                        hidden = if (shown) hidden - key else hidden + key
                                        vm.settingsStore.homeHiddenSections = hidden
                                        vm.bumpHomePrefs()
                                    },
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                var iconMenu by remember { mutableStateOf(false) }
                                val currentIcon = roomIcons[area.id]
                                Box {
                                    OutlinedButton(onClick = { iconMenu = true }) {
                                        val pair = currentIcon?.let { tabIconPairForKey(it) }
                                        if (pair != null) {
                                            Icon(pair.first, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(TAB_ICON_OPTIONS.firstOrNull { it.key == currentIcon }?.label ?: "Automatic icon")
                                    }
                                    DropdownMenu(expanded = iconMenu, onDismissRequest = { iconMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Automatic icon") },
                                            onClick = {
                                                vm.settingsStore.setRoomIconKey(area.id, null)
                                                roomIcons = vm.settingsStore.roomIconKeys()
                                                vm.bumpHomePrefs()
                                                iconMenu = false
                                            },
                                        )
                                        for (option in TAB_ICON_OPTIONS) {
                                            DropdownMenuItem(
                                                text = { Text(option.label) },
                                                leadingIcon = {
                                                    tabIconPairForKey(option.key)?.first?.let {
                                                        Icon(it, null, modifier = Modifier.size(18.dp))
                                                    }
                                                },
                                                onClick = {
                                                    vm.settingsStore.setRoomIconKey(area.id, option.key)
                                                    roomIcons = vm.settingsStore.roomIconKeys()
                                                    vm.bumpHomePrefs()
                                                    iconMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { moveRoom(area.id, true) }) {
                                    Icon(Icons.Filled.ArrowUpward, "Move up", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { moveRoom(area.id, false) }) {
                                    Icon(Icons.Filled.ArrowDownward, "Move down", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Lights", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Which lights count on Home. Indicator lights are hidden by default — switch one on here to add it back. \"All off\" on Home touches only the lights shown here.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (lights.isEmpty()) {
            item {
                Text(
                    "No lights found yet. They appear here once your instance reports them.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        } else {
            item {
                // Same grouped container as the sections above.
                Box(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column {
                        for (light in lights) {
                            val shown = isLightShownOnHome(light, overrides)
                            val defaultExcluded = light.entityId !in overrides && isIndicatorLight(light)
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(light.friendlyName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = shown,
                                        onCheckedChange = { on ->
                                            vm.settingsStore.setHomeLightOverride(light.entityId, on)
                                            overrides = vm.settingsStore.homeLightOverrides()
                                            vm.bumpHomePrefs()
                                        },
                                    )
                                }
                                if (defaultExcluded) {
                                    Text(
                                        "Hidden by default (indicator light)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Reset customization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Restores the default Home layout, section order, room names and icons, navigation style, and badge settings. Your favorites and tabs are never deleted by a reset.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    vm.resetCustomization()
                    hidden = vm.settingsStore.homeHiddenSections
                    sectionOrder = vm.settingsStore.homeSectionOrder()
                    displayNames = vm.settingsStore.roomDisplayNames()
                    roomIcons = vm.settingsStore.roomIconKeys()
                    roomOrder = vm.settingsStore.roomOrder()
                },
            ) {
                Text("Reset home customization")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
