package com.garrietta.haven.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Color styles: one choice, in Settings, Appearance, that recolors the
 * whole app in light and dark. A style is a scheme (surfaces, text,
 * accents), the bar and its selected capsule, and, for Home Assistant's
 * style, the colors of the home's states too. Shapes, type and motion do
 * not change with it, and neither does what a state means: heating is
 * the heating family in every style, only its hue can differ.
 *
 * Everything here is made of ramps from Palette.kt; nothing is a color
 * literal.
 */

/** The styles, in the order Appearance shows them. [key] is stored: never rename one. */
enum class ColorStyle(val key: String, val label: String, val blurb: String) {
    HAVEN("haven", "Haven", "Leaf and lime on warm paper"),
    HOME_ASSISTANT("home_assistant", "Home Assistant", "Its blue, its amber for what is on, gray cards"),
    WALLPAPER("wallpaper", "Wallpaper", "Accents from your phone's wallpaper"),
    TIDE("tide", "Tide", "Deep sea blue on pale water"),
    ORCHID("orchid", "Orchid", "Violet with a soft lilac"),
    GRAPHITE("graphite", "Graphite", "Gray; only what is on has color");

    companion object {
        fun fromKey(key: String?): ColorStyle = entries.firstOrNull { it.key == key } ?: HAVEN
    }
}

/**
 * The style the app is drawn in right now. [HavenTheme] sets it before
 * its content composes (and recomposes everything when it changes); the
 * widgets set it from the stored choice before they draw. The color
 * functions that are not composables (state families, the bar) read it,
 * so their many call sites need no style passed in.
 */
object ActiveStyle {
    @Volatile var current: ColorStyle = ColorStyle.HAVEN

    /** The wallpaper style's capsule and its ink, taken from the dynamic scheme when it is built. */
    @Volatile internal var wallpaperCapsule: Pair<Color, Color>? = null
}

/** The bar and its selected capsule for a style: the same by day and by night. */
data class StyleChrome(
    val bar: Color,
    val barOnBlack: Color,
    val capsule: Color,
    val onCapsule: Color,
    val idleGlyph: Color,
)

fun styleChrome(style: ColorStyle): StyleChrome = when (style) {
    ColorStyle.HAVEN -> StyleChrome(Palette.Ink, Palette.Moss.t15, Palette.Lime, Palette.Leaf.t10, Palette.Moss.t87)
    ColorStyle.HOME_ASSISTANT -> StyleChrome(Palette.Graphite.t12, Palette.Graphite.t17, Palette.HaBlue.t80, Palette.HaBlue.t10, Palette.Graphite.t80)
    ColorStyle.WALLPAPER -> {
        val (capsule, ink) = ActiveStyle.wallpaperCapsule ?: (Palette.Lime to Palette.Leaf.t10)
        StyleChrome(Palette.Ink, Palette.Moss.t15, capsule, ink, Palette.Moss.t87)
    }
    ColorStyle.TIDE -> StyleChrome(Palette.TideMist.t10, Palette.TideMist.t17, Palette.Tide.t85, Palette.Tide.t10, Palette.TideMist.t80)
    ColorStyle.ORCHID -> StyleChrome(Palette.OrchidMist.t10, Palette.OrchidMist.t17, Palette.Orchid.t85, Palette.Orchid.t10, Palette.OrchidMist.t80)
    ColorStyle.GRAPHITE -> StyleChrome(Palette.Graphite.t10, Palette.Graphite.t17, Palette.Graphite.t95, Palette.Graphite.t10, Palette.Graphite.t70)
}

/**
 * A style's scheme. Haven's is its own (cream paper, true black by
 * night). The others are built from a primary and a neutral on the same
 * tone recipe; Tide, Orchid and Graphite keep Haven's true black at
 * night, and Home Assistant's keeps its own near-black and gray cards.
 * The wallpaper style is built from the wallpaper in [HavenTheme].
 */
fun styleColorScheme(style: ColorStyle, dark: Boolean): ColorScheme = when (style) {
    ColorStyle.HAVEN, ColorStyle.WALLPAPER -> if (dark) havenDarkColorScheme() else havenLightColorScheme()
    ColorStyle.HOME_ASSISTANT -> builtScheme(dark, Palette.HaBlue, Palette.HaOrange, Palette.HaCyan, Palette.Graphite, trueBlack = false)
    ColorStyle.TIDE -> builtScheme(dark, Palette.Tide, Palette.TideMist, Palette.Rain, Palette.TideMist, trueBlack = true)
    ColorStyle.ORCHID -> builtScheme(dark, Palette.Orchid, Palette.OrchidMist, Palette.Tune, Palette.OrchidMist, trueBlack = true)
    ColorStyle.GRAPHITE -> builtScheme(dark, Palette.Graphite, Palette.Graphite, Palette.Graphite, Palette.Graphite, trueBlack = true)
}

/**
 * The tone recipe every built style shares: primary t40 on t90 by day,
 * t80 on t30 by night; the page at the neutral's t98 (t96 for Home
 * Assistant, whose cards are white) and the containers stepping down
 * from it, steeper than stock Material so tiles sit on the page with no
 * outline. By night, true black or Home Assistant's near black.
 */
