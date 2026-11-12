package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject

/*
 * Home Assistant's own settings, read and written from Haven: the
 * integrations, devices, entities and helpers; the areas, floors,
 * labels and zones; people and users; and the system pages (repairs,
 * logs, backups, general, restart). These are the same WebSocket and
 * REST calls Home Assistant's frontend makes, so a change here is a
 * change in Home Assistant.
 *
 * The parsers are pure and tested against the shapes Home Assistant
 * returns. A field Home Assistant leaves out is null or empty here,
 * never a made-up value; HA writes the string "null" into some name
 * fields, which is treated as absent the same way the registry
 * loader does.
 */

/** An integration's config entry: one row on the Integrations page. */
data class ConfigEntry(
    val entryId: String,
    val domain: String,
    val title: String,
    /** "loaded", "setup_error", "not_loaded", "setup_retry", "failed_unload", "migration_error". */
    val state: String,
    val disabledBy: String?,
    val source: String?,
)

data class RegistryDevice(
    val id: String,
    val name: String,
    val nameByUser: String?,
    val manufacturer: String?,
    val model: String?,
    val areaId: String?,
    val disabledBy: String?,
    val configEntries: List<String>,
    val entryType: String?,
) {
    /** What Home Assistant shows: the user's name when set, else the device's own. */
    val displayName: String get() = nameByUser ?: name
}

data class RegistryEntity(
    val entityId: String,
    /** The name set in Home Assistant, overriding the integration's. */
    val name: String?,
    val originalName: String?,
    val icon: String?,
    val areaId: String?,
    val deviceId: String?,
    val disabledBy: String?,
    val hiddenBy: String?,
    val platform: String?,
    val entityCategory: String?,
) {
    val domain: String get() = entityId.substringBefore('.')
}

data class RepairIssue(
    val domain: String,
    val issueId: String,
    val severity: String,
    val translationKey: String?,
    val ignored: Boolean,
    val fixable: Boolean,
    val learnMoreUrl: String?,
) {
    /** A readable title without Home Assistant's translation files: "Integration · what". */
    val title: String
        get() = "${humanizeOptionLabel(domain)} · ${humanizeOptionLabel(translationKey ?: issueId)}"
}

data class LogEntry(
    val name: String,
    val message: String,
    val level: String,
    val source: String?,
    /** Seconds since the epoch. */
    val timestamp: Double,
    val count: Int,
    val exception: String?,
)

data class HaZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val icon: String?,
    val passive: Boolean,
)

data class HaPerson(
    val id: String,
    val name: String,
    val userId: String?,
    val deviceTrackers: List<String>,
    /** Set in configuration.yaml: Home Assistant does not let the UI change it. */
    val fromYaml: Boolean,
)

data class HaUser(
    val id: String,
    val name: String,
    val isOwner: Boolean,
    val isActive: Boolean,
    val isAdmin: Boolean,
)

data class HaFloor(val id: String, val name: String, val level: Int?)

data class HaLabel(val id: String, val name: String, val color: String?)

data class HaBackup(
    val id: String,
    val name: String,
    /** ISO date as Home Assistant reports it. */
    val date: String,
    val sizeBytes: Long?,
)

data class HaHelper(
    /** The helper's own domain: input_boolean, counter, timer... */
    val domain: String,
    /** Its storage id, which delete and update address. */
    val id: String,
    val name: String,
    val icon: String?,
) {
    val entityIdGuess: String get() = "$domain.$id"
}

/** Home Assistant's core configuration, from get_config. */
data class CoreConfig(
    val locationName: String,
    val version: String,
    val timeZone: String?,
    val unitSystem: String?,
    val currency: String?,
    val country: String?,
    val language: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Int?,
    val state: String?,
    val safeMode: Boolean,
    val components: Int,
)

/** The helper domains Home Assistant can create and delete from its UI. */
val HELPER_DOMAINS = listOf(
    "input_boolean", "input_number", "input_select", "input_text",
    "input_datetime", "input_button", "counter", "timer", "schedule",
)

