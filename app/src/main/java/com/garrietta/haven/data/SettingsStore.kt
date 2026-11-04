package com.garrietta.haven.data

import android.content.Context

/** Plain (non-secret) user preferences. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("haven_settings", Context.MODE_PRIVATE)

    /** "system", "light", or "dark". */
    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    /**
     * Stage B: Android dynamic (wallpaper) color. OPTIONAL and
     * DISABLED BY DEFAULT — the fixed Haven palettes are the
     * identity; dynamic color must never override them unless
     * the user explicitly turns this on.
     */
    var dynamicColor: Boolean
        get() = prefs.getBoolean("dynamic_color", false)
        set(value) = prefs.edit().putBoolean("dynamic_color", value).apply()

    /**
     * Stage C: folded pill presentation style — "floating" (the current
     * 64dp bar) or "compact" (a tighter 56dp bar). Unknown values read
     * as floating (see navStyleFromKey); the key is stored verbatim.
     */
    var navStyle: String
        get() = prefs.getString("nav_style", "floating") ?: "floating"
        set(value) = prefs.edit().putString("nav_style", value).apply()

    /**
     * Stage E §9: condition-badge visibility. Navigation dot (Home tab)
     * and room-heading dots, each on by default. Device badges are the
     * tiles' own state badges (always on — they carry tap actions, so
     * they are not a visibility preference).
     */
    var badgeNav: Boolean
        get() = prefs.getBoolean("badge_nav", true)
        set(value) = prefs.edit().putBoolean("badge_nav", value).apply()

    var badgeRoom: Boolean
        get() = prefs.getBoolean("badge_room", true)
        set(value) = prefs.edit().putBoolean("badge_room", value).apply()

    /** When on, spring animations are replaced with instant state changes. */
    var reduceMotion: Boolean
        get() = prefs.getBoolean("reduce_motion", false)
        set(value) = prefs.edit().putBoolean("reduce_motion", value).apply()

    // ---- Round 6: thermostat display-range override ----
    // Explicit dial-axis ends (°F) for the thermostat popout,
    // sheet dial, and detail. Null = unset = Automatic: the
    // decade formula in thermostatDisplayRange governs. The
    // override is display-range only — it never touches live
    // setpoints. Presence (contains) distinguishes "unset" from a
    // stored value; the resolver clamps to [40, 100] and drops
    // the pair if clamping would invert it, so the stored value
    // itself needs no validation here beyond whole degrees.
    /** Explicit thermostat dial-axis minimum, or null = Automatic. */
    var thermostatDisplayMin: Float?
        get() = if (prefs.contains("thermostat_display_min")) {
            prefs.getFloat("thermostat_display_min", 50f)
        } else {
            null
        }
        set(value) = if (value == null) {
            prefs.edit().remove("thermostat_display_min").apply()
        } else {
            prefs.edit().putFloat("thermostat_display_min", value).apply()
        }

    /** Explicit thermostat dial-axis maximum, or null = Automatic. */
    var thermostatDisplayMax: Float?
        get() = if (prefs.contains("thermostat_display_max")) {
            prefs.getFloat("thermostat_display_max", 80f)
        } else {
            null
        }
        set(value) = if (value == null) {
            prefs.edit().remove("thermostat_display_max").apply()
        } else {
            prefs.edit().putFloat("thermostat_display_max", value).apply()
        }

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

    /**
     * Keys of the Home sections (see HOME_SECTIONS) the user hid.
     * UI rewrite round §4 defaults guard: when the key has NEVER been
     * stored, the fresh-state default ([DEFAULT_HIDDEN_HOME_SECTIONS]:
     * Greeting, Cameras, Home Status, Favorites shown) applies — so a
     * fresh install matches the spec without wiping anyone's stored
     * choice. A stored set, even an empty one (the user explicitly
     * showed everything), is always returned as stored.
     */
    var homeHiddenSections: Set<String>
        get() = if (prefs.contains("home_hidden_sections")) {
            prefs.getStringSet("home_hidden_sections", emptySet())?.toSet() ?: emptySet()
        } else {
            DEFAULT_HIDDEN_HOME_SECTIONS
        }
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

    /**
     * Stage D: the Favorites section — an ORDERED list of entity ids,
     * stored as one JSON array like tabs_json. Corrupt or missing data
     * loads as empty, never a crash. Ids are kept even when their entity
     * is currently absent from states (an entity can go unavailable
     * temporarily); the section renders only ids that resolve, while the
     * Manage sheet lists every stored id so a stale one can be removed.
     */
    fun favoriteEntityIds(): List<String> {
        val raw = prefs.getString("favorites_json", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                arr.optString(i).ifEmpty { null }?.let { out.add(it) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setFavoriteEntityIds(ids: List<String>) {
        val arr = org.json.JSONArray()
        for (id in ids) arr.put(id)
        prefs.edit().putString("favorites_json", arr.toString()).apply()
    }

    // ---- Stage G: dashboard arrangement + room presentation ----

    /** Ordered Home section keys (see HOME_SECTION_ORDER_DEFAULT); corrupt data loads as empty (resolves to canonical). */
    fun homeSectionOrder(): List<String> {
        val raw = prefs.getString("home_section_order_json", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                arr.optString(i).ifEmpty { null }?.let { out.add(it) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setHomeSectionOrder(keys: List<String>) {
        val arr = org.json.JSONArray()
        for (key in keys) arr.put(key)
        prefs.edit().putString("home_section_order_json", arr.toString()).apply()
    }

    /** Per-area display-name overrides (presentation only — area IDs never change). */
    fun roomDisplayNames(): Map<String, String> {
        val raw = prefs.getString("room_display_names_json", null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                obj.optString(key).ifEmpty { null }?.let { out[key] = it }
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setRoomDisplayName(areaId: String, name: String?) {
        val obj = org.json.JSONObject()
        for ((id, value) in roomDisplayNames()) obj.put(id, value)
        if (name.isNullOrBlank()) obj.remove(areaId) else obj.put(areaId, name)
        prefs.edit().putString("room_display_names_json", obj.toString()).apply()
    }

    /** Per-area icon keys (TAB_ICON_OPTIONS keys; presentation only). */
    fun roomIconKeys(): Map<String, String> {
        val raw = prefs.getString("room_icon_keys_json", null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                obj.optString(key).ifEmpty { null }?.let { out[key] = it }
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setRoomIconKey(areaId: String, iconKey: String?) {
        val obj = org.json.JSONObject()
        for ((id, value) in roomIconKeys()) obj.put(id, value)
        if (iconKey == null) obj.remove(areaId) else obj.put(areaId, iconKey)
        prefs.edit().putString("room_icon_keys_json", obj.toString()).apply()
    }

    /** Ordered area ids for Home room sections (presentation only). */
    fun roomOrder(): List<String> {
        val raw = prefs.getString("room_order_json", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                arr.optString(i).ifEmpty { null }?.let { out.add(it) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setRoomOrder(areaIds: List<String>) {
        val arr = org.json.JSONArray()
        for (id in areaIds) arr.put(id)
        prefs.edit().putString("room_order_json", arr.toString()).apply()
    }

    // ---- Shape picker (Settings -> Shapes, per-entity detail) ----

    /** Global default badge shape key, or null = the domain default. */
    var globalBadgeShape: String?
        get() = prefs.getString("shape_global_badge", null)
        set(value) = prefs.edit().putString("shape_global_badge", value).apply()

    /** Global default card-outline shape key, or null = the default corners. */
    var globalCardShape: String?
        get() = prefs.getString("shape_global_card", null)
        set(value) = prefs.edit().putString("shape_global_card", value).apply()

    private fun shapeOverrides(key: String): Map<String, String> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val keyName = keys.next()
                out[keyName] = obj.optString(keyName)
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun setShapeOverride(storageKey: String, current: Map<String, String>, entityId: String, shapeKey: String?) {
        val obj = org.json.JSONObject()
        for ((id, value) in current) obj.put(id, value)
        if (shapeKey == null) obj.remove(entityId) else obj.put(entityId, shapeKey)
        prefs.edit().putString(storageKey, obj.toString()).apply()
    }

    /** Per-entity badge shape overrides (entity id -> shape key). */
    fun badgeShapeOverrides(): Map<String, String> = shapeOverrides("shape_badge_overrides_json")

    /** Records or clears one per-entity badge shape override; null clears back to the default. */
    fun setBadgeShapeOverride(entityId: String, shapeKey: String?) =
        setShapeOverride("shape_badge_overrides_json", badgeShapeOverrides(), entityId, shapeKey)

    /** Per-entity card-outline shape overrides (entity id -> shape key). */
    fun cardShapeOverrides(): Map<String, String> = shapeOverrides("shape_card_overrides_json")

    // ---- Per-entity card customization (customization round, item 2) ----
    // Same JSON-object pattern as the shape overrides: corrupt data
    // loads as empty, never a crash; a null value clears the override.

    private fun stringMap(key: String): Map<String, String> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val keyName = keys.next()
                obj.optString(keyName).ifEmpty { null }?.let { out[keyName] = it }
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun setStringMap(storageKey: String, current: Map<String, String>, entityId: String, value: String?) {
        val obj = org.json.JSONObject()
        for ((id, v) in current) obj.put(id, v)
        if (value == null) obj.remove(entityId) else obj.put(entityId, value)
        prefs.edit().putString(storageKey, obj.toString()).apply()
    }

    /** Per-entity display titles (entity id -> custom title). */
    fun customTitles(): Map<String, String> = stringMap("card_custom_titles_json")
    fun setCustomTitle(entityId: String, title: String?) =
        setStringMap("card_custom_titles_json", customTitles(), entityId, title?.takeIf { it.isNotBlank() })

    /** Per-entity icon keys (entity id -> CARD_ICON_OPTIONS key). */
    fun customIconKeys(): Map<String, String> = stringMap("card_custom_icons_json")
    fun setCustomIconKey(entityId: String, iconKey: String?) =
        setStringMap("card_custom_icons_json", customIconKeys(), entityId, iconKey)

    /** Per-entity tile-color preset keys (entity id -> TILE_PRESETS key). */
    fun customTilePresets(): Map<String, String> = stringMap("card_custom_tiles_json")
    fun setCustomTilePreset(entityId: String, presetKey: String?) =
        setStringMap("card_custom_tiles_json", customTilePresets(), entityId, presetKey)

    /** Records or clears one per-entity card-outline shape override; null clears back to the default. */
    fun setCardShapeOverride(entityId: String, shapeKey: String?) =
        setShapeOverride("shape_card_overrides_json", cardShapeOverrides(), entityId, shapeKey)
}
