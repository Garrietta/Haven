package com.garrietta.haven
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.ShapePrefs
import com.garrietta.haven.ui.components.MorphSurface
import com.garrietta.haven.ui.components.badgeRectInRow
import com.garrietta.haven.ui.components.fallbackFromRect
import com.garrietta.haven.ui.components.hideWhileTraveling
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.components.formatCondition
import com.garrietta.haven.ui.theme.FloatingCorner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
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
import android.util.Log
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.CameraStreamState
import com.garrietta.haven.data.FRONT_DOOR_BATTERY_SENSOR_ID
import com.garrietta.haven.data.FRONT_DOOR_LIVE_STREAM_SWITCH_ID
import com.garrietta.haven.data.FRONT_DOOR_LIVE_VIEW_ID
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.RingWebRtcSession
import com.garrietta.haven.data.frontDoorShowsLiveBadge
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.data.displayCameraName
import org.webrtc.SurfaceViewRenderer
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.DialogTitleText
import com.garrietta.haven.ui.components.TravelingBadge
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.LocalWindowSizeClass
import com.garrietta.haven.ui.components.windowSizeClassFor
import com.garrietta.haven.ui.components.SheetPageContent
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.KodaMenuRow
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.pillLabelVisible
import com.garrietta.haven.ui.theme.sheetSpring
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
import com.garrietta.haven.ui.components.LocalMorphProgress
import com.garrietta.haven.ui.components.LocalHiddenCardId
import com.garrietta.haven.ui.components.LocalHiddenHeroId
import com.garrietta.haven.ui.components.LocalMorphCardAlpha
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.lerpRect
import com.garrietta.haven.ui.components.morphBackgroundAlpha
import com.garrietta.haven.ui.components.morphCardAlpha
import com.garrietta.haven.ui.components.morphExitCardAlpha
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.components.morphCloseContentAlpha
import com.garrietta.haven.ui.components.morphCornerTop
import com.garrietta.haven.ui.components.weatherConditionColor
import com.garrietta.haven.ui.components.ScrollVisibilityState
import com.garrietta.haven.ui.components.heroHeaderAlpha
import com.garrietta.haven.ui.theme.springBounds
import com.garrietta.haven.ui.theme.springSpatial
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.formatTemp
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
import com.garrietta.haven.ui.screens.RoomManagerScreen
import com.garrietta.haven.ui.screens.SettingsScreen
import com.garrietta.haven.ui.screens.ShapePickerDialog
import com.garrietta.haven.ui.screens.TabManagerScreen
import com.garrietta.haven.ui.screens.ThermostatPopout
import com.garrietta.haven.ui.screens.TodayScreen
import com.garrietta.haven.ui.screens.GreenhousePanelScreen
import com.garrietta.haven.ui.screens.MediaPanelScreen
import com.garrietta.haven.ui.screens.SecurityPanelScreen
import com.garrietta.haven.ui.screens.UpdatesPanelScreen
import com.garrietta.haven.ui.screens.VacuumPanelScreen
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

/**
 * Shared nav selection for the pill bar and the vertical rail:
 * Home is always index 0, user tabs follow in order, Settings is
 * always last. A legacy screen selects nothing. Both chrome
 * variants read this one function, so they can never disagree
 * about which tab is selected.
 */
private fun selectedNavIndex(
    current: Screen,
    barItems: List<NavItem>,
    userTabs: List<com.garrietta.haven.data.UserTab>,
): Int? = when (current) {
    Screen.Today -> 0
    is Screen.UserTab -> userTabs.indexOfFirst { it.id == current.tabId }.takeIf { it >= 0 }?.plus(1)
    Screen.Settings -> barItems.lastIndex
    else -> null
}

