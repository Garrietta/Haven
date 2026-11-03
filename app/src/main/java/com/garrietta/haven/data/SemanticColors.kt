package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.ui.components.SemanticState

/**
 * Stage E (redesign): the CENTRALIZED semantic color system — the single
 * domain+state -> visual mapping the brief's §7 requires. Stage B defined
 * the [SemanticState] slots; this file fills them with deliberate light
 * AND dark variants and the pure resolver that maps a live entity to its
 * slot. Every onContainer pair clears 4.5:1 against its container in both
 * themes (unit-tested); glyphs use onContainer, so the 3:1 icon floor is
 * covered by the same test.
 *
 * Division of labor, stated plainly: semantic colors communicate STATE
 * on the surfaces that read state at a glance — Home Status rows and the
 * §9 condition dots (nav, room). They do NOT replace the tile badge
 * pipeline ([badgeColors]): that pipeline carries verified behavior
 * Stage E must not regress — the #92 primary+onPrimary contrast fix for
 * lit-light badges, the dual heat_cool setpoint containers, and the
 * wallpaper-dynamic accent washes (the standing "dynamic coloring stays
 * prominent" law). A future round may migrate badge domains one by one
 * with a device verdict each; this overnight program, with no device
 * verification possible, does not rewrite verified code blind.
 *
 * Colors are never the ONLY indicator: status text always accompanies
 * every semantic surface, and Unknown/Unavailable slots keep their
 * explicit labels.
 */

/** A semantic visual: container fill + content (text/glyph) color. */
data class SemanticVisual(val container: Color, val onContainer: Color)

/**
 * The filled mapping. Light containers are pastels (the mockup's lime
 * on-light, lavender media, peach climate, yellow plug family) with dark
 * content; dark containers are the same hues deepened, with light
 * content. Families share pairs deliberately: heating IS the coral
 * family, cooling IS the blue family, motion amber IS the unlocked
 * amber — the same state means the same color app-wide.
 */
fun semanticVisual(state: SemanticState, isDark: Boolean): SemanticVisual = when (state) {
    SemanticState.DeviceOn -> if (isDark) SemanticVisual(Color(0xFF4A5D23), Color(0xFFE8F5C8))
        else SemanticVisual(Color(0xFFDDEBBE), Color(0xFF2A3A12))
    SemanticState.DeviceOff -> if (isDark) SemanticVisual(Color(0xFF2B2B2B), Color(0xFFC9C9C4))
        else SemanticVisual(Color(0xFFE8E6DF), Color(0xFF5B5A51))
    SemanticState.LockLocked -> if (isDark) SemanticVisual(Color(0xFF1E3A5F), Color(0xFFD6E4F5))
        else SemanticVisual(Color(0xFFD6E4F5), Color(0xFF1E3A5F))
    SemanticState.LockUnlocked -> if (isDark) SemanticVisual(Color(0xFF5A3E00), Color(0xFFF5E0B8))
        else SemanticVisual(Color(0xFFF5E0B8), Color(0xFF5A3E00))
    SemanticState.LockFault, SemanticState.AlarmFault, SemanticState.EnergyCritical ->
        if (isDark) SemanticVisual(Color(0xFF93000A), Color(0xFFFFDAD6))
        else SemanticVisual(Color(0xFFF9DEDC), Color(0xFF410E0B))
    SemanticState.OpenAlert, SemanticState.HeatingActive ->
        if (isDark) SemanticVisual(Color(0xFF7A2E22), Color(0xFFF8D7CF))
        else SemanticVisual(Color(0xFFF8D7CF), Color(0xFF7A1F14))
    SemanticState.ClosedNeutral, SemanticState.MotionClear, SemanticState.ClimateIdle, SemanticState.MediaIdle ->
        if (isDark) SemanticVisual(Color(0xFF2B2B2B), Color(0xFFC9C9C4))
        else SemanticVisual(Color(0xFFE8E6DF), Color(0xFF5B5A51))
    SemanticState.MotionActive -> if (isDark) SemanticVisual(Color(0xFF5A3E00), Color(0xFFF5E0B8))
        else SemanticVisual(Color(0xFFF5E0B8), Color(0xFF5A3E00))
    SemanticState.CoolingActive -> if (isDark) SemanticVisual(Color(0xFF1E3A5F), Color(0xFFD6E4F5))
        else SemanticVisual(Color(0xFFD6E4F5), Color(0xFF1E3A5F))
    SemanticState.MediaPlaying -> if (isDark) SemanticVisual(Color(0xFF3D2E63), Color(0xFFE4DCF5))
        else SemanticVisual(Color(0xFFE4DCF5), Color(0xFF3D2E63))
    SemanticState.EnergyLow -> if (isDark) SemanticVisual(Color(0xFF3A4218), Color(0xFFE8EDC8))
        else SemanticVisual(Color(0xFFE8EDC8), Color(0xFF3A4218))
    SemanticState.EnergyHigh -> if (isDark) SemanticVisual(Color(0xFF5A3200), Color(0xFFFBE3C2))
        else SemanticVisual(Color(0xFFFBE3C2), Color(0xFF5A3200))
    SemanticState.Unavailable -> if (isDark) SemanticVisual(Color(0xFF2E2E2E), Color(0xFF9E9E9E))
        else SemanticVisual(Color(0xFFE0E0E0), Color(0xFF5F5F5A))
    SemanticState.Unknown -> if (isDark) SemanticVisual(Color(0xFF2B2B2B), Color(0xFFC9C9C4))
        else SemanticVisual(Color(0xFFE8E6DF), Color(0xFF5B5A51))
}

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

