package com.garrietta.haven.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.DiscoverySource
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
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
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
                        Text(instance.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..4 -> "Up late"
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val entities = states.values.sortedBy { it.friendlyName }
    val weather = entities.firstOrNull { it.domain == "weather" }
    val homePeople = entities.filter { (it.domain == "person" || it.domain == "device_tracker") && it.state == "home" }
    val nowPlaying = entities.filter { it.domain == "media_player" && it.state == "playing" }
    val lightsOn = entities.filter { it.domain == "light" && it.state == "on" }
    val alerts = entities.filter {
        it.domain == "binary_sensor" && it.state == "on" &&
            (it.attrString("device_class") in listOf("problem", "safety", "smoke", "co", "moisture", "tamper", "gas"))
    }
    // Sections as LazyListScope blocks so the narrow and wide layouts share
    // them exactly: one column folded, two side-by-side columns unfolded.
    val greetingSection: LazyListScope.() -> Unit = {
        item {
            Spacer(Modifier.height(16.dp))
            Text(greeting, fontSize = 30.sp, fontWeight = FontWeight.Bold)
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
        if (weather != null) {
            item {
                Text("Weather", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                EntityControlCard(weather, vm) { vm.openPopup(it) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    val homeSection: LazyListScope.() -> Unit = {
        if (homePeople.isNotEmpty()) {
            item {
                Text("Who's home", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(homePeople.joinToString(", ") { it.friendlyName }, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    val playingSection: LazyListScope.() -> Unit = {
        if (nowPlaying.isNotEmpty()) {
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
        if (lightsOn.isNotEmpty()) {
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
        if (alerts.isNotEmpty()) {
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
            LazyColumn(Modifier.weight(1f).padding(end = 8.dp)) {
                greetingSection()
                weatherSection()
                homeSection()
                playingSection()
                item { Spacer(Modifier.height(16.dp)) }
            }
            LazyColumn(Modifier.weight(1f).padding(start = 8.dp)) {
                lightsSection()
                alertsSection()
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
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
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        item {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Appearance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((value, label) in listOf("system" to "System", "light" to "Light", "dark" to "Dark")) {
                    FilterChip(
                        selected = themeMode == value,
                        onClick = { onThemeMode(value) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reduce motion", modifier = Modifier.weight(1f))
                Switch(checked = reduceMotion, onCheckedChange = onReduceMotion)
            }
            Spacer(Modifier.height(24.dp))
            Text("Connection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Instance: ${vm.tokenStore.instanceUrl ?: "—"}", fontSize = 14.sp)
            Text(
                "Status: " + when (connection) {
                    is ConnectionState.Connected -> "Connected"
                    is ConnectionState.Connecting -> "Connecting…"
                    is ConnectionState.Authenticating -> "Signing in…"
                    is ConnectionState.Error -> (connection as ConnectionState.Error).message
                    else -> "Disconnected"
                },
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(24.dp))
            Text("Tabs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "The pill bar is your tabs: Settings is always on, and the rest are yours to create, rename, and reorder.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { navigate(Screen.TabManager) }) { Text("Manage tabs") }
            Spacer(Modifier.height(24.dp))
            Text("Browse", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Today, Boards, Areas, and Search live here now that the pill bar holds your tabs.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navigate(Screen.Today) }) { Text("Today") }
                Button(onClick = { navigate(Screen.Dashboards) }) { Text("Boards") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navigate(Screen.Areas) }) { Text("Areas") }
                Button(onClick = { navigate(Screen.Search) }) { Text("Search") }
            }
            Spacer(Modifier.height(24.dp))
            Text("Create", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navigate(Screen.AutomationBuilder) }) { Text("Automation") }
                Button(onClick = { navigate(Screen.SceneBuilder) }) { Text("Scene") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navigate(Screen.ScriptBuilder) }) { Text("Script") }
                Button(onClick = { navigate(Screen.HelperBuilder) }) { Text("Helper") }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Guided builders: pick a trigger, a condition, and an action — no YAML, no web UI.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
