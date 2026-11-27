package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.CARD_CORNER_OPTIONS
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.TILE_PRESETS
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.MenuSection
import com.garrietta.haven.ui.components.SHAPE_OPTIONS
import com.garrietta.haven.ui.components.ShapeOption
import com.garrietta.haven.ui.components.choiceChipColors
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.shapeForKey
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.polygonShape

/*
 * What belongs to Haven alone: how it looks, how each type of device is
 * drawn, Home and the bar, notifications and the connection.
 *
 * A device's look is set for its type here, once for every lamp or every
 * lock, rather than device by device from its popup. A single device can
 * still differ from its type, from its own page under Devices &
 * services, Entities.
 */

@Composable
fun AppearancePage(vm: HavenViewModel, controls: AppearanceControls, onBack: () -> Unit) {
    SettingsPage("Appearance", onBack) {
        item(key = "theme") {
            MenuSection("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((value, label) in listOf("system" to "System", "light" to "Light", "dark" to "Dark")) {
                    FilterChip(
                        colors = choiceChipColors(),
                        selected = controls.themeMode == value,
                        onClick = { controls.onThemeMode(value) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            MenuGroup {
                MenuRow(
                    "appearance-dynamic-color", ExpressiveKind.SUNNY, Icons.Filled.Palette, "Dynamic color",
                    "Follow your wallpaper's colors instead of Haven's sage and lime.",
                    onClick = null,
                    trailing = { Switch(checked = controls.dynamicColor, onCheckedChange = controls.onDynamicColor) },
                    flat = true,
                )
                MenuDivider()
                MenuRow(
                    "appearance-motion", ExpressiveKind.BUN, Icons.Filled.Autorenew, "Reduce motion",
                    "Keep animations instant and minimal",
                    onClick = null,
                    trailing = { Switch(checked = controls.reduceMotion, onCheckedChange = controls.onReduceMotion) },
                    flat = true,
                )
            }
        }
    }
}

/**
 * The tiles taken off Home from their options, each with Show to put it
 * back. A hidden tile cannot be held on Home, so this page is how it
 * returns.
 */
@Composable
fun HiddenFromHomePage(vm: HavenViewModel, onBack: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val tick = vm.cardCustomVersion
    val states by vm.states.collectAsState()
    val nameOf = rememberEntityNamer(vm)
    val hidden = vm.settingsStore.homeLightOverrides().filterValues { !it }.keys
        .mapNotNull { states[it] }
        .sortedBy { nameOf(it).lowercase() }
    SettingsPage("Hidden from Home", onBack, intro = "Tiles you took off Home. They still show on Security and Climate.") {
        if (hidden.isEmpty()) {
            item(key = "empty") { SettingsNote("Nothing is hidden. Press and hold a tile on Home, then turn off Show on Home.") }
        } else {
            groupedRows(hidden, key = { "hidden-" + it.entityId }) { entity ->
                MenuRow(
                    "hidden-${entity.entityId}", shapeForDomain(entity.domain), com.garrietta.haven.ui.controls.domainIcon(entity),
                    nameOf(entity), com.garrietta.haven.data.tileState(entity),
                    onClick = null,
                    trailing = {
                        androidx.compose.material3.TextButton(onClick = {
                            vm.settingsStore.setHomeLightOverride(entity.entityId, true)
                            vm.bumpCardCustom()
                        }) { Text("Show") }
                    },
                    flat = true,
                )
            }
        }
    }
}

@Composable
fun NotificationsPage(vm: HavenViewModel, onBack: () -> Unit) {
    SettingsPage("Notifications", onBack, intro = "Haven watches Home Assistant while it runs and tells this phone about these.") {
        item(key = "rows") {
            MenuGroup {
                MenuRow(
                    "notif-garage", ExpressiveKind.BUN, Icons.Filled.Garage, "Garage & door alerts", "When a garage door or door opens",
                    onClick = null, trailing = { Switch(checked = vm.notifyGarage, onCheckedChange = { vm.notifyGarage = it }) }, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "notif-motion", ExpressiveKind.BUN, Icons.AutoMirrored.Filled.DirectionsRun, "Motion alerts", "When a motion sensor sees motion",
                    onClick = null, trailing = { Switch(checked = vm.notifyMotion, onCheckedChange = { vm.notifyMotion = it }) }, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "notif-lights", ExpressiveKind.BUN, Icons.Filled.Lightbulb, "Automated light alerts",
                    "When a light turns off by an automation or from elsewhere, never for your own taps",
                    onClick = null, trailing = { Switch(checked = vm.notifyLights, onCheckedChange = { vm.notifyLights = it }) }, flat = true,
                )
                MenuDivider()
                NotificationPermissionRow()
            }
        }
    }
}

@Composable
fun ConnectionPage(vm: HavenViewModel, onSignOut: () -> Unit, onBack: () -> Unit) {
    val connection by vm.connection.collectAsState()
    var dialog by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val tick = vm.connectionPrefsVersion
    // Reliability round: keyed on the endpoint actually in use, so the
    // rows follow a fallback / switch-back as it happens.
    val activeUrl by vm.activeUrl.collectAsState()
    val (label, target) = remember(tick, activeUrl) { vm.connectionDisplay() }
    val preferredUrl = remember(tick, activeUrl) { vm.connectionTarget() }
    val onFallback = activeUrl != null && preferredUrl != null && activeUrl != preferredUrl
    SettingsPage("Connection", onBack) {
        item(key = "rows") {
            MenuGroup {
                MenuRow(
                    "connection-instance", ExpressiveKind.CLOUD, Icons.Filled.Wifi, "Instance",
                    target?.let { "$label · $it" } ?: "—",
                    onClick = { dialog = true }, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "connection-status", ExpressiveKind.CLOUD, Icons.Filled.Wifi, "Status",
                    when (val c = connection) {
                        is ConnectionState.Connected -> "Connected · $label" +
                            if (onFallback) " (fallback — switches back when ${com.garrietta.haven.data.connectionLabel(preferredUrl)} answers)" else ""
                        is ConnectionState.Connecting -> "Connecting…"
                        is ConnectionState.Authenticating -> "Signing in…"
                        is ConnectionState.Error -> c.message
                        else -> "Disconnected"
                    },
                    onClick = null, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "connection-signout", ExpressiveKind.CLOUD, Icons.Filled.Logout, "Sign out",
                    "Forget this instance on this phone",
                    onClick = { confirmSignOut = true }, flat = true,
                )
            }
        }
    }
    if (dialog) ConnectionDialog(vm, onDismiss = { dialog = false })
    if (confirmSignOut) {
        ConfirmDialog("Sign out?", "Haven forgets this instance and its sign-in on this phone. Your settings stay.", "Sign out", onConfirm = onSignOut, onDismiss = { confirmSignOut = false })
    }
}

/**
 * App updates: which build is installed, the newest test build, and a
 * button that downloads it and hands it to Android's installer, which
 * asks to confirm. Installing needs the person's leave under Install
 * unknown apps; without it the button opens that page and says so.
 */
@Composable
fun AppUpdatesPage(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var request by remember { mutableStateOf(0) }
    var check by remember { mutableStateOf<com.garrietta.haven.update.UpdateCheck?>(null) }
    androidx.compose.runtime.LaunchedEffect(request) {
        check = null
        check = com.garrietta.haven.update.checkForUpdate(com.garrietta.haven.BuildConfig.GITHUB_REPO, com.garrietta.haven.BuildConfig.BUILD_NUMBER)
    }
    val install by com.garrietta.haven.update.ApkInstaller.state.collectAsState()
    var askedForPermission by remember { mutableStateOf(false) }
    val installed = com.garrietta.haven.BuildConfig.BUILD_NUMBER
    SettingsPage("App updates", onBack, intro = "Haven updates itself from its newest test build, made after every change.") {
        item(key = "installed") {
            MenuGroup {
                MenuRow(
                    "update-installed", ExpressiveKind.SUNNY, Icons.Filled.Autorenew, "Installed",
                    "Haven ${com.garrietta.haven.BuildConfig.VERSION_NAME} · " + if (installed > 0) "build $installed" else "a local build",
                    onClick = null, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "update-check", ExpressiveKind.SUNNY, Icons.Filled.Autorenew, "Check again",
                    when (val c = check) {
                        null -> "Checking…"
                        is com.garrietta.haven.update.UpdateCheck.Available -> "Build ${c.update.build} is ready"
                        is com.garrietta.haven.update.UpdateCheck.Current -> "Up to date with build ${c.update.build}"
                        com.garrietta.haven.update.UpdateCheck.NoBuilds -> "No test builds yet"
                        is com.garrietta.haven.update.UpdateCheck.Failed -> c.message
                    },
                    onClick = { request++ }, flat = true,
                )
            }
        }
        val available = (check as? com.garrietta.haven.update.UpdateCheck.Available)?.update
        if (available != null) {
            item(key = "available") {
                MenuSection(available.title)
                if (available.notes.isNotBlank()) SettingsNote(available.notes)
                val busy = install is com.garrietta.haven.update.ApkInstaller.State.Downloading ||
                    install is com.garrietta.haven.update.ApkInstaller.State.Installing
                androidx.compose.material3.Button(
                    enabled = !busy,
                    onClick = {
                        if (!com.garrietta.haven.update.ApkInstaller.canInstall(context)) {
                            askedForPermission = true
                            com.garrietta.haven.update.ApkInstaller.openInstallPermission(context)
                        } else {
                            askedForPermission = false
                            com.garrietta.haven.update.ApkInstaller.start(context, available.apkUrl)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(
                        when (val s = install) {
                            is com.garrietta.haven.update.ApkInstaller.State.Downloading ->
                                s.fraction?.let { "Downloading ${(it * 100).toInt()}%" } ?: "Downloading…"
                            com.garrietta.haven.update.ApkInstaller.State.Installing -> "Waiting for Android to install it…"
                            else -> "Download and install" +
                                if (available.apkSize > 0) " (${com.garrietta.haven.data.formatBytes(available.apkSize)})" else ""
                        },
                    )
                }
                (install as? com.garrietta.haven.update.ApkInstaller.State.Downloading)?.fraction?.let { fraction ->
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                }
                if (askedForPermission && !busy) {
                    SettingsNote("Let Haven install updates on the page that just opened, then tap Download and install again.")
                }
                (install as? com.garrietta.haven.update.ApkInstaller.State.Failed)?.let { failed ->
                    Text(failed.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        if (com.garrietta.haven.BuildConfig.DEBUG) {
            item(key = "debug-note") {
                SettingsNote("This is a debug build. Test builds are the release app, so one installs beside this one rather than over it.")
            }
        }
    }
}
