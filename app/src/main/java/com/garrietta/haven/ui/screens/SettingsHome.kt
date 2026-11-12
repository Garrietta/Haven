package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.updatesSummary
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.MenuSection
import com.garrietta.haven.ui.components.ScreenTitle
import com.garrietta.haven.ui.theme.ExpressiveKind

/*
 * Settings, laid out the way Home Assistant lays out its own: Devices &
 * services, Automations & scenes, Areas, labels & zones, Dashboards,
 * People, System and About, each a page that reads and writes Home
 * Assistant itself. Under them, what belongs to Haven alone:
 * appearance, how each type of device looks, Home and the bar,
 * notifications and the connection.
 *
 * Each thing has one place. Rooms are Home Assistant areas, so they are
 * kept under Areas, with Haven's own choices for a room (its name and
 * icon in Haven, where it shows) beside Home Assistant's. Tabs are
 * Haven's, under Home & navigation.
 */

/** Where each settings page lives, as the route a [Screen.SettingsPage] carries. */
object SettingsRoute {
    const val DEVICES = "devices"
    const val ADD_INTEGRATION = "integration/add"
    const val AUTOMATIONS = "automations"
    const val AREAS = "areas"
    const val PEOPLE = "people"
    const val SYSTEM = "system"
    const val REPAIRS = "system/repairs"
    const val LOGS = "system/logs"
    const val BACKUPS = "system/backups"
    const val GENERAL = "system/general"
    const val ABOUT = "about"
    const val APPEARANCE = "appearance"
    const val STYLES = "styles"
    const val HOME = "home"
    const val TABS = "tabs"
    const val NOTIFICATIONS = "notifications"
    const val CONNECTION = "connection"

    fun devices(tab: String) = "$DEVICES/$tab"
    fun automations(tab: String) = "$AUTOMATIONS/$tab"
    fun areas(tab: String) = "$AREAS/$tab"
    fun device(id: String) = "device/$id"
    fun entity(id: String) = "entity/$id"
    fun style(domain: String) = "style/$domain"
}

/** The app's own appearance choices, which HavenApp owns and passes down. */
data class AppearanceControls(
    val themeMode: String,
    val reduceMotion: Boolean,
    val onThemeMode: (String) -> Unit,
    val onReduceMotion: (Boolean) -> Unit,
    val dynamicColor: Boolean,
    val onDynamicColor: (Boolean) -> Unit,
    val onSignOut: () -> Unit,
)

@Composable
fun SettingsScreen(
    vm: HavenViewModel,
    themeMode: String,
    reduceMotion: Boolean,
    onThemeMode: (String) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    dynamicColor: Boolean,
    onDynamicColor: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    navigate: (Screen) -> Unit,
) {
    val states by vm.states.collectAsState()
    val connection by vm.connection.collectAsState()
    val updates = updatesSummary(states)
    val core = rememberLoad(connection is ConnectionState.Connected) { vm.admin.coreConfig() }
    val repairs = rememberLoad(connection is ConnectionState.Connected) { vm.admin.repairs() }
    val openRepairs = (repairs.state as? Loaded.Ready)?.value?.count { !it.ignored } ?: 0
    fun go(route: String) = navigate(Screen.SettingsPage(route))

    val homeAssistant: @Composable () -> Unit = {
        MenuSection("Home Assistant")
        MenuGroup {
            HubRow("ha-devices", Icons.Filled.Devices, "Devices & services", "Integrations, devices, entities and helpers") { go(SettingsRoute.devices("integrations")) }
            MenuDivider()
            HubRow("ha-automations", Icons.Filled.AutoAwesome, "Automations & scenes", "Automations, scenes and scripts") { go(SettingsRoute.automations("automations")) }
            MenuDivider()
            HubRow("ha-areas", Icons.Filled.Map, "Areas, labels & zones", "Rooms, floors, labels and zones") { go(SettingsRoute.areas("areas")) }
            MenuDivider()
            HubRow("ha-dashboards", Icons.Filled.Dashboard, "Dashboards", "Your Home Assistant dashboards") { navigate(Screen.Dashboards) }
            MenuDivider()
            HubRow("ha-people", Icons.Filled.People, "People", "People, their trackers, and users") { go(SettingsRoute.PEOPLE) }
            MenuDivider()
            val systemLine = listOfNotNull(
                if (updates.available > 0) "${updates.available} update${if (updates.available == 1) "" else "s"}" else null,
                if (openRepairs > 0) "$openRepairs repair${if (openRepairs == 1) "" else "s"}" else null,
            ).joinToString(" · ").ifEmpty { "Updates, repairs, logs, backups and restart" }
            HubRow("ha-system", Icons.Filled.Settings, "System", systemLine) { go(SettingsRoute.SYSTEM) }
            MenuDivider()
            val version = (core.state as? Loaded.Ready)?.value?.version
            HubRow("ha-about", Icons.Filled.Info, "About", version?.let { "Home Assistant $it" } ?: "Versions") { go(SettingsRoute.ABOUT) }
        }
    }
    val haven: @Composable () -> Unit = {
        MenuSection("Haven")
        MenuGroup {
            HubRow("app-appearance", Icons.Filled.Palette, "Appearance", "Light or dark, color, motion and badges") { go(SettingsRoute.APPEARANCE) }
            MenuDivider()
            HubRow("app-styles", Icons.Filled.Category, "Device styles", "Color and shapes for each type of device") { go(SettingsRoute.STYLES) }
            MenuDivider()
            HubRow("app-home", Icons.Filled.SpaceDashboard, "Home & navigation", "Home page, the bar, and your tabs") { go(SettingsRoute.HOME) }
            MenuDivider()
            HubRow("app-notifications", Icons.Filled.Notifications, "Notifications", "Alerts Haven sends to this phone") { go(SettingsRoute.NOTIFICATIONS) }
            MenuDivider()
            val tick = vm.connectionPrefsVersion
            val (label, target) = remember(tick) { vm.connectionDisplay() }
            val status = when (connection) {
                is ConnectionState.Connected -> "Connected"
                is ConnectionState.Connecting -> "Connecting"
                is ConnectionState.Authenticating -> "Signing in"
                is ConnectionState.Error -> "Not connected"
                else -> "Disconnected"
            }
            HubRow("app-connection", Icons.Filled.Wifi, "Connection", target?.let { "$status · $label" } ?: status) { go(SettingsRoute.CONNECTION) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            ScreenTitle("Settings", Modifier.padding(top = 16.dp))
            Spacer(Modifier.height(4.dp))
            if (LocalWideLayout.current) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { homeAssistant() }
                    Column(Modifier.weight(1f)) { haven() }
                }
            } else {
                homeAssistant()
                Spacer(Modifier.height(8.dp))
                haven()
            }
        }
    }
}

