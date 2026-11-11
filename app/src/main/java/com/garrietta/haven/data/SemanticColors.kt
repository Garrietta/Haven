package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.components.SemanticState
import com.garrietta.haven.ui.components.sensorFamily
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.theme.Palette
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.attentionDot
import com.garrietta.haven.ui.theme.colors

/*
 * Which color family an entity in a given state gets.
 *
 * This is the decision layer above ui/theme/Color.kt: the families and
 * their tones are defined there, and this file only answers "what does
 * this state mean". Two resolvers share the families so the same state
 * is the same color everywhere:
 *
 *   semanticStateFor / semanticVisual   status rows, attention dots
 *   domainCardColors                    tiles, badges, switch tracks
 *
 * Color is never the only signal. Every surface that takes a state
 * color also shows the state in words, and Unknown and Unavailable keep
 * their explicit labels.
 */

/** A semantic visual: container fill and the text or glyph on it. */
data class SemanticVisual(val container: Color, val onContainer: Color)

/** The family a semantic state is drawn in. Families are shared on purpose: heating and an open door are both warm, cooling and locked are both cool. */
fun semanticFamily(state: SemanticState): StateFamily = when (state) {
    SemanticState.DeviceOn -> StateFamily.On
    SemanticState.LockLocked -> StateFamily.Secure
    SemanticState.LockUnlocked, SemanticState.MotionActive -> StateFamily.Caution
    SemanticState.LockFault, SemanticState.AlarmFault, SemanticState.EnergyCritical -> StateFamily.Alert
    SemanticState.OpenAlert, SemanticState.HeatingActive -> StateFamily.Heat
    SemanticState.CoolingActive -> StateFamily.Cool
    SemanticState.MediaPlaying -> StateFamily.Media
    SemanticState.EnergyLow -> StateFamily.Power
    SemanticState.EnergyHigh -> StateFamily.Caution
    SemanticState.DeviceOff, SemanticState.ClosedNeutral, SemanticState.MotionClear,
    SemanticState.ClimateIdle, SemanticState.MediaIdle, SemanticState.Unavailable,
    SemanticState.Unknown -> StateFamily.Quiet
}

fun semanticVisual(state: SemanticState, isDark: Boolean): SemanticVisual =
    semanticFamily(state).colors(isDark).let { SemanticVisual(it.container, it.onContainer) }

/**
 * Energy thresholds (watts) for power-sensor slots. These are DOCUMENTED
 * Haven defaults, not Home Assistant values — the brief allows explained
 * defaults where no configurable threshold exists yet. Stage G adds user
 * thresholds; until then these numbers are the mapping, stated here so
 * a later round can find and change them in one place.
 */
const val ENERGY_WARN_WATTS_DEFAULT = 500.0
const val ENERGY_CRIT_WATTS_DEFAULT = 2000.0

/**
 * Maps a live entity to its semantic slot. Pure and unit-tested.
 * Resolution order matters: availability first (unavailable/unknown are
 * never reassuring states), then domain+state. Unmapped domains resolve
 * to Unknown — neutral, with their status text still shown — rather
 * than a guessed color. Armed alarm panels resolve to LockLocked (blue
 * = secure); only a TRIGGERED panel is AlarmFault.
 */
