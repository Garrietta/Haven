package com.garrietta.haven.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.garrietta.haven.AppGraph
import com.garrietta.haven.core.auth.AuthState
import com.garrietta.haven.core.net.ConnState
import com.garrietta.haven.ui.camera.CameraViewer
import com.garrietta.haven.ui.components.LocalIsDark
import com.garrietta.haven.ui.components.PillBar
import com.garrietta.haven.ui.components.PillItem
import com.garrietta.haven.ui.components.PopupCard
import com.garrietta.haven.ui.controls.EntityControls
import com.garrietta.haven.ui.screens.Actions
import com.garrietta.haven.ui.screens.CamerasScreen
import com.garrietta.haven.ui.screens.ConnectScreen
import com.garrietta.haven.ui.screens.DevicesScreen
import com.garrietta.haven.ui.screens.HomeScreen
import com.garrietta.haven.ui.screens.RoomScreen
import com.garrietta.haven.ui.screens.RoomsScreen
import com.garrietta.haven.ui.screens.SettingsScreen
import com.garrietta.haven.ui.screens.connText
import com.garrietta.haven.ui.screens.isAlert
import com.garrietta.haven.ui.screens.visibleEntities
import com.garrietta.haven.ui.theme.HavenTheme
import com.garrietta.haven.ui.theme.resolveHavenIsDark

/** A pushed screen above the tabs. */
sealed interface Sub {
    data class Room(val areaId: String) : Sub
    data class Camera(val entityId: String) : Sub
}

@Composable
fun HavenRoot(graph: AppGraph) {
    val themeMode by graph.prefs.themeMode.collectAsState()
    val dynamic by graph.prefs.dynamicColor.collectAsState()
    val dark = resolveHavenIsDark(themeMode, isSystemInDarkTheme())
    HavenTheme(darkTheme = dark, dynamicColor = dynamic) {
        CompositionLocalProvider(LocalIsDark provides dark) {
            StatusBarIcons(dark)
            val auth by graph.auth.state.collectAsState()
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (auth is AuthState.SignedIn) AppShell(graph) else ConnectScreen(graph)
            }
        }
    }
}

private val TABS = listOf(
    PillItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    PillItem("rooms", "Rooms", Icons.Filled.MeetingRoom, Icons.Outlined.MeetingRoom),
    PillItem("cameras", "Cameras", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    PillItem("devices", "Devices", Icons.Filled.DevicesOther, Icons.Outlined.DevicesOther),
    PillItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
private fun AppShell(graph: AppGraph) {
    var tab by rememberSaveable { mutableStateOf("home") }
    val stack = remember { mutableStateListOf<Sub>() }
    var popup by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val conn by graph.connection.state.collectAsState()
    val states by graph.store.entities.collectAsState()
    val entries by graph.store.entries.collectAsState()
    val alert = remember(states, entries) { visibleEntities(states, entries).any { isAlert(it) } }

    LaunchedEffect(Unit) { graph.store.errors.collect { snackbar.showSnackbar(it) } }

    val actions = remember {
        Actions(
            openControls = { popup = it },
            openCamera = { stack.add(Sub.Camera(it)) },
            openRoom = { stack.add(Sub.Room(it)) },
        )
    }
    BackHandler(enabled = popup != null) { popup = null }
    BackHandler(enabled = popup == null && stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = popup == null && stack.isEmpty() && tab != "home") { tab = "home" }

    val top = stack.lastOrNull()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val contentPadding = if (wide) {
            PaddingValues(start = 112.dp, end = 20.dp, top = status + 12.dp, bottom = nav + 24.dp)
        } else {
            PaddingValues(start = 16.dp, end = 16.dp, top = status + 12.dp, bottom = nav + 104.dp)
        }
        val items = TABS.map { if (it.key == "home") it.copy(badged = alert) else it }

        AnimatedContent(
            targetState = top ?: tab,
            transitionSpec = { (fadeIn(tween(220)) + slideInVertically { it / 24 }) togetherWith fadeOut(tween(160)) },
            label = "screen",
        ) { target ->
            when (target) {
                is Sub.Room -> RoomScreen(graph, target.areaId, actions, contentPadding) { stack.removeAt(stack.lastIndex) }
                is Sub.Camera -> CameraViewer(graph, target.entityId) { stack.removeAt(stack.lastIndex) }
                "rooms" -> RoomsScreen(graph, actions, contentPadding)
                "cameras" -> CamerasScreen(graph, actions, contentPadding)
                "devices" -> DevicesScreen(graph, actions, contentPadding)
                "settings" -> SettingsScreen(graph, contentPadding)
                else -> HomeScreen(graph, actions, contentPadding)
            }
        }

        val showBar = top !is Sub.Camera
        AnimatedVisibility(
            showBar,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = if (wide) Modifier.align(Alignment.CenterStart).statusBarsPadding().navigationBarsPadding().padding(start = 16.dp)
            else Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
        ) {
            PillBar(items, if (top == null) tab else null, onSelect = { stack.clear(); tab = it }, vertical = wide)
        }

        // Connection banner: only while not connected, never over a working session.
        AnimatedVisibility(
            conn !is ConnState.Connected && conn !is ConnState.Idle,
            enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp),
        ) {
            Row(
                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.inverseSurface).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (conn is ConnState.Connecting) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = MaterialTheme.colorScheme.inverseOnSurface, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(connText(conn), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelLarge)
            }
        }

        PopupCard(visible = popup != null, onDismiss = { popup = null }) {
            popup?.let { id -> EntityControls(graph, id, onOpenCamera = { actions.openCamera(it) }, onClose = { popup = null }) }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (wide) 16.dp else 96.dp))
    }
}

@Composable
private fun StatusBarIcons(dark: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(dark) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
