package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.TILE_PRESETS
import com.garrietta.haven.data.TilePreset
import com.garrietta.haven.data.customIconKeyFor
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.data.presetBadgeColors
import com.garrietta.haven.data.tilePresetColor
import com.garrietta.haven.data.tilePresetFor
import com.garrietta.haven.ui.components.CARD_ICON_GROUPS
import com.garrietta.haven.ui.components.CARD_ICON_KEYS
import com.garrietta.haven.ui.components.CARD_ICON_OPTIONS
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.cardIconForKey
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.theme.HavenChrome
import com.garrietta.haven.ui.theme.polygonShape

/**
 * A tile's options: its color, its icon, its name and its badge shape,
 * opened by pressing and holding the tile (and from the Card settings
 * row of a device's settings). A color or an icon applies the moment it
 * is tapped, so the tile behind the sheet changes as you choose; the
 * name is kept when the sheet closes. Opened from a grid that can be
 * rearranged, it also carries Arrange.
 *
 * Choices are stored per entity in SettingsStore and apply everywhere
 * the entity appears (tiles, the room switches on Home, popups, search),
 * because every surface resolves them through the same helpers and
 * recomposes on [HavenViewModel.bumpCardCustom]. A color is an on-state
 * color: it shows while the device is on, and an off tile stays quiet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSettingsSheet(vm: HavenViewModel, entityId: String, onArrange: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val entity = vm.states.value[entityId]
    val store = vm.settingsStore
    val custom = rememberCardCustom(vm)
    val iconKey = customIconKeyFor(custom.iconKeys, entityId, CARD_ICON_KEYS)
    val presetKey = custom.tilePresets[entityId]
    var title by remember { mutableStateOf(store.customTitles()[entityId] ?: entity?.friendlyName ?: "") }
    val keepTitle = {
        val trimmed = title.trim()
        val stored = if (trimmed.isEmpty() || trimmed == entity?.friendlyName) null else trimmed
        if (stored != store.customTitles()[entityId]) {
            store.setCustomTitle(entityId, stored)
            vm.bumpCardCustom()
        }
    }
    val close = {
        keepTitle()
        onDismiss()
    }
    fun pickIcon(key: String?) {
        store.setCustomIconKey(entityId, key)
        vm.bumpCardCustom()
    }
    fun pickColor(key: String?) {
        store.setCustomTilePreset(entityId, key)
        vm.bumpCardCustom()
    }
    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    ModalBottomSheet(onDismissRequest = close, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            if (entity != null) {
                SheetHeader(entity, title.ifBlank { entity.friendlyName }, iconKey, presetKey, isDark)
                Spacer(Modifier.height(24.dp))
            }

            SectionTitle("Color")
            Text(
                "Shown while it is on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            val automatic = entity?.let { domainCardColors(it, isDark)?.container }
                ?: MaterialTheme.colorScheme.surfaceContainerHigh
            val swatches: List<Pair<TilePreset?, Color>> =
                listOf(null to automatic) + TILE_PRESETS.map { it to tilePresetColor(it, isDark)!! }
            for (row in swatches.chunked(SWATCHES_PER_ROW)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((preset, color) in row) {
                        Swatch(
                            color = color,
                            label = preset?.let { SWATCH_NAMES[it.key] ?: it.label } ?: "Auto",
                            selected = presetKey == preset?.key,
                            onClick = { pickColor(preset?.key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(SWATCHES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Icon")
            Spacer(Modifier.height(12.dp))
            if (entity != null) {
                IconRow(listOf(IconChoice(null, "Automatic", domainIcon(entity))), iconKey, ::pickIcon)
            }
            for (group in CARD_ICON_GROUPS) {
                val options = CARD_ICON_OPTIONS.filter { it.group == group }
                if (options.isEmpty()) continue
                Text(
                    group,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                for (row in options.chunked(ICONS_PER_ROW)) {
                    IconRow(row.map { IconChoice(it.key, it.label, it.icon) }, iconKey, ::pickIcon)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("Name")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(entity?.friendlyName ?: "") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keepTitle() }),
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("Badge shape")
            Spacer(Modifier.height(8.dp))
            PressablePillButton(
                onClick = { vm.openShapePicker(entityId, false) },
                height = 52.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    shapeLabel(store.badgeShapeOverrides()[entityId] ?: store.globalBadgeShape) ?: "Domain default",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(24.dp))
            if (onArrange != null) {
                PressablePillButton(
                    onClick = {
                        close()
                        onArrange()
                    },
                    height = 52.dp,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) { Text("Arrange tiles", style = MaterialTheme.typography.labelLarge) }
                Spacer(Modifier.height(12.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PressablePillButton(
                    onClick = {
                        store.setCustomTitle(entityId, null)
                        store.setCustomIconKey(entityId, null)
                        store.setCustomTilePreset(entityId, null)
                        vm.bumpCardCustom()
                        onDismiss()
                    },
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) { Text("Reset", style = MaterialTheme.typography.labelLarge) }
                PressablePillButton(
                    onClick = close,
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                ) { Text("Done", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

private const val SWATCHES_PER_ROW = 5

/** The swatches go by their color's name; the preset labels say what each color is for. */
private val SWATCH_NAMES = mapOf(
    "heating" to "Coral",
    "cooling" to "Blue",
    "lock" to "Indigo",
    "alert" to "Red",
    "media" to "Lavender",
    "sage" to "Sage",
)
private const val ICONS_PER_ROW = 5
private val CHOICE_SIZE: Dp = 52.dp

