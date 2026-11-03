package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardCustomizationTest {

    @Test
    fun customTitleWinsElseFriendlyName() {
        assertEquals("Reading Lamp", customTitleFor(mapOf("light.lamp" to "Reading Lamp"), "light.lamp", "Lamp"))
        assertEquals("Lamp", customTitleFor(emptyMap(), "light.lamp", "Lamp"))
        assertEquals("Lamp", customTitleFor(mapOf("light.lamp" to "   "), "light.lamp", "Lamp"))
    }

    @Test
    fun unknownIconKeyReadsAsNoOverride() {
        val known = setOf("lightbulb", "power")
        assertEquals("power", customIconKeyFor(mapOf("switch.x" to "power"), "switch.x", known))
        assertNull(customIconKeyFor(mapOf("switch.x" to "nope"), "switch.x", known))
        assertNull(customIconKeyFor(emptyMap(), "switch.x", known))
    }

    @Test
    fun tilePresetResolvesBothThemesAndUnknownReadsAsDefault() {
        val preset = tilePresetFor(mapOf("light.lamp" to "media"), "light.lamp")
        assertEquals(Color(0xFFEFE0FF), tilePresetColor(preset, false))
        assertEquals(Color(0xFF3B2554), tilePresetColor(preset, true))
        assertNull(tilePresetFor(mapOf("light.lamp" to "nope"), "light.lamp"))
        assertNull(tilePresetColor(null, false))
    }

    @Test
    fun tilePresetsAndCornerOptionsAreUniqueAndComplete() {
        assertEquals(8, TILE_PRESETS.size)
        assertEquals(8, TILE_PRESETS.map { it.key }.toSet().size)
        assertEquals(8, CARD_CORNER_OPTIONS.size)
        assertEquals(
            listOf("corners_0", "corners_1", "corners_2", "corners_3", "corners_4", "corners_5", "corners_6", "corners_7"),
            CARD_CORNER_OPTIONS.map { it.key },
        )
        assertTrue(TILE_PRESETS.all { it.light != it.dark })
    }
}
