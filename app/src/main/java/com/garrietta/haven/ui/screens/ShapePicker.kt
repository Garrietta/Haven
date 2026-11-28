package com.garrietta.haven.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.garrietta.haven.data.TILE_PRESETS
import com.garrietta.haven.ui.components.isDarkBackdrop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.ShapePickerTarget
import com.garrietta.haven.ui.theme.FloatingCorner
import com.garrietta.haven.ui.components.SHAPE_OPTIONS
import com.garrietta.haven.data.CARD_CORNER_OPTIONS
import com.garrietta.haven.ui.components.ShapeOption
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.components.shapeForKey
import com.garrietta.haven.ui.components.shapeLabel
import androidx.compose.material3.Icon

/**
 * Shape picker dialog (2026-10-30): a grid of the M3E shapes
 * with live preview, reachable from Settings (global default) and
 * from each entity's sheet (per-entity override). Choices persist
 * in SettingsStore and apply immediately via
 * [HavenViewModel.bumpShapePrefs] (HavenApp re-reads the prefs
 * into LocalShapePrefs). The current domain shapes remain the
 * default unless changed: "Use default" clears a per-entity
 * override back to the global (or domain) shape. Centered dialog
 * panel on the #77 model (0.9 width capped at 480dp, 85% height
 * cap with inner scroll); the scrim tap and back both close.
 */