/**
 * §9 condition-dot fill: a saturated alert red that reads on both the
 * light pill (sage container) and the dark pill (near-black). Dots are
 * indicators, not text, but the color is chosen from the red family so
 * it cannot be mistaken for a routine state.
 */
fun alertDotColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)

/**
 * UI rewrite round §2: the Home dashboard card's colors — the user's
 * complete domain color matrix (2026-11-03), implemented exactly:
 * card hex → outer squircle surface; badge/switch hex → top-left
 * polygon badge fill AND top-right switch track when active. This
 * extends the centralized SemanticColors system (no second system is
 * forked); tiles and detail panels keep badgeColors.
 *
 * The resolver is STATE-based (the refactor's was domain-based): the
 * same domain renders different rows for locked vs unlocked, heating
 * vs cooling, armed vs disarmed.
 *
 * Glyph rule: icons inside ACTIVE light-mode badges render white;
 * inside active dark-mode badges they render deep tonal ink (#0A2005
 * for green rows, #111D52 otherwise) — every active pair clears
 * 4.5:1 (unit-tested).
 *
 * INACTIVE reconciliation (both rules satisfied): the matrix row is
 * card #EFF1E8/#1C201B with badge/switch #52564E/#8C9188, and the
 * user's explicit rule puts dark badge polygons on #2A2F28 so the
 * shape never disappears against the #1C201B card. So: light badge
 * fill #52564E with white glyph; dark badge fill #2A2F28 with glyph
 * #8C9188; switch track #52564E/#8C9188 with a muted thumb. Measured
 * dark inactive glyph contrast is ~4.2:1 — below 4.5 but above the
 * 3:1 icon floor; both hexes are user-specified, so the pair is kept
 * and reported, not altered.
 *
 * JUDGMENT CALLS (no matrix row exists; previous behavior kept):
 * - Climate with no heating/cooling action and not heat_cool mode
 *   (idle/off/fan_only) keeps the Heating row (the previous CLIMATE
 *   family) — greying an idle thermostat would be a regression.
 * - Lock jammed → Unlocked row (attention color); locking →
 *   Locked, unlocking → Unlocked.
 * - Alarm triggered/pending/armed_* → Armed row; unknown alarm
 *   states → Disarmed row (the previous LOCK family values).
 * - Media idle/paused (including favorited wide cards) keeps the
 *   Media row — a paused favorite must not read as dead.
 * - Vacuum docked/idle/paused → Inactive row (off); vacuum error →
 *   null (previous badge pipeline, so faults keep their tint).
 * - Binary sensors (motion/door/window) → null (previous pipeline);
 *   the matrix has no binary-sensor row.
 * - Generic non-power sensors → null (previous pipeline); the
 *   matrix's "Sensor" is read as power sensors. Temperature and
 *   humidity sensors are EXCLUDED by callers (coral/sky standing law)
 *   and also resolve to null here, so no path can override them.
 * - Greenhouse/irrigation name match applies to every domain EXCEPT
 *   climate (a greenhouse thermostat still reads as climate).
 * - Warm-white/cool-daylight light variants are NOT in the matrix
 *   and are not invented: lights use the Light On row. The kelvin/
 *   brightness container tint the refactor kept for lights is
 *   superseded by the matrix wherever domainCardColors resolves
 *   (HomeRoomCard's kelvin fallback only runs when it returns null,
 *   which lights never do now).
 */
