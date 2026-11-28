package com.garrietta.haven.ui.components

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.garrietta.haven.ui.theme.StateShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.garrietta.haven.ui.theme.Palette

class DesignTokensTest {

    private val scheme = lightColorScheme()

    @Test
    fun navigationChromeIsTheSameInBothThemes() {
        // The bar is the brand in miniature: near-black green with a
        // lime capsule, by day and by night. No theme changes it.
        assertEquals(Palette.Ink, PillBarContainer)
        assertEquals(Palette.Lime, PillBarActiveCapsule)
        val dark = darkColorScheme()
        assertEquals(PillBarActiveCapsule, selectedPillFill(scheme, false))
        assertEquals(PillBarActiveCapsule, selectedPillFill(dark, true))
        assertEquals(PillBarActiveGlyph, selectedPillGlyph(scheme, false))
        assertEquals(PillBarActiveGlyph, selectedPillGlyph(dark, true))
        assertEquals(PillBarUnselectedGlyph, pillUnselectedGlyph(scheme, false))
        assertEquals(PillBarUnselectedGlyph, pillUnselectedGlyph(dark, true))
        // The selected glyph on the capsule and the resting glyphs on
        // the bar both clear 4.5:1.
        assertTrue(contrastRatio(PillBarActiveGlyph, PillBarActiveCapsule) >= 4.5f)
        assertTrue(contrastRatio(PillBarUnselectedGlyph, PillBarContainer) >= 4.5f)
    }

    @Test
    fun lightTempBlendClampsAndDefaultsNeutral() {
        assertEquals(0f, lightTempBlendFraction(2000), 0.001f)
        assertEquals(1f, lightTempBlendFraction(6500), 0.001f)
        assertEquals(0f, lightTempBlendFraction(1000), 0.001f)
        assertEquals(1f, lightTempBlendFraction(9000), 0.001f)
        assertEquals(0.5f, lightTempBlendFraction(null), 0.001f)
    }

    @Test
    fun lightTempTintEndpointsAreWarmAndCool() {
        val warm = lightColorTempTint(2000)
        val cool = lightColorTempTint(6500)
        assertEquals(Palette.Honey.t80, warm)
        // Lerp at fraction 1 can differ by 1 ULP from the endpoint, so
        // compare components with tolerance instead of exact equality.
        val expected = Palette.Frost.t90
        assertEquals(expected.red, cool.red, 0.002f)
        assertEquals(expected.green, cool.green, 0.002f)
        assertEquals(expected.blue, cool.blue, 0.002f)
        // Candlelight is redder than daylight, daylight bluer than candlelight.
        assertTrue(warm.red > warm.blue)
        assertTrue(cool.blue > warm.blue)
    }

    @Test
    fun lightContainerBlendsSecondaryToTertiary() {
        assertEquals(scheme.secondaryContainer, lightContainerColor(scheme, 2000, true))
        val hot = lightContainerColor(scheme, 6500, true)
        assertEquals(scheme.tertiaryContainer.red, hot.red, 0.002f)
        assertEquals(scheme.tertiaryContainer.green, hot.green, 0.002f)
        assertEquals(scheme.tertiaryContainer.blue, hot.blue, 0.002f)
        assertEquals(scheme.surfaceContainerHighest, lightContainerColor(scheme, 2000, false))
    }

    @Test
    fun climateContainersFollowTheTokenTable() {
        assertEquals(scheme.errorContainer, heatSetpointContainer(scheme, false))
        assertEquals(scheme.tertiaryContainer, heatSetpointContainer(scheme, true))
        assertEquals(scheme.onErrorContainer, heatSetpointOnContainer(scheme, false))
        assertEquals(scheme.onTertiaryContainer, heatSetpointOnContainer(scheme, true))
        assertEquals(scheme.primaryContainer, coolSetpointContainer(scheme))
        assertEquals(scheme.surfaceContainerHighest, climateIdleContainer(scheme))
    }

    @Test
    fun switchBinaryAndMediaContainers() {
        assertEquals(scheme.secondaryContainer, switchContainer(scheme, true))
        assertEquals(scheme.surfaceContainerHighest, switchContainer(scheme, false))
        assertEquals(scheme.errorContainer, binarySensorContainer(scheme, true))
        assertEquals(scheme.surfaceContainerHighest, binarySensorContainer(scheme, false))
        assertEquals(scheme.tertiaryContainer, mediaContainerColor(scheme, true))
        assertEquals(scheme.surfaceContainerHighest, mediaContainerColor(scheme, false))
    }

