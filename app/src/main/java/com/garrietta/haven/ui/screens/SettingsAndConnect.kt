package com.garrietta.haven.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.garrietta.haven.AppGraph
import com.garrietta.haven.core.auth.normalizeBaseUrl
import com.garrietta.haven.core.net.ConnState
import com.garrietta.haven.ui.components.HavenChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

@Composable
fun SettingsScreen(graph: AppGraph, padding: PaddingValues) {
    val conn by graph.connection.state.collectAsState()
    val config by graph.store.config.collectAsState()
    val theme by graph.prefs.themeMode.collectAsState()
    val dynamic by graph.prefs.dynamicColor.collectAsState()
    val creds = graph.auth.credentials
    var confirmSignOut by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)) {
        ScreenTitle("Settings")
        Group("Connection") {
            Row2(Icons.Filled.Router, "Status", connText(conn))
            Row2(Icons.Filled.Home, "Home Assistant", listOf(config.locationName, config.version).filter { it.isNotBlank() }.joinToString(" · "))
            Row2(Icons.Filled.Wifi, "Local URL", creds?.localUrl ?: "Not set")
            Row2(Icons.Filled.Cloud, "Remote URL", creds?.remoteUrl ?: "Not set")
            Row2(Icons.Filled.Key, "Sign-in", if (creds?.isLongLived == true) "Long-lived token" else "Home Assistant account")
            Row2(Icons.Filled.Refresh, "Reconnect now", null) { graph.connection.reconnectNow() }
        }
        Group("Appearance") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (k, l) -> HavenChip(l, selected = theme == k) { graph.prefs.setThemeMode(k) } }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text("Wallpaper colors", style = MaterialTheme.typography.bodyLarge)
                    Text("Use Material You dynamic color instead of Haven's palette", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(dynamic, { graph.prefs.setDynamicColor(it) })
            }
        }
        Group("Cameras") {
            Row2(Icons.Filled.Videocam, "Live video", if (graph.cameras.webRtcAllowed) "WebRTC, then HLS, then MJPEG" else "HLS (WebRTC disabled after crashes on this version)")
        }
        Group("Account") {
            Row2(Icons.Filled.Logout, if (confirmSignOut) "Tap again to sign out" else "Sign out", null) {
                if (confirmSignOut) graph.auth.signOut() else confirmSignOut = true
            }
        }
        Text(
            "Haven 2.0 · native Home Assistant client",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

fun connText(c: ConnState): String = when (c) {
    is ConnState.Connected -> if (c.isLocal) "Connected · local" else "Connected · remote"
    is ConnState.Connecting -> "Connecting…"
    is ConnState.Waiting -> "Retrying in ${c.retryInMs / 1000}s"
    ConnState.Offline -> "Offline"
    ConnState.AuthRejected -> "Sign-in rejected"
    ConnState.Idle -> "Not connected"
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
    Column(Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 6.dp)) { content() }
}

@Composable
private fun Row2(icon: ImageVector, title: String, value: String?, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** First run: server URL → sign in with Home Assistant (OAuth) or a long-lived token. */
@Composable
fun ConnectScreen(graph: AppGraph) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var useToken by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Icon(Icons.Filled.Home, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Welcome to Haven", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Connect to your Home Assistant.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                url, { url = it; error = null }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Server address") }, placeholder = { Text("homeassistant.local:8123") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), shape = RoundedCornerShape(20.dp),
            )
            if (useToken) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    token, { token = it; error = null }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Long-lived access token") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(20.dp),
                )
            }
            error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val base = normalizeBaseUrl(url) ?: run { error = "Enter your Home Assistant address"; return@Button }
                    busy = true
                    scope.launch {
                        val reachable = withContext(Dispatchers.IO) {
                            runCatching { graph.http.newCall(Request.Builder().url("$base/api/").build()).execute().use { it.code == 401 || it.isSuccessful } }.getOrDefault(false)
                        }
                        if (!reachable) { busy = false; error = "Couldn't reach $base"; return@launch }
                        if (useToken) {
                            val ok = withContext(Dispatchers.IO) {
                                runCatching {
                                    graph.http.newCall(Request.Builder().url("$base/api/").header("Authorization", "Bearer ${token.trim()}").build()).execute().use { it.isSuccessful }
                                }.getOrDefault(false)
                            }
                            busy = false
                            if (ok) graph.auth.signInWithToken(base, token) else error = "That token was rejected"
                        } else {
                            busy = false
                            context.startActivity(Intent(Intent.ACTION_VIEW, graph.auth.beginOAuth(base)))
                        }
                    }
                },
                enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (useToken) "Connect" else "Sign in with Home Assistant")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { useToken = !useToken; error = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (useToken) "Sign in with my account instead" else "Use a long-lived token")
            }
        }
    }
}
