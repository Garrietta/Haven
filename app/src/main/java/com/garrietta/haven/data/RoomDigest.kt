package com.garrietta.haven.data

import com.garrietta.haven.ui.theme.StateFamily

/*
 * Home as rooms.
 *
 * The first screen answers "what is the house doing" in words and then
 * room by room, so everything here turns a set of entities into
 * something a person can read: one sentence for the house, and for each
 * room a color, a sentence and the few things in it worth a switch.
 * It is all pure, so the rules are tested without a screen.
 */

/** The devices a person operates. Sensors and helpers are read, not switched. */
private val SWITCHABLE = setOf(
    "light", "switch", "input_boolean", "fan", "climate", "lock", "cover", "media_player", "vacuum",
)

/** The ones a single tap can safely flip. */
private val TAP_TO_TOGGLE = setOf("light", "switch", "input_boolean", "fan")

/** An opening that is news when it is open. Blinds and shades are not. */
private val ENTRANCES = setOf("garage", "door", "gate")

private val NUMBER_WORDS = listOf(
    "No", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
)

/** How many round switches a room card has room for before "+N". */
const val ROOM_SWITCH_SLOTS = 4

fun tapToggles(entity: HaEntity): Boolean = entity.domain in TAP_TO_TOGGLE

private fun HaEntity.known(): Boolean = state.isNotBlank() && state != "unavailable" && state != "unknown"

private fun HaEntity.isOn(): Boolean = known() && domain in TAP_TO_TOGGLE && state == "on"

private fun HaEntity.isEntrance(): Boolean = domain == "cover" && attrString("device_class") in ENTRANCES

/** Unlocked, jammed, or a door left open: the states that want a look. */
private fun HaEntity.needsALook(): Boolean = known() && when {
    domain == "lock" -> state != "locked" && state != "locking"
    isEntrance() -> state == "open" || state == "opening"
    else -> false
}

private fun HaEntity.alertWord(): String = when {
    domain == "lock" && state == "jammed" -> "jammed"
    domain == "lock" -> "unlocked"
    else -> "open"
}

/** Whether a switch for this entity is drawn lit. */
fun switchIsActive(entity: HaEntity): Boolean = entity.known() && when (entity.domain) {
    "climate" -> entity.attrString("hvac_action") in setOf("heating", "cooling")
    "lock" -> true
    "cover" -> entity.state == "open" || entity.state == "opening"
    "media_player" -> entity.state == "playing"
    "vacuum" -> entity.state == "cleaning"
    else -> entity.isOn()
}

/** "21.5°", with no ".0" on a whole number. */
fun degrees(value: Double): String {
    val rounded = Math.round(value * 10) / 10.0
    val text = if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
    return "$text°"
}

/**
 * A device's name as it reads inside its own room: "Living Room Sofa
 * Lamp" in the living room is "Sofa Lamp". The room is only dropped
 * when two words or more are left, so "Garage Door" in the garage stays
 * "Garage Door" rather than becoming "Door".
 */
internal fun nameInRoom(name: String, room: String): String {
    val trimmed = name.trim()
    if (room.isBlank() || !trimmed.startsWith(room, ignoreCase = true)) return trimmed
    val rest = trimmed.substring(room.length).trim()
    return if (rest.split(' ').count { it.isNotBlank() } >= 2) rest else trimmed
}

/** One room, digested for its card on Home. */
data class RoomDigest(
    /** The color the card wears. [StateFamily.Quiet] draws the room as a single resting line. */
    val family: StateFamily,
    /** What is happening, in one line. */
    val summary: String,
    /** The room's temperature, when something in it reports one. */
    val reading: String?,
    /** Entity ids of the round switches, lit ones first. */
    val switches: List<String>,
    /** How many more switchable things the room has than it shows. */
    val more: Int,
) {
    val quiet: Boolean get() = family == StateFamily.Quiet
}

/**
 * Digests a room from its members.
 *
 * The color answers "does this room want attention, and what kind": a
 * door left open outranks a lamp left on, a lamp outranks the heating,
 * and a room whose only news is a locked door is calm blue. Nothing
 * happening is [StateFamily.Quiet], whatever the room contains.
 */
