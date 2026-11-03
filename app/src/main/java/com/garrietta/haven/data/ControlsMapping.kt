package com.garrietta.haven.data

/**
 * Android Device Controls mapping (customization round, item 10).
 * Pure logic, unit-tested: which HA domains become which system
 * control types, and how thermostat actions translate. The service
 * ([HavenControlsService]) only renders these specs and forwards
 * actions to HaClient — no mapping decision lives in the service.
 *
 * Deliberate exclusions (no suitable Controls API template, or a
 * safety footgun): lock (a quick-settings toggle that unlocks is
 * unsafe, and no lock template exists), media_player (no play/pause
 * template; a toggle would mislead), vacuum (no start/dock
 * template), camera (ThumbnailTemplate would need live streaming),
 * sensors and binary_sensors (read-only; no action template fits).
 * Excluded entities simply never appear as controls — never as a
 * control that does nothing.
 */
enum class ControlKind { TOGGLE, RANGE, TOGGLE_RANGE, TEMPERATURE }

/**
 * Feedback round 3, item 6 (video 6d): what a Device Controls tap
 * opens in the app. Climate controls jump to the thermostat
 * popout/page; every other control type opens that entity's
 * EntityDetail — the same surface a home-page card tap opens
 * (popup on unfolded, detail as home cards do). Unknown domains
 * fall through to Detail, never to a dead end: the router only
 * routes ids whose state actually loaded.
 */
sealed interface ControlDeepLinkTarget {
    data class ThermostatPopout(val entityId: String) : ControlDeepLinkTarget
    data class Detail(val entityId: String) : ControlDeepLinkTarget
}

fun controlDeepLinkTarget(entityId: String, domain: String): ControlDeepLinkTarget =
    if (domain == "climate") ControlDeepLinkTarget.ThermostatPopout(entityId)
    else ControlDeepLinkTarget.Detail(entityId)

/**
 * Device-type codes, verified against SDK 35 android.jar
 * (javap -constants android.service.controls.DeviceTypes):
 * LIGHT 13, THERMOSTAT 49, OUTLET 15, SWITCH 21, FAN 8,
 * GARAGE 39, CURTAIN 36, BLINDS 34, SHUTTER 42, DOOR 37,
 * GENERIC_OPEN_CLOSE -3. The service passes these ints straight
 * to Control.StatefulBuilder.setDeviceType; the mapping file keeps
 * its own copies so unit tests never need the Android runtime.
 */
object ControlDeviceType {
    const val LIGHT = 13
    const val THERMOSTAT = 49
    const val OUTLET = 15
    const val SWITCH = 21
    const val FAN = 8
    const val GARAGE = 39
    const val CURTAIN = 36
    const val BLINDS = 34
    const val SHUTTER = 42
    const val DOOR = 37
    const val GENERIC_OPEN_CLOSE = -3
}

data class ControlSpec(
    val entityId: String,
    val title: String,
    val deviceType: Int,
    val kind: ControlKind,
    /** For RANGE: current position 0..100, or null. For TOGGLE_RANGE lights: brightness 0..100, or null. */
    val rangeValue: Float? = null,
    /** For TOGGLE kinds: current on/off. */
    val booleanValue: Boolean? = null,
    /** For TEMPERATURE: current mode string, hvac_modes, current/target temps. */
    val climateMode: String? = null,
    val climateModes: List<String> = emptyList(),
    /** For TEMPERATURE: the active mode from hvac_action (see climateActiveModeFromHvacAction), or null when idle. */
    val climateActiveMode: ClimateMode? = null,
    val currentTemp: Float? = null,
    val targetTemp: Float? = null,
    val targetHigh: Float? = null,
    val targetLow: Float? = null,
    val minTemp: Float? = null,
    val maxTemp: Float? = null,
)

enum class ClimateMode { OFF, HEAT, COOL, HEAT_COOL, ECO }

fun climateModeFromHa(mode: String): ClimateMode? = when (mode) {
    "off" -> ClimateMode.OFF
    "heat" -> ClimateMode.HEAT
    "cool" -> ClimateMode.COOL
    "heat_cool" -> ClimateMode.HEAT_COOL
    "eco" -> ClimateMode.ECO
    else -> null
}

fun climateModeToHa(mode: ClimateMode): String = when (mode) {
    ClimateMode.OFF -> "off"
    ClimateMode.HEAT -> "heat"
    ClimateMode.COOL -> "cool"
    ClimateMode.HEAT_COOL -> "heat_cool"
    ClimateMode.ECO -> "eco"
}

/**
 * Feedback round 2, item 8: the template's "current active mode"
 * is what the system is DOING right now (hvac_action), not the
 * selected mode. A heat_cool thermostat that is heating reports
 * active = HEAT; one that is cooling reports COOL; idle (or any
 * other action) reports null, and the service falls back to the
 * selected mode. The old code passed the selected mode for both
 * currentMode and currentActiveMode, so a heat_cool system
 * advertised HEAT_COOL as its active mode — a mode, not an
 * action — which some panel renderers cannot map to a detail
 * state and answer with the app intent instead of the in-panel
 * temperature widget. Pure, unit-tested.
 */
fun climateActiveModeFromHvacAction(hvacAction: String?): ClimateMode? = when (hvacAction) {
    "heating" -> ClimateMode.HEAT
    "cooling" -> ClimateMode.COOL
    else -> null
}