private fun JSONObject.str(key: String): String? =
    optString(key).takeIf { has(key) && !isNull(key) && it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

private fun JSONObject.dbl(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }

/** The `result` of a WebSocket reply as a list, whether it is the list or an object wrapping one under [key]. */
fun resultList(msg: JSONObject?, key: String? = null): List<JSONObject> {
    if (msg == null || !msg.optBoolean("success", true)) return emptyList()
    msg.optJSONArray("result")?.let { return it.objects() }
    val obj = msg.optJSONObject("result") ?: return emptyList()
    if (key != null) return obj.optJSONArray(key).objects()
    return emptyList()
}

/** Home Assistant's message for a failed reply, or null when it succeeded. */
fun commandError(msg: JSONObject?): String? = when {
    msg == null -> "Home Assistant did not answer."
    msg.optBoolean("success", false) -> null
    else -> msg.optJSONObject("error")?.optString("message")?.ifEmpty { null } ?: "Home Assistant refused the change."
}

fun parseConfigEntries(items: List<JSONObject>): List<ConfigEntry> = items.mapNotNull { o ->
    val id = o.str("entry_id") ?: return@mapNotNull null
    ConfigEntry(
        entryId = id,
        domain = o.str("domain") ?: "",
        title = o.str("title") ?: o.str("domain") ?: id,
        state = o.str("state") ?: "not_loaded",
        disabledBy = o.str("disabled_by"),
        source = o.str("source"),
    )
}.sortedWith(compareBy({ it.domain }, { it.title.lowercase() }))

fun parseDevices(items: List<JSONObject>): List<RegistryDevice> = items.mapNotNull { o ->
    val id = o.str("id") ?: return@mapNotNull null
    val name = o.str("name") ?: o.str("model") ?: o.str("manufacturer") ?: return@mapNotNull null
    RegistryDevice(
        id = id,
        name = name,
        nameByUser = o.str("name_by_user"),
        manufacturer = o.str("manufacturer"),
        model = o.str("model"),
        areaId = o.str("area_id"),
        disabledBy = o.str("disabled_by"),
        configEntries = o.optJSONArray("config_entries").strings(),
        entryType = o.str("entry_type"),
    )
}.sortedBy { it.displayName.lowercase() }

fun parseEntityRegistry(items: List<JSONObject>): List<RegistryEntity> = items.mapNotNull { o ->
    val id = o.str("entity_id") ?: return@mapNotNull null
    RegistryEntity(
        entityId = id,
        name = o.str("name"),
        originalName = o.str("original_name"),
        icon = o.str("icon"),
        areaId = o.str("area_id"),
        deviceId = o.str("device_id"),
        disabledBy = o.str("disabled_by"),
        hiddenBy = o.str("hidden_by"),
        platform = o.str("platform"),
        entityCategory = o.str("entity_category"),
    )
}.sortedBy { it.entityId }

fun parseRepairs(items: List<JSONObject>): List<RepairIssue> = items.mapNotNull { o ->
    val id = o.str("issue_id") ?: return@mapNotNull null
    RepairIssue(
        domain = o.str("domain") ?: "",
        issueId = id,
        severity = o.str("severity") ?: "warning",
        translationKey = o.str("translation_key"),
        ignored = o.optBoolean("ignored", false),
        fixable = o.optBoolean("is_fixable", false),
        learnMoreUrl = o.str("learn_more_url"),
    )
}.sortedWith(compareBy({ it.ignored }, { severityRank(it.severity) }, { it.domain }))

/** Critical first, then errors, then warnings. */
internal fun severityRank(severity: String): Int = when (severity) {
    "critical" -> 0
    "error" -> 1
    else -> 2
}

fun parseLogs(items: List<JSONObject>): List<LogEntry> = items.map { o ->
    val messages = o.optJSONArray("message").strings()
    val source = o.optJSONArray("source")?.let { arr ->
        val file = arr.optString(0).substringAfterLast('/')
        val line = arr.optInt(1, -1)
        if (file.isEmpty()) null else if (line >= 0) "$file:$line" else file
    }
    LogEntry(
        name = o.str("name") ?: "",
        message = messages.firstOrNull() ?: o.str("message") ?: "",
        level = o.str("level") ?: "ERROR",
        source = source,
        timestamp = o.dbl("timestamp") ?: 0.0,
        count = o.optInt("count", 1),
        exception = o.str("exception"),
    )
}.sortedByDescending { it.timestamp }

fun parseZones(items: List<JSONObject>): List<HaZone> = items.mapNotNull { o ->
    val id = o.str("id") ?: return@mapNotNull null
    HaZone(
        id = id,
        name = o.str("name") ?: id,
        latitude = o.dbl("latitude") ?: return@mapNotNull null,
        longitude = o.dbl("longitude") ?: return@mapNotNull null,
        radius = o.dbl("radius") ?: 100.0,
        icon = o.str("icon"),
        passive = o.optBoolean("passive", false),
    )
}.sortedBy { it.name.lowercase() }

/** person/list answers {storage: [...], config: [...]}; config ones come from YAML. */
fun parsePeople(msg: JSONObject?): List<HaPerson> {
    val result = msg?.optJSONObject("result") ?: return emptyList()
    fun read(arr: JSONArray?, yaml: Boolean) = arr.objects().mapNotNull { o ->
        val id = o.str("id") ?: return@mapNotNull null
        HaPerson(id, o.str("name") ?: id, o.str("user_id"), o.optJSONArray("device_trackers").strings(), yaml)
    }
    return (read(result.optJSONArray("storage"), false) + read(result.optJSONArray("config"), true))
        .sortedBy { it.name.lowercase() }
}

/** Real people's accounts: system-generated users (the supervisor, integrations) are left out. */
fun parseUsers(items: List<JSONObject>): List<HaUser> = items.mapNotNull { o ->
    if (o.optBoolean("system_generated", false)) return@mapNotNull null
    val id = o.str("id") ?: return@mapNotNull null
    HaUser(
        id = id,
        name = o.str("name") ?: o.str("username") ?: id,
        isOwner = o.optBoolean("is_owner", false),
        isActive = o.optBoolean("is_active", true),
        isAdmin = o.optJSONArray("group_ids").strings().contains("system-admin"),
    )
}.sortedWith(compareBy({ !it.isOwner }, { it.name.lowercase() }))

fun parseFloors(items: List<JSONObject>): List<HaFloor> = items.mapNotNull { o ->
    val id = o.str("floor_id") ?: return@mapNotNull null
    HaFloor(id, o.str("name") ?: id, if (o.has("level") && !o.isNull("level")) o.optInt("level") else null)
}.sortedWith(compareBy({ it.level ?: Int.MAX_VALUE }, { it.name.lowercase() }))

fun parseLabels(items: List<JSONObject>): List<HaLabel> = items.mapNotNull { o ->
    val id = o.str("label_id") ?: return@mapNotNull null
    HaLabel(id, o.str("name") ?: id, o.str("color"))
}.sortedBy { it.name.lowercase() }

/**
 * Backups from backup/info. Newer Home Assistant gives each backup a
 * backup_id and per-location sizes under agents; older versions a slug
 * and a size. Both read the same.
 */
fun parseBackups(msg: JSONObject?): List<HaBackup> {
    val items = resultList(msg, "backups")
    return items.mapNotNull { o ->
        val id = o.str("backup_id") ?: o.str("slug") ?: return@mapNotNull null
        val size = if (o.has("size") && !o.isNull("size")) {
            // Older versions report megabytes as a decimal.
            val raw = o.optDouble("size")
            if (raw < 1_000_000) (raw * 1024 * 1024).toLong() else raw.toLong()
        } else {
            o.optJSONObject("agents")?.let { agents ->
                agents.keys().asSequence().mapNotNull { k -> agents.optJSONObject(k)?.optLong("size", -1)?.takeIf { it >= 0 } }.firstOrNull()
            }
        }
        HaBackup(id, o.str("name") ?: id, o.str("date") ?: "", size)
    }.sortedByDescending { it.date }
}

/** A helper domain's list reply ({result: [{id, name, icon}]}). */
fun parseHelpers(domain: String, msg: JSONObject?): List<HaHelper> = resultList(msg).mapNotNull { o ->
    val id = o.str("id") ?: return@mapNotNull null
    HaHelper(domain, id, o.str("name") ?: id, o.str("icon"))
}

fun parseCoreConfig(msg: JSONObject?): CoreConfig? {
    val o = msg?.optJSONObject("result") ?: return null
    val units = o.optJSONObject("unit_system")
    val unitSystem = when {
        units == null -> null
        units.optString("temperature").contains("F") -> "US customary"
        else -> "Metric"
    }
    return CoreConfig(
        locationName = o.str("location_name") ?: "Home",
        version = o.str("version") ?: "",
        timeZone = o.str("time_zone"),
        unitSystem = unitSystem,
        currency = o.str("currency"),
        country = o.str("country"),
        language = o.str("language"),
        latitude = o.dbl("latitude"),
        longitude = o.dbl("longitude"),
        elevation = if (o.has("elevation") && !o.isNull("elevation")) o.optInt("elevation") else null,
        state = o.str("state"),
        safeMode = o.optBoolean("safe_mode", false) || o.optBoolean("recovery_mode", false),
        components = o.optJSONArray("components")?.length() ?: 0,
    )
}

/** How a config entry's state reads on its row. */
fun configEntryStatus(entry: ConfigEntry): String = when {
    entry.disabledBy != null -> "Disabled"
    entry.state == "loaded" -> "Loaded"
    entry.state == "setup_retry" -> "Retrying setup"
    entry.state == "setup_error" -> "Failed to set up"
    entry.state == "migration_error" -> "Needs migration"
    entry.state == "failed_unload" -> "Failed to unload"
    else -> "Not loaded"
}

/** True when an entry needs the person's attention. */
fun configEntryNeedsAttention(entry: ConfigEntry): Boolean =
    entry.disabledBy == null && entry.state in setOf("setup_error", "setup_retry", "migration_error", "failed_unload")

/** "1.2 GB", "340 MB", "12 KB". */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "${bytes / (1L shl 20)} MB"
    bytes >= 1L shl 10 -> "${bytes / (1L shl 10)} KB"
    else -> "$bytes B"
}