@Composable
private fun HubRow(key: String, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    MenuRow(
        badgeKey = key,
        badgeKind = ExpressiveKind.SUNNY,
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        flat = true,
    )
}

/** Draws the settings page a route names. */
@Composable
fun SettingsPageHost(
    vm: HavenViewModel,
    route: String,
    onBack: () -> Unit,
    navigate: (Screen) -> Unit,
    appearance: AppearanceControls,
) {
    val head = route.substringBefore('/')
    val rest = route.substringAfter('/', "")
    when {
        route == SettingsRoute.ADD_INTEGRATION -> AddIntegrationPage(vm, onBack)
        head == SettingsRoute.DEVICES -> DevicesServicesPage(vm, rest.ifEmpty { "integrations" }, onBack, navigate)
        head == "device" -> DeviceSettingsPage(vm, rest, onBack, navigate)
        head == "entity" -> EntitySettingsPage(vm, rest, onBack, navigate)
        head == SettingsRoute.AUTOMATIONS -> AutomationsScenesPage(vm, rest.ifEmpty { "automations" }, onBack, navigate)
        head == SettingsRoute.AREAS -> AreasLabelsZonesPage(vm, rest.ifEmpty { "areas" }, onBack, navigate)
        route == SettingsRoute.PEOPLE -> PeoplePage(vm, onBack)
        route == SettingsRoute.SYSTEM -> SystemPage(vm, onBack, navigate)
        route == SettingsRoute.REPAIRS -> RepairsPage(vm, onBack)
        route == SettingsRoute.LOGS -> LogsPage(vm, onBack)
        route == SettingsRoute.BACKUPS -> BackupsPage(vm, onBack)
        route == SettingsRoute.GENERAL -> GeneralPage(vm, onBack)
        route == SettingsRoute.ABOUT -> AboutPage(vm, onBack)
        route == SettingsRoute.APPEARANCE -> AppearancePage(vm, appearance, onBack)
        route == SettingsRoute.STYLES -> DeviceStylesPage(vm, onBack, navigate)
        head == "style" -> DeviceStylePage(vm, rest, onBack)
        route == SettingsRoute.HOME -> HomeNavigationPage(vm, onBack, navigate)
        route == SettingsRoute.TABS -> CustomTabsPage(vm, onBack, navigate)
        route == SettingsRoute.NOTIFICATIONS -> NotificationsPage(vm, onBack)
        route == SettingsRoute.CONNECTION -> ConnectionPage(vm, appearance.onSignOut, onBack)
        else -> SettingsPage("Settings", onBack) { item { SettingsNote("This page has moved.") } }
    }
}

/** A state holder so a page can open one dialog at a time. */
@Composable
fun <T> rememberDialog() = remember { mutableStateOf<T?>(null) }
