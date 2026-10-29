package com.garrietta.haven.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HOME_SECTIONS
import com.garrietta.haven.data.isIndicatorLight
import com.garrietta.haven.data.isLightShownOnHome

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
    var welcome by remember { mutableStateOf(vm.settingsStore.homeWelcomeMessage) }
    var hidden by remember { mutableStateOf(vm.settingsStore.homeHiddenSections) }
    var overrides by remember { mutableStateOf(vm.settingsStore.homeLightOverrides()) }
    val lights = states.values.filter { it.domain == "light" }.sortedBy { it.friendlyName }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        item {
            DetailTopBar("Manage home page", onBack)
            Text(
                "Home shows your welcome message, then the sections below. Changes apply as soon as you make them.",
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
        for ((key, label) in HOME_SECTIONS) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f))
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
        }
        for (light in lights) {
            item {
                val shown = isLightShownOnHome(light, overrides)
                val defaultExcluded = light.entityId !in overrides && isIndicatorLight(light)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(light.friendlyName, modifier = Modifier.weight(1f))
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
        item { Spacer(Modifier.height(16.dp)) }
    }
}
