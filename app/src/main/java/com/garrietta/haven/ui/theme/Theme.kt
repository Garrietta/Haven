package com.garrietta.haven.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext

/**
 * Which theme to draw: "light" and "dark" are the user's choice, and
 * anything else (including "system") follows the phone.
 */
fun resolveHavenIsDark(themeMode: String, isSystemDark: Boolean): Boolean =
    when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

/**
 * The root of the app. Everything Haven draws sits inside this one
 * Material 3 Expressive theme, which is where its four systems are
 * handed to every component at once:
 *
 *   color    Color.kt, ColorStyles.kt   the schemes, the state families
 *   type     Type.kt                    Google Sans Flex, one scale
 *   shape    Shape.kt                   the corner scale
 *   motion   Motion.kt                  Material's expressive scheme for
 *                                       stock components, Haven's springs
 *                                       for its own
 *
 * [style] is the color style chosen in Appearance. It is published to
 * [ActiveStyle] before the content composes, and the content is keyed on
 * it, so a change recolors everything at once. The wallpaper style takes
 * its accents from the wallpaper (where the phone offers them) and keeps
 * Haven's true-black ground at night; its capsule is the wallpaper's
 * bright accent.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HavenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: ColorStyle = ColorStyle.HAVEN,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme: ColorScheme = if (style == ColorStyle.WALLPAPER) {
        val dynamic = runCatching { if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }.getOrNull()
        ActiveStyle.wallpaperCapsule = dynamic?.let { if (darkTheme) it.primary to it.onPrimary else it.primaryContainer to it.onPrimaryContainer }
        when {
            dynamic == null -> styleColorScheme(ColorStyle.HAVEN, darkTheme)
            darkTheme -> dynamic.withHavenDarkSurfaces()
            else -> dynamic
        }
    } else {
        styleColorScheme(style, darkTheme)
    }
    ActiveStyle.current = style
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = HavenShapes,
        typography = HavenTypography,
    ) {
        key(style) { content() }
    }
}
