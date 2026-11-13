package com.garrietta.haven.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
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
 *   color    Color.kt     the fixed light and dark schemes
 *   type     Type.kt      Google Sans Flex, one scale
 *   shape    Shape.kt     the corner scale
 *   motion   Motion.kt    Material's expressive scheme for stock
 *                         components, Haven's springs for its own
 *
 * [dynamicColor] is the wallpaper-color option and is off unless the
 * user turns it on: the fixed palette is the identity, and the state
 * families never follow the wallpaper either way. When it is on, dark
 * still keeps Haven's true-black ground and container steps.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HavenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        !dynamicColor -> if (darkTheme) havenDarkColorScheme() else havenLightColorScheme()
        darkTheme -> runCatching { dynamicDarkColorScheme(context) }
            .getOrElse { havenDarkColorScheme() }
            .withHavenDarkSurfaces()
        else -> runCatching { dynamicLightColorScheme(context) }
            .getOrElse { havenLightColorScheme() }
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = HavenShapes,
        typography = HavenTypography,
        content = content,
    )
}
