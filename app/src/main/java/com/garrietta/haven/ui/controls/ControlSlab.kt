package com.garrietta.haven.ui.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.powerWatts
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.lightColorTempTint
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.settleSpring
import org.json.JSONObject
import kotlin.math.roundToInt

/*
 * A device pop-up leads with one large slab: the device's main control,
 * drawn as a level that fills from the bottom in the state's family
 * color. Tap it to switch the device; where the device has a level
 * (a lamp's brightness, a fan's speed) drag up or down anywhere on it to
 * set the level. It replaces a small toggle, a row of Turn on / Turn off
 * / Toggle buttons and a thin slider, several controls for one thing.
 */

/** The dimmest a drag can set. Zero would be "off", which is the tap's job. */
private const val MIN_PERCENT = 1

/** The range a light's warm-to-cool track covers when the light does not say. */
internal const val DEFAULT_WARMEST_KELVIN = 2000
internal const val DEFAULT_COOLEST_KELVIN = 6500

/**
 * The level a vertical drag lands on: [startPercent] plus the share of
 * the slab's [heightPx] the finger has moved up ([dragPx] is negative
 * going up), held between [minPercent] and 100.
 */
internal fun levelAfterDrag(startPercent: Float, dragPx: Float, heightPx: Float, minPercent: Int = MIN_PERCENT): Float {
    if (heightPx <= 0f) return startPercent.coerceIn(minPercent.toFloat(), 100f)
    return (startPercent - dragPx / heightPx * 100f).coerceIn(minPercent.toFloat(), 100f)
}

/** Home Assistant's 0..255 brightness as a whole percentage. */
internal fun brightnessPercent(brightness: Int?): Int =
    ((brightness ?: 255).coerceIn(0, 255) * 100f / 255f).roundToInt()

/** The kelvin at [fraction] of the way from [warmest] to [coolest], rounded to 50K as Home Assistant's own picker does. */
internal fun kelvinAt(fraction: Float, warmest: Int, coolest: Int): Int {
    if (coolest <= warmest) return warmest
    val raw = warmest + fraction.coerceIn(0f, 1f) * (coolest - warmest)
    return ((raw / 50f).roundToInt() * 50).coerceIn(warmest, coolest)
}

/** Where [kelvin] sits between [warmest] and [coolest], 0..1; the middle when unknown. */
internal fun kelvinFraction(kelvin: Int?, warmest: Int, coolest: Int): Float {
    if (kelvin == null || coolest <= warmest) return 0.5f
    return ((kelvin - warmest).toFloat() / (coolest - warmest)).coerceIn(0f, 1f)
}

/** A watt reading as the slab's caption: whole watts, or one decimal under ten. */
internal fun wattsCaption(watts: Double): String =
    if (watts < 10) "${(watts * 10).roundToInt() / 10.0} W in use" else "${watts.roundToInt()} W in use"

/**
 * The slab itself. [percent] is the level shown at rest (0..100), [active]
 * whether the track is lit; [family] colors the fill and a lit track,
 * [restFamily] an unlit one. [onLevel] makes the slab draggable: the level
 * follows the finger and is committed once, on release.
 */
@Composable
fun ControlSlab(
    percent: Int,
    active: Boolean,
    family: StateFamily,
    label: String,
    caption: String,
    icon: ImageVector,
    tapLabel: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    restFamily: StateFamily = StateFamily.Quiet,
    levelCaption: String = caption,
    onLevel: ((Int) -> Unit)? = null,
) {
    // While a finger is on the slab it shows the level being set, not
    // the last one Home Assistant reported, so the fill follows the finger.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: percent.toFloat()
    val fill by animateFloatAsState(shown / 100f, settleSpring(), label = "slab fill")

    val isDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val lit = family.colors(isDark)
    val rest = restFamily.colors(isDark)
    val litTrack = active || dragging != null
    val track by animateColorAsState(if (litTrack) lit.container else rest.container, label = "slab track")
    val trackInk = if (litTrack) lit.onContainer else rest.onContainer
    val haptics = LocalHapticFeedback.current
    val text = dragging?.let { "${it.roundToInt()}%" } ?: label
    val under = if (dragging != null) levelCaption else caption

    Box(
        modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(track)
            .pointerInput(onTap) {
                detectTapGestures {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onTap()
                }
            }
            .then(
                if (onLevel == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(percent, onLevel) {
                        var start = 0f
                        var moved = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                start = percent.toFloat().coerceAtLeast(MIN_PERCENT.toFloat())
                                moved = 0f
                                dragging = start
                            },
                            onDragEnd = {
                                dragging?.let { onLevel(it.roundToInt()) }
                                dragging = null
                            },
                            onDragCancel = { dragging = null },
                        ) { change, dy ->
                            change.consume()
                            moved += dy
                            val next = levelAfterDrag(start, moved, size.height.toFloat())
                            // A tick every tenth, so the level can be felt as well as seen.
                            if (next.roundToInt() / 10 != (dragging ?: next).roundToInt() / 10) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            }
                            dragging = next
                        }
                    }
                },
            )
            .semantics {
                role = if (onLevel != null) Role.Button else Role.Switch
                stateDescription = text
                onClick(label = tapLabel) {
                    onTap()
                    true
                }
                if (onLevel != null) {
                    setProgress(label = levelCaption) { value ->
                        onLevel(value.roundToInt().coerceIn(MIN_PERCENT, 100))
                        true
                    }
                }
            },
    ) {
        // The label on the unlit part, in the track's ink...
        SlabLabel(text, under, icon, trackInk)
        // ...and the lit part over it, with the same label in the fill's
        // ink, clipped to the fill: the text reads wherever the edge is.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    val top = size.height * (1f - fill.coerceIn(0f, 1f))
                    clipRect(top = top) { this@drawWithContent.drawContent() }
                }
                .background(lit.accent),
        ) {
            SlabLabel(text, under, icon, lit.onAccent)
        }
    }
}

