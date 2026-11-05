package com.garrietta.haven.data

import android.content.Context
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import kotlin.math.abs

/**
 * Feedback round 20, item 5: resizable cards. A card's size is one
 * of three grid-expressible spans — NORMAL (one column), WIDE (a
 * full row), or TALL (284dp: two 136dp rows plus one 12dp spacing,
 * round 24 item 6 — the old 272dp minimum never rendered because
 * the card's heightIn(min = 136dp) clobbered the wrapper's
 * fillMaxSize; TALL is now an exact height on the card itself).
 * A true multi-row tall span is NOT expressible in LazyVerticalGrid:
 * it has no row spanning, so a tall cell's row-mate stays
 * top-aligned at 136dp there. The manual row grids (Favorites,
 * RoomSectionGrid) DO pair a TALL card with two stacked NORMAL
 * cards in the adjacent column. A wide card never shifts its
 * neighbors' grid indices (which drive corner variants): indices
 * come from position in the card list, never from row layout.
 */
enum class CardSize { NORMAL, WIDE, TALL }

/**
 * Round 24, item 6: a TALL card is exactly two NORMAL rows plus
 * one grid spacing: 136dp * 2 + 12dp = 284dp. The two stacked
 * NORMAL cards beside it in a paired row measure 136 + 12 + 136
 * = 284dp, so the pair is flush. Pure, unit-tested.
 */
fun tallCardHeightDp(): androidx.compose.ui.unit.Dp = 284.dp

/**
 * Round 25, item 6: the TALL height for a paired row, spacing-
 * aware — 136dp * 2 + the grid's spacing. With the folded 12dp
 * spacing this is 284dp (== [tallCardHeightDp]); with the
 * unfolded right pane's 14dp spacing it is 286dp, so the TALL
 * card's edges align exactly with its two stacked NORMAL
 * neighbors (136 + 14 + 136). Pure, unit-tested.
 */
fun tallPairHeightDp(spacing: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    136.dp * 2 + spacing

/** Persistence key for one card placement: page scope + entity id. */
fun cardSizeKey(scope: String, entityId: String): String = "$scope:$entityId"

/**
 * Drag-vector snap for the resize handle. A mostly-horizontal
 * drag past the threshold resizes width (right = WIDE, left =
 * NORMAL); a mostly-vertical drag resizes height (down = TALL,
 * up = NORMAL). Below threshold the size is unchanged. Pure,
 * unit-tested.
 */
fun snapCardSize(current: CardSize, dxPx: Float, dyPx: Float, thresholdPx: Float = 24f): CardSize {
    if (abs(dxPx) < thresholdPx && abs(dyPx) < thresholdPx) return current
    return if (abs(dxPx) >= abs(dyPx)) {
        if (dxPx > 0) CardSize.WIDE else CardSize.NORMAL
    } else {
        if (dyPx > 0) CardSize.TALL else CardSize.NORMAL
    }
}

/**
 * Persists card sizes in SharedPreferences as one JSON object
 * mapping [cardSizeKey] to "wide"/"tall" (absent = normal).
 * Card sizes are the user's own configuration, not instance
 * data: they survive sign-out and reload, and a missing or
 * corrupt entry loads as empty, never a crash.
 */
class CardSizeStore(context: Context) {

    private val prefs = context.getSharedPreferences("haven_card_sizes", Context.MODE_PRIVATE)

    fun loadAll(): Map<String, CardSize> {
        val raw = prefs.getString("sizes_json", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, CardSize>()
            for (key in obj.keys()) {
                when (obj.optString(key)) {
                    "wide" -> out[key] = CardSize.WIDE
                    "tall" -> out[key] = CardSize.TALL
                }
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setSize(key: String, size: CardSize) {
        val all = loadAll().toMutableMap()
        if (size == CardSize.NORMAL) all.remove(key) else all[key] = size
        val obj = JSONObject()
        for ((k, v) in all) {
            obj.put(k, if (v == CardSize.WIDE) "wide" else "tall")
        }
        prefs.edit().putString("sizes_json", obj.toString()).apply()
    }
}
