package com.garrietta.haven.ui.screens
import com.garrietta.haven.ui.components.compositeOver
import com.garrietta.haven.ui.components.ensureContrast
import com.garrietta.haven.ui.components.CoolBlue
import com.garrietta.haven.ui.components.HeatOrange

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.FRONT_DOOR_LIVE_VIEW_ID
import com.garrietta.haven.data.displayCameraName
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.data.inactiveCardColors
import com.garrietta.haven.data.presetAppliesForState
import com.garrietta.haven.data.presetBadgeColors
import com.garrietta.haven.data.tilePresetFor
import com.garrietta.haven.data.tilePresetColor
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.badgeColors
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.controls.shapeTapAction
import com.garrietta.haven.ui.components.hideBodyWhenMorphSource
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.hideHeroWhenMorphSource
import com.garrietta.haven.ui.components.reportHeroBounds
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.components.reportCardColor
import com.garrietta.haven.ui.components.reportCardShape
import com.garrietta.haven.ui.controls.ExpressiveStepper
import com.garrietta.haven.ui.controls.applyClimateSetpoint
import com.garrietta.haven.ui.controls.entityIcon
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.polygonShape
import com.garrietta.haven.ui.theme.pressBounce
import org.json.JSONObject

/**
 * Apple Home-style tiles, for TAB PAGES ONLY. Boards, Today, and Areas
 * keep their full cards; a tab page shows one tile per entity instead:
 * icon + name + natural status, in an adaptive grid (2 columns folded,
 * more unfolded). Tap opens the entity's full page — thermostat page for
 * climate, live fullscreen for cameras, per-domain page for everything
 * else — rising out of the tapped tile on the house spring. Long-press
 * always opens the more-info/detail sheet.
 *
 * Cards that can't tile keep their card rendering through [fallbackCard]:
 * gauge and button cards (their visuals/actions don't survive as a tile),
 * cards with no entity (markdown, area), and unparseable YAML (which keeps
 * its repair link instead of vanishing).
 */
@Composable
fun TabTileGrid(
    vm: HavenViewModel,
    cards: List<String>,
    states: Map<String, HaEntity>,
    navigate: ((Screen) -> Unit)? = null,
    fallbackCard: @Composable (String) -> Unit,
) {
    LazyVerticalGrid(
        // Round 6: wide layouts flow at 220dp minimum tiles, so
        // domain tabs (Thermostat/Cameras/Garage) spread across the
        // full inner-fold width instead of jamming into a narrow
        // 2-column strip with a stranded single tile.
        columns = if (LocalWideLayout.current) GridCells.Adaptive(minSize = 220.dp) else GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Expressive-menus round, item 6: grid position feeds the
        // general card shape's rotating 12dp corner.
        var tileIndex = 0
        for (yaml in cards) {
            val parsed = try {
                parseCardYaml(yaml)
            } catch (e: Exception) {
                null
            }
            val type = parsed?.optString("type") ?: ""
            val ids = if (parsed == null || type == "gauge" || type == "button") {
                emptyList()
            } else {
                cardEntityIds(parsed)
            }
            if (ids.isEmpty()) {
                // Non-entity cards have no stable entity id; key by
                // their YAML so they still never swap slots with a
                // neighbor on a panel return.
                item(key = "card:$yaml", span = { GridItemSpan(maxLineSpan) }) { fallbackCard(yaml) }
            } else {
                for (id in ids) {
                    val entity = states[id]
                    if (entity != null) {
                        // Stable entity_id keys (2026-11-01 round 1):
                        // unkeyed items recomposed into siblings'
                        // slots on a panel return, which read as
                        // clipped titles and one-frame badge flashes.
                        item(key = id) {
                            EntityTile(
                                vm,
                                entity,
                                navigate = navigate,
                                gridIndex = tileIndex++,
                            )
                        }
                    } else {
                        item(key = "missing:$id") { MissingEntityTile(id, gridIndex = tileIndex++) }
                    }
                }
            }
        }
    }
}

