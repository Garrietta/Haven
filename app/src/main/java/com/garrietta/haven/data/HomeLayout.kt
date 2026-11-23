package com.garrietta.haven.data

import com.garrietta.haven.ui.theme.StateFamily

/*
 * How Home organizes the house: the places in it, the devices a person
 * uses in each, and the status deck that answers "is everything all
 * right" before anything is pressed.
 *
 * A place is a Home Assistant area when a device has one. When it has
 * none, the place is read off the device's own name ("Garage Camera",
 * "Greenhouse Heater", "Outdoor camera"), so a home whose areas are only
 * half filled in still falls into rooms. What is left belongs to the
 * whole home (a thermostat, a speaker group).
 *
 * Only what a person controls is a tile. A device's settings (a camera's
 * detection switches, a vacuum's child lock), its readings and its
 * buttons stay on the device's own page, and a device that reaches Home
 * Assistant twice (a plug exposed as a light and a switch, a TV through
 * two integrations) is one tile. Everything here is a pure function, so
 * it is tested without a screen.
 */

/** One place in the house and the devices on its tiles, in order. */
data class HomePlace(
    /** The area id, or "place:" and the name for a place read off device names. */
    val id: String,
    val name: String,
    /** The Home Assistant area, when the place is one. */
    val areaId: String?,
    val entityIds: List<String>,
    /** Temperature and humidity readings in the place, for its heading. */
    val readingIds: List<String> = emptyList(),
)

/** The name of the place for devices that belong to no room. */
const val WHOLE_HOME = "Whole home"

/** What a device-registry entry says about a device: its name and model. */
data class DeviceFacts(val name: String?, val model: String?)

/** Domains a person controls from a tile. */
private val CONTROL_DOMAINS = setOf(
    "light", "switch", "fan", "cover", "lock", "climate", "vacuum", "media_player",
    "valve", "water_heater", "humidifier", "input_boolean",
)

/** A device with one of these is that thing; its switches are its settings. */
private val MAJOR_DOMAINS = setOf("vacuum", "camera", "climate", "media_player", "cover", "lock", "alarm_control_panel")

/** One of these per physical device. */
private val ONE_PER_DEVICE = setOf("vacuum", "climate", "cover", "lock", "media_player", "alarm_control_panel")

/**
 * Words that name a place, matched whole in a device's name. Several
 * words can name one place ("family room" is the living room).
 */
private val PLACE_WORDS: List<Pair<String, String>> = listOf(
    "living room" to "Living Room", "family room" to "Living Room", "lounge" to "Living Room",
    "front door" to "Front Door", "doorbell" to "Front Door", "porch" to "Front Door",
    "back door" to "Back Door",
    "greenhouse" to "Greenhouse", "garage" to "Garage",
    "kitchen" to "Kitchen", "dining" to "Dining Room", "bedroom" to "Bedroom", "office" to "Office",
    "bathroom" to "Bathroom", "laundry" to "Laundry", "basement" to "Basement", "nursery" to "Nursery",
    "hallway" to "Hallway", "attic" to "Attic",
    "backyard" to "Outside", "back yard" to "Outside", "patio" to "Outside", "deck" to "Outside",
    "driveway" to "Outside", "garden" to "Outside", "yard" to "Outside", "outdoor" to "Outside", "outside" to "Outside",
)

/** The order places come in on Home: the whole home, the living spaces, then the edges of the house. */
private val PLACE_ORDER = listOf(
    WHOLE_HOME, "Living Room", "Kitchen", "Dining Room", "Office", "Bedroom", "Nursery", "Bathroom",
    "Laundry", "Hallway", "Basement", "Attic", "Garage", "Front Door", "Back Door", "Outside", "Greenhouse",
)

private fun normalized(text: String): String =
    " " + text.lowercase().replace('’', '\'').replace(Regex("[^a-z0-9']+"), " ").trim() + " "

