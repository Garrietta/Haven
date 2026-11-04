package com.garrietta.haven.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailPopupTitleHeader
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ALARM_ARM_MODES
import com.garrietta.haven.data.ALARM_ID
import com.garrietta.haven.data.GREENHOUSE_AUTOMATION_IDS
import com.garrietta.haven.data.GREENHOUSE_TIMER_IDS
import com.garrietta.haven.data.GREENHOUSE_WATER_DUE_ID
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.MEDIA_PLAYER_IDS
import com.garrietta.haven.data.PresenceGroup
import com.garrietta.haven.data.REMOTE_TV_ID
import com.garrietta.haven.data.SIREN_ID
import com.garrietta.haven.data.SPRINKLER_BATTERY_ID
import com.garrietta.haven.data.SPRINKLER_DURATION_ID
import com.garrietta.haven.data.SPRINKLER_SWITCH_ID
import com.garrietta.haven.data.TvCommand
import com.garrietta.haven.data.VACUUM_AUTO_EMPTY_ID
import com.garrietta.haven.data.VACUUM_AUTO_WASH_ID
import com.garrietta.haven.data.VACUUM_BATTERY_ID
import com.garrietta.haven.data.VACUUM_CHILD_LOCK_ID
import com.garrietta.haven.data.VACUUM_CLEANING_MODE_ID
import com.garrietta.haven.data.VACUUM_CLEAN_ROOM_ID
import com.garrietta.haven.data.VACUUM_CONSUMABLES
import com.garrietta.haven.data.VACUUM_DND_END_ID
import com.garrietta.haven.data.VACUUM_DND_START_ID
import com.garrietta.haven.data.VACUUM_DOCK_BUTTON_ID
import com.garrietta.haven.data.VACUUM_DOCK_STATUS_ID
import com.garrietta.haven.data.VACUUM_DRY_MOP_BUTTON_ID
import com.garrietta.haven.data.VACUUM_EMPTY_BIN_BUTTON_ID
import com.garrietta.haven.data.VACUUM_FIND_ROBOT_ID
import com.garrietta.haven.data.VACUUM_ID
import com.garrietta.haven.data.VACUUM_MAP_CAMERA_ID
import com.garrietta.haven.data.VACUUM_MOP_INTENSITY_ID
import com.garrietta.haven.data.VACUUM_PAUSE_BUTTON_ID
import com.garrietta.haven.data.VACUUM_START_BUTTON_ID
import com.garrietta.haven.data.VACUUM_STOP_DRY_MOP_BUTTON_ID
import com.garrietta.haven.data.VACUUM_SUCTION_ID
import com.garrietta.haven.data.VACUUM_TASK_STATUS_ID
import com.garrietta.haven.data.VACUUM_TOTAL_AREA_ID
import com.garrietta.haven.data.VACUUM_TOTAL_COUNT_ID
import com.garrietta.haven.data.VACUUM_TOTAL_TIME_ID
import com.garrietta.haven.data.VACUUM_WASH_MOP_BUTTON_ID
import com.garrietta.haven.data.VACUUM_WATER_LEVEL_ID
import com.garrietta.haven.data.alarmArmService
import com.garrietta.haven.data.alarmModeLabel
import com.garrietta.haven.data.batteryForTracker
import com.garrietta.haven.data.consumableFraction
import com.garrietta.haven.data.dndRange
import com.garrietta.haven.data.formatCountdown
import com.garrietta.haven.data.groupAllTrackers
import com.garrietta.haven.data.groupPresence
import com.garrietta.haven.data.lastTriggeredLine
import com.garrietta.haven.data.locationForTracker
import com.garrietta.haven.data.parseHaDuration
import com.garrietta.haven.data.prettifyIdName
import com.garrietta.haven.data.securityChecklist
import com.garrietta.haven.data.timerFraction
import com.garrietta.haven.data.unmappedTrackers
import com.garrietta.haven.data.updateDisplayName
import com.garrietta.haven.data.updatesSummary
import com.garrietta.haven.data.vacuumTileLine
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.screens.rememberCardCustom
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.data.detailPageAccent
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.components.reportCardShape
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.controls.MediaSquircle
import com.garrietta.haven.ui.controls.MediaTransportRow
import com.garrietta.haven.ui.controls.MediaVolumeRow
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.ServiceSlider
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.data.humanizeOptionLabel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Round 3 surfaces (2026-11-02): vacuum, media, security, greenhouse,
 * presence, and system updates. All full-screen panels render as
 * plain columns with horizontal padding — the PanelOverlay morph
 * container owns the scroll wrapper, the back bar, and the
 * status-bar padding, exactly like the existing detail pages —
 * so no panel here owns its own scroll or fills the screen.
 * No surface shows a raw entity id or a raw ISO timestamp.
 * Nothing arms, disarms, installs, or sounds a siren without an
 * explicit tap; there is no automatic arming or installing
 * anywhere in this file.
 */

