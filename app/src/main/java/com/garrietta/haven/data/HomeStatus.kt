package com.garrietta.haven.data

/**
 * Stage D (redesign): the Home Status summary — a unified, honest view
 * of what needs attention, derived ONLY from live Home Assistant states.
 * No invented metrics: a category appears only when at least one real
 * entity is in the flagged state, and unknown/unavailable entities are
 * never counted as flagged (a lock whose state is unknown is neither
 * "locked" nor "unlocked" — it is absent, and its own tile shows it).
 *
 * Per-category visibility uses the same hidden-sections set as the Home
 * sections, with keys "status:<category-key>" (see HomeEditor).
 */
enum class HomeStatusCategory(val key: String, val label: String) {
    LOCKS("locks", "Locks"),
    DOORS("doors", "Doors & windows"),
    LIGHTS("lights", "Lights on"),
    MOTION("motion", "Motion"),
    ALARM("alarm", "Alarm"),
    ENERGY("energy", "Energy"),
}

/** One flagged Home Status row: its category, summary, and the entities behind it. */
data class HomeStatusItem(
    val category: HomeStatusCategory,
    val title: String,
    val detail: String,
    val entities: List<HaEntity>,
)

private fun deviceClass(entity: HaEntity): String? = entity.attrString("device_class")

private fun humanizeState(state: String): String =
    state.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }

private fun countDetail(entities: List<HaEntity>, singular: String, plural: String): String =
    if (entities.size == 1) entities[0].friendlyName else "${entities.size} $plural"

/**
 * Derives the flagged status rows from live entities, in brief order:
 * unlocked locks, open doors/windows, lights on, active motion, armed
 * alarm panels, current power draw. [lightOverrides] applies the Home
 * lights visibility rule (indicator lights excluded unless explicitly
 * re-added) so a device LED cannot keep "Lights on" permanently lit.
 * Pure and unit-tested.
 */
fun deriveHomeStatus(
    entities: List<HaEntity>,
    lightOverrides: Map<String, Boolean> = emptyMap(),
): List<HomeStatusItem> {
    val out = mutableListOf<HomeStatusItem>()

    val unlocked = entities.filter { it.domain == "lock" && it.state == "unlocked" }
    if (unlocked.isNotEmpty()) {
        out.add(
            HomeStatusItem(
                HomeStatusCategory.LOCKS,
                "Unlocked",
                countDetail(unlocked, "lock unlocked", "locks unlocked"),
                unlocked,
            )
        )
    }

    val open = entities.filter {
        (it.domain == "binary_sensor" && deviceClass(it) in setOf("door", "window") && it.state == "on") ||
            (it.domain == "cover" && it.state in setOf("open", "opening"))
    }
    if (open.isNotEmpty()) {
        out.add(
            HomeStatusItem(
                HomeStatusCategory.DOORS,
                "Open",
                countDetail(open, "open", "doors/windows open"),
                open,
            )
        )
    }

    val lightsOn = entities.filter {
        it.domain == "light" && it.state == "on" && isLightShownOnHome(it, lightOverrides)
    }
    if (lightsOn.isNotEmpty()) {
        out.add(
            HomeStatusItem(
                HomeStatusCategory.LIGHTS,
                "Lights on",
                countDetail(lightsOn, "light on", "lights on"),
                lightsOn,
            )
        )
    }

    val motion = entities.filter {
        it.domain == "binary_sensor" && deviceClass(it) == "motion" && it.state == "on"
    }
    if (motion.isNotEmpty()) {
        out.add(
            HomeStatusItem(
                HomeStatusCategory.MOTION,
                "Motion detected",
                countDetail(motion, "motion active", "sensors active"),
                motion,
            )
        )
    }

    val armed = entities.filter {
        it.domain == "alarm_control_panel" &&
            it.state != "disarmed" && it.state != "unavailable" && it.state != "unknown"
    }
    if (armed.isNotEmpty()) {
        out.add(
            HomeStatusItem(
                HomeStatusCategory.ALARM,
                humanizeState(armed[0].state),
                countDetail(armed, armed[0].friendlyName, "alarms"),
                armed,
            )
        )
    }

    val powerSensors = entities.filter {
        it.domain == "sensor" && deviceClass(it) == "power" && it.state.toDoubleOrNull() != null
    }
    if (powerSensors.isNotEmpty()) {
        val watts = powerSensors.sumOf { it.state.toDouble() }
        val rounded = if (watts == watts.toInt().toDouble()) watts.toInt().toString() else "%.1f".format(watts)
        out.add(
            HomeStatusItem(
                HomeStatusCategory.ENERGY,
                "Power now",
                "$rounded W across ${powerSensors.size} sensor" + if (powerSensors.size == 1) "" else "s",
                powerSensors,
            )
        )
    }

    return out
}
