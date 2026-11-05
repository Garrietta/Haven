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
import com.garrietta.haven.ui.theme.StateShape

/**
 * Pure design-token logic for the ARCHITECTURE AND SYSTEM FOUNDATIONS round.
 *
 * The color pipeline maps each HA domain to explicit Material 3 tonal
 * containers, never static hex fills — except where a glyph tint needs a
 * warm-to-cool color-temperature ramp (lights), which is still a pure
 * function of kelvin, not a hardcoded fill.
 *
 * DEVIATION (media): the spec asks for dynamic artwork extraction (Palette /
 * Monet seed colors, as Koda does) where artwork exists. Haven's pipeline
 * carries no artwork bitmap for media entities — MediaControl shows
 * title/artist text only, and no Palette or Coil dependency exists — so
 * media falls back to the tonal mapping (playing = tertiary family,
 * idle = surfaceContainerHighest) everywhere. No artwork path was invented.
 */

/**
 * The general card shape (expressive-menus round, item 6): three
 * 28dp corners + one 12dp corner, the 12dp corner rotating by grid
 * index — the UI rewrite's card rule, now the app-wide default for
 * card shells. A per-entity or global shape-picker override still
 * beats it everywhere (the picker is the user's explicit choice);
 * this is only the fallback. Single (non-grid) cards use index 0.
 * Pure, so the rule itself is unit-testable via its callers' tests.
 */
/**
 * Customization round, item 1: EIGHT card-corner variants, cycling by
 * grid index so a grid never reads as identical tiles. Variants 0-3
 * are the expressive-menus rule (three 28dp + one 12dp, rotating);
 * 4-7 add the sharper / softer / pillow / diagonal edges the user
 * asked for. The center/badge language is untouched — only the
 * outer edges vary. Per-entity corner picks (CARD_CORNER_OPTIONS
 * keys "corners_0".."corners_7", via shapeForKey) beat this cycle.
 */
fun generalCardShape(index: Int): RoundedCornerShape = when ((index % 8 + 8) % 8) {
    0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 28.dp)
    1 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 12.dp)
    2 -> RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 28.dp)
    3 -> RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 28.dp)
    4 -> RoundedCornerShape(8.dp)
    5 -> RoundedCornerShape(28.dp)
    6 -> RoundedCornerShape(36.dp)
    else -> RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 28.dp)
}

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
// Light color temperature (spec item 2: secondary -> tertiary by kelvin)
// ---------------------------------------------------------------------------

/** 2000K (candle warm) = 0, 6500K (daylight) = 1; unknown kelvin = neutral 0.5. */
fun lightTempBlendFraction(kelvin: Int?): Float =
    if (kelvin == null) 0.5f else (kelvin - 2000).coerceIn(0, 4500) / 4500f

/** Glyph tint following bulb color temperature: warm amber to cool white-blue.
 * Not a scheme role: a continuous kelvin ramp (2000K→6500K), not a container. */
fun lightColorTempTint(kelvin: Int?): Color =
    lerp(Color(0xFFFFB300), Color(0xFFCCE4FF), lightTempBlendFraction(kelvin))

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

/**
 * Semantic state colors, NOT ColorScheme roles: the scheme has no
 * heat/cool role, so heating orange and cooling blue are named values
 * owned here — the single source of truth. Every heat/cool constant
 * elsewhere (dial arcs, tiles, thermostat pages) must reference these,
 * never re-declare the hex. Values are run #38's Google-Home
 * orange/blue: container pastels are unreadable as dial text/thumbs.
 */
val HeatOrange = Color(0xFFD84315)
val CoolBlue = Color(0xFF1565C0)

/**
 * Prominent dynamic color for a climate entity: warm orange while
 * heating, cool blue while cooling. hvac_action, when present,
 * decides completely (it is the live truth): heating/preheating →
 * heat, cooling → cool, anything else (idle, off, fan, drying) →
 * neutral — an idle thermostat in heat mode is not heating, so it
 * must not read heat-colored. Only when hvac_action is absent does
 * the mode state decide, with heat_cool / auto defaulting to the
 * heating color (the heating selector is the default selector).
 * Off, dry, and fan_only have no heating/cooling direction and get
 * the neutral blue-gray — never a heating or cooling color for a
 * state that is neither.
 */
