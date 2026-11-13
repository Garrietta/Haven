package com.garrietta.haven.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.garrietta.haven.ui.theme.Corner
import com.garrietta.haven.ui.theme.HavenChrome
import com.garrietta.haven.ui.theme.Palette
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.StateShape
import com.garrietta.haven.ui.theme.leafCard
import com.garrietta.haven.ui.theme.signal

/*
 * Component tokens: the decisions that turn an entity and its state into
 * a color, a shape or a size. Everything here is a pure function (no
 * composition, no Android), so each rule is unit-tested on the JVM, and
 * every color it returns is a Palette tone or a scheme role. The roles
 * themselves live in ui/theme/Color.kt.
 */

/**
 * The default card outline for a grid position: Haven's leaf card, its
 * tight corner turning with the index. A shape the user picked for an
 * entity (see [cardCornerOption]) wins over this everywhere.
 */
fun generalCardShape(index: Int): RoundedCornerShape = leafCard(index)

/**
 * The eight outlines the card-shape picker offers, by the number stored
 * in its "corners_N" key. 0-3 are the leaf card's four rotations; 4-7
 * are the sharp, soft, pillow and diagonal alternatives. The numbers are
 * persisted, so their meaning is frozen.
 */
fun cardCornerOption(option: Int): RoundedCornerShape = when (Math.floorMod(option, 8)) {
    0, 1, 2, 3 -> leafCard(option)
    4 -> RoundedCornerShape(Corner.xs)
    5 -> RoundedCornerShape(Corner.xl)
    6 -> RoundedCornerShape(36.dp)
    else -> RoundedCornerShape(topStart = Corner.xl, topEnd = Corner.sm, bottomEnd = Corner.sm, bottomStart = Corner.xl)
}

// ---------------------------------------------------------------------------
// Light color temperature// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
// Light color temperature (spec item 2: secondary -> tertiary by kelvin)
// ---------------------------------------------------------------------------

/** 2000K (candle warm) = 0, 6500K (daylight) = 1; unknown kelvin = neutral 0.5. */
fun lightTempBlendFraction(kelvin: Int?): Float =
    if (kelvin == null) 0.5f else (kelvin - 2000).coerceIn(0, 4500) / 4500f

/** A glyph tint that follows the bulb's color temperature, warm amber to cool white. */
fun lightColorTempTint(kelvin: Int?): Color =
    lerp(Palette.Honey.t80, Palette.Frost.t90, lightTempBlendFraction(kelvin))

/** The container a lit light lives in, blended across secondary -> tertiary by kelvin. */
fun lightContainerColor(scheme: ColorScheme, kelvin: Int?, isOn: Boolean): Color =
    if (!isOn) scheme.surfaceContainerHighest
    else lerp(scheme.secondaryContainer, scheme.tertiaryContainer, lightTempBlendFraction(kelvin))

// ---------------------------------------------------------------------------
// Climate tokens (spec item 2)
// ---------------------------------------------------------------------------

/**
 * Heating container: errorContainer in single-heat mode, transitioning
 * smoothly (via animateColorAsState at the call site) to tertiaryContainer
 * when the thermostat is in dual setpoint (heat_cool) band mode.
 */
fun heatSetpointContainer(scheme: ColorScheme, dualBand: Boolean): Color =
    if (dualBand) scheme.tertiaryContainer else scheme.errorContainer

fun heatSetpointOnContainer(scheme: ColorScheme, dualBand: Boolean): Color =
    if (dualBand) scheme.onTertiaryContainer else scheme.onErrorContainer

/** Cooling container: primaryContainer. */
fun coolSetpointContainer(scheme: ColorScheme): Color = scheme.primaryContainer

fun coolSetpointOnContainer(scheme: ColorScheme): Color = scheme.onPrimaryContainer

/** Idle/off container: surfaceContainerHighest. */
fun climateIdleContainer(scheme: ColorScheme): Color = scheme.surfaceContainerHighest

// ---------------------------------------------------------------------------
// Switch / binary-sensor tokens (spec item 2: state severity, binary triggers)
// ---------------------------------------------------------------------------

