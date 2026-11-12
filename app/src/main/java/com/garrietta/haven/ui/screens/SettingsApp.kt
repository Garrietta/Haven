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
            MenuSection("Badges")
            MenuGroup {
                MenuRow(
                    "badges-nav", ExpressiveKind.FLOWER, Icons.Filled.Visibility, "On the Home tab",
                    "A dot when a lock is unlocked, a door or window is open, or the alarm is triggered.",
                    onClick = null,
                    trailing = { Switch(checked = vm.badgeNav, onCheckedChange = { vm.badgeNav = it }) },
                    flat = true,
                )
                MenuDivider()
                MenuRow(
                    "badges-room", ExpressiveKind.WAVE, Icons.Filled.Home, "On rooms",
                    "A dot on a room when something in it is unlocked, open or triggered.",
                    onClick = null,
                    trailing = { Switch(checked = vm.badgeRoom, onCheckedChange = { vm.badgeRoom = it }) },
                    flat = true,
                )
            }
        }
    }
}

/** The family a type of device is drawn in when it is on and has no color of its own. */
internal fun typeFamily(domain: String): StateFamily = when (domain) {
    "light" -> StateFamily.Light
    "switch", "input_boolean", "fan", "vacuum" -> StateFamily.On
    "climate" -> StateFamily.Heat
    "lock", "cover", "alarm_control_panel" -> StateFamily.Secure
    "media_player" -> StateFamily.Media
    "weather" -> StateFamily.Cool
    else -> StateFamily.Quiet
}

/** "All devices" is the global default; the rest are one entity type each. */
private const val ALL_TYPES = "all"

/** A small card in a type's color, with its badge in its badge shape. */
@Composable
private fun StylePreview(card: Shape, badge: Shape, fill: Color, badgeFill: Color, size: Int = 56) {
    Box(
        Modifier.size(width = (size * 1.25f).dp, height = size.dp).background(fill, card),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(Modifier.padding((size / 7).dp).size((size / 2.5f).dp).background(badgeFill, badge))
    }
}

/** What a type's preview looks like with the stored choices. */
private data class TypeStyle(val card: Shape, val badge: Shape, val family: StateFamily, val tileLabel: String?, val badgeKey: String?, val cardKey: String?)

private fun typeStyle(vm: HavenViewModel, domain: String): TypeStyle {
    val store = vm.settingsStore
    val badgeKey = if (domain == ALL_TYPES) store.globalBadgeShape else store.domainBadgeShapes()[domain]
    val cardKey = if (domain == ALL_TYPES) store.globalCardShape else store.domainCardShapes()[domain]
    val tile = if (domain == ALL_TYPES) null else store.domainTilePresets()[domain]?.let { key -> TILE_PRESETS.firstOrNull { it.key == key } }
    val badgeShape = badgeKey?.let { shapeForKey(it) }
        ?: store.globalBadgeShape?.let { shapeForKey(it) }
        ?: polygonShape(if (domain == ALL_TYPES) ExpressiveKind.SUNNY else shapeForDomain(domain))
    val cardShape = cardKey?.let { shapeForKey(it) } ?: store.globalCardShape?.let { shapeForKey(it) } ?: generalCardShape(0)
    return TypeStyle(cardShape, badgeShape, tile?.family ?: typeFamily(if (domain == ALL_TYPES) "switch" else domain), tile?.label, badgeKey, cardKey)
}

