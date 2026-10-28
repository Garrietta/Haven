package com.garrietta.haven.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** The house spring: bouncy, shared by every animated control in Haven. */
fun <T> havenSpring(): FiniteAnimationSpec<T> = spring(stiffness = 380f, dampingRatio = 0.62f)

/** Instant spec used when reduce-motion is on. Finite, so it also serves animateContentSize and expand/shrink. */
fun <T> havenMotion(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f) else havenSpring()

@Composable
fun HavenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = if (darkTheme) {
        try {
            dynamicDarkColorScheme(context)
        } catch (e: Exception) {
            darkColorScheme()
        }
    } else {
        try {
            dynamicLightColorScheme(context)
        } catch (e: Exception) {
            lightColorScheme()
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content,
    )
}
