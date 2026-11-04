package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.components.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI rewrite round §2: the user's complete state-based color matrix
 * for Home cards. Every badge glyph must clear 4.5:1 against its
 * badge background in both themes — EXCEPT the inactive dark pair
 * (#8C9188 on #2A2F28, ~4.2:1), whose hexes are user-specified and
 * are kept and reported rather than altered; that pair must still
 * clear the 3:1 icon floor. Temperature/humidity sensors keep their
 * semantic path; binary sensors and generic sensors keep the
 * existing pipeline.
 */
class DomainCardColorsTest {

    private fun entity(
        id: String,
        state: String,
        attrs: Map<String, Any?> = emptyMap(),
    ): HaEntity = HaEntity(id, state, attrs)

    @Test
    fun everyBadgeGlyphClearsFourPointFiveInBothThemes() {
        val cases = listOf(
            entity("light.a", "on"),
            entity("light.a", "off"),
            entity("switch.a", "on"),
            entity("switch.a", "off"),
            entity("switch.plug", "on", mapOf("current_power_w" to 320.0)),
            entity("climate.a", "heat"),
            entity("climate.a", "cool", mapOf("hvac_action" to "cooling")),
            entity("climate.a", "heat_cool", mapOf("hvac_action" to "idle")),
            entity("lock.a", "locked"),
            entity("lock.a", "unlocked"),
            entity("cover.a", "open"),
            entity("cover.a", "closed"),
            entity("alarm_control_panel.a", "disarmed"),
            entity("alarm_control_panel.a", "armed_away"),
            entity("media_player.a", "playing"),
            entity("vacuum.a", "cleaning"),
            entity("vacuum.a", "docked"),
            entity("weather.a", "sunny"),
            entity("automation.greenhouse_brain", "on"),
            entity("sensor.power", "320", mapOf("device_class" to "power")),
            entity("light.a", "unavailable"),
            entity("sensor.a", "unknown"),
        )
        for (e in cases) {
            for (isDark in listOf(false, true)) {
                val colors = domainCardColors(e, isDark)
                assertNotNull("${e.entityId} ${e.state} dark=$isDark must resolve", colors)
                val ratio = contrastRatio(colors!!.badgeGlyph, colors.badgeBg)
                // The inactive dark pair is user-specified (~4.2:1):
                // it must clear the 3:1 icon floor, not 4.5:1.
                val floor = if (isDark && colors.container == Color(0xFF1C201B)) 3.0f else 4.5f
                assertTrue(
                    "${e.entityId} ${e.state} dark=$isDark glyph contrast $ratio below $floor:1",
                    ratio >= floor,
                )
                assertNotNull(colors.container)
            }
        }
    }

    @Test
    fun lightFamilyMatchesDirectiveTable() {
        val on = domainCardColors(entity("light.island", "on"), false)!!
        assertEquals(Color(0xFFC8F5A6), on.container)
        assertEquals(Color(0xFF1B3708), on.badgeBg)
        val onDark = domainCardColors(entity("light.island", "on"), true)!!
        assertEquals(Color(0xFF2B4E14), onDark.container)
        assertEquals(Color(0xFFB8F397), onDark.badgeBg)
        // Off is the inactive row, not the light row.
        val off = domainCardColors(entity("light.living_room", "off"), false)!!
        assertEquals(Color(0xFFE6EBD3), off.container)
        assertEquals(Color(0xFF52564E), off.badgeBg)
        assertEquals(Color.White, off.badgeGlyph)
        val offDark = domainCardColors(entity("light.living_room", "off"), true)!!
        assertEquals(Color(0xFF1C201B), offDark.container)
        // Reconciliation: dark badge polygon #2A2F28 (visible
        // against the #1C201B card) with glyph #8C9188.
        assertEquals(Color(0xFF2A2F28), offDark.badgeBg)
        assertEquals(Color(0xFF8C9188), offDark.badgeGlyph)
    }

    @Test
    fun climateLockMediaPowerFamiliesMatchDirectiveTable() {
        assertEquals(Color(0xFFFFD1B9), domainCardColors(entity("climate.greenhouse", "heat"), false)!!.container)
        assertEquals(Color(0xFF532215), domainCardColors(entity("climate.greenhouse", "heat"), true)!!.container)
        assertEquals(Color(0xFFCCD7FF), domainCardColors(entity("lock.front_door", "locked"), false)!!.container)
        assertEquals(Color(0xFFEFCEFF), domainCardColors(entity("media_player.sonos", "playing"), false)!!.container)
        assertEquals(Color(0xFFFFE082), domainCardColors(entity("switch.workbench", "on", mapOf("current_power_w" to 320.0)), false)!!.container)
    }

    @Test
    fun plugSwitchUsesPowerFamilyButPlainSwitchUsesLightFamily() {
        val plug = domainCardColors(entity("switch.workbench", "on", mapOf("current_power_w" to 320.0)), false)!!
        val plain = domainCardColors(entity("switch.porch", "on"), false)!!
        assertEquals(Color(0xFFFFE082), plug.container)
        assertEquals(Color(0xFFC8F5A6), plain.container)
    }

    @Test
    fun unavailableIsInactiveGreyInBothThemes() {
        val light = domainCardColors(entity("light.a", "unavailable"), false)!!
        assertEquals(Color(0xFFE6EBD3), light.container)
        val dark = domainCardColors(entity("climate.a", "unavailable"), true)!!
        assertEquals(Color(0xFF1C201B), dark.container)
    }

    @Test
    fun temperatureSensorBypassesTableKeepsSemanticPath() {
        // device_class temperature (and humidity) must NOT resolve
        // here: the caller keeps the coral/sky semantic path.
        assertNull(domainCardColors(entity("sensor.temp", "72", mapOf("device_class" to "temperature")), false))
        assertNull(domainCardColors(entity("sensor.hum", "45", mapOf("device_class" to "humidity")), false))
        // A power sensor DOES resolve to the power family.
        assertNotNull(domainCardColors(entity("sensor.watts", "320", mapOf("device_class" to "power")), false))
    }

    @Test
    fun stateRowsFollowTheMatrix() {
        // Cooling and heat_cool differ from heating; unlocked/open
        // differ from locked/closed; armed differs from disarmed.
        assertEquals(Color(0xFFBDDEFF), domainCardColors(entity("climate.a", "cool", mapOf("hvac_action" to "cooling")), false)!!.container)
        assertEquals(Color(0xFF1A304E), domainCardColors(entity("climate.a", "cool", mapOf("hvac_action" to "cooling")), true)!!.container)
        assertEquals(Color(0xFF3E2723), domainCardColors(entity("climate.a", "heat_cool", mapOf("hvac_action" to "idle")), true)!!.container)
        assertEquals(Color(0xFFFFCDC4), domainCardColors(entity("lock.a", "unlocked"), false)!!.container)
        assertEquals(Color(0xFF5C1916), domainCardColors(entity("lock.a", "unlocked"), true)!!.container)
        assertEquals(Color(0xFF222C61), domainCardColors(entity("cover.a", "closed"), true)!!.container)
        assertEquals(Color(0xFF532215), domainCardColors(entity("alarm_control_panel.a", "armed_away"), true)!!.container)
        assertEquals(Color(0xFF2B4E14), domainCardColors(entity("vacuum.a", "cleaning"), true)!!.container)
        assertEquals(Color(0xFF1F3D0C), domainCardColors(entity("automation.greenhouse_brain", "on"), true)!!.container)
        assertEquals(Color(0xFFBDDEFF), domainCardColors(entity("weather.a", "sunny"), false)!!.container)
        // Active dark glyphs are tonal ink, not white.
        assertEquals(Color(0xFF111D52), domainCardColors(entity("lock.a", "unlocked"), true)!!.badgeGlyph)
        assertEquals(Color(0xFF0A2005), domainCardColors(entity("vacuum.a", "cleaning"), true)!!.badgeGlyph)
        // Feedback round 4, item 3 (SUPERSEDES the rewrite's white
        // rule): active light glyphs are the pastel container.
        assertEquals(Color(0xFFFFCDC4), domainCardColors(entity("lock.a", "unlocked"), false)!!.badgeGlyph)
        assertEquals(Color(0xFFC8F5A6), domainCardColors(entity("light.a", "on"), false)!!.badgeGlyph)
        // Climate keeps its climate rows even for greenhouse names.
        assertEquals(Color(0xFFFFD1B9), domainCardColors(entity("climate.greenhouse", "heat"), false)!!.container)
        // Idle climate keeps the Heating row (previous CLIMATE family).
        assertEquals(Color(0xFFFFD1B9), domainCardColors(entity("climate.a", "heat", mapOf("hvac_action" to "idle")), false)!!.container)
    }

    // Feedback round 4, item 3: in the light theme, every ACTIVE
    // row's badge glyph is exactly its pastel container (the
    // switch thumb color), while the badge polygon stays deep
    // tonal ink. Inactive rows keep their white/gray glyphs.
    @Test
    fun lightThemeActiveGlyphEqualsContainer() {
        val active = listOf(
            entity("light.a", "on"),
            entity("switch.a", "on"),
            entity("switch.plug", "on", mapOf("current_power_w" to 320.0)),
            entity("climate.a", "heat"),
            entity("climate.a", "cool", mapOf("hvac_action" to "cooling")),
            entity("climate.a", "heat_cool", mapOf("hvac_action" to "idle")),
            entity("lock.a", "locked"),
            entity("lock.a", "unlocked"),
            entity("cover.a", "open"),
            entity("cover.a", "closed"),
            entity("alarm_control_panel.a", "disarmed"),
            entity("alarm_control_panel.a", "armed_away"),
            entity("media_player.a", "playing"),
            entity("vacuum.a", "cleaning"),
            entity("weather.a", "sunny"),
            entity("automation.greenhouse_brain", "on"),
            entity("sensor.power", "320", mapOf("device_class" to "power")),
        )
        for (e in active) {
            val colors = domainCardColors(e, false)!!
            assertEquals("${e.entityId} ${e.state} light glyph", colors.container, colors.badgeGlyph)
        }
    }

    @Test
    fun switchColorsFollowStateRows() {
        val light = domainSwitchColors(entity("light.a", "on"), false)!!
        assertEquals(Color(0xFF1B3708), light.trackChecked)
        assertEquals(Color(0xFFC8F5A6), light.thumbChecked)
        assertEquals(Color(0xFF52564E), light.trackUnchecked)
        val lightDark = domainSwitchColors(entity("light.a", "on"), true)!!
        assertEquals(Color(0xFFB8F397), lightDark.trackChecked)
        assertEquals(Color(0xFF2B4E14), lightDark.thumbChecked)
        // Dark unchecked track is the matrix switch hex, NOT the badge polygon.
        assertEquals(Color(0xFF8C9188), lightDark.trackUnchecked)
        val lock = domainSwitchColors(entity("lock.a", "locked"), false)!!
        assertEquals(Color(0xFF111D52), lock.trackChecked)
        assertEquals(Color(0xFF410002), lock.trackUnchecked)
        val cover = domainSwitchColors(entity("cover.a", "open"), true)!!
        assertEquals(Color(0xFFFFB4AB), cover.trackChecked)
        assertEquals(Color(0xFFB9C3FF), cover.trackUnchecked)
        assertNull(domainSwitchColors(entity("sensor.a", "72"), false))
        assertNull(domainSwitchColors(entity("media_player.a", "playing"), false))
    }

    @Test
    fun unmappedDomainsFallThroughToExistingPipeline() {
        // Docked vacuum is Inactive (off) per the matrix; binary
        // sensors have no matrix row and keep the old pipeline.
        assertEquals(Color(0xFFE6EBD3), domainCardColors(entity("vacuum.t2292", "docked"), false)!!.container)
        assertNull(domainCardColors(entity("binary_sensor.door", "off", mapOf("device_class" to "door")), false))
    }
}
