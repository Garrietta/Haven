package com.garrietta.haven

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.CameraStreamState
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.RisingPageContent
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.pillScrub
import com.garrietta.haven.ui.components.pillScrubItem
import com.garrietta.haven.ui.components.rememberPillScrubState
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.screens.AreasScreen
import com.garrietta.haven.ui.screens.AreaDetailScreen
import com.garrietta.haven.ui.screens.AutomationBuilderScreen
import com.garrietta.haven.ui.screens.DashboardEditScreen
import com.garrietta.haven.ui.screens.DashboardsScreen
import com.garrietta.haven.ui.screens.DetailPageScreen
import com.garrietta.haven.ui.screens.HelperBuilderScreen
import com.garrietta.haven.ui.screens.HomeEditorScreen
import com.garrietta.haven.ui.screens.LoginScreen
import com.garrietta.haven.ui.screens.SceneBuilderScreen
import com.garrietta.haven.ui.screens.ScriptBuilderScreen
import com.garrietta.haven.ui.screens.SearchScreen
import com.garrietta.haven.ui.screens.SettingsScreen
import com.garrietta.haven.ui.screens.TabManagerScreen
import com.garrietta.haven.ui.screens.ThermostatPageScreen
import com.garrietta.haven.ui.screens.TodayScreen
import com.garrietta.haven.ui.screens.UserTabScreen
import com.garrietta.haven.ui.theme.HavenTheme
import com.garrietta.haven.ui.theme.havenMotion
import com.garrietta.haven.ui.theme.havenSpring

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
)

/**
 * The pill bar IS the tab bar: Home always first, user tabs next, Settings
 * always last and always present. Boards/Areas/Search stay reachable from
 * Settings -> Browse; Home returned to the bar because a home with no tab
 * is a home you can't get back to. A legacy screen shows the bar with
 * nothing selected.
 */
private fun pillBarItems(tabs: List<com.garrietta.haven.data.UserTab>): List<NavItem> {
    val items = mutableListOf(
        NavItem(Screen.Today, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    )
    for (tab in tabs) {
        val (icon, outlinedIcon) = tabIcons(tab)
        items.add(NavItem(Screen.UserTab(tab.id), tab.title, icon, outlinedIcon))
    }
    items.add(NavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings))
    return items
}

/**
 * Per-tab pill icons: the first card's entity domain wins (camera, climate,
 * light, cover); title keywords cover entity-less tabs; GridView is the
 * fallback. Only domains with a matching outlined variant get a pair, so
 * the selected pill can still fill its icon like Home and Settings do.
 */
private fun tabIcons(tab: com.garrietta.haven.data.UserTab): Pair<ImageVector, ImageVector> {
    val domain = tab.cards.firstNotNullOfOrNull { yaml ->
        try {
            parseCardYaml(yaml).optString("entity").ifEmpty { null }?.substringBefore(".")
        } catch (e: Exception) {
            null
        }
    } ?: when {
        tab.title.contains("camera", ignoreCase = true) -> "camera"
        tab.title.contains("thermostat", ignoreCase = true) ||
            tab.title.contains("climate", ignoreCase = true) -> "climate"
        tab.title.contains("garage", ignoreCase = true) -> "cover"
        tab.title.contains("light", ignoreCase = true) ||
            tab.title.contains("lamp", ignoreCase = true) -> "light"
        else -> null
    }
    return when (domain) {
        "camera" -> Icons.Filled.Videocam to Icons.Outlined.Videocam
        "climate" -> Icons.Filled.Thermostat to Icons.Outlined.Thermostat
        "light" -> Icons.Filled.Lightbulb to Icons.Outlined.Lightbulb
        "cover" -> Icons.Filled.Home to Icons.Outlined.Home
        else -> Icons.Filled.GridView to Icons.Outlined.GridView
    }
}

private fun isTopLevel(screen: Screen): Boolean = when (screen) {
    Screen.Today, Screen.Dashboards, Screen.Areas, Screen.Search, Screen.Settings -> true
    is Screen.UserTab -> true
    else -> false
}

