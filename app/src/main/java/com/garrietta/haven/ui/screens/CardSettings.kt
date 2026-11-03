package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.TILE_PRESETS
import com.garrietta.haven.data.customIconKeyFor
import com.garrietta.haven.ui.components.CARD_ICON_KEYS
import com.garrietta.haven.ui.components.CARD_ICON_OPTIONS
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.controls.PressablePillButton

/**
 * Card settings (customization round, item 2): ONE surface for
 * per-entity customization, reached from a card/panel menu or from
 * the edit pencil beside a detail/panel title. Title/rename, icon
 * picker, badge-shape picker (delegates to the existing shape
 * picker — no second shape store), and tile color from a preset
 * list only (the app palette + domain matrix colors; no free color
 * wheel). Saved choices persist in SettingsStore and apply
 * EVERYWHERE the entity appears, because every surface reads the
 * same store maps through the same resolution helpers and
 * recomposes on [HavenViewModel.bumpCardCustom].
 *
 * Surfaces that honor the overrides: Home room cards, Favorites
 * cards (same composables), media cards (title fallback + icon +
 * tile color; the playing track title still wins over a custom
 * title), user-tab EntityTile cards, EntityControlCard headers,
 * detail/panel headers (title + icon + pencil), sheet headers,
 * Home Status panel rows, Favorites sheet rows, Search rows (via
 * EntityControlCard), and the Round 3 vacuum block title.
 * Surfaces that cannot honor them (named, not silently skipped):
 * YAML user-tab cards with an explicit YAML name (the YAML text
 * wins), camera hero quadrants (a joined picture, not a tile),
 * group panels (Home Status group cards, Round 3 section banners —
 * not single entities), the thermostat dial (its own title row),
 * and PersonSheet (display-only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSettingsSheet(vm: HavenViewModel, entityId: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val entity = vm.states.value[entityId]
    val store = vm.settingsStore
    var title by remember { mutableStateOf(store.customTitles()[entityId] ?: entity?.friendlyName ?: "") }
    var iconKey by remember {
        mutableStateOf(customIconKeyFor(store.customIconKeys(), entityId, CARD_ICON_KEYS))
    }
    var presetKey by remember { mutableStateOf(store.customTilePresets()[entityId]) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Card settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (entity != null) {
                Text(entity.entityId, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Text("Title", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(entity?.friendlyName ?: "") },
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Text("Icon", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            CARD_ICON_OPTIONS.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { option ->
                        val selected = option.key == iconKey
                        Box(
                            Modifier.weight(1f).padding(4.dp).size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { iconKey = option.key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(option.icon, contentDescription = option.label, modifier = Modifier.size(24.dp))
                        }
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            PressablePillButton(
                onClick = { iconKey = null },
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) { Text("Use domain icon", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(16.dp))
            Text("Badge shape", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            PressablePillButton(
                onClick = { vm.openShapePicker(entityId, false) },
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    shapeLabel(store.badgeShapeOverrides()[entityId] ?: store.globalBadgeShape) ?: "Domain default",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Tile color", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            TILE_PRESETS.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { preset ->
                        val selected = preset.key == presetKey
                        Column(
                            Modifier.weight(1f).padding(4.dp).clickable { presetKey = preset.key },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape)
                                    .background(preset.light)
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape,
                                    ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(preset.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            PressablePillButton(
                onClick = { presetKey = null },
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) { Text("Default tile color", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                PressablePillButton(
                    onClick = {
                        store.setCustomTitle(entityId, null)
                        store.setCustomIconKey(entityId, null)
                        store.setCustomTilePreset(entityId, null)
                        vm.bumpCardCustom()
                        onDismiss()
                    },
                    height = 56.dp,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) { Text("Reset", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(12.dp))
                PressablePillButton(
                    onClick = {
                        val trimmed = title.trim()
                        store.setCustomTitle(entityId, if (trimmed.isEmpty() || trimmed == entity?.friendlyName) null else trimmed)
                        store.setCustomIconKey(entityId, iconKey)
                        store.setCustomTilePreset(entityId, presetKey)
                        vm.bumpCardCustom()
                        onDismiss()
                    },
                    height = 56.dp,
                    modifier = Modifier.weight(1f),
                ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** The Favorites-pencil-style edit affordance beside a detail/panel title (item 2 companion ask). */
@Composable
fun CardSettingsPencil(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Outlined.Edit, contentDescription = "Card settings", modifier = Modifier.size(20.dp))
    }
}

/**
 * Snapshot of the card-customization maps, subscribed to
 * [HavenViewModel.cardCustomVersion] so any Card settings save
 * recomposes every call site that reads it — the same pattern as
 * homePrefsVersion/shapePrefsVersion.
 */
data class CardCustomSnapshot(
    val titles: Map<String, String>,
    val iconKeys: Map<String, String>,
    val tilePresets: Map<String, String>,
)

@Composable
fun rememberCardCustom(vm: HavenViewModel): CardCustomSnapshot {
    @Suppress("UNUSED_VARIABLE")
    val version = vm.cardCustomVersion
    val store = vm.settingsStore
    return remember(version) {
        CardCustomSnapshot(store.customTitles(), store.customIconKeys(), store.customTilePresets())
    }
}
