package com.garrietta.haven.ui.screens
import com.garrietta.haven.ui.components.compositeOver
import com.garrietta.haven.ui.components.ensureContrast
import com.garrietta.haven.ui.components.CoolBlue
import com.garrietta.haven.ui.components.HeatOrange

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.DialogTitleText
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.hideWhileTraveling
import com.garrietta.haven.ui.components.LocalHeroTarget
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.coolSetpointContainer
import com.garrietta.haven.ui.components.coolSetpointOnContainer
import com.garrietta.haven.ui.components.heatSetpointContainer
import com.garrietta.haven.ui.components.heatSetpointOnContainer
import com.garrietta.haven.ui.theme.StateShape
import com.garrietta.haven.ui.theme.stateShape
import com.garrietta.haven.ui.controls.ExpressiveStepper
import com.garrietta.haven.ui.controls.applyClimateSetpoint
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.stateText


/**
 * The centered expressive thermostat pop-out (Round 4: the
 * PRIMARY and only thermostat panel — tap and long-press on the
 * Ecobee card both land here; the old secondary Details page
 * is deleted): the radial dial on a mode-colored scallop disc
 * up top, Mode/Fan pills underneath, then 24-hour
 * temperature/humidity history charts. Back and the panel's back
 * arrow dismiss; there is no Details route anymore. The
 * thermostat's shape IS its control surface (the dial changes
 * temp, the pills change mode/fan); there is no single binary
 * actuation for it, so the shape-tap = actuate rule applies to
 * its inner controls, not to the card shape as a whole. The dial
 * keeps every standing rule (1° snap, handle-grab-only 32dp
 * nearest-wins, inside-tap toggle, clamps, gap-proofing,
 * widen-on-grab, pulsing bloom, dual numbers) — the redesign is
 * shapes/spacing/layout, not interaction. The disc is a square
 * box, so the min-dimension polygon scale fits exactly: content
 * fills the mask, never inset, no white crescents.
 */
