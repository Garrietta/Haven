package com.garrietta.haven.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.formatHaTimestamp
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.theme.ExpressiveKind
import kotlinx.coroutines.launch

/*
 * Automations & scenes, as in Home Assistant: every automation with its
 * on/off switch and when it last ran, every scene, every script. New
 * ones start in Haven's guided builders; an existing one can be run,
 * switched, opened or deleted.
 */

private val AUTOMATION_TABS = listOf(
    "automations" to "Automations",
    "scenes" to "Scenes",
    "scripts" to "Scripts",
)

/** The REST config kind and the id it is addressed by, for one automation, scene or script. */
internal fun configAddress(entity: HaEntity): Pair<String, String>? = when (entity.domain) {
    "automation" -> entity.attrString("id")?.let { "automation" to it }
    "scene" -> entity.attrString("id")?.let { "scene" to it }
    "script" -> "script" to entity.entityId.substringAfter('.')
    else -> null
}

/** "Ran Nov 12, 2:14 PM", or "Never ran". */
internal fun lastRanLine(entity: HaEntity): String =
    formatHaTimestamp(entity.attrString("last_triggered"))?.let { "Ran $it" } ?: "Never ran"

@Composable
fun AutomationsScenesPage(vm: HavenViewModel, tab: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    var selected by remember { mutableStateOf(tab) }
    var query by remember { mutableStateOf("") }
    val states by vm.states.collectAsState()
    val scope = rememberCoroutineScope()
    val status = rememberSettingsStatus()
    var actions by rememberDialog<HaEntity>()
    var confirmDelete by rememberDialog<HaEntity>()
    val domain = when (selected) {
        "scenes" -> "scene"
        "scripts" -> "script"
        else -> "automation"
    }
    val q = query.trim().lowercase()
    val items = states.values.filter { it.domain == domain }
        .filter { q.isEmpty() || it.friendlyName.lowercase().contains(q) || it.entityId.contains(q) }
        .sortedBy { it.friendlyName.lowercase() }

    SettingsPage("Automations & scenes", onBack) {
        item(key = "tabs") { SettingsTabs(AUTOMATION_TABS, selected) { selected = it; query = "" } }
        statusItem(status)
        item(key = "new-$domain") {
            Button(
                onClick = {
                    navigate(
                        when (domain) {
                            "scene" -> Screen.SceneBuilder
                            "script" -> Screen.ScriptBuilder
                            else -> Screen.AutomationBuilder
                        },
                    )
                },
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New ${if (domain == "automation") "automation" else domain}")
            }
            SettingsSearch(query, { query = it }, "Search ${items.size} ${AUTOMATION_TABS.first { it.first == selected }.second.lowercase()}")
        }
        if (items.isEmpty()) item(key = "none") { SettingsNote("None yet.") }
        groupedRows(items, key = { it.entityId }) { entity ->
            when (domain) {
                "automation" -> MenuRow(
                    badgeKey = entity.entityId,
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Schedule,
                    title = entity.friendlyName,
                    subtitle = lastRanLine(entity) + if (entity.state == "off") " · Off" else "",
                    onClick = { actions = entity },
                    trailing = {
                        Switch(
                            checked = entity.state == "on",
                            onCheckedChange = { on -> vm.callService("automation", if (on) "turn_on" else "turn_off", entity.entityId) },
                        )
                    },
                    flat = true,
                )
                else -> MenuRow(
                    badgeKey = entity.entityId,
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.AutoAwesome,
                    title = entity.friendlyName,
                    subtitle = if (domain == "script") lastRanLine(entity) else formatHaTimestamp(entity.state)?.let { "Last set $it" } ?: "Never set",
                    onClick = { actions = entity },
                    trailing = {
                        IconButton(onClick = {
                            vm.callService(domain, "turn_on", entity.entityId)
                            status.report(null, "${entity.friendlyName} ${if (domain == "scene") "set" else "started"}")
                        }) { Icon(Icons.Filled.PlayArrow, contentDescription = if (domain == "scene") "Set ${entity.friendlyName}" else "Run ${entity.friendlyName}") }
                    },
                    flat = true,
                )
            }
        }
    }

    actions?.let { entity ->
        val address = configAddress(entity)
        AlertDialog(
            onDismissRequest = { actions = null },
            title = { Text(entity.friendlyName) },
            text = {
                Column {
                    Text(entity.entityId, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        actions = null
                        when (entity.domain) {
                            "automation" -> vm.callService("automation", "trigger", entity.entityId)
                            else -> vm.callService(entity.domain, "turn_on", entity.entityId)
                        }
                        status.report(null, "${entity.friendlyName} ${if (entity.domain == "scene") "set" else "run"}")
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (entity.domain == "scene") "Set this scene" else "Run now") }
                    OutlinedButton(onClick = {
                        actions = null
                        navigate(Screen.SettingsPage(SettingsRoute.entity(entity.entityId)))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Entity settings") }
                    if (address != null) {
                        OutlinedButton(onClick = { actions = null; confirmDelete = entity }, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
                    } else {
                        SettingsNote("Written in YAML, so it can only be changed there.")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { actions = null }) { Text("Close") } },
        )
    }
    confirmDelete?.let { entity ->
        ConfirmDialog(
            title = "Delete ${entity.friendlyName}?",
            text = "It is removed from Home Assistant for good.",
            confirmLabel = "Delete",
            onConfirm = {
                val (kind, id) = configAddress(entity) ?: return@ConfirmDialog
                scope.launch { status.report(vm.admin.deleteConfig(kind, id), "${entity.friendlyName} deleted") }
            },
            onDismiss = { confirmDelete = null },
        )
    }
}
