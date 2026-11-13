package com.garrietta.haven.ui.screens

import com.garrietta.haven.data.AreaMember
import com.garrietta.haven.data.nameUnderDevice
import com.garrietta.haven.data.pickerGroups
import com.garrietta.haven.data.areaMembers
import androidx.compose.runtime.rememberCoroutineScope
import com.garrietta.haven.data.HaArea
import com.garrietta.haven.data.HaFloor
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.Placement
import com.garrietta.haven.data.cleanDeviceName
import com.garrietta.haven.data.deviceCountLabel
import com.garrietta.haven.data.isHomeRoomEntity
import com.garrietta.haven.data.resolveOrder
import com.garrietta.haven.data.tabPlacementFor
import com.garrietta.haven.ui.components.LazyListReorder
import com.garrietta.haven.ui.components.NavStyle
import com.garrietta.haven.ui.components.ReorderHandle
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.TAB_ICON_OPTIONS
import com.garrietta.haven.ui.components.moveKeyed
import com.garrietta.haven.ui.components.rememberLazyListReorder
import com.garrietta.haven.ui.components.reorderRow
import com.garrietta.haven.ui.components.tabIconPairForKey
import com.garrietta.haven.ui.theme.settleSpring

/*
 * Rooms and tabs as lists of rows: a grip to drag each by, its icon,
 * its name and a line saying what is in it and where it is pinned. Tap
 * a row to open its settings under it. The order is written once, when
 * the finger lifts. Rooms are Home Assistant's areas and live under
 * Areas, labels & zones; tabs are Haven's own and live under Home &
 * navigation. A tab's own cards reorder the same way inside it.
 */

private const val ROOM_KEY = "room:"
private const val TAB_KEY = "tab:"

/** Where a room or tab is pinned, as its row says it. */
internal fun placementLabel(placement: Placement): String = when (placement) {
    Placement.ROOM_CHIP -> "Room chip"
    Placement.NAV_TAB -> "On the bar"
    Placement.HIDDEN -> "Hidden"
}

/** "3 cards", "1 card", "No cards". */
internal fun cardCountLabel(count: Int): String = when (count) {
    0 -> "No cards"
    1 -> "1 card"
    else -> "$count cards"
}

/** [ids] with [id] moved [delta] places, held inside the list. */
internal fun stepped(ids: List<String>, id: String, delta: Int): List<String> {
    val i = ids.indexOf(id)
    if (i < 0) return ids
    val j = (i + delta).coerceIn(ids.indices)
    if (i == j) return ids
    return ids.toMutableList().apply { add(j, removeAt(i)) }
}


private val AREA_TABS = listOf(
    "areas" to "Areas",
    "floors" to "Floors",
    "labels" to "Labels",
    "zones" to "Zones",
)

/**
 * Areas, labels & zones, as in Home Assistant. Areas are Haven's rooms:
 * each one opens to Home Assistant's settings for it (name, floor,
 * devices) beside Haven's (its name and icon in Haven, where it shows),
 * and the grip sets the order Home shows them in.
 */