// ---- Shared small pieces ----

@Composable
private fun Round3SectionTitle(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PanelSectionTitle(title: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PanelBodyText(text: String) {
    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Expressive-menus round: a LARGE 56dp pill button that never looks
 * enabled when it is not (was a default OutlinedButton). Covers the
 * vacuum / security / media panel actions; calls are identical.
 */
@Composable
private fun PillAction(
    label: String,
    enabled: Boolean = true,
    // Feedback round 5, item 4: the panel action pill carries the panel entity's tile color
    // when a preset is set, else the scheme default. onClick stays LAST so existing
    // trailing-lambda call sites keep working; disabled pills keep their muted law via
    // the caller's alpha/enabled handling (PressablePillButton has no disabled color of its
    // own — callers dim via Modifier.alpha, as the vacuum panel does).
    containerColor: androidx.compose.ui.graphics.Color? = null,
    contentColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
) {
    PressablePillButton(
        onClick = onClick,
        enabled = enabled,
        height = 56.dp,
        containerColor = containerColor ?: MaterialTheme.colorScheme.secondaryContainer,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

/** One labeled on/off row (vacuum toggles, greenhouse automations). */
@Composable
private fun ToggleRow(label: String, sub: String?, checked: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (sub != null) {
                Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}

/**
 * Grouped card for Round 4 panel redesigns (Security, vacuum):
 * surfaceContainerLow, extraLarge shape, 16dp padding. Sections
 * that were plain columns become cards; service calls, button
 * ids, and behaviors inside are byte-identical.
 */
@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = generalCardShape(0),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
    Spacer(Modifier.height(16.dp))
}

/**
 * Expressive action pill for Round 4 panel redesigns: a
 * PressablePillButton that reads selected (primaryContainer) vs
 * unselected (secondaryContainer). Labels are humanized by the
 * caller where they come from HA enums.
 */
@Composable
private fun PanelPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    PressablePillButton(
        onClick = onClick,
        height = 56.dp,
        containerColor = if (selected) scheme.primaryContainer else scheme.secondaryContainer,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSecondaryContainer,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Humanize only strings that look like raw HA tokens (contain an
 * underscore or hyphen); already-friendly text ("Motion detected")
 * passes through untouched, so no existing wording changes case.
 */
private fun humanizeIfRaw(raw: String): String =
    if ('_' in raw || '-' in raw) humanizeOptionLabel(raw) else raw

/** Segmented option chips for a select entity; falls back to plain current-value text when the live options list is empty. */
@Composable
private fun SelectChips(entity: HaEntity, vm: HavenViewModel, label: String) {
    val options = entity.attrList("options")?.mapNotNull { it as? String } ?: emptyList()
    Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(4.dp))
    if (options.isEmpty()) {
        // Round 4: the fallback current-value line is humanized
        // too ("Boost_IQ" never renders as "Boost Iq" or raw).
        PanelBodyText("Current: ${humanizeIfRaw(entity.state)}")
    } else {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in options) {
                // Round 4: expressive pill treatment + humanized
                // label; the service call (raw option string) is
                // byte-identical — only the display text changes.
                PanelPill(
                    label = humanizeOptionLabel(option),
                    selected = entity.state == option,
                    onClick = {
                        vm.callService("select", "select_option", entity.entityId, JSONObject().put("option", option))
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ---- Home dashboard blocks (tile -> morph panel) ----

/**
 * Round 3 Home blocks, in order: security banner, vacuum tile,
 * media tiles, greenhouse summary card. Each block renders only
 * when its entities exist in the live state map; a missing
 * domain leaves no empty shell behind. Shared by the folded
 * LazyColumn (as one item) and the unfolded left column.
 */
/**
 * Visual-fidelity round §4: the Round 3 Home blocks as four
 * INDEPENDENTLY gated sections (Security, Robot vacuum, TV &
 * media, Greenhouse & irrigation) — the combined "Round 3 blocks"
 * toggle is unbundled. Each block renders only when its domain
 * exists in live state and keeps the original 16dp trailing
 * spacing, so folded and unfolded spacing is unchanged. The
 * stored preference migrates via migrateRound3Hidden /
 * migrateRound3SectionOrder (combined-on maps to all four on).
 */
@Composable
fun Round3SecurityBlock(vm: HavenViewModel, states: Map<String, HaEntity>, navigate: (Screen) -> Unit) {
    val alarm = states[ALARM_ID]
    if (alarm != null) {
        SecurityBannerSection(alarm, vm) { navigate(Screen.SecurityPanel) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun Round3VacuumBlock(vm: HavenViewModel, states: Map<String, HaEntity>, navigate: (Screen) -> Unit) {
    val vacuum = states[VACUUM_ID]
    if (vacuum != null) {
        VacuumTileSection(vacuum, states, vm) { navigate(Screen.VacuumPanel) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun Round3MediaBlock(vm: HavenViewModel, states: Map<String, HaEntity>, navigate: (Screen) -> Unit) {
    val media = MEDIA_PLAYER_IDS.mapNotNull { states[it] }
    if (media.isNotEmpty()) {
        MediaRowSection(media, vm) { entity -> navigate(Screen.MediaPanel(entity.entityId)) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun Round3GreenhouseBlock(vm: HavenViewModel, states: Map<String, HaEntity>, navigate: (Screen) -> Unit) {
    val greenhousePresent = (GREENHOUSE_AUTOMATION_IDS + GREENHOUSE_TIMER_IDS +
        listOf(GREENHOUSE_WATER_DUE_ID, SPRINKLER_SWITCH_ID)).any { states.containsKey(it) }
    if (greenhousePresent) {
        GreenhouseSummaryCard(states) { navigate(Screen.GreenhousePanel) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SecurityBannerSection(alarm: HaEntity, vm: HavenViewModel, onOpen: () -> Unit) {
    Round3SectionTitle("Security")
    Box(
        Modifier.fillMaxWidth()
            .reportCardBounds(ALARM_ID).reportCardShape(ALARM_ID, generalCardShape(0))
            .hideWhenMorphSource(ALARM_ID)
            .clip(generalCardShape(0))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Security · ${alarmModeLabel(alarm.state)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap for the full panel, or arm right here.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in ALARM_ARM_MODES) {
                    FilterChip(
                        selected = alarm.state == "armed_$mode",
                        onClick = { vm.callService("alarm_control_panel", alarmArmService(mode), ALARM_ID) },
                        label = { Text(alarmModeLabel(mode)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VacuumTileSection(
    vacuum: HaEntity,
    states: Map<String, HaEntity>,
    vm: HavenViewModel,
    onOpen: () -> Unit,
) {
    Round3SectionTitle("Robot vacuum")
    val battery = states[VACUUM_BATTERY_ID]?.state?.toIntOrNull()?.toString()
    Box(
        Modifier.fillMaxWidth()
            .reportCardBounds(VACUUM_ID).reportCardShape(VACUUM_ID, generalCardShape(0))
            .hideWhenMorphSource(VACUUM_ID)
            .clip(generalCardShape(0))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = accentFor(VACUUM_ID))
                Spacer(Modifier.width(8.dp))
                // Customization round, item 2: the vacuum block
                // title honors a custom title for vacuum.t2292.
                Text(
                    customTitleFor(rememberCardCustom(vm).titles, vacuum.entityId, vacuum.friendlyName),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                vacuumTileLine(vacuum.state, battery),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillAction("Clean") { vm.callService("button", "press", VACUUM_START_BUTTON_ID) }
                PillAction("Dock") { vm.callService("button", "press", VACUUM_DOCK_BUTTON_ID) }
            }
        }
    }
}

@Composable
private fun MediaRowSection(
    media: List<HaEntity>,
    vm: HavenViewModel,
    onOpen: (HaEntity) -> Unit,
) {
    Round3SectionTitle("TV & media")
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (entity in media) {
            MediaTile(entity, vm) { onOpen(entity) }
        }
    }
}

@Composable
private fun MediaTile(entity: HaEntity, vm: HavenViewModel, onOpen: () -> Unit) {
    val unavailable = entity.state == "unavailable"
    val off = entity.state == "off"
    val title = entity.attrString("media_title")
    val app = entity.attrString("app_name")
    val accent = accentFor(entity.entityId)
    Box(
        Modifier.width(172.dp)
            .reportCardBounds(entity.entityId).reportCardShape(entity.entityId, generalCardShape(0))
            .hideWhenMorphSource(entity.entityId)
            .clip(generalCardShape(0))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = null,
                    tint = if (unavailable || off) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                )
                Spacer(Modifier.weight(1f))
                // Quick power toggle badge: its tap toggles power only,
                // it never opens the panel (the badge consumes the tap).
                IconButton(
                    onClick = {
                        if (entity.state == "on" || entity.state == "playing" || entity.state == "paused" || entity.state == "idle") {
                            vm.turnOff(entity.entityId)
                        } else if (!unavailable) {
                            vm.turnOn(entity.entityId)
                        }
                    },
                    enabled = !unavailable,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = if (unavailable) MaterialTheme.colorScheme.onSurfaceVariant
                        else if (off) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                entity.friendlyName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    unavailable -> "Unavailable"
                    off -> "Off"
                    title != null -> title
                    app != null -> app
                    else -> entity.stateText()
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GreenhouseSummaryCard(states: Map<String, HaEntity>, onOpen: () -> Unit) {
    Round3SectionTitle("Greenhouse & irrigation")
    val automationsOn = GREENHOUSE_AUTOMATION_IDS.count { states[it]?.state == "on" }
    val automationsTotal = GREENHOUSE_AUTOMATION_IDS.count { states.containsKey(it) }
    val waterDue = states[GREENHOUSE_WATER_DUE_ID]?.state
    val sprinkler = states[SPRINKLER_SWITCH_ID]
    Box(
        Modifier.fillMaxWidth()
            .reportCardBounds(GREENHOUSE_AUTOMATION_IDS.first()).reportCardShape(GREENHOUSE_AUTOMATION_IDS.first(), generalCardShape(0))
            .hideWhenMorphSource(GREENHOUSE_AUTOMATION_IDS.first())
            .clip(generalCardShape(0))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Spa, contentDescription = null, tint = accentFor(GREENHOUSE_AUTOMATION_IDS.first()))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Greenhouse & irrigation",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            if (automationsTotal > 0) {
                Text(
                    "$automationsOn of $automationsTotal automations on",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (waterDue != null) {
                Text(
                    "Water due: $waterDue",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sprinkler != null) {
                Text(
                    if (sprinkler.state == "unavailable") "Sprinklers: unavailable" else "Sprinklers: ${sprinkler.stateText()}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("Tap for automations, timers, and watering.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Presence (Who's home): avatar chips + person sheet ----

/**
 * Who's-home, replacing the old raw comma-separated string:
 * Material 3 avatar chips grouped by person (Garrett, Haley),
 * with unmapped trackers under a clearly labeled Other row.
 * Tapping a chip opens a bottom sheet with that group's devices,
 * battery levels, and geocoded locations from the real sensors.
 * Person entities are not devices and are excluded: the caller
 * passes device trackers that are currently home only.
 */
@Composable
fun PresenceBlock(homeTrackers: List<HaEntity>, states: Map<String, HaEntity>) {
    val groups = groupPresence(homeTrackers)
    var selected by remember { mutableStateOf<PresenceGroup?>(null) }
    Text("Who's home", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    if (groups.isEmpty()) {
        Text("Nobody home", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    } else {
        for (group in groups) {
            if (group.isOther) {
                // UI rewrite round §7: "Other" lives INSIDE its
                // rounded presence chip beside the count badge —
                // never as a bare label outside the container
                // pushing device names out of alignment. The
                // per-device chips are gone; tapping this one chip
                // still opens the sheet with every unmapped device
                // (same as a person's chip).
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { selected = group }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Other",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${group.members.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            } else {
                // One chip per person, regardless of device count.
                PresenceChip(
                    label = group.label,
                    initial = group.initials,
                    accentId = group.members.first().entityId,
                ) { selected = group }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    val open = selected
    if (open != null) {
        PersonSheet(
            group = open,
            devices = if (open.isOther) unmappedTrackers(states, groups) else groupAllTrackers(open, states),
            states = states,
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun PresenceChip(label: String, initial: String, accentId: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(accentFor(accentId)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonSheet(
    group: PresenceGroup,
    devices: List<HaEntity>,
    states: Map<String, HaEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(
                if (group.isOther) "Other devices" else group.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (group.isOther) "Devices not mapped to a person — never attributed to the wrong person."
                else "Devices for ${group.label}, with battery and location from the real sensors.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (devices.isEmpty()) {
                PanelBodyText("No devices to show.")
            }
            for (device in devices) {
                val battery = batteryForTracker(device, states)
                val location = locationForTracker(device, states)
                Column {
                    Text(device.friendlyName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        device.stateText(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (battery != null) {
                        Text("Battery $battery%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (location != null) {
                        Text(location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ---- Full-screen panels (PanelOverlay owns scroll + back bar) ----

/** Vacuum panel: lidar map snapshot, transport, dock actions, selectors, consumables, toggles, DND, lifetime stats. */
@Composable
fun VacuumPanelScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val vacuum = states[VACUUM_ID]
    if (vacuum == null) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            DetailPopupTitleHeader("Robot vacuum")
            PanelBodyText("The vacuum is no longer available.")
        }
        return
    }
    // Feedback round 5, item 4: the panel's action pills carry the vacuum's tile color
    // (ungated — see detailPageAccent; a docked vacuum's panel still shows its sage).
    val vacuumPanelCustom = rememberCardCustom(vm)
    val vacuumPanelAccent = detailPageAccent(vacuumPanelCustom.tilePresets, vacuum, isDarkBackdrop(MaterialTheme.colorScheme.background))
    val vacuumPanelPillContent = if (vacuumPanelAccent != null) MaterialTheme.colorScheme.onSurface else null
    // Lidar map: the map camera's latest snapshot, cache-first like
    // the camera hero — a snapshot, never claimed as a live render.
    LaunchedEffect(VACUUM_MAP_CAMERA_ID) { vm.refreshSnapshot(VACUUM_MAP_CAMERA_ID) }
    val mapBitmap = vm.snapshotFor(VACUUM_MAP_CAMERA_ID)
    val battery = states[VACUUM_BATTERY_ID]?.state?.toIntOrNull()
    val dockStatus = states[VACUUM_DOCK_STATUS_ID]?.state
    val taskStatus = states[VACUUM_TASK_STATUS_ID]?.state
    Column(Modifier.padding(horizontal = 16.dp)) {
        DetailPopupTitleHeader(vacuum.friendlyName)
        Text(
            vacuumTileLine(vacuum.state, battery?.toString()),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        if (dockStatus != null || taskStatus != null) {
            PanelBodyText(
                listOfNotNull(
                    dockStatus?.let { "Dock: ${humanizeIfRaw(it)}" },
                    taskStatus?.let { "Task: ${humanizeIfRaw(it)}" },
                ).joinToString(" · "),
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        PanelSectionTitle("Lidar map")
        if (mapBitmap != null) {
            Image(
                bitmap = mapBitmap.asImageBitmap(),
                contentDescription = "Vacuum lidar map snapshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)),
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PanelBodyText("Map snapshot not available yet.")
                    Spacer(Modifier.height(8.dp))
                    PillAction("Refresh map", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.refreshSnapshot(VACUUM_MAP_CAMERA_ID) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PanelCard {
        PanelSectionTitle("Transport")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillAction("Start", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_START_BUTTON_ID) }
            PillAction("Pause", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_PAUSE_BUTTON_ID) }
            PillAction("Return to Base", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_DOCK_BUTTON_ID) }
        }
        }
        PanelCard {
        PanelSectionTitle("Dock actions")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillAction("Empty Dust Bin", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_EMPTY_BIN_BUTTON_ID) }
            PillAction("Wash Mop", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_WASH_MOP_BUTTON_ID) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillAction("Dry Mop", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_DRY_MOP_BUTTON_ID) }
            PillAction("Stop Dry Mop", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", VACUUM_STOP_DRY_MOP_BUTTON_ID) }
        }
        }
        PanelCard {
        PanelSectionTitle("Cleaning setup")
        for ((id, label) in listOf(
            VACUUM_CLEANING_MODE_ID to "Cleaning mode",
            VACUUM_SUCTION_ID to "Suction level",
            VACUUM_MOP_INTENSITY_ID to "Mop intensity",
            VACUUM_WATER_LEVEL_ID to "Water level",
            VACUUM_CLEAN_ROOM_ID to "Clean room",
        )) {
            val select = states[id]
            if (select != null) SelectChips(select, vm, label)
        }
        }
        PanelCard {
        PanelSectionTitle("Consumables")
        for (consumable in VACUUM_CONSUMABLES) {
            val sensor = states[consumable.sensorId]
            val remaining = sensor?.state?.toDoubleOrNull()
            val fraction = consumableFraction(remaining, consumable.fullLifeHours)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(consumable.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (remaining != null) "${remaining.toInt()} h remaining" else "Remaining unknown",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (fraction != null) {
                        LinearProgressIndicator(
                            progress = { fraction.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                PillAction("Reset", containerColor = vacuumPanelAccent, contentColor = vacuumPanelPillContent) { vm.callService("button", "press", consumable.resetButtonId) }
            }
            Spacer(Modifier.height(12.dp))
        }
        }
        PanelCard {
        PanelSectionTitle("Options")
        for ((id, label) in listOf(
            VACUUM_AUTO_EMPTY_ID to "Auto-empty",
            VACUUM_AUTO_WASH_ID to "Auto-wash",
            VACUUM_CHILD_LOCK_ID to "Child lock",
            VACUUM_FIND_ROBOT_ID to "Find robot",
        )) {
            val toggle = states[id]
            if (toggle != null) {
                ToggleRow(label, null, toggle.state == "on") { vm.toggle(id) }
                Spacer(Modifier.height(8.dp))
            }
        }
        val dnd = dndRange(states[VACUUM_DND_START_ID]?.state, states[VACUUM_DND_END_ID]?.state)
        if (dnd != null) {
            Text("Do not disturb: $dnd", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
        }
        }
        PanelCard {
        PanelSectionTitle("Lifetime")
        val area = states[VACUUM_TOTAL_AREA_ID]?.state?.toDoubleOrNull()
        val count = states[VACUUM_TOTAL_COUNT_ID]?.state?.toIntOrNull()
        val time = states[VACUUM_TOTAL_TIME_ID]?.state?.toDoubleOrNull()
        if (area != null) PanelBodyText("Total cleaning area: ${"%,.0f".format(area)}")
        if (count != null) PanelBodyText("Total cleans: $count")
        if (time != null) PanelBodyText("Total cleaning time: ${"%.1f".format(time)} h")
        if (area == null && count == null && time == null) {
            PanelBodyText("Lifetime stats are not available right now.")
        }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Media panel: Google TV-style D-pad over remote.tv, power, mute, volume, source, transport. Unavailable players render disabled, never controllable. */
@Composable
fun MediaPanelScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    if (entity == null) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            DetailPopupTitleHeader("TV & media")
            PanelBodyText("This player is no longer available.")
        }
        return
    }
    val unavailable = entity.state == "unavailable"
    val off = entity.state == "off"
    val playing = entity.state == "playing"
    val title = entity.attrString("media_title")
    val artist = entity.attrString("media_artist")
    val app = entity.attrString("app_name")
    val source = entity.attrString("source")
    val sources = entity.attrList("source_list")?.mapNotNull { it as? String } ?: emptyList()
    val volume = entity.attrDouble("volume_level") ?: 0.5
    val muted = entity.attrBool("is_volume_muted") ?: false
    val remote = states[REMOTE_TV_ID]
    // Feedback round 5, item 4: the panel's action pill + volume slider carry the player's
    // tile color (container for the pill, badge ink for the slider, as on the sheet).
    val mediaPanelCustom = rememberCardCustom(vm)
    val mediaPanelAccent = detailPageAccent(mediaPanelCustom.tilePresets, entity, isDarkBackdrop(MaterialTheme.colorScheme.background))
    val mediaPanelBadge = com.garrietta.haven.data.detailBadgeOverride(
        mediaPanelCustom.tilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
    )
    val mediaPanelPillContent = if (mediaPanelAccent != null) MaterialTheme.colorScheme.onSurface else null
    Column(Modifier.padding(horizontal = 16.dp)) {
        DetailPopupTitleHeader(entity.friendlyName)
        if (unavailable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Unavailable", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            PanelBodyText("This player is not reachable right now, so no controls are offered for it.")
            Spacer(Modifier.height(16.dp))
            return@Column
        }
        Text(
            when {
                off -> "Off"
                title != null -> title
                app != null -> app
                else -> entity.stateText()
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist != null || source != null) {
            Spacer(Modifier.height(4.dp))
            PanelBodyText(listOfNotNull(artist, source?.let { "Source: $it" }).joinToString(" · "))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PillAction(if (off) "Turn on" else "Turn off", containerColor = mediaPanelAccent, contentColor = mediaPanelPillContent) {
                if (off) vm.turnOn(entityId) else vm.turnOff(entityId)
            }
            Text(
                if (playing) "Playing" else entity.stateText(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        PanelSectionTitle("Remote")
        if (remote == null) {
            PanelBodyText("No TV remote was found, so the D-pad is not shown.")
        } else if (remote.state == "unavailable") {
            PanelBodyText("TV remote unavailable — the D-pad is disabled until it comes back.")
        } else {
            if (remote.state == "off") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PanelBodyText("Remote is off. ")
                    Text(
                        "Turn remote on",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { vm.turnOn(REMOTE_TV_ID) },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            DpadGrid { command ->
                vm.callService(
                    "remote", "send_command", REMOTE_TV_ID,
                    JSONObject().put("command", JSONArray().put(command)),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PanelSectionTitle("Volume")
        // Expressive-menus round: mute squircle pill + slider (was a
        // bare IconButton); the slider and its call are unchanged.
        MediaVolumeRow(
            muted = muted,
            onToggleMute = {
                vm.callService(
                    "media_player", "volume_mute", entityId,
                    JSONObject().put("is_volume_muted", !muted),
                )
            },
        ) {
            // Customization round, item 8: media badge fill
            // accent (very light purple #DAB9FF in dark mode).
            // Feedback round 5, item 4: a preset tile color's dark
            // ink wins over the matrix fill, as on the sheet.
            val panelMediaAccent = mediaPanelBadge?.first
                ?: vm.states.value[entityId]?.let {
                    domainCardColors(it, isDarkBackdrop(MaterialTheme.colorScheme.background))?.badgeBg
                } ?: accentFor(entityId)
            ServiceSlider(
                (volume * 100).toFloat(), 0f..100f,
                { vm.callService("media_player", "volume_set", entityId, JSONObject().put("volume_level", it / 100f)) },
                panelMediaAccent,
                formatValue = { "${it.toInt()}%" },
            )
        }
        if (sources.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PanelSectionTitle("Input source")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Expressive-menus round: sources are large 56dp
                // PanelPills (the real output affordance — HA has no
                // queue), not 32dp FilterChips.
                for (item in sources) {
                    PanelPill(
                        label = item,
                        selected = source == item,
                        onClick = {
                            vm.callService("media_player", "select_source", entityId, JSONObject().put("source", item))
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PanelSectionTitle("Transport")
        MediaTransportRow(
            playing = playing,
            onPrevious = { vm.callService("media_player", "media_previous_track", entityId) },
            onPlayPause = { vm.callService("media_player", "media_play_pause", entityId) },
            onNext = { vm.callService("media_player", "media_next_track", entityId) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** 3x3 Google TV-style D-pad: Back / Up / Home, Left / OK / Right, Mute / Down / (spacer). */
@Composable
private fun DpadGrid(onCommand: (String) -> Unit) {
    val rows: List<List<Pair<String, String>?>> = listOf(
        listOf("Back" to TvCommand.BACK, "↑" to TvCommand.UP, "Home" to TvCommand.HOME),
        listOf("←" to TvCommand.LEFT, "OK" to TvCommand.CENTER, "→" to TvCommand.RIGHT),
        listOf("Mute" to TvCommand.MUTE, "↓" to TvCommand.DOWN, null),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (cell in row) {
                    if (cell == null) {
                        Spacer(Modifier.width(88.dp))
                    } else {
                        PressablePillButton(
                            onClick = { onCommand(cell.second) },
                            modifier = Modifier.width(88.dp),
                            height = 56.dp,
                            shape = MediaSquircle,
                            containerColor = if (cell.first == "OK") MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (cell.first == "OK") MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                        ) { Text(cell.first, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Security panel: large arming selector, PIN pad ONLY when the
 * alarm entity reports a code format, open door / motion
 * checklist from real binary sensors, and a two-step emergency
 * siren trigger. Every arm/disarm/siren action is an explicit
 * tap — nothing here arms automatically.
 */
@Composable
fun SecurityPanelScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val alarm = states[ALARM_ID]
    if (alarm == null) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            DetailPopupTitleHeader("Security")
            PanelBodyText("The alarm panel is no longer available.")
        }
        return
    }
    var code by remember { mutableStateOf("") }
    var sirenConfirm by remember { mutableStateOf(false) }
    val codeFormat = alarm.attrString("code_format")
    val codeData = if (code.isNotEmpty()) JSONObject().put("code", code) else null
    val siren = states[SIREN_ID]
    Column(Modifier.padding(horizontal = 16.dp)) {
        DetailPopupTitleHeader("Security")
        Text(
            alarmModeLabel(alarm.state),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        PanelBodyText("Arming changes state immediately on tap — nothing arms on its own.")
        Spacer(Modifier.height(16.dp))
        // Round 4: grouped card + expressive pills; arm service
        // calls and labels are byte-identical.
        PanelCard {
            PanelSectionTitle("Arm")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in ALARM_ARM_MODES) {
                    PanelPill(
                        label = alarmModeLabel(mode),
                        selected = alarm.state == "armed_$mode",
                        onClick = { vm.callService("alarm_control_panel", alarmArmService(mode), ALARM_ID, codeData) },
                    )
                }
                PanelPill(
                    label = "Disarm",
                    selected = alarm.state == "disarmed",
                    onClick = { vm.callService("alarm_control_panel", "alarm_disarm", ALARM_ID, codeData) },
                )
            }
        }
        if (codeFormat != null) {
            PanelCard {
            PanelSectionTitle("PIN")
            Text(
                "Code: ${"•".repeat(code.length)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
            for (row in digits.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (digit in row) {
                        OutlinedButton(
                            onClick = {
                                when (digit) {
                                    "C" -> code = ""
                                    "⌫" -> code = code.dropLast(1)
                                    else -> code += digit
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(digit) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            } // PIN PanelCard
        } else {
            PanelBodyText("This alarm panel does not ask for a PIN, so no keypad is shown.")
            Spacer(Modifier.height(8.dp))
        }
        PanelCard {
            PanelSectionTitle("Doors & motion")
            val rows = securityChecklist(states.values.filter { it.domain == "binary_sensor" })
            if (rows.isEmpty()) {
                PanelBodyText("No door or motion sensors to show.")
            }
            for (row in rows) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(row.statusText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (row.active) "●" else "○",
                        fontSize = 16.sp,
                        color = if (row.active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (siren != null) {
            PanelCard {
            PanelSectionTitle("Emergency siren")
            if (siren.state == "on") {
                PillAction("Stop siren") { vm.callService("siren", "turn_off", SIREN_ID) }
            } else if (!sirenConfirm) {
                Button(onClick = { sirenConfirm = true }) { Text("Sound outdoor siren") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        sirenConfirm = false
                        vm.callService("siren", "turn_on", SIREN_ID)
                    }) { Text("Confirm — sound siren") }
                    OutlinedButton(onClick = { sirenConfirm = false }) { Text("Cancel") }
                }
            }
            Spacer(Modifier.height(8.dp))
            PanelBodyText("The siren only sounds after the confirm tap.")
            } // siren PanelCard
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Greenhouse & irrigation panel: automation toggle cards, timer countdown rings, water-due counter, sprinklers in their real state. */
@Composable
fun GreenhousePanelScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    Column(Modifier.padding(horizontal = 16.dp)) {
        DetailPopupTitleHeader("Greenhouse & irrigation")
        PanelSectionTitle("Automations")
        var anyAutomation = false
        for (id in GREENHOUSE_AUTOMATION_IDS) {
            val auto = states[id]
            if (auto == null) continue
            anyAutomation = true
            ToggleRow(auto.friendlyName, lastTriggeredLine(auto), auto.state == "on") { vm.toggle(id) }
            Spacer(Modifier.height(8.dp))
        }
        if (!anyAutomation) PanelBodyText("No greenhouse automations found.")
        Spacer(Modifier.height(8.dp))
        PanelSectionTitle("Timers")
        var anyTimer = false
        for (id in GREENHOUSE_TIMER_IDS) {
            val timer = states[id]
            if (timer == null) continue
            anyTimer = true
            TimerRingRow(timer)
            Spacer(Modifier.height(12.dp))
        }
        if (!anyTimer) PanelBodyText("No greenhouse timers found.")
        Spacer(Modifier.height(8.dp))
        PanelSectionTitle("Watering")
        val counter = states[GREENHOUSE_WATER_DUE_ID]
        if (counter != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Water due", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(counter.state, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedButton(onClick = { vm.callService("counter", "decrement", GREENHOUSE_WATER_DUE_ID) }) { Text("−") }
                OutlinedButton(onClick = { vm.callService("counter", "increment", GREENHOUSE_WATER_DUE_ID) }) { Text("+") }
                OutlinedButton(onClick = { vm.callService("counter", "reset", GREENHOUSE_WATER_DUE_ID) }) { Text("Reset") }
            }
            Spacer(Modifier.height(12.dp))
        }
        val sprinkler = states[SPRINKLER_SWITCH_ID]
        if (sprinkler != null) {
            ToggleRow(
                label = sprinkler.friendlyName,
                sub = if (sprinkler.state == "unavailable") "Unavailable — shown as-is, never as working" else sprinkler.stateText(),
                checked = sprinkler.state == "on",
                enabled = sprinkler.state != "unavailable",
            ) { vm.toggle(SPRINKLER_SWITCH_ID) }
            Spacer(Modifier.height(8.dp))
        }
        val duration = states[SPRINKLER_DURATION_ID]
        if (duration != null) {
            Text(
                if (duration.state == "unavailable") "Watering duration: unavailable" else "Watering duration: ${duration.stateText()}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
        }
        val battery = states[SPRINKLER_BATTERY_ID]
        if (battery != null) {
            Text(
                "Sprinkler battery: ${battery.stateText()}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TimerRingRow(timer: HaEntity) {
    val fraction = timerFraction(timer)
    val remaining = parseHaDuration(timer.attrString("remaining"))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            if (fraction != null) {
                CircularProgressIndicator(
                    progress = { fraction.toFloat() },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                )
            } else {
                CircularProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Text(
                if (remaining != null) formatCountdown(remaining) else timer.stateText(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(prettifyIdName(timer), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(timer.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * System updates panel: every update.* entity in one place, with
 * a badge count of the ones actually showing an update, release
 * notes where the entity provides them, and one-tap install per
 * update. Installs happen ONLY on the button tap — never
 * automatically, never on panel open.
 */
@Composable
fun UpdatesPanelScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val summary = updatesSummary(states)
    Column(Modifier.padding(horizontal = 16.dp)) {
        DetailPopupTitleHeader("System Updates")
        Text(
            if (summary.available == 0) "Everything is up to date"
            else if (summary.available == 1) "1 update available" else "${summary.available} updates available",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        PanelBodyText("${summary.total} update sources tracked. Nothing installs without your tap.")
        Spacer(Modifier.height(16.dp))
        if (summary.updates.isEmpty()) {
            PanelBodyText("No update entities found.")
        }
        for (update in summary.updates) {
            val available = update.state == "on"
            val installed = update.attrString("installed_version")
            val latest = update.attrString("latest_version")
            val notes = update.attrString("release_summary")
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(updateDisplayName(update), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        if (installed != null || latest != null) {
                            Text(
                                listOfNotNull(
                                    installed?.let { "Installed $it" },
                                    latest?.let { "Latest $it" },
                                ).joinToString(" → "),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (available) {
                        Button(onClick = { vm.callService("update", "install", update.entityId) }) { Text("Install") }
                    } else {
                        Text("Up to date", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (notes != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
