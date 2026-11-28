package com.garrietta.haven.data

import android.app.ApplicationExitInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraLiveRulesTest {

    private fun e(id: String, state: String = "idle", vararg attrs: Pair<String, Any?>) = HaEntity(id, state, mapOf(*attrs))

    @Test
    fun aCameraWithABatteryOnItsDeviceIsABatteryCamera() {
        val states = listOf(
            e("camera.outdoor"),
            e("sensor.outdoor_battery", "76", "device_class" to "battery"),
            e("camera.garage"),
            e("sensor.garage_signal", "-60", "device_class" to "signal_strength"),
        ).associateBy { it.entityId }
        val devices = mapOf(
            "camera.outdoor" to "dev-outdoor",
            "sensor.outdoor_battery" to "dev-outdoor",
            "camera.garage" to "dev-garage",
            "sensor.garage_signal" to "dev-garage",
        )
        assertTrue(cameraHasBattery("camera.outdoor", states, devices))
        assertFalse(cameraHasBattery("camera.garage", states, devices))
        // No device known: only the camera's own battery level counts.
        assertFalse(cameraHasBattery("camera.outdoor", states, emptyMap()))
        val withLevel = mapOf("camera.porch" to e("camera.porch", "idle", "battery_level" to 80))
        assertTrue(cameraHasBattery("camera.porch", withLevel, emptyMap()))
    }

    @Test
    fun withoutCapabilitiesTheCameraStateDecidesTheTransport() {
        assertEquals(listOf("web_rtc"), fallbackStreamTypes(e("camera.front_door", "idle", "brand" to "Ring")))
        assertEquals(listOf("hls"), fallbackStreamTypes(e("camera.garage", "idle", "supported_features" to 2)))
        assertEquals(listOf("web_rtc"), fallbackStreamTypes(e("camera.x", "idle", "frontend_stream_type" to "web_rtc", "supported_features" to 2)))
        assertEquals(emptyList<String>(), fallbackStreamTypes(e("camera.still", "idle", "supported_features" to 0)))
    }

    @Test
    fun onlyACrashCountsAgainstWebRtc() {
        assertTrue(exitReasonIsCrash(ApplicationExitInfo.REASON_CRASH_NATIVE))
        assertTrue(exitReasonIsCrash(ApplicationExitInfo.REASON_CRASH))
        assertFalse(exitReasonIsCrash(ApplicationExitInfo.REASON_LOW_MEMORY))
        assertFalse(exitReasonIsCrash(ApplicationExitInfo.REASON_USER_REQUESTED))
        assertFalse(exitReasonIsCrash(ApplicationExitInfo.REASON_OTHER))
    }
}