private fun builtScheme(
    dark: Boolean,
    primary: TonalRamp,
    secondary: TonalRamp,
    tertiary: TonalRamp,
    neutral: TonalRamp,
    trueBlack: Boolean,
): ColorScheme = if (!dark) {
    val whiteCards = !trueBlack
    lightColorScheme(
        primary = primary.t40, onPrimary = primary.t100,
        primaryContainer = primary.t90, onPrimaryContainer = primary.t10, inversePrimary = primary.t80,
        secondary = secondary.t40, onSecondary = secondary.t100,
        secondaryContainer = secondary.t90, onSecondaryContainer = secondary.t10,
        tertiary = tertiary.t40, onTertiary = tertiary.t100,
        tertiaryContainer = tertiary.t90, onTertiaryContainer = tertiary.t10,
        error = Palette.Alarm.t40, onError = Palette.Alarm.t100,
        errorContainer = Palette.Alarm.t90, onErrorContainer = Palette.Alarm.t10,
        background = if (whiteCards) neutral.t96 else neutral.t98, onBackground = neutral.t10,
        surface = if (whiteCards) neutral.t96 else neutral.t98, onSurface = neutral.t10,
        surfaceVariant = neutral.t90, onSurfaceVariant = neutral.t30,
        surfaceTint = primary.t40,
        inverseSurface = neutral.t20, inverseOnSurface = neutral.t95,
        surfaceDim = neutral.t87, surfaceBright = neutral.t98,
        surfaceContainerLowest = neutral.t100,
        surfaceContainerLow = if (whiteCards) neutral.t100 else neutral.t95,
        surfaceContainer = if (whiteCards) neutral.t100 else neutral.t94,
        surfaceContainerHigh = if (whiteCards) neutral.t94 else neutral.t90,
        surfaceContainerHighest = if (whiteCards) neutral.t90 else neutral.t87,
        outline = neutral.t50, outlineVariant = neutral.t80,
        scrim = neutral.t0,
    )
} else {
    val ground = if (trueBlack) neutral.t0 else neutral.t6
    darkColorScheme(
        primary = primary.t80, onPrimary = primary.t20,
        primaryContainer = primary.t30, onPrimaryContainer = primary.t90, inversePrimary = primary.t40,
        secondary = secondary.t80, onSecondary = secondary.t20,
        secondaryContainer = secondary.t30, onSecondaryContainer = secondary.t90,
        tertiary = tertiary.t80, onTertiary = tertiary.t20,
        tertiaryContainer = tertiary.t30, onTertiaryContainer = tertiary.t90,
        error = Palette.Alarm.t80, onError = Palette.Alarm.t20,
        errorContainer = Palette.Alarm.t30, onErrorContainer = Palette.Alarm.t90,
        background = ground, onBackground = neutral.t95,
        surface = ground, onSurface = neutral.t95,
        surfaceVariant = neutral.t17, onSurfaceVariant = neutral.t80,
        surfaceTint = primary.t80,
        inverseSurface = neutral.t92, inverseOnSurface = neutral.t17,
        surfaceDim = ground, surfaceBright = neutral.t22,
        surfaceContainerLowest = ground,
        surfaceContainerLow = if (trueBlack) neutral.t6 else neutral.t8,
        surfaceContainer = if (trueBlack) neutral.t10 else neutral.t12,
        surfaceContainerHigh = if (trueBlack) neutral.t15 else neutral.t17,
        surfaceContainerHighest = if (trueBlack) neutral.t20 else neutral.t22,
        outline = neutral.t40, outlineVariant = neutral.t22,
        scrim = neutral.t0,
    )
}

/**
 * The ramp a state family is drawn from in [style]. Every style but Home
 * Assistant's keeps Haven's families; Home Assistant's uses its own
 * frontend's state colors: amber for lights and switches that are on,
 * orange heating, blue cooling, green locked, red open, light blue media.
 */
fun familyRamp(style: ColorStyle, family: StateFamily): TonalRamp =
    if (style != ColorStyle.HOME_ASSISTANT) {
        if (family == StateFamily.Quiet) styleNeutral(style) else havenFamilyRamp(family)
    } else when (family) {
        StateFamily.On, StateFamily.Light, StateFamily.Power -> Palette.HaAmber
        StateFamily.Heat, StateFamily.Caution -> Palette.HaOrange
        StateFamily.Cool -> Palette.HaCool
        StateFamily.Secure -> Palette.HaGreen
        StateFamily.Alert -> Palette.HaRed
        StateFamily.Media -> Palette.HaBlue
        StateFamily.Water -> Palette.HaCyan
        StateFamily.Quiet -> Palette.Graphite
    }

/**
 * Home Assistant's way of showing a state: the card stays the plain
 * card, and only the icon's round badge takes the state's color, a soft
 * tint behind the icon in the full color. Haven's styles color the
 * whole tile instead.
 */
internal fun homeAssistantFamily(family: StateFamily, isDark: Boolean): FamilyColors {
    val ramp = familyRamp(ColorStyle.HOME_ASSISTANT, family)
    val gray = Palette.Graphite
    return if (family == StateFamily.Quiet) {
        if (isDark) FamilyColors(gray.t12, gray.t90, gray.t22, gray.t70) else FamilyColors(gray.t100, gray.t10, gray.t92, gray.t40)
    } else {
        if (isDark) FamilyColors(gray.t12, gray.t90, ramp.t25, ramp.t80) else FamilyColors(gray.t100, gray.t10, ramp.t92, ramp.t50)
    }
}

/** The neutral a style's resting things are drawn in: Haven's sage-tinted gray, or the style's own. */
fun styleNeutral(style: ColorStyle): TonalRamp = when (style) {
    ColorStyle.HAVEN, ColorStyle.WALLPAPER -> Palette.Moss
    ColorStyle.HOME_ASSISTANT, ColorStyle.GRAPHITE -> Palette.Graphite
    ColorStyle.TIDE -> Palette.TideMist
    ColorStyle.ORCHID -> Palette.OrchidMist
}
