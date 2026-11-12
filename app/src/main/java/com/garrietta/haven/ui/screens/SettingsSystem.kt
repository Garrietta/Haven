package com.garrietta.haven.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.CoreConfig
import com.garrietta.haven.data.HaPerson
import com.garrietta.haven.data.LogEntry
import com.garrietta.haven.data.formatBytes
import com.garrietta.haven.data.updatesSummary
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.MenuSection
import com.garrietta.haven.ui.theme.ExpressiveKind
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * People and System, as in Home Assistant. System gathers what keeps
 * the instance running: updates, repairs, logs, backups, the general
 * settings (name, location, units, time zone), checking the
 * configuration and restarting. Restart asks first.
 */

@Composable
fun PeoplePage(vm: HavenViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val people = rememberLoad("people") { vm.admin.people() }
    val users = rememberLoad("users") { vm.admin.users() }
    val states by vm.states.collectAsState()
    var editing by rememberDialog<HaPerson>()
    var adding by remember { mutableStateOf(false) }
    val trackers = states.values.filter { it.domain == "device_tracker" }.sortedBy { it.friendlyName.lowercase() }

    SettingsPage("People", onBack, intro = "People are who lives here; their phones and trackers tell Home Assistant where they are.") {
        statusItem(status)
        item(key = "add") { Button(onClick = { adding = true }, modifier = Modifier.padding(bottom = 12.dp)) { Text("Add person") } }
        loadedItems(people, "people") { list ->
            groupedRows(list, key = { "person-" + it.id }) { person ->
                val where = states["person.${person.id}"]?.state ?: states.values.firstOrNull { it.domain == "person" && it.friendlyName == person.name }?.state
                MenuRow(
                    badgeKey = "person-" + person.id,
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.Person,
                    title = person.name,
                    subtitle = listOfNotNull(
                        where?.let { if (it == "home") "Home" else if (it == "not_home") "Away" else it },
                        if (person.deviceTrackers.isEmpty()) "No trackers" else "${person.deviceTrackers.size} tracker${if (person.deviceTrackers.size == 1) "" else "s"}",
                        if (person.fromYaml) "Set in YAML" else null,
                    ).joinToString(" · "),
                    onClick = if (person.fromYaml) null else { { editing = person } },
                    flat = true,
                )
            }
        }
        item(key = "users-title") { MenuSection("Users") }
        loadedItems(users, "users") { list ->
            groupedRows(list, key = { "user-" + it.id }) { user ->
                MenuRow(
                    badgeKey = "user-" + user.id,
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Person,
                    title = user.name,
                    subtitle = listOfNotNull(
                        if (user.isOwner) "Owner" else if (user.isAdmin) "Administrator" else "User",
                        if (!user.isActive) "Inactive" else null,
                    ).joinToString(" · "),
                    onClick = null,
                    flat = true,
                )
            }
        }
    }
    if (adding) {
        TextPromptDialog("Add person", "Name", "", onDismiss = { adding = false }, confirmLabel = "Add") { name ->
            if (name.isNotEmpty()) scope.launch { status.report(vm.admin.createPerson(name), "$name added"); people.reload() }
        }
    }
    editing?.let { person ->
        PersonDialog(
            person = person,
            trackers = trackers.map { it.entityId to it.friendlyName },
            onDismiss = { editing = null },
            onSave = { name, chosen -> scope.launch { status.report(vm.admin.updatePerson(person, name, chosen), "$name saved"); people.reload() } },
            onDelete = { scope.launch { status.report(vm.admin.deletePerson(person.id), "${person.name} removed"); people.reload() } },
        )
    }
}