fun switchContainer(scheme: ColorScheme, isOn: Boolean): Color =
    if (isOn) scheme.secondaryContainer else scheme.surfaceContainerHighest

/** A triggered binary sensor sits in the error container; untriggered is idle. */
fun binarySensorContainer(scheme: ColorScheme, triggered: Boolean): Color =
    if (triggered) scheme.errorContainer else scheme.surfaceContainerHighest

// ---------------------------------------------------------------------------
// Media tokens (tonal fallback; see the header deviation note)
// ---------------------------------------------------------------------------

fun mediaContainerColor(scheme: ColorScheme, playing: Boolean): Color =
    if (playing) scheme.tertiaryContainer else scheme.surfaceContainerHighest

// ---------------------------------------------------------------------------
// State-shape badging (spec item 4)
// ---------------------------------------------------------------------------

/**
 * Morph progress for a domain badge: 0 = idle shape, 1 = active shape.
 * Climate is active only while it is actually heating/cooling (hvac_action
 * wins when present; otherwise the mode state decides, and "off"/"idle" is
 * never active). Sensors never morph — no severity threshold is invented
 * for them, so they stay on their idle octagon.
 */
fun badgeMorphProgress(domain: String, state: String, hvacAction: String?): Float =
    when (domain) {
        "light", "switch", "input_boolean", "automation", "script", "scene" ->
            if (state == "on") 1f else 0f
        "climate" ->
            if (state == "off" || hvacAction == "idle") 0f
            else if (hvacAction in setOf("heating", "cooling", "preheating", "drying") ||
                state in setOf("heat", "cool", "heat_cool", "auto", "dry", "fan_only")
            ) 1f else 0f
        "cover" -> if (state == "closed") 0f else 1f
        "media_player" -> if (state == "playing") 1f else 0f
        "weather" -> 1f
        "binary_sensor" -> if (state == "on") 1f else 0f
        else -> if (state == "on") 1f else 0f
    }

/** Alert badges (binary sensors triggered) rotate continuously and pulse. */
fun badgeAlert(domain: String, state: String): Boolean =
    domain == "binary_sensor" && state == "on"

/** The idle shape for a domain (see the per-domain table in the spec). */
fun badgeIdleShape(domain: String): StateShape = when (domain) {
    "light", "switch", "input_boolean" -> StateShape.HEXAGON
    "climate" -> StateShape.SQUIRCLE
    "cover", "lock" -> StateShape.SHIELD
    "media_player" -> StateShape.SQUIRCLE
    "weather", "sensor" -> StateShape.OCTAGON
    else -> StateShape.SQUIRCLE
}

/** The active shape for a domain; weather picks by condition (see below). */
fun badgeActiveShape(domain: String, condition: String? = null): StateShape = when (domain) {
    "light", "switch", "input_boolean" -> StateShape.SCALLOP12
    "climate" -> StateShape.ARC_WAVE
    "cover", "lock" -> StateShape.NOTCHED_DIAMOND
    "media_player" -> StateShape.STAR8
    "weather" -> weatherActiveShape(condition)
    "sensor" -> StateShape.OCTAGON
    else -> StateShape.SCALLOP12
}

/** Weather's active polygon: sunburst for clear conditions, cloud otherwise. */
fun weatherActiveShape(condition: String?): StateShape = when (condition?.lowercase()) {
    "sunny", "clear", "clear-night", "partlycloudy", "partly-cloudy" -> StateShape.SUNBURST
    else -> StateShape.CLOUD_POLY
}

/**
 * The destination polygon for a traveling badge (2026-10-29
 * verdict-fix round): the state polygon the pop-up header badge
 * renders at rest — the settled end of [badgeMorphProgress], never a
 * mid-animation blend. The traveling badge morphs its polygon from
 * the source card's polygon to this shape across the morph (see
 * TravelingBadge), so the icon reads as one element transforming,
 * not sliding into place unchanged. Pure, unit-tested.
 */
fun travelBadgeDestinationShape(domain: String, state: String, hvacAction: String?, condition: String?): StateShape =
    if (badgeMorphProgress(domain, state, hvacAction) >= 0.5f) badgeActiveShape(domain, condition)
    else badgeIdleShape(domain)