@Composable
fun ThermostatPopout(entity: HaEntity, vm: HavenViewModel, onClose: () -> Unit) {
    val modeColor = climateModeColor(entity.state, entity.attrString("hvac_action"))
    val min = entity.attrDouble("min_temp")?.toFloat() ?: 10f
    val max = entity.attrDouble("max_temp")?.toFloat() ?: 30f
    // Traveling header target (2026-10-29 pop-up round): the title
    // row reports its root rect into the measurement frame's hero
    // target, so the overlay's traveling header lands exactly here.
    // Read during composition (never inside the layout callback).
    val heroTarget = LocalHeroTarget.current
    // The dialog panel scrolls: no own verticalScroll here.
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Badge by EVERY pop-up (2026-10-29 verdict overrules
            // #68's deliberate pop-out exception): the traveling
            // badge dissolves into this 48dp header badge exactly
            // like every other pop-up header, and tapping it
            // dismisses. Badge only hides while traveling
            // (2026-11-01 polish round): the name/state text stays
            // visible from the panel's first frame — hiding the
            // whole row held the title back until the final 2%
            // handoff window. No top-right X (2026-11-01 round 1
            // panel chrome): the panel's back arrow + BackHandler
            // are the dismiss paths.
            DomainBadge(
                entity,
                domainIcon(entity),
                accentFor(entity.entityId),
                modifier = Modifier.size(48.dp).hideWhileTraveling().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClose() },
            )
            Spacer(Modifier.width(12.dp))
            // Standard dialog header (2026-10-30): name + state
            // below it, exactly like every sheet/detail header.
            Column(Modifier.weight(1f)) {
                DialogTitleText(entity.friendlyName, Modifier.fillMaxWidth())
                Text(entity.dialogHeaderState(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Scalloped disc + radial dial: the disc is the pop-out's
        // expressive shape, mode-colored; the dial sits centered on it.
        Box(
            // 320dp disc, 240dp dial (hotfix, was 260dp): the old
            // dial's outer (cool) thumb bloom reached 144dp from
            // center against the 160dp disc radius, and the
            // SCALLOP12 edge dips inward past that at the top —
            // which is where both thumbs sit when setpoints are
            // close (67/69) — so the thumbs crowded the scallop
            // border. At 240dp the outer bloom reaches 134dp,
            // clearing the deepest scallop dip with room to spare.
            // Grab targets use the same radii, so taps still land
            // on the thumb they see.
            Modifier.size(320.dp).background(modeColor.copy(alpha = 0.20f), stateShape(StateShape.SCALLOP12)),
            contentAlignment = Alignment.Center,
        ) {
            ThermostatHero(
                entity, vm, min, max, diameter = 240.dp,
                // Numbers sit on the scallop disc: floor them against
                // the disc color itself (2026-10-30 dial-clash fix).
                numberBackdrop = compositeOver(
                    modeColor.copy(alpha = 0.20f),
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Mode + Fan as M3 segmented pill bars (2026-11-01 round 1):
        // the dropdown pills are gone — every option is visible at
        // once, in the brief's order, filtered to what HA reports.
        val modes = entity.attrList("hvac_modes")?.mapNotNull { it as? String } ?: emptyList()
        if (modes.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("Mode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                SegmentedOptionBar(
                    options = orderedHvacModes(modes).map { it to formatHvacMode(it) },
                    selected = entity.state,
                    onSelect = { vm.setClimateHvacMode(entity.entityId, it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        val fanModes = entity.attrList("fan_modes")?.mapNotNull { it as? String } ?: emptyList()
        if (fanModes.isNotEmpty()) {
            val fanMode = entity.attrString("fan_mode")
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("Fan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                SegmentedOptionBar(
                    options = orderedFanModes(fanModes).map {
                        it to it.replace('_', ' ').replaceFirstChar { c -> c.uppercase() }
                    },
                    selected = fanMode,
                    onSelect = { vm.setClimateFanMode(entity.entityId, it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        // No bottom action (Round 4): the old Details button
        // routed to the deleted secondary page — the panel's back
        // arrow + BackHandler are the dismiss paths, and the
        // history charts below carry what Details used to hold.
        val states by vm.states.collectAsState()
        ThermostatHistorySection(entity, vm, states)
    }
}

/**
 * One M3 segmented pill bar (2026-11-01 round 1): the replacement
 * for the Mode/Fan dropdown pills and for the setpoint selector.
 * [options] are (value, label) pairs in display order; [selected]
 * is the current value; [activeColors] optionally tints a
 * segment's active container/content (the Heat/Cool selector bar
 * passes the heat/cool setpoint tokens so each side reads in its
 * own color — otherwise the scheme defaults apply). Options the
 * thermostat does not report never render: callers filter to the
 * entity's hvac_modes / fan_modes first, so a segment can never
 * send a mode HA would reject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedOptionBar(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColors: ((String) -> Pair<Color, Color>?)? = null,
) {
    if (options.isEmpty()) return
    SingleChoiceSegmentedButtonRow(modifier) {
        for ((index, option) in options.withIndex()) {
            val (value, label) = option
            val active = activeColors?.invoke(value)
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = if (active != null) {
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = active.first,
                        activeContentColor = active.second,
                    )
                } else {
                    // Unified selected treatment (hotfix): the
                    // default secondaryContainer reads as dim grey
                    // on this dark palette, so a selected Mode/Fan
                    // segment looked unselected next to the
                    // saturated Heat/Cool setpoint segments. Every
                    // bar now selects with the same weight — filled
                    // container + checkmark + semibold label — with
                    // the hue semantic on the setpoint bar (heat/
                    // cool) and palette-primary elsewhere.
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** Canonical mode order for the segmented Mode bar: Off / Heat /
 * Cool / Heat·Cool first (the brief's order), then any further
 * modes the thermostat reports, in reported order. */
private fun orderedHvacModes(modes: List<String>): List<String> {
    val canonical = listOf("off", "heat", "cool", "heat_cool", "auto", "dry", "fan_only")
    return canonical.filter { it in modes } + modes.filter { it !in canonical }
}

/** Canonical fan order for the segmented Fan bar: Auto / On first,
 * then any further fan modes in reported order. */
private fun orderedFanModes(modes: List<String>): List<String> {
    val canonical = listOf("auto", "on")
    return canonical.filter { it in modes } + modes.filter { it !in canonical }
}

/** Hero dial: Google Home's dual numbers + inside steppers + ring marks.
 *
 * Heat/cool thermostats have two setpoints and the dial edits exactly one
 * of them: the active selector, toggled by tapping inside the dial face
 * (never by tapping the ring — ring taps change nothing). Both numbers
 * show together, heat left and cool right, the active one at full
 * strength and the inactive one muted — that highlight is the selector
 * indicator, so the old display-only chip is gone. Heat + cool dots sit
 * on the ring at their setpoint angles, and the current temperature
 * shows as a dot at its angle plus the small number at the top of the
 * ring. The −/+ steppers sit below the numbers, inside the dial face:
 * they are real buttons, so their taps are consumed by the button and
 * never reach the dial's inside-tap toggle, and drags starting on them
 * are ignored because they do not start on the handle. Single-setpoint
 * thermostats show their one number large plus steppers, and have no
 * selector. Every change — handle drag, steppers — moves in whole
 * degrees and is clamped so heating can never pass cooling.
 */
@Composable
fun ThermostatHero(
    entity: HaEntity,
    vm: HavenViewModel,
    min: Float,
    max: Float,
    diameter: Dp = 300.dp,
    /** Color directly behind the dial numbers (the scallop disc). */
    numberBackdrop: Color,
) {
    val current = entity.attrDouble("current_temperature")
    val accent = accentFor(entity.entityId)
    val lowInit = entity.attrDouble("target_temp_low")
    val highInit = entity.attrDouble("target_temp_high")
    val singleInit = entity.attrDouble("temperature")
    val heatCool = lowInit != null && highInit != null
    // Setpoint state re-initializes whenever HA reports a new setpoint,
    // same as the old midpoint dial did.
    var heat by remember(lowInit) { mutableStateOf(lowInit?.toFloat()?.coerceIn(min, max) ?: min) }
    var cool by remember(highInit) { mutableStateOf(highInit?.toFloat()?.coerceIn(min, max) ?: max) }
    var single by remember(singleInit) { mutableStateOf(singleInit?.toFloat()?.coerceIn(min, max) ?: min) }
    // The selector follows the hvac mode (cooling when the thermostat is
    // cooling, otherwise heating) and survives setpoint updates within a
    // mode; the user flips it with an inside tap.
    var selector by remember(entity.entityId, entity.state) { mutableStateOf(defaultSelector(entity.state)) }
    // Pulsing thumb: the dial's fluid indicator while the system is
    // actively heating or cooling (hvac_action, not mode — a thermostat
    // in heat mode that is idle does not pulse).
    val pulseSelector = when (entity.attrString("hvac_action")) {
        "heating" -> SetpointSelector.HEATING
        "cooling" -> if (heatCool) SetpointSelector.COOLING else SetpointSelector.HEATING
        else -> null
    }
    val dialAccent: Color = if (heatCool) {
        if (selector == SetpointSelector.HEATING) HeatOrange else CoolBlue
    } else {
        when (entity.state) {
            "heat" -> HeatOrange
            "cool" -> CoolBlue
            else -> accent
        }
    }
    val stepActive: (Float) -> Unit = { delta ->
        val next = if (heatCool) {
            if (selector == SetpointSelector.HEATING) {
                clampHeatSetpoint((heat + delta).toDouble(), min.toDouble(), cool.toDouble()).toFloat()
            } else {
                clampCoolSetpoint((cool + delta).toDouble(), heat.toDouble(), max.toDouble()).toFloat()
            }
        } else {
            snapToDegree((single + delta).toDouble()).toFloat().coerceIn(min, max)
        }
        if (heatCool) {
            if (selector == SetpointSelector.HEATING) heat = next else cool = next
        } else {
            single = next
        }
        applyClimateSetpoint(vm, entity, selector, next.toDouble())
    }
    // Round 4 display range: the arc maps 55–85°F (see
    // thermostatDisplayRange), so 67°/69° thumbs get clear
    // separation instead of crowding on the HA range. Values
    // outside it are NOT rewritten — their thumbs clamp to the
    // arc end for drawing and the flag below says so.
    val displayRange = thermostatDisplayRange(min, max)
    val outOfRangeNote: String? = when {
        heatCool && (heat < displayRange.start || heat > displayRange.endInclusive) ->
            "Heat ${heat.toDouble().formatTemp()} is outside the dial range " +
                "(${displayRange.start.toDouble().formatTemp()}–${displayRange.endInclusive.toDouble().formatTemp()}) — shown at the arc end"
        heatCool && (cool < displayRange.start || cool > displayRange.endInclusive) ->
            "Cool ${cool.toDouble().formatTemp()} is outside the dial range " +
                "(${displayRange.start.toDouble().formatTemp()}–${displayRange.endInclusive.toDouble().formatTemp()}) — shown at the arc end"
        !heatCool && (single < displayRange.start || single > displayRange.endInclusive) ->
            "Setpoint ${single.toDouble().formatTemp()} is outside the dial range " +
                "(${displayRange.start.toDouble().formatTemp()}–${displayRange.endInclusive.toDouble().formatTemp()}) — shown at the arc end"
        else -> null
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // The spec's dial: 240-degree sweep, dual pill thumbs, mode arcs.
        // The center content overlays the dial face, as run #38 had it.
        Box(contentAlignment = Alignment.Center) {
            ThermostatDial(
                heat = heat,
                cool = cool,
                single = single,
                dual = heatCool,
                current = current?.toFloat(),
                currentLabel = current?.let { it.formatTemp() },
                range = displayRange,
                mode = entity.state,
                selector = selector,
                onHeatChange = { heat = it },
                onCoolChange = { cool = it },
                onSingleChange = { single = it },
                onFinished = { sel, v -> applyClimateSetpoint(vm, entity, sel, v.toDouble()) },
                onInsideTap = if (heatCool) {
                    { selector = toggleSelector(selector) }
                } else {
                    null
                },
                pulseSelector = pulseSelector,
                diameter = diameter,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (heatCool) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KineticTempText(
                            heat,
                            HeatOrange,
                            active = selector == SetpointSelector.HEATING,
                            backdrop = numberBackdrop,
                        )
                        Spacer(Modifier.width(16.dp))
                        KineticTempText(
                            cool,
                            CoolBlue,
                            active = selector == SetpointSelector.COOLING,
                            backdrop = numberBackdrop,
                        )
                    }
                } else {
                    KineticTempText(single, dialAccent, active = true, fontSize = 72.sp, backdrop = numberBackdrop)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveStepper("−", onClick = { stepActive(-1f) })
                    ExpressiveStepper("+", onClick = { stepActive(1f) })
                }
            }
        }
        // Setpoint selector, OUTSIDE the dial face (2026-11-01
        // round 1): the Heat/Cool selector used to live as chips
        // colliding with the scalloped dial border and the dual
        // 68°/69° numbers. It now sits in its own segmented pill
        // bar below the dial, in heat/cool mode only, tinted per
        // side (heat container / cool container) — the dial law
        // itself is untouched: inside-tap still toggles, handles
        // are still grab-only, clamps still hold.
        if (outOfRangeNote != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                outOfRangeNote,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (heatCool) {
            // Round 4: a full 24dp of daylight above the pill so
            // it never overlaps the dial's bottom edge.
            Spacer(Modifier.height(24.dp))
            val scheme = MaterialTheme.colorScheme
            SegmentedOptionBar(
                options = listOf(
                    SetpointSelector.HEATING.name to "Heat ${heat.toDouble().formatTemp()}",
                    SetpointSelector.COOLING.name to "Cool ${cool.toDouble().formatTemp()}",
                ),
                selected = selector.name,
                onSelect = { selector = SetpointSelector.valueOf(it) },
                modifier = Modifier.fillMaxWidth(),
                activeColors = { value ->
                    if (value == SetpointSelector.HEATING.name) {
                        heatSetpointContainer(scheme, false) to heatSetpointOnContainer(scheme, false)
                    } else {
                        coolSetpointContainer(scheme) to coolSetpointOnContainer(scheme)
                    }
                },
            )
        }
    }
}

/**
 * Kinetic counter typography: whole degrees — the standing 1-degree rule
 * overrules the spec's %.1f format — sliding + fading + scaling by
 * direction, with tabular figures so digits never jitter.
 *
 * The default size is the DUAL-number size (the single-setpoint call
 * site passes 72.sp explicitly). Dual numbers sit inside the dial's
 * ring, so their width must clear the track's inner edge: at 64.sp
 * the "67° + 16dp + 70°" row spans ~208dp (half-width ~104dp),
 * past the 260dp pop-out dial's ~102dp inner edge at rest — and the
 * kinetic scale-out (1.15x) pushes it to ~239dp, a definite overlap
 * with the bar (the 2026-10-29 verdict video's circled 67°/70°).
 * At 52.sp the row spans ~169dp (~194dp at peak scale), clearing the
 * bar on both the 260dp pop-out dial and the 300dp page dial (whose
 * inner edge is ~122dp). The single number stays 72.sp: ~108dp wide,
 * no overlap.
 */
@Composable
private fun KineticTempText(value: Float, color: Color, active: Boolean, fontSize: TextUnit = 52.sp, backdrop: Color) {
    val text = value.toDouble().formatTemp()
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val up = (targetState.removeSuffix("°").toFloatOrNull() ?: 0f) >=
                (initialState.removeSuffix("°").toFloatOrNull() ?: 0f)
            if (up) {
                (slideInVertically { -it / 2 } + fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith
                    (slideOutVertically { it / 2 } + fadeOut() + scaleOut(targetScale = 1.15f))
            } else {
                (slideInVertically { it / 2 } + fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut() + scaleOut(targetScale = 1.15f))
            }.using(SizeTransform(clip = false))
        },
        label = "kineticTemp",
    ) { t ->
        Text(
            t,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            // Contrast-floored numbers (2026-10-30 verdict): the
            // raw token fails on the pastel disc in both themes, and
            // the 0.45-alpha inactive number fails outright. Active
            // numbers meet 4.5:1; inactive ones (52sp+ bold = large
            // text) meet 3:1 while staying visibly muted.
            color = if (active) ensureContrast(color, backdrop, 4.5f)
            else ensureContrast(compositeOver(color.copy(alpha = 0.45f), backdrop), backdrop, 3.0f),
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

/**
 * 24-hour temperature/humidity history charts for the thermostat
 * pop-out (Round 4): the Ecobee's linked sensors (see
 * findLinkedSensors — the seeded sensor.thermostat_current_*
 * pair first), each with its own sparkline + Min/Current/Max.
 * A chart renders only when its sensor exists in live state;
 * a sensor with no cached history yet shows the shimmer, never
 * an invented series. Placed below the Mode and Fan bars in the
 * pop-out, inside the panel's scroll wrapper.
 */
@Composable
private fun ThermostatHistorySection(entity: HaEntity, vm: HavenViewModel, states: Map<String, HaEntity>) {
    val sensors = findLinkedSensors(states, entity)
    val temp = sensors.firstOrNull { it.entityId.contains("temperature") }
    val hum = sensors.firstOrNull { it.entityId.contains("humidity") }
    if (temp == null && hum == null) return
    Spacer(Modifier.height(12.dp))
    Text("Last 24 hours", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    if (temp != null) ThermostatSensorHistory(temp, vm)
    if (hum != null) ThermostatSensorHistory(hum, vm)
}

@Composable
private fun ThermostatSensorHistory(sensor: HaEntity, vm: HavenViewModel) {
    val cached = vm.historyFor(sensor.entityId)
    LaunchedEffect(sensor.entityId) { vm.refreshHistory(sensor.entityId) }
    val unit = sensor.attrString("unit_of_measurement")
    Column(Modifier.fillMaxWidth()) {
        Text(sensorDisplayName(sensor), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(sensor.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (cached == null) {
            ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
        } else if (cached.size >= 2) {
            Sparkline(cached, accentFor(sensor.entityId), Modifier.fillMaxWidth().height(120.dp))
            val mm = sparklineMinCurrentMax(cached)
            if (mm != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Min ${fmtThermostatHistory(mm.first, unit)} · Current ${fmtThermostatHistory(mm.second, unit)} · Max ${fmtThermostatHistory(mm.third, unit)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text("No history yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Whole numbers print whole; the rest get one decimal; unit suffix when present. */
private fun fmtThermostatHistory(v: Double, unit: String?): String {
    val num = if (v == v.toInt().toDouble()) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
    return if (unit != null) "$num $unit" else num
}
