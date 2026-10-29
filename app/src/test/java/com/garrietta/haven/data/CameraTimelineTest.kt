package com.garrietta.haven.data

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraTimelineTest {

    private fun entity(id: String) = HaEntity(id, "idle", mapOf("friendly_name" to id))

    @Test
    fun siblingMatchesSamePrefixRecording() {
        val entities = listOf(
            entity("camera.front_door_live_view"),
            entity("camera.front_door_last_recording"),
            entity("camera.tapo_1_live_view"),
        )
        assertEquals(
            "camera.front_door_last_recording",
            findRecordingSibling("camera.front_door_live_view", entities)?.entityId,
        )
    }

    @Test
    fun siblingNeverSelfAndNeverOtherDevice() {
        val entities = listOf(
            entity("camera.front_door_live_view"),
            // Another device's recording must not stand in.
            entity("camera.backyard_last_recording"),
        )
        assertNull(findRecordingSibling("camera.front_door_live_view", entities))
    }

    @Test
    fun siblingRequiresRecordingInId() {
        val entities = listOf(
            entity("camera.front_door_live_view"),
            entity("camera.front_door_snapshot"),
        )
        assertNull(findRecordingSibling("camera.front_door_live_view", entities))
    }

    @Test
    fun siblingFallsBackToLastSegmentPrefix() {
        val entities = listOf(
            entity("camera.outdoor_hd"),
            entity("camera.outdoor_recording"),
        )
        assertEquals(
            "camera.outdoor_recording",
            findRecordingSibling("camera.outdoor_hd", entities)?.entityId,
        )
    }

    @Test
    fun siblingDeterministicLowestId() {
        val entities = listOf(
            entity("camera.front_door_live_view"),
            entity("camera.front_door_recording_b"),
            entity("camera.front_door_recording_a"),
        )
        assertEquals(
            "camera.front_door_recording_a",
            findRecordingSibling("camera.front_door_live_view", entities)?.entityId,
        )
    }

    @Test
    fun timestampFormatsInGivenZone() {
        assertEquals(
            "Oct 29, 7:23 AM",
            formatHaTimestamp("2026-10-29T07:23:00+00:00", ZoneId.of("UTC")),
        )
    }

    @Test
    fun timestampNullOrGarbageYieldsNull() {
        assertNull(formatHaTimestamp(null))
        assertNull(formatHaTimestamp(""))
        assertNull(formatHaTimestamp("not-a-date"))
    }
}
