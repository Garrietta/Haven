package com.garrietta.haven

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.CameraStreamState
import com.garrietta.haven.data.RingWebRtcSession
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.SheetPageContent
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.KodaMenuRow
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.pillLabelVisible
import com.garrietta.haven.ui.theme.sheetSpring
import com.garrietta.haven.ui.theme.springMorph
import com.garrietta.haven.ui.components.pillScrub
import com.garrietta.haven.ui.components.pillScrubItem
import com.garrietta.haven.ui.components.rememberPillScrubState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntSize
import com.garrietta.haven.ui.components.LocalCardBounds
import com.garrietta.haven.ui.components.LocalHiddenCardId
import com.garrietta.haven.ui.components.MorphSurface
import com.garrietta.haven.ui.components.centerTargetRect
import com.garrietta.haven.ui.components.morphBackgroundBlend
import com.garrietta.haven.ui.components.morphContentAlpha
import com.garrietta.haven.ui.components.morphCornerTop
import com.garrietta.haven.ui.components.ScrollVisibilityState
import com.garrietta.haven.ui.components.detailTargetRect
import com.garrietta.haven.ui.components.fallbackFromRect
import com.garrietta.haven.ui.components.lerpRect
import com.garrietta.haven.ui.components.sheetTargetRect
import com.garrietta.haven.ui.theme.springBounds
import com.garrietta.haven.ui.theme.springSpatial
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.screens.AreasScreen
import com.garrietta.haven.ui.screens.AreaDetailScreen
import com.garrietta.haven.ui.screens.AutomationBuilderScreen
import com.garrietta.haven.ui.screens.CameraClipsScreen
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
import com.garrietta.haven.ui.screens.ThermostatPopout
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

/**
 * Open sheet morph: the entity id plus the source card rect captured
 * at open (null = the card never reported bounds, e.g. a board card;
 * the sheet then morphs from the graceful fallback rect — the target
 * shifted 80dp down — never an instant appearance, never a crash).
 * [targetSize] is filled by the one-frame measurement pass. The
 * overlay always prefers the card's CURRENT bounds over this captured
 * rect (cards re-report on relayout), so rotation mid-morph and cards
 * that moved between open and dismiss track correctly; this captured
 * rect is only the fallback.
 */
private class SheetAnim(val id: String, val source: Rect?) {
    var targetSize by mutableStateOf<IntSize?>(null)
}

/** Open detail morph: the backstack screen plus its source card rect (same fallback rule as sheets). */
private class DetailAnim(val screen: Screen, val entityId: String, val source: Rect?)

