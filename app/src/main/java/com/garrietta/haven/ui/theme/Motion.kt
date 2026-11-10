package com.garrietta.haven.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/*
 * Haven's motion.
 *
 * Anything that answers a finger is a spring; a tween is only for things
 * a spring models badly (a timed exit, a crossfade). Stock Material
 * components take their motion from MotionScheme.expressive(), set in
 * HavenTheme. Everything Haven draws itself uses one of the named
 * springs below, and nothing else: a call site never writes spring(...)
 * with its own numbers.
 *
 * The numbers were tuned on the phone, a round at a time. They are
 * grouped here by what they move, which is how to pick one:
 *
 *   touch       a press, a toggle           pressBounce, springSnappy
 *   travel      something crossing space    havenSpring, springSpatial, barSpring
 *   container   a thing changing size       springBounds, springMorph,
 *               or becoming another thing   popupMorphSpring, sheetSpring
 *   settle      coming to rest, no bounce   settleSpring
 *   exit        leaving                     exitFast
 */

// --- Touch -----------------------------------------------------------------

/** The one press spring: every press-scale (tiles, steppers, transports). */
fun <T> pressBounce(): FiniteAnimationSpec<T> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

/** Tighter than a press, for quick controls that should not wobble. */
fun <T> springSnappy(): FiniteAnimationSpec<T> =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

// --- Travel ----------------------------------------------------------------

/** The house spring: bouncy, the default feel of anything animated. */
fun <T> havenSpring(): FiniteAnimationSpec<T> = spring(stiffness = 380f, dampingRatio = 0.62f)

/** The house feel on a canonical stiffness. */
fun <T> springBouncy(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

/** Entrances and panel travel. The navigation bar hides and returns on this. */
fun <T> springSpatial(): FiniteAnimationSpec<T> =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/**
 * The navigation capsule sliding between tabs. These are the numbers of
 * Material's expressive fast-spatial spring, pinned so the bar keeps its
 * feel whatever the theme's motion scheme is set to.
 */
fun <T> barSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

// --- Container -------------------------------------------------------------

/** Container transforms: a rectangle interpolating to another rectangle. */
fun <T> springBounds(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/** Polygon and bounds morphs: a badge blooming, a shape changing. */
fun <T> springMorph(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.72f, stiffness = 320f)

/**
 * A tile becoming its popup. Same numbers as [sheetSpring] and its own
 * name, so the popup's opening speed can be tuned without moving sheets.
 */
fun <T> popupMorphSpring(): FiniteAnimationSpec<T> = spring(stiffness = 500f, dampingRatio = 0.85f)

/** A sheet rising: quick, with a soft landing and almost no overshoot. */
fun <T> sheetSpring(): FiniteAnimationSpec<T> = spring(stiffness = 500f, dampingRatio = 0.85f)

// --- Settle and exit -------------------------------------------------------

/**
 * Coming to rest with no visible bounce: every popup dismissal, and the
 * spring back to open after a cancelled drag. Opening is expressive;
 * closing is calm.
 */
fun <T> settleSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.95f, stiffness = 350f)

/** Emphasized decelerate: timed entrances, sheets, expands. */
val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/** Emphasized accelerate: timed exits and dismissals. */
val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/** A timed exit: 180ms on the accelerate curve. */
fun <T> exitFast(): FiniteAnimationSpec<T> = tween(durationMillis = 180, easing = EmphasizedAccelerate)

// --- Reduce motion ---------------------------------------------------------

/**
 * The house spring, or an instant one when the user has asked for less
 * motion. Finite either way, so it also serves animateContentSize.
 */
fun <T> havenMotion(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f) else havenSpring()
