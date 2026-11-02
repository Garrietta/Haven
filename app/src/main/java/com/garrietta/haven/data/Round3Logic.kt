package com.garrietta.haven.data

/**
 * Round 3 pure logic (2026-11-02): presence grouping, vacuum
 * consumables, timer countdowns, update aggregation, the security
 * checklist, and display formatting. Every function here is pure
 * over [HaEntity] values so it is unit-testable without Compose;
 * the panels in Round3Panels.kt only render what these return.
 *
 * Entity ids below come ONLY from the user's Home Assistant
 * template export (Entities_6_shc1.txt, 366 entities). Nothing
 * here invents a binding: an absent id is omitted by the caller,
 * never guessed. One naming correction from that export: the
 * sensor reset button is button.robovac_reset_sensors (plural),
 * not the singular form.
 */

/**
 * Humanizes a raw HA enum/option/state token for display (Round 4):
 * underscores and hyphens become spaces, camelCase splits, every
 * word title-cases — except short all-caps acronyms, which stay
 * upper-case ("Boost_IQ" -> "Boost IQ", never "Boost Iq"; a
 * lowercase "iq" token maps to the same acronym). Pure, so panel
 * labels can never show a raw token again.
 */
fun humanizeOptionLabel(raw: String): String {
    if (raw.isBlank()) return raw
    val acronyms = mapOf("iq" to "IQ", "led" to "LED")
    val spaced = raw.replace('_', ' ').replace('-', ' ')
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    return spaced.split(' ').filter { it.isNotEmpty() }.joinToString(" ") { word ->
        when {
            word.length <= 4 && word.all { it.isUpperCase() || it.isDigit() } -> word
            acronyms.containsKey(word.lowercase()) -> acronyms[word.lowercase()]!!
            else -> word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

// ---- Vacuum (eufy AE C10) ----

const val VACUUM_ID = "vacuum.t2292"
const val VACUUM_MAP_CAMERA_ID = "camera.eufy_ae_c10_map"
const val VACUUM_BATTERY_ID = "sensor.robovac_battery"
const val VACUUM_DOCK_STATUS_ID = "sensor.robovac_dock_status"
const val VACUUM_TASK_STATUS_ID = "sensor.robovac_task_status"
const val VACUUM_START_BUTTON_ID = "button.eufy_ae_c10_start_cleaning"
const val VACUUM_PAUSE_BUTTON_ID = "button.eufy_ae_c10_pause"
const val VACUUM_DOCK_BUTTON_ID = "button.eufy_ae_c10_return_to_base"
const val VACUUM_EMPTY_BIN_BUTTON_ID = "button.empty_dust_bin"
const val VACUUM_WASH_MOP_BUTTON_ID = "button.wash_mop"
const val VACUUM_DRY_MOP_BUTTON_ID = "button.dry_mop"
const val VACUUM_STOP_DRY_MOP_BUTTON_ID = "button.robovac_stop_dry_mop"
const val VACUUM_CLEANING_MODE_ID = "select.robovac_cleaning_mode"
const val VACUUM_SUCTION_ID = "select.robovac_suction_level"
const val VACUUM_MOP_INTENSITY_ID = "select.robovac_mop_intensity"
const val VACUUM_WATER_LEVEL_ID = "select.robovac_water_level"
const val VACUUM_CLEAN_ROOM_ID = "select.robovac_clean_room"
const val VACUUM_AUTO_EMPTY_ID = "switch.robovac_auto_empty"
const val VACUUM_AUTO_WASH_ID = "switch.robovac_auto_wash"
const val VACUUM_CHILD_LOCK_ID = "switch.robovac_child_lock"
const val VACUUM_FIND_ROBOT_ID = "switch.robovac_find_robot"
const val VACUUM_DND_START_ID = "time.robovac_do_not_disturb_start"
const val VACUUM_DND_END_ID = "time.robovac_do_not_disturb_end"
const val VACUUM_TOTAL_AREA_ID = "sensor.eufy_ae_c10_total_cleaning_area"
const val VACUUM_TOTAL_COUNT_ID = "sensor.eufy_ae_c10_total_cleaning_count"
const val VACUUM_TOTAL_TIME_ID = "sensor.eufy_ae_c10_total_cleaning_time"

/** One vacuum consumable: its remaining-life sensor, its reset button, and its full-life scale. */
data class Consumable(
    val label: String,
    val sensorId: String,
    val resetButtonId: String,
    /** Full-life hours for this part, per the eufy integration's published lifetimes. */
    val fullLifeHours: Double,
)

val VACUUM_CONSUMABLES = listOf(
    Consumable("Filter", "sensor.robovac_filter_remaining", "button.robovac_reset_filter", 360.0),
    Consumable("Rolling brush", "sensor.robovac_rolling_brush_remaining", "button.robovac_reset_rolling_brush", 360.0),
    Consumable("Side brush", "sensor.robovac_side_brush_remaining", "button.robovac_reset_side_brush", 180.0),
    Consumable("Mopping cloth", "sensor.robovac_mopping_cloth_remaining", "button.robovac_reset_mopping_cloth", 180.0),
    // Export-confirmed id is plural: button.robovac_reset_sensors.
    Consumable("Sensors", "sensor.robovac_sensor_remaining", "button.robovac_reset_sensors", 60.0),
    Consumable("Cleaning tray", "sensor.robovac_cleaning_tray_remaining", "button.robovac_reset_cleaning_tray", 30.0),
)

/** Remaining-life fraction for a consumable, clamped to [0, 1]. Null when the sensor state is not numeric. */
fun consumableFraction(remainingHours: Double?, fullLifeHours: Double): Double? {
    if (remainingHours == null || fullLifeHours <= 0.0) return null
    return (remainingHours / fullLifeHours).coerceIn(0.0, 1.0)
}

/** "Docked · 100%" tile line from the vacuum state and its battery sensor. */
fun vacuumTileLine(vacuumState: String, batteryPercent: String?): String {
    val state = vacuumState.replace('_', ' ').replaceFirstChar { it.uppercase() }
    return if (batteryPercent != null) "$state · $batteryPercent%" else state
}

/** "22:00:00" -> "22:00"; anything else passes through unchanged. */
fun shortTime(state: String): String {
    val parts = state.split(":")
    return if (parts.size >= 2 && parts[0].all { it.isDigit() } && parts[1].all { it.isDigit() }) {
        "${parts[0]}:${parts[1]}"
    } else {
        state
    }
}

/** "22:00–08:00" from the two DND time entities, or null when either is missing. */
fun dndRange(start: String?, end: String?): String? {
    if (start == null || end == null) return null
    return "${shortTime(start)}–${shortTime(end)}"
}

// ---- Media ----

const val REMOTE_TV_ID = "remote.tv"
const val SIREN_ID = "siren.outdoor_camera_siren"

/** The media players in scope for Round 3, in tile order. */
val MEDIA_PLAYER_IDS = listOf(
    "media_player.family_room_tv",
    "media_player.garrett_s_tv",
    "media_player.tv",
    "media_player.surround_sound",
    "media_player.hag_display",
)

/** Google TV / Android TV remote commands sent through remote.send_command. */
object TvCommand {
    const val UP = "DPAD_UP"
    const val DOWN = "DPAD_DOWN"
    const val LEFT = "DPAD_LEFT"
    const val RIGHT = "DPAD_RIGHT"
    const val CENTER = "DPAD_CENTER"
    const val BACK = "BACK"
    const val HOME = "HOME"
    const val MUTE = "VOLUME_MUTE"
}

// ---- Security ----

const val ALARM_ID = "alarm_control_panel.bloomington_mode"

/** Arming modes offered by the banner chips and the panel selector. */
val ALARM_ARM_MODES = listOf("home", "away", "night")

fun alarmArmService(mode: String): String = when (mode) {
    "home" -> "alarm_arm_home"
    "away" -> "alarm_arm_away"
    "night" -> "alarm_arm_night"
    else -> "alarm_disarm"
}

fun alarmModeLabel(mode: String): String = when (mode) {
    "home" -> "Home"
    "away" -> "Away"
    "night" -> "Night"
    "disarmed" -> "Disarmed"
    else -> mode.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * One security checklist row: a door/motion/occupancy binary sensor
 * and its human status. Device class drives the wording (a door
 * sensor's "on" is Open, a motion sensor's "on" is Motion); sensors
 * without a device class fall back to the raw state text. Only
 * sensors whose state is on/off are listed — unavailable and
 * unknown sensors are never presented as a clean bill of health.
 */
data class SecurityCheckRow(
    val entityId: String,
    val name: String,
    val active: Boolean,
    val statusText: String,
)

fun securityChecklist(binarySensors: List<HaEntity>): List<SecurityCheckRow> {
    return binarySensors.filter { it.state == "on" || it.state == "off" }
        .filter { sensor ->
            val deviceClass = sensor.attrString("device_class")
            deviceClass in setOf("door", "motion", "occupancy", "presence", "window") ||
                sensor.entityId.contains("front_door") ||
                sensor.entityId.contains("garage")
        }
        .sortedBy { it.friendlyName.lowercase() }
        .map { sensor ->
            val on = sensor.state == "on"
            val status = when (sensor.attrString("device_class")) {
                "door", "window" -> if (on) "Open" else "Closed"
                "motion" -> if (on) "Motion detected" else "Clear"
                "occupancy", "presence" -> if (on) "Occupied" else "Clear"
                else -> if (on) "Active" else "Clear"
            }
            SecurityCheckRow(sensor.entityId, sensor.friendlyName, on, status)
        }
}

// ---- Greenhouse & irrigation ----

val GREENHOUSE_AUTOMATION_IDS = listOf(
    "automation.greenhouse_brain",
    "automation.greenhouse_fan_predictive",
    "automation.greenhouse_frost_heater_control",
    "automation.raise_greenhouse_humidity",
)
val GREENHOUSE_TIMER_IDS = listOf(
    "timer.greenhouse_fan_purge",
    "timer.greenhouse_manual_water_hold",
    "timer.greenhouse_mist_pulse",
    "timer.greenhouse_morning_water",
)
const val GREENHOUSE_WATER_DUE_ID = "counter.greenhouse_water_due"
const val SPRINKLER_SWITCH_ID = "switch.smart_sprinkler_timer_1b0092afd0b7_watering"
const val SPRINKLER_DURATION_ID = "number.smart_sprinkler_timer_1b0092afd0b7_watering_duration"
const val SPRINKLER_BATTERY_ID = "sensor.smart_sprinkler_timer_1b0092afd0b7_battery"

/** "greenhouse_fan_purge" -> "Greenhouse fan purge" for timers whose friendly name is just the raw object id. Names that already contain spaces pass through untouched. */
fun prettifyIdName(entity: HaEntity): String {
    val name = entity.friendlyName
    return if (!name.contains(' ')) {
        name.substringAfter(".").replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
    } else {
        name
    }
}

/**
 * Countdown formatter: 3661 s -> "1 h 1 min", 900 s -> "15 min",
 * 45 s -> "45 s". Timer "remaining" attributes are durations,
 * never timestamps, so this is safe to show on any surface.
 */
fun formatCountdown(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return when {
        hours > 0 -> "$hours h $minutes min"
        minutes > 0 -> "$minutes min"
        else -> "$secs s"
    }
}
fun parseHaDuration(raw: String?): Double? {
    if (raw == null) return null
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(":")
    return try {
        when (parts.size) {
            1 -> parts[0].toDouble()
            2 -> parts[0].toDouble() * 60.0 + parts[1].toDouble()
            3 -> parts[0].toDouble() * 3600.0 + parts[1].toDouble() * 60.0 + parts[2].toDouble()
            else -> null
        }
    } catch (_: NumberFormatException) {
        null
    }
}

/**
 * Countdown fraction for a timer: remaining / duration from the
 * live attributes. Null when either side is missing — the caller
 * then shows the remaining text with an empty ring, never a
 * fabricated progress value.
 */
fun timerFraction(timer: HaEntity): Double? {
    val remaining = parseHaDuration(timer.attrString("remaining"))
    val duration = parseHaDuration(timer.attrString("duration"))
    if (remaining == null || duration == null || duration <= 0.0) return null
    return (remaining / duration).coerceIn(0.0, 1.0)
}

/**
 * Human "last triggered" line from an automation's last_triggered
 * attribute. Returns null when the attribute is absent, unknown,
 * or unparseable — the caller then omits the line rather than
 * showing a raw ISO timestamp (detail surfaces never show raw
 * ISO timestamps).
 */
fun lastTriggeredLine(entity: HaEntity): String? {
    val raw = entity.attrString("last_triggered") ?: return null
    if (raw.isBlank() || raw == "unknown" || raw == "unavailable") return null
    return try {
        val normalized = raw.replace("Z", "+00:00")
        val instant = java.time.OffsetDateTime.parse(normalized)
        val local = instant.atZoneSameInstant(java.time.ZoneId.systemDefault())
        val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
        "Last triggered ${local.format(fmt)}"
    } catch (_: Exception) {
        null
    }
}

// ---- Presence (Who's home) ----

/**
 * One person's chip group. [isOther] marks the unmapped row:
 * its members are never attributed to a person.
 */
data class PresenceGroup(
    val label: String,
    val initials: String,
    val isOther: Boolean,
    val members: List<HaEntity>,
)

/**
 * Groups home device trackers by person. Ownership comes from
 * the tracker itself: a "Garrett"/"Haley" name or id prefix maps
 * to that person; device_tracker.z_fold_8 maps to Garrett because
 * the Z Fold 8 is Garrett's phone. Everything else — Vulpo,
 * iPad (46864), and any future unmapped tracker — lands in Other,
 * never silently dropped and never attributed to the wrong person.
 * Person entities (e.g. person.home, literally named "Home") are
 * not devices and are excluded by the caller.
 */
fun groupPresence(homeTrackers: List<HaEntity>): List<PresenceGroup> {
    val garrett = mutableListOf<HaEntity>()
    val haley = mutableListOf<HaEntity>()
    val other = mutableListOf<HaEntity>()
    for (tracker in homeTrackers) {
        val id = tracker.entityId.lowercase()
        val name = tracker.friendlyName.lowercase()
        when {
            name.contains("haley") || id.contains("haley") -> haley += tracker
            name.contains("garrett") || id.contains("garrett") ||
                id == "device_tracker.z_fold_8" -> garrett += tracker
            else -> other += tracker
        }
    }
    return buildList {
        if (garrett.isNotEmpty()) add(PresenceGroup("Garrett", "G", false, garrett.sortedBy { it.friendlyName.lowercase() }))
        if (haley.isNotEmpty()) add(PresenceGroup("Haley", "H", false, haley.sortedBy { it.friendlyName.lowercase() }))
        if (other.isNotEmpty()) add(PresenceGroup("Other", "?", true, other.sortedBy { it.friendlyName.lowercase() }))
    }
}

/** All trackers belonging to a group, home or away, for the person sheet. */
fun groupAllTrackers(group: PresenceGroup, states: Map<String, HaEntity>): List<HaEntity> {
    val ids = group.members.map { it.entityId }.toSet()
    return states.values.filter { it.domain == "device_tracker" && it.entityId in ids }
        .sortedBy { it.friendlyName.lowercase() }
}

/**
 * Every tracker NOT owned by a named person group — the Other
 * sheet's full roster, home or not, so an away or unknown
 * tracker (Vulpo, iPad 46864) is never silently dropped from
 * the sheet just because it is not currently home.
 */
fun unmappedTrackers(states: Map<String, HaEntity>, groups: List<PresenceGroup>): List<HaEntity> {
    val owned = groups.filter { !it.isOther }.flatMap { it.members.map { member -> member.entityId } }.toSet()
    return states.values.filter { it.domain == "device_tracker" && it.entityId !in owned }
        .sortedBy { it.friendlyName.lowercase() }
}

/** Battery percent for a tracker, from the sensor whose id embeds the tracker's object id. Null when absent or non-numeric. */
fun batteryForTracker(tracker: HaEntity, states: Map<String, HaEntity>): Int? {
    val objectId = tracker.entityId.substringAfter(".")
    val sensor = states.values.firstOrNull { entity ->
        entity.domain == "sensor" && entity.entityId.contains(objectId) &&
            entity.entityId.contains("battery_level")
    } ?: return null
    return sensor.state.toIntOrNull()
}

/** Geocoded location for a tracker, or null when the sensor is absent, unavailable, or unknown. */
fun locationForTracker(tracker: HaEntity, states: Map<String, HaEntity>): String? {
    val objectId = tracker.entityId.substringAfter(".")
    val sensor = states.values.firstOrNull { entity ->
        entity.domain == "sensor" && entity.entityId.contains(objectId) &&
            entity.entityId.contains("geocoded_location")
    } ?: return null
    return sensor.state.takeIf { it.isNotBlank() && it != "unavailable" && it != "unknown" }
}

// ---- System updates ----

data class UpdateSummary(val total: Int, val available: Int, val updates: List<HaEntity>)

/** All update.* entities, available (state == "on") first, then by name. */
fun updatesSummary(states: Map<String, HaEntity>): UpdateSummary {
    val updates = states.values.filter { it.domain == "update" }
        .sortedWith(compareByDescending<HaEntity> { it.state == "on" }.thenBy { it.friendlyName })
    return UpdateSummary(updates.size, updates.count { it.state == "on" }, updates)
}

/** "browser_mod Update" -> "browser_mod": the trailing " Update" is a suffix, not part of the name. */
fun updateDisplayName(entity: HaEntity): String =
    entity.friendlyName.removeSuffix(" Update").removeSuffix(" update")
