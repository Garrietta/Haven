package com.garrietta.haven.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import com.garrietta.haven.ui.components.KodaMenuDivider
import com.garrietta.haven.ui.components.KodaMenuGroup
import com.garrietta.haven.ui.components.KodaMenuRow
import com.garrietta.haven.ui.components.KodaMenuSection
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.pillClearanceBottom
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.DiscoverySource
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.isLive
import com.garrietta.haven.ui.controls.EntityControlCard
import java.util.Calendar

@Composable
fun LoginScreen(vm: HavenViewModel) {
    var url by remember { mutableStateOf(vm.tokenStore.instanceUrl ?: "") }
    var token by remember { mutableStateOf("") }
    // The authorize page renders in-app (see OAuthWebViewScreen): Chrome's stack
    // cannot load this phone's instance, so no browser-based flow is used.
    var oauthUrl by remember { mutableStateOf<String?>(null) }
    if (oauthUrl != null) {
        BackHandler { oauthUrl = null }
        OAuthWebViewScreen(vm, oauthUrl!!) { oauthUrl = null }
        return
    }
    val discovered by vm.discoveredInstances.collectAsState()
    val connecting = vm.connection.collectAsState().value is ConnectionState.Connecting ||
        vm.connection.collectAsState().value is ConnectionState.Authenticating
    // mDNS discovery runs only while the login screen is visible.
    DisposableEffect(Unit) {
        vm.discovery.startMdns()
        onDispose { vm.discovery.stopMdns() }
    }
    Column(
        // The login screen paints the scheme background itself: without
        // this, the window background showed through — white under the
        // Light activity theme — so dark-mode text was light-on-white and
        // unreadable, and the screen "bounced" to dark after sign-in when
        // the Scaffold painted. The DayNight activity theme covers the
        // window; this covers the content, in both modes, with no
        // hardcoded color.
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Haven", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Your home, designed — not a web page in a frame.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Home Assistant URL") },
            placeholder = { Text("https://home.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Find on my network",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        if (discovered.isEmpty()) {
            Text(
                "Looking for Home Assistant on your Wi-Fi…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            for (instance in discovered) {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        url = instance.url
                        vm.preflight(instance.url) {}
                    }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(instance.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(instance.url, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (instance.source == DiscoverySource.MDNS) "Nearby" else "Scan",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.scanNetwork() },
            enabled = !vm.scanning,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (vm.scanning) "Scanning…" else "Scan network") }
        vm.scanMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (url.isNotBlank()) {
                    // Pre-flight first: a dead address shows an in-app explanation
                    // instead of a dead page. On success the authorize page opens
                    // in-app (OAuthWebViewScreen), never in Chrome.
                    vm.preflight(url) { clean ->
                        oauthUrl = vm.startOAuth(clean).toString()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign in with Home Assistant") }
        Spacer(Modifier.height(4.dp))
        Text(
            "Opens the sign-in page inside Haven.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Long-lived access token — no browser needed",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Signs in directly from the app, so it works even when the phone's browser can't reach your instance.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Long-lived access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                if (url.isNotBlank() && token.isNotBlank()) {
                    vm.preflight(url) { clean -> vm.loginWithToken(clean, token.trim()) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Connect with token") }
        if (connecting || vm.probing) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp))
                Text(
                    if (vm.probing) "Checking that address…" else "Connecting…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        vm.probeOk?.let { ok ->
            Spacer(Modifier.height(16.dp))
            Text(ok, color = MaterialTheme.colorScheme.primary)
        }
        vm.authError?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        val conn = vm.connection.collectAsState().value
        if (conn is ConnectionState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(conn.message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Long-lived tokens live in your Home Assistant profile: Profile → Long-Lived Access Tokens → Create Token. Haven encrypts tokens on-device and never sends them anywhere except your instance.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TodayScreen(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    val connection by vm.connection.collectAsState()
    // Read so Home editor saves recompose this screen from the store.
    @Suppress("UNUSED_VARIABLE")
    val homePrefsVersion = vm.homePrefsVersion
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val autoGreeting = when (hour) {
        in 0..4 -> "Up late"
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    // A custom welcome message replaces the greeting verbatim; empty keeps
    // the automatic time-based greeting exactly as before.
    val customWelcome = vm.settingsStore.homeWelcomeMessage
    val greeting = if (customWelcome.isBlank()) autoGreeting else customWelcome
    val hiddenSections = vm.settingsStore.homeHiddenSections
    val showSection: (String) -> Boolean = { it !in hiddenSections }
    val entities = states.values.sortedBy { it.friendlyName }
    val weather = entities.firstOrNull { it.domain == "weather" }
    val homePeople = entities.filter { (it.domain == "person" || it.domain == "device_tracker") && it.state == "home" }
    val nowPlaying = entities.filter { it.domain == "media_player" && it.state == "playing" }
    // Home shows only the lights the user (or the default rule) allows:
    // indicator lights are excluded unless explicitly re-added, and "All
    // off" below therefore touches displayed lights only.
    val lightOverrides = vm.settingsStore.homeLightOverrides()
    val lightsOn = entities.filter {
        it.domain == "light" && it.state == "on" && isLightShownOnHome(it, lightOverrides)
    }
    val alerts = entities.filter {
        it.domain == "binary_sensor" && it.state == "on" &&
            (it.attrString("device_class") in listOf("problem", "safety", "smoke", "co", "moisture", "tamper", "gas"))
    }
    // Sections as LazyListScope blocks so the narrow and wide layouts share
    // them exactly: one column folded, two side-by-side columns unfolded.
    val greetingSection: LazyListScope.() -> Unit = {
        item {
            if (showSection("greeting")) {
                Spacer(Modifier.height(16.dp))
                Text(greeting, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            if (!connection.isLive()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    when (connection) {
                        is ConnectionState.Connecting, ConnectionState.Authenticating -> "Connecting to your home…"
                        is ConnectionState.Error -> (connection as ConnectionState.Error).message
                        else -> "Not connected"
                    },
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    val weatherSection: LazyListScope.() -> Unit = {
        if (showSection("weather") && weather != null) {
            item {
                Text("Weather", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                // Home shows current conditions only — dial, condition,
                // humidity. The card owns its gestures like every other
                // entity: tap opens the full weather page, long-press
                // keeps the sheet. The card's own header click is off
                // so it cannot swallow the outer gestures, and the
                // card still reports its bounds as the morph source.
                Box(
                    Modifier.combinedClickable(
                        onClick = { navigate(Screen.EntityDetail(weather.entityId)) },
                        onLongClick = { vm.openPopup(weather.entityId) },
                    ),
                ) {
                    EntityControlCard(weather, vm, showForecast = false, headerClickable = false) { vm.openPopup(it) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    val homeSection: LazyListScope.() -> Unit = {
        if (showSection("home") && homePeople.isNotEmpty()) {
            item {
                Text("Who's home", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(homePeople.joinToString(", ") { it.friendlyName }, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    val playingSection: LazyListScope.() -> Unit = {
        if (showSection("playing") && nowPlaying.isNotEmpty()) {
            item {
                Text("Now playing", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            items(nowPlaying) { player ->
                EntityControlCard(player, vm) { vm.openPopup(it) }
                Spacer(Modifier.height(12.dp))
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
    val lightsSection: LazyListScope.() -> Unit = {
        if (showSection("lights") && lightsOn.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${lightsOn.size} light${if (lightsOn.size == 1) "" else "s"} on",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = {
                        for (light in lightsOn) vm.turnOff(light.entityId)
                    }) { Text("All off") }
                }
                Spacer(Modifier.height(8.dp))
            }
            items(lightsOn.take(4)) { light ->
                EntityControlCard(light, vm) { vm.openPopup(it) }
                Spacer(Modifier.height(12.dp))
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
    val alertsSection: LazyListScope.() -> Unit = {
        if (showSection("alerts") && alerts.isNotEmpty()) {
            item {
                Text("Needs attention", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            items(alerts) { alert ->
                EntityControlCard(alert, vm) { vm.openPopup(it) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (LocalWideLayout.current) {
        Row(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            LazyColumn(
                Modifier.weight(1f).padding(end = 8.dp),
                contentPadding = PaddingValues(bottom = pillClearanceBottom()),
            ) {
                greetingSection()
                weatherSection()
                homeSection()
                playingSection()
                item { Spacer(Modifier.height(16.dp)) }
            }
            LazyColumn(
                Modifier.weight(1f).padding(start = 8.dp),
                contentPadding = PaddingValues(bottom = pillClearanceBottom()),
            ) {
                lightsSection()
                alertsSection()
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = pillClearanceBottom()),
        ) {
            greetingSection()
            weatherSection()
            homeSection()
            playingSection()
            lightsSection()
            alertsSection()
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SettingsScreen(
    vm: HavenViewModel,
    themeMode: String,
    reduceMotion: Boolean,
    onThemeMode: (String) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    navigate: (Screen) -> Unit,
) {
    val connection by vm.connection.collectAsState()
    // Koda Settings menu DNA: dark rounded rows with shaped icon badges,
    // ALL-CAPS muted section headers, and bold title + muted subtitle
    // rows with chevrons — on Haven's dynamic colors, not Koda's palette.
    // The explanatory texts the old screen showed under each heading now
    // live as the rows' subtitles, so nothing is lost in the restyle.
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
    ) {
        item {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            KodaMenuSection("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((value, label) in listOf("system" to "System", "light" to "Light", "dark" to "Dark")) {
                    FilterChip(
                        selected = themeMode == value,
                        onClick = { onThemeMode(value) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "appearance-motion",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Autorenew,
                    title = "Reduce motion",
                    subtitle = "Keep animations instant and minimal",
                    onClick = null,
                    trailing = { Switch(checked = reduceMotion, onCheckedChange = onReduceMotion) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Connection")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "connection-instance",
                    badgeKind = ExpressiveKind.CLOUD,
                    icon = Icons.Filled.Wifi,
                    title = "Instance",
                    subtitle = vm.tokenStore.instanceUrl ?: "—",
                    onClick = null,
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "connection-status",
                    badgeKind = ExpressiveKind.CLOUD,
                    icon = Icons.Filled.Wifi,
                    title = "Status",
                    subtitle = when (connection) {
                        is ConnectionState.Connected -> "Connected"
                        is ConnectionState.Connecting -> "Connecting…"
                        is ConnectionState.Authenticating -> "Signing in…"
                        is ConnectionState.Error -> (connection as ConnectionState.Error).message
                        else -> "Disconnected"
                    },
                    onClick = null,
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Tabs")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "tabs-manage",
                    badgeKind = ExpressiveKind.BURST,
                    icon = Icons.Filled.List,
                    title = "Manage tabs",
                    subtitle = "The pill bar is your tabs: Home is always first, Settings is always last, and the rest are yours to create, rename, and reorder.",
                    onClick = { navigate(Screen.TabManager) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Home page")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "home-manage",
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.Home,
                    title = "Manage home page",
                    subtitle = "Choose your welcome message, which sections Home shows, and which lights count as Home lights. Indicator lights are hidden from Home by default and can be added back here.",
                    onClick = { navigate(Screen.HomeEditor) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Browse")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "browse-boards",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Dashboard,
                    title = "Boards",
                    subtitle = "Your Lovelace dashboards",
                    onClick = { navigate(Screen.Dashboards) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "browse-areas",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.GridView,
                    title = "Areas",
                    subtitle = "Rooms and their entities",
                    onClick = { navigate(Screen.Areas) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "browse-search",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Search,
                    title = "Search",
                    subtitle = "Find any entity",
                    onClick = { navigate(Screen.Search) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Create")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "create-automation",
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Autorenew,
                    title = "Automation",
                    subtitle = "Guided builder: pick a trigger, a condition, and an action — no YAML, no web UI.",
                    onClick = { navigate(Screen.AutomationBuilder) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "create-scene",
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Palette,
                    title = "Scene",
                    subtitle = "Capture the current state of your devices",
                    onClick = { navigate(Screen.SceneBuilder) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "create-script",
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Code,
                    title = "Script",
                    subtitle = "A reusable sequence of actions",
                    onClick = { navigate(Screen.ScriptBuilder) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "create-helper",
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Build,
                    title = "Helper",
                    subtitle = "Input booleans, numbers, timers, and more",
                    onClick = { navigate(Screen.HelperBuilder) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            Spacer(Modifier.height(16.dp))
            Text(
                "Haven v1.0 (debug). Native client — no WebView. Energy view, admin panels, widgets, Wear/Auto, and voice satellite are later rounds.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
