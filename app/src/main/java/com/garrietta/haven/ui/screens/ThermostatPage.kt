package com.garrietta.haven.ui.screens
import com.garrietta.haven.data.detailBadgeOverride
import com.garrietta.haven.data.detailPageAccent
import com.garrietta.haven.ui.components.LocalSuppressTraveler
import com.garrietta.haven.ui.components.compositeOver
import com.garrietta.haven.ui.components.isDarkBackdrop
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.garrietta.haven.ui.controls.entityIcon
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
    // Feedback round 4, item 2: the header badge carries the
    // card's preset pair, like every other detail header.
    val popoutCustom = rememberCardCustom(vm)
    val popoutBadge = detailBadgeOverride(
        popoutCustom.tilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
    )
    // Feedback round 5, item 4: the tile preset accent tints the
    // stepper pills below the disc, like the other popup elements.
    val popoutAccent = detailPageAccent(
        popoutCustom.tilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
    )
    // Feedback round 4, item 4: with the traveler suppressed
    // (detail from a sub-panel), the badge renders from frame 0.
    val suppressTraveler = LocalSuppressTraveler.current
    val modeColor = climateModeColor(entity.state, entity.attrString("hvac_action"))
    val min = entity.attrDouble("min_temp")?.toFloat() ?: 10f
    val max = entity.attrDouble("max_temp")?.toFloat() ?: 30f
    // Round 6: the dial range is computed HERE, in the popout,
    // through the ONE shared resolver (see
    // resolveThermostatDisplayRange): the decade formula around
    // the current setpoints (66/67° → 50–80°) unless Settings
    // pins both override values, in which case the override wins
    // on this surface exactly as on the sheet and detail. Values
    // themselves are never rewritten; out-of-range thumbs clamp
    // to the arc end and the Hero flags it. The Hero takes this
    // range as a parameter, so the popout surface cannot silently
    // fall back to a different range.
    val lowAttr = entity.attrDouble("target_temp_low")?.toFloat()
    val highAttr = entity.attrDouble("target_temp_high")?.toFloat()
    val singleAttr = entity.attrDouble("temperature")?.toFloat()
    val displaySetpoints = if (lowAttr != null && highAttr != null) {
        listOf(lowAttr, highAttr)
    } else {
        listOfNotNull(singleAttr)
    }
    val displayRange = resolveThermostatDisplayRange(
        min, max, displaySetpoints,
        vm.settingsStore.thermostatDisplayMin,
        vm.settingsStore.thermostatDisplayMax,
    )
    // UI rewrite round §6: the dial state is hoisted here so the
    // popout renders the dial INSIDE the scallop disc and the
    // Heat/Cool bar BELOW it (see rememberThermostatDialState).
    val dialState = rememberThermostatDialState(entity, vm, min, max, displayRange)
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
                // Feedback round 11, item 3: the one icon path.
                entityIcon(popoutCustom.iconKeys, entity),
                accentFor(entity.entityId),
                modifier = Modifier.size(48.dp).then(if (suppressTraveler) Modifier else Modifier.hideWhileTraveling()).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClose() },
                badgeOverride = popoutBadge,
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
        // expressive shape, mode-colored; the dial sits centered on
        // it, and the Heat/Cool bar sits BELOW it (never inside it).
        // UI rewrite round §6 geometry (CORRECTING the old comment,
        // which compared the bloom against the 160dp disc radius
        // instead of the scallop valley): SCALLOP12's valleys sit at
        // 0.72 x radius = 115.2dp, but the 240dp dial's outer bloom
        // reaches 134dp — so bloom crossed the border at the top,
        // where 67/69 thumbs sit near the valleys. THERMOSTAT_SCALLOP
        // valleys sit at 0.88 x radius = 140.8dp. Round 6 moves the
        // track inset 20dp -> 16dp and the sweep 240° -> 270° (see
        // ThermostatDial). Round 8 moves the inset 16dp -> 12dp and
        // the thumbs from 30x20 ovals to 20dp circles, so with the
        // 240dp dial the hard-edged elements still clear the
        // valley: track outer 119dp grabbed (21.8 margin), thumb
        // outer 130dp (10.8, full stagger + circle radius), track
        // inner edge 100dp idle (was 96dp), so the dual-number row
        // GAINS clearance: its inner width is now 200dp (was
        // 192dp) against a ~184dp row. The
        // bloom is a soft alpha fade, unchanged in kind from round
        // 5; its peak-pulse extent past the valley is a fade, never
        // a hard edge, and the scallop itself is untouched (the
        // user likes it). Thumb grab targets are untouched — only
        // the visual thumb shape changed.
        Box(
            Modifier.size(320.dp).background(modeColor.copy(alpha = 0.20f), stateShape(StateShape.THERMOSTAT_SCALLOP)),
            contentAlignment = Alignment.Center,
        ) {
            ThermostatDialBlock(
                dialState, entity, vm, displayRange, diameter = 240.dp,
                // Numbers sit on the scallop disc: floor them against
                // the disc color itself (2026-10-30 dial-clash fix).
                numberBackdrop = compositeOver(
                    modeColor.copy(alpha = 0.20f),
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }
        // Feedback round 5, item 6: the - / + steppers live HERE,
        // below the scallop disc with 16dp of daylight — never
        // inside the dial face, never on the radial. The scallop
        // itself is untouched (the user likes it).
        Spacer(Modifier.height(16.dp))
        ThermostatSteppers(dialState, popoutAccent)
        // The Heat/Cool bar + out-of-range note live BELOW the disc
        // with 28dp of daylight (the bar used to live inside the
        // disc, where its square corners crossed the scallop's
        // curved bottom edge — the flagged overlap).
        ThermostatBelowDial(dialState)
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

/** Hero dial: Google Home's dual numbers + steppers below the dial + ring marks.
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
    displayRange: ClosedFloatingPointRange<Float>,
    diameter: Dp = 300.dp,
    /** Color directly behind the dial numbers (the scallop disc). */
    numberBackdrop: Color,
) {
    val state = rememberThermostatDialState(entity, vm, min, max, displayRange)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ThermostatDialBlock(state, entity, vm, displayRange, diameter, numberBackdrop)
        // Feedback round 5, item 6: the steppers sit below the
        // dial block, clear of the radial — same move as the
        // popout, so no surface keeps them inside the dial face.
        Spacer(Modifier.height(16.dp))
        ThermostatSteppers(state, accent = null)
        ThermostatBelowDial(state)
    }
}