@Composable
private fun BoxScope.SlabLabel(text: String, caption: String, icon: ImageVector, ink: Color) {
    Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
        Text(text, style = MaterialTheme.typography.displayMedium, color = ink)
        Text(caption, style = MaterialTheme.typography.titleSmall, color = ink)
    }
    Icon(
        icon,
        contentDescription = null,
        tint = ink,
        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(32.dp),
    )
}

/** A lamp: the fill is the brightness, in the lamp's yellow. */
@Composable
fun BrightnessSlab(entity: HaEntity, vm: HavenViewModel, modifier: Modifier = Modifier) {
    val isOn = entity.state == "on"
    val modes = entity.attrList("supported_color_modes")?.mapNotNull { it as? String } ?: emptyList()
    val dimmable = modes.any { it != "onoff" } || entity.attributes.containsKey("brightness")
    val reported = brightnessPercent(entity.attrInt("brightness")).coerceAtLeast(MIN_PERCENT)
    ControlSlab(
        percent = when {
            !isOn -> 0
            dimmable -> reported
            else -> 100
        },
        active = isOn,
        family = StateFamily.Light,
        label = when {
            !isOn -> "Off"
            dimmable -> "$reported%"
            else -> "On"
        },
        caption = when {
            dimmable && isOn -> "Brightness"
            isOn -> "Tap to switch off"
            else -> "Tap to switch on"
        },
        levelCaption = "Brightness",
        icon = Icons.Filled.Lightbulb,
        tapLabel = if (isOn) "Turn off" else "Turn on",
        onTap = { vm.toggle(entity.entityId) },
        modifier = modifier,
        onLevel = if (dimmable) { level -> vm.setLightBrightness(entity.entityId, level) } else null,
    )
}

/** A switch or helper: full when on. A plug with a meter says what it draws. */
@Composable
fun PowerSlab(entity: HaEntity, vm: HavenViewModel, modifier: Modifier = Modifier) {
    val isOn = entity.state == "on"
    val watts = powerWatts(entity)
    ControlSlab(
        percent = if (isOn) 100 else 0,
        active = isOn,
        family = if (watts != null) StateFamily.Power else StateFamily.On,
        label = if (isOn) "On" else "Off",
        caption = when {
            watts != null && isOn -> wattsCaption(watts)
            isOn -> "Tap to switch off"
            else -> "Tap to switch on"
        },
        icon = domainIcon(entity),
        tapLabel = if (isOn) "Turn off" else "Turn on",
        onTap = { vm.toggle(entity.entityId) },
        modifier = modifier,
    )
}

/** A fan: the fill is its speed, where it has one. */
@Composable
fun FanSlab(entity: HaEntity, vm: HavenViewModel, modifier: Modifier = Modifier) {
    val isOn = entity.state == "on"
    val speed = entity.attrInt("percentage")
    val hasSpeed = entity.attributes.containsKey("percentage")
    ControlSlab(
        percent = when {
            !isOn -> 0
            speed != null -> speed.coerceIn(MIN_PERCENT, 100)
            else -> 100
        },
        active = isOn,
        family = StateFamily.On,
        label = when {
            !isOn -> "Off"
            speed != null -> "$speed%"
            else -> "On"
        },
        caption = if (hasSpeed && isOn) "Speed" else if (isOn) "Tap to switch off" else "Tap to switch on",
        levelCaption = "Speed",
        icon = domainIcon(entity),
        tapLabel = if (isOn) "Turn off" else "Turn on",
        onTap = { vm.toggle(entity.entityId) },
        modifier = modifier,
        onLevel = if (hasSpeed) {
            { level -> vm.callService("fan", "set_percentage", entity.entityId, JSONObject().put("percentage", level)) }
        } else null,
    )
}