@Composable
fun DeviceStylesPage(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val shapeTick = vm.shapePrefsVersion
    @Suppress("UNUSED_VARIABLE") val cardTick = vm.cardCustomVersion
    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val types = listOf(ALL_TYPES to "All devices") + DOMAIN_DEFAULT_OPTIONS
    SettingsPage(
        "Device styles",
        onBack,
        intro = "How each type of device looks on Home and in its popup: its color, its badge and its card. " +
            "A type you leave alone follows All devices. One device can still differ, from its page under Devices & services.",
    ) {
        groupedRows(types, key = { "style-" + it.first }) { (domain, label) ->
            val style = typeStyle(vm, domain)
            val family = style.family.colors(isDark)
            Row(
                Modifier.fillMaxWidth().clickable { navigate(Screen.SettingsPage(SettingsRoute.style(domain))) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StylePreview(style.card, style.badge, family.container, family.accent, size = 44)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        listOfNotNull(
                            style.tileLabel,
                            shapeLabel(style.badgeKey)?.let { "$it badge" },
                            shapeLabel(style.cardKey),
                        ).joinToString(" · ").ifEmpty { if (domain == ALL_TYPES) "Haven's shapes" else "Follows All devices" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One type's style: a large preview, then its color, badge shape and card shape. */
@Composable
fun DeviceStylePage(vm: HavenViewModel, domain: String, onBack: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val shapeTick = vm.shapePrefsVersion
    @Suppress("UNUSED_VARIABLE") val cardTick = vm.cardCustomVersion
    val store = vm.settingsStore
    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val label = if (domain == ALL_TYPES) "All devices" else DOMAIN_DEFAULT_OPTIONS.firstOrNull { it.first == domain }?.second ?: domain
    val style = typeStyle(vm, domain)
    val tileKey = if (domain == ALL_TYPES) null else store.domainTilePresets()[domain]
    fun setBadge(key: String?) {
        if (domain == ALL_TYPES) store.globalBadgeShape = key else store.setDomainBadgeShape(domain, key)
        vm.bumpShapePrefs()
    }
    fun setCard(key: String?) {
        if (domain == ALL_TYPES) store.globalCardShape = key else store.setDomainCardShape(domain, key)
        vm.bumpShapePrefs()
    }

    SettingsPage(label, onBack) {
        item(key = "preview") {
            val family = style.family.colors(isDark)
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                StylePreview(style.card, style.badge, family.container, family.accent, size = 120)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (domain != ALL_TYPES) {
            item(key = "color") {
                MenuSection("Color when on")
                OptionGrid(
                    options = listOf<String?>(null) + TILE_PRESETS.map { it.key },
                    selected = tileKey,
                    columns = 4,
                    label = { key -> TILE_PRESETS.firstOrNull { it.key == key }?.label ?: "Automatic" },
                    onPick = { key ->
                        store.setDomainTilePreset(domain, key)
                        vm.bumpCardCustom()
                    },
                ) { key, selected ->
                    val fill = key?.let { k -> TILE_PRESETS.first { it.key == k }.family.colors(isDark).container }
                        ?: typeFamily(domain).colors(isDark).container
                    Box(
                        Modifier.size(44.dp).background(fill, CircleShape)
                            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                    )
                }
            }
        }
        item(key = "badge") {
            MenuSection("Badge shape")
            ShapeOptionGrid(SHAPE_OPTIONS, style.badgeKey, if (domain == ALL_TYPES) "Haven's" else "As All devices", isDark, ::setBadge)
        }
        item(key = "card") {
            MenuSection("Card shape")
            ShapeOptionGrid(
                CARD_CORNER_OPTIONS.map { ShapeOption(it.key, it.label) } + SHAPE_OPTIONS,
                style.cardKey,
                if (domain == ALL_TYPES) "Haven's leaf card" else "As All devices",
                isDark,
                ::setCard,
            )
        }
    }
}

/** A grid of choices, "none" (null) first, each drawn by [swatch] over its label. */
@Composable
private fun OptionGrid(
    options: List<String?>,
    selected: String?,
    columns: Int,
    label: (String?) -> String,
    onPick: (String?) -> Unit,
    swatch: @Composable (String?, Boolean) -> Unit,
) {
    Column {
        for (row in options.chunked(columns)) {
            Row(Modifier.fillMaxWidth()) {
                for (key in row) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onPick(key) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        swatch(key, key == selected)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label(key),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (key == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ShapeOptionGrid(options: List<ShapeOption>, selected: String?, noneLabel: String, isDark: Boolean, onPick: (String?) -> Unit) {
    val fill = StateFamily.On.colors(isDark).accent
    OptionGrid(
        options = listOf<String?>(null) + options.map { it.key },
        selected = selected,
        columns = 4,
        label = { key -> if (key == null) noneLabel else shapeLabel(key) ?: key },
        onPick = onPick,
    ) { key, isSelected ->
        Box(
            Modifier.size(48.dp)
                .border(if (isSelected) 3.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                .padding(4.dp)
                .background(if (key == null) MaterialTheme.colorScheme.surfaceContainerHighest else fill, key?.let { shapeForKey(it) } ?: RoundedCornerShape(12.dp)),
        )
    }
}

@Composable
fun HomeNavigationPage(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    SettingsPage("Home & navigation", onBack) {
        item(key = "home") {
            MenuGroup {
                MenuRow(
                    "home-page", ExpressiveKind.SUNNY, Icons.Filled.SpaceDashboard, "Home page",
                    "Welcome message, which sections show and in what order, favorites and lights",
                    onClick = { navigate(Screen.HomeEditor) }, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "home-tabs", ExpressiveKind.SUNNY, Icons.Filled.ViewQuilt, "Your tabs",
                    "${vm.userTabs.size} tab${if (vm.userTabs.size == 1) "" else "s"}: their order, cards and where they show",
                    onClick = { navigate(Screen.SettingsPage(SettingsRoute.TABS)) }, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "home-rooms", ExpressiveKind.SUNNY, Icons.Filled.Map, "Rooms",
                    "Order, names in Haven, and where each shows: under Areas",
                    onClick = { navigate(Screen.SettingsPage(SettingsRoute.areas("areas"))) }, flat = true,
                )
                MenuDivider()
                MenuRow(
                    "home-boards", ExpressiveKind.SUNNY, Icons.Filled.GridView, "Dashboards",
                    "Home Assistant's dashboards, drawn natively",
                    onClick = { navigate(Screen.Dashboards) }, flat = true,
                )
            }
        }
        item(key = "bar") {
            MenuSection("Navigation bar")
            NavigationSettings(vm)
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
    val (label, target) = remember(tick) { vm.connectionDisplay() }
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
                        is ConnectionState.Connected -> "Connected"
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
