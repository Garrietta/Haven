package com.garrietta.haven.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.lightContainerColor
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.components.hideWhenMorphSource
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
        contentPadding = PaddingValues(bottom = 16.dp),
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
                                onThermostat = navigate?.let { nav ->
                                    { entityId: String -> nav(Screen.Thermostat(entityId)) }
                                },
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

/** Tap routing: full page for every domain (thermostat page, live camera, per-domain page). */
private fun openPage(vm: HavenViewModel, entity: HaEntity, navigate: ((Screen) -> Unit)?, onThermostat: ((String) -> Unit)? = null) {
    when (entity.domain) {
        "climate" -> if (onThermostat != null) onThermostat(entity.entityId) else vm.openPopup(entity.entityId)
        "camera" -> vm.openCameraFullscreen(entity.entityId)
        else -> if (navigate != null) navigate(Screen.EntityDetail(entity.entityId)) else vm.openPopup(entity.entityId)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityTile(vm: HavenViewModel, entity: HaEntity, navigate: ((Screen) -> Unit)? = null, onThermostat: ((String) -> Unit)? = null) {
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
        shape = RoundedCornerShape(28.dp),
        color = if (entity.domain == "light" && entity.state == "on") {
            // Lit tiles tint across secondary -> tertiary by color temp.
            lightContainerColor(MaterialTheme.colorScheme, entity.attrInt("color_temp_kelvin"), true)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth()
            .onGloballyPositioned { tileBounds = it.boundsInWindow() }
            .reportCardBounds(entity.entityId)
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .hideWhenMorphSource(entity.entityId)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    tileBounds?.let { vm.pageOrigin = it }
                    openPage(vm, entity, navigate, onThermostat)
                },
                onLongClick = { vm.openPopup(entity.entityId) },
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (entity.domain == "camera") {
                CameraTileVisual(vm, entity, accent)
            } else {
                Box(
                    Modifier.size(52.dp).background(
                        if (active) accent else accent.copy(alpha = 0.18f),
                        polygonShape(shapeForDomain(entity.domain)),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        domainIcon(entity),
                        contentDescription = null,
                        tint = if (active) Color.White else accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(entity.friendlyName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2)
            if (entity.domain == "climate") {
                ClimateTileBody(vm, entity)
            } else {
                Text(
                    tileStatus(entity),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

// Heat/cool colors match the thermostat page's dial colors.
private val TILE_HEAT_COLOR = Color(0xFFD84315)
private val TILE_COOL_COLOR = Color(0xFF1565C0)

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
                    TILE_HEAT_COLOR
                } else {
                    TILE_HEAT_COLOR.copy(alpha = 0.45f)
                },
            )
            Spacer(Modifier.width(10.dp))
            Text(
                high!!.formatTemp(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (selector == SetpointSelector.COOLING) {
                    TILE_COOL_COLOR
                } else {
                    TILE_COOL_COLOR.copy(alpha = 0.45f)
                },
            )
        }
    } else if (single != null) {
        val singleColor = when (entity.state) {
            "heat" -> TILE_HEAT_COLOR
            "cool" -> TILE_COOL_COLOR
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
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(entity.entityId) {
        bitmap = vm.client.fetchCameraBitmap(entity.entityId)
    }
    val snap = bitmap
    if (snap != null) {
        Image(
            snap.asImageBitmap(),
            contentDescription = entity.friendlyName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(20.dp)),
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
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Entity $id is unavailable", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