/** Whether a cover is mid-travel (the warning chevron lifts while moving). */
fun coverIsMoving(state: String): Boolean = state == "opening" || state == "closing"

// ---------------------------------------------------------------------------
// Prominent dynamic colors (2026-10-29 verdict: the #41 tokens exist
// but are not VISIBLE enough). These are the per-domain dynamic colors
// the UI scales with state: thermostat mode, light kelvin + brightness,
// weather condition. All pure, all unit-tested.
// ---------------------------------------------------------------------------

/** Heating and cooling at full strength: dial arcs, setpoints, the mode readout. */
val HeatOrange = Palette.HeatSignal
val CoolBlue = Palette.CoolSignal

/** A climate entity that is neither heating nor cooling right now. */
val ClimateNeutral = Palette.Cloud.t60

/**
 * The color a climate entity speaks in. hvac_action is the live truth
 * and decides whenever it is present: heating is heat, cooling is cool,
 * anything else (idle, off, fan, drying) is neutral, because a
 * thermostat in heat mode that is not running is not heating. Only
 * without it does the mode decide, heat_cool and auto reading as heat.
 */
fun climateModeColor(state: String, hvacAction: String?): Color = when {
    hvacAction == "heating" || hvacAction == "preheating" -> HeatOrange
    hvacAction == "cooling" -> CoolBlue
    hvacAction != null -> ClimateNeutral
    state == "heat" -> HeatOrange
    state == "cool" -> CoolBlue
    state == "heat_cool" || state == "auto" -> HeatOrange
    else -> ClimateNeutral
}

/**
 * Sensor kind, resolved synchronously from the entity id, the
 * device_class attribute, or the friendly name — never from an
 * async lookup — so badge colors and icons resolve in the same
 * composition frame on every surface (the Round 4 no-flash rule:
 * a token that needs a later frame to settle is a flash).
 * TEMPERATURE/HUMIDITY are the Ecobee pair; BATTERY, POWER, and
 * LIGHT cover the other sensors with an obvious icon; anything
 * else is OTHER and keeps the muted badge pair.
 */
enum class SensorKind { TEMPERATURE, HUMIDITY, BATTERY, POWER, LIGHT, PRESSURE, OTHER }

fun sensorKind(entityId: String, deviceClass: String?, friendlyName: String?): SensorKind {
    val id = entityId.lowercase()
    val dc = deviceClass?.lowercase()
    val name = friendlyName?.lowercase() ?: ""
    return when {
        dc == "temperature" || "_temperature" in id || "temperature" in name -> SensorKind.TEMPERATURE
        dc == "humidity" || "_humidity" in id || "humidity" in name -> SensorKind.HUMIDITY
        dc == "battery" || "_battery" in id || "battery" in name -> SensorKind.BATTERY
        dc in setOf("power", "energy") || "_power" in id || "_energy" in id ||
            "power" in name || "energy" in name -> SensorKind.POWER
        dc == "illuminance" || "illuminance" in id || "illuminance" in name -> SensorKind.LIGHT
        dc == "pressure" || "_pressure" in id || "pressure" in name -> SensorKind.PRESSURE
        else -> SensorKind.OTHER
    }
}

/**
 * The family a sensor's reading belongs to. Null for a reading with no
 * meaning of its own (a count, a text state), which stays neutral.
 */
fun sensorFamily(kind: SensorKind): StateFamily? = when (kind) {
    SensorKind.TEMPERATURE -> StateFamily.Heat
    SensorKind.HUMIDITY -> StateFamily.Water
    SensorKind.POWER -> StateFamily.Power
    SensorKind.BATTERY -> StateFamily.On
    SensorKind.LIGHT -> StateFamily.Caution
    SensorKind.PRESSURE -> StateFamily.Cool
    SensorKind.OTHER -> null
}

/**
 * A badge fill and glyph for a sensor where the theme is not known:
 * mid tones that read on either ground. Tiles do not use this; they
 * take the sensor's family through domainCardColors.
 */
