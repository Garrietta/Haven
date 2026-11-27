package com.garrietta.haven.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.FakeNet
import okhttp3.FakeSocket
import okhttp3.Response
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class EngineTest {
    private lateinit var scope: CoroutineScope
    private lateinit var client: HaClient
    private val local = "http://10.0.0.11:8123"
    private val cloud = "https://abc.ui.nabu.casa"

    @Before fun setUp() {
        FakeNet.reset()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        client = HaClient(scope, okhttp3.OkHttpClient())
    }
    @After fun tearDown() { client.disconnect(); scope.cancel() }

    private fun waitFor(ms: Long = 2000, what: String = "condition", cond: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) { if (cond()) return; Thread.sleep(10) }
        fail("timed out waiting for $what")
    }
    private fun sock(i: Int): FakeSocket { waitFor(what = "socket #$i") { FakeNet.sockets.size > i }; return FakeNet.sockets[i] }
    private fun authOk(s: FakeSocket) { s.serverSays("""{"type":"auth_required"}"""); s.serverSays("""{"type":"auth_ok"}""") }
    private fun sentTypes(s: FakeSocket) = s.sent.map { JSONObject(it).optString("type") }

    @Test fun fallsBackToCloudWhenLanFails_andAuthsWithToken() {
        client.connect(listOf(local, cloud), "tok1")
        val s0 = sock(0)
        assertEquals("ws://10.0.0.11:8123/api/websocket", s0.url)
        s0.fail("No route to host")
        val s1 = sock(1)
        assertEquals("wss://abc.ui.nabu.casa/api/websocket", s1.url)
        s1.serverSays("""{"type":"auth_required"}""")
        assertEquals("tok1", JSONObject(s1.sent[0]).getString("access_token"))
        s1.serverSays("""{"type":"auth_ok"}""")
        waitFor(what = "connected") { client.connection.value == ConnectionState.Connected }
        assertEquals(cloud, client.activeUrl.value)
        val sub = s1.sent.map { JSONObject(it) }.first { it.optString("type") == "subscribe_events" }
        assertEquals("state_changed", sub.optString("event_type"))
        assertTrue("get_states sent", sentTypes(s1).contains("get_states"))
    }

    @Test fun staleSocketCannotClobberLiveSession() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.activeUrl.value == local }
        // explicit reconnect (e.g. mode switch) -> new socket; old one retired
        client.connect(listOf(cloud), "tok")
        val s1 = sock(1); authOk(s1)
        waitFor { client.activeUrl.value == cloud }
        // events on the retired socket are ignored; its later failure changes nothing
        s0.serverSays("""{"id":1,"type":"event","event":{"event_type":"state_changed","data":{"entity_id":"light.a","new_state":{"entity_id":"light.a","state":"on","attributes":{}}}}}""")
        s0.fail("boom")
        Thread.sleep(200)
        assertNull(client.states.value["light.a"])
        assertEquals(ConnectionState.Connected, client.connection.value)
        assertEquals(cloud, client.activeUrl.value)
        assertEquals("no extra sockets", 2, FakeNet.sockets.size)
        // events on the live socket apply
        s1.serverSays("""{"id":1,"type":"event","event":{"event_type":"state_changed","data":{"entity_id":"light.a","new_state":{"entity_id":"light.a","state":"on","attributes":{}}}}}""")
        waitFor { client.states.value["light.a"]?.state == "on" }
    }

    @Test fun inFlightCallFailsPromptlyWhenSessionDrops() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        val result = java.util.concurrent.atomic.AtomicReference<Boolean?>(null)
        client.callService("light", "toggle", "light.a") { ok -> result.set(ok) }
        s0.fail("Software caused connection abort")
        waitFor(what = "callback") { result.get() != null }
        assertEquals(false, result.get())
        assertTrue(client.lastServiceError.value!!.startsWith("Not connected"))
        // and a reconnect pass started immediately
        sock(1)
    }

    @Test fun commandWithoutSessionFailsInsteadOfHanging() {
        val got = java.util.concurrent.atomic.AtomicReference<JSONObject?>(null)
        client.sendCommand("camera/stream", build = { it.put("entity_id", "camera.x") }, onResult = { got.set(it) })
        waitFor { got.get() != null }
        assertFalse(got.get()!!.optBoolean("success", true))
        assertEquals(HaClient.NOT_CONNECTED, got.get()!!.getJSONObject("error").getString("code"))
    }

    @Test fun resultsStillRouteFromADrainingLink() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        val got = java.util.concurrent.atomic.AtomicReference<JSONObject?>(null)
        val id = client.sendCommand("network/url", onResult = { got.set(it) })
        client.connect(listOf(cloud), "tok") // retires s0 with drain
        s0.serverSays("""{"id":$id,"type":"result","success":true,"result":{"cloud":"$cloud"}}""")
        waitFor { got.get() != null }
        assertTrue(got.get()!!.optBoolean("success"))
    }

    @Test fun authInvalidNeverAutoReconnects() {
        val calls = AtomicInteger(0)
        client.onAuthInvalid = { calls.incrementAndGet() }
        client.connect(listOf(local, cloud), "bad")
        val s0 = sock(0)
        s0.serverSays("""{"type":"auth_required"}""")
        s0.serverSays("""{"type":"auth_invalid","message":"Invalid access token"}""")
        Thread.sleep(1500)
        assertEquals(1, calls.get())
        assertEquals("no retry with a rejected token", 1, FakeNet.sockets.size)
        val st = client.connection.value
        assertTrue(st is ConnectionState.Error && !st.retrying)
        client.reconnectNow(); client.ensureAlive(); client.setNetworkAvailable(false); client.setNetworkAvailable(true)
        Thread.sleep(200)
        assertEquals("still no retry", 1, FakeNet.sockets.size)
        // a renewed token + connect starts over
        client.connect(listOf(local, cloud), "good")
        sock(1)
    }

    @Test fun upgradesBackToLanMakeBeforeBreak() {
        FakeNet.http = { req ->
            if (req.url.startsWith(local) && req.url.endsWith("/manifest.json")) Response(req, 200, """{"name":"Home Assistant"}""")
            else Response(req, 503, "")
        }
        client.connect(listOf(local, cloud), "tok")
        sock(0).fail("unreachable")
        val s1 = sock(1); authOk(s1)
        waitFor { client.activeUrl.value == cloud }
        // network settles (e.g. Wi-Fi came back): same list, session on fallback -> immediate probe
        client.updateEndpoints(listOf(local, cloud), "tok")
        val s2 = sock(2, )
        assertTrue(s2.url.startsWith("ws://10.0.0.11"))
        assertEquals("old session still up during upgrade", cloud, client.activeUrl.value)
        authOk(s2)
        waitFor { client.activeUrl.value == local }
        assertEquals(ConnectionState.Connected, client.connection.value)
        assertFalse("old link drains before closing", s1.closed)
        waitFor(7000, "drain close") { s1.closed }
        assertEquals(ConnectionState.Connected, client.connection.value)
        assertEquals(local, client.activeUrl.value)
    }

    @Test fun networkReturnRetriesImmediately() {
        client.setNetworkAvailable(false)
        client.connect(listOf(cloud), "tok")
        sock(0).fail("no network")
        Thread.sleep(1500)
        assertEquals("paused while offline", 1, FakeNet.sockets.size)
        val st = client.connection.value
        assertTrue(st is ConnectionState.Error && st.retrying)
        client.setNetworkAvailable(true)
        sock(1)
    }

    @Test fun endpointUpdateKeepsHealthySessionOnPreferred() {
        client.connect(listOf(local, cloud), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.activeUrl.value == local }
        client.updateEndpoints(listOf(local, cloud), "tok")
        Thread.sleep(200)
        assertEquals(1, FakeNet.sockets.size)
        // mode switch that excludes the current endpoint replaces it
        client.updateEndpoints(listOf(cloud), "tok")
        sock(1)
    }

    @Test fun pingWithoutPongForcesReconnect() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        client.ensureAlive()
        assertTrue(sentTypes(s0).contains("ping"))
        waitFor(7000, "reconnect after missing pong") { FakeNet.sockets.size > 1 }
    }

    @Test fun pongKeepsSession() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        client.ensureAlive()
        val ping = s0.sent.map { JSONObject(it) }.last { it.optString("type") == "ping" }
        s0.serverSays("""{"id":${ping.getInt("id")},"type":"pong"}""")
        Thread.sleep(5600)
        assertEquals(1, FakeNet.sockets.size)
        assertFalse(s0.closed)
    }

    @Test fun rest401RenewsOnceThenPauses() {
        client.connect(listOf(local), "old")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        val refreshes = AtomicInteger(0)
        client.tokenRefresher = { stale -> refreshes.incrementAndGet(); if (stale == "old") "new" else null }
        FakeNet.http = { req ->
            if (req.header("Authorization") == "Bearer new") Response(req, 200, "[]") else Response(req, 401, "")
        }
        kotlinx.coroutines.runBlocking { client.fetchHistory("sensor.t", 24) }
        assertEquals(1, refreshes.get())
        assertEquals("new", client.currentToken())
        // now the instance refuses everything and no renewal is possible -> REST pauses
        client.tokenRefresher = { null }
        FakeNet.http = { req -> Response(req, 401, "") }
        FakeNet.requests.clear()
        kotlinx.coroutines.runBlocking { client.fetchHistory("sensor.t", 24) }
        val first = FakeNet.requests.size
        kotlinx.coroutines.runBlocking { client.fetchHistory("sensor.t", 24); client.fetchCameraSnapshot("camera.x", 640) }
        assertEquals("no further 401s once rejected", first, FakeNet.requests.size)
        client.updateToken("newer")
        kotlinx.coroutines.runBlocking { client.fetchHistory("sensor.t", 24) }
        assertTrue(FakeNet.requests.size > first)
    }

    @Test fun snapshotAsksForWidthAndInstanceOnlyBearer() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        FakeNet.http = { req -> Response(req, 200, "jpeg") }
        kotlinx.coroutines.runBlocking { client.fetchCameraSnapshot("camera.garage", 640) }
        val r = FakeNet.requests.last()
        assertTrue(r.url, r.url.startsWith("$local/api/camera_proxy/camera.garage?width=640&cb="))
        assertEquals("Bearer tok", r.header("Authorization"))
        kotlinx.coroutines.runBlocking { client.fetchImageBitmap("https://cdn.example.com/thumb.jpg") }
        assertNull("no token to foreign hosts", FakeNet.requests.last().header("Authorization"))
        kotlinx.coroutines.runBlocking { client.fetchImageBitmap("/api/media_source/thumb") }
        assertEquals("Bearer tok", FakeNet.requests.last().header("Authorization"))
    }

    @Test fun todoEditsAreServiceCallsWithPayload() {
        client.connect(listOf(local), "tok")
        val s0 = sock(0); authOk(s0)
        waitFor { client.connection.value == ConnectionState.Connected }
        client.todoCreate("todo.shopping", "Milk")
        val msg = s0.sent.map { JSONObject(it) }.last()
        assertEquals("call_service", msg.getString("type"))
        assertEquals("todo", msg.getString("domain"))
        assertEquals("add_item", msg.getString("service"))
        assertEquals("todo.shopping", msg.getJSONObject("target").getString("entity_id"))
        assertEquals("Milk", msg.getJSONObject("service_data").getString("item"))
    }
}
