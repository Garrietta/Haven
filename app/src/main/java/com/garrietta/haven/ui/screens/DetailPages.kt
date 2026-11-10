package com.garrietta.haven.ui.screens
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import com.garrietta.haven.ui.theme.FloatingCorner
import com.garrietta.haven.ui.theme.LargeTopShape
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.theme.havenMotion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailPopupHeader
import com.garrietta.haven.DetailPopupTitleHeader
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.RecordingFullscreen
import com.garrietta.haven.data.HaClient
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.findRecordingSibling
import com.garrietta.haven.data.formatHaTimestamp
import com.garrietta.haven.data.isRingCamera
import com.garrietta.haven.data.displayCameraName
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.VerticalPillToggle
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.shapeLabel
import com.garrietta.haven.ui.controls.domainIcon
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import com.garrietta.haven.ui.components.ShimmerBox
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.Sparkline
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.chartLineColor
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.ui.controls.GarageDoorVisual
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.data.detailBadgeOverride
import com.garrietta.haven.data.detailPageAccent
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.sparklineMinCurrentMax
import com.garrietta.haven.ui.components.weatherConditionColor
import com.garrietta.haven.ui.controls.EntityControlCard
import com.garrietta.haven.ui.controls.ForecastTimeline
import com.garrietta.haven.ui.controls.MediaTransportRow
import com.garrietta.haven.ui.controls.MediaVolumeRow
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.FanControl
import com.garrietta.haven.ui.controls.LightControl
import com.garrietta.haven.ui.controls.LockControl
import com.garrietta.haven.ui.controls.ServiceSlider
import com.garrietta.haven.ui.controls.SwitchControl
import com.garrietta.haven.ui.controls.VacuumControl
import com.garrietta.haven.ui.controls.WeatherControl
import com.garrietta.haven.ui.controls.WeatherHeroCard
import com.garrietta.haven.ui.controls.SensorTile
import com.garrietta.haven.ui.controls.entityIcon
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.stateText
import java.util.Locale
import org.json.JSONObject
import com.garrietta.haven.ui.components.SectionTitle

/**
 * Pop-ups, one per entity (2026-10-29 pop-up round — every detail
 * destination is a bottom-anchored floating shelf now, never a
 * full-span page): a consistent header (back, domain badge, name), a
 * hero state, the domain's primary control, then the history chart.
 * No attribute dump: value, state, controls, chart — nothing raw.
 * Tap a tile opens its pop-up; long-press still opens the quick sheet.
 *
 * Honesty rule, same as the thermostat page: a section renders only when
 * backing state exists. No invented readings, scenes, schedules, or
 * history — a history strip appears only for numeric states, through the
 * existing history endpoint, and sections with no state are omitted.
 * Climate keeps its own thermostat page; cameras open the live fullscreen
 * viewer (a page of its own, landscape-locked) instead of a static page.
 */
@Composable
fun DetailPageScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit, navigate: (Screen) -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    if (entity == null) {
        // Dialog-owned insets/sizing: no fillMaxSize, no status
        // bars padding — the dialog panel owns both (2026-10-30).
        // Feedback round 16, item 2: the top edge buffer matches
        // the thermostat popout's 16dp (was flush to the panel
        // edge); sides were already 16dp, bottom untouched.
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            DetailPopupTitleHeader("Unavailable")
            Text("This entity is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    // Feedback round 4, item 2: the badge pair for this entity
    // — every badge and toggle on this page carries the card's
    // exact theme instead of reverting to the default green.
    // (Climate renders ThermostatPopout, which resolves its own
    // pair internally.) Feedback round 17, item 1: the TILE's
    // pair (tileBadgePair), not the preset-only override — the
    // old null fallback sent headers to the hash accent (eufy
    // read yellow in panels while its tile read muted green).
    val detailCustom = rememberCardCustom(vm)
    val detailBadge = tileBadgePair(
        detailCustom.tilePresets, detailCustom.domainTilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
        MaterialTheme.colorScheme, accentFor(entity.entityId),
    )
    // Feedback round 5, items 2 + 4: the assigned tile color itself (preset container), gated
    // by the same active rules as the badge pair — the detail toggle track (item 2) and the
    // page-wide accent (item 4) share this one resolution, so they can never disagree.
    val detailAccent = detailPageAccent(
        detailCustom.tilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
        detailCustom.domainTilePresets,
    )
    // Climate safety net (Round 4): any path still landing on an
    // EntityDetail for climate renders the primary dial panel
    // directly — the secondary thermostat page is deleted.
    if (entity.domain == "climate") {
        ThermostatPopout(entity, vm, onClose = onBack)
        return
    }
    // Weather's full page is an expressive page (condition hero +
    // expanded forecast), not the generic attribute-dump detail.
    if (entity.domain == "weather") {
        WeatherPageScreen(entity, vm, onBack)
        return
    }
    if (entity.domain == "camera") {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            DetailPopupHeader(entity, onBack, displayTitle = detailHeaderCustom(vm, entity).first, displayIcon = detailHeaderCustom(vm, entity).second, badgeOverride = detailBadge)
            Spacer(Modifier.height(16.dp))
            Text(entity.stateText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accentFor(entityId))
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                vm.openCameraFullscreen(entityId)
                onBack()
            }) {
                // Ring shows its latest motion recording (labeled as
                // such in fullscreen); native cameras show live.
                // The button promises live only where it is live.
                Text(if (isRingCamera(entity)) "Open latest recording" else "Open live view")
            }
            Spacer(Modifier.height(16.dp))
            // Round 21, item 7: the clips timeline used to be a
            // camera-tile long-press — long-press is Arrange now,
            // so the timeline lives here as an Event history row
            // (the Front Door keeps its in-panel Event History
            // tab). Dismiss first, then navigate — the sheet's
            // Details handoff pattern.
            MenuRow(
                badgeKey = "clips-${entity.entityId}",
                badgeKind = shapeForDomain(entity.domain),
                icon = domainIcon(entity),
                title = "Event history",
                subtitle = "Recorded clips timeline",
                onClick = {
                    onBack()
                    navigate(Screen.CameraClips(entityId))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            // Round 21, item 5: the settings rows at the very
            // bottom, for every tile. Details is omitted — the
            // Open live view button above is this surface's
            // Details action.
            Spacer(Modifier.height(24.dp))
            EntityPopupMenuRows(vm, entity, onDetails = null)
            Spacer(Modifier.height(16.dp))
        }
        return
    }
    DetailPageContent(vm, entity, onBack, badgeOverride = detailBadge, pageAccent = detailAccent)
}