/**
 * A config flow step, as Home Assistant describes it: a form of fields
 * to fill, a menu to pick from, a link to finish in the browser, or the
 * end (created or aborted).
 */
sealed class FlowStep {
    abstract val flowId: String

    data class Form(override val flowId: String, val stepId: String, val fields: List<FlowField>, val errors: Map<String, String>) : FlowStep()
    data class Menu(override val flowId: String, val stepId: String, val options: List<Pair<String, String>>) : FlowStep()
    data class External(override val flowId: String, val url: String) : FlowStep()
    data class Done(override val flowId: String, val title: String) : FlowStep()
    data class Aborted(override val flowId: String, val reason: String) : FlowStep()
}

/** One field of a config flow form. */
data class FlowField(
    val name: String,
    val kind: FieldKind,
    val required: Boolean,
    val default: Any?,
    val options: List<Pair<String, String>>,
) {
    val label: String get() = humanizeOptionLabel(name)
}

enum class FieldKind { TEXT, PASSWORD, NUMBER, BOOLEAN, SELECT, UNSUPPORTED }

/**
 * Reads a config flow reply. Fields come either as voluptuous types
 * ("string", "integer", "boolean", "select") or as selectors
 * ({"selector": {"text": {...}}}); a field Haven cannot draw is
 * UNSUPPORTED, and the form says so rather than sending a guess.
 */