/** Open thermostat pop-out morph: the climate entity plus its source card rect (same fallback rule as sheets). */
private class PopoutAnim(val id: String, val source: Rect?) {
    var targetSize by mutableStateOf<IntSize?>(null)
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
        // Morph state machine (2026-10-29 architectural overhaul):
        // the sheet and detail overlays below are explicit single-surface
        // morphs, not framework sharedBounds. Each morph captures its
        // source card's root rect at open and interpolates ONE rect to
        // its target (and exactly back on dismiss); the source card
        // renders alpha 0 while its morph is active (see
        // LocalHiddenCardId), so source and destination are never both
        // visible. The camera fullscreen keeps its own rise, and
        // vm.pageOrigin is still set by tile taps and sheet Details
        // because the camera path needs it.
        val detailScreen = current as? Screen.Thermostat ?: current as? Screen.EntityDetail
        val cardBounds = remember { mutableStateMapOf<String, Rect>() }
        var sheetAnim by remember { mutableStateOf<SheetAnim?>(null) }
        var detailAnim by remember { mutableStateOf<DetailAnim?>(null) }
        val sheetProgress = remember { Animatable(0f) }
        val detailProgress = remember { Animatable(0f) }
        val sheetDrag = remember { Animatable(0f) }
        var popoutAnim by remember { mutableStateOf<PopoutAnim?>(null) }
        val popoutProgress = remember { Animatable(0f) }
        var sheetSurfaceBounds by remember { mutableStateOf<Rect?>(null) }
        // No exiting flags and no morph generation anymore: dismiss
        // is synchronous (see dismissDetail/dismissSheet/dismissPopout
        // below), so there is no in-flight exit animation for a
        // top-level navigation to race with. The #52 async stale-
        // dismiss race is impossible in a synchronous dismiss, and
        // the flags that guarded it are gone with the animation.
        // Scroll-away state lives here, above goTop, so a tab switch
        // can reset it: it used to persist across tabs (remembered on
        // density only), so a pill hidden by scrolling on one tab
        // stayed hidden on the next until the user scrolled up there —
        // the pill that vanished on the Thermostat tab with no scroll
        // happening. A fresh tab always starts with the pill visible.
        val scrollDensity = LocalDensity.current
        val scrollVisibility = remember(scrollDensity) {
            ScrollVisibilityState(with(scrollDensity) { 8.dp.toPx() })
        }
        val navigate: (Screen) -> Unit = { screen -> backstack.add(screen) }
        val goTop: (Screen) -> Unit = {
            vm.pageOrigin = null
            vm.closePopup()
            sheetAnim = null
            detailAnim = null
            popoutAnim = null
            vm.closeClimatePopout()
            scrollVisibility.reset()
            backstack.clear()
            backstack.add(it)
        }
        val scope = rememberCoroutineScope()
        // INSTANT RETURN (2026-10-29 verdict videos on run #60).
        // Dismiss used to animate progress 1 -> 0 on the bounds spring
        // and only then unhide the source card: during the whole exit
        // the card stayed hidden while a shrinking, content-dissolved
        // panel sat over it, and the card then popped back in stages
        // (badge first, text later, after async content reloaded).
        // That is exactly the reported hesitation, white flash,
        // disappearing polygon, and clipped polygon bottom on return.
        // The fix is his first offered option: the morph surface is
        // removed and the source card restored in the SAME
        // recomposition — no exit shrink, no exit fade. The card was
        // composed throughout (only alpha-hidden), so unhide is
        // atomic: all elements present in the same frame, never
        // staged, never blank, never clipped mid-animation. Enter
        // morphs are unchanged (grow + steep content fade +
        // background blend); the sheet drag spring-back on
        // release-below-threshold is a gesture cancel, not a return
        // morph, and is kept. The camera rise/sink keeps its own exit
        // animation (separate path, not complained about on return).
        // The dismisses are synchronous, so the #52 async stale-
        // dismiss race cannot occur; identity checks stay (never pop
        // or clear a destination that is not this morph's own).
        // This SUPERSEDES #60's exit morph for sheets, pop-out, and
        // detail pages (the exit animation is gone, not retuned).
        fun dismissDetail() {
            val anim = detailAnim ?: return
            if (backstack.lastOrNull() == anim.screen && backstack.size > 1) {
                backstack.removeAt(backstack.lastIndex)
            }
            if (detailAnim?.screen == anim.screen) {
                detailAnim = null
            }
        }
        fun dismissSheet() {
            val anim = sheetAnim ?: return
            if (vm.popupEntityId == anim.id) vm.closePopup()
            if (sheetAnim?.id == anim.id) sheetAnim = null
        }
        fun dismissPopout() {
            val anim = popoutAnim ?: return
            if (vm.climatePopoutId == anim.id) vm.closeClimatePopout()
            if (popoutAnim?.id == anim.id) popoutAnim = null
        }
        val goBack: () -> Unit = {
            if (detailAnim != null) dismissDetail()
            else if (popoutAnim != null) dismissPopout()
            else if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
        }
        val signOut: () -> Unit = {
            vm.pageOrigin = null
            vm.closePopup()
            sheetAnim = null
            detailAnim = null
            popoutAnim = null
            vm.closeClimatePopout()
            vm.signOut()
            backstack.clear()
            backstack.add(Screen.Today)
        }
        BackHandler(enabled = backstack.size > 1) { goBack() }
        // Morph open effects: the morph objects are captured
        // synchronously above (falling back to a fade-only appearance
        // when the card never reported bounds, e.g. a board card);
        // these effects animate progress 0 -> 1 on the bounds spring
        // (snapped under reduce-motion). There is no exit animation:
        // dismiss drops the anim state instantly (instant return —
        // see dismissDetail/dismissSheet/dismissPopout), and external
        // clears (goTop, signOut) do the same.
        // Synchronous capture (mirrors #49's lastDetail/lastPopupId):
        // the morph object exists in the same frame its trigger
        // appears, so the overlay renders (at progress 0, i.e. exactly
        // over the hidden card) with no one-frame gap. The effects
        // below own only the progress animation.
        if (vm.popupEntityId != null && sheetAnim?.id != vm.popupEntityId) {
            sheetAnim = SheetAnim(vm.popupEntityId!!, cardBounds[vm.popupEntityId!!])
        }
        if (detailScreen != null && detailAnim?.screen != detailScreen) {
            detailAnim = DetailAnim(detailScreen, detailEntityId(detailScreen), cardBounds[detailEntityId(detailScreen)])
        }
        LaunchedEffect(vm.popupEntityId) {
            val id = vm.popupEntityId
            val anim = sheetAnim
            if (id != null && anim?.id == id) {
                sheetDrag.snapTo(0f)
                // Always animate (unless reduce-motion): a sourced
                // morph interpolates from the card, a sourceless one
                // from the graceful fallback rect — never an instant
                // appearance.
                if (!reduceMotion) {
                    sheetProgress.snapTo(0f)
                    sheetProgress.animateTo(1f, springBounds())
                } else sheetProgress.snapTo(1f)
            } else if (id == null && anim != null) {
                sheetAnim = null
            }
        }
        LaunchedEffect(detailScreen?.let { detailEntityId(it) }) {
            val anim = detailAnim
            if (detailScreen != null && anim != null && detailEntityId(anim.screen) == detailEntityId(detailScreen)) {
                if (!reduceMotion) {
                    detailProgress.snapTo(0f)
                    detailProgress.animateTo(1f, springBounds())
                } else detailProgress.snapTo(1f)
            } else if (detailScreen == null && anim != null) {
                detailAnim = null
            }
        }
        // Thermostat pop-out open effect (mirrors the sheet effect):
        // capture synchronously, animate progress 0 -> 1 on the bounds
        // spring (snapped under reduce-motion).
        if (vm.climatePopoutId != null && popoutAnim?.id != vm.climatePopoutId) {
            popoutAnim = PopoutAnim(vm.climatePopoutId!!, cardBounds[vm.climatePopoutId!!])
        }
        LaunchedEffect(vm.climatePopoutId) {
            val id = vm.climatePopoutId
            val anim = popoutAnim
            if (id != null && anim?.id == id) {
                if (!reduceMotion) {
                    popoutProgress.snapTo(0f)
                    popoutProgress.animateTo(1f, springBounds())
                } else popoutProgress.snapTo(1f)
            } else if (id == null && anim != null) {
                popoutAnim = null
            }
        }
        // Camera overlay transitions share the same language:
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
            // Morph locals: cards report bounds here, and the active
            // morph source hides here. The scaffold blur from #49 is
            // GONE: the pill now exits the viewport during modals (see
            // the pill-exit below), which replaces the blur as the answer
            // to the scrim problem, and the scrim alone dims the page.
            val modalActive = sheetAnim != null || detailAnim != null || popoutAnim != null
            // Scroll-away (production-upgrade round): the pill slides
            // out of view on a downward scroll past 8dp and returns on
            // an upward scroll past 8dp (the spec sample's thresholds,
            // converted to pixels). Combined with the modal rule, the
            // bar is visible exactly when no modal is open AND
            // scrolling has not hidden it. The connection is attached
            // to the scaffold content only, so sheet/detail inner
            // scrolls never move the bar. PillSlideSpring
            // (0.85/MediumLow) maps onto springSpatial, the single
            // pill-motion token — no new token. (The state itself is
            // hoisted above goTop so tab switches can reset it.)
            val pillHidden = modalActive || !scrollVisibility.isVisible
            val pillExit by animateFloatAsState(
                if (pillHidden) 1f else 0f,
                animationSpec = if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f) else springSpatial(),
                label = "pillExit",
            )
            CompositionLocalProvider(
                LocalCardBounds provides cardBounds,
                LocalHiddenCardId provides (sheetAnim?.id ?: detailAnim?.entityId ?: popoutAnim?.id),
            ) {
            // Overlay root: its root-coordinate origin and size anchor
            // every morph rect (see MorphSurface). All morph overlays
            // below render inside this Box, above the scaffold.
            var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
            var overlaySize by remember { mutableStateOf(IntSize.Zero) }
            Box(
                Modifier.fillMaxSize().onGloballyPositioned {
                    overlayOrigin = it.boundsInRoot().topLeft
                    overlaySize = it.size
                },
            ) {
            Scaffold(
            // Transparent container: the default background painted a
            // full-width opaque band in the reserved bottom-bar zone —
            // near-white in light mode, the "white box" that sliced
            // scrolling content at its top edge. With a transparent
            // scaffold AND content extending full-height (bottom
            // padding zeroed below), lists scroll visibly beneath the
            // floating pill, which carries its own shadow; no box edge
            // ever cuts an element mid-scroll. Lists carry their own
            // bottom clearance (pillClearanceBottom) so the last item
            // still scrolls fully above the pill.
            containerColor = Color.Transparent,
            // Screens consume the status-bar inset themselves
            // (statusBarsPadding on each root), so the Scaffold must not add
            // it again — otherwise headers sit a full inset too low. The
            // bottom-bar height is NOT reserved in content anymore (see
            // the zeroed bottom padding below); lists carry their own
            // clearance instead.
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
                        // Floating island, auto-width and centered
                        // (production-upgrade round, superseding #49's
                        // full-width-minus-margins bar): the navigation
                        // bars window inset, then 20dp bottom spacing
                        // above the insets, never pinned edge-to-edge.
                        modifier = Modifier.fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 20.dp)
                            // Pill exit: during any modal, or while
                            // scrolled away, the pill fades out and
                            // slides 96dp down out of the viewport on
                            // the pill spring; it returns when the
                            // modal closes or the user scrolls back
                            // up. The scrim (fillMaxSize, above the
                            // scaffold) already intercepts touches, so
                            // the exiting pill is never tappable
                            // mid-modal.
                            .graphicsLayer {
                                alpha = 1f - pillExit
                                translationY = 96.dp.toPx() * pillExit
                            },
                    )
                }
            },
        ) { padding ->
            // The scaffold's padding is only the bottom-bar height
            // (contentWindowInsets are zeroed above); it is deliberately
            // NOT applied at the bottom, so tab/Home lists extend
            // full-height and scroll beneath the floating pill (see the
            // transparent container above). Start/top/end are kept
            // from the scaffold for correctness.
            val layoutDirection = LocalLayoutDirection.current
            val contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = 0.dp,
            )
            // Scroll-away lives here: nested scrolls from the tab/Home
            // lists reach this Box (sheet/detail overlays are siblings
            // outside it, so their inner scrolls never move the bar).
            Box(Modifier.fillMaxSize().padding(contentPadding).nestedScroll(scrollVisibility.nestedScrollConnection)) {
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
            // Sheet morph overlay: one scrim plus one morphing surface.
            // The scrim fades with progress and blocks touches from the
            // first frame (even during the measurement frame below,
            // when progress is still 0). The surface interpolates a
            // single rect from the card captured at open to the measured
            // panel rect; the source card underneath renders alpha 0
            // for the whole morph, so nothing ever doubles. Dismissing
            // is instant (no exit morph — see dismissSheet): the
            // surface vanishes and the card reappears in the same
            // frame; the sheet -> detail handoff clears the sheet
            // instantly and the detail morph starts from the same
            // card, so the card never flashes back in between.
            if (sheetAnim != null) {
                val anim = sheetAnim!!
                BackHandler { dismissSheet() }
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * sheetProgress.value))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { dismissSheet() },
                )
                val targetSize = anim.targetSize
                if (targetSize == null) {
                    // Measurement frame: the panel content, invisible,
                    // laid out once at its final width so its height can
                    // be captured. Below the scrim, so it can neither
                    // flash nor take touches; replaced by the morph
                    // surface on the next frame.
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth()
                                .heightIn(max = screenHeight * 0.92f)
                                .alpha(0f)
                                .onGloballyPositioned { anim.targetSize = it.size },
                        ) {
                            EntityPopupSheetContent(
                                vm, navigate, anim.id, dragging = false,
                                sheetBounds = sheetSurfaceBounds,
                                onClose = { dismissSheet() },
                                onDetails = { dismissSheet() },
                            )
                        }
                    }
                } else {
                    val target = sheetTargetRect(overlayOrigin, overlaySize, targetSize)
                    val density = LocalDensity.current
                    // Current bounds first, captured rect as fallback:
                    // cards re-report on relayout, so rotation
                    // mid-morph and cards that moved between open and
                    // dismiss track correctly instead of landing on a
                    // stale rect and snapping. Sourceless cards morph
                    // from the graceful fallback (target + 80dp down),
                    // never an instant appearance.
                    val source = cardBounds[anim.id] ?: anim.source
                    // Geometry progress is clamped: the bounds spring
                    // overshoots past 1 (0.8 damping). lerpRect clamps
                    // internally; this makes it explicit and shared
                    // with the weather content fade below.
                    val sheetP = sheetProgress.value.coerceIn(0f, 1f)
                    val rect = if (source != null) lerpRect(source, target, sheetP)
                        else lerpRect(fallbackFromRect(target, with(density) { 80.dp.toPx() }), target, sheetP)
                    val dismissThresholdPx = with(density) { 120.dp.toPx() }
                    MorphSurface(
                        rect = rect,
                        overlayOrigin = overlayOrigin,
                        targetSize = targetSize,
                        cornerTop = 28.dp,
                        // Every morph fades its content with the steep
                        // curve (see morphContentAlpha): the #53
                        // weather-only linear fade did not satisfy the
                        // verdict — the clipping/doubling read appears
                        // on sheets AND detail pages wherever the
                        // destination duplicates its card — so the
                        // fade is now general, and the Koda
                        // reveal-as-it-grows rule is superseded for
                        // morph content.
                        contentAlpha = morphContentAlpha(sheetP),
                        background = MaterialTheme.colorScheme.surfaceContainerHigh,
                        fromBackground = MaterialTheme.colorScheme.surfaceContainerLow,
                        backgroundBlend = morphBackgroundBlend(sheetP),
                        modifier = Modifier.pointerInput(anim.id) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (sheetDrag.value > dismissThresholdPx) {
                                        dismissSheet()
                                    } else {
                                        // Gesture cancel: spring back to
                                        // rest. This is NOT a return
                                        // morph — the instant-return
                                        // rule applies to dismiss, not
                                        // to cancelling a drag.
                                        scope.launch { sheetDrag.animateTo(0f, springMorph()) }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { sheetDrag.animateTo(0f, springMorph()) }
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                scope.launch { sheetDrag.snapTo((sheetDrag.value + dragAmount).coerceAtLeast(0f)) }
                            }
                        },
                        dragOffsetY = sheetDrag.value,
                        onPlaced = { sheetSurfaceBounds = it },
                    ) {
                        EntityPopupSheetContent(
                            vm, navigate, anim.id, dragging = sheetDrag.value > 1f,
                            sheetBounds = sheetSurfaceBounds,
                            onClose = { dismissSheet() },
                            onDetails = { dismissSheet() },
                        )
                    }
                }
            }
            // Detail morph overlay: the page grows out of its card on
            // the bounds spring and draws above all chrome (the old
            // bottom-sheet rise for detail pages is gone — the
            // visual-bugs round replaced it with the container morph,
            // and this round replaces the morph mechanism itself).
            // Back is instant (see dismissDetail): the surface
            // vanishes and the card reappears in the same frame —
            // no exit shrink, no staged reappearance.
            if (detailAnim != null) {
                val anim = detailAnim!!
                val target = detailTargetRect(overlayOrigin, overlaySize)
                val density = LocalDensity.current
                // Same current-bounds-first rule as sheets (see above):
                // rotation mid-morph and moved cards track correctly,
                // sourceless cards rise from the graceful fallback.
                val source = cardBounds[anim.entityId] ?: anim.source
                // Clamped geometry progress (see the sheet block):
                // the bounds spring overshoots past 1, and the raw
                // value fed the corner formula below went negative.
                val detailP = detailProgress.value.coerceIn(0f, 1f)
                val rect = if (source != null) lerpRect(source, target, detailP)
                    else lerpRect(fallbackFromRect(target, with(density) { 80.dp.toPx() }), target, detailP)
                MorphSurface(
                    rect = rect,
                    overlayOrigin = overlayOrigin,
                    targetSize = overlaySize,
                    cornerTop = morphCornerTop(detailP),
                    background = MaterialTheme.colorScheme.background,
                    contentAlpha = morphContentAlpha(detailP),
                    fromBackground = MaterialTheme.colorScheme.surfaceContainerLow,
                    backgroundBlend = morphBackgroundBlend(detailP),
                ) {
                    content(anim.screen)
                }
            }
            // Thermostat pop-out: a centered expressive morph (not a
            // bottom sheet, not a full page). The panel is measured
            // once at 16dp side margins, then centered in the overlay;
            // the morph runs card -> centered panel on enter and
            // returns instantly on dismiss (no exit morph — see
            // dismissPopout), with the same steep content fade and
            // current-bounds-first rule as sheets and details. The
            // morph surface itself is transparent: the panel draws its
            // own shapes (the scallop disc is the pop-out's own
            // background, inside a 32dp rounded panel).
            if (popoutAnim != null) {
                val anim = popoutAnim!!
                BackHandler { dismissPopout() }
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * popoutProgress.value))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { dismissPopout() },
                )
                val targetSize = anim.targetSize
                if (targetSize == null) {
                    // Measurement frame: invisible, at the panel's final
                    // width (overlay minus 16dp margins), height capped
                    // at 92% of the overlay. Below the scrim, so it can
                    // neither flash nor take touches.
                    val density = LocalDensity.current
                    val maxHeight = with(density) { overlaySize.height.toDp() * 0.92f }
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.fillMaxWidth()
                                .heightIn(max = maxHeight)
                                .alpha(0f)
                                .onGloballyPositioned { anim.targetSize = it.size },
                        ) {
                            ThermostatPopoutContent(
                                vm, anim.id,
                                onDetails = {
                                    dismissPopout()
                                    navigate(Screen.Thermostat(anim.id))
                                },
                                onClose = { dismissPopout() },
                            )
                        }
                    }
                } else {
                    val target = centerTargetRect(overlayOrigin, overlaySize, targetSize)
                    val density = LocalDensity.current
                    val source = cardBounds[anim.id] ?: anim.source
                    val popoutP = popoutProgress.value.coerceIn(0f, 1f)
                    val rect = if (source != null) lerpRect(source, target, popoutP)
                        else lerpRect(fallbackFromRect(target, with(density) { 80.dp.toPx() }), target, popoutP)
                    MorphSurface(
                        rect = rect,
                        overlayOrigin = overlayOrigin,
                        targetSize = targetSize,
                        cornerTop = 0.dp,
                        contentAlpha = morphContentAlpha(popoutP),
                        background = Color.Transparent,
                    ) {
                        // The pop-out panel's own background blends
                        // from the card color over the first 30% of
                        // enter (same morphBackgroundBlend rule as
                        // sheets/details) so the pop-out grows out of
                        // the card instead of flashing in white.
                        val popoutPanelBg = lerp(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            morphBackgroundBlend(popoutP),
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                popoutPanelBg,
                                RoundedCornerShape(32.dp),
                            ),
                        ) {
                            ThermostatPopoutContent(
                                vm, anim.id,
                                onDetails = {
                                    dismissPopout()
                                    navigate(Screen.Thermostat(anim.id))
                                },
                                onClose = { dismissPopout() },
                            )
                        }
                    }
                }
            }
            } // overlay root Box
            } // CompositionLocalProvider (morph locals)
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
        is Screen.CameraClips -> CameraClipsScreen(vm, screen.entityId, goBack)
        is Screen.AreaDetail -> AreaDetailScreen(vm, screen.areaId, goBack, navigate)
        Screen.DashboardEdit -> DashboardEditScreen(vm, goBack)
        Screen.AutomationBuilder -> AutomationBuilderScreen(vm, goBack)
        Screen.SceneBuilder -> SceneBuilderScreen(vm, goBack)
        Screen.ScriptBuilder -> ScriptBuilderScreen(vm, goBack)
        Screen.HelperBuilder -> HelperBuilderScreen(vm, goBack)
    }
}

