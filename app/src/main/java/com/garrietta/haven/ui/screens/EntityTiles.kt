package com.garrietta.haven.ui.screens
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
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.badgeColors
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.controls.shapeTapAction
import com.garrietta.haven.ui.components.hideBodyWhenMorphSource
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.hideHeroWhenMorphSource
import com.garrietta.haven.ui.components.reportHeroBounds
import com.garrietta.haven.ui.components.pillClearanceBottom
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.controls.ExpressiveStepper
import com.garrietta.haven.ui.controls.applyClimateSetpoint
import com.garrietta.haven.ui.controls.domainIcon
import com.garrietta.haven.ui.controls.formatTemp
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
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
        modifier = Modifier.fillMaxSize(),
    ) {
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
                item(span = { GridItemSpan(maxLineSpan) }) { fallbackCard(yaml) }
            } else {
                for (id in ids) {
                    val entity = states[id]
                    if (entity != null) {
                        item {
                            EntityTile(
                                vm,
                                entity,
                                navigate = navigate,
                            )
                        }
                    } else {
                        item { MissingEntityTile(id) }
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
private fun tileStatus(entity: HaEntity): String = when (entity.domain) {
    "light", "switch", "input_boolean" -> if (entity.state == "on") "On" else "Off"
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

/** Tap routing: climate opens the centered expressive pop-out, cameras
 * open fullscreen live, everything else opens its full page (or the
 * sheet when there is no navigator, e.g. Boards cards). */
private fun openPage(vm: HavenViewModel, entity: HaEntity, navigate: ((Screen) -> Unit)?) {
    when (entity.domain) {
        "climate" -> vm.openClimatePopout(entity.entityId)
        "camera" -> vm.openCameraFullscreen(entity.entityId)
        else -> if (navigate != null) navigate(Screen.EntityDetail(entity.entityId)) else vm.openPopup(entity.entityId)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityTile(vm: HavenViewModel, entity: HaEntity, navigate: ((Screen) -> Unit)? = null) {
    val accent = accentFor(entity.entityId)
    val active = ((entity.domain == "light" || entity.domain == "switch" || entity.domain == "input_boolean") &&
        entity.state == "on") || (entity.domain == "cover" && entity.state == "open")
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
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = when {
            // Lit tiles tint by kelvin AND brightness: a dim light reads
            // dimmer than a bright one, never full-tint at 10%.
            entity.domain == "light" && entity.state == "on" ->
                lightBrightnessContainer(
                    MaterialTheme.colorScheme,
                    entity.attrInt("color_temp_kelvin"),
                    entity.attrInt("brightness")?.let { it * 100 / 255 },
                    true,
                )
            // Climate tiles carry their mode color at low alpha — the
            // mode is visible on the tab grid, not just on the page.
            entity.domain == "climate" ->
                lerp(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    climateModeColor(entity.state, entity.attrString("hvac_action")),
                    0.15f,
                )
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth()
            .onGloballyPositioned { tileBounds = it.boundsInWindow() }
            .reportCardBounds(entity.entityId)
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
                onClick = {
                    tileBounds?.let { vm.pageOrigin = it }
                    openPage(vm, entity, navigate)
                },
                // Long-press on a camera opens its recorded-clips
                // timeline (full page); every other domain keeps the
                // quick sheet. Without a navigator (Boards cards) the
                // sheet is still the fallback.
                onLongClick = {
                    if (entity.domain == "camera" && navigate != null) navigate(Screen.CameraClips(entity.entityId))
                    else vm.openPopup(entity.entityId)
                },
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
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
                val badgeColor = if (entity.domain == "climate") {
                    climateModeColor(entity.state, entity.attrString("hvac_action"))
                } else accent
                val badgeActive = active || (entity.domain == "climate" &&
                    entity.state != "off" && entity.attrString("hvac_action") != "idle")
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
                // Off-state grey (2026-10-29 verdict: off icons grey
                // everywhere, tiles matching pop-ups): when the tile
                // is not active, domains with an off state take the
                // pop-up's badgeColors pair verbatim (off light =
                // surfaceContainerHighest + onSurfaceVariant, the
                // same grey the pop-up header already used) instead
                // of a full-strength accent glyph on a faint wash.
                // On-state tiles keep their full color + white glyph
                // exactly; covers keep their position rendering
                // (closed is a position, not power) and domains
                // without an off state keep the old wash.
                val greyOffDomains = setOf(
                    "light", "switch", "input_boolean", "fan",
                    "media_player", "binary_sensor", "climate",
                )
                val (offBadgeBg, offBadgeGlyph) = badgeColors(
                    MaterialTheme.colorScheme, entity, accent,
                )
                val badgeBg = if (badgeActive) badgeColor
                    else if (entity.domain in greyOffDomains) offBadgeBg
                    else badgeColor.copy(alpha = 0.18f)
                val badgeGlyph = if (badgeActive) Color.White
                    else if (entity.domain in greyOffDomains) offBadgeGlyph
                    else badgeColor
                Box(
                    Modifier.size(64.dp).background(
                        badgeBg,
                        polygonShape(shapeForDomain(entity.domain)),
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
                        domainIcon(entity),
                        contentDescription = null,
                        tint = badgeGlyph,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                entity.friendlyName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Traveling header: the tile's name hides with its
                // badge while the morph is open, so the overlay's
                // single traveling header is the only copy on screen.
                modifier = if (entity.domain != "camera") Modifier.hideHeroWhenMorphSource(entity.entityId) else Modifier,
            )
            if (entity.domain == "climate") {
                ClimateTileBody(vm, entity)
            } else {
                Text(
                    tileStatus(entity),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = if (entity.domain != "camera") Modifier.hideHeroWhenMorphSource(entity.entityId) else Modifier,
                )
            }
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
    Spacer(Modifier.height(8.dp))
    if (heatCool) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                low!!.formatTemp(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (selector == SetpointSelector.HEATING) {
                    HeatOrange
                } else {
                    HeatOrange.copy(alpha = 0.45f)
                },
            )
            Spacer(Modifier.width(10.dp))
            Text(
                high!!.formatTemp(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (selector == SetpointSelector.COOLING) {
                    CoolBlue
                } else {
                    CoolBlue.copy(alpha = 0.45f)
                },
            )
        }
    } else if (single != null) {
        val singleColor = when (entity.state) {
            "heat" -> HeatOrange
            "cool" -> CoolBlue
            else -> accentFor(entity.entityId)
        }
        Text(single.formatTemp(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = singleColor)
    }
    if (current != null) {
        Text(
            "Indoor ${current.formatTemp()}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                polygonShape(ExpressiveKind.FLOWER),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun MissingEntityTile(id: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Entity $id is unavailable", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
