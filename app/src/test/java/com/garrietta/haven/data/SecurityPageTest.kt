package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPageTest {

    private fun entity(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
        HaEntity(id, state, mapOf("friendly_name" to name) + attrs.toMap())

    private val frontDoor = entity("camera.front_door", "idle", "Front Door")
    private val garageCam = entity("camera.garage", "idle", "Garage")
    private val frontMotionSwitch = entity("switch.front_door_motion_detection", "on", "Front Door Motion detection")
    private val frontPerson = entity("switch.front_door_person_detection", "off", "Front Door Person detection")
    private val frontMotion = entity("binary_sensor.front_door_motion", "off", "Front Door Motion", "device_class" to "motion")
    private val porchLight = entity("switch.porch_light", "on", "Porch Light")
    private val looseSwitch = entity("switch.hallway_motion_lights", "on", "Hallway motion lights")

    private val states = listOf(frontDoor, garageCam, frontMotionSwitch, frontPerson, frontMotion, porchLight, looseSwitch)
        .associateBy { it.entityId }
    private val devices = mapOf(
        "camera.front_door" to "dev-ring",
        "switch.front_door_motion_detection" to "dev-ring",
        "switch.front_door_person_detection" to "dev-ring",
        "binary_sensor.front_door_motion" to "dev-ring",
        "camera.garage" to "dev-tapo",
        "switch.porch_light" to "dev-ring",
    )

    @Test
    fun detectionSwitchesAreTheOnesNamedForMotionOrDetection() {
        assertTrue(isDetectionSwitch(frontMotionSwitch))
        assertTrue(isDetectionSwitch(frontPerson))
        assertFalse(isDetectionSwitch(porchLight))
        // A motion sensor reports motion; it does not switch detection.
        assertFalse(isDetectionSwitch(frontMotion))
    }

    @Test
    fun aCameraGetsTheSwitchesAndSensorOnItsOwnDevice() {
        val cameras = securityCameras(listOf("camera.front_door", "camera.garage"), states, devices)
        assertEquals(listOf("Front Door", "Garage"), cameras.map { it.name })
        assertEquals(
            listOf("switch.front_door_motion_detection", "switch.front_door_person_detection"),
            cameras[0].switches.map { it.entityId },
        )
        assertEquals("binary_sensor.front_door_motion", cameras[0].motionSensor?.entityId)
        // Nothing on the Tapo's device: no switches, no sensor, never a guess by name.
        assertTrue(cameras[1].switches.isEmpty())
        assertNull(cameras[1].motionSensor)
    }

    @Test
    fun switchesOnNoCamerasDeviceAreListedOnTheirOwn() {
        val loose = looseDetectionSwitches(listOf("camera.front_door", "camera.garage"), states, devices)
        assertEquals(listOf("switch.hallway_motion_lights"), loose.map { it.entityId })
    }

    @Test
    fun aSwitchUnderItsCameraDropsTheCamerasName() {
        assertEquals("Motion detection", detectionLabel(frontMotionSwitch, "Front Door"))
        assertEquals("Person detection", detectionLabel(frontPerson, "Front Door"))
        assertEquals("Hallway motion lights", detectionLabel(looseSwitch, "Front Door"))
    }

    @Test
    fun theOpeningLineNamesWhatIsNotShut() {
        val calm = securitySummary(
            listOf(
                entity("lock.side_door", "locked", "Side Door"),
                entity("binary_sensor.back_door", "off", "Back Door", "device_class" to "door"),
            ).associateBy { it.entityId },
        )
        assertEquals(SecuritySummary("Everything is shut and locked.", calm = true), calm)

        val busy = securitySummary(
            listOf(
                entity("lock.side_door", "unlocked", "Side Door"),
                entity("binary_sensor.back_door", "on", "Back Door", "device_class" to "door"),
                entity("cover.garage_door", "open", "Garage Door", "device_class" to "garage"),
                // Unavailable is neither shut nor open.
                entity("lock.shed", "unavailable", "Shed"),
            ).associateBy { it.entityId },
        )
        assertEquals("Back Door and Garage Door are open, and Side Door is unlocked.", busy.line)
        assertFalse(busy.calm)
    }

    @Test
    fun longListsAreShortened() {
        assertEquals("A, B and 2 more", namesList(listOf("A", "B", "C", "D")))
        assertEquals("A", namesList(listOf("A")))
    }

    @Test
    fun alarmStatesMapToTheirChoice() {
        assertEquals("home", alarmChoice("armed_home"))
        assertEquals("disarmed", alarmChoice("disarmed"))
        assertNull(alarmChoice("arming"))
    }
}
