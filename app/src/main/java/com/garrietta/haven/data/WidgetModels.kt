package com.garrietta.haven.data

/*
 * What Haven's home screen widgets show, as pure functions of the states,
 * so the widgets draw only what these return and the rules are tested.
 */

/** The Climate widget: the main thermostat in a few numbers. */
data class ClimateWidgetModel(
    val entityId: String,
    val name: String,
    val current: Double?,
    /** The one set point, or null for a heat-and-cool range. */
    val target: Double?,
    val low: Double?,
    val high: Double?,
    val activity: String,
    val humidity: Double?,
    /** "heating", "cooling" or "idle": what color the widget wears. */
    val mood: String,
    val step: Double,
)

/**
 * The thermostat the Climate widget shows: one that is heating or cooling
 * first, else the first by name.
 */
fun climateWidgetModel(states: Map<String, HaEntity>): ClimateWidgetModel? {
    val thermostats = climateThermostats(states)
    val t = thermostats.firstOrNull { it.attrString("hvac_action") == "heating" || it.attrString("hvac_action") == "cooling" }
        ?: thermostats.firstOrNull()
        ?: return null
    val action = t.attrString("hvac_action")
    return ClimateWidgetModel(
        entityId = t.entityId,
        name = t.friendlyName,
        current = t.attrDouble("current_temperature"),
        target = t.attrDouble("temperature"),
        low = t.attrDouble("target_temp_low"),
        high = t.attrDouble("target_temp_high"),
        activity = thermostatActivity(t),
        humidity = t.attrDouble("current_humidity"),
        mood = when {
            t.state == "off" -> "off"
            action == "heating" || (action == null && t.state == "heat") -> "heating"
            action == "cooling" || (action == null && t.state == "cool") -> "cooling"
            else -> "idle"
        },
        step = setpointStep(t),
    )
}

/** One switch on the Controls widget. */
data class ControlTile(val entityId: String, val name: String, val on: Boolean, val available: Boolean)

private val TOGGLEABLE = setOf("light", "switch", "fan", "input_boolean", "humidifier", "siren")

/** The domains a Controls widget tile can switch on and off with a tap. */
fun isWidgetToggleable(entityId: String): Boolean = entityId.substringBefore('.') in TOGGLEABLE

/**
 * The Controls widget's switches: the favorites that switch on and off,
 * in their order; with no such favorite, the lights that are on, so the
 * widget is useful before anything has been chosen. At most [max].
 */
fun controlTiles(states: Map<String, HaEntity>, favorites: List<String>, max: Int = 6): List<ControlTile> {
    val chosen = favorites.filter(::isWidgetToggleable).mapNotNull(states::get)
    val source = chosen.ifEmpty {
        states.values.filter { it.domain == "light" && it.state == "on" }.sortedBy { it.friendlyName.lowercase() }
    }
    return source.take(max).map {
        ControlTile(it.entityId, it.friendlyName, on = it.state == "on", available = it.state != "unavailable")
    }
}

/** The thermostat's next set point after one tap of − or + (or of the heat end of a range). */
fun nudgedSetpoint(model: ClimateWidgetModel, up: Boolean): Double? {
    val base = model.target ?: model.low ?: return null
    return base + if (up) model.step else -model.step
}
