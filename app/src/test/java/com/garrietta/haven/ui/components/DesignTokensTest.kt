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
    fun coverMovingOnlyWhileOpeningOrClosing() {
        assertTrue(coverIsMoving("opening"))
        assertTrue(coverIsMoving("closing"))
        assertFalse(coverIsMoving("open"))
        assertFalse(coverIsMoving("closed"))
    }
}