@Composable
private fun PersonDialog(
    person: HaPerson,
    trackers: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(person.name) }
    var chosen by remember { mutableStateOf(person.deviceTrackers.toSet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(person.name) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                FormTextField("Name", name, { name = it })
                Text("Trackers", style = MaterialTheme.typography.labelLarge)
                for ((id, label) in trackers) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = id in chosen,
                            onCheckedChange = { on -> chosen = if (on) chosen + id else chosen - id },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (trackers.isEmpty()) SettingsNote("No device trackers in Home Assistant.")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmDelete = true }) { Text("Remove person") }
            }
        },
        confirmButton = {
            com.garrietta.haven.ui.controls.DialogActionButtons(
                onCancel = onDismiss,
                onConfirm = { onDismiss(); onSave(name.trim().ifEmpty { person.name }, chosen.toList()) },
                confirmLabel = "Save",
            )
        },
    )
    if (confirmDelete) {
        ConfirmDialog("Remove ${person.name}?", "Their history stays; they stop being tracked as a person.", "Remove", onConfirm = { onDismiss(); onDelete() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
fun SystemPage(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val states by vm.states.collectAsState()
    val updates = updatesSummary(states)
    val repairs = rememberLoad("repairs") { vm.admin.repairs() }
    var confirmRestart by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    fun go(route: String) = navigate(Screen.SettingsPage(route))

    SettingsPage("System", onBack) {
        statusItem(status)
        item(key = "rows") {
            val open = (repairs.state as? Loaded.Ready)?.value?.count { !it.ignored }
            MenuGroup {
                MenuRow("sys-updates", ExpressiveKind.BUN, Icons.Filled.SystemUpdate, "Updates",
                    if (updates.available == 0) "Everything is up to date" else "${updates.available} available",
                    onClick = { navigate(Screen.UpdatesPanel) }, flat = true)
                MenuDivider()
                MenuRow("sys-repairs", ExpressiveKind.BUN, Icons.Filled.Build, "Repairs",
                    when (open) { null -> "What Home Assistant needs you to look at"; 0 -> "Nothing to repair"; else -> "$open to look at" },
                    onClick = { go(SettingsRoute.REPAIRS) }, flat = true)
                MenuDivider()
                MenuRow("sys-logs", ExpressiveKind.BUN, Icons.Filled.Description, "Logs", "Errors and warnings Home Assistant has logged", onClick = { go(SettingsRoute.LOGS) }, flat = true)
                MenuDivider()
                MenuRow("sys-backups", ExpressiveKind.BUN, Icons.Filled.Backup, "Backups", "Make and see backups", onClick = { go(SettingsRoute.BACKUPS) }, flat = true)
                MenuDivider()
                MenuRow("sys-general", ExpressiveKind.BUN, Icons.Filled.Settings, "General", "Name, location, units and time zone", onClick = { go(SettingsRoute.GENERAL) }, flat = true)
            }
            MenuSection("Configuration")
            MenuGroup {
                MenuRow("sys-check", ExpressiveKind.WAVE, Icons.Filled.CheckCircle, "Check configuration",
                    if (checking) "Checking…" else "Find mistakes in the YAML before restarting",
                    onClick = {
                        checking = true
                        scope.launch {
                            val result = vm.admin.checkConfig()
                            checking = false
                            status.report(if (result == "valid") null else result, "The configuration is valid")
                        }
                    }, flat = true)
                MenuDivider()
                MenuRow("sys-reload", ExpressiveKind.WAVE, Icons.Filled.Refresh, "Reload YAML", "Pick up YAML changes without restarting",
                    onClick = {
                        vm.callService("homeassistant", "reload_all")
                        status.report(null, "Reloading the YAML configuration")
                    }, flat = true)
                MenuDivider()
                MenuRow("sys-restart", ExpressiveKind.WAVE, Icons.Filled.PowerSettingsNew, "Restart Home Assistant", "Everything stops for a minute",
                    onClick = { confirmRestart = true }, flat = true)
            }
        }
    }
    if (confirmRestart) {
        ConfirmDialog(
            title = "Restart Home Assistant?",
            text = "Devices stop responding until it is back, usually within a minute. Haven reconnects by itself.",
            confirmLabel = "Restart",
            onConfirm = {
                vm.callService("homeassistant", "restart")
                status.report(null, "Restarting")
            },
            onDismiss = { confirmRestart = false },
        )
    }
}

@Composable
fun RepairsPage(vm: HavenViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val context = LocalContext.current
    val repairs = rememberLoad("repairs-page") { vm.admin.repairs() }
    SettingsPage("Repairs", onBack, intro = "Problems Home Assistant found. Ignoring one hides it until it changes.") {
        statusItem(status)
        loadedItems(repairs, "repairs") { list ->
            if (list.isEmpty()) item(key = "none") { SettingsNote("Nothing to repair.") }
            groupedRows(list, key = { "repair-${it.domain}-${it.issueId}" }) { issue ->
                MenuRow(
                    badgeKey = "repair-" + issue.issueId,
                    badgeKind = ExpressiveKind.BURST,
                    icon = Icons.Filled.Warning,
                    title = issue.title,
                    subtitle = listOfNotNull(
                        issue.severity.replaceFirstChar { it.uppercase() },
                        if (issue.ignored) "Ignored" else null,
                        if (issue.learnMoreUrl != null) "Tap to learn more" else null,
                    ).joinToString(" · "),
                    onClick = issue.learnMoreUrl?.let { url -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                    trailing = {
                        androidx.compose.material3.TextButton(onClick = {
                            scope.launch {
                                status.report(vm.admin.ignoreRepair(issue, !issue.ignored), if (issue.ignored) "Shown again" else "Ignored")
                                repairs.reload()
                            }
                        }) { Text(if (issue.ignored) "Unignore" else "Ignore") }
                    },
                    flat = true,
                )
            }
        }
    }
}

private val logTime = DateTimeFormatter.ofPattern("MMM d, h:mm a")

/** "Nov 12, 2:14 PM" from seconds since the epoch. */
internal fun logWhen(seconds: Double, zone: ZoneId = ZoneId.systemDefault()): String =
    if (seconds <= 0) "" else logTime.withZone(zone).format(Instant.ofEpochMilli((seconds * 1000).toLong()))

@Composable
fun LogsPage(vm: HavenViewModel, onBack: () -> Unit) {
    val status = rememberSettingsStatus()
    val logs = rememberLoad("logs") { vm.admin.logs() }
    var open by rememberDialog<LogEntry>()
    var level by remember { mutableStateOf("all") }
    SettingsPage("Logs", onBack) {
        statusItem(status)
        item(key = "actions") {
            SettingsTabs(listOf("all" to "All", "ERROR" to "Errors", "WARNING" to "Warnings"), level) { level = it }
            androidx.compose.foundation.layout.Row {
                OutlinedButton(onClick = { logs.reload() }) { Text("Refresh") }
                Spacer(Modifier.padding(4.dp))
                OutlinedButton(onClick = {
                    vm.callService("system_log", "clear")
                    status.report(null, "Logs cleared")
                    logs.reload()
                }) { Text("Clear") }
            }
            Spacer(Modifier.height(12.dp))
        }
        loadedItems(logs, "logs") { list ->
            val shown = list.filter { level == "all" || it.level == level || (level == "ERROR" && it.level == "CRITICAL") }
            if (shown.isEmpty()) item(key = "none") { SettingsNote("Nothing logged.") }
            groupedRows(shown, key = { "log-${it.name}-${it.timestamp}-${it.message.hashCode()}" }) { entry ->
                MenuRow(
                    badgeKey = "log-" + entry.level,
                    badgeKind = if (entry.level == "WARNING") ExpressiveKind.CLOUD else ExpressiveKind.BURST,
                    icon = if (entry.level == "WARNING") Icons.Filled.Info else Icons.Filled.Warning,
                    title = entry.message.lineSequence().first().take(140),
                    subtitle = listOfNotNull(
                        entry.name.substringAfterLast('.').ifEmpty { null },
                        logWhen(entry.timestamp).ifEmpty { null },
                        if (entry.count > 1) "×${entry.count}" else null,
                    ).joinToString(" · "),
                    onClick = { open = entry },
                    flat = true,
                )
            }
        }
    }
    open?.let { entry ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(entry.level.lowercase().replaceFirstChar { it.uppercase() }) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(listOfNotNull(entry.name, entry.source).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                    entry.exception?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { open = null }) { Text("Close") } },
        )
    }
}

@Composable
fun BackupsPage(vm: HavenViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val backups = rememberLoad("backups") { vm.admin.backups() }
    var confirm by remember { mutableStateOf(false) }
    SettingsPage("Backups", onBack, intro = "Backups are made by Home Assistant and kept where its backup settings say.") {
        statusItem(status)
        item(key = "create") {
            Button(onClick = { confirm = true }, modifier = Modifier.padding(bottom = 12.dp)) { Text("Back up now") }
        }
        loadedItems(backups, "backups") { list ->
            if (list.isEmpty()) item(key = "none") { SettingsNote("No backups yet.") }
            groupedRows(list, key = { "backup-" + it.id }) { backup ->
                MenuRow(
                    badgeKey = "backup-" + backup.id,
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Backup,
                    title = backup.name,
                    subtitle = listOfNotNull(
                        com.garrietta.haven.data.formatHaTimestamp(backup.date) ?: backup.date.take(10),
                        backup.sizeBytes?.let { formatBytes(it) },
                    ).joinToString(" · "),
                    onClick = null,
                    flat = true,
                )
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            "Back up now?", "Home Assistant makes a backup with its backup settings. It can take a few minutes.", "Back up",
            onConfirm = { scope.launch { status.report(vm.admin.createBackup(), "Backup started"); backups.reload() } },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
fun GeneralPage(vm: HavenViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    val core = rememberLoad("core") { vm.admin.coreConfig() }
    var prompt by remember { mutableStateOf<String?>(null) }
    var units by remember { mutableStateOf(false) }

    suspend fun save(done: String, build: (JSONObject) -> Unit) {
        status.report(vm.admin.updateCore(build), done)
        core.reload()
    }

    SettingsPage("General", onBack, intro = "Home Assistant's own settings for this home.") {
        statusItem(status)
        loadedItems(core, "the general settings") { config ->
            item(key = "general") {
                MenuGroup {
                    MenuRow("gen-name", ExpressiveKind.BUN, Icons.Filled.Settings, "Name", config.locationName, onClick = { prompt = "name" }, flat = true)
                    MenuDivider()
                    MenuRow("gen-units", ExpressiveKind.BUN, Icons.Filled.Settings, "Units", config.unitSystem ?: "—", onClick = { units = true }, flat = true)
                    MenuDivider()
                    MenuRow("gen-tz", ExpressiveKind.BUN, Icons.Filled.Settings, "Time zone", config.timeZone ?: "—", onClick = { prompt = "tz" }, flat = true)
                    MenuDivider()
                    MenuRow("gen-currency", ExpressiveKind.BUN, Icons.Filled.Settings, "Currency", config.currency ?: "—", onClick = { prompt = "currency" }, flat = true)
                    MenuDivider()
                    MenuRow("gen-elevation", ExpressiveKind.BUN, Icons.Filled.Settings, "Elevation", config.elevation?.let { "$it m" } ?: "—", onClick = { prompt = "elevation" }, flat = true)
                    MenuDivider()
                    MenuRow(
                        "gen-location", ExpressiveKind.BUN, Icons.Filled.Settings, "Location",
                        if (config.latitude != null && config.longitude != null) "%.4f, %.4f".format(config.latitude, config.longitude) else "—",
                        onClick = { prompt = "location" }, flat = true,
                    )
                    MenuDivider()
                    MenuRow("gen-region", ExpressiveKind.BUN, Icons.Filled.Settings, "Country and language", listOfNotNull(config.country, config.language).joinToString(" · ").ifEmpty { "—" }, onClick = null, flat = true)
                }
            }
            if (prompt != null || units) item(key = "dialogs") { GeneralDialogs(config, prompt, units, { prompt = null }, { units = false }) { done, build -> scope.launch { save(done, build) } } }
        }
    }
}

@Composable
private fun GeneralDialogs(
    config: CoreConfig,
    prompt: String?,
    units: Boolean,
    closePrompt: () -> Unit,
    closeUnits: () -> Unit,
    save: (String, (JSONObject) -> Unit) -> Unit,
) {
    when (prompt) {
        "name" -> TextPromptDialog("Name", "Name", config.locationName, closePrompt) { v -> if (v.isNotEmpty()) save("Renamed") { it.put("location_name", v) } }
        "tz" -> TextPromptDialog("Time zone", "Time zone", config.timeZone ?: "", closePrompt, supporting = "An IANA name, like America/Indiana/Indianapolis.") { v ->
            if (v.isNotEmpty()) save("Time zone changed") { it.put("time_zone", v) }
        }
        "currency" -> TextPromptDialog("Currency", "Currency code", config.currency ?: "", closePrompt, supporting = "Three letters, like USD.") { v ->
            if (v.length == 3) save("Currency changed") { it.put("currency", v.uppercase()) }
        }
        "elevation" -> TextPromptDialog("Elevation", "Meters above sea level", config.elevation?.toString() ?: "", closePrompt, numeric = true) { v ->
            v.toDoubleOrNull()?.let { m -> save("Elevation changed") { it.put("elevation", m.toInt()) } }
        }
        "location" -> TextPromptDialog(
            "Location", "Latitude, longitude",
            if (config.latitude != null && config.longitude != null) "${config.latitude}, ${config.longitude}" else "",
            closePrompt, supporting = "Used for sunrise, sunset and the home zone.",
        ) { v ->
            val parts = v.split(',').map { it.trim().toDoubleOrNull() }
            if (parts.size == 2 && parts.all { it != null }) save("Location changed") { it.put("latitude", parts[0]).put("longitude", parts[1]) }
        }
    }
    if (units) {
        ChoiceDialog(
            "Units", listOf("metric" to "Metric", "us_customary" to "US customary"),
            if (config.unitSystem == "Metric") "metric" else "us_customary", closeUnits,
        ) { v -> save("Units changed") { it.put("unit_system", v) } }
    }
}

@Composable
fun AboutPage(vm: HavenViewModel, onBack: () -> Unit) {
    val core = rememberLoad("about") { vm.admin.coreConfig() }
    val context = LocalContext.current
    val havenVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    SettingsPage("About", onBack) {
        loadedItems(core, "the versions") { config ->
            item(key = "about") {
                MenuGroup {
                    MenuRow("about-ha", ExpressiveKind.SUNNY, Icons.Filled.Info, "Home Assistant", config.version, onClick = null, flat = true)
                    MenuDivider()
                    MenuRow("about-home", ExpressiveKind.SUNNY, Icons.Filled.Info, "Home", config.locationName, onClick = null, flat = true)
                    MenuDivider()
                    MenuRow("about-components", ExpressiveKind.SUNNY, Icons.Filled.Info, "Loaded components", config.components.toString(), onClick = null, flat = true)
                    MenuDivider()
                    MenuRow("about-state", ExpressiveKind.SUNNY, Icons.Filled.Info, "State",
                        (config.state ?: "—").lowercase().replaceFirstChar { it.uppercase() } + if (config.safeMode) " · Recovery mode" else "",
                        onClick = null, flat = true)
                    MenuDivider()
                    MenuRow("about-haven", ExpressiveKind.SUNNY, Icons.Filled.Info, "Haven", havenVersion, onClick = null, flat = true)
                }
            }
        }
    }
}
