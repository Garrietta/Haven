package com.garrietta.haven.ui.controls
import com.garrietta.haven.ui.components.CARD_ICON_KEYS
import com.garrietta.haven.ui.components.cardIconForKey
import com.garrietta.haven.ui.components.formatCondition
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.reportCardShape
import com.garrietta.haven.data.customIconKeyFor
import com.garrietta.haven.ui.components.CoolBlue
import com.garrietta.haven.ui.components.HeatOrange

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.PillToggle
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.controls.GarageDoorVisual
import com.garrietta.haven.ui.screens.rememberCardCustom
import com.garrietta.haven.ui.screens.resolveThermostatDisplayRange
import com.garrietta.haven.ui.components.cardIconForKey
import com.garrietta.haven.ui.components.CARD_ICON_KEYS
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.data.humanizeOptionLabel
import com.garrietta.haven.data.customIconKeyFor
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.ui.components.SensorKind
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.sensorCardTint
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.components.lightColorTempTint
import com.garrietta.haven.ui.components.weatherConditionColorThemed
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.hideBodyWhenMorphSource
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.hideHeroWhenMorphSource
import com.garrietta.haven.ui.components.reportHeroBounds
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.MorphShape
import com.garrietta.haven.ui.theme.havenMotion
import com.garrietta.haven.ui.theme.pressBounce
import com.garrietta.haven.ui.theme.springSnappy
import com.garrietta.haven.ui.theme.polygonShape
import org.json.JSONObject
import com.garrietta.haven.ui.screens.SetpointSelector
import com.garrietta.haven.ui.screens.clampCoolSetpoint
import com.garrietta.haven.ui.screens.clampHeatSetpoint
import com.garrietta.haven.ui.screens.defaultSelector
import com.garrietta.haven.ui.screens.snapToDegree
import com.garrietta.haven.ui.screens.toggleSelector

fun Double.formatTemp(): String =
    if (this % 1.0 == 0.0) "${this.toInt()}°" else "%.1f°".format(this)