/** The place [text] names, or null: a Home Assistant area's name first, then the common words. */
fun placeNamed(text: String, areaNames: Collection<String>): String? {
    val haystack = normalized(text)
    areaNames.sortedByDescending { it.length }.firstOrNull { haystack.contains(normalized(it)) }?.let { return it }
    return PLACE_WORDS.firstOrNull { (word, _) -> haystack.contains(normalized(word)) }?.second
}

private fun HaEntity.known(): Boolean = state != "unavailable" && state != "unknown" && state.isNotBlank()

/**
 * The devices on Home's tiles, grouped into places.
 *
 * [entityAreas] is each entity's area (its own or its device's),
 * [entityDevices] its device, [devices] what the registry says about each
 * device, and [auxiliary] the entities Home Assistant marks as settings,
 * diagnostics or hidden. [shownOnHome] decides lights the owner has taken
 * off or put back.
 */
fun homePlaces(
    states: Map<String, HaEntity>,
    areas: List<HaArea>,
    entityAreas: Map<String, String>,
    entityDevices: Map<String, String>,
    devices: Map<String, DeviceFacts>,
    auxiliary: Set<String>,
    shownOnHome: (HaEntity) -> Boolean = { !isIndicatorLight(it) || it.domain != "light" },
): List<HomePlace> {
    val areaById = areas.associateBy { it.id }
    val areaNames = areas.map { it.name }

    // A physical device can be in the registry twice (a TV added by two
    // integrations): the same name and model are taken to be one device.
    fun deviceKey(entityId: String): String? {
        val deviceId = entityDevices[entityId] ?: return null
        val facts = devices[deviceId]
        val name = facts?.name?.lowercase()?.trim()
        val model = facts?.model?.lowercase()?.trim()
        return if (!name.isNullOrEmpty() && !model.isNullOrEmpty()) "$name|$model" else deviceId
    }

    val byDevice = states.values.groupBy { deviceKey(it.entityId) }
    fun deviceHas(key: String?, domains: Set<String>) = key != null && byDevice[key].orEmpty().any { it.domain in domains }

    val candidates = states.values.filter { e ->
        e.domain in CONTROL_DOMAINS &&
            e.entityId !in auxiliary &&
            (e.domain != "light" || shownOnHome(e)) &&
            !isDetectionSwitch(e)
    }.filter { e ->
        val key = deviceKey(e.entityId)
        when (e.domain) {
            // A plug shown as a light keeps only the light; a camera's,
            // vacuum's or TV's switches are its settings.
            "switch" -> !deviceHas(key, setOf("light")) && !deviceHas(key, MAJOR_DOMAINS)
            else -> true
        }
    }

    // One per device for the things a device is (a vacuum, a thermostat),
    // preferring one that answers, then one with an area, then the
    // shortest id; and a device's switches lose any that do not answer
    // when another does.
    val kept = candidates.groupBy { deviceKey(it.entityId) to (if (it.domain in ONE_PER_DEVICE) it.domain else it.entityId) }
        .flatMap { (group, members) ->
            if (group.first == null || members.size == 1) members
            else listOf(
                members.sortedWith(
                    compareBy<HaEntity>({ !it.known() }, { entityAreas[it.entityId] == null }, { it.entityId.length }, { it.entityId }),
                ).first(),
            )
        }
        .let { list ->
            val answering = list.filter { it.domain == "switch" && it.known() }.mapNotNull { deviceKey(it.entityId) }.toSet()
            list.filterNot { it.domain == "switch" && !it.known() && deviceKey(it.entityId) in answering }
        }

    fun placeOf(e: HaEntity): Pair<String, String?> {
        entityAreas[e.entityId]?.let { id -> areaById[id]?.let { return it.name to it.id } }
        val deviceName = entityDevices[e.entityId]?.let { devices[it]?.name }.orEmpty()
        val named = placeNamed("$deviceName ${e.friendlyName}", areaNames) ?: return WHOLE_HOME to null
        val area = areas.firstOrNull { it.name.equals(named, ignoreCase = true) }
        return (area?.name ?: named) to area?.id
    }

    val readings = states.values.filter {
        it.domain == "sensor" && it.entityId !in auxiliary && it.known() &&
            it.attrString("device_class") in setOf("temperature", "humidity")
    }

    val grouped = kept.groupBy { placeOf(it) }
    val readingsByPlace = readings.groupBy { placeOf(it) }
    return grouped.map { (place, members) ->
        val (name, areaId) = place
        HomePlace(
            id = areaId ?: "place:" + name.lowercase().replace(' ', '_'),
            name = name,
            areaId = areaId,
            entityIds = members.sortedWith(compareBy({ tileRank(it) }, { it.friendlyName.lowercase() })).map { it.entityId },
            readingIds = readingsByPlace[place].orEmpty().sortedBy { it.attrString("device_class") != "temperature" }.map { it.entityId },
        )
    }.sortedWith(compareBy({ PLACE_ORDER.indexOf(it.name).let { i -> if (i < 0) PLACE_ORDER.size else i } }, { it.name }))
}

