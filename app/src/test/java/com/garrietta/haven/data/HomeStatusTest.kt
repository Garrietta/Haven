package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage D (redesign): Home Status derives ONLY from live states.
 * Unknown/unavailable entities are never flagged, and a category
 * appears only when at least one real entity is in the flagged state.
 */
class HomeStatusTest {

    private fun entity(
        id: String,
        state: String,
        deviceClass: String? = null,
        name: String? = null,
    ): HaEntity {
        val attrs = mutableMapOf<String, Any?>()
        if (deviceClass != null) attrs["device_class"] = deviceClass
        if (name != null) attrs["friendly_name"] = name
        return HaEntity(id, state, attrs)
    }

    @Test
    fun emptyWhenNothingFlagged() {
        val entities = listOf(
            entity("lock.front_door", "locked", name = "Front Door"),
            entity("binary_sensor.front_door", "off", "door", "Front Door"),
            entity("light.lamp", "off", name = "Lamp"),
            entity("alarm_control_panel.home", "disarmed", name = "Home"),
        )
        assertTrue(deriveHomeStatus(entities).isEmpty())
    }

    @Test
    fun unknownAndUnavailableNeverFlagged() {
        val entities = listOf(
            entity("lock.front_door", "unavailable", name = "Front Door"),
            entity("lock.back_door", "unknown", name = "Back Door"),
            entity("alarm_control_panel.home", "unavailable", name = "Home"),
            entity("binary_sensor.front_door", "unavailable", "door", "Front Door"),
        )
        assertTrue(deriveHomeStatus(entities).isEmpty())
    }

    @Test
    fun locksDoorsLightsMotionAlarmInBriefOrder() {
        val entities = listOf(
            entity("sensor.power", "100", "power", "Plug Power"),
            entity("alarm_control_panel.home", "armed_away", name = "Home"),
            entity("binary_sensor.motion", "on", "motion", "Hall Motion"),
            entity("light.lamp", "on", name = "Lamp"),
            entity("binary_sensor.front_door", "on", "door", "Front Door"),
            entity("lock.front_door", "unlocked", name = "Front Door"),
        )
        val items = deriveHomeStatus(entities)
        assertEquals(
            listOf(
                HomeStatusCategory.LOCKS,
                HomeStatusCategory.DOORS,
                HomeStatusCategory.LIGHTS,
                HomeStatusCategory.MOTION,
                HomeStatusCategory.ALARM,
                HomeStatusCategory.ENERGY,
            ),
            items.map { it.category },
        )
        assertEquals("Unlocked", items[0].title)
        assertEquals("Front Door", items[0].detail)
        assertEquals("Armed away", items[4].title)
        assertEquals("100 W across 1 sensor", items[5].detail)
    }

    @Test
    fun multipleEntitiesSummarizeAsCounts() {
        val entities = listOf(
            entity("lock.front_door", "unlocked", name = "Front Door"),
            entity("lock.back_door", "unlocked", name = "Back Door"),
            entity("cover.garage", "open", name = "Garage"),
            entity("binary_sensor.window", "on", "window", "Kitchen Window"),
        )
        val items = deriveHomeStatus(entities)
        assertEquals("2 locks unlocked", items[0].detail)
        assertEquals(2, items[0].entities.size)
        assertEquals("2 doors/windows open", items[1].detail)
    }

    @Test
    fun coversCountAsDoorsAndPowerSums() {
        val entities = listOf(
            entity("cover.garage", "opening", name = "Garage"),
            entity("cover.blind", "closed", name = "Blind"),
            entity("sensor.a", "60.5", "power", "A"),
            entity("sensor.b", "40", "power", "B"),
            entity("sensor.c", "not_a_number", "power", "C"),
            entity("sensor.d", "1234", "energy", "D"),
        )
        val items = deriveHomeStatus(entities)
        assertEquals(2, items.size)
        assertEquals(HomeStatusCategory.DOORS, items[0].category)
        assertEquals("100.5 W across 2 sensors", items[1].detail)
    }

    @Test
    fun indicatorLightsExcludedUnlessExplicitlyReAdded() {
        val dnd = entity("light.tv_dnd", "on", name = "TV Dnd")
        assertTrue(deriveHomeStatus(listOf(dnd)).isEmpty())
        val shown = deriveHomeStatus(listOf(dnd), mapOf("light.tv_dnd" to true))
        assertEquals(1, shown.size)
        assertEquals(HomeStatusCategory.LIGHTS, shown[0].category)
    }
}
