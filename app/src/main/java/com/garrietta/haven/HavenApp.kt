package com.garrietta.haven

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.screens.AreasScreen
import com.garrietta.haven.ui.screens.AreaDetailScreen
import com.garrietta.haven.ui.screens.AutomationBuilderScreen
import com.garrietta.haven.ui.screens.DashboardEditScreen
import com.garrietta.haven.ui.screens.DashboardsScreen
import com.garrietta.haven.ui.screens.EntityDetailScreen
import com.garrietta.haven.ui.screens.HelperBuilderScreen
import com.garrietta.haven.ui.screens.LoginScreen
import com.garrietta.haven.ui.screens.SceneBuilderScreen
import com.garrietta.haven.ui.screens.ScriptBuilderScreen
import com.garrietta.haven.ui.screens.SearchScreen
import com.garrietta.haven.ui.screens.HomeEditorScreen
import com.garrietta.haven.ui.screens.SettingsScreen
import com.garrietta.haven.ui.screens.TodayScreen
import com.garrietta.haven.ui.screens.TabManagerScreen
import com.garrietta.haven.ui.screens.ThermostatPageScreen
import com.garrietta.haven.ui.screens.UserTabScreen
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.components.pillScrub
import com.garrietta.haven.ui.components.pillScrubItem
import com.garrietta.haven.ui.components.rememberPillScrubState
import com.garrietta.haven.ui.theme.HavenTheme
import com.garrietta.haven.ui.theme.havenMotion

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
        val navigate: (Screen) -> Unit = { backstack.add(it) }
        val goTop: (Screen) -> Unit = {
            backstack.clear()
            backstack.add(it)
        }
        val goBack: () -> Unit = {
            if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
        }
        BackHandler(enabled = backstack.size > 1) { goBack() }
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
                when (val screen = current) {
                    Screen.Today -> TodayScreen(vm, navigate)
                    Screen.Dashboards -> DashboardsScreen(vm, navigate)
                    Screen.Areas -> AreasScreen(vm, navigate)
                    Screen.Search -> SearchScreen(vm, navigate)
                    Screen.Settings -> SettingsScreen(
                        vm = vm,
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
                        onSignOut = {
                            vm.signOut()
                            backstack.clear()
                            backstack.add(Screen.Today)
                        },
                        navigate = navigate,
                    )
                    is Screen.UserTab -> UserTabScreen(vm, screen.tabId, navigate)
                    Screen.TabManager -> TabManagerScreen(vm, goBack, navigate)
                    Screen.HomeEditor -> HomeEditorScreen(vm, goBack)
                    is Screen.EntityDetail -> EntityDetailScreen(vm, screen.entityId, goBack)
                    is Screen.Thermostat -> ThermostatPageScreen(vm, screen.entityId, goBack)
                    is Screen.AreaDetail -> AreaDetailScreen(vm, screen.areaId, goBack, navigate)
                    Screen.DashboardEdit -> DashboardEditScreen(vm, goBack)
                    Screen.AutomationBuilder -> AutomationBuilderScreen(vm, goBack)
                    Screen.SceneBuilder -> SceneBuilderScreen(vm, goBack)
                    Screen.ScriptBuilder -> ScriptBuilderScreen(vm, goBack)
                    Screen.HelperBuilder -> HelperBuilderScreen(vm, goBack)
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
            if (vm.fullscreenCameraId != null) {
                CameraFullscreen(vm, vm::closeCameraFullscreen)
            }
            }
        }
        }
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
    ModalBottomSheet(onDismissRequest = vm::closePopup) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            if (entity == null) {
                Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Plain title, not the card's header bar: the header is a
                // dead control here (this IS the detail surface), so the
                // card renders without it and the name still shows.
                Text(entity.friendlyName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(entity.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                EntityControlCard(entity, vm, onDetail = { }, showHeader = false)
                Spacer(Modifier.height(12.dp))
                Row {
                    Button(onClick = {
                        vm.closePopup()
                        // A climate entity's full control lives on its
                        // thermostat page, not the generic detail screen.
                        if (entity.domain == "climate") {
                            navigate(Screen.Thermostat(popupId))
                        } else {
                            navigate(Screen.EntityDetail(popupId))
                        }
                    }) { Text("Details") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = vm::closePopup) { Text("Close") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Camera fullscreen: the viewer locks the Activity to landscape while open
 * and restores the previous orientation on exit, so the feed fills the
 * screen rotated 90 degrees instead of letterboxing in portrait. Exit via
 * the X button or the system back gesture — both are always visible.
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
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(cameraId, refreshKey) {
        bitmap = vm.client.fetchCameraBitmap(cameraId)
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (bitmap != null) {
            Image(
                bitmap!!.asImageBitmap(),
                contentDescription = entity?.friendlyName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("Loading camera…", color = Color.White, modifier = Modifier.align(Alignment.Center))
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
        TextButton(
            onClick = { refreshKey++ },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Refresh", color = Color.White)
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
                    modifier = Modifier.height(48.dp).pillScrubItem(scrub, index),
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
