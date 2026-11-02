package com.garrietta.haven.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Front Door live probe's honesty rule (Round 2):
 * the LIVE badge shows ONLY while frames render on a live path.
 * WebRTC frames always qualify; MJPEG frames qualify only while the
 * ring-mqtt live-stream switch is confirmed on (frames with the
 * switch off are the recording path, per the 2026-10-30 verdict).
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
    fun watchdogCap_isPositiveAndBounded() {
        // A cap must exist (never left on) but still allow a real
        // viewing session: minutes, not seconds or hours.
        assertTrue(FRONT_DOOR_LIVE_MAX_MS >= 60_000L)
        assertTrue(FRONT_DOOR_LIVE_MAX_MS <= 30 * 60 * 1000L)
    }
}
