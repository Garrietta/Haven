package com.garrietta.haven.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One user-created tab: a title and its cards, each card stored as raw YAML
 * text (HAOS-style) so the editor round-trips exactly what the user typed.
 * The YAML is parsed to JSON only at render time.
 */
data class UserTab(val id: String, val title: String, val cards: List<String>)

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
                out.add(UserTab(id, obj.optString("title").ifEmpty { "Tab" }, cards))
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
            arr.put(JSONObject().put("id", tab.id).put("title", tab.title).put("cards", cardsArr))
        }
        prefs.edit().putString("tabs_json", arr.toString()).apply()
    }
}