/**
 * Feedback round 5, item 6: the thermostat - / + steppers as
 * their own centered row, rendered BELOW the dial (never inside
 * the dial face), so they can never sit on the temperature
 * radial. Item 4: the tile preset accent tints the pills when
 * set (content = onSurface, the pill-content law); null keeps
 * the scheme defaults byte-identical.
 */
@Composable
private fun ThermostatSteppers(
    state: ThermostatDialState,
    accent: Color?,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveStepper(
            "−",
            onClick = { state.stepActive(-1f) },
            containerColor = accent,
            contentColor = if (accent != null) scheme.onSurface else null,
        )
        ExpressiveStepper(
            "+",
            onClick = { state.stepActive(1f) },
            containerColor = accent,
            contentColor = if (accent != null) scheme.onSurface else null,
        )
    }
}


/**
 * UI rewrite round §6: the thermostat dial's state, hoisted out of
 * [ThermostatHero] so the popout can render the dial INSIDE the
 * scalloped disc and the Heat/Cool bar BELOW it (the bar used to
 * live inside the disc, where its square corners crossed the
 * scallop's curved bottom edge). The MutableStates are remembered
 * here with the same keys Hero used (setpoints re-initialize
 * whenever HA reports a new setpoint; the selector follows the hvac
 * mode and survives setpoint updates within a mode); the derived
 * values (pulseSelector, dialAccent, outOfRangeNote) are recomputed
 * on every composition from the current state values, so they never
 * go stale. Setpoint law is untouched: 1°F snap, handle-grab-only,
 * clamps/gap-proofing, values never rewritten (out-of-range thumbs
 * clamp to the arc end for drawing and the note says so).
 */
