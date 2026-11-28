package com.garrietta.haven.core.protocol

import com.garrietta.haven.core.model.Area
import com.garrietta.haven.core.model.Device
import com.garrietta.haven.core.model.EntityEntry
import com.garrietta.haven.core.model.Floor
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject

/** JSON → plain Kotlin values (Map / List / String / Number / Boolean / null). */
fun jsonToKotlin(value: Any?): Any? = when (value) {
    null, JSONObject.NULL -> null
    is JSONObject -> jsonObjectToMap(value)
    is JSONArray -> (0 until value.length()).map { jsonToKotlin(value.opt(it)) }
    else -> value
}

fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
    val out = LinkedHashMap<String, Any?>(obj.length())
    val keys = obj.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        out[k] = jsonToKotlin(obj.opt(k))
    }
    return out
}

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).ifEmpty { null }

/**
 * Decoder for `subscribe_entities` — HA's compressed state stream (what the
 * web frontend uses instead of get_states + state_changed). Shapes, verified
 * against the live instance:
 *
 *  - add:    {"a": {"light.x": {"s": "on", "a": {...}, "c": "ctx" | {"id":…}, "lc": 1.7e9, "lu": 1.7e9}}}
 *  - change: {"c": {"light.x": {"+": {"s":…, "a": {changed attrs}, "lc":…, "lu":…, "c":…}, "-": {"a": ["removed_attr"]}}}}
 *  - remove: {"r": ["light.x"]}
 *
 * `lu` is omitted when equal to `lc`; a diff's `lc` implies `lu` too.
 */
object CompressedStates {

    fun apply(current: Map<String, HaEntity>, event: JSONObject): Map<String, HaEntity> {
        var next: MutableMap<String, HaEntity>? = null
        fun mutable(): MutableMap<String, HaEntity> = next ?: HashMap(current).also { next = it }

        event.optJSONObject("a")?.let { adds ->
            val keys = adds.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val e = adds.optJSONObject(id) ?: continue
                mutable()[id] = decodeFull(id, e)
            }
        }
        event.optJSONObject("c")?.let { changes ->
            val keys = changes.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val diff = changes.optJSONObject(id) ?: continue
                val base = (next ?: current)[id] ?: continue
                mutable()[id] = applyDiff(base, diff)
            }
        }
        event.optJSONArray("r")?.let { removed ->
            for (i in 0 until removed.length()) {
                val id = removed.optString(i)
                if (id.isNotEmpty()) mutable().remove(id)
            }
        }
        return next ?: current
    }

    fun decodeFull(id: String, e: JSONObject): HaEntity {
        val lc = e.optDouble("lc", 0.0)
        return HaEntity(
            entityId = id,
            state = e.optString("s"),
            attributes = e.optJSONObject("a")?.let { jsonObjectToMap(it) } ?: emptyMap(),
            lastChanged = lc,
            lastUpdated = if (e.has("lu")) e.optDouble("lu", lc) else lc,
            contextId = contextId(e.opt("c")),
        )
    }

    fun applyDiff(base: HaEntity, diff: JSONObject): HaEntity {
        var entity = base
        diff.optJSONObject("+")?.let { plus ->
            val attrs = plus.optJSONObject("a")
            val newAttrs = if (attrs != null) base.attributes + jsonObjectToMap(attrs) else base.attributes
            val lc = if (plus.has("lc")) plus.optDouble("lc") else null
            val lu = if (plus.has("lu")) plus.optDouble("lu") else lc
            entity = entity.copy(
                state = if (plus.has("s")) plus.optString("s") else entity.state,
                attributes = newAttrs,
                lastChanged = lc ?: entity.lastChanged,
                lastUpdated = lu ?: entity.lastUpdated,
                contextId = if (plus.has("c")) contextId(plus.opt("c")) else entity.contextId,
            )
        }
        diff.optJSONObject("-")?.optJSONArray("a")?.let { removed ->
            val keys = (0 until removed.length()).map { removed.optString(it) }.toSet()
            entity = entity.copy(attributes = entity.attributes.filterKeys { it !in keys })
        }
        return entity
    }

    private fun contextId(c: Any?): String? = when (c) {
        is String -> c.ifEmpty { null }
        is JSONObject -> c.optString("id").ifEmpty { null }
        else -> null
    }
}

object Registries {
    fun areas(arr: JSONArray?): List<Area> = buildList {
        if (arr == null) return@buildList
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            add(
                Area(
                    id = o.optString("area_id"),
                    name = o.optString("name"),
                    icon = o.str("icon"),
                    floorId = o.str("floor_id"),
                    picture = o.str("picture"),
                    temperatureEntityId = o.str("temperature_entity_id"),
                    humidityEntityId = o.str("humidity_entity_id"),
                ),
            )
        }
    }

    fun floors(arr: JSONArray?): List<Floor> = buildList {
        if (arr == null) return@buildList
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            add(Floor(o.optString("floor_id"), o.optString("name"), if (o.isNull("level")) null else o.optInt("level")))
        }
    }

    fun devices(arr: JSONArray?): List<Device> = buildList {
        if (arr == null) return@buildList
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            add(
                Device(
                    id = o.optString("id"),
                    name = o.str("name_by_user") ?: o.str("name") ?: "Device",
                    areaId = o.str("area_id"),
                    manufacturer = o.str("manufacturer"),
                    model = o.str("model"),
                    isService = o.str("entry_type") == "service",
                ),
            )
        }
    }

    /** `config/entity_registry/list_for_display` (compact keys ei/en/ai/di/pl/ec/hb/ic). */
    fun entitiesForDisplay(result: JSONObject?): List<EntityEntry> = buildList {
        if (result == null) return@buildList
        val cats = result.optJSONObject("entity_categories")
        val arr = result.optJSONArray("entities") ?: return@buildList
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val ec = if (o.has("ec")) cats?.optString(o.optInt("ec").toString())?.ifEmpty { null } else null
            add(
                EntityEntry(
                    entityId = o.optString("ei"),
                    name = o.str("en"),
                    areaId = o.str("ai"),
                    deviceId = o.str("di"),
                    platform = o.str("pl"),
                    category = ec,
                    hidden = o.optBoolean("hb", false),
                    icon = o.str("ic"),
                ),
            )
        }
    }

    fun config(o: JSONObject?): ServerConfig {
        if (o == null) return ServerConfig()
        return ServerConfig(
            locationName = o.str("location_name") ?: "Home",
            temperatureUnit = o.optJSONObject("unit_system")?.str("temperature") ?: "°C",
            version = o.optString("version"),
            timeZone = o.str("time_zone"),
        )
    }
}
