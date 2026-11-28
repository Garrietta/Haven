package com.garrietta.haven.ui.screens
import com.garrietta.haven.Screen
import com.garrietta.haven.data.detailBadgeOverride
import com.garrietta.haven.data.detailPageAccent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.MenuSection
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.components.DialogTitleText
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.chartLineColor
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.LocalHeroTarget
import com.garrietta.haven.ui.components.LocalPopupDragHandle
import com.garrietta.haven.ui.components.LocalPopupDragRangePx
import com.garrietta.haven.ui.components.morphHeaderBadgeAlpha
import com.garrietta.haven.ui.components.popupDragDetector
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.coolSetpointContainer
import com.garrietta.haven.ui.components.coolSetpointOnContainer
import com.garrietta.haven.ui.components.heatSetpointContainer
import com.garrietta.haven.ui.components.heatSetpointOnContainer
import com.garrietta.haven.ui.theme.StateShape
import com.garrietta.haven.ui.theme.stateShape
import com.garrietta.haven.ui.controls.ExpressiveStepper
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.applyClimateSetpoint
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.entityIcon
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.theme.HavenChrome
import androidx.compose.foundation.text.TextAutoSize


/**
 * A thermostat's popup: the same header every device popup has, then
 * [ThermostatControl], its last 24 hours and the rows every popup ends
 * with. The Climate tab shows the same control, so a thermostat looks
 * and works the same wherever it is opened.
 */
