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

    // Feedback round, item 5: preset-driven badge tints — dark
    // near-black badge + bright tint glyph, hue preserved,
    // contrast held for every preset in both themes.
    @Test
    fun presetBadgeIsDarkTintWithBrightGlyphAndHuePreserved() {
        for (preset in TILE_PRESETS) {
            for (isDark in listOf(false, true)) {
                val base = if (isDark) preset.dark else preset.light
                val (bg, glyph) = presetBadgeColors(preset, isDark)
                val (bh, _, bv) = rgbToHsv(bg.red, bg.green, bg.blue)
                val (gh, gs, gl) = rgbToHsl(glyph.red, glyph.green, glyph.blue)
                val (ph, _, _) = rgbToHsv(base.red, base.green, base.blue)
                assertTrue("badge dark for ${preset.key} dark=$isDark, v=$bv", bv <= 0.2f)
                assertTrue("glyph bright tint for ${preset.key} dark=$isDark, s=$gs l=$gl", gs >= 0.9f && gl in 0.65f..0.8f)
                assertTrue(
                    "glyph contrast for ${preset.key} dark=$isDark",
                    contrastRatio(glyph, bg) >= 4.5,
                )
                assertTrue("badge hue preserved for ${preset.key}", hueDistance(bh, ph) <= 12f)
                assertTrue("glyph hue preserved for ${preset.key}", hueDistance(gh, ph) <= 12f)
            }
        }
    }

    @Test
    fun amberPresetReadsAmberOnYellowTintedNearBlack() {
        val amber = TILE_PRESETS.first { it.key == "amber" }
        val (bg, glyph) = presetBadgeColors(amber, false)
        val (bh, _, _) = rgbToHsv(bg.red, bg.green, bg.blue)
        val (gh, _, _) = rgbToHsv(glyph.red, glyph.green, glyph.blue)
        // Yellow hue ~45°; both land in the amber/gold band.
        assertTrue("badge hue $bh", bh in 35f..55f)
        assertTrue("glyph hue $gh", gh in 35f..55f)
    }

    private fun luminance(c: Color): Double {
        fun lin(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val (l1, l2) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (l1 + 0.05) / (l2 + 0.05)
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = kotlin.math.abs(a - b) % 360f
        return minOf(d, 360f - d)
    }
}