private class ThermostatDialState(
    val heat: MutableState<Float>,
    val cool: MutableState<Float>,
    val single: MutableState<Float>,
    val selector: MutableState<SetpointSelector>,
    val heatCool: Boolean,
    val pulseSelector: SetpointSelector?,
    val dialAccent: Color,
    val stepActive: (Float) -> Unit,
    val outOfRangeNote: String?,
)

@Composable
private fun rememberThermostatDialState(
    entity: HaEntity,
    vm: HavenViewModel,
    min: Float,
    max: Float,
    displayRange: ClosedFloatingPointRange<Float>,
): ThermostatDialState {
    val accent = accentFor(entity.entityId)
    val lowInit = entity.attrDouble("target_temp_low")
    val highInit = entity.attrDouble("target_temp_high")
    val singleInit = entity.attrDouble("temperature")
    val heatCool = lowInit != null && highInit != null
    val heat = remember(lowInit) { mutableStateOf(lowInit?.toFloat()?.coerceIn(min, max) ?: min) }
    val cool = remember(highInit) { mutableStateOf(highInit?.toFloat()?.coerceIn(min, max) ?: max) }
    val single = remember(singleInit) { mutableStateOf(singleInit?.toFloat()?.coerceIn(min, max) ?: min) }
    val selector = remember(entity.entityId, entity.state) { mutableStateOf(defaultSelector(entity.state)) }
    // Pulsing thumb: the dial's fluid indicator while the system is
    // actively heating or cooling (hvac_action, not mode — a
    // thermostat in heat mode that is idle does not pulse).
    val pulseSelector = when (entity.attrString("hvac_action")) {
        "heating" -> SetpointSelector.HEATING
        "cooling" -> if (heatCool) SetpointSelector.COOLING else SetpointSelector.HEATING
        else -> null
    }
    val dialAccent: Color = if (heatCool) {
        if (selector.value == SetpointSelector.HEATING) HeatOrange else CoolBlue
    } else {
        when (entity.state) {
            "heat" -> HeatOrange
            "cool" -> CoolBlue
            else -> accent
        }
    }
    val stepActive: (Float) -> Unit = { delta ->
        val next = if (heatCool) {
            if (selector.value == SetpointSelector.HEATING) {
                clampHeatSetpoint((heat.value + delta).toDouble(), min.toDouble(), cool.value.toDouble()).toFloat()
            } else {
                clampCoolSetpoint((cool.value + delta).toDouble(), heat.value.toDouble(), max.toDouble()).toFloat()
            }
        } else {
            snapToDegree((single.value + delta).toDouble()).toFloat().coerceIn(min, max)
        }
        if (heatCool) {
            if (selector.value == SetpointSelector.HEATING) heat.value = next else cool.value = next
        } else {
            single.value = next
        }
        applyClimateSetpoint(vm, entity, selector.value, next.toDouble())
    }
    // Values outside the display range are NOT rewritten — their
    // thumbs clamp to the arc end for drawing and the flag says so.
    val outOfRangeNote: String? = when {
        heatCool && (heat.value < displayRange.start || heat.value > displayRange.endInclusive) ->
            "Heat ${heat.value.toDouble().formatTemp()} is outside the dial range " +
                "(${displayRange.start.toDouble().formatTemp()}–${displayRange.endInclusive.toDouble().formatTemp()}) — shown at the arc end"
        heatCool && (cool.value < displayRange.start || cool.value > displayRange.endInclusive) ->
            "Cool ${cool.value.toDouble().formatTemp()} is outside the dial range " +
                "(${displayRange.start.toDouble().formatTemp()}–${displayRange.endInclusive.toDouble().formatTemp()}) — shown at the arc end"
        !heatCool && (single.value < displayRange.start || single.value > displayRange.endInclusive) ->
            "Setpoint ${single.value.toDouble().formatTemp()} is outside the dial range " +
                "(${displayRange.start.toDouble().formatTemp()}–${displayRange.endInclusive.toDouble().formatTemp()}) — shown at the arc end"
        else -> null
    }
    return ThermostatDialState(heat, cool, single, selector, heatCool, pulseSelector, dialAccent, stepActive, outOfRangeNote)
}

