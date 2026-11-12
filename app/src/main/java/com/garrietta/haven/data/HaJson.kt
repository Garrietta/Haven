package com.garrietta.haven.data

import org.json.JSONObject

/**
 * A text field from a Home Assistant reply, "" when it is missing or null.
 *
 * Home Assistant sends unset fields as JSON null ("area_id": null), and
 * Android's org.json turns null into the text "null" through optString,
 * while the desktop org.json the unit tests run on returns "". Read through
 * optString, an entity that follows its device's area got the area "null"
 * on the phone and never reached its room, and the tests could not see it.
 * Every nullable registry field goes through here instead.
 */
fun JSONObject.text(key: String): String {
    if (!has(key) || isNull(key)) return ""
    val value = optString(key)
    return if (value == "null") "" else value
}
