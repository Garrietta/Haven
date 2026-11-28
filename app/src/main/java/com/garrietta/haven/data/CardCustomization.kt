package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors

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
/**
 * A tile color the user can pin to a device or a whole domain. A preset
 * is a state family by another name, so a pinned tile is drawn with
 * exactly the recipe an automatic one gets: same tones, same badge, same
 * switch. The keys are stored in settings and are frozen.
 */
data class TilePreset(val key: String, val label: String, val family: StateFamily) {
    val light: Color get() = family.colors(isDark = false).container
    val dark: Color get() = family.colors(isDark = true).container
}

val TILE_PRESETS: List<TilePreset> = listOf(
    TilePreset("lime", "Lime", StateFamily.On),
    TilePreset("heating", "Heating coral", StateFamily.Heat),
    TilePreset("cooling", "Cooling blue", StateFamily.Cool),
    TilePreset("lock", "Lock indigo", StateFamily.Secure),
    TilePreset("alert", "Alert red", StateFamily.Alert),
    TilePreset("media", "Media lavender", StateFamily.Media),
    TilePreset("amber", "Amber", StateFamily.Power),
    TilePreset("sage", "Sage neutral", StateFamily.Quiet),
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

/**
 * Tile preset for an entity, or null for "default" (unknown keys read as default, never crash).
 * Feedback round 15, item 5: resolution is per-entity first, then the DOMAIN default
 * ([domainPresets] keyed by domain, e.g. "light" covers light.lamp_socket_1 — a socket-named
 * lamp is still a light), then default. Callers that do not know the domain pass nothing and
 * keep the old per-entity-only behavior.
 */
fun tilePresetFor(
    presets: Map<String, String>,
    entityId: String,
    domainPresets: Map<String, String> = emptyMap(),
    domain: String? = null,
): TilePreset? {
    val key = presets[entityId] ?: domain?.let { domainPresets[it] } ?: return null
    return TILE_PRESETS.firstOrNull { it.key == key }
}

/** The preset's container color for the current theme, or null for default. */
fun tilePresetColor(preset: TilePreset?, isDark: Boolean): Color? =
    preset?.let { if (isDark) it.dark else it.light }

/**
 * The badge behind a pinned tile's icon, and the icon: the preset
 * family's own accent pair, so a yellow tile gets a yellow-tinted badge
 * and never the default green.
 */
fun presetBadgeColors(preset: TilePreset, isDark: Boolean): Pair<Color, Color> =
    preset.family.colors(isDark).let { it.accent to it.onAccent }

/**
 * Feedback round 4, item 2: the detail/sheet/traveler badge pair
 * for an entity with a custom tile preset — the SAME pair the
 * dashboard card renders (presetBadgeColors), gated by the
 * round-2 active rules, so a detail panel, long-press sheet, or
 * traveling badge never reverts to the matrix/default green
 * while the card shows the assigned color. Null when no preset
 * is set or the entity is not in its active state (callers then
 * keep their existing matrix/default colors, exactly as an
 * inactive card renders inactive gray).
 */
fun detailBadgeOverride(
    presets: Map<String, String>,
    entity: HaEntity,
    isDark: Boolean,
    domainPresets: Map<String, String> = emptyMap(),
): Pair<Color, Color>? {
    val preset = tilePresetFor(presets, entity.entityId, domainPresets, entity.domain) ?: return null
    if (!presetAppliesForState(entity.domain, entity.state)) return null
    return presetBadgeColors(preset, isDark)
}

/**
 * Feedback round 5, items 2 + 4: the assigned tile color itself — the preset's container
 * (pastel in light theme, deep tone in dark) — for an entity with a custom tile preset.
 * Item 2: this SUPERSEDES round 4's judgment call that set the detail big-toggle track to the
 * preset badge bg (dark tonal ink): on video the user corrected that the toggle BACKGROUND
 * should be the assigned color (Standing Lamp's track yellow, like its card), while the knob
 * stays white. Note the knob-contrast consequence the user accepted: the big toggle's knob is
 * hardcoded white, so a pastel track + white knob is lower contrast than the old dark track —
 * the disc edge is still visible (white vs pastel luminance differ), and the track color is
 * what he asked to see.
 *
 * GATING — Round 21, item 4 (SUPERSEDES the round-5 divergence
 * below it): this accent IS gated by [presetAppliesForState] now,
 * exactly like [detailBadgeOverride] and the tile itself. The
 * round-5 rule made the popup follow the preset even while the
 * entity was inactive — so a CLOSED garage door (blue preset,
 * inactive on the tile) showed a BLUE panel under a GREEN tile,
 * and the user filmed it as a bug: "the green color on the tile
 * isn't carrying over here to the control scheme." The panel must
 * show the color the tile shows: preset while the preset applies,
 * matrix/scheme defaults otherwise (the garage pills fall back to
 * secondaryContainer and the door to primary — both green in the
 * matrix family, matching the tile's inactive row). The tile and
 * the popup can never disagree about the entity's color again.
 * Null when no preset is set (never invent color where the user
 * chose none) AND when the preset doesn't apply for the current
 * state; callers then keep defaults.
 * (Toggle off-state still reads gray: VerticalPillToggle/PillToggle use surfaceVariant when
 * unchecked, whatever accent is passed.)
 */
fun detailPageAccent(
    presets: Map<String, String>,
    entity: HaEntity,
    isDark: Boolean,
    domainPresets: Map<String, String> = emptyMap(),
): Color? {
    val preset = tilePresetFor(presets, entity.entityId, domainPresets, entity.domain) ?: return null
    if (!presetAppliesForState(entity.domain, entity.state)) return null
    return tilePresetColor(preset, isDark)
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
    return CardSwitchColors(badgeBg, container, restingSwitchTrack(isDark), restingSwitchThumb(isDark))
}
