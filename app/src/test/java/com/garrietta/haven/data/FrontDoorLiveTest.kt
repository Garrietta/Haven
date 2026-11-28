package com.garrietta.haven.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Front Door live probe's honesty rule (Round 2,
 * corrected Round 5): the LIVE badge shows ONLY while frames render
 * on a live path. WebRTC frames always qualify; MJPEG frames qualify
 * only while the ring-mqtt live-stream switch is confirmed on AND
 * only when they come from the ring-mqtt live camera
 * (camera.front_door_69a1) — the panel feeds this function solely
 * with 69a1 frames. live_view proxy frames are the recording path
 * even with the switch on (the #93 rule's error, superseded Round
 * 5), so they never reach this function; frames with the switch
 * off are likewise recordings.
 */
class FrontDoorLiveTest {

    @Test
    fun webrtcFrames_showBadgeRegardlessOfSwitch() {
        assertTrue(frontDoorShowsLiveBadge(webrtcFrames = true, mjpegFrames = false, liveSwitchOn = false))
        assertTrue(frontDoorShowsLiveBadge(webrtcFrames = true, mjpegFrames = false, liveSwitchOn = true))
    }

    @Test
    fun mjpegFrames_showBadgeOnlyWhileSwitchOn() {
        assertTrue(frontDoorShowsLiveBadge(webrtcFrames = false, mjpegFrames = true, liveSwitchOn = true))
        assertFalse(frontDoorShowsLiveBadge(webrtcFrames = false, mjpegFrames = true, liveSwitchOn = false))
    }

    @Test
    fun noFrames_neverShowBadge() {
        assertFalse(frontDoorShowsLiveBadge(webrtcFrames = false, mjpegFrames = false, liveSwitchOn = true))
        assertFalse(frontDoorShowsLiveBadge(webrtcFrames = false, mjpegFrames = false, liveSwitchOn = false))
    }

    @Test
    fun ringMqttLiveCameraId_isThe69a1Entity() {
        // Round 5 Bug C: the live path streams the ring-mqtt
        // camera, not the core Ring live_view entity (whose proxy
        // serves only the last motion recording).
        assertTrue(FRONT_DOOR_RING_MQTT_CAMERA_ID == "camera.front_door_69a1")
        assertTrue(FRONT_DOOR_RING_MQTT_CAMERA_ID != FRONT_DOOR_LIVE_VIEW_ID)
    }

    @Test
    fun watchdogCap_isPositiveAndBounded() {
        // A cap must exist (never left on) but still allow a real
        // viewing session: minutes, not seconds or hours.
        assertTrue(FRONT_DOOR_LIVE_MAX_MS >= 60_000L)
        assertTrue(FRONT_DOOR_LIVE_MAX_MS <= 30 * 60 * 1000L)
    }
}