    @Test
    fun badgeMorphProgressByDomainAndState() {
        assertEquals(1f, badgeMorphProgress("light", "on", null), 0.001f)
        assertEquals(0f, badgeMorphProgress("light", "off", null), 0.001f)
        assertEquals(0f, badgeMorphProgress("climate", "off", null), 0.001f)
        assertEquals(0f, badgeMorphProgress("climate", "heat", "idle"), 0.001f)
        assertEquals(1f, badgeMorphProgress("climate", "heat", "heating"), 0.001f)
        assertEquals(1f, badgeMorphProgress("climate", "heat_cool", null), 0.001f)
        assertEquals(0f, badgeMorphProgress("cover", "closed", null), 0.001f)
        assertEquals(1f, badgeMorphProgress("cover", "open", null), 0.001f)
        assertEquals(1f, badgeMorphProgress("media_player", "playing", null), 0.001f)
        assertEquals(0f, badgeMorphProgress("media_player", "paused", null), 0.001f)
        assertEquals(1f, badgeMorphProgress("weather", "sunny", null), 0.001f)
        assertEquals(1f, badgeMorphProgress("binary_sensor", "on", null), 0.001f)
        assertEquals(0f, badgeMorphProgress("binary_sensor", "off", null), 0.001f)
        // Sensors never morph: no severity threshold is invented for them.
        assertEquals(0f, badgeMorphProgress("sensor", "72.5", null), 0.001f)
    }

    @Test
    fun badgeAlertOnlyForTriggeredBinarySensors() {
        assertTrue(badgeAlert("binary_sensor", "on"))
        assertFalse(badgeAlert("binary_sensor", "off"))
        assertFalse(badgeAlert("light", "on"))
    }

    @Test
    fun badgeShapesFollowThePerDomainTable() {
        assertEquals(StateShape.HEXAGON, badgeIdleShape("light"))
        assertEquals(StateShape.SCALLOP12, badgeActiveShape("light"))
        assertEquals(StateShape.SQUIRCLE, badgeIdleShape("climate"))
        assertEquals(StateShape.ARC_WAVE, badgeActiveShape("climate"))
        assertEquals(StateShape.SHIELD, badgeIdleShape("cover"))
        assertEquals(StateShape.NOTCHED_DIAMOND, badgeActiveShape("cover"))
        assertEquals(StateShape.SQUIRCLE, badgeIdleShape("media_player"))
        assertEquals(StateShape.STAR8, badgeActiveShape("media_player"))
        assertEquals(StateShape.OCTAGON, badgeIdleShape("weather"))
        assertEquals(StateShape.SUNBURST, badgeActiveShape("weather", "sunny"))
        assertEquals(StateShape.CLOUD_POLY, badgeActiveShape("weather", "rainy"))
        assertEquals(StateShape.OCTAGON, badgeIdleShape("sensor"))
        assertEquals(StateShape.OCTAGON, badgeActiveShape("sensor"))
    }

    @Test
    fun travelBadgeDestinationIsTheSettledStateShape() {
        // Active states land on the active polygon; idle states on
        // the idle polygon. Weather always lands active (its morph
        // progress is 1), picking sunburst vs cloud by condition.
        assertEquals(StateShape.SCALLOP12, travelBadgeDestinationShape("light", "on", null, null))
        assertEquals(StateShape.HEXAGON, travelBadgeDestinationShape("light", "off", null, null))
        assertEquals(StateShape.ARC_WAVE, travelBadgeDestinationShape("climate", "heat_cool", "heating", null))
        assertEquals(StateShape.SQUIRCLE, travelBadgeDestinationShape("climate", "off", "idle", null))
        assertEquals(StateShape.SHIELD, travelBadgeDestinationShape("cover", "closed", null, null))
        assertEquals(StateShape.NOTCHED_DIAMOND, travelBadgeDestinationShape("cover", "open", null, null))
        assertEquals(StateShape.SUNBURST, travelBadgeDestinationShape("weather", "partlycloudy", null, "partlycloudy"))
        assertEquals(StateShape.CLOUD_POLY, travelBadgeDestinationShape("weather", "rainy", null, "rainy"))
        assertEquals(StateShape.OCTAGON, travelBadgeDestinationShape("sensor", "72.5", null, null))
        assertEquals(StateShape.STAR8, travelBadgeDestinationShape("media_player", "playing", null, null))
    }

    @Test
    fun coverMovingOnlyWhileOpeningOrClosing() {
        assertTrue(coverIsMoving("opening"))
        assertTrue(coverIsMoving("closing"))
        assertFalse(coverIsMoving("open"))
        assertFalse(coverIsMoving("closed"))
    }