/** Thermostat pop-out content: looks the entity up (so setpoint/mode
 * changes recompose the open pop-out) and renders the pop-out, or a
 * fallback if the entity vanished while open. */
@Composable
private fun ThermostatPopoutContent(
    vm: HavenViewModel,
    entityId: String,
    onDetails: () -> Unit,
    onClose: () -> Unit,
) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    if (entity == null) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    } else {
        ThermostatPopout(entity, vm, onDetails, onClose)
    }
}

/**
 * The sheet panel's CONTENT (not the panel itself): the morph surface
 * in HavenApp owns positioning, scrim, and drag; this is the column
 * laid out inside it at the panel's measured size. Kept separate so
 * the same content serves the one-frame measurement pass and the morph
 * surface. [dragging] drives the dynamic handle (wide/thin while
 * entering or dragging, narrow/thick at rest). [sheetBounds] is the
 * surface's window rect for the camera rise origin on Details.
 * [onClose] dismisses instantly (instant return — no exit morph);
 * [onDetails] dismisses instantly for the sheet -> detail handoff
 * (the detail morph starts from the same card, so the card never
 * flashes back in between).
 */
@Composable
private fun EntityPopupSheetContent(
    vm: HavenViewModel,
    navigate: (Screen) -> Unit,
    popupId: String,
    dragging: Boolean,
    sheetBounds: androidx.compose.ui.geometry.Rect?,
    onClose: () -> Unit,
    onDetails: () -> Unit,
) {
    val states by vm.states.collectAsState()
    val entity = states[popupId]
    val reduceMotion = LocalReduceMotion.current
    // Entering phase for the dynamic handle: wide/thin while the sheet
    // rises, narrow/thick at rest.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(popupId) {
        kotlinx.coroutines.delay(350)
        settled = true
    }
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
                        Text(entity.friendlyName, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            onDetails()
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
                        onClick = onClose,
                        shape = RoundedCornerShape(50),
                    ) { Text("Close") }
                }
            }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Fullscreen camera viewer.
 *
 * Live-only by design (his 2026-10-29 13:34 direction: never fall
 * back to the last recorded clip). Non-Ring cameras play their live
 * MJPEG stream exactly as before. Ring cameras negotiate a live
 * session in order: WebRTC (HA's own live routing, WS
 * camera/webrtc/offer) first, then HLS-live (WS camera/stream with a
 * 30s window — Ring session start is slow) — and on total live
 * failure show error + Retry. Recordings live on the long-press
 * clips timeline, never here.
 */
