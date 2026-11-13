package com.garrietta.haven

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.FRONT_DOOR_LIVE_MAX_MS
import com.garrietta.haven.data.FRONT_DOOR_LIVE_STREAM_SWITCH_ID
import com.garrietta.haven.data.frontDoorRtspUrl
import com.garrietta.haven.data.motionSensorForCamera
import com.garrietta.haven.data.HaClient
import com.garrietta.haven.data.storageKey
import com.garrietta.haven.data.HistoryPoint
import com.garrietta.haven.data.HaDiscovery
import com.garrietta.haven.data.LovelaceConfig
import com.garrietta.haven.data.ProbeResult
import com.garrietta.haven.data.ScanOutcome
import com.garrietta.haven.data.SettingsStore
import com.garrietta.haven.data.CardSize
import com.garrietta.haven.data.CardSizeStore
import com.garrietta.haven.data.TabsStore
import com.garrietta.haven.data.cardSizeKey
import com.garrietta.haven.data.UserTab
import com.garrietta.haven.data.cardToYaml
import com.garrietta.haven.data.changeCardType
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.data.DEFAULT_HIDDEN_HOME_SECTIONS
import com.garrietta.haven.data.HOME_SECTION_ORDER_DEFAULT
import com.garrietta.haven.data.ConnectionMode
import com.garrietta.haven.data.TokenStore
import com.garrietta.haven.data.connectionLabel
import com.garrietta.haven.data.connectionModeFromKey
import com.garrietta.haven.data.isLocalAddress
import com.garrietta.haven.data.alternateConnectionTarget
import com.garrietta.haven.data.authRetryDelayMs
import com.garrietta.haven.data.MAX_AUTH_RETRIES
import com.garrietta.haven.data.parseStateCache
import com.garrietta.haven.data.resolveConnectionTarget
import com.garrietta.haven.data.serializeStateCache
import com.garrietta.haven.data.shouldAutoPinNewTab
import com.garrietta.haven.data.HavenNotificationManager
import com.garrietta.haven.data.Placement
import com.garrietta.haven.data.placementFromKey
import com.garrietta.haven.data.tabPlacementFor
import com.garrietta.haven.data.movedByDelta
import com.garrietta.haven.data.resolveOrder
import com.garrietta.haven.ui.components.MAX_PINNED_TABS
import com.garrietta.haven.ui.components.RoomBarItem
import com.garrietta.haven.ui.components.barExtraSlots
import com.garrietta.haven.ui.components.canPinBarExtra
import com.garrietta.haven.ui.screens.RoomFilter
import com.garrietta.haven.ui.components.NavStyle
import com.garrietta.haven.ui.components.navStyleFromKey
import com.garrietta.haven.data.isNetworkAvailable
import com.garrietta.haven.data.normalizeInstanceUrl
import com.garrietta.haven.data.probeFailureMessage
import com.garrietta.haven.data.probeInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** Top-level navigation destinations. Detail screens push on top of these. */
sealed class Screen {
    data object Today : Screen()
    /** Stage F: first-class Scenes destination (brief §4/§10). */
    data object Scenes : Screen()
    /** Stage F: first-class Energy destination (brief §4/§11). */
    data object Energy : Screen()
    /** The cameras, their motion detection, the alarm, doors and locks. */
    data object Security : Screen()
    /** Thermostats, and every temperature and humidity reading. */
    data object Climate : Screen()
    data object Dashboards : Screen()
    data object Areas : Screen()
    data object Search : Screen()
    /** UI rewrite round: the custom-tabs browse list (Settings -> Browse -> User Tabs). */
    data object UserTabs : Screen()
    data object Settings : Screen()
    /** A user-created tab; Home and Settings are the built-in tabs. */
    data class UserTab(val tabId: String) : Screen()
    /** A page under Settings, named by its route (see SettingsRoute). */
    data class SettingsPage(val route: String) : Screen()
    /** Feedback round 19, item 5: a room pinned as a navigation tab — renders Today with that room's filter. */
    data class RoomTab(val areaId: String) : Screen()
    data object HomeEditor : Screen()
    data class EntityDetail(val entityId: String) : Screen()
    /** Round 3 morph panels: full-screen panels on both postures, reusing the PanelOverlay morph machinery. */
    data object VacuumPanel : Screen()
    data class MediaPanel(val entityId: String) : Screen()
    data object SecurityPanel : Screen()
    data object GreenhousePanel : Screen()
    data object UpdatesPanel : Screen()
    /** UI rewrite round §7: a Home Status group's full-screen panel. */
    data class HomeStatusPanel(val categoryKey: String) : Screen()
    /** A room, opened from its card on Home as a popup. A null area is "No room". */
    data class RoomPanel(val areaId: String?) : Screen()
    /** Scrollable recorded-clips timeline for a camera (long-press on its tile). */
    data class CameraClips(val entityId: String) : Screen()
    data class AreaDetail(val areaId: String) : Screen()
    data object DashboardEdit : Screen()
    data object AutomationBuilder : Screen()
    data object SceneBuilder : Screen()
    data object ScriptBuilder : Screen()
    data object HelperBuilder : Screen()
}

/**
 * The shape picker target: null = closed; entityId null = the
 * global default; forCard false = badge shape, true = card
 * outline shape.
 */
data class ShapePickerTarget(val entityId: String?, val forCard: Boolean)

class HavenViewModel(application: Application) : AndroidViewModel(application) {

    val tokenStore = TokenStore(application)
    val settingsStore = SettingsStore(application)
    val tabsStore = TabsStore(application)
    val cardSizeStore = CardSizeStore(application)
    val client = HaClient(viewModelScope)

    /** Home Assistant's own settings, for Settings' Home Assistant pages. */
    val admin = com.garrietta.haven.data.HaAdminApi(client)

    /** Where home is, from the home zone, as a starting point for a new zone. */
    fun homeLocationGuess(): Pair<Double, Double>? {
        val home = client.states.value["zone.home"] ?: return null
        val lat = home.attrDouble("latitude") ?: return null
        val lon = home.attrDouble("longitude") ?: return null
        return lat to lon
    }

    /** User-created tabs, in pill-bar order. Loaded synchronously so the app can open on the first tab. */
    var userTabs by mutableStateOf<List<UserTab>>(tabsStore.loadTabs())
        private set

    /**
     * Feedback round 20, item 5: card sizes, mirrored in Compose
     * state so grids recompose on change (the store alone is not
     * observable). The store is the source of truth across
     * restarts; this state is its live mirror, updated only
     * through [setCardSize]. Scope keys: "home", "room:<areaId>"
     * ("room:unassigned" for No room), "tab:<tabId>".
     */
    private var cardSizesState by mutableStateOf(cardSizeStore.loadAll())

    fun cardSize(scope: String, entityId: String): CardSize =
        cardSizesState[cardSizeKey(scope, entityId)] ?: CardSize.NORMAL

    fun setCardSize(scope: String, entityId: String, size: CardSize) {
        cardSizeStore.setSize(cardSizeKey(scope, entityId), size)
        cardSizesState = cardSizeStore.loadAll()
    }

    /**
     * Feedback round 19, item 3: the Home room filter, hoisted out
     * of TodayScreen into the ViewModel. TodayScreen used to own
     * it in remember, so every AnimatedContent/direct swap (popup
     * open/close, detail return) disposed TodayScreen and reset
     * the filter to My Home — the 2026-11-04 video's "back to My
     * Home instead of my room" bug. As ViewModel state it survives
     * popup opens, closes, and recompositions; returning from a
     * detail lands back in the room view. Tab taps to Home reset
     * it to All in goTop (Home means My Home); chips set it.
     */
    internal var roomFilter by mutableStateOf<RoomFilter>(RoomFilter.All)

    /**
     * Feedback round 19, item 5: per-area placements, mirrored in
     * Compose state so the bar/rail/chips recompose on change
     * (the store alone is not observable). The store is the
     * source of truth across restarts; this state is its live
     * mirror, updated only through [setRoomPlacement].
     */
    private var roomPlacementsState by mutableStateOf(settingsStore.roomPlacements())

    fun roomPlacement(areaId: String): Placement =
        placementFromKey(roomPlacementsState[areaId])

    /** Combined bar-extra count: pinned user tabs + nav-tab rooms (the 2-cap counts both). */
    fun barPinnedExtrasCount(): Int =
        userTabs.count { it.pinned } +
            roomPlacementsState.values.count { placementFromKey(it) == Placement.NAV_TAB }

    /**
     * Once: the cameras leave Home for the Security tab, where they sit
     * with their motion switches. A stored Home layout keeps everything
     * else; Manage home page can bring the cameras back.
     */
    private fun moveCamerasToSecurity() {
        if (settingsStore.camerasMovedToSecurity) return
        settingsStore.homeHiddenSections = settingsStore.homeHiddenSections + "cameras"
        settingsStore.camerasMovedToSecurity = true
    }

    /** How many pinned tabs and rooms fit beside the built-in tabs now shown. */
    fun barExtraSlotsNow(): Int = barExtraSlots(navShowSecurity, navShowScenes, navShowEnergy, navShowClimate)

    /** Placement-selector gate: may THIS group pin to the bar right now? */
    fun canPinToBar(alreadyOnBar: Boolean): Boolean {
        val roomNavTabs = roomPlacementsState.values.count { placementFromKey(it) == Placement.NAV_TAB }
        return canPinBarExtra(userTabs.count { it.pinned }, roomNavTabs, alreadyOnBar, barExtraSlotsNow())
    }

    /**
     * Whether a built-in tab can be switched on without pushing a pinned
     * tab or room off the bar: the extras already pinned must still fit.
     */
    fun canShowBuiltIn(security: Boolean, scenes: Boolean, energy: Boolean, climate: Boolean = navShowClimate): Boolean =
        barPinnedExtrasCount() <= barExtraSlots(security, scenes, energy, climate)