    @Test
    fun climateModeColorFollowsHeatingCooling() {
        assertEquals(HeatOrange, climateModeColor("heat", null))
        assertEquals(CoolBlue, climateModeColor("cool", null))
        // heat_cool / auto default to the heating color (heating
        // selector is the default selector).
        assertEquals(HeatOrange, climateModeColor("heat_cool", null))
        assertEquals(HeatOrange, climateModeColor("auto", null))
        // hvac_action is the live truth and wins over the mode.
        assertEquals(CoolBlue, climateModeColor("heat_cool", "cooling"))
        assertEquals(HeatOrange, climateModeColor("heat_cool", "heating"))
        // Off / idle / dry / fan_only are neither: neutral, never a
        // heating or cooling color.
        assertEquals(ClimateNeutral, climateModeColor("off", null))
        assertEquals(ClimateNeutral, climateModeColor("heat", "idle"))
        assertEquals(ClimateNeutral, climateModeColor("dry", null))
        assertEquals(ClimateNeutral, climateModeColor("fan_only", null))
        assertTrue(HeatOrange != CoolBlue && HeatOrange != ClimateNeutral && CoolBlue != ClimateNeutral)
    }

    @Test
    fun lightBrightnessContainerScalesWithBrightness() {
        // Off is always the neutral idle container.
        assertEquals(scheme.surfaceContainerHighest, lightBrightnessContainer(scheme, 3000, 100, false))
        // Full brightness equals the kelvin-tinted container.
        val full = lightContainerColor(scheme, 3000, true)
        val atFull = lightBrightnessContainer(scheme, 3000, 100, true)
        assertEquals(full.red, atFull.red, 0.002f)
        assertEquals(full.green, atFull.green, 0.002f)
        assertEquals(full.blue, atFull.blue, 0.002f)
        // Dimmer is closer to neutral than full; the 0.25 floor keeps
        // a dim light tinted, never gray.
        val dim = lightBrightnessContainer(scheme, 3000, 0, true)
        assertTrue(dim.red != scheme.surfaceContainerHighest.red ||
            dim.green != scheme.surfaceContainerHighest.green ||
            dim.blue != scheme.surfaceContainerHighest.blue)
        // Monotonic: brightness 25 < 50 < 100 in distance from neutral
        // toward the tinted container (checked on one channel).
        val c25 = lightBrightnessContainer(scheme, 3000, 25, true)
        val c50 = lightBrightnessContainer(scheme, 3000, 50, true)
        val d25 = kotlin.math.abs(c25.red - scheme.surfaceContainerHighest.red)
        val d50 = kotlin.math.abs(c50.red - scheme.surfaceContainerHighest.red)
        val d100 = kotlin.math.abs(atFull.red - scheme.surfaceContainerHighest.red)
        assertTrue(d25 <= d50)
        assertTrue(d50 <= d100)
        // Unknown brightness means full.
        assertEquals(atFull.red, lightBrightnessContainer(scheme, 3000, null, true).red, 0.002f)
    }

    @Test
    fun weatherConditionColorCoversConditions() {
        // Each kind of sky has its own color, and anything unrecognised
        // reads as plain cloud rather than as fair weather.
        val sunny = weatherConditionColor("sunny")
        assertEquals(sunny, weatherConditionColor("clear"))
        assertEquals(weatherConditionColor("rainy"), weatherConditionColor("pouring"))
        val distinct = listOf("sunny", "clear-night", "rainy", "snowy", "lightning", "partlycloudy", "fog", "windy")
            .map { weatherConditionColor(it) }
        assertEquals(distinct.size, distinct.toSet().size)
        assertEquals(weatherConditionColor("partlycloudy"), weatherConditionColor(null))
        assertEquals(weatherConditionColor("partlycloudy"), weatherConditionColor("exceptional"))
        // Sun is warm, rain is cool.
        assertTrue(sunny.red > sunny.blue)
        assertTrue(weatherConditionColor("rainy").blue > weatherConditionColor("rainy").red)
    }

    @Test
    fun formatConditionSplitsAndTitles() {
        assertEquals("Partly Cloudy", formatCondition("partlycloudy"))
        assertEquals("Partly Cloudy", formatCondition("partly-cloudy"))
        assertEquals("Partly Cloudy", formatCondition("partlyCloudy"))
        assertEquals("Clear Night", formatCondition("clear-night"))
        assertEquals("Clear Night", formatCondition("clear_night"))
        assertEquals("Snowy Rainy", formatCondition("snowy-rainy"))
        assertEquals("Lightning Rainy", formatCondition("lightning-rainy"))
        assertEquals("Sunny", formatCondition("sunny"))
        assertEquals("Windy Variant", formatCondition("windy-variant"))
        assertEquals("", formatCondition(""))
    }

