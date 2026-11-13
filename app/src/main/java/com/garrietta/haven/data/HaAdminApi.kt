package com.garrietta.haven.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The calls behind Settings' Home Assistant pages. Reads answer null
 * when Home Assistant did not (not connected, or a command this version
 * lacks), so a page can say "could not load" instead of showing an
 * empty list as if it were true. Writes answer null on success and
 * Home Assistant's message on failure. Registry writes re-read the
 * registries, so Home and the rooms follow at once.
 */
class HaAdminApi(private val client: HaClient) {

    private suspend fun list(type: String, key: String? = null, build: (JSONObject) -> Unit = {}): List<JSONObject>? {
        val msg = client.command(type, build = build) ?: return null
        if (!msg.optBoolean("success", false)) return null
        return resultList(msg, key)
    }

    private suspend fun write(type: String, refresh: Boolean = false, build: (JSONObject) -> Unit): String? {
        val error = commandError(client.command(type, build = build))
        if (error == null && refresh) client.refreshRegistries()
        return error
    }

    private fun restError(code: Int, body: String?): String? = when {
        code in 200..299 -> null
        code == 0 -> "Could not reach Home Assistant."
        else -> body?.let { runCatching { JSONObject(it).optString("message") }.getOrNull() }?.ifEmpty { null }
            ?: "Home Assistant answered $code."
    }

    // ---- Integrations ----

    suspend fun configEntries(): List<ConfigEntry>? = list("config_entries/get")?.let(::parseConfigEntries)

    suspend fun reloadEntry(entryId: String): String? =
        client.rest("POST", "/api/config/config_entries/entry/$entryId/reload").let { (c, b) -> restError(c, b) }

    suspend fun setEntryDisabled(entryId: String, disabled: Boolean): String? = write("config_entries/disable") {
        it.put("entry_id", entryId).put("disabled_by", if (disabled) "user" else JSONObject.NULL)
    }

    suspend fun deleteEntry(entryId: String): String? =
        client.rest("DELETE", "/api/config/config_entries/entry/$entryId").let { (c, b) -> restError(c, b) }

