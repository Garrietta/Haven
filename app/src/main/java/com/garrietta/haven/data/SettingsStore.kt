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
}
