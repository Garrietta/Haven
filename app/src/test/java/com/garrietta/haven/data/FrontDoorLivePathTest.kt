package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where the doorbell's live stream is, and what the panel says when it cannot be shown. */
class FrontDoorLivePathTest {

    @Test
    fun livePathComesFromWhatRingMqttReports() {
        // The add-on addresses the stream to its own internal hostname;
        // only the path is usable from a phone.
        assertEquals(
            "5c475ecd13a7_live",
            ringMqttLivePath(mapOf("stream_Source" to "rtsp://03cabcc9-ring-mqtt:8554/5c475ecd13a7_live")),
        )
        // The key's name does not matter, the value's shape does.
        assertEquals("abc_live", ringMqttLivePath(mapOf("anything" to "RTSP://host:8554/abc_live", "n" to 3)))
        // The recorded-event stream is not the live one.
        assertNull(ringMqttLivePath(mapOf("event" to "rtsp://host:8554/abc_event")))
        assertNull(ringMqttLivePath(mapOf("still_Image_URL" to "https://host/snap.jpg")))
        assertNull(ringMqttLivePath(emptyMap()))
        assertNull(ringMqttLivePath(null))
    }

    @Test
    fun aReportedPathReplacesTheDefaultOne() {
        assertEquals("rtsp://10.0.0.11:8554/abc_live", frontDoorRtspUrl("http://10.0.0.11:8123", "abc_live"))
        assertEquals("rtsp://10.0.0.11:8554/$FRONT_DOOR_RTSP_PATH", frontDoorRtspUrl("http://10.0.0.11:8123"))
        // Off the home network there is no stream to reach, whatever the path.
        assertNull(frontDoorRtspUrl("https://abc.ui.nabu.casa", "abc_live"))
    }

    @Test
    fun aFallbackSaysItIsARecordingAndWhy() {
        val away = frontDoorNotLiveLine(null)
        val unreachable = frontDoorNotLiveLine("rtsp://10.0.0.11:8554/abc_live")
        assertTrue(away.contains("home network") && away.contains("recording"))
        assertTrue(unreachable.contains("10.0.0.11:8554") && unreachable.contains("recording"))
    }
}
