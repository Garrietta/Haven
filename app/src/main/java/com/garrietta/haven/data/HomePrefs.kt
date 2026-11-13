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
    // Stage D: Home Status (real entities only) and Favorites, both
    // hideable like every other section. Per-category Status toggles
    // use "status:<category-key>" keys in the same hidden set.
    "status" to "Home Status",
    "favorites" to "Favorites",
    // Stage G: the Round 3 blocks (vacuum, media, security,
    // greenhouse, updates) are hideable and orderable like every
    // other Home block.
    // Visual-fidelity round §4: the combined "round3" toggle is
    // UNBUNDLED into four independent toggles (Security, Robot
    // vacuum, TV & media, Greenhouse & irrigation). The legacy
    // "round3" key is migrated at every read site (see
    // migrateLegacySectionOrder / migrateLegacyHiddenSections below) so no
    // stored preference is ever lost: combined-on maps to all four
    // on, combined-off maps to all four off.
    "round3-security" to "Security",
    "round3-vacuum" to "Robot vacuum",
    "round3-media" to "TV & media",
    "round3-greenhouse" to "Greenhouse & irrigation",
)
// Room sections are toggled by "room:<areaId>" keys in the same
// hidden-sections set (see HomeEditorScreen); they are not listed
// here because rooms come from the HA area registry, not a fixed list.
// Legacy keys ("playing", "lights", "alerts") from the flat Home
// layout are ignored: those sections no longer exist — their
// entities now live in their rooms.

/**
 * Stage G: the canonical Home section order (the layout as shipped
 * before arrangement existed). Stored orders resolve against it.
 * The greeting is NOT orderable — it is the header, always first
 * (like Home in the pill bar) — and the room-filter chips stay pinned
 * after it (they are a filter control, not a section). Orderable
 * blocks are everything below: hero, weather, who's-home, status,
 * favorites, round-3 blocks, rooms.
 */
/**
 * UI rewrite round §4: the fresh-state default — Greeting, Cameras,
 * Home Status, Favorites shown; everything else hidden. Governs
 * DEFAULTS ONLY: it applies exactly when the user has never stored a
 * hidden-sections preference (the key is absent). A stored set — even
 * an empty one (the user explicitly showed everything) — always wins,
 * and this constant never overwrites it. Rooms are not part of this
 * rule (room visibility is governed by their own room toggles,
 * default shown).
 */
val DEFAULT_HIDDEN_HOME_SECTIONS: Set<String> = setOf(
    "weather", "home",
    "round3-security", "round3-vacuum", "round3-media", "round3-greenhouse",
)

val HOME_SECTION_ORDER_DEFAULT: List<String> = listOf(
    "cameras", "weather", "home", "status", "favorites",
    "round3-security", "round3-vacuum", "round3-media", "round3-greenhouse",
    "rooms",
)

/**
 * Visual-fidelity round §4: the four Round 3 block keys, in the
 * position the combined "round3" key used to occupy.
 */
val ROUND3_BLOCK_KEYS: List<String> = listOf(
    "round3-security", "round3-vacuum", "round3-media", "round3-greenhouse",
)

/**
 * Migrates a stored Home section order from the combined "round3"
 * key to the four independent keys, in place (same position, same
 * relative order). Orders without the legacy key pass through
 * untouched. Pure and unit-tested. Applied at read sites (Today,
 * HomeEditor) rather than by rewriting stored orders, so a stored
 * order is never mutated by merely rendering it.
 */
fun migrateLegacySectionOrder(stored: List<String>): List<String> =
    stored.flatMap { if (it == "round3") ROUND3_BLOCK_KEYS else listOf(it) }

/**
 * Migrates a stored hidden-sections set from the combined "round3"
 * key to the four independent keys: combined-hidden maps to all four
 * hidden, combined-shown (or absent) maps to all four shown. The
 * legacy key itself is always dropped. Pure and unit-tested.
 */
fun migrateLegacyHiddenSections(hidden: Set<String>): Set<String> =
    if ("round3" in hidden) hidden - "round3" + ROUND3_BLOCK_KEYS.toSet()
    else hidden - "round3"

/**
 * Stage G: resolves a stored arrangement against the canonical order.
 * Stored keys not in [canonical] are dropped (a removed section can
 * never break the layout); canonical keys missing from [stored] are
 * appended in canonical order (a new section can never vanish from an
 * old stored order); duplicates keep their first occurrence. Pure and
 * unit-tested. Used for Home sections AND room order (same contract).
 */
fun resolveOrder(stored: List<String>, canonical: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    val out = mutableListOf<String>()
    for (key in stored) {
        if (key in canonical && seen.add(key)) out.add(key)
    }
    for (key in canonical) {
        if (seen.add(key)) out.add(key)
    }
    return out
}

/**
 * Round 24, item 6: moves [item] by [delta] positions within
 * [list] (negative = toward the start), clamping at both ends.
 * A move that would not change the position returns the list
 * unchanged (same instance); an unknown item returns the list
 * unchanged. Pure, unit-tested. The drag-reorder gesture calls
 * this once per threshold crossing, so a long drag walks the
 * card across the grid one slot at a time.
 */
fun <T> movedByDelta(list: List<T>, item: T, delta: Int): List<T> {
    val i = list.indexOf(item)
    if (i < 0 || delta == 0) return list
    val j = (i + delta).coerceIn(list.indices)
    if (i == j) return list
    val out = list.toMutableList()
    val moved = out.removeAt(i)
    out.add(j, moved)
    return out
}

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
 * Above horizon" tile), and update (firmware/instance rows).
 * Feedback round 17, item 5, closes the standing residual:
 * conversation.home_assistant ("Home Assistant", state unknown)
 * is the actual "Home Assistant / Unknown" tile under No room
 * (the brief's sensor.home_assistant does not exist in the
 * registry — both ids are excluded outright), and any
 * unknown/unavailable entity in a system-service domain
 * (conversation, stt, tts, notify) is excluded too — those are
 * HA services, never room devices. The state rule is NARROW
 * on purpose: an unavailable real device (an offline switch,
 * a dead media player) still shows under No room, so it can be
 * assigned. Excluding them here — and from the RoomManager
 * unassigned list, so its counts agree with Home — keeps "No
 * room" to real unassigned devices only. Pure, unit-tested.
 */
fun isHomeRoomEntity(entity: HaEntity): Boolean {
    if (entity.entityId == "sensor.home_assistant" || entity.entityId == "conversation.home_assistant") return false
    if (entity.domain in setOf("conversation", "stt", "tts", "notify") &&
        (entity.state == "unknown" || entity.state == "unavailable")
    ) return false
    return entity.domain !in setOf(
        "camera", "weather", "person", "device_tracker",
        "zone", "sun", "update",
    )
}

/**
 * Feedback round 17, item 4: the device-count label for room
 * headers ("1 device", never "1 devices"). Pure, unit-tested.
 */
fun deviceCountLabel(count: Int): String = when (count) {
    1 -> "1 device"
    else -> "$count devices"
}

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
