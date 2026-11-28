package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The cold-start disk cache (filesDir/haven_state_cache.json): the
 * latest states, areas and the parts of the device registry Home sorts
 * by, loaded before the first frame so Home draws the house as it was
 * last seen while the socket connects behind it. Without the registry
 * part, Home could not tell a device from a helper until it arrived.
 *
 * Pure serialize/parse (no Android runtime) so the round trip is
 * unit-tested; the ViewModel owns the file IO, best effort. Device
 * manufacturers are left out: they come from the live registry, and a
 * stale one must not outlive it.
 */
data class StateCacheData(
    val states: Map<String, HaEntity>,
    val areas: List<HaArea>,
    val entityAreas: Map<String, String>,
    val entityDevices: Map<String, String> = emptyMap(),
    val deviceInfo: Map<String, HaDeviceInfo> = emptyMap(),
    val auxiliary: Set<String> = emptySet(),
)

/** Serializes the cached parts to one JSON document. */
fun serializeStateCache(
    states: Map<String, HaEntity>,
    areas: List<HaArea>,
    entityAreas: Map<String, String>,
    entityDevices: Map<String, String> = emptyMap(),
    deviceInfo: Map<String, HaDeviceInfo> = emptyMap(),
    auxiliary: Set<String> = emptySet(),
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
    val entityDevicesObj = JSONObject()
    for ((entityId, deviceId) in entityDevices) entityDevicesObj.put(entityId, deviceId)
    root.put("entityDevices", entityDevicesObj)
    val devicesObj = JSONObject()
    for ((id, d) in deviceInfo) {
        val o = JSONObject().put("name", d.name).put("service", d.service)
        d.model?.let { o.put("model", it) }
        devicesObj.put(id, o)
    }
    root.put("devices", devicesObj)
    root.put("auxiliary", JSONArray(auxiliary.toList()))
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
        val entityDevices = mutableMapOf<String, String>()
        root.optJSONObject("entityDevices")?.let { o ->
            for (k in o.keys()) o.optString(k).takeIf { it.isNotEmpty() }?.let { entityDevices[k] = it }
        }
        val deviceInfo = mutableMapOf<String, HaDeviceInfo>()
        root.optJSONObject("devices")?.let { o ->
            for (k in o.keys()) {
                val d = o.optJSONObject(k) ?: continue
                val name = d.optString("name")
                if (name.isEmpty()) continue
                deviceInfo[k] = HaDeviceInfo(name, d.optString("model").ifEmpty { null }, null, d.optBoolean("service"))
            }
        }
        val auxiliary = mutableSetOf<String>()
        root.optJSONArray("auxiliary")?.let { a -> for (i in 0 until a.length()) a.optString(i).takeIf { it.isNotEmpty() }?.let(auxiliary::add) }
        StateCacheData(states, areas, entityAreas, entityDevices, deviceInfo, auxiliary)
    } catch (_: Exception) {
        null
    }
}
