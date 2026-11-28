package com.garrietta.haven.core.net

import android.util.Log
import com.garrietta.haven.core.auth.AuthManager
import com.garrietta.haven.core.auth.Credentials
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

sealed interface ConnState {
    data object Idle : ConnState
    data class Connecting(val url: String) : ConnState
    data class Connected(val url: String, val isLocal: Boolean) : ConnState
    data class Waiting(val reason: String, val retryInMs: Long) : ConnState
    data object Offline : ConnState
    data object AuthRejected : ConnState
}

class HaError(val code: String, message: String) : Exception(message)

/** Pure connection planning, unit tested. */
object ConnectionPlan {
    /** LAN first when on an unmetered local network, remote first otherwise; never empty if any URL exists. */
    fun order(local: String?, remote: String?, onLocalNetwork: Boolean): List<String> {
        val l = listOfNotNull(local)
        val r = listOfNotNull(remote)
        return (if (onLocalNetwork) l + r else r + l).distinct()
    }

    fun wsUrl(base: String): String =
        base.trimEnd('/').replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/api/websocket"

    /** Backoff between full passes: 1s, 2s, 4s, 8s, 15s, then 30s. */
    fun backoffMs(failedPasses: Int): Long = when (failedPasses) {
        0 -> 1_000L; 1 -> 2_000L; 2 -> 4_000L; 3 -> 8_000L; 4 -> 15_000L; else -> 30_000L
    }

    fun connectTimeoutMs(url: String): Long =
        if (com.garrietta.haven.core.auth.isLanUrl(url)) 3_000L else 8_000L
}

