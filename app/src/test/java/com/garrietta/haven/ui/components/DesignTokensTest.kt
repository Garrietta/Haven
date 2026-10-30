package com.garrietta.haven.ui.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.theme.StateShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokensTest {

    private val scheme = lightColorScheme()

    @Test
    fun pillLabelVisibleOnlyWhenSelectedAndShort() {
        assertTrue(pillLabelVisible("Bedroom", true))
        assertTrue(pillLabelVisible("1234567890", true))
        assertFalse(pillLabelVisible("Living Room", true))
        assertFalse(pillLabelVisible("Bedroom", false))
        assertFalse(pillLabelVisible("Living Room", false))
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
        assertEquals(Color(0xFFFFB300), lightColorTempTint(2000))
        // Lerp at fraction 1 can differ by 1 ULP from the endpoint, so
        // compare components with tolerance instead of exact equality.
        val cool = lightColorTempTint(6500)
        val expected = Color(0xFFCCE4FF)
        assertEquals(expected.red, cool.red, 0.002f)
        assertEquals(expected.green, cool.green, 0.002f)
        assertEquals(expected.blue, cool.blue, 0.002f)
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
        assertEquals(Color(0xFFD84315), climateModeColor("heat", null))
        assertEquals(Color(0xFF1565C0), climateModeColor("cool", null))
        // heat_cool / auto default to the heating color (heating
        // selector is the default selector).
        assertEquals(Color(0xFFD84315), climateModeColor("heat_cool", null))
        assertEquals(Color(0xFFD84315), climateModeColor("auto", null))
        // hvac_action is the live truth and wins over the mode.
        assertEquals(Color(0xFF1565C0), climateModeColor("heat_cool", "cooling"))
        assertEquals(Color(0xFFD84315), climateModeColor("heat_cool", "heating"))
        // Off / idle / dry / fan_only are neither: neutral, never a
        // heating or cooling color.
        assertEquals(Color(0xFF78909C), climateModeColor("off", null))
        assertEquals(Color(0xFF78909C), climateModeColor("heat", "idle"))
        assertEquals(Color(0xFF78909C), climateModeColor("dry", null))
        assertEquals(Color(0xFF78909C), climateModeColor("fan_only", null))
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
        assertEquals(Color(0xFFFFB300), weatherConditionColor("sunny"))
        assertEquals(Color(0xFF5C6BC0), weatherConditionColor("clear-night"))
        assertEquals(Color(0xFF1E88E5), weatherConditionColor("rainy"))
        assertEquals(Color(0xFF81D4FA), weatherConditionColor("snowy"))
        assertEquals(Color(0xFF7E57C2), weatherConditionColor("lightning"))
        assertEquals(Color(0xFF90A4AE), weatherConditionColor("partlycloudy"))
        assertEquals(Color(0xFF90A4AE), weatherConditionColor(null))
        assertEquals(Color(0xFF90A4AE), weatherConditionColor("exceptional"))
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
}
