package com.garrietta.haven.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

/**
 * Follow-up B (2026-11-03): Koda's bar spring, pinned as a local
 * token. Koda reads MaterialTheme.motionScheme.fastSpatialSpec()
 * with an expressive scheme (its theme sets
 * motionScheme = MotionScheme.expressive() on material3
 * 1.5.0-alpha29, where MotionScheme is public). Haven stays on
 * material3 1.4.0 STABLE, where MotionScheme is internal, so the
 * theme cannot pin the scheme — the expressive fast spatial
 * numbers are pinned here instead (ExpressiveMotionTokens:
 * damping 0.6, stiffness 800), giving identical motion with no
 * dependency move. Bar ports use this token, never a copy.
 */
fun <T> kodaBarSpec(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.6f, stiffness = 800f)

/** Emphasized bounds: the spec's EmphasizedBounds, for container transforms. */
fun <T> springBounds(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/**
 * Feedback round 7: the settle spring, pinned from Koda's
 * PLAYER_CONTAINER_SPRING as-is (damping 0.95, stiffness 350) — near-
 * critically damped, deliberately NO visible bounce. Used for every
 * popup dismiss (drag-release settle and programmatic dismiss alike)
 * and for the spring-back to open after a cancelled drag. Open
 * animations stay on springBounds(); this token is the return flight
 * only. The user's "bounce back down" means the spring return into
 * the tile, not literal overshoot — no bounce token is used here.
 */
fun <T> settleSpring(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.95f, stiffness = 350f)

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

/**
 * Feedback round 16, item 4b: the popup morph spring. Same numbers
 * as [sheetSpring] (500/0.85, matched to Koda's recorded player
 * expansion) but its own token, so popup open speed tunes
 * independently of sheet rise. The five popup open effects
 * (sheet/detail/panel-detail/popout/camera-dialog) run on this;
 * dismiss keeps [settleSpring] (Koda's player settle).
 */
fun <T> popupMorphSpring(): FiniteAnimationSpec<T> = spring(stiffness = 500f, dampingRatio = 0.85f)

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

/**
 * Stage B (redesign): the PINNED Haven light scheme — the permanent
 * light-theme identity, identical on every device regardless of
 * wallpaper. Soft warm off-white background, sage/muted-green
 * surfaces, charcoal primary text, bright lime accents used
 * selectively (selected pill, active toggles), pastel-friendly
 * containers with soft card/background separation. This replaces
 * the dynamic-M3 light scheme (see [HavenTheme]): the fixed palette
 * IS the design system, and device-state colors stay automatic from
 * real HA states via the semantic tokens (Stage E), never from the
 * wallpaper. Pure (no Android calls), unit-tested for contrast.
 */
fun havenLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF3E6B2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC1E88A),
    onPrimaryContainer = Color(0xFF1E2A12),
    secondary = Color(0xFF5A6B52),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE5D0),
    onSecondaryContainer = Color(0xFF25301F),
    // Stage B fix (unit-test caught): the first pinned tertiary
    // (0xFF6E7F5E) gave white-on-tertiary only 4.32:1, below the 4.5
    // floor. This slightly deeper sage clears 4.72:1 with the same
    // character; onTertiary stays White like primary/secondary.
    tertiary = Color(0xFF677958),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4E8D8),
    onTertiaryContainer = Color(0xFF232B1E),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAF7F0),
    onBackground = Color(0xFF1E1E1A),
    surface = Color(0xFFF1EFE4),
    onSurface = Color(0xFF1E1E1A),
    surfaceVariant = Color(0xFFE9EDE0),
    onSurfaceVariant = Color(0xFF5B5A51),
    surfaceTint = Color(0xFF3E6B2F),
    inverseSurface = Color(0xFF2E2F29),
    inverseOnSurface = Color(0xFFF4F1E6),
    inversePrimary = Color(0xFFAED6A0),
    surfaceDim = Color(0xFFE7E4D6),
    surfaceBright = Color(0xFFFFFBF2),
    surfaceContainerLowest = Color(0xFFFFFBF2),
    surfaceContainerLow = Color(0xFFF4F1E6),
    surfaceContainer = Color(0xFFE9EDE0),
    surfaceContainerHigh = Color(0xFFDDE5D0),
    surfaceContainerHighest = Color(0xFFCBD8BC),
    outline = Color(0xFFC6C4B4),
    outlineVariant = Color(0xFFE1DFD0),
    scrim = Color.Black,
)

