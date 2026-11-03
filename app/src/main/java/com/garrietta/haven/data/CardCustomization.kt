package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color

/**
 * Per-entity card customization (customization round, item 2).
 *
 * One entity, four choices, stored as plain JSON maps in SettingsStore
 * (the shape-picker override pattern): a display title, an icon key, a
 * tile-color preset key. The badge-shape choice already lives in the
 * shape-picker overrides, so Card settings reuses that picker rather
 * than inventing a second shape store. All resolution here is pure,
 * so it is unit-testable without Compose.
 *
 * Resolution law: a stored override beats the HA default everywhere
 * the override is read; an unknown or blank key reads as "no
 * override" (never a crash, never a guessed color). A tile preset
 * stores BOTH a light and a dark color so the tile stays
 * theme-appropriate; the badge colors stay matrix-driven, so the
 * 4.5:1 title/status floor (ensureContrast at the call sites) still
 * holds against a preset container.
 */
data class TilePreset(val key: String, val label: String, val light: Color, val dark: Color)

/** Preset list: the app's own palette (sage/lime/charcoal/warm off-white) plus the domain matrix card colors. */
val TILE_PRESETS: List<TilePreset> = listOf(
    TilePreset("lime", "Lime", Color(0xFFC8F5A6), Color(0xFF2B4E14)),
    TilePreset("heating", "Heating coral", Color(0xFFFFDBD0), Color(0xFF532215)),
    TilePreset("cooling", "Cooling blue", Color(0xFFD4E3FF), Color(0xFF1A304E)),
    TilePreset("lock", "Lock indigo", Color(0xFFDCE1FF), Color(0xFF222C61)),
    TilePreset("alert", "Alert red", Color(0xFFFFDAD6), Color(0xFF5C1916)),
    TilePreset("media", "Media lavender", Color(0xFFEFE0FF), Color(0xFF3B2554)),
    TilePreset("amber", "Amber", Color(0xFFFFE082), Color(0xFF4D3800)),
    TilePreset("sage", "Sage neutral", Color(0xFFEFF1E8), Color(0xFF1C201B)),
)

/** Corner-shape option keys for card outlines (customization round, item 1). */
data class CardCornerOption(val key: String, val label: String)

val CARD_CORNER_OPTIONS: List<CardCornerOption> = listOf(
    CardCornerOption("corners_0", "Corners 28 / 12 (bottom-right)"),
    CardCornerOption("corners_1", "Corners 28 / 12 (bottom-left)"),
    CardCornerOption("corners_2", "Corners 28 / 12 (top-right)"),
    CardCornerOption("corners_3", "Corners 28 / 12 (top-left)"),
    CardCornerOption("corners_4", "Sharp 8dp"),
    CardCornerOption("corners_5", "Soft 28dp"),
    CardCornerOption("corners_6", "Pillow 36dp"),
    CardCornerOption("corners_7", "Slope 28 / 12 diagonal"),
)

/** Display title: custom title wins, else the HA friendly name. Blank custom titles read as no override. */
fun customTitleFor(titles: Map<String, String>, entityId: String, friendlyName: String): String {
    val custom = titles[entityId]?.trim().orEmpty()
    return if (custom.isEmpty()) friendlyName else custom
}

/** Icon key: stored key wins only when it is a known picker key, else null (caller falls back to the domain icon). */
fun customIconKeyFor(iconKeys: Map<String, String>, entityId: String, knownKeys: Set<String>): String? {
    val key = iconKeys[entityId] ?: return null
    return if (key in knownKeys) key else null
}

/** Tile preset for an entity, or null for "default" (unknown keys read as default, never crash). */
fun tilePresetFor(presets: Map<String, String>, entityId: String): TilePreset? {
    val key = presets[entityId] ?: return null
    return TILE_PRESETS.firstOrNull { it.key == key }
}

/** The preset's container color for the current theme, or null for default. */
fun tilePresetColor(preset: TilePreset?, isDark: Boolean): Color? =
    preset?.let { if (isDark) it.dark else it.light }

/**
 * Feedback round, item 5: when a custom tile preset is set, the
 * card's badge + icon colors derive from that preset — they do
 * NOT stay matrix-driven (the customization round's rule, which
 * the user explicitly overrode: "those should become equally as
 * like dark versions of the colors that I've assigned. So instead
 * of being a green tinted, almost black, it should be a yellow
 * tinted, almost black. And this should be like an amber,
 * orange-ish color on both of those").
 *
 * Derivation, hue-preserving (pure RGB<->HSV/HSL, no android
 * dependency so unit tests run on the JVM):
 * - badgeBg: the preset's theme color darkened to a near-black
 *   tint (HSV value 0.13, saturation kept high) — a yellow tile
 *   gives a yellow-tinted almost-black badge, never matrix green.
 * - badgeGlyph: the preset hue at full saturation and 0.72 HSL
 *   lightness — the BRIGHT tint of the assigned color, which for
 *   the yellow preset reads amber/gold, exactly as he described.
 *   (A pure vivid HSV glyph was tried first and REJECTED: vivid
 *   blue/purple are perceptually dark and fail 4.5:1 on a
 *   near-black badge. The 0.72-lightness tint clears 4.5:1 for
 *   every preset in both themes — pinned by CardCustomizationTest.)
 * Unset cards keep the matrix (callers only use this when
 * [tilePresetFor] returns a preset). Switch tracks stay
 * matrix-driven: he pointed at badges/icons, not switches.
 */
