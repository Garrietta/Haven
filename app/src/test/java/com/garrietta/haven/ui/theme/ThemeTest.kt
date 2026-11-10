package com.garrietta.haven.ui.theme

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.components.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.garrietta.haven.ui.components.relativeLuminance

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
    fun lightIdentity() {
        // Paper ground, near-black ink, the leaf green for actions and
        // a lime container: Haven's own palette, never the wallpaper's.
        assertEquals(Palette.Cream.t98, light.background)
        assertEquals(light.background, light.surface)
        assertEquals(Palette.Cream.t10, light.onSurface)
        assertEquals(Palette.Leaf.t40, light.primary)
        assertEquals(Palette.Glow.t90, light.primaryContainer)
        // Containers step away from the page in order.
        val steps = listOf(
            light.surfaceContainerLowest, light.background, light.surfaceContainerLow,
            light.surfaceContainer, light.surfaceContainerHigh, light.surfaceContainerHighest,
        ).map { relativeLuminance(it) }
        assertEquals(steps.sortedDescending(), steps)
    }

    @Test
    fun darkIdentity() {
        // True black, containers climbing out of it in order, and the
        // lime itself as the color of things you can press.
        assertEquals(Color.Black, dark.background)
        assertEquals(Color.Black, dark.surface)
        assertEquals(Palette.Lime, dark.primary)
        val steps = listOf(
            dark.background, dark.surfaceContainerLow, dark.surfaceContainer,
            dark.surfaceContainerHigh, dark.surfaceContainerHighest,
        ).map { relativeLuminance(it) }
        assertEquals(steps.sorted(), steps)
        assertEquals(steps.size, steps.toSet().size)
    }

    @Test
    fun wallpaperColorKeepsHavensDarkGround() {
        // The dynamic-color option takes accents from the wallpaper but
        // never the black ground or the container steps.
        val foreign = androidx.compose.material3.darkColorScheme(
            primary = Color.Magenta, background = Color.Gray, surface = Color.Gray, surfaceContainer = Color.Gray,
        ).withHavenDarkSurfaces()
        assertEquals(Color.Magenta, foreign.primary)
        assertEquals(dark.background, foreign.background)
        assertEquals(dark.surface, foreign.surface)
        assertEquals(dark.surfaceContainer, foreign.surfaceContainer)
        assertEquals(dark.surfaceContainerHighest, foreign.surfaceContainerHighest)
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