    @Test
    fun dialogPanelGeometryMatchesSpec() {
        assertEquals(324f, dialogPanelWidthDp(360f), 0.001f)
        assertEquals(480f, dialogPanelWidthDp(800f), 0.001f)
        assertEquals(680f, dialogPanelMaxHeightDp(800f), 0.001f)
    }

    @Test
    fun contrastRatioKnownPairs() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.1f)
        assertEquals(1f, contrastRatio(Color.Black, Color.Black), 0.001f)
        assertTrue(contrastRatio(Color(0xFFD84315), Color.White) > 4f)
    }

    @Test
    fun ensureContrastLiftsAndPreserves() {
        assertEquals(Color.Black, ensureContrast(Color.Black, Color.White))
        assertEquals(Color.White, ensureContrast(Color.White, Color.Black))
        val darkDisc = Color(0xFF2B2930)
        val lifted = ensureContrast(Color(0xFFD84315), darkDisc)
        assertTrue(contrastRatio(lifted, darkDisc) >= 4.5f)
        // Inactive-number composite: 45% heat orange over a pale disc,
        // floored to the 3.0 large-text ratio instead of washing out.
        val paleDisc = Color(0xFFFBE9E0)
        val inactive = ensureContrast(
            compositeOver(Color(0xFFD84315).copy(alpha = 0.45f), paleDisc),
            paleDisc,
            3.0f,
        )
        assertTrue(contrastRatio(inactive, paleDisc) >= 3.0f)
    }

    @Test
    fun litLightBadgeGlyphClearsContrastFloor() {
        // Hotfix regression: round 1 paired the primary badge with
        // an onPrimaryContainer glyph — pale-on-pale in the dark
        // theme (primary is a light pastel there, and so is
        // onPrimaryContainer). The matched primary + onPrimary
        // pair, floored, must clear 4.5:1 on both schemes, so the
        // polygon badge reads on every dynamic palette.
        val lamp = com.garrietta.haven.data.HaEntity(
            "light.standing_lamp_socket_1", "on",
            mapOf("friendly_name" to "Standing Lamp"),
        )
        for (scheme in listOf(lightColorScheme(), androidx.compose.material3.darkColorScheme())) {
            val (bg, glyph) = badgeColors(scheme, lamp, Color.Gray)
            assertTrue(contrastRatio(glyph, bg) >= 4.5f)
        }
    }

    @Test
    fun dialogNameSize_shrinksLongNamesInsteadOfEllipsizing() {
        // 2026-10-30 verdict: "Thermostat Current Temperatu…"
        // must show in full — size steps down before any ellipsis.
        assertEquals(20.sp, dialogNameSize("Lamp"))
        assertEquals(20.sp, dialogNameSize("Ecobee Thermostat"))
        assertEquals(17.sp, dialogNameSize("Thermostat Current Humidity"))
        assertEquals(15.sp, dialogNameSize("Thermostat Current Temperature"))
    }

    // Round 4: sensor semantics — synchronous kind resolution and
    // semantic badge colors (never async, so no flash).
    @Test
    fun sensorKind_ecobeePair() {
        assertEquals(SensorKind.TEMPERATURE, sensorKind("sensor.thermostat_current_temperature", "temperature", "Thermostat Current Temperature"))
        assertEquals(SensorKind.HUMIDITY, sensorKind("sensor.thermostat_current_humidity", "humidity", "Thermostat Current Humidity"))
    }

    @Test
    fun sensorKind_byIdDeviceClassAndName() {
        assertEquals(SensorKind.BATTERY, sensorKind("sensor.robovac_battery", null, "Robovac Battery"))
        assertEquals(SensorKind.POWER, sensorKind("sensor.plug_power", "power", "Plug Power"))
        assertEquals(SensorKind.LIGHT, sensorKind("sensor.office_illuminance", "illuminance", "Office Illuminance"))
        assertEquals(SensorKind.OTHER, sensorKind("sensor.mystery", null, "Mystery"))
    }

    @Test
    fun sensorBadgeColors_semanticPairs() {
        // Temperature is warm, humidity is cool, and each icon reads on
        // its fill. A reading with no meaning of its own gets no pair.
        val (heatFill, heatGlyph) = sensorBadgeColors(SensorKind.TEMPERATURE)!!
        val (waterFill, waterGlyph) = sensorBadgeColors(SensorKind.HUMIDITY)!!
        assertTrue(heatFill.red > heatFill.blue)
        assertTrue(waterFill.blue > waterFill.red)
        assertTrue(contrastRatio(heatGlyph, heatFill) >= 4.5f)
        assertTrue(contrastRatio(waterGlyph, waterFill) >= 4.5f)
        assertEquals(null, sensorBadgeColors(SensorKind.OTHER))
    }

    @Test
    fun sensorCardTint_semanticOrNull() {
        assertEquals(sensorBadgeColors(SensorKind.TEMPERATURE)!!.first, sensorCardTint(SensorKind.TEMPERATURE))
        assertEquals(sensorBadgeColors(SensorKind.HUMIDITY)!!.first, sensorCardTint(SensorKind.HUMIDITY))
        assertEquals(null, sensorCardTint(SensorKind.OTHER))
    }

    // ------------------------------------------------------------------
    // Round 8 dark-theme pass
    // ------------------------------------------------------------------

    @Test
    fun darkConditionColorsBrightenOnlyGreyFamily() {
        // Saturated hues are identical in both themes (same semantic
        // colors, not a second palette); only the grey family lifts.
        for (grey in listOf("partlycloudy", "cloudy", "fog")) {
            val day = weatherConditionColorThemed(grey, false)
            val night = weatherConditionColorThemed(grey, true)
            assertEquals(weatherConditionColor(grey), day)
            assertTrue("$grey lifts on black", relativeLuminance(night) > relativeLuminance(day))
        }
        for (vivid in listOf("sunny", "rainy", "lightning", "snowy", "clear-night")) {
            assertEquals(weatherConditionColor(vivid), weatherConditionColorThemed(vivid, true))
        }
        // Dark fills separate from true-black background.
        for (condition in listOf("partlycloudy", "cloudy", "fog", "sunny", "rainy", "snowy", "lightning", "clear-night")) {
            assertTrue(contrastRatio(weatherConditionColorThemed(condition, true), Color.Black) >= 3.0f)
        }
    }

    @Test
    fun chipGlyphFollowsTheFillInBothThemes() {
        // Dark ink on a light fill, white on a dark one, whatever the
        // theme: the fill decides, so no chip's icon dissolves into it.
        for (isDark in listOf(false, true)) {
            assertTrue(relativeLuminance(weatherChipGlyphColor(weatherConditionColor("sunny"), isDark)) < 0.2f)
            assertEquals(Color.White, weatherChipGlyphColor(weatherConditionColor("lightning"), isDark))
            for (condition in listOf("sunny", "partlycloudy", "cloudy", "fog", "rainy", "snowy", "lightning", "clear-night", "windy", "hail")) {
                val fill = weatherConditionColorThemed(condition, isDark)
                assertTrue(
                    "$condition dark=$isDark",
                    contrastRatio(weatherChipGlyphColor(fill, isDark), fill) >= 3.0f,
                )
            }
        }
    }

    @Test
    fun legibleSecondaryLiftsDimPairsAndPreservesPassing() {
        val dimScheme = scheme.copy(onSurfaceVariant = Color(0xFF666666))
        val lifted = legibleSecondaryOn(dimScheme, Color.Black)
        assertTrue(contrastRatio(lifted, Color.Black) >= 4.5f)
        assertTrue(lifted != Color(0xFF666666))
        // A passing pair is returned unchanged (light theme untouched).
        assertEquals(scheme.onSurfaceVariant, legibleSecondaryOn(scheme, scheme.background))
        // Backdrop detection honors explicit theme overrides via luminance.
        assertTrue(isDarkBackdrop(Color.Black))
        assertFalse(isDarkBackdrop(Color.White))
        assertTrue(isDarkBackdrop(darkColorScheme().background))
    }

    @Test
    fun selectedPillIsLimeCapsuleInBothThemes() {
        // UI rewrite round: the selected pill is the SAME lime
        // capsule in both themes — the Round 8 blue dark pill is
        // gone (it was the flagged dark-rail bug). The capsule
        // stays lime whether dynamic color is on or off.
        assertEquals(PillBarActiveCapsule, selectedPillFill(scheme, false))
        assertEquals(PillBarActiveGlyph, selectedPillGlyph(scheme, false))
        assertEquals(PillBarActiveCapsule, selectedPillFill(scheme, true))
        assertEquals(PillBarActiveGlyph, selectedPillGlyph(scheme, true))
        assertTrue(contrastRatio(PillBarActiveGlyph, PillBarActiveCapsule) >= 4.5f)
    }
}
