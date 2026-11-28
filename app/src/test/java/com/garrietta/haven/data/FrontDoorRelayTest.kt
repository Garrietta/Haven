package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** The camera that relays ring-mqtt's live stream through Home Assistant, for the front door away from home. */
class FrontDoorRelayTest {
    private fun e(id: String, state: String, name: String) = HaEntity(id, state, mapOf("friendly_name" to name))

    @Test
    fun theKnownRelayCameraIsFound() {
        val states = listOf(e(FRONT_DOOR_RELAY_CAMERA_ID, "idle", "127_0_0_1")).associateBy { it.entityId }
        assertEquals(FRONT_DOOR_RELAY_CAMERA_ID, frontDoorRelayCamera(states))
    }

    @Test
    fun aRenamedRelayIsFoundByItsName() {
        val states = listOf(e("camera.front_door_relay", "idle", "Front Door (ring-mqtt)")).associateBy { it.entityId }
        assertEquals("camera.front_door_relay", frontDoorRelayCamera(states))
    }

    @Test
    fun aRelayThatIsDownIsNone() {
        val states = listOf(e(FRONT_DOOR_RELAY_CAMERA_ID, "unavailable", "127_0_0_1")).associateBy { it.entityId }
        assertEquals(null, frontDoorRelayCamera(states))
        assertEquals(null, frontDoorRelayCamera(emptyMap()))
    }
}
