package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ALARM_ARM_MODES
import com.garrietta.haven.data.SECURITY_DETECTION
import com.garrietta.haven.data.SECURITY_DOORS
import com.garrietta.haven.data.SECURITY_CAMERAS
import com.garrietta.haven.data.securitySectionFor
import com.garrietta.haven.data.takenOff
import com.garrietta.haven.data.curated
import com.garrietta.haven.data.CameraSecurity
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.SIREN_ID
import com.garrietta.haven.data.alarmArmService
import com.garrietta.haven.data.alarmChoice
import com.garrietta.haven.data.alarmModeLabel
import com.garrietta.haven.data.detectionLabel
import com.garrietta.haven.data.formatHaTimestamp
import com.garrietta.haven.data.looseDetectionSwitches
import com.garrietta.haven.data.securityAlarm
import com.garrietta.haven.data.securityCameras
import com.garrietta.haven.data.securityChecklist
import com.garrietta.haven.data.securitySummary
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.polygonShape
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.ScreenTitle
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.choiceChipColors
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.theme.Corner
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors

/*
 * The Security tab: whether the house is shut, the alarm, every camera,
 * the switches that turn each camera's detection on and off, and the
 * doors and locks. The cameras live here rather than on Home, which
 * keeps to what the house is doing.
 *
 * Nothing on this page unlocks or sounds anything in one tap: a lock
 * opens its own popup (which asks before unlocking), and the siren asks
 * for a second tap.
 */