fun sensorBadgeColors(kind: SensorKind): Pair<Color, Color>? = when (kind) {
    SensorKind.TEMPERATURE -> Palette.Ember.t60 to Palette.Ember.t10
    SensorKind.HUMIDITY -> Palette.Rain.t70 to Palette.Rain.t10
    else -> null
}

/** The hue a sensor's card is washed with where it is drawn nested. Null for [SensorKind.OTHER]. */
fun sensorCardTint(kind: SensorKind): Color? = when (kind) {
    SensorKind.TEMPERATURE -> Palette.Ember.t60
    SensorKind.HUMIDITY -> Palette.Rain.t70
    else -> null
}

/**
 * The line a chart is drawn in. Keyed by what is measured, never by
 * which entity, so two temperature sensors always draw the same line.
 */
fun chartLineColor(kind: SensorKind, isDark: Boolean): Color =
    (sensorFamily(kind) ?: StateFamily.Media).signal(isDark)

/**
 * Light container scaled by brightness: the kelvin-tinted container
 * at full brightness, fading toward the neutral idle container as the
 * light dims (floored at 0.25 so a dim light still reads tinted, never
 * gray). Off lights are the neutral idle container, same as before.
 * brightnessPct is HA's brightness_pct (0-100); null means unknown and
 * is treated as full brightness.
 */
fun lightBrightnessContainer(
    scheme: ColorScheme,
    kelvin: Int?,
    brightnessPct: Int?,
    isOn: Boolean,
): Color {
    if (!isOn) return scheme.surfaceContainerHighest
    val full = lightContainerColor(scheme, kelvin, true)
    val f = ((brightnessPct ?: 100).coerceIn(0, 100) / 100f).coerceIn(0.25f, 1f)
    return lerp(scheme.surfaceContainerHighest, full, f)
}

/**
 * Weather's prominent color by condition — the same hue family the
 * weather page hero, the front weather card tint, and the forecast
 * chips all draw from, so condition changes are unmissable.
 * Not a scheme role: per-condition hues (there is no "rainy" role).
 */
// ---------------------------------------------------------------------------
// Condition display, centered-dialog geometry, and contrast floors
// (2026-10-30 verdict round on run #75)
// ---------------------------------------------------------------------------

/**
 * Display form of an HA condition string: compound/camelCase split
 * + Title Case, with spaces — never a raw condition on screen.
 * "partlycloudy" -> "Partly Cloudy" (the glued compound HA reports
 * for partly cloudy); "clear-night" / "clear_night" -> "Clear Night";
 * "snowy-rainy" -> "Snowy Rainy"; "sunny" -> "Sunny". Pure.
 */
fun formatCondition(condition: String): String {
    if (condition.isBlank()) return condition
    val spaced = condition
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        .replace('_', ' ')
        .replace('-', ' ')
    return spaced.split(' ').filter { it.isNotEmpty() }.flatMap { word ->
        when (word.lowercase()) {
            "partlycloudy" -> listOf("Partly", "Cloudy")
            else -> listOf(word.lowercase().replaceFirstChar { it.uppercase() })
        }
    }.joinToString(" ")
}

/**
 * Dialog header name size by name length (2026-10-30 verdict:
 * long entity names truncated to "Thermostat Current
 * Temperatu…" in dialog headers). Steps down so the full name
 * shows on one line instead of ellipsizing: 20sp through 22
 * characters, 17sp through 28, 15sp beyond. The traveling header
 * lerps to this same size, so the name never changes size at the
 * handoff. Extremely long names still ellipsize as a last resort.
 * Pure, unit-tested.
 */
fun dialogNameSize(name: String): TextUnit = when {
    name.length <= 22 -> 20.sp
    name.length <= 28 -> 17.sp
    else -> 15.sp
}

/**
 * Centered-dialog panel width (verdict spec, exact): 90% of the
 * container, capped at 480dp — proportional on compact screens and
 * expanded foldables alike. Pure, unit-tested; the dialog overlay
 * applies it, never a hardcoded width.
 */
fun dialogPanelWidthDp(containerWidthDp: Float): Float =
    (containerWidthDp * 0.9f).coerceAtMost(480f)

/**
 * Centered-dialog panel max height: 85% of the container; the inner
 * body scrolls past it (scroll fallback for dense entities). Pure.
 */
