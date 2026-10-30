package com.garrietta.haven
import com.garrietta.haven.ui.theme.FloatingCorner

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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.CameraStreamState
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.TravelingBadge
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
import com.garrietta.haven.ui.components.LocalArcBounds
import com.garrietta.haven.ui.components.LocalArcTarget
import com.garrietta.haven.ui.components.LocalCardBounds
import com.garrietta.haven.ui.components.LocalHeroBounds
import com.garrietta.haven.ui.components.LocalHeroTarget
import com.garrietta.haven.ui.components.LocalHiddenCardId
import com.garrietta.haven.ui.components.LocalHiddenHeroId
import com.garrietta.haven.ui.components.LocalMorphCardAlpha
import com.garrietta.haven.ui.components.MorphSurface
import com.garrietta.haven.ui.components.centerTargetRect
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.morphBackgroundAlpha
import com.garrietta.haven.ui.components.morphBackgroundBlend
import com.garrietta.haven.ui.components.morphCardAlpha
import com.garrietta.haven.ui.components.morphContentAlpha
import com.garrietta.haven.ui.components.weatherConditionColor
import com.garrietta.haven.ui.components.ScrollVisibilityState
import com.garrietta.haven.ui.components.fallbackFromRect
import com.garrietta.haven.ui.components.heroHeaderAlpha
import com.garrietta.haven.ui.components.lerpRect
import com.garrietta.haven.ui.components.morphPopupCorner
import com.garrietta.haven.ui.components.popupTargetRect
import com.garrietta.haven.ui.theme.springBounds
import com.garrietta.haven.ui.theme.springSpatial
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.formatTemp
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
        // Traveling-header state (2026-10-29 pop-up round): cards
        // report header rects here, measurement frames report their
        // pop-up header target here (id + rect, so a stale target from
        // a previous morph can never steer the next one).
        val heroBounds = remember { mutableStateMapOf<String, Rect>() }
        var heroTarget by remember { mutableStateOf<Pair<String, Rect>?>(null) }
        // Traveling-arc state (2026-10-29 verdict-fix round): the
        // weather dial panel's source rects and pop-up target rect,
        // mirroring the traveling-header state above. Reset on every
        // dismiss path alongside heroTarget (see the reset sites).
        val arcBounds = remember { mutableStateMapOf<String, Rect>() }
        var arcTarget by remember { mutableStateOf<Pair<String, Rect>?>(null) }
        var sheetAnim by remember { mutableStateOf<SheetAnim?>(null) }
        var detailAnim by remember { mutableStateOf<DetailAnim?>(null) }
        val sheetProgress = remember { Animatable(0f) }
        val detailProgress = remember { Animatable(0f) }
        val sheetDrag = remember { Animatable(0f) }
        var popoutAnim by remember { mutableStateOf<PopoutAnim?>(null) }
        val popoutProgress = remember { Animatable(0f) }
        var sheetSurfaceBounds by remember { mutableStateOf<Rect?>(null) }
        // Morph generation + exiting flags (restored from #52, for
        // the animated exit below): an exit animation is async
        // again, so a top-level navigation (goTop/signOut) or a
        // handoff must be able to cancel it. goTop/signOut bump the
        // generation and clear instantly; an in-flight exit
        // coroutine checks the generation + identity in its finally
        // block and only clears state that still belongs to the
        // morph that started. The exiting flags keep a second
        // dismiss (double back-press, scrim + Close together) from
        // launching a second animation on the same progress
        // Animatable.
        var morphGeneration by remember { mutableIntStateOf(0) }
        var sheetExiting by remember { mutableStateOf(false) }
        var detailExiting by remember { mutableStateOf(false) }
        var popoutExiting by remember { mutableStateOf(false) }
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
            morphGeneration++
            vm.pageOrigin = null
            vm.closePopup()
            sheetAnim = null
            detailAnim = null
            popoutAnim = null
            heroTarget = null
            arcTarget = null
            vm.closeClimatePopout()
            scrollVisibility.reset()
            backstack.clear()
            backstack.add(it)
        }
        val scope = rememberCoroutineScope()
        // CONTAINER-TRANSFORM EXIT (2026-10-29 natural-animations
        // round — SUPERSEDES #62's instant return, explicitly).
        // #62 chose his first offered option (immediately return),
        // but on device the instant reads as "a sudden smack back to
        // the previous state": the surface vanishes and the card
        // reappears in one frame, which is a snap, not a transition.
        // The fix is his second option: exit is the smooth reverse
        // of enter on the same bounds spring, with elements present
        // throughout — destination content dissolves (the reverse of
        // the late enter fade), the surface background dissolves onto
        // the card (the reverse of the background-alpha ramp), and
        // the card fades back in whole (the reverse of the early
        // card fade-out). No snap, no blank panel, no staged
        // reappearance, no doubling: at no frame are both contents at
        // full opacity, and at no frame is the panel blank-white.
        // Enter morphs are unchanged in shape (grow + background
        // blend); only the content/background/card alpha curves
        // changed (see MorphOverlay). The sheet drag spring-back on
        // release-below-threshold is a gesture cancel, not a return
        // morph, and is kept; release-past-threshold flings the
        // sheet off the bottom (see the sheet block). The camera
        // rise/sink keeps its own exit animation (separate path).
        // Instant clears exist ONLY for handoffs and top-level
        // navigation: a sheet -> detail (or pop-out -> page) handoff
        // MUST clear instantly, because the destination morph starts
        // from the same card and an animated sheet exit would
        // overlap the detail enter.
        fun dismissDetailInstant() {
            val anim = detailAnim ?: return
            if (backstack.lastOrNull() == anim.screen && backstack.size > 1) {
                backstack.removeAt(backstack.lastIndex)
            }
            if (detailAnim?.screen == anim.screen) {
                detailAnim = null
            }
            // Fresh measurement on the next open: a stale target rect
            // (e.g. from before a rotation) would otherwise steer the
            // next morph's traveling header to the wrong place.
            heroTarget = null
            arcTarget = null
        }
        fun dismissSheetInstant() {
            val anim = sheetAnim ?: return
            if (vm.popupEntityId == anim.id) vm.closePopup()
            if (sheetAnim?.id == anim.id) sheetAnim = null
            heroTarget = null
            arcTarget = null
        }
        fun dismissPopoutInstant() {
            val anim = popoutAnim ?: return
            if (vm.climatePopoutId == anim.id) vm.closeClimatePopout()
            if (popoutAnim?.id == anim.id) popoutAnim = null
            heroTarget = null
            arcTarget = null
        }
        // Animated dismisses: Back, scrim tap, and Close all use
        // these. The progress animates 1 -> 0 on the bounds spring
        // (snapped under reduce-motion); only then is the trigger
        // cleared, and only if the generation + identity still
        // match the morph that started (see above).
        fun dismissDetail() {
            val anim = detailAnim ?: return
            if (detailExiting) return
            detailExiting = true
            val gen = morphGeneration
            scope.launch {
                try {
                    if (reduceMotion) detailProgress.snapTo(0f)
                    else detailProgress.animateTo(0f, springBounds())
                } finally {
                    detailExiting = false
                    if (gen == morphGeneration && detailAnim?.screen == anim.screen) {
                        dismissDetailInstant()
                    }
                }
            }
        }
        fun dismissSheet() {
            val anim = sheetAnim ?: return
            if (sheetExiting) return
            sheetExiting = true
            val gen = morphGeneration
            scope.launch {
                try {
                    if (reduceMotion) sheetProgress.snapTo(0f)
                    else sheetProgress.animateTo(0f, springBounds())
                } finally {
                    sheetExiting = false
                    if (gen == morphGeneration && sheetAnim?.id == anim.id) {
                        dismissSheetInstant()
                    }
                }
            }
        }
        fun dismissPopout() {
            val anim = popoutAnim ?: return
            if (popoutExiting) return
            popoutExiting = true
            val gen = morphGeneration
            scope.launch {
                try {
                    if (reduceMotion) popoutProgress.snapTo(0f)
                    else popoutProgress.animateTo(0f, springBounds())
                } finally {
                    popoutExiting = false
                    if (gen == morphGeneration && popoutAnim?.id == anim.id) {
                        dismissPopoutInstant()
                    }
                }
            }
        }
        val goBack: () -> Unit = {
            if (detailAnim != null) dismissDetail()
            else if (popoutAnim != null) dismissPopout()
            else if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
        }
        val signOut: () -> Unit = {
            morphGeneration++
            vm.pageOrigin = null
            vm.closePopup()
            sheetAnim = null
            detailAnim = null
            popoutAnim = null
            heroTarget = null
            arcTarget = null
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
        // (snapped under reduce-motion). Exit is the smooth reverse
        // (see dismissDetail/dismissSheet/dismissPopout): progress
        // animates 1 -> 0, then the trigger clears. External clears
        // (goTop, signOut) bump the generation and drop the anim
        // state instantly, cancelling any in-flight exit.
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
                heroTarget = null
                arcTarget = null
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
                heroTarget = null
                arcTarget = null
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
                heroTarget = null
                arcTarget = null
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
                LocalHeroBounds provides heroBounds,
                LocalArcBounds provides arcBounds,
                LocalHiddenHeroId provides (sheetAnim?.id ?: detailAnim?.entityId ?: popoutAnim?.id),
                // Progress-driven source-card alpha (container
                // transform): the active morph's progress drives the
                // card fade-out on enter and the fade-back-in on
                // exit, so the card and the surface read as one
                // element. No morph -> fully visible.
                LocalMorphCardAlpha provides morphCardAlpha(
                    when {
                        sheetAnim != null -> sheetProgress.value
                        detailAnim != null -> detailProgress.value
                        popoutAnim != null -> popoutProgress.value
                        else -> 1f
                    },
                ),
            ) {
            // Overlay root: its root-coordinate origin and size anchor
            // every morph rect (see MorphSurface). All morph overlays
            // below render inside this Box, above the scaffold.
            var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
            var overlaySize by remember { mutableStateOf(IntSize.Zero) }
            // Floating-shelf geometry (2026-10-29 pop-up round): every
            // pop-up sits 16dp in from each side and 20dp + the
            // navigation inset above the overlay bottom, so the dimmed
            // page stays visible around it on all sides.
            val shelfDensity = LocalDensity.current
            val shelfSideMarginPx = with(shelfDensity) { 16.dp.toPx() }
            val shelfBottomMarginPx = with(shelfDensity) { 20.dp.toPx() } +
                WindowInsets.navigationBars.getBottom(shelfDensity).toFloat()
            val shelfBottomMarginDp = with(shelfDensity) { shelfBottomMarginPx.toDp() }
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
            // DARK-MODE FIX (2026-10-29 pop-up round): this Box paints
            // the scheme background. The activity theme is a Light
            // parent (DayNight does not link in this material3
            // version) and the Scaffold is transparent, so the window's
            // white background used to show through on every page that
            // did not paint its own background — light pages carrying
            // dark-scheme cards and a dark pill. With the scheme
            // background here, page, cards, pill, sheets, and pop-ups
            // all come from ONE colorScheme per theme, in both light
            // and dark system themes. The pill still floats above it
            // on its own shadow; nothing about the floating bar
            // changes.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(contentPadding).nestedScroll(scrollVisibility.nestedScrollConnection)) {
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
            // POP-UP ROUND (2026-10-29): the sheet is a floating shelf
            // now — 16dp side margins, bottom margin above the pill
            // clearance — never a full-width bottom panel. The scrim
            // fades with progress and blocks touches from the first
            // frame (even during the measurement frame below, when
            // progress is still 0). The surface interpolates a
            // single rect from the card captured at open to the
            // measured shelf rect; the source card underneath fades
            // out over the first 40% of enter and back in over the
            // last 40% of exit (progress-driven alpha, not a binary
            // hide), so nothing ever doubles and nothing ever pops.
            // Dismiss is the smooth reverse of enter (see
            // dismissSheet); only the sheet -> detail handoff clears
            // instantly, so the detail morph starts from the same card
            // without the card flashing back in between. Dragging past
            // the threshold flings the sheet off the bottom (see
            // below) instead of shrinking it back.
            if (sheetAnim != null) {
                val anim = sheetAnim!!
                BackHandler { dismissSheet() }
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = 0.4f * sheetProgress.value *
                                    (1f - (sheetDrag.value / overlaySize.height.toFloat()).coerceIn(0f, 1f)),
                            ),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { dismissSheet() },
                )
                val targetSize = anim.targetSize
                if (targetSize == null) {
                    // Measurement frame: the panel content, invisible,
                    // laid out once at its final shelf geometry (same
                    // side/bottom margins as the surface below) so its
                    // height can be captured AND its header can report
                    // the traveling header's target rect. Below the
                    // scrim, so it can neither flash nor take touches;
                    // replaced by the morph surface on the next frame.
                    // The hero provider lives ONLY here: the surface
                    // content's header sits at the interpolated rect
                    // mid-morph and must never overwrite the target.
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    Box(
                        Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = shelfBottomMarginDp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier.fillMaxWidth()
                                .heightIn(max = screenHeight * 0.92f)
                                .alpha(0f)
                                .onGloballyPositioned { anim.targetSize = it.size },
                        ) {
                            CompositionLocalProvider(
                                LocalHeroTarget provides { r -> heroTarget = anim.id to r },
                                LocalArcTarget provides { r -> arcTarget = anim.id to r },
                            ) {
                                EntityPopupSheetContent(
                                    vm, navigate, anim.id, dragging = false,
                                    sheetBounds = sheetSurfaceBounds,
                                    onClose = { dismissSheet() },
                                    onDetails = { dismissSheetInstant() },
                                )
                            }
                        }
                    }
                } else {
                    val target = popupTargetRect(overlayOrigin, overlaySize, targetSize, shelfSideMarginPx, shelfBottomMarginPx)
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
                        // Floating shelf: all four corners morph
                        // 28dp -> 32dp across the whole animation
                        // (see morphPopupCorner) — the radius
                        // transitions smoothly, never snaps in.
                        cornerAll = morphPopupCorner(sheetP),
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
                        // The surface background itself ramps in over
                        // [0, 0.35] (see morphBackgroundAlpha): at
                        // progress 0 the surface is invisible and the
                        // card shows through it — no blank solid rect
                        // ever replaces the card in a single frame.
                        backgroundAlpha = morphBackgroundAlpha(sheetP),
                        progress = sheetP,
                        modifier = Modifier.pointerInput(anim.id) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (sheetDrag.value > dismissThresholdPx) {
                                        // Fling-off: the sheet slides
                                        // off the bottom on the morph
                                        // spring, then clears
                                        // instantly — the scrim is
                                        // already 0 by then (its alpha
                                        // is multiplied by 1 - drag /
                                        // height above), so no snap is
                                        // visible. Guarded like the
                                        // animated dismiss (generation
                                        // + identity in the finally).
                                        if (!sheetExiting) {
                                            sheetExiting = true
                                            val gen = morphGeneration
                                            scope.launch {
                                                try {
                                                    if (reduceMotion) sheetDrag.snapTo(overlaySize.height.toFloat())
                                                    else sheetDrag.animateTo(overlaySize.height.toFloat(), springMorph())
                                                } finally {
                                                    sheetExiting = false
                                                    if (gen == morphGeneration && sheetAnim?.id == anim.id) {
                                                        dismissSheetInstant()
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Gesture cancel: spring back to
                                        // rest. This is NOT a return
                                        // morph — the fling/dismiss
                                        // rules above do not apply to
                                        // cancelling a drag.
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
                            onDetails = { dismissSheetInstant() },
                        )
                    }
                    // Traveling header, above the surface: the card's
                    // badge/name/state travel to the sheet's header
                    // as ONE element (see TravelingHeader). Rendered
                    // only once the measurement frame has reported the
                    // target; before that the card header is still
                    // visible, so nothing is ever duplicated.
                    TravelingHeader(
                        vm, anim.id, heroBounds[anim.id],
                        heroTarget?.takeIf { it.first == anim.id }?.second,
                        sheetP, overlayOrigin,
                        onDismiss = { dismissSheet() },
                    )
                    // Traveling arc, above the surface (see
                    // TravelingArc): the weather card's dial panel
                    // travels into the pop-up's dial panel as ONE
                    // element, dissolving into it over the last 10%.
                    TravelingArc(
                        vm, anim.id, arcBounds[anim.id],
                        arcTarget?.takeIf { it.first == anim.id }?.second,
                        sheetP, overlayOrigin,
                    )
                }
            }
            // Detail morph overlay: POP-UP ROUND (2026-10-29) — the
            // full-span page is GONE. Every detail destination opens
            // as a bottom-anchored floating shelf, exactly like the
            // sheets above: 16dp side margins, bottom margin above
            // the pill clearance, 92% max height, all four corners
            // morphing 28dp -> 32dp. This supersedes the old
            // "weather's full page stands" exception: weather detail
            // is a pop-up too. The shelf grows out of its card on the
            // bounds spring and draws above all chrome; Back and
            // outside-tap are the smooth reverse of enter (see
            // dismissDetail) — the card fades back in whole, never a
            // snap, never staged. A dim scrim fades with progress
            // (tappable now: a floating pop-up dismisses on outside
            // tap, like sheets and the pop-out) so Home dims
            // progressively as the shelf grows instead of staying
            // fully bright behind it. The shelf's travel target comes
            // from a one-frame measurement pass at the shelf's final
            // geometry (see below): the detail content is fixed at
            // 92% height, so no size measurement is needed — only the
            // top bar's rect, which the surface content could never
            // report correctly mid-morph (its rect is interpolated).
            if (detailAnim != null) {
                val anim = detailAnim!!
                val density = LocalDensity.current
                // Clamped geometry progress (see the sheet block):
                // the bounds spring overshoots past 1, and the raw
                // value fed the corner formula below went negative.
                val detailP = detailProgress.value.coerceIn(0f, 1f)
                val detailSize = IntSize(
                    (overlaySize.width - 2 * shelfSideMarginPx).roundToInt(),
                    (overlaySize.height * 0.92f).roundToInt(),
                )
                val target = popupTargetRect(overlayOrigin, overlaySize, detailSize, shelfSideMarginPx, shelfBottomMarginPx)
                // Same current-bounds-first rule as sheets (see above):
                // rotation mid-morph and moved cards track correctly,
                // sourceless cards rise from the graceful fallback.
                val source = cardBounds[anim.entityId] ?: anim.source
                val rect = if (source != null) lerpRect(source, target, detailP)
                    else lerpRect(fallbackFromRect(target, with(density) { 80.dp.toPx() }), target, detailP)
                val targetHero = heroTarget?.takeIf { it.first == anim.entityId }?.second
                // Clickable dim scrim, above the scaffold and below
                // the shelf: outside taps dismiss the pop-up.
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * detailP)) // scrim dim: black overlay, not a scheme role
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { dismissDetail() },
                )
                if (targetHero == null) {
                    // Measurement frame: the detail content, invisible,
                    // laid out once at the shelf's final geometry so
                    // its top bar can report the traveling header's
                    // target rect. Below the scrim's alpha (progress
                    // is ~0 here), so it can neither flash nor take
                    // touches; replaced by the morph surface on the
                    // next frame. The hero provider lives ONLY here.
                    Box(
                        Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = shelfBottomMarginDp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier.fillMaxWidth()
                                .height(with(density) { detailSize.height.toDp() })
                                .alpha(0f),
                        ) {
                            CompositionLocalProvider(
                                LocalHeroTarget provides { r -> heroTarget = anim.entityId to r },
                                LocalArcTarget provides { r -> arcTarget = anim.entityId to r },
                            ) {
                                content(anim.screen)
                            }
                        }
                    }
                } else {
                    MorphSurface(
                        rect = rect,
                        overlayOrigin = overlayOrigin,
                        targetSize = detailSize,
                        cornerTop = 28.dp,
                        // Floating shelf: all four corners morph
                        // 28dp -> 32dp across the whole animation.
                        cornerAll = morphPopupCorner(detailP),
                        background = MaterialTheme.colorScheme.background,
                        contentAlpha = morphContentAlpha(detailP),
                        fromBackground = MaterialTheme.colorScheme.surfaceContainerLow,
                        backgroundBlend = morphBackgroundBlend(detailP),
                        backgroundAlpha = morphBackgroundAlpha(detailP),
                        progress = detailP,
                    ) {
                        content(anim.screen)
                    }
                    // Traveling header, above the surface (see the
                    // sheet block): the card's badge/name/state travel
                    // to the pop-up's top bar as ONE element.
                    TravelingHeader(
                        vm, anim.entityId, heroBounds[anim.entityId],
                        targetHero, detailP, overlayOrigin,
                        onDismiss = { dismissDetail() },
                    )
                    // Traveling arc (see the sheet block): the
                    // weather card's dial panel travels into the
                    // detail pop-up's dial panel as ONE element.
                    TravelingArc(
                        vm, anim.entityId, arcBounds[anim.entityId],
                        arcTarget?.takeIf { it.first == anim.entityId }?.second,
                        detailP, overlayOrigin,
                    )
                }
            }
            // Thermostat pop-out: a centered expressive morph (not a
            // bottom sheet, not a full page). The panel is measured
            // once at 16dp side margins, then centered in the overlay;
            // the morph runs card -> centered panel on enter and
            // reverses smoothly on dismiss (see dismissPopout), with
            // the same steep content fade and current-bounds-first
            // rule as sheets and details. The morph surface itself is
            // transparent: the panel draws its own shapes (the
            // scallop disc is the pop-out's own background, inside a
            // 32dp rounded panel), and the panel background ramps in
            // over [0, 0.35] like every other morph surface so no
            // solid rect ever replaces the card in a single frame.
            // Only the pop-out -> page handoff clears instantly.
            if (popoutAnim != null) {
                val anim = popoutAnim!!
                BackHandler { dismissPopout() }
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * popoutProgress.value)) // scrim dim: black overlay, not a scheme role
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
                    // neither flash nor take touches. The hero provider
                    // lives ONLY here, so the pop-out's title row
                    // reports the traveling header's target from final
                    // geometry — the surface content must never
                    // overwrite it mid-morph.
                    val density = LocalDensity.current
                    val maxHeight = with(density) { overlaySize.height.toDp() * 0.92f }
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.fillMaxWidth()
                                .heightIn(max = maxHeight)
                                .alpha(0f)
                                .onGloballyPositioned { anim.targetSize = it.size },
                        ) {
                            CompositionLocalProvider(
                                LocalHeroTarget provides { r -> heroTarget = anim.id to r },
                                LocalArcTarget provides { r -> arcTarget = anim.id to r },
                            ) {
                                ThermostatPopoutContent(
                                    vm, anim.id,
                                    onDetails = {
                                        dismissPopoutInstant()
                                        navigate(Screen.Thermostat(anim.id))
                                    },
                                    onClose = { dismissPopout() },
                                )
                            }
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
                        // The pop-out's radius morphs 28dp -> 32dp
                        // across the whole animation (see
                        // morphPopupCorner): the old fixed 32dp panel
                        // corners inside a square-cornered surface are
                        // exactly the "radius doesn't smoothly
                        // transition" read from the verdict. The panel
                        // keeps its own 32dp shape; the surface clip
                        // governs during the morph and lands on 32dp.
                        cornerAll = morphPopupCorner(popoutP),
                        contentAlpha = morphContentAlpha(popoutP),
                        background = Color.Transparent,
                        progress = popoutP,
                    ) {
                        // The pop-out panel's own background blends
                        // from the card color over the first 30% of
                        // enter (same morphBackgroundBlend rule as
                        // sheets/details) so the pop-out grows out of
                        // the card instead of flashing in white — and
                        // its alpha ramps in over [0, 0.35] (same
                        // morphBackgroundAlpha rule), so at progress 0
                        // the card shows through the transparent panel
                        // and no solid rect ever replaces it.
                        val popoutPanelBg = lerp(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            morphBackgroundBlend(popoutP),
                        ).copy(alpha = morphBackgroundAlpha(popoutP))
                        Box(
                            Modifier.fillMaxSize().background(
                                popoutPanelBg,
                                FloatingCorner,
                            ),
                        ) {
                            ThermostatPopoutContent(
                                vm, anim.id,
                                onDetails = {
                                    dismissPopoutInstant()
                                    navigate(Screen.Thermostat(anim.id))
                                },
                                onClose = { dismissPopout() },
                            )
                        }
                    }
                    // Traveling header, above the surface (see the
                    // sheet block): the climate card's badge/name/state
                    // travel to the pop-out's title row as ONE element.
                    TravelingHeader(
                        vm, anim.id, heroBounds[anim.id],
                        heroTarget?.takeIf { it.first == anim.id }?.second,
                        popoutP, overlayOrigin,
                        onDismiss = { dismissPopout() },
                    )
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
 * [onClose] runs the smooth animated exit (see dismissSheet);
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
    // Traveling header target (2026-10-29 pop-up round): read during
    // composition; the header row below reports its root rect into it
    // only inside the measurement frame (the provider is absent in the
    // morph surface, so the target can never be overwritten mid-morph).
    val heroTarget = LocalHeroTarget.current
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
                Row(
                    Modifier.fillMaxWidth()
                        .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DomainBadge(
                        entity,
                        domainIcon(entity),
                        accent,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entity.friendlyName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
 * The single traveling header (2026-10-29 pop-up round, Relay-style):
 * the card's badge and state travel position + size from the
 * card's header rect to the pop-up's header rect as ONE element —
 * the way Relay's contact photos expand and travel into the header —
 * never two copies. Mechanism, and why it finally answers the
 * doubling/flash/snap saga instead of retuning it:
 * - The card's own header is hidden from frame 0 (see
 *   hideHeroWhenMorphSource): at progress 0 the traveling header
 *   sits exactly on the card header's rect, so the handoff is
 *   invisible — no second badge, name, or state ever appears beside
 *   the traveling one. The card's BODY hides the same way now (see
 *   hideBodyWhenMorphSource): on #67 the arc, humidity line, and
 *   sensor charts stayed visible while the pop-up's duplicates
 *   faded in over them.
 * - The badge MORPHS SHAPE across the travel ([TravelingBadge]):
 *   tile icons render the domain's expressive polygon while header
 *   badges render state polygons, so a tile icon visibly transforms
 *   into the header shape as it travels (card sources already render
 *   the state polygon, so theirs is correctly the identity). A
 *   fixed polygon sliding into place unchanged was the 2026-10-29
 *   verdict: "relay style icons don't transition into shapes like
 *   they should".
 * - The NAME does not travel: a tile's badge-only source rect is
 *   far narrower than the name, so the traveling name ellipsized
 *   mid-morph ("Ecobee Therm...", "Eco..."). The name fades in at
 *   the destination instead, with the pop-up header's late content
 *   fade — full name, never clipped through the morph.
 * - The pop-up's own header is hidden by the late content fade
 *   (morphContentAlpha) until the morph nearly completes; the
 *   traveling header dissolves into it over the last 10% (see
 *   heroHeaderAlpha) once it has arrived at the target rect.
 * - Exit is the exact reverse: the header reappears at the target,
 *   travels back to the card, and the card header returns in the
 *   same frame the morph clears — never a snap, never staged
 *   (badge-then-text), never a blank panel.
 * - No sharedBounds crossfade (both endpoints visible = #49's
 *   ghosting), no blank growing panel (#60/#62's white flash), no
 *   instant snap return (#62's "sudden smack"). One element travels;
 *   the rest of the pop-up content fades in around it.
 * Rendered only when both rects are known: a sourceless card or a
 * pop-up that never reported its header simply gets the existing
 * fade mechanism, never a crash and never a jump. The badge itself
 * is the dismiss affordance (tap it = dismiss, same as Back/scrim/
 * Close); taps elsewhere on the header fall through to the surface
 * below. The badge click disables once fully dissolved (progress
 * 1), so the destination header's own badge owns taps at rest.
 * Name size lerps 16sp -> 20sp card-to-pop-up; the badge fills the
 * lerped row height, so it grows/shrinks with the travel.
 */
@Composable
private fun TravelingHeader(
    vm: HavenViewModel,
    entityId: String,
    source: Rect?,
    target: Rect?,
    progress: Float,
    overlayOrigin: Offset,
    onDismiss: () -> Unit,
) {
    val states by vm.states.collectAsState()
    val entity = states[entityId] ?: return
    if (source == null || target == null) return
    val p = progress.coerceIn(0f, 1f)
    val rect = lerpRect(source, target, p)
    val density = LocalDensity.current
    val accent = accentFor(entity.entityId)
    // Tile sources report their square badge rect; card sources
    // report their wide header row. A near-square source is a tile
    // badge (domain polygon); a wide source is a card header (state
    // polygon already) — see TravelingBadge for why this matters.
    val tileSource = source.width < source.height * 1.5f
    val headerAlpha = heroHeaderAlpha(p)
    Box(
        Modifier
            .offset {
                IntOffset(
                    (rect.left - overlayOrigin.x).roundToInt(),
                    (rect.top - overlayOrigin.y).roundToInt(),
                )
            }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            .clipToBounds()
            .graphicsLayer { alpha = headerAlpha },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TravelingBadge(
                entity,
                domainIcon(entity),
                accent,
                p,
                tileSource,
                modifier = Modifier.size(with(density) { rect.height.toDp() })
                    .then(
                        if (headerAlpha > 0f) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDismiss() } else Modifier,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            // No traveling name (see KDoc): it ellipsized mid-morph
            // from narrow source rects. The destination header's
            // name fades in whole with the late content fade.
            Text(
                entity.stateText(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The traveling weather arc (2026-10-29 verdict-fix round): the
 * Home weather card's dial panel travels into the pop-up's dial
 * panel as ONE element — the arc visibly moves, never doubles.
 * Mirrors TravelingHeader exactly: the card-side panel rect
 * (reported by WeatherControl via reportArcBounds) lerps to the
 * measurement frame's panel rect on the same progress, the panel
 * content (condition-tinted box + Dial) fills the lerped rect, and
 * the traveler dissolves into the destination panel over the last
 * 10% (heroHeaderAlpha) while the destination panel dissolves in
 * on the inverse curve (dissolveIntoTraveler) — so at every frame
 * exactly one arc is visible. Non-weather entities return early
 * (their arc bounds are never reported); a missing source or
 * target renders nothing (the destination's late fade still shows
 * the arc at rest). The panel keeps its 16dp padding at every
 * size, so the Dial diameter is the lerped panel width minus 32dp
 * — at both ends that is the 220dp default, and mid-travel the
 * dial scales with the panel instead of clipping. The humidity
 * line and timeline are destination-only layout and fade in late
 * with the rest of the pop-up content; the badge + arc panel are
 * the traveling elements.
 */
@Composable
private fun TravelingArc(
    vm: HavenViewModel,
    entityId: String,
    source: Rect?,
    target: Rect?,
    progress: Float,
    overlayOrigin: Offset,
) {
    val states by vm.states.collectAsState()
    val entity = states[entityId] ?: return
    if (entity.domain != "weather") return
    if (source == null || target == null) return
    val p = progress.coerceIn(0f, 1f)
    val rect = lerpRect(source, target, p)
    val density = LocalDensity.current
    val temp = entity.attrDouble("temperature") ?: 0.0
    val conditionColor = weatherConditionColor(entity.state)
    Box(
        Modifier
            .offset {
                IntOffset(
                    (rect.left - overlayOrigin.x).roundToInt(),
                    (rect.top - overlayOrigin.y).roundToInt(),
                )
            }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            .graphicsLayer { alpha = heroHeaderAlpha(p) }
            .background(conditionColor.copy(alpha = 0.14f), MaterialTheme.shapes.extraLarge)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Dial(
            value = temp.toFloat(),
            range = -10f..40f,
            onValueChange = {},
            centerText = temp.formatTemp(),
            centerSub = entity.state.replace('_', ' ').replaceFirstChar { it.uppercase() },
            accent = conditionColor,
            diameter = with(density) { (rect.width.toDp() - 32.dp).coerceAtLeast(0.dp) },
        )
    }
}

/**
 * Fullscreen camera viewer.
 *
 * Live-only by design (his 2026-10-29 13:34 direction: never fall
 * back to the last recorded clip). Every camera plays its MJPEG
 * proxy stream here — including Ring, as of the 2026-10-29
 * MJPEG-live test round. Ring previously negotiated HLS-live
 * first (WS camera/stream, 30s) then WebRTC (WS camera/webrtc/offer);
 * both attempts are REMOVED: HA's core Ring integration answers
 * camera/stream deterministically with "does not support play
 * stream service" and webrtc/offer deterministically with an empty
 * answer, so attempting them only burned 20-50s before the only
 * core transport that can explain the frontend's confirmed live
 * view on the default core Ring integration (no go2rtc, no extras):
 * the MJPEG proxy. The proxy is the live path UNDER TEST in this
 * build, not the last-recording fallback — the #38 conclusion that
 * the proxy serves a recording for Ring is what the wave test
 * overturns or confirms. Badge honesty holds the "Live" badge
 * back for Ring until that verdict: a stale clip also renders
 * frames. Recordings live on the long-press clips timeline,
 * never here.
 */

@Composable
private fun CameraFullscreen(vm: HavenViewModel, onClose: () -> Unit, enterProgress: Float) {
    val cameraId = vm.fullscreenCameraId ?: return
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    // Ring live test path (2026-10-29): straight to the MJPEG proxy.
    // The HLS-first (30s) + WebRTC-second attempts are REMOVED for
    // Ring: both are proven dead by deterministic HA errors (see the
    // KDoc above), not flakiness — attempting them first only burned
    // 20-50s before the only core transport that can explain the
    // frontend's confirmed live view. The proxy here is the live path
    // UNDER TEST, not the last-recording fallback: the wave test
    // decides whether its frames are live. No "Live" badge until
    // that verdict lands — a stale clip also renders frames, so
    // badge honesty holds the badge back in this build.
    val isRing = entity?.let { isRingCamera(it) } == true
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) { // video surface: black by design, not a scheme role
        CameraMjpegBody(
            vm, cameraId, enterProgress, recording = false, onClose = onClose,
            connectingText = if (isRing) "Loading live view…" else null,
            errorPrefix = if (isRing) "MJPEG proxy" else null,
        )
        // Badge held for Ring in this test build (see above); other
        // cameras never carried a badge here either.
        CameraHeader(entity?.friendlyName ?: "Camera", badgeText = null, onClose = onClose)
    }
}

/**
 * Shared MJPEG body: stream effect + Playing / Connecting / Error
 * UI, used by fullscreen (live, all cameras including Ring) and by
 * the recording fullscreen (a timeline entry). [recording] switches
 * the copy only;
 * a recording view must never say "live".
 */
@Composable
private fun CameraMjpegBody(
    vm: HavenViewModel,
    cameraId: String,
    enterProgress: Float,
    recording: Boolean,
    onClose: () -> Unit,
    // Test-build labels for the Ring MJPEG-live path: the connecting
    // text and the error-stage prefix. Null keeps the existing copy
    // (non-Ring fullscreen and the recording view are byte-identical).
    connectingText: String? = null,
    errorPrefix: String? = null,
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
                            MaterialTheme.shapes.extraSmall,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (connectingText != null) connectingText
                        else if (recording) "Loading recording…"
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
                Text(
                    if (errorPrefix != null) "$errorPrefix: ${state.message}" else state.message,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                )
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
    Box(Modifier.fillMaxSize().background(Color.Black)) { // video surface: black by design, not a scheme role
        CameraMjpegBody(vm, recordingEntityId, enterProgress, recording = true, onClose = onClose)
        CameraHeader(entity?.friendlyName ?: "Camera", badgeText = "Recording", onClose = onClose)
    }
}
/** Dismissible banner for the last service-call failure. */
@Composable
private fun ServiceErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
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
                shape = FloatingCorner,
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

/** Back arrow + title row used by every FULL screen (Areas,
 * builders, dashboards, editors, tabs, clips timeline). Detail
 * pop-ups do NOT use this: they use [DetailPopupHeader] (traveled
 * icon, no arrow — see below). Reports its root rect into the
 * measurement frame's hero target when one is provided: detail
 * pop-ups travel to their top bar, so the bar's rect is the travel
 * target. Outside a measurement frame the provider is null and this
 * is a plain row. Title + arrow colors are explicit (theme audit,
 * 2026-10-29 verdict-fix round): no text or icon on any header may
 * depend on an ambient default color. */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    val heroTarget = LocalHeroTarget.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
            .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
    Spacer(Modifier.height(4.dp))
}

/**
 * Title-only detail pop-up header, for the vanished-entity fallback
 * (no entity, so no badge to travel or tap): the title with no
 * arrow, dismiss via scrim/Back like every other pop-up. Exists so
 * even the fallback pop-up never shows a ←.
 */
@Composable
fun DetailPopupTitleHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(4.dp))
}

/**
 * Detail pop-up header (2026-10-29 verdict-fix round): NO back arrow.
 * The traveled icon lands in the top-left spot — the pop-up header's
 * leading element IS the badge, and tapping it dismisses (same as
 * Back/scrim/Close). Title + state sit beside it, mirroring the
 * sheet header (48dp badge + 20sp name + 13sp state) so the
 * traveling header dissolves into it exactly. Reports its root rect
 * into the measurement frame's hero target like [DetailTopBar] did,
 * so the travel target is unchanged. The thermostat pop-out keeps
 * its own title row + top-right X (not a back arrow) and is
 * untouched by this header.
 */
@Composable
fun DetailPopupHeader(entity: HaEntity, onDismiss: () -> Unit) {
    val heroTarget = LocalHeroTarget.current
    val accent = accentFor(entity.entityId)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
            .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DomainBadge(
            entity,
            domainIcon(entity),
            accent,
            modifier = Modifier.size(48.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entity.friendlyName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entity.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
