package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Round 25, item 2: the cold-start disk cache — the latest states
 * JSON array, areas, and entityAreas, persisted to
 * filesDir/haven_state_cache.json whenever get_states or registry
 * fetches succeed and loaded synchronously in HavenViewModel.init
 * before first composition, so TodayScreen has cached entities,
 * heroCameras resolves the 4 camera IDs, and CameraQuadrant paints
 * the cached disk JPEGs on frame 0 with no green-placeholder flash.
 *
 * Pure serialize/parse (no Android runtime) so the round-trip is
 * unit-tested; the ViewModel owns the file IO, best effort. The
 * manufacturer field is deliberately NOT serialized — it is patched
 * in from the device registry after connect, and a stale
 * manufacturer must never outlive the registry that supplied it.
 */
data class StateCacheData(
    val states: Map<String, HaEntity>,
    val areas: List<HaArea>,
    val entityAreas: Map<String, String>,
)

/** Serializes the three cached parts to one JSON document. */
fun serializeStateCache(
    states: Map<String, HaEntity>,
    areas: List<HaArea>,
    entityAreas: Map<String, String>,
): String {
    val root = JSONObject()
    val statesArray = JSONArray()
    for (entity in states.values) {
        val o = JSONObject()
        o.put("entity_id", entity.entityId)
        o.put("state", entity.state)
        o.put("attributes", HaEntity.anyToJson(entity.attributes))
        entity.lastChanged?.let { o.put("last_changed", it) }
        entity.lastUpdated?.let { o.put("last_updated", it) }
        statesArray.put(o)
    }
    root.put("states", statesArray)
    val areasArray = JSONArray()
    for (area in areas) {
        val o = JSONObject().put("id", area.id).put("name", area.name)
        area.icon?.let { o.put("icon", it) }
        areasArray.put(o)
    }
    root.put("areas", areasArray)
    val entityAreasObj = JSONObject()
    for ((entityId, areaId) in entityAreas) entityAreasObj.put(entityId, areaId)
    root.put("entityAreas", entityAreasObj)
    return root.toString()
}

/**
 * Parses a cache document. Returns null for corrupt or truncated
 * input (never throws) — the caller treats null as "no cache" and
 * starts from the empty live flows.
 */
fun parseStateCache(text: String): StateCacheData? {
    return try {
        val root = JSONObject(text)
        val states = mutableMapOf<String, HaEntity>()
        val statesArray = root.optJSONArray("states") ?: return null
        for (i in 0 until statesArray.length()) {
            val o = statesArray.optJSONObject(i) ?: continue
            val entity = HaEntity.fromJson(o)
            if (entity.entityId.isNotEmpty()) states[entity.entityId] = entity
        }
        val areas = mutableListOf<HaArea>()
        val areasArray = root.optJSONArray("areas")
        if (areasArray != null) {
            for (i in 0 until areasArray.length()) {
                val o = areasArray.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isEmpty()) continue
                areas.add(HaArea(id, o.optString("name"), o.optString("icon").ifEmpty { null }))
            }
        }
        val entityAreas = mutableMapOf<String, String>()
        val entityAreasObj = root.optJSONObject("entityAreas")
        if (entityAreasObj != null) {
            val keys = entityAreasObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = entityAreasObj.optString(k)
                if (v.isNotEmpty()) entityAreas[k] = v
            }
        }
        StateCacheData(states, areas, entityAreas)
    } catch (_: Exception) {
        null
    }
}