fun presetBadgeColors(preset: TilePreset, isDark: Boolean): Pair<Color, Color> {
    val base = if (isDark) preset.dark else preset.light
    val (h, s, _) = rgbToHsv(base.red, base.green, base.blue)
    val badgeBg = hsvToColor(h, s.coerceAtLeast(0.55f), 0.13f)
    val badgeGlyph = hslToColor(h, 1.0f, 0.72f)
    return badgeBg to badgeGlyph
}

/**
 * Feedback round 2, item 1: a custom tile preset is an ON-state
 * color. It applies only while the entity is in its active state;
 * when the entity is off (or its state is unknown), the card must
 * render the matrix INACTIVE gray — the custom on-color never
 * persists into the off state. The active set is per-domain, using
 * only states HA actually reports (never an invented state):
 * - binary toggleables (light/switch/input_boolean/fan): "on" only.
 * - vacuum: cleaning/returning only (docked/idle is its off state).
 * - lock: "locked" only (unlocked is the non-active state).
 * - cover: anything but "closed" (open/opening/closing are active).
 * - climate: anything but "off" (heat/cool/heat_cool/dry/fan_only/
 *   auto are all live modes; "off" is the off state).
 * - media/sensors/everything else: always active — media idle/
 *   paused keeps its row (standing rewrite judgment) and sensors
 *   have no off state to gray into (a motion sensor reading "off"
 *   is its normal state, not an off state).
 * - universal: blank/unavailable/unknown is inactive for every
 *   domain (no state, no color).
 * Callers apply this to the container AND the badge pair together;
 * when it returns false with a preset set, they render the matrix
 * INACTIVE row, not the matrix live row.
 */
fun presetAppliesForState(domain: String, state: String): Boolean {
    if (state.isBlank() || state == "unavailable" || state == "unknown") return false
    return when (domain) {
        "light", "switch", "input_boolean", "fan" -> state == "on"
        "vacuum" -> state == "cleaning" || state == "returning"
        "lock" -> state == "locked"
        "cover" -> state != "closed"
        "climate" -> state != "off"
        else -> true
    }
}

/**
 * Feedback round 2, item 2: the card switch follows the custom
 * theme too, with the same chromatic styling the badge icon
 * already uses (feedback round, item 5). Checked track = the
 * preset badge bg (dark tint of the assigned color), checked
 * thumb = the preset container; unchecked = the matrix inactive
 * pair (a preset card is gray when off, so its switch reads gray
 * too). Callers use this whenever a preset is set — the PillSwitch
 * checked boolean already selects the right side, so active and
 * inactive cards both read correctly through one colors object.
 */
fun presetSwitchColors(preset: TilePreset, isDark: Boolean): CardSwitchColors {
    val (badgeBg, _) = presetBadgeColors(preset, isDark)
    val container = tilePresetColor(preset, isDark)!!
    val inactiveTrack = if (isDark) Color(0xFF8C9188) else Color(0xFF52564E)
    val mutedThumb = if (isDark) Color(0xFF9E9E9E) else Color.White
    return CardSwitchColors(badgeBg, container, inactiveTrack, mutedThumb)
}

/** Pure RGB (0..1) -> HSV (h 0..360, s 0..1, v 0..1). */
fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> (60f * ((g - b) / delta) + 360f) % 360f
        max == g -> 60f * ((b - r) / delta) + 120f
        else -> 60f * ((r - g) / delta) + 240f
    }
    val s = if (max == 0f) 0f else delta / max
    return Triple(h, s, max)
}

/** Pure HSV -> Compose [Color] (opaque). */
fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f / 60f
    val c = v * s
    val x = c * (1f - kotlin.math.abs(hh % 2f - 1f))
    val m = v - c
    val (r, g, b) = when (hh.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

/** Pure RGB (0..1) -> HSL (h 0..360, s 0..1, l 0..1). */
fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val delta = max - min
    if (delta == 0f) return Triple(0f, 0f, l)
    val s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)
    val h = when (max) {
        r -> (60f * ((g - b) / delta) + 360f) % 360f
        g -> 60f * ((b - r) / delta) + 120f
        else -> 60f * ((r - g) / delta) + 240f
    }
    return Triple(h, s, l)
}

/** Pure HSL -> Compose [Color] (opaque). */
fun hslToColor(h: Float, s: Float, l: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f / 60f
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs(hh % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when (hh.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
