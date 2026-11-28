package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * How one section of a tab (Security's cameras, Climate's readings) has
 * been arranged by hand: what was taken off it, what was put on it that
 * Home Assistant would not have put there, and the order. Everything a
 * section shows by default and nobody has touched stays, in its own
 * order, after whatever has been placed.
 */
data class PageCuration(
    val hidden: Set<String> = emptySet(),
    val added: List<String> = emptyList(),
    val order: List<String> = emptyList(),
) {
    /** Puts [id] on the section (bringing it back if it had been taken off). */
    fun add(id: String): PageCuration = copy(added = (added + id).distinct(), hidden = hidden - id)

    /** Takes [id] off: one that was added is forgotten, one that is there by default is hidden. */
    fun remove(id: String): PageCuration =
        if (id in added) copy(added = added - id, order = order - id) else copy(hidden = hidden + id)

    /** Brings back an item that was taken off. */
    fun show(id: String): PageCuration = copy(hidden = hidden - id)

    /** The shown items' order after the one at [from] is dropped at [to]. */
    fun moved(shown: List<String>, from: Int, to: Int): PageCuration {
        if (from !in shown.indices || to !in shown.indices) return this
        val list = shown.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(order = list)
    }

    fun toJson(): String = JSONObject()
        .put("hidden", JSONArray(hidden.toList()))
        .put("added", JSONArray(added))
        .put("order", JSONArray(order))
        .toString()

    companion object {
        fun fromJson(text: String?): PageCuration {
            if (text.isNullOrBlank()) return PageCuration()
            return runCatching {
                val o = JSONObject(text)
                fun list(key: String) = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
                PageCuration(list("hidden").toSet(), list("added"), list("order"))
            }.getOrDefault(PageCuration())
        }
    }
}

/**
 * What a section shows: its defaults and what was added (those that
 * still exist), less what was taken off, in the arranged order, the
 * unarranged after it in their own order.
 */
fun curated(defaults: List<String>, curation: PageCuration, exists: (String) -> Boolean = { true }): List<String> {
    val all = (defaults + curation.added.filter(exists)).distinct().filterNot { it in curation.hidden }
    val position = curation.order.withIndex().associate { (index, id) -> id to index }
    return all.sortedBy { position[it] ?: Int.MAX_VALUE }
}

/** What was taken off a section and can be brought back. */
fun takenOff(defaults: List<String>, curation: PageCuration): List<String> =
    (defaults + curation.added).distinct().filter { it in curation.hidden }

/** Which Security section an added entity joins: a camera, a detection switch, or doors and locks. */
fun securitySectionFor(entityId: String): String = when (entityId.substringBefore('.')) {
    "camera" -> SECURITY_CAMERAS
    "switch", "input_boolean" -> SECURITY_DETECTION
    else -> SECURITY_DOORS
}

/** Which Climate section an added entity joins: a thermostat, or a reading. */
fun climateSectionFor(entityId: String): String =
    if (entityId.startsWith("climate.")) CLIMATE_THERMOSTATS else CLIMATE_READINGS

const val SECURITY_CAMERAS = "security.cameras"
const val SECURITY_DOORS = "security.doors"
const val SECURITY_DETECTION = "security.detection"
const val CLIMATE_THERMOSTATS = "climate.thermostats"
const val CLIMATE_READINGS = "climate.readings"