@Composable
fun HavenApp(vm: HavenViewModel) {
    var themeMode by remember { mutableStateOf(vm.settingsStore.themeMode) }
    var reduceMotion by remember { mutableStateOf(vm.settingsStore.reduceMotion) }
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    HavenTheme(darkTheme = darkTheme) {
        // Status-bar/navigation-bar icon colors follow the background's
        // luminance (Koda's approach): light background -> dark icons, dark
        // background -> light icons. The old theme hardcoded dark icons, which
        // made them invisible on the dark theme.
        StatusBarAppearance(MaterialTheme.colorScheme.background)
        if (!vm.isAuthenticated) {
            LoginScreen(vm)
            return@HavenTheme
        }
        val backstack = remember {
            mutableStateListOf<Screen>(Screen.Today)
        }
        val current = backstack.last()
        // Page-transition state: a page rises out of the tapped item's
        // saved bounds (vm.pageOrigin) on the house spring, and settles
        // back toward them on back. Top-level jumps clear it.
        var entering by remember { mutableStateOf(false) }
        var exitingScreen by remember { mutableStateOf<Screen?>(null) }
        val enterProgress = remember { Animatable(0f) }
        val exitProgress = remember { Animatable(1f) }
        val navigate: (Screen) -> Unit = { screen ->
            if ((screen is Screen.Thermostat || screen is Screen.EntityDetail) && vm.pageOrigin != null) {
                entering = true
            }
            backstack.add(screen)
        }
        val goTop: (Screen) -> Unit = {
            entering = false
            exitingScreen = null
            vm.setPageOrigin(null)
            backstack.clear()
            backstack.add(it)
        }
        val goBack: () -> Unit = {
            when {
                exitingScreen != null -> Unit // already animating out; ignore
                entering -> {
                    // Back mid-enter: abandon the animation, pop at once.
                    entering = false
                    if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
                }
                (current is Screen.Thermostat || current is Screen.EntityDetail) && vm.pageOrigin != null -> {
                    exitingScreen = current
                }
                backstack.size > 1 -> backstack.removeAt(backstack.lastIndex)
            }
        }
        val signOut: () -> Unit = {
            entering = false
            exitingScreen = null
            vm.setPageOrigin(null)
            vm.signOut()
            backstack.clear()
            backstack.add(Screen.Today)
        }
        BackHandler(enabled = backstack.size > 1) { goBack() }
        LaunchedEffect(entering) {
            if (entering) {
                enterProgress.snapTo(0f)
                if (reduceMotion) {
                    enterProgress.snapTo(1f)
                } else {
                    enterProgress.animateTo(1f, havenSpring())
                }
                entering = false
            }
        }
        LaunchedEffect(exitingScreen) {
            val leaving = exitingScreen
            if (leaving != null) {
                exitProgress.snapTo(1f)
                if (reduceMotion) {
                    exitProgress.snapTo(0f)
                } else {
                    exitProgress.animateTo(0f, havenSpring())
                }
                if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
                exitingScreen = null
                vm.setPageOrigin(null)
            }
        }
        // Camera overlay transitions share the same language: the live
        // view rises out of the tapped camera tile and settles back on close.
        var cameraExiting by remember { mutableStateOf(false) }
        val cameraEnterProgress = remember { Animatable(0f) }
        val cameraExitProgress = remember { Animatable(1f) }
        val cameraId = vm.fullscreenCameraId
        LaunchedEffect(cameraId) {
            if (cameraId != null && !cameraExiting) {
                cameraEnterProgress.snapTo(0f)
                if (reduceMotion) {
                    cameraEnterProgress.snapTo(1f)
                } else {
                    cameraEnterProgress.animateTo(1f, havenSpring())
                }
            }
        }
        LaunchedEffect(cameraExiting) {
            if (cameraExiting) {
                cameraExitProgress.snapTo(1f)
                if (reduceMotion) {
                    cameraExitProgress.snapTo(0f)
                } else {
                    cameraExitProgress.animateTo(0f, havenSpring())
                }
                vm.closeCameraFullscreen()
                vm.setPageOrigin(null)
                cameraExiting = false
            }
        }
        val requestCameraClose: () -> Unit = {
            when {
                cameraExiting -> Unit
                vm.pageOrigin != null -> cameraExiting = true
                else -> {
                    vm.closeCameraFullscreen()
                    vm.setPageOrigin(null)
                }
            }
        }
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        // Wide-layout detection lives here, above the Scaffold, so every
        // screen (and the overlays) reads the same value.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 600.dp
            CompositionLocalProvider(LocalWideLayout provides wideLayout) {
            Scaffold(
            // Screens consume the status-bar inset themselves
            // (statusBarsPadding on each root), so the Scaffold must not add
            // it again — otherwise headers sit a full inset too low. The
            // bottom bar's height is still reserved by the Scaffold.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel(current)) {
                    val barItems = pillBarItems(vm.userTabs)
                    val selectedIndex = when (current) {
                        Screen.Today -> 0
                        is Screen.UserTab -> vm.userTabs.indexOfFirst { it.id == current.tabId }.takeIf { it >= 0 }?.plus(1)
                        Screen.Settings -> barItems.lastIndex
                        else -> null
                    }
                    PillNav(
                        items = barItems,
                        selectedIndex = selectedIndex,
                        onSelect = goTop,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val content: @Composable (Screen) -> Unit = { screen ->
                    ScreenContent(
                        screen = screen,
                        vm = vm,
                        navigate = navigate,
                        goBack = goBack,
                        themeMode = themeMode,
                        reduceMotion = reduceMotion,
                        onThemeMode = {
                            themeMode = it
                            vm.settingsStore.themeMode = it
                        },
                        onReduceMotion = {
                            reduceMotion = it
                            vm.settingsStore.reduceMotion = it
                        },
                        onSignOut = signOut,
                    )
                }
                val leaving = exitingScreen
                when {
                    leaving != null && backstack.size > 1 -> {
                        // Reverse: the page settles back toward its origin
                        // over the screen it came from.
                        content(backstack[backstack.lastIndex - 1])
                        RisingPageContent(vm.pageOrigin, exitProgress.value) {
                            content(leaving)
                        }
                    }
                    entering && backstack.size > 1 -> {
                        // Enter: the page rises out of the tapped item over
                        // the screen it came from.
                        content(backstack[backstack.lastIndex - 1])
                        RisingPageContent(vm.pageOrigin, enterProgress.value) {
                            content(current)
                        }
                    }
                    else -> content(current)
                }
                // A failed tap used to vanish silently; now it banners here on
                // every screen until dismissed.
                val serviceError by vm.serviceError.collectAsState()
                if (serviceError != null) {
                    ServiceErrorBanner(
                        serviceError!!,
                        onDismiss = vm::dismissServiceError,
                        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp),
                    )
                }
            }
        }
            // Overlays render after the Scaffold so they cover the pill bar
            // too: the more-info sheet and the landscape camera viewer.
            if (vm.popupEntityId != null) {
                EntityPopupSheet(vm, navigate)
            }
            if (cameraId != null) {
                if (cameraExiting) {
                    RisingPageContent(vm.pageOrigin, cameraExitProgress.value, background = Color.Black) {
                        CameraFullscreen(vm, requestCameraClose)
                    }
                } else {
                    RisingPageContent(vm.pageOrigin, cameraEnterProgress.value, background = Color.Black) {
                        CameraFullscreen(vm, requestCameraClose)
                    }
                }
            }
            }
        }
        }
    }
}