private fun cardEntityIds(card: JSONObject): List<String> {
    card.optString("entity").ifEmpty { null }?.let { return listOf(it) }
    val arr = card.optJSONArray("entities") ?: return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        when (val item = arr.get(i)) {
            is String -> out.add(item)
            is JSONObject -> item.optString("entity").ifEmpty { null }?.let { out.add(it) }
        }
    }
    return out
}

/** Natural status line: light on/off rides the icon state, the rest read out in words. */
/** HA brightness is 0–255; tiles show it as a percentage. */
internal fun lightBrightnessPercent(brightness255: Int): Int = brightness255 * 100 / 255

private fun tileStatus(entity: HaEntity): String = when (entity.domain) {
    "light" -> if (entity.state == "on") {
        // Scannable state: a lit light reads its brightness, not
        // just "On" — "On · 75%" tells the whole state at a glance.
        val b = entity.attrInt("brightness")?.let { lightBrightnessPercent(it) }
        if (b != null) "On · $b%" else "On"
    } else "Off"
    "switch", "input_boolean" -> if (entity.state == "on") "On" else "Off"
    "cover" -> {
        val pos = entity.attrInt("current_position")
        val base = entity.stateText()
        if (pos != null && pos != 0 && pos != 100) "$base · $pos% open" else base
    }
    "climate" -> {
        val cur = entity.attrDouble("current_temperature")
        if (cur != null) "${cur.formatTemp()} · ${entity.stateText()}" else entity.stateText()
    }
    "camera" -> "Tap to view"
    else -> entity.stateText()
}

/** Tap routing: climate opens the expressive pop-out panel, cameras
 * open fullscreen live, everything else opens its full page panel
 * (or the sheet panel when there is no navigator, e.g. Boards
 * cards). Lights never reach this function on tap anymore — a light
 * tile tap toggles inline (see EntityTile) — but a light long-press
 * routes through here to its panel, like every other domain. */
