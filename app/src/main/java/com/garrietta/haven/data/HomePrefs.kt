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
    "greeting" to "Greeting",
    "weather" to "Weather",
    "home" to "Who's home",
    // The camera quadrant hero at the top of Home (2026-10-30).
    "cameras" to "Cameras",
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