/** One place for every destination, so the transition wrappers share it. */
@Composable
private fun ScreenContent(
    screen: Screen,
    vm: HavenViewModel,
    navigate: (Screen) -> Unit,
    goBack: () -> Unit,
    themeMode: String,
    reduceMotion: Boolean,
    onThemeMode: (String) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    when (screen) {
        Screen.Today -> TodayScreen(vm, navigate)
        Screen.Dashboards -> DashboardsScreen(vm, navigate)
        Screen.Areas -> AreasScreen(vm, navigate)
        Screen.Search -> SearchScreen(vm, navigate)
        Screen.Settings -> SettingsScreen(
            vm = vm,
            themeMode = themeMode,
            reduceMotion = reduceMotion,
            onThemeMode = onThemeMode,
            onReduceMotion = onReduceMotion,
            onSignOut = onSignOut,
            navigate = navigate,
        )
        is Screen.UserTab -> UserTabScreen(vm, screen.tabId, navigate)
        Screen.TabManager -> TabManagerScreen(vm, goBack, navigate)
        Screen.HomeEditor -> HomeEditorScreen(vm, goBack)
        // Detail pages replaced the old generic detail screen: per-domain
        // pages (climate still routes to its thermostat page inside).
        is Screen.EntityDetail -> DetailPageScreen(vm, screen.entityId, goBack)
        is Screen.Thermostat -> ThermostatPageScreen(vm, screen.entityId, goBack)
        is Screen.AreaDetail -> AreaDetailScreen(vm, screen.areaId, goBack, navigate)
        Screen.DashboardEdit -> DashboardEditScreen(vm, goBack)
        Screen.AutomationBuilder -> AutomationBuilderScreen(vm, goBack)
        Screen.SceneBuilder -> SceneBuilderScreen(vm, goBack)
        Screen.ScriptBuilder -> ScriptBuilderScreen(vm, goBack)
        Screen.HelperBuilder -> HelperBuilderScreen(vm, goBack)
    }
}

