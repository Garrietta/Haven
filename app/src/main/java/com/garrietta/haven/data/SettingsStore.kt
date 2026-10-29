package com.garrietta.haven.data

import android.content.Context

/** Plain (non-secret) user preferences. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("haven_settings", Context.MODE_PRIVATE)

    /** "system", "light", or "dark". */
    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    /** When on, spring animations are replaced with instant state changes. */
    var reduceMotion: Boolean
        get() = prefs.getBoolean("reduce_motion", false)
        set(value) = prefs.edit().putBoolean("reduce_motion", value).apply()

    /** The dashboard url_path last loaded (null = the default dashboard, sent with no url_path). */
    var selectedDashboardPath: String?
        get() = prefs.getString("selected_dashboard_path", null)
        set(value) = prefs.edit().putString("selected_dashboard_path", value).apply()

    /** True once a dashboard choice has been recorded; distinguishes "default dashboard chosen" from "never chosen". */
    var hasSelectedDashboard: Boolean
        get() = prefs.getBoolean("has_selected_dashboard", false)
        set(value) = prefs.edit().putBoolean("has_selected_dashboard", value).apply()

    // ---- Home page customization (Settings -> Manage home page) ----

    /** Custom welcome message shown on Home in place of the greeting. Empty = the automatic time-based greeting. */
    var homeWelcomeMessage: String
        get() = prefs.getString("home_welcome_message", "") ?: ""
        set(value) = prefs.edit().putString("home_welcome_message", value).apply()

    /** Keys of the Home sections (see HOME_SECTIONS) the user hid. Empty = show everything. */
    var homeHiddenSections: Set<String>
        get() = prefs.getStringSet("home_hidden_sections", emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet("home_hidden_sections", value.toSet()).apply()

    /** Explicit per-light Home visibility choices (entity id -> shown), as one JSON object string. */
    fun homeLightOverrides(): Map<String, Boolean> {
        val raw = prefs.getString("home_light_overrides_json", null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val out = mutableMapOf<String, Boolean>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                out[key] = obj.optBoolean(key)
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Records one explicit per-light choice; it persists and always beats the default exclusion rule. */
    fun setHomeLightOverride(entityId: String, shown: Boolean) {
        val obj = org.json.JSONObject()
        for ((id, value) in homeLightOverrides()) obj.put(id, value)
        obj.put(entityId, shown)
        prefs.edit().putString("home_light_overrides_json", obj.toString()).apply()
    }
}