fun HaEntity.stateText(): String {
    val unit = attrString("unit_of_measurement")
    return when (domain) {
        "sensor" -> if (unit != null) "$state $unit" else state
        // Weather conditions go through the display formatter:
        // HA reports "partlycloudy" glued, which must read
        // "Partly Cloudy" everywhere (2026-10-30 verdict). Other
        // domains keep the plain underscore transform — their
        // strings ("heat_cool", "playing") are already readable.
        "weather" -> formatCondition(state)
        else -> state.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

/**
 * The single header state line shared by every surface that shows an
 * entity's name + state (2026-10-30 verdict round): cards, the
 * traveling header, and dialog headers all call this one function,
 * so the string can never disagree between the card, the traveler,
 * and the destination — the "redrawing of the text" the verdict
 * reports was partly three call sites formatting the same state
 * three ways. Weather appends its temperature (the temperature
 * attribute is the state's number); every other domain is exactly
 * [stateText]. Pure, unit-tested.
 */
fun HaEntity.dialogHeaderState(): String {
    if (domain != "weather") return stateText()
    val temp = attrDouble("temperature")?.formatTemp()
    return if (temp != null) "${formatCondition(state)} · $temp" else stateText()
}

/**
 * Sensor card titles (Round 4): HA names the Ecobee pair
 * "Thermostat Current Temperature" / "Thermostat Current
 * Humidity", which truncates on cards. The redundant prefix
 * strips, so titles read "Temperature" and "Humidity" in full.
 * Every other entity keeps its friendly name byte-identical.
 */
fun sensorDisplayName(entity: HaEntity): String {
    if (entity.domain != "sensor") return entity.friendlyName
    val prefix = "Thermostat Current "
    return if (entity.friendlyName.startsWith(prefix)) {
        entity.friendlyName.removePrefix(prefix)
    } else {
        entity.friendlyName
    }
}

/**
 * Feedback round 11, item 3: the ONE icon resolution every entity
 * badge uses — tile, sheet header, detail header, thermostat popout
 * header, control-card header, and traveler. A stored custom icon
 * key wins (unknown keys read as no override); otherwise the domain
 * icon. Before this round the traveler and several headers read
 * [domainIcon] directly, so an entity with a custom icon showed one
 * glyph on its tile and the domain glyph everywhere else (the eufy
 * AE C10 case: custom vacuum glyph on the tile, circular-arrows
 * domain glyph on the detail header and the traveler).
 */
fun entityIcon(iconKeys: Map<String, String>, entity: HaEntity): ImageVector =
    cardIconForKey(customIconKeyFor(iconKeys, entity.entityId, CARD_ICON_KEYS)) ?: domainIcon(entity)

fun domainIcon(entity: HaEntity): ImageVector = when (entity.domain) {
    "light" -> Icons.Filled.Lightbulb
    "switch", "input_boolean" -> Icons.Filled.Power
    "cover" -> Icons.Filled.Home
    "climate" -> Icons.Filled.Thermostat
    "vacuum" -> Icons.Filled.Autorenew
    "media_player" -> Icons.Filled.Tv
    "fan" -> Icons.Filled.Refresh
    "lock" -> if (entity.state == "locked") Icons.Filled.Lock else Icons.Filled.LockOpen
    "camera" -> Icons.Filled.Videocam
    "weather" -> Icons.Filled.WbSunny
    // Sensors never get the generic "i" badge where a domain
    // icon exists (Round 4): temperature gets the thermometer,
    // humidity the droplet, battery/power/light their icons.
    "sensor" -> when (sensorKind(entity.entityId, entity.attrString("device_class"), entity.friendlyName)) {
        SensorKind.TEMPERATURE -> Icons.Filled.Thermostat
        SensorKind.HUMIDITY -> Icons.Filled.WaterDrop
        SensorKind.BATTERY -> Icons.Filled.BatteryFull
        SensorKind.POWER -> Icons.Filled.Bolt
        SensorKind.LIGHT -> Icons.Filled.WbSunny
        SensorKind.OTHER -> Icons.Filled.Info
    }
    "binary_sensor" -> Icons.Filled.Visibility
    "scene", "script" -> Icons.Filled.PlayArrow
    "automation" -> Icons.Filled.Build
    "todo" -> Icons.Filled.List
    "alarm_control_panel" -> Icons.Filled.Warning
    "timer" -> Icons.Filled.Schedule
    "counter" -> Icons.Filled.Add
    "person", "device_tracker" -> Icons.Filled.Person
    else -> Icons.Filled.Info
}

/** A slider that only calls the service when the user lifts their finger. */
@Composable
/**
 * Slider with local drag state (the thumb follows the finger
 * immediately; the service call fires only on release) and an
 * optional live value label. [formatValue] renders the LOCAL drag
 * value above the slider, so the number tracks the finger — the
 * scannable value Nerwyn-style cards show — instead of waiting on
 * the server round-trip. Call sites that already label their value
 * above the slider (fan speed, input_number) pass nothing.
 */
fun ServiceSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onFinished: (Float) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    formatValue: ((Float) -> String)? = null,
) {
    var local by remember(value) { mutableStateOf(value) }
    Column(modifier) {
        if (formatValue != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(formatValue(local), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            valueRange = range,
            onValueChangeFinished = { onFinished(local) },
            colors = SliderDefaults.colors(activeTrackColor = accent, thumbColor = accent),
        )
    }
}

/**
 * Koda-informed pressable pill control (spec item 5 + the Koda
 * now-playing references: big pill-capsule transports with clear pressed
 * states, on warm tonal containers). A filled tonal pill with a reactive
 * press bounce — scale to 0.92 on the single pressBounce spring — and a
 * bounded ripple clipped to the pill. Stepper buttons and media
 * transports share it, so every control press feels like one set. Not a
 * Material Button: the pill shape and press scale are the expressive
 * language here. The caller sizes it via [modifier] (width) and [height].
 */
@Composable
fun PressablePillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    // Disabled pills (2026-11-01 round 1: the garage Close pill
    // while the door already reports closed) render muted and
    // take no taps; every existing caller defaults to enabled.
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    // Internal padding around the content. Zero by default (steppers
    // and media transports size themselves by width/height and need
    // none); text-label pills pass generous padding so the pill
    // reads as a button, not bare text (the garage Open/Close/Stop
    // controls, 2026-10-29 verdict video: "expand these buttons so
    // they're not so tightly spaced — there should be a nice padding
    // around the text").
    contentPadding: PaddingValues = PaddingValues(0.dp),
    // Clip shape (expressive-menus round, 2026-11-03): the full pill
    // by default, so every existing caller is byte-identical; media
    // transports and D-pads pass the squircle for the big central
    // button + flanking-button language of the reference player.
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(50),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        animationSpec = if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f) else pressBounce(),
        label = "pressPill",
    )
    Box(
        modifier
            .height(height)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(shape)
            .background(containerColor)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * The expressive stepper (spec item 5): a pill-capsule button, not a
 * basic rounded rectangle, with the press bounce above. Used wherever
 * steppers appear — the thermostat dial, the thermostat tile — and sized
 * by [height] (48dp on the page, 32dp on the tile).
 */
@Composable
fun ExpressiveStepper(
    symbol: String,
    onClick: () -> Unit,
    height: Dp = 48.dp,
    modifier: Modifier = Modifier,
    // Feedback round 5, item 4: the thermostat steppers carry the
    // tile preset accent when set; null keeps the scheme defaults
    // byte-identical (tile steppers pass nothing).
    containerColor: androidx.compose.ui.graphics.Color? = null,
    contentColor: androidx.compose.ui.graphics.Color? = null,
) {
    PressablePillButton(
        onClick = onClick,
        modifier = modifier.width(height * 1.5f),
        height = height,
        containerColor = containerColor ?: MaterialTheme.colorScheme.secondaryContainer,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            symbol,
            fontSize = if (height >= 48.dp) 22.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/**
 * The card shell: blob badge + name + state header, then the domain's own
 * control. Detail surfaces pass showHeader = false: the header row is a
 * dead control there (it would open the very sheet/screen already shown),
 * so it is hidden instead of rendered as a bar that opens nothing.
 *
 * [morphSource] reports this card's root bounds to the morph map and
 * hides the card while its morph is active (see MorphOverlay). Nested
 * cards (inside the sheet, where the panel is the surface) and cards
 * on detail pages (where the page overlay is the surface) must NOT be
 * morph sources: a card that both reports and renders during its own
 * morph reads as a doubled ghost, as in the 2026-10-29 evidence video.
 * Defaults to true exactly when the card is not nested.
 */
/**
 * Shape-tap actuation for binary entities: light/switch/input_boolean/
 * fan toggle, cover toggles open/closed, lock toggles locked/unlocked.
 * Null for every other domain (sensor, weather, camera, media,
 * climate, vacuum): those shapes have no inner target and taps fall
 * through to the card's background action (open page/pop-out).
 * Shared by tiles and card headers so the model is identical
 * everywhere: shape = actuate, background = open.
 */
fun shapeTapAction(vm: HavenViewModel, entity: HaEntity): (() -> Unit)? = when (entity.domain) {
    "light", "switch", "input_boolean", "fan" -> ({ vm.toggle(entity.entityId) })
    "cover" -> ({ vm.toggleCover(entity.entityId) })
    "lock" -> ({ vm.toggleLock(entity.entityId) })
    else -> null
}

@Composable
fun EntityControlCard(entity: HaEntity, vm: HavenViewModel, showHeader: Boolean = true, showForecast: Boolean = true, nested: Boolean = false, morphSource: Boolean = !nested, headerClickable: Boolean = true,
    // Feedback round 5, item 4: the page accent funnel. The assigned tile color (preset
    // container, or null) and badge pair (or null) thread from the detail page / sheet into
    // every domain control below, so one resolution themes the whole surface. Default null
    // keeps every existing caller (home tiles, boards, areas, search) byte-identical.
    // onDetail stays LAST: Browse/Dashboards pass it as a trailing lambda, and params
    // appended after a lambda parameter break those call sites (round-5 CI #112 lesson,
    // same class as the PillAction reorder).
    pageAccent: Color? = null, pageBadge: Pair<Color, Color>? = null,
    onDetail: (String) -> Unit) {
    val accent = accentFor(entity.entityId)
    // Snapshot-held visuals (2026-10-30 verdict round): when this
    // card is the active morph source, its badge, name, state, and
    // container tint resolve from the open-time snapshot, never the
    // live entity — the live state can move mid-flight (a setpoint
    // drag settling, hvac_action flipping), which read as a color
    // shift on open and a dark-then-muted flash on dismiss. Only
    // morph sources take the snapshot: destination cards sharing
    // the id (dialog-inner cards) stay live, and body controls and
    // actuation still use the live entity — only display visuals
    // are held. No snapshot (sheet without one, no morph) = live.
    val snap = if (morphSource) vm.travelSnapshots[entity.entityId] else null
    val display = snap ?: entity
    // Card-outline shape (2026-10-30 picker): a per-entity
    // override beats the global default; without either, the
    // token extraLarge corners are unchanged.
    val cardShape = LocalShapePrefs.current.cardShapeFor(entity.entityId, entity.domain)
        ?: generalCardShape(0)
    Card(
        shape = cardShape,
        // Hierarchy tokens: SurfaceContainer for root cards (Home, Boards,
        // Areas, detail pages), SurfaceContainerLow for nested cards
        // (inside the sheet, where the panel is already a container).
        // Weather and climate root cards carry their dynamic color at
        // low alpha — condition on the weather front card, mode on
        // climate cards — so state reads on the card face itself.
        colors = CardDefaults.cardColors(
            containerColor = if (nested) MaterialTheme.colorScheme.surfaceContainerLow
            else if (display.domain == "weather") lerp(
                MaterialTheme.colorScheme.surfaceContainer,
                weatherConditionColorThemed(display.state, isDarkBackdrop(MaterialTheme.colorScheme.background)),
                0.20f,
            )
            else if (display.domain == "climate") lerp(
                MaterialTheme.colorScheme.surfaceContainer,
                climateModeColor(display.state, display.attrString("hvac_action")),
                0.15f,
            )
            // Sensor cards carry their semantic tint at low alpha
            // (Round 4, same language as climate/weather cards):
            // coral for temperature, sky for humidity.
            else if (display.domain == "sensor") {
                val tint = sensorCardTint(sensorKind(display.entityId, display.attrString("device_class"), display.friendlyName))
                if (tint != null) lerp(MaterialTheme.colorScheme.surfaceContainer, tint, 0.15f)
                else MaterialTheme.colorScheme.surfaceContainer
            }
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth().then(if (morphSource) Modifier.reportCardBounds(entity.entityId).reportCardShape(entity.entityId, cardShape).hideWhenMorphSource(entity.entityId) else Modifier),
    ) {
        // Body hide (2026-10-29 verdict-fix round): the whole card
        // body vanishes from frame 0 while its morph is open, exactly
        // like the header hide above — on #67 only the header hid,
        // so the arc, humidity line, charts, and lamp heroes stayed
        // visible under the pop-up's duplicates. Gated on
        // morphSource like the root hide: nested/destination cards
        // sharing the entity id must never hide.
        Column(Modifier.padding(16.dp).then(if (morphSource) Modifier.hideBodyWhenMorphSource(entity.entityId) else Modifier)) {
            if (showHeader) {
                // headerClickable=false lets an outer gesture own the
                // card (the Home weather card's combinedClickable):
                // an inner clickable would consume the tap and swallow
                // the long-press, breaking tap → page / long-press →
                // sheet. The chevron stays: the card is still tappable.
                // Shape-tap = actuate: for binary entities the badge
                // is its own target (tap the shape, the entity flips)
                // while the name/state/chevron open the detail; without
                // a shape action the whole header is one detail target,
                // exactly as before.
                val shapeAction = if (headerClickable) shapeTapAction(vm, entity) else null
                // Climate cards route to the primary scalloped
                // dial panel (Round 4), never the sheet: tap or
                // long-press on the Ecobee card opens the pop-out.
                // Every other domain keeps the caller's detail
                // routing (sheet on Boards/Areas/Search).
                val detailAction: () -> Unit = {
                    if (entity.domain == "climate") vm.openClimatePopout(entity.entityId)
                    else onDetail(entity.entityId)
                }
                val badgeClick = when {
                    shapeAction != null -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = shapeAction,
                    )
                    // No ripple (same as the shape-action path
                    // above): a ripple on a morph-source header
                    // freezes mid-animation when the card hides for
                    // its dialog (2026-10-30 verdict).
                    headerClickable -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { detailAction() }
                    else -> Modifier
                }
                // No ripple, same frozen-ripple reason as above:
                // this row hides with the card when its dialog opens.
                val detailClick = if (headerClickable) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { detailAction() } else Modifier
                // Traveling header (2026-10-29 pop-up round): the
                // header row reports its root rect and hides the
                // moment its morph opens, so the overlay's single
                // traveling header — badge, name, state, Relay-style —
                // replaces it exactly at the same rect, never beside
                // it. Only morph sources report: nested/detail-inner
                // cards are destinations, never sources.
                // Round 6 badge-only hide: the ROW reports the
                // hero bounds, but only the BADGE hides while its
                // traveler flies (LocalHiddenHeroId) — the name and
                // state stay visible, so the card can never blank.
                val heroMods = if (morphSource) Modifier.reportHeroBounds(entity.entityId) else Modifier
                Row(
                    Modifier.fillMaxWidth().then(heroMods),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // State-driven badge: morphs with the entity's state.
                    // 56dp badge (up from 48dp) + 16dp gap to the name:
                    // the expressive shape background is larger than
                    // the text block beside it, so the morph pulse and
                    // rotation never touch the name (2026-10-29
                    // verdict: "the text overlaps [the] — this
                    // expressive shape background should be larger").
                    // Customization round, item 2: the card header
                    // honors the same overrides as Home cards.
                    val headerCustom = rememberCardCustom(vm)
                    val headerIcon = cardIconForKey(customIconKeyFor(headerCustom.iconKeys, entity.entityId, CARD_ICON_KEYS)) ?: domainIcon(display)
                    val headerTitle = customTitleFor(headerCustom.titles, entity.entityId, sensorDisplayName(display))
                    DomainBadge(display, headerIcon, accent, Modifier.size(56.dp).then(if (morphSource) Modifier.hideHeroWhenMorphSource(entity.entityId) else Modifier).then(badgeClick), badgeOverride = pageBadge)
                    Spacer(Modifier.width(16.dp))
                    Row(
                        Modifier.weight(1f).then(detailClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(headerTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(display.dialogHeaderState(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            when (entity.domain) {
                "light" -> LightControl(entity, vm)
                "switch" -> SwitchControl(entity, vm, pageAccent)
                "cover" -> CoverControl(entity, vm, pageAccent)
                "climate" -> ClimateControl(entity, vm, pageAccent)
                "vacuum" -> VacuumControl(entity, vm, pageAccent, pageBadge)
                "media_player" -> MediaControl(entity, vm, pageAccent, pageBadge)
                "fan" -> FanControl(entity, vm, pageAccent)
                "lock" -> LockControl(entity, vm, pageAccent)
                else -> MoreControl(entity, vm, showForecast = showForecast)
            }
        }
    }
}

@Composable
fun LightControl(entity: HaEntity, vm: HavenViewModel, showBulb: Boolean = true) {
    val isOn = entity.state == "on"
    // Bulb color follows color temperature (secondary -> tertiary blend
    // family, as a warm-amber to cool-white glyph tint); unknown kelvin
    // reads neutral instead of defaulting to amber.
    val accent = lightColorTempTint(entity.attrInt("color_temp_kelvin"))
    val brightness = entity.attrInt("brightness") ?: 255
    val pct = brightness * 100f / 255f
    val modes = entity.attrList("supported_color_modes")?.mapNotNull { it as? String } ?: emptyList()
    val kelvin = entity.attrInt("color_temp_kelvin")
    // The bulb used to snap between shapes; now it blooms on the house spring.
    val reduceMotion = LocalReduceMotion.current
    // Snappy, not bouncy, for the on/off morph: the bouncy house spring
    // settles over ~1s with overshoot, so the bulb shape lagged a full
    // second behind the header text that flips discretely with the state
    // — the "header and bulb disagree" window. The tight spring lands
    // with the text; the bouncy spring stays everywhere else.
    val bulbSpec: FiniteAnimationSpec<Float> =
        if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f) else springSnappy()
    val bulbProgress by animateFloatAsState(
        if (isOn) 1f else 0f,
        animationSpec = bulbSpec,
        label = "bulb",
    )
    val bulbScale by animateFloatAsState(
        if (isOn) 1f else 0.9f,
        animationSpec = bulbSpec,
        label = "bulbScale",
    )
    // The bulb row is the card/sheet toggle affordance. The light
    // detail panel hides it (showBulb = false, 2026-11-01 round 1):
    // the panel header already carries the bulb badge, so the bulb
    // row would duplicate that icon — the panel's toggle is the
    // large vertical pill instead. Sliders below are unaffected.
    if (showBulb) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(76.dp).graphicsLayer(scaleX = bulbScale, scaleY = bulbScale)
                .clickable { vm.toggle(entity.entityId) },
            contentAlignment = Alignment.Center,
        ) {
            MorphShape(
                from = ExpressiveKind.BUN,
                to = ExpressiveKind.SUNNY,
                progress = bulbProgress,
                color = if (isOn) accent else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(76.dp),
            )
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = if (isOn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (isOn) "On" else "Off", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (isOn) {
                // Verdict 2026-10-30: this subtitle washed out in
                // light mode — full onSurface, not the variant role.
                Text("Tap the bulb to switch off", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    }
    if (modes.contains("brightness") || entity.attributes.containsKey("brightness")) {
        Spacer(Modifier.height(8.dp))
        Text("Brightness", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ServiceSlider(pct, 0f..100f, { vm.setLightBrightness(entity.entityId, it.toInt()) }, accent, formatValue = { "${it.toInt()}%" })
    }
    if (modes.contains("color_temp") || kelvin != null) {
        Text("Warm to cool", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ServiceSlider(
            (kelvin ?: 3500).toFloat(), 2000f..6500f,
            { vm.setLightColorTemp(entity.entityId, it.toInt()) }, lightColorTempTint(kelvin),
            formatValue = { "${it.toInt()}K" },
        )
    }
    if (modes.any { it == "hs" || it == "rgb" || it == "xy" }) {
        Spacer(Modifier.height(4.dp))
        val hues = listOf(0f, 30f, 60f, 120f, 180f, 220f, 280f, 320f)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (hue in hues) {
                val dot = Color.hsv(hue, 0.85f, 0.95f)
                Box(
                    Modifier.size(30.dp)
                        .background(dot, polygonShape(ExpressiveKind.FLOWER))
                        .clickable { vm.setLightHsColor(entity.entityId, hue, 100f) },
                )
            }
        }
    }
}

@Composable
fun SwitchControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Feedback round 5, item 4: the toggle track is the assigned tile color when a preset
        // is set (same pastel-container language as the detail big toggle), else the hashed
        // identity accent as before. Unchecked stays surfaceVariant inside PillToggle.
        PillToggle(entity.state == "on", { vm.toggle(entity.entityId) }, pageAccent ?: accentFor(entity.entityId))
        Spacer(Modifier.width(12.dp))
        Text(if (entity.state == "on") "On" else "Off", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CoverControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null) {
    val position = entity.attrInt("current_position") ?: if (entity.state == "open") 100 else 0
    val accent = accentFor(entity.entityId)
    // Feedback round 5, item 4: the sheet door fill, pills, and slider follow the assigned
    // tile color when a preset is set (the video-3 garage sheet: sky blue door + pills +
    // slider), else the hashed identity accent as before. Disabled pills keep their
    // surfaceContainerHighest muted treatment — the invalid-direction law is untouched.
    val pillContainer = pageAccent ?: MaterialTheme.colorScheme.secondaryContainer
    val pillContent = if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
    val isGarage = entity.attrString("device_class") == "garage"
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Door visual: the door slides up as the cover opens, on the house spring.
        val reduceMotion = LocalReduceMotion.current
        val doorFractionTarget = (100 - position) / 100f
        val animatedDoorFraction by animateFloatAsState(
            doorFractionTarget.coerceAtLeast(0.08f),
            animationSpec = havenMotion(reduceMotion),
            label = "door",
        )
        Box(
            Modifier.size(width = 84.dp, height = 110.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large),
        ) {
            Box(
                Modifier.fillMaxWidth()
                    .height(110.dp * animatedDoorFraction)
                    .background(pageAccent ?: accent, MaterialTheme.shapes.large)
                    .align(Alignment.TopCenter),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isGarage) "Garage door · ${entity.stateText()}" else entity.stateText(),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text("$position% open", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            // Sheet-panel path of the GarageDoorMenu rule: the
            // round-1 disable landed only on the detail-page path,
            // so this sheet control left Close fully enabled while
            // closed. Invalid-direction actions are disabled AND
            // dimmed to 0.38 alpha (muted colors alone read as
            // identical greys on this dark dynamic palette). The
            // state/position test uses the REPORTED position (not
            // the display fallback, which defaults unknown to 0 and
            // would disable Close mid-travel).
            val reportedPosition = entity.attrInt("current_position")
            val isClosed = entity.state == "closed" || reportedPosition == 0
            val isOpen = entity.state == "open" || reportedPosition == 100
            // Customization round, item 5: same 56dp pills as
            // the panel, same invalid-direction law.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PressablePillButton(
                    onClick = { vm.callService("cover", "open_cover", entity.entityId) },
                    modifier = Modifier.alpha(if (isOpen) 0.38f else 1f),
                    height = 56.dp,
                    enabled = !isOpen,
                    containerColor = if (isOpen) MaterialTheme.colorScheme.surfaceContainerHighest else pillContainer,
                    contentColor = if (isOpen) MaterialTheme.colorScheme.onSurfaceVariant else pillContent,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { Text("Open", fontWeight = FontWeight.SemiBold) }
                PressablePillButton(
                    onClick = { vm.callService("cover", "close_cover", entity.entityId) },
                    modifier = Modifier.alpha(if (isClosed) 0.38f else 1f),
                    height = 56.dp,
                    enabled = !isClosed,
                    containerColor = if (isClosed) MaterialTheme.colorScheme.surfaceContainerHighest else pillContainer,
                    contentColor = if (isClosed) MaterialTheme.colorScheme.onSurfaceVariant else pillContent,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { Text("Close", fontWeight = FontWeight.SemiBold) }
                PressablePillButton(
                    onClick = { vm.callService("cover", "stop_cover", entity.entityId) },
                    height = 56.dp,
                    containerColor = pageAccent ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { Text("Stop", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    ServiceSlider(position.toFloat(), 0f..100f, { vm.setCoverPosition(entity.entityId, it.toInt()) }, pageAccent ?: accent, formatValue = { "${it.toInt()}%" })
}


@Composable
fun ClimateControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null) {
    val accent = accentFor(entity.entityId)
    val current = entity.attrDouble("current_temperature")
    // Heat/cool thermostats (like the Ecobee) report target_temp_high/low
    // instead of a single temperature. The sheet dial edits exactly one
    // setpoint — the active selector, toggled by tapping inside the dial
    // face — and shows it large with its name in the subtitle, so the
    // sheet never has to fall back to a hardcoded value or a midpoint.
    val high = entity.attrDouble("target_temp_high")
    val low = entity.attrDouble("target_temp_low")
    val heatCool = high != null && low != null
    val min = entity.attrDouble("min_temp")?.toFloat() ?: 10f
    val max = entity.attrDouble("max_temp")?.toFloat() ?: 30f
    // Round 6: the sheet dial uses the SAME resolved display
    // window as the popout — the decade formula around the current
    // setpoints (66/67° → 50–80°), or the Settings override when
    // both override values are set (see
    // resolveThermostatDisplayRange) — NOT the full HA min..max.
    // State coercion stays in HA range exactly like the popout
    // (out-of-window values pin at the ends); only the drawn bar
    // + slider window change. Display-range only: the override
    // never touches live setpoints.
    val displaySetpoints = if (heatCool) {
        listOfNotNull(low?.toFloat(), high?.toFloat())
    } else {
        listOfNotNull(entity.attrDouble("temperature")?.toFloat())
    }
    val displayRange = resolveThermostatDisplayRange(
        min, max, displaySetpoints,
        vm.settingsStore.thermostatDisplayMin,
        vm.settingsStore.thermostatDisplayMax,
    )
    var heat by remember(low) { mutableStateOf(low?.toFloat()?.coerceIn(min, max) ?: min) }
    var cool by remember(high) { mutableStateOf(high?.toFloat()?.coerceIn(min, max) ?: max) }
    var single by remember(entity.attrDouble("temperature")) {
        mutableStateOf(entity.attrDouble("temperature")?.toFloat()?.coerceIn(min, max) ?: min)
    }
    var selector by remember(entity.entityId, entity.state) { mutableStateOf(defaultSelector(entity.state)) }
    val active: Float = if (heatCool) {
        if (selector == SetpointSelector.HEATING) heat else cool
    } else {
        single
    }
    val dialAccent: Color = if (heatCool) {
        if (selector == SetpointSelector.HEATING) HeatOrange else CoolBlue
    } else {
        accent
    }
    val setActive: (Float) -> Unit = {
        if (heatCool) {
            if (selector == SetpointSelector.HEATING) heat = it else cool = it
        } else {
            single = it
        }
    }
    val modes = entity.attrList("hvac_modes")?.mapNotNull { it as? String } ?: emptyList()
    val presets = entity.attrList("preset_modes")?.mapNotNull { it as? String } ?: emptyList()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Dial(
            value = active,
            range = displayRange,
            onValueChange = setActive,
            onFinished = { applyClimateSetpoint(vm, entity, selector, it.toDouble()) },
            centerText = active.toDouble().formatTemp(),
            centerSub = if (heatCool) {
                val name = if (selector == SetpointSelector.HEATING) "Heating setpoint" else "Cooling setpoint"
                if (current != null) "$name · Now ${current.formatTemp()}" else name
            } else if (current != null) {
                "Now ${current.formatTemp()}"
            } else {
                entity.stateText()
            },
            accent = dialAccent,
            onInsideTap = if (heatCool) {
                { selector = toggleSelector(selector) }
            } else {
                null
            },
            snapToWhole = true,
            showRangeEnds = true,
        )
        // The slider moves the same active setpoint and snaps to whole
        // degrees on release, so every path changes temperature in 1°
        // intervals. Its window matches the dial's dynamic window.
        // Feedback round 5, item 4: the sheet climate slider follows the assigned tile color;
        // the dial itself keeps its heat/cool mode colors (they carry mode meaning, item 6
        // reworks only the dial's label/stepper layout around the kept scallop).
        ServiceSlider(active, displayRange, { applyClimateSetpoint(vm, entity, selector, snapToDegree(it.toDouble())) }, pageAccent ?: accent, formatValue = { "${snapToDegree(it.toDouble()).toInt()}°" })
        if (modes.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (mode in modes) {
                    FilterChip(
                        selected = entity.state == mode,
                        onClick = { vm.setClimateHvacMode(entity.entityId, mode) },
                        label = { Text(mode.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }
        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (preset in presets) {
                    FilterChip(
                        selected = entity.attrString("preset_mode") == preset,
                        onClick = {
                            vm.callService("climate", "set_preset_mode", entity.entityId, JSONObject().put("preset_mode", preset))
                        },
                        label = { Text(preset.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }
    }
}

/**
 * Applies a thermostat setpoint in whole degrees. Heat/cool thermostats
 * take a high/low pair, so only the ACTIVE selector's value moves — the
 * other setpoint is sent back unchanged, clamped so heating can never
 * pass cooling. Single-setpoint thermostats take the value directly.
 * This replaces the old midpoint move (both setpoints shifting to
 * preserve their gap), which the selector model supersedes.
 */
fun applyClimateSetpoint(vm: HavenViewModel, entity: HaEntity, selector: SetpointSelector, value: Double) {
    val high = entity.attrDouble("target_temp_high")
    val low = entity.attrDouble("target_temp_low")
    val min = entity.attrDouble("min_temp")
    val max = entity.attrDouble("max_temp")
    if (high != null && low != null) {
        if (selector == SetpointSelector.HEATING) {
            vm.setClimateTemperatureRange(entity.entityId, clampHeatSetpoint(value, min, high), high)
        } else {
            vm.setClimateTemperatureRange(entity.entityId, low, clampCoolSetpoint(value, low, max))
        }
    } else {
        vm.setClimateTemperature(entity.entityId, snapToDegree(value))
    }
}

@Composable
/**
 * Vacuum audit (customization round, item 6): the generic control
 * was the weakest vacuum surface — small pre-expressive buttons
 * and RAW fan-speed strings ("boost_iq"). Now: 56dp expressive
 * pills for Start/Pause/Dock/Locate (same service calls), a
 * humanized status line ("Docked" reads as Docked, not "docked"),
 * battery with the same line, and humanized fan speeds
 * ("Boost IQ") via the Round 3 humanizer. No capability is
 * invented: only services HA's vacuum domain exposes are offered
 * (start/pause/return_to_base/locate/set_fan_speed), and an
 * error/unknown state shows its real state text, never a guess.
 * The rich Round 3 panel for vacuum.t2292 is untouched; this
 * generic control is what every OTHER vacuum entity (e.g. the
 * HomeKit bridge vacuum_homekit of the same Eufy) gets.
 */
fun VacuumControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null, pageBadge: Pair<Color, Color>? = null) {
    val battery = entity.attrInt("battery_level")
    val fanSpeed = entity.attrString("fan_speed")
    val speeds = entity.attrList("fan_speed_list")?.mapNotNull { it as? String } ?: emptyList()
    val humanStatus = humanizeOptionLabel(entity.state)
    Text(
        if (battery != null) "$humanStatus · $battery%" else "Status: $humanStatus",
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
    )
    if (battery != null) {
        Spacer(Modifier.height(8.dp))
        // Feedback round 5, item 4: the battery meter follows the badge pair (dark ink in
        // light theme, bright tint in dark — the same pair the badge renders), so the meter
        // stays visible on the page background in both themes. No preset → default primary.
        val batteryColor = pageBadge?.let { if (isDarkBackdrop(MaterialTheme.colorScheme.background)) it.second else it.first }
        if (batteryColor != null) {
            LinearProgressIndicator(progress = { (battery / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = batteryColor)
        } else {
            LinearProgressIndicator(progress = { (battery / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(12.dp))
    // Feedback round 5, item 4: every action pill carries the assigned tile color (the
    // video-3 vacuum sheet: sage Start/Dock/Locate), with theme-aware onSurface text. The
    // video-3 sheet is the headline case — a DOCKED vacuum — so this reads pageAccent, not
    // the state-gated badge pair (see detailPageAccent's gating note).
    val vacuumPillContainer = pageAccent ?: MaterialTheme.colorScheme.secondaryContainer
    val vacuumPillContent = if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entity.state == "cleaning") {
            PressablePillButton(onClick = { vm.callService("vacuum", "pause", entity.entityId) }, height = 56.dp,
                containerColor = vacuumPillContainer, contentColor = vacuumPillContent,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) { Text("Pause", fontWeight = FontWeight.SemiBold) }
        } else {
            PressablePillButton(onClick = { vm.callService("vacuum", "start", entity.entityId) }, height = 56.dp,
                containerColor = vacuumPillContainer, contentColor = vacuumPillContent,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) { Text("Start", fontWeight = FontWeight.SemiBold) }
        }
        PressablePillButton(onClick = { vm.callService("vacuum", "return_to_base", entity.entityId) }, height = 56.dp,
            containerColor = vacuumPillContainer, contentColor = vacuumPillContent,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) { Text("Dock", fontWeight = FontWeight.SemiBold) }
        PressablePillButton(onClick = { vm.callService("vacuum", "locate", entity.entityId) }, height = 56.dp,
            containerColor = vacuumPillContainer, contentColor = vacuumPillContent,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) { Text("Locate", fontWeight = FontWeight.SemiBold) }
    }
    if (speeds.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (speed in speeds) {
                FilterChip(
                    selected = fanSpeed == speed,
                    onClick = {
                        vm.callService("vacuum", "set_fan_speed", entity.entityId, JSONObject().put("fan_speed", speed))
                    },
                    label = { Text(humanizeOptionLabel(speed)) },
                    // Feedback round 5, item 4: the selected speed chip carries the tile
                    // color; unselected chips stay scheme (their signal is selected-vs-not).
                    colors = if (pageAccent != null) FilterChipDefaults.filterChipColors(selectedContainerColor = pageAccent) else FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}

@Composable
fun MediaControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null, pageBadge: Pair<Color, Color>? = null) {
    val title = entity.attrString("media_title") ?: "Nothing playing"
    val artist = entity.attrString("media_artist")
    val volume = entity.attrDouble("volume_level") ?: 0.5
    val playing = entity.state == "playing"
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        if (artist != null) Text(artist, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        // Expressive-menus round: the reference-player transport
        // (big central squircle + flanking squircles) replaces the
        // 56dp pill row; service calls are identical. Feedback
        // round 5, item 4: the central squircle carries the tile
        // color when a preset is set; flanking squircles stay
        // scheme (their signal is flanking-vs-central, not color).
        MediaTransportRow(
            playing = playing,
            onPrevious = { vm.callService("media_player", "media_previous_track", entity.entityId) },
            onPlayPause = { vm.callService("media_player", "media_play_pause", entity.entityId) },
            onNext = { vm.callService("media_player", "media_next_track", entity.entityId) },
            pageAccent = pageAccent,
        )
        Spacer(Modifier.height(12.dp))
        val muted = entity.attrBool("is_volume_muted") ?: false
        MediaVolumeRow(
            muted = muted,
            onToggleMute = {
                vm.callService(
                    "media_player", "volume_mute", entity.entityId,
                    JSONObject().put("is_volume_muted", !muted),
                )
            },
        ) {
            // Customization round, item 8: media badge fill
            // accent (very light purple #DAB9FF in dark mode),
            // never the deep-ink glyph or the hashed identity accent.
            // Feedback round 5, item 4: a preset tile color wins over
            // the matrix badge fill (its dark ink, so the slider stays
            // visible on the page background in both themes).
            val sheetMediaAccent = pageBadge?.first
                ?: domainCardColors(entity, isDarkBackdrop(MaterialTheme.colorScheme.background))?.badgeBg
                ?: accentFor(entity.entityId)
            ServiceSlider(
                (volume * 100).toFloat(), 0f..100f,
                { vm.callService("media_player", "volume_set", entity.entityId, JSONObject().put("volume_level", it / 100f)) },
                sheetMediaAccent,
                formatValue = { "${it.toInt()}%" },
            )
        }
        val sources = entity.attrList("source_list")?.mapNotNull { it as? String } ?: emptyList()
        if (sources.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val currentSource = entity.attrString("source") ?: "Source"
            Spacer(Modifier.height(12.dp))
            Box {
                // The source is the real output affordance (HA has no
                // queue): a large 56dp pill trigger, not an outlined
                // button, with large menu items below. Feedback round
                // 5, item 4: the source pill carries the tile color.
                PressablePillButton(
                    onClick = { expanded = true },
                    height = 56.dp,
                    containerColor = pageAccent ?: MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(currentSource, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    for (source in sources) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(source, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (source == currentSource) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                expanded = false
                                vm.callService("media_player", "select_source", entity.entityId, JSONObject().put("source", source))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FanControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null) {
    val pct = entity.attrInt("percentage") ?: 0
    val presets = entity.attrList("preset_modes")?.mapNotNull { it as? String } ?: emptyList()
    val oscillating = entity.attrBool("oscillating") ?: false
    Text("Speed $pct%", fontWeight = FontWeight.SemiBold)
    // Feedback round 5, item 4: speed slider + oscillate toggle follow the tile color.
    ServiceSlider(
        pct.toFloat(), 0f..100f,
        { vm.callService("fan", "set_percentage", entity.entityId, JSONObject().put("percentage", it.toInt())) },
        pageAccent ?: accentFor(entity.entityId),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        PillToggle(
            oscillating,
            { vm.callService("fan", "oscillate", entity.entityId, JSONObject().put("oscillating", !oscillating)) },
            pageAccent ?: accentFor(entity.entityId),
        )
        Spacer(Modifier.width(12.dp))
        Text("Oscillate", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (presets.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (preset in presets) {
                FilterChip(
                    selected = entity.attrString("preset_mode") == preset,
                    onClick = {
                        vm.callService("fan", "set_preset_mode", entity.entityId, JSONObject().put("preset_mode", preset))
                    },
                    label = { Text(preset) },
                )
            }
        }
    }
}

@Composable
fun LockControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null) {
    var confirmUnlock by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entity.stateText(), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (entity.state == "locked") {
            // Expressive-menus round: large 56dp pill, same tap.
            // Feedback round 5, item 4: the pill carries the tile color.
            PressablePillButton(
                onClick = { confirmUnlock = true },
                height = 56.dp,
                containerColor = pageAccent ?: MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) { Text("Unlock", fontWeight = FontWeight.SemiBold) }
        } else {
            PressablePillButton(
                onClick = { vm.callService("lock", "lock", entity.entityId) },
                height = 56.dp,
                containerColor = pageAccent ?: MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) { Text("Lock", fontWeight = FontWeight.SemiBold) }
        }
    }
    if (confirmUnlock) {
        // Unlock-confirmation preserved exactly; only the buttons
        // grow into large expressive pills (confirm = filled).
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
