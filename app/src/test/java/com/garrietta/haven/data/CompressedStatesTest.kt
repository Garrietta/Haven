package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Backend round: subscribe_entities decoding on the 1.x model (shapes from the live instance). */
class CompressedStatesTest {
    private val snapshot = JSONObject(
        """{"a":{
          "light.lamp_socket_1":{"s":"off","a":{"supported_color_modes":["onoff"],"friendly_name":"Lamp"},"c":"01M8G5","lc":1795838461.0},
          "camera.tapo_1_live_view":{"s":"idle","a":{"friendly_name":"Living Room Camera"},"c":"01M8G7","lc":1795838463.0,"lu":1795840853.0}
        }}""",
    )

    @Test fun snapshotDecodesWithIsoTimestamps() {
        val r = CompressedStateStream.apply(emptyMap(), snapshot)
        val lamp = r.states.getValue("light.lamp_socket_1")
        assertEquals("off", lamp.state)
        assertEquals("Lamp", lamp.friendlyName)
        assertEquals(java.time.Instant.ofEpochSecond(1795838461).toString(), lamp.lastChanged)
        assertEquals(lamp.lastChanged, lamp.lastUpdated)
        assertEquals(java.time.Instant.ofEpochSecond(1795840853).toString(), r.states.getValue("camera.tapo_1_live_view").lastUpdated)
        // Parses the way the rest of 1.x reads timestamps.
        java.time.OffsetDateTime.parse(lamp.lastChanged)
    }

    @Test fun diffKeepsUntouchedAttributesAndReportsParent() {
        val base = CompressedStateStream.apply(emptyMap(), snapshot).states
        val r = CompressedStateStream.apply(
            base,
            JSONObject("""{"c":{"light.lamp_socket_1":{"+":{"s":"on","a":{"brightness":200},"lc":1795839000.0,"c":{"id":"x","parent_id":"auto1","user_id":null}}}}}"""),
        )
        val lamp = r.states.getValue("light.lamp_socket_1")
        assertEquals("on", lamp.state)
        assertEquals(200, lamp.attrInt("brightness"))
        assertEquals("Lamp", lamp.friendlyName)
        assertEquals(1, r.changes.size)
        assertEquals("off", r.changes[0].old?.state)
        assertEquals("auto1", r.changes[0].parentId)
        assertSame(base["camera.tapo_1_live_view"], r.states["camera.tapo_1_live_view"])
    }

    @Test fun removalsAndAttributeDeletes() {
        val base = CompressedStateStream.apply(emptyMap(), snapshot).states
        val r = CompressedStateStream.apply(base, JSONObject("""{"r":["camera.tapo_1_live_view"],"c":{"light.lamp_socket_1":{"-":{"a":["supported_color_modes"]}}}}"""))
        assertEquals(setOf("light.lamp_socket_1"), r.states.keys)
        assertNull(r.states.getValue("light.lamp_socket_1").attributes["supported_color_modes"])
        assertTrue(r.changes.any { it.new == null && it.entityId == "camera.tapo_1_live_view" })
    }

    @Test fun emptyEventChangesNothing() {
        val base = CompressedStateStream.apply(emptyMap(), snapshot).states
        assertSame(base, CompressedStateStream.apply(base, JSONObject("{}")).states)
    }
}