private fun openPage(vm: HavenViewModel, entity: HaEntity, navigate: ((Screen) -> Unit)?) {
    when (entity.domain) {
        "climate" -> vm.openClimatePopout(entity.entityId)
        // Round 4 Front Door unification: the Cameras-tab tile for
        // the front door opens the SAME #93 panel (Live View /
        // Event History toggle) as the Home hero — not the legacy
        // fullscreen viewer. Every other camera keeps fullscreen.
        "camera" -> if (entity.entityId == FRONT_DOOR_LIVE_VIEW_ID) vm.openCameraDialog(entity.entityId) else vm.openCameraFullscreen(entity.entityId)
        else -> if (navigate != null) navigate(Screen.EntityDetail(entity.entityId)) else vm.openPopup(entity.entityId)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityTile(vm: HavenViewModel, entity: HaEntity, navigate: ((Screen) -> Unit)? = null, gridIndex: Int = 0) {
    val accent = accentFor(entity.entityId)
    // Snapshot-held visuals (2026-10-30 verdict round, same rule as
    // cards): while this tile's morph is open, its badge, name,
    // state, and container tint resolve from the open/dismiss-time
    // snapshot, never the live entity — the live state can move
    // mid-flight (a setpoint drag settling, hvac_action flipping),
    // which read as a color shift on open and a dark-then-muted
    // flash on dismiss. Body controls and actuation stay live.
    val snap = vm.travelSnapshots[entity.entityId]
    val display = snap ?: entity
    // Customization round, item 2: user-tab tiles honor the same
    // overrides as Home cards (title, icon, tile color).
    val tileCustom = rememberCardCustom(vm)
    val tileTitle = customTitleFor(tileCustom.titles, entity.entityId,
        if (entity.domain == "sensor") sensorDisplayName(display) else displayCameraName(display))
    // Feedback round 11, item 3: the one icon path.
    val tileIcon = entityIcon(tileCustom.iconKeys, entity)
    val tilePreset = tilePresetFor(tileCustom.tilePresets, entity.entityId, tileCustom.domainTilePresets, entity.domain)
    // Feedback round 2, item 1: same ON-state gating as Home cards
    // — a preset tile whose entity is off renders INACTIVE gray.
    val tilePresetActive = tilePreset != null && presetAppliesForState(display.domain, display.state)
    val tilePresetContainer = if (tilePresetActive) tilePresetColor(tilePreset, isDarkBackdrop(MaterialTheme.colorScheme.background)) else null
    val tileInactiveContainer = if (tilePreset != null) inactiveCardColors(isDarkBackdrop(MaterialTheme.colorScheme.background)).container else null
    // Feedback round 2, item 3: same polygon-clip guard as Home
    // cards — polygon outlines cut into the bottom text zone, so
    // polygon tiles shift their text block up 12dp (spacer 16->4,
    // bottom padding 16->28, height unchanged).
    val tileOutline = LocalShapePrefs.current.cardShapeFor(entity.entityId, entity.domain) ?: generalCardShape(gridIndex)
    val tileIsPolygon = tileOutline !is RoundedCornerShape
    // The tile's window bounds feed the camera rise origin (and are
    // kept for it). Detail/sheet morphs use the root bounds reported
    // to the shared map (see MorphOverlay), never this rect.
    var tileBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = com.garrietta.haven.ui.components.LocalReduceMotion.current
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.97f else 1f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.spring(stiffness = 10_000f, dampingRatio = 1f) else pressBounce(),
        label = "tilePress",
    )
    // Feedback round 16, item 4a: the tile's container color as
    // a named value, so the morph can report it alongside the
    // bounds/shape — the surface must start at the tile's own
    // color, not panel cream (the white exit flash).
    val tileContainerColor = tilePresetContainer ?: tileInactiveContainer ?: when {
            // Lit tiles tint by kelvin AND brightness: a dim light reads
            // dimmer than a bright one, never full-tint at 10%.
            display.domain == "light" && display.state == "on" ->
                lightBrightnessContainer(
                    MaterialTheme.colorScheme,
                    display.attrInt("color_temp_kelvin"),
                    display.attrInt("brightness")?.let { it * 100 / 255 },
                    true,
                )
            // Climate tiles carry their mode color at low alpha — the
            // mode is visible on the tab grid, not just on the page.
            display.domain == "climate" ->
                lerp(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    climateModeColor(display.state, display.attrString("hvac_action")),
                    0.15f,
                )
            else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        // Expressive-menus round, item 6: the general card shape
        // (a shape-picker override still beats it, as on Home).
        shape = tileOutline,
        color = tileContainerColor,
        modifier = Modifier.fillMaxWidth()
            .onGloballyPositioned { tileBounds = it.boundsInWindow() }
            .reportCardBounds(entity.entityId)
            .reportCardShape(entity.entityId, tileOutline)
            .reportCardColor(entity.entityId, tileContainerColor)
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .hideWhenMorphSource(entity.entityId)
            // Body hide (2026-10-29 verdict-fix round): the whole
            // tile vanishes from frame 0 while its morph is open,
            // exactly like the header hide — the climate tile's dual
            // numbers and steppers were the one body section the
            // header hide did not cover, so they doubled against the
            // pop-out's dial numbers. The surface grows from the
            // tile rect, so the restore on dismiss is invisible.
            .hideBodyWhenMorphSource(entity.entityId)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                // Lights toggle inline on tap (the same vm.toggle
                // path the badge shape action uses, optimistic guard
                // included) — no panel, no dialog. Every other
                // domain keeps its tap routing (climate panel,
                // camera fullscreen, detail panel).
                onClick = {
                    if (entity.domain == "light") {
                        vm.toggle(entity.entityId)
                    } else {
                        tileBounds?.let { vm.pageOrigin = it }
                        openPage(vm, entity, navigate)
                    }
                },
                // Long-press on a camera opens its recorded-clips
                // timeline (full page, unchanged); a long-press on
                // a light opens its panel (the tap no longer does);
                // every other domain keeps the quick sheet panel.
                // Without a navigator (Boards cards) the sheet is
                // still the fallback.
                onLongClick = {
                    if (entity.domain == "camera" && navigate != null) navigate(Screen.CameraClips(entity.entityId))
                    // Round 4: climate long-press opens the primary
                    // dial panel, same as tap (the secondary Details
                    // page is deleted); light long-press opens its
                    // panel; every other domain keeps the sheet.
                    else if (entity.domain == "climate") vm.openClimatePopout(entity.entityId)
                    else if (entity.domain == "light") openPage(vm, entity, navigate)
                    else vm.openPopup(entity.entityId)
                },
            ),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (tileIsPolygon) 28.dp else 16.dp)) {
            if (entity.domain == "camera") {
                CameraTileVisual(vm, entity, accent)
            } else {
                // Shape-tap = actuate: the expressive shape IS the
                // control for binary entities (tap the bulb, the light
                // flips; tap the garage shape, the door moves), while
                // the background tap still opens the page. The badge's
                // own combinedClickable keeps long-press -> sheet, so
                // no gesture is lost to the inner target. Non-actuable
                // domains get no inner target: taps fall through to
                // the tile's background action. Climate badges carry
                // the mode color (full when the system is on, muted
                // when off/idle); the light shape morph on toggle is
                // untouched (DomainBadge still morphs hexagon->scallop).
                // One badge pipeline (2026-10-30 verdict round): the
                // tile badge takes badgeColors verbatim — the same
                // function the traveling badge and every dialog
                // header badge use — so a tile icon's color at rest
                // is identical to its dialog color, on open AND on
                // dismiss. The old tile pipeline used full-strength
                // climateModeColor/accent for active tiles, which no
                // dialog ever used: the shift was structural, not
                // state drift. Tradeoff, stated plainly: tile badges
                // now read as container/wash tones like dialogs
                // (climate = setpoint containers, lights = kelvin
                // containers, sensors = accent wash) instead of
                // full-strength color — the tile container tints
                // (kelvin/brightness, climate mode) are untouched,
                // so tiles still read their state at a glance.
                // Off-state grey (2026-10-29 verdict: off icons grey
                // everywhere, tiles matching pop-ups) falls out of
                // badgeColors itself: off lights, fans, media, and
                // untriggered binary sensors all return the pop-up
                // grey pair (surfaceContainerHighest +
                // onSurfaceVariant) from that one function.
                // Feedback round, item 5: a set preset drives the
                // badge pair too (dark tint + vivid glyph from the
                // preset hue); unset tiles keep badgeColors.
                val (badgeBg, badgeGlyph) = if (tilePresetActive) {
                    presetBadgeColors(tilePreset!!, isDarkBackdrop(MaterialTheme.colorScheme.background))
                } else if (tilePreset != null) {
                    val inactive = inactiveCardColors(isDarkBackdrop(MaterialTheme.colorScheme.background))
                    inactive.badgeBg to inactive.badgeGlyph
                } else {
                    badgeColors(
                        MaterialTheme.colorScheme, display, accent,
                    )
                }
                val shapeAction = shapeTapAction(vm, entity)
                val badgeInteraction = remember { MutableInteractionSource() }
                val badgePressed by badgeInteraction.collectIsPressedAsState()
                val badgeScale by androidx.compose.animation.core.animateFloatAsState(
                    if (badgePressed) 0.92f else 1f,
                    animationSpec = if (reduceMotion) androidx.compose.animation.core.spring(stiffness = 10_000f, dampingRatio = 1f) else pressBounce(),
                    label = "badgePress",
                )
                // 64dp badge (up from 52dp) + 16dp gap to the name:
                // the expressive shape background is larger than the
                // text block it sits over, so the 1.08x pulse and the
                // rotation never touch the name (2026-10-29 verdict:
                // "the text overlaps [the] — this expressive shape
                // background should be larger").
                // Round 4 split zone (light/switch tiles, mirroring
                // HomeRoomCard): the badge area wears a subtle
                // pill/squircle underlay and toggles inline on tap;
                // the name/status zone below opens the panel.
                val tileIsBinary = entity.domain in setOf("light", "switch", "input_boolean", "fan")
                val tileZoneInteraction = remember { MutableInteractionSource() }
                Box(
                    if (tileIsBinary) Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
                        .combinedClickable(
                            interactionSource = tileZoneInteraction,
                            indication = null,
                            onClick = { vm.toggle(entity.entityId) },
                            onLongClick = { openPage(vm, entity, navigate) },
                        )
                        .padding(8.dp) else Modifier,
                ) {
                Box(
                    Modifier.size(64.dp).background(
                        badgeBg,
                        // Tile badge shape (2026-10-30 picker): a
                        // per-entity override beats the global
                        // default; without either, the domain shape.
                        LocalShapePrefs.current.badgeShapeFor(entity.entityId, entity.domain)
                            ?: polygonShape(shapeForDomain(entity.domain)),
                    )
                        .graphicsLayer(scaleX = badgeScale, scaleY = badgeScale)
                        // Traveling header anchor (2026-10-29 pop-up
                        // round): the badge rect is the tile header's
                        // travel origin, and the badge hides the moment
                        // its morph opens so the overlay's single
                        // traveling header replaces it exactly.
                        // Camera tiles keep the rise path and do not
                        // report: their fullscreen is not a morph.
                        .then(if (entity.domain != "camera") Modifier.reportHeroBounds(entity.entityId).hideHeroWhenMorphSource(entity.entityId) else Modifier)
                        .then(if (shapeAction != null) Modifier.combinedClickable(
                            interactionSource = badgeInteraction,
                            indication = null,
                            onClick = shapeAction,
                            onLongClick = { vm.openPopup(entity.entityId) },
                        ) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tileIcon,
                        contentDescription = null,
                        tint = badgeGlyph,
                        modifier = Modifier.size(26.dp),
                    )
                }
                } // badge-zone Box
            }
            Spacer(Modifier.height(if (tileIsPolygon) 4.dp else 16.dp))
            // Round 4 split zone, lower half: tapping the name or
            // status opens the panel (binary tiles); long-press
            // anywhere does the same. Non-binary tiles keep their
            // surface-level routing; this zone targets openPage
            // for all of them, which matches tap routing except
            // cameras (fullscreen) — camera tiles are excluded
            // here because their visual branch above already owns
            // the tap path.
            val tileTextZoneInteraction = remember { MutableInteractionSource() }
            Column(
                Modifier.fillMaxWidth().then(
                    if (entity.domain != "camera") Modifier.combinedClickable(
                        interactionSource = tileTextZoneInteraction,
                        indication = null,
                        onClick = { openPage(vm, entity, navigate) },
                        onLongClick = { openPage(vm, entity, navigate) },
                    ) else Modifier,
                ),
            ) {
            Text(
                // Display name: the Ring camera's HA name is
                // "Front Door Live view" — the tile title from the
                // unfolded video. displayCameraName strips that
                // suffix for Ring only; sensor names strip the
                // "Thermostat Current " prefix; every other entity
                // is byte-identical.
                tileTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                // Single line, always (2026-11-01 round 1): see
                // HomeRoomCard — wrapping titles reflowed on panel
                // return and read as clipped text.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Round 6 badge-only hide: the tile's name stays
                // visible while the morph is open; only its badge
                // hides while the traveler flies.
                modifier = Modifier,
            )
            if (entity.domain == "climate") {
                ClimateTileBody(vm, entity)
            } else {
                Text(
                    tileStatus(display),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier,
                )
            }
            } // text-zone Column
        }
    }
}

