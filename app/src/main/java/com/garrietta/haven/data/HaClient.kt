package com.garrietta.haven.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Talks to Home Assistant over its WebSocket API (auth, states, events, services,
 * registries, Lovelace) plus REST for history and camera snapshots.
 *
 * Command strings follow developers.home-assistant.io/docs/api/websocket. The
 * config-panel save commands live in [ConfigCommands] so they can be corrected in
 * one place if Home Assistant ever renames them.
 */
object ConfigCommands {
    const val LOVE_LACE_GET = "lovelace/config"
    const val LOVE_LACE_LIST = "lovelace/dashboards/list"
    const val LOVE_LACE_SAVE = "lovelace/config/save"
    const val TODO_CREATE = "todo/item_create"
    const val TODO_UPDATE = "todo/item_update"
    const val TODO_DELETE = "todo/item_delete"
}

/**
 * Decodes a JPEG at (at least) [targetWidth] pixels wide using a
 * power-of-two inSampleSize, or at full size when [targetWidth] <= 0.
 * Used for camera snapshots fetched over the network and for the
 * last-known snapshots loaded from disk on a cold start.
 */
fun decodeJpegScaled(bytes: ByteArray, targetWidth: Int): Bitmap? {
    if (bytes.isEmpty()) return null
    return try {
        if (targetWidth <= 0) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = snapshotInSampleSize(bounds.outWidth, bounds.outHeight, targetWidth)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}

/**
 * Reliability round (2026-11-26) — the connection engine.
 *
 * A session is a [Link]: one WebSocket to one endpoint, with its own
 * generation number. Every listener callback carries its link, so a
 * replaced or abandoned socket can never clobber the live one. (The
 * old client kept a single `socket` field: a URL switch opened a
 * second socket without closing the first, and when the first later
 * failed it nulled the NEW socket, flagged an error over a healthy
 * session, and scheduled a third.)
 *
 * The client is given an ordered endpoint list (see
 * [connectionCandidates]): it tries each in turn with a per-endpoint
 * connect timeout, backs off between full passes (pausing while the
 * phone is offline), and once connected through a fallback it
 * re-probes the preferred endpoint and switches back
 * make-before-break: the new socket authenticates first, the old one
 * keeps routing in-flight results for [LINK_DRAIN_MS], then closes.
 *
 * auth_invalid never triggers a reconnect from here — retrying a
 * rejected token feeds Home Assistant's IP ban. The owner refreshes
 * the token and calls [connect] again via [onAuthInvalid].
 */
class HaClient(
    private val scope: CoroutineScope,
    baseHttp: OkHttpClient = sharedHttp,
) {
    /** REST client: the shared pool plus a 401 handler that renews the OAuth token once. */
    private val http: OkHttpClient = baseHttp.newBuilder()
        .authenticator(RefreshingAuthenticator())
        .build()

    /** MJPEG proxy streams: no read timeout — the frame watchdog owns stall detection. */
    private val streamHttp: OkHttpClient by lazy {
        http.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    }

    /** Reachability probe for the preferred endpoint: answers in well under a second on a LAN. */
    private val probeHttp: OkHttpClient by lazy {
        baseHttp.newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _activeUrl = MutableStateFlow<String?>(null)

    /**
     * The endpoint the live, authenticated session runs on — the truth
     * the Settings row and the Local/Cloud label show (the old label
     * showed the *preferred* target, so a silent cloud fallback still
     * read "Local"). Null while not connected.
     */
    val activeUrl: StateFlow<String?> = _activeUrl.asStateFlow()

    private val _states = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    val states: StateFlow<Map<String, HaEntity>> = _states.asStateFlow()

    /**
     * Round 25, item 2: restores the cold-start disk cache into the
     * flows before first composition, so TodayScreen, heroCameras,
     * and the camera quadrants have last-known data on frame 0.
     * Only non-empty parts are applied (a partial cache must never
     * blank a part that already arrived). The live get_states and
     * registry fetches overwrite wholesale once connected.
     */
    fun restoreCachedStates(
        states: Map<String, HaEntity>,
        areas: List<HaArea>,
        entityAreas: Map<String, String>,
        entityDevices: Map<String, String> = emptyMap(),
        deviceInfo: Map<String, HaDeviceInfo> = emptyMap(),
        auxiliary: Set<String> = emptySet(),
    ) {
        if (states.isNotEmpty()) _states.value = states
        if (areas.isNotEmpty()) _areas.value = areas
        if (entityAreas.isNotEmpty()) _entityAreas.value = entityAreas
        if (entityDevices.isNotEmpty()) _entityDevices.value = entityDevices
        if (deviceInfo.isNotEmpty()) _deviceInfo.value = deviceInfo
        if (auxiliary.isNotEmpty()) _auxiliaryEntities.value = auxiliary
    }

    /**
     * Feedback round 19, item 6: one parsed `state_changed`
     * event per applied state update, for the notification
     * manager. Fires ONLY when the event is applied to [_states]:
     * entity removals (new_state null) and optimistic-guard
     * drops (stale echoes of the user's own tap) never notify.
     * Set by the ViewModel before connecting; null means nobody
     * listens (the state flow itself is untouched either way).
     */
    var stateChangedListener: ((StateChangeEvent) -> Unit)? = null

    /**
     * The live session's token was rejected (auth_invalid). The owner
     * renews it and calls [connect]; the client never retries a
     * rejected token, never signs out, and never clears tokens.
     * Invoked on an OkHttp thread.
     */
    var onAuthInvalid: (() -> Unit)? = null

    /**
     * A live session dropped (not a failed attempt, not a manual
     * close). The client is already re-running its endpoint pass;
     * this is informational. Invoked on an OkHttp thread.
     */
    var onTransportFailure: (() -> Unit)? = null

    /**
     * Renews the access token after a REST 401. Called on an OkHttp
     * worker thread and allowed to block; receives the token the
     * failed request carried and returns a fresh one, or null when no
     * renewal is possible.
     */
    @Volatile
    var tokenRefresher: ((staleToken: String) -> String?)? = null

    private val _areas = MutableStateFlow<List<HaArea>>(emptyList())
    val areas: StateFlow<List<HaArea>> = _areas.asStateFlow()

    /** entity_id -> area_id, resolved from the entity registry then the device registry. */
    private val _entityAreas = MutableStateFlow<Map<String, String>>(emptyMap())
    val entityAreas: StateFlow<Map<String, String>> = _entityAreas.asStateFlow()

    /**
     * entity_id -> device_id, from the entity registry (expressive-
     * menus round, item 7). Previously kept only as the private
     * registryEntityDevices map for manufacturer/area resolution;
     * exposing it lets camera cards find a motion sensor on the
     * SAME device — a genuine registry link, never a name guess.
     * Empty until the entity registry arrives: empty means
     * unknown, never "no motion sensor".
     */
    /**
     * Entities Home Assistant files as configuration or diagnostics, or
     * has hidden: kept out of rooms and lists of what a room holds.
     */
    private val _auxiliaryEntities = MutableStateFlow<Set<String>>(emptySet())
    val auxiliaryEntities: StateFlow<Set<String>> = _auxiliaryEntities.asStateFlow()

    private val _entityDevices = MutableStateFlow<Map<String, String>>(emptyMap())
    val entityDevices: StateFlow<Map<String, String>> = _entityDevices.asStateFlow()

    /**
     * Customization round, item 3: device-registry info by device
     * id — name (name_by_user wins over name, matching HA's own
     * display rule), model, manufacturer. Additive: nothing about
     * the existing area/manufacturer publishing changes. Empty
     * until the device registry arrives; empty means unknown, never
     * "no devices".
     */
    private val _deviceInfo = MutableStateFlow<Map<String, HaDeviceInfo>>(emptyMap())
    val deviceInfo: StateFlow<Map<String, HaDeviceInfo>> = _deviceInfo.asStateFlow()

    /**
     * Latest registry parts, kept so each fetch can recompute the
     * map independently (2026-10-30 verdict round): entity-registry
     * areas, entity→device links, device-registry areas.
     */
    @Volatile private var registryEntityAreas: Map<String, String?> = emptyMap()
    @Volatile private var registryEntityDevices: Map<String, String> = emptyMap()
    @Volatile private var registryDeviceAreas: Map<String, String?> = emptyMap()
    private val deviceInfoById: MutableMap<String, HaDeviceInfo> = ConcurrentHashMap()

    private val _lovelace = MutableStateFlow<LovelaceConfig?>(null)
    val lovelace: StateFlow<LovelaceConfig?> = _lovelace.asStateFlow()

    /** The dashboard this client loads/saves, resolved by [refreshLovelace]. */
    var lovelaceUrlPath: String? = null
        private set

    /** Every dashboard the instance lists, for the Boards picker. */
    private val _dashboards = MutableStateFlow<List<HaDashboard>>(emptyList())
    val dashboards: StateFlow<List<HaDashboard>> = _dashboards.asStateFlow()

    /** The dashboard entry that produced the loaded config, if it was a listed one. */
    private val _selectedDashboard = MutableStateFlow<HaDashboard?>(null)
    val selectedDashboard: StateFlow<HaDashboard?> = _selectedDashboard.asStateFlow()

    /** Dashboard the user last picked (persisted by the ViewModel); tried first. */
    var preferredLovelacePath: String? = null
    var hasPreferredLovelacePath: Boolean = false

    /**
     * Called with (urlPath, hasSelection) whenever a dashboard loads, so the
     * ViewModel can persist the choice. hasSelection is always true here: a
     * null urlPath means "the default dashboard" won, which is a real choice,
     * not "unset" — the distinction matters on the next launch.
     */
    var onDashboardResolved: ((String?, Boolean) -> Unit)? = null

    private val _lovelaceError = MutableStateFlow<String?>(null)
    val lovelaceError: StateFlow<String?> = _lovelaceError.asStateFlow()

    /**
     * The last service-call failure, if any. A failed tap used to vanish
     * silently (fire-and-forget with no result callback and a send() that
     * ignored its own return value); now every failure lands here so the UI
     * can show it, and every call is logged under the Haven tag so logcat
     * proves what left the phone.
     */
    private val _lastServiceError = MutableStateFlow<String?>(null)
    val lastServiceError: StateFlow<String?> = _lastServiceError.asStateFlow()

    fun clearServiceError() {
        _lastServiceError.value = null
    }

    /** Immediate local state flip for optimistic UI; the next state_changed event reconciles it. */
    fun setLocalState(entityId: String, newState: String) {
        _states.update { current ->
            val old = current[entityId] ?: return@update current
            current + (entityId to old.copy(state = newState))
        }
    }

    /**
     * Sticky optimistic flips (see OptimisticStateGuard): a tap's expected
     * state survives stale or intermediate server events that still say
     * the old value, so off feels exactly as instant as on. The ViewModel
     * records the expectation beside every optimistic flip and clears it
     * when the service call fails.
     */
    private val optimisticGuard = OptimisticStateGuard()

    fun expectOptimistic(entityId: String, state: String) = optimisticGuard.expect(entityId, state)

    fun clearOptimistic(entityId: String) = optimisticGuard.clear(entityId)

    /**
     * Device-registry manufacturer per entity, resolved in afterAuth.
     * State payloads never carry it, so every entity built from a payload
     * (get_states, state_changed) gets it re-attached here — otherwise the
     * first event after connect would wipe the manufacturer signal that
     * Ring detection relies on (see isRingCamera).
     */
    @Volatile private var manufacturerByEntity: Map<String, String> = emptyMap()

    private fun attachManufacturer(entity: HaEntity): HaEntity {
        val maker = manufacturerByEntity[entity.entityId] ?: return entity
        return entity.copy(manufacturer = maker)
    }

    /** Re-attaches manufacturers to every currently held entity (runs when the device registry lands). */
    private fun reattachManufacturers() {
        if (manufacturerByEntity.isEmpty()) return
        _states.update { current -> current.mapValues { (_, entity) -> attachManufacturer(entity) } }
    }

    // ---- Connection engine -------------------------------------------------

    /** One WebSocket to one endpoint. Every callback carries its link. */
    private class Link(val gen: Int, val url: String) {
        @Volatile var ws: WebSocket? = null
        @Volatile var authed = false
        @Volatile var rejected = false

        /** Replaced or closed on purpose: it may still route results, nothing else. */
        @Volatile var retired = false
        var down = false // guarded by HaClient.lock
        var timeoutJob: Job? = null

        fun send(obj: JSONObject): Boolean = try {
            ws?.send(obj.toString()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private class PendingCall(val gen: Int, val onResult: (JSONObject) -> Unit)

    private val lock = Any()
    private val linkGen = AtomicInteger(0)
    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, PendingCall>()

    // All guarded by [lock].
    private var endpoints: List<String> = emptyList()
    private var attemptIndex = 0
    private var failedPasses = 0
    private var primary: Link? = null
    private var upgradeLink: Link? = null
    private val draining = mutableSetOf<Link>()
    private var reconnectJob: Job? = null
    private var upgradeJob: Job? = null

    @Volatile private var manualDisconnect = true
    @Volatile private var offline = false

    /** Set by auth_invalid; only a fresh [connect] or [updateToken] clears it. */
    @Volatile private var authRejected = false

    /** Base URL for REST and players: the authenticated endpoint, else the preferred one. */
    @Volatile private var instanceUrl: String = ""

    @Volatile private var token: String = ""

    /** A token the instance refused over REST; REST pauses until the token changes (no 401 storms). */
    @Volatile private var restRejectedToken: String? = null

    private fun restAllowed(): Boolean {
        val rejected = restRejectedToken
        return instanceUrl.isNotEmpty() && (rejected == null || rejected != token)
    }

    /** The current access token (REST + players read it per request). */
    fun currentToken(): String = token

    /**
     * Swaps in a renewed access token without touching the socket —
     * Home Assistant validates the token only at auth time, so a live
     * session survives renewal. REST calls pick the new token up at
     * once. Also clears an earlier auth rejection.
     */
    fun updateToken(accessToken: String) {
        if (accessToken.isEmpty()) return
        token = accessToken
        restRejectedToken = null
        authRejected = false
    }

    /** Connects to one endpoint (login, explicit retry). */
    fun connect(url: String, accessToken: String) = connect(listOf(url), accessToken)

    /**
     * Starts a fresh session over [candidates], first = preferred.
     * Whatever link exists is retired (an authenticated one drains its
     * in-flight results first), backoff resets, and the pass starts
     * immediately.
     */
    fun connect(candidates: List<String>, accessToken: String) {
        val list = normalizeEndpoints(candidates)
        if (list.isEmpty() || accessToken.isEmpty()) return
        val old: Link?
        val oldUpgrade: Link?
        synchronized(lock) {
            manualDisconnect = false
            authRejected = false
            token = accessToken
            restRejectedToken = null
            endpoints = list
            if (primary?.authed != true) instanceUrl = list.first()
            reconnectJob?.cancel()
            reconnectJob = null
            upgradeJob?.cancel()
            upgradeJob = null
            failedPasses = 0
            old = primary
            primary = null
            oldUpgrade = upgradeLink
            upgradeLink = null
        }
        _activeUrl.value = null
        old?.let { retireLink(it, drain = it.authed) }
        oldUpgrade?.let { retireLink(it, drain = false) }
        startPass()
    }

    /**
     * New candidate order (network change, foreground, detected URLs)
     * WITHOUT tearing down a healthy session: a live link on the new
     * preferred endpoint is kept; a live link on a now-secondary
     * endpoint stays up while the preferred one is probed; a live link
     * on an endpoint no longer listed is replaced. With no live link
     * the pass restarts immediately, which is what lets a returning
     * network reconnect at once instead of waiting out the backoff.
     */
    fun updateEndpoints(candidates: List<String>, accessToken: String? = null) {
        val list = normalizeEndpoints(candidates)
        if (list.isEmpty()) return
        if (!accessToken.isNullOrEmpty() && accessToken != token) updateToken(accessToken)
        val p: Link?
        val changed: Boolean
        synchronized(lock) {
            if (manualDisconnect) return
            changed = list != endpoints
            endpoints = list
            p = primary
        }
        when {
            p != null && p.authed -> when {
                p.url !in list -> connect(list, token)
                p.url != list.first() -> startUpgradeProbe(immediate = true)
                else -> stopUpgradeProbe()
            }
            p != null && !p.retired -> {
                // An attempt is in flight: restart only when it targets
                // something other than the new preferred endpoint.
                if (changed && p.url != list.first()) restartPassNow()
            }
            else -> if (!authRejected) restartPassNow()
        }
    }

    /** Shown while the owner renews a rejected token (the status line reads "Reconnecting…"). */
    fun markRenewingSignIn() {
        if (_connection.value !is ConnectionState.Connected) {
            _connection.value = ConnectionState.Error("Renewing sign-in…", retrying = true)
        }
    }

    /** Status-line tap / manual retry: verify a live session, or start a fresh pass now. */
    fun reconnectNow() {
        if (manualDisconnect || authRejected) return
        val p = synchronized(lock) { primary }
        when {
            p != null && p.authed -> ensureAlive()
            p != null && !p.retired -> Unit // an attempt is already running
            else -> restartPassNow()
        }
    }

    private fun restartPassNow() {
        val inFlight: Link?
        synchronized(lock) {
            if (manualDisconnect) return
            reconnectJob?.cancel()
            reconnectJob = null
            failedPasses = 0
            inFlight = primary?.takeIf { !it.authed }
            if (inFlight != null) primary = null
        }
        inFlight?.let { retireLink(it, drain = false) }
        startPass()
    }

    /**
     * Connectivity from the platform callback. Going offline keeps a
     * live link (it may survive a blip — pings decide) but stops
     * scheduled retries; coming back online retries at once.
     */
    fun setNetworkAvailable(available: Boolean) {
        val wasOffline = offline
        offline = !available
        if (!available) {
            val idle = synchronized(lock) {
                val noLink = primary == null
                if (noLink) {
                    reconnectJob?.cancel()
                    reconnectJob = null
                }
                noLink
            }
            if (idle && !manualDisconnect && !authRejected) {
                _connection.value = ConnectionState.Error("Waiting for a network connection", retrying = true)
            }
            return
        }
        if (wasOffline) reconnectNow()
    }

    /**
     * Foreground check: a socket that slept through Doze can look
     * connected while its TCP flow is long dead. An application ping
     * that gets no pong within [LIVENESS_PONG_TIMEOUT_MS] forces a
     * fresh pass; with no live link a pass starts now.
     */
    fun ensureAlive() {
        if (manualDisconnect || authRejected) return
        val link = synchronized(lock) { primary }
        if (link == null || link.retired) {
            reconnectNow()
            return
        }
        if (!link.authed) return
        val answered = AtomicBoolean(false)
        val id = nextId.getAndIncrement()
        val sent = dispatch(id, JSONObject().put("id", id).put("type", "ping")) { answered.set(true) }
        if (!sent) return
        scope.launch {
            delay(LIVENESS_PONG_TIMEOUT_MS)
            val stillPrimary = synchronized(lock) { primary === link && !link.down }
            if (!answered.get() && stillPrimary) {
                Log.w(TAG, "no pong within ${LIVENESS_PONG_TIMEOUT_MS}ms on ${hostLabel(link.url)}; reconnecting")
                cancelSocket(link)
                onLinkDown(link, "Connection stalled")
            }
        }
    }

    fun disconnect() {
        val links: List<Link>
        synchronized(lock) {
            manualDisconnect = true
            reconnectJob?.cancel()
            reconnectJob = null
            upgradeJob?.cancel()
            upgradeJob = null
            links = listOfNotNull(primary, upgradeLink) + draining
            primary = null
            upgradeLink = null
            draining.clear()
        }
        for (link in links) {
            link.retired = true
            link.timeoutJob?.cancel()
            try {
                link.ws?.close(1000, "bye")
            } catch (e: Exception) {
                // Already gone.
            }
            failPending(link.gen, "Disconnected")
        }
        _activeUrl.value = null
        _connection.value = ConnectionState.Disconnected
    }

    private fun normalizeEndpoints(candidates: List<String>): List<String> =
        candidates.map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }.distinct()

    private fun wsUrlFor(base: String): String {
        val wsBase = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> "ws://$base"
        }
        return "$wsBase/api/websocket"
    }

    private fun hostLabel(url: String): String = connectionHostOf(url) ?: "?"

    private fun startPass() {
        synchronized(lock) {
            if (manualDisconnect || endpoints.isEmpty()) return
            attemptIndex = 0
        }
        openPrimary()
    }

    private fun openPrimary() {
        val link: Link
        synchronized(lock) {
            if (manualDisconnect || endpoints.isEmpty()) return
            val url = endpoints[attemptIndex.coerceIn(0, endpoints.size - 1)]
            link = Link(linkGen.incrementAndGet(), url)
            primary = link
        }
        _connection.value = ConnectionState.Connecting
        Log.d(TAG, "connect attempt #${link.gen} -> ${hostLabel(link.url)}")
        openLink(link)
        armHandshakeTimeout(link)
    }

    private fun armHandshakeTimeout(link: Link) {
        link.timeoutJob = scope.launch {
            delay(endpointConnectTimeoutMs(link.url) + AUTH_HANDSHAKE_TIMEOUT_MS)
            if (!link.authed) {
                Log.w(TAG, "attempt #${link.gen} to ${hostLabel(link.url)} timed out")
                cancelSocket(link)
                onLinkDown(link, "Timed out reaching ${hostLabel(link.url)}")
            }
        }
    }

    private fun cancelSocket(link: Link) {
        try {
            link.ws?.cancel()
        } catch (e: Exception) {
            // Already gone.
        }
    }

    private fun openLink(link: Link) {
        val request = try {
            Request.Builder().url(wsUrlFor(link.url)).build()
        } catch (e: IllegalArgumentException) {
            null
        }
        if (request == null) {
            scope.launch { onLinkDown(link, "Invalid address") }
            return
        }
        val client = http.newBuilder()
            .connectTimeout(endpointConnectTimeoutMs(link.url), TimeUnit.MILLISECONDS)
            .pingInterval(WS_PING_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .build()
        link.ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(link, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Answer the server's close so onClosed fires promptly.
                try {
                    webSocket.close(1000, null)
                } catch (e: Exception) {
                    // Already closing.
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onLinkDown(link, t.message ?: "Connection failed")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onLinkDown(link, "Connection closed")
            }
        })
    }

    /** One link went away. Only the primary link's loss drives reconnection. */
    private fun onLinkDown(link: Link, reason: String) {
        var role = ROLE_STALE
        synchronized(lock) {
            if (link.down) return
            link.down = true
            draining.remove(link)
            when {
                link === primary -> {
                    primary = null
                    role = ROLE_PRIMARY
                }
                link === upgradeLink -> {
                    upgradeLink = null
                    role = ROLE_UPGRADE
                }
            }
        }
        link.timeoutJob?.cancel()
        failPending(link.gen, reason)
        if (role != ROLE_PRIMARY) return
        if (link.authed) _activeUrl.value = null
        if (manualDisconnect || link.retired || link.rejected) return
        if (link.authed) {
            Log.w(TAG, "session on ${hostLabel(link.url)} dropped: $reason")
            stopUpgradeProbe()
            _connection.value = ConnectionState.Error(reason, retrying = true)
            onTransportFailure?.invoke()
            startPass()
        } else {
            Log.d(TAG, "attempt #${link.gen} to ${hostLabel(link.url)} failed: $reason")
            advanceOrBackoff(reason)
        }
    }

    /** Next candidate right away; after a full failed pass, back off (or wait for the network). */
    private fun advanceOrBackoff(reason: String) {
        val delayMs: Long
        synchronized(lock) {
            if (manualDisconnect) return
            if (attemptIndex + 1 < endpoints.size) {
                attemptIndex++
                delayMs = -1L
            } else {
                failedPasses++
                delayMs = reconnectBackoffMs(failedPasses - 1, Math.random())
            }
        }
        if (delayMs < 0) {
            openPrimary()
            return
        }
        _connection.value = ConnectionState.Error(
            if (offline) "Waiting for a network connection" else reason,
            retrying = true,
        )
        if (offline) return
        val job = scope.launch {
            delay(delayMs)
            startPass()
        }
        synchronized(lock) {
            reconnectJob?.cancel()
            reconnectJob = job
        }
    }

    private fun onPrimaryAuthed(link: Link) {
        val onFallback: Boolean
        synchronized(lock) {
            if (primary !== link) return
            failedPasses = 0
            reconnectJob?.cancel()
            reconnectJob = null
            onFallback = endpoints.firstOrNull()?.let { it != link.url } ?: false
        }
        instanceUrl = link.url
        _activeUrl.value = link.url
        _connection.value = ConnectionState.Connected
        Log.d(TAG, "connected via ${hostLabel(link.url)}" + if (onFallback) " (fallback)" else "")
        afterAuth()
        if (onFallback) startUpgradeProbe() else stopUpgradeProbe()
    }

    /** The upgrade link authenticated: it becomes the session; the old one drains. */
    private fun promote(link: Link) {
        val old: Link?
        synchronized(lock) {
            if (manualDisconnect || upgradeLink !== link) return
            old = primary
            primary = link
            upgradeLink = null
            attemptIndex = endpoints.indexOf(link.url).coerceAtLeast(0)
            reconnectJob?.cancel()
            reconnectJob = null
        }
        Log.d(TAG, "switched to preferred endpoint ${hostLabel(link.url)}")
        old?.let { retireLink(it, drain = it.authed) }
        onPrimaryAuthed(link)
    }

    /** Retires [link]: a drained link keeps routing results for [LINK_DRAIN_MS], then closes. */
    private fun retireLink(link: Link, drain: Boolean) {
        link.retired = true
        link.timeoutJob?.cancel()
        if (!drain) {
            cancelSocket(link)
            failPending(link.gen, "Connection replaced")
            return
        }
        synchronized(lock) { draining.add(link) }
        scope.launch {
            delay(LINK_DRAIN_MS)
            try {
                link.ws?.close(1000, "switched")
            } catch (e: Exception) {
                // Already gone.
            }
            synchronized(lock) { draining.remove(link) }
            failPending(link.gen, "Connection replaced")
        }
    }

    private fun startUpgradeProbe(immediate: Boolean = false) {
        synchronized(lock) {
            if (upgradeJob?.isActive == true && !immediate) return
            upgradeJob?.cancel()
            upgradeJob = null
        }
        val job = scope.launch {
            if (!immediate) delay(PREFERRED_ENDPOINT_PROBE_MS)
            while (isActive) {
                var preferred: String? = null
                var done = false
                synchronized(lock) {
                    val p = primary
                    val first = endpoints.firstOrNull()
                    if (manualDisconnect || p == null || !p.authed || first == null || p.url == first) {
                        done = true
                    } else if (upgradeLink == null) {
                        preferred = first
                    }
                }
                if (done) break
                val target = preferred
                if (target != null && !offline && probeReachable(target)) openUpgrade(target)
                delay(PREFERRED_ENDPOINT_PROBE_MS)
            }
        }
        synchronized(lock) { upgradeJob = job }
    }

    private fun stopUpgradeProbe() {
        val pendingUpgrade: Link?
        synchronized(lock) {
            upgradeJob?.cancel()
            upgradeJob = null
            pendingUpgrade = upgradeLink
            upgradeLink = null
        }
        pendingUpgrade?.let { retireLink(it, drain = false) }
    }

    private fun openUpgrade(url: String) {
        val link: Link
        synchronized(lock) {
            if (manualDisconnect || upgradeLink != null) return
            link = Link(linkGen.incrementAndGet(), url)
            upgradeLink = link
        }
        Log.d(TAG, "preferred endpoint ${hostLabel(url)} answers; opening upgrade link #${link.gen}")
        openLink(link)
        armHandshakeTimeout(link)
    }

    /** Cheap, unauthenticated check that [url] is a reachable Home Assistant (its PWA manifest). */
    private suspend fun probeReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$url/manifest.json").header("Accept", "application/json").build()
            probeHttp.newCall(req).execute().use { resp ->
                resp.isSuccessful && resp.body?.string()?.contains("Home Assistant", ignoreCase = true) == true
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Fails every pending call that was sent on link [gen] (asynchronously, never inline). */
    private fun failPending(gen: Int, reason: String) {
        val ids = pending.entries.filter { it.value.gen == gen }.map { it.key }
        if (ids.isEmpty()) return
        val removed = ids.mapNotNull { id -> pending.remove(id)?.let { id to it.onResult } }
        if (removed.isEmpty()) return
        scope.launch(Dispatchers.Default) {
            for ((id, cb) in removed) deliverFailure(id, cb, reason)
        }
    }

    private fun failAsync(id: Int, cb: (JSONObject) -> Unit, reason: String) {
        scope.launch(Dispatchers.Default) { deliverFailure(id, cb, reason) }
    }

    private fun deliverFailure(id: Int, cb: (JSONObject) -> Unit, reason: String) {
        val msg = JSONObject()
            .put("id", id)
            .put("type", "result")
            .put("success", false)
            .put("error", JSONObject().put("code", NOT_CONNECTED).put("message", reason))
        try {
            cb(msg)
        } catch (e: Exception) {
            Log.w(TAG, "result callback failed: ${e.message}")
        }
    }

    /**
     * Sends [obj] on the live, authenticated session. A command that
     * cannot leave the phone fails its callback (asynchronously) with
     * error code [NOT_CONNECTED], so no caller waits on a reply that
     * can never come — the old client left such callbacks pending
     * forever, which is why the camera viewers needed blind timeouts.
     */
    private fun dispatch(id: Int, obj: JSONObject, onResult: ((JSONObject) -> Unit)?): Boolean {
        val link = synchronized(lock) { primary?.takeIf { it.authed && !it.retired } }
        if (link == null) {
            if (onResult != null) failAsync(id, onResult, "Not connected")
            return false
        }
        if (onResult != null) pending[id] = PendingCall(link.gen, onResult)
        if (!link.send(obj)) {
            pending.remove(id)?.let { failAsync(id, it.onResult, "Not connected") }
            return false
        }
        return true
    }

    /** Sends a command and routes its `result` message to [onResult]. Returns the id. */
    fun sendCommand(type: String, build: (JSONObject) -> Unit = {}, onResult: ((JSONObject) -> Unit)? = null): Int {
        val id = nextId.getAndIncrement()
        val obj = JSONObject().put("id", id).put("type", type)
        build(obj)
        dispatch(id, obj, onResult)
        return id
    }

    private fun handleMessage(link: Link, text: String) {
        val msg = try {
            JSONObject(text)
        } catch (e: Exception) {
            return
        }
        val type = msg.optString("type")
        // Results and pongs route by id from ANY link — a draining link
        // still completes the calls that were sent on it.
        if (type == "result" || type == "pong") {
            pending.remove(msg.optInt("id", -1))?.onResult?.invoke(msg)
            return
        }
        val role = synchronized(lock) {
            when {
                link === primary -> ROLE_PRIMARY
                link === upgradeLink -> ROLE_UPGRADE
                else -> ROLE_STALE
            }
        }
        if (role == ROLE_STALE || link.retired) return
        when (type) {
            "auth_required" -> {
                if (role == ROLE_PRIMARY) _connection.value = ConnectionState.Authenticating
                link.send(JSONObject().put("type", "auth").put("access_token", token))
            }
            "auth_ok" -> {
                link.authed = true
                link.timeoutJob?.cancel()
                if (role == ROLE_UPGRADE) promote(link) else onPrimaryAuthed(link)
            }
            "auth_invalid" -> {
                link.rejected = true
                link.timeoutJob?.cancel()
                try {
                    link.ws?.close(1000, "auth_invalid")
                } catch (e: Exception) {
                    // Already gone.
                }
                if (role == ROLE_PRIMARY) {
                    Log.w(TAG, "token rejected by ${hostLabel(link.url)}")
                    authRejected = true
                    synchronized(lock) {
                        reconnectJob?.cancel()
                        reconnectJob = null
                    }
                    _connection.value = ConnectionState.Error("Sign-in rejected. Check the token or sign in again.")
                    onAuthInvalid?.invoke()
                }
            }
            "event" -> if (role == ROLE_PRIMARY && link.authed) handleEvent(msg)
        }
    }

    private fun handleEvent(msg: JSONObject) {
        // Round 5 Bug B: a WebRTC subscription event,
        // correlated by the command id it answers. The old
        // code only handled state_changed here, so the SDP
        // answer event was silently dropped — which is why
        // the device read "empty answer": the answer was
        // never empty, it never reached the callback. An
        // answer event delivers (main thread) and closes
        // the subscription; a candidate event keeps it open
        // (the session sends a complete offer, so server
        // trickle candidates are logged, not applied —
        // there is no remote-candidate entry point on the
        // session; if ICE fails on device, that is the
        // next gap to close, not this routing).
        val webrtcId = msg.optInt("id", -1)
        val webrtcSub = webrtcEventHandlers[webrtcId]
        if (webrtcSub != null) {
            val parsed = parseWebRtcEventAnswer(msg, webrtcSub.sessionId)
            if (parsed != null) {
                webrtcEventHandlers.remove(webrtcId)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    webrtcSub.onAnswer(parsed)
                }
            } else {
                Log.d(TAG, "FrontDoorLive: webrtc event (non-answer) for subscription $webrtcId")
            }
            return
        }
        val event = msg.optJSONObject("event") ?: return
        if (event.optString("event_type") != "state_changed") return
        val data = event.optJSONObject("data") ?: return
        val entityId = data.optString("entity_id")
        if (entityId.isEmpty()) return
        val oldState = data.optJSONObject("old_state")
        val newState = data.optJSONObject("new_state")
        if (newState == null) {
            optimisticGuard.clear(entityId)
            _states.update { it - entityId }
            return
        }
        val incoming = newState.optString("state")
        // A contradicting event while a tap's expectation
        // is live is stale or intermediate: drop it so the
        // optimistic flip the user already saw is not
        // reverted for a second before the confirming
        // event arrives. A matching event confirms the
        // flip (the guard clears itself) and applies.
        if (optimisticGuard.shouldDrop(entityId, incoming)) return
        val entity = attachManufacturer(HaEntity.fromJson(newState))
        _states.update { it + (entityId to entity) }
        // Feedback round 19, item 6: applied
        // events only (see stateChangedListener).
        // parent_id lives on new_state.context
        // (null = direct change, non-null =
        // automation/script). device_class and
        // friendly_name come from attributes.
        val attributes = newState.optJSONObject("attributes")
        val context = newState.optJSONObject("context")
        stateChangedListener?.invoke(
            StateChangeEvent(
                entityId = entityId,
                domain = entityId.substringBefore("."),
                oldState = oldState?.optString("state")?.ifEmpty { null },
                newState = incoming,
                deviceClass = attributes?.text("device_class")?.ifEmpty { null },
                friendlyName = attributes?.text("friendly_name")?.ifEmpty { null },
                parentId = context?.text("parent_id")?.ifEmpty { null },
            )
        )
    }

    /**
     * Pending WebRTC offer subscriptions (Round 5 Bug B): HA's
     * `camera/webrtc/offer` is an event subscription — the SDP
     * answer arrives later as a `type: "event"` message correlated
     * by the command id, not as the command's result. Keyed by that
     * command id; the entry carries a session id captured from the
     * result (when it carried one) plus the answer callback, and is
     * removed once the answer event arrives (candidates keep it
     * open — see [handleEvent]).
     */
    private data class WebRtcSubscription(val sessionId: String?, val onAnswer: (WebRtcAnswer) -> Unit)
    private val webrtcEventHandlers = ConcurrentHashMap<Int, WebRtcSubscription>()

    /**
     * Best effort: asks the instance for its own URLs (`network/url`:
     * internal / external / Nabu Casa), falling back to `cloud/status`'s
     * remote domain. Both are admin commands; a refusal just means
     * null. Lets a local login discover the remote-UI URL (and a cloud
     * login the LAN URL) without the user pasting anything.
     */
    fun fetchInstanceUrls(onResult: (DetectedUrls?) -> Unit) {
        sendCommand("network/url", onResult = { msg ->
            val parsed = if (msg.optBoolean("success", false)) parseNetworkUrls(msg.optJSONObject("result")) else null
            if (parsed?.cloud != null) {
                onResult(parsed)
                return@sendCommand
            }
            sendCommand("cloud/status", onResult = { cmsg ->
                val cloud = if (cmsg.optBoolean("success", false)) parseCloudStatusUrl(cmsg.optJSONObject("result")) else null
                onResult(
                    when {
                        parsed != null -> parsed.copy(cloud = cloud)
                        cloud != null -> DetectedUrls(null, null, cloud)
                        else -> null
                    },
                )
            })
        })
    }

    /**
     * Reads a registry result defensively: the HA WebSocket result is
     * normally a bare JSONArray, but some versions/proxies wrap it in
     * an object (under "result", "areas", "entities", or "devices").
     * A result in none of those shapes yields an empty list — the
     * caller then leaves its previously published state alone.
     */
    private fun registryItems(msg: JSONObject): List<JSONObject> {
        val direct = msg.optJSONArray("result")
        if (direct != null) return (0 until direct.length()).mapNotNull { direct.optJSONObject(it) }
        val wrapped = msg.optJSONObject("result")
        if (wrapped != null) {
            for (key in listOf("areas", "entities", "devices")) {
                val arr = wrapped.optJSONArray(key)
                if (arr != null) return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            }
        }
        return emptyList()
    }

    /**
     * Re-reads the area, entity, and device registries from HA (on
     * connect via [afterAuth], and after every room mutation so the
     * Home room sections and the Rooms screen reflect what HA
     * itself now holds — Haven never keeps a local-only room list).
     *
     * The three fetches are INDEPENDENT (2026-10-30 verdict round):
     * each publishes its own part and recomputes the entity→area map
     * from whatever has arrived so far, via the pure
     * [resolveEntityAreas] (entity area wins, device area fallback).
     * The old shape nested the device fetch inside the entity
     * callback, so any failure or shape surprise in the entity call
     * silently skipped the device call too — and _entityAreas was
     * only ever published from that inner callback, so a failed
     * entity fetch left the map EMPTY while areas displayed fine:
     * exactly the "rooms toggled ON, no sections on Home" symptom.
     * A failed or empty fetch now leaves the previously published
     * map alone (never blanked by a partial failure).
     */
    fun refreshRegistries() {
        sendCommand("config/area_registry/list", onResult = { msg ->
            val items = registryItems(msg)
            val list = mutableListOf<HaArea>()
            for (o in items) {
                val id = o.text("area_id").ifEmpty { o.text("id") }
                if (id.isEmpty()) continue
                list.add(
                    HaArea(
                        id, o.text("name"), o.text("icon").ifEmpty { null },
                        o.text("floor_id").ifEmpty { null },
                    ),
                )
            }
            if (list.isNotEmpty()) _areas.value = list.sortedBy { it.name }
        })
        sendCommand("config/entity_registry/list", onResult = { msg ->
            val items = registryItems(msg)
            val entityToArea = mutableMapOf<String, String?>()
            val entityToDevice = mutableMapOf<String, String>()
            val auxiliary = mutableSetOf<String>()
            for (o in items) {
                val entityId = o.optString("entity_id")
                if (entityId.isEmpty()) continue
                if (o.text("entity_category").isNotEmpty() || o.text("hidden_by").isNotEmpty()) auxiliary += entityId
                val areaId = o.text("area_id")
                if (areaId.isNotEmpty()) entityToArea[entityId] = areaId
                val deviceId = o.text("device_id")
                // An entity with no device has "device_id": null, which
                // [text] reads as "" (Android's optString would have made it
                // the text "null" and a "Device null" row). The literal check
                // stays as a guard for a registry that really sends the text.
                if (deviceId.isNotEmpty() && !deviceId.equals("null", ignoreCase = true)) entityToDevice[entityId] = deviceId
            }
            if (entityToArea.isNotEmpty() || entityToDevice.isNotEmpty()) {
                registryEntityAreas = entityToArea
                registryEntityDevices = entityToDevice
                _auxiliaryEntities.value = auxiliary
                publishEntityAreas()
            }
        })
        sendCommand("config/device_registry/list", onResult = { dmsg ->
            val items = registryItems(dmsg)
            val deviceToArea = mutableMapOf<String, String?>()
            val deviceToMaker = mutableMapOf<String, String>()
            for (d in items) {
                val id = d.optString("id")
                if (id.isEmpty()) continue
                val areaId = d.text("area_id")
                if (areaId.isNotEmpty()) deviceToArea[id] = areaId
                // Manufacturer per device: the second Ring signal.
                // Camera state attributes on this instance do not
                // carry `brand`, which is why run #38's label never
                // fired — the device registry is the ground truth
                // for who made the device.
                val maker = d.text("manufacturer")
                if (maker.isNotEmpty()) deviceToMaker[id] = maker
                // Device info for device-aware search (item 3).
                // Feedback round 17, item 4: the literal string
                // "null" is NOT a name — HA carries it in
                // registry name fields, and the picker rendered
                // it verbatim. Mirrors the device_id "null" guard
                // below: a "null" name is treated as absent.
                val displayName = d.text("name_by_user").ifEmpty { d.text("name") }
                if (displayName.isNotEmpty() && !displayName.equals("null", ignoreCase = true)) {
                    deviceInfoById[id] = HaDeviceInfo(
                        displayName,
                        d.text("model").ifEmpty { null },
                        maker.ifEmpty { null },
                    )
                }
            }
            if (deviceInfoById.isNotEmpty()) _deviceInfo.value = deviceInfoById.toMap()
            if (deviceToArea.isNotEmpty() || deviceToMaker.isNotEmpty()) {
                registryDeviceAreas = deviceToArea
                manufacturerByEntity = registryEntityDevices.mapNotNull { (entityId, deviceId) ->
                    deviceToMaker[deviceId]?.let { entityId to it }
                }.toMap()
                reattachManufacturers()
                publishEntityAreas()
            }
        })
    }

    /**
     * Recomputes [_entityAreas] from the latest registry parts.
     * Skipped until at least one registry part has arrived, so a
     * device-first arrival (which knows no entity→device links yet)
     * can never blank a previously resolved map.
     */
    private fun publishEntityAreas() {
        if (registryEntityAreas.isEmpty() && registryEntityDevices.isEmpty()) return
        // A device's configuration and diagnostic entities (a smart plug's
        // LED, auto-update and signal level) and hidden ones stay with the
        // device, as on Home Assistant's own area pages, rather than filling
        // the room the device is in.
        val auxiliary = _auxiliaryEntities.value
        _entityAreas.value = resolveEntityAreas(
            registryEntityAreas, registryEntityDevices, registryDeviceAreas,
        ).filterKeys { it !in auxiliary }
        _entityDevices.value = registryEntityDevices
    }


    /**
     * Room mutations for the Rooms & areas screen (2026-10-30):
     * create / rename / delete areas and assign entities to areas,
     * all against HA's real registry WebSocket APIs, so changes
     * made here appear in the HA instance itself. Callbacks report
     * (success, errorMessage); on success the registries are
     * re-read so areas + entityAreas flows update. The WS result
     * handler invokes the callback for failures too, so an HA
     * rejection (duplicate room name, refused delete) surfaces as
     * a message instead of a silent no-op.
     */
    fun createArea(name: String, onResult: (Boolean, String?) -> Unit) {
        sendCommand("config/area_registry/create", { it.put("name", name) }) { msg ->
            val ok = msg.optBoolean("success", false)
            if (ok) refreshRegistries()
            onResult(ok, msg.optJSONObject("error")?.optString("message"))
        }
    }

    fun updateArea(areaId: String, name: String, onResult: (Boolean, String?) -> Unit) {
        sendCommand("config/area_registry/update", { it.put("area_id", areaId).put("name", name) }) { msg ->
            val ok = msg.optBoolean("success", false)
            if (ok) refreshRegistries()
            onResult(ok, msg.optJSONObject("error")?.optString("message"))
        }
    }

    fun deleteArea(areaId: String, onResult: (Boolean, String?) -> Unit) {
        sendCommand("config/area_registry/delete", { it.put("area_id", areaId) }) { msg ->
            val ok = msg.optBoolean("success", false)
            if (ok) refreshRegistries()
            onResult(ok, msg.optJSONObject("error")?.optString("message"))
        }
    }

    /** Assigns an entity to an area, or unassigns it when [areaId] is null (sent as JSON null). */
    fun updateEntityArea(entityId: String, areaId: String?, onResult: (Boolean, String?) -> Unit) {
        sendCommand("config/entity_registry/update", {
            it.put("entity_id", entityId)
            if (areaId == null) it.put("area_id", JSONObject.NULL) else it.put("area_id", areaId)
        }) { msg ->
            val ok = msg.optBoolean("success", false)
            if (ok) refreshRegistries()
            onResult(ok, msg.optJSONObject("error")?.optString("message"))
        }
    }

    private fun afterAuth() {
        // Re-subscribe to state_changed ONLY. Reliability round: this
        // used to be `sendCommand("subscribe_events") { it.put(...) }`
        // — a trailing lambda binds to the LAST parameter (onResult),
        // so event_type was never sent and HA subscribed the app to
        // EVERY bus event (service calls, automation runs, logbook,
        // timers…): several times the traffic through the relay, and
        // a refused subscription for non-admin users.
        sendCommand("subscribe_events", build = { it.put("event_type", "state_changed") })
        // States arrive via the result message, which needs a handler — so re-send
        // get_states with a callback that actually stores them.
        sendCommand("get_states", onResult = { msg ->
            val result = msg.optJSONArray("result") ?: return@sendCommand
            val map = mutableMapOf<String, HaEntity>()
            for (i in 0 until result.length()) {
                val e = HaEntity.fromJson(result.optJSONObject(i) ?: continue)
                map[e.entityId] = attachManufacturer(e)
            }
            _states.value = map
        })
        refreshRegistries()
        refreshLovelace()
    }

    /**
     * Loads the Lovelace config. Diagnosis of the run #13 Boards failure
     * ("Unknown config specified: lovelace"): Home Assistant's lovelace
     * websocket handler looks the dashboard up by url_path in its dashboards
     * dict and returns config_not_found when the key is absent — so the
     * instance simply has no dashboard keyed "lovelace". The old code
     * resolved one path from get_panels and died on that single miss.
     *
     * The new resolution never assumes a name: it lists every dashboard via
     * `lovelace/dashboards/list` (the command the HA frontend itself uses),
     * falls back to get_panels lovelace panels when the list is unavailable,
     * then tries candidates in order — the user's last pick, listed
     * dashboards, panel-derived paths, the default (no url_path sent: YAML
     * default dashboards are keyed by null, not "lovelace"), then "lovelace"
     * itself. The first success wins and is persisted; if nothing loads, the
     * error says so plainly instead of spinning, and Retry re-runs the whole
     * enumeration. A YAML-mode dashboard still DISPLAYS fine; only saving
     * is gated (see the edit screen), because HA has no partial-edit API
     * and YAML dashboards must be edited as files.
     */
    fun refreshLovelace() {
        _lovelaceError.value = null
        if (_connection.value != ConnectionState.Connected) {
            _lovelaceError.value = "Not connected to Home Assistant yet — check the connection, then tap Retry."
            return
        }
        sendCommand(ConfigCommands.LOVE_LACE_LIST, onResult = { listMsg ->
            val listed = if (listMsg.optBoolean("success", false)) {
                parseDashboardsList(listMsg.opt("result"))
            } else {
                null
            }
            if (listed != null) _dashboards.value = listed
            fetchPanelsThenTry(listed)
        })
    }

    private fun fetchPanelsThenTry(listed: List<HaDashboard>?) {
        sendCommand("get_panels", onResult = { panelsMsg ->
            val panelPaths = mutableListOf<String>()
            val panels = panelsMsg.optJSONObject("result")
            if (panels != null) {
                val keys = panels.keys()
                while (keys.hasNext()) {
                    val panel = panels.optJSONObject(keys.next()) ?: continue
                    if (panel.optString("component_name") == "lovelace") {
                        panel.optString("url_path").ifEmpty { null }?.let { panelPaths.add(it) }
                    }
                }
            }
            val candidates = mutableListOf<DashboardCandidate>()
            if (hasPreferredLovelacePath) {
                candidates.add(DashboardCandidate(preferredLovelacePath, listed?.firstOrNull { it.urlPath == preferredLovelacePath }))
            }
            listed?.forEach { candidates.add(DashboardCandidate(it.urlPath, it)) }
            panelPaths.forEach { path ->
                if (candidates.none { it.urlPath == path }) {
                    candidates.add(DashboardCandidate(path, listed?.firstOrNull { it.urlPath == path }))
                }
            }
            candidates.add(DashboardCandidate(null, null))
            if (candidates.none { it.urlPath == "lovelace" }) {
                candidates.add(DashboardCandidate("lovelace", null))
            }
            tryLovelaceCandidates(candidates, 0, listed)
        })
    }

    private data class DashboardCandidate(val urlPath: String?, val dashboard: HaDashboard?)

    private fun tryLovelaceCandidates(candidates: List<DashboardCandidate>, index: Int, listed: List<HaDashboard>?) {
        if (index >= candidates.size) {
            val tried = listed?.takeIf { it.isNotEmpty() }?.joinToString { it.title ?: it.urlPath ?: "default" }
            _lovelaceError.value = "Couldn't load dashboards: none of your dashboards would open" +
                (if (tried != null) " (tried: $tried). " else ". ") +
                "If your dashboards live in YAML files instead of the UI editor, check the dashboard list in Home Assistant, then tap Retry."
            return
        }
        val candidate = candidates[index]
        sendCommand(ConfigCommands.LOVE_LACE_GET, build = {
            candidate.urlPath?.let { path -> it.put("url_path", path) }
        }, onResult = { msg ->
            if (!msg.optBoolean("success", false)) {
                val message = msg.optJSONObject("error")?.optString("message") ?: "unknown error"
                Log.w("Haven", "lovelace/config FAILED url_path=${candidate.urlPath}: $message")
                tryLovelaceCandidates(candidates, index + 1, listed)
                return@sendCommand
            }
            val result = msg.optJSONObject("result")
            if (result == null) {
                tryLovelaceCandidates(candidates, index + 1, listed)
                return@sendCommand
            }
            try {
                _lovelace.value = parseLovelace(result)
                lovelaceUrlPath = candidate.urlPath
                _selectedDashboard.value = candidate.dashboard
                onDashboardResolved?.invoke(candidate.urlPath, true)
                _lovelaceError.value = null
            } catch (e: Exception) {
                Log.w("Haven", "lovelace parse FAILED: ${e.message}")
                _lovelaceError.value = "Couldn't read dashboards: ${e.message}"
            }
        })
    }

    /**
     * Parses `lovelace/dashboards/list` results. The command returns a JSON
     * array of dashboard objects; a wrapping object is tolerated defensively
     * so a shape change degrades to "list unavailable" (panel fallback)
     * instead of a crash.
     */
    private fun parseDashboardsList(result: Any?): List<HaDashboard>? {
        val rawArr: JSONArray? = when (result) {
            is JSONArray -> result
            is JSONObject -> result.optJSONArray("dashboards")
            else -> null
        }
        val arr = rawArr ?: return null
        val out = mutableListOf<HaDashboard>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out.add(
                HaDashboard(
                    urlPath = obj.optString("url_path").ifEmpty { null },
                    title = obj.optString("title").ifEmpty { null },
                    mode = obj.optString("mode").ifEmpty { null },
                )
            )
        }
        return out
    }

    private fun parseLovelace(result: JSONObject): LovelaceConfig {
        val views = mutableListOf<LovelaceView>()
        val viewsJson = result.optJSONArray("views")
        if (viewsJson != null) {
            for (i in 0 until viewsJson.length()) {
                val v = viewsJson.optJSONObject(i) ?: continue
                val cards = mutableListOf<JSONObject>()
                val cardsJson = v.optJSONArray("cards")
                if (cardsJson != null) {
                    for (j in 0 until cardsJson.length()) {
                        cards.add(cardsJson.optJSONObject(j) ?: continue)
                    }
                }
                views.add(
                    LovelaceView(
                        title = v.optString("title"),
                        path = v.optString("path").ifEmpty { null },
                        cards = cards,
                    )
                )
            }
        }
        return LovelaceConfig(result.optString("title").ifEmpty { null }, views)
    }

    /**
     * Service call. The message shape is Home Assistant's documented
     * call_service form (type/domain/service/service_data?/target?) — verified
     * against developers.home-assistant.io/docs/api/websocket and R1HA's
     * identical implementation. What used to be broken was everything around
     * it: the call was fire-and-forget with no result callback, and send()
     * failures were swallowed, so a tap that never reached the instance looked
     * exactly like success. Now the result is always observed: failures surface
     * in [lastServiceError], and every call and outcome is logged. A call that
     * cannot leave the phone (no live session, or the session dropped before
     * the reply) reports "not sent" instead of a generic rejection.
     */
    fun callService(
        domain: String,
        service: String,
        entityId: String? = null,
        data: JSONObject? = null,
        onDone: ((Boolean) -> Unit)? = null,
    ) {
        val id = nextId.getAndIncrement()
        val obj = JSONObject().put("id", id).put("type", "call_service")
        obj.put("domain", domain)
        obj.put("service", service)
        if (entityId != null) obj.put("target", JSONObject().put("entity_id", entityId))
        if (data != null) obj.put("service_data", data)
        Log.d(TAG, "call_service $domain.$service target=$entityId")
        dispatch(id, obj) { msg ->
            if (msg.optBoolean("success", false)) {
                Log.d(TAG, "call_service ok $domain.$service -> $entityId")
                onDone?.invoke(true)
            } else {
                val err = msg.optJSONObject("error")
                val code = err?.optString("code") ?: "error"
                if (code == NOT_CONNECTED) {
                    Log.w(TAG, "call_service NOT SENT (no live session) $domain.$service -> $entityId")
                    _lastServiceError.value = "Not connected — the action was not sent. Check the connection and try again."
                } else {
                    val message = err?.optString("message")?.ifEmpty { null } ?: "Home Assistant rejected the call"
                    Log.w(TAG, "call_service FAILED $domain.$service -> $entityId: $code $message")
                    _lastServiceError.value = "$domain.$service: $message"
                }
                onDone?.invoke(false)
            }
        }
    }

    fun toggle(entityId: String, onDone: ((Boolean) -> Unit)? = null) =
        callService(entityId.substringBefore("."), "toggle", entityId, null, onDone)

    /**
     * WebRTC offer round-trip for Ring live: sends the complete SDP
     * offer via `camera/webrtc/offer` and parses the answer + session
     * id (see parseWebRtcAnswer). Round 5: the command is an event
     * subscription — when the success result carries no answer yet,
     * the answer is awaited as an event correlated by the command id
     * (see handleMessage + parseWebRtcEventAnswer), not reported as
     * "answer missing SDP". On an unknown-command error it falls
     * back to the legacy `camera/web_rtc` command (older HA). The
     * callback posts to the main thread; it may never fire if the
     * socket is down, so callers must time out (as the HLS path does).
     */
    fun requestWebRtcOffer(entityId: String, offerSdp: String, onResult: (WebRtcAnswer) -> Unit) {
        val post = { a: WebRtcAnswer ->
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(a) }
        }
        fun send(command: String) {
            // The command id is needed inside onResult to register
            // the event subscription; sendCommand assigns it
            // synchronously before any message can arrive, so
            // capturing it this way is safe.
            var cmdId = -1
            cmdId = sendCommand(command, build = {
                it.put("entity_id", entityId)
                it.put("offer", offerSdp)
            }, onResult = { msg ->
                if (msg.optBoolean("success", false)) {
                    val result = msg.optJSONObject("result")
                    val inlineAnswer = result?.optString("answer")?.ifEmpty { null }
                        ?: result?.optString("sdp")?.ifEmpty { null }
                    val sessionId = result?.optString("session_id")?.ifEmpty { null }
                    if (inlineAnswer != null) {
                        // Old shape: the result itself carries the
                        // answer — deliver it at once.
                        post(WebRtcAnswer(inlineAnswer, sessionId, null))
                    } else if (command == "camera/webrtc/offer") {
                        // Subscription shape (Round 5 Bug B): success
                        // with no answer yet — the answer arrives
                        // later as an event correlated by cmdId (see
                        // handleMessage). Do NOT report "answer
                        // missing SDP" here: that error is exactly
                        // what the device read while real answers
                        // were dropped by event routing. The
                        // caller's timeout still bounds the wait.
                        webrtcEventHandlers[cmdId] = WebRtcSubscription(sessionId, onResult)
                        Log.d("Haven", "FrontDoorLive: webrtc offer subscribed (id=$cmdId), awaiting answer event")
                    } else {
                        post(parseWebRtcAnswer(msg))
                    }
                } else {
                    val err = msg.optJSONObject("error")
                    val code = err?.optString("code")
                    if (command == "camera/webrtc/offer" && code == "unknown_command") {
                        send("camera/web_rtc")
                    } else {
                        post(WebRtcAnswer(null, null, err?.optString("message")?.ifEmpty { null } ?: "offer rejected"))
                    }
                }
            })
        }
        send("camera/webrtc/offer")
    }

    /**
     * Best-effort trickle ICE send: a local candidate gathered after
     * the answer, via `camera/webrtc/candidate`. Failures are ignored
     * by design — the offer carries the complete gathered set, so
     * trickle is an accelerator, never a requirement. No callback: the
     * caller never blocks on it.
     */
    fun sendWebRtcCandidate(entityId: String, sessionId: String, candidateSdp: String) {
        sendCommand("camera/webrtc/candidate", build = {
            it.put("entity_id", entityId)
            it.put("session_id", sessionId)
            it.put("candidate", candidateSdp)
        })
    }

    /** One browsable media-source child (a clip, a folder, or a source). */
    data class MediaChild(
        val mediaContentId: String,
        val mediaContentType: String,
        val title: String,
        val thumbnail: String?,
        val canPlay: Boolean,
        val canExpand: Boolean,
    )

    /**
     * Media-source browse: the children of [mediaContentId] (root is
     * ""). Null means the call failed (unknown source, socket down);
     * an empty list means a valid but childless node. The callback
     * posts to the main thread; it may never fire if the socket is
     * down, so callers must time out.
     */
    fun browseMedia(mediaContentId: String, mediaContentType: String, onResult: (List<MediaChild>?) -> Unit) {
        val post = { v: List<MediaChild>? ->
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(v) }
        }
        sendCommand("media_source/browse_media", build = {
            it.put("media_content_type", mediaContentType)
            it.put("media_content_id", mediaContentId)
        }, onResult = { msg ->
            if (!msg.optBoolean("success", false)) {
                post(null)
                return@sendCommand
            }
            val result = msg.optJSONObject("result") ?: run { post(null); return@sendCommand }
            val arr = result.optJSONArray("children") ?: run { post(emptyList()); return@sendCommand }
            val out = mutableListOf<MediaChild>()
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                out.add(
                    MediaChild(
                        mediaContentId = c.optString("media_content_id"),
                        mediaContentType = c.optString("media_content_type"),
                        title = c.optString("title"),
                        thumbnail = c.text("thumbnail").ifEmpty { null },
                        canPlay = c.optBoolean("can_play", false),
                        canExpand = c.optBoolean("can_expand", false),
                    ),
                )
            }
            post(out)
        })
    }

    /**
     * Media-source resolve: the playable URL + mime type for a
     * browsed child. Null means unresolvable. Relative URLs resolve
     * against the instance (as the HLS path does); callers attach the
     * Bearer token when fetching.
     */
    fun resolveMedia(mediaContentId: String, mediaContentType: String, onResult: (Pair<String, String>?) -> Unit) {
        val post = { v: Pair<String, String>? ->
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(v) }
        }
        sendCommand("media_source/resolve_media", build = {
            it.put("media_content_type", mediaContentType)
            it.put("media_content_id", mediaContentId)
        }, onResult = { msg ->
            if (!msg.optBoolean("success", false)) {
                post(null)
                return@sendCommand
            }
            val result = msg.optJSONObject("result") ?: run { post(null); return@sendCommand }
            val url = result.optString("url").ifEmpty { null }
            val mime = result.optString("mime_type").ifEmpty { null }
            post(if (url != null && mime != null) url to mime else null)
        })
    }


    /** Saves a full Lovelace config (views/cards) back to the instance. */
    fun saveLovelace(config: JSONObject, onDone: (Boolean) -> Unit) {
        // The save handler looks the dashboard up by url_path too; saving
        // without it failed the same silent way the fetch did.
        sendCommand(ConfigCommands.LOVE_LACE_SAVE, build = {
            it.put("config", config)
            lovelaceUrlPath?.let { path -> it.put("url_path", path) }
        }, onResult = { msg ->
            onDone(msg.optBoolean("success", false))
        })
    }

    /**
     * One command, awaited: the result message, or null when Home
     * Assistant does not answer within [timeoutMs] (not connected, or
     * a command this version does not know). Settings uses it for the
     * registries and admin lists it reads on demand.
     */
    suspend fun command(type: String, timeoutMs: Long = 10_000, build: (JSONObject) -> Unit = {}): JSONObject? =
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                val id = sendCommand(type, build) { msg -> if (cont.isActive) cont.resumeWith(Result.success(msg)) }
                cont.invokeOnCancellation { pending.remove(id) }
            }
        }

    /**
     * One REST call to the instance's API, as (HTTP status, body). Status
     * is 0 when the request could not be made at all. [path] starts with
     * "/api/".
     */
    suspend fun rest(method: String, path: String, body: JSONObject? = null): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val requestBody = body?.toString()?.toRequestBody("application/json".toMediaTypeOrNull())
                ?: if (method == "POST") "".toRequestBody(null) else null
            val req = Request.Builder()
                .url("$instanceUrl$path")
                .header("Authorization", "Bearer $token")
                .method(method, requestBody)
                .build()
            http.newCall(req).execute().use { resp -> resp.code to resp.body?.string() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            0 to e.message
        }
    }

    /** Saves an automation/script/scene config through the REST config API. */
    fun saveRestConfig(kind: String, slug: String, config: JSONObject, onDone: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val ok = try {
                val body = config.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val req = Request.Builder()
                    .url("$instanceUrl/api/config/$kind/config/$kind.$slug")
                    .header("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                http.newCall(req).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post { onDone(ok) }
        }
    }

    /** Todo items for a todo entity, via the todo.get_items service with a response. */
    fun fetchTodoItems(entityId: String, onItems: (JSONArray?) -> Unit) {
        sendCommand("call_service", build = {
            it.put("domain", "todo")
            it.put("service", "get_items")
            it.put("target", JSONObject().put("entity_id", entityId))
            it.put("return_response", true)
        }, onResult = { msg ->
            val response = msg.optJSONObject("result")?.optJSONObject("response")
            val items: JSONArray? = when {
                response == null -> null
                response.has(entityId) -> response.optJSONObject(entityId)?.optJSONArray("items")
                response.has("items") -> response.optJSONArray("items")
                else -> null
            }
            onItems(items)
        })
    }

    /*
     * Todo item edits. Reliability round: these used to send
     * `todo/item_create|update|delete` with the payload in a trailing
     * lambda — which Kotlin binds to the LAST parameter (onResult), so
     * the commands left with no entity_id/item at all, and those WS
     * commands do not exist in Home Assistant anyway. Item edits are
     * the todo services (todo.add_item / update_item / remove_item),
     * whose `item` field takes the uid; [onDone] reports the outcome so
     * the list refetches after the change lands, not before.
     */
    fun todoCreate(entityId: String, summary: String, onDone: ((Boolean) -> Unit)? = null) =
        callService("todo", "add_item", entityId, JSONObject().put("item", summary), onDone)

    fun todoUpdate(entityId: String, uid: String, status: String, onDone: ((Boolean) -> Unit)? = null) =
        callService("todo", "update_item", entityId, JSONObject().put("item", uid).put("status", status), onDone)

    fun todoDelete(entityId: String, uid: String, onDone: ((Boolean) -> Unit)? = null) =
        callService("todo", "remove_item", entityId, JSONObject().put("item", uid), onDone)

    /** Numeric history for sparklines, via REST. Returns points sorted by time. */
    suspend fun fetchHistory(entityId: String, hours: Int): List<HistoryPoint> = withContext(Dispatchers.IO) {
        if (!restAllowed()) return@withContext emptyList()
        try {
            val start = Date(System.currentTimeMillis() - hours * 3600L * 1000L)
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            val url = "$instanceUrl/api/history/period/${fmt.format(start)}" +
                "?filter_entity_id=$entityId&minimal_response&no_attributes"
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            val body = http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                resp.body?.string()
            } ?: return@withContext emptyList()
            val outer = JSONArray(body)
            if (outer.length() == 0) return@withContext emptyList()
            val series = outer.optJSONArray(0) ?: return@withContext emptyList()
            val points = mutableListOf<HistoryPoint>()
            for (i in 0 until series.length()) {
                val point = series.optJSONObject(i) ?: continue
                val value = point.optString("state").toDoubleOrNull() ?: continue
                val changed = point.optString("last_changed")
                val ms = parseIsoToMs(changed) ?: continue
                points.add(HistoryPoint(ms, value))
            }
            points.sortedBy { it.timeMs }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    /** A camera snapshot: the JPEG as served (persisted verbatim) and its decoded bitmap. */
    class CameraSnapshot(val bytes: ByteArray, val bitmap: Bitmap)

    /**
     * A camera entity's current snapshot via the camera proxy. With
     * [widthPx] > 0, Home Assistant scales the image server-side
     * (`?width=`, honored by HA's camera proxy) and the decode is
     * sampled down to the same width, so a quadrant tile moves ~40KB
     * through the relay instead of a full-resolution frame. The
     * response is always closed (the old code leaked it, which cost a
     * fresh TLS handshake on the next fetch), and an HTTP error is a
     * null, never an attempt to decode an error page.
     */
    suspend fun fetchCameraSnapshot(entityId: String, widthPx: Int = 0): CameraSnapshot? = withContext(Dispatchers.IO) {
        if (!restAllowed()) return@withContext null
        try {
            val widthParam = if (widthPx > 0) "width=$widthPx&" else ""
            val req = Request.Builder()
                .url("$instanceUrl/api/camera_proxy/$entityId?${widthParam}cb=${System.currentTimeMillis()}")
                .header("Authorization", "Bearer $token")
                .build()
            val bytes = http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.bytes()
            } ?: return@withContext null
            val bitmap = decodeJpegScaled(bytes, widthPx) ?: return@withContext null
            CameraSnapshot(bytes, bitmap)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    /** A camera entity's current snapshot at full size (compat for callers that need no bytes). */
    suspend fun fetchCameraBitmap(entityId: String, widthPx: Int = 0): Bitmap? =
        fetchCameraSnapshot(entityId, widthPx)?.bitmap

    /**
     * True when [absoluteUrl] points at this instance (any configured
     * endpoint). The Bearer token is attached ONLY to such URLs — the
     * old image fetch sent it to whatever host a media thumbnail named.
     */
    private fun isInstanceUrl(absoluteUrl: String): Boolean {
        val host = connectionHostOf(absoluteUrl) ?: return false
        val known = synchronized(lock) { endpoints + instanceUrl }
        return known.any { connectionHostOf(it) == host }
    }

    private fun resolveAgainstInstance(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "$instanceUrl$url"

    /** Fetches an image URL (media-source thumbnails), resolved against the instance; Bearer only for the instance. */
    suspend fun fetchImageBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val absolute = resolveAgainstInstance(url)
            val builder = Request.Builder().url(absolute)
            if (isInstanceUrl(absolute)) {
                if (!restAllowed()) return@withContext null
                builder.header("Authorization", "Bearer $token")
            }
            val bytes = http.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.bytes()
            } ?: return@withContext null
            decodeJpegScaled(bytes, 0)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    /**
     * Live MJPEG playback for a camera, via `/api/camera_proxy_stream/`.
     * Emits [CameraStreamState.Connecting] immediately, then [Playing] per
     * decoded frame. A first frame must arrive within [FIRST_FRAME_TIMEOUT_MS]
     * and frames must keep arriving within [STALL_TIMEOUT_MS]; otherwise an
     * explicit [Error] fires so the viewer shows error + Retry instead of a
     * frozen frame that looks live. Slow snapshot cameras (Ring) get generous
     * timeouts: their proxy stream only produces a frame per snapshot fetch —
     * which is also why this call has no socket read timeout (the old 10s
     * default killed Ring streams before the watchdog ever ran).
     * Callbacks post to the main thread; cancel the returned Job to stop —
     * cancelling now also aborts the HTTP call, so closing a viewer stops
     * the download at once (it used to keep pulling frames in the
     * background until the server hung up).
     */
    fun streamCamera(
        entityId: String,
        onState: (CameraStreamState) -> Unit,
    ): Job {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        return scope.launch(Dispatchers.IO) {
            val job = coroutineContext[Job]
            fun post(state: CameraStreamState) {
                if (job?.isActive != false) main.post { onState(state) }
            }
            post(CameraStreamState.Connecting)
            if (!restAllowed()) {
                post(CameraStreamState.Error("Not signed in to Home Assistant"))
                return@launch
            }
            val req = Request.Builder()
                .url("$instanceUrl/api/camera_proxy_stream/$entityId")
                .header("Authorization", "Bearer $token")
                .build()
            val call = streamHttp.newCall(req)
            job?.invokeOnCompletion { call.cancel() }
            try {
                call.execute().use { resp ->
                    if (!resp.isSuccessful) {
                        post(CameraStreamState.Error("Camera stream unavailable (HTTP ${resp.code})"))
                        return@launch
                    }
                    val contentType = resp.header("Content-Type") ?: ""
                    val boundary = Regex("boundary=([^;\\s]+)").find(contentType)?.groupValues?.get(1)
                        ?.trim('"', '\'')
                    if (boundary.isNullOrEmpty()) {
                        post(CameraStreamState.Error("Camera stream format not recognized"))
                        return@launch
                    }
                    val body = resp.body?.byteStream()
                    if (body == null) {
                        post(CameraStreamState.Error("Camera stream is empty"))
                        return@launch
                    }
                    val parser = MjpegParser(boundary)
                    val startedAt = System.currentTimeMillis()
                    val lastFrameAt = java.util.concurrent.atomic.AtomicLong(startedAt)
                    val sawFrame = AtomicBoolean(false)
                    val failed = AtomicBoolean(false)
                    val buf = ByteArray(16 * 1024)
                    // Watchdog: a CHILD of the stream job, so cancelling the
                    // stream (viewer closed, Retry pressed) kills it too. A
                    // timeout cancels the call, which unblocks the read.
                    val watchdog = launch(Dispatchers.Default) {
                        while (isActive) {
                            delay(1000)
                            val now = System.currentTimeMillis()
                            val message = when {
                                !sawFrame.get() && now - startedAt > FIRST_FRAME_TIMEOUT_MS -> "Camera did not start streaming"
                                sawFrame.get() && now - lastFrameAt.get() > STALL_TIMEOUT_MS -> "Camera stream stalled"
                                else -> null
                            }
                            if (message != null) {
                                failed.set(true)
                                post(CameraStreamState.Error(message))
                                call.cancel()
                                break
                            }
                        }
                    }
                    try {
                        while (isActive && !failed.get()) {
                            val read = body.read(buf)
                            if (read < 0) break
                            if (read == 0) continue
                            for (jpeg in parser.feed(buf.copyOf(read))) {
                                val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: continue
                                lastFrameAt.set(System.currentTimeMillis())
                                sawFrame.set(true)
                                post(CameraStreamState.Playing(bitmap))
                            }
                        }
                        // Stream ended cleanly without ever producing a frame.
                        if (isActive && !sawFrame.get() && !failed.get()) post(CameraStreamState.Error("Camera stream ended"))
                    } finally {
                        watchdog.cancel()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // A cancelled call (viewer closed, watchdog fired) is not a new error.
                if (isActive && !call.isCanceled()) post(CameraStreamState.Error("Could not reach the camera stream"))
            }
        }
    }

    /**
     * HA's stream path for a camera: WS `camera/stream` returns an HLS
     * playlist URL (relative, e.g. `/api/hls/<token>/playlist.m3u8`) when
     * the instance can stream the camera, or null when it cannot (unknown
     * entity, no stream source, stream integration unavailable). The
     * callback posts to the main thread and now ALWAYS fires: a command
     * that cannot be sent, or whose session drops before the reply, fails
     * promptly with a diagnostic instead of hanging until a timeout. This is
     * the Ring true-live attempt: HA's own
     * frontend plays the same entity live through this path, while the
     * MJPEG proxy serves its last motion recording. The callback also
     * delivers [diagnoseHlsStreamMessage]'s line (null on success):
     * the old signature dropped HA's error, so a rejected command and
     * a genuinely unstreamable camera looked identical — the
     * diagnostic is what the fullscreen error UI shows.
     */
    fun requestHlsUrl(entityId: String, onResult: (url: String?, diagnostic: String?) -> Unit) {
        val post = { url: String?, diagnostic: String? ->
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(url, diagnostic) }
        }
        sendCommand("camera/stream", build = { it.put("entity_id", entityId) }, onResult = { msg ->
            post(parseHlsStreamUrl(msg), diagnoseHlsStreamMessage(msg))
        })
    }

    /**
     * Live-camera buffering: start rendering after 0.5s of media
     * instead of ExoPlayer's 2.5s default. A camera is live, so a
     * deep buffer only adds latency — this is most of the "Loading
     * camera…" wait on a fast LAN, and over the relay it still trims
     * two seconds off every open.
     */
    private fun liveLoadControl(): androidx.media3.exoplayer.DefaultLoadControl =
        androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 10_000, 500, 1_000)
            .build()

    /**
     * Media requests go through the same OkHttp pool (keep-alive, the
     * 401 renewal) instead of a separate HttpURLConnection stack. The
     * Bearer header is attached only for this instance.
     */
    private fun mediaDataSourceFactory(absoluteUrl: String): androidx.media3.datasource.DataSource.Factory {
        val factory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(http)
        if (isInstanceUrl(absoluteUrl)) {
            factory.setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        }
        return factory
    }

    /**
     * An ExoPlayer configured for HA's HLS streams: the playlist URL is
     * resolved against the instance, and every playlist/segment request
     * carries the Bearer token (HLS URLs are authenticated). The caller
     * owns the player and must release it.
     */
    fun createHlsPlayer(context: android.content.Context, playlistUrl: String): androidx.media3.exoplayer.ExoPlayer {
        val absolute = resolveAgainstInstance(playlistUrl)
        val source = androidx.media3.exoplayer.hls.HlsMediaSource.Factory(mediaDataSourceFactory(absolute))
            .setAllowChunklessPreparation(true)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(absolute))
        return androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setLoadControl(liveLoadControl())
            .build()
            .apply {
                setMediaSource(source)
                prepare()
                playWhenReady = true
            }
    }

    /**
     * An ExoPlayer configured for the ring-mqtt RTSP feed (round 21,
     * item 3): the Front Door panel's fallback live source when
     * camera.front_door_69a1 is down. RTSP is unauthenticated on the
     * LAN — no Bearer header — and RTP is forced over TCP, which is
     * the reliable transport through the Pi's go2rtc listener and
     * the only one ExoPlayer's RTSP stack should negotiate here.
     * The caller owns the player and must release it.
     */
    fun createRtspPlayer(context: android.content.Context, rtspUrl: String): androidx.media3.exoplayer.ExoPlayer {
        val source = androidx.media3.exoplayer.rtsp.RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(rtspUrl))
        return androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setLoadControl(liveLoadControl())
            .build()
            .apply {
                setMediaSource(source)
                prepare()
                playWhenReady = true
            }
    }

    /**
     * Player for a resolved media-source clip (usually mp4): same
     * Bearer auth + relative-URL resolution as the HLS path, with the
     * resolved mime type on the MediaItem so ExoPlayer never has to
     * guess from an extension-less proxy URL. Caller releases.
     */
    fun createMediaPlayer(context: android.content.Context, mediaUrl: String, mimeType: String?): androidx.media3.exoplayer.ExoPlayer {
        val absolute = resolveAgainstInstance(mediaUrl)
        val item = androidx.media3.common.MediaItem.Builder()
            .setUri(absolute)
            .apply { if (mimeType != null) setMimeType(mimeType) }
            .build()
        return androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaSource(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(mediaDataSourceFactory(absolute))
                    .createMediaSource(item),
            )
            prepare()
            playWhenReady = true
        }
    }

    /**
     * REST 401 handler: renews the token once through [tokenRefresher]
     * and replays the request. When no fresh token is available the
     * stale one is marked rejected and REST pauses until the token
     * changes — every 401 counts as a failed login toward Home
     * Assistant's IP ban, so the client never keeps knocking.
     */
    private inner class RefreshingAuthenticator : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val header = response.request.header("Authorization") ?: return null
            if (!header.startsWith("Bearer ")) return null
            if (response.priorResponse != null) return null
            val stale = header.removePrefix("Bearer ")
            val refresher = tokenRefresher
            val fresh = if (refresher == null) {
                null
            } else {
                try {
                    refresher(stale)
                } catch (e: Exception) {
                    null
                }
            }
            if (fresh.isNullOrEmpty() || fresh == stale) {
                restRejectedToken = stale
                Log.w(TAG, "REST 401 with no renewed token; pausing REST until the token changes")
                return null
            }
            updateToken(fresh)
            return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
        }
    }

    companion object {
        /** Slow snapshot cameras need this long to produce a first frame. */
        const val FIRST_FRAME_TIMEOUT_MS = 20_000L

        /** No frame for this long after playback started means a dead stream. */
        const val STALL_TIMEOUT_MS = 30_000L

        /** Error code carried by a synthetic result for a command that never reached the instance. */
        const val NOT_CONNECTED = "not_connected"

        private const val TAG = "Haven"
        private const val ROLE_STALE = 0
        private const val ROLE_PRIMARY = 1
        private const val ROLE_UPGRADE = 2

        /**
         * One connection pool for the whole process (app + Device
         * Controls service). Keep-alive matters most through the Nabu
         * Casa relay, where every new TLS handshake costs several
         * round trips before the first byte of a snapshot.
         */
        val sharedHttp: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    /** HA timestamps carry microseconds and an offset; java.time parses both exactly (SimpleDateFormat read "123456" as milliseconds). */
    private fun parseIsoToMs(iso: String): Long? {
        if (iso.isEmpty()) return null
        return try {
            java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.Instant.parse(iso).toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }
}
