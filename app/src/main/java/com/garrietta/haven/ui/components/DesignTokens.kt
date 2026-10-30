package com.garrietta.haven.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

// ---------------------------------------------------------------------------
// Pill bar label clamp (spec item 3b)
// ---------------------------------------------------------------------------

/**
 * A tab label is shown only when the tab is selected AND its title is at
 * most 10 characters. Longer titles collapse to an icon badge, which is
 * what fixes the old "Living Room lig" truncation.
 */
fun pillLabelVisible(label: String, isSelected: Boolean): Boolean =
    isSelected && label.length <= 10

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