// Heat/cool colors match the thermostat page's dial colors.

/**
 * Google Home Favorites-style climate tile: dual numbers (heat left,
 * cool right, the active selector at full strength and the inactive one
 * muted) or the single number, an "Indoor 66°" line when a current
 * temperature exists, and −/+ steppers stepping the active selector by
 * one degree. The steppers are real buttons, so their taps are consumed
 * by the button and never trigger the tile's tap-to-open; tap anywhere
 * else on the tile still opens the thermostat page, and long-press still
 * opens the sheet.
 */
@Composable
private fun ClimateTileBody(vm: HavenViewModel, entity: HaEntity) {
    val low = entity.attrDouble("target_temp_low")
    val high = entity.attrDouble("target_temp_high")
    val single = entity.attrDouble("temperature")
    val current = entity.attrDouble("current_temperature")
    val heatCool = low != null && high != null
    val selector = defaultSelector(entity.state)
    // Number backdrop = the tile container itself (same lerp as the
    // tile root): setpoint numbers are floored against it, so the
    // 0.45-alpha inactive numbers can no longer wash out (2026-10-30
    // verdict: "indoor setpoints" in light mode, dial clash in both).
    // Active numbers meet 4.5:1; inactive ones (28sp bold = large
    // text) meet 3:1 while staying visibly muted.
    val tileBackdrop = lerp(
        MaterialTheme.colorScheme.surfaceContainerLow,
        climateModeColor(entity.state, entity.attrString("hvac_action")),
        0.15f,
    )
    fun setpointNumber(base: Color, active: Boolean): Color =
        if (active) ensureContrast(base, tileBackdrop, 4.5f)
        else ensureContrast(compositeOver(base.copy(alpha = 0.45f), tileBackdrop), tileBackdrop, 3.0f)
    Spacer(Modifier.height(8.dp))
    if (heatCool) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                low!!.formatTemp(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = setpointNumber(HeatOrange, selector == SetpointSelector.HEATING),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                high!!.formatTemp(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = setpointNumber(CoolBlue, selector == SetpointSelector.COOLING),
            )
        }
    } else if (single != null) {
        val singleColor = when (entity.state) {
            "heat" -> HeatOrange
            "cool" -> CoolBlue
            else -> accentFor(entity.entityId)
        }
        Text(single.formatTemp(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = setpointNumber(singleColor, true))
    }
    if (current != null) {
        // Verdict 2026-10-30: the Indoor line washed out in light
        // mode — full onSurface, not the variant role.
        Text(
            "Indoor ${current.formatTemp()}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
    if (heatCool || single != null) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveStepper(
                "−",
                onClick = {
                    tileStepNext(entity, selector, -1.0)?.let {
                        applyClimateSetpoint(vm, entity, selector, it)
                    }
                },
                height = 32.dp,
            )
            ExpressiveStepper(
                "+",
                onClick = {
                    tileStepNext(entity, selector, 1.0)?.let {
                        applyClimateSetpoint(vm, entity, selector, it)
                    }
                },
                height = 32.dp,
            )
        }
    }
}