/** The tile's badge as it will look while on, beside its name. */
@Composable
private fun SheetHeader(entity: HaEntity, name: String, iconKey: String?, presetKey: String?, isDark: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val preset = presetKey?.let { key -> TILE_PRESETS.firstOrNull { it.key == key } }
    val (badgeBg, badgeGlyph) = preset?.let { presetBadgeColors(it, isDark) }
        ?: tileBadgePair(emptyMap(), emptyMap(), entity, isDark, scheme, accentFor(entity.entityId))
    val shape = LocalShapePrefs.current.badgeShapeFor(entity.entityId, entity.domain)
        ?: polygonShape(shapeForDomain(entity.domain))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(64.dp).background(badgeBg, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(cardIconForKey(iconKey) ?: domainIcon(entity), contentDescription = null, tint = badgeGlyph, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(entity.entityId, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * One color to choose. The chosen one sits in the lime capsule, the
 * app's sign for "this one", with a check on the color itself.
 */
@Composable
private fun Swatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = label }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(CHOICE_SIZE).background(if (selected) HavenChrome.capsule else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(if (selected) CHOICE_SIZE - 12.dp else CHOICE_SIZE - 4.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    // The swatches are pastel by day and deep by night, as onSurface is the other way round.
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private data class IconChoice(val key: String?, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/** A row of icons to choose, each with its name under it; the chosen one is the lime capsule. */
@Composable
private fun IconRow(choices: List<IconChoice>, chosen: String?, onPick: (String?) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (choice in choices) {
            val selected = choice.key == chosen
            Column(
                Modifier.weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(role = Role.RadioButton) { onPick(choice.key) }
                    .semantics { this.selected = selected; contentDescription = choice.label }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(CHOICE_SIZE).background(
                        if (selected) HavenChrome.capsule else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        choice.icon,
                        contentDescription = null,
                        tint = if (selected) HavenChrome.onCapsule else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    choice.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        repeat(ICONS_PER_ROW - choices.size) { Spacer(Modifier.weight(1f)) }
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
    // Feedback round 15, item 5: domain defaults resolve after
    // per-entity presets (see tilePresetFor); a domain color
    // drives badge/icon tints as dark versions of itself through
    // the same presetBadgeColors derivation as a per-entity color.
    val domainTilePresets: Map<String, String> = emptyMap(),
)

@Composable
fun rememberCardCustom(vm: HavenViewModel): CardCustomSnapshot {
    @Suppress("UNUSED_VARIABLE")
    val version = vm.cardCustomVersion
    val store = vm.settingsStore
    return remember(version) {
        CardCustomSnapshot(store.customTitles(), store.customIconKeys(), store.customTilePresets(), store.domainTilePresets())
    }
}