@Composable
fun ShapePickerDialog(vm: HavenViewModel, target: ShapePickerTarget, onClose: () -> Unit) {
    BackHandler { onClose() }
    val entity = target.entityId?.let { vm.states.value[it] }
    val store = vm.settingsStore
    val currentKey = if (target.forCard) {
        if (target.entityId != null) store.cardShapeOverrides()[target.entityId] else store.globalCardShape
    } else {
        if (target.entityId != null) store.badgeShapeOverrides()[target.entityId] else store.globalBadgeShape
    }
    val accent = if (entity != null) accentFor(entity.entityId) else MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            val panelHeight = (maxHeight * 0.85f)
            Column(
                Modifier.fillMaxWidth(0.9f).widthIn(max = 480.dp)
                    .heightIn(max = panelHeight)
                    .clip(FloatingCorner)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* consume: panel taps must not dismiss */ }
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    if (target.forCard) "Card shape" else "Badge shape",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (target.entityId != null) "Applies immediately to ${entity?.friendlyName ?: "this entity"}."
                    else "Applies immediately everywhere, unless an entity has its own override.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                if (target.entityId != null) {
                    // Expressive-menus round: large 56dp pill, same tap.
                    PressablePillButton(
                        onClick = {
                            if (target.forCard) store.setCardShapeOverride(target.entityId, null)
                            else store.setBadgeShapeOverride(target.entityId, null)
                            vm.bumpShapePrefs()
                            onClose()
                        },
                        height = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    ) { Text("Use default", fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.height(12.dp))
                }
                // Customization round, item 1: the card picker
                // offers the 8 corner variants FIRST (the default
                // grid language), then the expressive polygons —
                // one picker, not a second one.
                val pickerOptions = if (target.forCard) {
                    CARD_CORNER_OPTIONS.map { ShapeOption(it.key, it.label) } + SHAPE_OPTIONS
                } else {
                    SHAPE_OPTIONS
                }
                pickerOptions.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            val selected = option.key == currentKey
                            Column(
                                Modifier.weight(1f).padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        if (target.forCard) {
                                            if (target.entityId != null) store.setCardShapeOverride(target.entityId, option.key)
                                            else store.globalCardShape = option.key
                                        } else {
                                            if (target.entityId != null) store.setBadgeShapeOverride(target.entityId, option.key)
                                            else store.globalBadgeShape = option.key
                                        }
                                        vm.bumpShapePrefs()
                                        onClose()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(56.dp).background(accent, shapeForKey(option.key) ?: RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (entity != null) {
                                        Icon(
                                            domainIcon(entity),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    shapeLabel(option.key) ?: option.key,
                                    fontSize = 12.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                PressablePillButton(
                    onClick = onClose,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) { Text("Close", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/**
 * Feedback round 15, item 5: per-domain defaults — the Settings ->
 * Shapes dialog that sets the DEFAULT badge shape, DEFAULT card
 * shape, and DEFAULT tile color for one entity type. A domain
 * default applies to every entity of that domain (a socket-named
 * lamp is still a light: defaults key off domain, never name),
 * and a per-entity Card setting still wins over it. Saves apply
 * immediately and the dialog stays open, so all three defaults
 * for a domain are set in one visit; the two version reads below
 * are the recomposition subscription (same pattern as
 * rememberCardCustom), since the store maps are read directly.
 * A chosen default tile color drives badge and icon tints as
 * dark versions of itself through presetBadgeColors, exactly as
 * a per-entity color does.
 */
private val DOMAIN_DEFAULT_OPTIONS = listOf(
    "light" to "Lights",
    "switch" to "Switches",
    "input_boolean" to "Helpers",
    "fan" to "Fans",
    "climate" to "Thermostats",
    "lock" to "Locks",
    "cover" to "Covers",
    "vacuum" to "Vacuums",
    "media_player" to "Media players",
    "sensor" to "Sensors",
    "binary_sensor" to "Binary sensors",
    "alarm_control_panel" to "Alarm panels",
    "weather" to "Weather",
)

@Composable
fun DomainDefaultsDialog(vm: HavenViewModel, onClose: () -> Unit) {
    BackHandler { onClose() }
    @Suppress("UNUSED_VARIABLE")
    val shapeVersion = vm.shapePrefsVersion
    @Suppress("UNUSED_VARIABLE")
    val cardVersion = vm.cardCustomVersion
    val store = vm.settingsStore
    var domain by remember { mutableStateOf("light") }
    var domainExpanded by remember { mutableStateOf(false) }
    val domainLabel = DOMAIN_DEFAULT_OPTIONS.firstOrNull { it.first == domain }?.second ?: domain
    val badgeKey = store.domainBadgeShapes()[domain]
    val cardKey = store.domainCardShapes()[domain]
    val tileKey = store.domainTilePresets()[domain]
    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            val panelHeight = (maxHeight * 0.85f)
            Column(
                Modifier.fillMaxWidth(0.9f).widthIn(max = 480.dp)
                    .heightIn(max = panelHeight)
                    .clip(FloatingCorner)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* consume: panel taps must not dismiss */ }
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    "Domain defaults",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Defaults for one entity type. They apply to every entity of that type " +
                        "unless it has its own Card settings, which always win.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Box {
                    // Feedback round 16, item 5: ONLY this bar
                    // changes — cream, differentiated from the
                    // green selector around it. Nothing else in
                    // the dialog moves.
                    OutlinedButton(
                        onClick = { domainExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF6F5EC),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text("Entity type: $domainLabel") }
                    DropdownMenu(
                        expanded = domainExpanded,
                        onDismissRequest = { domainExpanded = false },
                    ) {
                        for ((key, label) in DOMAIN_DEFAULT_OPTIONS) {
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                domainExpanded = false
                                domain = key
                            })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Badge shape", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                PressablePillButton(
                    onClick = {
                        store.setDomainBadgeShape(domain, null)
                        vm.bumpShapePrefs()
                    },
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { Text("Use default", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(12.dp))
                SHAPE_OPTIONS.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            val selected = option.key == badgeKey
                            Column(
                                Modifier.weight(1f).padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        store.setDomainBadgeShape(domain, option.key)
                                        vm.bumpShapePrefs()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(56.dp).background(accent, shapeForKey(option.key) ?: RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {}
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    shapeLabel(option.key) ?: option.key,
                                    fontSize = 12.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Card shape", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                PressablePillButton(
                    onClick = {
                        store.setDomainCardShape(domain, null)
                        vm.bumpShapePrefs()
                    },
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { Text("Use default", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(12.dp))
                val cardOptions = CARD_CORNER_OPTIONS.map { ShapeOption(it.key, it.label) } + SHAPE_OPTIONS
                cardOptions.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            val selected = option.key == cardKey
                            Column(
                                Modifier.weight(1f).padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        store.setDomainCardShape(domain, option.key)
                                        vm.bumpShapePrefs()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(56.dp).background(accent, shapeForKey(option.key) ?: RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {}
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    shapeLabel(option.key) ?: option.key,
                                    fontSize = 12.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Tile color", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                PressablePillButton(
                    onClick = {
                        store.setDomainTilePreset(domain, null)
                        vm.bumpCardCustom()
                    },
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { Text("Use default", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(12.dp))
                TILE_PRESETS.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { preset ->
                            val selected = preset.key == tileKey
                            Column(
                                Modifier.weight(1f).padding(4.dp)
                                    .clickable {
                                        store.setDomainTilePreset(domain, preset.key)
                                        vm.bumpCardCustom()
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape)
                                        .background(if (isDark) preset.dark else preset.light)
                                        .border(
                                            width = if (selected) 2.dp else 0.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape,
                                        ),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    preset.label,
                                    fontSize = 11.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PressablePillButton(
                    onClick = onClose,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) { Text("Close", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