/**
 * More-info popup: tapping any entity now opens this bottom sheet with its
 * full control (dial, mode, fan, temps for the thermostat) instead of only
 * pushing a full detail screen. Details is still one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityPopupSheet(vm: HavenViewModel, navigate: (Screen) -> Unit) {
    val popupId = vm.popupEntityId ?: return
    val states by vm.states.collectAsState()
    val entity = states[popupId]
    // The sheet's own bounds become the page origin when Details is tapped,
    // so the full page rises out of the sheet on the house spring.
    var sheetBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val reduceMotion = LocalReduceMotion.current
    ModalBottomSheet(onDismissRequest = vm::closePopup) {
        Column(
            Modifier.fillMaxWidth()
                .onGloballyPositioned { sheetBounds = it.boundsInWindow() }
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            // Koda-style handle: a centered expressive pill, not a bare line.
            Box(
                Modifier.align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
            )
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = havenMotion(reduceMotion)) +
                    slideInVertically(animationSpec = havenMotion(reduceMotion)) { it / 4 },
            ) {
                Column {
                    if (entity == null) {
                        Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val accent = accentFor(entity.entityId)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DomainBadge(
                                entity.domain,
                                domainIcon(entity),
                                accent,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entity.friendlyName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(entity.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Plain control, not the card's header bar: the header
                        // is a dead control here (this IS the detail surface),
                        // so the card renders without it and the name still shows.
                        EntityControlCard(entity, vm, onDetail = { }, showHeader = false)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Button(
                                onClick = {
                                    sheetBounds?.let { vm.setPageOrigin(it) }
                                    vm.closePopup()
                                    when (entity.domain) {
                                        "climate" -> navigate(Screen.Thermostat(popupId))
                                        // Cameras open their live viewer, not a static page.
                                        "camera" -> vm.openCameraFullscreen(popupId)
                                        else -> navigate(Screen.EntityDetail(popupId))
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                            ) { Text("Details") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = vm::closePopup,
                                shape = RoundedCornerShape(50),
                            ) { Text("Close") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Camera fullscreen, LIVE: the viewer locks the Activity to landscape
 * while open and restores the previous orientation on exit, so the feed
 * fills the screen rotated 90 degrees instead of letterboxing in
 * portrait. Exit via the X button or the system back gesture — both are
 * always visible.
 *
 * The old viewer fetched one snapshot from `/api/camera_proxy/` and showed
 * it until Refresh was tapped — that was the still-image bug, on every
 * camera: it never opened a stream. This viewer plays the MJPEG proxy
 * stream (`/api/camera_proxy_stream/`) for real, on all four curated
 * cameras. While the stream connects or reconnects it shows the last
 * live frame from memory, dimmed, with a loading indicator; on return to
 * a previously viewed camera that frame shows instantly while the stream
 * resumes. A failed stream shows an explicit error + Retry — never a
 * silent still pretending to be live.
 */
