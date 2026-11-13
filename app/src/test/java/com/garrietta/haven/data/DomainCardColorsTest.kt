package com.garrietta.haven.data

import com.garrietta.haven.ui.components.contrastRatio
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which color family a tile is drawn in, and that every family's pairs
 * are readable. The colors themselves are tones of the generated palette
 * (ui/theme), so nothing here pins a hex: the tests pin the decisions
 * (this state means that family) and the guarantees (this pair clears
 * that contrast), which is what must not drift.
 */
class DomainCardColorsTest {

    private fun entity(
        id: String,
        state: String,
        attrs: Map<String, Any?> = emptyMap(),
    ): HaEntity = HaEntity(id, state, attrs)

    private val everyTile = listOf(
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
        entity("vacuum.a", "error"),
        entity("weather.a", "sunny"),
        entity("automation.greenhouse_brain", "on"),
        entity("sensor.power", "320", mapOf("device_class" to "power")),
        entity("sensor.temp", "21.5", mapOf("device_class" to "temperature")),
        entity("sensor.humidity", "44", mapOf("device_class" to "humidity")),
        entity("sensor.count", "3"),
        entity("binary_sensor.motion", "on", mapOf("device_class" to "motion")),
        entity("binary_sensor.door", "on", mapOf("device_class" to "door")),
        entity("binary_sensor.door", "off", mapOf("device_class" to "door")),
        entity("light.a", "unavailable"),
        entity("sensor.a", "unknown"),
    )

    @Test
    fun everyTileResolvesAndItsBadgeIconIsReadableInBothThemes() {
        for (e in everyTile) {
            for (isDark in listOf(false, true)) {
                val colors = domainCardColors(e, isDark)
                assertNotNull("${e.entityId} ${e.state} dark=$isDark must resolve", colors)
                val ratio = contrastRatio(colors!!.badgeGlyph, colors.badgeBg)
                assertTrue(
                    "${e.entityId} ${e.state} dark=$isDark icon contrast $ratio below 4.5:1",
                    ratio >= 4.5f,
                )
            }
        }
    }

    @Test
    fun everyFamilyKeepsItsTextReadableInBothThemes() {
        for (family in StateFamily.entries) {
            for (isDark in listOf(false, true)) {
                val c = family.colors(isDark)
                val text = contrastRatio(c.onContainer, c.container)
                val icon = contrastRatio(c.onAccent, c.accent)
                assertTrue("$family dark=$isDark text on container $text below 4.5:1", text >= 4.5f)
                assertTrue("$family dark=$isDark icon on badge $icon below 4.5:1", icon >= 4.5f)
            }
        }
    }

    @Test
    fun aTileIsItsFamilysContainerBadgeAndIcon() {
        for (isDark in listOf(false, true)) {
            val on = StateFamily.On.colors(isDark)
            val plug = domainCardColors(entity("switch.a", "on"), isDark)!!
            assertEquals(on.container, plug.container)
            assertEquals(on.accent, plug.badgeBg)
            assertEquals(on.onAccent, plug.badgeGlyph)
        }
    }

    @Test
    fun byDayTheBadgeIconIsTheCardsOwnColor() {
        // The icon is cut from the tile: on a light theme it is exactly
        // the container color, so badge and card read as one piece.
        for (e in everyTile) {
            // A lit lamp is the exception: its badge glows (see the next test).
            if (domainFamily(e) == StateFamily.Light) continue
            val colors = domainCardColors(e, isDark = false)!!
            assertEquals("${e.entityId} ${e.state}", colors.container, colors.badgeGlyph)
        }
    }

    @Test
    fun aLitLampIsYellowAndItsBadgeIsTheBrightToneByDay() {
        val lamp = StateFamily.Light.colors(isDark = false)
        val tile = domainCardColors(entity("light.a", "on"), isDark = false)!!
        assertEquals(lamp.accent, tile.badgeBg)
        // Bright, not deep: lighter than the card it sits on is dark.
        assertTrue(contrastRatio(tile.badgeBg, tile.container) < 2f)
        assertTrue(contrastRatio(tile.badgeGlyph, tile.badgeBg) >= 7f)
        // And it is not the green of a switch that is merely on.
        assertTrue(tile.container != StateFamily.On.colors(isDark = false).container)
    }

