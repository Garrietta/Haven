package com.garrietta.haven.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.theme.springMorph
import com.garrietta.haven.ui.controls.ExpressiveStepper
import com.garrietta.haven.ui.controls.applyClimateSetpoint
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.stateText
import org.json.JSONObject

private val HEAT_COLOR = Color(0xFFD84315)
private val COOL_COLOR = Color(0xFF1565C0)

/**
 * The thermostat as a full page, not a sheet, laid out the way Google
 * Home lays its thermostat out: a hero dial showing BOTH setpoints
 * together (heat left in warm orange, cool right in cool blue, the
 * active selector at full strength and the inactive one muted) with
 * −/+ steppers below the numbers inside the dial, heat + cool markers
 * and a current-temperature indicator on the ring, then a gradient
 * heat/cool range bar with the live operating state, then Google-style
 * rows (Mode, Comfort, Fan) and Indoor/Outdoor rows below. Every row
 * reads only what Home Assistant exposes — a row with no backing state
 * hides instead of inventing one.
 *
 * The dial still edits one setpoint at a time, in whole degrees: in
 * heat/cool mode an inside tap toggles the active selector between the
 * heating and cooling setpoints (the highlighted number IS the indicator
 * now — no separate chip); only grabbing the radial handle and dragging
 * it moves the value, clamped so the setpoints cannot cross and the
 * bottom gap is ignored. Google Home's drag-anywhere-on-ring is NOT
 * adopted: handle-grab-only stays. Fahrenheit throughout, and onFinished
 * still reports the actually-set value.
 */
@Composable
fun ThermostatPageScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    val min = entity?.attrDouble("min_temp")?.toFloat() ?: 10f
    val max = entity?.attrDouble("max_temp")?.toFloat() ?: 30f
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        DetailTopBar(entity?.friendlyName ?: entityId, onBack)
        if (entity == null) {
            Text(
                "This entity is no longer available.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (LocalWideLayout.current) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ThermostatHero(entity, vm, min, max)
                    Spacer(Modifier.height(16.dp))
                    HeatCoolRangeBar(entity, min, max)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp)) {
                    ThermostatCards(entity, vm, states)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThermostatHero(entity, vm, min, max)
                Spacer(Modifier.height(16.dp))
                HeatCoolRangeBar(entity, min, max)
                Spacer(Modifier.height(16.dp))
                ThermostatCards(entity, vm, states)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
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
private fun ThermostatHero(
    entity: HaEntity,
    vm: HavenViewModel,
    min: Float,
    max: Float,
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
        if (selector == SetpointSelector.HEATING) HEAT_COLOR else COOL_COLOR
    } else {
        when (entity.state) {
            "heat" -> HEAT_COLOR
            "cool" -> COOL_COLOR
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
                range = min..max,
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
                diameter = 300.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (heatCool) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KineticTempText(
                            heat,
                            HEAT_COLOR,
                            active = selector == SetpointSelector.HEATING,
                        )
                        Spacer(Modifier.width(16.dp))
                        KineticTempText(
                            cool,
                            COOL_COLOR,
                            active = selector == SetpointSelector.COOLING,
                        )
                    }
                } else {
                    KineticTempText(single, dialAccent, active = true, fontSize = 72.sp)
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
    }
}

/**
 * Kinetic counter typography: whole degrees — the standing 1-degree rule
 * overrules the spec's %.1f format — sliding + fading + scaling by
 * direction, with tabular figures so digits never jitter.
 */
@Composable
private fun KineticTempText(value: Float, color: Color, active: Boolean, fontSize: TextUnit = 64.sp) {
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
            color = if (active) color else color.copy(alpha = 0.45f),
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

/**
 * Gradient heat ↔ cool bar with setpoint markers, a current-temperature
 * marker, and the live operating state. Single-setpoint thermostats get a
 * neutral track with one setpoint marker instead of a heat/cool gradient.
 */
@Composable
private fun HeatCoolRangeBar(entity: HaEntity, min: Float, max: Float) {
    val low = entity.attrDouble("target_temp_low")
    val high = entity.attrDouble("target_temp_high")
    val single = entity.attrDouble("temperature")
    val current = entity.attrDouble("current_temperature")
    val stateLabel = hvacActionLabel(entity.attrString("hvac_action")) ?: entity.stateText()
    val heatCool = low != null && high != null
    val minD = min.toDouble()
    val maxD = max.toDouble()
    Column(Modifier.fillMaxWidth()) {
        val barBackground = if (heatCool) {
            Modifier.background(Brush.horizontalGradient(listOf(HEAT_COLOR, COOL_COLOR)))
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(50)).then(barBackground),
        ) {
            if (low != null) RangeMarker(rangeFraction(low, minD, maxD), HEAT_COLOR, maxWidth)
            if (high != null) RangeMarker(rangeFraction(high, minD, maxD), COOL_COLOR, maxWidth)
            if (single != null) RangeMarker(rangeFraction(single, minD, maxD), COOL_COLOR, maxWidth)
            if (current != null) RangeMarker(rangeFraction(current, minD, maxD), Color.White, maxWidth)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (low != null) "Heat ${low.formatTemp()}"
                else if (single != null) "Set ${single.formatTemp()}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stateLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (high != null) "Cool ${high.formatTemp()}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A dot on the range bar at [fraction] of the bar width (positive padding only). */
@Composable
private fun RangeMarker(fraction: Float, color: Color, barWidth: Dp) {
    val x = (barWidth * fraction - 6.dp).coerceAtLeast(0.dp)
    Box(
        Modifier.padding(start = x, top = 8.dp).size(12.dp)
            .background(color, RoundedCornerShape(50)),
    )
}

@Composable
private fun ThermostatCard(label: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in options) {
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = {
                    Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() })
                },
            )
        }
    }
}