data class DomainCardColors(val container: Color, val badgeBg: Color, val badgeGlyph: Color)

/** One row of the user's matrix: light/dark card + badge + dark glyph ink. */
private enum class MatrixRow(
    val lightContainer: Color,
    val darkContainer: Color,
    val lightBadge: Color,
    val darkBadge: Color,
    val darkInk: Color,
) {
    LIGHT_ON(Color(0xFFC8F5A6), Color(0xFF2B4E14), Color(0xFF1B3708), Color(0xFFB8F397), Color(0xFF0A2005)),
    HEATING(Color(0xFFFFDBD0), Color(0xFF532215), Color(0xFF8C1D00), Color(0xFFFFB59E), Color(0xFF111D52)),
    COOLING(Color(0xFFD4E3FF), Color(0xFF1A304E), Color(0xFF001C3A), Color(0xFFA5C8FF), Color(0xFF111D52)),
    HEAT_COOL(Color(0xFFFFDBD0), Color(0xFF3E2723), Color(0xFF8C1D00), Color(0xFFFFCCBC), Color(0xFF111D52)),
    LOCKED(Color(0xFFDCE1FF), Color(0xFF222C61), Color(0xFF111D52), Color(0xFFB9C3FF), Color(0xFF111D52)),
    UNLOCKED(Color(0xFFFFDAD6), Color(0xFF5C1916), Color(0xFF410002), Color(0xFFFFB4AB), Color(0xFF111D52)),
    DISARMED(Color(0xFFDCE1FF), Color(0xFF222C61), Color(0xFF111D52), Color(0xFFB9C3FF), Color(0xFF111D52)),
    ARMED(Color(0xFFFFDBD0), Color(0xFF532215), Color(0xFF8C1D00), Color(0xFFFFB59E), Color(0xFF111D52)),
    MEDIA(Color(0xFFEFE0FF), Color(0xFF3B2554), Color(0xFF2B1246), Color(0xFFDAB9FF), Color(0xFF111D52)),
    POWER(Color(0xFFFFE082), Color(0xFF4D3800), Color(0xFF261900), Color(0xFFFFE082), Color(0xFF111D52)),
    VACUUM_ACTIVE(Color(0xFFC8F5A6), Color(0xFF2B4E14), Color(0xFF1B3708), Color(0xFFB8F397), Color(0xFF0A2005)),
    GREENHOUSE(Color(0xFFC8F5A6), Color(0xFF1F3D0C), Color(0xFF143800), Color(0xFFA6F285), Color(0xFF0A2005)),
    WEATHER(Color(0xFFD4E3FF), Color(0xFF1A304E), Color(0xFF001C3A), Color(0xFFA5C8FF), Color(0xFF111D52)),
    INACTIVE(Color(0xFFEFF1E8), Color(0xFF1C201B), Color(0xFF52564E), Color(0xFF2A2F28), Color(0xFF111D52)),
}

/** A matrix row as card colors, with the glyph rule applied. */
private fun MatrixRow.toCardColors(isDark: Boolean): DomainCardColors {
    val badgeBg = if (isDark) darkBadge else lightBadge
    val glyph = if (this == MatrixRow.INACTIVE) {
        if (isDark) Color(0xFF8C9188) else Color.White
    } else if (isDark) {
        darkInk
    } else {
        Color.White
    }
    return DomainCardColors(if (isDark) darkContainer else lightContainer, badgeBg, glyph)
}

/** A power reading on the entity itself (plug-style switches/sensors). */
private fun hasPowerReading(entity: HaEntity): Boolean =
    entity.attrDouble("current_power_w") != null ||
        entity.attrDouble("power") != null ||
        entity.attrDouble("current_power") != null