    fun setRoomPlacement(areaId: String, placement: Placement) {
        if (placement == Placement.NAV_TAB &&
            !canPinBarExtra(
                userTabs.count { it.pinned },
                roomPlacementsState.values.count { placementFromKey(it) == Placement.NAV_TAB },
                roomPlacement(areaId) == Placement.NAV_TAB,
                barExtraSlotsNow(),
            )
        ) return
        settingsStore.setRoomPlacement(areaId, placement.storageKey())
        roomPlacementsState = settingsStore.roomPlacements()
        bumpHomePrefs()
    }

    /** Bar/rail room tabs in stored room order (snapshot read — the transition spec reads this at transition time). */
    fun roomBarItemsSnapshot(): List<RoomBarItem> {
        val ordered = resolveOrder(settingsStore.roomOrder(), areas.value.map { it.id })
        val names = settingsStore.roomDisplayNames()
        val icons = settingsStore.roomIconKeys()
        return ordered.mapNotNull { areaId ->
            val area = areas.value.firstOrNull { it.id == areaId } ?: return@mapNotNull null
            if (roomPlacement(areaId) != Placement.NAV_TAB) return@mapNotNull null
            RoomBarItem(areaId, names[areaId] ?: area.name, icons[areaId])
        }
    }

    /**
     * Feedback round 19, item 5: the placement selector's single
     * write path for a tab. NAV_TAB maps to pinned (cap-checked,
     * chip/hidden cleared — placement is exclusive); ROOM_CHIP
     * maps to chip (pinned/hidden cleared); HIDDEN clears pinned
     * and chip and sets hidden (browse-list-only, exactly the old
     * unpinned tab's visibility). The old pinned+hidden
     * combination no longer has a selector value.
     */
    fun setTabPlacement(tabId: String, placement: Placement) {
        val tab = userTabs.firstOrNull { it.id == tabId } ?: return
        when (placement) {
            Placement.NAV_TAB -> {
                if (!canPinBarExtra(
                        userTabs.count { it.pinned },
                        roomPlacementsState.values.count { placementFromKey(it) == Placement.NAV_TAB },
                        tab.pinned,
                        barExtraSlotsNow(),
                    )
                ) return
                mutateTab(tabId) { it.copy(pinned = true, chip = false, hidden = false) }
            }
            Placement.ROOM_CHIP -> mutateTab(tabId) { it.copy(pinned = false, chip = true, hidden = false) }
            Placement.HIDDEN -> mutateTab(tabId) { it.copy(pinned = false, chip = false, hidden = true) }
        }
    }

    /** Feedback round 19, item 6: event notification toggles, persisted like navShowScenes. */
    private var notifyGarageState by mutableStateOf(settingsStore.notifyGarage)
    var notifyGarage: Boolean
        get() = notifyGarageState
        set(value) {
            notifyGarageState = value
            settingsStore.notifyGarage = value
        }

    private var notifyMotionState by mutableStateOf(settingsStore.notifyMotion)
    var notifyMotion: Boolean
        get() = notifyMotionState
        set(value) {
            notifyMotionState = value
            settingsStore.notifyMotion = value
        }

    private var notifyLightsState by mutableStateOf(settingsStore.notifyLights)
    var notifyLights: Boolean
        get() = notifyLightsState
        set(value) {
            notifyLightsState = value
            settingsStore.notifyLights = value
        }

    /** Feedback round 19, item 6: the notification manager (created here so MainActivity can ensure channels). */
    val notificationManager = HavenNotificationManager(getApplication(), settingsStore)

    /**
     * Feedback round 19, item 6: local actuation timestamps
     * (entity id -> epoch ms). Every Haven tap funnels through
     * [actuate], so a confirming on->off event within the window
     * is the user's own action, never an automation — the
     * light-shutoff rule suppresses exactly those. The window is
     * generous (10s) because slow round-trips must not leak a
     * self-tap notification; an automation firing on the same
     * light inside the window is the accepted tradeoff, and a
     * non-null parent id still notifies regardless.
     */
    private val localActuations = mutableMapOf<String, Long>()
    fun wasLocallyActuated(id: String, windowMs: Long = 10_000L): Boolean {
        val at = localActuations[id] ?: return false
        return System.currentTimeMillis() - at < windowMs
    }

    /**
     * Bumped whenever the Home editor saves, so the Today screen recomposes
     * from the store even when it is still in the composition. Returning
     * from the editor recomposes Today anyway; this covers every other path.
     */
    var homePrefsVersion by mutableStateOf(0)
        private set

    fun bumpHomePrefs() { homePrefsVersion++ }

    /**
     * Bumped whenever the shape picker saves, so HavenApp recomposes
     * its shape-preference provider from the store and every badge
     * and card outline updates immediately, without relaunching.
     */
    var shapePrefsVersion by mutableStateOf(0)
        private set

    fun bumpShapePrefs() { shapePrefsVersion++ }

    var shapePickerTarget by mutableStateOf<ShapePickerTarget?>(null)
        private set

    fun openShapePicker(entityId: String?, forCard: Boolean) { shapePickerTarget = ShapePickerTarget(entityId, forCard) }
    fun closeShapePicker() { shapePickerTarget = null }

    /**
     * Customization round, item 2: bumped whenever Card settings
     * saves, so every surface reading the customization maps
     * recomposes immediately (same pattern as shapePrefsVersion).
     */
    var cardCustomVersion by mutableStateOf(0)
        private set

    fun bumpCardCustom() { cardCustomVersion++ }

    /** The entity whose Card settings sheet is open, or null. */
    var cardSettingsTarget by mutableStateOf<String?>(null)
        private set

    fun openCardSettings(entityId: String) { cardSettingsTarget = entityId }
    fun closeCardSettings() { cardSettingsTarget = null }

    // ---- Connection switcher (feedback round 16, item 7) ----
    //
    // Two URL slots: the legacy instanceUrl (local when it
    // classifies local) and the pasted Nabu Casa remote-UI URL.
    // The row in Settings -> Connection shows Local vs Cloud for
    // the EFFECTIVE target (anything not a local address reads
    // Cloud), and tapping it switches mode + cloud URL. The same
    // HA token works through both URLs (remote UI forwards to
    // the same instance), so a switch never re-logs in.
    // AUTO means Wi-Fi -> local, off Wi-Fi -> cloud. Wi-Fi
    // TRANSPORT is the platform-allowed signal: SSID-specific
    // "home" detection would need location permission, so any
    // Wi-Fi counts as local-capable; if the local URL is
    // unreachable on a foreign Wi-Fi, pinning Local/Cloud in
    // the dialog overrides AUTO. Cloud-URL auto-detection from
    // HA's cloud integration is not exposed through any API the
    // app uses, so the cloud side is a paste field — no URL is
    // ever guessed or hardcoded from a screenshot.
    var connectionPrefsVersion by mutableStateOf(0)
        private set

    fun bumpConnectionPrefs() { connectionPrefsVersion++ }

    /** The stored local URL, if the legacy URL classifies local. */
    val connectionLocalUrl: String?
        get() = tokenStore.instanceUrl.takeIf { isLocalAddress(it) }

    /** The pasted cloud URL, else a cloud-classified legacy URL. */
    val connectionCloudUrl: String?
        get() = tokenStore.cloudInstanceUrl
            ?: tokenStore.instanceUrl.takeIf { !isLocalAddress(it) }

    val connectionMode: ConnectionMode
        get() = connectionModeFromKey(tokenStore.connectionModeKey)

