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

    // Feedback round 2, item 1: the preset is an ON-state color —
    // it applies only while the entity is active; off/unknown
    // states read inactive for every domain.
    @Test
    fun presetAppliesOnlyWhenActive() {
        // Binary toggleables: "on" only.
        assertTrue(presetAppliesForState("light", "on"))
        assertTrue(!presetAppliesForState("light", "off"))
        assertTrue(presetAppliesForState("switch", "on"))
        assertTrue(!presetAppliesForState("fan", "off"))
        // Vacuum: cleaning/returning only.
        assertTrue(presetAppliesForState("vacuum", "cleaning"))
        assertTrue(presetAppliesForState("vacuum", "returning"))
        assertTrue(!presetAppliesForState("vacuum", "docked"))
        assertTrue(!presetAppliesForState("vacuum", "idle"))
        // Lock: locked only; cover: anything but closed.
        assertTrue(presetAppliesForState("lock", "locked"))
        assertTrue(!presetAppliesForState("lock", "unlocked"))
        assertTrue(presetAppliesForState("cover", "open"))
        assertTrue(presetAppliesForState("cover", "opening"))
        assertTrue(!presetAppliesForState("cover", "closed"))
        // Climate: anything but off.
        assertTrue(presetAppliesForState("climate", "heat_cool"))
        assertTrue(presetAppliesForState("climate", "heat"))
        assertTrue(!presetAppliesForState("climate", "off"))
        // Media idle/paused keeps its row; sensors have no off state.
        assertTrue(presetAppliesForState("media_player", "idle"))
        assertTrue(presetAppliesForState("sensor", "off"))
        // Universal: blank/unavailable/unknown is inactive everywhere.
        assertTrue(!presetAppliesForState("light", "unavailable"))
        assertTrue(!presetAppliesForState("climate", "unknown"))
        assertTrue(!presetAppliesForState("media_player", ""))
    }

    // Feedback round 2, item 2: the switch follows the preset —
    // checked track = preset badge bg, checked thumb = preset
    // container; unchecked = the matrix inactive pair.
    @Test
    fun presetSwitchColorsFollowPresetWhenChecked() {
        val lime = TILE_PRESETS.first { it.key == "lime" }
        val colors = presetSwitchColors(lime, false)
        val (badgeBg, _) = presetBadgeColors(lime, false)
        assertEquals(badgeBg, colors.trackChecked)
        assertEquals(Color(0xFFC8F5A6), colors.thumbChecked)
        assertEquals(Color(0xFF52564E), colors.trackUnchecked)
        assertEquals(Color.White, colors.thumbUnchecked)
        val dark = presetSwitchColors(lime, true)
        assertEquals(Color(0xFF8C9188), dark.trackUnchecked)
        assertEquals(Color(0xFF9E9E9E), dark.thumbUnchecked)
    }

    // Feedback round 3, item 4 (video a3): the Home Status panel
    // MUST match the home page for custom colors. Verified in code
    // (not assumed): HomeStatusPanelScreen renders the SAME
    // HomeRoomCard composable as the home page, reading the SAME
    // SettingsStore maps through the SAME rememberCardCustom
    // snapshot, for the SAME entity IDs — there is no panel-only
    // color path to diverge. This test pins the shared resolution
    // for the panel's exact inputs (both lamps on, Lamp lime,
    // Standing Lamp amber, light theme): container + badge +
    // switch all resolve from the assigned preset on either
    // surface, because both surfaces call these same functions.
    // (A panel that showed matrix lime for the amber lamp while
    // home showed amber cannot come from this resolution — the
    // store read at panel-open time lacked the amber entry, e.g.
    // presets lost on a reinstall; re-assigning once restores
    // every surface, since the store is global by entity ID.)
    @Test
    fun panelInputsResolveSamePresetColorsAsHome() {
        val presets = mapOf(
            "light.lamp_socket_1" to "lime",
            "light.standing_lamp_socket_1" to "amber",
        )
        for ((entityId, key, container) in listOf(
            Triple("light.lamp_socket_1", "lime", Color(0xFFC8F5A6)),
            Triple("light.standing_lamp_socket_1", "amber", Color(0xFFFFE082)),
        )) {
            val preset = tilePresetFor(presets, entityId)
            assertEquals("preset key for $entityId", key, preset?.key)
            assertTrue("preset applies while on for $entityId", presetAppliesForState("light", "on"))
            assertEquals("container for $entityId", container, tilePresetColor(preset, false))
            val (bg, glyph) = presetBadgeColors(preset!!, false)
            val (bh, _, bv) = rgbToHsv(bg.red, bg.green, bg.blue)
            val (gh, _, _) = rgbToHsv(glyph.red, glyph.green, glyph.blue)
            val (ph, _, _) = rgbToHsv(container.red, container.green, container.blue)
            assertTrue("badge dark for $entityId", bv <= 0.2f)
            assertTrue("badge hue matches preset for $entityId", hueDistance(bh, ph) <= 12f)
            assertTrue("glyph hue matches preset for $entityId", hueDistance(gh, ph) <= 12f)
            val switch = presetSwitchColors(preset, false)
            assertEquals("switch track follows preset badge for $entityId", bg, switch.trackChecked)
            assertEquals("switch thumb follows preset container for $entityId", container, switch.thumbChecked)
        }
        // And the off-state gate is identical too: a preset card
        // that is off renders inactive gray on both surfaces.
        assertTrue(!presetAppliesForState("light", "off"))
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