@Composable
private fun CameraFullscreen(vm: HavenViewModel, onClose: () -> Unit, enterProgress: Float) {
    val cameraId = vm.fullscreenCameraId ?: return
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    val isRing = entity?.let { isRingCamera(it) } == true
    BackHandler { onClose() }
    if (!isRing) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraMjpegBody(vm, cameraId, enterProgress, recording = false, onClose = onClose)
            CameraHeader(entity?.friendlyName ?: cameraId, badgeText = null, onClose = onClose)
        }
        return
    }
    // Ring live-only path.
    var attempt by remember(cameraId) { mutableStateOf(0) }
    var webrtcFailed by remember(cameraId, attempt) { mutableStateOf<String?>(null) }
    var webrtcPlaying by remember(cameraId, attempt) { mutableStateOf(false) }
    var hlsUrl by remember(cameraId, attempt) { mutableStateOf<String?>(null) }
    var hlsFailed by remember(cameraId, attempt) { mutableStateOf<String?>(null) }
    var hlsReady by remember(cameraId, attempt) { mutableStateOf(false) }
    var hlsPlayer by remember(cameraId, attempt) { mutableStateOf<ExoPlayer?>(null) }
    var renderer by remember(cameraId, attempt) { mutableStateOf<SurfaceViewRenderer?>(null) }
    val context = LocalContext.current
    // Plain (non-state) holder for this attempt's renderer view. The
    // DisposableEffect below captures it, so the old session is
    // closed with its own view even though the renderer STATE resets
    // to null during the Retry recomposition before onDispose runs
    // (reading state in onDispose would skip the close and leak the
    // failed session on every Retry).
    val rendererHolder = remember(cameraId, attempt) { object { var view: SurfaceViewRenderer? = null } }
    // One WebRTC session per attempt; disposed (locally torn down)
    // on Retry and on view close. No WS close command is sent: no
    // confirmed camera/webrtc/close command exists, and the HA-side
    // session expires on its own.
    val webrtcSession = remember(cameraId, attempt) {
        RingWebRtcSession(
            context.applicationContext,
            sendOffer = { offer, cb -> vm.client.requestWebRtcOffer(cameraId, offer, cb) },
            sendCandidate = { sessionId, candidate -> vm.client.sendWebRtcCandidate(cameraId, sessionId, candidate) },
        )
    }
    DisposableEffect(cameraId, attempt) {
        val session = webrtcSession
        val holder = rendererHolder
        onDispose {
            holder.view?.let { session.close(it) }
        }
    }
    // Start the session once both the session and the renderer exist.
    LaunchedEffect(webrtcSession, renderer) {
        val view = renderer ?: return@LaunchedEffect
        try {
            webrtcSession.start(
                view,
                onFirstFrame = { webrtcPlaying = true },
                onFailed = { stage -> if (webrtcFailed == null) webrtcFailed = stage },
            )
        } catch (e: Exception) {
            // Defense in depth beside the session's own init guard:
            // any throw here becomes the named error UI + HLS-live
            // fallback, never a crash dialog.
            if (webrtcFailed == null) webrtcFailed = "init: ${e.message ?: e.javaClass.simpleName}"
        }
    }
    // WebRTC negotiation guard: 20s for offer + answer + first track.
    // Without it a stalled negotiation would sit on "Loading" forever.
    LaunchedEffect(cameraId, attempt) {
        kotlinx.coroutines.delay(20_000)
        if (!webrtcPlaying && webrtcFailed == null) webrtcFailed = "negotiation timed out"
    }
    // HLS-live, second path, only after WebRTC has failed. The 30s
    // window (up from the old 10s) is deliberate: Ring SIP + ffmpeg
    // session start is slow, and a short window was the most plausible
    // cause of the earlier HLS failures. Late answers are dropped so
    // no second player appears beside the first.
    LaunchedEffect(cameraId, attempt, webrtcFailed) {
        if (webrtcFailed == null) return@LaunchedEffect
        var answered = false
        val timeout = launch {
            kotlinx.coroutines.delay(30_000)
            if (!answered) hlsFailed = "negotiation timed out"
        }
        vm.client.requestHlsUrl(cameraId) { url ->
            if (answered) return@requestHlsUrl
            answered = true
            timeout.cancel()
            if (hlsFailed != null) return@requestHlsUrl
            if (url != null) hlsUrl = url else hlsFailed = "no stream URL"
        }
    }
    DisposableEffect(cameraId, attempt, hlsUrl) {
        val url = hlsUrl ?: return@DisposableEffect onDispose {}
        val player = vm.client.createHlsPlayer(context, url)
        hlsPlayer = player
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) hlsReady = true
            }
            override fun onPlayerError(error: PlaybackException) {
                hlsUrl = null
                hlsFailed = "player error"
            }
        })
        onDispose {
            hlsPlayer = null
            player?.release()
        }
    }
    val live = webrtcPlaying || hlsReady
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // WebRTC renderer: always composed (the session needs it from
        // the start), invisible until the first frame lands, so the
        // connecting UI covers it meanwhile. Keyed on attempt so a
        // Retry gets a fresh view (the old session released the old
        // one); the holder feeds the session-close effect above.
        androidx.compose.runtime.key(attempt) {
            AndroidView(
                factory = { ctx -> SurfaceViewRenderer(ctx) },
                update = {
                    rendererHolder.view = it
                    renderer = it
                },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val s = 0.94f + 0.06f * enterProgress
                scaleX = s
                scaleY = s
                alpha = if (webrtcPlaying) 0.7f + 0.3f * enterProgress else 0f
            },
            )
        }
        // HLS-live player, only while it is the live path. The update
        // block re-binds the player on recomposition: without it the
        // factory-captured player goes stale when the HLS attempt
        // changes, and the view can show a released player.
        if (hlsPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = hlsPlayer
                        useController = false
                    }
                },
                update = { it.player = hlsPlayer },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val s = 0.94f + 0.06f * enterProgress
                    scaleX = s
                    scaleY = s
                    alpha = if (hlsReady) 0.7f + 0.3f * enterProgress else 0f
                },
            )
        }
        if (!live) {
            if (webrtcFailed != null && hlsFailed != null) {
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Live view unavailable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "WebRTC: $webrtcFailed\nHLS: $hlsFailed",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { attempt++ }) { Text("Retry") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                }
            } else {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(64.dp).background(Color.White.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("◉", color = Color.White, fontSize = 28.sp) }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (webrtcFailed == null) "Loading live view…"
                        else "WebRTC unavailable — trying live stream…",
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                }
            }
        }
        // Badge honesty, same rule as before but for live only:
        // "Live" only when a live stream is actually rendering —
        // never on negotiation alone, never for a recording.
        CameraHeader(
            entity?.friendlyName ?: cameraId,
            badgeText = if (live) "Live" else null,
            onClose = onClose,
        )
    }
}