/** The dial itself plus its center overlay (numbers, indoor label, steppers) — the part that sits ON the scallop disc. */
@Composable
private fun ThermostatDialBlock(
    state: ThermostatDialState,
    entity: HaEntity,
    vm: HavenViewModel,
    displayRange: ClosedFloatingPointRange<Float>,
    diameter: Dp,
    numberBackdrop: Color,
) {
    val current = entity.attrDouble("current_temperature")
    // Feedback round 3, item 5: the dial used to sit in a Column
    // with its range-end legend row below it (min at the bar's left
    // end, max at its right end, spread to the dial width) — the
    // popout dial used the dynamic range since round 2 but never
    // SHOWED the window's ends. Feedback round 8, item (d):
    // that row is GONE — the ends are now drawn inside the dial
    // canvas itself, directly beneath the arc endpoints (see
    // ThermostatDial), so the dial block is just the dial +
    // centered overlay and centers cleanly in the scallop. The
    // row stays clear of the steppers trivially: it no longer
    // exists (steppers live below the disc / below the dial block,
    // never in this row).
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
        ThermostatDial(
            heat = state.heat.value,
            cool = state.cool.value,
            single = state.single.value,
            dual = state.heatCool,
            current = current?.toFloat(),
            // Round 6: the canvas label is retired — the indoor
            // temperature renders as a Compose Text above the
            // steppers below (one label, not two), so it can never
            // collide with the dial's bottom gap or duplicate the
            // center numbers.
            currentLabel = null,
            range = displayRange,
            mode = entity.state,
            selector = state.selector.value,
            onHeatChange = { state.heat.value = it },
            onCoolChange = { state.cool.value = it },
            onSingleChange = { state.single.value = it },
            onFinished = { sel, v -> applyClimateSetpoint(vm, entity, sel, v.toDouble()) },
            onInsideTap = if (state.heatCool) {
                { state.selector.value = toggleSelector(state.selector.value) }
            } else {
                null
            },
            pulseSelector = state.pulseSelector,
            diameter = diameter,
        )
        Column(Modifier.offset(y = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Feedback round 8, item (b): the readout sits 16dp
            // BELOW the dial's geometric center, filling the
            // empty bottom-gap space instead of floating high —
            // numbers + Indoor label move together, and the
            // offset keeps glyphs clear of the ring on both dial
            // sizes (see the gap math below). Offset, not
            // padding: a visual shift with no layout change.
            // Hoisted above the dual/single branch so the measurer
            // slot never shifts if the mode changes mid-lifetime.
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            if (state.heatCool) {
                // Feedback round 5, item 5: the gap between the two
                // numbers is COMPUTED from dial geometry, never a
                // fixed guess — the measured label widths plus the
                // track's inner width (diameter minus twice the
                // 12dp track inset and the 8dp idle half-stroke =
                // diameter − 40dp; see ThermostatDial's
                // trackRadiusPx/trackStroke) feed dualNumberGapPx,
                // which spreads the pair to fill the inner width
                // with 8dp of clearance per side, floored at an
                // ample 32dp. Round 8's 12dp inset widens the inner
                // width again (240dp popout: 200dp, was 192dp;
                // 300dp hero: 260dp, was 252dp). Feedback round 8,
                // item 2: the pair drops 52.sp -> 44.sp, because at
                // 52.sp the popout row (~185dp) kissed the ring at
                // rest and the kinetic 1.15x scale-out pushed it
                // past the ring mid-change — the user's screenshot
                // showed the numbers clipping the radial. At
                // 44.sp the labels span ~129dp, the row spreads
                // to ~184dp (8dp daylight per side at rest), and
                // even the 1.15x outgoing peak (~98dp from center
                // against the 100dp track inner edge) clears the
                // ring on the 240dp popout — label-box corners may
                // kiss as in round 5, glyphs clear on side
                // bearings. The 16dp drop (item (b)) is already
                // in these numbers: the row's bottom corners sit
                // ~7dp inside the ring at rest.
                // Visual only: setpoints are never touched, only
                // label positions.
                val numberStyle = TextStyle(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                )
                val heatLabelWidth = textMeasurer.measure(
                    state.heat.value.toDouble().formatTemp(), numberStyle,
                ).size.width.toFloat()
                val coolLabelWidth = textMeasurer.measure(
                    state.cool.value.toDouble().formatTemp(), numberStyle,
                ).size.width.toFloat()
                val innerWidthPx = with(density) { (diameter - 40.dp).toPx() }
                val dualGap = with(density) {
                    dualNumberGapPx(
                        heatLabelWidth, coolLabelWidth, innerWidthPx,
                        32.dp.toPx(), 8.dp.toPx(),
                    ).toDp()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KineticTempText(
                        state.heat.value,
                        HeatOrange,
                        active = state.selector.value == SetpointSelector.HEATING,
                        backdrop = numberBackdrop,
                    )
                    Spacer(Modifier.width(dualGap))
                    KineticTempText(
                        state.cool.value,
                        CoolBlue,
                        active = state.selector.value == SetpointSelector.COOLING,
                        backdrop = numberBackdrop,
                    )
                }
            } else {
                KineticTempText(state.single.value, state.dialAccent, active = true, fontSize = 72.sp, backdrop = numberBackdrop)
            }
            // Round 6: the indoor temperature label lives HERE,
            // in the dial's center below the numbers — never
            // inside the dial canvas. Feedback round 5, item 6:
            // the - / + steppers NO LONGER live here: they moved
            // below the dial (see ThermostatSteppers, rendered by
            // the popout under the scallop disc and by the hero
            // under the dial block), clear of the radial, so the
            // centered overlay holds only numbers + Indoor text
            // and no text overlaps the ring at rest.
            if (current != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Indoor ${current.formatTemp()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }
}

/**
 * Everything that sits BELOW the scallop disc: the out-of-range
 * note, then (heat/cool mode only) the Heat/Cool segmented bar with
 * 28dp of daylight above it — never inside the disc, so the bar can
 * never overlap the scallop's bottom edge. The dial law itself is
 * untouched: inside-tap still toggles, handles are still grab-only,
 * clamps still hold.
 */
@Composable
private fun ThermostatBelowDial(state: ThermostatDialState) {
    if (state.outOfRangeNote != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            state.outOfRangeNote,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.heatCool) {
        Spacer(Modifier.height(28.dp))
        val scheme = MaterialTheme.colorScheme
        SegmentedOptionBar(
            options = listOf(
                SetpointSelector.HEATING.name to "Heat ${state.heat.value.toDouble().formatTemp()}",
                SetpointSelector.COOLING.name to "Cool ${state.cool.value.toDouble().formatTemp()}",
            ),
            selected = state.selector.value.name,
            onSelect = { state.selector.value = SetpointSelector.valueOf(it) },
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

/**
 * Kinetic counter typography: whole degrees — the standing 1-degree rule
 * overrules the spec's %.1f format — sliding + fading + scaling by
 * direction, with tabular figures so digits never jitter.
 *
 * The default size is the DUAL-number size (the single-setpoint call
 * site passes 72.sp explicitly). Dual numbers sit inside the dial's
 * ring, so their width must clear the track's inner edge: at 64.sp
 * the old "67° + 16dp + 70°" row spanned ~208dp (half-width ~104dp),
 * past the 260dp pop-out dial's ~102dp inner edge at rest — and the
 * kinetic scale-out (1.15x) pushed it to ~239dp, a definite overlap
 * with the bar (the 2026-10-29 verdict video's circled 67°/70°).
 * At 52.sp with the feedback-round-5 computed gap the row spanned
 * labels + gap: ~185dp at rest on the 240dp pop-out dial (inner
 * width 192dp since round 6's 16dp inset — ~7dp of daylight total,
 * no kiss) and ~236dp on the 300dp hero dial (inner width 252dp,
 * full 8dp clearance per side). The kinetic peak scale (1.15x, outgoing text only, sliding
 * vertically away) could transiently touch the ring during a value
 * change; at rest the pair was clear. Feedback round 8, item 2:
 * the dual size drops to 44.sp — the largest size whose 1.15x
 * outgoing peak still clears the 240dp popout's 100dp inner edge
 * (~98dp) — because the user still saw the 52.sp numbers clipping
 * the radial. The single number stays 72.sp: ~108dp wide, no overlap.
 */
@Composable
private fun KineticTempText(value: Float, color: Color, active: Boolean, fontSize: TextUnit = 44.sp, backdrop: Color) {
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
