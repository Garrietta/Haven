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
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HOME_SECTIONS
import com.garrietta.haven.data.migrateLegacyHiddenSections
import com.garrietta.haven.data.migrateLegacySectionOrder
import com.garrietta.haven.data.HOME_SECTION_ORDER_DEFAULT
import com.garrietta.haven.data.HomeStatusCategory
import com.garrietta.haven.data.isIndicatorLight
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.data.resolveOrder
import com.garrietta.haven.ui.components.SectionTitle

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
fun HomeEditorScreen(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    var welcome by remember { mutableStateOf(vm.settingsStore.homeWelcomeMessage) }
    var hidden by remember { mutableStateOf(migrateLegacyHiddenSections(vm.settingsStore.homeHiddenSections)) }
    var overrides by remember { mutableStateOf(vm.settingsStore.homeLightOverrides()) }
    // Stage G: arrangement state. Rendered orders resolve
    // against the canonical lists at render time, so sections
    // that arrive after first composition still slot in
    // correctly. (Round 24, item 5: room display names, icons,
    // and order moved to Rooms & tabs — the duplicate Rooms
    // block here is gone.)
    var sectionOrder by remember { mutableStateOf(migrateLegacySectionOrder(vm.settingsStore.homeSectionOrder())) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    val lights = states.values.filter { it.domain == "light" }.sortedBy { it.friendlyName }
    val resolvedSectionOrder = resolveOrder(sectionOrder, HOME_SECTION_ORDER_DEFAULT)
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
            SectionTitle("Welcome message")
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
            SectionTitle("Sections")
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
            SectionTitle("Favorites")
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
                FavoritesSheet(vm, states, navigate = navigate, onDismiss = { showFavoritesSheet = false })
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Home Status items")
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
            SectionTitle("Lights")
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
            SectionTitle("Reset customization")
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
                },
            ) {
                Text("Reset home customization")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
