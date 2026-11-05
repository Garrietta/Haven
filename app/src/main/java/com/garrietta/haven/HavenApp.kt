package com.garrietta.haven
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.nonTabSlideDirection
import com.garrietta.haven.ui.components.shouldApplyFoldedNavClearance
import com.garrietta.haven.ui.components.LocalSuppressTraveler
import com.garrietta.haven.ui.components.PopupDragHandle
import com.garrietta.haven.ui.components.PopupDragState
import com.garrietta.haven.ui.components.PopupDismissConnection
import com.garrietta.haven.ui.components.LocalCardColors
import com.garrietta.haven.ui.components.LocalCardShapes
import com.garrietta.haven.ui.components.centeredPopupEnter
import com.garrietta.haven.ui.components.morphCloseContentAlpha
import com.garrietta.haven.ui.components.morphEnterContentAlpha
import com.garrietta.haven.ui.components.morphPanelBackgroundBlend
import com.garrietta.haven.ui.components.morphPopupShape
import com.garrietta.haven.ui.components.tileMorphTransform
import com.garrietta.haven.ui.components.dragProgressAfterDelta
import com.garrietta.haven.ui.components.dragRangeForCenteredPopup
import com.garrietta.haven.ui.components.dragRangeForPanelOverlay
import com.garrietta.haven.ui.components.popupDragDetector
import com.garrietta.haven.ui.components.shouldDismissDrag
import com.garrietta.haven.ui.components.ShapePrefs
import com.garrietta.haven.ui.components.LocalModalActive
import com.garrietta.haven.ui.components.LocalPopupDragHandle
import com.garrietta.haven.ui.components.LocalPopupDragRangePx
import com.garrietta.haven.ui.components.morphHeaderBadgeAlpha
import com.garrietta.haven.ui.components.MorphSurface
import com.garrietta.haven.ui.components.morphPanelContainerAlphaOpen
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.controls.entityIcon
import com.garrietta.haven.ui.components.fallbackFromRect
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.components.formatCondition
import com.garrietta.haven.data.alertDotColor
import com.garrietta.haven.data.ControlDeepLinkTarget
import com.garrietta.haven.data.controlDeepLinkTarget
import com.garrietta.haven.data.hasImportantAlert
import com.garrietta.haven.data.isWideLayout
import com.garrietta.haven.ui.components.NavItem
import com.garrietta.haven.ui.components.NavPillGeometry
import com.garrietta.haven.ui.components.NavStyle
import com.garrietta.haven.ui.components.navActiveScreen
import com.garrietta.haven.ui.components.shouldNavigateOnTabTap
import com.garrietta.haven.ui.components.navPillGeometry
import com.garrietta.haven.ui.components.railIndicatorTop
import com.garrietta.haven.ui.components.pillBarItems
import com.garrietta.haven.ui.components.tabSlideDirection
import com.garrietta.haven.ui.components.tabSlideOffsets
import com.garrietta.haven.ui.components.selectedNavIndex
import com.garrietta.haven.ui.theme.FloatingCorner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import android.view.SurfaceView
import com.garrietta.haven.data.cameraSupportsStream
import com.garrietta.haven.data.detailBadgeOverride
import com.garrietta.haven.data.detailPageAccent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.garrietta.haven.data.CameraStreamState
import com.garrietta.haven.data.FRONT_DOOR_LIVE_STREAM_SWITCH_ID
import com.garrietta.haven.data.FRONT_DOOR_LIVE_VIEW_ID
import com.garrietta.haven.data.FRONT_DOOR_RING_MQTT_CAMERA_ID
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.RingWebRtcSession
import com.garrietta.haven.data.frontDoorShowsLiveBadge
import com.garrietta.haven.data.resolveFrontDoorCameraSource
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.data.displayCameraName
import org.webrtc.SurfaceViewRenderer
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.DialogTitleText
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.LocalWindowSizeClass
import com.garrietta.haven.ui.components.windowSizeClassFor
import com.garrietta.haven.ui.components.SheetPageContent
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.KodaMenuRow
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.PillBarActiveCapsule
import com.garrietta.haven.ui.components.PillBarActiveGlyph
import com.garrietta.haven.ui.components.PillBarContainer
import com.garrietta.haven.ui.components.PillBarUnselectedGlyph
import com.garrietta.haven.ui.components.pillUnselectedGlyph
import com.garrietta.haven.ui.theme.settleSpring
import com.garrietta.haven.ui.theme.sheetSpring
import com.garrietta.haven.ui.components.pillScrub
import com.garrietta.haven.ui.components.pillScrubItem
import com.garrietta.haven.ui.components.pillScrubVertical
import com.garrietta.haven.ui.components.rememberPillScrubState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import com.garrietta.haven.ui.components.LocalArcBounds
import com.garrietta.haven.ui.components.LocalArcTarget
import com.garrietta.haven.ui.components.LocalCardBounds
import com.garrietta.haven.ui.components.LocalHeroBounds
import com.garrietta.haven.ui.components.LocalHeroTarget
import com.garrietta.haven.ui.components.LocalHiddenCardId
import com.garrietta.haven.ui.components.LocalHiddenHeroId
import com.garrietta.haven.ui.components.LocalMorphCardAlpha
import com.garrietta.haven.ui.components.LocalMorphClosing
import com.garrietta.haven.ui.components.LocalMorphProgress
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.lerpRect
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.components.morphPanelContainerAlphaClose
import com.garrietta.haven.ui.components.weatherConditionColorThemed
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.selectedPillFill
import com.garrietta.haven.ui.components.selectedPillGlyph
import com.garrietta.haven.ui.components.ScrollVisibilityState
import com.garrietta.haven.ui.components.heroHeaderAlpha
import com.garrietta.haven.ui.theme.popupMorphSpring
import com.garrietta.haven.ui.theme.springBounds
import com.garrietta.haven.ui.theme.kodaBarSpec
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
import com.garrietta.haven.ui.screens.EnergyScreen
import com.garrietta.haven.ui.screens.ScenesScreen
import com.garrietta.haven.ui.screens.DetailPageScreen
import com.garrietta.haven.ui.screens.HelperBuilderScreen
import com.garrietta.haven.ui.screens.HomeEditorScreen
import com.garrietta.haven.ui.screens.HomeStatusPanelScreen
import com.garrietta.haven.ui.screens.LoginScreen
import com.garrietta.haven.ui.screens.SceneBuilderScreen
import com.garrietta.haven.ui.screens.ScriptBuilderScreen
import com.garrietta.haven.ui.screens.SearchScreen
import com.garrietta.haven.ui.screens.RoomFilter
import com.garrietta.haven.ui.screens.RoomsTabsScreen
import com.garrietta.haven.ui.screens.SettingsScreen
import com.garrietta.haven.ui.screens.DomainDefaultsDialog
import com.garrietta.haven.ui.screens.ShapePickerDialog
import com.garrietta.haven.ui.screens.CardSettingsSheet
import com.garrietta.haven.ui.screens.CardSettingsPencil
import com.garrietta.haven.ui.screens.rememberCardCustom
import com.garrietta.haven.ui.screens.UserTabsScreen
import com.garrietta.haven.ui.screens.ThermostatPopout
import com.garrietta.haven.ui.screens.TodayScreen
import com.garrietta.haven.ui.screens.GreenhousePanelScreen
import com.garrietta.haven.ui.screens.MediaPanelScreen
import com.garrietta.haven.ui.screens.SecurityPanelScreen
import com.garrietta.haven.ui.screens.UpdatesPanelScreen
import com.garrietta.haven.ui.screens.VacuumPanelScreen
import com.garrietta.haven.ui.screens.UserTabScreen
import com.garrietta.haven.ui.theme.HavenTheme
import com.garrietta.haven.ui.theme.resolveHavenIsDark

// Stage C: the navigation model (NavItem, pillBarItems, tabIcons,
// selectedNavIndex) moved to ui/components/NavItems.kt as pure,
// unit-testable functions — same behavior, plus hidden-tab filtering
// and picker icon overrides. See that file for the contract.

private fun isTopLevel(screen: Screen): Boolean = when (screen) {
    Screen.Today, Screen.Scenes, Screen.Energy, Screen.Dashboards, Screen.Areas, Screen.Search, Screen.UserTabs, Screen.Settings -> true
    is Screen.UserTab -> true
    is Screen.RoomTab -> true
    else -> false
}

// Stage F cleanup: the old private selectedNavIndex that lived here
// (3-arg, userTabs-indexed) is DELETED — it was dead code left by
// Stage C's move (both call sites already used the NavItems version),
// and its userTabs math was wrong under hidden-tab filtering. The
// live function is selectedNavIndex in ui/components/NavItems.kt.

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
 * Feedback round 16, item 4c: the render sites prefer this CAPTURED
 * rect over the live map (see resolveMorphSource): while a panel
 * for an entity is open, that panel's own cards report their
 * panel-internal rects under the same id, so a live-first read
 * lands the exit morph on the panel-internal rect instead of the
 * tapped tile. The live map stays the fallback for cards that
 * never reported bounds. Rotation mid-panel is the one accepted
 * edge: the exit lands on the pre-rotation tile rect rather than
 * tracking the moved tile — far rarer than the always-wrong
 * poisoned read it replaces.
 * sourceColor is the tile's container color captured at the same
 * instant (item 4a): the morph surface starts at the tile's own
 * color instead of flashing panel cream over the hidden tile.
 */
private class SheetAnim(val id: String, val source: Rect?, val sourceColor: Color? = null)

/** Open detail morph: the backstack screen plus its source card rect (same fallback rule as sheets). */
private class DetailAnim(val screen: Screen, val entityId: String, val source: Rect?, val sourceColor: Color? = null)

/** Feedback round 5, item 1: a detail opened from a panel card, stacked above the panel overlay
 * WITHOUT touching the backstack. The panel stays the backstack top, so the base page never
 * switches to the panel page (the "middleman" screen). Back from this layer returns to the panel
 * popup, not Home. The source rect is captured at tap time from the panel card, same rule as
 * the detail block's open-captured rect. */
private class PanelDetailAnim(val entityId: String, val source: Rect?, val sourceColor: Color? = null)

/** Open thermostat pop-out morph: the climate entity plus its source card rect (same fallback rule as sheets). */
private class PopoutAnim(val id: String, val source: Rect?, val sourceColor: Color? = null)

