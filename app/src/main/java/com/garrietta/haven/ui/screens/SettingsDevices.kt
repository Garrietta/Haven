package com.garrietta.haven.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ConfigEntry
import com.garrietta.haven.data.FieldKind
import com.garrietta.haven.data.FlowStep
import com.garrietta.haven.data.HaHelper
import com.garrietta.haven.data.RegistryDevice
import com.garrietta.haven.data.RegistryEntity
import com.garrietta.haven.data.configEntryNeedsAttention
import com.garrietta.haven.data.configEntryStatus
import com.garrietta.haven.data.EntityGroup
import com.garrietta.haven.data.entityGroup
import com.garrietta.haven.data.isOnOffDomain
import com.garrietta.haven.data.mainControl
import com.garrietta.haven.data.nameUnderDevice
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.data.humanizeOptionLabel
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.MenuSection
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.theme.ExpressiveKind
import kotlinx.coroutines.launch
import org.json.JSONObject

/*
 * Devices & services, as in Home Assistant: integrations, the devices
 * they bring, every entity, and the helpers made in the UI. A device
 * and an entity each have a page with Home Assistant's settings for it
 * and, for an entity, how its card looks in Haven.
 */

private val DEVICE_TABS = listOf(
    "integrations" to "Integrations",
    "devices" to "Devices",
    "entities" to "Entities",
    "helpers" to "Helpers",
)

