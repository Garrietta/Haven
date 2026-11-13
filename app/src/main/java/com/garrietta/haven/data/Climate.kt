package com.garrietta.haven.data

/*
 * The Climate tab's rules, kept pure: which entities are thermostats and
 * which are readings, how a device's temperature and humidity become one
 * card, which readings are outdoors, and the one line that sums the house
 * up. ClimateScreen draws what these return.
 */

/** One place's air: a device's (or a lone sensor's) temperature and humidity, either of which may be missing. */
data class ClimateReading(
    /** The device id, or the entity id of a sensor that belongs to no device. */
    val key: String,
    val name: String,
    val areaId: String?,
    val temperature: HaEntity?,
    val humidity: HaEntity?,
    val outdoor: Boolean,
) {
    val temperatureValue: Double? get() = temperature?.state?.toDoubleOrNull()
    val humidityValue: Double? get() = humidity?.state?.toDoubleOrNull()
}

private val TEMPERATURE_UNITS = setOf("°F", "°C", "K")

/** A sensor that measures air temperature, by its class or, lacking one, its unit. */
fun isTemperatureSensor(e: HaEntity): Boolean {
    if (e.domain != "sensor") return false
    val cls = e.attrString("device_class")
    if (cls != null) return cls == "temperature"
    return e.attrString("unit_of_measurement") in TEMPERATURE_UNITS
}

/** A sensor that measures relative humidity, by its class or its id and unit. */
fun isHumiditySensor(e: HaEntity): Boolean {
    if (e.domain != "sensor") return false
    val cls = e.attrString("device_class")
    if (cls != null) return cls == "humidity"
    return e.attrString("unit_of_measurement") == "%" && e.entityId.contains("humidity")
}

private val OUTDOOR_WORDS = listOf("outdoor", "outside", "exterior", "weather", "porch", "patio", "yard")

/** Whether a name reads as outdoors: "Outdoor temperature", "Porch sensor". */
fun soundsOutdoor(name: String): Boolean = OUTDOOR_WORDS.any { name.contains(it, ignoreCase = true) }

/** "Bedroom Temperature" reads as "Bedroom" once the card says what it measures. */
fun climateName(name: String): String {
    val trimmed = name.replace(Regex("(?i)\\s*(current\\s+)?(temperature|humidity)\\s*$"), "").trim()
    return trimmed.ifBlank { name }
}

/**
 * Every temperature and humidity reading in the house, one card per
 * device: an ecobee remote sensor's temperature and humidity are one
 * place, not two. A sensor on no device is a card of its own. Entities
 * Home Assistant files as configuration or diagnostics ([skip]), and
 * the thermostats' own climate entities, are left to their own places.
 * Outdoor readings are marked, by name, so they can stand apart.
 */
fun climateReadings(
    states: Map<String, HaEntity>,
    entityDevices: Map<String, String>,
    deviceNames: Map<String, String>,
    entityAreas: Map<String, String>,
    skip: Set<String> = emptySet(),
    /** Area names, for a sensor whose own name says only what it measures ("Temperature"). */
    areaNames: Map<String, String> = emptyMap(),
): List<ClimateReading> {
    val sensors = states.values.filter { it.entityId !in skip && (isTemperatureSensor(it) || isHumiditySensor(it)) }
    return sensors.groupBy { entityDevices[it.entityId] ?: it.entityId }.map { (key, group) ->
        val temperature = group.filter(::isTemperatureSensor).minByOrNull { it.entityId.length }
        val humidity = group.filter(::isHumiditySensor).minByOrNull { it.entityId.length }
        val areaId = group.firstNotNullOfOrNull { entityAreas[it.entityId] }
        val ownName = climateName((temperature ?: humidity)!!.friendlyName)
        val generic = ownName.equals("temperature", true) || ownName.equals("humidity", true)
        val name = deviceNames[key] ?: (if (generic) areaId?.let(areaNames::get) else null) ?: ownName
        ClimateReading(
            key = key,
            name = name,
            areaId = areaId,
            temperature = temperature,
            humidity = humidity,
            outdoor = soundsOutdoor(name) || group.any { soundsOutdoor(it.friendlyName) },
        )
    }.sortedWith(compareBy({ it.outdoor }, { it.temperature == null }, { it.name.lowercase() }))
}

