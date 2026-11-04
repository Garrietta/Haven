package com.garrietta.haven.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One user-created tab: a title and its cards, each card stored as raw YAML
 * text (HAOS-style) so the editor round-trips exactly what the user typed.
 * The YAML is parsed to JSON only at render time.
 * Stage C: [icon] is the picker's stored key, or null for automatic
 * (derived from the first card / title as before); [hidden] removes the
 * tab from the pill bar while keeping it listed in TabManager, so it can
 * always be shown again. Both fields are migration-safe: older JSON
 * without them loads as icon=null, hidden=false.
 * Feedback round 15, item 3: [pinned] places the tab ON the bar/rail
 * (between Energy and Settings) — at most two tabs may be pinned, a
 * cap enforced by the ViewModel setter, not by this store. Pinned
 * and hidden are independent: a pinned tab shows on the bar even
 * while hidden from the Browse list's bar section. Older JSON
 * without it loads as pinned=false.
 */
data class UserTab(
    val id: String,
    val title: String,
    val cards: List<String>,
    val icon: String? = null,
    val hidden: Boolean = false,
    val pinned: Boolean = false,
)

/**
 * Persists user tabs in SharedPreferences as one JSON array. Tabs are the
 * user's own configuration, not instance data: they survive sign-out and
 * reload, and nothing here ever assumes a tab exists — a missing or corrupt
 * entry loads as an empty list, never a crash.
 */
class TabsStore(context: Context) {

    private val prefs = context.getSharedPreferences("haven_tabs", Context.MODE_PRIVATE)

    /** True once the first-run starter tabs have been considered (seeded or deliberately skipped). */
    var seeded: Boolean
        get() = prefs.getBoolean("seeded", false)
        set(value) = prefs.edit().putBoolean("seeded", value).apply()

    /** True once the Cameras tab has been curated to the 4-camera set (or confirmed absent). */
    var camerasCurated: Boolean
        get() = prefs.getBoolean("cameras_curated", false)
        set(value) = prefs.edit().putBoolean("cameras_curated", value).apply()

    fun loadTabs(): List<UserTab> {
        val raw = prefs.getString("tabs_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<UserTab>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isEmpty()) continue
                val cardsArr = obj.optJSONArray("cards") ?: JSONArray()
                val cards = mutableListOf<String>()
                for (j in 0 until cardsArr.length()) {
                    cardsArr.optString(j).ifEmpty { null }?.let { cards.add(it) }
                }
                out.add(
                    UserTab(
                        id,
                        obj.optString("title").ifEmpty { "Tab" },
                        cards,
                        icon = obj.optString("icon").ifEmpty { null },
                        hidden = obj.optBoolean("hidden"),
                        pinned = obj.optBoolean("pinned"),
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTabs(tabs: List<UserTab>) {
        val arr = JSONArray()
        for (tab in tabs) {
            val cardsArr = JSONArray()
            for (card in tab.cards) cardsArr.put(card)
            val obj = JSONObject()
                .put("id", tab.id)
                .put("title", tab.title)
                .put("cards", cardsArr)
                .put("hidden", tab.hidden)
                .put("pinned", tab.pinned)
            // Icon keys are written only when set, so older readers
            // (and older JSON) keep seeing the auto-derived icon.
            if (tab.icon != null) obj.put("icon", tab.icon)
            arr.put(obj)
        }
        prefs.edit().putString("tabs_json", arr.toString()).apply()
    }
}
