package com.garrietta.haven.data

/**
 * Home page customization: which sections the Today screen shows, the
 * custom welcome message, and per-light visibility.
 *
 * Section keys are stable storage keys, never display strings, so a label
 * change can't orphan a saved toggle. The greeting is a section too: it is
 * the header row the welcome message replaces.
 */
val HOME_SECTIONS: List<Pair<String, String>> = listOf(
    // Order matches the Home layout (2026-10-30 verdict round):
    // greeting FIRST, then the camera hero, then weather, then
    // who's-home. Room sections follow (room:<areaId> keys).
    "greeting" to "Greeting",
    // The camera quadrant hero at the top of Home (2026-10-30).
    "cameras" to "Cameras",
    "weather" to "Weather",
    "home" to "Who's home",
)
// Room sections are toggled by "room:<areaId>" keys in the same
// hidden-sections set (see HomeEditorScreen); they are not listed
// here because rooms come from the HA area registry, not a fixed list.
// Legacy keys ("playing", "lights", "alerts") from the flat Home
// layout are ignored: those sections no longer exist — their
// entities now live in their rooms.

/**
 * Groups entities by their HA area (2026-10-30 Home reorg):
 * area id -> members, with null for entities assigned to no area.
 * Pure: the Today screen filters domains (cameras to the hero,
 * weather to its block, people to who's-home) and sorts members
 * on top of this grouping. Unit-tested.
 */
fun groupEntitiesByRoom(
    entities: List<HaEntity>,
    entityAreas: Map<String, String>,
): Map<String?, List<HaEntity>> = entities.groupBy { entityAreas[it.entityId] }

/**
 * Resolves each entity to its HA area from the two registry levels
 * (2026-10-30 verdict round): the entity registry's own area_id wins;
 * when an entity has none, its device registry entry's area_id is the
 * fallback (HA's own convention — entities normally carry no area and
 * inherit their device's). An entity with neither stays unmapped
 * (absent from the result, never mapped to a blank area). Pure, so
 * each registry fetch can publish independently: a partial result
 * (entity areas without device areas, or vice versa) still resolves
 * everything it can, and a failed fetch never blanks a map that
 * already resolved. Unit-tested.
 */
fun resolveEntityAreas(
    entityAreas: Map<String, String?>,
    entityDevices: Map<String, String>,
    deviceAreas: Map<String, String?>,
): Map<String, String> {
    val out = mutableMapOf<String, String>()
    for ((entityId, area) in entityAreas) {
        if (!area.isNullOrBlank()) { out[entityId] = area; continue }
        val deviceId = entityDevices[entityId]
        val deviceArea = deviceId?.let { deviceAreas[it] }
        if (!deviceArea.isNullOrBlank()) out[entityId] = deviceArea
    }
    // Entities that appear only via a device link (no entity-registry
    // row at all) still resolve through their device's area.
    for ((entityId, deviceId) in entityDevices) {
        if (entityId !in out) {
            val deviceArea = deviceAreas[deviceId]
            if (!deviceArea.isNullOrBlank()) out[entityId] = deviceArea
        }
    }
    return out
}

/**
 * Whether an entity belongs in Home's room sections at all
 * (2026-10-30 verdict round). Room sections hold real, room-assignable
 * devices; cameras live in the hero, weather in its block, people in
 * who's-home, and the system domains below are never room devices:
 * zone (zone.home renders as the "Home / 1" tile), sun (the "Sun /
 * Above horizon" tile), and update (firmware/instance rows such as
 * "Home Assistant / Unknown"). Excluding them here — and from the
 * RoomManager unassigned list, so its counts agree with Home — keeps
 * "No room" to real unassigned devices only. Pure, unit-tested.
 */
fun isHomeRoomEntity(entity: HaEntity): Boolean =
    entity.domain !in setOf(
        "camera", "weather", "person", "device_tracker",
        "zone", "sun", "update",
    )

/**
 * Default exclusion for the Home Lights section: indicator lights, not room
 * lights. A light is an indicator light when its entity_id contains "dnd"
 * (case-insensitive) OR its friendly name ends with "Dnd"
 * (case-insensitive). No specific entity ids are hardcoded here, so any
 * current or future indicator light is excluded by the same rule, and a
 * renamed room light is never caught by it.
 *
 * This is the DEFAULT only: an explicit per-light choice in the editor
 * always wins (see [isLightShownOnHome]), so a light Garrett re-adds stays
 * added across states refreshes.
 */
fun isIndicatorLight(entity: HaEntity): Boolean =
    entity.entityId.contains("dnd", ignoreCase = true) ||
        entity.friendlyName.endsWith("Dnd", ignoreCase = true)

/**
 * Whether a light belongs on the Home page. An explicit per-light override
 * (entity id -> shown) wins over the default rule in both directions: ON
 * re-adds an indicator light, OFF hides a room light. With no override the
 * default rule applies, computed live from the entity's current id and
 * friendly name on every load — so a states refresh can never silently
 * re-exclude a light Garrett explicitly re-added, and a newly appearing
 * indicator light is excluded without any stored list to go stale.
 */
fun isLightShownOnHome(entity: HaEntity, overrides: Map<String, Boolean>): Boolean =
    overrides[entity.entityId] ?: !isIndicatorLight(entity)
