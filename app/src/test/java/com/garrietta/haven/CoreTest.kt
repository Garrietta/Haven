package com.garrietta.haven

import com.garrietta.haven.core.auth.Pkce
import com.garrietta.haven.core.auth.isLanUrl
import com.garrietta.haven.core.auth.normalizeBaseUrl
import com.garrietta.haven.core.camera.MjpegParser
import com.garrietta.haven.core.camera.Transport
import com.garrietta.haven.core.camera.transportsFor
import com.garrietta.haven.core.model.EntityEntry
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.net.ConnectionPlan
import com.garrietta.haven.core.net.HaRest
import com.garrietta.haven.core.protocol.CompressedStates
import com.garrietta.haven.core.protocol.Registries
import com.garrietta.haven.core.store.HomeStore
import com.garrietta.haven.core.store.Optimistic
import com.garrietta.haven.core.store.OptimisticOverlay
import com.garrietta.haven.core.store.StateCache
import com.garrietta.haven.ui.screens.defaultFavorites
import com.garrietta.haven.ui.screens.isAlert
import com.garrietta.haven.ui.screens.visibleEntities
import com.garrietta.haven.ui.stateText
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressedStatesTest {
    // Shapes captured from the live instance (subscribe_entities).
    private val initial = JSONObject(
        """{"a":{
          "light.lamp_socket_1":{"s":"off","a":{"supported_color_modes":["onoff"],"friendly_name":"Lamp","supported_features":0},"c":"01M8G5","lc":1795838461.88},
          "camera.tapo_1_live_view":{"s":"idle","a":{"friendly_name":"Living Room Camera","supported_features":3},"c":"01M8G7","lc":1795838463.5,"lu":1795840853.8}
        }}""",
    )

    @Test fun initialSnapshotDecodes() {
        val m = CompressedStates.apply(emptyMap(), initial)
        assertEquals(2, m.size)
        val lamp = m.getValue("light.lamp_socket_1")
        assertEquals("off", lamp.state)
        assertEquals("Lamp", lamp.friendlyName)
        assertEquals(listOf("onoff"), lamp.attrStrings("supported_color_modes"))
        assertEquals(1795838461.88, lamp.lastUpdated, 1e-6) // lu defaults to lc
        assertEquals(1795840853.8, m.getValue("camera.tapo_1_live_view").lastUpdated, 1e-6)
    }

    @Test fun diffChangesStateAndAttributes() {
        val m = CompressedStates.apply(emptyMap(), initial)
        val next = CompressedStates.apply(m, JSONObject("""{"c":{"light.lamp_socket_1":{"+":{"s":"on","a":{"brightness":128},"lc":1795839000.0,"c":"x"}}}}"""))
        val lamp = next.getValue("light.lamp_socket_1")
        assertEquals("on", lamp.state)
        assertEquals(128, lamp.attrInt("brightness"))
        assertEquals("Lamp", lamp.friendlyName) // untouched attrs kept
        assertEquals(1795839000.0, lamp.lastChanged, 1e-6)
        assertEquals(1795839000.0, lamp.lastUpdated, 1e-6)
        assertSame(m["camera.tapo_1_live_view"], next["camera.tapo_1_live_view"])
    }

    @Test fun diffRemovesAttributes() {
        val m = CompressedStates.apply(emptyMap(), initial)
        val next = CompressedStates.apply(m, JSONObject("""{"c":{"light.lamp_socket_1":{"-":{"a":["supported_features"]}}}}"""))
        assertNull(next.getValue("light.lamp_socket_1").attributes["supported_features"])
    }

    @Test fun removalAndUnknownDiffs() {
        val m = CompressedStates.apply(emptyMap(), initial)
        val next = CompressedStates.apply(m, JSONObject("""{"r":["camera.tapo_1_live_view"],"c":{"light.ghost":{"+":{"s":"on"}}}}"""))
        assertEquals(setOf("light.lamp_socket_1"), next.keys)
    }

    @Test fun noChangeReturnsSameMap() {
        val m = CompressedStates.apply(emptyMap(), initial)
        assertSame(m, CompressedStates.apply(m, JSONObject("{}")))
    }

    @Test fun registryForDisplay() {
        val r = JSONObject(
            """{"entity_categories":{"0":"config","1":"diagnostic"},"entities":[
              {"ei":"light.lamp_socket_1","pl":"switch_as_x","ai":"living_room","di":"83ae","en":"Lamp"},
              {"ei":"sensor.robovac_battery","pl":"robovac_mqtt","di":"e849","ec":1},
              {"ei":"switch.x","hb":true}]}""",
        )
        val list = Registries.entitiesForDisplay(r).associateBy { it.entityId }
        assertEquals("living_room", list.getValue("light.lamp_socket_1").areaId)
        assertEquals("diagnostic", list.getValue("sensor.robovac_battery").category)
        assertTrue(list.getValue("switch.x").hidden)
    }
}

