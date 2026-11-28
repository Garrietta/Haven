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

/** What a tap on a headline's state word does. */
sealed interface HeadlineAction {
    data class TurnOff(val ids: List<String>) : HeadlineAction
    data class Close(val ids: List<String>) : HeadlineAction
    data class Lock(val ids: List<String>) : HeadlineAction
    /** Sends vacuums back to their docks. */
    data class Dock(val ids: List<String>) : HeadlineAction
    data class Pause(val ids: List<String>) : HeadlineAction
    /** Opens the device's control, for a change one tap should not make: unlocking a door. */
    data class Show(val id: String) : HeadlineAction
}

/**
 * One line of the headline: "[text] [state]." The state word is drawn
 * as a pill in [family]'s color, and tapping it does [action], which is
 * always the way back to rest: what is on goes off, what is open closes.
 */
data class HeadlineClause(
    val text: String,
    val state: String,
    val family: StateFamily,
    val action: HeadlineAction,
)

/**
 * The sentence at the top of Home: what the house is doing, a line for
 * each kind of thing, and nothing for what is at rest. [resting] is the
 * plain line that stands in when no light is on.
 */
data class HomeHeadline(val clauses: List<HeadlineClause>, val resting: String? = null)

/** More lines than this and the sentence pushes the rooms off the screen. */
const val HEADLINE_LINES = 3

private fun count(n: Int): String = NUMBER_WORDS.getOrElse(n) { n.toString() }

private fun doorName(entity: HaEntity): String =
    entity.friendlyName.trim().removeSuffix(" Lock").removeSuffix(" lock")

/**
 * Lights lead, because they are what people leave on; then whatever
 * wants a look (a door open or unlocked); then what is running or
 * playing. A locked door is only worth a line when nothing is open.
 */
fun homeHeadline(entities: Collection<HaEntity>): HomeHeadline {
    val lights = entities.filter { it.domain == "light" && it.known() }
    val lit = lights.filter { it.state == "on" }
    val open = entities.filter { it.isEntrance() && it.needsALook() }
    val unlocked = entities.filter { it.domain == "lock" && it.needsALook() }
    val cleaning = entities.filter { it.domain == "vacuum" && it.known() && it.state == "cleaning" }
    val playing = entities.filter { it.domain == "media_player" && it.state == "playing" }
    val locks = entities.filter { it.domain == "lock" && it.known() }

    val clauses = buildList {
        if (lit.isNotEmpty()) {
            val subject = if (lit.size == 1) "One light" else "${count(lit.size)} lights"
            add(HeadlineClause(subject, "on", StateFamily.Light, HeadlineAction.TurnOff(lit.map { it.entityId })))
        }
        if (open.isNotEmpty()) {
            val subject = if (open.size == 1) "${doorName(open[0])} is" else "${count(open.size)} doors are"
            add(HeadlineClause(subject, "open", StateFamily.Alert, HeadlineAction.Close(open.map { it.entityId })))
        }
        if (unlocked.isNotEmpty()) {
            val subject = if (unlocked.size == 1) "${doorName(unlocked[0])} is" else "${count(unlocked.size)} doors are"
            val word = if (unlocked.size == 1) unlocked[0].alertWord() else "unlocked"
            add(HeadlineClause(subject, word, StateFamily.Alert, HeadlineAction.Lock(unlocked.map { it.entityId })))
        }
        if (cleaning.isNotEmpty()) {
            val subject = if (cleaning.size == 1) "${cleaning[0].friendlyName.trim()} is" else "${count(cleaning.size)} vacuums are"
            add(HeadlineClause(subject, "running", StateFamily.On, HeadlineAction.Dock(cleaning.map { it.entityId })))
        }
        if (playing.isNotEmpty()) {
            val subject = if (playing.size == 1) "${playing[0].friendlyName.trim()} is" else "${count(playing.size)} players are"
            add(HeadlineClause(subject, "playing", StateFamily.Media, HeadlineAction.Pause(playing.map { it.entityId })))
        }
        if (locks.isNotEmpty() && unlocked.isEmpty() && open.isEmpty()) {
            val subject = if (locks.size == 1) "${doorName(locks[0])} is" else "Doors are"
            add(HeadlineClause(subject, "locked", StateFamily.Secure, HeadlineAction.Show(locks[0].entityId)))
        }
    }.take(HEADLINE_LINES)

    val resting = when {
        lit.isNotEmpty() -> null
        lights.isNotEmpty() -> "All lights off."
        clauses.isEmpty() -> "All quiet."
        else -> null
    }
    return HomeHeadline(clauses, resting)
}

/**
 * The line under the headline. [inside] opens [thermostatId], the
 * thermostat it was read from, when tapped.
 */
data class HomeSubline(val inside: String?, val thermostatId: String?, val outside: String?)

/** Inside is the average of the thermostats; outside is the weather. Null when neither is known. */
fun homeSubline(entities: Collection<HaEntity>, weather: HaEntity?): HomeSubline? {
    val thermostats = entities.filter { it.domain == "climate" && it.attrDouble("current_temperature") != null }
    val inside = thermostats.mapNotNull { it.attrDouble("current_temperature") }
        .takeIf { it.isNotEmpty() }?.average()
    val outside = weather?.attrDouble("temperature")
    if (inside == null && outside == null) return null
    return HomeSubline(
        inside = inside?.let { "${degrees(it)} inside" },
        thermostatId = thermostats.firstOrNull()?.entityId,
        outside = outside?.let { "${degrees(it)} outside" },
    )
}
