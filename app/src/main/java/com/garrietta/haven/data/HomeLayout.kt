package com.garrietta.haven.data

import com.garrietta.haven.ui.theme.StateFamily

/*
 * How Home organizes the house: the devices in it, grouped by what they
 * are, and the status deck that answers "is everything all right" before
 * anything is pressed.
 *
 * Home goes by the devices alone. Home Assistant's areas, floors, labels
 * and categories are not read: in a home where they were never kept up
 * they say more about the past than the house. A device belongs to a
 * group by what it is (a light, a thermostat, a door, a TV), except that
 * whatever is named for the greenhouse, the sprinklers or the garden is
 * the Greenhouse, wherever it plugs in.
 *
 * Only active, useful devices are tiles, and only things that belong to a
 * device: a helper made in Home Assistant (an input boolean a dashboard
 * theme left behind, a template sensor) is not a device, and neither is
 * an entry the registry marks as a service (an integration, an add-on).
 * A leftover is left out too: an
 * entity Home Assistant restored from a setup that no longer loads, or
 * one that has not answered for a week ([STALE_DAYS]); a TV that is
 * merely switched off for the evening stays. A device's settings (a
 * camera's detection switches, a vacuum's child lock), its readings and
 * its buttons stay on the device's own page, and a device that reaches
 * Home Assistant twice (a plug exposed as a light and a switch, a TV
 * through two integrations) is one tile. Everything here is a pure
 * function, so it is tested without a screen.
 */

/** One group of devices on Home and the devices on its tiles, in order. */
data class HomePlace(
    /** "group:" and the group's name. */
    val id: String,
    val name: String,
    /** Never set: Home does not read Home Assistant's areas. Kept for the tile options sheet's signature. */
    val areaId: String?,
    val entityIds: List<String>,
    /** Temperature and humidity readings that belong to the group, for its heading. */
    val readingIds: List<String> = emptyList(),
)

/** The groups, in the order Home shows them. */
const val GROUP_CLIMATE = "Climate"
const val GROUP_LIGHTS = "Lights"
const val GROUP_DOORS = "Doors"
const val GROUP_GREENHOUSE = "Greenhouse"
const val GROUP_SWITCHES = "Plugs & switches"
const val GROUP_VACUUM = "Vacuum"
const val GROUP_MEDIA = "TV & audio"
private val GROUP_ORDER = listOf(GROUP_CLIMATE, GROUP_LIGHTS, GROUP_DOORS, GROUP_GREENHOUSE, GROUP_SWITCHES, GROUP_VACUUM, GROUP_MEDIA)

/** A device that has not answered for this many days is a leftover. */
const val STALE_DAYS = 7L

/** What a device-registry entry says about a device: its name, its model, and whether it is a service rather than hardware. */
data class DeviceFacts(val name: String?, val model: String?, val service: Boolean = false)

/** Domains a person controls from a tile. */
private val CONTROL_DOMAINS = setOf(
    "light", "switch", "fan", "cover", "lock", "climate", "vacuum", "media_player",
    "valve", "water_heater", "humidifier", "input_boolean",
)

/** A device with one of these is that thing; its switches are its settings. */
private val MAJOR_DOMAINS = setOf("vacuum", "camera", "climate", "media_player", "cover", "lock", "alarm_control_panel")

/** One of these per physical device. */
private val ONE_PER_DEVICE = setOf("vacuum", "climate", "cover", "lock", "media_player", "alarm_control_panel")

/** Names that make a device part of the greenhouse, whatever it is. */
private val GREENHOUSE_WORDS = listOf("greenhouse", "sprinkler", "irrigation", "garden", "watering", "plant")

private fun normalized(text: String): String =
    " " + text.lowercase().replace('’', '\'').replace(Regex("[^a-z0-9']+"), " ").trim() + " "

private fun HaEntity.known(): Boolean = state != "unavailable" && state != "unknown" && state.isNotBlank()

private fun isoMillis(iso: String?): Long? = iso?.let {
    runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
}

/**
 * Whether [entity] is a leftover rather than a device in use: restored by
 * Home Assistant from a setup that no longer loads, or not answering and
 * not for at least [STALE_DAYS] days as of [nowMillis]. A device that is
 * answering is never a leftover, however old its last change.
 */