/**
 * A Google Home-style row pill: small label above the value, with a
 * chevron that flips when expanded. The pill only toggles [expanded];
 * the caller renders the expanded chips below it. The toggle is the
 * last parameter so call sites can pass it as a trailing lambda.
 */
@Composable
private fun GooglePill(
    label: String,
    value: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    // The chevron animates instead of snapping: it was an instant flip,
    // one of the rigid transitions in the audit.
    val chevronRotation by animateFloatAsState(
        if (expanded) 180f else 0f,
        animationSpec = springMorph(),
        label = "chevron",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onToggle,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).graphicsLayer {
                    rotationZ = chevronRotation
                },
            )
        }
    }
}

/** One label/value row for the Indoor/Outdoor card. */
@Composable
private fun TempRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThermostatCards(entity: HaEntity, vm: HavenViewModel, states: Map<String, HaEntity>) {
    // Expanded states live here, unconditionally, so the pills below can
    // appear or disappear with their backing state without breaking the
    // remember order.
    var modeExpanded by remember { mutableStateOf(false) }
    var comfortExpanded by remember { mutableStateOf(false) }
    var fanExpanded by remember { mutableStateOf(false) }
    val modes = entity.attrList("hvac_modes")?.mapNotNull { it as? String } ?: emptyList()
    if (modes.isNotEmpty()) {
        GooglePill("Mode", formatHvacMode(entity.state), modeExpanded) { modeExpanded = !modeExpanded }
        if (modeExpanded) {
            Spacer(Modifier.height(8.dp))
            ChipRow(modes, entity.state) { vm.setClimateHvacMode(entity.entityId, it) }
        }
        Spacer(Modifier.height(12.dp))
    }
    val preset = findPreset(states, entity)
    val presetModes = entity.attrList("preset_modes")?.mapNotNull { it as? String } ?: emptyList()
    if (preset != null || presetModes.isNotEmpty()) {
        // The old Schedule card's content lives here now: the current
        // preset as the pill value (HA exposes no next-schedule
        // transition, so nothing invented), its modes as the chips.
        GooglePill(
            "Comfort",
            preset?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Choose",
            comfortExpanded,
        ) { comfortExpanded = !comfortExpanded }
        if (comfortExpanded && presetModes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ChipRow(presetModes, entity.attrString("preset_mode")) {
                vm.callService(
                    "climate",
                    "set_preset_mode",
                    entity.entityId,
                    JSONObject().put("preset_mode", it),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val fanModes = entity.attrList("fan_modes")?.mapNotNull { it as? String } ?: emptyList()
    if (fanModes.isNotEmpty()) {
        val fanMode = entity.attrString("fan_mode")
        GooglePill(
            "Fan",
            fanMode?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Choose",
            fanExpanded,
        ) { fanExpanded = !fanExpanded }
        if (fanExpanded) {
            Spacer(Modifier.height(8.dp))
            ChipRow(fanModes, fanMode) { vm.setClimateFanMode(entity.entityId, it) }
        }
        Spacer(Modifier.height(12.dp))
    }
    val clearHold = findClearHoldButton(states, entity)
    if (clearHold != null) {
        ThermostatCard("Hold") {
            Text(
                "Manual changes hold until the next scheduled change.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.callService("button", "press", clearHold.entityId) }) {
                Text("Resume schedule")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val sensors = findLinkedSensors(states, entity)
    if (sensors.isNotEmpty()) {
        ThermostatCard("Sensors") {
            for (sensor in sensors) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sensor.friendlyName, fontSize = 14.sp)
                    Text(
                        sensor.stateText(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val current = entity.attrDouble("current_temperature")
    val humidity = entity.attrDouble("humidity")
    val outdoor = findOutdoorTemperature(states, entity)
    if (current != null || humidity != null || outdoor != null) {
        // Google Home's temperature rows: no card label, just the rows —
        // and the Outdoor row only when a real source backs it.
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                if (current != null) {
                    TempRow("Indoor temperature", current.formatTemp())
                }
                if (outdoor != null) {
                    if (current != null) Spacer(Modifier.height(8.dp))
                    TempRow("Outdoor temperature", outdoor.formatTemp())
                }
                if (humidity != null) {
                    if (current != null || outdoor != null) Spacer(Modifier.height(8.dp))
                    TempRow("Humidity", "${humidity.toInt()}%")
                }
            }
        }
    }
}
