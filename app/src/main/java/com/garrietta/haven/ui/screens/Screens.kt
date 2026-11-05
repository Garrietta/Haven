package com.garrietta.haven.ui.screens

import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Garage
import com.garrietta.haven.ui.components.NavStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.layout.RowScope
import android.provider.Settings
import android.os.Build
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import com.garrietta.haven.data.HOME_SECTION_ORDER_DEFAULT
import com.garrietta.haven.data.ConnectionMode
import com.garrietta.haven.data.alertDotColor
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.ui.components.PillSwitch
import com.garrietta.haven.data.domainSwitchColors
import com.garrietta.haven.data.hasImportantAlert
import com.garrietta.haven.data.migrateRound3Hidden
import com.garrietta.haven.data.migrateRound3SectionOrder
import com.garrietta.haven.data.resolveOrder
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.data.customIconKeyFor
import com.garrietta.haven.data.inactiveCardColors
import com.garrietta.haven.data.presetAppliesForState
import com.garrietta.haven.data.presetBadgeColors
import com.garrietta.haven.data.presetSwitchColors
import com.garrietta.haven.data.tilePresetColor
import com.garrietta.haven.data.tilePresetFor
import com.garrietta.haven.ui.components.CARD_ICON_KEYS
import com.garrietta.haven.ui.components.cardIconForKey
import com.garrietta.haven.ui.controls.GarageDoorVisual
import com.garrietta.haven.ui.components.SensorKind
import com.garrietta.haven.ui.controls.DialogActionButtons
import com.garrietta.haven.ui.controls.MediaSquircle
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.ServiceSlider
import com.garrietta.haven.ui.components.tabIconPairForKey
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.data.CardSize
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.UserTab
import com.garrietta.haven.data.groupEntitiesByRoom
import com.garrietta.haven.ui.components.shapeLabel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import com.garrietta.haven.data.updatesSummary
import com.garrietta.haven.ui.components.KodaMenuDivider
import com.garrietta.haven.ui.components.KodaMenuGroup
import com.garrietta.haven.ui.components.KodaMenuRow
import com.garrietta.haven.ui.components.KodaMenuSection
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.DiscoverySource
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.isLive
import com.garrietta.haven.data.cleanDeviceName
import com.garrietta.haven.data.deviceCountLabel
import com.garrietta.haven.data.isHomeRoomEntity
import com.garrietta.haven.ui.components.CardTapRoute
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.cardTapRoute
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.components.CardResizeHandle
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.resizeSettle
import com.garrietta.haven.ui.components.weatherConditionColorThemed
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.legibleSecondaryOn
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.components.reportCardColor
import com.garrietta.haven.ui.components.reportCardShape
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.reportHeroBounds
import com.garrietta.haven.ui.components.hideHeroWhenMorphSource
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.domainIcon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.badgeColors
import com.garrietta.haven.ui.components.sensorCardTint
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.ensureContrast
import com.garrietta.haven.ui.components.hideBodyWhenMorphSource
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.shapeTapAction
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.theme.polygonShape
import com.garrietta.haven.ui.theme.pressBounce
import com.garrietta.haven.data.FRONT_DOOR_LIVE_VIEW_ID
import com.garrietta.haven.data.FRONT_DOOR_RING_MQTT_CAMERA_ID
import com.garrietta.haven.data.FRONT_DOOR_SNAPSHOT_ID
import com.garrietta.haven.data.Placement
import com.garrietta.haven.data.frontDoorQuadrantShowsLive
import com.garrietta.haven.data.frontDoorResolverStates
import com.garrietta.haven.data.resolveFrontDoorCameraSource
import com.garrietta.haven.data.tabPlacementFor
import com.garrietta.haven.data.displayCameraName
import com.garrietta.haven.data.isRingCamera
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
fun TodayScreen(
    vm: HavenViewModel,
    // Feedback round 9, item 2: scroll states hoisted in HavenApp
    // above the AnimatedContent/direct branch swap (see there).
    // Before the lambda params (round 5 lesson).
    homeListState: LazyListState,
    homeLeftScrollState: ScrollState,
    homeRightScrollState: ScrollState,
    navigate: (Screen) -> Unit,
    onSignInExpired: () -> Unit,
) {
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
    val hiddenSections = migrateRound3Hidden(vm.settingsStore.homeHiddenSections)
    val showSection: (String) -> Boolean = { it !in hiddenSections }
    val entities = states.values
    val weather = entities.firstOrNull { it.domain == "weather" }
    val homeTrackers = entities.filter { it.domain == "device_tracker" && it.state == "home" }
    // Person entities are not devices: person.home (literally
    // named "Home") is excluded from Who's-home by design — the
    // chips group real device trackers by person instead.
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
    // Room sections hold real, room-assignable devices only
    // (2026-10-30 verdict round): cameras live in the hero,
    // weather in its block, people in who's-home, and zone/sun/
    // update are system entities, never room devices — the "Home /
    // 1", "Sun / Above horizon", and "Home Assistant / Unknown"
    // tiles that used to pollute "No room".
    //
    // Visual-fidelity round §6: room MEMBERSHIP (area -> ordered
    // entity-id lists) is memoized on the roster inputs only —
    // state keys, areas, entity areas, light overrides, prefs
    // version. A value update (a light toggling, a temp changing)
    // does NOT recompute grouping/sorting over ~366 entities; it
    // only recomposes the one card that collects that entity's
    // flow (see HavenViewModel.entityFlow). The remember block
    // reads vm.states.value (the StateFlow value, NOT the
    // collected State) so the snapshot read itself subscribes to
    // nothing — recomputation is driven by the keys alone. A
    // friendly-name rename bumps homePrefsVersion (room edits go
    // through the ViewModel), so ordering re-sorts after renames.
    val stateKeys = states.keys
    val membership: Map<String?, List<String>> = remember(
        stateKeys, entityAreas, areas, lightOverrides, homePrefsVersion,
    ) {
        val snapshot = vm.states.value
        val roomEntities = snapshot.values.filter { isHomeRoomEntity(it) }
        val grouped = groupEntitiesByRoom(roomEntities, entityAreas)
        grouped.mapValues { (_, members) ->
            members
                .filter { it.domain != "light" || isLightShownOnHome(it, lightOverrides) }
                .sortedBy { it.friendlyName }
                .map { it.entityId }
        }
    }
    fun roomMemberIds(areaId: String?): List<String> = membership[areaId] ?: emptyList()

    // The greeting is a plain composable (GreetingBlock) so both
    // layouts render it ABOVE the camera hero: the 2026-10-30 verdict
    // puts the greeting first on Home, then the hero, then weather,
    // then who's-home, then rooms. Weather and who's-home are plain
    // composables too (WeatherBlock, PresenceBlock), shared by the folded
    // LazyColumn and the unfolded scrolling Column.
    val greetingSection: LazyListScope.() -> Unit = {
        item {
            GreetingBlock(greeting, connection, showSection("greeting"), states.isNotEmpty(), onSignInExpired)
        }
    }
    val weatherSection: LazyListScope.() -> Unit = {
        if (showSection("weather") && weather != null) {
            item { WeatherBlock(weather, vm, navigate) }
        }
    }
    val homeSection: LazyListScope.() -> Unit = {
        if (showSection("home") && homeTrackers.isNotEmpty()) {
            item { PresenceBlock(homeTrackers, states) }
        }
    }
    // Round 3 dashboard blocks (security banner, vacuum tile,
    // media tiles, greenhouse card): always after who's-home,
    // rendering only the domains that exist in live state.
    // Visual-fidelity round §4: the combined block is split into
    // four independently gated blocks (see Round3Panels).
    val round3SecuritySection: LazyListScope.() -> Unit = {
        if (showSection("round3-security")) item { Round3SecurityBlock(vm, states, navigate) }
    }
    val round3VacuumSection: LazyListScope.() -> Unit = {
        if (showSection("round3-vacuum")) item { Round3VacuumBlock(vm, states, navigate) }
    }
    val round3MediaSection: LazyListScope.() -> Unit = {
        if (showSection("round3-media")) item { Round3MediaBlock(vm, states, navigate) }
    }
    val round3GreenhouseSection: LazyListScope.() -> Unit = {
        if (showSection("round3-greenhouse")) item { Round3GreenhouseBlock(vm, states, navigate) }
    }
    // Visible room sections, in HA order, after the hidden-section
    // filter: one (areaId, heading, member ids) triple per room,
    // with a null areaId for "No room". The room-filter chips narrow
    // this list; "My Home" (null filter) shows all ASSIGNED rooms.
    // Stage G: room presentation (display names, icons, order) is a
    // PRESENTATION layer over HA areas — area IDs never change, and
    // the HA order is the fallback for any area missing from the
    // stored order. "No room" has no area, so no override applies.
    val displayNames = vm.settingsStore.roomDisplayNames()
    val roomIcons = vm.settingsStore.roomIconKeys()
    val orderedAreaIds = resolveOrder(vm.settingsStore.roomOrder(), areas.map { it.id })
    // never come up empty from a stale filter.
    val allRooms: List<RoomSection> = buildList {
        for (areaId in orderedAreaIds) {
            val area = areas.firstOrNull { it.id == areaId } ?: continue
            if ("room:${area.id}" in hiddenSections) continue
            val members = roomMemberIds(area.id)
            if (members.isEmpty()) continue
            add(
                RoomSection(
                    area.id,
                    displayNames[area.id] ?: area.name,
                    members,
                    roomIcons[area.id],
                )
            )
        }
        val unassigned = roomMemberIds(null)
        if (unassigned.isNotEmpty()) add(RoomSection(null, "No room", unassigned, null))
    }
    // Room-filter chips (Gemini mockup round, 2026-11-01): "My Home"
    // plus one chip per visible room, in stored room order.
    // Feedback round 18, item 1 (SUPERSEDES the visual-fidelity
    // §4 "filter narrows room sections only" rule): the filter is
    // a VIEW SWITCH. "My Home" renders ONLY the overview sections
    // (greeting, cameras, Home Status, Favorites, enabled overview
    // blocks); individual room sections never render under it.
    // A room view renders ONLY the greeting, the chips, and that
    // room's grid — cameras, Home Status, Favorites, weather,
    // who's-home, and the Round 3 blocks all hide. "No room" is a
    // room view (unassigned entities only), never a My Home
    // section.
    // Feedback round 19, item 3: the filter lives on the
    // ViewModel now, not in TodayScreen's remember (see there).
    val roomFilter = vm.roomFilter
    // Feedback round 18, item 1 (DECIDED organization rule):
    // "My Home" shows ONLY the overview sections — room sections
    // never render under it (visibleRooms = emptyList()). A room
    // view shows ONLY that room's grid; the overview blocks hide
    // (isRoomView gates them at every render site below).
    val isRoomView = isRoomViewFilter(roomFilter)
    val visibleRooms = visibleRoomsFor(roomFilter, allRooms)
    // Chips actually shown: "My Home" + one per visible room,
    // plus tabs placed as chips (round 19, item 5). Placement is
    // exclusive: Hidden rooms and nav-tab rooms are NOT chips,
    // and a tab chip navigates to the tab (it is never the
    // selected filter — leaving Home selects nothing there).
    // "No room" has no placement and is always a chip.
    val roomChips: List<HomeChip> = buildHomeChips(
        allRooms,
        roomFilter,
        vm::roomPlacement,
        vm.userTabs,
        onRoomFilter = { vm.roomFilter = it },
        onNavigateTab = { navigate(it) },
    )

    // Room-filter state is hoisted in the ViewModel (round 19,
    // item 3 — never inside a pane composable, and never in
    // TodayScreen's remember): the selection survives popup
    // opens, detail returns, and fold/unfold posture changes.
    val wide = LocalWideLayout.current
    val sectionOrder = resolveOrder(
        migrateRound3SectionOrder(vm.settingsStore.homeSectionOrder()),
        HOME_SECTION_ORDER_DEFAULT,
    )
    if (!wide) {
        // Folded: ONE full-width LazyColumn (Round 4 unified Home;
        // the 1200dp cap was removed in Round 6). Feedback round
        // 18, item 1: room-filter chips are a view switch — room
        // views hide every overview block below; "My Home" hides
        // every room section (visibleRooms is empty there).
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
                state = homeListState,
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // Greeting FIRST (2026-10-30 verdict: the hero is no
                // longer the first element on Home), then the room-filter
                // chips, pinned after the greeting. Stage G: the blocks
                // below render in the stored section order — the greeting
                // is the pinned header and is not orderable.
                greetingSection()
                item { RoomFilterChips(roomChips) }
                item { Spacer(Modifier.height(12.dp)) }
                for (key in sectionOrder) {
                    when (key) {
                        "cameras" -> if (showSection("cameras") && !isRoomView) item { CameraQuadrantHero(vm, navigate, states) }
                        "weather" -> if (!isRoomView) weatherSection()
                        "home" -> if (!isRoomView) homeSection()
                        // Stage D: Home Status (real entities only,
                        // inline reveal) then Favorites, both hideable
                        // like every other section.
                        "status" -> if (showSection("status") && !isRoomView) item { HomeStatusSection(vm, states, navigate) }
                        "favorites" -> if (showSection("favorites") && !isRoomView) item { FavoritesSection(vm, states, navigate) }
                        "round3-security" -> if (!isRoomView) round3SecuritySection()
                        "round3-vacuum" -> if (!isRoomView) round3VacuumSection()
                        "round3-media" -> if (!isRoomView) round3MediaSection()
                        "round3-greenhouse" -> if (!isRoomView) round3GreenhouseSection()
                        "rooms" -> for (section in visibleRooms) {
                            // Stable section keys (2026-11-01 round 1):
                            // a panel return must never recompose a room
                            // section into a sibling's slot — keys pin
                            // each section to its area.
                            item(key = "room:${section.areaId ?: "unassigned"}") {
                                RoomSectionGrid(section.name, section.members, states, vm, 2, navigate, section.iconKey, areaId = section.areaId)
                            }
                        }
                    }
                }
                // Feedback round 16, item 8: the last row must
                // draw fully — trailing reserve 16dp -> 32dp.
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    } else {
        // Visual-fidelity round §2: unfolded TWO-PANE Home (the
        // mockup's split — this deliberately reverses Round 4's
        // unified-column decision and Stage D's decision not to
        // build the split, by explicit new directive). Left ~42%:
        // masthead, greeting, room filter chips, Cameras section
        // with the 2x2 grid. Right ~58%: Favorites heading +
        // circular pencil button, then the enabled Home sections
        // below in stored order. Each pane scrolls independently;
        // neither pane can strand an empty column (the left always
        // has greeting/chips; the right has Favorites + sections
        // on My Home, room grids on room views — round 18, item 1).
        // The room filter lives hoisted above both panes, so it
        // survives fold/unfold.
        Row(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        ) {
            Column(
                Modifier.weight(0.42f).verticalScroll(homeLeftScrollState),
            ) {
                GreetingBlock(greeting, connection, showSection("greeting"), states.isNotEmpty(), onSignInExpired)
                RoomFilterChips(roomChips)
                Spacer(Modifier.height(12.dp))
                if (showSection("cameras") && !isRoomView) CameraQuadrantHero(vm, navigate, states)
                Spacer(Modifier.height(16.dp))
                // Customization round, item 9: Home Status lives in
                // the LEFT pane directly below the cameras on
                // unfolded (favorites stays leading the right pane).
                // Its visibility still follows the stored section
                // prefs; the right-pane loop skips "status" so it
                // never renders twice. Default order cameras ->
                // status -> favorites already holds in
                // HOME_SECTION_ORDER_DEFAULT and is unchanged.
                // Round 18, item 1: both hide on room views.
                if (showSection("status") && !isRoomView) HomeStatusSection(vm, states, navigate)
            }
            Spacer(Modifier.width(16.dp))
            BoxWithConstraints(Modifier.weight(0.58f)) {
                // Measured grid columns (§2): TWO columns only
                // when the pane actually allows >=175dp per card
                // (2 * 175 + 14 spacing = 364dp); otherwise one
                // column. Never assume a Fold posture gives
                // 175dp/card — the measurement decides.
                val gridColumns = if (maxWidth >= 364.dp) 2 else 1
                Column(
                    Modifier.fillMaxSize().verticalScroll(homeRightScrollState),
                ) {
                    if (showSection("favorites") && !isRoomView) {
                        FavoritesSection(vm, states, navigate, columnsOverride = gridColumns, spacing = 14.dp)
                    }
                    for (key in sectionOrder) {
                        when (key) {
                            // Greeting, cameras, and favorites are
                            // placed by the panes themselves, never
                            // here; rooms render from visibleRooms
                            // (already room-filtered) below.
                            // Round 18, item 1: every overview key
                            // below hides on room views.
                            "weather" -> if (showSection("weather") && weather != null && !isRoomView) {
                                WeatherBlock(weather, vm, navigate)
                                Spacer(Modifier.height(16.dp))
                            }
                            "home" -> if (showSection("home") && homeTrackers.isNotEmpty() && !isRoomView) {
                                PresenceBlock(homeTrackers, states)
                                Spacer(Modifier.height(16.dp))
                            }
                            // "status" is placed by the left pane
                            // (item 9) — never here, never twice.
                            "status" -> { }
                            "round3-security" -> if (showSection("round3-security") && !isRoomView) Round3SecurityBlock(vm, states, navigate)
                            "round3-vacuum" -> if (showSection("round3-vacuum") && !isRoomView) Round3VacuumBlock(vm, states, navigate)
                            "round3-media" -> if (showSection("round3-media") && !isRoomView) Round3MediaBlock(vm, states, navigate)
                            "round3-greenhouse" -> if (showSection("round3-greenhouse") && !isRoomView) Round3GreenhouseBlock(vm, states, navigate)
                            "rooms" -> for (section in visibleRooms) {
                                RoomSectionGrid(section.name, section.members, states, vm, gridColumns, navigate, section.iconKey, spacing = 14.dp, areaId = section.areaId)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                    // Feedback round 16, item 8: the last row
                    // (Bedroom media tile) must draw fully —
                    // trailing reserve 16dp -> 32dp.
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Room-filter selection for Home (Gemini mockup round, 2026-11-01):
 * All = "My Home" (every visible room), Area(id) = one HA room,
 * Unassigned = the "No room" section. Data objects/classes so chip
 * equality is structural, never string matching on room names.
 */
internal sealed interface RoomFilter {
    data object All : RoomFilter
    data class Area(val id: String) : RoomFilter
    data object Unassigned : RoomFilter
}

/**
 * Feedback round 18, item 1: the room-filter rule, extracted pure
 * so it is unit-testable. "My Home" (All) yields NO room sections
 * — the overview blocks render instead; a room view yields only
 * that room's section(s).
 */
internal fun visibleRoomsFor(filter: RoomFilter, allRooms: List<RoomSection>): List<RoomSection> =
    when (filter) {
        RoomFilter.All -> emptyList()
        is RoomFilter.Area -> allRooms.filter { it.areaId == filter.id }
        RoomFilter.Unassigned -> allRooms.filter { it.areaId == null }
    }

/** Feedback round 18, item 1: true for any room view (Area/Unassigned). */
internal fun isRoomViewFilter(filter: RoomFilter): Boolean = filter != RoomFilter.All

/**
 * Stage G: one Home room section with its presentation-layer
 * overrides applied (display name + icon key). areaId null = the
 * unassigned ("No room") section, which has no HA area to override.
 */
internal data class RoomSection(
    val areaId: String?,
    val name: String,
    val members: List<String>,
    val iconKey: String?,
)

/**
 * Horizontally scrollable room-filter chips (mockup: "My Home" pale
 * green when selected, gray pills otherwise). Custom pills, not
 * FilterChip: FilterChip draws a checkmark on selection, which the
 * mockup does not have. Selected = primaryContainer/onPrimaryContainer
 * from the scheme (the mockup's pale green, scheme-correct in both
 * modes); unselected = surfaceContainer/onSurfaceVariant.
 * Feedback round 19, item 5: a chip is either a room filter or a
 * tab navigation — the row carries both, each with its own tap
 * action, so tab chips join the room chips without a second row.
 */
internal data class HomeChip(
    val label: String,
    val iconKey: String?,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Round 21, item 1: the room-chip list, extracted pure so the tap →
 * filter mapping is unit-testable. The regression that motivated it:
 * RoomFilterChips rendered `.clickable { chip.onClick }` — a function
 * reference, never invoked — so every chip tap was a no-op and My
 * Home stayed selected after Bedroom/Living Room taps. The call site
 * now invokes chip.onClick(); this builder owns the mapping the taps
 * dispatch through.
 */
internal fun buildHomeChips(
    allRooms: List<RoomSection>,
    roomFilter: RoomFilter,
    roomPlacement: (String) -> Placement,
    userTabs: List<UserTab>,
    onRoomFilter: (RoomFilter) -> Unit,
    onNavigateTab: (Screen) -> Unit,
): List<HomeChip> = buildList {
    add(HomeChip("My Home", null, roomFilter == RoomFilter.All) { onRoomFilter(RoomFilter.All) })
    for (section in allRooms) {
        if (section.areaId != null && roomPlacement(section.areaId) != Placement.ROOM_CHIP) continue
        val filter = if (section.areaId == null) RoomFilter.Unassigned else RoomFilter.Area(section.areaId)
        add(HomeChip(section.name, section.iconKey, roomFilter == filter) { onRoomFilter(filter) })
    }
    for (tab in userTabs) {
        if (tabPlacementFor(tab.pinned, tab.chip, tab.hidden) != Placement.ROOM_CHIP) continue
        add(HomeChip(tab.title, tab.icon, false) { onNavigateTab(Screen.UserTab(tab.id)) })
    }
}

@Composable
private fun RoomFilterChips(
    chips: List<HomeChip>,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (chip in chips) {
            val isSelected = chip.selected
            val glyphColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer,
                    )
                    .clickable { chip.onClick() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                // Stage G: a room's chosen icon rides in its chip
                // (Filled variant; unknown keys render no icon).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = chip.iconKey?.let { tabIconPairForKey(it)?.first }
                    if (icon != null) {
                        Icon(icon, null, tint = glyphColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        chip.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = glyphColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Compact Home weather block (2026-10-30 verdict: "a very small
 * [card], maybe even one third the height"): one row — badge, name,
 * condition + temp inline — no arc, no big number on Home. The full
 * dialog (arc + hourly forecast) is untouched: tapping still opens
 * it, long-press still opens the sheet. Shared by the folded list
 * and the unfolded left column.
 */
@Composable
private fun WeatherBlock(weather: HaEntity, vm: HavenViewModel, navigate: (Screen) -> Unit) {
    Text("Weather", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    HomeWeatherCard(weather, vm, navigate)
    Spacer(Modifier.height(16.dp))
}

/** One room section as a mockup-language grid: heading + rows of
 * [columns] cards (2 folded / Fold8, 3 on EXPANDED). A plain Column,
 * so it slots into both the folded LazyColumn (as one item per room)
 * and the unfolded left/right columns. A short final row pads its
 * missing cells with spacers so cards keep their grid width.
 */
@Composable
/**
 * One room section: heading + a [columns]-column card grid
 * (Round 4: 2 folded, 4 unfolded, full width on both postures).
 */
private fun RoomSectionGrid(
    name: String,
    memberIds: List<String>,
    states: Map<String, HaEntity>,
    vm: HavenViewModel,
    columns: Int,
    navigate: (Screen) -> Unit,
    iconKey: String? = null,
    spacing: Dp = 12.dp,
    // Feedback round 20, item 5: areaId scopes the persisted
    // size (room:<areaId>:<entityId>); null = No room.
    areaId: String? = null,
) {
    // Feedback round 20, item 5: Arrange mode shows the drag
    // handles; exiting keeps sizes (they live in prefs).
    var arrangeMode by remember { mutableStateOf(false) }
    // Round 21, item 7: back exits Arrange (enabled only while
    // this section is arranging, so a section that is not
    // arranging never swallows another section's back press).
    BackHandler(enabled = arrangeMode) { arrangeMode = false }
    val scope = "room:${areaId ?: "unassigned"}"
    // Resolved here, per section, from the already-collected map:
    // no extra subscription, no re-sort (membership order is
    // memoized in TodayScreen). Cards below still collect their
    // own entity flows, so resolving here costs no card
    // recomposition on unrelated updates.
    val members = memberIds.mapNotNull { states[it] }
    Column {
        // Stage E §9: room dot for an important alert in this room
        // (unlocked lock, open door/window, triggered alarm) — never a
        // count. Armed alarms are secure, motion is transient, and
        // lights/energy are routine, so none of them light the dot.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // Stage G: the room's chosen icon leads its heading.
            val icon = iconKey?.let { tabIconPairForKey(it)?.first }
            if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f, fill = false))
            // Round 21, item 7: the Arrange button is gone — a
            // long-press on any card in this section enters
            // Arrange/resize mode instead (onArrangeToggle below).
            // Back exits Arrange (see the BackHandler at the end
            // of this section); exiting keeps sizes (prefs).
            if (vm.badgeRoom && hasImportantAlert(members)) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(8.dp)
                        .background(alertDotColor(isDarkBackdrop(MaterialTheme.colorScheme.background)), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Visual-fidelity round §1: active or favorite media
        // players span the full width as 2x1 cards (their own
        // rows), never chunked beside standard cards — the
        // mockup's Sonos Arc row. Everything else chunks into
        // the standard grid below.
        val wideMedia = members.filter { member ->
            member.domain == "media_player" &&
                (member.state == "playing" || member.entityId in vm.favorites)
        }
        val standard = members - wideMedia.toSet()
        for ((wideIndex, media) in wideMedia.withIndex()) {
            HomeRoomMediaCard(vm, media.entityId, gridIndex = wideIndex, arrangeMode = arrangeMode, onArrangeToggle = { arrangeMode = !arrangeMode }, navigate = navigate)
            Spacer(Modifier.height(spacing))
        }
        // Feedback round 20, item 5: wide-aware rows, same rule
        // as FavoritesSection — a WIDE card takes its own
        // full-width row, consecutive non-wide cards chunk as
        // before, gridIndex still counts flat position in the
        // standard list. With no wide cards this reproduces the
        // old chunked loop exactly.
        var flatIdx = 0
        var i = 0
        while (i < standard.size) {
            if (vm.cardSize(scope, standard[i].entityId) == CardSize.WIDE) {
                val member = standard[i]
                Box(Modifier.fillMaxWidth().resizeSettle()) {
                    HomeRoomCard(vm, member.entityId, navigate, gridIndex = flatIdx, arrangeMode = arrangeMode, onArrangeToggle = { arrangeMode = !arrangeMode })
                    if (arrangeMode) {
                        CardResizeHandle(
                            CardSize.WIDE,
                            { vm.setCardSize(scope, member.entityId, it) },
                            Modifier.align(Alignment.BottomEnd),
                        )
                    }
                }
                Spacer(Modifier.height(spacing))
                i++
                flatIdx++
            } else {
                val row = mutableListOf<HaEntity>()
                val rowStart = flatIdx
                while (i < standard.size && row.size < columns &&
                    vm.cardSize(scope, standard[i].entityId) != CardSize.WIDE
                ) {
                    row.add(standard[i])
                    i++
                    flatIdx++
                }
                Row {
                    for ((colIdx, member) in row.withIndex()) {
                        val size = vm.cardSize(scope, member.entityId)
                        Box(
                            Modifier.weight(1f).resizeSettle()
                                .then(if (size == CardSize.TALL) Modifier.heightIn(min = 272.dp) else Modifier),
                        ) {
                            HomeRoomCard(
                                vm, member.entityId, navigate, gridIndex = rowStart + colIdx,
                                modifier = if (size == CardSize.TALL) Modifier.fillMaxSize() else Modifier,
                                arrangeMode = arrangeMode,
                                onArrangeToggle = { arrangeMode = !arrangeMode },
                            )
                            if (arrangeMode) {
                                CardResizeHandle(
                                    size,
                                    { vm.setCardSize(scope, member.entityId, it) },
                                    Modifier.align(Alignment.BottomEnd),
                                )
                            }
                        }
                        if (member != row.last()) Spacer(Modifier.width(spacing))
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.width(spacing))
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(spacing))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Mockup status line for a room card: the whole state at a glance,
 * in the mockup's words ("On • 100%", "Heating to 75%", "Locked").
 * Lights read brightness; climate reads its heating/cooling target
 * when one exists; covers read position; everything else reads its
 * plain state text. Never a raw attribute dump.
 */
private fun homeCardStatus(entity: HaEntity): String = when (entity.domain) {
    "light" -> if (entity.state == "on") {
        val b = entity.attrInt("brightness")?.let { lightBrightnessPercent(it) }
        if (b != null) "On • $b%" else "On"
    } else "Off"
    "switch", "input_boolean" -> if (entity.state == "on") "On" else "Off"
    "cover" -> {
        val pos = entity.attrInt("current_position")
        val base = entity.stateText()
        if (pos != null && pos != 0 && pos != 100) "$base • $pos% open" else base
    }
    "climate" -> {
        val action = entity.attrString("hvac_action")
        val target = entity.attrDouble("temperature")
            ?: entity.attrDouble("target_temp_low")
            ?: entity.attrDouble("target_temp_high")
        when {
            action == "heating" && target != null -> "Heating to ${target.formatTemp()}"
            action == "cooling" && target != null -> "Cooling to ${target.formatTemp()}"
            else -> entity.stateText()
        }
    }
    else -> entity.stateText()
}

/**
 * Visual-fidelity round §1: the default Home card shape — a subtle
 * asymmetric M3E shape (28dp / 12dp / 28dp / 28dp, the directive's
 * suggestion), alternating its mirror by a stable per-entity hash
 * so a card's corners NEVER flip on recomposition, entity
 * updates, or grid reordering (positional alternation would flip
 * every card below an insertion). Shape-picker overrides and the
 * global default still win over this (see HomeRoomCard).
 */
/**
 * UI rewrite round §3: card corners rotate by GRID INDEX % 4 (not
 * the refactor's per-entity hash mirror): three 28dp corners + one
 * 12dp corner — index0 → 12dp at bottomEnd; index1 → 12dp at
 * bottomStart; index2 → 12dp at topEnd; index3 → 12dp at topStart.
 * A per-entity shape-picker override still beats this default (see
 * the call sites); the rotation only sets the default.
 */
// UI rewrite's card rule, now delegating to the app-wide general
// shape (expressive-menus round, item 6); kept as the name every
// Home card call site already uses.
fun homeCardShapeByIndex(index: Int): RoundedCornerShape = generalCardShape(index)

/**
 * Visual-fidelity round §1: the wide (2x1) media card — the
 * mockup's Sonos Arc row. Fixed 136dp like the standard cards, so
 * a media row never unbalances its neighbors. Layout: a 48dp
 * expressive squircle badge left, a centered title + subtitle
 * column (track title/artist while playing, else the player name
 * + state — never invented), a circular play/pause button
 * top-right (media_play_pause, the existing pathway). Below the
 * row, exactly ONE bar, from real attributes only: a progress bar
 * while playing with media_position + media_duration, else the
 * volume slider when volume_level exists. Tapping the rest of
 * the card opens the detail panel, like every other card.
 */
@Composable
fun HomeRoomMediaCard(vm: HavenViewModel, entityId: String, gridIndex: Int = 0, arrangeMode: Boolean = false, onArrangeToggle: (() -> Unit)? = null, modifier: Modifier = Modifier, navigate: (Screen) -> Unit) {
    val liveEntity by vm.entityFlow(entityId).collectAsState()
    val entity = liveEntity ?: return
    val snap = vm.travelSnapshots[entity.entityId]
    val display = snap ?: entity
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkBackdrop(scheme.background)
    val cardColors = domainCardColors(display, isDark)
    val mediaCustom = rememberCardCustom(vm)
    // Feedback round, item 5: a set preset drives the badge pair
    // too (dark tint + vivid glyph from the preset hue), not just
    // the container — the matrix only applies to unset cards.
    // Feedback round 2, item 1: the preset is an ON-state color —
    // a preset media card whose player is off/unavailable renders
    // the matrix INACTIVE gray, never the custom on-color.
    val mediaPreset = tilePresetFor(mediaCustom.tilePresets, entity.entityId, mediaCustom.domainTilePresets, entity.domain)
    val mediaPresetActive = mediaPreset != null && presetAppliesForState(display.domain, display.state)
    val container = (if (mediaPresetActive) tilePresetColor(mediaPreset, isDark) else null)
        ?: (if (mediaPreset != null) inactiveCardColors(isDark).container else null)
        ?: cardColors?.container ?: scheme.surfaceContainerLow
    val (badgeBg, badgeGlyph) = if (mediaPresetActive) {
        presetBadgeColors(mediaPreset!!, isDark)
    } else if (mediaPreset != null) {
        val inactive = inactiveCardColors(isDark)
        inactive.badgeBg to inactive.badgeGlyph
    } else if (cardColors != null) {
        cardColors.badgeBg to cardColors.badgeGlyph
    } else {
        badgeColors(scheme, display, accentFor(entity.entityId))
    }
    val playing = display.state == "playing"
    // Custom title replaces the friendly-name fallback only; a
    // playing track title still wins (item 2).
    val title = if (playing) {
        display.attrString("media_title") ?: customTitleFor(mediaCustom.titles, entity.entityId, display.friendlyName)
    } else {
        customTitleFor(mediaCustom.titles, entity.entityId, display.friendlyName)
    }
    val mediaIcon = cardIconForKey(customIconKeyFor(mediaCustom.iconKeys, entity.entityId, CARD_ICON_KEYS))
        ?: domainIcon(display)
    val subtitle = if (playing) {
        display.attrString("media_artist") ?: display.stateText()
    } else {
        display.stateText()
    }
    val position = display.attrDouble("media_position")
    val duration = display.attrDouble("media_duration")
    val volume = display.attrDouble("volume_level")
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = com.garrietta.haven.ui.components.LocalReduceMotion.current
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.97f else 1f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.spring(stiffness = 10_000f, dampingRatio = 1f) else pressBounce(),
        label = "mediaCardPress",
    )
    // Feedback round 11, item 2: the outline is a named value so
    // the morph landing can report this exact shape.
    val mediaCardOutline = LocalShapePrefs.current.cardShapeFor(entity.entityId, entity.domain)
        ?: homeCardShapeByIndex(gridIndex)
    Surface(
        shape = mediaCardOutline,
        color = container,
        modifier = modifier.fillMaxWidth()
            // Round 21, item 6: the 136dp floor, like HomeRoomCard
            // (was a fixed height — a TALL-resized media card left
            // a 136dp card in a 272dp box). Content stays
            // top-aligned when the card grows.
            .heightIn(min = 136.dp)
            .reportCardBounds(entity.entityId)
            .reportCardShape(entity.entityId, mediaCardOutline)
            .reportCardColor(entity.entityId, container)
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .hideWhenMorphSource(entity.entityId)
            .hideBodyWhenMorphSource(entity.entityId)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                // Feedback round 17, item 2: media taps open
                // the detail page (MediaPageControl) like
                // every other domain — the old tap-to-sheet
                // was the same inconsistency as vacuum/cover.
                // Round 21, item 7: long-press enters Arrange
                // mode instead of the settings sheet; taps are
                // swallowed while in Arrange mode. Round 21,
                // item 5: navigate is non-null — every caller
                // passes it, and the sheet fallback is gone.
                onClick = { if (!arrangeMode) navigate(Screen.EntityDetail(entity.entityId)) },
                onLongClick = { onArrangeToggle?.invoke() },
            ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(
                        badgeBg,
                        LocalShapePrefs.current.badgeShapeFor(entity.entityId, entity.domain)
                            ?: polygonShape(shapeForDomain(entity.domain)),
                    )
                        .reportHeroBounds(entity.entityId)
                        .hideHeroWhenMorphSource(entity.entityId),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        mediaIcon,
                        contentDescription = null,
                        tint = badgeGlyph,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ensureContrast(scheme.onSurface, container, 4.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = ensureContrast(scheme.onSurfaceVariant, container, 4.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Expressive-menus round: the card play/pause is a
                // 64dp squircle (was a 48dp circle) with a filled
                // primary container while playing — same tap, same
                // service call; body tap still opens the panel.
                PressablePillButton(
                    onClick = { vm.callService("media_player", "media_play_pause", entity.entityId) },
                    modifier = Modifier.size(64.dp),
                    height = 64.dp,
                    shape = MediaSquircle,
                    containerColor = if (playing) scheme.primaryContainer else scheme.surfaceContainerHighest,
                    contentColor = if (playing) scheme.onPrimaryContainer else scheme.onSurface,
                ) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // One bar only, from real attributes: progress while
            // playing (position advances; a paused bar would be
            // stale), else volume when the player reports it.
            if (playing && position != null && duration != null && duration > 0.0) {
                LinearProgressIndicator(
                    progress = { (position / duration).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (volume != null) {
                ServiceSlider(
                    value = volume.toFloat(),
                    range = 0f..1f,
                    onFinished = { level ->
                        vm.callService(
                            "media_player",
                            "volume_set",
                            entity.entityId,
                            org.json.JSONObject().put("volume_level", level),
                        )
                    },
                    accent = badgeBg,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Mockup-language room card (Gemini mockup round, 2026-11-01;
 * visual-fidelity round §1 for the mockup's exact proportions):
 * one coherent card background (no inner sub-box), a 44dp
 * expressive badge with a 22dp icon top-left, a compact control or
 * current value top-right, a 16sp bold title and a 13sp subtitle
 * below, all at a fixed 136dp height so every standard card shares
 * baselines. Colors come from the centralized domain table
 * ([domainCardColors]) — kelvin/brightness tints stay on the
 * detail panels, not here — except temperature/humidity sensors,
 * which keep their semantic coral/sky path by standing law.
 *
 * The card collects its OWN entity flow (see
 * [HavenViewModel.entityFlow]), so a single entity update
 * recomposes only its own card, never the dashboard.
 *
 * Interactions mirror the tab tiles exactly, plus the §1 switch
 * row: background tap on a LIGHT toggles it directly inline (no
 * panel, no dialog — the 2026-11-01 panels round's card-level
 * model); background tap on any other domain opens its
 * full-screen panel (Home behavior — never a full page); badge
 * tap actuates where a shape action exists; long-press enters
 * Arrange mode (round 21, item 7). Morph wiring: the card
 * reports its bounds and its badge anchors the traveling header
 * (the badge rect is reported directly, so the 44dp badge size is
 * traveler-exact — no row-derivation size to update).
 *
 * Lock safety: the card Switch NEVER unlocks one-tap. Turning a
 * locked lock off opens the same confirmation dialog as the
 * detail panel ("Unlock <name>?"); locking is direct, mirroring
 * LockControl. The badge-zone tap for locks opens the panel
 * (never toggles); covers toggle via vm.toggleCover, whose
 * optimistic final state keeps the guard working.
 *
 * Dark-mode legibility: name and status are floored to 4.5:1 against
 * the card's own container via ensureContrast (the #83 light-glyph
 * precedent) — the dim-grey-on-tinted-tile failure from the unfolded
 * video's Living Room lights tab cannot recur here.
 *
 * Corners: the default is a subtle asymmetric M3E shape, stable
 * per entity (hash alternation, never positional, so a card's
 * corners never flip when neighbors change or entities update).
 * A per-entity shape-picker override beats the global default,
 * which beats this alternating default.
 */
@Composable
fun HomeRoomCard(vm: HavenViewModel, entityId: String, navigate: (Screen) -> Unit, gridIndex: Int = 0, modifier: Modifier = Modifier, arrangeMode: Boolean = false, onArrangeToggle: (() -> Unit)? = null, onOpenPanel: (() -> Unit)? = null) {
    // Entity-scoped state (§6): this card recomposes only when
    // its own entity changes. An entity that vanished from state
    // renders nothing (membership rebuilds without it).
    val liveEntity by vm.entityFlow(entityId).collectAsState()
    val entity = liveEntity ?: return
    val accent = accentFor(entity.entityId)
    // Snapshot-held visuals, same rule as tiles and cards: while this
    // card's morph is open, badge/name/state/container resolve from
    // the open-time snapshot, never the live entity. Actuation stays
    // live.
    val snap = vm.travelSnapshots[entity.entityId]
    val display = snap ?: entity
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkBackdrop(scheme.background)
    val isBinary = display.domain in setOf("light", "switch", "input_boolean", "fan")
    // Feedback round 17, item 2: taps ALWAYS open the control
    // panel (climate popout, vacuum panel for vacuum.t2292,
    // detail page for everything else) — the old binary-only
    // rule sent vacuum/cover/media taps to the settings sheet
    // while Lamp/Ecobee taps opened full panels, the
    // inconsistency filmed on the 2026-11-04 runs. Follow-up A
    // (2026-11-03): an explicit onOpenPanel override replaces
    // the routing wholesale (surface tap, surface long-press,
    // badge long-press). The Home Status panel passes
    // navigate-to-detail so panel cards never open a panel UNDER
    // the opaque detail panel (the detail block renders before
    // any sheet block, so a panel opened from inside a panel
    // would paint underneath it and read as a dead tap until
    // back reveals it). Badge-zone taps (toggle/toggleCover)
    // and the switch stay live as on the home page. Default
    // null keeps every existing call site byte-identical.
    val openPanel: () -> Unit = onOpenPanel ?: {
        when (cardTapRoute(display.domain, entity.entityId)) {
            CardTapRoute.CLIMATE_POPOUT -> vm.openClimatePopout(entity.entityId)
            CardTapRoute.VACUUM_PANEL -> navigate(Screen.VacuumPanel)
            CardTapRoute.ENTITY_DETAIL -> navigate(Screen.EntityDetail(entity.entityId))
        }
    }
    // Round 21, item 7 (SUPERSEDES round 17, item 2's long-press
    // sheet rule): long-press ANYWHERE on a card enters
    // Arrange/resize mode (onArrangeToggle), and the settings sheet
    // is gone — the popup itself carries the settings rows now
    // (round 21, item 5). Taps are swallowed while in Arrange
    // mode: a card in Arrange never opens a panel or actuates.
    // With an onOpenPanel override (Home Status panel cards) the
    // override still wins for taps, and the arrange toggle stays
    // null there — long-press inside a panel is a no-op, since
    // resizing a card from inside a panel has no meaning.
    // §1 container + badge: the domain table wins where it has a
    // row; temperature/humidity sensors bypass it (standing law)
    // and fall through to the existing semantic pipeline below.
    val sensorKindForCard = if (display.domain == "sensor") {
        sensorKind(display.entityId, display.attrString("device_class"), display.friendlyName)
    } else {
        null
    }
    val cardColors = if (sensorKindForCard == SensorKind.TEMPERATURE ||
        sensorKindForCard == SensorKind.HUMIDITY
    ) {
        null
    } else {
        domainCardColors(display, isDark)
    }
    // Customization round, item 2: same-store overrides read here
    // as on every other surface (see CardSettings.kt for the list).
    val roomCustom = rememberCardCustom(vm)
    val cardDisplayName = customTitleFor(roomCustom.titles, entity.entityId, sensorDisplayName(display))
    val cardIcon = cardIconForKey(customIconKeyFor(roomCustom.iconKeys, entity.entityId, CARD_ICON_KEYS))
        ?: domainIcon(display)
    // Feedback round, item 5: a set preset drives the badge pair
    // too (see the media card above) — even for temp/humidity
    // sensors, whose coral/sky bypass only governs unset cards.
    // Feedback round 2, item 1: the preset is an ON-state color —
    // it applies only while the entity is active; a preset card
    // whose entity is off renders the matrix INACTIVE gray, never
    // the custom on-color. (The display snapshot's state, so a
    // morph-open card keeps the color it opened with.)
    val roomPreset = tilePresetFor(roomCustom.tilePresets, entity.entityId, roomCustom.domainTilePresets, entity.domain)
    val presetActive = roomPreset != null && presetAppliesForState(display.domain, display.state)
    val container = (if (presetActive) tilePresetColor(roomPreset, isDark) else null)
        ?: (if (roomPreset != null) inactiveCardColors(isDark).container else null)
        ?: cardColors?.container ?: when {
        display.domain == "light" && display.state == "on" ->
            lightBrightnessContainer(
                scheme,
                display.attrInt("color_temp_kelvin"),
                display.attrInt("brightness")?.let { lightBrightnessPercent(it) },
                true,
            )
        display.domain == "climate" ->
            lerp(
                scheme.surfaceContainerLow,
                climateModeColor(display.state, display.attrString("hvac_action")),
                0.15f,
            )
        // Round 4: sensor cards tint toward their semantic color
        // (same 0.15 lerp as climate/weather), so temperature reads
        // warm and humidity reads sky-blue, never grey.
        display.domain == "sensor" -> {
            val tint = sensorCardTint(sensorKind(display.entityId, display.attrString("device_class"), display.friendlyName))
            if (tint != null) lerp(scheme.surfaceContainerLow, tint, 0.15f) else scheme.surfaceContainerLow
        }
        else -> scheme.surfaceContainerLow
    }
    val nameColor = ensureContrast(scheme.onSurface, container, 4.5f)
    val statusColor = ensureContrast(scheme.onSurfaceVariant, container, 4.5f)
    // Feedback round 17, item 1: the badge pair is the shared
    // tile rule (tileBadgePair) — the exact pair the traveler
    // and headers now carry, so card, flight, and destination
    // can never disagree (the eufy yellow was the tile using
    // this rule while headers used the hash fallback).
    val (badgeBg, badgeGlyph) = tileBadgePair(
        roomCustom.tilePresets, roomCustom.domainTilePresets, display, isDark, scheme, accent,
    )
    val shapeAction = shapeTapAction(vm, entity)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = com.garrietta.haven.ui.components.LocalReduceMotion.current
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.97f else 1f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.spring(stiffness = 10_000f, dampingRatio = 1f) else pressBounce(),
        label = "roomCardPress",
    )
    val badgeInteraction = remember { MutableInteractionSource() }
    val badgePressed by badgeInteraction.collectIsPressedAsState()
    val badgeScale by androidx.compose.animation.core.animateFloatAsState(
        if (badgePressed) 0.92f else 1f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.spring(stiffness = 10_000f, dampingRatio = 1f) else pressBounce(),
        label = "roomCardBadgePress",
    )
    // Lock confirmation (§1 safety): turning a locked lock off
    // from the card Switch opens the detail panel's exact dialog
    // copy. Locking is direct, mirroring LockControl.
    // Feedback round 2, item 3: polygon card outlines (the
    // expressive shapes from the shape picker) are inscribed in a
    // circle of the card's min dimension, so their edges cut into
    // the content box near the bottom — subtitle descenders ("heat
    // cool", "closed", the bottom of "O") get clipped by the polygon
    // edge. Rounded-corner outlines never clip (28dp corners clear
    // the 16dp-padded text zone), so only polygon outlines get the
    // shifted layout: the badge-to-text spacer shrinks 16dp -> 4dp
    // and the bottom padding grows 16dp -> 28dp, moving the whole
    // text block up 12dp with the card height unchanged at 136dp
    // (content stays 129dp: 16 + 44 + 4 + title + subtitle + 28).
    // Bottom padding alone cannot move top-anchored content — the
    // spacer reduction is the mechanism; the padding growth keeps
    // the total height (and the Surface's internal layout) intact.
    val cardOutline = LocalShapePrefs.current.cardShapeFor(entity.entityId, entity.domain)
        ?: homeCardShapeByIndex(gridIndex)
    val isPolygonCard = cardOutline !is RoundedCornerShape
    var confirmUnlock by remember { mutableStateOf(false) }
    Surface(
        // Card-outline shape (2026-10-30 picker): a per-entity
        // override beats the global default; without either, the
        // §1 stable alternating asymmetric shape (never
        // extraLarge here — the mockup's cards are asymmetric).
        shape = cardOutline,
        color = container,
        // Feedback round 5, item 3: 136dp is a FLOOR, not a
        // ceiling. The fixed height left ≤6dp of slack on the
        // filmed variants (48dp garage door visual, 26sp climate
        // trailing temperature, title + subtitle line heights),
        // so any font-scale growth overflowed the Surface clip
        // and cut subtitle descenders ("Cooling to 67°") and whole
        // lines ("Closed") at the card's bottom edge. With a
        // minimum height, standard cards stay exactly 136dp and
        // overflowing content grows its card instead of clipping
        // — a real layout buffer, never an overlay/scrim, never
        // negative padding. A grown card makes its grid row
        // taller; its row-mate stays 136dp top-aligned, which
        // reads better than cut text. The polygon path keeps its
        // round-2 spacer/padding shift (inscribed-circle clip).
        modifier = modifier.fillMaxWidth()
            .heightIn(min = 136.dp)
            .reportCardBounds(entity.entityId)
            .reportCardShape(entity.entityId, cardOutline)
            .reportCardColor(entity.entityId, container)
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .hideWhenMorphSource(entity.entityId)
            .hideBodyWhenMorphSource(entity.entityId)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                // Round 4 split zones (binary cards): the badge row
                // below owns tap-to-toggle, the text zone below owns
                // tap-to-panel, and long-press ANYWHERE enters
                // Arrange mode (round 21, item 7 — the old
                // long-press-opens-sheet rule is gone) — so this
                // surface-level tap is the panel fallback for taps
                // on card padding only. Climate's tap still opens
                // the dial panel. (The old tap-anywhere-toggles
                // model for lights is deliberately replaced by the
                // zone split.) While in Arrange mode the tap is
                // swallowed: no panel, no actuation.
                onClick = { if (!arrangeMode) openPanel() },
                onLongClick = { onArrangeToggle?.invoke() },
            ),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (isPolygonCard) 28.dp else 16.dp)) {
            // §1: NO inner sub-box. Round 4's badge zone wore a
            // surfaceContainerHighest background + 24dp corners +
            // 8dp padding — the "wide rectangular inner sub-box"
            // the mockup does not have. The zone split BEHAVIOR
            // stays (badge-row tap toggles, text tap opens the
            // panel, long-press opens the panel); only the visible
            // inner background is removed, so the card reads as
            // one coherent surface with the badge directly in it.
            val zoneInteraction = remember { MutableInteractionSource() }
            val zoneModifier = when {
                isBinary -> Modifier.fillMaxWidth()
                    .combinedClickable(
                        interactionSource = zoneInteraction,
                        indication = null,
                        onClick = { if (!arrangeMode) vm.toggle(entity.entityId) },
                        onLongClick = { onArrangeToggle?.invoke() },
                    )
                display.domain == "cover" -> Modifier.fillMaxWidth()
                    .combinedClickable(
                        interactionSource = zoneInteraction,
                        indication = null,
                        onClick = { if (!arrangeMode) vm.toggleCover(entity.entityId) },
                        onLongClick = { onArrangeToggle?.invoke() },
                    )
                else -> Modifier
            }
            Box(zoneModifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(
                        badgeBg,
                        // Tile badge shape (2026-10-30 picker): a
                        // per-entity override beats the global
                        // default; without either, the domain shape.
                        LocalShapePrefs.current.badgeShapeFor(entity.entityId, entity.domain)
                            ?: polygonShape(shapeForDomain(entity.domain)),
                    )
                        .graphicsLayer(scaleX = badgeScale, scaleY = badgeScale)
                        // Traveling header anchor: the badge rect is
                        // the travel origin (tile precedent), and the
                        // badge hides the moment its morph opens.
                        .reportHeroBounds(entity.entityId)
                        .hideHeroWhenMorphSource(entity.entityId)
                        .then(if (shapeAction != null) Modifier.combinedClickable(
                            interactionSource = badgeInteraction,
                            indication = null,
                            onClick = { if (!arrangeMode) shapeAction() },
                            onLongClick = { onArrangeToggle?.invoke() },
                        ) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        cardIcon,
                        contentDescription = null,
                        tint = badgeGlyph,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                // Trailing control, mockup-style: a toggle for
                // binary entities (tap flips, never opens the
                // dialog — the Switch consumes its own press), a
                // toggle for locks (unlock gated on confirmation)
                // and covers (via toggleCover), the current
                // temperature for climate, nothing for the rest.
                // UI rewrite round §3: the 52x30 pill switch with
                // matrix track/thumb colors. The stock Switch below
                // is a defensive fallback only — domainSwitchColors
                // covers every domain that reaches this branch.
                // Feedback round 2, item 2: a set preset drives the
                // switch too (preset badge bg track, preset
                // container thumb when checked; inactive gray when
                // unchecked) — the badge icon's chromatic styling,
                // extended to the switch.
                val pillSwitchColors = if (roomPreset != null) {
                    presetSwitchColors(roomPreset, isDark)
                } else {
                    domainSwitchColors(display, isDark)
                }
                when (display.domain) {
                    "light", "switch", "input_boolean", "fan" -> if (pillSwitchColors != null) {
                        PillSwitch(
                            checked = display.state == "on",
                            onCheckedChange = { vm.toggle(entity.entityId) },
                            colors = pillSwitchColors,
                        )
                    } else {
                        Switch(
                            checked = display.state == "on",
                            onCheckedChange = { vm.toggle(entity.entityId) },
                        )
                    }
                    "lock" -> if (pillSwitchColors != null) {
                        PillSwitch(
                            checked = display.state == "locked",
                            onCheckedChange = { wantLocked ->
                                if (!wantLocked && display.state in setOf("locked", "locking")) {
                                    confirmUnlock = true
                                } else {
                                    vm.toggleLock(entity.entityId)
                                }
                            },
                            colors = pillSwitchColors,
                        )
                    } else {
                        Switch(
                            checked = display.state == "locked",
                            onCheckedChange = { wantLocked ->
                                if (!wantLocked && display.state in setOf("locked", "locking")) {
                                    confirmUnlock = true
                                } else {
                                    vm.toggleLock(entity.entityId)
                                }
                            },
                        )
                    }
                    "cover" -> if (display.attrString("device_class") == "garage") {
                        // Customization round, item 5: a garage card
                        // shows the door itself, not a switch — tap
                        // rises/lowers it; the panel keeps the same
                        // visual plus Open/Close/Stop pills.
                        val garagePosition = display.attrInt("current_position")
                        val garageOpen = if (garagePosition != null) {
                            garagePosition.coerceIn(0, 100) / 100f
                        } else if (display.state == "open") 1f else if (display.state == "closed") 0f else 0.5f
                        GarageDoorVisual(
                            openFraction = garageOpen,
                            onToggle = { vm.toggleCover(entity.entityId) },
                            width = 44.dp,
                            height = 48.dp,
                        )
                    } else if (pillSwitchColors != null) {
                        PillSwitch(
                            checked = display.state == "open",
                            onCheckedChange = { vm.toggleCover(entity.entityId) },
                            colors = pillSwitchColors,
                        )
                    } else {
                        Switch(
                            checked = display.state == "open",
                            onCheckedChange = { vm.toggleCover(entity.entityId) },
                        )
                    }
                    "climate" -> display.attrDouble("current_temperature")?.let { cur ->
                        Text(
                            cur.formatTemp(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ensureContrast(scheme.onSurface, container, 4.5f),
                        )
                    }
                }
            }
            } // badge-zone Box
            Spacer(Modifier.height(if (isPolygonCard) 4.dp else 16.dp))
            // Round 4 split zone, lower half: tapping the name or
            // status text opens the expanded panel (binary cards);
            // long-press anywhere enters Arrange mode (round 21,
            // item 7). Non-binary cards keep their surface-level
            // routing; this zone is harmless there (same openPanel
            // target). Taps are swallowed while in Arrange mode.
            val textZoneInteraction = remember { MutableInteractionSource() }
            Column(
                Modifier.fillMaxWidth().combinedClickable(
                    interactionSource = textZoneInteraction,
                    indication = null,
                    onClick = { if (!arrangeMode) openPanel() },
                    onLongClick = { onArrangeToggle?.invoke() },
                ),
            ) {
            Text(
                cardDisplayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = nameColor,
                // Single line, always (2026-11-01 round 1): a
                // wrapping title reflowed mid-morph-return and
                // read as clipped text ("obee Thermostat"); the
                // full name is one ellipsis away, never sliced.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier,
            )
            Text(
                homeCardStatus(display),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier,
            )
            } // text-zone Column
        }
    }
    if (confirmUnlock) {
        // Unlock-confirmation preserved exactly; only the buttons
        // grow into large expressive pills (confirm = filled).
        AlertDialog(
            onDismissRequest = { confirmUnlock = false },
            title = { Text("Unlock ${display.friendlyName}?") },
            text = { Text("This removes the lock. Make sure that's what you want.") },
            confirmButton = {
                DialogActionButtons(
                    onCancel = { confirmUnlock = false },
                    onConfirm = {
                        confirmUnlock = false
                        vm.toggleLock(entity.entityId)
                    },
                    confirmLabel = "Unlock",
                )
            },
        )
    }
}

/**
 * The Home greeting block, shared by the narrow section and the
 * wide full-width slot (2026-10-30 verdict round): the greeting
 * text (or custom welcome), the connection line when not live,
 * and the surrounding spacers — byte-identical in both layouts.
 * The connection line renders even when the greeting itself is
 * hidden, exactly as the old section did.
 */
/**
 * Greeting + connection line (2026-11-01 polish round): a transient
 * connection loss is NEVER a red banner and never a modal — it is a
 * small inline spinner row in this exact slot, in-flow (it pushes
 * content down, it overlays nothing), which clears on its own the
 * moment the connection recovers. The row distinguishes three cases:
 * a retry already in flight (spinner + "Reconnecting…" when cached
 * content is on screen, "Connecting…" on a cold first load, so the
 * subtitle stays stable across Error <-> Connecting cycles during
 * an outage), a hard auth failure (no spinner — the token was
 * rejected and no retry is scheduled — inline variant-colored text
 * that taps through to sign in again), and plain Disconnected
 * (variant-colored "Not connected", no spinner). Retries are
 * indefinite, so there is no "retries exhausted" state to present:
 * the spinner simply stays until the connection recovers or the
 * failure turns into an auth rejection.
 */
@Composable
private fun GreetingBlock(
    greeting: String,
    connection: ConnectionState,
    showGreeting: Boolean,
    hasCachedContent: Boolean,
    onSignInExpired: (() -> Unit)?,
) {
    if (showGreeting) {
        Spacer(Modifier.height(16.dp))
        // Mockup masthead (Gemini mockup round, 2026-11-01): the very
        // large bold "Haven" masthead, with the greeting as the smaller
        // gray subheading under it — the mockup's header, not a second
        // greeting. The connection line below is untouched (#83).
        Text("Haven", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(greeting, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (!connection.isLive()) {
        Spacer(Modifier.height(4.dp))
        when (connection) {
            is ConnectionState.Connecting, ConnectionState.Authenticating -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasCachedContent) "Reconnecting…" else "Connecting to your home…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
            is ConnectionState.Error -> if (connection.retrying) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasCachedContent) "Reconnecting…" else "Connecting…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Text(
                    "Sign-in expired — tap to sign in again",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = if (onSignInExpired != null) Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) { onSignInExpired() } else Modifier,
                )
            }
            else -> Text(
                "Not connected",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

/**
 * Compact Home weather card (2026-10-30 verdict round): one row —
 * badge, name, condition + temperature inline ("Sunny · 72°") —
 * about 88dp tall, roughly a quarter of the old arc card's height.
 * The badge is 56dp at the row's start with a 16dp gap, exactly
 * like every other card header, so the traveling header's
 * badgeRectInRow derivation lands on the real badge with no size
 * or position pop (the hero rect is the padded row's INNER rect:
 * padding sits outside the reported bounds). The row reports its
 * hero bounds and hides while traveling; the card reports its
 * card bounds for the sheet morph. The state line is
 * dialogHeaderState — the same string the traveler and the dialog
 * header show — and the badge/name/state hold the open-time
 * snapshot while a morph is open, like cards and tiles.
 */
@Composable
private fun HomeWeatherCard(weather: HaEntity, vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val accent = accentFor(weather.entityId)
    val snap = vm.travelSnapshots[weather.entityId]
    val display = snap ?: weather
    // Round 8: theme-aware condition tint; the state line is floored
    // to 4.5:1 against this card's own container color.
    val homeWeatherContainer = lerp(
        MaterialTheme.colorScheme.surfaceContainer,
        weatherConditionColorThemed(display.state, isDarkBackdrop(MaterialTheme.colorScheme.background)),
        0.20f,
    )
    Card(
        shape = generalCardShape(0),
        colors = CardDefaults.cardColors(
            containerColor = homeWeatherContainer,
        ),
        modifier = Modifier.fillMaxWidth()
            .reportCardBounds(weather.entityId)
            .reportCardShape(weather.entityId, generalCardShape(0))
            .reportCardColor(weather.entityId, homeWeatherContainer)
            .hideWhenMorphSource(weather.entityId),
    ) {
        Row(
            Modifier.fillMaxWidth()
                // Round 21, item 5: the long-press sheet is gone;
                // the weather card is not an Arrange-able grid
                // card, so it has no long-press action anymore —
                // its settings live in the popup (tap → weather
                // page → menu rows at the bottom).
                .clickable { navigate(Screen.EntityDetail(weather.entityId)) }
                .padding(16.dp)
                .reportHeroBounds(weather.entityId),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Round 6 badge-only hide: the row reports the hero
            // bounds, only this badge hides while traveling.
            DomainBadge(display, domainIcon(display), accent, Modifier.size(56.dp).hideHeroWhenMorphSource(weather.entityId))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(display.friendlyName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(display.dialogHeaderState(), fontSize = 13.sp, color = legibleSecondaryOn(MaterialTheme.colorScheme, homeWeatherContainer), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    dynamicColor: Boolean,
    onDynamicColor: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    navigate: (Screen) -> Unit,
) {
    val connection by vm.connection.collectAsState()
    // Feedback round 16, item 7: the connection switcher dialog.
    var connectionDialog by remember { mutableStateOf(false) }
    val updateStates by vm.states.collectAsState()
    val updateSummary = updatesSummary(updateStates)
    // Koda Settings menu DNA: dark rounded rows with shaped icon badges,
    // ALL-CAPS muted section headers, and bold title + muted subtitle
    // rows with chevrons — on Haven's dynamic colors, not Koda's palette.
    // The explanatory texts the old screen showed under each heading now
    // live as the rows' subtitles, so nothing is lost in the restyle.
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)
            // Feedback round 18, item 6: opaque root so sub-screen
            // transitions never overlap transparent text.
            .background(MaterialTheme.colorScheme.background),
        // Feedback round 16, item 8: the last row (Instance
        // tile) must draw fully — the trailing reserve grows
        // 16dp -> 32dp so the final row clears the bottom edge.
        contentPadding = PaddingValues(bottom = 32.dp),
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
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "appearance-dynamic-color",
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.Palette,
                    title = "Dynamic color",
                    subtitle = "Follow your wallpaper colors instead of Haven's fixed sage and lime palette. Off by default.",
                    onClick = null,
                    trailing = { Switch(checked = dynamicColor, onCheckedChange = onDynamicColor) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Stage E §9: condition badges — a dot, never a count.
            // Device badges are the tiles' own state badges (always on:
            // they carry tap actions, so they are not a preference).
            KodaMenuSection("Badges")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "badges-nav",
                    badgeKind = ExpressiveKind.FLOWER,
                    icon = Icons.Filled.Visibility,
                    title = "Navigation badges",
                    subtitle = "Dot on the Home tab when a lock is unlocked, a door or window is open, or the alarm is triggered.",
                    onClick = null,
                    trailing = { Switch(checked = vm.badgeNav, onCheckedChange = { vm.badgeNav = it }) },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "badges-room",
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Home,
                    title = "Room badges",
                    subtitle = "Dot on a room heading when that room has an unlocked lock, open door or window, or triggered alarm.",
                    onClick = null,
                    trailing = { Switch(checked = vm.badgeRoom, onCheckedChange = { vm.badgeRoom = it }) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Connection")
            KodaMenuGroup {
                // Feedback round 16, item 7: the Instance row is
                // tappable and shows Local vs Cloud for the
                // EFFECTIVE target (anything not a local address
                // reads Cloud). The subtitle recomputes when
                // connectionPrefsVersion bumps (switch/save), so
                // it always shows the live target, not a stale
                // stored URL.
                val connTick = vm.connectionPrefsVersion
                val (connLabel, connTarget) = remember(connTick) { vm.connectionDisplay() }
                KodaMenuRow(
                    badgeKey = "connection-instance",
                    badgeKind = ExpressiveKind.CLOUD,
                    icon = Icons.Filled.Wifi,
                    title = "Instance",
                    subtitle = connTarget?.let { "$connLabel · $it" } ?: "—",
                    onClick = { connectionDialog = true },
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
                    title = "Rooms & tabs",
                    subtitle = "Rooms (saved to Home Assistant itself) and custom tabs, in one place — pick whether each one pins as a room chip, a navigation tab, or stays hidden.",
                    onClick = { navigate(Screen.RoomsTabs) },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("Notifications")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "notif-garage",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Garage,
                    title = "Garage & door alerts",
                    subtitle = "Notify when a garage door or door opens",
                    onClick = null,
                    flat = true,
                    trailing = {
                        // Feedback round 16, item 3: default
                        // switch–text buffer.
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = vm.notifyGarage, onCheckedChange = { vm.notifyGarage = it })
                    },
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "notif-motion",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = "Motion alerts",
                    subtitle = "Notify when a motion sensor detects motion",
                    onClick = null,
                    flat = true,
                    trailing = {
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = vm.notifyMotion, onCheckedChange = { vm.notifyMotion = it })
                    },
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "notif-lights",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Lightbulb,
                    title = "Automated light alerts",
                    subtitle = "Notify when a light turns off by automation or externally — never for your own taps",
                    onClick = null,
                    flat = true,
                    trailing = {
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = vm.notifyLights, onCheckedChange = { vm.notifyLights = it })
                    },
                )
                KodaMenuDivider()
                NotificationPermissionRow()
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
                KodaMenuDivider()
                // Feedback round 15, item 5: per-domain defaults —
                // badge shape, card shape, and tile color for one
                // entity type (e.g. light), applying to every
                // entity of that domain unless it has its own
                // Card settings. Per-entity settings still win.
                KodaMenuRow(
                    badgeKey = "shapes-domain",
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.Tune,
                    title = "Domain defaults",
                    subtitle = "Default shapes and colors by entity type",
                    onClick = { vm.openDomainDefaults() },
                    flat = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Round 6: thermostat display-range override. The dial
            // axis normally follows the decade formula in
            // thermostatDisplayRange (66/67° setpoints → 50–80°);
            // these two values, when BOTH set, pin the axis instead
            // on the popout, the sheet dial, and detail (all three
            // read resolveThermostatDisplayRange). Display-range
            // only: live setpoints are never touched. State is kept
            // locally here and written straight to
            // vm.settingsStore, so SettingsScreen's signature is
            // unchanged.
            KodaMenuSection("Thermostat")
            var thermoMin by remember { mutableStateOf(vm.settingsStore.thermostatDisplayMin) }
            var thermoMax by remember { mutableStateOf(vm.settingsStore.thermostatDisplayMax) }
            val thermoClampedMin = thermoMin?.coerceIn(40f, 100f)
            val thermoClampedMax = thermoMax?.coerceIn(40f, 100f)
            val thermoOverrideText = if (thermoClampedMin != null && thermoClampedMax != null && thermoClampedMin < thermoClampedMax) {
                "${thermoClampedMin.toInt()}–${thermoClampedMax.toInt()}°"
            } else {
                null
            }
            // The Automatic range, when exactly one climate entity
            // is present, so the status line can name the effective
            // window; with zero or several climates the status stays
            // generic rather than guessing which thermostat it means.
            val thermoFormulaText: String? = run {
                val climates = updateStates.values.filter { it.domain == "climate" }
                if (climates.size != 1) return@run null
                val climate = climates.first()
                val cMin = climate.attrDouble("min_temp")?.toFloat() ?: 10f
                val cMax = climate.attrDouble("max_temp")?.toFloat() ?: 30f
                val low = climate.attrDouble("target_temp_low")?.toFloat()
                val high = climate.attrDouble("target_temp_high")?.toFloat()
                val setpoints = if (low != null && high != null) {
                    listOf(low, high)
                } else {
                    listOfNotNull(climate.attrDouble("temperature")?.toFloat())
                }
                val r = thermostatDisplayRange(cMin, cMax, setpoints)
                "${r.start.toInt()}–${r.endInclusive.toInt()}°"
            }
            val thermoStatus = when {
                thermoOverrideText != null ->
                    "Override ($thermoOverrideText) — pinned on the thermostat dial, sheet, and detail, whatever the setpoints are."
                thermoMin != null && thermoMax != null ->
                    "Override values do not form a valid range (minimum must be below maximum) — using Automatic" +
                        (thermoFormulaText?.let { " ($it from setpoints)" } ?: "") + "."
                thermoMin != null || thermoMax != null ->
                    "Only one override value is set — Automatic governs until both minimum and maximum are set" +
                        (thermoFormulaText?.let { " ($it from setpoints)" } ?: "") + "."
                thermoFormulaText != null ->
                    "Automatic ($thermoFormulaText from setpoints) — the dial range follows your setpoints."
                else ->
                    "Automatic — the dial range follows your setpoints."
            }
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "thermostat-range",
                    badgeKind = ExpressiveKind.SUNNY,
                    icon = Icons.Filled.Thermostat,
                    title = "Dial range",
                    subtitle = thermoStatus,
                    onClick = null,
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "thermostat-min",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.Thermostat,
                    title = "Display minimum",
                    subtitle = "Low end of the dial axis. Tap −/+ to set an override (starts at 50°); 1° steps, clamped to 40–100°.",
                    onClick = null,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val base = thermoMin
                                val next = if (base == null) 50f else (base - 1f).coerceIn(40f, 100f)
                                thermoMin = next
                                vm.settingsStore.thermostatDisplayMin = next
                            }) { Text("−") }
                            Text(
                                thermoMin?.let { "${it.toInt()}°" } ?: "Automatic",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            TextButton(onClick = {
                                val base = thermoMin
                                val next = if (base == null) 50f else (base + 1f).coerceIn(40f, 100f)
                                thermoMin = next
                                vm.settingsStore.thermostatDisplayMin = next
                            }) { Text("+") }
                        }
                    },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "thermostat-max",
                    badgeKind = ExpressiveKind.WAVE,
                    icon = Icons.Filled.Thermostat,
                    title = "Display maximum",
                    subtitle = "High end of the dial axis. Tap −/+ to set an override (starts at 80°); 1° steps, clamped to 40–100°.",
                    onClick = null,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val base = thermoMax
                                val next = if (base == null) 80f else (base - 1f).coerceIn(40f, 100f)
                                thermoMax = next
                                vm.settingsStore.thermostatDisplayMax = next
                            }) { Text("−") }
                            Text(
                                thermoMax?.let { "${it.toInt()}°" } ?: "Automatic",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            TextButton(onClick = {
                                val base = thermoMax
                                val next = if (base == null) 80f else (base + 1f).coerceIn(40f, 100f)
                                thermoMax = next
                                vm.settingsStore.thermostatDisplayMax = next
                            }) { Text("+") }
                        }
                    },
                    flat = true,
                )
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "thermostat-reset",
                    badgeKind = ExpressiveKind.CLOUD,
                    icon = Icons.Filled.Thermostat,
                    title = "Reset to Automatic",
                    subtitle = "Clear the minimum/maximum override; the dial range follows your setpoints again.",
                    onClick = {
                        thermoMin = null
                        thermoMax = null
                        vm.settingsStore.thermostatDisplayMin = null
                        vm.settingsStore.thermostatDisplayMax = null
                    },
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
                KodaMenuDivider()
                KodaMenuRow(
                    badgeKey = "browse-usertabs",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.GridView,
                    title = "User tabs",
                    subtitle = "Your custom tabs",
                    onClick = { navigate(Screen.UserTabs) },
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
            Spacer(Modifier.height(16.dp))
            KodaMenuSection("System")
            KodaMenuGroup {
                KodaMenuRow(
                    badgeKey = "system-updates",
                    badgeKind = ExpressiveKind.BUN,
                    icon = Icons.Filled.SystemUpdate,
                    title = "System Updates",
                    subtitle = if (updateSummary.available == 0) {
                        "Everything is up to date (${updateSummary.total} sources tracked)"
                    } else {
                        "${updateSummary.available} update${if (updateSummary.available == 1) "" else "s"} available"
                    },
                    onClick = { navigate(Screen.UpdatesPanel) },
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
    // Feedback round 16, item 7: the connection switcher.
    if (connectionDialog) {
        ConnectionDialog(vm, onDismiss = { connectionDialog = false })
    }
}

/**
 * Feedback round 16, item 7: the connection switcher dialog.
 * Mode + cloud URL save through [HavenViewModel.switchConnection],
 * which reconnects with the existing token (Nabu Casa remote UI
 * forwards to the same instance, so no re-login). The cloud URL
 * is pasted by the user: HA's cloud integration does not expose
 * the remote-UI URL through any API the app uses, so nothing is
 * guessed or hardcoded. AUTO keys on Wi-Fi transport (no location
 * permission involved): local on Wi-Fi, cloud elsewhere.
 */
@Composable
private fun ConnectionDialog(vm: HavenViewModel, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(vm.connectionMode) }
    var cloudUrl by remember { mutableStateOf(vm.connectionCloudUrl ?: "") }
    val (label, target) = vm.connectionDisplay()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection") },
        text = {
            Column {
                Text(
                    "Current: $label" + (target?.let { " · $it" } ?: ""),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                for ((m, title, sub) in listOf(
                    Triple(ConnectionMode.AUTO, "Automatic", "Local on Wi-Fi, cloud elsewhere"),
                    Triple(ConnectionMode.LOCAL, "Local only", "Always use the local address"),
                    Triple(ConnectionMode.CLOUD, "Cloud only", "Always use the Nabu Casa remote UI"),
                )) {
                    Row(
                        Modifier.fillMaxWidth().clickable { mode = m },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = cloudUrl,
                    onValueChange = { cloudUrl = it },
                    label = { Text("Cloud URL (Nabu Casa remote UI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Paste the remote-UI URL from your Home Assistant Cloud settings. Nothing is detected automatically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.switchConnection(mode, cloudUrl); onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            // One clean rounded rectangle (2026-11-01 polish round):
            // ONLY the outer corners are rounded — every quadrant
            // used to round all four corners, so the touching inner
            // edges curved and the center cross looked broken.
            // Touching edges are square now; the 1dp gutters read
            // as the split between quadrants. Visual-fidelity round
            // §2: outer corners are 28dp (the mockup's hero).
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    CameraQuadrant(vm, cameras.getOrNull(0), Modifier.weight(1f), RoundedCornerShape(topStart = 28.dp))
                    CameraQuadrant(vm, cameras.getOrNull(1), Modifier.weight(1f), RoundedCornerShape(topEnd = 28.dp))
                }
                Row(Modifier.weight(1f)) {
                    CameraQuadrant(vm, cameras.getOrNull(2), Modifier.weight(1f), RoundedCornerShape(bottomStart = 28.dp))
                    CameraQuadrant(vm, cameras.getOrNull(3), Modifier.weight(1f), RoundedCornerShape(bottomEnd = 28.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CameraQuadrant(vm: HavenViewModel, cameraId: String?, modifier: Modifier = Modifier, corners: RoundedCornerShape = RoundedCornerShape(0.dp)) {
    Box(
        modifier.padding(1.dp).clip(corners)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // Morph source (2026-11-01 round 1): the quadrant
            // reports its root rect so the camera panel morphs
            // from — and, crucially, back to — this exact
            // quadrant on both postures. Before this, no quadrant
            // ever reported, so the panel fell back to the
            // full-width shifted rect and its close landed near
            // the top of the screen instead of on the quadrant.
            // It also hides while its panel is open, so the
            // snapshot never doubles the video.
            .then(if (cameraId != null) Modifier.reportCardBounds(cameraId).reportCardShape(cameraId, corners).hideWhenMorphSource(cameraId) else Modifier)
            .then(if (cameraId != null) Modifier.clickable { vm.openCameraDialog(cameraId) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraId == null) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
        } else {
            // Feedback round 18, item 7: per-entity flows instead
            // of the whole states map — a state change on any
            // other entity no longer recomposes this quadrant.
            // The front-door trio feeds the SHARED resolver (same
            // fallback/unavailable behavior as before); the motion
            // sensor arrives on its own flow (see motionSensorFlow).
            // Plain .value reads (not by-delegates) so the entity
            // smart-casts in the overlay block below.
            val entity = vm.entityFlow(cameraId).collectAsState().value
            val ringMqtt = vm.entityFlow(FRONT_DOOR_RING_MQTT_CAMERA_ID).collectAsState().value
            val liveView = vm.entityFlow(FRONT_DOOR_LIVE_VIEW_ID).collectAsState().value
            val snapshotEntity = vm.entityFlow(FRONT_DOOR_SNAPSHOT_ID).collectAsState().value
            val motionSensor = vm.motionSensorFlow(cameraId).collectAsState().value
            // Front Door live probe (Round 2): the Front Door
            // quadrant's static thumbnail comes from the dedicated
            // snapshot camera (camera.front_door_snapshot, "Ring
            // Doorbell") when it exists, instead of the live-view
            // camera's proxy — a still thumbnail for a still card.
            // The tap target stays the live-view panel, and the
            // morph-source / hide keys stay on cameraId, so the
            // panel morph is untouched. Falls back to cameraId when
            // the snapshot entity is absent.
            // Camera fallback round: when BOTH front-door sources
            // are down, the quadrant shows an honest unavailable
            // state instead of a stale cached snapshot. The tap
            // target, morph keys, and cameraId stay on live_view,
            // so the panel (with its own unavailable state) still
            // opens and the dialog routing is untouched.
            val frontDoorStates = frontDoorResolverStates(ringMqtt, liveView)
            val frontDoorDown = cameraId == FRONT_DOOR_LIVE_VIEW_ID &&
                resolveFrontDoorCameraSource(frontDoorStates) == null
            val thumbnailId = if (cameraId == FRONT_DOOR_LIVE_VIEW_ID &&
                snapshotEntity != null
            ) FRONT_DOOR_SNAPSHOT_ID else cameraId
            val snap = vm.snapshotFor(thumbnailId)
            // Cache-first, fetch deferred (2026-11-01 polish round):
            // when a cached image exists it is already on screen,
            // so the fresh fetch waits ~1s instead of racing app
            // open (the old immediate fetch left the top-right
            // quadrant blank for ~5s while four fetches queued).
            // With no cache there is nothing to protect, so the
            // fetch starts at once. refreshSnapshot itself only
            // swaps in a finished image and only when the cached
            // one is stale, so a failed or in-flight fetch never
            // blanks a quadrant.
            LaunchedEffect(thumbnailId) {
                if (vm.snapshotFor(thumbnailId) != null) kotlinx.coroutines.delay(1000)
                vm.refreshSnapshot(thumbnailId)
            }
            if (frontDoorDown) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Unavailable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (snap != null) {
                Image(snap.asImageBitmap(), contentDescription = entity?.friendlyName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
            // Mockup-language overlay (Gemini mockup round,
            // 2026-11-01): the mockup's Front Door card carries its
            // title over the photo with a LIVE badge — each quadrant
            // gets the same treatment: status pill top-left, camera
            // name bottom-left over a bottom scrim. The pill is
            // honest by construction: LIVE only for genuinely live
            // native feeds, Recording for Ring (never a LIVE badge
            // on a recording — standing honesty law). Feedback
            // round 19, item 4, refines the Front Door quadrant:
            // its badge follows the resolved live source (69a1
            // up = LIVE, fallback = Recording), not the brand —
            // see showLive below. The name uses
            // the display name, so the Ring quadrant never reads
            // "Front Door Live view". Expressive-menus round,
            // item 7: a Motion pill joins the status pill ONLY
            // while a motion binary_sensor on the camera's own
            // registry device reports "on" (see
            // motionSensorForCamera) — the mockup's "Motion
            // detected" was demo data, but the Ring doorbell's
            // real motion sensor now backs the same chip. No
            // linked sensor means no chip, never a guess.
            // Camera fallback round: no overlay on the
            // unavailable state — a "Recording" pill over an
            // "Unavailable" quadrant would contradict it.
            if (entity != null && !frontDoorDown) {
                // Feedback round 19, item 4: the Front Door
                // quadrant's badge follows the LIVE source, not
                // the camera brand — a recovered 69a1 earns LIVE
                // here; the fallback feed stays Recording even
                // while it plays (the live source is what is
                // down). Every other quadrant keeps the existing
                // brand rule (LIVE for native feeds, Recording
                // for Ring).
                val showLive = if (cameraId == FRONT_DOOR_LIVE_VIEW_ID) {
                    // Round 21, item 3: the quadrant reads LIVE
                    // for the fallback once the RTSP fallback has
                    // proven itself streaming this session
                    // (vm.frontDoorRtspProven); before any proof
                    // it stays Recording.
                    frontDoorQuadrantShowsLive(resolveFrontDoorCameraSource(frontDoorStates), false, rtspProven = vm.frontDoorRtspProven)
                } else {
                    !isRingCamera(entity)
                }
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().height(56.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                ),
                            ),
                    )
                    Row(
                        Modifier.align(Alignment.TopStart).padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showLive) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(Color(0xFF4CAF50)),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Recording", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Motion pill (item 7): only while the linked
                    // sensor is actually "on"; absent or off means
                    // no pill at all.
                    if (motionSensor?.state == "on") {
                        Spacer(Modifier.width(6.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Motion", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        displayCameraName(entity),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Feedback round 19, item 5: the unified Rooms & tabs manager.
 * REPLACES RoomManagerScreen + TabManagerScreen (both deleted):
 * one Settings entry, "Rooms & tabs", with the rooms section
 * first (create/rename/delete/assign, writing through to real
 * Home Assistant exactly as before) and the tabs section second
 * (navigation switches, pill style, reset, tab editors, add tab,
 * exactly as before). Every room and every tab carries a
 * placement selector — Room chip (Home chip row), Nav tab
 * (bar/rail), or Hidden (neither surface, still manageable
 * here). Home and Settings stay locked by construction (they
 * have no selector); the 2-cap on bar extras counts pinned tabs
 * plus nav-tab rooms together, and the 6-total holds.
 */
@Composable
fun RoomsTabsScreen(vm: HavenViewModel, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val states by vm.states.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    var addQuery by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var deleteArmed by remember { mutableStateOf<String?>(null) }
    var addFor by remember { mutableStateOf<String?>(null) }
    var assignFor by remember { mutableStateOf<String?>(null) }
    var newTabTitle by remember { mutableStateOf("") }
    // (tabId, cardIndex, yaml) of the card currently in the YAML editor, if any.
    var editingCard by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    fun done(err: String?, ok: String) {
        if (err != null) { message = err; messageIsError = true }
        else { message = ok; messageIsError = false }
    }

    // Status-bar inset: same missing-inset class as the clips screen
    // (this root Column was the only other DetailTopBar screen
    // without one — every sibling pads its root).
    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Rooms & tabs", onBack)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Text(
                    "Rooms live in Home Assistant — changes here appear in HA itself. Tabs are local to Haven. " +
                        "Each room or tab pins to exactly one surface: the Home chip row, the navigation bar, or neither.",
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
                        Text(deviceCountLabel(members.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        OutlinedButton(onClick = {
                            if (addFor == area.id) addFor = null else { addFor = area.id; addQuery = "" }
                        }) {
                            Text(if (addFor == area.id) "Close" else "Add entity…")
                        }
                    }
                    // Customization round, item 3: the add list
                    // is SEARCHABLE (was an unfiltered dropdown) and
                    // shows each candidate's device name when known,
                    // so entities and devices are both findable here.
                    // Blank query shows the first 25; a query shows
                    // all name/id/device matches.
                    if (addFor == area.id) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = addQuery,
                            onValueChange = { addQuery = it },
                            label = { Text("Search entities or devices") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        val q = addQuery.trim().lowercase()
                        val allCandidates = states.values
                            .filter { entityAreas[it.entityId] != area.id }
                            .sortedBy { it.friendlyName }
                        val candidates = if (q.isEmpty()) allCandidates.take(25) else allCandidates.filter { candidate ->
                            candidate.friendlyName.lowercase().contains(q) ||
                                candidate.entityId.lowercase().contains(q) ||
                                (deviceInfo[entityDevices[candidate.entityId]]?.name?.lowercase()?.contains(q) == true)
                        }
                        if (q.isEmpty() && allCandidates.size > 25) {
                            Text("Showing 25 of ${allCandidates.size} — type to search all.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (candidates.isEmpty()) {
                            Text("No entities match \"$addQuery\".", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        candidates.forEach { candidate ->
                            // Feedback round 17, item 4: the
                            // subtitle is omitted when the
                            // registry name is null, blank, or
                            // the literal string "null" — the
                            // 2026-11-04 runs rendered "null"
                            // verbatim under entity names.
                            val deviceName = cleanDeviceName(deviceInfo[entityDevices[candidate.entityId]]?.name)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(candidate.friendlyName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (deviceName != null) {
                                        Text(deviceName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                OutlinedButton(onClick = {
                                    addFor = null
                                    vm.assignEntityToRoom(candidate.entityId, area.id) { err ->
                                        done(err, "${candidate.friendlyName} moved to ${area.name}")
                                    }
                                }) { Text("Add") }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PlacementSelector(
                        selected = vm.roomPlacement(area.id),
                        barDisabled = !vm.canPinToBar(vm.roomPlacement(area.id) == Placement.NAV_TAB),
                        onSelect = { vm.setRoomPlacement(area.id, it) },
                    )
                }
            }
            item {
                // No-room here mirrors Home's room sections
                // (2026-10-30 verdict round): system entities
                // (zone/sun/update) are not room-assignable devices
                // and never appear, so this count agrees with Home.
                // No room has no area id, so it has no placement:
                // it is always a Home chip, never a bar tab.
                val unassigned = states.values
                    .filter { entityAreas[it.entityId] == null && isHomeRoomEntity(it) }
                    .sortedBy { it.friendlyName }
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Text("No room", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(deviceCountLabel(unassigned.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            item {
                Text("Tabs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Home is always first and Settings is always last — everything else here is yours. " +
                        "Reorder tabs to change their order in User Tabs.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                // Stage C: navigation customization lives here (not a new
                // settings section — Stage G owns the settings IA reorg).
                // Hidden tabs stay listed below with a selector, so a
                // hidden tab can always come back; Settings can never be
                // hidden or removed, by construction in pillBarItems.
                Text("Navigation", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                // Feedback round 15, item 3: Scenes and Energy are
                // hideable from the bar/rail; Home and Settings are
                // locked and have no switch. A hidden built-in tab
                // stays reachable from Settings -> Browse.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Scenes on bar", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    // Feedback round 16, item 3: default switch–text buffer.
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = vm.navShowScenes, onCheckedChange = { vm.navShowScenes = it })
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Energy on bar", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    // Feedback round 16, item 3: default switch–text buffer.
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = vm.navShowEnergy, onCheckedChange = { vm.navShowEnergy = it })
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Up to two extras can sit on the bar — pinned tabs and rooms-as-tabs share the two slots. " +
                        "Use the placement selector below each room or tab; Home and Settings always stay on the bar.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pill style", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    var styleExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { styleExpanded = true }) {
                            Text(if (vm.navStyle == NavStyle.COMPACT) "Compact" else "Floating")
                        }
                        DropdownMenu(
                            expanded = styleExpanded,
                            onDismissRequest = { styleExpanded = false },
                        ) {
                            DropdownMenuItem(text = { Text("Floating") }, onClick = {
                                styleExpanded = false
                                vm.navStyle = NavStyle.FLOATING
                            })
                            DropdownMenuItem(text = { Text("Compact") }, onClick = {
                                styleExpanded = false
                                vm.navStyle = NavStyle.COMPACT
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Reset navigation restores the floating pill, automatic icons, shows all tabs, " +
                            "puts Scenes and Energy back on the bar, and clears all pins and chip placements. " +
                            "Your tabs are never deleted by a reset.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.resetNavigation() }) { Text("Reset") }
                }
                Spacer(Modifier.height(16.dp))
            }
            for (tab in vm.userTabs) {
                item {
                    TabEditorCard(vm, tab, states, onEditYaml = { index, yaml ->
                        editingCard = Triple(tab.id, index, yaml)
                    })
                    Spacer(Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            PlacementSelector(
                                selected = tabPlacementFor(tab.pinned, tab.chip, tab.hidden),
                                barDisabled = !vm.canPinToBar(tab.pinned),
                                onSelect = { vm.setTabPlacement(tab.id, it) },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            item {
                Text("Add a tab", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTabTitle,
                        onValueChange = { newTabTitle = it },
                        label = { Text("Tab title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val id = vm.addTab(newTabTitle)
                        newTabTitle = ""
                        navigate(Screen.UserTab(id))
                    }) { Text("Add") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    editingCard?.let { (tabId, index, yaml) ->
        CardYamlEditor(
            initialYaml = yaml,
            onDismiss = { editingCard = null },
            onSave = { newYaml ->
                vm.updateCardYaml(tabId, index, newYaml)
                editingCard = null
            },
        )
    }
}

/**
 * Feedback round 19, item 5: the placement selector — three
 * buttons splitting the row, the selected one filled. One
 * control for one placement, so the bar, the chips, and this
 * screen can never disagree about where a group lives. The Nav
 * tab option disables at the bar cap (with the reason line);
 * a group already on the bar is never disabled out from under
 * itself (see canPinToBar's alreadyOnBar rule).
 */
@Composable
private fun PlacementSelector(
    selected: Placement,
    barDisabled: Boolean,
    onSelect: (Placement) -> Unit,
) {
    Column {
        Text("Pin as", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlacementOption("Room chip", selected == Placement.ROOM_CHIP) { onSelect(Placement.ROOM_CHIP) }
            PlacementOption("Nav tab", selected == Placement.NAV_TAB, enabled = !barDisabled) { onSelect(Placement.NAV_TAB) }
            PlacementOption("Hidden", selected == Placement.HIDDEN) { onSelect(Placement.HIDDEN) }
        }
        if (barDisabled && selected != Placement.NAV_TAB) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Bar full — at most two tabs and rooms-as-tabs combined.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.PlacementOption(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(label, maxLines = 1)
        }
    }
}

/**
 * Feedback round 19, item 6: the notification permission row.
 * The toggles above gate posting per category; this row reports
 * the Android 13+ runtime permission itself, with a button to
 * system settings when it is missing (the launch-time ask runs
 * once in MainActivity — this is the re-grant path). The status
 * is read on each composition; returning from system settings
 * refreshes it on the next visit.
 */
@Composable
private fun NotificationPermissionRow() {
    val context = LocalContext.current
    val granted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    KodaMenuRow(
        badgeKey = "notif-permission",
        badgeKind = ExpressiveKind.BUN,
        icon = Icons.Filled.Notifications,
        title = "Notification permission",
        subtitle = if (granted) "Granted — alerts can post." else "Not granted — alerts are held until you allow them.",
        onClick = null,
        flat = true,
        trailing = {
            if (!granted) {
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) { Text("Allow") }
            }
        },
    )
}