fun semanticStateFor(
    entity: HaEntity,
    energyWarnWatts: Double = ENERGY_WARN_WATTS_DEFAULT,
    energyCritWatts: Double = ENERGY_CRIT_WATTS_DEFAULT,
): SemanticState {
    if (entity.state == "unavailable") return SemanticState.Unavailable
    if (entity.state == "unknown" || entity.state.isBlank()) return SemanticState.Unknown
    val deviceClass = entity.attrString("device_class")
    return when (entity.domain) {
        "light", "switch", "input_boolean", "fan" ->
            if (entity.state == "on") SemanticState.DeviceOn else SemanticState.DeviceOff
        "lock" -> when (entity.state) {
            "locked" -> SemanticState.LockLocked
            "unlocked", "opening", "closing" -> SemanticState.LockUnlocked
            "jammed" -> SemanticState.LockFault
            else -> SemanticState.Unknown
        }
        "binary_sensor" -> when {
            deviceClass == "motion" ->
                if (entity.state == "on") SemanticState.MotionActive else SemanticState.MotionClear
            deviceClass in setOf("door", "window") ->
                if (entity.state == "on") SemanticState.OpenAlert else SemanticState.ClosedNeutral
            deviceClass in setOf("smoke", "co", "gas", "problem", "tamper") ->
                if (entity.state == "on") SemanticState.AlarmFault else SemanticState.ClosedNeutral
            entity.state == "on" -> SemanticState.OpenAlert
            else -> SemanticState.ClosedNeutral
        }
        "cover" -> when (entity.state) {
            "open", "opening" -> SemanticState.OpenAlert
            "closed", "closing" -> SemanticState.ClosedNeutral
            else -> SemanticState.Unknown
        }
        "climate" -> when (entity.attrString("hvac_action")) {
            "heating" -> SemanticState.HeatingActive
            "cooling" -> SemanticState.CoolingActive
            else -> SemanticState.ClimateIdle
        }
        "media_player" ->
            if (entity.state == "playing") SemanticState.MediaPlaying else SemanticState.MediaIdle
        "alarm_control_panel" -> when (entity.state) {
            "triggered" -> SemanticState.AlarmFault
            "disarmed" -> SemanticState.ClosedNeutral
            else -> SemanticState.LockLocked
        }
        "vacuum" -> when (entity.state) {
            "cleaning", "returning" -> SemanticState.DeviceOn
            "error" -> SemanticState.AlarmFault
            else -> SemanticState.DeviceOff
        }
        "sensor" -> run {
            val watts = if (deviceClass == "power") entity.state.toDoubleOrNull() else null
            when {
                watts == null -> SemanticState.Unknown
                watts < energyWarnWatts -> SemanticState.EnergyLow
                watts < energyCritWatts -> SemanticState.EnergyHigh
                else -> SemanticState.EnergyCritical
            }
        }
        else -> SemanticState.Unknown
    }
}

/**
 * Energy slot for a watt total, using the same documented thresholds as
 * [semanticStateFor] but without fabricating a sensor entity — the
 * Energy screen's total is a sum, not an entity. Pure, unit-tested.
 */
fun energySlotForWatts(
    watts: Double,
    energyWarnWatts: Double = ENERGY_WARN_WATTS_DEFAULT,
    energyCritWatts: Double = ENERGY_CRIT_WATTS_DEFAULT,
): SemanticState = when {
    watts < energyWarnWatts -> SemanticState.EnergyLow
    watts < energyCritWatts -> SemanticState.EnergyHigh
    else -> SemanticState.EnergyCritical
}

/**
 * §9 "important home alert" for nav and room dots: an unlocked lock, an
 * open door/window, or a TRIGGERED alarm. Deliberately narrow: an armed
 * alarm is secure (not an alert), motion is transient (it has its own
 * tile badge and Home Status row), and lights/energy are routine, not
 * alerts. Dots are never counts — one dot means "something needs
 * attention", and Home Status names it.
 */
fun hasImportantAlert(entities: List<HaEntity>): Boolean = entities.any { entity ->
    (entity.domain == "lock" && entity.state == "unlocked") ||
        (entity.domain == "binary_sensor" &&
            entity.attrString("device_class") in setOf("door", "window") && entity.state == "on") ||
        (entity.domain == "cover" && entity.state in setOf("open", "opening")) ||
        (entity.domain == "alarm_control_panel" && entity.state == "triggered")
}

/** The attention dot's fill: red on either ground, so it is never mistaken for a routine state. */
fun alertDotColor(isDark: Boolean): Color = attentionDot(isDark)

/**
 * A tile's colors: the card fill, the polygon badge behind its icon, and
 * the icon. All three come from one family (see FamilyColors for the
 * tone recipe), which is what makes a grid of tiles read as one set.
 */
data class DomainCardColors(val container: Color, val badgeBg: Color, val badgeGlyph: Color)

private fun StateFamily.card(isDark: Boolean): DomainCardColors =
    colors(isDark).let { DomainCardColors(it.container, it.accent, it.onAccent) }

/** The resting tile: off, idle, unavailable. Also what a preset tile wears while its entity is off. */
fun inactiveCardColors(isDark: Boolean): DomainCardColors = StateFamily.Quiet.card(isDark)

/** A power reading on the entity itself (plug-style switches/sensors). */
private fun hasPowerReading(entity: HaEntity): Boolean =
    entity.attrDouble("current_power_w") != null ||
        entity.attrDouble("power") != null ||
        entity.attrDouble("current_power") != null

/**
 * The family a tile is drawn in, from the entity's live state. Null for
 * a domain with no tile meaning of its own; callers then fall back to
 * the neutral surface.
 *
 * The calls worth knowing, because they are choices and not lookups:
 * - A thermostat that is on but idle stays [StateFamily.Heat] rather
 *   than going quiet: a grey thermostat reads as broken.
 * - Media stays [StateFamily.Media] when paused, so a favourite that is
 *   not playing does not look dead.
 * - A plug with a power reading is [StateFamily.Power], a plain switch
 *   is [StateFamily.On].
 * - Anything named greenhouse or irrigation is [StateFamily.On], except
 *   a thermostat, which is always climate.
 * - Sensors take the family of what they measure; a reading with no
 *   meaning of its own stays quiet.
 */