@Composable
fun DevicesServicesPage(vm: HavenViewModel, tab: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    var selected by remember { mutableStateOf(tab) }
    var query by remember { mutableStateOf("") }
    val status = rememberSettingsStatus()
    val scope = rememberCoroutineScope()
    val entries = rememberLoad("entries") { vm.admin.configEntries() }
    val devices = rememberLoad("devices") { vm.admin.devices() }
    val registry = rememberLoad("entities") { vm.admin.entities() }
    val helpers = rememberLoad("helpers") { vm.admin.helpers() }
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    var entryActions by rememberDialog<ConfigEntry>()
    var confirmDeleteEntry by rememberDialog<ConfigEntry>()
    var confirmDeleteHelper by rememberDialog<HaHelper>()
    val q = query.trim().lowercase()

    SettingsPage("Devices & services", onBack) {
        item(key = "tabs") {
            SettingsTabs(DEVICE_TABS, selected) { selected = it; query = "" }
        }
        statusItem(status)
        when (selected) {
            "integrations" -> {
                item(key = "add") {
                    Button(onClick = { navigate(Screen.SettingsPage(SettingsRoute.ADD_INTEGRATION)) }, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add integration")
                    }
                }
                loadedItems(entries, "integrations") { list ->
                    val shown = list.filter { q.isEmpty() || it.title.lowercase().contains(q) || it.domain.contains(q) }
                        .sortedByDescending { configEntryNeedsAttention(it) }
                    item(key = "search") { SettingsSearch(query, { query = it }, "Search integrations") }
                    groupedRows(shown, key = { "entry-" + it.entryId }) { entry ->
                        MenuRow(
                            badgeKey = "entry-" + entry.domain,
                            badgeKind = ExpressiveKind.FLOWER,
                            icon = if (configEntryNeedsAttention(entry)) Icons.Filled.Warning else Icons.Filled.Extension,
                            title = entry.title,
                            subtitle = "${humanizeOptionLabel(entry.domain)} · ${configEntryStatus(entry)}",
                            onClick = { entryActions = entry },
                            flat = true,
                        )
                    }
                }
            }
            "devices" -> loadedItems(devices, "devices") { list ->
                val areaName = areas.associate { it.id to it.name }
                val entityCount = (registry.state as? Loaded.Ready)?.value?.groupingBy { it.deviceId }?.eachCount() ?: emptyMap()
                val shown = list.filter { it.entryType != "service" }
                    .filter { q.isEmpty() || it.displayName.lowercase().contains(q) || (it.manufacturer ?: "").lowercase().contains(q) }
                item(key = "search") { SettingsSearch(query, { query = it }, "Search ${list.size} devices") }
                groupedRows(shown, key = { "device-" + it.id }) { device ->
                    MenuRow(
                        badgeKey = "device-" + device.id,
                        badgeKind = ExpressiveKind.BUN,
                        icon = Icons.Filled.Devices,
                        title = device.displayName,
                        subtitle = listOfNotNull(
                            device.areaId?.let { areaName[it] },
                            listOfNotNull(device.manufacturer, device.model).joinToString(" ").ifEmpty { null },
                            entityCount[device.id]?.let { "$it entit${if (it == 1) "y" else "ies"}" },
                            if (device.disabledBy != null) "Disabled" else null,
                        ).joinToString(" · "),
                        onClick = { navigate(Screen.SettingsPage(SettingsRoute.device(device.id))) },
                        flat = true,
                    )
                }
            }
            "entities" -> {
                // The registry when it loads; the live states otherwise, so
                // the list is never empty just because the registry is slow.
                val known = (registry.state as? Loaded.Ready)?.value
                val ids = (known?.map { it.entityId } ?: emptyList()).union(states.keys).sorted()
                val byId = known?.associateBy { it.entityId } ?: emptyMap()
                val shown = ids.filter { id ->
                    q.isEmpty() || id.contains(q) || (states[id]?.friendlyName ?: byId[id]?.name ?: "").lowercase().contains(q)
                }
                item(key = "search") { SettingsSearch(query, { query = it }, "Search ${ids.size} entities") }
                groupedRows(shown.take(400), key = { "entity-$it" }) { id ->
                    val entity = states[id]
                    val reg = byId[id]
                    MenuRow(
                        badgeKey = id,
                        badgeKind = shapeForDomain(id.substringBefore('.')),
                        icon = entity?.let { domainIcon(it) } ?: Icons.Filled.Tune,
                        title = entity?.friendlyName ?: reg?.name ?: reg?.originalName ?: id,
                        subtitle = listOfNotNull(
                            id,
                            if (reg?.disabledBy != null) "Disabled" else null,
                            if (reg?.hiddenBy != null) "Hidden" else null,
                        ).joinToString(" · "),
                        onClick = { navigate(Screen.SettingsPage(SettingsRoute.entity(id))) },
                        flat = true,
                    )
                }
                if (shown.size > 400) item(key = "more") { SettingsNote("Showing 400 of ${shown.size}. Search to narrow the list.") }
            }
            "helpers" -> {
                item(key = "new") {
                    Button(onClick = { navigate(Screen.HelperBuilder) }, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create helper")
                    }
                }
                loadedItems(helpers, "helpers") { list ->
                    if (list.isEmpty()) item(key = "none") { SettingsNote("No helpers made in Home Assistant's UI yet.") }
                    groupedRows(list, key = { "helper-${it.domain}-${it.id}" }) { helper ->
                        val entityId = states.keys.firstOrNull { it == helper.entityIdGuess }
                        MenuRow(
                            badgeKey = "helper-" + helper.id,
                            badgeKind = shapeForDomain(helper.domain),
                            icon = Icons.Filled.Tune,
                            title = helper.name,
                            subtitle = humanizeOptionLabel(helper.domain) + (entityId?.let { " · ${states[it]?.state}" } ?: ""),
                            onClick = entityId?.let { id -> { navigate(Screen.SettingsPage(SettingsRoute.entity(id))) } },
                            trailing = {
                                IconButton(onClick = { confirmDeleteHelper = helper }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${helper.name}")
                                }
                            },
                            flat = true,
                        )
                    }
                }
            }
        }
    }

    entryActions?.let { entry ->
        IntegrationActionsDialog(
            entry = entry,
            onDismiss = { entryActions = null },
            onReload = { scope.launch { status.report(vm.admin.reloadEntry(entry.entryId), "${entry.title} reloaded"); entries.reload() } },
            onToggle = {
                val disable = entry.disabledBy == null
                scope.launch {
                    status.report(vm.admin.setEntryDisabled(entry.entryId, disable), "${entry.title} ${if (disable) "disabled" else "enabled"}")
                    entries.reload()
                }
            },
            onDelete = { confirmDeleteEntry = entry },
        )
    }
    confirmDeleteEntry?.let { entry ->
        ConfirmDialog(
            title = "Delete ${entry.title}?",
            text = "Home Assistant removes this integration with its devices and entities. Setting it up again starts from scratch.",
            confirmLabel = "Delete",
            onConfirm = { scope.launch { status.report(vm.admin.deleteEntry(entry.entryId), "${entry.title} deleted"); entries.reload() } },
            onDismiss = { confirmDeleteEntry = null },
        )
    }
    confirmDeleteHelper?.let { helper ->
        ConfirmDialog(
            title = "Delete ${helper.name}?",
            text = "The helper and its entity are removed from Home Assistant. Automations that use it will stop working.",
            confirmLabel = "Delete",
            onConfirm = { scope.launch { status.report(vm.admin.deleteHelper(helper), "${helper.name} deleted"); helpers.reload() } },
            onDismiss = { confirmDeleteHelper = null },
        )
    }
}

