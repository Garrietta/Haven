package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml

/**
 * One card's YAML <-> JSON bridge, HAOS-style. Cards are stored as YAML text
 * (what the user edits) and rendered as JSON (what LovelaceCardView consumes).
 * JSON is valid YAML, so pasted JSON parses too. A parse failure is an
 * exception with a message the editor shows inline — a broken card never
 * saves, and never takes the tab down with it.
 */

/** Parses one card's YAML into the JSONObject the native renderer consumes. */
fun parseCardYaml(text: String): JSONObject {
    val loaded = Yaml().load<Any?>(text) ?: throw IllegalArgumentException("Card is empty")
    if (loaded !is Map<*, *>) throw IllegalArgumentException("A card must be a mapping, starting with a type: line")
    return mapToJson(loaded)
}

private fun mapToJson(map: Map<*, *>): JSONObject {
    val out = JSONObject()
    for ((k, v) in map) {
        out.put(k.toString(), anyToJson(v))
    }
    return out
}

private fun anyToJson(v: Any?): Any? = when (v) {
    null -> JSONObject.NULL
    is Map<*, *> -> mapToJson(v)
    is List<*> -> JSONArray().apply { for (item in v) put(anyToJson(item)) }
    else -> v
}

/** The card's type ("" when absent — the renderer then shows its placeholder). */
fun cardType(card: JSONObject): String = card.optString("type")

/** Returns a copy of [card] with its type replaced. */
fun changeCardType(card: JSONObject, newType: String): JSONObject {
    val copy = JSONObject(card.toString())
    copy.put("type", newType)
    return copy
}

/** Serializes a card JSONObject back to YAML for the editor. */
fun cardToYaml(card: JSONObject): String {
    val sb = StringBuilder()
    writeYamlMap(card, sb, 0)
    return sb.toString().trimEnd() + "\n"
}

private fun writeYamlMap(obj: JSONObject, sb: StringBuilder, indent: Int) {
    val pad = "  ".repeat(indent)
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        writeYamlEntry(key, obj.opt(key), sb, pad, indent)
    }
}

private fun writeYamlEntry(key: String, value: Any?, sb: StringBuilder, pad: String, indent: Int) {
    when (value) {
        is JSONObject -> {
            if (value.length() == 0) {
                sb.append("$pad$key: {}\n")
            } else {
                sb.append("$pad$key:\n")
                writeYamlMap(value, sb, indent + 1)
            }
        }
        is JSONArray -> {
            if (value.length() == 0) {
                sb.append("$pad$key: []\n")
            } else {
                sb.append("$pad$key:\n")
                writeYamlList(value, sb, indent + 1)
            }
        }
        else -> sb.append("$pad$key: ${yamlScalar(value)}\n")
    }
}

private fun writeYamlList(arr: JSONArray, sb: StringBuilder, indent: Int) {
    val pad = "  ".repeat(indent)
    for (i in 0 until arr.length()) {
        val item = arr.opt(i)
        when (item) {
            is JSONObject -> {
                val keys = item.keys()
                if (!keys.hasNext()) {
                    sb.append("$pad- {}\n")
                    continue
                }
                var first = true
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = item.opt(k)
                    // First key rides on the dash line; the rest align under it.
                    val entryPad = if (first) "$pad- " else "  ".repeat(indent + 1)
                    when (v) {
                        is JSONObject -> {
                            if (v.length() == 0) {
                                sb.append("$entryPad$k: {}\n")
                            } else {
                                sb.append("$entryPad$k:\n")
                                writeYamlMap(v, sb, indent + 2)
                            }
                        }
                        is JSONArray -> {
                            if (v.length() == 0) {
                                sb.append("$entryPad$k: []\n")
                            } else {
                                sb.append("$entryPad$k:\n")
                                writeYamlList(v, sb, indent + 2)
                            }
                        }
                        else -> sb.append("$entryPad$k: ${yamlScalar(v)}\n")
                    }
                    first = false
                }
            }
            is JSONArray -> {
                sb.append("$pad-\n")
                writeYamlList(item, sb, indent + 1)
            }
            else -> sb.append("$pad- ${yamlScalar(item)}\n")
        }
    }
}

private val PLAIN_UNSAFE = setOf("true", "false", "null", "~", "yes", "no", "on", "off")

private fun yamlScalar(v: Any?): String = when (v) {
    null, JSONObject.NULL -> "null"
    is Boolean -> v.toString()
    is Number -> v.toString()
    is String -> {
        val needsQuotes = v.isEmpty() || v.contains(": ") || v.contains(" #") ||
            v.contains("\n") || v.startsWith("*") || v.startsWith("&") ||
            v.startsWith("!") || v.startsWith("|") || v.startsWith(">") ||
            v.startsWith("%") || v.startsWith("@") || v.startsWith("`") ||
            v.startsWith("\"") || v.startsWith("'") ||
            v.lowercase() in PLAIN_UNSAFE || v.toDoubleOrNull() != null
        if (needsQuotes) {
            "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        } else {
            v
        }
    }
    else -> "\"$v\""
}