fun domainFamily(entity: HaEntity): StateFamily? {
    if (entity.state == "unavailable" || entity.state == "unknown" || entity.state.isBlank()) {
        return StateFamily.Quiet
    }
    if (entity.domain != "climate" &&
        (entity.entityId.contains("greenhouse", ignoreCase = true) ||
            entity.entityId.contains("irrigation", ignoreCase = true) ||
            entity.friendlyName.contains("greenhouse", ignoreCase = true) ||
            entity.friendlyName.contains("irrigation", ignoreCase = true))
    ) {
        return StateFamily.On
    }
    return when (entity.domain) {
        "light" -> if (entity.state == "on") StateFamily.Light else StateFamily.Quiet
        "switch", "input_boolean", "fan" -> when {
            entity.state != "on" -> StateFamily.Quiet
            hasPowerReading(entity) -> StateFamily.Power
            else -> StateFamily.On
        }
        "climate" -> when (entity.attrString("hvac_action")) {
            "cooling" -> StateFamily.Cool
            else -> StateFamily.Heat
        }
        "lock" -> when (entity.state) {
            "locked", "locking" -> StateFamily.Secure
            else -> StateFamily.Alert
        }
        "cover" -> when (entity.state) {
            "open", "opening" -> StateFamily.Alert
            else -> StateFamily.Secure
        }
        "alarm_control_panel" -> when {
            entity.state == "triggered" || entity.state == "pending" ||
                entity.state.startsWith("armed") -> StateFamily.Heat
            else -> StateFamily.Secure
        }
        "media_player" -> StateFamily.Media
        "vacuum" -> when (entity.state) {
            "cleaning", "returning" -> StateFamily.On
            "error" -> StateFamily.Alert
            else -> StateFamily.Quiet
        }
        "weather" -> StateFamily.Cool
        "sensor" ->
            if (entity.attrString("device_class") == "power" || hasPowerReading(entity)) StateFamily.Power
            else sensorFamily(
                sensorKind(entity.entityId, entity.attrString("device_class"), entity.friendlyName),
            ) ?: StateFamily.Quiet
        "binary_sensor" -> when {
            entity.state != "on" -> StateFamily.Quiet
            entity.attrString("device_class") == "motion" -> StateFamily.Caution
            else -> StateFamily.Alert
        }
        else -> null
    }
}

fun domainCardColors(entity: HaEntity, isDark: Boolean): DomainCardColors? =
    domainFamily(entity)?.card(isDark)

/**
 * A tile switch's colors. The track is the family's badge tone and the
 * thumb is the card's own fill, so the switch looks cut from the tile.
 * Off uses the quiet track; a lock or cover has no "off", so its other
 * position is its other state's family.
 */
data class CardSwitchColors(
    val trackChecked: Color,
    val thumbChecked: Color,
    val trackUnchecked: Color,
    val thumbUnchecked: Color,
)

/** The quiet switch track and its thumb, shared by every tile switch. */
fun restingSwitchTrack(isDark: Boolean): Color = if (isDark) Palette.Moss.t60 else Palette.Moss.t35

fun restingSwitchThumb(isDark: Boolean): Color = if (isDark) Palette.Moss.t20 else Color.White

fun domainSwitchColors(entity: HaEntity, isDark: Boolean): CardSwitchColors? {
    val thumb = restingSwitchThumb(isDark)
    return when (entity.domain) {
        "light" -> {
            // A lit lamp's track is its bright yellow, so the thumb is the dark ink on it.
            val lit = StateFamily.Light.card(isDark)
            CardSwitchColors(lit.badgeBg, lit.badgeGlyph, restingSwitchTrack(isDark), thumb)
        }
        "switch", "input_boolean", "fan" -> {
            val on = (if (hasPowerReading(entity)) StateFamily.Power else StateFamily.On).card(isDark)
            CardSwitchColors(on.badgeBg, on.container, restingSwitchTrack(isDark), thumb)
        }
        "lock" -> {
            val locked = StateFamily.Secure.card(isDark)
            CardSwitchColors(locked.badgeBg, locked.container, StateFamily.Alert.card(isDark).badgeBg, thumb)
        }
        "cover" -> {
            val open = StateFamily.Alert.card(isDark)
            CardSwitchColors(open.badgeBg, open.container, StateFamily.Secure.card(isDark).badgeBg, thumb)
        }
        else -> null
    }
}
