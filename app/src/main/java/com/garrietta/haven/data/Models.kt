package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject

/** One Home Assistant entity: its id, current state, and attributes. */
data class HaEntity(
    val entityId: String,
    val state: String,
    val attributes: Map<String, Any?>,
    val lastChanged: String? = null,
    val lastUpdated: String? = null,
) {
    val domain: String get() = entityId.substringBefore(".")
    val friendlyName: String get() = (attributes["friendly_name"] as? String) ?: entityId

    fun attrString(key: String): String? = attributes[key] as? String
    fun attrDouble(key: String): Double? = (attributes[key] as? Number)?.toDouble()
    fun attrInt(key: String): Int? = (attributes[key] as? Number)?.toInt()
    fun attrBool(key: String): Boolean? = attributes[key] as? Boolean
    @Suppress("UNCHECKED_CAST")
    fun attrList(key: String): List<Any?>? = attributes[key] as? List<Any?>
    @Suppress("UNCHECKED_CAST")
    fun attrMap(key: String): Map<String, Any?>? = attributes[key] as? Map<String, Any?>

    companion object {
        fun fromJson(o: JSONObject): HaEntity {
            val attrs = mutableMapOf<String, Any?>()
            val ao = o.optJSONObject("attributes")
            if (ao != null) {
                val keys = ao.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    attrs[k] = jsonToAny(ao.get(k))
                }
            }
            return HaEntity(
                entityId = o.optString("entity_id"),
                state = o.optString("state"),
                attributes = attrs,
                lastChanged = o.optString("last_changed").ifEmpty { null },
                lastUpdated = o.optString("last_updated").ifEmpty { null },
            )
        }

        fun jsonToAny(v: Any?): Any? = when (v) {
            is JSONObject -> {
                val m = mutableMapOf<String, Any?>()
                val ks = v.keys()
                while (ks.hasNext()) {
                    val k = ks.next()
                    m[k] = jsonToAny(v.get(k))
                }
                m
            }
            is JSONArray -> (0 until v.length()).map { jsonToAny(v.get(it)) }
            JSONObject.NULL -> null
            else -> v
        }

        fun anyToJson(v: Any?): Any? = when (v) {
            null -> JSONObject.NULL
            is Map<*, *> -> {
                val o = JSONObject()
                for ((k, value) in v) o.put(k.toString(), anyToJson(value))
                o
            }
            is List<*> -> {
                val a = JSONArray()
                for (item in v) a.put(anyToJson(item))
                a
            }
            else -> v
        }
    }
}

/** An Area from the area registry. */
data class HaArea(
    val id: String,
    val name: String,
    val icon: String? = null,
)

/** One Lovelace view (tab) with its cards kept as raw JSON so the editor can round-trip them. */
data class LovelaceView(
    val title: String,
    val path: String?,
    val cards: List<JSONObject>,
)

data class LovelaceConfig(
    val title: String?,
    val views: List<LovelaceView>,
)

/**
 * One dashboard as listed by `lovelace/dashboards/list`. urlPath is null for
 * the default dashboard when it is not a named storage dashboard; mode is
 * "storage" or "yaml" when the instance reports it.
 */
data class HaDashboard(val urlPath: String?, val title: String?, val mode: String?)

/** Connection lifecycle shown in the UI. */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Authenticating : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/** One history point for sparklines (state as a number, epoch millis). */
data class HistoryPoint(val timeMs: Long, val value: Double)