fun climateModeColor(state: String, hvacAction: String?): Color = when {
    hvacAction == "heating" || hvacAction == "preheating" -> HeatOrange
    hvacAction == "cooling" -> CoolBlue
    hvacAction != null -> Color(0xFF78909C)
    state == "heat" -> HeatOrange
    state == "cool" -> CoolBlue
    state == "heat_cool" || state == "auto" -> HeatOrange
    else -> Color(0xFF78909C)
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
 * Semantic badge colors for sensors (Round 4, replacing the
 * #89 purely-muted sensor badges): warm coral-red for
 * temperature, sky-blue for humidity. Fixed hues, not scheme
 * roles — the scheme has no temperature/humidity role — with
 * glyph colors floored to 3:1 against the fill (the large-icon
 * floor used elsewhere for 26dp glyphs). Pure and synchronous:
 * badgeColors calls this with the entity alone, so no badge can
 * flash a different shade before settling.
 */
fun sensorBadgeColors(kind: SensorKind): Pair<Color, Color>? = when (kind) {
    SensorKind.TEMPERATURE -> Color(0xFFEF5350) to Color.White
    SensorKind.HUMIDITY -> Color(0xFF29B6F6) to Color(0xFF01579B)
    else -> null
}

/** Card-face tint for a sensor, at the same low alpha climate and weather cards use. Null for OTHER. */
fun sensorCardTint(kind: SensorKind): Color? = when (kind) {
    SensorKind.TEMPERATURE -> Color(0xFFEF5350)
    SensorKind.HUMIDITY -> Color(0xFF29B6F6)
    else -> null
}

/**
 * Feedback round 20, item 2: the ONE chart-line color source. The
 * 24h strips used accentFor(entityId) — a per-entity hash color —
 * so two temperature sensors drew different lines (the 5179 sensor
 * teal, the Temperature sensor red). Lines are keyed by
 * measurement, never by entity: temperature and humidity use the
 * exact badge-bypass hues; power, battery, illuminance, and
 * pressure use their matrix rows' badge tones (pale on dark
 * theme, deep ink on light theme, so the line reads on both);
 * unclassified sensors share the MEDIA row tones, so any two
 * like sensors still match. Badges themselves are untouched
 * (they already follow the bypasses). Pure, unit-tested.
 */
fun chartLineColor(kind: SensorKind, isDark: Boolean): Color = when (kind) {
    SensorKind.TEMPERATURE -> Color(0xFFEF5350)
    SensorKind.HUMIDITY -> Color(0xFF29B6F6)
    SensorKind.POWER -> if (isDark) Color(0xFFFFE082) else Color(0xFF261900)
    SensorKind.BATTERY -> if (isDark) Color(0xFFA6F285) else Color(0xFF143800)
    SensorKind.LIGHT -> if (isDark) Color(0xFFB8F397) else Color(0xFF1B3708)
    SensorKind.PRESSURE -> if (isDark) Color(0xFFA5C8FF) else Color(0xFF001C3A)
    SensorKind.OTHER -> if (isDark) Color(0xFFDAB9FF) else Color(0xFF2B1246)
}

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
fun weatherConditionColor(condition: String?): Color = when (condition?.lowercase()) {
    "sunny", "clear" -> Color(0xFFFFB300)
    "clear-night" -> Color(0xFF5C6BC0)
    "partlycloudy", "partly-cloudy" -> Color(0xFF90A4AE)
    "cloudy" -> Color(0xFF78909C)
    "rainy", "pouring" -> Color(0xFF1E88E5)
    "snowy", "snowy-rainy" -> Color(0xFF81D4FA)
    "lightning", "lightning-rainy" -> Color(0xFF7E57C2)
    "fog" -> Color(0xFF9E9E9E)
    "hail" -> Color(0xFF4FC3F7)
    "windy", "windy-variant" -> Color(0xFF80CBC4)
    else -> Color(0xFF90A4AE)
}

// ---------------------------------------------------------------------------
// Round 8 dark-theme pass (audit vs the Koda dark reference: true-black
// background, white primary text, blue selected pill, colorful content)
// ---------------------------------------------------------------------------

/**
 * True when [color] is a dark backdrop (luminance below 0.5). Call
 * sites derive dark from the scheme's background luminance rather
 * than isSystemInDarkTheme(), so the user's explicit Light/Dark
 * theme-mode override is honored, not just the system setting.
 */
fun isDarkBackdrop(color: Color): Boolean = color.luminance() < 0.5f

/**
 * Dark-theme condition colors. The saturated hues (sunny amber,
 * rainy blue, snowy sky, lightning purple) are already bright
 * against black and are kept identical to light — same semantic
 * colors, not a second palette. Only the grey family changes: the
 * light greys (partly-cloudy blue-grey, cloudy blue-grey, fog
 * grey) read as flat grey blobs on black at light values, so
 * their dark variants are brightened to clear the background
 * while staying grey (a cloud is still grey, just legible grey).
 * Pure, unit-tested.
 */
fun weatherConditionColorDark(condition: String?): Color = when (condition?.lowercase()) {
    "partlycloudy", "partly-cloudy" -> Color(0xFFB0BEC5)
    "cloudy" -> Color(0xFFA7BAC2)
    "fog" -> Color(0xFFBDBDBD)
    else -> weatherConditionColor(condition)
}

/** Theme-aware condition color: dark variants in dark, light values otherwise. */
fun weatherConditionColorThemed(condition: String?, isDark: Boolean): Color =
    if (isDark) weatherConditionColorDark(condition) else weatherConditionColor(condition)

/**
 * Glyph color for a condition chip/badge fill. Light theme keeps
 * the historical white glyph everywhere (unchanged). Dark theme:
 * light fills (sunny amber, brightened cloudy greys, rainy blue,
 * snowy sky) take a near-black glyph, because a white glyph on a
 * mid-grey/amber chip fails the 3:1 large-glyph floor and the icon
 * dissolves into the chip — the "grey blob" defect. Dark fills
 * (clear-night indigo, lightning purple) keep white. Every branch
 * clears 3:1 against its fill. Pure, unit-tested.
 */
fun weatherChipGlyphColor(fill: Color, isDark: Boolean): Color {
    if (!isDark) return Color.White
    return if (relativeLuminance(fill) > 0.22f) Color(0xFF102027) else Color.White
}

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

/**
 * Selected-pill fill and glyph, shared by the folded PillNav and
 * the unfolded VerticalSidePill so the two chrome variants can
 * never disagree. Light: the directive's lime #97F676 (visual-
 * fidelity round §2, replacing Stage B's #BEE986) — a fixed Haven
 * identity token, NOT scheme.secondaryContainer, so the selected
 * pill stays lime whether dynamic color is on or off. Dark:
 * Haven's ACCENTS blue (0xFF3B82F6 — the same value as the blue
 * entry in Expressive.kt's ACCENTS list, and the Koda selected-
 * pill blue reference), because dynamic-dark secondaryContainer is
 * a dim desaturated tone that reads as a grey blob on black.
 * Glyphs: near-black #0A2005 on lime (≈11:1), white on blue
 * (3.68:1, above the 3:1 large-glyph floor). Pure, unit-tested.
 */
/**
 * UI rewrite round (2026-11-03): the navigation chrome is theme-independent —
 * the same near-black container, lime capsule, ink glyph, and pale-sage
 * unselected icons in BOTH light and dark themes. This supersedes Round 8's
 * split tokens (Koda-blue dark pill, secondaryContainer light pill), which
 * produced the flagged blue-highlight bug on the unfolded dark rail. The
 * scheme/isDark parameters stay so call sites need no signature change.
 */
val PillBarContainer = Color(0xFF161D17)
val PillBarActiveCapsule = Color(0xFF9BF67C)
val PillBarActiveGlyph = Color(0xFF0A2005)
val PillBarUnselectedGlyph = Color(0xFFD7E2D4)

fun selectedPillFill(scheme: ColorScheme, isDark: Boolean): Color = PillBarActiveCapsule

fun selectedPillGlyph(scheme: ColorScheme, isDark: Boolean): Color = PillBarActiveGlyph

fun pillUnselectedGlyph(scheme: ColorScheme, isDark: Boolean): Color =
    // Feedback round 3, item 1 (video ba): CORRECTION of round 2
    // item 5. Round 2 removed the pill background and made the
    // unselected glyph theme-aware — but the user wants the pill
    // AND its background kept; only the transition clip box goes.
    // With the opaque #161D17 container restored, the round-2
    // glyph change's precondition (no dark container) is gone, so
    // the #108 law returns: pale sage #D7E2D4 in BOTH themes.
    PillBarUnselectedGlyph

// ---------------------------------------------------------------------------
// Stage B token slots for Stage E (semantic colors).
// ---------------------------------------------------------------------------
/**
 * Stage B ONLY defines these slots. The full domain+state → visual
 * mapping (with deliberate light+dark variants, reused across Home,
 * rooms, badges, and summaries) is Stage E's work in a new
 * SemanticColors.kt — do NOT resolve colors from this enum here,
 * and do NOT half-build the mapping in this file in a way Stage E
 * must rip out. Each slot names the brief §7 semantic state and
 * its intended meaning; Stage E assigns the actual colors.
 *
 * Palette vs state, kept sharp (Stage B rule): the fixed sage/lime
 * palette above is the DESIGN SYSTEM (surfaces, chrome, text). The
 * slots below are DEVICE-STATE colors, always automatic from real
 * HA states, never manual overrides and never wallpaper-derived.
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