fun dialogPanelMaxHeightDp(containerHeightDp: Float): Float =
    containerHeightDp * 0.85f

/**
 * WCAG relative luminance (sRGB linearization). Alpha is ignored:
 * callers composite translucent colors over their backdrop first
 * (see [compositeOver]).
 */
fun relativeLuminance(color: Color): Float {
    fun lin(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * lin(color.red) + 0.7152f * lin(color.green) + 0.0722f * lin(color.blue)
}

/** WCAG contrast ratio between two opaque colors (1..21). Pure. */
fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = relativeLuminance(foreground)
    val l2 = relativeLuminance(background)
    val (hi, lo) = if (l1 >= l2) l1 to l2 else l2 to l1
    return (hi + 0.05f) / (lo + 0.05f)
}

/** Alpha-composites [foreground] over opaque [background]. Pure. */
fun compositeOver(foreground: Color, background: Color): Color {
    val a = foreground.alpha.coerceIn(0f, 1f)
    return Color(
        foreground.red * a + background.red * (1f - a),
        foreground.green * a + background.green * (1f - a),
        foreground.blue * a + background.blue * (1f - a),
        1f,
    )
}

/**
 * Returns [foreground] unchanged when it already meets [minRatio]
 * against [background]; otherwise blends it toward black or white
 * (whichever contrasts more with the backdrop) just far enough to
 * meet the ratio — hue family preserved, readability guaranteed.
 * The verdict's dial-number fix: dark red/blue setpoint numbers on
 * the pale pastel flower disc fail in both themes at full strength,
 * and the 0.45-alpha inactive numbers fail outright; this floors
 * them (4.5 for active numbers, 3.0 for the large-text inactive
 * ones) instead of trusting the raw token. Pure.
 */
fun ensureContrast(foreground: Color, background: Color, minRatio: Float = 4.5f): Color {
    if (contrastRatio(foreground, background) >= minRatio) return foreground
    val towardWhite = contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)
    val target = if (towardWhite) Color.White else Color.Black
    var t = 0.1f
    while (t < 1f) {
        val candidate = lerp(foreground, target, t)
        if (contrastRatio(candidate, background) >= minRatio) return candidate
        t += 0.1f
    }
    return target
}

// ---------------------------------------------------------------------------
/** The color of the sky right now, by Home Assistant condition. */
fun weatherConditionColor(condition: String?): Color = when (condition?.lowercase()) {
    "sunny", "clear" -> Palette.Honey.t80
    "clear-night" -> Palette.Guard.t50
    "partlycloudy", "partly-cloudy" -> Palette.Cloud.t70
    "cloudy" -> Palette.Cloud.t60
    "rainy", "pouring" -> Palette.Frost.t50
    "snowy", "snowy-rainy" -> Palette.Rain.t85
    "lightning", "lightning-rainy" -> Palette.Tune.t50
    "fog" -> Palette.Cream.t60
    "hail" -> Palette.Rain.t70
    "windy", "windy-variant" -> Palette.Dew.t70
    else -> Palette.Cloud.t70
}

// ---------------------------------------------------------------------------
// Dark ground
// ---------------------------------------------------------------------------

/**
 * True when [color] is a dark backdrop (luminance below 0.5). Call
 * sites derive dark from the scheme's background luminance rather
 * than isSystemInDarkTheme(), so the user's explicit Light/Dark
 * theme-mode override is honored, not just the system setting.
 */
fun isDarkBackdrop(color: Color): Boolean = color.luminance() < 0.5f

/**
 * Condition colors on true black. The saturated ones are already bright
 * enough and stay as they are; only the greys lift, because a mid grey
 * cloud on black is a flat blob.
 */
fun weatherConditionColorDark(condition: String?): Color = when (condition?.lowercase()) {
    "partlycloudy", "partly-cloudy" -> Palette.Cloud.t80
    "cloudy" -> Palette.Cloud.t70
    "fog" -> Palette.Cream.t70
    else -> weatherConditionColor(condition)
}