    @Test
    fun statesMapToFamilies() {
        fun family(id: String, state: String, attrs: Map<String, Any?> = emptyMap()) =
            domainFamily(entity(id, state, attrs))

        assertEquals(StateFamily.Light, family("light.a", "on"))
        assertEquals(StateFamily.Quiet, family("light.a", "off"))
        assertEquals(StateFamily.On, family("switch.a", "on"))
        assertEquals(StateFamily.Quiet, family("fan.a", "off"))

        assertEquals(StateFamily.Heat, family("climate.a", "heat", mapOf("hvac_action" to "heating")))
        assertEquals(StateFamily.Cool, family("climate.a", "cool", mapOf("hvac_action" to "cooling")))
        // On but idle stays warm: a grey thermostat reads as broken.
        assertEquals(StateFamily.Heat, family("climate.a", "heat", mapOf("hvac_action" to "idle")))
        assertEquals(StateFamily.Heat, family("climate.a", "heat_cool"))

        assertEquals(StateFamily.Secure, family("lock.a", "locked"))
        assertEquals(StateFamily.Secure, family("lock.a", "locking"))
        assertEquals(StateFamily.Alert, family("lock.a", "unlocked"))
        assertEquals(StateFamily.Alert, family("lock.a", "jammed"))
        assertEquals(StateFamily.Alert, family("cover.a", "open"))
        assertEquals(StateFamily.Alert, family("cover.a", "opening"))
        assertEquals(StateFamily.Secure, family("cover.a", "closed"))

        assertEquals(StateFamily.Secure, family("alarm_control_panel.a", "disarmed"))
        assertEquals(StateFamily.Heat, family("alarm_control_panel.a", "armed_home"))
        assertEquals(StateFamily.Heat, family("alarm_control_panel.a", "triggered"))

        // Paused media keeps its color: a favourite that is not playing is not dead.
        assertEquals(StateFamily.Media, family("media_player.a", "playing"))
        assertEquals(StateFamily.Media, family("media_player.a", "paused"))

        assertEquals(StateFamily.On, family("vacuum.a", "cleaning"))
        assertEquals(StateFamily.Quiet, family("vacuum.a", "docked"))
        assertEquals(StateFamily.Alert, family("vacuum.a", "error"))
        assertEquals(StateFamily.Cool, family("weather.a", "rainy"))
    }

    @Test
    fun aPlugWithAPowerReadingIsPowerAndAPlainSwitchIsOn() {
        assertEquals(StateFamily.Power, domainFamily(entity("switch.plug", "on", mapOf("current_power_w" to 320.0))))
        assertEquals(StateFamily.On, domainFamily(entity("switch.plain", "on")))
        // Off is off, whatever it could measure.
        assertEquals(StateFamily.Quiet, domainFamily(entity("switch.plug", "off", mapOf("current_power_w" to 0.0))))
    }

    @Test
    fun sensorsTakeTheFamilyOfWhatTheyMeasure() {
        fun family(id: String, deviceClass: String?) =
            domainFamily(entity(id, "1", deviceClass?.let { mapOf("device_class" to it) } ?: emptyMap()))

        assertEquals(StateFamily.Heat, family("sensor.a", "temperature"))
        assertEquals(StateFamily.Water, family("sensor.a", "humidity"))
        assertEquals(StateFamily.Power, family("sensor.a", "power"))
        assertEquals(StateFamily.On, family("sensor.a", "battery"))
        // A reading with no meaning of its own stays neutral.
        assertEquals(StateFamily.Quiet, family("sensor.people_count", null))
    }