@Composable
private fun CameraFullscreen(vm: HavenViewModel, onClose: () -> Unit) {
    val cameraId = vm.fullscreenCameraId ?: return
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    val activity = LocalContext.current as? Activity
    DisposableEffect(cameraId) {
        val prev = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            if (activity != null && prev != null) {
                activity.requestedOrientation = prev
            }
        }
    }
    BackHandler { onClose() }
    var streamState by remember(cameraId) { mutableStateOf<CameraStreamState>(CameraStreamState.Connecting) }
    var attempt by remember(cameraId) { mutableStateOf(0) }
    // The stream job lives in the ViewModel scope, so the effect cancels it
    // explicitly on dispose or Retry; joining keeps exactly one stream per view.
    LaunchedEffect(cameraId, attempt) {
        val job = vm.client.streamCamera(cameraId) { state ->
            if (state is CameraStreamState.Playing) {
                vm.cacheCameraFrame(cameraId, state.frame)
            }
            streamState = state
        }
        try {
            job.join()
        } finally {
            job.cancel()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = streamState) {
            is CameraStreamState.Playing -> {
                Image(
                    state.frame.asImageBitmap(),
                    contentDescription = entity?.friendlyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is CameraStreamState.Connecting -> {
                val last = vm.lastCameraFrame(cameraId)
                if (last != null) {
                    Image(
                        last.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        alpha = 0.4f,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (last != null) "Reconnecting…" else "Loading camera…",
                        color = Color.White,
                    )
                }
            }
            is CameraStreamState.Error -> {
                val last = vm.lastCameraFrame(cameraId)
                if (last != null) {
                    Image(
                        last.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        alpha = 0.25f,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Live view unavailable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { attempt++ }) { Text("Retry") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                }
            }
        }
        Text(
            entity?.friendlyName ?: cameraId,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

/** Dismissible banner for the last service-call failure. */
@Composable
private fun ServiceErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/**
 * The pill-bar bottom navigation, with Koda's item treatment: the selected
 * pill grows (padding 12 -> 20dp) on the house spring, its label fades and
 * expands in, and its icon fills — while the others shrink back to outlined
 * icons with no label. Dragging along the bar scrubs the highlight under the
 * finger (haptic tick per tab) and commits on release, exactly like Koda's
 * nav scrub. The bar container itself stays Haven's own floating pill.
 */
@Composable
private fun PillNav(items: List<NavItem>, selectedIndex: Int?, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val currentIndex = selectedIndex ?: -1
    val scrub = rememberPillScrubState(
        tabIds = items.indices.toList(),
        onHoverChange = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
        onCommit = { target ->
            if (target != currentIndex) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onSelect(items[target].screen)
            }
        },
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        // Scrolls horizontally once tabs overflow; short bars stay centered.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 8.dp).pillScrub(scrub),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for ((index, item) in items.withIndex()) {
                val selected = (scrub.hoveredTab ?: currentIndex) == index
                val animatedPadding by animateDpAsState(
                    if (selected) 20.dp else 12.dp,
                    animationSpec = havenMotion(reduceMotion),
                    label = "pillPad",
                )
                val animatedContainerColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "pillColor",
                )
                val animatedContentColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "pillContent",
                )
                // Selected pill pops on the house spring, Koda-style.
                val pillScale by animateFloatAsState(
                    if (selected) 1.06f else 1f,
                    animationSpec = havenMotion(reduceMotion),
                    label = "pillScale",
                )
                Surface(
                    selected = selected,
                    onClick = {
                        if (index != currentIndex) {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onSelect(item.screen)
                        }
                    },
                    shape = CircleShape,
                    color = animatedContainerColor,
                    contentColor = animatedContentColor,
                    modifier = Modifier.height(48.dp)
                        .graphicsLayer(scaleX = pillScale, scaleY = pillScale)
                        .pillScrubItem(scrub, index),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = animatedPadding)
                            .animateContentSize(animationSpec = havenMotion(reduceMotion)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (selected) item.icon else item.outlinedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn() + expandHorizontally(animationSpec = havenMotion(reduceMotion)),
                            exit = fadeOut() + shrinkHorizontally(animationSpec = havenMotion(reduceMotion)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Drives the system-bar icon colors from a background color's luminance,
 * Koda-style: a light background gets dark icons, a dark background gets
 * light icons. Restores the previous values on dispose so a screen with a
 * different background (camera fullscreen) can't leak its choice.
 */
@Composable
fun StatusBarAppearance(background: Color) {
    val view = LocalView.current
    val lightIcons = background.luminance() > 0.5f
    DisposableEffect(view, lightIcons) {
        val controller = ViewCompat.getWindowInsetsController(view)
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        if (controller != null) {
            controller.isAppearanceLightStatusBars = lightIcons
            controller.isAppearanceLightNavigationBars = lightIcons
        }
        onDispose {
            if (controller != null) {
                if (prevStatus != null) controller.isAppearanceLightStatusBars = prevStatus
                if (prevNav != null) controller.isAppearanceLightNavigationBars = prevNav
            }
        }
    }
}

/** Back arrow + title row used by every detail screen. */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
