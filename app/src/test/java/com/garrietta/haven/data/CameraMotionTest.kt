package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraMotionTest {

    private fun sensor(id: String, state: String, deviceClass: String? = null) =
        HaEntity(
            entityId = id,
            state = state,
            attributes = mapOf("friendly_name" to id) +
                (deviceClass?.let { mapOf("device_class" to it) } ?: emptyMap()),
        )

    @Test
    fun ringSameDeviceMotionFound() {
        val states = mapOf(
            "camera.front_door_live_view" to sensor("camera.front_door_live_view", "streaming"),
            "binary_sensor.front_door_motion" to sensor("binary_sensor.front_door_motion", "on", "motion"),
        )
        val devices = mapOf(
            "camera.front_door_live_view" to "ring_dev",
            "binary_sensor.front_door_motion" to "ring_dev",
        )
        assertEquals(
            "binary_sensor.front_door_motion",
            motionSensorForCamera("camera.front_door_live_view", states, devices)?.entityId,
        )
    }

    @Test
    fun tapoDeviceWithNoBinarySensorReturnsNull() {
        // Tapo motion is an enable-switch, not a binary_sensor: the
        // helper must not invent a motion sensor from the switch.
        val states = mapOf(
            "camera.tapo_1_live_view" to sensor("camera.tapo_1_live_view", "streaming"),
            "switch.tapo_1_motion_detection" to sensor("switch.tapo_1_motion_detection", "on"),
        )
        val devices = mapOf(
            "camera.tapo_1_live_view" to "tapo1",
            "switch.tapo_1_motion_detection" to "tapo1",
        )
        assertNull(motionSensorForCamera("camera.tapo_1_live_view", states, devices))
    }

    @Test
    fun cameraWithoutDeviceLinkReturnsNull() {
        val states = mapOf(
            "camera.front_door_live_view" to sensor("camera.front_door_live_view", "streaming"),
            "binary_sensor.front_door_motion" to sensor("binary_sensor.front_door_motion", "on", "motion"),
        )
        // Registry has not linked the camera yet: empty means
        // unknown, so no chip — never a name-based guess.
        assertNull(motionSensorForCamera("camera.front_door_live_view", states, emptyMap()))
    }

    @Test
    fun deviceClassBeatsIdFallback() {
        val states = mapOf(
            "camera.front_door_live_view" to sensor("camera.front_door_live_view", "streaming"),
            "binary_sensor.front_door_motion" to sensor("binary_sensor.front_door_motion", "off", "motion"),
            "binary_sensor.front_door_motion_audio" to sensor("binary_sensor.front_door_motion_audio", "on"),
        )
        val devices = mapOf(
            "camera.front_door_live_view" to "ring_dev",
            "binary_sensor.front_door_motion" to "ring_dev",
            "binary_sensor.front_door_motion_audio" to "ring_dev",
        )
        assertEquals(
            "binary_sensor.front_door_motion",
            motionSensorForCamera("camera.front_door_live_view", states, devices)?.entityId,
        )
    }
}
