package com.garrietta.haven.data

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.garrietta.haven.Screen
import com.garrietta.haven.ui.components.CardTapRoute
import com.garrietta.haven.ui.components.NavItem
import com.garrietta.haven.ui.components.badgeColors
import com.garrietta.haven.ui.components.cardTapRoute
import com.garrietta.haven.ui.components.navActiveScreen
import com.garrietta.haven.ui.components.pillBarItems
import com.garrietta.haven.ui.components.railIndicatorTop
import com.garrietta.haven.ui.components.selectedNavIndex
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.screens.garageDoorFillFraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feedback round 17: the round's new pure logic, pinned by tests. */
class Round17LogicTest {

    private fun entity(entityId: String, state: String, attrs: Map<String, Any?> = emptyMap()) =
        HaEntity(entityId, state, mapOf("friendly_name" to entityId) + attrs)

    private val scheme = lightColorScheme()
    private val accent = Color(0xFFFFEB3B)

    // Item 1 — the shared tile badge pair (traveler == tile == header).

    @Test fun tileBadgePair_presetActiveUsesPresetPair() {
        val amber = TILE_PRESETS.first { it.key == "amber" }
        val pair = tileBadgePair(
            mapOf("light.lamp_socket_1" to "amber"), emptyMap(),
            entity("light.lamp_socket_1", "on"), false, scheme, accent,
        )
        assertEquals(presetBadgeColors(amber, false), pair)
    }

    @Test fun tileBadgePair_presetInactiveUsesInactivePair() {
        val pair = tileBadgePair(
            mapOf("light.lamp_socket_1" to "amber"), emptyMap(),
            entity("light.lamp_socket_1", "off"), false, scheme, accent,
        )
        val inactive = inactiveCardColors(false)
        assertEquals(inactive.badgeBg to inactive.badgeGlyph, pair)
    }

    @Test fun tileBadgePair_domainPresetAppliesAndEntityPresetWins() {
        val amber = TILE_PRESETS.first { it.key == "amber" }
        val lime = TILE_PRESETS.first { it.key == "lime" }
        val lamp = entity("light.lamp_socket_1", "on")
        assertEquals(
            presetBadgeColors(amber, false),
            tileBadgePair(emptyMap(), mapOf("light" to "amber"), lamp, false, scheme, accent),
        )
        assertEquals(
            presetBadgeColors(lime, false),
            tileBadgePair(
                mapOf("light.lamp_socket_1" to "lime"), mapOf("light" to "amber"),
                lamp, false, scheme, accent,
            ),
        )
    }

    @Test fun tileBadgePair_noPresetDockedVacuumStaysMutedGreen() {
        // The eufy case: no preset, docked — the tile reads the
        // matrix INACTIVE pair, so the traveler and headers must
        // too (the old hash fallback read bright yellow here).
        val pair = tileBadgePair(
            emptyMap(), emptyMap(),
            entity("vacuum.t2292", "docked"), false, scheme, accent,
        )
        val inactive = inactiveCardColors(false)
        assertEquals(inactive.badgeBg to inactive.badgeGlyph, pair)
        val matrix = domainCardColors(entity("vacuum.t2292", "docked"), false)!!
        assertEquals(matrix.badgeBg to matrix.badgeGlyph, pair)
    }

    @Test fun tileBadgePair_noPresetNoMatrixFallsBackToHashAccent() {
        val script = entity("script.test", "off")
        assertEquals(
            badgeColors(scheme, script, accent),
            tileBadgePair(emptyMap(), emptyMap(), script, false, scheme, accent),
        )
    }

    // Item 2 — tap routing.

    @Test fun cardTapRoute_mapsDomains() {
        assertEquals(CardTapRoute.CLIMATE_POPOUT, cardTapRoute("climate", "climate.thermostat"))
        assertEquals(CardTapRoute.VACUUM_PANEL, cardTapRoute("vacuum", "vacuum.t2292"))
        assertEquals(CardTapRoute.ENTITY_DETAIL, cardTapRoute("vacuum", "vacuum.other"))
        assertEquals(CardTapRoute.ENTITY_DETAIL, cardTapRoute("cover", "cover.garage"))
        assertEquals(CardTapRoute.ENTITY_DETAIL, cardTapRoute("light", "light.lamp_socket_1"))
        assertEquals(CardTapRoute.ENTITY_DETAIL, cardTapRoute("sensor", "sensor.temp"))
        assertEquals(CardTapRoute.ENTITY_DETAIL, cardTapRoute("media_player", "media_player.tv"))
    }

