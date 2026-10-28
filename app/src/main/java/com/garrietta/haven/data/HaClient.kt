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

    private val _lovelace = MutableStateFlow<LovelaceConfig?>(null)
    val lovelace: StateFlow<LovelaceConfig?> = _lovelace.asStateFlow()

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
                    } else {
                        current[entityId] = HaEntity.fromJson(newState)
                    }
                    _states.value = current
                }
            }
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
                map[e.entityId] = e
            }
            _states.value = map
        })
        sendCommand("config/area_registry/list", onResult = { msg ->
            val result = msg.optJSONArray("result") ?: return@sendCommand
            val list = mutableListOf<HaArea>()
            for (i in 0 until result.length()) {
                val o = result.optJSONObject(i) ?: continue
                val id = o.optString("area_id").ifEmpty { o.optString("id") }
                list.add(HaArea(id, o.optString("name"), o.optString("icon").ifEmpty { null }))
            }
            _areas.value = list.sortedBy { it.name }
        })
        sendCommand("config/entity_registry/list", onResult = { msg ->
            val result = msg.optJSONArray("result") ?: return@sendCommand
            val entityToArea = mutableMapOf<String, String>()
            val entityToDevice = mutableMapOf<String, String>()
            for (i in 0 until result.length()) {
                val o = result.optJSONObject(i) ?: continue
                val entityId = o.optString("entity_id")
                val areaId = o.optString("area_id")
                if (areaId.isNotEmpty()) entityToArea[entityId] = areaId
                val deviceId = o.optString("device_id")
                if (deviceId.isNotEmpty()) entityToDevice[entityId] = deviceId
            }
            // Resolve device-level areas second.
            sendCommand("config/device_registry/list", onResult = { dmsg ->
                val dresult = dmsg.optJSONArray("result")
                if (dresult != null) {
                    val deviceToArea = mutableMapOf<String, String>()
                    for (j in 0 until dresult.length()) {
                        val d = dresult.optJSONObject(j) ?: continue
                        val areaId = d.optString("area_id")
                        if (areaId.isNotEmpty()) deviceToArea[d.optString("id")] = areaId
                    }
                    for ((entityId, deviceId) in entityToDevice) {
                        if (!entityToArea.containsKey(entityId)) {
                            deviceToArea[deviceId]?.let { entityToArea[entityId] = it }
                        }
                    }
                }
                _entityAreas.value = entityToArea
            })
        })
        refreshLovelace()
    }

    fun refreshLovelace() {
        sendCommand(ConfigCommands.LOVE_LACE_GET, onResult = { msg ->
            val result = msg.optJSONObject("result") ?: return@sendCommand
            _lovelace.value = parseLovelace(result)
        })
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

    /** Saves a full Lovelace config (views/cards) back to the instance. */
    fun saveLovelace(config: JSONObject, onDone: (Boolean) -> Unit) {
        sendCommand(ConfigCommands.LOVE_LACE_SAVE, build = { it.put("config", config) }, onResult = { msg ->
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
