package com.garrietta.haven

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.CameraStreamState
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalSharedTransitionScope
import com.garrietta.haven.ui.components.LocalSharedVisibilityScope
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.SheetPageContent
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.KodaMenuRow
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.pillLabelVisible
import com.garrietta.haven.ui.components.sharedCardBounds
import com.garrietta.haven.ui.theme.EmphasizedAccelerate
import com.garrietta.haven.ui.theme.EmphasizedDecelerate
import com.garrietta.haven.ui.theme.sheetSpring
import com.garrietta.haven.ui.theme.springMorph
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

/** Entity id a detail screen belongs to (the shared card key it morphs from). */
private fun detailEntityId(screen: Screen): String = when (screen) {
    is Screen.Thermostat -> screen.entityId
    is Screen.EntityDetail -> screen.entityId
    else -> ""
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
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
        // Detail destinations morph out of their card (spec item 2),
        // they do not slide or rise: the overlay below renders the page
        // above the scaffold, and its root claims the card's sharedBounds
        // key, so the card grows into the page on the bounds spring with
        // 150ms fades. Back is a plain pop; the exit morph plays
        // automatically from the AnimatedVisibility below. This replaces
        // the old bottom-sheet-rise path (entering/exitingScreen) for
        // detail pages; the camera fullscreen keeps its own rise, and
        // vm.pageOrigin is still set by tile taps and sheet Details
        // because the camera path needs it.
        val detailScreen = current as? Screen.Thermostat ?: current as? Screen.EntityDetail
        // lastDetail keeps the page alive through the exit morph after
        // back has already popped the backstack (same pattern as the
        // sheet's lastPopupId).
        var lastDetail by remember { mutableStateOf<Screen?>(null) }
        if (detailScreen != null) lastDetail = detailScreen
        val navigate: (Screen) -> Unit = { screen -> backstack.add(screen) }
        val goTop: (Screen) -> Unit = {
            vm.pageOrigin = null
            backstack.clear()
            backstack.add(it)
        }
        val goBack: () -> Unit = {
            if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
        }
        val signOut: () -> Unit = {
            vm.pageOrigin = null
            vm.signOut()
            backstack.clear()
            backstack.add(Screen.Today)
        }
        BackHandler(enabled = backstack.size > 1) { goBack() }
        // Camera overlay transitions share the same language: the live
        // view rises as a bottom sheet out of the tapped camera tile and
        // sinks back on close.
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
                    cameraEnterProgress.animateTo(1f, sheetSpring())
                }
            }
        }
        LaunchedEffect(cameraExiting) {
            if (cameraExiting) {
                cameraExitProgress.snapTo(1f)
                if (reduceMotion) {
                    cameraExitProgress.snapTo(0f)
                } else {
                    cameraExitProgress.animateTo(0f, sheetSpring())
                }
                vm.closeCameraFullscreen()
                vm.pageOrigin = null
                cameraExiting = false
            }
        }
        val requestCameraClose: () -> Unit = {
            when {
                cameraExiting -> Unit
                vm.pageOrigin != null -> cameraExiting = true
                else -> {
                    vm.closeCameraFullscreen()
                    vm.pageOrigin = null
                }
            }
        }
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        // Wide-layout detection lives here, above the Scaffold, so every
        // screen (and the overlays) reads the same value.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 600.dp
            CompositionLocalProvider(LocalWideLayout provides wideLayout) {
            // Sheet presence drives the backdrop blur on the whole
            // scaffold — content AND pill bar, never the overlay itself
            // — up to 24dp, on the emphasized-decelerate entrance easing.
            // The old code blurred only the content box, so mid-enter the
            // pill read as a separate bright layer over the dimmed
            // background (2026-10-29 evidence video: the pill "above the
            // scrim" was really the pill unblurred while everything else
            // dimmed; the full-screen scrim above the scaffold was
            // already covering it).
            val sheetPresence by animateFloatAsState(
                if (vm.popupEntityId != null) 1f else 0f,
                animationSpec = tween(durationMillis = 300, easing = EmphasizedDecelerate),
                label = "sheetPresence",
            )
            // One content lambda shared by the scaffold and the detail
            // overlay below, so both render the same screens.
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
            SharedTransitionLayout {
            // Tiles read this always-visible scope for sharedBounds; the
            // sheet panel declares the same key in its own scope below, so
            // a card morphs into the sheet instead of the sheet popping in.
            // PAGES keep the recorded bottom-sheet rise — only sheets morph.
            AnimatedVisibility(visible = true) {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                LocalSharedVisibilityScope provides this,
            ) {
            // Backdrop blur while a sheet is open: up to 24dp, scaled by
            // sheet presence, skipped entirely under reduce-motion, and
            // applied to the whole scaffold — content and pill bar — so
            // the pill dims and blurs with everything else instead of
            // reading as a bright layer over the dim. Never the overlay.
            val density = LocalDensity.current
            val blurPx = if (reduceMotion) 0f else with(density) { (24.dp * sheetPresence).toPx() }
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    if (blurPx > 0.5f) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP,
                        ).asComposeRenderEffect()
                    }
                },
            ) {
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
                        // Floating island, never pinned edge-to-edge: the
                        // navigationBars window inset, then 16dp
                        // horizontal and 12dp bottom padding (the spec's
                        // literal numbers; reconciles #41's 16dp
                        // side+bottom margins by keeping 16dp sides and
                        // taking the spec's 12dp bottom).
                        modifier = Modifier.fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                // Detail pages do NOT render here: the scaffold shows
                // the screen underneath, and the page itself lives in
                // the overlay below (above all chrome). Every other
                // destination renders normally.
                val base = when (current) {
                    is Screen.Thermostat, is Screen.EntityDetail ->
                        backstack.getOrNull(backstack.lastIndex - 1) ?: Screen.Today
                    else -> current
                }
                content(base)
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
            } // blur Box
            } // CompositionLocalProvider (tile shared-transition scope)
            } // AnimatedVisibility (tile shared-transition scope)
            // The sheet overlay renders inside the shared-transition layout
            // so its panel can morph from the tapped card, and above the
            // scaffold so it covers the pill bar too. It has its own
            // visibility scope; the panel declares the card's key in it.
            // lastPopupId keeps the content alive through the exit
            // animation after closePopup has already cleared the id.
            var lastPopupId by remember { mutableStateOf<String?>(null) }
            if (vm.popupEntityId != null) lastPopupId = vm.popupEntityId
            // The panel positions itself by the shared-bounds morph
            // alone. The old enter/exit also slid the panel vertically,
            // so two transforms positioned it at once — one source of
            // the jumpy, off-center first frames in the evidence video.
            AnimatedVisibility(
                visible = vm.popupEntityId != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 250, easing = EmphasizedDecelerate)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = EmphasizedAccelerate)),
            ) {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalSharedVisibilityScope provides this,
                ) {
                    val sheetId = lastPopupId
                    if (sheetId != null) EntityPopupSheet(vm, navigate, sheetId)
                }
            }
            // Detail-page overlay (spec item 2): renders above the
            // scaffold — above the status bar and the pill bar, never
            // under them — with the card's sharedBounds key on its root,
            // so the card morphs into the page on the bounds spring with
            // 150ms fades. Placed after the sheet overlay so the Details
            // handoff (sheet -> page) draws the page above the closing
            // sheet. lastDetail keeps the page alive through the exit
            // morph after back has popped the backstack.
            AnimatedVisibility(
                visible = detailScreen != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 150, easing = EmphasizedDecelerate)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150, easing = EmphasizedAccelerate)),
            ) {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalSharedVisibilityScope provides this,
                ) {
                    val detail = lastDetail
                    if (detail != null) {
                        Box(
                            Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .sharedCardBounds("card-${detailEntityId(detail)}"),
                        ) {
                            content(detail)
                        }
                    }
                }
            }
            } // SharedTransitionLayout
            // Camera overlays stay outside the shared layout: they rise as
            // bottom-sheet pages from their tile origin, not as morphs.
            if (cameraId != null) {
                if (cameraExiting) {
                    SheetPageContent(vm.pageOrigin, cameraExitProgress.value, background = Color.Black) {
                        CameraFullscreen(vm, requestCameraClose, cameraExitProgress.value)
                    }
                } else {
                    SheetPageContent(vm.pageOrigin, cameraEnterProgress.value, background = Color.Black) {
                        CameraFullscreen(vm, requestCameraClose, cameraEnterProgress.value)
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
 * More-info popup, now an inline overlay instead of a ModalBottomSheet:
 * a Dialog window breaks shared-element coordinate spaces, so the sheet
 * must render inline for the card->sheet morph to work. The panel has no
 * half-expanded state (hidden or fully shown, so the abrupt half-expanded
 * snapping is gone by construction); dragging down past the threshold
 * dismisses it, otherwise it springs back. Back and scrim tap dismiss.
 * The sheet's own bounds become the page origin when Details is tapped,
 * so the full page rises as a bottom sheet out of the sheet on the
 * sheet spring. PAGES keep the rise; only this sheet morphs.
 */
@Composable
private fun EntityPopupSheet(vm: HavenViewModel, navigate: (Screen) -> Unit, popupId: String) {
    val states by vm.states.collectAsState()
    val entity = states[popupId]
    var sheetBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    // Entering phase for the dynamic handle: wide/thin while the sheet
    // rises, narrow/thick at rest.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(popupId) {
        kotlinx.coroutines.delay(350)
        settled = true
    }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    BackHandler { vm.closePopup() }
    Box(Modifier.fillMaxSize()) {
        // Scrim: tap dismisses.
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { vm.closePopup() },
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .sharedCardBounds("card-$popupId")
                .onGloballyPositioned { sheetBounds = it.boundsInWindow() }
                .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                .pointerInput(popupId) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset.value > dismissThresholdPx) {
                                vm.closePopup()
                            } else {
                                scope.launch { dragOffset.animateTo(0f, springMorph()) }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dragOffset.animateTo(0f, springMorph()) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { dragOffset.snapTo((dragOffset.value + dragAmount).coerceAtLeast(0f)) }
                    }
                },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = screenHeight * 0.92f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                // Fluid dynamic drag handle: 48x3.5 while entering or
                // dragging, 32x4 at rest, on spring 0.6/Low.
                val dragging = dragOffset.value > 1f
                val handleSpec: FiniteAnimationSpec<Dp> = if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f)
                    else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                val handleWidth by animateDpAsState(
                    if (dragging || !settled) 48.dp else 32.dp,
                    animationSpec = handleSpec,
                    label = "sheetHandleW",
                )
                val handleHeight by animateDpAsState(
                    if (dragging || !settled) 3.5.dp else 4.dp,
                    animationSpec = handleSpec,
                    label = "sheetHandleH",
                )
                Box(
                    Modifier.align(Alignment.CenterHorizontally)
                        .size(width = handleWidth, height = handleHeight)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
                )
                Spacer(Modifier.height(12.dp))
                    if (entity == null) {
                        Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val accent = accentFor(entity.entityId)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DomainBadge(
                                entity,
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
                        // Nested: the sheet panel is already a container.
                        EntityControlCard(entity, vm, onDetail = { }, showHeader = false, nested = true)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            KodaMenuRow(
                                badgeKey = entity.entityId,
                                badgeKind = shapeForDomain(entity.domain),
                                icon = domainIcon(entity),
                                title = "Details",
                                subtitle = "Open the full page",
                                onClick = {
                                    sheetBounds?.let { vm.pageOrigin = it }
                                    vm.closePopup()
                                    when (entity.domain) {
                                        "climate" -> navigate(Screen.Thermostat(popupId))
                                        // Cameras open their live viewer, not a static page.
                                        "camera" -> vm.openCameraFullscreen(popupId)
                                        else -> navigate(Screen.EntityDetail(popupId))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = vm::closePopup,
                                shape = RoundedCornerShape(50),
                            ) { Text("Close") }
                        }
                    }
                Spacer(Modifier.height(8.dp))
            }
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
 * live frame from memory, dimmed, with a low-profile shimmer skeleton;
 * on return to a previously viewed camera that frame shows instantly
 * while the stream resumes. A failed stream shows an explicit error +
 * Retry — never a silent still pretending to be live. The image region
 * itself interpolates (scale 0.94 -> 1, alpha 0.7 -> 1) with the page
 * progress, so the fullscreen image grows out of the grid slot instead
 * of flashing in at full size — the page rise itself is unchanged.
 */
@Composable
private fun CameraFullscreen(vm: HavenViewModel, onClose: () -> Unit, enterProgress: Float) {
    val cameraId = vm.fullscreenCameraId ?: return
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    // Ring's MJPEG proxy stream is its last motion recording, not live
    // video (see isRingCamera) — so Ring tries HA's HLS stream path
    // first (the path HA's own frontend plays live) and only falls back
    // to the MJPEG recording, labeled as such, when HA cannot stream it.
    // Non-Ring cameras stay on the MJPEG path, untouched.
    val isRing = entity?.let { isRingCamera(it) } == true
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
    // Ring HLS negotiation: hlsUrl set when HA returns a playlist, hlsFailed
    // when it cannot stream (or answers nothing within the timeout — the
    // WS callback may never fire if the socket is down). hlsReady flips
    // only when ExoPlayer actually reaches READY: the "Live" badge is gated
    // on it, never on the URL existing. Retry (attempt++) resets all three
    // and re-runs the negotiation from scratch.
    var hlsUrl by remember(cameraId, attempt, isRing) { mutableStateOf<String?>(null) }
    var hlsFailed by remember(cameraId, attempt, isRing) { mutableStateOf(false) }
    var hlsReady by remember(cameraId, attempt, isRing) { mutableStateOf(false) }
    LaunchedEffect(cameraId, attempt, isRing) {
        if (!isRing) return@LaunchedEffect
        var answered = false
        val timeout = launch {
            delay(10_000)
            if (!answered) hlsFailed = true
        }
        vm.client.requestHlsUrl(cameraId) { url ->
            if (answered) return@requestHlsUrl
            answered = true
            timeout.cancel()
            // Timed out already: the MJPEG fallback owns the view now,
            // so a late playlist must not start a second player beside it.
            if (hlsFailed) return@requestHlsUrl
            if (url != null) hlsUrl = url else hlsFailed = true
        }
    }
    // MJPEG runs for every non-Ring camera, and for Ring only once HLS has
    // failed — never alongside a live HLS player, and never instead of
    // trying HLS. The stream job lives in the ViewModel scope, so the
    // effect cancels it explicitly on dispose or Retry; joining keeps
    // exactly one stream per view.
    val useMjpeg = !isRing || hlsFailed
    LaunchedEffect(cameraId, attempt, useMjpeg) {
        if (!useMjpeg) return@LaunchedEffect
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
    // The HLS player, owned by this view: created when the URL lands,
    // released when the URL clears (player error) or the view leaves.
    // A player error falls back to the MJPEG recording rather than an
    // error screen — the recording label, not silence, is the fallback.
    val context = LocalContext.current
    var hlsPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(hlsUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) hlsReady = true
            }

            override fun onPlayerError(error: PlaybackException) {
                hlsUrl = null
                hlsFailed = true
            }
        }
        val player = hlsUrl?.let { vm.client.createHlsPlayer(context, it) }
        player?.addListener(listener)
        hlsPlayer = player
        onDispose {
            player?.removeListener(listener)
            player?.release()
            hlsPlayer = null
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // HLS live video, only once ExoPlayer is actually READY: before
        // that the MJPEG connecting UI below covers negotiation, so a
        // black unready player never flashes in.
        val player = hlsPlayer
        if (player != null && hlsReady) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val s = 0.94f + 0.06f * enterProgress
                    scaleX = s
                    scaleY = s
                    alpha = 0.7f + 0.3f * enterProgress
                },
            )
        } else when (val state = streamState) {
            is CameraStreamState.Playing -> {
                Image(
                    state.frame.asImageBitmap(),
                    contentDescription = entity?.friendlyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val s = 0.94f + 0.06f * enterProgress
                        scaleX = s
                        scaleY = s
                        alpha = 0.7f + 0.3f * enterProgress
                    },
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
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val s = 0.94f + 0.06f * enterProgress
                            scaleX = s
                            scaleY = s
                            alpha = 0.7f + 0.3f * enterProgress
                        },
                    )
                }
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Low-profile shimmer skeleton, not a spinner on black.
                    ShimmerBox(Modifier.size(120.dp, 8.dp).clip(RoundedCornerShape(50)))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        // Ring negotiating HLS must not say "recording"
                        // yet (live is still possible), and Ring on the
                        // MJPEG fallback must not say "camera" (it is a
                        // recording). Non-Ring text is unchanged.
                        if (isRing && !hlsFailed) "Loading live view…"
                        else if (isRing) "Loading last recording…"
                        else if (last != null) "Reconnecting…"
                        else "Loading camera…",
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
                    Text(
                        if (isRing) "Recording unavailable" else "Live view unavailable",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { attempt++ }) { Text("Retry") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                }
            }
        }
        Column(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
        ) {
            Text(
                entity?.friendlyName ?: cameraId,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            // The badge is the honesty mechanism: "Live" only when the
            // HLS player is actually READY; "Last recording" only once
            // HLS has failed and the MJPEG fallback is in play; nothing
            // while negotiation is still open, so the viewer never claims
            // either before it knows.
            val badgeText = when {
                isRing && hlsReady -> "Live"
                isRing && hlsFailed -> "Last recording · Not live"
                else -> null
            }
            if (badgeText != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.15f),
                ) {
                    Text(
                        badgeText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
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
 * The pill-bar bottom navigation, rebuilt on measured positions: a single
 * indicator morphs between tabs by animating its left/right bounds with
 * spring(0.74, 380) from the tabs' measured window rects — it is never
 * instantly reattached as a fixed-size pill. The indicator target follows
 * the scrub hover when present, else the selected tab. Labels are
 * single-line with ellipsis, and a title over 10 characters suppresses its
 * static label in favor of an icon badge (selected: icon + label row with
 * a fade+scale crossfade; unselected: icon only) — the old "Living Room
 * lig" truncation is gone by construction. The shell is a floating island:
 * 32dp corners, surfaceContainer, subtle shadow, 16dp bottom margin at the
 * call site. Dragging along the bar still scrubs the highlight (haptic
 * tick per tab) and commits on release, Koda-style.
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
    // Measured tab positions, in window coordinates: the bar origin is
    // subtracted, so the indicator stays correct while the row scrolls.
    var barOrigin by remember { mutableStateOf(0f) }
    var itemRects by remember { mutableStateOf<Map<Int, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    val targetIndex = scrub.hoveredTab ?: currentIndex
    val targetRect = itemRects[targetIndex]
    val indicatorSpec = if (reduceMotion) spring<Float>(stiffness = 10_000f, dampingRatio = 1f)
        else spring(dampingRatio = 0.74f, stiffness = 380f)
    val indicatorLeft by animateFloatAsState(
        targetRect?.let { it.left - barOrigin } ?: 0f,
        animationSpec = indicatorSpec,
        label = "pillLeft",
    )
    val indicatorRight by animateFloatAsState(
        targetRect?.let { it.right - barOrigin } ?: 0f,
        animationSpec = indicatorSpec,
        label = "pillRight",
    )
    val density = LocalDensity.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .onGloballyPositioned { barOrigin = it.boundsInWindow().left },
        ) {
            // The morphing indicator, behind the items.
            if (targetRect != null) {
                Box(
                    Modifier.align(Alignment.CenterStart)
                        .offset { IntOffset(indicatorLeft.roundToInt(), 0) }
                        .width(with(density) { (indicatorRight - indicatorLeft).toDp() })
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                )
            }
            // Scrolls horizontally once tabs overflow; short bars stay centered.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 8.dp).pillScrub(scrub),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((index, item) in items.withIndex()) {
                    val selected = (scrub.hoveredTab ?: currentIndex) == index
                    val showLabel = pillLabelVisible(item.label, selected)
                    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier.height(48.dp)
                            .onGloballyPositioned { itemRects = itemRects + (index to it.boundsInWindow()) }
                            .pillScrubItem(scrub, index)
                            .clickable {
                                if (index != currentIndex) {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onSelect(item.screen)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = showLabel,
                            transitionSpec = {
                                // Vertical slide + fade, clipped: the old
                                // fade+scale crossfade drew outgoing and
                                // incoming labels in the same row space at
                                // once, so during tab switches the pill
                                // showed colliding text ("HomeThermostat"
                                // in the 2026-10-29 evidence video). A
                                // vertical slide can never overlap
                                // horizontally, and togetherWith already
                                // clips to the animated size by default.
                                slideInVertically(
                                    animationSpec = tween(durationMillis = 180, easing = EmphasizedDecelerate),
                                ) { it } + fadeIn(animationSpec = tween(durationMillis = 180, easing = EmphasizedDecelerate)) togetherWith
                                    slideOutVertically(
                                        animationSpec = tween(durationMillis = 120, easing = EmphasizedAccelerate),
                                    ) { -it } + fadeOut(animationSpec = tween(durationMillis = 120, easing = EmphasizedAccelerate))
                            },
                            label = "pillLabel",
                        ) { show ->
                            if (show) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(item.icon, contentDescription = item.label, tint = contentColor, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        item.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = contentColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            } else {
                                Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Icon(
                                        if (selected) item.icon else item.outlinedIcon,
                                        contentDescription = item.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(24.dp),
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
