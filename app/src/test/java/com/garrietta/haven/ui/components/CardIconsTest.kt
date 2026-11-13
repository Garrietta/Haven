package com.garrietta.haven.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardIconsTest {

    /** Keys people already have stored; renaming one would silently lose their choice. */
    private val storedKeys = listOf(
        "lightbulb", "power", "home", "thermostat", "autorenew", "cleaning", "tv", "speaker", "music",
        "volume", "refresh", "lock", "lock_open", "garage", "videocam", "camera", "security", "visibility",
        "warning", "water", "sunny", "cloudy", "cloud", "umbrella", "battery", "bolt", "wifi", "spa",
        "nightlight", "person", "schedule", "list", "play", "build", "info",
    )

    @Test
    fun everyStoredKeyIsStillOffered() {
        for (key in storedKeys) assertTrue("$key is gone", key in CARD_ICON_KEYS)
    }

    @Test
    fun keysAreUniqueAndEveryIconHasAGroup() {
        assertEquals(CARD_ICON_OPTIONS.size, CARD_ICON_KEYS.size)
        for (option in CARD_ICON_OPTIONS) assertTrue("${option.key}: ${option.group}", option.group in CARD_ICON_GROUPS)
    }

    @Test
    fun theThingsAskedForAreThere() {
        for (key in listOf("heater", "fan", "garage_door", "sprinkler")) assertTrue(key in CARD_ICON_KEYS)
    }

    @Test
    fun aSwitchIsNamedForWhatItPowers() {
        assertEquals("heater", suggestedIconKey("switch", null, "Greenhouse Heater"))
        assertEquals("heater", suggestedIconKey("switch", "outlet", "Space heater"))
        assertEquals("sprinkler", suggestedIconKey("switch", null, "Sprinklers Zone 1"))
        assertEquals("sprinkler", suggestedIconKey("input_boolean", null, "Front irrigation"))
        assertEquals("water_heater", suggestedIconKey("switch", null, "Water Heater"))
        assertEquals("humidifier", suggestedIconKey("switch", null, "Basement Dehumidifier"))
        assertEquals("fan", suggestedIconKey("switch", null, "Kitchen Fan"))
        assertEquals("coffee", suggestedIconKey("switch", null, "Coffee Maker"))
        assertEquals("pool", suggestedIconKey("switch", null, "Pool Pump"))
        assertEquals("ac", suggestedIconKey("switch", null, "Bedroom AC"))
        assertEquals("tree", suggestedIconKey("input_boolean", null, "Christmas Tree"))
    }

    @Test
    fun aPlainSwitchKeepsItsIconUnlessItIsAnOutlet() {
        assertNull(suggestedIconKey("switch", null, "Do Not Disturb"))
        assertEquals("outlet", suggestedIconKey("switch", "outlet", "Desk"))
    }

    @Test
    fun whatHomeAssistantSaysWinsOverTheName() {
        assertEquals("fan", suggestedIconKey("fan", null, "Ceiling"))
        assertEquals("valve", suggestedIconKey("valve", "water", "Main"))
        assertEquals("garage_door", suggestedIconKey("cover", "garage", "Big Door"))
        assertEquals("blinds", suggestedIconKey("cover", "blind", "Office"))
        assertEquals("curtains", suggestedIconKey("cover", "curtain", "Living Room"))
    }

    @Test
    fun aCoverWithNoClassIsReadByName() {
        assertEquals("garage_door", suggestedIconKey("cover", null, "Garage Door"))
        assertEquals("gate", suggestedIconKey("cover", null, "Driveway Gate"))
        assertNull(suggestedIconKey("cover", null, "Office"))
    }

    @Test
    fun aLightStaysALightUnlessItIsALampOrATree() {
        assertEquals("lamp", suggestedIconKey("light", null, "Floor Lamp"))
        assertEquals("tree", suggestedIconKey("light", null, "Christmas Tree"))
        assertNull(suggestedIconKey("light", null, "Ceiling Fan Light"))
        assertNull(suggestedIconKey("light", null, "Heat Lamp Bulb").takeUnless { it == "lamp" })
    }

    @Test
    fun otherDomainsAreLeftAlone() {
        assertNull(suggestedIconKey("sensor", "temperature", "Greenhouse Heater Temperature"))
        assertNull(suggestedIconKey("binary_sensor", "door", "Garage Door"))
    }
}