@Composable
private fun IntegrationActionsDialog(
    entry: ConfigEntry,
    onDismiss: () -> Unit,
    onReload: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.title) },
        text = {
            Column {
                Text("${humanizeOptionLabel(entry.domain)} · ${configEntryStatus(entry)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                if (entry.disabledBy == null) {
                    OutlinedButton(onClick = { onDismiss(); onReload() }, modifier = Modifier.fillMaxWidth()) { Text("Reload") }
                }
                OutlinedButton(onClick = { onDismiss(); onToggle() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (entry.disabledBy == null) "Disable" else "Enable")
                }
                OutlinedButton(onClick = { onDismiss(); onDelete() }, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** A device: its name and room in Home Assistant, whether it is enabled, and its entities. */
@Composable
fun DeviceSettingsPage(vm: HavenViewModel, deviceId: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val devices = rememberLoad(deviceId) { vm.admin.devices() }
    val registry = rememberLoad("entities-$deviceId") { vm.admin.entities() }
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    var rename by remember { mutableStateOf(false) }
    var pickArea by remember { mutableStateOf(false) }
    val device: RegistryDevice? = (devices.state as? Loaded.Ready)?.value?.firstOrNull { it.id == deviceId }

    SettingsPage(device?.displayName ?: "Device", onBack) {
        statusItem(status)
        loadedItems(devices, "the device") { _ ->
            if (device == null) {
                item { SettingsNote("This device is no longer in Home Assistant.") }
                return@loadedItems
            }
            // The device's own on/off, first and large, as its maker's app
            // shows it: a smart plug's outlet, a lamp, a fan.
            val main = mainControl((registry.state as? Loaded.Ready)?.value?.filter { it.deviceId == deviceId } ?: emptyList())
            val mainEntity = main?.let { states[it.entityId] }
            if (mainEntity != null) {
                item(key = "main-control") {
                    com.garrietta.haven.ui.controls.PowerSlab(mainEntity, vm, Modifier.padding(bottom = 8.dp))
                }
            }
            item(key = "ha") {
                MenuSection("In Home Assistant")
                MenuGroup {
                    MenuRow("dev-name", ExpressiveKind.BUN, Icons.Filled.Devices, "Name", device.displayName, onClick = { rename = true }, flat = true)
                    MenuDivider()
                    MenuRow(
                        "dev-area", ExpressiveKind.BUN, Icons.Filled.Devices, "Area",
                        device.areaId?.let { id -> areas.firstOrNull { it.id == id }?.name } ?: "No area",
                        onClick = { pickArea = true }, flat = true,
                    )
                    MenuDivider()
                    MenuRow(
                        "dev-enabled", ExpressiveKind.BUN, Icons.Filled.Devices, "Enabled",
                        if (device.disabledBy == null) "Its entities are on" else "Disabled by ${device.disabledBy}",
                        onClick = null,
                        trailing = {
                            Switch(checked = device.disabledBy == null, onCheckedChange = { on ->
                                scope.launch {
                                    status.report(
                                        vm.admin.updateDevice(deviceId) { it.put("disabled_by", if (on) JSONObject.NULL else "user") },
                                        if (on) "Device enabled" else "Device disabled",
                                    )
                                    devices.reload()
                                }
                            })
                        },
                        flat = true,
                    )
                    val made = listOfNotNull(device.manufacturer, device.model).joinToString(" ")
                    if (made.isNotEmpty()) {
                        MenuDivider()
                        MenuRow("dev-made", ExpressiveKind.BUN, Icons.Filled.Devices, "Made by", made, onClick = null, flat = true)
                    }
                }
            }
            val own = (registry.state as? Loaded.Ready)?.value?.filter { it.deviceId == deviceId } ?: emptyList()
            if (own.isEmpty()) item(key = "no-entities") { SettingsNote("No entities on this device.") }
            // Grouped the way Home Assistant groups them, controls first. A
            // tap opens the entity's controls; the gear opens its settings.
            for (group in EntityGroup.entries) {
                val members = own.filter { entityGroup(it) == group }
                    .sortedWith(compareBy({ it.disabledBy != null }, { states[it.entityId]?.friendlyName ?: it.entityId }))
                if (members.isEmpty()) continue
                item(key = "group-${group.name}") { MenuSection(group.title) }
                groupedRows(members, key = { "dev-entity-" + it.entityId }) { reg ->
                    DeviceEntityRow(vm, reg, states[reg.entityId], device.displayName, navigate)
                }
            }
        }
    }
    if (rename && device != null) {
        TextPromptDialog(
            title = "Rename device",
            label = "Name",
            initial = device.displayName,
            supporting = "Leave empty to use the name the integration gave it.",
            onDismiss = { rename = false },
        ) { name ->
            scope.launch {
                status.report(
                    vm.admin.updateDevice(deviceId) { it.put("name_by_user", name.ifEmpty { null } ?: JSONObject.NULL) },
                    "Renamed",
                )
                devices.reload()
            }
        }
    }
    if (pickArea && device != null) {
        ChoiceDialog(
            title = "Area",
            options = listOf<Pair<String?, String>>(null to "No area") + areas.map { it.id to it.name },
            selected = device.areaId,
            onDismiss = { pickArea = false },
        ) { areaId ->
            scope.launch {
                status.report(vm.admin.updateDevice(deviceId) { it.put("area_id", areaId ?: JSONObject.NULL) }, "Area changed")
                devices.reload()
            }
        }
    }
}

/**
 * One of a device's entities: named under the device, with what it reads
 * now. A switch flips in place and a button presses in place; a tap opens
 * the entity's controls, and the gear its settings.
 */
@Composable
private fun DeviceEntityRow(
    vm: HavenViewModel,
    reg: RegistryEntity,
    entity: com.garrietta.haven.data.HaEntity?,
    deviceName: String,
    navigate: (Screen) -> Unit,
) {
    val name = entity?.friendlyName ?: reg.name ?: reg.originalName ?: reg.entityId
    MenuRow(
        badgeKey = reg.entityId,
        badgeKind = shapeForDomain(reg.domain),
        icon = entity?.let { domainIcon(it) } ?: Icons.Filled.Tune,
        title = nameUnderDevice(name, deviceName),
        subtitle = listOfNotNull(
            if (reg.disabledBy != null) "Disabled" else entity?.stateText(),
            reg.entityId,
        ).joinToString(" · "),
        onClick = if (entity != null) {
            { navigate(Screen.EntityDetail(reg.entityId)) }
        } else {
            { navigate(Screen.SettingsPage(SettingsRoute.entity(reg.entityId))) }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entity != null && isOnOffDomain(reg.domain) && (entity.state == "on" || entity.state == "off")) {
                    Switch(checked = entity.state == "on", onCheckedChange = { vm.toggle(reg.entityId) })
                } else if (entity != null && reg.domain in setOf("button", "input_button")) {
                    androidx.compose.material3.TextButton(onClick = { vm.callService(reg.domain, "press", reg.entityId) }) { Text("Press") }
                }
                IconButton(onClick = { navigate(Screen.SettingsPage(SettingsRoute.entity(reg.entityId))) }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Settings for $name")
                }
            }
        },
        flat = true,
    )
}

/**
 * An entity: Home Assistant's settings for it (name, icon, area, id,
 * visible, enabled) and how its card looks in Haven, which otherwise
 * follows its type's style.
 */
@Composable
fun EntitySettingsPage(vm: HavenViewModel, entityId: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val registry = rememberLoad("entity-$entityId") { vm.admin.entities() }
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    @Suppress("UNUSED_VARIABLE") val cardTick = vm.cardCustomVersion
    @Suppress("UNUSED_VARIABLE") val shapeTick = vm.shapePrefsVersion
    val entity = states[entityId]
    val reg: RegistryEntity? = (registry.state as? Loaded.Ready)?.value?.firstOrNull { it.entityId == entityId }
    var prompt by remember { mutableStateOf<String?>(null) }
    var pickArea by remember { mutableStateOf(false) }
    val domain = entityId.substringBefore('.')
    val store = vm.settingsStore

    suspend fun update(done: String, build: (JSONObject) -> Unit) {
        status.report(vm.admin.updateEntity(entityId, build), done)
        registry.reload()
    }

    SettingsPage(entity?.friendlyName ?: reg?.name ?: entityId, onBack, intro = entityId) {
        statusItem(status)
        if (entity != null) {
            item(key = "controls") {
                MenuGroup {
                    MenuRow(
                        entityId, shapeForDomain(domain), domainIcon(entity), "Controls", entity.state,
                        onClick = { navigate(Screen.EntityDetail(entityId)) }, flat = true,
                    )
                }
            }
        }
        loadedItems(registry, "this entity's settings") { _ ->
            item(key = "ha") {
                MenuSection("In Home Assistant")
                if (reg == null) {
                    SettingsNote("This entity has no unique ID, so Home Assistant cannot change its settings from the UI. Its configuration is in YAML.")
                    return@item
                }
                MenuGroup {
                    MenuRow("ent-name", ExpressiveKind.BUN, Icons.Filled.Tune, "Name", reg.name ?: reg.originalName ?: entity?.friendlyName ?: "Default", onClick = { prompt = "name" }, flat = true)
                    MenuDivider()
                    MenuRow("ent-icon", ExpressiveKind.BUN, Icons.Filled.Tune, "Icon", reg.icon ?: "The integration's icon", onClick = { prompt = "icon" }, flat = true)
                    MenuDivider()
                    MenuRow(
                        "ent-area", ExpressiveKind.BUN, Icons.Filled.Tune, "Area",
                        reg.areaId?.let { id -> areas.firstOrNull { it.id == id }?.name } ?: if (reg.deviceId != null) "Same as its device" else "No area",
                        onClick = { pickArea = true }, flat = true,
                    )
                    MenuDivider()
                    MenuRow("ent-id", ExpressiveKind.BUN, Icons.Filled.Tune, "Entity ID", reg.entityId, onClick = { prompt = "id" }, flat = true)
                    MenuDivider()
                    MenuRow(
                        "ent-visible", ExpressiveKind.BUN, Icons.Filled.Tune, "Visible", "Shown on Home Assistant's dashboards",
                        onClick = null,
                        trailing = {
                            Switch(checked = reg.hiddenBy == null, onCheckedChange = { on ->
                                scope.launch { update(if (on) "Shown" else "Hidden") { it.put("hidden_by", if (on) JSONObject.NULL else "user") } }
                            })
                        },
                        flat = true,
                    )
                    MenuDivider()
                    MenuRow(
                        "ent-enabled", ExpressiveKind.BUN, Icons.Filled.Tune, "Enabled", if (reg.disabledBy == null) "Home Assistant keeps it up to date" else "Disabled by ${reg.disabledBy}",
                        onClick = null,
                        trailing = {
                            Switch(checked = reg.disabledBy == null, onCheckedChange = { on ->
                                scope.launch { update(if (on) "Enabled" else "Disabled") { it.put("disabled_by", if (on) JSONObject.NULL else "user") } }
                            })
                        },
                        flat = true,
                    )
                    if (reg.deviceId != null) {
                        MenuDivider()
                        MenuRow(
                            "ent-device", ExpressiveKind.BUN, Icons.Filled.Devices, "Device", "Open its device",
                            onClick = { navigate(Screen.SettingsPage(SettingsRoute.device(reg.deviceId))) }, flat = true,
                        )
                    }
                }
            }
        }
        item(key = "haven") {
            val typeLabel = DOMAIN_DEFAULT_OPTIONS.firstOrNull { it.first == domain }?.second ?: humanizeOptionLabel(domain)
            val ownTitle = store.customTitles()[entityId]
            val ownIcon = store.customIconKeys()[entityId]
            val ownTile = store.customTilePresets()[entityId]
            val ownBadge = store.badgeShapeOverrides()[entityId]
            val ownCard = store.cardShapeOverrides()[entityId]
            val custom = listOf(ownTitle, ownIcon, ownTile, ownBadge, ownCard).any { it != null }
            MenuSection("In Haven")
            MenuGroup {
                MenuRow(
                    "ent-card", ExpressiveKind.SUNNY, Icons.Filled.Tune, "Title, icon and color",
                    listOfNotNull(ownTitle?.let { "\"$it\"" }, ownIcon?.let { "own icon" }, ownTile?.let { "own color" }).joinToString(" · ").ifEmpty { "As its type" },
                    onClick = { vm.openCardSettings(entityId) }, flat = true,
                )
                MenuDivider()
                MenuRow("ent-badge", ExpressiveKind.SUNNY, Icons.Filled.Tune, "Badge shape", shapeLabel(ownBadge) ?: "As its type", onClick = { vm.openShapePicker(entityId, false) }, flat = true)
                MenuDivider()
                MenuRow("ent-cardshape", ExpressiveKind.SUNNY, Icons.Filled.Tune, "Card shape", shapeLabel(ownCard) ?: "As its type", onClick = { vm.openShapePicker(entityId, true) }, flat = true)
                MenuDivider()
                MenuRow(
                    "ent-type", ExpressiveKind.SUNNY, Icons.Filled.Tune, "$typeLabel style",
                    if (custom) "This one differs from its type. Tap to make it match." else "This one follows the style for $typeLabel",
                    onClick = if (custom) {
                        {
                            store.setCustomTitle(entityId, null)
                            store.setCustomIconKey(entityId, null)
                            store.setCustomTilePreset(entityId, null)
                            store.setBadgeShapeOverride(entityId, null)
                            store.setCardShapeOverride(entityId, null)
                            vm.bumpCardCustom()
                            vm.bumpShapePrefs()
                            status.report(null, "Now follows the style for $typeLabel")
                        }
                    } else {
                        { navigate(Screen.SettingsPage(SettingsRoute.style(domain))) }
                    },
                    flat = true,
                )
            }
        }
    }

    when (prompt) {
        "name" -> TextPromptDialog(
            title = "Name", label = "Name", initial = reg?.name ?: "",
            supporting = "Leave empty to use the integration's name${reg?.originalName?.let { " ($it)" } ?: ""}.",
            onDismiss = { prompt = null },
        ) { name -> scope.launch { update("Renamed") { it.put("name", name.ifEmpty { null } ?: JSONObject.NULL) } } }
        "icon" -> TextPromptDialog(
            title = "Icon", label = "Material Design Icons name", initial = reg?.icon ?: "",
            supporting = "For example mdi:lamp. Leave empty for the integration's icon.",
            onDismiss = { prompt = null },
        ) { icon -> scope.launch { update("Icon changed") { it.put("icon", icon.ifEmpty { null } ?: JSONObject.NULL) } } }
        "id" -> TextPromptDialog(
            title = "Entity ID", label = "Entity ID", initial = entityId,
            supporting = "Automations and dashboards that use the old ID will need updating.",
            onDismiss = { prompt = null },
        ) { newId ->
            if (newId.isNotEmpty() && newId != entityId) {
                scope.launch {
                    val error = vm.admin.updateEntity(entityId) { it.put("new_entity_id", newId) }
                    status.report(error, "Renamed to $newId")
                    if (error == null) {
                        onBack()
                        navigate(Screen.SettingsPage(SettingsRoute.entity(newId)))
                    }
                }
            }
        }
    }
    if (pickArea && reg != null) {
        ChoiceDialog(
            title = "Area",
            options = listOf<Pair<String?, String>>(null to if (reg.deviceId != null) "Same as its device" else "No area") + areas.map { it.id to it.name },
            selected = reg.areaId,
            onDismiss = { pickArea = false },
        ) { areaId -> scope.launch { update("Area changed") { it.put("area_id", areaId ?: JSONObject.NULL) } } }
    }
}

/**
 * Sets up a new integration through Home Assistant's own config flow:
 * pick the integration, then answer its forms. A step that has to happen
 * in a browser (signing in to a cloud account) opens the browser.
 */
@Composable
fun AddIntegrationPage(vm: HavenViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val handlers = rememberLoad("handlers") { vm.admin.flowHandlers() }
    var query by remember { mutableStateOf("") }
    var step by remember { mutableStateOf<FlowStep?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    fun show(result: Pair<FlowStep?, String?>) {
        busy = false
        error = result.second
        step = result.first ?: step
        values = (result.first as? FlowStep.Form)?.fields?.associate { it.name to it.default } ?: emptyMap()
    }

    val leave: () -> Unit = {
        step?.let { current -> if (current !is FlowStep.Done && current !is FlowStep.Aborted) scope.launch { vm.admin.abortFlow(current.flowId) } }
        onBack()
    }

    SettingsPage("Add integration", leave, intro = "Home Assistant asks the questions; Haven shows them. Each answer goes straight to Home Assistant.") {
        error?.let { item(key = "error") { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp)) } }
        when (val current = step) {
            null -> loadedItems(handlers, "integrations") { list ->
                val q = query.trim().lowercase()
                item(key = "search") { SettingsSearch(query, { query = it }, "Search ${list.size} integrations") }
                groupedRows(list.filter { q.isEmpty() || it.contains(q) || humanizeOptionLabel(it).lowercase().contains(q) }.take(300), key = { "handler-$it" }) { handler ->
                    MenuRow(
                        "handler-$handler", ExpressiveKind.FLOWER, Icons.Filled.Extension, humanizeOptionLabel(handler), handler,
                        onClick = if (busy) null else {
                            {
                                busy = true
                                scope.launch { show(vm.admin.startFlow(handler)) }
                            }
                        },
                        flat = true,
                    )
                }
            }
            is FlowStep.Form -> item(key = "form-${current.stepId}") {
                Column {
                    Text(humanizeOptionLabel(current.stepId), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    for ((field, message) in current.errors) {
                        Text("${humanizeOptionLabel(field)}: ${humanizeOptionLabel(message)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    var unsupported = false
                    for (field in current.fields) {
                        val label = field.label + if (field.required) "" else " (optional)"
                        when (field.kind) {
                            FieldKind.TEXT, FieldKind.PASSWORD, FieldKind.NUMBER -> FormTextField(
                                label, values[field.name]?.toString() ?: "",
                                { values = values + (field.name to it) },
                                password = field.kind == FieldKind.PASSWORD, numeric = field.kind == FieldKind.NUMBER,
                            )
                            FieldKind.BOOLEAN -> Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.weight(1f))
                                Switch(checked = values[field.name] == true, onCheckedChange = { values = values + (field.name to it) })
                            }
                            FieldKind.SELECT -> {
                                Text(label, style = MaterialTheme.typography.labelLarge)
                                SettingsTabs(field.options, values[field.name]?.toString() ?: "") { values = values + (field.name to it) }
                            }
                            FieldKind.UNSUPPORTED -> {
                                unsupported = true
                                SettingsNote("${field.label}: Haven cannot show this kind of question yet; Home Assistant's page can.")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = !busy && !unsupported,
                        onClick = {
                            busy = true
                            val data = JSONObject()
                            for (field in current.fields) {
                                val value = values[field.name] ?: continue
                                when (field.kind) {
                                    FieldKind.NUMBER -> value.toString().toDoubleOrNull()?.let { n -> data.put(field.name, if (n % 1.0 == 0.0) n.toLong() else n) }
                                    FieldKind.BOOLEAN -> data.put(field.name, value == true)
                                    else -> if (value.toString().isNotEmpty()) data.put(field.name, value.toString())
                                }
                            }
                            scope.launch { show(vm.admin.continueFlow(current.flowId, data)) }
                        },
                    ) { Text(if (busy) "Sending…" else "Next") }
                }
            }
            is FlowStep.Menu -> item(key = "menu-${current.stepId}") {
                MenuGroup {
                    current.options.forEachIndexed { index, (value, label) ->
                        if (index > 0) MenuDivider()
                        MenuRow("menu-$value", ExpressiveKind.FLOWER, Icons.Filled.Extension, label, null, onClick = {
                            busy = true
                            scope.launch { show(vm.admin.continueFlow(current.flowId, JSONObject().put("next_step_id", value))) }
                        }, flat = true)
                    }
                }
            }
            is FlowStep.External -> item(key = "external") {
                Column {
                    SettingsNote("This step finishes in the browser. Come back here when it is done.")
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(current.url))) }) { Text("Open in browser") }
                    OutlinedButton(onClick = {
                        busy = true
                        scope.launch { show(vm.admin.continueFlow(current.flowId, JSONObject())) }
                    }) { Text("I've finished") }
                }
            }
            is FlowStep.Done -> item(key = "done") {
                Column {
                    Text("${current.title.ifEmpty { "The integration" }} is set up.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Done") }
                }
            }
            is FlowStep.Aborted -> item(key = "aborted") {
                Column {
                    Text("Home Assistant stopped: ${humanizeOptionLabel(current.reason)}.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { step = null; error = null }) { Text("Choose another") }
                }
            }
        }
    }
}