/**
 * The tile stepper's next value: the active setpoint stepped by [delta].
 * Heat/cool clamping happens inside [applyClimateSetpoint]; the single
 * path snaps to whole degrees and clamps to the real min/max (null
 * bounds skipped, never invented). Null when there is no setpoint to
 * step, and then the caller sends nothing.
 */
private fun tileStepNext(entity: HaEntity, selector: SetpointSelector, delta: Double): Double? {
    val low = entity.attrDouble("target_temp_low")
    val high = entity.attrDouble("target_temp_high")
    if (low != null && high != null) {
        return if (selector == SetpointSelector.HEATING) low + delta else high + delta
    }
    val single = entity.attrDouble("temperature") ?: return null
    var next = snapToDegree(single + delta)
    entity.attrDouble("min_temp")?.let { next = next.coerceAtLeast(it) }
    entity.attrDouble("max_temp")?.let { next = next.coerceAtMost(it) }
    return next
}

/** Camera tiles show the snapshot when one loads; otherwise the camera badge. */
@Composable
private fun CameraTileVisual(vm: HavenViewModel, entity: HaEntity, accent: Color) {
    // Cached snapshot: switching tabs back to a camera shows the last
    // snapshot immediately, refreshed silently in the background —
    // no blank tile + reload on every tab return (2026-10-29
    // verdict: elements "don't stay cached in the background").
    val snap = vm.snapshotFor(entity.entityId)
    LaunchedEffect(entity.entityId) {
        vm.refreshSnapshot(entity.entityId)
    }
    if (snap != null) {
        Image(
            snap.asImageBitmap(),
            contentDescription = entity.friendlyName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.large),
        )
    } else {
        Box(
            Modifier.size(52.dp).background(
                accent.copy(alpha = 0.18f),
                // Camera placeholder shape (2026-10-30 picker):
                // override beats the global default, else FLOWER.
                LocalShapePrefs.current.badgeShapeFor(entity.entityId, entity.domain)
                    ?: polygonShape(ExpressiveKind.FLOWER),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun MissingEntityTile(id: String, gridIndex: Int = 0) {
    Surface(
        shape = generalCardShape(gridIndex),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Entity $id is unavailable", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