    /** The integrations a new entry can be set up for, by domain. */
    suspend fun flowHandlers(): List<String>? {
        val (code, body) = client.rest("GET", "/api/config/config_entries/flow_handlers?type=integration")
        if (code !in 200..299 || body == null) return null
        return runCatching { JSONArray(body) }.getOrNull()?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).ifEmpty { null } }.sorted()
        }
    }

    suspend fun startFlow(handler: String): Pair<FlowStep?, String?> {
        val (code, body) = client.rest(
            "POST", "/api/config/config_entries/flow",
            JSONObject().put("handler", handler).put("show_advanced_options", false),
        )
        return flowReply(code, body)
    }

    suspend fun continueFlow(flowId: String, data: JSONObject): Pair<FlowStep?, String?> {
        val (code, body) = client.rest("POST", "/api/config/config_entries/flow/$flowId", data)
        return flowReply(code, body)
    }

    suspend fun abortFlow(flowId: String) {
        client.rest("DELETE", "/api/config/config_entries/flow/$flowId")
    }

    private fun flowReply(code: Int, body: String?): Pair<FlowStep?, String?> {
        val error = restError(code, body)
        if (error != null) return null to error
        val step = body?.let { runCatching { JSONObject(it) }.getOrNull() }?.let(::parseFlowStep)
        return step to (if (step == null) "Home Assistant answered with a step Haven cannot show." else null)
    }

    // ---- Devices and entities ----

    suspend fun devices(): List<RegistryDevice>? = list("config/device_registry/list")?.let(::parseDevices)

    suspend fun updateDevice(deviceId: String, build: (JSONObject) -> Unit): String? =
        write("config/device_registry/update", refresh = true) { build(it.put("device_id", deviceId)) }

    suspend fun entities(): List<RegistryEntity>? = list("config/entity_registry/list")?.let(::parseEntityRegistry)

    suspend fun updateEntity(entityId: String, build: (JSONObject) -> Unit): String? =
        write("config/entity_registry/update", refresh = true) { build(it.put("entity_id", entityId)) }

    // ---- Helpers ----

    /** Every UI-made helper, by domain; a domain this version lacks is skipped. */
    suspend fun helpers(): List<HaHelper> = HELPER_DOMAINS.flatMap { domain ->
        parseHelpers(domain, client.command("$domain/list", timeoutMs = 5_000))
    }.sortedBy { it.name.lowercase() }

    suspend fun deleteHelper(helper: HaHelper): String? =
        write("${helper.domain}/delete") { it.put("${helper.domain}_id", helper.id) }

    // ---- Automations, scenes, scripts ----

    /** Deletes a UI-made automation, scene or script by its config id. */
    suspend fun deleteConfig(kind: String, configId: String): String? =
        client.rest("DELETE", "/api/config/$kind/config/$configId").let { (c, b) -> restError(c, b) }

    // ---- Areas, floors, labels, zones ----

    suspend fun floors(): List<HaFloor>? = list("config/floor_registry/list")?.let(::parseFloors)

    suspend fun createFloor(name: String): String? = write("config/floor_registry/create") { it.put("name", name) }

    suspend fun deleteFloor(id: String): String? = write("config/floor_registry/delete") { it.put("floor_id", id) }

    suspend fun setAreaFloor(areaId: String, floorId: String?): String? = write("config/area_registry/update", refresh = true) {
        it.put("area_id", areaId).put("floor_id", floorId ?: JSONObject.NULL)
    }

    suspend fun labels(): List<HaLabel>? = list("config/label_registry/list")?.let(::parseLabels)

    suspend fun createLabel(name: String): String? = write("config/label_registry/create") { it.put("name", name) }

    suspend fun deleteLabel(id: String): String? = write("config/label_registry/delete") { it.put("label_id", id) }

    suspend fun zones(): List<HaZone>? = list("zone/list")?.let(::parseZones)

    suspend fun createZone(name: String, latitude: Double, longitude: Double, radius: Double): String? = write("zone/create") {
        it.put("name", name).put("latitude", latitude).put("longitude", longitude).put("radius", radius).put("passive", false)
    }

    suspend fun updateZone(zone: HaZone): String? = write("zone/update") {
        it.put("zone_id", zone.id).put("name", zone.name).put("latitude", zone.latitude)
            .put("longitude", zone.longitude).put("radius", zone.radius).put("passive", zone.passive)
    }

    suspend fun deleteZone(id: String): String? = write("zone/delete") { it.put("zone_id", id) }

    // ---- People ----

    suspend fun people(): List<HaPerson>? {
        val msg = client.command("person/list") ?: return null
        if (!msg.optBoolean("success", false)) return null
        return parsePeople(msg)
    }

    suspend fun createPerson(name: String): String? = write("person/create") {
        it.put("name", name).put("device_trackers", JSONArray())
    }

    suspend fun updatePerson(person: HaPerson, name: String, trackers: List<String>): String? = write("person/update") {
        it.put("person_id", person.id).put("name", name).put("device_trackers", JSONArray(trackers))
            .put("user_id", person.userId ?: JSONObject.NULL)
    }

    suspend fun deletePerson(id: String): String? = write("person/delete") { it.put("person_id", id) }

    /** Accounts, for owners and admins; null for anyone else, as Home Assistant refuses. */
    suspend fun users(): List<HaUser>? = list("config/auth/list")?.let(::parseUsers)

    // ---- System ----

    suspend fun repairs(): List<RepairIssue>? = list("repairs/list_issues", key = "issues")?.let(::parseRepairs)

    suspend fun ignoreRepair(issue: RepairIssue, ignore: Boolean): String? = write("repairs/ignore_issue") {
        it.put("domain", issue.domain).put("issue_id", issue.issueId).put("ignore", ignore)
    }

    suspend fun logs(): List<LogEntry>? = list("system_log/list")?.let(::parseLogs)

    suspend fun backups(): List<HaBackup>? {
        val msg = client.command("backup/info") ?: return null
        if (!msg.optBoolean("success", false)) return null
        return parseBackups(msg)
    }

    /**
     * Starts a backup with the settings chosen in Home Assistant, or, on
     * a version without that, a plain full backup.
     */
    suspend fun createBackup(): String? {
        val automatic = client.command("backup/generate_with_automatic_settings")
        if (automatic != null && automatic.optBoolean("success", false)) return null
        return write("backup/generate") { }
    }

    suspend fun coreConfig(): CoreConfig? = parseCoreConfig(client.command("get_config"))

    suspend fun updateCore(build: (JSONObject) -> Unit): String? = write("config/core/update", build = build)

    /** "valid", or Home Assistant's list of what is wrong in the configuration. */
    suspend fun checkConfig(): String {
        val (code, body) = client.rest("POST", "/api/config/core/check_config")
        if (code !in 200..299) return restError(code, body) ?: "Could not check the configuration."
        val json = body?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return "Could not read the answer."
        return if (json.optString("result") == "valid") "valid" else json.optString("errors").ifEmpty { "The configuration is not valid." }
    }
}