class AuthAndPlanTest {
    @Test fun pkceChallengeMatchesRfc7636Example() {
        // RFC 7636 appendix B.
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        assertTrue(Pkce.newVerifier().length >= 43)
    }

    @Test fun normalizeUrls() {
        assertEquals("http://10.0.0.11:8123", normalizeBaseUrl("10.0.0.11:8123"))
        assertEquals("http://homeassistant.local:8123", normalizeBaseUrl("homeassistant.local"))
        assertEquals("https://abc.ui.nabu.casa", normalizeBaseUrl("https://abc.ui.nabu.casa/lovelace/0"))
        assertNull(normalizeBaseUrl("  "))
    }

    @Test fun lanDetection() {
        assertTrue(isLanUrl("http://10.0.0.11:8123"))
        assertTrue(isLanUrl("http://192.168.1.4:8123"))
        assertTrue(isLanUrl("http://homeassistant.local:8123"))
        assertFalse(isLanUrl("https://abc.ui.nabu.casa"))
        assertFalse(isLanUrl("http://172.40.0.1"))
    }

    @Test fun connectionOrder() {
        assertEquals(listOf("L", "R"), ConnectionPlan.order("L", "R", onLocalNetwork = true))
        assertEquals(listOf("R", "L"), ConnectionPlan.order("L", "R", onLocalNetwork = false))
        assertEquals(listOf("R"), ConnectionPlan.order(null, "R", true))
        assertEquals("ws://10.0.0.11:8123/api/websocket", ConnectionPlan.wsUrl("http://10.0.0.11:8123"))
        assertEquals("wss://x.ui.nabu.casa/api/websocket", ConnectionPlan.wsUrl("https://x.ui.nabu.casa/"))
        assertEquals(30_000L, ConnectionPlan.backoffMs(9))
    }
}

class StoreAndUiLogicTest {
    private fun e(id: String, state: String, attrs: Map<String, Any?> = emptyMap()) = HaEntity(id, state, attrs)

    @Test fun optimisticHoldsUntilConfirmedOrExpired() {
        val server = mapOf("light.a" to e("light.a", "off"))
        val edit = Optimistic(e("light.a", "on"), expiresAtMs = 10_000) { it.state == "on" }
        assertEquals("on", OptimisticOverlay.merge(server, mapOf("light.a" to edit), 5_000).getValue("light.a").state)
        // Server confirms → server value shows.
        val confirmed = mapOf("light.a" to e("light.a", "on", mapOf("brightness" to 10)))
        assertEquals(10, OptimisticOverlay.merge(confirmed, mapOf("light.a" to edit), 5_000).getValue("light.a").attrInt("brightness"))
        // Expired → server value shows.
        assertEquals("off", OptimisticOverlay.merge(server, mapOf("light.a" to edit), 10_001).getValue("light.a").state)
    }

    @Test fun activeRules() {
        assertTrue(HomeStore.isActive(e("lock.f", "unlocked")))
        assertFalse(HomeStore.isActive(e("lock.f", "locked")))
        assertTrue(HomeStore.isActive(e("cover.g", "opening")))
        assertFalse(HomeStore.isActive(e("climate.t", "off")))
        assertTrue(HomeStore.isActive(e("media_player.tv", "paused")))
    }

