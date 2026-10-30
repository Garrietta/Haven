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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
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

class HaClient(
    private val scope: CoroutineScope,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _states = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    val states: StateFlow<Map<String, HaEntity>> = _states.asStateFlow()

    private val _areas = MutableStateFlow<List<HaArea>>(emptyList())
    val areas: StateFlow<List<HaArea>> = _areas.asStateFlow()

    /** entity_id -> area_id, resolved from the entity registry then the device registry. */
    private val _entityAreas = MutableStateFlow<Map<String, String>>(emptyMap())
    val entityAreas: StateFlow<Map<String, String>> = _entityAreas.asStateFlow()

    /**
     * Latest registry parts, kept so each fetch can recompute the
     * map independently (2026-10-30 verdict round): entity-registry
     * areas, entity→device links, device-registry areas.
     */
    private var registryEntityAreas: Map<String, String?> = emptyMap()
    private var registryEntityDevices: Map<String, String> = emptyMap()
    private var registryDeviceAreas: Map<String, String?> = emptyMap()

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
        val current = _states.value.toMutableMap()
        val old = current[entityId] ?: return
        current[entityId] = old.copy(state = newState)
        _states.value = current
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
    private var manufacturerByEntity: Map<String, String> = emptyMap()

    private fun attachManufacturer(entity: HaEntity): HaEntity {
        val maker = manufacturerByEntity[entity.entityId] ?: return entity
        return entity.copy(manufacturer = maker)
    }

    /** Re-attaches manufacturers to every currently held entity (runs when the device registry lands). */
    private fun reattachManufacturers() {
        if (manufacturerByEntity.isEmpty()) return
        val patched = _states.value.mapValues { (_, entity) -> attachManufacturer(entity) }
        _states.value = patched
    }

    private var socket: WebSocket? = null
    private var instanceUrl: String = ""
    private var token: String = ""
    private var manualDisconnect = false
    private var reconnectJob: Job? = null
    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, (JSONObject) -> Unit>()

    fun connect(url: String, accessToken: String) {
        manualDisconnect = false
        instanceUrl = url.trimEnd('/')
        token = accessToken
        openSocket()
    }

    fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        try {
            socket?.close(1000, "bye")
        } catch (e: Exception) {
            // Already gone.
        }
        socket = null
        _connection.value = ConnectionState.Disconnected
    }

    private fun wsUrl(): String {
        val base = instanceUrl
        val wsBase = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> "ws://$base"
        }
        return "$wsBase/api/websocket"
    }

    private fun openSocket() {
        _connection.value = ConnectionState.Connecting
        val request = Request.Builder().url(wsUrl()).build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Server speaks first with auth_required; we answer in onMessage.
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                if (!manualDisconnect) {
                    _connection.value = ConnectionState.Error(t.message ?: "Connection failed")
                    scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                if (!manualDisconnect) {
                    _connection.value = ConnectionState.Error("Connection closed")
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(3000)
            if (!manualDisconnect) openSocket()
        }
    }

    private fun send(obj: JSONObject): Boolean {
        return try {
            socket?.send(obj.toString()) ?: false
        } catch (e: Exception) {
            // Socket gone; the reconnect loop will pick it up.
            false
        }
    }

    /** Sends a command and routes its `result` message to [onResult]. Returns the id. */
    fun sendCommand(type: String, build: (JSONObject) -> Unit = {}, onResult: ((JSONObject) -> Unit)? = null): Int {
        val id = nextId.getAndIncrement()
        val obj = JSONObject().put("id", id).put("type", type)
        build(obj)
        if (onResult != null) pending[id] = onResult
        send(obj)
        return id
    }

    private fun handleMessage(text: String) {
        val msg = try {
            JSONObject(text)
        } catch (e: Exception) {
            return
        }
        when (msg.optString("type")) {
            "auth_required" -> {
                _connection.value = ConnectionState.Authenticating
                send(JSONObject().put("type", "auth").put("access_token", token))
            }
            "auth_ok" -> {
                _connection.value = ConnectionState.Connected
                afterAuth()
            }
            "auth_invalid" -> {
                _connection.value = ConnectionState.Error("Sign-in rejected. Check the token or sign in again.")
            }
            "result" -> {
                val id = msg.optInt("id", -1)
                val cb = pending.remove(id)
                if (msg.optBoolean("success", false)) {
                    cb?.invoke(msg)
                } else {
                    cb?.invoke(msg)
                }
            }
            "event" -> {
                val event = msg.optJSONObject("event") ?: return
                if (event.optString("event_type") == "state_changed") {
                    val data = event.optJSONObject("data") ?: return
                    val entityId = data.optString("entity_id")
                    val newState = data.optJSONObject("new_state")
                    val current = _states.value.toMutableMap()
                    if (newState == null || newState == JSONObject.NULL) {
                        current.remove(entityId)
                        optimisticGuard.clear(entityId)
                    } else {
                        val incoming = newState.optString("state")
                        // A contradicting event while a tap's expectation
                        // is live is stale or intermediate: drop it so the
                        // optimistic flip the user already saw is not
                        // reverted for a second before the confirming
                        // event arrives. A matching event confirms the
                        // flip (the guard clears itself) and applies.
                        if (!optimisticGuard.shouldDrop(entityId, incoming)) {
                            current[entityId] = attachManufacturer(HaEntity.fromJson(newState))
                        }
                    }
                    _states.value = current
                }
            }
        }
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
                val id = o.optString("area_id").ifEmpty { o.optString("id") }
                if (id.isEmpty()) continue
                list.add(HaArea(id, o.optString("name"), o.optString("icon").ifEmpty { null }))
            }
            if (list.isNotEmpty()) _areas.value = list.sortedBy { it.name }
        })
        sendCommand("config/entity_registry/list", onResult = { msg ->
            val items = registryItems(msg)
            val entityToArea = mutableMapOf<String, String?>()
            val entityToDevice = mutableMapOf<String, String>()
            for (o in items) {
                val entityId = o.optString("entity_id")
                if (entityId.isEmpty()) continue
                val areaId = o.optString("area_id")
                if (areaId.isNotEmpty()) entityToArea[entityId] = areaId
                val deviceId = o.optString("device_id")
                if (deviceId.isNotEmpty()) entityToDevice[entityId] = deviceId
            }
            if (entityToArea.isNotEmpty() || entityToDevice.isNotEmpty()) {
                registryEntityAreas = entityToArea
                registryEntityDevices = entityToDevice
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
                val areaId = d.optString("area_id")
                if (areaId.isNotEmpty()) deviceToArea[id] = areaId
                // Manufacturer per device: the second Ring signal.
                // Camera state attributes on this instance do not
                // carry `brand`, which is why run #38's label never
                // fired — the device registry is the ground truth
                // for who made the device.
                val maker = d.optString("manufacturer")
                if (maker.isNotEmpty()) deviceToMaker[id] = maker
            }
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
        _entityAreas.value = resolveEntityAreas(
            registryEntityAreas, registryEntityDevices, registryDeviceAreas,
        )
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
        // Re-subscribe; the result callback is not needed for steady-state updates.
        sendCommand("subscribe_events") { it.put("event_type", "state_changed") }
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
     * in [lastServiceError], and every call and outcome is logged.
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
        Log.d("Haven", "call_service $domain.$service target=$entityId")
        pending[id] = { msg ->
            if (msg.optBoolean("success", false)) {
                Log.d("Haven", "call_service ok $domain.$service -> $entityId")
                onDone?.invoke(true)
            } else {
                val err = msg.optJSONObject("error")
                val message = err?.optString("message") ?: "Home Assistant rejected the call"
                val code = err?.optString("code") ?: "error"
                Log.w("Haven", "call_service FAILED $domain.$service -> $entityId: $code $message")
                _lastServiceError.value = "$domain.$service: $message"
                onDone?.invoke(false)
            }
        }
        if (!send(obj)) {
            pending.remove(id)
            Log.w("Haven", "call_service NOT SENT (no live socket) $domain.$service -> $entityId")
            _lastServiceError.value = "Not connected — the action was not sent. Check the connection and try again."
            onDone?.invoke(false)
        }
    }

    fun toggle(entityId: String, onDone: ((Boolean) -> Unit)? = null) =
        callService(entityId.substringBefore("."), "toggle", entityId, null, onDone)

    /**
     * WebRTC offer round-trip for Ring live: sends the complete SDP
     * offer via `camera/webrtc/offer` and parses the answer + session
     * id (see parseWebRtcAnswer). On an unknown-command error it falls
     * back to the legacy `camera/web_rtc` command (older HA). The
     * callback posts to the main thread; it may never fire if the
     * socket is down, so callers must time out (as the HLS path does).
     */
    fun requestWebRtcOffer(entityId: String, offerSdp: String, onResult: (WebRtcAnswer) -> Unit) {
        val post = { a: WebRtcAnswer ->
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(a) }
        }
        fun send(command: String) {
            sendCommand(command, build = {
                it.put("entity_id", entityId)
                it.put("offer", offerSdp)
            }, onResult = { msg ->
                if (msg.optBoolean("success", false)) {
                    post(parseWebRtcAnswer(msg))
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
                        thumbnail = c.optString("thumbnail").ifEmpty { null },
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

    /** Fetches an arbitrary image URL (media-source thumbnails) with Bearer auth, resolved against the instance. */
    suspend fun fetchImageBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val absolute = if (url.startsWith("http")) url else "$instanceUrl$url"
            val req = Request.Builder()
                .url(absolute)
                .header("Authorization", "Bearer $token")
                .build()
            val resp = http.newCall(req).execute()
            val stream = resp.body?.byteStream() ?: return@withContext null
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            null
        }
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
                val resp = http.newCall(req).execute()
                resp.isSuccessful
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

    fun todoCreate(entityId: String, summary: String) {
        sendCommand(ConfigCommands.TODO_CREATE) {
            it.put("entity_id", entityId)
            it.put("item", JSONObject().put("summary", summary))
        }
    }

    fun todoUpdate(entityId: String, uid: String, status: String) {
        sendCommand(ConfigCommands.TODO_UPDATE) {
            it.put("entity_id", entityId)
            it.put("item", JSONObject().put("uid", uid).put("status", status))
        }
    }

    fun todoDelete(entityId: String, uid: String) {
        sendCommand(ConfigCommands.TODO_DELETE) {
            it.put("entity_id", entityId)
            it.put("item", JSONObject().put("uid", uid))
        }
    }

    /** Numeric history for sparklines, via REST. Returns points sorted by time. */
    suspend fun fetchHistory(entityId: String, hours: Int): List<HistoryPoint> = withContext(Dispatchers.IO) {
        try {
            val start = Date(System.currentTimeMillis() - hours * 3600L * 1000L)
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            val url = "$instanceUrl/api/history/period/${fmt.format(start)}" +
                "?filter_entity_id=$entityId&minimal_response"
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            val resp = http.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
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
            emptyList()
        }
    }

    /** A camera entity's current snapshot, via the camera proxy. */
    suspend fun fetchCameraBitmap(entityId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$instanceUrl/api/camera_proxy/$entityId?cb=${System.currentTimeMillis()}")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = http.newCall(req).execute()
            val stream = resp.body?.byteStream() ?: return@withContext null
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
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
     * timeouts: their proxy stream only produces a frame per snapshot fetch.
     * Callbacks post to the main thread; cancel the returned Job to stop.
     */
    fun streamCamera(
        entityId: String,
        onState: (CameraStreamState) -> Unit,
    ): Job {
        val post = { state: CameraStreamState ->
            android.os.Handler(android.os.Looper.getMainLooper()).post { onState(state) }
        }
        return scope.launch(Dispatchers.IO) {
            post(CameraStreamState.Connecting)
            try {
                val req = Request.Builder()
                    .url("$instanceUrl/api/camera_proxy_stream/$entityId")
                    .header("Authorization", "Bearer $token")
                    .build()
                val resp = http.newCall(req).execute()
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
                val parser = MjpegParser(boundary)
                val body = resp.body?.byteStream()
                if (body == null) {
                    post(CameraStreamState.Error("Camera stream is empty"))
                    return@launch
                }
                var lastFrameAt = System.currentTimeMillis()
                val startedAt = lastFrameAt
                var sawFrame = false
                val failed = java.util.concurrent.atomic.AtomicBoolean(false)
                val buf = ByteArray(8192)
                // Watchdog: a CHILD of the stream job, so cancelling the
                // stream (viewer closed, Retry pressed) kills it too, and
                // its timeouts fire even while the read blocks.
                val watchdog = launch(Dispatchers.Default) {
                    while (true) {
                        delay(1000)
                        val now = System.currentTimeMillis()
                        if (!sawFrame && now - startedAt > FIRST_FRAME_TIMEOUT_MS) {
                            failed.set(true)
                            post(CameraStreamState.Error("Camera did not start streaming"))
                            break
                        }
                        if (sawFrame && now - lastFrameAt > STALL_TIMEOUT_MS) {
                            failed.set(true)
                            post(CameraStreamState.Error("Camera stream stalled"))
                            break
                        }
                    }
                }
                try {
                    while (!failed.get()) {
                        val read = body.read(buf)
                        if (read < 0) break
                        if (read == 0) continue
                        val frames = parser.feed(buf.copyOf(read))
                        for (jpeg in frames) {
                            val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                            if (bitmap != null) {
                                lastFrameAt = System.currentTimeMillis()
                                sawFrame = true
                                post(CameraStreamState.Playing(bitmap))
                            }
                        }
                    }
                    // Stream ended cleanly without ever producing a frame.
                    if (!sawFrame && !failed.get()) post(CameraStreamState.Error("Camera stream ended"))
                } finally {
                    watchdog.cancel()
                    try {
                        body.close()
                    } catch (ignored: Exception) {
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                post(CameraStreamState.Error("Could not reach the camera stream"))
            }
        }
    }

    /**
     * HA's stream path for a camera: WS `camera/stream` returns an HLS
     * playlist URL (relative, e.g. `/api/hls/<token>/playlist.m3u8`) when
     * the instance can stream the camera, or null when it cannot (unknown
     * entity, no stream source, stream integration unavailable). The
     * callback posts to the main thread; it may never fire if the socket
     * is down, so callers must time out and fall back (see
     * CameraFullscreen). This is the Ring true-live attempt: HA's own
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
     * An ExoPlayer configured for HA's HLS streams: the playlist URL is
     * resolved against the instance, and every playlist/segment request
     * carries the Bearer token (HLS URLs are authenticated). The caller
     * owns the player and must release it. HLS support lives in the
     * exoplayer artifact itself — no extra HLS module is needed.
     */
    fun createHlsPlayer(context: android.content.Context, playlistUrl: String): androidx.media3.exoplayer.ExoPlayer {
        val absolute = if (playlistUrl.startsWith("http")) playlistUrl else "$instanceUrl$playlistUrl"
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        val source = androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(absolute))
        return androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
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
        val absolute = if (mediaUrl.startsWith("http")) mediaUrl else "$instanceUrl$mediaUrl"
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        val item = androidx.media3.common.MediaItem.Builder()
            .setUri(absolute)
            .apply { if (mimeType != null) setMimeType(mimeType) }
            .build()
        return androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaSource(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(item),
            )
            prepare()
            playWhenReady = true
        }
    }

    companion object {
        /** Slow snapshot cameras need this long to produce a first frame. */
        const val FIRST_FRAME_TIMEOUT_MS = 20_000L

        /** No frame for this long after playback started means a dead stream. */
        const val STALL_TIMEOUT_MS = 30_000L
    }

    private fun parseIsoToMs(iso: String): Long? {
        if (iso.isEmpty()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        for (pattern in formats) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                return fmt.parse(iso)?.time
            } catch (e: Exception) {
                // Try the next pattern.
            }
        }
        return null
    }
}