fun domainCardColors(entity: HaEntity, isDark: Boolean): DomainCardColors? {
    if (entity.state == "unavailable" || entity.state == "unknown" || entity.state.isBlank()) {
        return MatrixRow.INACTIVE.toCardColors(isDark)
    }
    // Greenhouse/irrigation identity wins over domain — except
    // climate, which always reads as climate.
    if (entity.domain != "climate" &&
        (entity.entityId.contains("greenhouse", ignoreCase = true) ||
            entity.entityId.contains("irrigation", ignoreCase = true) ||
            entity.friendlyName.contains("greenhouse", ignoreCase = true) ||
            entity.friendlyName.contains("irrigation", ignoreCase = true))
    ) {
        return MatrixRow.GREENHOUSE.toCardColors(isDark)
    }
    return when (entity.domain) {
        "light" ->
            if (entity.state == "on") MatrixRow.LIGHT_ON.toCardColors(isDark)
            else MatrixRow.INACTIVE.toCardColors(isDark)
        "switch", "input_boolean", "fan" -> when {
            entity.state != "on" -> MatrixRow.INACTIVE.toCardColors(isDark)
            hasPowerReading(entity) -> MatrixRow.POWER.toCardColors(isDark)
            else -> MatrixRow.LIGHT_ON.toCardColors(isDark)
        }
        "climate" -> when (entity.attrString("hvac_action")) {
            "heating" -> MatrixRow.HEATING.toCardColors(isDark)
            "cooling" -> MatrixRow.COOLING.toCardColors(isDark)
            else -> if (entity.state == "heat_cool") MatrixRow.HEAT_COOL.toCardColors(isDark)
                else MatrixRow.HEATING.toCardColors(isDark)
        }
        "lock" -> when (entity.state) {
            "locked", "locking" -> MatrixRow.LOCKED.toCardColors(isDark)
            else -> MatrixRow.UNLOCKED.toCardColors(isDark)
        }
        "cover" -> when (entity.state) {
            "open", "opening" -> MatrixRow.UNLOCKED.toCardColors(isDark)
            else -> MatrixRow.LOCKED.toCardColors(isDark)
        }
        "alarm_control_panel" -> when {
            entity.state == "disarmed" -> MatrixRow.DISARMED.toCardColors(isDark)
            entity.state == "triggered" || entity.state == "pending" ||
                entity.state.startsWith("armed") -> MatrixRow.ARMED.toCardColors(isDark)
            else -> MatrixRow.DISARMED.toCardColors(isDark)
        }
        "media_player" -> MatrixRow.MEDIA.toCardColors(isDark)
        "vacuum" -> when (entity.state) {
            "cleaning", "returning" -> MatrixRow.VACUUM_ACTIVE.toCardColors(isDark)
            "error" -> null
            else -> MatrixRow.INACTIVE.toCardColors(isDark)
        }
        "weather" -> MatrixRow.WEATHER.toCardColors(isDark)
        "sensor" ->
            if (entity.attrString("device_class") == "power" || hasPowerReading(entity)) {
                MatrixRow.POWER.toCardColors(isDark)
            } else {
                null
            }
        else -> null
    }
}

/**
 * Switch track/thumb colors for a card's M3 pill switch, from the same
 * matrix rows: the track follows the entity's state row (the badge
 * hex), the checked thumb uses the checked state's card hex, the
 * unchecked thumb is muted. For on/off toggleables the unchecked
 * track is the Inactive row's switch hex (#52564E/#8C9188 — note the
 * dark track differs from the dark badge polygon #2A2F28 by the
 * reconciliation above); for locks/covers the unchecked track is the
 * other-state row (Unlocked/Closed), because unlocked/closed are live
 * states, not "off". Null for domains with no card switch.
 */
data class CardSwitchColors(
    val trackChecked: Color,
    val thumbChecked: Color,
    val trackUnchecked: Color,
    val thumbUnchecked: Color,
)

fun domainSwitchColors(entity: HaEntity, isDark: Boolean): CardSwitchColors? {
    val mutedThumb = if (isDark) Color(0xFF9E9E9E) else Color.White
    val inactiveTrack = if (isDark) Color(0xFF8C9188) else Color(0xFF52564E)
    return when (entity.domain) {
        "light", "switch", "input_boolean", "fan" -> {
            // Mirrors domainCardColors: lights always Light On; other
            // toggleables with a power reading resolve to Power.
            val onRow = if (entity.domain == "light") MatrixRow.LIGHT_ON
                else if (hasPowerReading(entity)) MatrixRow.POWER
                else MatrixRow.LIGHT_ON
            val on = onRow.toCardColors(isDark)
            CardSwitchColors(on.badgeBg, on.container, inactiveTrack, mutedThumb)
        }
        "lock" -> {
            val locked = MatrixRow.LOCKED.toCardColors(isDark)
            val unlocked = MatrixRow.UNLOCKED.toCardColors(isDark)
            CardSwitchColors(locked.badgeBg, locked.container, unlocked.badgeBg, mutedThumb)
        }
        "cover" -> {
            val open = MatrixRow.UNLOCKED.toCardColors(isDark)
            val closed = MatrixRow.LOCKED.toCardColors(isDark)
            CardSwitchColors(open.badgeBg, open.container, closed.badgeBg, mutedThumb)
        }
        else -> null
    }
}