/** Entity id a detail screen belongs to (the shared card key it morphs from). Round 3 panels morph from their source card where one exists on Home; the updates panel has no Home card, so it uses the graceful fallback rect. */
private fun detailEntityId(screen: Screen): String = when (screen) {
    is Screen.EntityDetail -> screen.entityId
    is Screen.MediaPanel -> screen.entityId
    is Screen.VacuumPanel -> "vacuum.t2292"
    is Screen.SecurityPanel -> "alarm_control_panel.bloomington_mode"
    is Screen.GreenhousePanel -> "automation.greenhouse_brain"
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
private class DetailAnim(val screen: Screen, val entityId: String, val source: Rect?) {
    var targetSize by mutableStateOf<IntSize?>(null)
}

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
    // Read so shape-picker saves recompose the provider below from the store.
    @Suppress("UNUSED_VARIABLE")
    val shapePrefsVersion = vm.shapePrefsVersion
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
        // Panel state machine (2026-11-01 morphing-panels round,
        // superseding the 2026-10-30 centered-dialog round): the
        // sheet, detail, and pop-out overlays below are FULL-SCREEN
        // panels, not centered dialogs and not framework
        // sharedBounds. Each panel captures its source card's root
        // rect at open for the traveling header/arc (which
        // interpolate ONE rect to the panel header, and exactly
        // back on dismiss); the source card fades on morphCardAlpha
        // (see LocalMorphCardAlpha), so source and destination are
        // never both visible. The camera fullscreen keeps its own
        // rise, and vm.pageOrigin is still set by tile taps and
        // panel Details because the camera path needs it.
        val detailScreen = when (current) {
            is Screen.EntityDetail,
            is Screen.VacuumPanel, is Screen.MediaPanel,
            is Screen.SecurityPanel, is Screen.GreenhousePanel,
            is Screen.UpdatesPanel -> current
            else -> null
        }
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
        var popoutAnim by remember { mutableStateOf<PopoutAnim?>(null) }
        val popoutProgress = remember { Animatable(0f) }
        // The live panel's root rect (camera-rise origin for
        // sheet Details actions); re-reported on every panel place.
        var sheetPanelBounds by remember { mutableStateOf<Rect?>(null) }
        // Morph generation + exiting flags (restored from #52, for
        // the animated exit below): an exit animation is async
        // again, so a top-level navigation (goTop/signOut) or a
        // handoff must be able to cancel it. goTop/signOut bump the
        // generation and clear instantly; an in-flight exit
        // coroutine checks the generation + identity in its finally
        // block and only clears state that still belongs to the
        // morph that started. The exiting flags keep a second
        // dismiss (double back-press, back arrow + Close together) from
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
            vm.closeCameraDialog()
            vm.closeShapePicker()
            vm.clearAllTravelSnapshots()
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
        // Animated dismisses: Back, the back arrow, and Close all use
        // these. The progress animates 1 -> 0 on the bounds spring
        // (snapped under reduce-motion); only then is the trigger
        // cleared, and only if the generation + identity still
        // match the morph that started (see above).
        fun dismissDetail() {
            val anim = detailAnim ?: return
            if (detailExiting) return
            detailExiting = true
            vm.captureTravelSnapshot(anim.entityId)
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
            vm.captureTravelSnapshot(anim.id)
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
            vm.captureTravelSnapshot(anim.id)
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
            vm.closeCameraDialog()
            vm.closeShapePicker()
            vm.clearAllTravelSnapshots()
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
                vm.captureTravelSnapshot(id)
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
                vm.clearTravelSnapshot(anim.id)
            }
        }
        LaunchedEffect(detailScreen?.let { detailEntityId(it) }) {
            val anim = detailAnim
            if (detailScreen != null && anim != null && detailEntityId(anim.screen) == detailEntityId(detailScreen)) {
                vm.captureTravelSnapshot(detailEntityId(detailScreen))
                if (!reduceMotion) {
                    detailProgress.snapTo(0f)
                    detailProgress.animateTo(1f, springBounds())
                } else detailProgress.snapTo(1f)
            } else if (detailScreen == null && anim != null) {
                detailAnim = null
                heroTarget = null
                arcTarget = null
                vm.clearTravelSnapshot(anim.entityId)
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
                vm.captureTravelSnapshot(id)
                if (!reduceMotion) {
                    popoutProgress.snapTo(0f)
                    popoutProgress.animateTo(1f, springBounds())
                } else popoutProgress.snapTo(1f)
            } else if (id == null && anim != null) {
                popoutAnim = null
                heroTarget = null
                arcTarget = null
                vm.clearTravelSnapshot(anim.id)
            }
        }
        // Camera overlay transitions share the same language:
        // view rises as a bottom sheet out of the tapped camera tile and
        // sinks back on close.
        var cameraExiting by remember { mutableStateOf(false) }
        val cameraEnterProgress = remember { Animatable(0f) }
        val cameraExitProgress = remember { Animatable(1f) }
        // Centered live-feed dialog state (2026-10-30, Home
        // quadrant hero): mirrors the sheet dismiss pattern —
        // animated exit on the bounds spring, then the id clears.
        var cameraDialogExiting by remember { mutableStateOf(false) }
        val cameraDialogProgress = remember { Animatable(0f) }
        // The camera panel's source rect, captured at open (2026-11-01
        // round 1): the quadrant hero never reported card bounds
        // before, so the panel fell back to the full-width shifted
        // rect and its close landed near the top of the screen
        // instead of on the tapped quadrant. The quadrant now
        // reports (see CameraQuadrant) and this snapshot freezes
        // that rect at open, so a Home recompose or scroll while
        // the panel is up can never move the landing rect — the
        // close lands on the measured quadrant rect on both
        // postures, in root coordinates like every other morph.
        var cameraDialogSource by remember { mutableStateOf<Rect?>(null) }
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
        fun dismissCameraDialog() {
            val id = vm.cameraDialogId ?: return
            if (cameraDialogExiting) return
            // Front Door live probe: the switch turns off the moment
            // dismissal starts, not after the close animation lands —
            // the panel's dispose effect disengages again (no-op).
            vm.disengageFrontDoorLive("panel-close")
            cameraDialogExiting = true
            scope.launch {
                try {
                    if (reduceMotion) cameraDialogProgress.snapTo(0f)
                    else cameraDialogProgress.animateTo(0f, springBounds())
                } finally {
                    cameraDialogExiting = false
                    if (vm.cameraDialogId == id) vm.closeCameraDialog()
                }
            }
        }
        LaunchedEffect(vm.cameraDialogId) {
            val id = vm.cameraDialogId
            if (id != null && !cameraDialogExiting) {
                cameraDialogSource = cardBounds[id]
                if (reduceMotion) cameraDialogProgress.snapTo(1f)
                else {
                    cameraDialogProgress.snapTo(0f)
                    cameraDialogProgress.animateTo(1f, springBounds())
                }
            }
        }
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        // Wide-layout detection lives here, above the Scaffold, so every
        // screen (and the overlays) reads the same value.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 600.dp
            val windowSizeClass = windowSizeClassFor(maxWidth)
            CompositionLocalProvider(
                LocalWideLayout provides wideLayout,
                LocalWindowSizeClass provides windowSizeClass,
            ) {
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
            // GONE: the pill now exits the viewport during panels (see
            // the pill-exit below), which replaces the blur as the answer
            // to the panel problem, and the panel itself covers the page.
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
                // The camera panel's quadrant is a morph source too:
                // it hides while its panel is open (see
                // CameraQuadrant), exactly like sheet/detail/pop-out
                // sources, so the snapshot never doubles the video.
                LocalHiddenCardId provides (sheetAnim?.id ?: detailAnim?.entityId ?: popoutAnim?.id ?: vm.cameraDialogId),
                LocalHeroBounds provides heroBounds,
                LocalArcBounds provides arcBounds,
                LocalHiddenHeroId provides (sheetAnim?.id ?: detailAnim?.entityId ?: popoutAnim?.id),
                // Shape picker state (2026-10-30): badges and card
                // outlines resolve through this; re-read from the
                // store whenever a picker save bumps the version.
                LocalShapePrefs provides ShapePrefs(
                    vm.settingsStore.globalBadgeShape,
                    vm.settingsStore.globalCardShape,
                    vm.settingsStore.badgeShapeOverrides(),
                    vm.settingsStore.cardShapeOverrides(),
                ),
                // Progress-driven source-card alpha (container
                // transform): the active morph's progress drives the
                // card fade-out on enter, so the card and the surface
                // read as one element. On EXIT the source card stays
                // HIDDEN until the morph lands (2026-11-01 round 1):
                // the 2026-11-01 polish round's fully-opaque-under-
                // panel exit composited the card over the leaving
                // panel's remnants, which read as the thermostat
                // dial bleeding through over already-visible grid
                // tiles — and, with the close content fade in
                // PanelOverlay, an early card return would double
                // against the still-shrinking surface. The card
                // reappears only once progress reaches ~0, the same
                // frame the panel clears, so the landing is exact.
                // No morph -> fully visible.
                LocalMorphCardAlpha provides if (sheetExiting || detailExiting || popoutExiting) {
                    val exitP = when {
                        sheetAnim != null -> sheetProgress.value
                        detailAnim != null -> detailProgress.value
                        popoutAnim != null -> popoutProgress.value
                        else -> 1f
                    }.coerceIn(0f, 1f)
                    // Round 4 exit flash fix (proven curve mismatch:
                    // the old binary hide left the slot blank while
                    // panel content was already gone, then popped
                    // the card in one frame): the card stays hidden
                    // exactly while panel content is visible
                    // (progress > 0.85) and fades in smoothly over
                    // [0.85 -> 0] — no frame has both at full
                    // opacity (no doubling) and no frame has
                    // neither (no blank slot, no pop).
                    morphExitCardAlpha(exitP)
                } else morphCardAlpha(
                    when {
                        sheetAnim != null -> sheetProgress.value
                        detailAnim != null -> detailProgress.value
                        popoutAnim != null -> popoutProgress.value
                        else -> 1f
                    },
                ),
            ) {
            // Overlay root: its root-coordinate origin anchors every
            // traveler rect. All panel overlays below render inside
            // this Box, above the scaffold (and above the rail).
            var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
            Box(
                Modifier.fillMaxSize().onGloballyPositioned {
                    overlayOrigin = it.boundsInRoot().topLeft
                },
            ) {
            // Unfolded chrome (Round 4): the floating bottom pill
            // becomes a floating vertical SIDE PILL on the left
            // edge (Koda-style, margined — NOT #86's full-height
            // rail), carrying the same tabs, selection, and goTop
            // behavior as the pill (see VerticalSidePill). Folded
            // keeps the pill unchanged. The side pill is nav
            // chrome, not scroll-away chrome: it never slides away
            // on scroll, and an open panel (a later sibling of
            // this Row) covers it exactly as today's pill-exit rule
            // covers the pill.
            Row(Modifier.fillMaxSize()) {
            if (LocalWideLayout.current && isTopLevel(current)) {
                val barItems = pillBarItems(vm.userTabs)
                VerticalSidePill(
                    items = barItems,
                    selectedIndex = selectedNavIndex(current, barItems, vm.userTabs),
                    onSelect = goTop,
                )
            }
            Scaffold(
                modifier = if (LocalWideLayout.current) Modifier.weight(1f) else Modifier,
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
                // Folded only: unfolded shows the rail instead, so
                // no bottom bar is reserved or rendered there.
                if (!LocalWideLayout.current && isTopLevel(current)) {
                    val barItems = pillBarItems(vm.userTabs)
                    val selectedIndex = selectedNavIndex(current, barItems, vm.userTabs)
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
                            // up. The panel (fillMaxSize, above the
                            // scaffold) already intercepts touches, so
                            // the exiting pill is never tappable
                            // mid-panel.
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
                    is Screen.EntityDetail,
                    is Screen.VacuumPanel, is Screen.MediaPanel,
                    is Screen.SecurityPanel, is Screen.GreenhousePanel,
                    is Screen.UpdatesPanel ->
                        backstack.getOrNull(backstack.lastIndex - 1) ?: Screen.Today
                    else -> current
                }
                // Round 4: top-level tab switches use a shared-axis
                // transition (horizontal slide + fade on the house
                // spring) instead of an instant swap; panel morphs
                // are untouched (they already use springBounds).
                // Under reduce-motion the switch stays instant.
                val reduceMotionTop = LocalReduceMotion.current
                if (reduceMotionTop) {
                    content(base)
                } else {
                    AnimatedContent(
                        targetState = base,
                        transitionSpec = {
                            (slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut())
                        }.using(SizeTransform(clip = false)),
                        label = "topLevelScreen",
                    ) { screen -> content(screen) }
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
            } // Row: side pill + scaffold (unfolded) / scaffold alone (folded)
            // Sheet panel overlay: FULL-SCREEN PANEL (2026-11-01
            // panels round) — SUPERSEDES the 2026-10-30 centered
            // dialog. The sheet's content is unchanged; only its
            // container morphs from the source card rect to
            // full-screen (see PanelOverlay). No scrim: the opaque
            // panel covers the page at rest. The badge still
            // travels into the panel's header (see
            // TravelingHeader) while the panel morphs — both, not
            // one or the other. No drag-to-dismiss: Back and the
            // panel's back arrow dismiss (see dismissSheet).
            if (sheetAnim != null) {
                val anim = sheetAnim!!
                BackHandler { dismissSheet() }
                val sheetP = sheetProgress.value.coerceIn(0f, 1f)
                // Measurement frame (panel geometry, below the
                // panel): the panel content, invisible, laid out once
                // at its final full-screen size so its header can
                // report the traveling header's target rect. The hero
                // provider lives ONLY here: the live panel's header
                // sits at the interpolated morph rect mid-animation
                // and must never overwrite the target.
                if (anim.targetSize == null) {
                    PanelMeasureFrame(onMeasured = { anim.targetSize = it }) {
                        CompositionLocalProvider(
                            LocalHeroTarget provides { r -> heroTarget = anim.id to r },
                            LocalArcTarget provides { r -> arcTarget = anim.id to r },
                        ) {
                            EntityPopupSheetContent(
                                vm, navigate, anim.id, panelBounds = sheetPanelBounds,
                                onDetails = { dismissSheetInstant() },
                            )
                        }
                    }
                }
                PanelOverlay(
                    progress = sheetP,
                    source = cardBounds[anim.id] ?: anim.source,
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    onDismiss = { dismissSheet() },
                    isClosing = sheetExiting,
                    onPanelPlaced = { sheetPanelBounds = it },
                ) {
                    EntityPopupSheetContent(
                        vm, navigate, anim.id, panelBounds = sheetPanelBounds,
                        onDetails = { dismissSheetInstant() },
                    )
                }
                // Traveling header, above the panel: the card's
                // badge/state travel to the panel's header as ONE
                // element. Renders at the source rect until the
                // measurement frame reports the target (see
                // TravelingHeader) — never a gap, never a pop.
                TravelingHeader(
                    vm, anim.id, heroBounds[anim.id],
                    heroTarget?.takeIf { it.first == anim.id }?.second,
                    sheetP, overlayOrigin,
                    onDismiss = { dismissSheet() },
                )
                // Traveling arc, above the panel (see TravelingArc).
                TravelingArc(
                    vm, anim.id, arcBounds[anim.id],
                    arcTarget?.takeIf { it.first == anim.id }?.second,
                    sheetP, overlayOrigin,
                )
            }
            // Detail panel overlay: FULL-SCREEN PANEL (2026-11-01
            // panels round) — SUPERSEDES the 2026-10-30 centered
            // dialog, exactly like the sheets above. The detail
            // content is unchanged; the one-frame measurement pass
            // reports only the header/arc target rects, never a
            // panel size — the panel is full-screen. Back and the
            // panel's back arrow are the smooth reverse of enter
            // (see dismissDetail); only the sheet -> detail
            // handoff clears instantly.
            if (detailAnim != null) {
                val anim = detailAnim!!
                BackHandler { dismissDetail() }
                val detailP = detailProgress.value.coerceIn(0f, 1f)
                // Round 4: the thermostat detail special-case is
                // gone with Screen.Thermostat — every detail panel
                // content is dialog-owned (plain column, no own
                // scroll), so the panel's scroll wrapper owns
                // scrolling for all of them. The #92 inDialog crash
                // fix for the deleted route is moot.
                if (anim.targetSize == null) {
                    PanelMeasureFrame(onMeasured = { anim.targetSize = it }) {
                        CompositionLocalProvider(
                            LocalHeroTarget provides { r -> heroTarget = anim.entityId to r },
                            LocalArcTarget provides { r -> arcTarget = anim.entityId to r },
                        ) {
                            content(anim.screen)
                        }
                    }
                }
                PanelOverlay(
                    progress = detailP,
                    source = cardBounds[anim.entityId] ?: anim.source,
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    onDismiss = { dismissDetail() },
                    isClosing = detailExiting,
                ) {
                    content(anim.screen)
                }
                TravelingHeader(
                    vm, anim.entityId, heroBounds[anim.entityId],
                    heroTarget?.takeIf { it.first == anim.entityId }?.second,
                    detailP, overlayOrigin,
                    onDismiss = { dismissDetail() },
                )
                TravelingArc(
                    vm, anim.entityId, arcBounds[anim.entityId],
                    arcTarget?.takeIf { it.first == anim.entityId }?.second,
                    detailP, overlayOrigin,
                )
            }
            // Thermostat pop-out panel (Round 4: the ONLY
            // thermostat panel — tap and long-press on the Ecobee
            // card both land here; the old pop-out -> Details page
            // handoff is deleted with Screen.Thermostat): a panel
            // morphing from the climate card rect to full-screen,
            // so the dial gets full touch targets on narrow cover
            // screens.
            if (popoutAnim != null) {
                val anim = popoutAnim!!
                BackHandler { dismissPopout() }
                val popoutP = popoutProgress.value.coerceIn(0f, 1f)
                if (anim.targetSize == null) {
                    PanelMeasureFrame(onMeasured = { anim.targetSize = it }) {
                        CompositionLocalProvider(
                            LocalHeroTarget provides { r -> heroTarget = anim.id to r },
                            LocalArcTarget provides { r -> arcTarget = anim.id to r },
                        ) {
                            ThermostatPopoutContent(
                                vm, anim.id,
                                onClose = { dismissPopout() },
                            )
                        }
                    }
                }
                PanelOverlay(
                    progress = popoutP,
                    source = cardBounds[anim.id] ?: anim.source,
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    onDismiss = { dismissPopout() },
                    isClosing = popoutExiting,
                ) {
                    ThermostatPopoutContent(
                        vm, anim.id,
                        onClose = { dismissPopout() },
                    )
                }
                // Traveling header, above the panel (see the sheet
                // block): the climate card's badge/state travel to
                // the panel's title row as ONE element.
                TravelingHeader(
                    vm, anim.id, heroBounds[anim.id],
                    heroTarget?.takeIf { it.first == anim.id }?.second,
                    popoutP, overlayOrigin,
                    onDismiss = { dismissPopout() },
                )
            }
            // Camera live-feed panel (2026-11-01 panels round): the
            // Home quadrant hero's tap target, formerly a centered
            // 16:9 dialog — now a full-screen panel like every other
            // device surface, so no centered device dialog remains.
            // The MJPEG body fills the panel; Ring uses the #65
            // proxy path through the same body and stays labeled
            // as a recording, never live. No back bar: the panel's
            // own header row carries the Close button, and Back
            // dismisses (see dismissCameraDialog). This block moved
            // inside the overlay root so the morph shares its
            // origin with the other panels; the camera fullscreen
            // below still draws above it.
            if (vm.cameraDialogId != null) {
                val camId = vm.cameraDialogId!!
                BackHandler { dismissCameraDialog() }
                val camP = cameraDialogProgress.value.coerceIn(0f, 1f)
                val camStates by vm.states.collectAsState()
                val camEntity = camStates[camId]
                PanelOverlay(
                    progress = camP,
                    // Captured-at-open quadrant rect (live map as
                    // first-frame fallback): never the sourceless
                    // fallback, so the close lands on the quadrant.
                    source = cameraDialogSource ?: cardBounds[camId],
                    overlayOrigin = overlayOrigin,
                    panelBackground = Color.Black,
                    onDismiss = { dismissCameraDialog() },
                    showBackBar = false,
                    scrollContent = false,
                    isClosing = cameraDialogExiting,
                ) {
                    // Front Door live probe (Round 2): the Front
                    // Door panel alone gets the Live View / Event
                    // History toggle; every other camera keeps the
                    // exact body below, byte for byte.
                    if (camId == FRONT_DOOR_LIVE_VIEW_ID) {
                        FrontDoorCameraPanel(vm, camId, camP, onClose = { dismissCameraDialog() })
                    } else {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        // The hero panel's MJPEG body is a recording
                        // for Ring (same verdict as fullscreen) and
                        // live for native cameras; `recording` only
                        // switches the connecting/error copy.
                        CameraMjpegBody(vm, camId, camP, recording = camEntity?.let { isRingCamera(it) } == true, onClose = { dismissCameraDialog() })
                        Row(
                            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Single top-left back button (2026-11-01
                            // round 1 panel chrome): the top-right X
                            // is gone — BackHandler + this arrow are
                            // the dismiss paths, like every panel.
                            IconButton(onClick = { dismissCameraDialog() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    camEntity?.let { displayCameraName(it) } ?: "Camera",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                // Ring honesty (2026-10-30 verdict):
                                // the proxy serves the most recent
                                // motion recording for Ring, never
                                // live — the panel says so under
                                // the name, like the fullscreen.
                                if (camEntity?.let { isRingCamera(it) } == true) {
                                    Text(
                                        "Latest motion recording · Not live",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    } // else: non-Front-Door cameras keep the exact body above
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
            // Shape picker dialog (2026-10-30): above every other
            // overlay; opened from Settings (global default) or an
            // entity sheet (per-entity override).
            if (vm.shapePickerTarget != null) {
                ShapePickerDialog(vm, vm.shapePickerTarget!!, onClose = { vm.closeShapePicker() })
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
        Screen.Today -> TodayScreen(vm, navigate, onSignInExpired = onSignOut)
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
        Screen.RoomManager -> RoomManagerScreen(vm, goBack)
        // Detail pages replaced the old generic detail screen: per-domain
        // pages (climate still routes to its thermostat page inside).
        is Screen.EntityDetail -> DetailPageScreen(vm, screen.entityId, goBack)
        // Round 3 panels: full-screen morph panels, same machinery.
        is Screen.VacuumPanel -> VacuumPanelScreen(vm, goBack)
        is Screen.MediaPanel -> MediaPanelScreen(vm, screen.entityId, goBack)
        is Screen.SecurityPanel -> SecurityPanelScreen(vm, goBack)
        is Screen.GreenhousePanel -> GreenhousePanelScreen(vm, goBack)
        is Screen.UpdatesPanel -> UpdatesPanelScreen(vm, goBack)
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
        ThermostatPopout(entity, vm, onClose)
    }
}

/**
 * The sheet panel's CONTENT (not the panel itself): the panel
 * overlay in HavenApp owns positioning and sizing; this is
 * the column laid out inside it at the panel's intrinsic size. Kept
 * separate so the same content serves the one-frame measurement pass
 * and the live panel. [panelBounds] is the panel's window
 * rect for the camera rise origin on Details. [onDetails] dismisses
 * instantly for the sheet -> detail handoff (the detail panel
 * starts from the same card, so the card never flashes back in
 * between); the panel's back arrow + BackHandler are the dismiss
 * paths (2026-11-01 round 1 panel chrome — no Close button here).
 */
@Composable
private fun EntityPopupSheetContent(
    vm: HavenViewModel,
    navigate: (Screen) -> Unit,
    popupId: String,
    panelBounds: androidx.compose.ui.geometry.Rect?,
    onDetails: () -> Unit,
) {
    val states by vm.states.collectAsState()
    val entity = states[popupId]
    // Traveling header target (2026-10-29 pop-up round): read during
    // composition; the header row below reports its root rect into it
    // only inside the measurement frame (the provider is absent in the
    // live panel, so the target can never be overwritten mid-panel).
    val heroTarget = LocalHeroTarget.current
    // The panel scrolls: plain column, no own scroll, no drag
    // handle (2026-10-30 round — panels do not drag, and the sheet
    // handle was a sheet affordance).
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp).padding(bottom = 24.dp),
    ) {
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
                    // Badge only hides while traveling (2026-11-01
                    // polish round): the title text stays visible
                    // from the panel's first frame.
                    DomainBadge(
                        entity,
                        domainIcon(entity),
                        accent,
                        modifier = Modifier.size(48.dp).hideWhileTraveling(),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        DialogTitleText(
                if (entity.domain == "sensor") sensorDisplayName(entity) else displayCameraName(entity),
                Modifier.fillMaxWidth(),
            )
                        Text(entity.dialogHeaderState(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            panelBounds?.let { vm.pageOrigin = it }
                            onDetails()
                            when (entity.domain) {
                                // Round 4: the secondary thermostat
                                // page is deleted — a sheet climate
                                // Details tap opens the primary dial
                                // panel instead (safety net; the
                                // sheet's onDetails dismiss above
                                // already ran before this when).
                                "climate" -> vm.openClimatePopout(popupId)
                                // Cameras open their fullscreen viewer,
                                // not a static page (Ring: the latest
                                // motion recording, labeled as such).
                                "camera" -> vm.openCameraFullscreen(popupId)
                                else -> navigate(Screen.EntityDetail(popupId))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Shape picker (2026-10-30): per-entity overrides,
                // reachable from every entity's sheet (long-press
                // any card or tile). The badge and card-outline
                // shapes are picked independently; the subtitle
                // shows the effective choice (override, else the
                // global default, else the domain default).
                Spacer(Modifier.height(8.dp))
                KodaMenuRow(
                    badgeKey = "shape-badge-${entity.entityId}",
                    badgeKind = shapeForDomain(entity.domain),
                    icon = domainIcon(entity),
                    title = "Badge shape",
                    subtitle = shapeLabel(vm.settingsStore.badgeShapeOverrides()[entity.entityId] ?: vm.settingsStore.globalBadgeShape) ?: "Domain default",
                    onClick = { vm.openShapePicker(entity.entityId, false) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                KodaMenuRow(
                    badgeKey = "shape-card-${entity.entityId}",
                    badgeKind = shapeForDomain(entity.domain),
                    icon = domainIcon(entity),
                    title = "Card shape",
                    subtitle = shapeLabel(vm.settingsStore.cardShapeOverrides()[entity.entityId] ?: vm.settingsStore.globalCardShape) ?: "Default corners",
                    onClick = { vm.openShapePicker(entity.entityId, true) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * The full-screen panel overlay (2026-11-01 panels round —
 * SUPERSEDES the 2026-10-30 centered dialog): every device detail
 * opens as a panel morphing from its source card rect to
 * full-screen, on BOTH folded and unfolded postures. No centered
 * device dialog remains; the only dialog left is the shape picker
 * (see ShapePickerDialog, intentionally preserved).
 * - Container: NO scrim — the panel is opaque at rest (scheme
 *   background, or black for cameras) and covers the page, so no
 *   dimmed page shows around it. A touch interceptor fills the
 *   screen behind the morphing rect so taps outside the still-
 *   small panel mid-flight never reach the page below; it takes
 *   no action itself (dismiss is Back / the back arrow / the
 *   panel's own Close or badge, never an outside tap).
 * - Morph: the panel rect lerps from the source card rect (or the
 *   graceful fallback rect, target shifted 80dp down, when the
 *   card never reported bounds) to the full overlay rect on the
 *   caller-supplied morph [progress] — the SAME Animatable +
 *   springBounds progress that drove the old dialog, so an
 *   appear interrupted by a dismiss reverses cleanly mid-flight
 *   instead of jumping (an Animatable's animateTo cancels and
 *   restarts from the current value). The rect, corners
 *   (28dp -> 0dp via [morphCornerTop]), and background ramp
 *   ([morphBackgroundAlpha]: transparent at the card, opaque once
 *   grown) all come from MorphOverlay helpers via [MorphSurface];
 *   no parallel morph system. Content is laid out once at full
 *   size and clipped to the growing rect (never scaled), so it is
 *   present from the panel's first frame and never materializes
 *   late; destination headers hide only their badge while
 *   traveling (see TravelingHeader), never their text.
 * - Chrome: status-bar padding + a back arrow row above the
 *   content (suppressed only for the camera panel, whose own
 *   header row carries Close). No drag-to-dismiss: Back and the
 *   back arrow are the dismiss paths, plus each panel's own
 *   Close/badge affordance.
 * - Scroll: the panel body scrolls (the old dialog's scroll
 *   wrapper, same structure), so dialog-threaded pages that own
 *   no scroll of their own (sheet, detail, pop-out) scroll here.
 *   [scrollContent] = false skips the scroll wrapper for content
 *   that owns its own full-size layout (the camera panel's video
 *   body), where a fillMaxSize child inside a scroll container
 *   would have no definite height to fill.
 * [onPanelPlaced] reports the panel's full-screen root rect once
 * it reaches rest (the camera-rise origin for sheet Details
 * actions); at rest the rect is exact where it is used.
 */
@Composable
private fun PanelOverlay(
    progress: Float,
    source: Rect?,
    overlayOrigin: Offset,
    panelBackground: Color,
    onDismiss: () -> Unit,
    showBackBar: Boolean = true,
    scrollContent: Boolean = true,
    // True while this panel is animating closed: the surface
    // container still lerps back to the card rect, but the inner
    // content fades out in the first stretch of the close (see
    // morphCloseContentAlpha) instead of shrinking at full opacity
    // over the grid — the thermostat dial bleed-through fix.
    isClosing: Boolean = false,
    onPanelPlaced: ((Rect) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val p = progress.coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize()) {
        // Touch interceptor (see the KDoc): consumes taps outside
        // the still-small morph rect mid-flight, takes no action.
        Box(
            Modifier.fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { },
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val targetWidthPx = with(density) { maxWidth.toPx() }
            val targetHeightPx = with(density) { maxHeight.toPx() }
            val target = Rect(
                overlayOrigin.x,
                overlayOrigin.y,
                overlayOrigin.x + targetWidthPx,
                overlayOrigin.y + targetHeightPx,
            )
            val from = source ?: fallbackFromRect(target, with(density) { 80.dp.toPx() })
            if (p >= 1f && onPanelPlaced != null) {
                LaunchedEffect(Unit) { onPanelPlaced(target) }
            }
            MorphSurface(
                rect = lerpRect(from, target, p),
                overlayOrigin = overlayOrigin,
                targetSize = IntSize(targetWidthPx.roundToInt(), targetHeightPx.roundToInt()),
                cornerTop = morphCornerTop(p),
                cornerAll = morphCornerTop(p),
                background = panelBackground,
                backgroundAlpha = morphBackgroundAlpha(p),
                // Enter: content present from the first frame (the
                // panels round's rule). Close: content fades out
                // early via morphCloseContentAlpha so the shrinking
                // surface never carries full-opacity panel content
                // over the grid tiles.
                contentAlpha = if (isClosing) morphCloseContentAlpha(p) else 1f,
                progress = p,
            ) {
                if (scrollContent) {
                    Box(
                        Modifier.fillMaxSize().statusBarsPadding()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            if (showBackBar) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            content()
                        }
                    }
                } else {
                    // Full-size content (camera video): no scroll
                    // wrapper, so fillMaxSize resolves against the
                    // panel's definite size, not an unbounded scroll
                    // height. No back bar here either (callers that
                    // skip scrolling carry their own header).
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * The panel's one-frame measurement pass: the same content, laid
 * out once at final full-screen geometry (status-bar padding
 * included, matching PanelOverlay), invisible and below the panel,
 * so its header/arc rows can report the traveling elements'
 * target rects. The live panel must never provide the hero/arc
 * targets: its header sits at the interpolated morph rect
 * mid-animation. [onMeasured] fires once (the caller stores it as
 * the measured flag and drops this frame); the content may compose
 * twice for one frame, which is safe — history refreshes dedupe
 * in the view model.
 */
@Composable
private fun PanelMeasureFrame(
    onMeasured: (IntSize) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .alpha(0f)
            .onGloballyPositioned { onMeasured(it.size) },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Round 4 header-badge alignment fix: the live panel
            // sits below a 48dp back-bar row (IconButton) +
            // status-bar padding, but this measurement frame had
            // status-bar padding only — so every measured header
            // target rect sat ~48dp too high, roughly where the
            // back arrow sits, and the final-2% handoff read as
            // "spawn over the back arrow and drop into place".
            // The spacer aligns measured targets with live ones.
            Spacer(Modifier.height(48.dp))
            content()
        }
    }
}

/**
 * The single traveling header (2026-10-29 pop-up round, Relay-style;
 * 2026-10-30 verdict round: TRUE element travel, not recreation;
 * 2026-11-01 polish round: BADGE ONLY). The badge lerps along its
 * own rect (derived from the source and target header rows with
 * [badgeRectInRow]: tiles report their badge rect directly, cards
 * and dialog headers report their header row, and the badge is
 * always the row's leading square, so a 64dp tile badge, a 56dp
 * card badge, and the 48dp dialog badge land on exactly the same
 * rect at rest — no size pop at either end, which is what the old
 * row-rect travel got wrong). The name and state NO LONGER travel:
 * they live in the dialog header from the panel's first frame
 * (destination headers hide only their badge while traveling,
 * never their text), because the traveling text was the "title
 * draws late" complaint — the user looks at the panel, not at the
 * badge's flight path, and the panel's own title only arrived in
 * the final 2% handoff window. With text omnipresent in the panel
 * and only the badge traveling, there is never a doubled header
 * and never a late title.
 *
 * The destination header's badge wears [hideWhileTraveling], so
 * during the flight the traveler is the ONLY badge on screen;
 * the destination badge materializes only in the final 2% handoff
 * window ([heroHeaderAlpha]), where traveler and destination
 * coincide — never two copies, never a color shift between a
 * mid-morph traveler and a fading-in destination. The traveler reads the
 * open/dismiss-time snapshot ([HavenViewModel.travelSnapshots]),
 * never the live states flow, so no state emission mid-flight
 * — its own or any other entity's — can recompute its color,
 * shape, or text, or cost a frame at 120Hz; the dialog body
 * stays live. A sourceless card renders nothing (the
 * destination's late fade still shows the header at rest); a
 * missing target renders at the source rect until the
 * measurement frame reports, never a gap and never a jump.
 * The badge itself is the dismiss affordance (tap it =
 * dismiss); its click disables once fully handed off (progress
 * 1), so the destination header's own badge owns taps at rest.
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
    // Snapshot-first (see the ViewModel KDoc): the live flow is
    // only the first-frame fallback before the open effect
    // captures, and it is read as a plain value, never collected
    // — so unrelated state emissions never recompose the flight.
    val entity = vm.travelSnapshots[entityId] ?: vm.states.value[entityId] ?: return
    if (source == null) return
    // Round 4 origin guard: a never-reported source rect lerps
    // from (0, 0) — the window origin, over the back arrow —
    // so a zero-size source renders nothing instead of a badge
    // that appears to spawn at the origin and drop into place.
    if (source.width <= 0f || source.height <= 0f) return
    // No target yet (measurement frame pending): sit exactly on
    // the source badge rect instead of vanishing — the card
    // header hides from frame 0, so returning early here left a
    // gap where no header showed at all, then the traveler popped
    // in mid-path (the 2026-10-30 verdict's icon jump). Once the
    // target arrives the same lerp carries it continuously to
    // the dialog header.
    val targetRect = target ?: source
    val p = progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    // Tile sources report their square badge rect; card sources
    // report their wide header row. A near-square source is a
    // tile badge (domain polygon); a wide source is a card header
    // (state polygon already) — see TravelingBadge for why this
    // matters. The badge path is badge-rect to badge-rect, never
    // row-rect to row-rect: row heights differ (64/56/48dp), so
    // row travel always popped the badge size at the handoff.
    val tileSource = source.width < source.height * 1.5f
    val sourceBadge = if (tileSource) source
        else badgeRectInRow(source, with(density) { 56.dp.toPx() })
    val targetBadge = badgeRectInRow(targetRect, with(density) { 48.dp.toPx() })
    val rect = lerpRect(sourceBadge, targetBadge, p)
    val accent = accentFor(entity.entityId)
    val headerAlpha = heroHeaderAlpha(p)
    // Badge only (2026-11-01 polish round): the name/state text
    // used to ride beside the badge here, but the destination
    // header now shows its own text from frame 0 — keeping the
    // traveler's text would double it through the flight.
    Box(
        Modifier
            .offset {
                IntOffset(
                    (rect.left - overlayOrigin.x).roundToInt(),
                    (rect.top - overlayOrigin.y).roundToInt(),
                )
            }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            .graphicsLayer { alpha = headerAlpha },
        contentAlignment = Alignment.Center,
    ) {
        TravelingBadge(
            entity,
            domainIcon(entity),
            accent,
            p,
            tileSource,
            modifier = Modifier.size(with(density) { rect.width.toDp() })
                .then(
                    if (headerAlpha > 0f) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() } else Modifier,
                ),
        )
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
 * the traveler hands off to the destination panel in the final
 * 2% window (heroHeaderAlpha) while the destination panel dissolves in
 * on the inverse curve (dissolveIntoTraveler) — so at every frame
 * exactly one arc is visible. Non-weather entities return early
 * (their arc bounds are never reported); a missing source renders
 * nothing (the destination's late fade still shows the arc at
 * rest); a missing target renders at the source rect until the
 * measurement frame reports, same as the header. The panel keeps
 * its 16dp padding at every
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
    val entity = vm.travelSnapshots[entityId] ?: vm.states.value[entityId] ?: return
    if (entity.domain != "weather") return
    if (source == null) return
    // Same no-target rule as TravelingHeader: sit on the source
    // rect until the measurement frame reports, never a gap.
    val targetRect = target ?: source
    val p = progress.coerceIn(0f, 1f)
    val rect = lerpRect(source, targetRect, p)
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
            centerSub = formatCondition(entity.state),
            accent = conditionColor,
            diameter = with(density) { (rect.width.toDp() - 32.dp).coerceAtLeast(0.dp) },
        )
    }
}

/**
 * Fullscreen camera viewer.
 *
 * Live for native cameras; a labeled RECORDING for Ring
 * (2026-10-30 verdict confirms #38: the MJPEG proxy serves the
 * most recent motion recording for Ring, and his "bouncing back
 * and forth" is a fresh clip looking live-ish vs an older clip
 * reading as a recording). Ring previously negotiated HLS-live
 * first (WS camera/stream, 30s) then WebRTC (WS camera/webrtc/offer);
 * both attempts are REMOVED: HA's core Ring integration answers
 * camera/stream deterministically with "does not support play
 * stream service" and webrtc/offer deterministically with an empty
 * answer. The proxy stays as the Ring path — but labeled as a
 * recording everywhere (connecting text, persistent caption,
 * error copy), never presented as live, no "Live" badge. The
 * wave test is moot for this path: it cannot pass by
 * construction. Genuine live for Ring would need a new path
 * (ring-mqtt RTSP via a Generic Camera entity, or the frontend's
 * actual transport once diagnosed). Timeline recordings live on
 * the long-press clips timeline, as before.
 */

@Composable
private fun CameraFullscreen(vm: HavenViewModel, onClose: () -> Unit, enterProgress: Float) {
    val cameraId = vm.fullscreenCameraId ?: return
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    // Ring recording path (2026-10-30 verdict CONFIRMS #38): the
    // MJPEG proxy serves the most recent motion recording for
    // Ring — a fresh clip looks live-ish (a person moving on the
    // porch), an older clip is obviously a recording, which is
    // exactly the "bouncing back and forth" he reports. The HLS
    // and WebRTC attempts stay REMOVED (deterministic HA errors,
    // not flakiness). The proxy is therefore LABELED as a
    // recording everywhere it appears for Ring — never presented
    // as live, no "Live" badge — and the wave test is now moot
    // for this path: it cannot pass by construction. A cued wave
    // would only gate a FUTURE genuine live path (ring-mqtt RTSP
    // via a Generic Camera entity, or the frontend's actual
    // transport once diagnosed), not this one.
    val isRing = entity?.let { isRingCamera(it) } == true
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) { // video surface: black by design, not a scheme role
        CameraMjpegBody(
            vm, cameraId, enterProgress, recording = isRing, onClose = onClose,
            connectingText = if (isRing) "Loading recording…" else null,
            errorPrefix = if (isRing) "MJPEG proxy" else null,
        )
        // Ring carries its recording caption persistently (see
        // above); other cameras never carried a badge here either.
        CameraHeader(
            entity?.let { displayCameraName(it) } ?: "Camera",
            badgeText = if (isRing) "Latest motion recording · Not live" else null,
            onClose = onClose,
        )
    }
}

/**
 * Front Door panel (Round 2 live probe): the hero panel for
 * camera.front_door_live_view ONLY — every other camera keeps the
 * plain MJPEG body. A segmented Live View / Event History toggle
 * sits at the top, under the name.
 *
 * Live View is an attempt chain, never a claim:
 *  1. The tab engages the ring-mqtt live-stream switch
 *     (ViewModel-owned, auto-off on panel close, tab flip, app
 *     background, and a 10-minute watchdog cap).
 *  2. WebRTC via the existing RingWebRtcSession + the app's WS
 *     connection (camera/webrtc/offer). A usable non-empty answer
 *     plus a first decoded frame earns the LIVE badge; an empty /
 *     failed answer, or a 20s offer timeout, falls through promptly
 *     to step 3 — no hang, no endless spinner, and the toggle to
 *     Event History is always tappable.
 *  3. The MJPEG proxy stream for the live-view camera, with the
 *     switch on. Frames here earn the LIVE badge ONLY while the
 *     switch state is confirmed on (see frontDoorShowsLiveBadge);
 *     if the switch reverts to off mid-stream, the badge drops and
 *     the status line says the frames are being treated as a
 *     recording. The wave test is what proves live: the status line
 *     tells the tester to wave and watch for it promptly.
 *  4. If MJPEG also errors, the status line names both failures and
 *     offers one tap to Event History.
 *
 * Event History is the UNCHANGED recording body (the MJPEG proxy
 * labeled "Latest motion recording · Not live"), exactly as before.
 * The panel never scrolls internally (PanelOverlay passes
 * scrollContent = false for cameras): this is a full-size Box with
 * a top chrome overlay, no verticalScroll anywhere — the #92
 * dialog-scroll crash class stays closed.
 *
 * Instrumentation: every transition logs under the Haven tag with
 * the FrontDoorLive prefix; the status line mirrors the active
 * path on screen. No entity id is ever printed on this surface.
 */
private enum class FrontDoorTab { LIVE, HISTORY }

private enum class FrontDoorLivePhase { WEBRTC_CONNECTING, WEBRTC_LIVE, MJPEG }

@Composable
private fun FrontDoorCameraPanel(
    vm: HavenViewModel,
    cameraId: String,
    enterProgress: Float,
    onClose: () -> Unit,
) {
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    val switchOn = states[FRONT_DOOR_LIVE_STREAM_SWITCH_ID]?.state == "on"
    val battery = states[FRONT_DOOR_BATTERY_SENSOR_ID]?.state
    val engaged = vm.frontDoorLiveEngaged
    val stopReason = vm.frontDoorLiveStopReason

    var tab by remember { mutableStateOf(FrontDoorTab.LIVE) }
    var phase by remember { mutableStateOf(FrontDoorLivePhase.WEBRTC_CONNECTING) }
    var webrtcAttempt by remember { mutableIntStateOf(0) }
    var webrtcFrames by remember { mutableStateOf(false) }
    var webrtcOutcome by remember { mutableStateOf<String?>(null) }
    var mjpegFrames by remember { mutableStateOf(false) }
    var mjpegError by remember { mutableStateOf<String?>(null) }

    // Engagement: the Live tab owns the switch. Resetting the probe
    // on every Live activation keeps a stale phase from surviving a
    // History round-trip or a background stop.
    LaunchedEffect(tab) {
        if (tab == FrontDoorTab.LIVE) {
            phase = FrontDoorLivePhase.WEBRTC_CONNECTING
            webrtcAttempt++
            webrtcFrames = false
            webrtcOutcome = null
            mjpegFrames = false
            mjpegError = null
            vm.startFrontDoorLive()
        }
    }
    // Backstop disengage: fires when the tab flips AND when the
    // panel unmounts (the dismiss hook fires first; this is a no-op
    // then). Never fires for History — already disengaged on flip.
    DisposableEffect(tab) {
        onDispose {
            if (tab == FrontDoorTab.LIVE) vm.disengageFrontDoorLive("tab-or-panel-leave")
        }
    }

    // WebRTC session: created in this effect, closed in its onDispose,
    // so leaving a WebRTC phase (fallback, background stop, tab flip)
    // always tears the peer connection down. The renderer is held in
    // a plain holder (not State): the AndroidView factory runs during
    // composition, before effects, so it is always set when this
    // effect runs — and a fresh renderer per attempt means a Resume
    // after a background stop never re-inits a released surface.
    val context = LocalContext.current
    val rendererHolder = remember { object { var renderer: SurfaceViewRenderer? = null } }
    val inWebRtcPhase = phase == FrontDoorLivePhase.WEBRTC_CONNECTING ||
        phase == FrontDoorLivePhase.WEBRTC_LIVE
    DisposableEffect(inWebRtcPhase, webrtcAttempt, engaged) {
        val renderer = rendererHolder.renderer
        if (!inWebRtcPhase || !engaged || renderer == null) {
            onDispose { }
        } else {
            val attemptAtStart = webrtcAttempt
            val session = RingWebRtcSession(
                appContext = context.applicationContext,
                sendOffer = { offerSdp, onAnswer ->
                    Log.d("Haven", "FrontDoorLive: webrtc offer sent (attempt $attemptAtStart)")
                    vm.client.requestWebRtcOffer(cameraId, offerSdp) { answer ->
                        if (webrtcAttempt != attemptAtStart) return@requestWebRtcOffer
                        if (answer.error != null || answer.answerSdp.isNullOrEmpty()) {
                            Log.w("Haven", "FrontDoorLive: webrtc answer EMPTY/failed: ${answer.error ?: "no SDP"}")
                            webrtcOutcome = "empty answer (${answer.error ?: "no SDP"})"
                            phase = FrontDoorLivePhase.MJPEG
                        } else {
                            Log.d("Haven", "FrontDoorLive: webrtc answer usable")
                            webrtcOutcome = "answer usable"
                        }
                    }
                },
                sendCandidate = { sessionId, candidateSdp ->
                    vm.client.sendWebRtcCandidate(cameraId, sessionId, candidateSdp)
                },
            )
            session.start(
                renderer = renderer,
                onFirstFrame = {
                    if (webrtcAttempt != attemptAtStart) return@start
                    Log.d("Haven", "FrontDoorLive: webrtc FIRST FRAME — live frames rendering")
                    webrtcFrames = true
                    phase = FrontDoorLivePhase.WEBRTC_LIVE
                },
                onFailed = { stage ->
                    if (webrtcAttempt != attemptAtStart) return@start
                    Log.w("Haven", "FrontDoorLive: webrtc failed stage=$stage")
                    webrtcOutcome = "failed ($stage)"
                    phase = FrontDoorLivePhase.MJPEG
                },
            )
            onDispose {
                session.close(renderer)
            }
        }
    }

    // Offer timeout: the WS callback may never fire on a dead socket,
    // so a WebRTC attempt that hears nothing in 20s falls through to
    // MJPEG instead of hanging. The effect restarts on every phase
    // change, so a completed attempt cancels its own timeout.
    LaunchedEffect(phase, webrtcAttempt) {
        if (phase != FrontDoorLivePhase.WEBRTC_CONNECTING) return@LaunchedEffect
        delay(20_000)
        Log.w("Haven", "FrontDoorLive: webrtc offer timed out (20s, no answer)")
        webrtcOutcome = "timed out (no answer in 20s)"
        phase = FrontDoorLivePhase.MJPEG
    }

    val showLive = frontDoorShowsLiveBadge(webrtcFrames, mjpegFrames, switchOn)
    // Round 4: clean user-facing status only — the #93 debug
    // "Live path: …" wording (switch + MJPEG internals, WebRTC
    // outcomes, "?" placeholders) is retired. The LIVE badge
    // rule is unchanged: it shows only while frames render on a
    // live path.
    val pathLine = when {
        tab == FrontDoorTab.HISTORY -> "Latest motion recording · Not live"
        !engaged -> "Live paused — the live-stream switch is off"
        phase == FrontDoorLivePhase.WEBRTC_CONNECTING -> "Connecting to live view…"
        phase == FrontDoorLivePhase.WEBRTC_LIVE -> "Live"
        phase == FrontDoorLivePhase.MJPEG && mjpegFrames && switchOn -> "Live view"
        phase == FrontDoorLivePhase.MJPEG && mjpegFrames && !switchOn -> "Latest motion recording"
        phase == FrontDoorLivePhase.MJPEG && mjpegError != null -> "Live view unavailable"
        phase == FrontDoorLivePhase.MJPEG -> "Connecting to live view…"
        else -> ""
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            tab == FrontDoorTab.HISTORY -> {
                CameraMjpegBody(vm, cameraId, enterProgress, recording = true, onClose = onClose)
            }
            !engaged -> {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        when (stopReason) {
                            "background" -> "Live paused when the app backgrounded"
                            "watchdog-cap" -> "Live stopped after the 10-minute cap"
                            else -> "Starting live…"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The live-stream switch is off, so the doorbell battery is safe.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                    if (stopReason != null) {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            phase = FrontDoorLivePhase.WEBRTC_CONNECTING
                            webrtcAttempt++
                            webrtcFrames = false
                            webrtcOutcome = null
                            mjpegFrames = false
                            mjpegError = null
                            vm.startFrontDoorLive()
                        }) { Text("Resume live") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { tab = FrontDoorTab.HISTORY }) {
                            Text("Show latest recording", color = Color.White)
                        }
                    }
                }
            }
            phase == FrontDoorLivePhase.MJPEG -> {
                CameraMjpegBody(
                    vm, cameraId, enterProgress, recording = false, onClose = onClose,
                    onFirstFrame = {
                        Log.d("Haven", "FrontDoorLive: mjpeg first frame (switch on=$switchOn)")
                        mjpegFrames = true
                    },
                    onError = { message ->
                        Log.w("Haven", "FrontDoorLive: mjpeg error: $message")
                        mjpegError = message
                    },
                )
            }
            else -> {
                // WebRTC phases share one mounted renderer: moving
                // CONNECTING -> LIVE must NOT remount the view, or
                // the session's frames would land on a released
                // surface. The key is the attempt, not the phase.
                key(webrtcAttempt) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).also { rendererHolder.renderer = it }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (phase == FrontDoorLivePhase.WEBRTC_CONNECTING) {
                    Column(
                        Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Connecting to live view…", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
        // Top scrim + chrome, above the body on both tabs.
        Box(
            Modifier.align(Alignment.TopStart).fillMaxWidth().height(240.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier.align(Alignment.TopStart).fillMaxWidth()
                .statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    entity?.let { displayCameraName(it) } ?: "Camera",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showLive) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(Color(0xFFD32F2F))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape)
                                .background(Color.White),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f)).padding(4.dp),
            ) {
                FrontDoorTabOption("Live View", tab == FrontDoorTab.LIVE) { tab = FrontDoorTab.LIVE }
                FrontDoorTabOption("Event History", tab == FrontDoorTab.HISTORY) { tab = FrontDoorTab.HISTORY }
            }
            Spacer(Modifier.height(8.dp))
            Text(pathLine, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "Live-stream switch: " + if (switchOn) "On" else "Off" +
                    (battery?.let { " · Battery $it%" } ?: "") +
                    " · Auto-off on close, background, or 10 min",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            if (tab == FrontDoorTab.LIVE && engaged) {
                TextButton(onClick = { tab = FrontDoorTab.HISTORY }) {
                    Text("Show latest recording instead", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RowScope.FrontDoorTabOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
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
    // Ring recording labels: the connecting text and the
    // error-stage prefix. Null keeps the existing copy (non-Ring
    // fullscreen and the recording view are byte-identical).
    connectingText: String? = null,
    errorPrefix: String? = null,
    // Front Door live probe (Round 2): fired once per stream start
    // on the first decoded frame, and on the error stage. Null keeps
    // every existing caller byte-identical; the callbacks run on the
    // stream's main-thread post, never during composition.
    onFirstFrame: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
) {
    var streamState by remember(cameraId) { mutableStateOf<CameraStreamState>(CameraStreamState.Connecting) }
    var attempt by remember(cameraId) { mutableStateOf(0) }
    LaunchedEffect(cameraId, attempt) {
        var firstFrameSeen = false
        val job = vm.client.streamCamera(cameraId) { state ->
            if (state is CameraStreamState.Playing) {
                vm.cacheCameraFrame(cameraId, state.frame)
                if (!firstFrameSeen) {
                    firstFrameSeen = true
                    onFirstFrame?.invoke()
                }
            }
            if (state is CameraStreamState.Error) onError?.invoke(state.message)
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
        CameraHeader(entity?.let { displayCameraName(it) } ?: "Camera", badgeText = "Recording", onClose = onClose)
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
 * The unfolded vertical navigation rail (2026-11-01 panels round):
 * the pill bar's unfolded form — same items ([pillBarItems]),
 * same selection ([selectedNavIndex]), same goTop destination
 * behavior, on the left edge at full height. Differences, all
 * deliberate: items stack icon-over-label in 72dp rows (touch targets at least as
 * large as the pill's 48dp rows); every destination shows its text
 * label below its icon (2026-11-01 round 1 — the pill's
 * selected-only / 10-char label rule does NOT apply to the rail);
 * the active indicator is a fixed 56dp x 32dp pill around the icon
 * only, never the item-height capsule; the rail never scrolls away
 * (it is nav chrome, and an open panel covers it anyway); and it
 * enters on the pill spring from a 16dp left offset instead of
 * appearing instantly. The column scrolls vertically once tabs
 * overflow the viewport height.
 */
@Composable
/**
 * The unfolded navigation (Round 4, superseding #86's full-height
 * 88dp rail): a FLOATING vertical side pill modeled after Koda's
 * pill — a margined, vertically centered island, NOT a full-height
 * bar. Same tabs, selection, and goTop behavior as the folded
 * pill. Ripple and active highlight live strictly inside the
 * 56x32dp icon oval: the item box itself is indication-null
 * (label taps still select), and only the icon oval carries a
 * default ripple clipped to CircleShape.
 */
@Composable
private fun VerticalSidePill(items: List<NavItem>, selectedIndex: Int?, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val currentIndex = selectedIndex ?: -1
    // Side-pill enter: spring in from the left edge on mount (the
    // pill remounts whenever the window crosses the wide
    // threshold, so this is also the fold/unfold transition).
    // Snapped under reduce-motion, like every other spring.
    val pillEnter = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) pillEnter.animateTo(1f, springSpatial())
    }
    // Measured icon positions, in window coordinates: the pill
    // origin is subtracted, so the indicator stays correct while
    // the column scrolls. The indicator anchors to the ICON rect,
    // not the whole item row: a fixed 56dp x 32dp pill around the
    // icon only, so the highlight never stretches into a tall
    // vertical capsule and never changes shape across tabs.
    var pillOrigin by remember { mutableStateOf(0f) }
    var iconRects by remember { mutableStateOf<Map<Int, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    val targetIconRect = iconRects[currentIndex]
    val density = LocalDensity.current
    val indicatorSpec = if (reduceMotion) spring<Float>(stiffness = 10_000f, dampingRatio = 1f)
        else springSpatial()
    // Icon is 24dp tall, pill is 32dp: center the pill on the icon
    // (4dp overhang top and bottom).
    val indicatorTop by animateFloatAsState(
        targetIconRect?.let { it.top - pillOrigin - with(density) { 4.dp.toPx() } } ?: 0f,
        animationSpec = indicatorSpec,
        label = "sidePillTop",
    )
    Box(
        modifier
            .width(88.dp)
            .fillMaxHeight()
            .padding(start = 12.dp, top = 24.dp, bottom = 24.dp)
            .graphicsLayer {
                alpha = pillEnter.value
                translationX = -16.dp.toPx() * (1f - pillEnter.value)
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = FloatingCorner,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            Box(
                Modifier.onGloballyPositioned { pillOrigin = it.boundsInWindow().top },
            ) {
                // The indicator: one fixed 56dp x 32dp pill around
                // the selected icon, behind the items.
                if (targetIconRect != null) {
                    Box(
                        Modifier.align(Alignment.TopCenter)
                            .offset { IntOffset(0, indicatorTop.roundToInt()) }
                            .size(56.dp, 32.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    )
                }
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for ((index, item) in items.withIndex()) {
                        val highlighted = index == currentIndex
                        // Side-pill labels are ALWAYS shown below
                        // the icon: the pill's selected-only / 10-char
                        // rule does not apply here — the side pill
                        // has room for every destination's label, and
                        // icon-only destinations (Garage Door, Living
                        // Room lights) were unreadable.
                        val contentColor = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier.width(80.dp).height(72.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (index != currentIndex) {
                                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        onSelect(item.screen)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Ripple lives ONLY on the 56x32dp
                                // icon oval (clipped to CircleShape);
                                // the label above/below it taps
                                // through the indication-null box.
                                Box(
                                    Modifier.size(56.dp, 32.dp).clip(CircleShape)
                                        .clickable {
                                            if (index != currentIndex) {
                                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                onSelect(item.screen)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        if (highlighted) item.icon else item.outlinedIcon,
                                        contentDescription = item.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(24.dp)
                                            .onGloballyPositioned { iconRects = iconRects + (index to it.boundsInWindow()) },
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
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
    // Round 4: the indicator is a FIXED 56x32dp oval centered on the
    // selected/scrub-hovered tab's ICON rect — never the full square
    // text bounding box (the old whole-item rect + 8dp padding made
    // a 48dp-tall capsule that swallowed the label). Icon rects are
    // recorded on the icon boxes below; the item rect is only the
    // first-frame fallback before any icon has reported.
    var barOrigin by remember { mutableStateOf(0f) }
    var itemRects by remember { mutableStateOf<Map<Int, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    var iconRects by remember { mutableStateOf<Map<Int, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    val targetIndex = scrub.hoveredTab ?: currentIndex
    val targetRect = iconRects[targetIndex] ?: itemRects[targetIndex]
    val indicatorSpec = if (reduceMotion) spring<Float>(stiffness = 10_000f, dampingRatio = 1f)
        else spring(dampingRatio = 0.74f, stiffness = 380f)
    val density = LocalDensity.current
    val indicatorCenterX by animateFloatAsState(
        targetRect?.let { (it.left + it.right) / 2f - barOrigin } ?: 0f,
        animationSpec = indicatorSpec,
        label = "pillCenterX",
    )
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
            // The morphing indicator, behind the items: a fixed
            // 56x32dp oval centered on the target icon (Round 4 —
            // the old whole-item capsule is gone). Vertically
            // centered in the bar; only its horizontal center
            // animates, so the highlight glides tab to tab without
            // ever resizing into the label's bounding box.
            if (targetRect != null) {
                Box(
                    Modifier.align(Alignment.CenterStart)
                        .offset { IntOffset((indicatorCenterX - with(density) { 28.dp.toPx() }).roundToInt(), 0) }
                        .size(56.dp, 32.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                )
            }
            // Scrolls horizontally once tabs overflow; short bars stay centered.
            Row(
                // Row horizontal padding 18dp (2026-11-01 polish
                // round, was 10dp): the selected indicator overhangs
                // its item by 8dp each side, so at 10dp the first /
                // last selected pill sat ~2dp from the bar's
                // background edge — flush, while middle tabs float
                // inset. 18dp gives edge selections the same ~10dp
                // breathing room inside the bar. Height unchanged
                // (8 + 48 + 8 = 64dp); auto-width centering untouched.
                modifier = Modifier.wrapContentWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp).pillScrub(scrub),
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (index != currentIndex) {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onSelect(item.screen)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Round 4: ripple lives ONLY on the 56x32dp
                        // icon oval (clipped to CircleShape); the
                        // label taps through the indication-null
                        // item box, so no ripple or highlight ever
                        // covers the full text bounding box.
                        val onIconTap = {
                            if (index != currentIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onSelect(item.screen)
                            }
                        }
                        if (showLabel) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(56.dp, 32.dp).clip(CircleShape).clickable(onClick = onIconTap),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(item.icon, contentDescription = item.label, tint = contentColor, modifier = Modifier.size(24.dp)
                                        .onGloballyPositioned { iconRects = iconRects + (index to it.boundsInWindow()) })
                                }
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
                            Box(
                                Modifier.size(56.dp, 32.dp).clip(CircleShape).clickable(onClick = onIconTap),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (highlighted) item.icon else item.outlinedIcon,
                                    contentDescription = item.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                        .onGloballyPositioned { iconRects = iconRects + (index to it.boundsInWindow()) },
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
 * arrow, dismiss via the panel's back arrow/Back like every other
 * pop-up. Exists so even the fallback pop-up never shows a ←.
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
        // Badge only hides while traveling (2026-11-01 polish
        // round): the name/state text stays visible from the
        // panel's first frame — hiding the whole row held the
        // title back until the final 2% handoff window.
        DomainBadge(
            entity,
            domainIcon(entity),
            accent,
            modifier = Modifier.size(48.dp).hideWhileTraveling().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            DialogTitleText(
                if (entity.domain == "sensor") sensorDisplayName(entity) else displayCameraName(entity),
                Modifier.fillMaxWidth(),
            )
            Text(entity.dialogHeaderState(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