/**
 * Stage B (redesign): the PINNED Haven dark scheme — Round 8's
 * true-black container steps with FIXED near-white on-colors
 * (Round 8 kept on-colors dynamic; the fixed palette now pins
 * them too, so dark is identical regardless of wallpaper). The
 * dark onSurfaceVariant is pinned to a warm light grey that clears
 * 4.5:1 against every dark container step — this is the token-level
 * floor for app-wide non-weather secondary text (Stage B item 2):
 * every onSurfaceVariant use in dark passes without per-site edits,
 * and [legibleSecondaryOn] still floors edge cases where text sits
 * on an arbitrary container. Pure, unit-tested.
 */
fun havenDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFAED6A0),
    onPrimary = Color(0xFF1E2A12),
    primaryContainer = Color(0xFF2E4A2E),
    onPrimaryContainer = Color(0xFFD6EBCC),
    secondary = Color(0xFF9DB08F),
    onSecondary = Color(0xFF1E2A12),
    secondaryContainer = Color(0xFF2A352A),
    onSecondaryContainer = Color(0xFFD6E2CC),
    tertiary = Color(0xFF9FB4AD),
    onTertiary = Color(0xFF1E2A12),
    tertiaryContainer = Color(0xFF25302E),
    onTertiaryContainer = Color(0xFFD2E0DC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color(0xFFF2F2EF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF2F2EF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFC9C9C4),
    surfaceTint = Color(0xFFAED6A0),
    inverseSurface = Color(0xFFFAF7F0),
    inverseOnSurface = Color(0xFF1E1E1A),
    inversePrimary = Color(0xFF3E6B2F),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2B2B2B),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    surfaceContainerHighest = Color(0xFF383838),
    outline = Color(0xFF3F3F3A),
    outlineVariant = Color(0xFF262622),
    scrim = Color.Black,
)

/**
 * Pure theme-mode resolution, so it is unit-testable without a
 * composition: "light" forces light, "dark" forces dark, anything
 * else (including "system") follows the system. Pure.
 */
fun resolveHavenIsDark(themeMode: String, isSystemDark: Boolean): Boolean =
    when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

@Composable
fun HavenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Stage B: Android dynamic (wallpaper) color. OPTIONAL and
     * DISABLED BY DEFAULT — the fixed Haven palettes above are the
     * identity, and dynamic color must never override them
     * unexpectedly. When on, light/dark resolve to the dynamic
     * schemes (dark keeps Round 8's pinned container steps);
     * when off (the default), both themes are fully pinned.
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = if (darkTheme) {
        if (dynamicColor) {
            val base = try {
                dynamicDarkColorScheme(context)
            } catch (e: Exception) {
                darkColorScheme()
            }
            // Round 8 dark pass, preserved under dynamic color:
            // true-black background with stepped surfaceContainers.
            // Only the container steps are pinned — every on-color
            // stays dynamic here, and near-white on-colors keep
            // their contrast against black.
            base.copy(
                background = Color.Black,
                surface = Color(0xFF121212),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF161616),
                surfaceContainer = Color(0xFF1E1E1E),
                surfaceContainerHigh = Color(0xFF2B2B2B),
                surfaceContainerHighest = Color(0xFF383838),
            )
        } else {
            havenDarkColorScheme()
        }
    } else {
        if (dynamicColor) {
            try {
                dynamicLightColorScheme(context)
            } catch (e: Exception) {
                lightColorScheme()
            }
        } else {
            havenLightColorScheme()
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = HavenShapes,
        content = content,
    )
}
