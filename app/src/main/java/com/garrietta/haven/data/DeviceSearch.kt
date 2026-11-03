package com.garrietta.haven.data

/**
 * Device-aware search (customization round, item 3). Pure grouping and
 * matching over real registry/state data — no device is ever invented.
 *
 * A device row is only ever shown when it has at least one entity in
 * live state (groupEntitiesByDevice builds from the states map, so a
 * registry device with no live entities simply never appears). A
 * device with no single switch surfaces its real entities instead of
 * a fake toggle — the DeviceSheet lists them with taps to their
 * popups, which is exactly the honest shape for the P400M dual
 * outlet (its auto-off switch, turn-off-in number, overheated
 * sensor, and auto-off-at sensor, not a master switch HA never
 * exposed).
 */
data class HaDeviceInfo(val name: String, val model: String?, val manufacturer: String?)

data class DeviceGroup(val deviceId: String, val info: HaDeviceInfo?, val entityIds: List<String>)

/** Group live entities by their registry device id. Entities with no device id are dropped (they are entity-only rows). */
fun groupEntitiesByDevice(
    entities: List<HaEntity>,
    entityDevices: Map<String, String>,
): List<DeviceGroup> {
    val grouped = linkedMapOf<String, MutableList<String>>()
    for (entity in entities) {
        val deviceId = entityDevices[entity.entityId] ?: continue
        // Feedback round, item 8: belt & braces beside the
        // HaClient parse guard — a blank or literal-"null" device
        // id is never a device, from any source.
        if (deviceId.isBlank() || deviceId.equals("null", ignoreCase = true)) continue
        grouped.getOrPut(deviceId) { mutableListOf() }.add(entity.entityId)
    }
    return grouped.entries.map { (deviceId, ids) -> DeviceGroup(deviceId, null, ids.sorted()) }
}

/** Attach registry info to groups built by [groupEntitiesByDevice]. */
fun attachDeviceInfo(groups: List<DeviceGroup>, deviceInfo: Map<String, HaDeviceInfo>): List<DeviceGroup> =
    groups.map { it.copy(info = deviceInfo[it.deviceId]) }

/** Display name for a device row: registry name, else "Device <id>" (never blank, never the literal "Device null"). */
fun deviceDisplayName(group: DeviceGroup): String {
    group.info?.name?.takeIf { it.isNotBlank() }?.let { return it }
    // Feedback round, item 8: a blank or literal-"null" id has no
    // honest "Device <id>" form — "Unknown device" is the floor.
    // (Model/manufacturer stay in the row subtitle, where both
    // call sites already render them — never duplicated into the
    // title.) Groups with such ids are dropped by
    // groupEntitiesByDevice before they reach a row; this is the
    // last-resort guard for direct callers.
    val id = group.deviceId.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    return if (id != null) "Device $id" else "Unknown device"
}

/**
 * Device match for a search query: registry name, model, or
 * manufacturer contains the query, OR any of the device's entities
 * matches by friendly name or entity id. Blank query matches all.
 */
fun deviceMatchesQuery(
    query: String,
    group: DeviceGroup,
    entitiesById: Map<String, HaEntity>,
): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    val info = group.info
    if (info != null) {
        if (info.name.lowercase().contains(q)) return true
        if ((info.model ?: "").lowercase().contains(q)) return true
        if ((info.manufacturer ?: "").lowercase().contains(q)) return true
    }
    return group.entityIds.any { id ->
        val entity = entitiesById[id]
        entity != null && (entity.friendlyName.lowercase().contains(q) || id.lowercase().contains(q))
    }
}
