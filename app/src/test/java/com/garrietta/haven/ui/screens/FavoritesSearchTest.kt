package com.garrietta.haven.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesSearchTest {

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(favoriteMatchesQuery("", "Lamp", "light.lamp"))
        assertTrue(favoriteMatchesQuery("   ", "Lamp", "light.lamp"))
    }

    @Test
    fun nameSubstringMatchIsCaseInsensitive() {
        assertTrue(favoriteMatchesQuery("lamp", "Lamp", "light.lamp"))
        assertTrue(favoriteMatchesQuery("LA", "Lamp", "light.lamp"))
        assertFalse(favoriteMatchesQuery("tv", "Lamp", "light.lamp"))
    }

    @Test
    fun entityIdIsFallbackMatch() {
        // Finds the entity even if its friendly name differs.
        assertTrue(favoriteMatchesQuery("lamp_socket", "Living Room Socket", "light.lamp_socket"))
        assertFalse(favoriteMatchesQuery("lamp_socket", "Living Room Socket", "light.other"))
    }
}
