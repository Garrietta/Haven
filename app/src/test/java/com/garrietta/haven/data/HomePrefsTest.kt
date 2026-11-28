package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePrefsTest {

    private fun light(entityId: String, friendlyName: String, state: String = "on") =
        HaEntity(entityId, state, mapOf("friendly_name" to friendlyName))

    @Test
    fun dndInEntityIdIsIndicator() {
        assertTrue(isIndicatorLight(light("light.smart_garage_door_24090408838301610702c4e7ae0ae280_dnd", "Garage Door DND")))
        assertTrue(isIndicatorLight(light("light.something_DND", "Something")))
    }

    @Test
    fun friendlyNameEndingInDndIsIndicator() {
        assertTrue(isIndicatorLight(light("light.hub_indicator", "Smart Hub Dnd")))
        assertTrue(isIndicatorLight(light("light.hub_indicator", "Smart Hub DND")))
    }

    @Test
    fun ordinaryLightsAreNotIndicators() {
        assertFalse(isIndicatorLight(light("light.lamp_socket_1", "Lamp")))
        assertFalse(isIndicatorLight(light("light.standing_lamp_socket_1", "Standing Lamp")))
        // "dnd" elsewhere in the name is not an ending match, and the id is clean.
        assertFalse(isIndicatorLight(light("light.bedroom", "Dnd Lamp")))
    }

    @Test
    fun defaultRuleHidesIndicatorsAndShowsRoomLights() {
        val overrides = emptyMap<String, Boolean>()
        assertFalse(isLightShownOnHome(light("light.smart_hub_25111733115125620801c4e7ae24d9ea_dnd", "Smart Hub DND"), overrides))
        assertTrue(isLightShownOnHome(light("light.lamp_socket_1", "Lamp"), overrides))
    }

    @Test
    fun explicitOnOverridesDefaultExclusion() {
        val dnd = light("light.smart_garage_door_24090408838301610702c4e7ae0ae280_dnd", "Garage Door DND")
        assertTrue(isLightShownOnHome(dnd, mapOf(dnd.entityId to true)))
    }

    @Test
    fun explicitOffHidesARoomLight() {
        val lamp = light("light.lamp_socket_1", "Lamp")
        assertFalse(isLightShownOnHome(lamp, mapOf(lamp.entityId to false)))
    }

    // ---- Stage G: arrangement resolution ----

    private val canonical = listOf("a", "b", "c", "d")

    @Test
    fun emptyStoredResolvesToCanonical() {
        assertEquals(canonical, resolveOrder(emptyList(), canonical))
    }

    @Test
    fun storedSubsetKeepsOrderAndAppendsMissing() {
        assertEquals(listOf("c", "a", "b", "d"), resolveOrder(listOf("c", "a"), canonical))
    }

    @Test
    fun unknownKeysDropped() {
        assertEquals(listOf("b", "a", "c", "d"), resolveOrder(listOf("b", "removed-section", "a"), canonical))
    }

    @Test
    fun duplicatesKeepFirstOccurrence() {
        assertEquals(listOf("a", "b", "c", "d"), resolveOrder(listOf("a", "b", "a", "c"), canonical))
    }

    @Test
    fun fullReorderRespected() {
        assertEquals(listOf("d", "c", "b", "a"), resolveOrder(listOf("d", "c", "b", "a"), canonical))
    }

    @Test
    fun defaultSectionOrderCoversAllToggleableBlocks() {
        // Every toggleable Home block except the pinned greeting must
        // appear in the canonical order, so arrangement can move it.
        for ((key, _) in HOME_SECTIONS) {
            if (key == "greeting") continue
            assertEquals("missing from default order: $key", true, key in HOME_SECTION_ORDER_DEFAULT)
        }
    }

    @Test
    fun round3OrderMigrationExpandsLegacyKeyInPlace() {
        assertEquals(
            listOf("cameras", "round3-security", "round3-vacuum", "round3-media", "round3-greenhouse", "rooms"),
            migrateLegacySectionOrder(listOf("cameras", "round3", "rooms")),
        )
        // Orders without the legacy key pass through untouched.
        assertEquals(
            listOf("cameras", "rooms"),
            migrateLegacySectionOrder(listOf("cameras", "rooms")),
        )
        // Already-migrated orders are stable (no duplication).
        assertEquals(
            listOf("round3-security", "round3-vacuum"),
            migrateLegacySectionOrder(listOf("round3-security", "round3-vacuum")),
        )
    }

    @Test
    fun round3HiddenMigrationMapsCombinedToAllFour() {
        // Combined-hidden maps to all four hidden.
        assertEquals(
            setOf("round3-security", "round3-vacuum", "round3-media", "round3-greenhouse"),
            migrateLegacyHiddenSections(setOf("round3")),
        )
        // Combined-shown (or absent) maps to all four shown, and the
        // legacy key never survives in the set.
        assertEquals(setOf("favorites"), migrateLegacyHiddenSections(setOf("favorites")))
        assertEquals(setOf("favorites"), migrateLegacyHiddenSections(setOf("favorites", "round3")) - ROUND3_BLOCK_KEYS.toSet())
        assertTrue(migrateLegacyHiddenSections(setOf("favorites", "round3")).containsAll(ROUND3_BLOCK_KEYS))
    }
}
