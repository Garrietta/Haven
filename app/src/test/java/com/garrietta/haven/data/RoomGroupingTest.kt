package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGroupingTest {

    private fun entity(id: String) = HaEntity(id, "on", mapOf("friendly_name" to id))

    @Test
    fun splitsAssignedAndUnassigned() {
        val entities = listOf(
            entity("light.lamp"),
            entity("light.standing_lamp"),
            entity("switch.fan"),
        )
        val areas = mapOf(
            "light.lamp" to "living_room",
            "light.standing_lamp" to "living_room",
        )
        val grouped = groupEntitiesByRoom(entities, areas)
        assertEquals(
            listOf("light.lamp", "light.standing_lamp"),
            grouped["living_room"]!!.map { it.entityId }.sorted(),
        )
        assertEquals(listOf("switch.fan"), grouped[null]!!.map { it.entityId })
    }

    @Test
    fun entitiesMissingFromMapLandInNoRoom() {
        val entities = listOf(entity("light.lamp"))
        val grouped = groupEntitiesByRoom(entities, emptyMap())
        assertTrue(grouped[null]!!.map { it.entityId }.contains("light.lamp"))
    }
}
