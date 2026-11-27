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
    /** On, running, live. Switches, fans, a vacuum at work. */
    On,
    /** A lamp that is lit. */
    Light,
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

/** The ramp this family is drawn from in the active color style. */
fun StateFamily.ramp(): TonalRamp = familyRamp(ActiveStyle.current, this)

/** Haven's own family ramps, which every style but Home Assistant's keeps. */
internal fun havenFamilyRamp(family: StateFamily): TonalRamp = when (family) {
    StateFamily.On -> Palette.Leaf
    StateFamily.Light -> Palette.Volt
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

/**
 * The ground a room card wears: its family's color, only just (t95 by
 * day, t17 by night). The room still says what it is doing, but the card
 * does not shout it; the switches on it, and a lit lamp's glow, carry
 * the color. Quiet rooms keep their own resting tone.
 */
fun StateFamily.roomGround(isDark: Boolean): Color =
    if (this == StateFamily.Quiet || ActiveStyle.current == ColorStyle.HOME_ASSISTANT) colors(isDark).container
    else ramp().let { if (isDark) it.t17 else it.t95 }

/**
 * The hot centre of a moment's glow ([grainGlow]): a lit lamp's light,
 * near white by day, and for heating and cooling a deeper tone of the
 * family, an ember or a frost, rather than a white spot.
 */
fun StateFamily.glow(isDark: Boolean): Color = ramp().let {
    when {
        isDark -> it.t40
        this == StateFamily.Light -> it.t99
        else -> it.t80
    }
}

fun StateFamily.colors(isDark: Boolean): FamilyColors = familyColorsIn(ActiveStyle.current, this, isDark)

/** A family's colors in [style], whichever style is active: for previews of a style not yet chosen. */
fun familyColorsIn(style: ColorStyle, family: StateFamily, isDark: Boolean): FamilyColors =
    if (style == ColorStyle.HOME_ASSISTANT) homeAssistantFamily(family, isDark) else family.havenFamilyColors(isDark, style)

private fun StateFamily.havenFamilyColors(isDark: Boolean, style: ColorStyle): FamilyColors = when (this) {
    // Quiet is not a color, it is the absence of one, so it does not
    // take the pastel recipe: it stays close to the page, in the style's
    // own neutral. In Haven's style its icon keeps a trace of green by
    // night, so a resting device reads as asleep rather than dead.
    StateFamily.Quiet -> {
        val n = styleNeutral(style)
        if (isDark) FamilyColors(
            container = n.t12,
            onContainer = n.t80,
            accent = n.t20,
            onAccent = if (n === Palette.Moss) Palette.Leaf.t80 else n.t80,
        ) else FamilyColors(
            container = n.t92,
            onContainer = n.t30,
            accent = n.t35,
            onAccent = n.t92,
        )
    }
    // A lit lamp is a light source, so by day its badge is the bright
    // tone, not the deep one: it is the one thing drawn glowing. By
    // night every badge is already bright.
    StateFamily.Light ->
        if (isDark) havenFamilyRamp(this).family(true)
        else Palette.Volt.let {
            FamilyColors(container = it.t90, onContainer = it.t10, accent = it.t80, onAccent = it.t10)
        }
    else -> havenFamilyRamp(this).family(isDark)
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
 * day and by night: a near-black bar carrying the selected capsule. In
 * Haven's own style it is the ink and the lime, the brand in miniature;
 * the other color styles bring their own (ColorStyles.kt).
 */
object HavenChrome {
    val bar: Color get() = styleChrome(ActiveStyle.current).bar
    /** The bar on true black, where the ink would vanish: one step up. */
    val barOnBlack: Color get() = styleChrome(ActiveStyle.current).barOnBlack
    /** The selected capsule: lime in Haven's style, the style's own bright accent in the others. */
    val capsule: Color get() = styleChrome(ActiveStyle.current).capsule
    val onCapsule: Color get() = styleChrome(ActiveStyle.current).onCapsule
    val idleGlyph: Color get() = styleChrome(ActiveStyle.current).idleGlyph
}

/** The dot that says "something needs attention". Red in both themes. */
fun attentionDot(isDark: Boolean): Color = if (isDark) Palette.Alarm.t70 else Palette.Alarm.t50
