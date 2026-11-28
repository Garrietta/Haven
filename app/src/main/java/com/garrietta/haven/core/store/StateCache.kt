package com.garrietta.haven.core.store

import android.content.Context
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.protocol.jsonObjectToMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Last-known states on disk so the app paints real data on the first frame
 * of a cold start; the live subscription replaces it within a second.
 * Camera access tokens are stripped (they rotate and must not persist).
 */
class StateCache(context: Context) {
    private val file = File(context.applicationContext.filesDir, "states_v2.json")

    fun load(): Map<String, HaEntity> = try {
        if (!file.isFile) emptyMap() else decode(file.readText())
    } catch (_: Exception) {
        emptyMap()
    }

    fun save(states: Map<String, HaEntity>) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(encode(states))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        } catch (_: Exception) {
        }
    }

    fun clear() { file.delete() }

    companion object {
        private val SECRET_ATTRS = setOf("access_token", "entity_picture")

        fun encode(states: Map<String, HaEntity>): String {
            val arr = JSONArray()
            for (e in states.values) {
                val attrs = JSONObject()
                for ((k, v) in e.attributes) if (k !in SECRET_ATTRS) attrs.put(k, toJson(v))
                arr.put(JSONObject().put("i", e.entityId).put("s", e.state).put("a", attrs).put("lc", e.lastChanged).put("lu", e.lastUpdated))
            }
            return arr.toString()
        }

        fun decode(text: String): Map<String, HaEntity> {
            val arr = JSONArray(text)
            val out = HashMap<String, HaEntity>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("i")
                out[id] = HaEntity(id, o.optString("s"), o.optJSONObject("a")?.let { jsonObjectToMap(it) } ?: emptyMap(), o.optDouble("lc", 0.0), o.optDouble("lu", 0.0))
            }
            return out
        }

        private fun toJson(v: Any?): Any? = when (v) {
            null -> JSONObject.NULL
            is Map<*, *> -> JSONObject().also { o -> v.forEach { (k, x) -> o.put(k.toString(), toJson(x)) } }
            is List<*> -> JSONArray().also { a -> v.forEach { a.put(toJson(it)) } }
            else -> v
        }
    }
}