fun parseFlowStep(json: JSONObject): FlowStep? {
    val flowId = json.str("flow_id") ?: ""
    return when (json.str("type")) {
        "form" -> FlowStep.Form(
            flowId,
            json.str("step_id") ?: "",
            json.optJSONArray("data_schema").objects().map(::parseFlowField),
            json.optJSONObject("errors")?.let { e -> e.keys().asSequence().associateWith { e.optString(it) } } ?: emptyMap(),
        )
        "menu" -> {
            val raw = json.opt("menu_options")
            val options = when (raw) {
                is JSONArray -> raw.strings().map { it to humanizeOptionLabel(it) }
                is JSONObject -> raw.keys().asSequence().map { it to raw.optString(it) }.toList()
                else -> emptyList()
            }
            FlowStep.Menu(flowId, json.str("step_id") ?: "", options)
        }
        "external", "external_step" -> FlowStep.External(flowId, json.str("url") ?: "")
        "create_entry" -> FlowStep.Done(flowId, json.str("title") ?: "")
        "abort" -> FlowStep.Aborted(flowId, json.str("reason") ?: "aborted")
        else -> null
    }
}

internal fun parseFlowField(o: JSONObject): FlowField {
    val name = o.str("name") ?: ""
    val required = o.optBoolean("required", false)
    val default = if (o.has("default") && !o.isNull("default")) o.opt("default") else null
    val selector = o.optJSONObject("selector")
    if (selector != null) {
        val kind = selector.keys().asSequence().firstOrNull()
        val config = kind?.let { selector.optJSONObject(it) }
        return when (kind) {
            "text" -> FlowField(name, if (config?.optString("type") == "password") FieldKind.PASSWORD else FieldKind.TEXT, required, default, emptyList())
            "number" -> FlowField(name, FieldKind.NUMBER, required, default, emptyList())
            "boolean" -> FlowField(name, FieldKind.BOOLEAN, required, default, emptyList())
            "select" -> FlowField(name, FieldKind.SELECT, required, default, selectOptions(config?.optJSONArray("options")))
            else -> FlowField(name, FieldKind.UNSUPPORTED, required, default, emptyList())
        }
    }
    return when (o.str("type")) {
        "string" -> FlowField(name, if (name.contains("password", ignoreCase = true)) FieldKind.PASSWORD else FieldKind.TEXT, required, default, emptyList())
        "integer", "float" -> FlowField(name, FieldKind.NUMBER, required, default, emptyList())
        "boolean" -> FlowField(name, FieldKind.BOOLEAN, required, default, emptyList())
        "select" -> FlowField(name, FieldKind.SELECT, required, default, selectOptions(o.optJSONArray("options")))
        else -> FlowField(name, FieldKind.UNSUPPORTED, required, default, emptyList())
    }
}