fun isLeftover(entity: HaEntity, nowMillis: Long): Boolean {
    if (entity.attrBool("restored") == true) return true
    if (entity.known()) return false
    val since = isoMillis(entity.lastChanged) ?: return false
    return nowMillis - since >= STALE_DAYS * 24 * 60 * 60 * 1000
}

/** The group a device belongs to, by what it is and, for the greenhouse, by its name. */
fun deviceGroup(entity: HaEntity, deviceName: String? = null): String? {
    val names = normalized("${deviceName.orEmpty()} ${entity.friendlyName} ${entity.entityId.substringAfter('.').replace('_', ' ')}")
    if (entity.domain != "climate" && GREENHOUSE_WORDS.any { names.contains(" $it") }) return GROUP_GREENHOUSE
    return when (entity.domain) {
        "climate", "water_heater", "humidifier" -> GROUP_CLIMATE
        "light" -> GROUP_LIGHTS
        "cover", "lock", "valve" -> GROUP_DOORS
        "switch", "fan", "input_boolean" -> GROUP_SWITCHES
        "vacuum" -> GROUP_VACUUM
        "media_player" -> GROUP_MEDIA
        else -> null
    }
}

/**
 * The devices on Home's tiles, in groups.
 *
 * [entityDevices] is each entity's device, [devices] what the registry
 * says about each device, and [auxiliary] the entities Home Assistant
 * marks as settings, diagnostics or hidden. [nowMillis] dates leftovers.
 * [shownOnHome] decides tiles the owner has taken off Home or put back.
 */
