package com.garrietta.haven

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.HaClient
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
    /** A user-created tab; Settings is the only built-in tab. */
    data class UserTab(val tabId: String) : Screen()
    data object TabManager : Screen()
    data class EntityDetail(val entityId: String) : Screen()
    data class AreaDetail(val areaId: String) : Screen()
    data object DashboardEdit : Screen()
    data object AutomationBuilder : Screen()
    data object SceneBuilder : Screen()
    data object ScriptBuilder : Screen()
    data object HelperBuilder : Screen()
}

class HavenViewModel(application: Application) : AndroidViewModel(application) {

    val tokenStore = TokenStore(application)
    val settingsStore = SettingsStore(application)
    val tabsStore = TabsStore(application)
    val client = HaClient(viewModelScope)

    /** User-created tabs, in pill-bar order. Loaded synchronously so the app can open on the first tab. */
    var userTabs by mutableStateOf<List<UserTab>>(tabsStore.loadTabs())
        private set

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
        // every tab must not get them back on the next launch.
        viewModelScope.launch {
            val first = client.states.first { it.isNotEmpty() }
            maybeSeedTabs(first)
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
     * (a fresh user with none of these entities gets no tabs, just Settings).
     * Cameras take ALL camera-domain entities, so the Ring front door is
     * included automatically; no entity id is guessed, only matched.
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
        val cameras = states.values.filter { it.domain == "camera" }.map { it.entityId }
        if (cameras.isNotEmpty()) {
            tabs.add(UserTab("seed_cameras", "Cameras", cameras.map { card("camera", it) }))
        }
        if (tabs.isNotEmpty()) {
            userTabs = tabs
            tabsStore.saveTabs(tabs)
        }
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
     */
    private fun actuate(id: String, optimisticState: String, call: ((Boolean) -> Unit) -> Unit) {
        val prev = client.states.value[id]?.state
        client.setLocalState(id, optimisticState)
        call { ok ->
            if (!ok && prev != null) client.setLocalState(id, prev)
        }
    }

    fun toggle(id: String) {
        val next = if (client.states.value[id]?.state == "on") "off" else "on"
        actuate(id, next) { done -> client.toggle(id, done) }
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

    fun setClimateHvacMode(id: String, mode: String) =
        client.callService("climate", "set_hvac_mode", id, JSONObject().put("hvac_mode", mode))

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
