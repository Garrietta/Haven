package com.garrietta.haven.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Haven's color roles.
 *
 * Three layers, each reading only the one below it:
 *
 *   Palette.kt   reference   tonal ramps, generated (tools/palette.py)
 *   this file    roles       the two schemes, and the state families
 *   data/, ui/   decisions   which role an entity in a given state gets
 *
 * The idea the whole identity hangs on: a calm canvas and vivid state.
 * Surfaces are warm paper by day and true black by night, tinted just
 * enough toward sage to feel like one material. Color is spent on what
 * the home is doing, so a lit lamp or an open door is the brightest
 * thing on the screen.
 */

/**
 * The light scheme: cream paper, sage containers, leaf-green ink for
 * actions and the lime for anything selected. Fixed, so Haven looks like
 * Haven on every phone; wallpaper color is an opt-in (see [HavenTheme]).
 */
fun havenLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Palette.Leaf.t40,
    onPrimary = Color.White,
    primaryContainer = Palette.Leaf.t90,
    onPrimaryContainer = Palette.Leaf.t10,
    inversePrimary = Palette.Leaf.t80,
    secondary = Palette.Sage.t40,
    onSecondary = Color.White,
    secondaryContainer = Palette.Sage.t90,
    onSecondaryContainer = Palette.Sage.t10,
    tertiary = Palette.Dew.t40,
    onTertiary = Color.White,
    tertiaryContainer = Palette.Dew.t90,
    onTertiaryContainer = Palette.Dew.t10,
    error = Palette.Alarm.t40,
    onError = Color.White,
    errorContainer = Palette.Alarm.t90,
    onErrorContainer = Palette.Alarm.t10,
    background = Palette.Cream.t98,
    onBackground = Palette.Cream.t10,
    surface = Palette.Cream.t98,
    onSurface = Palette.Cream.t10,
    surfaceVariant = Palette.Moss.t90,
    onSurfaceVariant = Palette.Moss.t30,
    surfaceTint = Palette.Leaf.t40,
    inverseSurface = Palette.Cream.t20,
    inverseOnSurface = Palette.Cream.t95,
    surfaceDim = Palette.Moss.t87,
    surfaceBright = Palette.Cream.t98,
    // The container steps run a little steeper than stock Material 3
    // (which stops at tone 90): chips and resting tiles sit straight on
    // the page with no outline, so they need the extra separation.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Palette.Moss.t95,
    surfaceContainer = Palette.Moss.t94,
    surfaceContainerHigh = Palette.Moss.t90,
    surfaceContainerHighest = Palette.Moss.t87,
    outline = Palette.Moss.t50,
    outlineVariant = Palette.Moss.t80,
    scrim = Color.Black,
)

/**
 * The dark scheme: true black, because the phone this is built for is
 * OLED and a home app gets opened in dark rooms. Containers climb out of
 * black in sage-tinted steps, and the primary is the lime itself, so the
 * things you can press glow.
 */
fun havenDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Palette.Lime,
    onPrimary = Palette.Leaf.t10,
    primaryContainer = Palette.Leaf.t30,
    onPrimaryContainer = Palette.Leaf.t92,
    inversePrimary = Palette.Leaf.t40,
    secondary = Palette.Sage.t80,
    onSecondary = Palette.Sage.t20,
    secondaryContainer = Palette.Sage.t30,
    onSecondaryContainer = Palette.Sage.t90,
    tertiary = Palette.Dew.t80,
    onTertiary = Palette.Dew.t20,
    tertiaryContainer = Palette.Dew.t30,
    onTertiaryContainer = Palette.Dew.t90,
    error = Palette.Alarm.t80,
    onError = Palette.Alarm.t20,
    errorContainer = Palette.Alarm.t30,
    onErrorContainer = Palette.Alarm.t90,
    background = Color.Black,
    onBackground = Palette.Cream.t95,
    surface = Color.Black,
    onSurface = Palette.Cream.t95,
    surfaceVariant = Palette.Moss.t17,
    onSurfaceVariant = Palette.Moss.t80,
    surfaceTint = Palette.Lime,
    inverseSurface = Palette.Cream.t92,
    inverseOnSurface = Palette.Cream.t17,
    surfaceDim = Color.Black,
    surfaceBright = Palette.Moss.t22,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Palette.Moss.t6,
    surfaceContainer = Palette.Moss.t10,
    surfaceContainerHigh = Palette.Moss.t15,
    surfaceContainerHighest = Palette.Moss.t20,
    outline = Palette.Moss.t40,
    outlineVariant = Palette.Moss.t22,
    scrim = Color.Black,
)

/**
 * The dark container steps on their own, for the wallpaper-color option:
 * a dynamic dark scheme keeps Haven's true-black ground and these steps,
 * and takes only its accents from the wallpaper.
 */