fun homeGroups(
    states: Map<String, HaEntity>,
    entityDevices: Map<String, String>,
    devices: Map<String, DeviceFacts>,
    auxiliary: Set<String>,
    nowMillis: Long,
    shownOnHome: (HaEntity) -> Boolean = { isShownOnHome(it, emptyMap()) },
): List<HomePlace> {
    // A physical device can be in the registry twice (a TV added by two
    // integrations): the same name and model are taken to be one device.
    fun deviceKey(entityId: String): String? {
        val deviceId = entityDevices[entityId] ?: return null
        val facts = devices[deviceId]
        val name = facts?.name?.lowercase()?.trim()
        val model = facts?.model?.lowercase()?.trim()
        return if (!name.isNullOrEmpty() && !model.isNullOrEmpty()) "$name|$model" else deviceId
    }

    // Until the registry has arrived nothing can be said about devices,
    // so everything stays; once it has, only what belongs to hardware does.
    val registryKnown = entityDevices.isNotEmpty()
    fun onDevice(entityId: String): Boolean {
        if (!registryKnown) return true
        val deviceId = entityDevices[entityId] ?: return false
        return devices[deviceId]?.service != true
    }
    val live = states.values.filter { !isLeftover(it, nowMillis) && onDevice(it.entityId) }
    val byDevice = live.groupBy { deviceKey(it.entityId) }
    fun deviceHas(key: String?, domains: Set<String>) = key != null && byDevice[key].orEmpty().any { it.domain in domains }

    val candidates = live.filter { e ->
        e.domain in CONTROL_DOMAINS &&
            e.entityId !in auxiliary &&
            shownOnHome(e) &&
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
    // preferring one that answers, then the shortest id; and a device's
    // switches lose any that do not answer when another does.
    val kept = candidates.groupBy { deviceKey(it.entityId) to (if (it.domain in ONE_PER_DEVICE) it.domain else it.entityId) }
        .flatMap { (group, members) ->
            if (group.first == null || members.size == 1) members
            else listOf(members.sortedWith(compareBy<HaEntity>({ !it.known() }, { it.entityId.length }, { it.entityId })).first())
        }
        .let { list ->
            val answering = list.filter { it.domain == "switch" && it.known() }.mapNotNull { deviceKey(it.entityId) }.toSet()
            list.filterNot { it.domain == "switch" && !it.known() && deviceKey(it.entityId) in answering }
        }

    fun deviceName(e: HaEntity) = entityDevices[e.entityId]?.let { devices[it]?.name }
    val grouped = kept.groupBy { deviceGroup(it, deviceName(it)) }.filterKeys { it != null }

    // Readings named for the greenhouse go under it; the thermostat's own
    // tile shows the inside temperature, so Climate's heading needs none.
    val greenhouseReadings = live.filter {
        it.domain == "sensor" && it.entityId !in auxiliary && it.known() &&
            it.attrString("device_class") in setOf("temperature", "humidity") &&
            deviceGroup(HaEntity("switch.x", "off", it.attributes), deviceName(it)) == GROUP_GREENHOUSE
    }.sortedBy { it.attrString("device_class") != "temperature" }.map { it.entityId }

    return grouped.map { (name, members) ->
        HomePlace(
            id = "group:" + name!!.lowercase().replace(' ', '_'),
            name = name,
            areaId = null,
            entityIds = members.sortedWith(compareBy({ tileRank(it) }, { it.friendlyName.lowercase() })).map { it.entityId },
            readingIds = if (name == GROUP_GREENHOUSE) greenhouseReadings else emptyList(),
        )
    }.sortedBy { GROUP_ORDER.indexOf(it.name) }
}

/** Within a group: the thermostat, then entrances, lights, the rest. */
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

/** Words that name a part of a device rather than the device itself. */
private val PART_WORDS = Regex(
    "^(socket|switch|plug|outlet|power|relay|light|main|channel|live|live view|live stream|stream|hd stream|sd stream|snapshot|camera)( \\d+)?( \\(.*\\))?$",
    RegexOption.IGNORE_CASE,
)

/**
 * The name a device goes by in Haven. Home Assistant names an entity
 * after its device and then the part: "Smart Garage Door Opener Garage
 * Door", "Outdoor camera HD Stream (Direct)", "Standing lamp Socket". The
 * part alone is the name when it says what the thing is ("Garage Door");
 * the device's name is when the part is only a socket, a stream or a
 * channel ("Outdoor camera", "Standing lamp"). A name not built on its
 * device's stays as it is.
 */
fun deviceDisplayName(friendlyName: String, deviceName: String?): String {
    val device = deviceName?.trim().orEmpty()
    if (device.isEmpty() || friendlyName.equals(device, ignoreCase = true)) return friendlyName
    if (!friendlyName.startsWith("$device ", ignoreCase = true)) return friendlyName
    val part = friendlyName.substring(device.length).trim()
    return if (part.isEmpty() || PART_WORDS.matches(part)) device else part
}

/** A place's heading line: its readings and what is on in it. "68° · 53% · Lamp is on". */
fun placeLine(place: HomePlace, states: Map<String, HaEntity>, nameOf: (HaEntity) -> String = { it.friendlyName }): String {
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
        1 -> parts += "${shortName(nameOf(on[0]), place.name)} is ${activeWord(on[0])}"
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
fun homeStatus(
    allStates: Map<String, HaEntity>,
    places: List<HomePlace>,
    nowMillis: Long,
    nameOf: (HaEntity) -> String = { it.friendlyName },
): List<StatusCard> {
    val states = allStates.filterValues { !isLeftover(it, nowMillis) }
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
            openEntrances.isNotEmpty() -> (if (openEntrances.size == 1) entranceOpenWord(openEntrances[0]) else "${openEntrances.size} open") to StateFamily.Alert
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

    places.firstOrNull { it.name == GROUP_GREENHOUSE }?.let { gh ->
        val members = gh.entityIds.mapNotNull(states::get)
        val running = members.filter { tileActive(it) }
        val value = when (running.size) {
            0 -> "Resting"
            1 -> "${shortName(nameOf(running[0]), "Greenhouse")} on"
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
            when { playing.isNotEmpty() -> if (playing.size == 1) "Playing" else "${playing.size} playing"; else -> "All off" },
            if (playing.isEmpty()) StateFamily.Quiet else StateFamily.Media,
            players.map { it.entityId },
        )
    }
    return cards
}

/**
 * A status card's value for one open entrance, short enough for the card:
 * what kind of entrance, not its full name, which the card's sheet shows.
 */
private fun entranceOpenWord(e: HaEntity): String = when {
    e.domain == "lock" -> "Unlocked"
    e.attrString("device_class") == "garage" -> "Garage open"
    e.attrString("device_class") == "gate" -> "Gate open"
    else -> "Door open"
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