/**
 * The weather full page: tap on the Home weather card lands here
 * (long-press still opens the sheet). A condition-colored expressive
 * hero — the dial + humidity on a large condition-tinted panel — then
 * an expanded hourly forecast (24 hours of condition-colored shape
 * chips, not the sheet's 6). No raw entity id, no attribute dump:
 * this page is a designed surface, not the generic detail.
 */
@Composable
private fun WeatherPageScreen(entity: HaEntity, vm: HavenViewModel, onBack: () -> Unit) {
    // Dialog-owned sizing/scroll: plain column, no fillMaxSize,
    // no status-bars padding, no own scroll (2026-10-30 round).
    // Feedback round 4, item 2: same pair as the generic detail
    // page, resolved locally (this page takes vm + entity).
    // Feedback round 17, item 1: the tile pair, not the
    // preset-only override (see the detail page above).
    val weatherCustom = rememberCardCustom(vm)
    val weatherBadge = tileBadgePair(
        weatherCustom.tilePresets, weatherCustom.domainTilePresets, entity,
        isDarkBackdrop(MaterialTheme.colorScheme.background),
        MaterialTheme.colorScheme, accentFor(entity.entityId),
    )
    Column(
        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
    ) {
        DetailPopupHeader(entity, onBack, displayTitle = detailHeaderCustom(vm, entity).first, displayIcon = detailHeaderCustom(vm, entity).second, badgeOverride = weatherBadge)
        Spacer(Modifier.height(16.dp))
        // Round 4: the static non-functional grey semicircle gauge
        // (WeatherControl's display-only Dial) is GONE from this
        // panel — replaced by an expressive hero card (current
        // temperature, condition icon, high/low, humidity). The
        // Dial survives only in sheets/cards via WeatherControl.
        WeatherHeroCard(entity, vm)
        Spacer(Modifier.height(16.dp))
        SectionTitle("Hourly forecast")
        Spacer(Modifier.height(8.dp))
        // Timeline, not a list (2026-10-29 verdict: the 24-row plain
        // list "just doesn't look very good — a forecast could be a
        // timeline"). The hero above and the condition coloring are
        // unchanged; only the hourly presentation changes.
        ForecastTimeline(entity, vm)
        Spacer(Modifier.height(16.dp))
        // Round 21, item 5: the settings rows at the very bottom,
        // for every tile. Details is omitted — this IS the full page.
        Spacer(Modifier.height(8.dp))
        EntityPopupMenuRows(vm, entity, onDetails = null)
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Feedback round 18, item 2: the detail-layout branch, pure so it
 * is unit-testable. Numeric states (sensors) keep the two-column
 * split (control left, history right); every non-numeric state
 * (light, cover, media_player, lock, switch, fan, vacuum, and
 * any non-numeric sensor state) uses the full popup width.
 */
internal fun useDetailTwoColumnLayout(state: String?): Boolean = state?.toDoubleOrNull() != null

@Composable
private fun DetailPageContent(
    vm: HavenViewModel,
    entity: HaEntity,
    onBack: () -> Unit,
    // Feedback round 4, item 2: the preset badge pair (or null),
    // resolved by DetailPageScreen from the same store maps the
    // card reads.
    badgeOverride: Pair<Color, Color>? = null,
    // Feedback round 5, items 2 + 4: the assigned tile color itself (preset container, or
    // null), resolved by DetailPageScreen next to the badge pair. The detail toggle track
    // (item 2) and every themed page element (item 4) read this one value.
    pageAccent: Color? = null,
) {
    val accent = accentFor(entity.entityId)
    val control: @Composable () -> Unit = {
        when (entity.domain) {
            // Light detail panel (2026-11-01 round 1): the primary
            // control is a large vertical pill toggle — the Turn
            // on / Turn off / Toggle text buttons below are
            // suppressed for lights, and LightControl hides its
            // bulb row here so the header badge is the panel's
            // only bulb icon. Brightness / color sliders stay.
            // Auto-off timer controls are NOT rendered: no lamp
            // in this codebase's entity model exposes an auto-off
            // entity or attribute to bind them to, and inventing
            // a binding would actuate nothing real — flagged in
            // the round report instead.
            "light" -> Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VerticalPillToggle(
                    checked = entity.state == "on",
                    onToggle = { vm.toggle(entity.entityId) },
                    // Feedback round 5, item 2 (SUPERSEDES round 4's
                    // badge-bg track): the toggle track is the
                    // preset's pastel container when a preset is
                    // active — Standing Lamp's track goes yellow
                    // like its card — else the default primary, as
                    // before. The knob stays hardcoded white inside
                    // VerticalPillToggle, per the user's correction
                    // (the background changes, not the knob).
                    onColor = pageAccent ?: MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (entity.state == "on") "On" else "Off",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                LightControl(entity, vm, showBulb = false)
                // Round 4: the light panel is never an empty white
                // screen — device status cards below the pill:
                // power state, room assignment, last-changed
                // timestamp, and the linked smart plug's state.
                // Any line with no backing data is omitted, never
                // fabricated; no entity ids are shown.
                LightStatusCard(entity, vm)
            }
            "switch", "input_boolean" -> SwitchControl(entity, vm, pageAccent)
            "cover" -> CoverPageControl(entity, vm, pageAccent)
            "lock" -> LockControl(entity, vm, pageAccent)
            // Detail keeps only the labeled strip (see history()):
            // SensorTile's own chart would duplicate it (2026-10-30).
            "sensor", "binary_sensor" -> SensorTile(entity, vm, showHeader = false, showChart = false)
            "media_player" -> MediaPageControl(entity, vm, pageAccent, badgeOverride)
            "vacuum" -> VacuumControl(entity, vm, pageAccent, badgeOverride)
            "fan" -> FanControl(entity, vm, pageAccent)
            "weather" -> WeatherControl(entity, vm, showForecast = true)
            // This inner card is page content, never a morph source: it
            // must not report bounds or hide (the page overlay itself
            // is the morph surface).
            else -> EntityControlCard(entity, vm, onDetail = { }, showHeader = false, morphSource = false, pageAccent = pageAccent, pageBadge = badgeOverride)
        }
        Spacer(Modifier.height(16.dp))
        // Generic on/off actions, only where they make sense: an entity
        // whose state is on/off and which is not a read-only domain.
        // Lights are excluded (2026-11-01 round 1): their panel's
        // vertical pill toggle replaces these text buttons.
        if (entity.state in listOf("on", "off") && entity.domain != "light" &&
            entity.domain !in listOf("binary_sensor", "sensor", "weather", "camera", "person", "device_tracker")
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.turnOn(entity.entityId) }) { Text("Turn on") }
                OutlinedButton(onClick = { vm.turnOff(entity.entityId) }) { Text("Turn off") }
                OutlinedButton(onClick = { vm.toggle(entity.entityId) }) { Text("Toggle") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    // History only (2026-10-29 pop-up round): the raw attribute dump
    // is gone — no attribute rows, no Last updated / Last changed
    // timestamps, no state class, no measurement or unit rows, no raw
    // ids. An intuitive home app shows the value, the state, the
    // controls, and the chart — never the HA attribute table. The
    // 24-hour history strip stays: it is a chart, not a dump.
    val history: @Composable () -> Unit = {
        if (entity.state.toDoubleOrNull() != null) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Last 24 hours")
            Spacer(Modifier.height(8.dp))
            DetailHistoryStrip(vm, entity)
            Spacer(Modifier.height(16.dp))
        }
    }
    // Dialog-owned sizing/scroll (2026-10-30 round): the dialog
    // panel caps height at 85% and scrolls, so detail roots are plain
    // columns — no fillMaxSize, no status-bars padding, no own scroll.
    if (LocalWideLayout.current) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            DetailPopupHeader(entity, onBack, displayTitle = detailHeaderCustom(vm, entity).first, displayIcon = detailHeaderCustom(vm, entity).second, badgeOverride = badgeOverride)
            // Feedback round 18, item 2: the two-column split
            // applies ONLY to numeric states (sensors: control
            // left, history right). Non-numeric entities (light,
            // cover, media_player, lock, switch, fan, vacuum) use
            // the full popup width — the old split crushed them
            // into the left 50% column. The hero panel is gone
            // for ALL domains: DetailPopupHeader already shows
            // icon, title, and state (the Garage Door / TV
            // duplicate is fixed by removal, not by gating).
            if (useDetailTwoColumnLayout(entity.state)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        control()
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        history()
                    }
                }
            } else {
                control()
                history()
            }
            // Round 21, item 5: the settings rows at the very
            // bottom of the popup, for every tile. Details is
            // omitted here — this IS the full page.
            Spacer(Modifier.height(24.dp))
            EntityPopupMenuRows(vm, entity, onDetails = null)
            Spacer(Modifier.height(16.dp))
        }
    } else {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            DetailPopupHeader(entity, onBack, displayTitle = detailHeaderCustom(vm, entity).first, displayIcon = detailHeaderCustom(vm, entity).second, badgeOverride = badgeOverride)
            control()
            history()
            // Round 21, item 5: the settings rows at the very
            // bottom of the popup, for every tile. Details is
            // omitted here — this IS the full page.
            Spacer(Modifier.height(24.dp))
            EntityPopupMenuRows(vm, entity, onDetails = null)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Fraction of the garage-door frame the door fills, 1 = fully closed
 * (door down, frame full), 0 = fully open (door up, frame empty).
 * Position is ground truth when reported; without it, open/closed
 * states map to the ends and any in-between state (opening, closing,
 * unknown) maps to mid-travel — a neutral placeholder, never an
 * invented position. Pure, unit-tested.
 */
fun garageDoorFillFraction(position: Int?, state: String): Float {
    if (position != null) return (100 - position.coerceIn(0, 100)) / 100f
    return when (state) {
        "open" -> 0f
        "closed" -> 1f
        else -> 0.5f
    }
}

/** Cover page: open/close/stop always, position slider only when the cover reports one. */
@Composable
private fun CoverPageControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null) {
    val position = entity.attrInt("current_position")
    val accent = accentFor(entity.entityId)
    val isGarage = entity.attrString("device_class") == "garage"
    if (isGarage) {
        GarageDoorMenu(entity, vm, position, pageAccent)
        return
    }
    Column {
        Text(entity.stateText(), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        if (position != null) {
            Text("$position% open", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        // Same invalid-direction rule as GarageDoorMenu: Close
        // disabled + dimmed while closed, Open disabled + dimmed
        // while open (Stop always enabled). See the sheet
        // CoverControl for the panel path of the same rule.
        val isClosed = entity.state == "closed" || position == 0
        val isOpen = entity.state == "open" || position == 100
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Feedback round 5, item 4: Open/Close carry the tile color; Stop stays the
            // outlined default (an outlined pill has no fill to tint, and accent-colored
            // text on the page background fails contrast). Disabled pills keep the dim law.
            FilledTonalButton(
                onClick = { vm.callService("cover", "open_cover", entity.entityId) },
                modifier = Modifier.alpha(if (isOpen) 0.38f else 1f),
                enabled = !isOpen,
                colors = if (pageAccent != null) ButtonDefaults.filledTonalButtonColors(containerColor = pageAccent, contentColor = MaterialTheme.colorScheme.onSurface) else ButtonDefaults.filledTonalButtonColors(),
            ) { Text("Open") }
            FilledTonalButton(
                onClick = { vm.callService("cover", "close_cover", entity.entityId) },
                modifier = Modifier.alpha(if (isClosed) 0.38f else 1f),
                enabled = !isClosed,
                colors = if (pageAccent != null) ButtonDefaults.filledTonalButtonColors(containerColor = pageAccent, contentColor = MaterialTheme.colorScheme.onSurface) else ButtonDefaults.filledTonalButtonColors(),
            ) { Text("Close") }
            OutlinedButton(onClick = { vm.callService("cover", "stop_cover", entity.entityId) }) { Text("Stop") }
        }
        if (position != null) {
            Spacer(Modifier.height(8.dp))
            ServiceSlider(position.toFloat(), 0f..100f, { vm.setCoverPosition(entity.entityId, it.toInt()) }, pageAccent ?: accent, formatValue = { "${it.toInt()}%" })
        }
    }
}

/**
 * Garage door position control (2026-11-01 round 1): the old
 * expressive menu is gone — its neon accent fill and quarter
 * dividers read as a four-slat orange graphic, and the accent
 * house badge duplicated the page header's badge. What remains is
 * a clean M3 position control: a scheme-colored open-fraction
 * track (position is ground truth; without one, open/closed map
 * to the ends and in-between states to mid-travel via
 * [garageDoorFillFraction], never an invented position), the
 * Open / Stop / Close pills, and the position slider where a
 * position is reported. Close is disabled and muted while the
 * door already reports closed (state closed or position 0), so a
 * no-op close can never be sent. Open/Stop/Close actuation and
 * state reporting are unchanged. No camera feed here — a PiP
 * camera feed is a later round, explicitly not added.
 */
@Composable
private fun GarageDoorMenu(entity: HaEntity, vm: HavenViewModel, position: Int?, pageAccent: Color? = null) {
    // Open fraction: 1 - closed fraction (see garageDoorFillFraction).
    val openFraction = 1f - garageDoorFillFraction(position, entity.state)
    val reduceMotion = LocalReduceMotion.current
    val animatedOpen by animateFloatAsState(openFraction.coerceIn(0f, 1f), havenMotion(reduceMotion), label = "garageDoorOpen")
    val openClamped = animatedOpen.coerceIn(0f, 1f)
    val isClosed = entity.state == "closed" || position == 0
    val isOpen = entity.state == "open" || position == 100
    Column {
        // Feedback round 17, item 2: the "Garage door · <state>"
        // line is GONE — it duplicated DetailPopupHeader
        // ("Garage Door / <state>") directly above. The position
        // line stays: the header carries no position.
        if (position != null) {
            Text("$position% open", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        // Customization round, item 5: the door itself, not an
        // abstract track — tapping it toggles, and the slatted
        // panel translates UP as the door opens (reverse on close).
        // The Open/Close/Stop pills and the position slider below
        // are unchanged, with the same invalid-direction law.
        // Feedback round 5, item 4: the slatted door fill is the tile color (the video-3
        // garage detail: sky blue door), else the scheme primary as before.
        // Round 24, item 2: split hero — the door visual left
        // (150x168) and Open / Close / Stop stacked vertically
        // in a column on the right, opposite the animated door.
        // Actuation is unchanged (same service calls, same
        // invalid-direction law below), and no confirmation was
        // added (decided against, permanently).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GarageDoorVisual(
                openFraction = openClamped,
                onToggle = { vm.toggleCover(entity.entityId) },
                width = 150.dp,
                height = 168.dp,
                doorColor = pageAccent ?: MaterialTheme.colorScheme.primary,
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            // Invalid-direction actions are disabled AND dimmed to
            // 0.38 alpha (2026-11-01 hotfix): on this dark dynamic
            // palette secondaryContainer and surfaceContainerHighest
            // read as near-identical greys, so the muted-colors-only
            // treatment from round 1 looked fully active while
            // closed. The alpha drop is unmistakable against the
            // full-alpha siblings, and enabled = false blocks taps.
            // Feedback round 5, item 4: Open/Close/Stop all carry the tile color (the
            // video-3 garage detail: sky blue pills). Disabled pills keep their muted
            // surfaceContainerHighest treatment — the invalid-direction law is untouched.
            PressablePillButton(
                onClick = { vm.callService("cover", "open_cover", entity.entityId) },
                modifier = Modifier.fillMaxWidth().alpha(if (isOpen) 0.38f else 1f),
                height = 56.dp,
                enabled = !isOpen,
                containerColor = if (isOpen) MaterialTheme.colorScheme.surfaceContainerHighest else (pageAccent ?: MaterialTheme.colorScheme.secondaryContainer),
                contentColor = if (isOpen) MaterialTheme.colorScheme.onSurfaceVariant else (if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Open", fontWeight = FontWeight.SemiBold)
            }
            PressablePillButton(
                onClick = { vm.callService("cover", "close_cover", entity.entityId) },
                modifier = Modifier.fillMaxWidth().alpha(if (isClosed) 0.38f else 1f),
                height = 56.dp,
                enabled = !isClosed,
                containerColor = if (isClosed) MaterialTheme.colorScheme.surfaceContainerHighest else (pageAccent ?: MaterialTheme.colorScheme.secondaryContainer),
                contentColor = if (isClosed) MaterialTheme.colorScheme.onSurfaceVariant else (if (pageAccent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
            PressablePillButton(
                onClick = { vm.callService("cover", "stop_cover", entity.entityId) },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                containerColor = pageAccent ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Stop", fontWeight = FontWeight.SemiBold)
            }
            }
        }
        if (position != null) {
            Spacer(Modifier.height(8.dp))
            ServiceSlider(position.toFloat(), 0f..100f, { vm.setCoverPosition(entity.entityId, it.toInt()) }, pageAccent ?: MaterialTheme.colorScheme.primary, formatValue = { "${it.toInt()}%" })
        }
    }
}

/**
 * Customization round, item 2: the detail header's Card-settings
 * values, computed once per page from the same store maps every
 * surface reads (title + icon). Round 21, item 5: the pencil
 * target is gone — Card settings now lives in the
 * EntityPopupMenuRows at the bottom of every popup, so the
 * header no longer opens it; the Triple is a Pair.
 */
@Composable
internal fun detailHeaderCustom(vm: HavenViewModel, entity: HaEntity): Pair<String, androidx.compose.ui.graphics.vector.ImageVector> {
    val custom = rememberCardCustom(vm)
    val base = if (entity.domain == "sensor") sensorDisplayName(entity) else displayCameraName(entity)
    val title = customTitleFor(custom.titles, entity.entityId, base)
    // Feedback round 11, item 3: the one icon path.
    val icon = entityIcon(custom.iconKeys, entity)
    return title to icon
}

/**
 * Round 21, item 5: the settings rows that used to live in the
 * long-press sheet, now at the bottom of the single-press popup
 * for EVERY tile. The sheet is gone (HavenApp's
 * EntityPopupSheetContent is deleted); these rows are the only
 * settings surface, shared verbatim by the generic detail page,
 * the thermostat popout, the weather page, the camera column,
 * and the vacuum panel. [onDetails] is the "open the full page"
 * destination — null when this surface IS the full page (generic
 * detail, thermostat popout, weather page: the row would navigate
 * to itself, and pushing a duplicate detail screen is a real
 * defect, not a no-op), non-null where a deeper surface exists
 * (vacuum panel: the generic detail page). Cameras carry their
 * own Details action (the Open live view button) plus an Event
 * history row, so they pass null here too. Badge shape / Card
 * shape / Card settings are always present.
 */
@Composable
internal fun EntityPopupMenuRows(
    vm: HavenViewModel,
    entity: HaEntity,
    onDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (onDetails != null) {
            MenuRow(
                badgeKey = entity.entityId,
                badgeKind = shapeForDomain(entity.domain),
                icon = domainIcon(entity),
                title = "Details",
                subtitle = "Open the full page",
                onClick = onDetails,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        MenuRow(
            badgeKey = "shape-badge-${entity.entityId}",
            badgeKind = shapeForDomain(entity.domain),
            icon = domainIcon(entity),
            title = "Badge shape",
            subtitle = shapeLabel(vm.settingsStore.badgeShapeOverrides()[entity.entityId] ?: vm.settingsStore.globalBadgeShape) ?: "Domain default",
            onClick = { vm.openShapePicker(entity.entityId, false) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        MenuRow(
            badgeKey = "shape-card-${entity.entityId}",
            badgeKind = shapeForDomain(entity.domain),
            icon = domainIcon(entity),
            title = "Card shape",
            subtitle = shapeLabel(vm.settingsStore.cardShapeOverrides()[entity.entityId] ?: vm.settingsStore.globalCardShape) ?: "Default corners",
            onClick = { vm.openShapePicker(entity.entityId, true) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        MenuRow(
            badgeKey = "card-settings-${entity.entityId}",
            badgeKind = shapeForDomain(entity.domain),
            icon = domainIcon(entity),
            title = "Card settings",
            subtitle = "Title, icon, badge shape, tile color",
            onClick = { vm.openCardSettings(entity.entityId) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Media page: transport only for players in a transportable state, volume
 * only when the player reports a level, source only when it lists sources.
 * Anything HA would reject never renders as a button.
 */
@Composable
private fun MediaPageControl(entity: HaEntity, vm: HavenViewModel, pageAccent: Color? = null, pageBadge: Pair<Color, Color>? = null) {
    val title = entity.attrString("media_title") ?: "Nothing playing"
    val artist = entity.attrString("media_artist")
    val volume = entity.attrDouble("volume_level")
    val playing = entity.state == "playing"
    val transportOk = entity.state in listOf("playing", "paused", "idle", "buffering")
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (artist != null) Text(artist, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        if (transportOk) {
            // Expressive-menus round: reference-player transport.
            // Feedback round 5, item 4: the central squircle carries the tile color.
            MediaTransportRow(
                playing = playing,
                onPrevious = { vm.callService("media_player", "media_previous_track", entity.entityId) },
                onPlayPause = { vm.callService("media_player", "media_play_pause", entity.entityId) },
                onNext = { vm.callService("media_player", "media_next_track", entity.entityId) },
                pageAccent = pageAccent,
            )
            Spacer(Modifier.height(12.dp))
        }
        if (volume != null) {
            Text("Volume", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // Customization round, item 8: the slider accent
                // is the media badge FILL (very light purple #DAB9FF
                // in dark mode), never the deep-ink badge glyph.
                // Feedback round 5, item 4: a preset tile color's dark
                // ink wins over the matrix fill, as on the sheet.
                val mediaSliderAccent = pageBadge?.first
                    ?: domainCardColors(entity, isDarkBackdrop(MaterialTheme.colorScheme.background))?.badgeBg
                    ?: accentFor(entity.entityId)
                ServiceSlider(
                    (volume * 100).toFloat(), 0f..100f,
                    { vm.callService("media_player", "volume_set", entity.entityId, JSONObject().put("volume_level", it / 100f)) },
                    mediaSliderAccent,
                    formatValue = { "${it.toInt()}%" },
                )
            }
        }
        val sources = entity.attrList("source_list")?.mapNotNull { it as? String } ?: emptyList()
        if (sources.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val currentSource = entity.attrString("source") ?: "Source"
            Spacer(Modifier.height(12.dp))
            Box {
                // Feedback round 5, item 4: the source pill carries the tile color.
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
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    for (source in sources) {
                        DropdownMenuItem(
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

/**
 * Real 24-hour history through the existing endpoint; numeric states
 * only. Loading and empty are distinct states: while the fetch is in
 * flight the strip shows a shimmer, and "No history yet" appears only
 * after a completed load returns too few points. The old code showed
 * "No history yet" during every load, so history visibly arrived late
 * behind a false empty state (2026-10-29 evidence video).
 *
 * CACHED (2026-10-29 verdict: "the plot should be cached"): the
 * strip reads the ViewModel history cache first and refreshes
 * silently in the background. On re-entry the cached plot shows
 * immediately — no shimmer, no reload — and the refresh never blanks
 * it: the cache keeps the last plottable series across a failed
 * refresh, and only updates when new data lands.
 */
@Composable
private fun DetailHistoryStrip(vm: HavenViewModel, entity: HaEntity) {
    val cached = vm.historyFor(entity.entityId)
    LaunchedEffect(entity.entityId) {
        vm.refreshHistory(entity.entityId)
    }
    if (cached == null) {
        ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
    } else if (cached.size >= 2) {
        val points = cached
        val mm = sparklineMinCurrentMax(points)
        val unit = entity.attrString("unit_of_measurement")
        // Smooth reveal (2026-10-30 verdict: never an empty box
        // snapping to a full chart): the shimmer above covers the
        // load, and the path fades in once data lands. The branch
        // composes fresh when data arrives, so the fade is driven
        // by a revealed flag, not by the (already-1) initial value.
        var revealed by remember { mutableStateOf(false) }
        LaunchedEffect(points) { revealed = true }
        val stripAlpha by animateFloatAsState(
            if (revealed) 1f else 0f,
            animationSpec = tween(300),
            label = "historyStripReveal",
        )
        Column(Modifier.alpha(stripAlpha)) {
            Sparkline(points, chartLineColor(sensorKind(entity.entityId, entity.attrString("device_class"), entity.friendlyName), isDarkBackdrop(MaterialTheme.colorScheme.background)), Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(4.dp))
            if (mm != null) {
                Text(
                    "Min ${fmtDetailHistory(mm.first, unit)} · Current ${fmtDetailHistory(mm.second, unit)} · Max ${fmtDetailHistory(mm.third, unit)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        Text("No history yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Whole numbers print whole; the rest get one decimal; unit suffix when present. */
private fun fmtDetailHistory(v: Double, unit: String?): String {
    val num = if (v == v.toInt().toDouble()) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
    return if (unit != null) "$num $unit" else num
}

/**
 * Scrollable recorded-clips timeline for a camera (long-press on its
 * tile). Content, honestly sourced:
 * - the Ring last-recording sibling entity (same id prefix +
 *   "recording"), with its HA timestamp, played as MJPEG in the
 *   RecordingFullscreen (labeled Recording, never Live);
 * - media-source clips found by browsing candidate roots (generic
 *   camera root, Ring root) to depth 2. A playable node is collected
 *   only if it names this camera or comes from the Ring root for a
 *   Ring camera — under-collection is safe, misattribution is not.
 * Media-source browse responses carry no timestamps, so clips are
 * listed without invented dates; the dated recording (when present)
 * leads. Where HA exposes nothing for a camera, the empty state says
 * so plainly instead of faking a list.
 */
@Composable
fun CameraClipsScreen(vm: HavenViewModel, entityId: String, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val entity = states[entityId]
    // Display name, not the raw friendly name: HA calls the Ring
    // camera "Front Door Live view", so the clips title used to read
    // "Front Door Live view clips" while the content says "Not live".
    // displayCameraName strips that suffix for Ring only.
    val name = entity?.let { displayCameraName(it) } ?: entityId
    var loading by remember(entityId) { mutableStateOf(true) }
    var clips by remember(entityId) { mutableStateOf<List<HaClient.MediaChild>>(emptyList()) }
    var playing by remember(entityId) { mutableStateOf<HaClient.MediaChild?>(null) }
    var playingUrl by remember(entityId) { mutableStateOf<Pair<String, String>?>(null) }
    var resolving by remember(entityId) { mutableStateOf(false) }
    var recordingId by remember(entityId) { mutableStateOf<String?>(null) }
    val sibling = remember(entityId, states) { findRecordingSibling(entityId, states.values) }
    val isRing = entity?.let { isRingCamera(it) } == true
    val objId = entityId.substringAfter(".")

    LaunchedEffect(entityId) {
        val found = withTimeoutOrNull(20_000) {
            suspend fun browse(id: String, type: String): List<HaClient.MediaChild>? =
                suspendCancellableCoroutine { cont ->
                    vm.client.browseMedia(id, type) { cont.resumeWith(Result.success(it)) }
                }
            fun belongs(child: HaClient.MediaChild) =
                child.title.contains(name, ignoreCase = true) ||
                    child.mediaContentId.contains(objId, ignoreCase = true)
            val out = mutableListOf<HaClient.MediaChild>()
            val ringRoot = "media-source://ring"
            val roots = listOf("" to "", "media-source://camera" to "media_source", ringRoot to "media_source")
            for ((rootId, rootType) in roots) {
                val children = browse(rootId, rootType) ?: continue
                for (child in children) {
                    if (child.canPlay) {
                        if (belongs(child) || (rootId == ringRoot && isRing)) out.add(child)
                        continue
                    }
                    if (!child.canExpand) continue
                    val generic = child.title.equals("Camera", true) ||
                        child.title.equals("Cameras", true) ||
                        (isRing && child.title.equals("Ring", true))
                    if (!belongs(child) && !generic) continue
                    val grandchildren = browse(child.mediaContentId, child.mediaContentType) ?: continue
                    for (gc in grandchildren) {
                        if (gc.canPlay && (belongs(gc) || (rootId == ringRoot && isRing))) out.add(gc)
                    }
                }
            }
            out.distinctBy { it.mediaContentId }
        } ?: emptyList()
        clips = found
        loading = false
    }

    // Resolve a tapped clip, then play it fullscreen.
    LaunchedEffect(playing) {
        val clip = playing ?: return@LaunchedEffect
        resolving = true
        playingUrl = null
        playingUrl = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Pair<String, String>?> { cont ->
                vm.client.resolveMedia(clip.mediaContentId, clip.mediaContentType) { cont.resumeWith(Result.success(it)) }
            }
        }
        resolving = false
    }

    Box(Modifier.fillMaxSize()) {
        // Status-bar inset (Gemini mockup round, 2026-11-01): every
        // other DetailTopBar caller pads its root, but this screen
        // did not — its title ran into the status bar on the
        // unfolded video (the purple-arrow frame). Same padding as
        // its siblings, nothing else changes.
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DetailTopBar(title = "$name clips", onBack = onBack)
            when {
                loading -> {
                    repeat(4) {
                        ShimmerBox(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                sibling == null && clips.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("No recorded clips", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Home Assistant exposes no recorded-clip list for this camera.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isRing) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "If your Ring integration has a last-recording entity, enabling it adds it here.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (sibling != null) {
                            item { TimelineSectionHeader("Last recording") }
                            item {
                                RecordingRow(vm, sibling) { recordingId = sibling.entityId }
                            }
                        }
                        if (clips.isNotEmpty()) {
                            item { TimelineSectionHeader("Recorded clips") }
                            items(clips, key = { it.mediaContentId }) { clip ->
                                ClipRow(vm, clip) { playing = clip }
                            }
                        }
                    }
                }
            }
        }
        if (recordingId != null) {
            androidx.activity.compose.BackHandler { recordingId = null }
            RecordingFullscreen(vm, recordingId!!, onClose = { recordingId = null }, enterProgress = { 1f })
        }
        if (playing != null) {
            androidx.activity.compose.BackHandler { playing = null }
            ClipPlayerScreen(vm, playing!!, playingUrl, resolving, onClose = { playing = null })
        }
    }
}

@Composable
private fun TimelineSectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RecordingRow(vm: HavenViewModel, sibling: HaEntity, onTap: () -> Unit) {
    // Cached snapshot: re-entering the timeline shows the last
    // recording thumbnail immediately, refreshed in the background.
    val bitmap = vm.snapshotFor(sibling.entityId)
    LaunchedEffect(sibling.entityId) {
        vm.refreshSnapshot(sibling.entityId)
    }
    Row(
        Modifier.fillMaxWidth().clickable { onTap() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(96.dp, 64.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // Explicit onSurface (dark-mode audit, 2026-11-01): this
            // bare Row provides no content color, so the old
            // color-less Text defaulted to black — invisible on the
            // dark background, same class as the empty-state heading.
            Text(sibling.friendlyName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            val stamp = formatHaTimestamp(sibling.lastChanged ?: sibling.lastUpdated)
            Text(
                stamp ?: "Last recording",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ClipRow(vm: HavenViewModel, clip: HaClient.MediaChild, onTap: () -> Unit) {
    // Cached thumbnail: re-entering the timeline shows the last
    // thumbnail immediately, refreshed in the background.
    val thumb = clip.thumbnail
    val bitmap = thumb?.let { vm.imageFor(it) }
    LaunchedEffect(clip.mediaContentId) {
        if (thumb != null) vm.refreshImage(thumb)
    }
    Row(
        Modifier.fillMaxWidth().clickable { onTap() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(96.dp, 64.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // Explicit onSurface: same bare-Row, no-content-color
            // case as RecordingRow above — the old color-less title
            // defaulted to black in dark mode.
            Text(clip.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Tap to play",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Fullscreen playback of one resolved media-source clip (scrubbable). */
@Composable
private fun ClipPlayerScreen(
    vm: HavenViewModel,
    clip: HaClient.MediaChild,
    resolved: Pair<String, String>?,
    resolving: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(resolved) {
        if (resolved == null) return@DisposableEffect onDispose {}
        val p = vm.client.createMediaPlayer(context, resolved.first, resolved.second)
        player = p
        onDispose {
            player = null
            p.release()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) { // video surface: black by design, not a scheme role
        when {
            resolving -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading clip…", color = Color.White, fontSize = 16.sp)
                }
            }
            resolved == null -> {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Clip unavailable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Home Assistant could not resolve this clip.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onClose) { Text("Close") }
                }
            }
            player != null -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    // Re-bind on recomposition: without this the
                    // factory-captured player goes stale when the
                    // resolved clip changes, and the view can show a
                    // released player (same staleness class as the
                    // HLS fullscreen fix in this round).
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Feedback round 7: the top-end Close chrome is removed
        // with every other popup chrome button. The content
        // "Close" button in the unavailable state above stays
        // (it is content, not chrome — that state has no other
        // visible dismiss affordance), and BackHandler stays wired
        // to onClose.
    }
}

/**
 * Device status card for the expanded light panel (Round 4):
 * power state (+ brightness % when the attribute exists), room
 * assignment (from the HA area registry), last-changed timestamp
 * (formatted, never a raw ISO string), and the linked smart plug
 * status — Lamp Socket 1 for Lamp
 * (light.lamp_socket_1 -> switch.smart_indoor_plug_3_socket_1)
 * and Standing lamp Socket 1 for Standing Lamp
 * (light.standing_lamp_socket_1 -> switch.standing_lamp_socket_1).
 * A row renders only when its data exists; the whole card hides
 * when nothing backs it. No entity ids on the surface.
 */
@Composable
private fun LightStatusCard(entity: HaEntity, vm: HavenViewModel) {
    val states by vm.states.collectAsState()
    val areas by vm.areas.collectAsState()
    val entityAreas by vm.entityAreas.collectAsState()
    val linkedPlugId = when (entity.entityId) {
        "light.lamp_socket_1" -> "switch.smart_indoor_plug_3_socket_1"
        "light.standing_lamp_socket_1" -> "switch.standing_lamp_socket_1"
        else -> null
    }
    val plug = linkedPlugId?.let { states[it] }
    val areaName = entityAreas[entity.entityId]?.let { areaId -> areas.firstOrNull { it.id == areaId }?.name }
    val lastChanged = formatHaTimestamp(entity.lastChanged ?: entity.lastUpdated)
    val brightness = entity.attrInt("brightness")?.let { it * 100 / 255 }
    val rows = buildList {
        add("Power" to if (entity.state == "on") {
            if (brightness != null) "On • $brightness%" else "On"
        } else "Off")
        if (areaName != null) add("Room" to areaName)
        if (lastChanged != null) add("Last changed" to lastChanged)
        if (plug != null) add(plug.friendlyName to if (plug.state == "on") "On" else "Off")
    }
    if (rows.isEmpty()) return
    Spacer(Modifier.height(16.dp))
    Surface(
        shape = generalCardShape(0),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            for ((label, value) in rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                // Feedback round 15, item 6: light detail-panel
                // rows share the thermostat panel's 12dp block
                // rhythm (was 8dp) — one default vertical
                // padding for all entity rows.
                if ((label to value) != rows.last()) Spacer(Modifier.height(12.dp))
            }
        }
    }
}
