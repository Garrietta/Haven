package com.garrietta.haven.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

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

/** Spring Snappy: tighter, for quick controls. Canonical tactile values:
 * Spring.DampingRatioLowBouncy + Spring.StiffnessMedium (was 0.78/Medium). */
fun <T> springSnappy(): FiniteAnimationSpec<T> =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

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
/** Emphasized spatial: the spec's EmphasizedSpatial, for entrances.
 * Canonical LowBouncy damping (was 0.78); stiffness stays MediumLow
 * because this is a spatial spring (pill exit, panel travel), not a
 * tactile press — the tactile targets above carry StiffnessMedium. */
fun <T> springSpatial(): FiniteAnimationSpec<T> =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/** Emphasized bounds: the spec's EmphasizedBounds, for container transforms. */
fun <T> springBounds(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/**
 * The SINGLE press token (spec item 4). Canonical tactile values:
 * Spring.DampingRatioMediumBouncy + Spring.StiffnessMedium, adopted
 * into the token in the design-system sprint (was 0.55/Medium — the
 * 0.05 damping difference is imperceptible, the canonical value is
 * greppable). Every press-scale interaction (tiles, steppers, media
 * transports) uses this and nothing else.
 */
fun <T> pressBounce(): FiniteAnimationSpec<T> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

/**
 * Overhaul-spec HavenMotion mapping (2026-10-29 architectural round):
 * the overhaul spec's three values map onto the single token set above
 * — they are NOT duplicated as new tokens:
 * SpatialSpring (0.82/MediumLow) -> springBounds (0.8/MediumLow) for the
 * modal rect interpolation (0.02 damping apart, imperceptible);
 * PillSpring (0.78/MediumLow) -> springSpatial (exact) for the pill exit;
 * InteractiveBounce (0.52/Medium) -> pressBounce (0.5/Medium), the one
 * press token. No new tokens were added for this round.
 *
 * Production-upgrade mapping (2026-10-29 verdict round): the new
 * draft's PillSlideSpring (0.85/MediumLow) -> springSpatial as well —
 * one pill-motion token for modal-exit and scroll-away alike, so the
 * pill never animates on two different springs at once.
 *
 * Design-system sprint mapping (canonical springs, adopted INTO the
 * tokens, never ad-hoc at call sites): tactile targets use
 * DampingRatioMediumBouncy + StiffnessMedium (pressBounce) and
 * DampingRatioLowBouncy + StiffnessMedium (springSnappy). Spatial and
 * container springs keep their tuned values — havenSpring (380/0.62),
 * springBounds (0.8/MediumLow), springMorph (0.72/320), sheet rise
 * (500/0.85) — because they are not tactile targets: the sheet rise
 * is deliberately almost overshoot-free (matched to Koda's sheet),
 * and the house spatial feel is capped ("nothing crazy"). Adopting
 * bouncy values into the spatial springs would break both.
 */

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

/**
 * Haven's shape scale, on the Material 3 token scale (ExtraSmall through
 * ExtraLarge): 4 / 8 / 12 / 16 / 28dp. Rectangular containers use these
 * tokens via MaterialTheme.shapes — never a hardcoded dp corner.
 * The expressive polygon system (Shapes.kt) is a separate layer above
 * tokens and is intentionally NOT part of this scale: polygons are
 * never replaced by rounded rectangles.
 *
 * Three-tier mapping, per the M3 token spec: reference values (the dp
 * numbers below) -> system tokens (MaterialTheme.shapes) -> component
 * tokens (DesignTokens.kt, which point at ColorScheme roles). Code
 * reads the system token, never the raw dp.
 */
val HavenShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Top-only variant of the large token, for fills whose bottom edge is
 * the element's own straight edge (the garage door fill). Derived from
 * HavenShapes.large so it tracks the token — never a hardcoded dp.
 * (Shapes fields are typed as Shape, so the top corners cannot be
 * read off MaterialTheme.shapes.large directly.)
 */
val LargeTopShape: Shape = (HavenShapes.large as RoundedCornerShape).copy(
    bottomStart = CornerSize(0.dp),
    bottomEnd = CornerSize(0.dp),
)

/**
 * The one corner above the token scale, kept as a single named value
 * rather than scattered dp: floating elements (pop-up panels, the pill
 * bar, the weather hero inside a pop-up) rest at 32dp, and the pop-up
 * corner morph animates to exactly this value (morphPopupCorner).
 * M3's Shapes scale caps at ExtraLarge (28dp); this endpoint
 * intentionally exceeds it, so it lives here — not as a hardcoded dp
 * at each call site.
 */
val FloatingCorner = RoundedCornerShape(32.dp)

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
        shapes = HavenShapes,
        content = content,
    )
}