/** Map one HA entity to a system control spec, or null when the domain is deliberately excluded. */
fun mapEntityToControl(entity: HaEntity): ControlSpec? {
    val title = entity.friendlyName
    return when (entity.domain) {
        "light" -> {
            val brightness = entity.attrInt("brightness")
            if (brightness != null) {
                ControlSpec(entity.entityId, title, ControlDeviceType.LIGHT, ControlKind.TOGGLE_RANGE,
                    rangeValue = brightness * 100f / 255f, booleanValue = entity.state == "on")
            } else {
                ControlSpec(entity.entityId, title, ControlDeviceType.LIGHT, ControlKind.TOGGLE,
                    booleanValue = entity.state == "on")
            }
        }
        "switch" -> {
            val type = if (entity.attrString("device_class") == "outlet") ControlDeviceType.OUTLET else ControlDeviceType.SWITCH
            ControlSpec(entity.entityId, title, type, ControlKind.TOGGLE, booleanValue = entity.state == "on")
        }
        "input_boolean" -> ControlSpec(entity.entityId, title, ControlDeviceType.SWITCH, ControlKind.TOGGLE,
            booleanValue = entity.state == "on")
        "fan" -> ControlSpec(entity.entityId, title, ControlDeviceType.FAN, ControlKind.TOGGLE,
            booleanValue = entity.state == "on")
        "cover" -> {
            val type = when (entity.attrString("device_class")) {
                "garage" -> ControlDeviceType.GARAGE
                "curtain" -> ControlDeviceType.CURTAIN
                "blind" -> ControlDeviceType.BLINDS
                "shutter" -> ControlDeviceType.SHUTTER
                "door" -> ControlDeviceType.DOOR
                else -> ControlDeviceType.GENERIC_OPEN_CLOSE
            }
            val position = entity.attrDouble("current_position")
            if (position != null) {
                ControlSpec(entity.entityId, title, type, ControlKind.RANGE, rangeValue = position.toFloat())
            } else {
                ControlSpec(entity.entityId, title, type, ControlKind.TOGGLE,
                    booleanValue = entity.state == "open" || entity.state == "opening")
            }
        }
        "climate" -> {
            @Suppress("UNCHECKED_CAST")
            val modes = (entity.attributes["hvac_modes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val target = entity.attrDouble("temperature")?.toFloat()
            val high = entity.attrDouble("target_temp_high")?.toFloat()
            val low = entity.attrDouble("target_temp_low")?.toFloat()
            val displayTemp = when {
                entity.state == "heat_cool" && high != null && low != null -> (high + low) / 2f
                else -> target
            }
            ControlSpec(
                entity.entityId, title, ControlDeviceType.THERMOSTAT, ControlKind.TEMPERATURE,
                climateMode = entity.state, climateModes = modes,
                climateActiveMode = climateActiveModeFromHvacAction(entity.attrString("hvac_action")),
                currentTemp = entity.attrDouble("current_temperature")?.toFloat(),
                targetTemp = displayTemp, targetHigh = high, targetLow = low,
                minTemp = entity.attrDouble("min_temp")?.toFloat(),
                maxTemp = entity.attrDouble("max_temp")?.toFloat(),
            )
        }
        else -> null
    }
}

/**
 * Heat/cool band shift (honest single-value control for a dual-band
 * thermostat): the FloatAction value is the NEW midpoint; the whole
 * band moves by (new - old midpoint), clamped so neither end leaves
 * [minTemp, maxTemp] when those attrs exist. The band width is never
 * collapsed to a single value. Returns (newLow, newHigh), or null
 * when there is no band to shift.
 */
fun shiftClimateBand(
    high: Float?, low: Float?, newMidpoint: Float, minTemp: Float?, maxTemp: Float?,
): Pair<Float, Float>? {
    if (high == null || low == null) return null
    val oldMid = (high + low) / 2f
    var delta = newMidpoint - oldMid
    if (minTemp != null) delta = maxOf(delta, minTemp - low)
    if (maxTemp != null) delta = minOf(delta, maxTemp - high)
    return Pair(low + delta, high + delta)
}

/** Single-mode setpoint, clamped to min/max when present. */
fun clampSetpoint(value: Float, minTemp: Float?, maxTemp: Float?): Float {
    var out = value
    if (minTemp != null) out = maxOf(out, minTemp)
    if (maxTemp != null) out = minOf(out, maxTemp)
    return out
}

/** BooleanAction on a thermostat: on -> first non-off mode (heat preferred), off -> "off". */
fun climateBooleanMode(modes: List<String>, on: Boolean): String? {
    if (!on) return if ("off" in modes) "off" else null
    if ("heat" in modes) return "heat"
    return modes.firstOrNull { it != "off" }
}

/**
 * Default control set: the user's Haven favorites first (in favorite
 * order, only those that map), then every other mappable entity
 * sorted by friendly name. The panel's add UI can still offer all
 * mapped entities; favorites-first is the default the user sees.
 */
fun defaultControls(entities: List<HaEntity>, favoriteIds: List<String>): List<ControlSpec> {
    val byId = entities.associateBy { it.entityId }
    val favoriteSpecs = favoriteIds.mapNotNull { byId[it] }.mapNotNull { mapEntityToControl(it) }
    val favoriteSet = favoriteSpecs.map { it.entityId }.toSet()
    val rest = entities.filter { it.entityId !in favoriteSet }
        .mapNotNull { mapEntityToControl(it) }
        .sortedBy { it.title.lowercase() }
    return favoriteSpecs + rest
}