    /** Wi-Fi transport only — no location permission involved. */
    fun isWifiConnected(): Boolean {
        return try {
            val cm = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    /** The URL the current mode + transport resolve to, if any. */
    fun connectionTarget(): String? = resolveConnectionTarget(
        connectionMode, connectionLocalUrl, connectionCloudUrl, isWifiConnected(),
    )

    /** (Local|Cloud, effective target) for the Settings row. */
    fun connectionDisplay(): Pair<String, String?> {
        val target = connectionTarget()
        return connectionLabel(target) to target
    }

    /** Last AUTO-resolved target: reconnects happen only on change. */
    private var lastAutoTarget: String? = null

    private val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = reevaluateAutoConnection()
        override fun onLost(network: Network) = reevaluateAutoConnection()
    }

    private fun reevaluateAutoConnection() {
        if (connectionMode != ConnectionMode.AUTO) return
        val target = connectionTarget() ?: return
        if (target == lastAutoTarget) return
        val token = tokenStore.accessToken ?: return
        lastAutoTarget = target
        client.connect(target, token)
    }

    /**
     * Saves the mode (+ a pasted cloud URL, when given) and
     * reconnects to the new effective target with the existing
     * token. A reconnect to the same URL is harmless; the dialog
     * is an explicit user action, so no change-guard here — the
     * guard lives in reevaluateAutoConnection, which must never
     * loop on transport flaps.
     */
    fun switchConnection(mode: ConnectionMode, cloudUrlInput: String?) {
        tokenStore.connectionModeKey = when (mode) {
            ConnectionMode.AUTO -> null
            ConnectionMode.LOCAL -> "local"
            ConnectionMode.CLOUD -> "cloud"
        }
        val cleanCloud = cloudUrlInput?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { normalizeInstanceUrl(it) }
        if (cleanCloud != null) tokenStore.cloudInstanceUrl = cleanCloud
        bumpConnectionPrefs()
        val target = connectionTarget()
        lastAutoTarget = target
        val token = tokenStore.accessToken
        if (target != null && token != null) client.connect(target, token)
    }

    val connection = client.connection
    val states = client.states
    val areas = client.areas
    val entityAreas = client.entityAreas
    val entityDevices = client.entityDevices
    val auxiliaryEntities = client.auxiliaryEntities
    val deviceInfo = client.deviceInfo

    /**
     * Visual-fidelity round §6: one entity's state as its own flow,
     * so a Home card recomposes ONLY when its own entity changes.
     * The dashboard used to collect the whole states map in
     * TodayScreen, so every entity update recomposed every card
     * and re-ran grouping/sorting over ~366 entities. Cards now
     * collect their own entityFlow (distinctUntilChanged: a new
     * map emission carrying the same entity object is a no-op),
     * while room MEMBERSHIP (area -> id lists) is memoized in
     * TodayScreen on (state keys, areas, entity areas, light
     * overrides) — membership recomputes only when the roster
     * changes, never on a value update. Flows are cached per id
     * (one SharedFlow per card id, WhileSubscribed so idle cards
     * cost nothing); the cache is bounded by the entity roster.
     */
    private val entityFlowCache = mutableMapOf<String, kotlinx.coroutines.flow.StateFlow<com.garrietta.haven.data.HaEntity?>>()

    fun entityFlow(id: String): kotlinx.coroutines.flow.StateFlow<com.garrietta.haven.data.HaEntity?> =
        entityFlowCache.getOrPut(id) {
            client.states.map { it[id] }.distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), client.states.value[id])
        }

    /**
     * Feedback round 18, item 7: one camera's motion sensor as its
     * own flow, so CameraQuadrant need not collect the whole states
     * map for the motion pill. Resolves via [motionSensorForCamera]
     * over the states + entityDevices flows; distinctUntilChanged
     * means a state emission that leaves this camera's sensor
     * untouched is a no-op. Cached per camera id like entityFlow.
     */
    private val motionSensorFlowCache = mutableMapOf<String, kotlinx.coroutines.flow.StateFlow<com.garrietta.haven.data.HaEntity?>>()

    fun motionSensorFlow(cameraId: String): kotlinx.coroutines.flow.StateFlow<com.garrietta.haven.data.HaEntity?> =
        motionSensorFlowCache.getOrPut(cameraId) {
            kotlinx.coroutines.flow.combine(client.states, client.entityDevices) { states, devices ->
                motionSensorForCamera(cameraId, states, devices)
            }.distinctUntilChanged()
                .stateIn(
                    viewModelScope, SharingStarted.WhileSubscribed(5000),
                    motionSensorForCamera(cameraId, client.states.value, client.entityDevices.value),
                )
        }
    val lovelace = client.lovelace
    val lovelaceError = client.lovelaceError
    val dashboards = client.dashboards
    val selectedDashboard = client.selectedDashboard

    /** Switch Boards to another dashboard (null = the default, sent with no url_path). */
    fun selectDashboard(path: String?) {
        client.preferredLovelacePath = path
        client.hasPreferredLovelacePath = true
        settingsStore.selectedDashboardPath = path
        settingsStore.hasSelectedDashboard = true
        client.refreshLovelace()
    }

    // ---- Entity popups + camera fullscreen ----
    // Round 21, item 5: the long-press settings sheet is gone —
    // the popupEntityId/openPopup/closePopup state that drove it
    // is deleted with it. Entity detail now opens only through
    // the backstack (navigate to Screen.EntityDetail).

    /** Camera id shown in the landscape fullscreen viewer, if any. */
    var fullscreenCameraId by mutableStateOf<String?>(null)
        private set

    fun openCameraFullscreen(entityId: String) { fullscreenCameraId = entityId }
    fun closeCameraFullscreen() { fullscreenCameraId = null }

    /**
     * The centered expressive thermostat pop-out (scalloped container,
     * radial dial, fan/mode underneath). One climate entity at a
     * time, closed by scrim tap, X, back, or the Details route
     * (which closes the pop-out and navigates to the full
     * thermostat page).
     */
    var climatePopoutId by mutableStateOf<String?>(null)
        private set

    fun openClimatePopout(entityId: String) { climatePopoutId = entityId }
    fun closeClimatePopout() { climatePopoutId = null }

    /**
     * Feedback round 3, item 6: the entity id from a Samsung
     * Device Controls tap, stored by MainActivity from the
     * provider's app intent (see appIntentFor). ControlDeepLinkRouter
     * in HavenApp consumes it once the entity's state has loaded
     * and routes to that entity's surface, then clears it. Null
     * means no pending deep link.
     */
    var controlDeepLinkEntityId by mutableStateOf<String?>(null)
        private set

    fun handleControlDeepLink(entityId: String?) {
        if (!entityId.isNullOrBlank()) controlDeepLinkEntityId = entityId
    }

    fun clearControlDeepLink() { controlDeepLinkEntityId = null }

    /** Camera id shown in the centered live-feed dialog (Home quadrant hero), if any. */
    var cameraDialogId by mutableStateOf<String?>(null)
        private set

    fun openCameraDialog(entityId: String) { cameraDialogId = entityId }
    fun closeCameraDialog() { cameraDialogId = null }

    /**
     * Front Door live probe (Round 2): ownership of the ring-mqtt
     * live-stream switch while the Front Door panel's Live View tab
     * is engaged. The switch is battery-dangerous on a 77% wireless
     * doorbell, so every exit turns it off: panel close, tab flip
     * to Event History, app background, and a watchdog cap. All
     * transitions log under the Haven tag with the FrontDoorLive
     * prefix so the device test can report the probe from logcat.
     * Idempotent by [frontDoorLiveEngaged]: a second disengage (the
     * panel's dismiss hook AND its dispose effect both fire) never
     * sends a duplicate turn_off, and a start while already engaged
     * never restarts the watchdog.
     */
    var frontDoorLiveEngaged by mutableStateOf(false)
        private set

    /**
     * Round 21, item 3: sticky in-memory proof that the RTSP
     * fallback streamed this session — set on the panel's RTSP
     * first frame, cleared on an RTSP error. The Front Door
     * quadrant reads LIVE for the fallback only while this is
     * true; before any proof it stays Recording. In-memory only
     * (never persisted): a fresh app launch must re-prove it.
     */
    var frontDoorRtspProven by mutableStateOf(false)
        private set

    fun markFrontDoorRtspProven() { frontDoorRtspProven = true }

    fun clearFrontDoorRtspProven() { frontDoorRtspProven = false }

    /**
     * Round 21, item 3: the ring-mqtt RTSP URL for the front door,
     * derived from the stored LOCAL instance URL. Null off-LAN
     * (cloud URL stored) — RTSP exists only on the Pi, so the
     * panel attempts it only when the device can reach the LAN.
     */
    val frontDoorRtspUrl: String?
        get() = frontDoorRtspUrl(connectionLocalUrl)

    /** The live stream's address using the path ring-mqtt reports, or the default path when it reports none. */
    fun frontDoorRtspUrlFor(path: String?): String? =
        frontDoorRtspUrl(connectionLocalUrl, path ?: com.garrietta.haven.data.FRONT_DOOR_RTSP_PATH)

    /** Why the live session last stopped, for the panel's paused UI. Null while never stopped. */
    var frontDoorLiveStopReason by mutableStateOf<String?>(null)
        private set

    private var frontDoorLiveWatchdog: Job? = null

    fun startFrontDoorLive() {
        if (frontDoorLiveEngaged) return
        frontDoorLiveEngaged = true
        frontDoorLiveStopReason = null
        Log.d("Haven", "FrontDoorLive: engage — switch.turn_on $FRONT_DOOR_LIVE_STREAM_SWITCH_ID")
        turnOn(FRONT_DOOR_LIVE_STREAM_SWITCH_ID)
        frontDoorLiveWatchdog?.cancel()
        frontDoorLiveWatchdog = viewModelScope.launch {
            delay(FRONT_DOOR_LIVE_MAX_MS)
            Log.w("Haven", "FrontDoorLive: watchdog cap reached — switch.turn_off $FRONT_DOOR_LIVE_STREAM_SWITCH_ID")
            disengageFrontDoorLive("watchdog-cap")
        }
    }

    fun disengageFrontDoorLive(reason: String) {
        if (!frontDoorLiveEngaged) return
        frontDoorLiveEngaged = false
        frontDoorLiveStopReason = reason
        frontDoorLiveWatchdog?.cancel()
        frontDoorLiveWatchdog = null
        Log.d("Haven", "FrontDoorLive: disengage ($reason) — switch.turn_off $FRONT_DOOR_LIVE_STREAM_SWITCH_ID")
        turnOff(FRONT_DOOR_LIVE_STREAM_SWITCH_ID)
    }

    /** App backgrounded (MainActivity.onStop): a backgrounded live session must never hold the switch. */
    fun onAppBackgrounded() {
        if (frontDoorLiveEngaged) disengageFrontDoorLive("background")
    }

    override fun onCleared() {
        // Round 16, item 7: drop the transport watcher.
        try {
            val cm = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.unregisterNetworkCallback(connectivityCallback)
        } catch (_: Exception) {
        }
        if (frontDoorLiveEngaged) {
            // Best effort: the process may be going away, so this is
            // fire-and-forget, never a reason to keep the ViewModel alive.
            try {
                client.callService("switch", "turn_off", FRONT_DOOR_LIVE_STREAM_SWITCH_ID)
            } catch (_: Exception) {
            }
        }
        super.onCleared()
    }

    /**
     * Travel snapshots (2026-10-30 verdict: "don't recreate
     * elements, physically move them"): the entity as it was when
     * its dialog opened (enter) or when its dismiss started
     * (return), so the traveling header/arc never recompute color,
     * shape, or text mid-flight from a states emission — and never
     * recompose on unrelated entities' state changes, which is
     * what keeps the flight fluid at 120Hz. Captured by the dialog
     * open effects and refreshed by the dismiss functions; cleared
     * when the dialog closes. The dialog body itself stays live.
     */
    val travelSnapshots = mutableStateMapOf<String, com.garrietta.haven.data.HaEntity>()

    fun captureTravelSnapshot(entityId: String) {
        states.value[entityId]?.let { travelSnapshots[entityId] = it }
    }

    fun clearTravelSnapshot(entityId: String) { travelSnapshots.remove(entityId) }
    fun clearAllTravelSnapshots() { travelSnapshots.clear() }

    /** The Home quadrant hero's cameras: the curated Cameras-tab set, from the caller's states snapshot. */
    fun heroCameras(states: Map<String, com.garrietta.haven.data.HaEntity>): List<String> = curatedCameras(states)

    /** Every camera the Security tab shows: the curated four first, then the rest by name. */
    fun securityCameraIds(states: Map<String, com.garrietta.haven.data.HaEntity>): List<String> {
        val curated = curatedCameras(states)
        val rest = states.values
            .filter { it.domain == "camera" && it.entityId !in excludedCameras && it.entityId !in curated }
            .sortedBy { it.friendlyName.lowercase() }
            .map { it.entityId }
        return curated + rest
    }

    /**
     * Room management (2026-10-30): thin wrappers over the HA
     * registry mutations. [onDone] receives null on success or
     * HA's error message on rejection, on the main thread.
     */
    fun refreshAreas() { client.refreshRegistries() }

    fun createRoom(name: String, onDone: (String?) -> Unit) {
        client.createArea(name) { ok, err ->
            viewModelScope.launch { onDone(if (ok) null else (err ?: "Home Assistant rejected the room.")) }
        }
    }

    fun renameRoom(areaId: String, name: String, onDone: (String?) -> Unit) {
        client.updateArea(areaId, name) { ok, err ->
            viewModelScope.launch { onDone(if (ok) null else (err ?: "Home Assistant rejected the rename.")) }
        }
    }

    fun deleteRoom(areaId: String, onDone: (String?) -> Unit) {
        client.deleteArea(areaId) { ok, err ->
            viewModelScope.launch { onDone(if (ok) null else (err ?: "Home Assistant rejected the delete.")) }
        }
    }

    fun assignEntityToRoom(entityId: String, areaId: String?, onDone: (String?) -> Unit) {
        client.updateEntityArea(entityId, areaId) { ok, err ->
            viewModelScope.launch { onDone(if (ok) null else (err ?: "Home Assistant rejected the assignment.")) }
        }
    }

    /**
     * Last live frame per camera, in memory only (never disk). The
     * fullscreen viewer shows the cached frame dimmed + "Connecting…" while
     * its stream starts or restarts, and instantly on returning to a camera
     * it has shown before. It never stands in for a dead stream: a failed
     * stream replaces it with an explicit error + Retry.
     */
    private val cameraFrameCache = com.garrietta.haven.data.CameraFrameCache()

    fun cacheCameraFrame(entityId: String, frame: android.graphics.Bitmap) {
        cameraFrameCache.put(entityId, frame)
    }

    fun lastCameraFrame(entityId: String): android.graphics.Bitmap? =
        cameraFrameCache.get(entityId)

    /**
     * In-app caches so returning to a page or tab never visibly
     * reloads (2026-10-29 verdict videos on run #60: "the plot should
     * be cached — most of this information should be cached in the
     * app"). History plots, camera snapshots, and timeline thumbnails
     * are fetched once, shown from cache on re-entry, and refreshed
     * silently in the background — the cached content stays on screen
     * while the refresh runs, never a shimmer or blank in place of
     * known data. A failed refresh never overwrites a good cache
     * (history keeps its last plottable series; bitmaps keep the last
     * decoded frame), so an outage degrades to slightly stale data,
     * never to a blank card. Entity states themselves already live in
     * [states] and were never re-fetched on return.
     */
    val historyCache = mutableStateMapOf<String, List<HistoryPoint>>()
    private val historyRefreshing = mutableSetOf<String>()

    fun historyFor(entityId: String): List<HistoryPoint>? = historyCache[entityId]

    fun refreshHistory(entityId: String) {
        if (entityId in historyRefreshing) return
        historyRefreshing += entityId
        viewModelScope.launch {
            try {
                val pts = client.fetchHistory(entityId, 24)
                if (pts.size >= 2 || !historyCache.containsKey(entityId)) {
                    historyCache[entityId] = pts
                }
            } finally {
                historyRefreshing -= entityId
            }
        }
    }

    val snapshotCache = mutableStateMapOf<String, android.graphics.Bitmap>()
    private val snapshotRefreshing = mutableSetOf<String>()
    /** Epoch millis of the image currently held for each camera (disk load or fresh fetch). */
    private val snapshotTimestamps = mutableMapOf<String, Long>()
    /** A cached snapshot younger than this is "fresh": no refetch (2026-11-01 polish round). */
    private val snapshotFreshMs = 60_000L

    /** Cache key: the entity id sanitized for use as a file name (disk and memory share it, so a disk-loaded image is found by [snapshotFor]). */
    private fun snapshotKey(entityId: String): String = entityId.replace(Regex("[^A-Za-z0-9_-]"), "_")

    fun snapshotFor(entityId: String): android.graphics.Bitmap? = snapshotCache[snapshotKey(entityId)]

    /**
     * Cache-first snapshot refresh (2026-11-01 polish round): a
     * fetch only starts when the held image is absent or older
     * than [snapshotFreshMs] — a fresh image is never swapped out
     * — and a finished image replaces the held one atomically, so
     * a failed or in-flight fetch can never blank a quadrant. On
     * success the image is also persisted to disk, so the NEXT
     * cold open paints last-known images immediately (see
     * [loadDiskSnapshots], run from init before first composition).
     */
    fun refreshSnapshot(entityId: String) {
        val key = snapshotKey(entityId)
        if (key in snapshotRefreshing) return
        val heldAt = snapshotTimestamps[key]
        if (snapshotCache.containsKey(key) && heldAt != null &&
            System.currentTimeMillis() - heldAt < snapshotFreshMs
        ) return
        snapshotRefreshing += key
        viewModelScope.launch {
            try {
                client.fetchCameraBitmap(entityId)?.let { bitmap ->
                    snapshotCache[key] = bitmap
                    snapshotTimestamps[key] = System.currentTimeMillis()
                    persistSnapshot(entityId, bitmap)
                }
            } finally {
                snapshotRefreshing -= key
            }
        }
    }

    private fun snapshotFile(entityId: String): java.io.File {
        val dir = java.io.File(getApplication<Application>().filesDir, "camera_snapshots")
        return java.io.File(dir, snapshotKey(entityId) + ".jpg")
    }

    /** Loads last-known snapshots from disk into the cache (best effort: a corrupt file is skipped, never fatal). */
    fun loadDiskSnapshots() {
        try {
            val dir = java.io.File(getApplication<Application>().filesDir, "camera_snapshots")
            if (!dir.isDirectory) return
            for (file in dir.listFiles() ?: return) {
                if (!file.name.endsWith(".jpg")) continue
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: continue
                    val key = file.nameWithoutExtension
                    snapshotCache[key] = bitmap
                    snapshotTimestamps[key] = file.lastModified()
                } catch (_: Exception) {
                    // Corrupt entry: skip it, keep the rest.
                }
            }
        } catch (_: Exception) {
            // Disk cache is best effort; a missing directory or permission
            // problem must never block startup.
        }
    }

    /**
     * Round 25, item 2: loads the cold-start state cache
     * (filesDir/haven_state_cache.json) into the client's flows
     * synchronously, before first composition. Best effort — a
     * missing or corrupt file means "no cache", never a crash.
     * Called from init right after loadDiskSnapshots().
     */
    private fun loadStateCache() {
        try {
            val file = java.io.File(getApplication<Application>().filesDir, "haven_state_cache.json")
            if (!file.isFile) return
            val data = parseStateCache(file.readText()) ?: return
            client.restoreCachedStates(data.states, data.areas, data.entityAreas)
        } catch (_: Exception) {
            // Disk cache is best effort; never block startup.
        }
    }

    /** Last state-cache write, for the 10s persist throttle below. */
    private var lastStateCachePersistMs: Long = 0L

    /**
     * Round 25, item 2: persists the latest states + areas +
     * entityAreas to haven_state_cache.json, throttled to at most
     * one write per 10 seconds (state_changed events arrive far
     * more often than the cache needs to move). Skips empty
     * states — a blank write would poison the next cold start.
     * Runs on Dispatchers.IO; best effort, never fatal.
     */
    private fun persistStateCacheThrottled(
        states: Map<String, com.garrietta.haven.data.HaEntity>,
        areas: List<com.garrietta.haven.data.HaArea>,
        entityAreas: Map<String, String>,
    ) {
        if (states.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastStateCachePersistMs < 10_000L) return
        lastStateCachePersistMs = now
        val text = serializeStateCache(states, areas, entityAreas)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(getApplication<Application>().filesDir, "haven_state_cache.json")
                file.writeText(text)
                // The widgets draw from this file: bring them up to date with it.
                com.garrietta.haven.widget.HavenWidgets.updateAll(getApplication())
            } catch (_: Exception) {
                // Disk cache is best effort.
            }
        }
    }

    private fun persistSnapshot(entityId: String, bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = snapshotFile(entityId)
                file.parentFile?.mkdirs()
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (_: Exception) {
                // Disk persistence is best effort; the in-memory image already updated.
            }
        }
    }

    val imageCache = mutableStateMapOf<String, android.graphics.Bitmap>()
    private val imageRefreshing = mutableSetOf<String>()

    fun imageFor(url: String): android.graphics.Bitmap? = imageCache[url]

    fun refreshImage(url: String) {
        if (url in imageRefreshing) return
        imageRefreshing += url
        viewModelScope.launch {
            try {
                client.fetchImageBitmap(url)?.let { imageCache[url] = it }
            } finally {
                imageRefreshing -= url
            }
        }
    }

    /**
     * On-screen bounds (window coordinates) of the tile, card, or sheet the
     * user just tapped to reach a full page. The page transition grows out
     * of this rect on the house spring and settles back toward it on back.
     * Set by the tapped item before navigating; cleared when the page is
     * left or the user jumps to another top-level tab.
     */
    var pageOrigin by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)

    var isAuthenticated by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    var pendingOAuthUrl by mutableStateOf<String?>(null)
        private set

    /** True while an OAuth code exchange is running (in-app WebView flow). */
    var oauthExchanging by mutableStateOf(false)
        private set

    // ---- Login pre-flight + discovery ----

    /** Local-network discovery (mDNS + subnet scan); results feed the login field. */
    val discovery = HaDiscovery(application)
    val discoveredInstances = discovery.found

    /** True while a pre-flight probe is running. */
    var probing by mutableStateOf(false)
        private set

    /** Confirmation line after a successful probe (e.g. "Found Home Assistant (2026.10) at …"). */
    var probeOk by mutableStateOf<String?>(null)
        private set

    /** True while a subnet scan is running. */
    var scanning by mutableStateOf(false)
        private set

    /** Plain-language outcome of the last subnet scan, if any. */
    var scanMessage by mutableStateOf<String?>(null)
        private set

    // ---- Dashboard editing (a deep copy of the Lovelace config, edited in-app) ----

    var editingViews by mutableStateOf<List<JSONObject>>(emptyList())
        private set
    var editingTitle by mutableStateOf("")
        private set

    init {
        moveCamerasToSecurity()
        // Last-known camera snapshots paint from the first frame:
        // load them before any composition runs (2026-11-01 polish
        // round). Best effort — see loadDiskSnapshots.
        loadDiskSnapshots()
        // Round 25, item 2: the state cache loads alongside the
        // snapshots, before first composition — TodayScreen,
        // heroCameras, and the camera quadrants paint cached data
        // on frame 0, and the socket updates silently behind.
        loadStateCache()
        // Round 25, item 2: persist the cache whenever the flows
        // move (the throttle inside keeps it to one write per 10s).
        viewModelScope.launch {
            combine(client.states, client.areas, client.entityAreas) { s, a, ea -> Triple(s, a, ea) }
                .collect { (s, a, ea) -> persistStateCacheThrottled(s, a, ea) }
        }
        // Dashboard choice must be wired before autoConnect: connect ->
        // auth_ok -> refreshLovelace reads the preference on its first run.
        client.preferredLovelacePath = settingsStore.selectedDashboardPath
        client.hasPreferredLovelacePath = settingsStore.hasSelectedDashboard
        client.onDashboardResolved = { path, has ->
            settingsStore.selectedDashboardPath = path
            settingsStore.hasSelectedDashboard = has
        }
        // UI rewrite round: starter-tab seeding is REMOVED — seeded tabs
        // used to push Settings off the pill bar. The Cameras curation
        // still runs on the first full states snapshot.
        viewModelScope.launch {
            val first = client.states.first { it.isNotEmpty() }
            maybeMigrateCameras(first)
        }
        // Round 16, item 7: seed the AUTO target BEFORE the
        // callback registers (its first fire is immediate and
        // must be a no-op, not a reconnect), then watch
        // transport changes for Wi-Fi <-> cellular flips.
        lastAutoTarget = connectionTarget()
        try {
            val cm = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.registerDefaultNetworkCallback(connectivityCallback)
        } catch (_: Exception) {
        }
        // Feedback round 19, item 6: feed parsed state_changed
        // events to the notification manager BEFORE connecting,
        // so no early event is missed. The VM's own actuation
        // timestamps suppress the user's own taps; the manager
        // owns channels, toggles, and the deep-link intent.
        client.stateChangedListener = { event ->
            notificationManager.onStateChanged(event, wasLocallyActuated(event.entityId))
        }
        // Round 25, item 1: auth rejections and transport drops
        // retry through the ViewModel — never sign out, never clear
        // tokens. Wired before autoConnect so no early failure is
        // missed.
        client.onAuthInvalid = { startAuthRetryLoop() }
        client.onTransportFailure = { handleTransportFailure() }
        autoConnect()
    }

    // ---- User tabs ----

    private fun mutateTab(tabId: String, f: (UserTab) -> UserTab) {
        userTabs = userTabs.map { if (it.id == tabId) f(it) else it }
        tabsStore.saveTabs(userTabs)
    }

    /** Creates a tab and returns its id, so the caller can navigate to it. */
    fun addTab(title: String): String {
        val id = "tab_${System.currentTimeMillis()}"
        // Feedback round 16, item 6: a new tab is auto-pinned
        // when a pin slot is free (cap 2, same rule as
        // setTabPinned). The "climate" tab was created fine but
        // unpinned, so it appeared nowhere the user looked —
        // not on the bar, and the empty tab screen reads as
        // nothing happening. Auto-pin puts it on the bar; the
        // Pin button still unpins it in one tap.
        // Feedback round 19, item 5: the cap counts bar extras
        // (pinned tabs + nav-tab rooms), not tabs alone.
        val pinned = shouldAutoPinNewTab(barPinnedExtrasCount())
        userTabs = userTabs + UserTab(id, title.trim().ifEmpty { "Tab" }, emptyList(), pinned = pinned)
        tabsStore.saveTabs(userTabs)
        return id
    }

    fun renameTab(tabId: String, title: String) {
        mutateTab(tabId) { it.copy(title = title.trim().ifEmpty { it.title }) }
    }

    fun deleteTab(tabId: String) {
        userTabs = userTabs.filter { it.id != tabId }
        tabsStore.saveTabs(userTabs)
    }

    /** Puts the tabs in the order of [ids]; a tab missing from [ids] keeps its place at the end. */
    fun setTabOrder(ids: List<String>) {
        val byId = userTabs.associateBy { it.id }
        val ordered = ids.mapNotNull { byId[it] } + userTabs.filter { it.id !in ids }
        if (ordered.map { it.id } == userTabs.map { it.id }) return
        userTabs = ordered
        tabsStore.saveTabs(userTabs)
    }

    fun moveTab(tabId: String, delta: Int) {
        val list = userTabs.toMutableList()
        val i = list.indexOfFirst { it.id == tabId }
        val j = (i + delta).coerceIn(list.indices)
        if (i < 0 || i == j) return
        val tab = list.removeAt(i)
        list.add(j, tab)
        userTabs = list
        tabsStore.saveTabs(userTabs)
    }

    fun addCardToTab(tabId: String, yaml: String) {
        mutateTab(tabId) { it.copy(cards = it.cards + yaml) }
    }

    // ---- Stage D favorites ----

    /** Ordered favorite entity ids, owned here so Home recomposes on change. */
    var favorites by mutableStateOf(settingsStore.favoriteEntityIds())
        private set

    fun addFavorite(entityId: String) {
        if (entityId in favorites) return
        favorites = favorites + entityId
        settingsStore.setFavoriteEntityIds(favorites)
    }

    fun removeFavorite(entityId: String) {
        favorites = favorites - entityId
        settingsStore.setFavoriteEntityIds(favorites)
    }

    /** Puts the favorites in the order of [ids], as a drag leaves them. */
    fun setFavoriteOrder(ids: List<String>) {
        if (ids.toSet() != favorites.toSet() || ids == favorites) return
        favorites = ids
        settingsStore.setFavoriteEntityIds(favorites)
    }

    fun moveFavorite(entityId: String, delta: Int) {
        val list = favorites.toMutableList()
        val i = list.indexOf(entityId)
        val j = (i + delta).coerceIn(list.indices)
        if (i < 0 || i == j) return
        val id = list.removeAt(i)
        list.add(j, id)
        favorites = list
        settingsStore.setFavoriteEntityIds(favorites)
    }

    // ---- Round 24, item 6: per-room entity order (drag reorder) ----

    /**
     * Per-room entity order, mirrored from the store so room
     * grids recompose on change. The render site resolves this
     * against the room's HA member list (resolveOrder), so a
     * stored order never drops a new member or keeps a removed
     * one. Null areaId = No room ("unassigned" key).
     */
    private var roomEntityOrdersState by mutableStateOf(settingsStore.roomEntityOrders())

    fun roomEntityOrder(areaId: String?): List<String> =
        roomEntityOrdersState[areaId ?: "unassigned"] ?: emptyList()

    /**
     * Moves [entityId] by [delta] positions within the room's
     * CURRENT display order ([orderedMemberIds], as rendered —
     * wide-media rows first, then the standard grid in row
     * order) and persists the result, so a dragged reorder
     * survives a restart. The stored order is over entity ids
     * only, so it survives a card moving between the
     * wide-media and standard sections (a play-state change).
     * No-op when the entity is unknown or the move would not
     * change its position.
     */
    fun moveRoomEntity(areaId: String?, orderedMemberIds: List<String>, entityId: String, delta: Int) {
        val moved = movedByDelta(orderedMemberIds, entityId, delta)
        if (moved === orderedMemberIds) return
        settingsStore.setRoomEntityOrder(areaId, moved)
        roomEntityOrdersState = settingsStore.roomEntityOrders()
    }

    // ---- Stage E badge preferences (persisting setters, like navStyle) ----

    private var badgeNavState by mutableStateOf(settingsStore.badgeNav)
    var badgeNav: Boolean
        get() = badgeNavState
        set(value) {
            badgeNavState = value
            settingsStore.badgeNav = value
        }

    private var badgeRoomState by mutableStateOf(settingsStore.badgeRoom)
    var badgeRoom: Boolean
        get() = badgeRoomState
        set(value) {
            badgeRoomState = value
            settingsStore.badgeRoom = value
        }

    // ---- Stage G reset customization ----

    /**
     * Restores presentation defaults WITHOUT deleting user content:
     * default Home sections shown (Greeting, Cameras, Home Status,
     * Favorites — the fresh-state default, not "show everything"),
     * canonical section order, room
     * presentation cleared (names/icons/order), navigation reset
     * (floating pill, automatic icons, all tabs shown), badge prefs
     * on. Favorites and tabs are CONTENT, not presentation, and are
     * never touched by a reset.
     */
    fun resetCustomization() {
        settingsStore.homeHiddenSections = DEFAULT_HIDDEN_HOME_SECTIONS
        settingsStore.setHomeSectionOrder(HOME_SECTION_ORDER_DEFAULT)
        settingsStore.setRoomOrder(emptyList())
        for (id in settingsStore.roomDisplayNames().keys.toList()) {
            settingsStore.setRoomDisplayName(id, null)
        }
        for (id in settingsStore.roomIconKeys().keys.toList()) {
            settingsStore.setRoomIconKey(id, null)
        }
        resetNavigation()
        badgeNav = true
        badgeRoom = true
        bumpHomePrefs()
    }

    // ---- Stage C navigation customization ----

    /** Picker icon key, or null for automatic. Unknown keys fall back to auto at render. */
    fun setTabIcon(tabId: String, iconKey: String?) {
        mutateTab(tabId) { it.copy(icon = iconKey) }
    }

    /** Hides the tab from the pill bar (it stays listed in TabManager). */
    fun setTabHidden(tabId: String, hidden: Boolean) {
        mutateTab(tabId) { it.copy(hidden = hidden) }
    }

    /**
     * Feedback round 15, item 3: pins the tab onto the bar/rail
     * (between Energy and Settings). At most [MAX_PINNED_TABS]
     * tabs may be pinned; pinning past the cap is ignored (the UI
     * disables the Pin action at the cap, this is the backstop).
     * Unpinning always succeeds. Pinned and hidden are independent:
     * a pinned tab shows on the bar even while hidden.
     */
    fun setTabPinned(tabId: String, pinned: Boolean) {
        // Feedback round 19, item 5: the cap counts bar extras
        // (pinned tabs + nav-tab rooms), not tabs alone.
        if (pinned && barPinnedExtrasCount() >= MAX_PINNED_TABS &&
            userTabs.firstOrNull { it.id == tabId }?.pinned != true
        ) return
        mutateTab(tabId) { it.copy(pinned = pinned) }
    }

    /** Feedback round 15, item 3: Scenes/Energy bar visibility, persisted like badgeNav. */
    private var navShowScenesState by mutableStateOf(settingsStore.navShowScenes)
    var navShowScenes: Boolean
        get() = navShowScenesState
        set(value) {
            navShowScenesState = value
            settingsStore.navShowScenes = value
        }

    private var navShowEnergyState by mutableStateOf(settingsStore.navShowEnergy)
    var navShowEnergy: Boolean
        get() = navShowEnergyState
        set(value) {
            navShowEnergyState = value
            settingsStore.navShowEnergy = value
        }

    // Never chosen: on when it fits beside what is already pinned, so an
    // update never pushes someone's pinned tab off their bar.
    private var navShowSecurityState by mutableStateOf(
        settingsStore.navShowSecurity
            ?: (barPinnedExtrasCount() <= barExtraSlots(true, settingsStore.navShowScenes, settingsStore.navShowEnergy)),
    )
    var navShowSecurity: Boolean
        get() = navShowSecurityState
        set(value) {
            navShowSecurityState = value
            settingsStore.navShowSecurity = value
        }

    // Never chosen: on when it fits beside Security and what is pinned.
    private var navShowClimateState by mutableStateOf(
        settingsStore.navShowClimate
            ?: (barPinnedExtrasCount() <= barExtraSlots(navShowSecurityState, settingsStore.navShowScenes, settingsStore.navShowEnergy, true)),
    )
    var navShowClimate: Boolean
        get() = navShowClimateState
        set(value) {
            navShowClimateState = value
            settingsStore.navShowClimate = value
        }

    /**
     * Folded pill style, owned here so TabManager writes recompose the
     * chrome. The setter persists too (a separate setNavStyle function
     * would clash with the property setter on the JVM), so callers
     * assign vm.navStyle directly.
     */
    private var navStyleState by mutableStateOf(navStyleFromKey(settingsStore.navStyle))
    var navStyle: NavStyle
        get() = navStyleState
        set(value) {
            navStyleState = value
            settingsStore.navStyle = value.key
        }

    /**
     * Resets navigation preferences to defaults WITHOUT deleting tabs:
     * floating style, automatic icons, all tabs shown, Scenes and
     * Energy back on the bar, all pins cleared (feedback round 15,
     * item 3: the reset is the default-bar restore). Tab deletion
     * stays an explicit per-tab action — a reset must never
     * destroy tabs.
     */
    fun resetNavigation() {
        navStyle = NavStyle.FLOATING
        navShowScenes = true
        navShowEnergy = true
        navShowSecurity = true
        navShowClimate = true
        // Feedback round 19, item 5: chip placements reset with
        // pins (both are bar/chip surfaces, not tab content).
        userTabs = userTabs.map { it.copy(icon = null, hidden = false, pinned = false, chip = false) }
        tabsStore.saveTabs(userTabs)
    }

    fun updateCardYaml(tabId: String, index: Int, yaml: String) {
        mutateTab(tabId) { tab ->
            if (index !in tab.cards.indices) tab
            else tab.copy(cards = tab.cards.toMutableList().also { it[index] = yaml })
        }
    }

    fun removeCardFromTab(tabId: String, index: Int) {
        mutateTab(tabId) { tab ->
            if (index !in tab.cards.indices) tab
            else tab.copy(cards = tab.cards.filterIndexed { i, _ -> i != index })
        }
    }

    fun moveCardInTab(tabId: String, from: Int, to: Int) {
        mutateTab(tabId) { tab ->
            if (from !in tab.cards.indices || to !in tab.cards.indices || from == to) tab
            else {
                val cards = tab.cards.toMutableList()
                val card = cards.removeAt(from)
                cards.add(to, card)
                tab.copy(cards = cards)
            }
        }
    }

    /** Re-serializes a card with a new type; false when the YAML won't parse (nothing saved). */
    fun changeTabCardType(tabId: String, index: Int, newType: String): Boolean {
        val tab = userTabs.firstOrNull { it.id == tabId } ?: return false
        val yaml = tab.cards.getOrNull(index) ?: return false
        return try {
            updateCardYaml(tabId, index, cardToYaml(changeCardType(parseCardYaml(yaml), newType)))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Camera entities that must never appear in the Cameras tab. */
    private val excludedCameras = setOf("camera.eufy_ae_c10_map", "camera.front_door_69a1")

    /**
     * The curated Cameras set, in tab order: Garage (Tapo C110),
     * Outdoor/Backyard (Tapo C402), Living Room, and one Ring — the
     * front_door_live_view, picked as the most reliable. The eufy vacuum
     * map is not a camera and the second Ring is out. Exact ids win where
     * they are known; everything else matches on the friendly name, never
     * on a guessed id.
     */
    private fun curatedCameras(states: Map<String, com.garrietta.haven.data.HaEntity>): List<String> {
        val cameras = states.values.filter { it.domain == "camera" && it.entityId !in excludedCameras }
        fun byId(id: String) = cameras.firstOrNull { it.entityId == id }?.entityId
        fun byName(vararg words: String) = cameras.firstOrNull { cam ->
            words.any { cam.friendlyName.contains(it, ignoreCase = true) }
        }?.entityId
        return listOfNotNull(
            byId("camera.tapo_2_live_view") ?: byName("garage"),
            byName("outdoor", "backyard"),
            byName("living"),
            byId("camera.front_door_live_view") ?: byName("ring", "doorbell"),
        ).distinct()
    }

    /**
     * One-time migration of the already-seeded Cameras tab to the curated
     * set. Changing the seed rule alone would do nothing for him: his tab
     * is already saved and the seeded flag is set. This runs once (guarded
     * by its own persisted flag), only rewrites an existing Cameras tab,
     * and never recreates one he deleted. Later edits in Manage tabs are
     * untouched — the flag is set the moment migration runs.
     */
    private fun maybeMigrateCameras(states: Map<String, com.garrietta.haven.data.HaEntity>) {
        if (tabsStore.camerasCurated) return
        val index = userTabs.indexOfFirst { it.id == "seed_cameras" || it.title == "Cameras" }
        if (index < 0) {
            // No Cameras tab to migrate (deleted, or never seeded):
            // consider it done so a later launch can't resurrect one.
            tabsStore.camerasCurated = true
            return
        }
        val curated = curatedCameras(states)
        if (curated.isEmpty()) return // Nothing matched yet; try again next launch.
        val cards = curated.map { "type: camera\nentity: $it\n" }
        if (userTabs[index].cards != cards) {
            mutateTab(userTabs[index].id) { it.copy(cards = cards) }
        }
        tabsStore.camerasCurated = true
    }

    // ---- Auth ----

    private fun autoConnect() {
        // Round 16, item 7: start on the mode-resolved target
        // (AUTO on home Wi-Fi -> local), falling back to the
        // legacy URL exactly as before when nothing resolves.
        val url = connectionTarget() ?: tokenStore.instanceUrl
        val token = tokenStore.accessToken
        if (url != null && token != null) {
            isAuthenticated = true
            if (tokenStore.refreshToken != null && tokenStore.tokenExpiresAtMs > 0 &&
                tokenStore.tokenExpiresAtMs < System.currentTimeMillis() + 60_000
            ) {
                refreshAndConnect(url)
            } else {
                client.connect(url, token)
            }
        }
    }

    fun loginWithToken(url: String, token: String) {
        val clean = normalizeInstanceUrl(url)
        // Round 16, item 7: slot the login URL without losing
        // the other side — a cloud login must not wipe a stored
        // local URL (switching back would strand the app), and
        // a local login replaces the local slot only. First-ever
        // login keeps the legacy field populated either way.
        if (isLocalAddress(clean)) {
            tokenStore.instanceUrl = clean
        } else {
            if (tokenStore.instanceUrl?.let { isLocalAddress(it) } != true) {
                tokenStore.instanceUrl = clean
            }
            tokenStore.cloudInstanceUrl = clean
        }
        bumpConnectionPrefs()
        tokenStore.accessToken = token
        tokenStore.refreshToken = null
        tokenStore.tokenExpiresAtMs = 0L
        authError = null
        isAuthenticated = true
        client.connect(clean, token)
    }

    /**
     * Pre-flights a typed or discovered address: normalizes it, then probes
     * <base>/manifest.json from inside the app. [onReady] runs with the
     * normalized URL only when the probe proves a Home Assistant is there;
     * otherwise [authError] carries the precise failure and nothing is launched.
     * Both the OAuth button and the token button go through this, so neither
     * can dump the user on a dead browser page or an opaque socket error.
     */
    fun preflight(rawUrl: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            probing = true
            authError = null
            probeOk = null
            val clean = normalizeInstanceUrl(rawUrl)
            val result = if (!isNetworkAvailable(getApplication())) {
                ProbeResult.NoNetwork
            } else {
                probeInstance(clean, OkHttpClient())
            }
            probing = false
            when (result) {
                is ProbeResult.Success -> {
                    probeOk = "Found Home Assistant" +
                        (result.version?.let { " ($it)" } ?: "") + " at $clean"
                    onReady(clean)
                }
                else -> authError = probeFailureMessage(result)
            }
        }
    }

    /** Scans the local subnet for Home Assistant; results land in [discoveredInstances]. */
    fun scanNetwork() {
        viewModelScope.launch {
            scanning = true
            scanMessage = null
            scanMessage = when (val outcome = discovery.scanSubnet(OkHttpClient())) {
                is ScanOutcome.Done -> if (outcome.found > 0) {
                    "Found ${outcome.found} instance${if (outcome.found == 1) "" else "s"}."
                } else {
                    "No Home Assistant found on this network. Check it's running and on the same Wi-Fi."
                }
                ScanOutcome.NeedsWifi -> "Discovery needs your home Wi-Fi — connect to it and try again."
                ScanOutcome.NoAddress -> "Couldn't read this phone's network address. Check Wi-Fi and try again."
            }
            scanning = false
        }
    }

    /** Opens the instance's OAuth authorize page in the system browser (PKCE S256). */
    fun startOAuth(url: String): Uri {
        val clean = normalizeInstanceUrl(url)
        tokenStore.instanceUrl = clean
        pendingOAuthUrl = clean
        val verifier = generateVerifier()
        tokenStore.pkceVerifier = verifier
        val state = generateVerifier().take(16)
        tokenStore.oauthState = state
        val authUrl = "$clean/auth/authorize" +
            "?response_type=code" +
            "&client_id=${URLEncoder.encode(OAUTH_CLIENT_ID, "UTF-8")}" +
            "&redirect_uri=${URLEncoder.encode(OAUTH_REDIRECT, "UTF-8")}" +
            "&state=${URLEncoder.encode(state, "UTF-8")}" +
            "&code_challenge=${URLEncoder.encode(codeChallenge(verifier), "UTF-8")}" +
            "&code_challenge_method=S256"
        return Uri.parse(authUrl)
    }

    fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "haven" || data.host != "auth-callback") return
        handleOAuthRedirect(data)
    }

    /**
     * Handles a haven://auth-callback redirect, whether it arrived via the
     * manifest intent-filter (external browser) or was intercepted from the
     * in-app authorize WebView. Verifies state, then exchanges the code.
     */
    fun handleOAuthRedirect(uri: Uri) {
        val storedState = tokenStore.oauthState
        if (storedState != null && uri.getQueryParameter("state") != storedState) {
            authError = "Sign-in response didn't match this request. Try again."
            return
        }
        val code = uri.getQueryParameter("code") ?: run {
            authError = uri.getQueryParameter("error") ?: "Sign-in was cancelled."
            return
        }
        val url = pendingOAuthUrl ?: tokenStore.instanceUrl ?: return
        exchangeCode(url, code)
    }

    private fun exchangeCode(url: String, code: String) {
        viewModelScope.launch {
            oauthExchanging = true
            val form = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", OAUTH_CLIENT_ID)
                .add("redirect_uri", OAUTH_REDIRECT)
            tokenStore.pkceVerifier?.let { form.add("code_verifier", it) }
            val ok = exchangeToken(url, form.build())
            oauthExchanging = false
            if (ok) {
                isAuthenticated = true
                client.connect(url, tokenStore.accessToken!!)
            } else {
                authError = "Could not finish sign-in. Try the long-lived token option."
            }
        }
    }

    /**
     * Round 25, item 1: the auth-retry loop's job handle. Only one
     * loop runs at a time — starting a new one (manual tap, fresh
     * auth_invalid) cancels the old. signOut() cancels it too, so a
     * retry can never reconnect after an explicit sign-out.
     */
    private var authRetryJob: Job? = null

    /** Round 25, item 1: last AUTO fallback switch, for the 30s cooldown. */
    private var lastFallbackAtMs: Long = 0L

    /**
     * Round 25, item 1: manual reconnect — the connection status
     * line under the greeting calls this, never signOut(). Attempts
     * a token refresh + reconnect immediately, without clearing
     * tokenStore; a transient failure keeps retrying with backoff
     * in the background (see [authRetryLoop]).
     */
    fun reconnectNow() {
        startAuthRetryLoop()
    }

    private fun startAuthRetryLoop() {
        authRetryJob?.cancel()
        authRetryJob = viewModelScope.launch { authRetryLoop() }
    }

    /**
     * Round 25, item 1: refresh + reconnect with backoff. Tries
     * the current target first; in AUTO, tries the alternate URL
     * before waiting. After MAX_AUTH_RETRIES failed attempts the
     * refresh token is presumed dead and the app surfaces "Your
     * session expired. Please sign in again." — the only path that
     * asks for a fresh sign-in, and only after ~5 minutes of
     * retries. Cancelled by signOut() and by a successful connect.
     * With no refresh token stored at all (long-lived-token
     * login), there is nothing to retry — it gives up immediately.
     */
    private suspend fun authRetryLoop() {
        if (tokenStore.refreshToken == null) {
            isAuthenticated = false
            authError = "Your session expired. Please sign in again."
            return
        }
        var attempt = 0
        while (true) {
            val target = connectionTarget() ?: tokenStore.instanceUrl ?: return
            if (tryRefresh(target)) {
                client.connect(target, tokenStore.accessToken!!)
                return
            }
            if (connectionMode == ConnectionMode.AUTO) {
                val alt = alternateConnectionTarget()
                if (alt != null && alt != target && tryRefresh(alt)) {
                    lastAutoTarget = alt
                    client.connect(alt, tokenStore.accessToken!!)
                    return
                }
            }
            attempt++
            if (attempt >= MAX_AUTH_RETRIES) {
                isAuthenticated = false
                authError = "Your session expired. Please sign in again."
                return
            }
            delay(authRetryDelayMs(attempt - 1))
        }
    }

    /** Round 25, item 1: one refresh-token exchange against [url]. False when no refresh token is stored. */
    private suspend fun tryRefresh(url: String): Boolean {
        val refresh = tokenStore.refreshToken ?: return false
        return exchangeToken(
            url,
            FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .add("client_id", OAUTH_CLIENT_ID)
                .build(),
        )
    }

    /** Round 25, item 1: the alternate URL for the current target (pure helper in Round16Logic). */
    private fun alternateConnectionTarget(): String? = alternateConnectionTarget(
        connectionMode, connectionLocalUrl, connectionCloudUrl, isWifiConnected(),
    )

    /**
     * Round 25, item 1: the socket dropped on the current URL.
     * In AUTO, switch to the alternate URL immediately (the same
     * token works through both) — at most once per 30 seconds, so
     * a flapping link cannot ping-pong between URLs. In
     * LOCAL/CLOUD the client's own same-URL backoff handles the
     * retry.
     */
    private fun handleTransportFailure() {
        if (connectionMode != ConnectionMode.AUTO) return
        val alt = alternateConnectionTarget() ?: return
        val current = connectionTarget() ?: return
        if (alt == current) return
        val now = System.currentTimeMillis()
        if (now - lastFallbackAtMs < 30_000L) return
        lastFallbackAtMs = now
        lastAutoTarget = alt
        val token = tokenStore.accessToken ?: return
        client.connect(alt, token)
    }

    private fun refreshAndConnect(url: String) {
        viewModelScope.launch {
            if (tryRefresh(url)) {
                client.connect(url, tokenStore.accessToken!!)
            } else {
                // Refresh failed; retry with backoff (+ the
                // alternate URL in AUTO) instead of stranding the
                // app in "Sign-in expired" — see authRetryLoop.
                startAuthRetryLoop()
            }
        }
    }

    private suspend fun exchangeToken(url: String, body: FormBody): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$url/auth/token").post(body).build()
            val resp = OkHttpClient().newCall(req).execute()
            val text = resp.body?.string() ?: return@withContext false
            if (!resp.isSuccessful) return@withContext false
            val json = JSONObject(text)
            val access = json.optString("access_token")
            if (access.isEmpty()) return@withContext false
            tokenStore.accessToken = access
            val refresh = json.optString("refresh_token")
            if (refresh.isNotEmpty()) tokenStore.refreshToken = refresh
            val expiresIn = json.optLong("expires_in", 0L)
            if (expiresIn > 0) tokenStore.tokenExpiresAtMs = System.currentTimeMillis() + expiresIn * 1000L
            true
        } catch (e: Exception) {
            false
        }
    }

    fun signOut() {
        // Round 25, item 1: the auth-retry loop must never
        // reconnect after an explicit sign-out — cancel it first.
        // signOut stays strictly on the Settings button; nothing
        // else in the app calls it.
        authRetryJob?.cancel()
        client.disconnect()
        tokenStore.clear()
        isAuthenticated = false
    }

    // ---- Service helpers (thin wrappers over call_service) ----

    /** The last service-call failure, surfaced as a banner so a tap can never fail silently again. */
    val serviceError = client.lastServiceError

    fun dismissServiceError() = client.clearServiceError()

    /**
     * Runs [call] with an optimistic local state flip, reverted if the call
     * fails. The next state_changed event reconciles the flip on success.
     * The flip is also recorded as a sticky expectation (see
     * OptimisticStateGuard): a stale or intermediate server event that
     * still says the old value must not visibly revert the tap — that is
     * what made off feel delayed while on felt instant.
     */
    private fun actuate(id: String, optimisticState: String, call: ((Boolean) -> Unit) -> Unit) {
        // Feedback round 19, item 6: every local tap funnels
        // through here, so the timestamp marks exactly the
        // user's own actuations for light-shutoff suppression.
        localActuations[id] = System.currentTimeMillis()
        val prev = client.states.value[id]?.state
        client.setLocalState(id, optimisticState)
        client.expectOptimistic(id, optimisticState)
        call { ok ->
            if (!ok && prev != null) {
                client.setLocalState(id, prev)
                client.clearOptimistic(id)
            }
        }
    }

    fun toggle(id: String) {
        val next = if (client.states.value[id]?.state == "on") "off" else "on"
        actuate(id, next) { done -> client.toggle(id, done) }
    }

    /**
     * Shape-tap actuation for covers: the optimistic state is the FINAL
     * state ("open"/"closed"), never the intermediate ("opening"/
     * "closing") — the OptimisticStateGuard drops any incoming state
     * that is not exactly the expected one while the window lives, so
     * an intermediate optimistic state would be dropped by the guard
     * itself and the confirming final state would arrive late. The
     * transient travel states are simply not shown during the window;
     * the final state confirms (or the window expires and later events
     * apply normally).
     */
    fun toggleCover(id: String) {
        val cur = client.states.value[id]?.state
        val next = if (cur == "open" || cur == "opening") "closed" else "open"
        val service = if (next == "open") "open_cover" else "close_cover"
        actuate(id, next) { done -> client.callService("cover", service, id, null, done) }
    }

    /** Shape-tap actuation for locks; same final-state rule as covers. */
    fun toggleLock(id: String) {
        val cur = client.states.value[id]?.state
        val next = if (cur == "locked" || cur == "locking") "unlocked" else "locked"
        val service = if (next == "locked") "lock" else "unlock"
        actuate(id, next) { done -> client.callService("lock", service, id, null, done) }
    }

    fun turnOn(id: String) = actuate(id, "on") { done ->
        client.callService(id.substringBefore("."), "turn_on", id, null, done)
    }

    /**
     * Stage F: scene activation. A DIRECT service call, not turnOn:
     * a scene's state is its last-activated timestamp, not on/off, so
     * actuate()'s optimistic "on" would paint a false state and fight
     * the guard. Reports Home Assistant's own success/failure via
     * [onDone] so the UI can show Sent/Failed honestly — never
     * "active", because scenes have no active state.
     */
    fun activateScene(entityId: String, onDone: (Boolean) -> Unit) {
        client.callService("scene", "turn_on", entityId, null, onDone)
    }

    fun turnOff(id: String) = actuate(id, "off") { done ->
        client.callService(id.substringBefore("."), "turn_off", id, null, done)
    }

    fun callService(domain: String, service: String, entityId: String? = null, data: JSONObject? = null) =
        client.callService(domain, service, entityId, data)

    fun setLightBrightness(id: String, pct: Int) =
        client.callService("light", "turn_on", id, JSONObject().put("brightness_pct", pct))

    fun setLightColorTemp(id: String, kelvin: Int) =
        client.callService("light", "turn_on", id, JSONObject().put("color_temp_kelvin", kelvin))

    fun setLightHsColor(id: String, hue: Float, saturation: Float) =
        client.callService("light", "turn_on", id, JSONObject().put("hs_color", JSONArray().put(hue).put(saturation)))

    fun setCoverPosition(id: String, position: Int) =
        client.callService("cover", "set_cover_position", id, JSONObject().put("position", position))

    fun setClimateTemperature(id: String, temperature: Double, hvacMode: String? = null) {
        val data = JSONObject().put("temperature", temperature)
        if (hvacMode != null) data.put("hvac_mode", hvacMode)
        client.callService("climate", "set_temperature", id, data)
    }

    /** Heat/cool pair version of [setClimateTemperature]. */
    fun setClimateTemperatureRange(id: String, low: Double, high: Double) {
        val data = JSONObject().put("target_temp_low", low).put("target_temp_high", high)
        client.callService("climate", "set_temperature", id, data)
    }

    fun setClimateHvacMode(id: String, mode: String) =
        client.callService("climate", "set_hvac_mode", id, JSONObject().put("hvac_mode", mode))

    fun setClimateFanMode(id: String, mode: String) =
        client.callService("climate", "set_fan_mode", id, JSONObject().put("fan_mode", mode))

    /** An ecobee comfort setting (Home, Away, Sleep) or any thermostat's preset. */
    fun setClimatePresetMode(id: String, preset: String) =
        client.callService("climate", "set_preset_mode", id, JSONObject().put("preset_mode", preset))

    // ---- Dashboard editing ----

    fun startEditing(config: LovelaceConfig) {
        editingTitle = config.title ?: "Dashboard"
        editingViews = config.views.map { view ->
            val copy = JSONObject()
            copy.put("title", view.title)
            if (view.path != null) copy.put("path", view.path)
            val cards = JSONArray()
            for (card in view.cards) cards.put(JSONObject(card.toString()))
            copy.put("cards", cards)
            copy
        }
    }

    fun addView(title: String) {
        val view = JSONObject().put("title", title).put("cards", JSONArray())
        editingViews = editingViews + view
    }

    fun removeView(index: Int) {
        if (index in editingViews.indices) editingViews = editingViews.filterIndexed { i, _ -> i != index }
    }

    fun addCard(viewIndex: Int, card: JSONObject) {
        if (viewIndex !in editingViews.indices) return
        editingViews[viewIndex].optJSONArray("cards")?.put(card)
        editingViews = editingViews.toList()
    }

    fun removeCard(viewIndex: Int, cardIndex: Int) {
        if (viewIndex !in editingViews.indices) return
        val cards = editingViews[viewIndex].optJSONArray("cards") ?: return
        if (cardIndex !in 0 until cards.length()) return
        val next = JSONArray()
        for (i in 0 until cards.length()) if (i != cardIndex) next.put(cards.get(i))
        editingViews[viewIndex].put("cards", next)
        editingViews = editingViews.toList()
    }

    fun moveCard(viewIndex: Int, from: Int, to: Int) {
        if (viewIndex !in editingViews.indices) return
        val cards = editingViews[viewIndex].optJSONArray("cards") ?: return
        if (from !in 0 until cards.length() || to !in 0 until cards.length() || from == to) return
        val list = (0 until cards.length()).map { cards.get(it) }.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        val next = JSONArray()
        for (item in list) next.put(item)
        editingViews[viewIndex].put("cards", next)
        editingViews = editingViews.toList()
    }

    fun buildEditingConfig(): JSONObject {
        val views = JSONArray()
        for (view in editingViews) views.put(view)
        return JSONObject().put("title", editingTitle).put("views", views)
    }

    fun saveEditing(onDone: (Boolean) -> Unit) {
        client.saveLovelace(buildEditingConfig(), onDone)
    }

    // ---- Creation (guided builders) ----

    /** Automations/scripts/scenes are saved over REST, not WebSocket. */
    fun saveAutomation(config: JSONObject, onDone: (Boolean) -> Unit) {
        val slug = slugify(config.optString("alias"))
        config.put("id", slug)
        client.saveRestConfig("automation", slug, config, onDone)
    }

    fun saveScript(config: JSONObject, onDone: (Boolean) -> Unit) {
        val slug = slugify(config.optString("alias"))
        config.put("id", slug)
        client.saveRestConfig("script", slug, config, onDone)
    }

    fun saveScene(config: JSONObject, onDone: (Boolean) -> Unit) {
        val slug = slugify(config.optString("name"))
        config.put("id", slug)
        client.saveRestConfig("scene", slug, config, onDone)
    }

    /** Helpers ARE WebSocket storage-collection commands: `<domain>/create`. */
    fun saveHelper(type: String, config: JSONObject, onDone: (Boolean) -> Unit) {
        client.sendCommand("$type/create", build = {
            val keys = config.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                it.put(k, config.get(k))
            }
        }, onResult = { msg -> onDone(msg.optBoolean("success", false)) })
    }

    private fun slugify(text: String): String {
        val slug = text.lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
            .trim('_').replace(Regex("_+"), "_")
        return slug.ifEmpty { "haven_item" }
    }

    private fun generateVerifier(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
        )
    }

    private fun codeChallenge(verifier: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return android.util.Base64.encodeToString(
            digest,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
        )
    }

    companion object {
        /** GitHub Pages site that declares haven://auth-callback as a redirect URI. */
        const val OAUTH_CLIENT_ID = "https://garrietta.github.io/Haven/"
        const val OAUTH_REDIRECT = "haven://auth-callback"
    }
}

/** Convenience: is the connection live right now? */
fun ConnectionState.isLive(): Boolean = this is ConnectionState.Connected
