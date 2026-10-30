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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Videocam
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.groupEntitiesByRoom
import com.garrietta.haven.ui.components.shapeLabel
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
    // Home shows only the lights the user (or the default rule) allows:
    // indicator lights are excluded unless explicitly re-added.
    val lightOverrides = vm.settingsStore.homeLightOverrides()
    // Rooms (2026-10-30): the Home page is organized by room —
    // one section per HA area, in HA's order, each room's devices
    // under it. Areas come from the HA area registry on load
    // (not a Haven-only list); the flat playing/lights/alerts
    // sections are gone — their entities live in their rooms.
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val roomEntities = entities.filter {
        it.domain != "camera" && it.domain != "weather" &&
            it.domain != "person" && it.domain != "device_tracker"
    }
    val grouped = groupEntitiesByRoom(roomEntities, entityAreas)
    fun roomMembers(areaId: String?): List<HaEntity> =
        (grouped[areaId] ?: emptyList())
            .filter { it.domain != "light" || isLightShownOnHome(it, lightOverrides) }
            .sortedBy { it.friendlyName }

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
    val roomsSections: LazyListScope.() -> Unit = {
        for (area in areas) {
            if ("room:${area.id}" in hiddenSections) continue
            val members = roomMembers(area.id)
            if (members.isEmpty()) continue
            item {
                Text(area.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            items(members) { member ->
                EntityControlCard(member, vm) { vm.openPopup(it) }
                Spacer(Modifier.height(12.dp))
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        val unassigned = roomMembers(null)
        if (unassigned.isNotEmpty()) {
            item {
                Text("No room", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            items(unassigned) { member ->
                EntityControlCard(member, vm) { vm.openPopup(it) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (LocalWideLayout.current) {
        // Expanded windows: cap the two-pane content at 1200dp and
        // center it, so tablet/desktop panes don't stretch to
        // full-window width. The cap never binds below 1200dp, so
        // Medium (Fold8 unfolded) renders exactly as before.
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 1200.dp).fillMaxHeight().padding(horizontal = 16.dp)) {
                if (showSection("cameras")) {
                    CameraQuadrantHero(vm, navigate, states)
                }
                Row(Modifier.weight(1f)) {
                    LazyColumn(
                        Modifier.weight(1f).padding(end = 8.dp),
                        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
                    ) {
                        greetingSection()
                        weatherSection()
                        homeSection()
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                    LazyColumn(
                        Modifier.weight(1f).padding(start = 8.dp),
                        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
                    ) {
                        roomsSections()
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = pillClearanceBottom()),
        ) {
            if (showSection("cameras")) item { CameraQuadrantHero(vm, navigate, states) }
            greetingSection()
            weatherSection()
            homeSection()
            roomsSections()
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
            KodaMenuSection("Rooms")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "rooms-manage",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.GridView,
                    title = "Rooms & areas",
                    subtitle = "Create, rename, and delete rooms, and assign devices to them — saved to Home Assistant itself.",
                    onClick = { navigate(Screen.RoomManager) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Shapes")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "shapes-badge",
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.Palette,
                    title = "Badge shape",
                    subtitle = shapeLabel(vm.settingsStore.globalBadgeShape) ?: "Domain default",
                    onClick = { vm.openShapePicker(null, false) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "shapes-card",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.GridView,
                    title = "Card shape",
                    subtitle = shapeLabel(vm.settingsStore.globalCardShape) ?: "Default corners",
                    onClick = { vm.openShapePicker(null, true) },
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

/**
 * Camera quadrant hero (2026-10-30): the very top of Home. ONE
 * rectangular expressive shape (24dp corners — rectangular in
 * spirit; a scallop would cut the quadrant images) divided into
 * four landscape quadrants (two top, two bottom), one per curated
 * camera (Garage, Outdoor, Living Room, Ring Front Door — the
 * Cameras tab set). Each quadrant shows purely the MOST RECENT
 * SNAPSHOT, refreshed silently in the background — the hero never
 * blocks on a live stream. Tapping a quadrant opens the centered
 * live-feed dialog for that camera (the #77 dialog model; Ring
 * uses the #65 proxy path). An empty slot shows a camera icon.
 */
@Composable
fun CameraQuadrantHero(vm: HavenViewModel, navigate: (Screen) -> Unit, states: Map<String, HaEntity>) {
    val cameras = vm.heroCameras(states)
    val camerasTab = vm.userTabs.firstOrNull { it.title == "Cameras" }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cameras", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            if (camerasTab != null) {
                TextButton(onClick = { navigate(Screen.UserTab(camerasTab.id)) }) { Text("See all") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    CameraQuadrant(vm, cameras.getOrNull(0), Modifier.weight(1f))
                    CameraQuadrant(vm, cameras.getOrNull(1), Modifier.weight(1f))
                }
                Row(Modifier.weight(1f)) {
                    CameraQuadrant(vm, cameras.getOrNull(2), Modifier.weight(1f))
                    CameraQuadrant(vm, cameras.getOrNull(3), Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CameraQuadrant(vm: HavenViewModel, cameraId: String?, modifier: Modifier = Modifier) {
    val states by vm.states.collectAsState()
    Box(
        modifier.padding(1.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (cameraId != null) Modifier.clickable { vm.openCameraDialog(cameraId) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraId == null) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
        } else {
            val snap = vm.snapshotFor(cameraId)
            LaunchedEffect(cameraId) { vm.refreshSnapshot(cameraId) }
            val entity = states[cameraId]
            if (snap != null) {
                Image(snap.asImageBitmap(), contentDescription = entity?.friendlyName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/**
 * Rooms & areas manager (2026-10-30): Apple-Home-style room
 * management that carries over to the actual HA instance. Areas
 * are read from HA's area registry on load; every mutation here
 * calls the real registry WebSocket APIs (area create/update/
 * delete, entity area assign/clear) and re-reads the registries
 * on success, so changes appear in HA itself. Errors (duplicate
 * names, a delete HA rejects) surface as a message, never a
 * crash. Device-level assignment is not exposed: Haven's model
 * is entities, and entity assignment covers every device Haven
 * shows.
 */
@Composable
fun RoomManagerScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val states by vm.states.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var deleteArmed by remember { mutableStateOf<String?>(null) }
    var addFor by remember { mutableStateOf<String?>(null) }
    var assignFor by remember { mutableStateOf<String?>(null) }
    fun done(err: String?, ok: String) {
        if (err != null) { message = err; messageIsError = true }
        else { message = ok; messageIsError = false }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Rooms & areas", onBack)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = pillClearanceBottom()),
        ) {
            item {
                Text(
                    "Rooms live in Home Assistant — changes here appear in HA itself.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    vm.refreshAreas()
                    message = "Refreshing rooms from Home Assistant…"
                    messageIsError = false
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh from Home Assistant")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("New room name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val name = newRoomName.trim()
                        if (name.isEmpty()) return@Button
                        vm.createRoom(name) { err ->
                            done(err, "Room \"$name\" created")
                            if (err == null) newRoomName = ""
                        }
                    }) { Text("Add room") }
                }
                Spacer(Modifier.height(12.dp))
                if (message != null) {
                    Text(
                        message!!,
                        fontSize = 13.sp,
                        color = if (messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(areas) { area ->
                val members = entityAreas.filter { it.value == area.id }.keys
                    .mapNotNull { states[it] }
                    .sortedBy { it.friendlyName }
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(area.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("${members.size} devices", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = renameText[area.id] ?: area.name,
                            onValueChange = { renameText = renameText + (area.id to it) },
                            label = { Text("Rename") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val name = (renameText[area.id] ?: area.name).trim()
                            if (name.isEmpty() || name == area.name) return@Button
                            vm.renameRoom(area.id, name) { err -> done(err, "Room renamed to \"$name\"") }
                        }) { Text("Rename") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            if (deleteArmed == area.id) {
                                deleteArmed = null
                                vm.deleteRoom(area.id) { err -> done(err, "Room \"${area.name}\" deleted") }
                            } else {
                                deleteArmed = area.id
                                message = "Tap Delete again to confirm"
                                messageIsError = false
                            }
                        }) { Text(if (deleteArmed == area.id) "Confirm delete" else "Delete") }
                    }
                    Spacer(Modifier.height(8.dp))
                    members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(member.friendlyName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = {
                                vm.assignEntityToRoom(member.entityId, null) { err ->
                                    done(err, "${member.friendlyName} moved to No room")
                                }
                            }) { Text("Remove") }
                        }
                    }
                    Row {
                        OutlinedButton(onClick = { addFor = if (addFor == area.id) null else area.id }) {
                            Text(if (addFor == area.id) "Close" else "Add entity…")
                        }
                    }
                    if (addFor == area.id) {
                        val candidates = states.values
                            .filter { entityAreas[it.entityId] != area.id }
                            .sortedBy { it.friendlyName }
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { addFor = null },
                        ) {
                            candidates.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.friendlyName) },
                                    onClick = {
                                        addFor = null
                                        vm.assignEntityToRoom(candidate.entityId, area.id) { err ->
                                            done(err, "${candidate.friendlyName} moved to ${area.name}")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                val unassigned = states.values
                    .filter { entityAreas[it.entityId] == null }
                    .sortedBy { it.friendlyName }
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Text("No room", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${unassigned.size} devices", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    unassigned.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(member.friendlyName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { assignFor = if (assignFor == member.entityId) null else member.entityId }) {
                                Text("Assign…")
                            }
                        }
                        if (assignFor == member.entityId) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { assignFor = null },
                            ) {
                                areas.forEach { area ->
                                    DropdownMenuItem(
                                        text = { Text(area.name) },
                                        onClick = {
                                            assignFor = null
                                            vm.assignEntityToRoom(member.entityId, area.id) { err ->
                                                done(err, "${member.friendlyName} moved to ${area.name}")
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