/** Select options as (value, label): plain strings, {value, label} objects, or [value, label] pairs. */
private fun selectOptions(arr: JSONArray?): List<Pair<String, String>> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        when (val item = arr.opt(i)) {
            is String -> item to item
            is JSONObject -> item.optString("value") to item.optString("label").ifEmpty { item.optString("value") }
            is JSONArray -> item.optString(0) to item.optString(1).ifEmpty { item.optString(0) }
            else -> null
        }
    }
}

/** How a device's entities are grouped on its page, in Home Assistant's order. */
enum class EntityGroup(val title: String) {
    CONTROLS("Controls"),
    SENSORS("Sensors"),
    CONFIGURATION("Configuration"),
    DIAGNOSTIC("Diagnostic"),
}

/** Domains a person operates rather than reads. */
private val CONTROL_DOMAINS = setOf(
    "switch", "light", "fan", "cover", "lock", "climate", "media_player", "input_boolean", "button",
    "vacuum", "siren", "humidifier", "valve", "water_heater", "select", "number", "scene", "script",
    "alarm_control_panel", "camera", "lawn_mower", "remote", "text", "input_button", "input_number",
    "input_select", "input_text",
)

/** Domains whose main state is on or off. */
private val ON_OFF_DOMAINS = setOf("switch", "light", "fan", "input_boolean", "humidifier", "siren")

/**
 * Home Assistant's grouping: anything it marks as configuration or
 * diagnostic goes there; of the rest, what can be operated is a control
 * and what can only be read is a sensor.
 */
fun entityGroup(entity: RegistryEntity): EntityGroup = when (entity.entityCategory) {
    "config" -> EntityGroup.CONFIGURATION
    "diagnostic" -> EntityGroup.DIAGNOSTIC
    else -> if (entity.domain in CONTROL_DOMAINS) EntityGroup.CONTROLS else EntityGroup.SENSORS
}

/**
 * The device's own on/off, shown first on its page: an enabled control
 * that switches on and off, preferring the one that is the device itself
 * (Home Assistant gives that one no name of its own beyond the device's).
 * A smart plug's outlet, a lamp, a fan. Null when the device has none.
 */
fun mainControl(entities: List<RegistryEntity>): RegistryEntity? =
    entities.filter { entityGroup(it) == EntityGroup.CONTROLS && it.domain in ON_OFF_DOMAINS && it.disabledBy == null }
        .minWithOrNull(compareBy({ if (it.originalName == null) 0 else 1 }, { it.entityId }))

/** True for a domain whose state a switch can flip. */
fun isOnOffDomain(domain: String): Boolean = domain in ON_OFF_DOMAINS

/** An entity's name under its device's: "Heater Auto-off at" on "Heater" reads "Auto-off at". */
fun nameUnderDevice(name: String, deviceName: String): String {
    if (deviceName.isBlank() || !name.startsWith(deviceName, ignoreCase = true)) return name
    val rest = name.substring(deviceName.length).trim(' ', '-', ':', '·')
    return if (rest.isEmpty()) name else rest.replaceFirstChar { it.uppercase() }
}

/** What an area holds, as its page lists it: whole devices, and entities that belong to no device. */
sealed class AreaMember {
    abstract val name: String

    data class Device(val deviceId: String, override val name: String, val entityCount: Int) : AreaMember()

    data class Lone(val entity: HaEntity) : AreaMember() {
        override val name: String get() = entity.friendlyName
    }
}

/**
 * An area's entities gathered into their devices, the way Home Assistant
 * lists an area: one row for a smart plug, not one for each of its eight
 * timers and sensors. A device with no name in [deviceNames] takes its
 * shortest entity name.
 */
fun areaMembers(
    entities: List<HaEntity>,
    entityDevices: Map<String, String>,
    deviceNames: Map<String, String>,
): List<AreaMember> {
    val (withDevice, lone) = entities.partition { entityDevices[it.entityId] != null }
    val devices = withDevice.groupBy { entityDevices.getValue(it.entityId) }.map { (id, list) ->
        AreaMember.Device(id, deviceNames[id] ?: list.minBy { it.friendlyName.length }.friendlyName, list.size)
    }
    return (devices + lone.map { AreaMember.Lone(it) }).sortedBy { it.name.lowercase() }
}