    @Test fun garageDoorFillFraction_fullyOpenIsZero() {
        // The input that previously rendered the empty box: the
        // frame is now unconditional, and the fraction is 0.
        assertEquals(0f, garageDoorFillFraction(100, "open"), 0.001f)
        assertEquals(1f, garageDoorFillFraction(0, "closed"), 0.001f)
        assertEquals(0f, garageDoorFillFraction(null, "open"), 0.001f)
        assertEquals(1f, garageDoorFillFraction(null, "closed"), 0.001f)
    }

    // Item 3 — rail index mapping.

    @Test fun railIndicatorTop_isColumnPaddingPlusCenteringPlusIndexTimesRow() {
        // 12dp column padding + 8dp (48-32)/2 icon centering, rows 48dp.
        assertEquals(20, railIndicatorTop(0).value.toInt())
        assertEquals(68, railIndicatorTop(1).value.toInt())
        assertEquals(116, railIndicatorTop(2).value.toInt())
    }

    @Test fun navActiveScreen_subScreensHighlightSettings() {
        assertEquals(Screen.Settings, navActiveScreen(Screen.HomeEditor))
        assertEquals(Screen.Settings, navActiveScreen(Screen.SettingsPage("areas/areas")))
        assertEquals(Screen.Settings, navActiveScreen(Screen.Dashboards))
        assertEquals(Screen.Settings, navActiveScreen(Screen.Areas))
        assertEquals(Screen.Settings, navActiveScreen(Screen.Search))
        assertEquals(Screen.Settings, navActiveScreen(Screen.UserTabs))
        assertEquals(Screen.Settings, navActiveScreen(Screen.AutomationBuilder))
        assertEquals(Screen.Settings, navActiveScreen(Screen.SceneBuilder))
        assertEquals(Screen.Settings, navActiveScreen(Screen.ScriptBuilder))
        assertEquals(Screen.Settings, navActiveScreen(Screen.HelperBuilder))
        assertEquals(Screen.Settings, navActiveScreen(Screen.UpdatesPanel))
    }

    @Test fun navActiveScreen_browseDestinationsKeepNoSelection() {
        assertEquals(Screen.DashboardEdit, navActiveScreen(Screen.DashboardEdit))
        assertEquals(Screen.AreaDetail("area_1"), navActiveScreen(Screen.AreaDetail("area_1")))
        assertEquals(
            Screen.CameraClips("camera.front_door_live_view"),
            navActiveScreen(Screen.CameraClips("camera.front_door_live_view")),
        )
    }

    @Test fun selectedNavIndex_todayIsAlwaysZero() {
        val items: List<NavItem> = pillBarItems(true, true, emptyList())
        assertEquals(0, selectedNavIndex(Screen.Today, items))
        assertEquals(0, selectedNavIndex(navActiveScreen(Screen.Today), items))
    }

    // Item 4 — null subtitles and grammar.

    @Test fun cleanDeviceName_collapsesNullBlankAndLiteralNull() {
        assertNull(cleanDeviceName(null))
        assertNull(cleanDeviceName(""))
        assertNull(cleanDeviceName("   "))
        assertNull(cleanDeviceName("null"))
        assertNull(cleanDeviceName("NULL"))
        assertNull(cleanDeviceName("Null"))
        assertEquals("Living Room Hub", cleanDeviceName("Living Room Hub"))
    }

    @Test fun deviceCountLabel_singularAtOne() {
        assertEquals("0 devices", deviceCountLabel(0))
        assertEquals("1 device", deviceCountLabel(1))
        assertEquals("2 devices", deviceCountLabel(2))
        assertEquals("25 devices", deviceCountLabel(25))
    }

    // Item 5 — No-room filter.

    @Test fun isHomeRoomEntity_excludesHomeAssistantSystemEntities() {
        assertFalse(isHomeRoomEntity(entity("conversation.home_assistant", "unknown")))
        assertFalse(isHomeRoomEntity(entity("sensor.home_assistant", "unknown")))
        assertFalse(isHomeRoomEntity(entity("notify.mobile_app", "unknown")))
        assertFalse(isHomeRoomEntity(entity("stt.home_assistant", "unavailable")))
        assertFalse(isHomeRoomEntity(entity("tts.home_assistant", "unknown")))
        assertFalse(isHomeRoomEntity(entity("conversation.other", "unavailable")))
    }

    @Test fun isHomeRoomEntity_keepsUnavailableRealDevices() {
        // The state rule is narrow: only system-service domains
        // are filtered by state, so an offline real device still
        // shows under No room and can be assigned.
        assertTrue(isHomeRoomEntity(entity("switch.dead_plug", "unavailable")))
        assertTrue(isHomeRoomEntity(entity("media_player.tv", "unavailable")))
        assertTrue(isHomeRoomEntity(entity("light.lamp_socket_1", "unknown")))
        assertTrue(isHomeRoomEntity(entity("light.lamp_socket_1", "on")))
    }
}