/**
 * A lock: full and green when locked, empty on the alert track when not.
 * Locking is one tap; unlocking asks first, as it always has.
 */
@Composable
fun LockSlab(entity: HaEntity, vm: HavenViewModel, modifier: Modifier = Modifier) {
    var confirmUnlock by remember { mutableStateOf(false) }
    val locked = entity.state == "locked" || entity.state == "locking"
    ControlSlab(
        percent = if (locked) 100 else 0,
        active = locked,
        family = StateFamily.Secure,
        restFamily = StateFamily.Alert,
        label = entity.stateText(),
        caption = if (locked) "Tap to unlock" else "Tap to lock",
        icon = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
        tapLabel = if (locked) "Unlock" else "Lock",
        onTap = {
            if (locked) confirmUnlock = true else vm.callService("lock", "lock", entity.entityId)
        },
        modifier = modifier,
    )
    if (confirmUnlock) {
        AlertDialog(
            onDismissRequest = { confirmUnlock = false },
            title = { Text("Unlock ${entity.friendlyName}?") },
            text = { Text("This removes the lock. Make sure that's what you want.") },
            confirmButton = {
                DialogActionButtons(
                    onCancel = { confirmUnlock = false },
                    onConfirm = {
                        confirmUnlock = false
                        vm.callService("lock", "unlock", entity.entityId)
                    },
                    confirmLabel = "Unlock",
                )
            },
        )
    }
}

/**
 * Warm to cool as a track painted in the light it gives: amber at one
 * end, cool white at the other, with a handle where the lamp sits. Tap
 * a point or drag the handle; the color is sent once, on release.
 */
@Composable
fun ColorTempTrack(entity: HaEntity, vm: HavenViewModel, modifier: Modifier = Modifier) {
    val warmest = entity.attrInt("min_color_temp_kelvin") ?: DEFAULT_WARMEST_KELVIN
    val coolest = entity.attrInt("max_color_temp_kelvin") ?: DEFAULT_COOLEST_KELVIN
    val kelvin = entity.attrInt("color_temp_kelvin")
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: kelvinFraction(kelvin, warmest, coolest)
    val shownKelvin = dragging?.let { kelvinAt(it, warmest, coolest) } ?: kelvin
    val handleAt by animateFloatAsState(fraction, settleSpring(), label = "temp handle")
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val mid = (warmest + coolest) / 2

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Warm to cool", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(shownKelvin?.let { "${it}K" } ?: "", style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(lightColorTempTint(warmest), lightColorTempTint(mid), lightColorTempTint(coolest)),
                    ),
                )
                .pointerInput(warmest, coolest) {
                    detectTapGestures { at ->
                        val picked = at.x / size.width
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        vm.setLightColorTemp(entity.entityId, kelvinAt(picked, warmest, coolest))
                    }
                }
                .pointerInput(warmest, coolest, kelvin) {
                    detectHorizontalDragGestures(
                        onDragStart = { at -> dragging = (at.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragging?.let { vm.setLightColorTemp(entity.entityId, kelvinAt(it, warmest, coolest)) }
                            dragging = null
                        },
                        onDragCancel = { dragging = null },
                    ) { change, dx ->
                        change.consume()
                        dragging = ((dragging ?: 0.5f) + dx / size.width).coerceIn(0f, 1f)
                    }
                }
                .semantics {
                    stateDescription = shownKelvin?.let { "$it kelvin" } ?: "Unknown"
                    setProgress(label = "Set color temperature") { value ->
                        vm.setLightColorTemp(entity.entityId, value.roundToInt().coerceIn(warmest, coolest))
                        true
                    }
                },
        ) {
            val handleWidth = 8.dp
            val inset = 12.dp
            val travel = maxWidth - inset * 2 - handleWidth
            // The handle is the page's ink on a ring of the page, so it
            // reads on the amber end and on the white end alike.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = inset + travel * handleAt)
                    .width(handleWidth)
                    .height(40.dp)
                    .background(scheme.surface, CircleShape)
                    .padding(2.dp)
                    .background(scheme.onSurface, CircleShape),
            )
        }
    }
}
