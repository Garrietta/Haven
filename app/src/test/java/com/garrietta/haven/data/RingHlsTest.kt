package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests: Ring detection and HLS URL parsing are pure (JSONObject only). */
class RingHlsTest {

    private fun entity(attrs: Map<String, Any?> = emptyMap(), manufacturer: String? = null): HaEntity =
        HaEntity("camera.front_door_live_view", "idle", attrs, manufacturer = manufacturer)

    @Test
    fun brandAttributeDetectsRing() {
        assertTrue(isRingCamera(entity(mapOf("brand" to "Ring"))))
        assertTrue(isRingCamera(entity(mapOf("brand" to "ring"))))
    }

    @Test
    fun manufacturerDetectsRingWhenBrandMissing() {
        // The run #38 failure: state attributes carry no brand at all.
        assertTrue(isRingCamera(entity(manufacturer = "Ring")))
        assertTrue(isRingCamera(entity(manufacturer = "ring")))
    }

    @Test
    fun otherBrandsAndUnknownsAreNotRing() {
        assertFalse(isRingCamera(entity(mapOf("brand" to "Tapo"))))
        assertFalse(isRingCamera(entity(manufacturer = "TP-Link")))
        assertFalse(isRingCamera(entity()))
        assertFalse(isRingCamera(entity(mapOf("frontend_stream_type" to "web_rtc"))))
    }

    @Test
    fun hlsUrlParsedFromSuccessfulResult() {
        val msg = JSONObject(
            "{\"id\": 7, \"type\": \"result\", \"success\": true, " +
                "\"result\": {\"url\": \"/api/hls/abc123/playlist.m3u8\"}}",
        )
        assertEquals("/api/hls/abc123/playlist.m3u8", parseHlsStreamUrl(msg))
    }

    @Test
    fun hlsUrlNullOnFailureOrMissingUrl() {
        val failed = JSONObject(
            "{\"id\": 7, \"type\": \"result\", \"success\": false, " +
                "\"error\": {\"code\": \"unknown_error\", \"message\": \"nope\"}}",
        )
        assertNull(parseHlsStreamUrl(failed))
        val noUrl = JSONObject(
            "{\"id\": 7, \"type\": \"result\", \"success\": true, \"result\": {}}",
        )
        assertNull(parseHlsStreamUrl(noUrl))
        val blankUrl = JSONObject(
            "{\"id\": 7, \"type\": \"result\", \"success\": true, \"result\": {\"url\": \"\"}}",
        )
        assertNull(parseHlsStreamUrl(blankUrl))
    }
}
