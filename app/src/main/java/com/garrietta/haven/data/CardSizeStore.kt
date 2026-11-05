package com.garrietta.haven.data

import android.content.Context
import org.json.JSONObject
import kotlin.math.abs

/**
 * Feedback round 20, item 5: resizable cards. A card's size is one
 * of three grid-expressible spans — NORMAL (one column), WIDE (a
 * full row), or TALL (the 136dp floor doubled to 272dp minimum).
 * A true multi-row tall span is NOT expressible: LazyVerticalGrid
 * has no row spanning, and the manual row grids size rows by
 * their tallest card — so TALL is a minimum height, never a row
 * span, and a tall card's row-mate stays top-aligned at 136dp.
 * A wide card never shifts its neighbors' grid indices (which
 * drive corner variants): indices come from position in the
 * card list, never from row layout.
 */
enum class CardSize { NORMAL, WIDE, TALL }

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
