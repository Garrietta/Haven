package com.garrietta.haven.data

import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.garrietta.haven.ui.components.contrastRatio

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
        assertEquals(StateFamily.Media.colors(false).container, tilePresetColor(preset, false))
        assertEquals(StateFamily.Media.colors(true).container, tilePresetColor(preset, true))
        assertNull(tilePresetFor(mapOf("light.lamp" to "nope"), "light.lamp"))
        assertNull(tilePresetColor(null, false))
    }

    // A domain default covers every entity of that domain (a
    // socket-named lamp is still a light); a per-entity preset wins
    // over it; an unknown domain or key reads as default.
    @Test
    fun tilePresetDomainDefaultAppliesButPerEntityWins() {
        val domainPresets = mapOf("light" to "amber")
        val lamp = tilePresetFor(emptyMap(), "light.lamp_socket_1", domainPresets, "light")
        assertEquals("amber", lamp?.key)
        assertEquals(StateFamily.Power.colors(false).container, tilePresetColor(lamp, false))
        val overridden = tilePresetFor(
            mapOf("light.lamp_socket_1" to "media"), "light.lamp_socket_1", domainPresets, "light",
        )
        assertEquals("media", overridden?.key)
        assertNull(tilePresetFor(emptyMap(), "switch.x", domainPresets, "switch"))
        assertNull(tilePresetFor(emptyMap(), "light.lamp", domainPresets, null))
        assertNull(tilePresetFor(emptyMap(), "light.lamp", mapOf("light" to "nope"), "light"))
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

    // A pinned tile's badge is its own family's accent pair: a yellow
    // tile gets a yellow-tinted badge, never the default green. By day
    // the icon is the tile's own pastel; either way it is readable.
    @Test
    fun presetBadgeIsThePresetFamilysOwnAccentPair() {
        for (preset in TILE_PRESETS) {
            for (isDark in listOf(false, true)) {
                val family = preset.family.colors(isDark)
                val (bg, glyph) = presetBadgeColors(preset, isDark)
                assertEquals("badge for ${preset.key} dark=$isDark", family.accent, bg)
                assertEquals("icon for ${preset.key} dark=$isDark", family.onAccent, glyph)
                if (!isDark) assertEquals("icon is the tile's pastel for ${preset.key}", preset.light, glyph)
                assertTrue(
                    "icon contrast for ${preset.key} dark=$isDark",
                    contrastRatio(glyph, bg) >= 4.5,
                )
            }
        }
    }

    // The stored keys are the user's saved choices: each must keep
    // meaning the family it was saved as.
    @Test
    fun presetKeysKeepTheirFamilies() {
        val families = TILE_PRESETS.associate { it.key to it.family }
        assertEquals(
            mapOf(
                "lime" to StateFamily.On, "heating" to StateFamily.Heat, "cooling" to StateFamily.Cool,
                "lock" to StateFamily.Secure, "alert" to StateFamily.Alert, "media" to StateFamily.Media,
                "amber" to StateFamily.Power, "sage" to StateFamily.Quiet,
            ),
            families,
        )
    }

    // Feedback round 4, item 2: detail/sheet/traveler badges use
    // the card's preset pair only while the entity is active;
    // otherwise null (callers keep matrix/default colors, exactly
    // as an inactive card renders inactive gray).
    @Test
    fun detailBadgeOverrideGatesOnPresetAndActiveState() {
        val presets = mapOf("light.lamp" to "amber", "light.off" to "amber")
        val on = HaEntity("light.lamp", "on", emptyMap())
        val off = HaEntity("light.off", "off", emptyMap())
        val unset = HaEntity("light.plain", "on", emptyMap())
        val amber = TILE_PRESETS.first { it.key == "amber" }
        assertEquals(presetBadgeColors(amber, false), detailBadgeOverride(presets, on, false))
        assertEquals(presetBadgeColors(amber, true), detailBadgeOverride(presets, on, true))
        assertNull(detailBadgeOverride(presets, off, false))
        assertNull(detailBadgeOverride(presets, unset, false))
        assertNull(detailBadgeOverride(mapOf("light.lamp" to "nope"), on, false))
        assertNull(detailBadgeOverride(presets, HaEntity("light.lamp", "unavailable", emptyMap()), false))
    }

    // Feedback round 5, item 4: the popup accent was the tile
    // preset's container, deliberately UNGATED — a CLOSED garage
    // door with cooling blue and a DOCKED vacuum with sage
    // neutral still themed their popups. Round 21, item 4
    // REVERSES that rule: the accent is now gated by
    // presetAppliesForState, like the badge override — the
    // panel follows the tile, so a closed garage's panel shows
    // the tile's inactive matrix colors (green family), not the
    // inactive cooling blue. Active entities still get their
    // container in both themes; inactive ones get null (the
    // matrix/scheme fallback). Null only when no preset is set
    // (never invent color where the user chose none).
    @Test
    fun detailPageAccentReturnsContainerGated() {
        val cooling = TILE_PRESETS.first { it.key == "cooling" }
        val sage = TILE_PRESETS.first { it.key == "sage" }
        val presets = mapOf(
            "cover.garage" to "cooling",
            "vacuum.v" to "sage",
            "light.lamp" to "amber",
        )
        // Inactive-state entities get NO preset accent now.
        assertNull(detailPageAccent(presets, HaEntity("cover.garage", "closed", emptyMap()), false))
        assertNull(detailPageAccent(presets, HaEntity("cover.garage", "closed", emptyMap()), true))
        assertNull(detailPageAccent(presets, HaEntity("vacuum.v", "docked", emptyMap()), false))
        assertNull(detailPageAccent(presets, HaEntity("light.lamp", "off", emptyMap()), false))
        // Active entities and both themes, same rule.
        assertEquals(
            tilePresetColor(cooling, true),
            detailPageAccent(presets, HaEntity("cover.garage", "open", emptyMap()), true),
        )
        assertEquals(
            tilePresetColor(cooling, false),
            detailPageAccent(presets, HaEntity("cover.garage", "open", emptyMap()), false),
        )
        assertEquals(
            tilePresetColor(sage, true),
            detailPageAccent(presets, HaEntity("vacuum.v", "cleaning", emptyMap()), true),
        )
        assertEquals(
            tilePresetColor(sage, false),
            detailPageAccent(presets, HaEntity("vacuum.v", "cleaning", emptyMap()), false),
        )
        // Unset / unknown key: null, never an invented color.
        assertNull(detailPageAccent(presets, HaEntity("light.plain", "on", emptyMap()), false))
        assertNull(detailPageAccent(mapOf("light.lamp" to "nope"), HaEntity("light.lamp", "on", emptyMap()), false))
    }

    @Test
    fun amberPresetBadgeIsYellowTintedNotGreen() {
        val amber = TILE_PRESETS.first { it.key == "amber" }
        val lime = TILE_PRESETS.first { it.key == "lime" }
        for (isDark in listOf(false, true)) {
            val (bg, glyph) = presetBadgeColors(amber, isDark)
            // More red than blue in both badge and icon: the yellow family.
            assertTrue("badge is warm, dark=$isDark", bg.red > bg.blue)
            assertTrue("icon is warm, dark=$isDark", glyph.red > glyph.blue)
            assertTrue("not the lime pair", presetBadgeColors(lime, isDark).first != bg)
        }
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

    // A pinned tile's switch: checked is the preset's badge tone with
    // the tile's own color as the thumb; unchecked is the shared
    // resting pair.
    @Test
    fun presetSwitchColorsFollowPresetWhenChecked() {
        val lime = TILE_PRESETS.first { it.key == "lime" }
        for (isDark in listOf(false, true)) {
            val colors = presetSwitchColors(lime, isDark)
            val (badgeBg, _) = presetBadgeColors(lime, isDark)
            assertEquals(badgeBg, colors.trackChecked)
            assertEquals(tilePresetColor(lime, isDark), colors.thumbChecked)
            assertEquals(restingSwitchTrack(isDark), colors.trackUnchecked)
            assertEquals(restingSwitchThumb(isDark), colors.thumbUnchecked)
        }
    }

    // The Home Status panel draws the same HomeRoomCard as Home and
    // reads the same stored presets, so a pinned color cannot differ
    // between the two. This pins that shared resolution for the
    // panel's inputs: container, badge and switch all come from the
    // assigned preset.
    @Test
    fun panelInputsResolveSamePresetColorsAsHome() {
        val presets = mapOf(
            "light.lamp_socket_1" to "lime",
            "light.standing_lamp_socket_1" to "amber",
        )
        for ((entityId, key, family) in listOf(
            Triple("light.lamp_socket_1", "lime", StateFamily.On),
            Triple("light.standing_lamp_socket_1", "amber", StateFamily.Power),
        )) {
            val expected = family.colors(isDark = false)
            val preset = tilePresetFor(presets, entityId)
            assertEquals("preset key for $entityId", key, preset?.key)
            assertTrue("preset applies while on for $entityId", presetAppliesForState("light", "on"))
            assertEquals("container for $entityId", expected.container, tilePresetColor(preset, false))
            val (bg, glyph) = presetBadgeColors(preset!!, false)
            assertEquals("badge for $entityId", expected.accent, bg)
            assertEquals("icon for $entityId", expected.onAccent, glyph)
            val switch = presetSwitchColors(preset, false)
            assertEquals("switch track follows preset badge for $entityId", bg, switch.trackChecked)
            assertEquals("switch thumb follows preset container for $entityId", expected.container, switch.thumbChecked)
        }
        // The off-state gate is the same too: a pinned tile that is off
        // rests like any other on both surfaces.
        assertTrue(!presetAppliesForState("light", "off"))
    }
}
