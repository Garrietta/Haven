package com.garrietta.haven.ui.screens

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
import com.garrietta.haven.data.HaArea
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
 * Rooms & tabs: where rooms and tabs are made, named, ordered and
 * placed. Rooms write through to Home Assistant; tabs are local.
 *
 * Each room and tab is one row: a grip to drag it by, its icon, its
 * name and a line saying what is in it and where it is pinned. Tap the
 * row to open its settings under it. Drag the grip to reorder: the order
 * of rooms is the order Home shows them in, and the order of tabs is the
 * order of User Tabs. The new order is written once, when the finger
 * lifts. A tab's own cards reorder the same way inside the open tab.
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

@Composable
fun RoomsTabsScreen(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val states by vm.states.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    var addQuery by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var deleteArmed by remember { mutableStateOf<String?>(null) }
    var addFor by remember { mutableStateOf<String?>(null) }
    var assignFor by remember { mutableStateOf<String?>(null) }
    var newTabTitle by remember { mutableStateOf("") }
    // (tabId, cardIndex, yaml) of the card currently in the YAML editor, if any.
    var editingCard by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    // The row whose settings are open: one at a time, so the list stays short.
    var open by remember { mutableStateOf<String?>(null) }
    fun done(err: String?, ok: String) {
        if (err != null) { message = err; messageIsError = true }
        else { message = ok; messageIsError = false }
    }

    // Display names and icons are Haven's own overrides of a room, not
    // Home Assistant's; renaming in HA is the Rename field.
    var displayNames by remember { mutableStateOf(vm.settingsStore.roomDisplayNames()) }
    var roomIcons by remember { mutableStateOf(vm.settingsStore.roomIconKeys()) }

    // The order being dragged, while a drag is in flight; the stored
    // order otherwise. New rooms join at the end (resolveOrder).
    var roomDrag by remember { mutableStateOf<List<String>?>(null) }
    var tabDrag by remember { mutableStateOf<List<String>?>(null) }
    val roomIds = roomDrag ?: resolveOrder(vm.settingsStore.roomOrder(), areas.map { it.id })
    val tabIds = tabDrag ?: vm.userTabs.map { it.id }
    fun commitRooms(order: List<String>) {
        vm.settingsStore.setRoomOrder(order)
        vm.bumpHomePrefs()
    }

    val listState = rememberLazyListState()
    val reorder = rememberLazyListReorder(
        listState,
        canSwap = { a, b ->
            val x = a.toString()
            val y = b.toString()
            (x.startsWith(ROOM_KEY) && y.startsWith(ROOM_KEY)) || (x.startsWith(TAB_KEY) && y.startsWith(TAB_KEY))
        },
        onMove = { from, to ->
            val a = from.toString()
            val b = to.toString()
            if (a.startsWith(ROOM_KEY)) {
                roomDrag = moveKeyed(roomIds, a.removePrefix(ROOM_KEY), b.removePrefix(ROOM_KEY))
            } else {
                tabDrag = moveKeyed(tabIds, a.removePrefix(TAB_KEY), b.removePrefix(TAB_KEY))
            }
        },
        onDrop = {
            roomDrag?.let { commitRooms(it) }
            tabDrag?.let { vm.setTabOrder(it) }
            roomDrag = null
            tabDrag = null
        },
    )

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Rooms & tabs", onBack)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(key = "intro") {
                Text(
                    "Rooms live in Home Assistant — changes here appear in HA itself. Tabs are local to Haven. " +
                        "Each room or tab pins to exactly one surface: the Home chip row, the navigation bar, or neither.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    vm.refreshAreas()
                    message = "Refreshing rooms from Home Assistant…"
                    messageIsError = false
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh from Home Assistant")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("New room name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val name = newRoomName.trim()
                        if (name.isEmpty()) return@Button
                        vm.createRoom(name) { err ->
                            done(err, "Room \"$name\" created")
                            if (err == null) newRoomName = ""
                        }
                    }) { Text("Add room") }
                }
                Spacer(Modifier.height(12.dp))
                if (message != null) {
                    Text(
                        message!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                SectionTitle("Rooms")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Drag a room by its grip to change where it sits on Home. Tap it for its settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
            items(roomIds, key = { ROOM_KEY + it }) { areaId ->
                val area = areas.firstOrNull { it.id == areaId } ?: return@items
                val key = ROOM_KEY + areaId
                val members = entityAreas.filter { it.value == area.id }.keys
                    .mapNotNull { states[it] }
                    .sortedBy { it.friendlyName }
                val placement = vm.roomPlacement(area.id)
                ManagerRow(
                    reorder = reorder,
                    key = key,
                    title = displayNames[area.id]?.takeIf { it.isNotBlank() } ?: area.name,
                    subtitle = "${deviceCountLabel(members.size)} · ${placementLabel(placement)}",
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
                        members = members,
                        renameText = renameText[area.id] ?: area.name,
                        onRenameText = { renameText = renameText + (area.id to it) },
                        deleteArmed = deleteArmed == area.id,
                        onDelete = {
                            if (deleteArmed == area.id) {
                                deleteArmed = null
                                vm.deleteRoom(area.id) { err -> done(err, "Room \"${area.name}\" deleted") }
                            } else {
                                deleteArmed = area.id
                                message = "Tap Delete again to confirm"
                                messageIsError = false
                            }
                        },
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
                            val all = states.values
                                .filter { entityAreas[it.entityId] != area.id }
                                .sortedBy { it.friendlyName }
                            val matches = if (q.isEmpty()) all.take(25) else all.filter { candidate ->
                                candidate.friendlyName.lowercase().contains(q) ||
                                    candidate.entityId.lowercase().contains(q) ||
                                    (deviceInfo[entityDevices[candidate.entityId]]?.name?.lowercase()?.contains(q) == true)
                            }
                            Triple(matches, all.size, { id: String -> cleanDeviceName(deviceInfo[entityDevices[id]]?.name) })
                        },
                        onAdd = { candidate ->
                            addFor = null
                            vm.assignEntityToRoom(candidate.entityId, area.id) { err ->
                                done(err, "${candidate.friendlyName} moved to ${area.name}")
                            }
                        },
                        onRemove = { member ->
                            vm.assignEntityToRoom(member.entityId, null) { err ->
                                done(err, "${member.friendlyName} moved to No room")
                            }
                        },
                        placement = placement,
                        done = ::done,
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
                    Text("No room", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
            item(key = "tabs-head") {
                SectionTitle("Tabs")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Home is always first and Settings is always last — everything else here is yours. " +
                        "Drag a tab by its grip to change its order in User Tabs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                NavigationSettings(vm)
                Spacer(Modifier.height(16.dp))
            }
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
                Spacer(Modifier.height(24.dp))
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
    members: List<com.garrietta.haven.data.HaEntity>,
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
    candidatesFor: (String) -> Triple<List<com.garrietta.haven.data.HaEntity>, Int, (String) -> String?>,
    onAdd: (com.garrietta.haven.data.HaEntity) -> Unit,
    onRemove: (com.garrietta.haven.data.HaEntity) -> Unit,
    placement: Placement,
    done: (String?, String) -> Unit,
) {
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
        OutlinedButton(onClick = onDelete) { Text(if (deleteArmed) "Confirm delete" else "Delete room") }
    }
    Spacer(Modifier.height(12.dp))
    Text("Devices", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    members.forEach { member ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(member.friendlyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        val (candidates, total, deviceNameOf) = candidatesFor(q)
        if (q.isEmpty() && total > 25) {
            Text("Showing 25 of $total — type to search all.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (candidates.isEmpty()) {
            Text("No entities match \"$addQuery\".", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        candidates.forEach { candidate ->
            val deviceName = deviceNameOf(candidate.entityId)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(candidate.friendlyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (deviceName != null) {
                        Text(deviceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(onClick = { onAdd(candidate) }) { Text("Add") }
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
private fun NavigationSettings(vm: HavenViewModel) {
    // Hidden tabs stay listed with a selector, so a hidden tab can
    // always come back; Settings can never be hidden or removed, by
    // construction in pillBarItems. A hidden Scenes or Energy stays
    // reachable from Settings -> Browse.
    SectionTitle("Navigation")
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Show Scenes on bar", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = vm.navShowScenes, onCheckedChange = { vm.navShowScenes = it })
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Show Energy on bar", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = vm.navShowEnergy, onCheckedChange = { vm.navShowEnergy = it })
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Up to two extras can sit on the bar — pinned tabs and rooms-as-tabs share the two slots. " +
            "Use the placement selector in each room or tab; Home and Settings always stay on the bar.",
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
