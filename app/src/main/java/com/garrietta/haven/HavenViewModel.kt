package com.garrietta.haven

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.HaClient
import com.garrietta.haven.data.HistoryPoint
import com.garrietta.haven.data.HaDiscovery
import com.garrietta.haven.data.LovelaceConfig
import com.garrietta.haven.data.ProbeResult
import com.garrietta.haven.data.ScanOutcome
import com.garrietta.haven.data.SettingsStore
import com.garrietta.haven.data.TabsStore
import com.garrietta.haven.data.UserTab
import com.garrietta.haven.data.cardToYaml
import com.garrietta.haven.data.changeCardType
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.data.TokenStore
import com.garrietta.haven.data.isNetworkAvailable
import com.garrietta.haven.data.normalizeInstanceUrl
import com.garrietta.haven.data.probeFailureMessage
import com.garrietta.haven.data.probeInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    data object Dashboards : Screen()
    data object Areas : Screen()
    data object Search : Screen()
    data object Settings : Screen()
    /** A user-created tab; Home and Settings are the built-in tabs. */
    data class UserTab(val tabId: String) : Screen()
    data object TabManager : Screen()
    data object HomeEditor : Screen()
    /** Rooms & areas manager (Settings): create/rename/delete HA areas and assign entities to them. */
    data object RoomManager : Screen()
    data class EntityDetail(val entityId: String) : Screen()
    /** Full-page thermostat experience (hero dial, range bar, pills). */
    data class Thermostat(val entityId: String) : Screen()
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
    val client = HaClient(viewModelScope)

    /** User-created tabs, in pill-bar order. Loaded synchronously so the app can open on the first tab. */
    var userTabs by mutableStateOf<List<UserTab>>(tabsStore.loadTabs())
        private set

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

    val connection = client.connection
    val states = client.states
    val areas = client.areas
    val entityAreas = client.entityAreas
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

    /** Entity id shown in the more-info bottom sheet, if any. */
    var popupEntityId by mutableStateOf<String?>(null)
        private set

    fun openPopup(entityId: String) { popupEntityId = entityId }
    fun closePopup() { popupEntityId = null }

    /** Camera id shown in the landscape fullscreen viewer, if any. */
    var fullscreenCameraId by mutableStateOf<String?>(null)
        private set

    fun openCameraFullscreen(entityId: String) { fullscreenCameraId = entityId }
    fun closeCameraFullscreen() { fullscreenCameraId = null }

    /**
     * The centered expressive thermostat pop-out (scalloped container,
     * radial dial, fan/mode underneath). Mirrors popupEntityId: one
     * climate entity at a time, closed by scrim tap, X, back, or the
     * Details route (which closes the pop-out and navigates to the
     * full thermostat page).
     */
    var climatePopoutId by mutableStateOf<String?>(null)
        private set

    fun openClimatePopout(entityId: String) { climatePopoutId = entityId }
    fun closeClimatePopout() { climatePopoutId = null }

    /** Camera id shown in the centered live-feed dialog (Home quadrant hero), if any. */
    var cameraDialogId by mutableStateOf<String?>(null)
        private set

    fun openCameraDialog(entityId: String) { cameraDialogId = entityId }
    fun closeCameraDialog() { cameraDialogId = null }

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

    fun snapshotFor(entityId: String): android.graphics.Bitmap? = snapshotCache[entityId]

    fun refreshSnapshot(entityId: String) {
        if (entityId in snapshotRefreshing) return
        snapshotRefreshing += entityId
        viewModelScope.launch {
            try {
                client.fetchCameraBitmap(entityId)?.let { snapshotCache[entityId] = it }
            } finally {
                snapshotRefreshing -= entityId
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
        // Dashboard choice must be wired before autoConnect: connect ->
        // auth_ok -> refreshLovelace reads the preference on its first run.
        client.preferredLovelacePath = settingsStore.selectedDashboardPath
        client.hasPreferredLovelacePath = settingsStore.hasSelectedDashboard
        client.onDashboardResolved = { path, has ->
            settingsStore.selectedDashboardPath = path
            settingsStore.hasSelectedDashboard = has
        }
        // Starter tabs seed once, from the first full states snapshot. The
        // guard is the persisted flag, not the tab list: a user who deletes
        // every tab must not get them back on the next launch. The Cameras
        // curation runs right after seeding, on the same snapshot.
        viewModelScope.launch {
            val first = client.states.first { it.isNotEmpty() }
            maybeSeedTabs(first)
            maybeMigrateCameras(first)
        }
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
        userTabs = userTabs + UserTab(id, title.trim().ifEmpty { "Tab" }, emptyList())
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

    /**
     * Garrett's starter tabs, seeded once from his instance's entities — this
     * is his personal initial configuration, not defaults for fresh users
     * (a fresh user with none of these entities gets no tabs, just Home and
     * Settings). Cameras take the curated set (see [curatedCameras]), never
     * every camera-domain entity; no entity id is guessed, only matched.
     */
    private fun maybeSeedTabs(states: Map<String, com.garrietta.haven.data.HaEntity>) {
        if (tabsStore.seeded) return
        tabsStore.seeded = true
        if (userTabs.isNotEmpty()) return
        fun card(type: String, entityId: String) = "type: $type\nentity: $entityId\n"
        val tabs = mutableListOf<UserTab>()
        val climate = states["climate.thermostat"] ?: states.values.firstOrNull { it.domain == "climate" }
        if (climate != null) {
            val cards = mutableListOf(card("thermostat", climate.entityId))
            for (sensorId in listOf("sensor.thermostat_current_temperature", "sensor.thermostat_current_humidity")) {
                if (states.containsKey(sensorId)) cards.add(card("sensor", sensorId))
            }
            tabs.add(UserTab("seed_thermostat", "Thermostat", cards))
        }
        val garage = states.values.firstOrNull { it.domain == "cover" && it.friendlyName.contains("garage", ignoreCase = true) }
            ?: states.values.firstOrNull { it.domain == "cover" }
        if (garage != null) {
            tabs.add(UserTab("seed_garage", "Garage Door", listOf(card("tile", garage.entityId))))
        }
        val livingLights = listOf("light.lamp_socket_1", "light.standing_lamp_socket_1")
            .filter { states.containsKey(it) }
            .ifEmpty {
                states.values.filter { it.domain == "light" && it.friendlyName.contains("living", ignoreCase = true) }
                    .map { it.entityId }
            }
        if (livingLights.isNotEmpty()) {
            tabs.add(UserTab("seed_living_lights", "Living Room lights", livingLights.map { card("light", it) }))
        }
        val cameras = curatedCameras(states)
        if (cameras.isNotEmpty()) {
            tabs.add(UserTab("seed_cameras", "Cameras", cameras.map { card("camera", it) }))
        }
        if (tabs.isNotEmpty()) {
            userTabs = tabs
            tabsStore.saveTabs(tabs)
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
        val url = tokenStore.instanceUrl
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
        tokenStore.instanceUrl = clean
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

    private fun refreshAndConnect(url: String) {
        viewModelScope.launch {
            val ok = exchangeToken(
                url,
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", tokenStore.refreshToken ?: "")
                    .add("client_id", OAUTH_CLIENT_ID)
                    .build(),
            )
            if (ok) {
                client.connect(url, tokenStore.accessToken!!)
            } else {
                // Refresh failed; fall back to asking for sign-in again.
                isAuthenticated = false
                authError = "Your session expired. Please sign in again."
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