/** Theme-aware condition color: dark variants in dark, light values otherwise. */
fun weatherConditionColorThemed(condition: String?, isDark: Boolean): Color =
    if (isDark) weatherConditionColorDark(condition) else weatherConditionColor(condition)

/**
 * The glyph on a condition chip: dark ink on a light fill, white on a
 * dark one, decided by the fill's own luminance so every chip clears
 * the 3:1 icon floor in both themes. (isDark is kept for call sites; the
 * fill alone decides.)
 */
@Suppress("UNUSED_PARAMETER")
fun weatherChipGlyphColor(fill: Color, isDark: Boolean): Color =
    if (relativeLuminance(fill) > 0.22f) Palette.Cloud.t10 else Color.White

/**
 * Secondary text floored to 4.5:1 against its backdrop — the #92
 * badge-contrast discipline applied to secondary text. Identity
 * when the scheme's onSurfaceVariant already passes, so light
 * theme (and any passing dark pair) is untouched; only dim pairs
 * are lifted toward white/black. Pure, unit-tested.
 */
fun legibleSecondaryOn(scheme: ColorScheme, backdrop: Color): Color =
    ensureContrast(scheme.onSurfaceVariant, backdrop, 4.5f)

/** [legibleSecondaryOn] against the scheme background (the common case). */
fun legibleSecondary(scheme: ColorScheme): Color =
    legibleSecondaryOn(scheme, scheme.background)

/*
 * Navigation chrome. The bar looks the same by day and by night (see
 * HavenChrome): near-black green, a lime capsule on the selected tab.
 * The scheme and isDark parameters are kept so call sites read like any
 * other color lookup; they do not change the answer.
 */
val PillBarContainer = HavenChrome.bar
val PillBarActiveCapsule = HavenChrome.capsule
val PillBarActiveGlyph = HavenChrome.onCapsule
val PillBarUnselectedGlyph = HavenChrome.idleGlyph

@Suppress("UNUSED_PARAMETER")
fun selectedPillFill(scheme: ColorScheme, isDark: Boolean): Color = PillBarActiveCapsule

@Suppress("UNUSED_PARAMETER")
fun selectedPillGlyph(scheme: ColorScheme, isDark: Boolean): Color = PillBarActiveGlyph

@Suppress("UNUSED_PARAMETER")
fun pillUnselectedGlyph(scheme: ColorScheme, isDark: Boolean): Color = PillBarUnselectedGlyph

// ---------------------------------------------------------------------------
// Semantic states
// ---------------------------------------------------------------------------
/**
 * What an entity's state means, independent of its domain. Status rows
 * and attention dots resolve an entity to one of these and then to a
 * color family (data/SemanticColors.kt). Device-state color always
 * comes from the real Home Assistant state: it is never a manual
 * override and never follows the wallpaper.
 */
enum class SemanticState {
    /** Light/switch on: warm yellow/lime. */
    DeviceOn,
    /** Light/switch off: muted neutral. */
    DeviceOff,
    /** Lock securely locked: blue. */
    LockLocked,
    /** Lock unlocked: amber. */
    LockUnlocked,
    /** Lock fault/alert: red. Never a reassuring state when unknown. */
    LockFault,
    /** Door/window open: coral/warm red. */
    OpenAlert,
    /** Door/window closed: muted neutral. */
    ClosedNeutral,
    /** Motion detected: amber. Distinct from an alarm. */
    MotionActive,
    /** Motion clear: neutral. */
    MotionClear,
    /** Heating active: coral/warm. */
    HeatingActive,
    /** Cooling active: blue. */
    CoolingActive,
    /** Climate idle: neutral. */
    ClimateIdle,
    /** Media playing: lavender. */
    MediaPlaying,
    /** Media paused/idle: muted neutral. */
    MediaIdle,
    /** Energy low: restrained yellow/green. */
    EnergyLow,
    /** Energy high: progressively warmer yellow/orange. */
    EnergyHigh,
    /** Energy critical/warning threshold: red. */
    EnergyCritical,
    /** Entity unavailable: muted grey. */
    Unavailable,
    /** Entity unknown: neutral with an explicit unknown indication. */
    Unknown,
    /** Alarm/fault condition: red. */
    AlarmFault,
}
