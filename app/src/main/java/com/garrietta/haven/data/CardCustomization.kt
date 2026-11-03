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
