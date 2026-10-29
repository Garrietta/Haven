package com.garrietta.haven.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
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

/**
 * The spec's motion foundation (ARCHITECTURE AND SYSTEM FOUNDATIONS).
 * havenSpring (380/0.62) is kept for existing call sites — it is the
 * bouncy spring to within 20 stiffness — while new code uses these
 * named specs so the language stays explicit and greppable.
 */
/** Emphasized decelerate: standard entrances, sheets, expands. */
val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/** Emphasized accelerate: exits, dismissals. */
val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/** Spring Bouncy: the house feel, named. */
fun <T> springBouncy(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

/** Spring Snappy: tighter, for quick controls. */
fun <T> springSnappy(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)

/** Morph spring: shape and bounds morphs. */
fun <T> springMorph(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.72f, stiffness = 320f)

/**
 * The unified motion language (spec item 4 + the 2026-10-29 visual-bugs
 * round). The spec's HavenMotion names map onto the existing tokens —
 * they are NOT duplicated: FluidEasing IS EmphasizedDecelerate
 * (0.05, 0.7, 0.1, 1.0) and ExitFast's easing IS EmphasizedAccelerate
 * (0.3, 0.0, 0.8, 0.15), so code keeps using the existing names.
 */
/** Emphasized spatial: the spec's EmphasizedSpatial, for entrances. */
fun <T> springSpatial(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

/** Emphasized bounds: the spec's EmphasizedBounds, for container transforms. */
fun <T> springBounds(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/**
 * The SINGLE press token (spec item 4). The spec gives two PressBounce
 * definitions — 0.6/StiffnessLow in the stepper snippet and 0.55/Medium in
 * HavenMotion — and only one ships: 0.55/Medium. It lands faster and
 * reads as a button press rather than a wobble; the looser 0.6/Low
 * variant is not used anywhere. Every press-scale interaction (tiles,
 * steppers, media transports) uses this and nothing else.
 */
fun <T> pressBounce(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

/** ExitFast: the spec's 180ms exit tween on the accelerate easing. */
fun <T> exitFast(): FiniteAnimationSpec<T> =
    tween(durationMillis = 180, easing = EmphasizedAccelerate)

/**
 * The sheet-rise spring, matched to the recorded Koda mini-player
 * expansion: a fast rise (~half a second) with a soft settle and almost
 * no visible overshoot. Stiffer and more damped than the house spring on
 * purpose — Koda's sheet does not bounce the way its pills do.
 */
fun <T> sheetSpring(): FiniteAnimationSpec<T> = spring(stiffness = 500f, dampingRatio = 0.85f)

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