/** Within a place: the thermostat, then entrances, lights, the rest. */
private fun tileRank(e: HaEntity): Int = when (e.domain) {
    "climate" -> 0
    "cover", "lock" -> 1
    "light" -> 2
    "switch", "fan", "input_boolean", "valve" -> 3
    "vacuum" -> 4
    "media_player" -> 5
    else -> 6
}

/** Tiles that take the width of two: the thermostat, which shows more than a name and a word. */
fun isWideTile(entity: HaEntity): Boolean = entity.domain == "climate"

private fun temperature(value: Double?): String? = value?.let { "${Math.round(it)}°" }

/** The word or two under a tile's name: what the device is doing. */
fun tileState(entity: HaEntity): String {
    val s = entity.state
    if (entity.domain == "media_player" && (s == "unavailable" || s == "off" || s == "standby")) return "Off"
    if (s == "unavailable") return "Not responding"
    if (s == "unknown") return "Unknown"
    return when (entity.domain) {
        "light" -> if (s == "on") {
            entity.attrInt("brightness")?.let { "${(it * 100 + 127) / 255}%" } ?: "On"
        } else "Off"
        "switch", "fan", "input_boolean" -> if (s == "on") "On" else "Off"
        "cover" -> s.replaceFirstChar { it.uppercase() }
        "lock" -> s.replaceFirstChar { it.uppercase() }
        "vacuum" -> {
            val word = when (s) {
                "docked" -> "Docked"
                "cleaning" -> "Cleaning"
                "returning" -> "Going home"
                "paused" -> "Paused"
                "error" -> "Needs help"
                else -> s.replaceFirstChar { it.uppercase() }
            }
            entity.attrInt("battery_level")?.let { "$word · $it%" } ?: word
        }
        "media_player" -> when (s) {
            "playing" -> entity.attrString("media_title") ?: "Playing"
            "paused" -> "Paused"
            "idle", "on" -> "On"
            else -> s.replaceFirstChar { it.uppercase() }
        }
        "climate" -> climateLine(entity)
        else -> s.replaceFirstChar { it.uppercase() }
    }
}

/** What a thermostat is doing, in a few words: "Heating to 70°", "Holding 66–72°". */
fun climateLine(entity: HaEntity): String {
    val low = temperature(entity.attrDouble("target_temp_low"))
    val high = temperature(entity.attrDouble("target_temp_high"))
    val target = temperature(entity.attrDouble("temperature"))
    return when (entity.attrString("hvac_action")) {
        "heating" -> "Heating to ${target ?: low ?: "set point"}"
        "cooling" -> "Cooling to ${target ?: high ?: "set point"}"
        else -> when (entity.state) {
            "off" -> "Off"
            "heat_cool", "auto" -> if (low != null && high != null) "Holding ${low.dropLast(1)}–$high" else "Auto"
            "heat" -> target?.let { "Heat to $it" } ?: "Heat"
            "cool" -> target?.let { "Cool to $it" } ?: "Cool"
            else -> entity.state.replaceFirstChar { it.uppercase() }
        }
    }
}