fun ColorScheme.withHavenDarkSurfaces(): ColorScheme {
    val haven = havenDarkColorScheme()
    return copy(
        background = haven.background,
        surface = haven.surface,
        surfaceDim = haven.surfaceDim,
        surfaceBright = haven.surfaceBright,
        surfaceContainerLowest = haven.surfaceContainerLowest,
        surfaceContainerLow = haven.surfaceContainerLow,
        surfaceContainer = haven.surfaceContainer,
        surfaceContainerHigh = haven.surfaceContainerHigh,
        surfaceContainerHighest = haven.surfaceContainerHighest,
    )
}

// ---------------------------------------------------------------------------
// State families
// ---------------------------------------------------------------------------

/**
 * What the home is doing, as a color family. A family is a meaning, not
 * a device type: heating is [Heat] whether it is a thermostat tile, a
 * status row or a chart line, and a temperature reading is [Heat] too.
 * The same state is the same color everywhere in the app.
 */
enum class StateFamily {
    /** On, running, live. Lights, switches, a vacuum at work. */
    On,
    /** Heating, and temperature readings. */
    Heat,
    /** Cooling, and the weather. */
    Cool,
    /** Locked, closed, disarmed: the calm, safe states. */
    Secure,
    /** Unlocked, open, a fault: look at this. */
    Alert,
    /** Media. */
    Media,
    /** Power and energy. */
    Power,
    /** Motion and other passing cautions. */
    Caution,
    /** Humidity and water. */
    Water,
    /** Off, idle, unavailable. */
    Quiet,
}

/**
 * A family resolved for one theme.
 *
 * [container] is the quiet fill a tile or row wears, [onContainer] the
 * text on it. [accent] is the strong tone for the badge behind the
 * device's icon and for a switch track, [onAccent] the icon on it.
 *
 * The tone recipe is the same for every colored family: by day a
 * pastel container (t90) under a deep badge (t25) whose icon is the
 * container color again; by night a deep container (t25) under a bright
 * badge (t85) with dark ink (t12). Because the recipe is in tones, the
 * contrast is the same in every hue: 8.5:1 or better for every pair.
 */
data class FamilyColors(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
    val onAccent: Color,
)

private fun TonalRamp.family(isDark: Boolean): FamilyColors =
    if (isDark) FamilyColors(container = t25, onContainer = t92, accent = t85, onAccent = t12)
    else FamilyColors(container = t90, onContainer = t10, accent = t25, onAccent = t90)

fun StateFamily.ramp(): TonalRamp = when (this) {
    StateFamily.On -> Palette.Leaf
    StateFamily.Heat -> Palette.Ember
    StateFamily.Cool -> Palette.Frost
    StateFamily.Secure -> Palette.Guard
    StateFamily.Alert -> Palette.Alarm
    StateFamily.Media -> Palette.Tune
    StateFamily.Power -> Palette.Volt
    StateFamily.Caution -> Palette.Honey
    StateFamily.Water -> Palette.Rain
    StateFamily.Quiet -> Palette.Moss
}

fun StateFamily.colors(isDark: Boolean): FamilyColors = when (this) {
    // Quiet is not a color, it is the absence of one, so it does not
    // take the pastel recipe: it stays close to the page. By night its
    // icon keeps a trace of green, so a resting device reads as asleep
    // rather than dead.
    StateFamily.Quiet ->
        if (isDark) FamilyColors(
            container = Palette.Moss.t12,
            onContainer = Palette.Moss.t80,
            accent = Palette.Moss.t20,
            onAccent = Palette.Leaf.t80,
        ) else FamilyColors(
            container = Palette.Moss.t92,
            onContainer = Palette.Moss.t30,
            accent = Palette.Moss.t35,
            onAccent = Palette.Moss.t92,
        )
    else -> ramp().family(isDark)
}

/**
 * The single line color a family draws charts and small indicators in:
 * a mid tone that reads on the page in either theme.
 */
fun StateFamily.signal(isDark: Boolean): Color = ramp().let { if (isDark) it.t80 else it.t40 }

// ---------------------------------------------------------------------------
// Chrome that does not change with the theme
// ---------------------------------------------------------------------------

/**
 * The navigation bar is the one piece of Haven that looks the same by
 * day and by night: a near-black green bar carrying a lime capsule. It
 * is the brand in miniature, and the same two colors are the logo.
 */
object HavenChrome {
    val bar = Palette.Ink
    /** The bar on true black, where the ink would vanish: one step up. */
    val barOnBlack = Palette.Moss.t15
    val capsule = Palette.Lime
    val onCapsule = Palette.Leaf.t10
    val idleGlyph = Palette.Moss.t87
}

/** The dot that says "something needs attention". Red in both themes. */
fun attentionDot(isDark: Boolean): Color = if (isDark) Palette.Alarm.t70 else Palette.Alarm.t50