@Composable
fun HavenApp(vm: HavenViewModel) {
    var themeMode by remember { mutableStateOf(vm.settingsStore.themeMode) }
    var reduceMotion by remember { mutableStateOf(vm.settingsStore.reduceMotion) }
    // Stage B: dynamic (wallpaper) color, off by default. The fixed
    // Haven palettes are the identity; this only ever applies when
    // the user explicitly enables it in Settings → Appearance.
    var dynamicColor by remember { mutableStateOf(vm.settingsStore.dynamicColor) }
    val darkTheme = resolveHavenIsDark(themeMode, isSystemInDarkTheme())
    // Read so shape-picker saves recompose the provider below from the store.
    @Suppress("UNUSED_VARIABLE")
    val shapePrefsVersion = vm.shapePrefsVersion
    HavenTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
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
        // Feedback round 9, item 2 (ROOT CAUSE of Home losing its
        // scroll offset on detail return): the scaffold renders its
        // base page through TWO different parents — directly via
        // content(base) while a detail overlay is open, and inside
        // AnimatedContent(targetState = base) otherwise (round 4,
        // item 4's bleed/flash rule). Swapping parents DISPOSES
        // the base content, and with it any scroll state remembered
        // inside TodayScreen (the folded LazyColumn's implicit
        // LazyListState, the unfolded panes' rememberScrollState),
        // so Home recomposed at offset 0 on every detail return.
        // Hoisting the three Home scroll states HERE, above the
        // branch swap, keeps the state objects alive across it:
        // the list/panes reattach to the same state on return and
        // land on the exact pre-open offset. Consequence, stated
        // deliberately: the offset now also survives tab switches
        // (previously a switch disposed Home and reset it); the
        // branch-swap rule itself is untouched. Sign-out still
        // resets: these remembers live below the auth early-return,
        // so they dispose with the post-login composition.
        val homeListState = rememberLazyListState()
        val homeLeftScrollState = rememberScrollState()
        val homeRightScrollState = rememberScrollState()
        // Feedback round 19, item 2: the non-tab slide direction.
        // AnimatedContent's transitionSpec cannot tell a push from
        // a pop on its own (both are just initialState ->
        // targetState), so the direction is tracked here from the
        // backstack mutations themselves: navigate() pushes (+1),
        // goBack() pops (-1), goTop() jumps to a tab (+1, the old
        // forward behavior for tab jumps). The transitionSpec
        // reads this at transition time; a change without a
        // targetState change starts no transition.
        // Feedback round 19, item 3: the Round 9 branch swap itself
        // is GONE — AnimatedContent(targetState = base) stays
        // mounted at all times now (see the transition block), so
        // TodayScreen never disposes under a morphing popup. The
        // hoisted scroll states above stay: they are still what
        // keeps Home's offset across detail returns.
        var navDirection by remember { mutableStateOf(1) }
        val current = backstack.last()
        // Panel state machine (2026-11-01 morphing-panels round,
        // superseding the 2026-10-30 centered-dialog round): the
        // sheet, detail, and pop-out overlays below are FULL-SCREEN
        // panels, not centered dialogs and not framework
        // sharedBounds. Each panel captures its source card's root
        // rect at open for the traveling header/arc (which
        // interpolate ONE rect to the panel header, and exactly
        // back on dismiss); the source card stays rendered and
        // stationary throughout (Round 5, see
        // LocalMorphCardAlpha), and the panel's opaque surface
        // covers it, so source and destination are never both
        // visible. The camera fullscreen keeps its own
        // rise, and vm.pageOrigin is still set by tile taps and
        // panel Details because the camera path needs it.
        val detailScreen = when (current) {
            is Screen.EntityDetail,
            is Screen.VacuumPanel, is Screen.MediaPanel,
            is Screen.SecurityPanel, is Screen.GreenhousePanel,
            is Screen.UpdatesPanel, is Screen.HomeStatusPanel -> current
            else -> null
        }
        val cardBounds = remember { mutableStateMapOf<String, Rect>() }
        // Feedback round 11, item 2: the source tiles' outline
        // shapes, reported alongside cardBounds (see reportCardShape).
        val cardShapes = remember { mutableStateMapOf<String, androidx.compose.ui.graphics.Shape>() }
        val cardColors = remember { mutableStateMapOf<String, Color>() }
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
        // Feedback round 5, item 1: the stacked panel-detail layer (see PanelDetailAnim).
        var panelDetailAnim by remember { mutableStateOf<PanelDetailAnim?>(null) }
        val panelDetailProgress = remember { Animatable(0f) }
        val sheetProgress = remember { Animatable(0f) }
        val detailProgress = remember { Animatable(0f) }
        var popoutAnim by remember { mutableStateOf<PopoutAnim?>(null) }
        // Feedback round 2, item 7: the last ON-TAB rail
        // destination, remembered so the rail's selected
        // indicator survives popups (updated at the rail call
        // site below whenever selectedNavIndex returns
        // non-null). Feedback round 16, item 9: this stores
        // the SCREEN, never an index — after an add/pin/
        // remove the bar list shifts and a stored index
        // points one slot off (vid9: Add-a-tab, back out,
        // highlight wrong). The index is resolved fresh
        // against the current bar list every composition.
        var lastRailTabScreen by remember { mutableStateOf<Screen?>(null) }
        val popoutProgress = remember { Animatable(0f) }
        // Feedback round 7: per-surface drag state for the Koda
        // now-playing morph port (see MorphOverlay.PopupDragState).
        // Declared here, next to the progress animatables, so the
        // dismiss functions below can reference them.
        val sheetDrag = remember { PopupDragState() }
        val detailDrag = remember { PopupDragState() }
        val panelDetailDrag = remember { PopupDragState() }
        val popoutDrag = remember { PopupDragState() }
        val cameraDrag = remember { PopupDragState() }
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
        var panelDetailExiting by remember { mutableStateOf(false) }
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
        val navigate: (Screen) -> Unit = { screen ->
            navDirection = 1
            backstack.add(screen)
        }
        // Feedback round 3, item 6: Device Controls deep links.
        // The provider's app intent carries the tapped entity id
        // (see appIntentFor); MainActivity stores it on the VM.
        // The router below consumes it once the entity's state has
        // loaded and jumps to that entity's surface — it renders
        // nothing, so its states collection cannot recompose UI.
        ControlDeepLinkRouter(vm, current, navigate)
        val goTop: (Screen) -> Unit = {
            navDirection = 1
            // Feedback round 19, item 3: a tab jump to Home means
            // My Home (the old behavior fell out of TodayScreen
            // disposal; the ViewModel-hoisted filter needs the
            // explicit reset). A room tab jump sets that room's
            // filter. Other tabs leave the filter alone — it only
            // renders on Today anyway.
            when (it) {
                Screen.Today -> vm.roomFilter = RoomFilter.All
                is Screen.RoomTab -> vm.roomFilter = RoomFilter.Area(it.areaId)
                else -> {}
            }
            morphGeneration++
            vm.pageOrigin = null
            vm.closePopup()
            sheetAnim = null
            detailAnim = null
            panelDetailAnim = null
            popoutAnim = null
            // Feedback round 7: instant navigation clears any
            // in-flight drag state alongside the anims.
            sheetDrag.reset()
            detailDrag.reset()
            panelDetailDrag.reset()
            popoutDrag.reset()
            cameraDrag.reset()
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
        // Expressive-menus round, lamp-card fix (2026-11-03
        // video verdict): every instant dismiss ALSO clears the
        // entity's travel snapshot directly. The LaunchedEffect
        // null-branches below were supposed to do this, but they
        // require the anim to still be non-null when the trigger
        // clears — and these functions null the trigger AND the
        // anim in the same event, so the branch never fired and
        // the snapshot leaked on every close. A leaked snapshot
        // froze the source card at close-time state: later taps
        // flipped the live state (the lamp audibly shut off) while
        // the card kept showing the snapshot, until the next panel
        // open re-captured it. Clearing here is exact: the panel
        // is gone, so no flight needs the snapshot anymore, and a
        // sheet->detail handoff re-captures fresh for the detail.
        // The effect branches stay as backup.
        fun dismissDetailInstant() {
            val anim = detailAnim ?: return
            if (backstack.lastOrNull() == anim.screen && backstack.size > 1) {
                backstack.removeAt(backstack.lastIndex)
            }
            if (detailAnim?.screen == anim.screen) {
                detailAnim = null
            }
            // Feedback round 7: instant clears drop drag state too.
            detailDrag.reset()
            vm.clearTravelSnapshot(anim.entityId)
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
            // Feedback round 7: instant clears drop drag state too.
            sheetDrag.reset()
            vm.clearTravelSnapshot(anim.id)
            heroTarget = null
            arcTarget = null
        }
        fun dismissPopoutInstant() {
            val anim = popoutAnim ?: return
            if (vm.climatePopoutId == anim.id) vm.closeClimatePopout()
            if (popoutAnim?.id == anim.id) popoutAnim = null
            // Feedback round 7: instant clears drop drag state too.
            popoutDrag.reset()
            vm.clearTravelSnapshot(anim.id)
            heroTarget = null
            arcTarget = null
        }
        // Animated dismisses: Back, the back arrow, and Close all use
        // these. The progress animates 1 -> 0 on the settle spring
        // (feedback round 7: Koda's calm 0.95/350, replacing
        // springBounds for dismisses; opens stay on springBounds),
        // snapped under reduce-motion; only then is the trigger
        // cleared, and only if the generation + identity still
        // match the morph that started (see above).
        // Feedback round 7 drag-aware prologue (the single choke
        // point — Back, badge tap, scrim tap, and drag release all
        // land here): if a drag is in flight, mark settling BEFORE
        // flipping exiting, so isClosing stays false and the surface
        // keeps the OPEN fade curves through the settle (content
        // rides down visibly on the settle spring, instead of
        // vanishing on the close curve). Feedback round 18, item 5
        // (finger tracking): the surface DID move under the finger,
        // so the dismiss snap-to-released-progress below (before
        // the drag flag clears) is what keeps the surface from
        // jumping back to full-open for a frame.
        fun dismissDetail() {
            val anim = detailAnim ?: return
            if (detailExiting) return
            val fromDrag = detailDrag.isDragging
            if (fromDrag) detailDrag.settling = true
            detailExiting = true
            vm.captureTravelSnapshot(anim.entityId)
            val gen = morphGeneration
            scope.launch {
                try {
                    if (fromDrag) {
                        // Round 18, item 5: snap onto the released
                        // drag progress BEFORE clearing isDragging
                        // (progress() reads dragProgress while
                        // dragging, anim.value once cleared).
                        detailProgress.snapTo(detailDrag.dragProgress)
                        detailDrag.isDragging = false
                        detailDrag.pulling = false
                    }
                    if (reduceMotion) detailProgress.snapTo(0f)
                    else detailProgress.animateTo(0f, settleSpring())
                } finally {
                    detailExiting = false
                    detailDrag.settling = false
                    if (gen == morphGeneration && detailAnim?.screen == anim.screen) {
                        dismissDetailInstant()
                    }
                }
            }
        }
        fun dismissSheet() {
            val anim = sheetAnim ?: return
            if (sheetExiting) return
            val fromDrag = sheetDrag.isDragging
            if (fromDrag) sheetDrag.settling = true
            sheetExiting = true
            vm.captureTravelSnapshot(anim.id)
            val gen = morphGeneration
            scope.launch {
                try {
                    if (fromDrag) {
                        // Round 18, item 5: snap onto the released
                        // drag progress BEFORE clearing isDragging.
                        sheetProgress.snapTo(sheetDrag.dragProgress)
                        sheetDrag.isDragging = false
                        sheetDrag.pulling = false
                    }
                    if (reduceMotion) sheetProgress.snapTo(0f)
                    else sheetProgress.animateTo(0f, settleSpring())
                } finally {
                    sheetExiting = false
                    sheetDrag.settling = false
                    if (gen == morphGeneration && sheetAnim?.id == anim.id) {
                        dismissSheetInstant()
                    }
                }
            }
        }
        fun dismissPopout() {
            val anim = popoutAnim ?: return
            if (popoutExiting) return
            val fromDrag = popoutDrag.isDragging
            if (fromDrag) popoutDrag.settling = true
            popoutExiting = true
            vm.captureTravelSnapshot(anim.id)
            val gen = morphGeneration
            scope.launch {
                try {
                    if (fromDrag) {
                        // Round 18, item 5: snap onto the released
                        // drag progress BEFORE clearing isDragging.
                        popoutProgress.snapTo(popoutDrag.dragProgress)
                        popoutDrag.isDragging = false
                        popoutDrag.pulling = false
                    }
                    if (reduceMotion) popoutProgress.snapTo(0f)
                    else popoutProgress.animateTo(0f, settleSpring())
                } finally {
                    popoutExiting = false
                    popoutDrag.settling = false
                    if (gen == morphGeneration && popoutAnim?.id == anim.id) {
                        dismissPopoutInstant()
                    }
                }
            }
        }
        // Feedback round 5, item 1: the stacked panel-detail layer's dismiss paths. These mirror
        // dismissDetail/dismissDetailInstant exactly, except they never touch the backstack —
        // the panel stays the backstack top throughout, so dismissing returns to the panel popup.
        fun dismissPanelDetailInstant() {
            val anim = panelDetailAnim ?: return
            if (panelDetailAnim?.entityId == anim.entityId) {
                panelDetailAnim = null
            }
            // Feedback round 7: instant clears drop drag state too.
            panelDetailDrag.reset()
            vm.clearTravelSnapshot(anim.entityId)
            heroTarget = null
            arcTarget = null
        }
        fun dismissPanelDetail() {
            val anim = panelDetailAnim ?: return
            if (panelDetailExiting) return
            // Feedback round 7: same drag-aware prologue as the
            // other dismisses (see dismissDetail).
            val fromDrag = panelDetailDrag.isDragging
            if (fromDrag) panelDetailDrag.settling = true
            panelDetailExiting = true
            vm.captureTravelSnapshot(anim.entityId)
            val gen = morphGeneration
            scope.launch {
                try {
                    if (fromDrag) {
                        // Round 18, item 5: snap onto the released
                        // drag progress BEFORE clearing isDragging.
                        panelDetailProgress.snapTo(panelDetailDrag.dragProgress)
                        panelDetailDrag.isDragging = false
                        panelDetailDrag.pulling = false
                    }
                    if (reduceMotion) panelDetailProgress.snapTo(0f)
                    else panelDetailProgress.animateTo(0f, settleSpring())
                } finally {
                    panelDetailExiting = false
                    panelDetailDrag.settling = false
                    if (gen == morphGeneration && panelDetailAnim?.entityId == anim.entityId) {
                        dismissPanelDetailInstant()
                    }
                }
            }
        }
        // Feedback round 7: the drag controller for the Koda
        // now-playing port. One handle per surface, built at the
        // render block; the wrapper supplies the drag range per
        // event (it owns the placed rect). Rules, in order:
        // - Under reduce-motion the drag never engages (Back/dismiss
        //   snap as today). This is a deliberate new behavior: the
        //   existing idiom never disabled a gesture, but a scrubbed
        //   morph under reduce-motion would be animation by
        //   another name.
        // - While a dismiss animation owns the surface (exiting),
        //   deltas are ignored; a release then just clears the flag.
        // - A drag only engages at rest (anim >= 0.99): a grab
        //   mid-open simply does not engage, so the gesture never
        //   fights an in-flight open animation.
        // - First accepted delta latches dragging, records the
        //   start progress, and re-captures the travel snapshot
        //   (state may have changed since open, and the return
        //   flight must reflect current state). A cancelled drag
        //   never clears the snapshot.
        // - Release commits on Koda's rule (velocity momentum, else
        //   40dp travel) with a single ContextClick settle tick,
        //   then calls the surface's dismiss while still dragging
        //   so its prologue keeps the open curves; otherwise the
        //   surface — which never moved under the finger — settles
        //   back to open on the settle spring. DragCancel releases
        //   with zero velocity (travel rule decides).
        val dragHaptics = LocalHapticFeedback.current
        val dragCommitPx = with(LocalDensity.current) { 40.dp.toPx() }
        // Feedback round 18, item 5 (EXPLICIT REVERSAL of round
        // 11, item 1): FINGER-TRACKED dismiss. The morph progress
        // is the drag state's dragProgress while a drag is in
        // flight, so dragging down on a popup scrubs its morph
        // in real time with the finger; at rest it is the
        // animatable's value. The drag state's dragProgress is
        // travel bookkeeping AND the rendered progress now.
        fun dragProgressFor(
            anim: Animatable<Float, AnimationVector1D>,
            drag: PopupDragState,
        ): () -> Float = { if (drag.isDragging) drag.dragProgress else anim.value }
        fun onDragDelta(
            drag: PopupDragState,
            anim: Animatable<Float, AnimationVector1D>,
            isExiting: () -> Boolean,
            onDragStart: () -> Unit,
            dy: Float,
            rangePx: Float,
        ) {
            if (reduceMotion) return
            if (isExiting()) return
            if (!drag.isDragging) {
                if (anim.value < 0.99f) return
                drag.isDragging = true
                drag.dragProgress = anim.value
                drag.dragStartProgress = anim.value
                onDragStart()
            }
            drag.dragProgress = dragProgressAfterDelta(drag.dragProgress, dy, rangePx)
        }
        fun onDragRelease(
            drag: PopupDragState,
            anim: Animatable<Float, AnimationVector1D>,
            velocityY: Float,
            rangePx: Float,
            isExiting: () -> Boolean,
            dismiss: () -> Unit,
        ) {
            if (!drag.isDragging) return
            if (isExiting()) {
                drag.isDragging = false
                drag.pulling = false
                return
            }
            val released = drag.dragProgress
            val commit = shouldDismissDrag(
                velocityY,
                drag.dragStartProgress,
                released,
                rangePx,
                dragCommitPx,
            )
            if (commit) {
                dragHaptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                dismiss()
            } else {
                // Feedback round 18, item 5 (finger tracking):
                // the surface sits at the released drag progress,
                // so snap the animatable onto it WHILE isDragging
                // is still true — clearing the flag first would
                // let one frame read anim.value = 1f and jump the
                // surface back to full-open before the settle.
                scope.launch {
                    anim.snapTo(released)
                    drag.isDragging = false
                    drag.pulling = false
                    anim.animateTo(1f, settleSpring())
                }
            }
        }
        fun popupDragHandle(
            drag: PopupDragState,
            anim: Animatable<Float, AnimationVector1D>,
            isExiting: () -> Boolean,
            onDragStart: () -> Unit,
            dismiss: () -> Unit,
        ): PopupDragHandle = PopupDragHandle(
            state = drag,
            onDelta = { dy, range -> onDragDelta(drag, anim, isExiting, onDragStart, dy, range) },
            onRelease = { v, range -> onDragRelease(drag, anim, v, range, isExiting, dismiss) },
        )
        // Open a panel detail: capture the tapped panel card's rect now (the panel is still
        // composed, so cardBounds holds the panel card's rect); the open effect below animates
        // progress 0 -> 1. A retap while one is open replaces it and clears the stale snapshot.
        fun openPanelDetail(entityId: String) {
            val prev = panelDetailAnim
            if (prev != null && prev.entityId != entityId) {
                vm.clearTravelSnapshot(prev.entityId)
            }
            if (prev?.entityId == entityId) return
            panelDetailAnim = PanelDetailAnim(entityId, cardBounds[entityId], cardColors[entityId])
        }
        val goBack: () -> Unit = {
            if (panelDetailAnim != null) dismissPanelDetail()
            else if (detailAnim != null) dismissDetail()
            else if (popoutAnim != null) dismissPopout()
            else if (backstack.size > 1) {
                navDirection = -1
                backstack.removeAt(backstack.lastIndex)
            }
        }
        val signOut: () -> Unit = {
            morphGeneration++
            vm.pageOrigin = null
            vm.closePopup()
            sheetAnim = null
            detailAnim = null
            panelDetailAnim = null
            popoutAnim = null
            // Feedback round 7: same instant drag-state clear as goTop.
            sheetDrag.reset()
            detailDrag.reset()
            panelDetailDrag.reset()
            popoutDrag.reset()
            cameraDrag.reset()
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
            sheetAnim = SheetAnim(vm.popupEntityId!!, cardBounds[vm.popupEntityId!!], cardColors[vm.popupEntityId!!])
        }
        if (detailScreen != null && detailAnim?.screen != detailScreen) {
            detailAnim = DetailAnim(detailScreen, detailEntityId(detailScreen), cardBounds[detailEntityId(detailScreen)], cardColors[detailEntityId(detailScreen)])
        }
        LaunchedEffect(vm.popupEntityId) {
            val id = vm.popupEntityId
            val anim = sheetAnim
            if (id != null && anim?.id == id) {
                vm.captureTravelSnapshot(id)
                // Feedback round 7: open resets the drag state
                // (Koda's open-state backstop: a latched drag flag
                // can never survive into a fresh open).
                sheetDrag.reset()
                // Always animate (unless reduce-motion): a sourced
                // morph interpolates from the card, a sourceless one
                // from the graceful fallback rect — never an instant
                // appearance.
                if (!reduceMotion) {
                    sheetProgress.snapTo(0f)
                    sheetProgress.animateTo(1f, popupMorphSpring())
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
                // Feedback round 7: open resets the drag state.
                detailDrag.reset()
                if (!reduceMotion) {
                    detailProgress.snapTo(0f)
                    detailProgress.animateTo(1f, popupMorphSpring())
                } else detailProgress.snapTo(1f)
            } else if (detailScreen == null && anim != null) {
                detailAnim = null
                heroTarget = null
                arcTarget = null
                vm.clearTravelSnapshot(anim.entityId)
            }
        }
        // Feedback round 5, item 1: panel-detail open effect (mirrors the detail effect). The
        // snapshot is captured from the tapped panel card's rect; no traveler flies on this
        // layer (the source lives in the sub-panel underneath), so no hero/arc target is needed.
        LaunchedEffect(panelDetailAnim?.entityId) {
            val anim = panelDetailAnim
            if (anim != null) {
                vm.captureTravelSnapshot(anim.entityId)
                // Feedback round 7: open resets the drag state.
                panelDetailDrag.reset()
                if (!reduceMotion) {
                    panelDetailProgress.snapTo(0f)
                    panelDetailProgress.animateTo(1f, popupMorphSpring())
                } else panelDetailProgress.snapTo(1f)
            } else {
                heroTarget = null
                arcTarget = null
            }
        }
        // Thermostat pop-out open effect (mirrors the sheet effect):
        // capture synchronously, animate progress 0 -> 1 on the bounds
        // spring (snapped under reduce-motion).
        if (vm.climatePopoutId != null && popoutAnim?.id != vm.climatePopoutId) {
            popoutAnim = PopoutAnim(vm.climatePopoutId!!, cardBounds[vm.climatePopoutId!!], cardColors[vm.climatePopoutId!!])
        }
        LaunchedEffect(vm.climatePopoutId) {
            val id = vm.climatePopoutId
            val anim = popoutAnim
            if (id != null && anim?.id == id) {
                vm.captureTravelSnapshot(id)
                // Feedback round 7: open resets the drag state.
                popoutDrag.reset()
                if (!reduceMotion) {
                    popoutProgress.snapTo(0f)
                    popoutProgress.animateTo(1f, popupMorphSpring())
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
            // Feedback round 7: same drag-aware prologue as the
            // other dismisses (see dismissDetail), and the settle
            // spring for the return flight.
            val fromDrag = cameraDrag.isDragging
            if (fromDrag) cameraDrag.settling = true
            cameraDialogExiting = true
            scope.launch {
                try {
                    if (fromDrag) {
                        cameraDrag.isDragging = false
                        cameraDrag.pulling = false
                    }
                    if (reduceMotion) cameraDialogProgress.snapTo(0f)
                    else cameraDialogProgress.animateTo(0f, settleSpring())
                } finally {
                    cameraDialogExiting = false
                    cameraDrag.settling = false
                    if (vm.cameraDialogId == id) vm.closeCameraDialog()
                }
            }
        }
        LaunchedEffect(vm.cameraDialogId) {
            val id = vm.cameraDialogId
            if (id != null && !cameraDialogExiting) {
                cameraDialogSource = cardBounds[id]
                // Feedback round 7: open resets the drag state.
                cameraDrag.reset()
                if (reduceMotion) cameraDialogProgress.snapTo(1f)
                else {
                    cameraDialogProgress.snapTo(0f)
                    cameraDialogProgress.animateTo(1f, popupMorphSpring())
                }
            }
        }
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        // Wide-layout detection lives here, above the Scaffold, so every
        // screen (and the overlays) reads the same value.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Feedback round 16, item 1: unfolded/tablet mode
            // ONLY once width >= height (and >= 600dp) — see
            // isWideLayout. The old width-only gate flipped to
            // tablet mode at ~50% split-screen, where the width
            // already crossed 600dp but the height was far larger.
            val wideLayout = isWideLayout(maxWidth.value, maxHeight.value)
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
                    homeListState = homeListState,
                    homeLeftScrollState = homeLeftScrollState,
                    homeRightScrollState = homeRightScrollState,
                    navigate = navigate,
                    goBack = goBack,
                    // Feedback round 5, item 1: panel cards open the stacked panel-detail
                    // layer instead of pushing EntityDetail (see openPanelDetail).
                    onPanelDetail = { entityId -> openPanelDetail(entityId) },
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
                    dynamicColor = dynamicColor,
                    onDynamicColor = {
                        dynamicColor = it
                        vm.settingsStore.dynamicColor = it
                    },
                    onSignOut = signOut,
                )
            }
            // Morph locals: cards report bounds here, and the active
            // morph source hides here. The scaffold blur from #49 is
            // GONE: the pill now exits the viewport during panels (see
            // the pill-exit below), which replaces the blur as the answer
            // to the panel problem, and the panel itself covers the page.
            val modalActive = sheetAnim != null || detailAnim != null || panelDetailAnim != null || popoutAnim != null
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
                // Feedback round 17, item 6: dashboard chrome
                // hides competing affordances (the Favorites
                // edit pencil) while any modal is open.
                LocalModalActive provides modalActive,
                LocalCardBounds provides cardBounds,
                LocalCardShapes provides cardShapes,
                LocalCardColors provides cardColors,
                // The camera panel's quadrant is a morph source too
                // (see CameraQuadrant): its rect anchors the panel
                // morph, and the opaque panel covers it while open,
                // so the snapshot never doubles the video.
                // Round 6: NO source card or tile ever hides for a
                // morph, on enter or on exit. hideWhenMorphSource /
                // hideBodyWhenMorphSource are gated on this id, so
                // null makes them no-ops everywhere: the grid card
                // stays rendered and stationary at all times (the
                // missing-Humidity close-frame defect was a hide
                // local blanking the slot). The panel surface
                // covers it with the reconciled container curves
                // (see MorphOverlay: fade in over the first 15%
                // of open from the source card's color, fade out
                // over the final 25% of close), so no pitch-black
                // box ever cuts across adjacent cards.
                LocalHiddenCardId provides null,
                LocalHeroBounds provides heroBounds,
                LocalArcBounds provides arcBounds,
                // Round 6 badge-only hide: while a panel with a
                // traveling header is open (sheet, detail, or
                // pop-out — NOT the camera panel, which has no
                // traveler), the SOURCE card's badge icon hides via
                // hideHeroWhenMorphSource, so the traveling badge
                // is the only badge on screen during flight. The
                // card's name, state, and body NEVER hide (see
                // LocalHiddenCardId above): the badge sites were
                // split from the header-row/text sites for exactly
                // this, so hiding the badge cannot blank a card.
                // At rest the id is null and every badge shows.
                // Feedback round 18, item 4: the traveling badge
                // is gone, so NO source badge ever hides — the
                // hidden id is always null on both postures
                // (matching CenteredPopup, which already nulled
                // it). Header badges render in place and fade
                // with the content curves instead.
                LocalHiddenHeroId provides null,
                // Visual-fidelity round §5: the closing flag and the
                // active morph's progress, for the source cards'
                // closing-aware badge crossfade (see
                // hideHeroWhenMorphSource): panels keep their own
                // inner LocalMorphProgress (MorphSurface provides
                // it), which shadows this root one inside panel
                // subtrees, so panel behavior is untouched. Cards
                // read the root lambda only inside graphicsLayer,
                // never during composition — no per-frame
                // recomposition cost.
                // Feedback round 11, item 1: closing is true ONLY
                // on an actual dismiss. Round 7 also set it while a
                // drag was in flight (the source badge crossfaded
                // in as the popup was pulled down) — but a drag no
                // longer moves the popup, so there is nothing to
                // crossfade toward mid-gesture. The camera dialog
                // is deliberately absent, as before: it never sets
                // a hidden id, so no badge reads this for it.
                LocalMorphClosing provides (sheetExiting || detailExiting || panelDetailExiting || popoutExiting),
                // Feedback round 18, item 5 (EXPLICIT REVERSAL of
                // round 11, item 1): the progress IS finger-
                // tracked while dragging — dragProgressFor reads
                // the drag state's dragProgress while isDragging,
                // the animatable otherwise. Panels keep their own
                // inner LocalMorphProgress (MorphSurface provides
                // it), which shadows this root one inside panel
                // subtrees, so panel behavior is untouched. Cards
                // read the root lambda only inside graphicsLayer,
                // never during composition — no per-frame
                // recomposition cost.
                LocalMorphProgress provides {
                    when {
                        sheetAnim != null -> sheetProgress.value
                        detailAnim != null -> detailProgress.value
                        popoutAnim != null -> popoutProgress.value
                        else -> 1f
                    }
                },
                // Shape picker state (2026-10-30): badges and card
                // outlines resolve through this; re-read from the
                // store whenever a picker save bumps the version.
                LocalShapePrefs provides ShapePrefs(
                    vm.settingsStore.globalBadgeShape,
                    vm.settingsStore.globalCardShape,
                    vm.settingsStore.badgeShapeOverrides(),
                    vm.settingsStore.cardShapeOverrides(),
                    // Feedback round 15, item 5: domain defaults
                    // resolve between per-entity overrides and
                    // the globals (see ShapePrefs).
                    vm.settingsStore.domainBadgeShapes(),
                    vm.settingsStore.domainCardShapes(),
                ),
                // Round 6 source-card rule (Google Home container
                // transform, reconciled curves — see MorphOverlay):
                // the card NEVER hides or fades, on enter or on
                // exit (LocalHiddenCardId is null above, and this
                // alpha is 1f). It stays rendered and stationary
                // at all times; the panel surface does the
                // covering, fading in over the first 15% of open
                // from the source card's color and out over the
                // final 25% of close. The one place a visible
                // double could still form is the close tail: the
                // traveler returns to the source badge rect while
                // the panel container fades (p < 0.25) and the
                // card badge shows through — so the traveler
                // fades with the container on close (see
                // the traveler's isClosing curve, see MorphSurface), landing as
                // a short crossfade onto the coincident card badge
                // instead of a doubled badge.
                LocalMorphCardAlpha provides 1f,
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
            // Feedback round 4, item 1: the unfolded root is a
            // full-screen Box, NOT a Row. Round 3's zIndex-only
            // fix was insufficient: the Scaffold itself sat beside
            // the rail in the Row, so its content area ended at
            // the rail boundary and sliding pages vanished at that
            // line instead of reaching the physical bezel. Now the
            // Scaffold spans the FULL width (with 84dp start
            // content padding — rest position unchanged) and the
            // rail floats on top at CenterStart, so transitions
            // slide all the way to the bezel underneath the rail,
            // with only the rail's own shape obscuring them.
            // The Box paints the scheme background across its
            // whole width, so rail zone and content stay one
            // continuous background (the visual-fidelity round's
            // white-strip fix, unchanged): the pill floats over it
            // on its own container + shadow, Koda-style. This is
            // not a recolor of the pill: the pill keeps its
            // surfaceContainerHigh container in both themes.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Round 6 posture key boundary: the rail is keyed on
            // the wide flag, so a fold/unfold switch unmounts the
            // old navigation chrome completely before the new
            // layout paints — no "Haven Good evening" header
            // ghosting, no rail + pill layer stacking. The
            // Scaffold below is deliberately NOT keyed (its
            // screens keep scroll state across postures).
            key(LocalWideLayout.current) {
                // Feedback round, item 2: the rail mounts whenever
                // wide, REGARDLESS of top-level. The old
                // isTopLevel gate unmounted the rail the moment a
                // card opened a detail panel (current stopped being
                // top-level), so the Scaffold snapped 84dp wider
                // on tap and the rail slid back in on close — the
                // "pill bar pushes to the left and then has like a
                // choppy return" on unfolded. A mounted rail during
                // panels is covered, never interactive: panel
                // overlays are later siblings above this Row.
                // Feedback round 2, item 7: selectedNavIndex
                // returns null off-tab, and a popup IS off-tab
                // (current stops being a tab screen while a
                // sheet/detail/popout anim runs), so the old code
                // passed null and the selected indicator vanished
                // the moment a popup opened. While any modal anim
                // is active, fall back to the last on-tab index;
                // other off-tab screens keep the old unhighlighted
                // behavior (null), and dialogs never change
                // current, so they never need the fallback.
                if (LocalWideLayout.current) {
                    // Feedback round 15, items 3–4: the rail shows
                    // the user's bar (hideable Scenes/Energy, up to
                    // two pinned tabs); Settings submenus resolve
                    // to Settings for the active highlight below.
                    val barItems = pillBarItems(vm.navShowScenes, vm.navShowEnergy, vm.userTabs.filter { it.pinned }, vm.roomBarItemsSnapshot())
                    // Stage E §9: the Home dot lights only for a real
                    // important alert (unlocked lock, open door/window,
                    // triggered alarm) — never a count, never for
                    // routine states. Collected here so only the rail
                    // recomposes on state changes, not the whole app.
                    val chromeStates by vm.states.collectAsState()
                    val badged = if (vm.badgeNav && hasImportantAlert(chromeStates.values.toList())) setOf(Screen.Today) else emptySet()
                    // Feedback round 15, item 4: Settings submenus
                    // resolve to Settings, so the rail highlight
                    // persists inside them (and the
                    // lastRailTabScreen fallback below then holds
                    // Settings too).
                    val onTabIndex = selectedNavIndex(navActiveScreen(current), barItems)
                    if (onTabIndex != null) lastRailTabScreen = barItems[onTabIndex].screen
                    val railSelectedIndex = if (sheetAnim != null || detailAnim != null || popoutAnim != null) {
                        onTabIndex ?: lastRailTabScreen?.let { selectedNavIndex(it, barItems) }
                    } else {
                        onTabIndex
                    }
                    // Feedback round 3, item 1: the rail draws
                    // AFTER (on top of) the Scaffold content, so
                    // tab pages sliding left pass UNDER the pill
                    // instead of over it — the pill's own shape is
                    // the only thing obscuring the transition.
                    // zIndex changes draw order only: resting
                    // layout (84dp rail + weight(1f) Scaffold) is
                    // byte-identical, and the rail keeps its
                    // enter slide and scrub behavior.
                    VerticalSidePill(
                        items = barItems,
                        selectedIndex = railSelectedIndex,
                        // Feedback round 18, item 6: the tap guard
                        // compares screens, not indices — on a
                        // sub-screen the highlighted tab's screen
                        // differs from the actual current screen,
                        // so tapping the highlighted icon must
                        // navigate back to it.
                        currentScreen = current,
                        onSelect = goTop,
                        badgedScreens = badged,
                        // Feedback round 4, item 1: the rail floats
                        // on top of the full-width Scaffold at
                        // CenterStart (zIndex keeps it above the
                        // sliding pages, as in round 3).
                        modifier = Modifier.align(Alignment.CenterStart).zIndex(1f).statusBarsPadding().navigationBarsPadding(),
                    )
                }
            }
            Scaffold(
                // Feedback round 4, item 1: full width on both
                // postures — the 84dp rail zone is start content
                // padding now (see contentPadding below), not a
                // Row weight splitting the root.
                modifier = Modifier.fillMaxSize(),
            // Pill-bar directive (visual-fidelity round): the
            // container is the scheme background, NOT transparent.
            // Stage C went transparent to kill the old default
            // band, but transparency left the bottom-bar zone
            // showing the activity window's white background in
            // dark mode — the same unpainted-zone defect as the
            // rail strip (see the Row background above).
            containerColor = MaterialTheme.colorScheme.background,
            // Screens consume the status-bar inset themselves
            // (statusBarsPadding on each root), so the Scaffold must not add
            // it again — otherwise headers sit a full inset too low.
            // NO bottomBar slot (expressive-menus round, item 1):
            // the folded pill lives in the overlay below, floating
            // over full-height content — never in a Scaffold slot
            // with a reserved band behind it. See the overlay.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            // Expressive-menus round, item 1 (2026-11-03 video
            // verdict, SUPERSEDES Stage C's reservation): the
            // folded pill must float over all elements, and when
            // it disappears no band may remain. Stage C reserved
            // the pill zone in content (bar + 8dp + 20dp + nav
            // inset) so the pill never covered anything — but on
            // device that reservation reads as a big white box
            // behind the pill in light theme, and as a weird
            // blank box cutting into the tiles once the pill
            // scrolls away, because scaffold padding is static
            // while the pill exits. The user's explicit verdict
            // overrides Stage C's no-cover rule: content now runs
            // full-height beneath the pill (the pill floats over
            // it, Koda-style), and only the navigation-bars
            // inset is kept as bottom clearance on folded
            // top-level screens, so list ends clear the gesture
            // bar. The pill still auto-hides on scroll-down, so
            // content is fully readable while scrolling; at rest
            // it may overlap the last row, exactly like any
            // floating bar. The Stage C list-end sweep (plain
            // 16dp ends) is untouched.
            val layoutDirection = LocalLayoutDirection.current
            // Pill-bar directive (visual-fidelity round): the
            // unfolded remnant of the backing rectangle is
            // CLEARANCE, not a surface. Top-level unfolded
            // screens keep a small invisible bottom reserve —
            // 20dp daylight plus the navigation-bars inset,
            // mirroring the folded pill's float — so list ends
            // never sit under chrome. Folded keeps only the
            // navigation-bars inset (see below): the pill
            // itself floats over content, no zone reserved.
            val density = LocalDensity.current
            val navBottom = with(density) {
                WindowInsets.navigationBars.getBottom(density).toDp()
            }
            // Follow-up A (2026-11-03): the unfolded clearance is
            // gated on WIDE ONLY, not on isTopLevel. The old gate
            // dropped the 20dp + nav inset the moment a detail or
            // panel screen left the top level, so the underlying
            // page — VISIBLE behind the unfolded centered popup —
            // jumped 20dp on open and back on close. With the
            // wide-only gate the page never moves on panel
            // open/close, so exiting restores the exact prior view.
            // Folded keeps its gate: folded panels are opaque
            // full-screen, so the shift underneath is invisible.
            // Feedback round 16, item 8: the 20dp "daylight"
            // is gone — unfolded has no bottom pill to mirror,
            // so the reserve only ever pushed content up and
            // left the last row short of the bottom edge. The
            // gesture-bar inset stays, so content still clears
            // the system bar.
            val unfoldedBottomClearance =
                if (LocalWideLayout.current) navBottom else 0.dp
            // Feedback round 19, item 3: folded content ALWAYS
            // reserves the navigation-bars inset (matching the
            // unfolded clearance rule) — the old isTopLevel gate
            // dropped it the moment a detail panel left the top
            // level, so opening a panel at the bottom of the Home
            // list changed contentPadding and jumped the scroll
            // offset by exactly the inset.
            val foldedNavClearance =
                if (shouldApplyFoldedNavClearance(LocalWideLayout.current)) navBottom else 0.dp
            val contentPadding = PaddingValues(
                // Feedback round 19, item 1: the outer Box no
                // longer reserves the 84dp rail zone — the
                // transition container spans the full window
                // width, so sliding pages travel all the way to
                // the physical left bezel underneath the rail.
                // The 84dp inset now lives INSIDE AnimatedContent
                // (see the wrapper below): resting content still
                // sits right of the rail, exactly as before, and
                // every screen's scheme background still paints
                // full-bleed (this Box paints it across the full
                // width behind the inset content, and every screen
                // root background is the same scheme color).
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = padding.calculateBottomPadding() + unfoldedBottomClearance + foldedNavClearance,
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
                    is Screen.UpdatesPanel, is Screen.HomeStatusPanel ->
                        backstack.getOrNull(backstack.lastIndex - 1) ?: Screen.Today
                    else -> current
                }
                // Round 4: top-level tab switches use a shared-axis
                // transition (horizontal slide + fade) instead of
                // an instant swap; panel morphs are untouched (they
                // already use springBounds). Under reduce-motion
                // the switch stays instant.
                // Follow-up B (2026-11-03): Koda-directional tab
                // slides. Rank = index in the locked 4-tab bar
                // order (Home, Scenes, Energy, Settings — Koda's
                // visualTabOrder rule, Haven's bar order). When
                // BOTH the outgoing and incoming screens are tabs,
                // the slide direction follows the rank delta.
                // Feedback round 3, item 1 (video ba): the travel
                // is FULL in both directions — the outgoing page
                // slides ALL the way off-screen (not Koda's
                // one-third), and on unfolded the leftward travel
                // adds the 84dp rail width so the page clears the
                // left edge too. The rail draws on top (zIndex,
                // see the VerticalSidePill call site).
                // Resting page positioning is unchanged (content
                // still starts right of the rail). Tab tap
                // while a panel is open: goTop clears the panel
                // INSTANTLY first (pre-existing), then the
                // directional slide runs — no panel/tab animation
                // collision, because the panel is gone before the
                // slide starts. Details keep their own mechanisms
                // (unfolded centered popup, folded full-screen
                // morph); this spec touches tab switches only.
                val reduceMotionTop = LocalReduceMotion.current
                // Feedback round 3, item 1: leftward tab travel on
                // unfolded must clear the 84dp rail as well as the
                // content width, so the page slides fully off the
                // left screen edge (not just out of the content
                // area). Folded has no rail, so the clearance is 0.
                val railTransitionPx = with(LocalDensity.current) {
                    if (LocalWideLayout.current) 84.dp.roundToPx() else 0
                }
                // Feedback round 19, items 1 + 3: AnimatedContent
                // is ALWAYS mounted now (targetState = base).
                // Round 4, item 4's direct branch — content(base)
                // while a detail overlay was open — is DELETED:
                // swapping parents disposed TodayScreen, which
                // reset the room filter and flashed the page
                // underneath the morph. With base stable across
                // a morph (detail screens substitute their
                // backstack predecessor), no transition runs
                // under the overlay; the page simply stays.
                // Reduce-motion keeps an instant switch, but
                // through the same AnimatedContent (None
                // transitions), so composition identity never
                // changes with the motion setting either.
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = base,
                    transitionSpec = {
                        if (reduceMotionTop) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val barScreens = pillBarItems(vm.navShowScenes, vm.navShowEnergy, vm.userTabs.filter { it.pinned }, vm.roomBarItemsSnapshot()).map { it.screen }
                            // Feedback round 17, item 6: NO fades.
                            // The old slide + fade let the outgoing
                            // and incoming pages show through each
                            // other mid-transition (readable ghost
                            // text — Settings over Manage home page
                            // on the 2026-11-04 runs). Opaque slides
                            // keep the directional character (full
                            // travel for tabs and sub-screens alike,
                            // round 18, item 6) with no see-through
                            // overlap, and the same fade removal
                            // ends the back-navigation page fade
                            // that flashed cards semi-transparent
                            // on return to Home. The fade removal
                            // is the ghosting fix, and it STAYS.
                            // Feedback round 19, item 1, SUPERSEDES
                            // round 17's clip = true for exactly
                            // the clipping symptom: with the
                            // container spanning the full window
                            // width and clip = false, sliding pages
                            // travel all the way to the physical
                            // left bezel underneath the rail
                            // instead of stopping at the old
                            // content boundary — the 2026-11-04
                            // video's "clipped by that imaginary
                            // bar" defect. The rail still draws on
                            // top (zIndex), so pages slide UNDER
                            // it, never over it.
                            // Feedback round 19, item 2: switches
                            // involving a non-tab screen use the
                            // tracked push/pop direction instead
                            // of the old fixed forward slide — a
                            // pop slides the sub-page out to the
                            // right and the parent in from the
                            // left, the mirror of a push.
                            when (val direction = tabSlideDirection(barScreens, initialState, targetState)) {
                                1, -1 -> (slideInHorizontally(initialOffsetX = { tabSlideOffsets(direction, it, railTransitionPx).first }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { tabSlideOffsets(direction, it, railTransitionPx).second }))
                                else -> {
                                    val dir = nonTabSlideDirection(navDirection < 0)
                                    (slideInHorizontally(initialOffsetX = { tabSlideOffsets(dir, it, railTransitionPx).first }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { tabSlideOffsets(dir, it, railTransitionPx).second }))
                                }
                            }
                        }.using(SizeTransform(clip = false))
                    },
                    label = "topLevelScreen",
                ) { screen ->
                    // Feedback round 19, item 1: the 84dp rail
                    // inset, applied INSIDE the transition
                    // container so resting content sits right of
                    // the rail while the sliding page surface
                    // travels bezel to bezel. Screens keep their
                    // own horizontal padding on top of this.
                    Box(Modifier.fillMaxSize().padding(start = if (LocalWideLayout.current) 84.dp else 0.dp)) {
                        content(screen)
                    }
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
            } // Box: full-width scaffold + floating side pill (unfolded) / scaffold alone (folded)
            // Folded pill overlay (expressive-menus round, item 1):
            // the pill floats OVER full-height content — it is not
            // a Scaffold bottomBar and reserves no zone. The
            // wrapper carries the old bottomBar geometry (nav
            // inset, 20dp float, 8dp daylight) and the pill-exit
            // animation (fade + 96dp slide on scroll-away or
            // modal); the pill itself is unchanged (same PillNav,
            // same scrub, same selection). The wrapper paints
            // nothing and takes no taps outside the pill, so
            // touches fall through to content below. It sits
            // BELOW the panel overlays that follow, so an open
            // panel covers it exactly as before (and pillExit
            // already hides it during any modal). Keyed on the
            // wide flag like the rail, so posture switches
            // unmount it cleanly.
            key(LocalWideLayout.current) {
                if (!LocalWideLayout.current && isTopLevel(current)) {
                    val barItems = pillBarItems(vm.navShowScenes, vm.navShowEnergy, vm.userTabs.filter { it.pinned }, vm.roomBarItemsSnapshot())
                    val selectedIndex = selectedNavIndex(navActiveScreen(current), barItems)
                    // Stage E §9: same Home dot as the rail.
                    // Collected here so only the bar recomposes on
                    // state changes, not the whole app.
                    val chromeStates by vm.states.collectAsState()
                    val badged = if (vm.badgeNav && hasImportantAlert(chromeStates.values.toList())) setOf(Screen.Today) else emptySet()
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier.fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = 20.dp, top = 8.dp)
                                .graphicsLayer {
                                    alpha = 1f - pillExit
                                    translationY = 96.dp.toPx() * pillExit
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            PillNav(
                                items = barItems,
                                selectedIndex = selectedIndex,
                                // Feedback round 18, item 6: screen
                                // tap guard (see VerticalSidePill).
                                currentScreen = current,
                                onSelect = goTop,
                                geometry = navPillGeometry(vm.navStyle),
                                badgedScreens = badged,
                            )
                        }
                    }
                }
            }
            // Sheet panel overlay: FULL-SCREEN PANEL (2026-11-01
            // panels round) — SUPERSEDES the 2026-10-30 centered
            // dialog. The sheet's content is unchanged; only its
            // container morphs from the source card rect to
            // full-screen (see PanelOverlay). No scrim: the opaque
            // panel covers the page at rest. The badge still
            // travels into the panel's header (see
            // MorphSurface's traveler slot) while the panel morphs — both, not
            // one or the other. Feedback round 7: drag-to-dismiss
            // (Koda now-playing port) and Back dismiss; the back
            // arrow is gone (see PanelOverlay/CenteredPopup).
            if (sheetAnim != null) {
                val anim = sheetAnim!!
                BackHandler { dismissSheet() }
                // Feedback round 7: drag handle for this surface
                // (progress lambda below is drag-aware).
                val sheetHandle = popupDragHandle(sheetDrag, sheetProgress, { sheetExiting }, { vm.captureTravelSnapshot(anim.id) }, { dismissSheet() })
                // Feedback round, item 3: unfolded gets the
                // centered popup; folded keeps the full-screen
                // morph + traveler below, byte for byte.
                if (LocalWideLayout.current) {
                    CenteredPopup(
                        progress = dragProgressFor(sheetProgress, sheetDrag),
                        source = anim.source ?: cardBounds[anim.id],
                        overlayOrigin = overlayOrigin,
                        fromBackground = anim.sourceColor,
                        onDismiss = { dismissSheet() },
                        dragHandle = sheetHandle,
                        isClosing = sheetExiting && !sheetDrag.settling,
                        onPanelPlaced = { sheetPanelBounds = it },
                        landingShape = cardShapes[anim.id] ?: LocalShapePrefs.current.cardShape(anim.id),
                    ) {
                        EntityPopupSheetContent(
                            vm, navigate, anim.id, panelBounds = sheetPanelBounds,
                            onDetails = { dismissSheetInstant() },
                        )
                    }
                } else {
                // Round 6: no measurement frame — the panel is
                // composed ONCE, at target geometry, and the live
                // header reports the travel target from its
                // untransformed layout bounds (PanelOverlay
                // provides the hero/arc targets around the live
                // content). Progress is a lambda: nothing in this
                // block reads the Animatable during composition.
                PanelOverlay(
                    progress = dragProgressFor(sheetProgress, sheetDrag),
                    source = anim.source ?: cardBounds[anim.id],
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    sourceCardColor = anim.sourceColor ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    // Feedback round 7: settling keeps the open
                    // curves through a drag-initiated dismiss.
                    isClosing = sheetExiting && !sheetDrag.settling,
                    dragHandle = sheetHandle,
                    heroTargetReporter = { r -> heroTarget = anim.id to r },
                    arcTargetReporter = { r -> arcTarget = anim.id to r },
                    onPanelPlaced = { sheetPanelBounds = it },
                    landingShape = cardShapes[anim.id] ?: LocalShapePrefs.current.cardShape(anim.id),
                ) {
                    EntityPopupSheetContent(
                        vm, navigate, anim.id, panelBounds = sheetPanelBounds,
                        onDetails = { dismissSheetInstant() },
                    )
                }
                // Traveling arc, above the panel (see TravelingArc).
                TravelingArc(
                    vm, anim.id, arcBounds[anim.id],
                    arcTarget?.takeIf { it.first == anim.id }?.second,
                    progress = dragProgressFor(sheetProgress, sheetDrag), overlayOrigin,
                    isClosing = sheetExiting && !sheetDrag.settling,
                )
                } // else: folded full-screen morph (feedback item 3)
            }
            // Detail panel overlay: FULL-SCREEN PANEL (2026-11-01
            // panels round) — SUPERSEDES the 2026-10-30 centered
            // dialog, exactly like the sheets above. The detail
            // content is unchanged; the one-frame measurement pass
            // reports only the header/arc target rects, never a
            // panel size — the panel is full-screen. Back and the
            // drag are the smooth reverse of enter
            // (see dismissDetail); only the sheet -> detail
            // handoff clears instantly.
            if (detailAnim != null) {
                val anim = detailAnim!!
                BackHandler { dismissDetail() }
                // Feedback round 7: drag handle for this surface.
                val detailHandle = popupDragHandle(detailDrag, detailProgress, { detailExiting }, { vm.captureTravelSnapshot(anim.entityId) }, { dismissDetail() })
                // Feedback round 4, item 4: when this detail opened
                // from a sub-panel or another detail, the previous
                // backstack entry is an overlay screen — the source
                // card lives in the overlay this detail replaces,
                // not on the root dashboard, so its map rects are
                // the outgoing overlay's (or the base page's once
                // it lays out), never the tapped card's. No
                // traveler flies in that case: it would land on a
                // position matching no visible card (the stray
                // badge on Home), and the destination header badge
                // renders from frame 0 instead (LocalSuppressTraveler).
                // Details from tabs and ordinary pages keep the
                // traveler: their source card stays laid out in the
                // base at the same rect.
                val detailPrev = backstack.getOrNull(backstack.lastIndex - 1)
                val detailFromPanel = detailPrev is Screen.EntityDetail ||
                    detailPrev is Screen.VacuumPanel || detailPrev is Screen.MediaPanel ||
                    detailPrev is Screen.SecurityPanel || detailPrev is Screen.GreenhousePanel ||
                    detailPrev is Screen.UpdatesPanel || detailPrev is Screen.HomeStatusPanel
                // The morph source is the rect captured at open,
                // NOT the live map: once the base page lays out
                // (item 4's instant base below), its cards
                // overwrite the map with base-page rects, and a
                // live read would jump the morph origin mid-flight.
                // The live map stays the fallback for cards that
                // never reported bounds.
                val detailSource = anim.source ?: cardBounds[anim.entityId]
                // Feedback round, item 3: unfolded gets the
                // centered popup; folded keeps the full-screen
                // morph + traveler below, byte for byte.
                if (LocalWideLayout.current) {
                    CenteredPopup(
                        progress = dragProgressFor(detailProgress, detailDrag),
                        source = detailSource,
                        overlayOrigin = overlayOrigin,
                        fromBackground = anim.sourceColor,
                        onDismiss = { dismissDetail() },
                        dragHandle = detailHandle,
                        isClosing = detailExiting && !detailDrag.settling,
                        landingShape = cardShapes[anim.entityId] ?: LocalShapePrefs.current.cardShape(anim.entityId),
                    ) {
                        CompositionLocalProvider(LocalSuppressTraveler provides detailFromPanel) {
                            content(anim.screen)
                        }
                    }
                } else {
                // Round 4: the thermostat detail special-case is
                // gone with Screen.Thermostat — every detail panel
                // content is dialog-owned (plain column, no own
                // scroll), so the panel's scroll wrapper owns
                // scrolling for all of them. The #92 inDialog crash
                // fix for the deleted route is moot. Round 6: no
                // measurement frame here either (see the sheet
                // block); progress is a lambda.
                PanelOverlay(
                    progress = dragProgressFor(detailProgress, detailDrag),
                    source = detailSource,
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    sourceCardColor = anim.sourceColor ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    isClosing = detailExiting && !detailDrag.settling,
                    dragHandle = detailHandle,
                    heroTargetReporter = { r -> heroTarget = anim.entityId to r },
                    arcTargetReporter = { r -> arcTarget = anim.entityId to r },
                    landingShape = cardShapes[anim.entityId] ?: LocalShapePrefs.current.cardShape(anim.entityId),
                ) {
                    CompositionLocalProvider(LocalSuppressTraveler provides detailFromPanel) {
                        content(anim.screen)
                    }
                }
                TravelingArc(
                    vm, anim.entityId, arcBounds[anim.entityId],
                    arcTarget?.takeIf { it.first == anim.entityId }?.second,
                    progress = dragProgressFor(detailProgress, detailDrag), overlayOrigin,
                    isClosing = detailExiting && !detailDrag.settling,
                )
                } // else: folded full-screen morph (feedback item 3)
            }
            // Feedback round 5, item 1: the stacked panel-detail layer. Composed AFTER the detail
            // block, so it draws above the still-visible panel popup; it renders DetailPageScreen
            // directly (never via content(EntityDetail)), so the backstack stays
            // [Today, HomeStatusPanel] and the base page never switches to the panel page.
            // The source is the rect captured at tap time (the panel card's rect); the live map
            // is only the fallback for cards that never reported bounds. No traveler flies here:
            // the source lives in the sub-panel underneath, so the destination header badge
            // renders from frame 0 (LocalSuppressTraveler = true, same rule as round 4's
            // detailFromPanel gate). No hero/arc targets are reported on this layer — with no
            // traveler, nothing consumes them, and reporting would clobber the panel's targets.
            // Posture mirrors the detail block: centered popup unfolded, full-screen morph folded.
            if (panelDetailAnim != null) {
                val anim = panelDetailAnim!!
                BackHandler { dismissPanelDetail() }
                // Feedback round 7: the panel-detail layer uses the
                // same wrappers, so it gets the same drag wiring
                // (the spec listed sheet/detail/popout/camera, but
                // leaving this surface undraggable would be an
                // inconsistent popup).
                val panelDetailHandle = popupDragHandle(panelDetailDrag, panelDetailProgress, { panelDetailExiting }, { vm.captureTravelSnapshot(anim.entityId) }, { dismissPanelDetail() })
                val panelDetailSource = anim.source ?: cardBounds[anim.entityId]
                if (LocalWideLayout.current) {
                    CenteredPopup(
                        progress = dragProgressFor(panelDetailProgress, panelDetailDrag),
                        source = panelDetailSource,
                        overlayOrigin = overlayOrigin,
                        fromBackground = anim.sourceColor,
                        onDismiss = { dismissPanelDetail() },
                        dragHandle = panelDetailHandle,
                        isClosing = panelDetailExiting && !panelDetailDrag.settling,
                        landingShape = cardShapes[anim.entityId] ?: LocalShapePrefs.current.cardShape(anim.entityId),
                    ) {
                        CompositionLocalProvider(LocalSuppressTraveler provides true) {
                            DetailPageScreen(vm, anim.entityId, onBack = { dismissPanelDetail() })
                        }
                    }
                } else {
                PanelOverlay(
                    progress = dragProgressFor(panelDetailProgress, panelDetailDrag),
                    source = panelDetailSource,
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    sourceCardColor = anim.sourceColor ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    isClosing = panelDetailExiting && !panelDetailDrag.settling,
                    dragHandle = panelDetailHandle,
                    landingShape = cardShapes[anim.entityId] ?: LocalShapePrefs.current.cardShape(anim.entityId),
                ) {
                    CompositionLocalProvider(LocalSuppressTraveler provides true) {
                        DetailPageScreen(vm, anim.entityId, onBack = { dismissPanelDetail() })
                    }
                }
                } // else: folded full-screen morph (mirrors the detail block)
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
                // Feedback round 7: drag handle for this surface.
                val popoutHandle = popupDragHandle(popoutDrag, popoutProgress, { popoutExiting }, { vm.captureTravelSnapshot(anim.id) }, { dismissPopout() })
                // Feedback round, item 3: unfolded gets the
                // centered popup; folded keeps the full-screen
                // morph + traveler below, byte for byte.
                if (LocalWideLayout.current) {
                    CenteredPopup(
                        progress = dragProgressFor(popoutProgress, popoutDrag),
                        source = anim.source ?: cardBounds[anim.id],
                        overlayOrigin = overlayOrigin,
                        fromBackground = anim.sourceColor,
                        onDismiss = { dismissPopout() },
                        dragHandle = popoutHandle,
                        isClosing = popoutExiting && !popoutDrag.settling,
                        landingShape = cardShapes[anim.id] ?: LocalShapePrefs.current.cardShape(anim.id),
                    ) {
                        ThermostatPopoutContent(
                            vm, anim.id,
                            onClose = { dismissPopout() },
                        )
                    }
                } else {
                // Round 6: no measurement frame here either (see
                // the sheet block); progress is a lambda.
                PanelOverlay(
                    progress = dragProgressFor(popoutProgress, popoutDrag),
                    source = anim.source ?: cardBounds[anim.id],
                    overlayOrigin = overlayOrigin,
                    panelBackground = MaterialTheme.colorScheme.background,
                    sourceCardColor = anim.sourceColor ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    isClosing = popoutExiting && !popoutDrag.settling,
                    dragHandle = popoutHandle,
                    heroTargetReporter = { r -> heroTarget = anim.id to r },
                    arcTargetReporter = { r -> arcTarget = anim.id to r },
                    landingShape = cardShapes[anim.id] ?: LocalShapePrefs.current.cardShape(anim.id),
                ) {
                    ThermostatPopoutContent(
                        vm, anim.id,
                        onClose = { dismissPopout() },
                    )
                }
                } // else: folded full-screen morph (feedback item 3)
            }
            // Camera live-feed panel (2026-11-01 panels round): the
            // Home quadrant hero's tap target, formerly a centered
            // 16:9 dialog — now a full-screen panel like every other
            // device surface, so no centered device dialog remains.
            // The MJPEG body fills the panel; Ring uses the #65
            // proxy path through the same body and stays labeled
            // as a recording, never live. Feedback round 7: no
            // back bar and no header button — Back and the drag
            // dismiss (see dismissCameraDialog). This block moved
            // inside the overlay root so the morph shares its
            // origin with the other panels; the camera fullscreen
            // below still draws above it.
            if (vm.cameraDialogId != null) {
                val camId = vm.cameraDialogId!!
                BackHandler { dismissCameraDialog() }
                // Round 7: no composition-time Animatable read here — the
                // old `camP = cameraDialogProgress.value` recomposed this
                // whole block every animation frame. Progress travels as
                // a lambda and is read only inside graphicsLayer blocks.
                val camStates by vm.states.collectAsState()
                val camEntity = camStates[camId]
                // Feedback round 7: drag handle for this surface
                // (no travel snapshot: the camera panel has no
                // traveler, and its landing rect is the
                // captured-at-open quadrant rect).
                val cameraHandle = popupDragHandle(cameraDrag, cameraDialogProgress, { cameraDialogExiting }, { }, { dismissCameraDialog() })
                PanelOverlay(
                    progress = dragProgressFor(cameraDialogProgress, cameraDrag),
                    // Captured-at-open quadrant rect (live map as
                    // first-frame fallback): never the sourceless
                    // fallback, so the close lands on the quadrant.
                    source = cameraDialogSource ?: cardBounds[camId],
                    overlayOrigin = overlayOrigin,
                    panelBackground = Color.Black,
                    scrollContent = false,
                    isClosing = cameraDialogExiting && !cameraDrag.settling,
                    dragHandle = cameraHandle,
                    landingShape = cardShapes[camId] ?: LocalShapePrefs.current.cardShape(camId),
                ) {
                    // Front Door live probe (Round 2): the Front
                    // Door panel alone gets the Live View / Event
                    // History toggle; every other camera keeps the
                    // exact body below, byte for byte.
                    if (camId == FRONT_DOOR_LIVE_VIEW_ID) {
                        FrontDoorCameraPanel(vm, camId, { cameraDialogProgress.value.coerceIn(0f, 1f) }, onClose = { dismissCameraDialog() })
                    } else {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        // The hero panel's MJPEG body is a recording
                        // for Ring (same verdict as fullscreen) and
                        // live for native cameras; `recording` only
                        // switches the connecting/error copy.
                        // Feedback round, item 4: HLS-first for
                        // stream-capable non-Ring cameras; Ring
                        // keeps this MJPEG body via the route.
                        CameraLiveBody(vm, camId, { cameraDialogProgress.value.coerceIn(0f, 1f) }, recording = camEntity?.let { isRingCamera(it) } == true, onClose = { dismissCameraDialog() })
                        Row(
                            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Feedback round 7: no header button —
                            // the back arrow is removed with every
                            // other popup chrome button. BackHandler
                            // + the drag dismiss, like every panel.
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
                    SheetPageContent(vm.pageOrigin, { cameraExitProgress.value }, background = Color.Black) {
                        CameraFullscreen(vm, requestCameraClose, { cameraExitProgress.value })
                    }
                } else {
                    SheetPageContent(vm.pageOrigin, { cameraEnterProgress.value }, background = Color.Black) {
                        CameraFullscreen(vm, requestCameraClose, { cameraEnterProgress.value })
                    }
                }
            }
            // Shape picker dialog (2026-10-30): above every other
            // overlay; opened from Settings (global default) or an
            // entity sheet (per-entity override).
            if (vm.shapePickerTarget != null) {
                ShapePickerDialog(vm, vm.shapePickerTarget!!, onClose = { vm.closeShapePicker() })
            }
            // Feedback round 15, item 5: Domain defaults dialog,
            // same overlay layer as the shape picker.
            if (vm.domainDefaultsOpen) {
                DomainDefaultsDialog(vm, onClose = { vm.closeDomainDefaults() })
            }
            // Customization round, item 2: Card settings sheet,
            // above every other overlay like the shape picker.
            if (vm.cardSettingsTarget != null) {
                CardSettingsSheet(vm, vm.cardSettingsTarget!!, onDismiss = { vm.closeCardSettings() })
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
    // Feedback round 9, item 2: Home's scroll states, hoisted in
    // HavenApp above the AnimatedContent/direct branch swap so
    // Home keeps its offset across detail returns. Before the
    // lambda params (round 5 lesson: never append after a lambda).
    homeListState: LazyListState,
    homeLeftScrollState: ScrollState,
    homeRightScrollState: ScrollState,
    navigate: (Screen) -> Unit,
    goBack: () -> Unit,
    // Feedback round 5, item 1: panel cards open the stacked panel-detail layer. Default is a
    // no-op so existing call sites are unaffected; the HavenApp content lambda passes the real
    // opener, and only the HomeStatusPanel branch forwards it.
    onPanelDetail: (String) -> Unit = {},
    themeMode: String,
    reduceMotion: Boolean,
    onThemeMode: (String) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    dynamicColor: Boolean,
    onDynamicColor: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    when (screen) {
        Screen.Today -> TodayScreen(vm, homeListState, homeLeftScrollState, homeRightScrollState, navigate, onSignInExpired = onSignOut)
        Screen.Dashboards -> DashboardsScreen(vm, navigate)
        Screen.Areas -> AreasScreen(vm, navigate)
        Screen.Search -> SearchScreen(vm, navigate)
        Screen.UserTabs -> UserTabsScreen(vm, navigate)
        Screen.Settings -> SettingsScreen(
            vm = vm,
            themeMode = themeMode,
            reduceMotion = reduceMotion,
            onThemeMode = onThemeMode,
            onReduceMotion = onReduceMotion,
            dynamicColor = dynamicColor,
            onDynamicColor = onDynamicColor,
            onSignOut = onSignOut,
            navigate = navigate,
        )
        is Screen.UserTab -> UserTabScreen(vm, screen.tabId, navigate)
        // Stage F destinations (first-class bar items, brief §4).
        Screen.Scenes -> ScenesScreen(vm)
        Screen.Energy -> EnergyScreen(vm)
        Screen.RoomsTabs -> RoomsTabsScreen(vm, goBack, navigate)
        Screen.HomeEditor -> HomeEditorScreen(vm, goBack)
        is Screen.RoomTab -> TodayScreen(vm, homeListState, homeLeftScrollState, homeRightScrollState, navigate, onSignInExpired = onSignOut)
        // Detail pages replaced the old generic detail screen: per-domain
        // pages (climate still routes to its thermostat page inside).
        is Screen.EntityDetail -> DetailPageScreen(vm, screen.entityId, goBack)
        // Round 3 panels: full-screen morph panels, same machinery.
        is Screen.VacuumPanel -> VacuumPanelScreen(vm, goBack)
        is Screen.MediaPanel -> MediaPanelScreen(vm, screen.entityId, goBack)
        is Screen.SecurityPanel -> SecurityPanelScreen(vm, goBack)
        is Screen.GreenhousePanel -> GreenhousePanelScreen(vm, goBack)
        is Screen.UpdatesPanel -> UpdatesPanelScreen(vm, goBack)
        is Screen.HomeStatusPanel -> HomeStatusPanelScreen(vm, screen.categoryKey, navigate, onPanelDetail)
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
    // Feedback round 16, item 2: the sheet's top edge buffer
    // matches the thermostat popout's 16dp (was a 12dp spacer).
    // Sides were already 16dp; the bottom stays 24dp — content
    // still scrolls up from the very bottom.
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp).padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
            if (entity == null) {
                Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val accent = accentFor(entity.entityId)
                // Customization round, item 2: the sheet header
                // honors the same overrides as every card, plus the
                // edit pencil beside the title.
                val sheetCustom = rememberCardCustom(vm)
                val sheetTitle = com.garrietta.haven.data.customTitleFor(
                    sheetCustom.titles, entity.entityId,
                    if (entity.domain == "sensor") sensorDisplayName(entity) else displayCameraName(entity),
                )
                // Feedback round 11, item 3: the one icon path.
                val sheetIcon = entityIcon(sheetCustom.iconKeys, entity)
                // Feedback round 4, item 2: the sheet header badge
                // carries the card's pair, like every card.
                // Feedback round 17, item 1: the TILE's exact
                // pair (tileBadgePair), not the preset-only
                // override — the old detailBadgeOverride
                // returned null with no preset, so headers fell
                // back to the hash accent and eufy read YELLOW
                // in the sheet while its tile read muted green.
                val sheetBadgeOverride = tileBadgePair(
                    sheetCustom.tilePresets, sheetCustom.domainTilePresets, entity,
                    isDarkBackdrop(MaterialTheme.colorScheme.background),
                    MaterialTheme.colorScheme, accent,
                )
                // Feedback round 5, item 4: the sheet's controls carry
                // the assigned tile color itself (ungated — see
                // detailPageAccent: the video-3 garage/vacuum sheets
                // are closed/docked entities, and the user expects
                // the chosen color there regardless of state).
                val sheetAccent = detailPageAccent(
                    sheetCustom.tilePresets, entity,
                    isDarkBackdrop(MaterialTheme.colorScheme.background),
                    sheetCustom.domainTilePresets,
                )
                // Feedback round 18, item 5: header drag-to-
                // dismiss (see LocalPopupDragHandle); null on
                // plain detail pages, so no detector there.
                val sheetDragHandle = LocalPopupDragHandle.current
                val sheetDragRangePx = LocalPopupDragRangePx.current
                Row(
                    Modifier.fillMaxWidth()
                        .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) }
                        .then(
                            if (sheetDragHandle != null) {
                                Modifier.popupDragDetector(
                                    sheetDragHandle.state,
                                    { dy -> sheetDragHandle.onDelta(dy, sheetDragRangePx()) },
                                    { v -> sheetDragHandle.onRelease(v, sheetDragRangePx()) },
                                )
                            } else {
                                Modifier
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Feedback round 18, item 4: the badge renders
                    // in place and fades with the content curves
                    // (see morphHeaderBadgeAlpha) — no traveler.
                    DomainBadge(
                        entity,
                        sheetIcon,
                        accent,
                        modifier = Modifier.size(48.dp).morphHeaderBadgeAlpha(),
                        badgeOverride = sheetBadgeOverride,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        DialogTitleText(sheetTitle, Modifier.fillMaxWidth())
                        Text(entity.dialogHeaderState(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CardSettingsPencil(onClick = { vm.openCardSettings(entity.entityId) })
                }
                Spacer(Modifier.height(12.dp))
                // Plain control, not the card's header bar: the header
                // is a dead control here (this IS the detail surface),
                // so the card renders without it and the name still shows.
                // Nested: the sheet panel is already a container.
                EntityControlCard(entity, vm, onDetail = { }, showHeader = false, nested = true, pageAccent = sheetAccent, pageBadge = sheetBadgeOverride)
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
                // Customization round, item 2: the one Card
                // settings surface (title, icon, badge shape, tile
                // color), reachable from every entity's menu.
                Spacer(Modifier.height(8.dp))
                KodaMenuRow(
                    badgeKey = "card-settings-${entity.entityId}",
                    badgeKind = shapeForDomain(entity.domain),
                    icon = domainIcon(entity),
                    title = "Card settings",
                    subtitle = "Title, icon, badge shape, tile color",
                    onClick = { vm.openCardSettings(entity.entityId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * The full-screen panel overlay, Round 6 (120Hz pipeline +
 * reconciled curves — SUPERSEDES the Round 5 opaque-cover rule
 * and the measurement frame, both deleted this round):
 * every device detail opens as a panel morphing from its source
 * card rect to full-screen, on BOTH folded and unfolded postures.
 * No centered device dialog remains; the only dialog left is the
 * shape picker (see ShapePickerDialog, intentionally preserved).
 * - Container: NO scrim — the panel covers the page at rest. A
 *   touch interceptor fills the screen behind the morphing rect
 *   so taps outside the still-small panel mid-flight never reach
 *   the page below; it takes no action itself (dismiss is Back /
 *   the back arrow / the panel's own Close or badge, never an
 *   outside tap).
 * - Morph: the panel rect lerps from the source card rect (or the
 *   graceful fallback rect, target shifted 80dp down, when the
 *   card never reported bounds) to the full overlay rect on the
 *   caller-supplied morph [progress] LAMBDA — the SAME Animatable
 *   + springBounds progress that drove the old dialog, read ONLY
 *   inside MorphSurface's graphicsLayer/draw blocks, so an appear
 *   interrupted by a dismiss reverses cleanly mid-flight instead
 *   of jumping, and no morph frame costs a Compose Measure/Layout
 *   pass. The rect, corners, background color, and alphas all come
 *   from MorphOverlay helpers via [MorphSurface]; no parallel
 *   morph system. The background lerps from [sourceCardColor]
 *   (the source card's container color) to the panel background
 *   over the first 35% of the morph, and the container alpha
 *   follows the reconciled curves (fade in over the first 15% of
 *   open, fade out over the final 25% of close) — the surface,
 *   not a card hide, is what covers the stationary card, and
 *   because the card stays visible and the surface starts
 *   card-colored and transparent, no pitch-black box can slice
 *   across adjacent cards. Content is laid out once at full size
 *   and scales with the surface via graphicsLayer (Round 6 change
 *   from Round 5's clip-reveal); it fades in symmetrically on
 *   enter (invisible through p = 0.15, in by p = 0.50) and out
 *   early on close (gone by p = 0.85), so no panel content ever
 *   appears at full opacity over the card on the first frame.
 *   Destination headers hide only their badge while traveling
 *   (see MorphSurface's traveler slot), never their text.
 * - Travel targets: [heroTargetReporter]/[arcTargetReporter] are
 *   provided around the LIVE content (never a measurement frame):
 *   the content is laid out at target geometry, so its header's
 *   untransformed layout bounds ARE the travel target. The camera
 *   panel passes neither (it has no traveling header).
 * - Chrome: status-bar padding, nothing else. Feedback round 7
 *   removes the back arrow row (and the camera call site's
 *   showBackBar argument with it): Back, the drag, and each
 *   panel's own badge affordance are the dismiss paths.
 * - Scroll: the panel body scrolls (the old dialog's scroll
 *   wrapper, same structure), so dialog-threaded pages that own
 *   no scroll of their own (sheet, detail, pop-out) scroll here.
 *   [scrollContent] = false skips the scroll wrapper for content
 *   that owns its own full-size layout (the camera panel's video
 *   body), where a fillMaxSize child inside a scroll container
 *   would have no definite height to fill.
 * - Drag (feedback round 7, Koda now-playing port): with a
 *   [dragHandle], the scroll wrapper carries a
 *   PopupDismissConnection (downward overscroll at top drags the
 *   panel toward its tile; child controls that consume their own
 *   drags are untouched), and the non-scroll branch carries a
 *   direct vertical-drag detector on the content box. No
 *   competing parent pointerInput on scrollable popups: the
 *   nested-scroll protocol orders parent pre-scroll before child
 *   consumption, so no consume-order proof is needed.
 * [onPanelPlaced] reports the panel's full-screen root rect (the
 * camera-rise origin for sheet Details actions); the layout
 * bounds are the target rect from the first frame, so this fires
 * on placement rather than waiting for rest.
 */
@Composable
private fun PanelOverlay(
    progress: () -> Float,
    source: Rect?,
    overlayOrigin: Offset,
    panelBackground: Color,
    scrollContent: Boolean = true,
    // True while this panel is animating closed: the surface
    // container still lerps back to the card rect, but the inner
    // content fades out in the first stretch of the close (see
    // morphCloseContentAlpha) instead of shrinking at full opacity
    // over the grid — the thermostat dial bleed-through fix.
    // Feedback round 7: false while settling from a drag, so the
    // surface keeps the open curves through the settle.
    isClosing: Boolean = false,
    // The source card's container color (sheet/detail/pop-out pass
    // surfaceContainerHighest; cameras pass null and keep black).
    // The panel background lerps from this at p = 0 to
    // panelBackground at p = 0.35 (see MorphSurface).
    sourceCardColor: Color? = null,
    // Feedback round 7: drag handle (null = no drag; every current
    // caller passes one). Declared before the content lambda
    // (round 5 lesson: never append params after a lambda).
    dragHandle: PopupDragHandle? = null,
    heroTargetReporter: ((Rect) -> Unit)? = null,
    arcTargetReporter: ((Rect) -> Unit)? = null,
    onPanelPlaced: ((Rect) -> Unit)? = null,
    // Feedback round 11, item 2: the source tile's outline, passed
    // through to MorphSurface so the exit lands on its corners.
    landingShape: androidx.compose.ui.graphics.Shape? = null,
    content: @Composable () -> Unit,
) {
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
            MorphSurface(
                source = from,
                target = target,
                overlayOrigin = overlayOrigin,
                background = panelBackground,
                fromBackground = sourceCardColor,
                progress = progress,
                isClosing = isClosing,
                onPlaced = onPanelPlaced,
                landingShape = landingShape,
            ) {
                CompositionLocalProvider(
                    LocalHeroTarget provides heroTargetReporter,
                    LocalArcTarget provides arcTargetReporter,
                    // Feedback round 18, item 5: header
                    // drag-to-dismiss (see LocalPopupDragHandle).
                    LocalPopupDragHandle provides dragHandle,
                    LocalPopupDragRangePx provides { dragRangeForPanelOverlay(source?.top, target.top, target.height) },
                ) {
                    // Feedback round 7: the drag range is the exact
                    // top-edge travel from target to source tile.
                    val dragRangePx = dragRangeForPanelOverlay(source?.top, target.top, target.height)
                    if (scrollContent) {
                        // Feedback round 9, item 1 (ROOT CAUSE of
                        // round 7's dead drag): a NestedScrollConnection
                        // receives events ONLY from scrolling
                        // DESCENDANTS — it must be an ANCESTOR of the
                        // scrollable. Round 7 put nestedScroll AFTER
                        // verticalScroll, i.e. INSIDE the scrollable's
                        // own node, so the scrollable's post-scroll
                        // leftovers (the at-top downward drag) went
                        // to its ancestors and never reached this
                        // connection — onDragDelta never fired, no
                        // scrub, no dismiss, on every scrollContent
                        // surface. The connection goes OUTSIDE
                        // (before) verticalScroll. Do not "tidy"
                        // this order back: it is the fix.
                        Box(
                            Modifier.fillMaxSize().statusBarsPadding()
                                .then(
                                    if (dragHandle != null) {
                                        Modifier.nestedScroll(
                                            PopupDismissConnection(
                                                dragHandle.state,
                                                onDelta = { dy -> dragHandle.onDelta(dy, dragRangePx) },
                                                onRelease = { v -> dragHandle.onRelease(v, dragRangePx) },
                                            ),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                content()
                            }
                        }
                    } else {
                        // Full-size content (camera video): no scroll
                        // wrapper, so fillMaxSize resolves against the
                        // panel's definite size, not an unbounded scroll
                        // height. Feedback round 7: the direct drag
                        // detector lives here, where no scroll
                        // competition exists.
                        val cameraBoxModifier = if (dragHandle != null) {
                            Modifier.fillMaxSize().statusBarsPadding()
                                .popupDragDetector(
                                    dragHandle.state,
                                    onDelta = { dy -> dragHandle.onDelta(dy, dragRangePx) },
                                    onRelease = { v -> dragHandle.onRelease(v, dragRangePx) },
                                )
                        } else {
                            Modifier.fillMaxSize().statusBarsPadding()
                        }
                        Box(cameraBoxModifier) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Feedback round, item 3: the UNFOLDED/TABLET popup. This
 * deliberately reverses the both-postures full-screen law for
 * unfolded only — folded keeps PanelOverlay's full-screen morph.
 * Feedback round 18, item 2 (SUPERSEDES round 2, item 6's side
 * anchor): a CONTENT-SIZED surface (62% of the overlay, capped
 * at 460dp wide and 75% tall, scrolling internally when the
 * content is taller) ALWAYS centered in the content area, with
 * a 16dp side margin — the enter animation still runs from the
 * tile's direction (see centeredPopupEnter, fed with the popup
 * center computed analytically here — the target must be known
 * at frame 0, before layout measures the surface).
 * The thermostat disc (320dp + 32dp padding = 352dp) sets the
 * width floor: the narrowest wide layout is 600dp, and
 * 0.62 x (600 - 32) = 352dp, so the disc fits exactly even at the
 * threshold; internal polygons keep their sizes (shrinking the
 * disc would break the dual-number row and grab targets — the
 * window shrink + side anchor deliver the complaint, not a disc
 * resize). No traveler on this path (a traveler flying
 * to a side popup reads wrong), and two badge-visibility guards
 * replace them: LocalMorphProgress is pinned to 1f inside the
 * popup, so destination header badges render fully visible
 * from frame 0 (see morphHeaderBadgeAlpha); and the overlay
 * root nulls LocalHiddenHeroId while a modal is open, so the
 * source card's badge never hides behind the scrim (the page
 * behind a popup stays visible, unlike the opaque full-screen
 * panel). The camera panel keeps PanelOverlay on both postures
 * — a video surface needs the full area; the complaint was
 * about info pages. Scrim tap dismisses (a stated behavior
 * addition: full-screen panels have no scrim); the surface
 * consumes its own taps. Sheet callers pass onPanelPlaced so
 * the sheet→detail handoff (vm.pageOrigin) keeps working.
 * Feedback round 7: drag-to-dismiss (Koda now-playing port) —
 * the inner scroll column carries a PopupDismissConnection.
 * Feedback round 18, item 5 (EXPLICIT REVERSAL of round 11,
 * item 1): FINGER-TRACKED dismiss — dragging down scrubs the
 * morph progress with the finger (see dragProgressFor); header
 * rows carry popupDragDetector for the same.
 * Feedback round 18, item 2: unfolded popups are ALWAYS
 * centered in the content area (the old side anchor is gone),
 * capped at 460dp wide.
 */
@Composable
private fun CenteredPopup(
    progress: () -> Float,
    source: Rect?,
    overlayOrigin: Offset,
    onDismiss: () -> Unit,
    onPanelPlaced: ((Rect) -> Unit)? = null,
    // Feedback round 7: drag handle (null = no drag; every current
    // caller passes one). Before the content lambda (round 5).
    dragHandle: PopupDragHandle? = null,
    // Feedback round 11, item 2: the source tile's outline, for the
    // exit corner morph (see the Surface graphicsLayer below).
    landingShape: androidx.compose.ui.graphics.Shape? = null,
    // Feedback round 15, item 2: true while a dismiss animation
    // runs (mirrors PanelOverlay's isClosing), so the content
    // fades out early on close instead of late.
    isClosing: Boolean = false,
    // Feedback round 16, item 4a: the source tile's container
    // color, captured at open (null = sourceless/camera: keep the
    // container color throughout). The Surface keeps its
    // elevated container color (round 17, item 1); the content
    // wrapper below draws this lerp over it, so the unfolded
    // popup — which previously had no source-color lerp at all —
    // starts at the tile's own color instead of flashing panel
    // cream over the hidden tile on exit. Declared before the
    // content lambda (round 5 lesson).
    fromBackground: Color? = null,
    content: @Composable () -> Unit,
) {
    // Feedback round 7: the surface's placed rect in root
    // coordinates (same space as cardBounds — the existing
    // onPanelPlaced callback already reports boundsInRoot), for
    // the drag rect-lerp and range. NOT reused from onPanelPlaced:
    // that callback is the caller's handoff channel.
    var placedRect by remember { mutableStateOf<Rect?>(null) }
    Box(Modifier.fillMaxSize()) {
        // Scrim: dims the still-visible page behind the popup.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = 0.45f * progress().coerceIn(0f, 1f) }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { if (progress() > 0.5f) onDismiss() },
        )
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val overlayWidthPx = with(density) { maxWidth.toPx() }
            val overlayHeightPx = with(density) { maxHeight.toPx() }
            // Feedback round 18, item 2: ALWAYS centered in the
            // content area (the round-15 side anchor is gone).
            val popupCenter = Offset(
                overlayOrigin.x + overlayWidthPx / 2f,
                overlayOrigin.y + overlayHeightPx / 2f,
            )
            val distancePx = with(density) { 96.dp.toPx() }
            // Hoisted here, in the BoxWithConstraints scope: the
            // inner anchor Box below has a plain BoxScope receiver,
            // so maxHeight is not in scope inside it.
            val popupMaxHeight = maxHeight * 0.75f
            // The padded anchor box: 16dp side margins, always
            // centered (round 18, item 2).
            Box(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
            Surface(
                // Feedback round 11, item 2: the Surface shape is
                // the SUPERSET (36dp, the largest tile corner), so
                // the graphicsLayer morph clip below — not this
                // clip — governs the visible outline; otherwise
                // the Surface clip would cap a 36dp tile landing
                // at 28dp. The soft shadow follows this outline;
                // the visible corners follow the morph clip.
                // Feedback round 17, item 1: the container is the
                // ELEVATED surfaceContainerHigh with a 1dp
                // outlineVariant border — the old background color
                // blended invisibly into the black dashboard (the
                // 2026-11-04 "pure #000000" popup), so the popup
                // never reads as a surface of its own.
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .widthIn(max = 460.dp)
                    .heightIn(max = popupMaxHeight)
                    .graphicsLayer {
                        // Round 7 rule: progress read ONLY here.
                        // Feedback round 18, item 5 (finger
                        // tracking): the progress lambda IS the
                        // drag progress while dragging, so the
                        // popup scrubs with the finger.
                        // Feedback round 15, item 2: the
                        // enter transform is a rect morph from
                        // the source tile (tileMorphTransform),
                        // NOT the old side slide + fade — the
                        // surface grows out of the tapped tile
                        // and shrinks back onto it on exit. The
                        // target rect is analytic (center +
                        // overlay geometry + this layer's own
                        // untransformed size), never the placed
                        // rect, so the transform cannot feed
                        // back on itself. A sourceless popup
                        // keeps the old fade + scale in place.
                        // Item 2 (round 11): the clip lerps from
                        // the popup's 28dp corners into the
                        // source tile's corners, so the exit
                        // lands on the tile's own outline.
                        val p = progress()
                        if (source != null) {
                            val targetLeft = overlayOrigin.x + (overlayWidthPx - size.width) / 2f
                            val targetTop = overlayOrigin.y + (overlayHeightPx - size.height) / 2f
                            val target = androidx.compose.ui.geometry.Rect(
                                targetLeft, targetTop,
                                targetLeft + size.width, targetTop + size.height,
                            )
                            val morph = tileMorphTransform(source, target, p)
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            translationX = morph.translationX
                            translationY = morph.translationY
                            scaleX = morph.scaleX
                            scaleY = morph.scaleY
                            // Feedback round 17, item 1: the
                            // container alpha, mirroring
                            // MorphSurface — the old opaque-from-
                            // frame-0 surface sat on the tile as a
                            // blank tile-colored rect on open and
                            // a solid dark slab on close (the
                            // 2026-11-04 empty blue rectangle and
                            // eufy black box). Now the real tile
                            // shows through the scrim at small p
                            // and the surface fades in as it grows
                            // (out over the final 25% on close),
                            // so the source card is never blanked.
                            alpha = if (isClosing) morphPanelContainerAlphaClose(p) else morphPanelContainerAlphaOpen(p)
                        } else {
                            val enter = centeredPopupEnter(p, null, popupCenter, distancePx)
                            translationX = enter.offset.x
                            translationY = enter.offset.y
                            scaleX = enter.scale
                            scaleY = enter.scale
                            alpha = enter.alpha
                        }
                        shape = morphPopupShape(p, landingShape)
                        clip = true
                    }
                    .onGloballyPositioned { coords ->
                        placedRect = coords.boundsInRoot()
                        onPanelPlaced?.invoke(coords.boundsInRoot())
                    }
                    // Consume taps on the surface so they never
                    // reach the scrim dismiss beneath it.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { },
            ) {
                // Feedback round 16, item 4a: the color-lerp
                // rect. It covers the Surface exactly (same
                // size, drawn over the container color), lerping
                // from the tile's captured color to the container
                // color over the first 35% of the morph —
                // mirroring MorphSurface's fromBackground rule.
                // Progress is read ONLY in this draw block,
                // never during composition (round 6 rule), so
                // the morph still runs on the RenderThread.
                // At rest (p = 1) the rect IS the container
                // color; sourceless popups pass null and draw
                // the container color throughout. (Round 17:
                // the lerp target moved from background to
                // surfaceContainerHigh with the Surface itself,
                // or the settled panel would still read as
                // background under its new border. The color is
                // read here, in composable scope — drawBehind
                // cannot read MaterialTheme.)
                val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val p = progress().coerceIn(0f, 1f)
                        drawRect(lerp(fromBackground ?: containerColor, containerColor, morphPanelBackgroundBlend(p)))
                    },
                ) {
                // Feedback round 7: drag range is the vertical
                // travel of the surface center toward the tile
                // center (placed height fallback before first
                // placement; a drag cannot start before rest,
                // which requires placement). Hoisted above the
                // provider so the range lambda can close over it.
                val centeredDragRangePx = dragRangeForCenteredPopup(
                    placedRect?.center?.y,
                    source?.center?.y,
                    placedRect?.height ?: overlayHeightPx,
                )
                // Pin destination-header visibility: with no
                // traveler on this path, headers must render fully
                // visible from frame 0 (see the KDoc above).
                // Feedback round 18, item 5: header drag-to-dismiss.
                CompositionLocalProvider(
                    LocalMorphProgress provides { 1f },
                    LocalPopupDragHandle provides dragHandle,
                    LocalPopupDragRangePx provides { centeredDragRangePx },
                ) {
                    // Feedback round 9, item 1: same ancestor
                    // rule as PanelOverlay's scroll branch — the
                    // connection must sit OUTSIDE (before)
                    // verticalScroll, or the scrollable's leftover
                    // never reaches it and the drag is dead.
                    Column(
                        Modifier.fillMaxWidth()
                            .then(
                                if (dragHandle != null) {
                                    Modifier.nestedScroll(
                                        PopupDismissConnection(
                                            dragHandle.state,
                                            onDelta = { dy -> dragHandle.onDelta(dy, centeredDragRangePx) },
                                            onRelease = { v -> dragHandle.onRelease(v, centeredDragRangePx) },
                                        ),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .verticalScroll(rememberScrollState())
                            // Feedback round 15, item 2: the
                            // Koda reveal rule for the morph —
                            // content stays invisible while the
                            // surface is still growing and fades
                            // in late (out early on close), so no
                            // full-size content ever appears
                            // inside a still-small rect.
                            .graphicsLayer {
                                alpha = if (isClosing) morphCloseContentAlpha(progress())
                                else morphEnterContentAlpha(progress())
                            },
                    ) {
                        content()
                    }
                }
                } // color-lerp wrapper (round 16, item 4a)
            }
            }
        }
    }
}

/**
 * The traveling weather arc (2026-10-29 verdict-fix round): the
 * Home weather card's dial panel travels into the pop-up's dial
 * panel as ONE element — the arc visibly moves, never doubles.
 * Mirrors the traveling-badge pattern exactly: the card-side panel rect
 * (reported by WeatherControl via reportArcBounds) lerps to the
 * live panel's panel rect on the same progress, the panel content
 * (condition-tinted box + Dial) fills the target rect and scales
 * with it via graphicsLayer, and the traveler hands off to the
 * destination panel in the final 2% window (heroHeaderAlpha) while
 * the destination panel dissolves in on the inverse curve
 * (dissolveIntoTraveler) — so at every frame exactly one arc is
 * visible. Non-weather entities return early (their arc bounds are
 * never reported); a missing source renders nothing (the
 * destination's late fade still shows the arc at rest); a missing
 * target renders at the source rect until the live header reports,
 * same as the header. The panel keeps its 16dp padding at every
 * size, so the Dial diameter is the target panel width minus 32dp
 * — at both ends that is the 220dp default. The humidity line and
 * timeline are destination-only layout and fade in late with the
 * rest of the pop-up content; the badge + arc panel are the
 * traveling elements. Round 6: placement is static at the target
 * rect; only the graphicsLayer transform animates (see
 * the traveling badge), and the close-tail fade follows the same
 * reconciled container curve.
 */
@Composable
private fun TravelingArc(
    vm: HavenViewModel,
    entityId: String,
    source: Rect?,
    target: Rect?,
    progress: () -> Float,
    overlayOrigin: Offset,
    isClosing: Boolean = false,
) {
    val entity = vm.travelSnapshots[entityId] ?: vm.states.value[entityId] ?: return
    if (entity.domain != "weather") return
    // Round 7: same traveler-missing guard as the old header —
    // sit on the destination rect when the card never reported
    // bounds, so the arc never vanishes for the whole morph.
    val effectiveSource = source ?: target
    if (effectiveSource == null) return
    // Same no-target rule as the old header: sit on the source
    // rect until the live panel reports, never a gap.
    val targetRect = target ?: effectiveSource
    val density = LocalDensity.current
    val temp = entity.attrDouble("temperature") ?: 0.0
    val conditionColor = weatherConditionColorThemed(
        entity.state, isDarkBackdrop(MaterialTheme.colorScheme.background))
    Box(
        Modifier
            .offset {
                IntOffset(
                    (targetRect.left - overlayOrigin.x).roundToInt(),
                    (targetRect.top - overlayOrigin.y).roundToInt(),
                )
            }
            .size(with(density) { targetRect.width.toDp() }, with(density) { targetRect.height.toDp() })
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                val rect = lerpRect(effectiveSource, targetRect, p)
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = rect.left - targetRect.left
                translationY = rect.top - targetRect.top
                scaleX = if (targetRect.width > 0f) rect.width / targetRect.width else 1f
                scaleY = if (targetRect.height > 0f) rect.height / targetRect.height else 1f
                alpha = heroHeaderAlpha(p) * (if (isClosing) morphPanelContainerAlphaClose(p) else 1f)
            }
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
            diameter = with(density) { (targetRect.width.toDp() - 32.dp).coerceAtLeast(0.dp) },
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
private fun CameraFullscreen(vm: HavenViewModel, onClose: () -> Unit, enterProgress: () -> Float) {
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
        // Feedback round, item 4: HLS-first for stream-capable
        // non-Ring cameras; Ring keeps the MJPEG recording body
        // with its labels, byte for byte (CameraLiveBody routes).
        CameraLiveBody(
            vm, cameraId, enterProgress, recording = isRing, onClose = onClose,
            connectingText = if (isRing) "Loading recording…" else null,
            errorPrefix = if (isRing) "MJPEG proxy" else null,
        )
        // Ring carries its recording caption persistently (see
        // above); other cameras never carried a badge here either.
        CameraHeader(
            entity?.let { displayCameraName(it) } ?: "Camera",
            badgeText = if (isRing) "Latest motion recording · Not live" else null,
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
 *  2. WebRTC (GATED OFF, Round 7): was via RingWebRtcSession +
 *     the app's WS connection (camera/webrtc/offer). Two #103
 *     tombstones (21:57:53 + 21:58:52, identical stripped
 *     libjingle worker-thread SIGSEGV, same pc) prove the native
 *     crash survives Round 6's complete teardown rewrite and
 *     happens DURING the handshake — so the crashing path is not
 *     started at all (see webrtcEnabled in FrontDoorCameraPanel).
 *     A native SIGSEGV cannot be caught; gating is the only
 *     deterministic fix. The 69a1 body in step 3 is the live
 *     path and the wave test is its arbiter.
 *  3. The ring-mqtt live camera (camera.front_door_69a1) via its
 *     MJPEG proxy, once the live-stream switch reports on plus a
 *     ~2s settle wait (Round 5 Bug C). Frames here earn the LIVE
 *     badge while the switch is confirmed on (see
 *     frontDoorShowsLiveBadge); if the switch reverts to off
 *     mid-stream, the body swaps to the recording fallback, the
 *     badge drops, and the status line says so. The wave test is
 *     what proves live: the status line tells the tester to wave
 *     and watch for it promptly.
 *  4. If the 69a1 stream also errors, the panel falls back to the
 *     live-view camera's MJPEG proxy — labeled as the latest
 *     motion recording, never live, no badge — and the status line
 *     says the live view is unavailable. Event History offers the
 *     same recording body one tap away.
 *
 * Round 5 honesty correction (supersedes the #93 rule): the old
 * MJPEG+switch-on frames were recordings, not live — the
 * live_view proxy serves only the last motion recording MP4, with
 * or without the switch. After this fix, live_view proxy frames
 * are labeled as recordings everywhere in this panel, and the
 * LIVE badge appears ONLY for WebRTC frames (remote description
 * applied) or 69a1 live-stream frames.
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
    enterProgress: () -> Float,
    onClose: () -> Unit,
) {
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    val switchOn = states[FRONT_DOOR_LIVE_STREAM_SWITCH_ID]?.state == "on"
    // Camera fallback round: the resolved front-door video source,
    // re-derived from the live states map on every state change,
    // so a recovered 69a1 automatically retakes the lead (see
    // resolveFrontDoorCameraSource). Null means both sources down.
    val frontDoorSource = resolveFrontDoorCameraSource(states)
    val engaged = vm.frontDoorLiveEngaged
    val stopReason = vm.frontDoorLiveStopReason

    var tab by remember { mutableStateOf(FrontDoorTab.LIVE) }
    // Round 7: initial phase is MJPEG (WebRTC gated off) — never
    // WEBRTC_CONNECTING, so the WebRTC branch cannot mount first.
    var phase by remember { mutableStateOf(FrontDoorLivePhase.MJPEG) }
    var webrtcAttempt by remember { mutableIntStateOf(0) }
    var webrtcFrames by remember { mutableStateOf(false) }
    var webrtcOutcome by remember { mutableStateOf<String?>(null) }
    var mjpegFrames by remember { mutableStateOf(false) }
    var mjpegError by remember { mutableStateOf<String?>(null) }
    // Round 5 Bug C: the 69a1 live body has failed (and the panel
    // is on the recording fallback), and the 69a1 body is armed
    // (switch reported on + settle wait done). mjpegFrames feeds
    // the LIVE badge ONLY from the 69a1 body — never from the
    // live_view recording fallback.
    var live69a1Failed by remember { mutableStateOf(false) }
    var live69a1Ready by remember { mutableStateOf(false) }
    // Visual-fidelity round §3: capped auto-restart for the live
    // body. A post-frame stream error (stall, proxy drop) used to
    // fail the live body over to the recording immediately — the
    // "stops after several seconds" cutoff. Now the live body
    // remounts (keyed on liveBodyKey) up to 3 consecutive errors
    // with no intervening frame; any frame resets the count. Only
    // after the cap — or on a pre-frame failure, which keeps the
    // old immediate fallback — does the panel fall through to
    // the truthful recording fallback + status line. The count
    // and the remount key are SEPARATE: resetting the count on a
    // frame must not itself remount the body (that would
    // re-buffer a healthy stream).
    var liveRestarts by remember { mutableIntStateOf(0) }
    var liveBodyKey by remember { mutableIntStateOf(0) }
    var liveEverFramed by remember { mutableStateOf(false) }

    // Engagement: the Live tab owns the switch. Resetting the probe
    // on every Live activation keeps a stale phase from surviving a
    // History round-trip or a background stop.
    LaunchedEffect(tab) {
        if (tab == FrontDoorTab.LIVE) {
            // Round 7: WebRTC is gated off (see webrtcEnabled below).
            // The Live tab goes straight to the 69a1 MJPEG live body.
            phase = FrontDoorLivePhase.MJPEG
            webrtcFrames = false
            webrtcOutcome = null
            mjpegFrames = false
            mjpegError = null
            live69a1Failed = false
            live69a1Ready = false
            liveRestarts = 0
            liveBodyKey = 0
            liveEverFramed = false
            vm.startFrontDoorLive()
        }
    }
    // Round 5 Bug C settle wait: the 69a1 live body mounts only
    // once the switch reports on AND ~2s have passed, so the
    // ring-mqtt feed is actually up before its proxy is opened.
    // The effect restarts if the switch flips, so an off->on
    // re-arms the wait; off clears readiness at once, which swaps
    // the body to the recording fallback.
    LaunchedEffect(switchOn) {
        if (!switchOn) {
            live69a1Ready = false
            // A switch off ends the live session: a later on is a
            // fresh attempt, not a continuation of the old count.
            liveRestarts = 0
            liveEverFramed = false
        } else {
            delay(2000)
            live69a1Ready = true
        }
    }
    // Camera fallback round: re-arm on source flips. A recovered
    // 69a1 must retake the live body (clearing a latched failure),
    // and a flip away from 69a1 must drop any stale LIVE-badge
    // frame flag at once — the fallback body never feeds the badge,
    // so without this the badge could linger on fallback video.
    // This also fires once at open, where every reset is a no-op
    // except liveBodyKey (0 -> 1, harmless: the body keys on it).
    LaunchedEffect(frontDoorSource) {
        mjpegFrames = false
        mjpegError = null
        if (frontDoorSource == FRONT_DOOR_RING_MQTT_CAMERA_ID) {
            live69a1Failed = false
            liveRestarts = 0
            liveBodyKey += 1
            liveEverFramed = false
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

    // Round 6 teardown serialization: every fallback to MJPEG is
    // POSTED to the main handler (fallBackToMjpeg below) instead of
    // flipping phase inline. An inline flip disposes this effect
    // (session.close) while the signaling-thread observer callback
    // that caused the flip is still on the stack AND while the
    // AndroidView/SurfaceViewRenderer is unmounting — the
    // re-entrant teardown that, combined with #101's
    // factory.dispose() without pc.dispose(), is the native-abort
    // shape behind the Front Door process death. Posting defers the
    // flip by one main-loop pass, so the observer callback finishes
    // first and DisposableEffect.onDispose tears the session down
    // exactly once, on the main thread, in RingWebRtcSession's
    // strict order. No onRelease is added to the AndroidView:
    // session.close() is the single teardown owner, so the renderer
    // can never be released twice from two paths.
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val fallBackToMjpeg = { mainHandler.post { phase = FrontDoorLivePhase.MJPEG } }
    // WebRTC session: created in this effect, closed in its onDispose,
    // so leaving a WebRTC phase (fallback, background stop, tab flip)
    // always tears the peer connection down. The renderer is held in
    // a plain holder (not State): the AndroidView factory runs during
    // composition, before effects, so it is always set when this
    // effect runs — and a fresh renderer per attempt means a Resume
    // after a background stop never re-inits a released surface.
    val context = LocalContext.current
    val rendererHolder = remember { object { var renderer: SurfaceViewRenderer? = null } }
    // Round 7 WebRTC gate: false until the native crash is resolved.
    // Tombstones 21:57:53 + 21:58:52 on #103 (identical stripped
    // libjingle worker-thread SIGSEGV, same pc) prove the crash
    // survives Round 6's complete teardown rewrite and happens
    // DURING the handshake, before any fallback can run — so the
    // crashing path is not started at all. A native SIGSEGV cannot
    // be caught; not starting WebRTC is the only deterministic fix.
    // RingWebRtcSession + its tests stay in the tree for re-enable.
    // Re-enable condition: a newer libwebrtc build (GetStream
    // 1.3.10 is m125-based; LiveKit io.github.webrtc-sdk:android
    // 150-series or GetStream stream-video-webrtc-android 137.x
    // share the org.webrtc namespace) AND a device run with no
    // process death. Until then the 69a1 MJPEG body is the live
    // path and the wave test is its arbiter.
    val webrtcEnabled = false
    val inWebRtcPhase = webrtcEnabled &&
        (phase == FrontDoorLivePhase.WEBRTC_CONNECTING ||
            phase == FrontDoorLivePhase.WEBRTC_LIVE)
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
                            fallBackToMjpeg()
                        } else {
                            // Round 5 Bug A fix: the old code logged
                            // "answer usable" here but never invoked
                            // onAnswer, so the session never set
                            // the remote description and WebRTC
                            // could never produce frames. Applying
                            // the answer is what completes the
                            // negotiation (see RingWebRtcSession's
                            // setRemoteDescription in onAnswer).
                            Log.d("Haven", "FrontDoorLive: webrtc answer usable — applying remote description")
                            webrtcOutcome = "answer usable"
                            onAnswer(answer)
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
                    fallBackToMjpeg()
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
    // Visual-fidelity round §3: the effect is also keyed on the
    // frame flags — once actual live frames have been received,
    // the offer timeout is CANCELLED (the early return below) and
    // can never cut a healthy stream over to the recording. The
    // timeout still fires before the first frame, and a genuinely
    // failed connection still reaches its truthful failure state.
    // (Dormant while WebRTC is gated: phase is never CONNECTING —
    // but correct for any future re-enable.)
    LaunchedEffect(phase, webrtcAttempt, mjpegFrames, webrtcFrames) {
        if (phase != FrontDoorLivePhase.WEBRTC_CONNECTING) return@LaunchedEffect
        if (mjpegFrames || webrtcFrames) return@LaunchedEffect
        delay(20_000)
        Log.w("Haven", "FrontDoorLive: webrtc offer timed out (20s, no answer)")
        webrtcOutcome = "timed out (no answer in 20s)"
        fallBackToMjpeg()
    }

    // Round 5 honesty: mjpegFrames here is fed ONLY by the 69a1
    // live body (see the MJPEG phase below) — live_view recording
    // frames never reach it, so the badge can no longer fire on a
    // recording with the switch on (the #93 rule's error).
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
        // Camera fallback round: the fallback source gets a neutral
        // status line — no liveness claim (no LIVE badge either),
        // and no recording claim. On a fallback-body error the
        // later mjpegError branch reports "Live view unavailable".
        phase == FrontDoorLivePhase.MJPEG && frontDoorSource == FRONT_DOOR_LIVE_VIEW_ID && mjpegError == null -> "Front door camera"
        phase == FrontDoorLivePhase.MJPEG && live69a1Failed -> "Live view unavailable — showing latest recording"
        phase == FrontDoorLivePhase.MJPEG && liveRestarts > 0 && !mjpegFrames -> "Reconnecting to live view…"
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
                            // Round 7: gated — resume goes to MJPEG, never WebRTC.
                            phase = FrontDoorLivePhase.MJPEG
                            webrtcFrames = false
                            webrtcOutcome = null
                            mjpegFrames = false
                            mjpegError = null
                            live69a1Failed = false
                            live69a1Ready = false
                            liveRestarts = 0
                            liveBodyKey = 0
                            liveEverFramed = false
                            vm.startFrontDoorLive()
                        }) { Text("Resume live") }
                        // Visual-fidelity round §3: the segmented
                        // control is the sole visible mode selector
                        // — the "Show latest recording" button that
                        // used to sit here is removed (History is
                        // one tap away on the segmented control).
                    }
                }
            }
            phase == FrontDoorLivePhase.MJPEG -> {
                // Round 5 Bug C: the live body is the ring-mqtt
                // 69a1 camera's proxy, never the live_view proxy
                // (which serves only the last motion recording).
                // Until the switch reports on + the settle wait
                // lands, the connecting state shows; once the 69a1
                // stream errors, the body falls back to the
                // live_view proxy as a LABELED recording
                // (recording = true, and no onFirstFrame feeding
                // the badge — recording frames must never earn
                // LIVE). A 69a1 error also clears mjpegFrames so
                // the badge cannot linger on the fallback while
                // the switch is still on.
                // Camera fallback round: the two branches below
                // run FIRST. When 69a1's state is "unavailable"
                // (or it is absent), the Live tab skips the 69a1
                // attempt entirely — no switch-wait, no error
                // cycle — and shows the official camera's MJPEG
                // proxy directly, with NO onFirstFrame, so these
                // frames never feed mjpegFrames and never earn
                // the LIVE badge. When both sources are down the
                // panel shows an honest unavailable state, never
                // a blank frame or a stale last frame.
                when {
                    frontDoorSource == null -> {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Front door camera unavailable",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Both camera sources are unavailable right now.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                    frontDoorSource == FRONT_DOOR_LIVE_VIEW_ID -> {
                        CameraMjpegBody(
                            vm, FRONT_DOOR_LIVE_VIEW_ID, enterProgress, recording = false, onClose = onClose,
                            onError = { message ->
                                Log.w("Haven", "FrontDoorLive: fallback live_view error: $message")
                                mjpegError = message
                            },
                        )
                    }
                    !live69a1Failed && switchOn && live69a1Ready -> {
                        // Visual-fidelity round §3: keyed on the
                        // remount key (NOT the restart count), so
                        // a post-frame error remounts a fresh
                        // stream instead of failing over to the
                        // recording, while a frame resetting the
                        // count does not itself remount.
                        key(liveBodyKey) {
                            CameraMjpegBody(
                                vm, FRONT_DOOR_RING_MQTT_CAMERA_ID, enterProgress, recording = false, onClose = onClose,
                                onFirstFrame = {
                                    Log.d("Haven", "FrontDoorLive: 69a1 live first frame (switch on=$switchOn)")
                                    mjpegFrames = true
                                    liveEverFramed = true
                                    liveRestarts = 0
                                },
                                onError = { message ->
                                    if (liveEverFramed && liveRestarts < 3) {
                                        // Post-frame interruption:
                                        // restart the live body
                                        // (cap 3 consecutive errors
                                        // with no intervening frame)
                                        // instead of cutting over
                                        // to the recording. The
                                        // error is cleared so the
                                        // status reads
                                        // "Reconnecting…", not
                                        // "unavailable", mid-retry.
                                        Log.w("Haven", "FrontDoorLive: 69a1 live error: $message — restarting live (attempt ${liveRestarts + 1}/3)")
                                        mjpegFrames = false
                                        mjpegError = null
                                        liveRestarts += 1
                                        liveBodyKey += 1
                                    } else {
                                        // Pre-frame failure, or the
                                        // cap reached: the truthful
                                        // fallthrough to the labeled
                                        // recording fallback.
                                        Log.w("Haven", "FrontDoorLive: 69a1 live error: $message — falling back to recording")
                                        mjpegError = message
                                        mjpegFrames = false
                                        live69a1Failed = true
                                    }
                                },
                            )
                        }
                    }
                    !live69a1Failed && switchOn -> {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Connecting to live view…", color = Color.White, fontSize = 16.sp)
                        }
                    }
                    else -> {
                        CameraMjpegBody(
                            vm, cameraId, enterProgress, recording = true, onClose = onClose,
                            onError = { message ->
                                Log.w("Haven", "FrontDoorLive: recording fallback error: $message")
                                mjpegError = message
                            },
                        )
                    }
                }
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
                // Feedback round 7: the header back arrow is
                // removed with every other popup chrome button.
                // BackHandler + the drag dismiss (onClose stays:
                // the video bodies below use it for their error
                // paths).
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
            // Visual-fidelity round §3: the raw diagnostic line
            // ("Live-stream switch: On · Battery … · Auto-off …")
            // is removed, and the segmented control above is the
            // sole visible mode selector — the "Show latest
            // recording instead" button that used to sit here is
            // removed (History is one tap away on the segmented
            // control). pathLine status lines stay: they are
            // user-facing status, not diagnostics.
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
/**
 * Feedback round, item 4: the HLS-first live body for NON-Ring
 * cameras. Haven's live path was the MJPEG proxy for every camera:
 * HA ffmpeg-transcodes the Tapo RTSP feeds to MJPEG, which is
 * where the living-room/garage grain comes from (Haven decodes
 * the MJPEG at full resolution — no downsampling in the app —
 * so the grain is the source, not Haven). The HA app shows the
 * same cameras sharper because it plays the native H.264 stream,
 * and Haven already owns the HLS machinery (HaClient.requestHlsUrl
 * + createHlsPlayer, Bearer-authed) — it was just never wired
 * into a viewer.
 *
 * Routing (Ring untouched by construction):
 * - Ring camera, or a camera that does not advertise stream
 *   support (cameraSupportsStream: the SUPPORT_STREAM bit, the
 *   same gate HA's frontend uses) → the existing MJPEG body,
 *   byte for byte, with its connecting/error copy intact.
 * - Stream-capable non-Ring camera → request the HLS playlist
 *   (8s timeout); on a URL, play it with ExoPlayer on a
 *   SurfaceView (no media3-ui dependency); on any failure,
 *   timeout, or player error, fall back to the MJPEG body
 *   TERMINALLY for this mount (no HLS retry loop). Hero
 *   quadrants keep snapshots (still thumbnails, not the grain
 *   complaint); the Front Door panel's bodies keep
 *   CameraMjpegBody directly and are never routed through here.
 */
@Composable
private fun CameraLiveBody(
    vm: HavenViewModel,
    cameraId: String,
    enterProgress: () -> Float,
    recording: Boolean,
    onClose: () -> Unit,
    connectingText: String? = null,
    errorPrefix: String? = null,
    onFirstFrame: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
) {
    val states by vm.states.collectAsState()
    val entity = states[cameraId]
    // Ring + non-stream cameras + unknown entities: MJPEG, unchanged.
    if (entity == null || isRingCamera(entity) || !cameraSupportsStream(entity)) {
        CameraMjpegBody(vm, cameraId, enterProgress, recording, onClose, connectingText, errorPrefix, onFirstFrame, onError)
        return
    }
    var hlsUrl by remember(cameraId) { mutableStateOf<String?>(null) }
    var hlsFailed by remember(cameraId) { mutableStateOf(false) }
    LaunchedEffect(cameraId) {
        var done = false
        val timeout = launch {
            delay(8000)
            if (!done) {
                done = true
                hlsFailed = true
            }
        }
        vm.client.requestHlsUrl(cameraId) { url, _ ->
            if (!done) {
                done = true
                timeout.cancel()
                if (url != null) hlsUrl = url else hlsFailed = true
            }
        }
    }
    if (hlsFailed) {
        CameraMjpegBody(vm, cameraId, enterProgress, recording, onClose, connectingText, errorPrefix, onFirstFrame, onError)
        return
    }
    val url = hlsUrl
    if (url == null) {
        // Attempting: mirror the MJPEG Connecting stage (cached
        // frame dimmed + loading text) so the wait reads the same.
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val last = vm.lastCameraFrame(cameraId)
            if (last != null) {
                Image(
                    bitmap = last.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.25f },
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading camera…", color = Color.White, fontSize = 16.sp)
            }
        }
        return
    }
    CameraHlsPlayerBody(
        vm, cameraId, url, enterProgress, onFirstFrame,
        // Player error → MJPEG fallback, terminal for this mount.
        // onError is deliberately NOT forwarded: the fallback body
        // owns error display from here (same content, MJPEG).
        onPlayerError = { hlsFailed = true },
    )
}

/** ExoPlayer HLS playback on a bare SurfaceView (no media3-ui).
 *
 * Letterboxing (round 6, camera item): a bare SurfaceView at
 * fillMaxSize STRETCHES. MediaCodec's SCALE_TO_FIT (the player
 * default, and what createHlsPlayer leaves unset) means "scaled
 * to the surface dimensions" — it does NOT letterbox by itself;
 * the AOSP doc is explicit that a SCALE_TO_FIT surface must
 * itself be positioned at the proper final display aspect
 * ratio. Here the surface was the whole portrait screen, so a
 * 16:9 Tapo frame was pulled vertically to fill it. The MJPEG
 * body never had this bug: its Image uses ContentScale.Fit.
 * The fix sizes the SURFACE itself to the video's display
 * aspect (from onVideoSizeChanged, pixel aspect applied),
 * centered in the black Box, so the stretch-to-surface lands
 * undistorted and the leftover Box area is the black bars.
 * PlayerView does the same job via its AspectRatioFrameLayout;
 * this body has no media3-ui, so it does it by hand. The
 * explicit videoScalingMode below only pins the default — it
 * is NOT the letterboxing mechanism, and must not be mistaken
 * for one if this sizing is ever touched.
 */
@Composable
private fun CameraHlsPlayerBody(
    vm: HavenViewModel,
    cameraId: String,
    playlistUrl: String,
    enterProgress: () -> Float,
    onFirstFrame: (() -> Unit)?,
    onPlayerError: (String) -> Unit,
) {
    val context = LocalContext.current
    var rendered by remember(cameraId, playlistUrl) { mutableStateOf(false) }
    var videoAspect by remember(cameraId, playlistUrl) { mutableStateOf<Float?>(null) }
    val player = remember(cameraId, playlistUrl) {
        vm.client.createHlsPlayer(context, playlistUrl).apply {
            // Pinned default only — see the doc comment above.
            videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    val w = videoSize.width
                    val h = videoSize.height
                    if (w > 0 && h > 0) {
                        val aspect = w * videoSize.pixelWidthHeightRatio / h
                        if (aspect > 0f && aspect.isFinite()) videoAspect = aspect
                    }
                }

                override fun onRenderedFirstFrame() {
                    if (!rendered) {
                        rendered = true
                        onFirstFrame?.invoke()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    onPlayerError(error.message ?: "HLS playback failed")
                }
            })
        }
    }
    DisposableEffect(cameraId, playlistUrl) {
        onDispose { player.release() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!rendered) {
            val last = vm.lastCameraFrame(cameraId)
            if (last != null) {
                Image(
                    bitmap = last.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.25f },
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading camera…", color = Color.White, fontSize = 16.sp)
            }
        }
        // Fit: the largest rect of the video aspect inside the
        // Box, centered, so the black Box area becomes the bars.
        // Aspect unknown yet -> full-size surface (black until
        // the size event, which precedes the first frame).
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val aspect = videoAspect
            val surfaceModifier = if (aspect == null) {
                Modifier.fillMaxSize()
            } else if (maxWidth / maxHeight < aspect) {
                Modifier.size(width = maxWidth, height = maxWidth / aspect)
            } else {
                Modifier.size(width = maxHeight * aspect, height = maxHeight)
            }
            AndroidView(
                factory = { ctx -> SurfaceView(ctx).also { player.setVideoSurfaceView(it) } },
                modifier = surfaceModifier.graphicsLayer {
                    // Same enter curve as the MJPEG body.
                    val p = enterProgress().coerceIn(0f, 1f)
                    val s = 0.94f + 0.06f * p
                    scaleX = s
                    scaleY = s
                    alpha = 0.7f + 0.3f * p
                },
            )
        }
    }
}

@Composable
private fun CameraMjpegBody(
    vm: HavenViewModel,
    cameraId: String,
    enterProgress: () -> Float,
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
                    // Round 7: progress is a lambda, read ONLY here.
                    val p = enterProgress()
                    val s = 0.94f + 0.06f * p
                    scaleX = s
                    scaleY = s
                    alpha = 0.7f + 0.3f * p
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
// Feedback round 7: the top-right Close (X) is removed with every
// other popup chrome button; both hosts keep BackHandler, and the
// video bodies keep their own error-path Close buttons. The onClose
// parameter goes with the button (it had no other reader).
private fun BoxScope.CameraHeader(name: String, badgeText: String?) {
    Column(
        Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
    ) {
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (badgeText != null) {
            Spacer(Modifier.height(4.dp))
            Text(badgeText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

/**
 * Fullscreen MJPEG playback of a recorded clip (timeline entries):
 * explicitly a recording — badge says "Recording", never "Live".
 * Public so the clips timeline screen can host it.
 */
@Composable
fun RecordingFullscreen(vm: HavenViewModel, recordingEntityId: String, onClose: () -> Unit, enterProgress: () -> Float) {
    val states by vm.states.collectAsState()
    val entity = states[recordingEntityId]
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) { // video surface: black by design, not a scheme role
        CameraMjpegBody(vm, recordingEntityId, enterProgress, recording = true, onClose = onClose)
        CameraHeader(entity?.let { displayCameraName(it) } ?: "Camera", badgeText = "Recording")
    }
}
/**
 * Feedback round 3, item 6 (video 6d): routes Samsung Device
 * Controls taps into the app. The Controls provider's app intent
 * carries the tapped entity id (see appIntentFor); MainActivity
 * stores it on the VM as [HavenViewModel.controlDeepLinkEntityId].
 * This router consumes it once the entity's state has loaded —
 * cold start: states arrive after connect, so the effect re-runs
 * per emission until the entity appears — then jumps to that
 * entity's surface and clears the slot:
 * - climate → the thermostat popout/page (vm.openClimatePopout,
 *   the same machinery a thermostat card tap uses);
 * - everything else → EntityDetail, exactly as a home-page card
 *   tap does (popup on unfolded, detail as home cards do).
 * Unknown ids (the sign-in control's "haven_open_app", deleted
 * entities) clear without routing once states are non-empty, so a
 * stale link never hangs the slot. A duplicate detail — already
 * viewing that entity — is skipped. Renders nothing: its states
 * collection recomposes only this router, never UI.
 */
@Composable
private fun ControlDeepLinkRouter(vm: HavenViewModel, current: Screen, navigate: (Screen) -> Unit) {
    val pendingDeepLink = vm.controlDeepLinkEntityId
    val deepLinkStates by vm.states.collectAsState()
    LaunchedEffect(pendingDeepLink, deepLinkStates) {
        val id = pendingDeepLink ?: return@LaunchedEffect
        val entity = deepLinkStates[id]
        if (entity == null) {
            if (deepLinkStates.isNotEmpty()) vm.clearControlDeepLink()
            return@LaunchedEffect
        }
        when (controlDeepLinkTarget(id, entity.domain)) {
            is ControlDeepLinkTarget.ThermostatPopout -> vm.openClimatePopout(id)
            is ControlDeepLinkTarget.Detail -> {
                if (current !is Screen.EntityDetail || current.entityId != id) {
                    navigate(Screen.EntityDetail(id))
                }
            }
        }
        vm.clearControlDeepLink()
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
private fun VerticalSidePill(items: List<NavItem>, selectedIndex: Int?, currentScreen: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier, badgedScreens: Set<Screen> = emptySet()) {
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val currentIndex = selectedIndex ?: -1
    // Side-pill enter: spring in from the left edge on mount (the
    // pill remounts whenever the window crosses the wide
    // threshold, so this is also the fold/unfold transition).
    // Snapped under reduce-motion, like every other spring.
    // Round 7 (kept): slide only, never fade — icons stay mounted
    // and visible from frame 0 on every posture-switch remount.
    val pillEnter = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) pillEnter.animateTo(1f, springSpatial())
    }
    // Expressive-menus round, item 5: the unfolded rail scrubs
    // exactly like the folded pill bar (drag along the rail to
    // pick a tab, commit on release, haptic tick per tab). The
    // highlight + indicator follow the finger while scrubbing.
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
    val scrubIndex = scrub.hoveredTab ?: currentIndex
    val density = LocalDensity.current
    // Follow-up B (2026-11-03): the rail's selection glide uses
    // Koda's bar spec (expressive fast spatial) instead of the
    // house spring — the rail IS a bar, so its selection motion
    // matches the folded bar's. Koda's rail has no sliding
    // indicator (each item animates its own container color), so
    // there is no Koda indicator spec to copy; the glide keeps
    // Haven's indicator and takes Koda's spec. Reduce-motion
    // keeps the stiff snap.
    val indicatorSpec = if (reduceMotion) spring<Float>(stiffness = 10_000f, dampingRatio = 1f)
        else kodaBarSpec()
    // Feedback round 17, item 3: the indicator offset comes from
    // the item INDEX ([railIndicatorTop]), never from measured
    // window rects. The old boundsInWindow measurements went
    // stale across bar-list changes and the pill-enter
    // translation, clearing the indicator or parking it on the
    // wrong tab (Home displayed with the highlight off Home on
    // the 2026-11-04 runs). Index math cannot go stale, and the
    // glide animation above still carries the motion. The
    // indicator anchors to the icon oval: a fixed 56dp x 32dp
    // pill around the icon only, so the highlight never
    // stretches into a tall vertical capsule and never changes
    // shape across tabs. No selection (index out of range)
    // hides the indicator, same as before.
    val indicatorTop by animateFloatAsState(
        if (scrubIndex in items.indices) with(density) { railIndicatorTop(scrubIndex).toPx() } else 0f,
        animationSpec = indicatorSpec,
        label = "sidePillTop",
    )
    // Round 8: ICON-ONLY, Koda-style. The labels are gone entirely
    // — this kills the truncation defect by construction (there is
    // no label text left to truncate: "Living Room lights" can no
    // longer render as "Living Room li..."). The rail therefore
    // NARROWS from Round 6's 120dp label width to an icon-only
    // width: 12dp start margin + a 72dp pill container (the 56dp
    // icon oval + 8dp padding each side) = 84dp total, matching
    // Koda's unfolded icon-only rail. The enter slide stays halved
    // at -8dp: the old -16dp slide pushed the pill past its margin
    // in the first frames, reading as a strip at the far left edge.
    // The selected highlight is the same fixed 56x32dp pill behind
    // the icon as the folded PillNav (see selectedPillFill), so the
    // two chrome variants can never disagree. Labels survive only
    // as icon contentDescriptions for accessibility.
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkBackdrop(scheme.background)
    Box(
        modifier
            .width(84.dp)
            .fillMaxHeight()
            .padding(start = 12.dp, top = 24.dp, bottom = 24.dp)
            // Round 7: slide only, never fade. The old alpha ramp
            // started the whole rail (icons + labels + surface) at
            // invisible on every posture-switch remount (Round 6's
            // key boundary remounts chrome on fold/unfold), so the
            // rail icons transiently disappeared on every fold.
            // Translation alone carries the enter motion; icons
            // stay mounted and visible from frame 0.
            .graphicsLayer {
                translationX = -8.dp.toPx() * (1f - pillEnter.value)
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = FloatingCorner,
            // Feedback round 3, item 1 (video ba): the pill
            // background is RESTORED — opaque #161D17 container
            // in both themes, exactly as #108 had it. Round 2
            // item 5 removed it (a misread: the user wanted the
            // transition clip box gone, not the pill). The
            // shadow restores the floating-bar look the #108
            // pill had (container + shadow, Koda-style).
            color = PillBarContainer,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
        ) {
            Box(
                Modifier,
            ) {
                // The indicator: one fixed 56dp x 32dp pill around
                // the selected icon, behind the items. Centered on
                // the column (the column is 56dp of icons + 8dp
                // padding each side, so TopCenter is the icon
                // center); strictly bounded to the icon oval, never
                // stretching to row height, never clipping the rail.
                // Round 17, item 3: positioned by index math
                // (railIndicatorTop), not measured rects.
                if (scrubIndex in items.indices) {
                    Box(
                        Modifier.align(Alignment.TopCenter)
                            .offset { IntOffset(0, indicatorTop.roundToInt()) }
                            .size(56.dp, 32.dp)
                            .background(selectedPillFill(scheme, isDark), CircleShape),
                    )
                }
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 12.dp).pillScrubVertical(scrub),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for ((index, item) in items.withIndex()) {
                        val highlighted = index == scrubIndex
                        // Follow-up B (2026-11-03): Koda's rail
                        // items cross-fade their glyph color on
                        // tween 200 (same as the folded bar's
                        // glyph port); the rail keeps its icon-only
                        // form and its sliding indicator (Round 8
                        // law), so only the tint animation is
                        // ported — Koda's labeled 28dp-corner rail
                        // items are NOT copied, because Haven's
                        // rail is icon-only by directive.
                        val contentColor by animateColorAsState(
                            if (highlighted) selectedPillGlyph(scheme, isDark)
                            else pillUnselectedGlyph(scheme, isDark),
                            animationSpec = tween(200),
                            label = "railGlyphColor",
                        )
                        // Folded-pill item geometry (48dp tall), so
                        // the rail and the bar share one item rhythm.
                        Box(
                            modifier = Modifier.width(56.dp).height(48.dp)
                                .pillScrubItem(scrub, index)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (shouldNavigateOnTabTap(item.screen, currentScreen)) {
                                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        onSelect(item.screen)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            // Ripple lives ONLY on the 56x32dp
                            // icon oval (clipped to CircleShape);
                            // the item box taps through it.
                            Box(
                                Modifier.size(56.dp, 32.dp).clip(CircleShape)
                                    .clickable {
                                        if (shouldNavigateOnTabTap(item.screen, currentScreen)) {
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
                                    // Round 17, item 3: no rect
                                    // reporting anymore — the
                                    // indicator is index math.
                                    modifier = Modifier.size(24.dp),
                                )
                                // Stage E §9: same condition dot as the
                                // folded pill — never a count.
                                if (item.screen in badgedScreens) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 6.dp)
                                            .size(8.dp)
                                            .background(alertDotColor(true), CircleShape)
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
 * UI rewrite round (2026-11-03): the folded pill bar. The ACTIVE
 * destination renders ONE unified capsule (44dp tall, CircleShape,
 * #9BF67C fill) wrapping BOTH the filled 22dp icon AND the 14sp
 * bold label, both #0A2005 inside the capsule — this fixes the two
 * flagged bugs (black label outside the green oval against the
 * black bar; capsule wrapping only the icon). Follow-up B
 * (2026-11-03): Koda's bar animation — item padding animates
 * 12dp ↔ 20dp, the label fades + expands/shrinks horizontally,
 * colors cross-fade on tween 200, all on the expressive fast
 * spatial spring (see the item block).
 * Unselected destinations are a 22dp outlined icon, no label. The
 * bar keeps its opaque #161D17 container in both themes
 * (feedback round 3, item 1, restoring #108 after round 2 item 5
 * removed it on a misread); unselected glyphs are #D7E2D4 in both
 * themes (see pillUnselectedGlyph). No blue anywhere. The old measured sliding
 * indicator is gone (there is no indicator to glide — the capsule
 * IS the item), so no rect tracking and no animation state.
 * Dragging along the bar still scrubs the highlight (haptic tick
 * per tab) and commits on release, Koda-style. Floating vs Compact
 * moves bar height (64dp vs 52dp), outer padding, and corner radius
 * via [NavPillGeometry]. The folded scroll-away and the invisible
 * unfolded bottom clearance are untouched.
 */
@Composable
private fun PillNav(items: List<NavItem>, selectedIndex: Int?, currentScreen: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier, geometry: NavPillGeometry = navPillGeometry(NavStyle.FLOATING), badgedScreens: Set<Screen> = emptySet()) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkBackdrop(scheme.background)
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
    // Auto-width floating island, centered, capped at the screen
    // width (beyond which the row scrolls exactly as before).
    Box(modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints {
            Surface(
                modifier = Modifier.wrapContentWidth().widthIn(max = maxWidth),
                shape = RoundedCornerShape(geometry.cornerRadius),
                // Feedback round 3, item 1 (video ba): the pill
                // background is RESTORED — opaque #161D17
                // container in both themes, exactly as #108 had
                // it. Round 2 item 5 removed it (a misread: the
                // user wanted the transition clip box gone, not
                // the pill). The shadow restores the floating-bar
                // look the #108 pill had.
                color = PillBarContainer,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth().horizontalScroll(rememberScrollState()).padding(horizontal = geometry.rowHorizontalPadding, vertical = geometry.rowVerticalPadding).pillScrub(scrub),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for ((index, item) in items.withIndex()) {
                        val highlighted = (scrub.hoveredTab ?: currentIndex) == index
                        val onTap = {
                            if (shouldNavigateOnTabTap(item.screen, currentScreen)) {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onSelect(item.screen)
                            }
                        }
                        // Follow-up B (2026-11-03): Koda's bar
                        // treatment, ported from Koda's
                        // HorizontalFloatingToolbar item code
                        // (HomeScreen.kt): the item's horizontal
                        // padding animates 12dp (unselected) ↔ 20dp
                        // (selected) on the fast spatial spring,
                        // the label fades + expands/shrinks
                        // horizontally on the same spec, and the
                        // capsule/glyph colors cross-fade on tween
                        // 200. ONE item composable (no branch swap)
                        // so selection animates instead of popping.
                        // Resting geometry is unchanged except the
                        // selected padding 16dp → 20dp (Koda's
                        // selected value): 44dp item, 22dp icon, 8dp
                        // icon–label gap, 14sp bold label, #9BF67C
                        // capsule on #161D17, unselected #D7E2D4.
                        // Bar container radii (32dp Floating /
                        // 26dp Compact) and heights (64dp / 52dp)
                        // are Haven's own Floating/Compact style
                        // distinction, kept: Koda's container is
                        // the M3 default (fully round, 64dp tall,
                        // 8dp edge padding, 4dp item gap), and
                        // Haven's container/item radius
                        // relationship already matches it (large
                        // container radius + fully-round item
                        // pills). Haven's row edge padding (18dp /
                        // 12dp) and 4dp item gap are kept likewise.
                        val itemPadding by animateDpAsState(
                            if (highlighted) 20.dp else 12.dp,
                            animationSpec = kodaBarSpec(),
                            label = "pillItemPadding",
                        )
                        val capsuleColor by animateColorAsState(
                            if (highlighted) PillBarActiveCapsule else Color.Transparent,
                            animationSpec = tween(200),
                            label = "pillItemColor",
                        )
                        val glyphColor by animateColorAsState(
                            if (highlighted) PillBarActiveGlyph else pillUnselectedGlyph(scheme, isDark),
                            animationSpec = tween(200),
                            label = "pillGlyphColor",
                        )
                        Row(
                            modifier = Modifier.height(geometry.itemHeight)
                                .pillScrubItem(scrub, index)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onTap() }
                                .background(capsuleColor, CircleShape)
                                .padding(horizontal = itemPadding)
                                .animateContentSize(kodaBarSpec()),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (highlighted) item.icon else item.outlinedIcon,
                                    contentDescription = item.label,
                                    tint = glyphColor,
                                    modifier = Modifier.size(22.dp),
                                )
                                // Stage E §9: a condition dot on the
                                // icon's corner — never a count.
                                if (item.screen in badgedScreens) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .background(alertDotColor(!highlighted), CircleShape)
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = highlighted,
                                enter = fadeIn() + expandHorizontally(
                                    animationSpec = kodaBarSpec()
                                ),
                                exit = fadeOut() + shrinkHorizontally(
                                    animationSpec = kodaBarSpec()
                                ),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        item.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PillBarActiveGlyph,
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
/**
 * Customization round, item 2: optional displayTitle/displayIcon
 * (the caller's Card-settings overrides) and an optional edit
 * pencil beside the title opening Card settings. Callers without
 * a ViewModel keep the old behavior exactly.
 */
@Composable
fun DetailPopupHeader(
    entity: HaEntity,
    onDismiss: () -> Unit,
    displayTitle: String? = null,
    displayIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onEdit: (() -> Unit)? = null,
    // Feedback round 4, item 2: the preset badge pair for this
    // entity (detailBadgeOverride), or null to keep badgeColors.
    badgeOverride: Pair<Color, Color>? = null,
) {
    val heroTarget = LocalHeroTarget.current
    val accent = accentFor(entity.entityId)
    // Feedback round 18, item 5: header drag-to-dismiss (see
    // LocalPopupDragHandle); null on plain detail pages.
    val headerDragHandle = LocalPopupDragHandle.current
    val headerDragRangePx = LocalPopupDragRangePx.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
            .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) }
            .then(
                if (headerDragHandle != null) {
                    Modifier.popupDragDetector(
                        headerDragHandle.state,
                        { dy -> headerDragHandle.onDelta(dy, headerDragRangePx()) },
                        { v -> headerDragHandle.onRelease(v, headerDragRangePx()) },
                    )
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Feedback round 18, item 4: the badge renders in place
        // and fades with the content curves (see
        // morphHeaderBadgeAlpha) — no traveler, no flash.
        DomainBadge(
            entity,
            displayIcon ?: domainIcon(entity),
            accent,
            modifier = Modifier.size(48.dp).morphHeaderBadgeAlpha().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() },
            badgeOverride = badgeOverride,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            DialogTitleText(
                displayTitle ?: if (entity.domain == "sensor") sensorDisplayName(entity) else displayCameraName(entity),
                Modifier.fillMaxWidth(),
            )
            Text(entity.dialogHeaderState(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onEdit != null) {
            CardSettingsPencil(onClick = onEdit)
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