/** Whether a tile is "on": lit, running, open, playing; it then wears its family's color. */
fun tileActive(entity: HaEntity): Boolean = entity.known() && when (entity.domain) {
    "light", "switch", "fan", "input_boolean" -> entity.state == "on"
    "cover" -> entity.state == "open" || entity.state == "opening" || entity.state == "closing"
    "lock" -> entity.state != "locked"
    "vacuum" -> entity.state == "cleaning" || entity.state == "returning"
    "media_player" -> entity.state == "playing" || entity.state == "paused"
    "climate" -> entity.attrString("hvac_action") in setOf("heating", "cooling")
    else -> false
}

/** A place's heading line: its readings and what is on in it. "68° · 53% · Lamp is on". */
fun placeLine(place: HomePlace, states: Map<String, HaEntity>): String {
    val parts = mutableListOf<String>()
    // A thermostat's tile already shows the temperature in large type.
    val hasThermostat = place.entityIds.any { states[it]?.domain == "climate" }
    for (id in place.readingIds.take(2).takeUnless { hasThermostat }.orEmpty()) {
        val e = states[id] ?: continue
        val v = e.state.toDoubleOrNull() ?: continue
        parts += if (e.attrString("device_class") == "temperature") "${Math.round(v)}°" else "${Math.round(v)}%"
    }
    val on = place.entityIds.mapNotNull(states::get).filter { tileActive(it) && it.domain != "climate" }
    when (on.size) {
        0 -> {}
        1 -> parts += "${on[0].friendlyName} is ${activeWord(on[0])}"
        else -> parts += "${on.size} on"
    }
    return parts.joinToString(" · ")
}

private fun activeWord(e: HaEntity): String = when (e.domain) {
    "cover" -> e.state
    "lock" -> e.state
    "vacuum" -> "cleaning"
    "media_player" -> "playing"
    else -> "on"
}

/** The status cards across the top of Home. */
enum class StatusKind { Climate, Security, Lights, Garage, Greenhouse, Vacuum, Media }

/** One status card: what it is about, its headline value, a line under it, its color, and what it covers. */
data class StatusCard(
    val kind: StatusKind,
    val title: String,
    val value: String,
    val family: StateFamily,
    val entityIds: List<String>,
)

/**
 * The status deck: one card for each kind of thing the house has, in a
 * fixed order, saying how it is. A card is colored only when something
 * wants attention or is running; at rest it is quiet.
 */
