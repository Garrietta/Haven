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
import com.garrietta.haven.ui.components.NavStyle
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.TAB_ICON_OPTIONS
import com.garrietta.haven.ui.components.moveKeyed
import com.garrietta.haven.ui.components.tabIconPairForKey
import com.garrietta.haven.ui.theme.settleSpring

private const val ROOM_KEY = "room:"
private const val TAB_KEY = "tab:"

private val AREA_TABS = listOf(
    "areas" to "Areas",
    "floors" to "Floors",
    "labels" to "Labels",
    "zones" to "Zones",
)

/**
 * Areas, labels & zones, as in Home Assistant. Each area opens to Home
 * Assistant's settings for it: its name, floor and devices.
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

    val roomIds = areas.sortedBy { it.name.lowercase() }.map { it.id }
    val listState = rememberLazyListState()

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
                "Areas are your rooms in Home Assistant, so changes here appear there too. Tap one for its settings.",
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
            val holds = areaMembers(members, entityDevices, deviceInfo.mapValues { it.value.name })
            ManagerRow(
                title = area.name,
                subtitle = deviceCountLabel(holds.size),
                icon = Icons.Filled.Home,
                expanded = open == key,
                onToggle = { open = if (open == key) null else key },
                modifier = Modifier.animateItem(),
            ) {
                RoomSettings(
                    vm = vm,
                    area = area,
                    holds = holds,
                    renameText = renameText[area.id] ?: area.name,
                    onRenameText = { renameText = renameText + (area.id to it) },
                    deleteArmed = false,
                    onDelete = { confirmDelete = area },
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
 * A room or tab as one row: grip, icon, name, what it holds and where
 * it is pinned, and a chevron. Tapping the row opens [content] under it.
 */
@Composable
private fun ManagerRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, settleSpring(), label = "chevron")
    // The row in the hand is a step brighter than the rows it passes.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
    adding: Boolean,
    onAdding: () -> Unit,
    addQuery: String,
    onAddQuery: (String) -> Unit,
    candidatesFor: (String) -> Pair<List<com.garrietta.haven.data.PickerGroup>, Int>,
    onAdd: (com.garrietta.haven.data.HaEntity) -> Unit,
    onAddDevice: (deviceId: String, name: String) -> Unit,
    onRemove: (AreaMember) -> Unit,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
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
}
