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
    "playing" to "Now playing",
    "lights" to "Lights",
    "alerts" to "Needs attention",
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