/**
 * One WebSocket session to Home Assistant at a time.
 *
 * All mutable state is touched only from [loop] (a single-threaded
 * dispatcher), so there is no locking and no cross-socket confusion: every
 * socket callback carries its generation and is dropped if stale.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HaConnection(
    private val scope: CoroutineScope,
    private val http: OkHttpClient,
    private val auth: AuthManager,
    private val network: NetworkMonitor,
) {
    private val loop = Dispatchers.Default.limitedParallelism(1)
    private val wsHttp = http.newBuilder().pingInterval(0, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build()

    private val _state = MutableStateFlow<ConnState>(ConnState.Idle)
    val state: StateFlow<ConnState> = _state.asStateFlow()

    /** Base URL of the live session (REST calls use it). */
    @Volatile var activeBaseUrl: String? = null
        private set

    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, CancellableContinuation<JSONObject>>()
    private val subscriptions = ConcurrentHashMap<Int, Sub>()

    private class Sub(val gen: Int, val onEvent: (JSONObject) -> Unit, val onClosed: () -> Unit)

    private var socket: WebSocket? = null
    private var gen = 0
    private var authed = false
    private var runner: Job? = null
    private var lastPong = 0L
    private var authRetried = false

    /** Called (on the loop) after every successful auth, before anything else is sent. */
    var onConnected: (suspend () -> Unit)? = null

    private var watchingNetwork = false

    fun start() {
        runner?.cancel()
        runner = scope.launch(loop) { runForever() }
        if (!watchingNetwork) {
            watchingNetwork = true
            scope.launch {
                // Network changes (Wi-Fi ↔ cellular, VPN) trigger an immediate re-plan.
                network.changes.collect { if (runner != null) reconnectNow() }
            }
        }
    }

    fun stop() {
        runner?.cancel()
        runner = null
        scope.launch(loop) { closeSocket("stopped") }
        _state.value = ConnState.Idle
    }

    fun reconnectNow() {
        scope.launch(loop) {
            closeSocket("reconnect")
            runner?.cancel()
            runner = scope.launch(loop) { runForever() }
        }
    }

    private suspend fun runForever() {
        var failedPasses = 0
        while (scope.isActive) {
            val creds = auth.credentials ?: run { _state.value = ConnState.Idle; return }
            if (!network.isOnline()) {
                _state.value = ConnState.Offline
                delay(2_000)
                continue
            }
            val order = ConnectionPlan.order(creds.localUrl, creds.remoteUrl, network.isOnLocalNetwork())
            var connected = false
            for (base in order) {
                _state.value = ConnState.Connecting(base)
                val result = openAndAuth(base)
                if (result == OpenResult.AUTH_REJECTED) {
                    _state.value = ConnState.AuthRejected
                    return
                }
                if (result == OpenResult.OK) {
                    connected = true
                    failedPasses = 0
                    val isLocal = base == creds.localUrl
                    _state.value = ConnState.Connected(base, isLocal)
                    activeBaseUrl = base
                    try { onConnected?.invoke() } catch (e: Exception) { Log.w(TAG, "onConnected: ${e.message}") }
                    superviseUntilDown(creds, base, isLocal)
                    break
                }
            }
            activeBaseUrl = null
            if (!connected) {
                val wait = ConnectionPlan.backoffMs(failedPasses++)
                _state.value = ConnState.Waiting("Can't reach Home Assistant", wait)
                delay(wait)
            }
        }
    }

    private enum class OpenResult { OK, FAILED, AUTH_REJECTED }

    /** Opens a socket to [base] and completes the auth handshake within the endpoint's budget. */
    private suspend fun openAndAuth(base: String): OpenResult {
        closeSocket("new attempt")
        val myGen = ++gen
        authed = false
        val outcome = CompletableResult<OpenResult>()
        val req = Request.Builder().url(ConnectionPlan.wsUrl(base)).build()
        socket = wsHttp.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch(loop) { if (myGen == gen) onText(text, outcome) }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch(loop) { if (myGen == gen) socketDown("failure: ${t.message}", outcome) }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch(loop) { if (myGen == gen) socketDown("closed $code", outcome) }
            }
        })
        return try {
            withTimeout(ConnectionPlan.connectTimeoutMs(base) + 4_000) { outcome.await() }
        } catch (e: Exception) {
            closeSocket("handshake timeout")
            OpenResult.FAILED
        }
    }

    private suspend fun onText(text: String, handshake: CompletableResult<OpenResult>) {
        if (text.startsWith("[")) {
            val arr = try { JSONArray(text) } catch (_: Exception) { return }
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { onMessage(it, handshake) }
        } else {
            val obj = try { JSONObject(text) } catch (_: Exception) { return }
            onMessage(obj, handshake)
        }
    }

    private suspend fun onMessage(msg: JSONObject, handshake: CompletableResult<OpenResult>) {
        when (msg.optString("type")) {
            "auth_required" -> {
                val token = auth.accessToken(forceRefresh = authRetried)
                if (token == null) { handshake.complete(OpenResult.AUTH_REJECTED); return }
                socket?.send(JSONObject().put("type", "auth").put("access_token", token).toString())
            }
            "auth_ok" -> {
                authed = true
                authRetried = false
                lastPong = System.currentTimeMillis()
                // First message after auth: opt in to batched (coalesced) messages.
                socket?.send(
                    JSONObject().put("id", nextId.getAndIncrement()).put("type", "supported_features")
                        .put("features", JSONObject().put("coalesce_messages", 1)).toString(),
                )
                handshake.complete(OpenResult.OK)
            }
            "auth_invalid" -> {
                if (!authRetried && auth.credentials?.isLongLived == false) {
                    // An access token can expire between "expiresAt" and use: refresh once.
                    authRetried = true
                    handshake.complete(OpenResult.FAILED)
                } else {
                    handshake.complete(OpenResult.AUTH_REJECTED)
                }
            }
            "result" -> pending.remove(msg.optInt("id", -1))?.let { if (it.isActive) it.resume(msg) }
            "pong" -> {
                lastPong = System.currentTimeMillis()
                pending.remove(msg.optInt("id", -1))?.let { if (it.isActive) it.resume(msg) }
            }
            "event" -> {
                val sub = subscriptions[msg.optInt("id", -1)] ?: return
                val event = msg.optJSONObject("event") ?: return
                try { sub.onEvent(event) } catch (e: Exception) { Log.w(TAG, "event handler: ${e.message}") }
            }
        }
    }

    private var downSignal: CompletableResult<String>? = null

    private fun socketDown(reason: String, handshake: CompletableResult<OpenResult>) {
        socket = null
        authed = false
        handshake.complete(OpenResult.FAILED)
        downSignal?.complete(reason)
        failAll(reason)
    }

    /** Pings every 25s; returns when the socket drops, stops answering, or the LAN becomes reachable while on remote. */
    private suspend fun superviseUntilDown(creds: Credentials, base: String, isLocal: Boolean) {
        val down = CompletableResult<String>()
        downSignal = down
        var ticks = 0
        while (!down.isDone() && socket != null) {
            val waited = withTimeoutOrNullSafe(5_000) { down.await() }
            if (waited != null) break
            ticks++
            if (ticks % 5 == 0) {
                socket?.send(JSONObject().put("id", nextId.getAndIncrement()).put("type", "ping").toString())
                if (System.currentTimeMillis() - lastPong > 60_000) {
                    closeSocket("no pong")
                    break
                }
            }
            // On remote but the LAN is configured and we're on Wi-Fi: probe it every 30s and switch back.
            if (!isLocal && ticks % 6 == 0 && creds.localUrl != null && network.isOnLocalNetwork()) {
                if (probe(creds.localUrl)) {
                    Log.d(TAG, "LAN reachable again — switching from $base")
                    closeSocket("switch to LAN")
                    break
                }
            }
        }
        downSignal = null
        closeSocket("session over")
    }

    private suspend fun probe(base: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val quick = http.newBuilder().connectTimeout(2, TimeUnit.SECONDS).callTimeout(3, TimeUnit.SECONDS).build()
            quick.newCall(Request.Builder().url("$base/api/").build()).execute().use { it.code == 401 || it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun closeSocket(reason: String) {
        val s = socket ?: return
        socket = null
        gen++
        authed = false
        try { s.close(1000, reason) } catch (_: Exception) {}
        failAll(reason)
    }

    private fun failAll(reason: String) {
        val ids = pending.keys.toList()
        for (id in ids) pending.remove(id)?.let { if (it.isActive) it.resume(errorResult(NOT_CONNECTED, reason)) }
        val subs = subscriptions.keys.toList()
        for (id in subs) subscriptions.remove(id)?.let { s -> scope.launch { s.onClosed() } }
    }

    // ---- Public protocol API ---------------------------------------------

    val isConnected: Boolean get() = _state.value is ConnState.Connected

    /** Sends a command and returns its `result` payload; throws [HaError] on failure or timeout. */
    suspend fun call(type: String, timeoutMs: Long = 15_000, build: JSONObject.() -> Unit = {}): Any? {
        val msg = send(type, timeoutMs, build)
        if (!msg.optBoolean("success", false)) {
            val err = msg.optJSONObject("error")
            throw HaError(err?.optString("code") ?: "error", err?.optString("message")?.ifEmpty { null } ?: "Request failed")
        }
        return msg.opt("result")
    }

    private suspend fun send(type: String, timeoutMs: Long, build: JSONObject.() -> Unit): JSONObject {
        val id = nextId.getAndIncrement()
        val obj = JSONObject().put("id", id).put("type", type).apply(build)
        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    pending[id] = cont
                    cont.invokeOnCancellation { pending.remove(id) }
                    scope.launch(loop) {
                        val s = socket
                        if (s == null || !authed || !s.send(obj.toString())) {
                            pending.remove(id)?.let { if (it.isActive) it.resume(errorResult(NOT_CONNECTED, "Not connected")) }
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            pending.remove(id)
            errorResult("timeout", "Home Assistant didn't answer in time")
        }
    }

    /**
     * Opens a subscription. Events go to [onEvent] (on a background thread);
     * [onClosed] fires when the socket carrying it drops. Returns the id for
     * [unsubscribe], or throws if HA refused the subscription.
     */
    suspend fun subscribe(
        type: String,
        build: JSONObject.() -> Unit = {},
        onClosed: () -> Unit = {},
        onEvent: (JSONObject) -> Unit,
    ): Int {
        val id = nextId.getAndIncrement()
        val obj = JSONObject().put("id", id).put("type", type).apply(build)
        subscriptions[id] = Sub(gen, onEvent, onClosed)
        val result = try {
            withTimeout(15_000) {
                suspendCancellableCoroutine<JSONObject> { cont ->
                    pending[id] = cont
                    cont.invokeOnCancellation { pending.remove(id) }
                    scope.launch(loop) {
                        val s = socket
                        if (s == null || !authed || !s.send(obj.toString())) {
                            pending.remove(id)?.let { if (it.isActive) it.resume(errorResult(NOT_CONNECTED, "Not connected")) }
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            errorResult("timeout", "Subscription timed out")
        }
        if (!result.optBoolean("success", false)) {
            subscriptions.remove(id)
            val err = result.optJSONObject("error")
            throw HaError(err?.optString("code") ?: "error", err?.optString("message") ?: "Subscription refused")
        }
        return id
    }

    fun unsubscribe(id: Int) {
        if (subscriptions.remove(id) == null) return
        scope.launch {
            try { call("unsubscribe_events") { put("subscription", id) } } catch (_: Exception) {}
        }
    }

    /** Fire-and-forget command (trickle ICE etc.). */
    fun fire(type: String, build: JSONObject.() -> Unit) {
        scope.launch { try { call(type, build = build) } catch (_: Exception) {} }
    }

    private fun errorResult(code: String, message: String) = JSONObject()
        .put("success", false).put("error", JSONObject().put("code", code).put("message", message))

    companion object {
        const val TAG = "Haven"
        const val NOT_CONNECTED = "not_connected"
    }
}

/** Tiny one-shot result holder usable from the single-threaded loop. */
private class CompletableResult<T> {
    private val deferred = kotlinx.coroutines.CompletableDeferred<T>()
    fun complete(value: T) { deferred.complete(value) }
    fun isDone() = deferred.isCompleted
    suspend fun await(): T = deferred.await()
}

private suspend fun <T> withTimeoutOrNullSafe(ms: Long, block: suspend () -> T): T? =
    kotlinx.coroutines.withTimeoutOrNull(ms) { block() }