    @Test fun stateTexts() {
        assertEquals("50%", stateText(e("light.a", "on", mapOf("brightness" to 128))))
        assertEquals("Off", stateText(e("light.a", "off")))
        assertEquals("67.5°F", stateText(e("sensor.t", "67.5", mapOf("unit_of_measurement" to "°F"))))
        assertEquals("21 W", stateText(e("sensor.p", "21.0", mapOf("unit_of_measurement" to "W"))))
        assertEquals("Open", stateText(e("binary_sensor.d", "on", mapOf("device_class" to "door"))))
        assertEquals("Heating to 70°", stateText(e("climate.t", "heat", mapOf("hvac_action" to "heating", "temperature" to 70.0))))
        assertEquals("Heat cool · 67–70°", stateText(e("climate.t", "heat_cool", mapOf("hvac_action" to "idle", "target_temp_low" to 67.0, "target_temp_high" to 70.0))))
        assertEquals("Unavailable", stateText(e("light.a", "unavailable")))
    }

    @Test fun visibleFiltersHiddenDiagnosticAndSystem() {
        val states = listOf(e("light.a", "on"), e("sensor.diag", "1"), e("update.x", "off"), e("switch.hidden", "on"), e("sun.sun", "above_horizon"))
            .associateBy { it.entityId }
        val entries = mapOf(
            "sensor.diag" to EntityEntry("sensor.diag", category = "diagnostic"),
            "switch.hidden" to EntityEntry("switch.hidden", hidden = true),
        )
        assertEquals(listOf("light.a"), visibleEntities(states, entries).map { it.entityId })
    }

    @Test fun alertsAndDefaults() {
        assertTrue(isAlert(e("binary_sensor.door", "on", mapOf("device_class" to "door"))))
        assertFalse(isAlert(e("binary_sensor.motion", "on", mapOf("device_class" to "motion"))))
        assertTrue(isAlert(e("cover.garage", "open", mapOf("device_class" to "garage"))))
        assertTrue(isAlert(e("lock.front", "unlocked")))
        val favs = defaultFavorites(listOf(e("switch.s", "on"), e("light.l", "on"), e("sensor.x", "1")))
        assertEquals(listOf("light.l", "switch.s"), favs)
    }

    @Test fun transportOrder() {
        assertEquals(listOf(Transport.WEBRTC, Transport.HLS, Transport.MJPEG), transportsFor(listOf("hls", "web_rtc"), true))
        assertEquals(listOf(Transport.HLS, Transport.MJPEG), transportsFor(listOf("hls", "web_rtc"), false))
        assertEquals(listOf(Transport.MJPEG), transportsFor(emptyList(), true))
    }

    @Test fun cacheRoundTripDropsCameraTokens() {
        val states = mapOf("camera.c" to e("camera.c", "idle", mapOf("access_token" to "secret", "friendly_name" to "Cam", "list" to listOf(1, 2))))
        val back = StateCache.decode(StateCache.encode(states))
        assertEquals("Cam", back.getValue("camera.c").friendlyName)
        assertNull(back.getValue("camera.c").attributes["access_token"])
        assertEquals(listOf(1, 2), back.getValue("camera.c").attributes["list"])
    }

    @Test fun historyParsing() {
        val json = JSONArray("""[[{"state":"66.7","last_changed":"2026-11-27T22:00:00+00:00"},{"state":"unavailable","last_changed":"2026-11-27T22:05:00+00:00"},{"state":"67.1","last_changed":"2026-11-27T22:10:00+00:00"}]]""")
        val pts = HaRest.parseHistory(json)
        assertEquals(2, pts.size)
        assertEquals(67.1, pts[1].value, 1e-9)
        assertEquals(600.0, pts[1].timeSec - pts[0].timeSec, 1e-6)
    }

    @Test fun mjpegParserRandomChunks() {
        val frames = List(10) { i -> ByteArray(50_000 + i) { (it + i).toByte() } }
        val stream = frames.fold(ByteArray(0)) { acc, f ->
            acc + "--b\r\nContent-Type: image/jpeg\r\nContent-Length: ${f.size}\r\n\r\n".toByteArray() + f + "\r\n".toByteArray()
        }
        val p = MjpegParser("b")
        val out = mutableListOf<ByteArray>()
        var pos = 0
        val rnd = kotlin.random.Random(7)
        while (pos < stream.size) { val n = minOf(stream.size - pos, 1 + rnd.nextInt(9000)); out += p.feed(stream, pos, n); pos += n }
        assertEquals(10, out.size)
        frames.indices.forEach { assertTrue(frames[it].contentEquals(out[it])) }
    }
}
