package com.garrietta.haven.ui.theme

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.components.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage B (redesign): the pinned Haven light/dark palettes are the
 * permanent theme identity, so their contrast floors are pinned by
 * test — a palette change that breaks legibility must fail here,
 * not on the user's phone.
 */
class ThemeTest {

    private val light = havenLightColorScheme()
    private val dark = havenDarkColorScheme()

    private fun assertTextPair(foreground: Color, background: Color) {
        assertTrue(
            "contrast ${contrastRatio(foreground, background)} below 4.5:1",
            contrastRatio(foreground, background) >= 4.5f,
        )
    }

    @Test
    fun pinnedLightTextPairsClearFourPointFive() {
        assertTextPair(light.onBackground, light.background)
        assertTextPair(light.onSurface, light.surface)
        // onSurfaceVariant must clear 4.5:1 on every light surface it
        // can sit on — background plus all container steps.
        val surfaces = listOf(
            light.background,
            light.surface,
            light.surfaceContainerLowest,
            light.surfaceContainerLow,
            light.surfaceContainer,
            light.surfaceContainerHigh,
            light.surfaceContainerHighest,
        )
        for (surface in surfaces) {
            assertTextPair(light.onSurfaceVariant, surface)
        }
    }

    @Test
    fun pinnedDarkTextPairsClearFourPointFive() {
        assertTextPair(dark.onBackground, dark.background)
        assertTextPair(dark.onSurface, dark.surface)
        val surfaces = listOf(
            dark.background,
            dark.surface,
            dark.surfaceContainerLowest,
            dark.surfaceContainerLow,
            dark.surfaceContainer,
            dark.surfaceContainerHigh,
            dark.surfaceContainerHighest,
        )
        for (surface in surfaces) {
            assertTextPair(dark.onSurfaceVariant, surface)
        }
    }

    @Test
    fun pinnedAccentPairsClearFourPointFive() {
        for (scheme in listOf(light, dark)) {
            assertTextPair(scheme.onPrimary, scheme.primary)
            assertTextPair(scheme.onPrimaryContainer, scheme.primaryContainer)
            assertTextPair(scheme.onSecondary, scheme.secondary)
            assertTextPair(scheme.onSecondaryContainer, scheme.secondaryContainer)
            assertTextPair(scheme.onTertiary, scheme.tertiary)
            assertTextPair(scheme.onTertiaryContainer, scheme.tertiaryContainer)
            assertTextPair(scheme.onError, scheme.error)
            assertTextPair(scheme.onErrorContainer, scheme.errorContainer)
        }
    }

    @Test
    fun pinnedLightIdentityValues() {
        // Warm off-white background, charcoal text, lime primary
        // container — the mockup sage/lime identity, not wallpaper.
        assertEquals(Color(0xFFFAF7F0), light.background)
        assertEquals(Color(0xFF1E1E1A), light.onSurface)
        assertEquals(Color(0xFF1E1E1A), light.onBackground)
        assertEquals(Color(0xFFC1E88A), light.primaryContainer)
    }

    @Test
    fun pinnedDarkIdentityValues() {
        // True-black background and container steps (Round 8),
        // with the pinned warm light-grey secondary floor.
        assertEquals(Color.Black, dark.background)
        assertEquals(Color(0xFF121212), dark.surface)
        assertEquals(Color(0xFF383838), dark.surfaceContainerHighest)
        assertEquals(Color(0xFFC9C9C4), dark.onSurfaceVariant)
    }

    @Test
    fun resolveHavenIsDarkMatrix() {
        assertFalse(resolveHavenIsDark("light", true))
        assertFalse(resolveHavenIsDark("light", false))
        assertTrue(resolveHavenIsDark("dark", true))
        assertTrue(resolveHavenIsDark("dark", false))
        assertTrue(resolveHavenIsDark("system", true))
        assertFalse(resolveHavenIsDark("system", false))
        // Unknown values follow the system, never crash or default dark.
        assertTrue(resolveHavenIsDark("bogus", true))
        assertFalse(resolveHavenIsDark("bogus", false))
    }
}