@Composable
fun ThermostatPopout(entity: HaEntity, vm: HavenViewModel, onClose: () -> Unit, navigate: ((Screen) -> Unit)? = null) {
    val custom = rememberCardCustom(vm)
    val nameOf = rememberEntityNamer(vm)
    val badge = tileBadgePair(
        custom.tilePresets, custom.domainTilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
        MaterialTheme.colorScheme, accentFor(entity.entityId),
    )
    // Dragging the header pulls the popup down to close it; null outside popups.
    val dragHandle = LocalPopupDragHandle.current
    val dragRangePx = LocalPopupDragRangePx.current
    // The header reports where it lands so the badge travelling from the tile lands on it.
    val heroTarget = LocalHeroTarget.current
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) }
                .then(
                    if (dragHandle != null) {
                        Modifier.popupDragDetector(
                            dragHandle.state,
                            { dy -> dragHandle.onDelta(dy, dragRangePx()) },
                            { v -> dragHandle.onRelease(v, dragRangePx()) },
                        )
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DomainBadge(
                entity,
                entityIcon(custom.iconKeys, entity),
                accentFor(entity.entityId),
                modifier = Modifier.size(48.dp).morphHeaderBadgeAlpha().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClose() },
                badgeOverride = badge,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                DialogTitleText(nameOf(entity), Modifier.fillMaxWidth())
                Text(entity.dialogHeaderState(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(8.dp))
        ThermostatControl(entity, vm)
        val states by vm.states.collectAsState()
        ThermostatHistorySection(entity, vm, states)
        Spacer(Modifier.height(16.dp))
        EntityPopupMenuRows(vm, entity, onClose = onClose, navigate = navigate, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * A thermostat, controlled: the dial in its scalloped disc in the color
 * of what it is doing, − and + under it, then Mode, Fan and Comfort in
 * the app's one choice row, each only where the thermostat reports it.
 * The disc takes the width it is given, up to 320dp. Its popup and the
 * Climate tab both draw this, so there is one thermostat design.
 */
@Composable
fun ThermostatControl(entity: HaEntity, vm: HavenViewModel, modifier: Modifier = Modifier) {
    val custom = rememberCardCustom(vm)
    val accent = detailPageAccent(
        custom.tilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
        custom.domainTilePresets,
    )
    val modeColor = climateModeColor(entity.state, entity.attrString("hvac_action"))
    val min = entity.attrDouble("min_temp")?.toFloat() ?: 10f
    val max = entity.attrDouble("max_temp")?.toFloat() ?: 30f
    // The dial's range follows the set points (a span of about 30
    // degrees around them); out-of-range thumbs clamp to the arc end
    // and the note under the dial says so.
    val lowAttr = entity.attrDouble("target_temp_low")?.toFloat()
    val highAttr = entity.attrDouble("target_temp_high")?.toFloat()
    val singleAttr = entity.attrDouble("temperature")?.toFloat()
    val setpoints = if (lowAttr != null && highAttr != null) listOf(lowAttr, highAttr) else listOfNotNull(singleAttr)
    val displayRange = resolveThermostatDisplayRange(min, max, setpoints, null, null)
    val dialState = rememberThermostatDialState(entity, vm, min, max, displayRange)
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        val disc = minOf(maxWidth, 320.dp)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(disc).background(modeColor.copy(alpha = 0.20f), stateShape(StateShape.THERMOSTAT_SCALLOP)),
                contentAlignment = Alignment.Center,
            ) {
                ThermostatDialBlock(
                    dialState, entity, vm, displayRange, diameter = disc * 0.75f,
                    numberBackdrop = compositeOver(modeColor.copy(alpha = 0.20f), MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
            Spacer(Modifier.height(16.dp))
            ThermostatSteppers(dialState, accent, outlineColor = MaterialTheme.colorScheme.outlineVariant)
            ThermostatBelowDial(dialState)
            val modes = entity.attrList("hvac_modes")?.mapNotNull { it as? String }.orEmpty()
            if (modes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                com.garrietta.haven.ui.components.LabeledChoiceRow(
                    "Mode", orderedHvacModes(modes).map { it to formatHvacMode(it) }, entity.state,
                    { vm.setClimateHvacMode(entity.entityId, it) },
                )
            }
            val fanModes = entity.attrList("fan_modes")?.mapNotNull { it as? String }.orEmpty()
            if (fanModes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                com.garrietta.haven.ui.components.LabeledChoiceRow(
                    "Fan", orderedFanModes(fanModes).map { it to choiceLabel(it) }, entity.attrString("fan_mode"),
                    { vm.setClimateFanMode(entity.entityId, it) },
                )
            }
            val presets = entity.attrList("preset_modes")?.mapNotNull { it as? String }.orEmpty()
            if (presets.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                com.garrietta.haven.ui.components.LabeledChoiceRow(
                    "Comfort", presets.map { it to choiceLabel(it) }, entity.attrString("preset_mode"),
                    { vm.setClimatePresetMode(entity.entityId, it) },
                )
            }
        }
    }
}

/** "fan_only" reads "Fan only", "home" reads "Home". */
private fun choiceLabel(value: String): String = value.replace('_', ' ').replaceFirstChar { it.uppercase() }


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
    // Round 25, item 4: a subtle 1dp outline so the stepper fill
    // reads against the popup background at rest. The popout passes
    // outlineVariant; the Hero passes nothing (unchanged).
    outlineColor: Color? = null,
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
            outlineColor = outlineColor,
        )
        ExpressiveStepper(
            "+",
            onClick = { state.stepActive(1f) },
            containerColor = accent,
            contentColor = if (accent != null) scheme.onSurface else null,
            outlineColor = outlineColor,
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
            // Round 21, item 2: the track color lerps from the
            // disc it sits on (numberBackdrop is the composited
            // scallop-disc color in the popout).
            trackBackdrop = numberBackdrop,
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
        com.garrietta.haven.ui.components.ChoiceRow(
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
    SectionTitle("Last 24 hours")
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
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val chartHeight = com.garrietta.haven.ui.components.historyChartHeight(maxWidth) * 0.7f
            if (cached == null) {
                ShimmerBox(Modifier.fillMaxWidth().height(chartHeight))
            } else if (cached.size >= 2) {
                Column {
                    Sparkline(cached, chartLineColor(sensorKind(sensor.entityId, sensor.attrString("device_class"), sensor.friendlyName), isDarkBackdrop(MaterialTheme.colorScheme.background)), Modifier.fillMaxWidth().height(chartHeight))
                    val mm = sparklineMinCurrentMax(cached)
                    if (mm != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Low ${fmtThermostatHistory(mm.first, unit)} · High ${fmtThermostatHistory(mm.third, unit)} over 24 hours",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (cached != null && cached.size < 2) {
            Text("No history yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun fmtThermostatHistory(v: Double, unit: String?): String = com.garrietta.haven.data.formatReading(v.toString(), unit, null)