    @Test
    fun binarySensorsSpeakOnlyWhenTriggered() {
        fun family(state: String, deviceClass: String) =
            domainFamily(entity("binary_sensor.a", state, mapOf("device_class" to deviceClass)))

        assertEquals(StateFamily.Caution, family("on", "motion"))
        assertEquals(StateFamily.Alert, family("on", "door"))
        assertEquals(StateFamily.Alert, family("on", "smoke"))
        assertEquals(StateFamily.Quiet, family("off", "motion"))
        assertEquals(StateFamily.Quiet, family("off", "door"))
    }

    @Test
    fun greenhouseAndIrrigationReadAsOnExceptAThermostat() {
        assertEquals(StateFamily.On, domainFamily(entity("automation.greenhouse_brain", "on")))
        assertEquals(StateFamily.On, domainFamily(entity("switch.irrigation_zone_1", "off")))
        assertEquals(StateFamily.Heat, domainFamily(entity("climate.greenhouse", "heat")))
    }

    @Test
    fun unavailableAndUnknownAreQuietInBothThemes() {
        for (isDark in listOf(false, true)) {
            val quiet = inactiveCardColors(isDark)
            assertEquals(quiet, domainCardColors(entity("light.a", "unavailable"), isDark))
            assertEquals(quiet, domainCardColors(entity("lock.a", "unknown"), isDark))
            assertEquals(quiet, domainCardColors(entity("media_player.a", ""), isDark))
        }
    }

    @Test
    fun domainsWithNoTileMeaningResolveToNothing() {
        assertNull(domainCardColors(entity("scene.movie_night", "2026-01-01T00:00:00"), false))
        assertNull(domainCardColors(entity("person.alex", "home"), false))
    }

    @Test
    fun switchColorsFollowTheTileFamilies() {
        for (isDark in listOf(false, true)) {
            val on = StateFamily.On.colors(isDark)
            val plain = domainSwitchColors(entity("switch.a", "on"), isDark)!!
            assertEquals(on.accent, plain.trackChecked)
            assertEquals(on.container, plain.thumbChecked)
            assertEquals(restingSwitchTrack(isDark), plain.trackUnchecked)

            // A lamp's track is its yellow, with the dark ink as the thumb.
            val lamp = StateFamily.Light.colors(isDark)
            val light = domainSwitchColors(entity("light.a", "on"), isDark)!!
            assertEquals(lamp.accent, light.trackChecked)
            assertEquals(lamp.onAccent, light.thumbChecked)

            val plug = domainSwitchColors(entity("switch.plug", "on", mapOf("current_power_w" to 5.0)), isDark)!!
            assertEquals(StateFamily.Power.colors(isDark).accent, plug.trackChecked)

            // A lock or a cover has no "off": its other position is its other state.
            val lock = domainSwitchColors(entity("lock.a", "locked"), isDark)!!
            assertEquals(StateFamily.Secure.colors(isDark).accent, lock.trackChecked)
            assertEquals(StateFamily.Alert.colors(isDark).accent, lock.trackUnchecked)
            val cover = domainSwitchColors(entity("cover.a", "open"), isDark)!!
            assertEquals(StateFamily.Alert.colors(isDark).accent, cover.trackChecked)
            assertEquals(StateFamily.Secure.colors(isDark).accent, cover.trackUnchecked)
        }
        assertNull(domainSwitchColors(entity("sensor.a", "1"), false))
    }

    @Test
    fun aSwitchThumbIsVisibleOnItsTrackEitherWay() {
        for (isDark in listOf(false, true)) {
            for (e in listOf(entity("light.a", "on"), entity("lock.a", "locked"), entity("cover.a", "open"))) {
                val s = domainSwitchColors(e, isDark)!!
                assertTrue(contrastRatio(s.thumbChecked, s.trackChecked) >= 3f)
                assertTrue(contrastRatio(s.thumbUnchecked, s.trackUnchecked) >= 3f)
            }
        }
    }
}
