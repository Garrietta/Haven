package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PageCurationTest {

    @Test
    fun untouchedSectionsShowTheirDefaults() {
        assertEquals(listOf("a", "b"), curated(listOf("a", "b"), PageCuration()))
    }

    @Test
    fun addedItemsJoinAndHiddenOnesLeave() {
        val c = PageCuration().add("x").remove("a")
        assertEquals(listOf("b", "x"), curated(listOf("a", "b"), c))
        assertEquals(listOf("a"), takenOff(listOf("a", "b"), c))
        assertEquals(listOf("a", "b", "x"), curated(listOf("a", "b"), c.show("a")))
    }

    @Test
    fun removingSomethingAddedForgetsIt() {
        val c = PageCuration().add("x").remove("x")
        assertEquals(emptyList<String>(), c.added)
        assertEquals(emptySet<String>(), c.hidden)
    }

    @Test
    fun theArrangedOrderLeadsAndNewDefaultsFollow() {
        val shown = curated(listOf("a", "b", "c"), PageCuration())
        val c = PageCuration().moved(shown, from = 2, to = 0)
        assertEquals(listOf("c", "a", "b"), curated(listOf("a", "b", "c"), c))
        assertEquals(listOf("c", "a", "b", "d"), curated(listOf("a", "b", "c", "d"), c))
    }

    @Test
    fun addedItemsThatNoLongerExistAreNotShown() {
        val c = PageCuration().add("gone")
        assertEquals(listOf("a"), curated(listOf("a"), c) { it != "gone" })
    }

    @Test
    fun itSurvivesStorage() {
        val c = PageCuration(setOf("a"), listOf("x"), listOf("x", "b"))
        assertEquals(c, PageCuration.fromJson(c.toJson()))
        assertEquals(PageCuration(), PageCuration.fromJson("not json"))
    }

    @Test
    fun addedEntitiesFindTheirSection() {
        assertEquals(SECURITY_CAMERAS, securitySectionFor("camera.porch"))
        assertEquals(SECURITY_DETECTION, securitySectionFor("switch.porch_motion"))
        assertEquals(SECURITY_DOORS, securitySectionFor("lock.front"))
        assertEquals(CLIMATE_THERMOSTATS, climateSectionFor("climate.main"))
        assertEquals(CLIMATE_READINGS, climateSectionFor("sensor.co2"))
    }
}