/**
 * Shared MJPEG body: stream effect + Playing / Connecting / Error
 * UI, used by non-Ring fullscreen (live) and by the recording
 * fullscreen (a timeline entry). [recording] switches the copy only;
 * a recording view must never say "live".
 */
@Composable
private fun CameraMjpegBody(
    vm: HavenViewModel,
    cameraId: String,
    enterProgress: Float,
    recording: Boolean,
    onClose: () -> Unit,
) {
    var streamState by remember(cameraId) { mutableStateOf<CameraStreamState>(CameraStreamState.Connecting) }
    var attempt by remember(cameraId) { mutableStateOf(0) }
    LaunchedEffect(cameraId, attempt) {
        val job = vm.client.streamCamera(cameraId) { state ->
            if (state is CameraStreamState.Playing) vm.cacheCameraFrame(cameraId, state.frame)
            streamState = state
        }
        try {
            job.join()
        } finally {
            job.cancel()
        }
    }
    val last = vm.lastCameraFrame(cameraId)
    when (val state = streamState) {
        is CameraStreamState.Playing -> {
            Image(
                bitmap = state.frame.asImageBitmap(),
                contentDescription = null,
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
            if (last != null) {
                Image(
                    bitmap = last.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.25f },
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(120.dp, 8.dp).background(
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(4.dp),
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (recording) "Loading recording…"
                        else if (last != null) "Reconnecting…" else "Loading camera…",
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                }
            }
        }
        is CameraStreamState.Error -> {
            if (last != null) {
                Image(
                    bitmap = last.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.25f },
                )
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (recording) "Recording unavailable" else "Live view unavailable",
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
}

/** Fullscreen name + live/recording badge (top-left) and close (top-right). */
@Composable
private fun BoxScope.CameraHeader(name: String, badgeText: String?, onClose: () -> Unit) {
    Column(
        Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
    ) {
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (badgeText != null) {
            Spacer(Modifier.height(4.dp))
            Text(badgeText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
    IconButton(
        onClick = onClose,
        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
    ) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White) }
}

/**
 * Fullscreen MJPEG playback of a recorded clip (timeline entries):
 * explicitly a recording — badge says "Recording", never "Live".
 * Public so the clips timeline screen can host it.
 */
@Composable
fun RecordingFullscreen(vm: HavenViewModel, recordingEntityId: String, onClose: () -> Unit, enterProgress: Float) {
    val states by vm.states.collectAsState()
    val entity = states[recordingEntityId]
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraMjpegBody(vm, recordingEntityId, enterProgress, recording = true, onClose = onClose)
        CameraHeader(entity?.friendlyName ?: recordingEntityId, badgeText = "Recording", onClose = onClose)
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
 * static label in favor of an icon badge (selected: icon + label row,
 * appearing with no animation — labels switch in the same recomposition
 * as the selection, so two labels can never overlap; unselected: icon
 * only) — the old "Living Room lig" truncation is gone by construction.
 * The shell is a floating island: 32dp corners, surfaceContainer, subtle
 * shadow, 16dp horizontal and 12dp bottom padding at the call site.
 * Dragging along the bar still scrubs the highlight (haptic
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
    // Auto-width floating island (production-upgrade round): the bar
    // is only as wide as its items, centered, capped at the screen
    // width (beyond which the row scrolls exactly as before). Raised
    // per the spec sample: surfaceContainerHigh at 0.96 alpha, tonal
    // 8dp, shadow 16dp. The measured sliding indicator, instant label
    // switch, and >10ch icon-only rule are unchanged.
    Box(modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints {
            Surface(
                modifier = Modifier.wrapContentWidth().widthIn(max = maxWidth),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
            ) {
                Box(
                    Modifier.wrapContentWidth()
                        .onGloballyPositioned { barOrigin = it.boundsInWindow().left },
                ) {
            // The morphing indicator, behind the items. It is padded
            // 8dp beyond the measured item rect on each side: item
            // width snaps instantly at selection while the indicator
            // glides (the #51 trade), so during the glide a label can
            // briefly extend past an exactly-sized indicator and read
            // as text overflowing its shape background. The padding
            // absorbs that transient overflow (2026-10-29 verdict:
            // "the text overlaps [the] — this expressive shape
            // background should be larger").
            if (targetRect != null) {
                val indicatorPad = with(density) { 8.dp.toPx() }
                Box(
                    Modifier.align(Alignment.CenterStart)
                        .offset { IntOffset((indicatorLeft - indicatorPad).roundToInt(), 0) }
                        .width(with(density) { (indicatorRight - indicatorLeft + indicatorPad * 2).toDp() })
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                )
            }
            // Scrolls horizontally once tabs overflow; short bars stay centered.
            Row(
                modifier = Modifier.wrapContentWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp).pillScrub(scrub),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((index, item) in items.withIndex()) {
                    // Highlight follows the scrub hover (the indicator
                    // glides under the finger); the LABEL follows only
                    // the committed selection, with no animation at all.
                    // The #49 vertical slide+fade still collided: while
                    // the label's size animated, its text clipped to
                    // "Thermo..." beside the fading outgoing label, and
                    // scrubbing flashed labels across tabs. A label now
                    // appears or vanishes in the same recomposition as
                    // the selection change, so two labels can never
                    // coexist in one frame; the indicator's glide is the
                    // only motion. Trade accepted: item width snaps
                    // instantly at selection while the indicator glides
                    // (masked by the indicator's arrival).
                    val highlighted = (scrub.hoveredTab ?: currentIndex) == index
                    val showLabel = pillLabelVisible(item.label, index == currentIndex)
                    val contentColor = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer
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
                        if (showLabel) {
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
                                    if (highlighted) item.icon else item.outlinedIcon,
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