fun roomDigest(room: String, members: List<HaEntity>): RoomDigest {
    val alerts = members.filter { it.needsALook() }
    val lights = members.filter { it.domain == "light" && it.isOn() }
    val otherOn = members.filter { it.domain != "light" && it.isOn() }
    val climate = members.firstOrNull {
        it.domain == "climate" && it.known() && it.attrString("hvac_action") in setOf("heating", "cooling")
    }
    val playing = members.filter { it.domain == "media_player" && it.state == "playing" }
    val cleaning = members.filter { it.domain == "vacuum" && it.state == "cleaning" }
    val locks = members.filter { it.domain == "lock" && it.known() }
    val locked = locks.isNotEmpty() && alerts.none { it.domain == "lock" }

    val family = when {
        alerts.isNotEmpty() -> StateFamily.Alert
        lights.isNotEmpty() || otherOn.isNotEmpty() || cleaning.isNotEmpty() -> StateFamily.On
        climate != null ->
            if (climate.attrString("hvac_action") == "cooling") StateFamily.Cool else StateFamily.Heat
        playing.isNotEmpty() -> StateFamily.Media
        locked -> StateFamily.Secure
        else -> StateFamily.Quiet
    }

    fun name(entity: HaEntity) = nameInRoom(entity.friendlyName, room)
    fun onPhrase(things: List<HaEntity>, plural: String): String? = when (things.size) {
        0 -> null
        1 -> "${name(things[0])} is on"
        2 -> "${name(things[0])} and ${name(things[1])} are on"
        else -> "${things.size} $plural on"
    }
    val parts = buildList {
        alerts.firstOrNull()?.let { add("${name(it)} is ${it.alertWord()}") }
        onPhrase(lights, "lights")?.let(::add)
        onPhrase(otherOn, "things")?.let(::add)
        climate?.let {
            val verb = if (it.attrString("hvac_action") == "cooling") "Cooling" else "Heating"
            add(it.attrDouble("temperature")?.let { target -> "$verb to ${degrees(target)}" } ?: verb)
        }
        playing.firstOrNull()?.let { add("${name(it)} is playing") }
        cleaning.firstOrNull()?.let { add("${name(it)} is cleaning") }
        if (locked) add("Locked")
    }
    val summary = when {
        parts.isEmpty() -> "Everything is off"
        else -> parts.take(2).joinToString(", ")
    }

    val reading = members.firstNotNullOfOrNull { it.takeIf { e -> e.domain == "climate" }?.attrDouble("current_temperature") }
        ?: members.firstNotNullOfOrNull {
            it.takeIf { e -> e.domain == "sensor" && e.attrString("device_class") == "temperature" }
                ?.state?.toDoubleOrNull()
        }

    val switchable = members.filter { it.domain in SWITCHABLE && it.known() }
        .sortedByDescending { switchIsActive(it) }
    return RoomDigest(
        family = family,
        summary = summary,
        reading = reading?.let(::degrees),
        switches = switchable.take(ROOM_SWITCH_SLOTS).map { it.entityId },
        more = (switchable.size - ROOM_SWITCH_SLOTS).coerceAtLeast(0),
    )
}

/**
 * The sentence at the top of Home.
 *
 * [lead] is the lights, because they are what people leave on. The
 * second half is the doors: "[subject] [state]", where [state] is drawn
 * as a pill in [family]'s color. It is absent in a home with no locks
 * and nothing open.
 */
data class HomeHeadline(
    val lead: String,
    val subject: String? = null,
    val state: String? = null,
    val family: StateFamily? = null,
)

fun homeHeadline(entities: Collection<HaEntity>): HomeHeadline {
    val lights = entities.filter { it.domain == "light" && it.known() }
    val on = lights.count { it.state == "on" }
    val lead = when {
        lights.isEmpty() -> "All quiet."
        on == 0 -> "All lights off."
        on == 1 -> "One light on."
        else -> "${NUMBER_WORDS.getOrElse(on) { on.toString() }} lights on."
    }
    fun doorName(entity: HaEntity) = entity.friendlyName.trim().removeSuffix(" Lock").removeSuffix(" lock")

    val alert = entities.firstOrNull { it.needsALook() }
    if (alert != null) {
        return HomeHeadline(lead, "${doorName(alert)} is", alert.alertWord(), StateFamily.Alert)
    }
    val locks = entities.filter { it.domain == "lock" && it.known() }
    return when (locks.size) {
        0 -> HomeHeadline(lead)
        1 -> HomeHeadline(lead, "${doorName(locks[0])} is", "locked", StateFamily.Secure)
        else -> HomeHeadline(lead, "Doors are", "locked", StateFamily.Secure)
    }
}

/** "21.5° inside · 9° outside", from whatever of the two is known. */
fun homeSubline(entities: Collection<HaEntity>, weather: HaEntity?): String? {
    val inside = entities.filter { it.domain == "climate" }
        .mapNotNull { it.attrDouble("current_temperature") }
        .takeIf { it.isNotEmpty() }?.average()
    val outside = weather?.attrDouble("temperature")
    return listOfNotNull(
        inside?.let { "${degrees(it)} inside" },
        outside?.let { "${degrees(it)} outside" },
    ).joinToString(" · ").ifEmpty { null }
}