fun homeStatus(states: Map<String, HaEntity>, places: List<HomePlace>): List<StatusCard> {
    val onTiles = places.flatMap { it.entityIds }.mapNotNull(states::get)
    val cards = mutableListOf<StatusCard>()

    states.values.firstOrNull { it.domain == "climate" }?.let { t ->
        val inside = temperature(t.attrDouble("current_temperature"))
        val family = when (t.attrString("hvac_action")) {
            "heating" -> StateFamily.Heat
            "cooling" -> StateFamily.Cool
            else -> StateFamily.Quiet
        }
        cards += StatusCard(StatusKind.Climate, "Climate", inside?.let { "$it inside" } ?: climateLine(t), family, listOf(t.entityId))
    }

    val alarm = states.values.firstOrNull { it.domain == "alarm_control_panel" }
    val openEntrances = states.values.filter {
        (it.domain == "lock" && it.known() && it.state != "locked") ||
            (it.domain == "cover" && it.attrString("device_class") in setOf("door", "garage", "gate") && (it.state == "open" || it.state == "opening"))
    }
    val hasCameras = states.values.any { it.domain == "camera" }
    val locks = states.values.filter { it.domain == "lock" && it.known() }
    if (alarm != null || hasCameras || locks.isNotEmpty() || openEntrances.isNotEmpty()) {
        val (value, family) = when {
            alarm?.state == "triggered" -> "Alarm!" to StateFamily.Alert
            openEntrances.isNotEmpty() -> (if (openEntrances.size == 1) "${openEntrances[0].friendlyName} open" else "${openEntrances.size} open") to StateFamily.Alert
            alarm?.state?.startsWith("armed") == true -> when (alarm.state) {
                "armed_away" -> "Armed away"
                "armed_night" -> "Armed night"
                "armed_vacation" -> "Armed vacation"
                else -> "Armed home"
            } to StateFamily.Secure
            alarm != null && alarm.state == "disarmed" -> "Disarmed" to StateFamily.Quiet
            locks.isNotEmpty() -> (if (locks.size == 1) "Locked" else "All locked") to StateFamily.Secure
            else -> "All closed" to StateFamily.Quiet
        }
        cards += StatusCard(StatusKind.Security, "Security", value, family, listOfNotNull(alarm?.entityId) + openEntrances.map { it.entityId })
    }

    val lights = onTiles.filter { it.domain == "light" }
    if (lights.isNotEmpty()) {
        val lit = lights.filter { it.state == "on" }
        cards += StatusCard(
            StatusKind.Lights, "Lights",
            when (lit.size) { 0 -> "All off"; lights.size -> "All on"; else -> "${lit.size} on" },
            if (lit.isEmpty()) StateFamily.Quiet else StateFamily.Light,
            lights.map { it.entityId },
        )
    }

    states.values.filter { it.domain == "cover" && it.attrString("device_class") == "garage" }.takeIf { it.isNotEmpty() }?.let { doors ->
        val open = doors.filter { it.state == "open" || it.state == "opening" }
        cards += StatusCard(
            StatusKind.Garage, if (doors.size == 1) "Garage" else "Garage doors",
            if (open.isEmpty()) tileState(doors[0]).takeIf { doors.size == 1 } ?: "Closed" else "Open",
            if (open.isEmpty()) StateFamily.Secure else StateFamily.Alert,
            doors.map { it.entityId },
        )
    }

    places.firstOrNull { it.name.equals("Greenhouse", ignoreCase = true) }?.let { gh ->
        val members = gh.entityIds.mapNotNull(states::get)
        val running = members.filter { tileActive(it) }
        val value = when (running.size) {
            0 -> "Resting"
            1 -> "${shortName(running[0].friendlyName, "Greenhouse")} on"
            else -> "${running.size} running"
        }
        cards += StatusCard(StatusKind.Greenhouse, "Greenhouse", value, if (running.isEmpty()) StateFamily.Quiet else StateFamily.On, gh.entityIds)
    }

    onTiles.firstOrNull { it.domain == "vacuum" }?.let { v ->
        cards += StatusCard(StatusKind.Vacuum, "Vacuum", tileState(v), if (tileActive(v)) StateFamily.On else StateFamily.Quiet, listOf(v.entityId))
    }

    onTiles.filter { it.domain == "media_player" }.takeIf { it.isNotEmpty() }?.let { players ->
        val playing = players.filter { it.state == "playing" }
        cards += StatusCard(
            StatusKind.Media, "TV & audio",
            when { playing.size == 1 -> "${playing[0].friendlyName} playing"; playing.size > 1 -> "${playing.size} playing"; else -> "All off" },
            if (playing.isEmpty()) StateFamily.Quiet else StateFamily.Media,
            players.map { it.entityId },
        )
    }
    return cards
}

/** "Greenhouse Heater" in the greenhouse is the "Heater". */
fun shortName(name: String, place: String): String {
    val trimmed = name.replace(Regex("^" + Regex.escape(place) + "\\s+", RegexOption.IGNORE_CASE), "")
    return trimmed.ifBlank { name }
}

/**
 * The people at home, by the name in front of their devices ("Haley's
 * iPhone" is Haley), each once, in the order found. Trackers named for no
 * one count as nobody.
 */
fun peopleHome(trackers: List<HaEntity>): List<String> =
    trackers.filter { it.domain == "device_tracker" && it.state == "home" }
        .mapNotNull { Regex("^([A-Z][\\p{L}-]+)['’]s\\b").find(it.friendlyName)?.groupValues?.get(1) }
        .distinct()
