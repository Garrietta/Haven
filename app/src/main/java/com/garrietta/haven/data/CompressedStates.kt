package com.garrietta.haven.data

import org.json.JSONObject

/**
 * Backend round (from the 2.0 rewrite): decoder for `subscribe_entities`,
 * HA's compressed state stream — what the web frontend uses instead of
 * `get_states` + `subscribe_events state_changed`. One subscription delivers
 * the full snapshot first (`a`), then small diffs (`c`) and removals (`r`),
 * so the initial load is a fraction of the bytes and every update carries
 * only what changed. Shapes (verified against the live instance):
 *
 *  - add:    {"a": {"light.x": {"s": "on", "a": {...}, "c": "ctx" | {"id","parent_id","user_id"}, "lc": 1.7e9, "lu": 1.7e9}}}
 *  - change: {"c": {"light.x": {"+": {"s":…, "a": {changed attrs}, "lc":…, "lu":…, "c":…}, "-": {"a": ["removed_attr"]}}}}
 *  - remove: {"r": ["light.x"]}
 *
 * `lu` is omitted when equal to `lc`. Timestamps are epoch seconds; they
 * are converted to ISO-8601 strings because the rest of 1.x reads
 * last_changed / last_updated as strings. Pure, unit tested.
 */
object CompressedStateStream {

    /** One applied entity change, for the notification listener. */
    data class Change(val entityId: String, val old: HaEntity?, val new: HaEntity?, val parentId: String?)

    data class Result(val states: Map<String, HaEntity>, val changes: List<Change>)

    fun apply(current: Map<String, HaEntity>, event: JSONObject): Result {
        var next: MutableMap<String, HaEntity>? = null
        val changes = mutableListOf<Change>()
        fun mutable(): MutableMap<String, HaEntity> = next ?: HashMap(current).also { next = it }

        event.optJSONObject("a")?.let { adds ->
            val keys = adds.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val e = adds.optJSONObject(id) ?: continue
                val entity = decodeFull(id, e)
                val old = (next ?: current)[id]
                mutable()[id] = entity
                changes += Change(id, old, entity, parentId(e.opt("c")))
            }
        }
        event.optJSONObject("c")?.let { diffs ->
            val keys = diffs.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val diff = diffs.optJSONObject(id) ?: continue
                val old = (next ?: current)[id] ?: continue
                val entity = applyDiff(old, diff)
                mutable()[id] = entity
                changes += Change(id, old, entity, parentId(diff.optJSONObject("+")?.opt("c")))
            }
        }
        event.optJSONArray("r")?.let { removed ->
            for (i in 0 until removed.length()) {
                val id = removed.optString(i)
                val old = mutable().remove(id) ?: continue
                changes += Change(id, old, null, null)
            }
        }
        return Result(next ?: current, changes)
    }

    fun decodeFull(id: String, e: JSONObject): HaEntity {
        val lc = e.optDouble("lc", Double.NaN)
        val lu = if (e.has("lu")) e.optDouble("lu", lc) else lc
        return HaEntity(
            entityId = id,
            state = e.optString("s"),
            attributes = e.optJSONObject("a")?.let { attrs(it) } ?: emptyMap(),
            lastChanged = iso(lc),
            lastUpdated = iso(lu),
        )
    }

    fun applyDiff(base: HaEntity, diff: JSONObject): HaEntity {
        var entity = base
        diff.optJSONObject("+")?.let { plus ->
            val lc = if (plus.has("lc")) plus.optDouble("lc") else null
            val lu = if (plus.has("lu")) plus.optDouble("lu") else lc
            entity = entity.copy(
                state = if (plus.has("s")) plus.optString("s") else entity.state,
                attributes = plus.optJSONObject("a")?.let { entity.attributes + attrs(it) } ?: entity.attributes,
                lastChanged = lc?.let { iso(it) } ?: entity.lastChanged,
                lastUpdated = lu?.let { iso(it) } ?: entity.lastUpdated,
            )
        }
        diff.optJSONObject("-")?.optJSONArray("a")?.let { removed ->
            val keys = (0 until removed.length()).map { removed.optString(it) }.toSet()
            entity = entity.copy(attributes = entity.attributes.filterKeys { it !in keys })
        }
        return entity
    }

    private fun attrs(o: JSONObject): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>(o.length())
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            out[k] = HaEntity.jsonToAny(o.get(k))
        }
        return out
    }

    /** Context: a bare id string, or an object when parent_id / user_id are set. */
    private fun parentId(c: Any?): String? =
        (c as? JSONObject)?.optString("parent_id")?.ifEmpty { null }?.takeIf { it != "null" }

    fun iso(epochSeconds: Double): String? =
        if (epochSeconds.isNaN()) null else java.time.Instant.ofEpochMilli((epochSeconds * 1000).toLong()).toString()
}