@Composable
fun AreasLabelsZonesPage(vm: HavenViewModel, tab: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    var selected by remember { mutableStateOf(tab) }
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val states by vm.states.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    val auxiliary by vm.auxiliaryEntities.collectAsState()
    val floors = rememberLoad("floors") { vm.admin.floors() }
    val labels = rememberLoad("labels") { vm.admin.labels() }
    val zones = rememberLoad("zones") { vm.admin.zones() }
    var addQuery by remember { mutableStateOf("") }
    val status = rememberSettingsStatus()
    var newRoomName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var confirmDelete by remember { mutableStateOf<HaArea?>(null) }
    val scope = rememberCoroutineScope()
    var addFor by remember { mutableStateOf<String?>(null) }
    var assignFor by remember { mutableStateOf<String?>(null) }
    // The row whose settings are open: one at a time, so the list stays short.
    var open by remember { mutableStateOf<String?>(null) }
    fun done(err: String?, ok: String) = status.report(err, ok)

    // Display names and icons are Haven's own overrides of a room, not
    // Home Assistant's; renaming in HA is the Rename field.
    var displayNames by remember { mutableStateOf(vm.settingsStore.roomDisplayNames()) }
    var roomIcons by remember { mutableStateOf(vm.settingsStore.roomIconKeys()) }

    // The order being dragged, while a drag is in flight; the stored
    // order otherwise. New rooms join at the end (resolveOrder).
    var roomDrag by remember { mutableStateOf<List<String>?>(null) }
    val roomIds = roomDrag ?: resolveOrder(vm.settingsStore.roomOrder(), areas.map { it.id })
    fun commitRooms(order: List<String>) {
        vm.settingsStore.setRoomOrder(order)
        vm.bumpHomePrefs()
    }

    val listState = rememberLazyListState()
    val reorder = rememberLazyListReorder(
        listState,
        canSwap = { a, b -> a.toString().startsWith(ROOM_KEY) && b.toString().startsWith(ROOM_KEY) },
        onMove = { from, to ->
            roomDrag = moveKeyed(roomIds, from.toString().removePrefix(ROOM_KEY), to.toString().removePrefix(ROOM_KEY))
        },
        onDrop = {
            roomDrag?.let { commitRooms(it) }
            roomDrag = null
        },
    )

    SettingsPage("Areas, labels & zones", onBack, listState = listState) {
        item(key = "tabs") { SettingsTabs(AREA_TABS, selected) { selected = it } }
        statusItem(status)
        if (selected != "areas") {
            when (selected) {
                "floors" -> floorItems(vm, floors, status)
                "labels" -> labelItems(vm, labels, status)
                "zones" -> zoneItems(vm, zones, status)
            }
            return@SettingsPage
        }
        item(key = "intro") {
            Text(
                "Areas are your rooms. They live in Home Assistant, so changes here appear there too. " +
                    "Drag a room by its grip to change where it sits on Home; tap it for its settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newRoomName,
                    onValueChange = { newRoomName = it },
                    label = { Text("New area name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val name = newRoomName.trim()
                    if (name.isEmpty()) return@Button
                    vm.createRoom(name) { err ->
                        done(err, "Area \"$name\" created")
                        if (err == null) newRoomName = ""
                    }
                }) { Text("Add") }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(roomIds, key = { ROOM_KEY + it }) { areaId ->
            val area = areas.firstOrNull { it.id == areaId } ?: return@items
            val key = ROOM_KEY + areaId
            val members = entityAreas.filter { it.value == area.id }.keys
                .mapNotNull { states[it] }
                .sortedBy { it.friendlyName }
            val placement = vm.roomPlacement(area.id)
            val holds = areaMembers(members, entityDevices, deviceInfo.mapValues { it.value.name })
            ManagerRow(
                reorder = reorder,
                key = key,
                title = displayNames[area.id]?.takeIf { it.isNotBlank() } ?: area.name,
                subtitle = "${deviceCountLabel(holds.size)} · ${placementLabel(placement)}",
                icon = roomIcons[area.id]?.let { tabIconPairForKey(it)?.first } ?: Icons.Filled.Home,
                expanded = open == key && reorder.draggingKey == null,
                onToggle = { open = if (open == key) null else key },
                onStep = { delta -> commitRooms(stepped(roomIds, areaId, delta)) },
                modifier = Modifier
                    .then(if (reorder.draggingKey == key) Modifier else Modifier.animateItem())
                    .reorderRow(reorder, key),
            ) {
                RoomSettings(
                    vm = vm,
                    area = area,
                    holds = holds,
                    renameText = renameText[area.id] ?: area.name,
                    onRenameText = { renameText = renameText + (area.id to it) },
                    deleteArmed = false,
                    onDelete = { confirmDelete = area },
                    displayName = displayNames[area.id] ?: "",
                    onDisplayName = { value ->
                        vm.settingsStore.setRoomDisplayName(area.id, value.ifBlank { null })
                        displayNames = vm.settingsStore.roomDisplayNames()
                        vm.bumpHomePrefs()
                    },
                    iconKey = roomIcons[area.id],
                    onIconKey = { key ->
                        vm.settingsStore.setRoomIconKey(area.id, key)
                        roomIcons = vm.settingsStore.roomIconKeys()
                        vm.bumpHomePrefs()
                    },
                    adding = addFor == area.id,
                    onAdding = { if (addFor == area.id) addFor = null else { addFor = area.id; addQuery = "" } },
                    addQuery = addQuery,
                    onAddQuery = { addQuery = it },
                    candidatesFor = { q ->
                        // What can go in a room: not a device's configuration and
                        // diagnostic entities, which stay with the device.
                        val all = states.values
                            .filter { entityAreas[it.entityId] != area.id && it.entityId !in auxiliary }
                            .sortedBy { it.friendlyName }
                        val matches = if (q.isEmpty()) all.take(25) else all.filter { candidate ->
                            candidate.friendlyName.lowercase().contains(q) ||
                                candidate.entityId.lowercase().contains(q) ||
                                (deviceInfo[entityDevices[candidate.entityId]]?.name?.lowercase()?.contains(q) == true)
                        }
                        pickerGroups(
                            matches,
                            entityDevices,
                            deviceInfo.mapValues { (_, info) -> cleanDeviceName(info.name) ?: info.name },
                        ) to all.size
                    },
                    // What was picked moves, and only that: one outlet of a dual
                    // plug can be the greenhouse fan while the other is elsewhere.
                    onAdd = { candidate ->
                        vm.assignEntityToRoom(candidate.entityId, area.id) { err ->
                            done(err, "${candidate.friendlyName} moved to ${area.name}")
                        }
                    },
                    onAddDevice = { deviceId, name ->
                        scope.launch {
                            done(vm.admin.updateDevice(deviceId) { it.put("area_id", area.id) }, "$name moved to ${area.name}")
                        }
                    },
                    onRemove = { member ->
                        when (member) {
                            // The device leaves, and so does each of its entities
                            // that was put here on its own.
                            is AreaMember.Device -> scope.launch {
                                var err = vm.admin.updateDevice(member.deviceId) { it.put("area_id", org.json.JSONObject.NULL) }
                                for (entity in member.entities) {
                                    if (err != null) break
                                    err = vm.admin.updateEntity(entity.entityId) { it.put("area_id", org.json.JSONObject.NULL) }
                                }
                                done(err, "${member.name} moved to No area")
                            }
                            is AreaMember.Lone -> vm.assignEntityToRoom(member.entity.entityId, null) { err ->
                                done(err, "${member.name} moved to No area")
                            }
                        }
                    },
                    placement = placement,
                    done = ::done,
                    floors = (floors.state as? Loaded.Ready)?.value ?: emptyList(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        item(key = "no-room") {
            // No room mirrors Home: system entities (zone, sun, update)
            // are not devices a room can hold, so they never appear and
            // this count agrees with Home's. It has no area id, so no
            // placement and no place in the order: it is always last.
            val unassigned = states.values
                .filter { entityAreas[it.entityId] == null && isHomeRoomEntity(it) }
                .sortedBy { it.friendlyName }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.extraLarge)
                    .padding(16.dp),
            ) {
                Text("No area", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(deviceCountLabel(unassigned.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                unassigned.forEach { member ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(member.friendlyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { assignFor = if (assignFor == member.entityId) null else member.entityId }) {
                            Text("Assign…")
                        }
                    }
                    if (assignFor == member.entityId) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { assignFor = null },
                        ) {
                            areas.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area.name) },
                                    onClick = {
                                        assignFor = null
                                        vm.assignEntityToRoom(member.entityId, area.id) { err ->
                                            done(err, "${member.friendlyName} moved to ${area.name}")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { area ->
        ConfirmDialog(
            title = "Delete ${area.name}?",
            text = "The area is removed from Home Assistant. Its devices stay, with no area.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteRoom(area.id) { err -> done(err, "Area \"${area.name}\" deleted") } },
            onDismiss = { confirmDelete = null },
        )
    }
}

/**
 * Your own tabs, in the order the bar and User Tabs show them: drag a
 * tab by its grip to move it, tap it to edit its cards and where it is
 * pinned.
 */
@Composable
fun CustomTabsPage(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    var newTabTitle by remember { mutableStateOf("") }
    // (tabId, cardIndex, yaml) of the card currently in the YAML editor, if any.
    var editingCard by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    var open by remember { mutableStateOf<String?>(null) }
    var tabDrag by remember { mutableStateOf<List<String>?>(null) }
    val tabIds = tabDrag ?: vm.userTabs.map { it.id }
    val listState = rememberLazyListState()
    val reorder = rememberLazyListReorder(
        listState,
        canSwap = { a, b -> a.toString().startsWith(TAB_KEY) && b.toString().startsWith(TAB_KEY) },
        onMove = { from, to ->
            tabDrag = moveKeyed(tabIds, from.toString().removePrefix(TAB_KEY), to.toString().removePrefix(TAB_KEY))
        },
        onDrop = {
            tabDrag?.let { vm.setTabOrder(it) }
            tabDrag = null
        },
    )

    SettingsPage(
        "Your tabs",
        onBack,
        intro = "Tabs are Haven's own pages of cards. Pin one to the bar, show it as a chip on Home, or keep it in User Tabs.",
        listState = listState,
    ) {
        if (tabIds.isEmpty()) item(key = "none") { SettingsNote("No tabs yet. Add one below.") }
        items(tabIds, key = { TAB_KEY + it }) { tabId ->
            val tab = vm.userTabs.firstOrNull { it.id == tabId } ?: return@items
            val key = TAB_KEY + tabId
            val placement = tabPlacementFor(tab.pinned, tab.chip, tab.hidden)
            ManagerRow(
                reorder = reorder,
                key = key,
                title = tab.title,
                subtitle = "${cardCountLabel(tab.cards.size)} · ${placementLabel(placement)}",
                icon = tab.icon?.let { tabIconPairForKey(it)?.first } ?: Icons.Filled.ViewQuilt,
                expanded = open == key && reorder.draggingKey == null,
                onToggle = { open = if (open == key) null else key },
                onStep = { delta -> vm.setTabOrder(stepped(tabIds, tabId, delta)) },
                modifier = Modifier
                    .then(if (reorder.draggingKey == key) Modifier else Modifier.animateItem())
                    .reorderRow(reorder, key),
            ) {
                TabEditorCard(vm, tab, states, onEditYaml = { index, yaml ->
                    editingCard = Triple(tab.id, index, yaml)
                })
                Spacer(Modifier.height(12.dp))
                PlacementSelector(
                    selected = placement,
                    barDisabled = !vm.canPinToBar(tab.pinned),
                    onSelect = { vm.setTabPlacement(tab.id, it) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        item(key = "add-tab") {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Add a tab")
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

/**
 * A room or tab as one row: grip, icon, name, what it holds and where
 * it is pinned, and a chevron. Tapping the row opens [content] under it.
 */
@Composable
private fun ManagerRow(
    reorder: LazyListReorder,
    key: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, settleSpring(), label = "chevron")
    // The row in the hand is a step brighter than the rows it passes.
    val lifted = reorder.draggingKey == key
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (lifted) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The grip has a full touch target of its own, so a drag
                // that starts on it never reads as a tap on the row.
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    ReorderHandle(reorder, key, title, onStep)
                }
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Close settings" else "Open settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(turn),
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

/** An open room's settings: names, icon, devices, and where it is pinned. */
@Composable
private fun RoomSettings(
    vm: HavenViewModel,
    area: HaArea,
    holds: List<AreaMember>,
    renameText: String,
    onRenameText: (String) -> Unit,
    deleteArmed: Boolean,
    onDelete: () -> Unit,
    displayName: String,
    onDisplayName: (String) -> Unit,
    iconKey: String?,
    onIconKey: (String?) -> Unit,
    adding: Boolean,
    onAdding: () -> Unit,
    addQuery: String,
    onAddQuery: (String) -> Unit,
    candidatesFor: (String) -> Pair<List<com.garrietta.haven.data.PickerGroup>, Int>,
    onAdd: (com.garrietta.haven.data.HaEntity) -> Unit,
    onAddDevice: (deviceId: String, name: String) -> Unit,
    onRemove: (AreaMember) -> Unit,
    placement: Placement,
    done: (String?, String) -> Unit,
    floors: List<HaFloor> = emptyList(),
) {
    val scope = rememberCoroutineScope()
    var pickFloor by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = renameText,
            onValueChange = onRenameText,
            label = { Text("Name in Home Assistant") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            val name = renameText.trim()
            if (name.isEmpty() || name == area.name) return@Button
            vm.renameRoom(area.id, name) { err -> done(err, "Room renamed to \"$name\"") }
        }) { Text("Rename") }
    }
    Spacer(Modifier.height(8.dp))
    // A display name and icon change how Haven shows the room, never
    // what Home Assistant calls it.
    OutlinedTextField(
        value = displayName,
        onValueChange = onDisplayName,
        label = { Text("Display name in Haven") },
        placeholder = { Text(area.name) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        var iconMenu by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { iconMenu = true }) {
                val pair = iconKey?.let { tabIconPairForKey(it) }
                if (pair != null) {
                    Icon(pair.first, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(TAB_ICON_OPTIONS.firstOrNull { it.key == iconKey }?.label ?: "Automatic icon")
            }
            DropdownMenu(expanded = iconMenu, onDismissRequest = { iconMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Automatic icon") },
                    onClick = {
                        onIconKey(null)
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
                            onIconKey(option.key)
                            iconMenu = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onDelete) { Text(if (deleteArmed) "Confirm delete" else "Delete area") }
    }
    if (floors.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { pickFloor = true }) {
            Text("Floor: " + (floors.firstOrNull { it.id == area.floorId }?.name ?: "None"))
        }
        if (pickFloor) {
            ChoiceDialog(
                title = "Floor",
                options = listOf<Pair<String?, String>>(null to "No floor") + floors.map { it.id to it.name },
                selected = area.floorId,
                onDismiss = { pickFloor = false },
            ) { floorId -> scope.launch { done(vm.admin.setAreaFloor(area.id, floorId), "Floor changed") } }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Devices", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (holds.isEmpty()) SettingsNote("Nothing here yet.")
    holds.forEach { member ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (member is AreaMember.Device) {
                    Text(member.contents, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            TextButton(onClick = { onRemove(member) }) { Text("Remove") }
        }
    }
    OutlinedButton(onClick = onAdding) {
        Text(if (adding) "Close" else "Add a device…")
    }
    // The add list is searchable, and shows each candidate's device
    // name where it has one, so devices are findable as well as
    // entities. A blank query shows the first 25.
    if (adding) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = addQuery,
            onValueChange = onAddQuery,
            label = { Text("Search entities or devices") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        val q = addQuery.trim().lowercase()
        val (groups, total) = candidatesFor(q)
        if (q.isEmpty() && total > 25) {
            Text("Showing 25 of $total — type to search all.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (groups.isEmpty()) {
            Text("No entities match \"$addQuery\".", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // A device with its entities under it: Add all moves the device,
        // Add on an entity moves that entity alone. The list stays open,
        // so several can be added one after another; each leaves the list
        // as it lands in the room.
        groups.forEach { group ->
            val deviceId = group.deviceId
            if (deviceId != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(group.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (group.entities.size > 1) TextButton(onClick = { onAddDevice(deviceId, group.name) }) { Text("Add all") }
                }
            }
            group.entities.forEach { candidate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = if (deviceId != null) 12.dp else 0.dp),
                ) {
                    Text(
                        if (deviceId != null) nameUnderDevice(candidate.friendlyName, group.name) else candidate.friendlyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { onAdd(candidate) }) { Text("Add") }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    PlacementSelector(
        selected = placement,
        barDisabled = !vm.canPinToBar(placement == Placement.NAV_TAB),
        onSelect = { vm.setRoomPlacement(area.id, it) },
    )
}

/** The bar's own switches: which built-in tabs show, the pill's style, and reset. */
@Composable
internal fun NavigationSettings(vm: HavenViewModel) {
    // Hidden tabs stay listed with a selector, so a hidden tab can
    // always come back; Settings can never be hidden or removed, by
    // construction in pillBarItems. A hidden Scenes or Energy stays
    // reachable from Settings -> Browse.
    // A built-in tab can always be switched off; it can be switched on
    // only while the tabs and rooms pinned to the bar still fit beside it.
    val security = vm.navShowSecurity
    val climate = vm.navShowClimate
    val scenes = vm.navShowScenes
    val energy = vm.navShowEnergy
    BarSwitch("Show Security on bar", security, security || vm.canShowBuiltIn(true, scenes, energy, climate)) { vm.navShowSecurity = it }
    Spacer(Modifier.height(8.dp))
    BarSwitch("Show Climate on bar", climate, climate || vm.canShowBuiltIn(security, scenes, energy, true)) { vm.navShowClimate = it }
    Spacer(Modifier.height(8.dp))
    BarSwitch("Show Scenes on bar", scenes, scenes || vm.canShowBuiltIn(security, true, energy)) { vm.navShowScenes = it }
    Spacer(Modifier.height(8.dp))
    BarSwitch("Show Energy on bar", energy, energy || vm.canShowBuiltIn(security, scenes, true)) { vm.navShowEnergy = it }
    Spacer(Modifier.height(8.dp))
    Text(
        "Up to two extras can sit on the bar — pinned tabs and rooms-as-tabs share the two slots. " +
            "Pin a room under Areas, or a tab under Your tabs; Home and Settings always stay on the bar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Pill style", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                "puts Scenes and Energy back on the bar, and clears all pins and chip placements. " +
                "Your tabs are never deleted by a reset.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { vm.resetNavigation() }) { Text("Reset") }
    }
}

@Composable
private fun BarSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (!enabled) {
                Text(
                    "Bar full. Unpin a tab or room to make room.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/**
 * Where a room or tab is pinned: three buttons splitting the row, the
 * chosen one filled. One control for one placement, so the bar, the
 * chips and this screen can never disagree. The bar option disables at
 * the bar's cap, with the reason under it; a group already on the bar
 * is never disabled out from under itself (canPinToBar).
 */
@Composable
private fun PlacementSelector(
    selected: Placement,
    barDisabled: Boolean,
    onSelect: (Placement) -> Unit,
) {
    Column {
        Text("Pin as", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlacementOption("Room chip", selected == Placement.ROOM_CHIP) { onSelect(Placement.ROOM_CHIP) }
            PlacementOption("Nav tab", selected == Placement.NAV_TAB, enabled = !barDisabled) { onSelect(Placement.NAV_TAB) }
            PlacementOption("Hidden", selected == Placement.HIDDEN) { onSelect(Placement.HIDDEN) }
        }
        if (barDisabled && selected != Placement.NAV_TAB) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Bar full — at most two tabs and rooms-as-tabs combined.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.PlacementOption(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(label, maxLines = 1)
        }
    }
}
