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
import androidx.compose.foundation.layout.heightIn
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
import com.garrietta.haven.data.CardSize
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.FRONT_DOOR_LIVE_VIEW_ID
import com.garrietta.haven.data.VACUUM_ID
import com.garrietta.haven.data.displayCameraName
import com.garrietta.haven.data.parseCardYaml
import com.garrietta.haven.ui.components.LocalShapePrefs
import com.garrietta.haven.ui.components.CardResizeHandle
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.resizeSettle
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
 * Tab pages render the ONE tile style (round 21, item 6): the same
 * [HomeRoomCard]/[HomeRoomMediaCard] as Home/room pages. The old
 * [EntityTile] is deleted — tab pages used to render its 64dp badge,
 * 15sp title, visible binary sub-box, and fullscreen camera routing,
 * all different from Home cards. Boards, Today, and Areas keep their
 * full cards; a tab page shows one tile per entity instead, in an
 * adaptive grid (2 columns folded, more unfolded). Tap opens the
 * entity's full page — thermostat popout for climate, vacuum panel
 * for vacuum.t2292, detail page for everything else (cameras open
 * the detail page like every Home card now; its Open live view
 * button reaches the viewer). Long-press enters Arrange/resize
 * mode (round 21, item 7), never a settings sheet.
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
    navigate: (Screen) -> Unit,
    // Feedback round 20, item 5: Arrange mode + resize, BEFORE
    // the trailing fallbackCard lambda (round-5 lesson: new
    // params never go after a lambda param).
    arrangeMode: Boolean = false,
    sizeFor: ((String) -> CardSize)? = null,
    onSizeChange: ((String, CardSize) -> Unit)? = null,
    // Round 21, item 7: long-press on any tab tile enters
    // Arrange mode (the Arrange button on UserTabScreen is gone).
    onArrangeToggle: (() -> Unit)? = null,
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
                        // Round 21, item 6: ONE tile style — tab
                        // pages render the current HomeRoomCard, not
                        // the old EntityTile (deleted below). Media
                        // players that are playing or favorited span
                        // the full line as HomeRoomMediaCard, exactly
                        // like RoomSectionGrid's wide-media rule;
                        // every other entity is a standard card.
                        // Camera tiles open the detail page like
                        // every Home card now; the detail page's Open
                        // live view button reaches the viewer, and
                        // its Event history row reaches the clips
                        // timeline (round 21, items 5 + 7).
                        val isWideMedia = entity.domain == "media_player" &&
                            (entity.state == "playing" || entity.entityId in vm.favorites)
                        val cardSize = sizeFor?.invoke(id) ?: CardSize.NORMAL
                        item(
                            key = id,
                            span = {
                                if (cardSize == CardSize.WIDE || isWideMedia) GridItemSpan(maxLineSpan)
                                else GridItemSpan(1)
                            },
                        ) {
                            Box(
                                Modifier.resizeSettle()
                                    .then(if (cardSize == CardSize.TALL) Modifier.heightIn(min = 272.dp) else Modifier),
                            ) {
                                if (isWideMedia) {
                                    HomeRoomMediaCard(
                                        vm, entity.entityId, gridIndex = tileIndex++,
                                        arrangeMode = arrangeMode,
                                        onArrangeToggle = onArrangeToggle,
                                        modifier = if (cardSize == CardSize.TALL) Modifier.fillMaxSize() else Modifier,
                                        navigate = navigate,
                                    )
                                } else {
                                    HomeRoomCard(
                                        vm, entity.entityId, navigate, gridIndex = tileIndex++,
                                        modifier = if (cardSize == CardSize.TALL) Modifier.fillMaxSize() else Modifier,
                                        arrangeMode = arrangeMode,
                                        onArrangeToggle = onArrangeToggle,
                                    )
                                }
                                if (arrangeMode && onSizeChange != null) {
                                    CardResizeHandle(
                                        cardSize,
                                        { onSizeChange(id, it) },
                                        Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            }
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

/**
 * Stream 4 (design-system sprint): lit light tiles show "On · N%",
 * converting HA's 0–255 brightness to a percentage. Restored in
 * round 21, item 6: the old EntityTile's body was deleted with it,
 * but the conversion is still the tile status text's source (see
 * homeCardStatus in Screens.kt) and its unit test still covers it.
 */
internal fun lightBrightnessPercent(brightness: Int): Int = brightness * 100 / 255

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
