package com.garrietta.haven.data

/*
 * What the Security tab shows, as pure functions: the line it opens
 * with, the cameras with the switches that control their detection,
 * and the detection switches that belong to no camera.
 *
 * A switch belongs to a camera only through the entity registry: it
 * sits on the camera's own device. Two things that merely share a name
 * are never joined (the same rule motionSensorForCamera follows), so a
 * switch on no camera's device is listed under its own name.
 */

/** A switch that turns detection on or off: motion, person, pet, vehicle. */
fun isDetectionSwitch(entity: HaEntity): Boolean {
    if (entity.domain != "switch") return false
    val id = entity.entityId.substringAfter('.').lowercase()
    val name = entity.friendlyName.lowercase()
    return DETECTION_WORDS.any { it in id || it in name }
}

private val DETECTION_WORDS = listOf("motion", "detection")

/** A camera on the Security tab, with what its own device offers for motion. */
data class CameraSecurity(
    val cameraId: String,
    val name: String,
    /** Its motion binary_sensor, when its device has one. */
    val motionSensor: HaEntity?,
    /** Its detection switches, by name. */
    val switches: List<HaEntity>,
)

fun securityCameras(
    cameraIds: List<String>,
    states: Map<String, HaEntity>,
    entityDevices: Map<String, String>,
): List<CameraSecurity> = cameraIds.mapNotNull { id ->
    val camera = states[id] ?: return@mapNotNull null
    val device = entityDevices[id]
    val switches = if (device == null) emptyList() else states.values
        .filter { isDetectionSwitch(it) && entityDevices[it.entityId] == device }
        .sortedBy { it.friendlyName.lowercase() }
    CameraSecurity(id, camera.friendlyName, motionSensorForCamera(id, states, entityDevices), switches)
}

/** Detection switches on no listed camera's device, by name. */
fun looseDetectionSwitches(
    cameraIds: List<String>,
    states: Map<String, HaEntity>,
    entityDevices: Map<String, String>,
): List<HaEntity> {
    val cameraDevices = cameraIds.mapNotNull { entityDevices[it] }.toSet()
    return states.values
        .filter { isDetectionSwitch(it) && entityDevices[it.entityId] !in cameraDevices }
        .sortedBy { it.friendlyName.lowercase() }
}

/**
 * A detection switch's name under its camera's: "Front Door Motion
 * detection" under "Front Door" reads "Motion detection". A switch
 * whose name does not start with the camera's keeps its own.
 */
fun detectionLabel(switch: HaEntity, cameraName: String): String {
    val name = switch.friendlyName.trim()
    if (cameraName.isBlank() || !name.startsWith(cameraName, ignoreCase = true)) return name
    val rest = name.substring(cameraName.length).trim(' ', '-', ':', '·')
    return if (rest.isEmpty()) name else rest.replaceFirstChar { it.uppercase() }
}

/** The Security tab's opening line, and whether it is the calm one. */
data class SecuritySummary(val line: String, val calm: Boolean)

/** Door-like binary sensors: their "on" means open. */
private val OPENING_CLASSES = setOf("door", "window", "garage_door", "opening")

/**
 * What is not shut: doors, windows and garage doors that report open,
 * and locks that are not locked. Unavailable things are left out, not
 * counted as shut or open, as everywhere else.
 */
fun securitySummary(states: Map<String, HaEntity>): SecuritySummary {
    val open = states.values.filter { entity ->
        when (entity.domain) {
            "binary_sensor" -> entity.state == "on" && entity.attrString("device_class") in OPENING_CLASSES
            "cover" -> entity.state in setOf("open", "opening") &&
                entity.attrString("device_class") in setOf("garage", "door", "gate")
            else -> false
        }
    }.map { it.friendlyName }.sorted()
    val unlocked = states.values
        .filter { it.domain == "lock" && it.state in setOf("unlocked", "unlocking", "open", "opening", "jammed") }
        .map { it.friendlyName }.sorted()
    if (open.isEmpty() && unlocked.isEmpty()) {
        val anything = states.values.any { it.domain == "lock" || it.domain == "cover" || it.attrString("device_class") in OPENING_CLASSES }
        return SecuritySummary(if (anything) "Everything is shut and locked." else "Nothing to watch yet.", calm = true)
    }
    val parts = buildList {
        if (open.isNotEmpty()) add("${namesList(open)} ${if (open.size == 1) "is" else "are"} open")
        if (unlocked.isNotEmpty()) add("${namesList(unlocked)} ${if (unlocked.size == 1) "is" else "are"} unlocked")
    }
    return SecuritySummary(parts.joinToString(", and ") + ".", calm = false)
}

/** "A", "A and B", "A, B and 2 more". */
internal fun namesList(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names[0]
    2 -> "${names[0]} and ${names[1]}"
    else -> "${names[0]}, ${names[1]} and ${names.size - 2} more"
}

/** The alarm panel the tab arms: the household's own when present, else the first there is. */
fun securityAlarm(states: Map<String, HaEntity>): HaEntity? =
    states[ALARM_ID] ?: states.values.filter { it.domain == "alarm_control_panel" }.minByOrNull { it.entityId }

/** The arming choice an alarm state belongs to: "home", "away", "night", or "disarmed". */
fun alarmChoice(state: String): String? = when (state) {
    "armed_home" -> "home"
    "armed_away" -> "away"
    "armed_night" -> "night"
    "disarmed" -> "disarmed"
    else -> null
}
