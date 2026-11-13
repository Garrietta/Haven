package com.garrietta.haven.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaFloor
import com.garrietta.haven.data.HaLabel
import com.garrietta.haven.data.HaZone
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.theme.ExpressiveKind
import kotlinx.coroutines.launch

/*
 * Floors, labels and zones: the rest of Home Assistant's Areas, labels
 * & zones page. Each is a list with a field to add one and a delete on
 * each row; a zone opens to change its name and radius.
 */

/** A one-line field and Add button for making something by name. */
@Composable
private fun AddByName(label: String, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            val trimmed = name.trim()
            if (trimmed.isNotEmpty()) {
                onAdd(trimmed)
                name = ""
            }
        }) { Text("Add") }
    }
}

/** A delete button that asks first. */
@Composable
private fun DeleteWithConfirm(what: String, detail: String, onDelete: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    IconButton(onClick = { asking = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete $what") }
    if (asking) {
        ConfirmDialog("Delete $what?", detail, "Delete", onConfirm = onDelete, onDismiss = { asking = false })
    }
}

fun LazyListScope.floorItems(vm: HavenViewModel, floors: LoadHandle<List<HaFloor>>, status: SettingsStatus) {
    item(key = "floor-add") {
        val scope = rememberCoroutineScope()
        SettingsNote("Floors group areas, so Home Assistant can say \"upstairs\" and mean every room on it. Put an area on a floor from its row under Areas.")
        AddByName("New floor") { name ->
            scope.launch { status.report(vm.admin.createFloor(name), "Floor \"$name\" added"); floors.reload() }
        }
    }
    loadedItems(floors, "floors") { list ->
        if (list.isEmpty()) item(key = "floor-none") { SettingsNote("No floors yet.") }
        groupedRows(list, key = { "floor-" + it.id }) { floor ->
            val scope = rememberCoroutineScope()
            MenuRow(
                badgeKey = "floor-" + floor.id,
                badgeKind = ExpressiveKind.WAVE,
                icon = Icons.Filled.Layers,
                title = floor.name,
                subtitle = floor.level?.let { "Level $it" },
                onClick = null,
                trailing = {
                    DeleteWithConfirm(floor.name, "Its areas stay, on no floor.") {
                        scope.launch { status.report(vm.admin.deleteFloor(floor.id), "Floor \"${floor.name}\" deleted"); floors.reload() }
                    }
                },
                flat = true,
            )
        }
    }
}

fun LazyListScope.labelItems(vm: HavenViewModel, labels: LoadHandle<List<HaLabel>>, status: SettingsStatus) {
    item(key = "label-add") {
        val scope = rememberCoroutineScope()
        SettingsNote("Labels tag anything, across rooms: \"Christmas lights\", \"Away mode\". Automations can act on everything with a label.")
        AddByName("New label") { name ->
            scope.launch { status.report(vm.admin.createLabel(name), "Label \"$name\" added"); labels.reload() }
        }
    }
    loadedItems(labels, "labels") { list ->
        if (list.isEmpty()) item(key = "label-none") { SettingsNote("No labels yet.") }
        groupedRows(list, key = { "label-" + it.id }) { label ->
            val scope = rememberCoroutineScope()
            MenuRow(
                badgeKey = "label-" + label.id,
                badgeKind = ExpressiveKind.FLOWER,
                icon = Icons.Filled.Label,
                title = label.name,
                subtitle = label.color,
                onClick = null,
                trailing = {
                    DeleteWithConfirm(label.name, "It comes off everything it is on.") {
                        scope.launch { status.report(vm.admin.deleteLabel(label.id), "Label \"${label.name}\" deleted"); labels.reload() }
                    }
                },
                flat = true,
            )
        }
    }
}

/** "120 m around 39.17, -86.53". */
internal fun zoneLine(zone: HaZone): String =
    "${zone.radius.toInt()} m around ${"%.4f".format(zone.latitude)}, ${"%.4f".format(zone.longitude)}" +
        if (zone.passive) " · Passive" else ""

fun LazyListScope.zoneItems(vm: HavenViewModel, zones: LoadHandle<List<HaZone>>, status: SettingsStatus) {
    item(key = "zone-add") {
        val scope = rememberCoroutineScope()
        var adding by remember { mutableStateOf(false) }
        SettingsNote("Zones are places people can be: home, work, school. Home is set under System, General.")
        Button(onClick = { adding = true }, modifier = Modifier.padding(bottom = 12.dp)) { Text("Add zone") }
        if (adding) {
            ZoneDialog(
                title = "New zone",
                initial = null,
                home = vm.homeLocationGuess(),
                onDismiss = { adding = false },
            ) { name, lat, lon, radius ->
                scope.launch { status.report(vm.admin.createZone(name, lat, lon, radius), "Zone \"$name\" added"); zones.reload() }
            }
        }
    }
    loadedItems(zones, "zones") { list ->
        if (list.isEmpty()) item(key = "zone-none") { SettingsNote("No zones made in the UI yet. Home is always a zone.") }
        groupedRows(list, key = { "zone-" + it.id }) { zone ->
            val scope = rememberCoroutineScope()
            var editing by remember { mutableStateOf(false) }
            MenuRow(
                badgeKey = "zone-" + zone.id,
                badgeKind = ExpressiveKind.CLOUD,
                icon = Icons.Filled.Place,
                title = zone.name,
                subtitle = zoneLine(zone),
                onClick = { editing = true },
                trailing = {
                    DeleteWithConfirm(zone.name, "Automations that use this zone will stop matching.") {
                        scope.launch { status.report(vm.admin.deleteZone(zone.id), "Zone \"${zone.name}\" deleted"); zones.reload() }
                    }
                },
                flat = true,
            )
            if (editing) {
                ZoneDialog(title = "Edit zone", initial = zone, home = null, onDismiss = { editing = false }) { name, lat, lon, radius ->
                    scope.launch {
                        status.report(vm.admin.updateZone(zone.copy(name = name, latitude = lat, longitude = lon, radius = radius)), "Zone saved")
                        zones.reload()
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneDialog(
    title: String,
    initial: HaZone?,
    home: Pair<Double, Double>?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var lat by remember { mutableStateOf((initial?.latitude ?: home?.first)?.toString() ?: "") }
    var lon by remember { mutableStateOf((initial?.longitude ?: home?.second)?.toString() ?: "") }
    var radius by remember { mutableStateOf((initial?.radius ?: 100.0).toInt().toString()) }
    val valid = name.isNotBlank() && lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null && radius.toDoubleOrNull() != null
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column {
                FormTextField("Name", name, { name = it })
                FormTextField("Latitude", lat, { lat = it }, numeric = true)
                FormTextField("Longitude", lon, { lon = it }, numeric = true)
                FormTextField("Radius in meters", radius, { radius = it }, numeric = true)
                if (initial == null && home != null) SettingsNote("Starts at home; change the numbers to place it elsewhere.")
            }
        },
        confirmButton = {
            com.garrietta.haven.ui.controls.DialogActionButtons(
                onCancel = onDismiss,
                onConfirm = {
                    if (valid) {
                        onDismiss()
                        onSave(name.trim(), lat.toDouble(), lon.toDouble(), radius.toDouble())
                    }
                },
                confirmLabel = "Save",
            )
        },
    )
}