@Composable
fun SecurityScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    // Each section shows what Home Assistant has, arranged by hand
    // (PageCuration): taken off, added to and put in order in Edit.
    val exists: (String) -> Boolean = { states[it] != null }
    val cameraDefaults = vm.securityCameraIds(states)
    val cameraIds = curated(cameraDefaults, vm.curation(SECURITY_CAMERAS), exists)
    val cameras = securityCameras(cameraIds, states, entityDevices)
    val loose = looseDetectionSwitches(cameraIds, states, entityDevices)
    val detectionDefaults = (cameras.flatMap { it.switches } + loose).map { it.entityId }.distinct()
    val detectionIds = curated(detectionDefaults, vm.curation(SECURITY_DETECTION), exists)
    val doorDefaults = securityDoorIds(states)
    val doorIds = curated(doorDefaults, vm.curation(SECURITY_DOORS), exists)
    val summary = securitySummary(states)
    val alarm = securityAlarm(states)
    val columns = if (LocalWideLayout.current) 3 else 2
    var editing by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var addQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "head") {
            EditableTitle("Security", editing) { editing = !editing; addQuery = "" }
            Spacer(Modifier.height(12.dp))
            if (!editing) {
                SummaryCard(summary.line, summary.calm)
                Spacer(Modifier.height(20.dp))
            }
        }
        if (editing) {
            fun section(key: String, title: String, defaults: List<String>, shown: List<String>) = EditorSection(
                key, title,
                shown.map { editorItem(states[it], it) },
                takenOff(defaults, vm.curation(key)).map { editorItem(states[it], it) },
            )
            pageEditor(
                vm,
                sections = listOf(
                    section(SECURITY_CAMERAS, "Cameras", cameraDefaults, cameraIds),
                    section(SECURITY_DOORS, "Doors & locks", doorDefaults, doorIds),
                    section(SECURITY_DETECTION, "Motion detection", detectionDefaults, detectionIds),
                ),
                candidates = { q ->
                    val onPage = (cameraIds + doorIds + detectionIds).toSet()
                    if (q.isEmpty()) emptyList() else states.values
                        .filter { it.entityId !in onPage && (it.friendlyName.lowercase().contains(q) || it.entityId.contains(q)) }
                        .sortedBy { it.friendlyName.lowercase() }
                        .take(25)
                },
                query = addQuery,
                onQuery = { addQuery = it },
                onAdd = { entity -> vm.updateCuration(securitySectionFor(entity.entityId)) { it.add(entity.entityId) } },
            )
            return@LazyColumn
        }
        if (alarm != null) {
            item(key = "alarm") {
                AlarmChoices(vm, alarm, onNeedsCode = { navigate(Screen.SecurityPanel) })
                Spacer(Modifier.height(20.dp))
            }
        }
        item(key = "cameras-title") {
            SectionTitle("Cameras")
            Spacer(Modifier.height(8.dp))
            if (cameras.isEmpty()) {
                Text(
                    "No cameras in Home Assistant yet. Cameras you add there appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The cameras as a grid of their own pictures; a tap opens the
        // camera, live where it can be.
        for (row in cameras.chunked(columns)) {
            item(key = "cameras-" + row.first().cameraId) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (camera in row) {
                        CameraQuadrant(
                            vm,
                            camera.cameraId,
                            Modifier.weight(1f).aspectRatio(16f / 9f),
                            RoundedCornerShape(Corner.lg),
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item(key = "motion") {
            Spacer(Modifier.height(12.dp))
            SectionTitle("Motion detection")
            Spacer(Modifier.height(8.dp))
            MotionDetection(vm, cameras, loose, detectionIds)
            Spacer(Modifier.height(20.dp))
        }
        item(key = "doors") {
            DoorsAndLocks(states, doorIds, navigate)
        }
        if (states[SIREN_ID] != null) {
            item(key = "siren") {
                Spacer(Modifier.height(20.dp))
                SirenRow(vm, states.getValue(SIREN_ID))
            }
        }
    }
}

/** The opening line, in the calm family when all is shut and the alert one when not. */
@Composable
private fun SummaryCard(line: String, calm: Boolean) {
    val family = (if (calm) StateFamily.Secure else StateFamily.Alert).colors(isDarkBackdrop(MaterialTheme.colorScheme.background))
    Row(
        Modifier.fillMaxWidth()
            .background(family.container, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(family.accent, polygonShape(ExpressiveKind.SUNNY)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (calm) Icons.Filled.Shield else Icons.Filled.GppMaybe, contentDescription = null, tint = family.onAccent)
        }
        Spacer(Modifier.width(16.dp))
        Text(line, style = MaterialTheme.typography.titleLarge, color = family.onContainer)
    }
}

/**
 * Arming as one choice of four. With a panel that wants a code the
 * choices open the alarm's own popup, which has the keypad.
 */
@Composable
private fun AlarmChoices(vm: HavenViewModel, alarm: HaEntity, onNeedsCode: () -> Unit) {
    val needsCode = alarm.attrString("code_format") != null
    val current = alarmChoice(alarm.state)
    SectionTitle("Alarm")
    // The chosen chip says the mode; a line says the state only while
    // the panel is between modes (arming, pending, triggered).
    if (current == null) {
        Spacer(Modifier.height(4.dp))
        Text(alarm.stateText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (mode in ALARM_ARM_MODES + "disarmed") {
            FilterChip(
                selected = current == mode,
                onClick = {
                    if (needsCode) {
                        onNeedsCode()
                    } else {
                        vm.callService("alarm_control_panel", alarmArmService(mode), alarm.entityId)
                    }
                },
                label = { Text(if (mode == "disarmed") "Off" else alarmModeLabel(mode)) },
                colors = choiceChipColors(),
            )
        }
    }
}

/**
 * One row per detection switch, under its camera's name, with when the
 * camera last saw motion where its device reports it. A camera with a
 * motion sensor and no switch still gets a row, so its last motion shows.
 */
@Composable
private fun MotionDetection(vm: HavenViewModel, cameras: List<CameraSecurity>, loose: List<HaEntity>, order: List<String>) {
    val position = order.withIndex().associate { (index, id) -> id to index }
    val keyed = buildList<Pair<String, @Composable () -> Unit>> {
        fun add(key: String, row: @Composable () -> Unit) = add(key to row)
        for (camera in cameras) {
            val motion = camera.motionSensor?.let { sensor ->
                if (sensor.state == "on") "Motion now"
                else formatHaTimestamp(sensor.lastChanged)?.let { "Last motion $it" }
            }
            if (camera.switches.isEmpty()) {
                if (motion != null) add("motion:" + camera.cameraId) { DetectionRow(vm, null, camera.name, motion, camera.cameraId) }
            } else {
                camera.switches.forEachIndexed { index, switch ->
                    val label = detectionLabel(switch, camera.name)
                    val sub = if (index == 0 && motion != null) "$label · $motion" else label
                    add(switch.entityId) { DetectionRow(vm, switch, camera.name, sub, camera.cameraId) }
                }
            }
        }
        for (switch in loose) add(switch.entityId) { DetectionRow(vm, switch, switch.friendlyName, null, null) }
        // Switches added by hand that belong to no camera listed.
        val listed = map { it.first }.toSet()
        for (id in order) if (id !in listed) vm.states.value[id]?.let { e -> add(id) { DetectionRow(vm, e, e.friendlyName, null, null) } }
    }
    // A switch taken off in Edit is not listed; the rest keep the arranged order.
    val rows = keyed
        .filter { (key, _) -> key.startsWith("motion:") || key in position }
        .sortedBy { (key, _) -> position[key] ?: Int.MAX_VALUE }
        .map { it.second }
    if (rows.isEmpty()) {
        Text(
            "No motion switches found. A camera whose device has a motion detection switch in Home Assistant shows it here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    MenuGroup(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) MenuDivider()
            row()
        }
    }
}

@Composable
private fun DetectionRow(vm: HavenViewModel, switch: HaEntity?, title: String, subtitle: String?, cameraId: String?) {
    MenuRow(
        badgeKey = "detect-" + (switch?.entityId ?: cameraId ?: title),
        badgeKind = ExpressiveKind.SUNNY,
        icon = Icons.Filled.Videocam,
        title = title,
        subtitle = subtitle,
        onClick = switch?.let { { vm.toggle(it.entityId) } },
        trailing = switch?.let { s ->
            {
                Switch(
                    checked = s.state == "on",
                    onCheckedChange = { vm.toggle(s.entityId) },
                    enabled = s.state == "on" || s.state == "off",
                )
            }
        },
        flat = true,
    )
}

/** What Doors & locks shows by default: the locks, the garage doors and gates, then the door, window and motion sensors. */
private fun securityDoorIds(states: Map<String, HaEntity>): List<String> {
    val locks = states.values.filter { it.domain == "lock" }.sortedBy { it.friendlyName.lowercase() }
    val garages = states.values
        .filter { it.domain == "cover" && it.attrString("device_class") in setOf("garage", "door", "gate") }
        .sortedBy { it.friendlyName.lowercase() }
    val sensors = securityChecklist(states.values.filter { it.domain == "binary_sensor" })
    return (locks + garages).map { it.entityId } + sensors.map { it.entityId }
}

/** Locks, garage doors and the door, window and motion sensors, in [ids]' order, each opening its own popup. */
@Composable
private fun DoorsAndLocks(states: Map<String, HaEntity>, ids: List<String>, navigate: (Screen) -> Unit) {
    if (ids.isEmpty()) return
    val checks = securityChecklist(states.values.filter { it.domain == "binary_sensor" }).associateBy { it.entityId }
    SectionTitle("Doors & locks")
    Spacer(Modifier.height(8.dp))
    MenuGroup(Modifier.fillMaxWidth()) {
        ids.forEachIndexed { index, id ->
            val entity = states[id] ?: return@forEachIndexed
            if (index > 0) MenuDivider()
            val check = checks[id]
            MenuRow(
                badgeKey = id,
                badgeKind = shapeForDomain(entity.domain),
                icon = domainIcon(entity),
                title = check?.name ?: entity.friendlyName,
                subtitle = check?.statusText ?: entity.stateText(),
                onClick = { navigate(Screen.EntityDetail(id)) },
                flat = true,
            )
        }
    }
}

/** The outdoor siren: a first tap asks, a second sounds it. Stopping is one tap. */
@Composable
private fun SirenRow(vm: HavenViewModel, siren: HaEntity) {
    var confirm by remember { mutableStateOf(false) }
    val on = siren.state == "on"
    SectionTitle("Siren")
    Spacer(Modifier.height(8.dp))
    MenuGroup(Modifier.fillMaxWidth()) {
        MenuRow(
            badgeKey = siren.entityId,
            badgeKind = ExpressiveKind.SUNNY,
            icon = Icons.Filled.NotificationsActive,
            title = siren.friendlyName,
            subtitle = when {
                on -> "Sounding"
                confirm -> "Tap Sound again to sound it"
                else -> "Sounds only after a second tap"
            },
            onClick = null,
            trailing = {
                Row {
                    if (confirm && !on) TextButton(onClick = { confirm = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        when {
                            on -> vm.callService("siren", "turn_off", siren.entityId)
                            confirm -> {
                                confirm = false
                                vm.callService("siren", "turn_on", siren.entityId)
                            }
                            else -> confirm = true
                        }
                    }) { Text(if (on) "Stop" else "Sound") }
                }
            },
            flat = true,
        )
    }
}