/** The thermostats, by name. */
fun climateThermostats(states: Map<String, HaEntity>): List<HaEntity> =
    states.values.filter { it.domain == "climate" }.sortedBy { it.friendlyName.lowercase() }

/** What a thermostat is doing, in a few words: "Heating to 70°", "Idle · set to 68°", "Off". */
fun thermostatActivity(t: HaEntity): String {
    if (t.state == "off") return "Off"
    if (t.state == "unavailable") return "Unavailable"
    val low = t.attrDouble("target_temp_low")
    val high = t.attrDouble("target_temp_high")
    val target = t.attrDouble("temperature")
    val setTo = when {
        target != null -> "${fmtDegrees(target)}°"
        low != null && high != null -> "${fmtDegrees(low)}–${fmtDegrees(high)}°"
        else -> null
    }
    return when (t.attrString("hvac_action")) {
        "heating" -> if (setTo != null) "Heating to $setTo" else "Heating"
        "cooling" -> if (setTo != null) "Cooling to $setTo" else "Cooling"
        "fan" -> "Fan only"
        "drying" -> "Drying"
        else -> if (setTo != null) "Idle · set to $setTo" else "Idle"
    }
}

/** A temperature as the screen shows it: whole when whole, else one decimal. */
fun fmtDegrees(v: Double): String =
    if (v == Math.rint(v)) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v)

/**
 * The house in one line: what it is inside, what it is outside, the
 * humidity. Inside is what the thermostats read, the number the house is
 * kept at; with no thermostat it is the average of the indoor readings
 * (a greenhouse or a garage would pull an average away from the house).
 */
data class ClimateSummary(val inside: Double?, val outside: Double?, val humidity: Double?, val unit: String)

fun climateSummary(
    readings: List<ClimateReading>,
    thermostats: List<HaEntity>,
    weather: HaEntity?,
): ClimateSummary {
    val indoor = readings.filterNot { it.outdoor }
    val insideTemps = thermostats.mapNotNull { it.attrDouble("current_temperature") }
        .ifEmpty { indoor.mapNotNull { it.temperatureValue } }
    val humidities = thermostats.mapNotNull { it.attrDouble("current_humidity") }
        .ifEmpty { indoor.mapNotNull { it.humidityValue } }
    val outside = readings.firstOrNull { it.outdoor }?.temperatureValue
        ?: weather?.attrDouble("temperature")
    val unit = (indoor.firstNotNullOfOrNull { it.temperature?.attrString("unit_of_measurement") }
        ?: weather?.attrString("temperature_unit") ?: "°F").removePrefix("°")
    return ClimateSummary(
        inside = insideTemps.takeIf { it.isNotEmpty() }?.average(),
        outside = outside,
        humidity = humidities.takeIf { it.isNotEmpty() }?.average(),
        unit = unit,
    )
}

/**
 * Where a temperature sits between cold and hot, 0 to 1, for the bar on
 * each reading: 55°F (13°C) is the cold end and 85°F (29°C) the hot.
 */
fun comfortPosition(value: Double, unit: String): Float {
    val (cold, hot) = if (unit.uppercase() == "C") 13.0 to 29.0 else 55.0 to 85.0
    return ((value - cold) / (hot - cold)).coerceIn(0.0, 1.0).toFloat()
}

/** A thermostat mode, as the chips say it. */
fun hvacModeLabel(mode: String): String = when (mode) {
    "heat" -> "Heat"
    "cool" -> "Cool"
    "heat_cool" -> "Heat & cool"
    "auto" -> "Auto"
    "off" -> "Off"
    "dry" -> "Dry"
    "fan_only" -> "Fan"
    else -> presetLabel(mode)
}

/** An ecobee comfort setting or a fan mode, as the chips say it: "away_indefinitely" reads "Away indefinitely". */
fun presetLabel(preset: String): String =
    preset.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

/** How far one tap of − or + moves a set point: the thermostat's own step, else a whole degree. */
fun setpointStep(t: HaEntity): Double = t.attrDouble("target_temp_step")?.takeIf { it > 0 } ?: 1.0
