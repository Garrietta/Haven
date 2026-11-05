package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import com.garrietta.haven.ui.controls.SheetIconSquircleButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.CardSize
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.HomeStatusCategory
import com.garrietta.haven.ui.components.CardResizeHandle
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.resizeSettle
import com.garrietta.haven.data.deriveHomeStatus
import com.garrietta.haven.data.isHomeRoomEntity
import com.garrietta.haven.data.groupEntitiesByDevice
import com.garrietta.haven.data.deviceDisplayName
import com.garrietta.haven.data.deviceMatchesQuery
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.data.attachDeviceInfo
import com.garrietta.haven.data.semanticStateFor
import com.garrietta.haven.data.semanticVisual
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.DetailPopupTitleHeader

/**
 * Stage D (redesign): the Home dashboard's two new sections, kept out of
 * Screens.kt (already 1,597 lines) as their own file.
 *
 * Home Status derives ONLY from live states (see deriveHomeStatus).
 * UI rewrite round §7: tapping a flagged row opens a FULL-SCREEN
 * morph panel listing the group's entities (no more inline text
 * expansion inside the dashboard card); tapping an entity opens its
 * detail page, exactly as the inline rows did. Nothing here invents
 * a value, and unknown/unavailable entities are never flagged.
 *
 * Favorites renders the stored entity ids (in order) as tiles and
 * manages them through a bottom sheet: add from all room-assignable
 * entities, remove, and reorder with up/down. Unresolved ids still
 * appear in the sheet (as "Unknown entity") so a stale id can be
 * removed; the section itself renders only ids that resolve.
 */

// Tree-proven Filled icons only (Stage E owns semantic colors; Stage D
// uses neutral glyphs so no per-domain color mapping is half-built).
private fun statusIcon(category: HomeStatusCategory): ImageVector = when (category) {
    HomeStatusCategory.LOCKS -> Icons.Filled.Security
    HomeStatusCategory.DOORS -> Icons.Filled.Home
    HomeStatusCategory.LIGHTS -> Icons.Filled.Lightbulb
    HomeStatusCategory.MOTION -> Icons.Filled.Visibility
    HomeStatusCategory.ALARM -> Icons.Filled.Warning
    HomeStatusCategory.ENERGY -> Icons.Filled.Power
}

@Composable
fun HomeStatusSection(
    vm: HavenViewModel,
    states: Map<String, HaEntity>,
    navigate: (Screen) -> Unit,
) {
    val hiddenSections = vm.settingsStore.homeHiddenSections
    val items = deriveHomeStatus(
        states.values.toList(),
        vm.settingsStore.homeLightOverrides(),
    ).filter { "status:${it.category.key}" !in hiddenSections }
    val anyCategoryVisible = HomeStatusCategory.entries.any { "status:${it.key}" !in hiddenSections }

    Text("Home Status", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    if (items.isEmpty()) {
        Card(
            shape = generalCardShape(0),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (anyCategoryVisible) "All clear" else "Home Status items are hidden",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (anyCategoryVisible) "Nothing needs attention right now."
                    else "Show items again in Settings → Manage home page.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        // Expressive-menus round, item 6: Home Status cards join
        // the general shape, alternating by list position.
        for ((statusIndex, item) in items.withIndex()) {
            val key = item.category.key
            Card(
                shape = generalCardShape(statusIndex),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
                // UI rewrite round §7: the card opens the group's
                // full-screen panel — no inline expansion.
                onClick = { navigate(Screen.HomeStatusPanel(key)) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Stage E: the row's icon chip wears the
                        // centralized semantic visual for the row's
                        // first entity (armed alarms read blue-secure,
                        // triggered read red — the chip follows the
                        // entity, not just the category). Status text
                        // always accompanies it: color is never the
                        // only indicator.
                        val visual = semanticVisual(
                            semanticStateFor(item.entities[0]),
                            isDarkBackdrop(MaterialTheme.colorScheme.background),
                        )
                        Box(
                            Modifier.size(32.dp).background(visual.container, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                statusIcon(item.category),
                                contentDescription = item.category.label,
                                tint = visual.onContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    Spacer(Modifier.height(16.dp))
}

/**
 * UI rewrite round §7: a Home Status group's full-screen morph
 * panel (same machinery as the Round 3 panels). It lists the
 * group's entities with their real states (derivation stays
 * real-entities-only, as Stage D built it); tapping an entity opens
 * its own detail panel, exactly as the inline rows did — routing
 * unchanged, presentation only. A group that cleared since the tap
 * shows the all-clear line instead of a stale list.
 */
@Composable
fun HomeStatusPanelScreen(
    vm: HavenViewModel,
    categoryKey: String,
    navigate: (Screen) -> Unit,
    // Feedback round 5, item 1: when provided, panel card taps open the stacked panel-detail
    // layer above this panel instead of pushing EntityDetail (which used to evict this panel
    // overlay and re-render the panel as the base page — the "middleman" screen). Null/fallback
    // keeps the old navigate(EntityDetail) route.
    onPanelDetail: ((String) -> Unit)? = null,
) {
    val states by vm.states.collectAsState()
    val item = deriveHomeStatus(
        states.values.toList(),
        vm.settingsStore.homeLightOverrides(),
    ).firstOrNull { it.category.key == categoryKey }
    Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        DetailPopupTitleHeader(item?.title ?: "Home Status")
        if (item == null) {
            Text(
                "Nothing needs attention in this group right now.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        Text(
            item.detail,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        // Follow-up A (2026-11-03), refining feedback item 7:
        // the panel shows the ACTUAL home-page cards for its
        // entities — real HomeRoomCards, not a simplified tile
        // (HomeStatusTile is deleted) — placed underneath the
        // panel header, left-oriented and butted against the
        // side. A left-aligned flow, not stretched weights: each
        // card is (maxWidth − 12dp) / 2 wide with 12dp spacing, so
        // a lone card hugs the left edge and two cards sit
        // side-by-side against the side; full two-card rows
        // necessarily measure 50/50, but the split is not the
        // rule — the home card design is. Tap → detail exactly
        // as the old rows did (via the onOpenPanel override, so
        // no sheet ever opens under this panel). Plain Rows
        // (not a lazy grid): the panel content already scrolls
        // in PanelOverlay's wrapper, and a lazy grid inside it
        // cannot measure.
        // Feedback round 3, item 2 (video a3): the rows live in a
        // Column inside the BoxWithConstraints — NOT as direct
        // Box children. A Box stacks its children (it does not
        // flow them vertically), so the old loop rendered every
        // row on top of the first and the trailing Spacer added
        // no height: the popup measured one row tall, the second
        // card overlapped the first, and the cards ended flush
        // with the popup's bottom edge (zero daylight below them
        // — reading as clipping). The Column stacks the rows,
        // keeps the 12dp gaps, and adds 16dp of daylight below
        // the last card, so the popup sizes to its content and
        // both cards are fully visible. This also fixes the
        // latent >2-entity case, where rows 3+ would all pile
        // onto row 1. The 62%/75% CenteredPopup caps still apply
        // — scroll only when content genuinely exceeds the
        // screen — but the caps now yield to real content height
        // instead of a mis-measured single row.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 12.dp) / 2
            Column {
                for ((rowIndex, pair) in item.entities.chunked(2).withIndex()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for ((colIndex, entity) in pair.withIndex()) {
                            Box(Modifier.width(cardWidth)) {
                                HomeRoomCard(
                                    vm,
                                    entity.entityId,
                                    navigate,
                                    rowIndex * 2 + colIndex,
                                ) {
                                    // Feedback round 5, item 1: stacked panel detail, no
                                    // backstack push, no base-page switch. Back from the
                                    // detail returns to this panel popup.
                                    if (onPanelDetail != null) onPanelDetail(entity.entityId)
                                    else navigate(Screen.EntityDetail(entity.entityId))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun FavoritesSection(
    vm: HavenViewModel,
    states: Map<String, HaEntity>,
    navigate: (Screen) -> Unit,
    // Visual-fidelity round §2: the unfolded right pane measures
    // its own grid (2 columns only when the pane allows
    // >=175dp/card, else 1) and passes it in. UI rewrite round §3:
    // folded defaults to 2 columns (the old wide default of 4 is
    // gone — the right pane is locked to 2, never 3 or 4), and the
    // grid spacing is a parameter (12dp folded, 14dp unfolded).
    columnsOverride: Int? = null,
    spacing: Dp = 12.dp,
) {
    var managing by remember { mutableStateOf(false) }
    // Feedback round 20, item 5: Arrange mode shows the drag
    // handles; exiting keeps sizes (they live in prefs).
    var arrangeMode by remember { mutableStateOf(false) }
    val columns = columnsOverride ?: 2
    val resolved = vm.favorites.mapNotNull { states[it] }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Favorites", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        // Feedback round 20, item 5: Arrange toggles the resize
        // handles; the pencil stays always-composed (round 18).
        TextButton(onClick = { arrangeMode = !arrangeMode }) { Text(if (arrangeMode) "Done" else "Arrange") }
        // Visual-fidelity round §2: the Manage TEXT button is
        // replaced by the mockup's circular pencil button (40dp
        // surfaceContainerHigh circle, Outlined Edit icon) opening
        // the same sheet. Feedback round 18, item 3 (SUPERSEDES
        // round 17, item 6, which hid the pencil while a modal
        // was open): the pencil STAYS in composition always — the
        // round-17 comment claimed hiding causes no reflow because
        // the title carries weight(1f), but the grid DOES shift:
        // the Row height changes from the 40dp pencil to the text
        // height when hidden. Always composed = never shifts.
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { managing = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Manage favorites",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    if (resolved.isEmpty()) {
        Text(
            "No favorites yet — tap the pencil button to add the devices you reach for most.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        // UI rewrite round §3: Favorites use the SAME card as room
        // grids — the legacy EntityTile (with its wide inner box +
        // status dot) is gone from this path. Active or favorite
        // media players span the full width as 2x1 cards, exactly
        // like RoomSectionGrid; everything else chunks into the
        // standard 136dp grid. (Behavior change, reported: a
        // Favorites light no longer toggles on a body tap — the
        // pill switch and the badge zone toggle, the body opens
        // the detail panel, like every room card. EntityTile still
        // serves TabTileGrid user-tab pages, which render arbitrary
        // YAML cards rather than Home dashboard cards.)
        val wideMedia = resolved.filter { member ->
            member.domain == "media_player" &&
                (member.state == "playing" || member.entityId in vm.favorites)
        }
        val standard = resolved - wideMedia.toSet()
        for ((wideIndex, media) in wideMedia.withIndex()) {
            HomeRoomMediaCard(vm, media.entityId, gridIndex = wideIndex, navigate = navigate)
            Spacer(Modifier.height(spacing))
        }
        // Feedback round 20, item 5: wide-aware rows. A WIDE card
        // takes its own full-width row; consecutive non-wide
        // cards chunk as before. gridIndex still counts flat
        // position in the standard list, so a wide card never
        // shifts its neighbors' corner variants. With no wide
        // cards this reproduces the old chunked loop exactly.
        var flatIdx = 0
        var i = 0
        while (i < standard.size) {
            if (vm.cardSize("home", standard[i].entityId) == CardSize.WIDE) {
                val member = standard[i]
                Box(Modifier.fillMaxWidth().resizeSettle()) {
                    HomeRoomCard(vm, member.entityId, navigate, gridIndex = flatIdx)
                    if (arrangeMode) {
                        CardResizeHandle(
                            CardSize.WIDE,
                            { vm.setCardSize("home", member.entityId, it) },
                            Modifier.align(Alignment.BottomEnd),
                        )
                    }
                }
                Spacer(Modifier.height(spacing))
                i++
                flatIdx++
            } else {
                val row = mutableListOf<HaEntity>()
                val rowStart = flatIdx
                while (i < standard.size && row.size < columns &&
                    vm.cardSize("home", standard[i].entityId) != CardSize.WIDE
                ) {
                    row.add(standard[i])
                    i++
                    flatIdx++
                }
                Row {
                    for ((colIdx, member) in row.withIndex()) {
                        val size = vm.cardSize("home", member.entityId)
                        Box(
                            Modifier.weight(1f).resizeSettle()
                                .then(if (size == CardSize.TALL) Modifier.heightIn(min = 272.dp) else Modifier),
                        ) {
                            HomeRoomCard(
                                vm, member.entityId, navigate, gridIndex = rowStart + colIdx,
                                modifier = if (size == CardSize.TALL) Modifier.fillMaxSize() else Modifier,
                            )
                            if (arrangeMode) {
                                CardResizeHandle(
                                    size,
                                    { vm.setCardSize("home", member.entityId, it) },
                                    Modifier.align(Alignment.BottomEnd),
                                )
                            }
                        }
                        if (member != row.last()) Spacer(Modifier.width(spacing))
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.width(spacing))
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(spacing))
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    if (managing) {
        FavoritesSheet(vm, states, onDismiss = { managing = false })
    }
}

/**
 * Favorites management: the stored order with up/down/remove, plus every
 * room-assignable entity not yet favorited with an Add button. Stored ids
 * that no longer resolve still list (as "Unknown entity") so they can be
 * removed instead of lingering invisibly.
 */
/**
 * Stage G: public so HomeEditor can offer management even when the
 * Favorites section itself is hidden — otherwise hiding the section
 * would strand the management UI with it.
 */
/**
 * Favorites search match (expressive-menus round, item 2): a blank
 * query matches everything; otherwise the friendly name matches by
 * substring, falling back to the entity id (so "lamp_socket" finds
 * the Lamp even if its name were different). Case-insensitive.
 * Pure, unit-tested.
 */
internal fun favoriteMatchesQuery(query: String, name: String, entityId: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return name.lowercase().contains(q) || entityId.lowercase().contains(q)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    vm: HavenViewModel,
    states: Map<String, HaEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val deviceInfo by vm.deviceInfo.collectAsState()
    val entityDevices by vm.entityDevices.collectAsState()
    val sheetCustom = rememberCardCustom(vm)
    var expandedDevice by remember { mutableStateOf<String?>(null) }
    var ungroupedExpanded by remember { mutableStateOf(false) }
    // Expressive-menus round, item 2: search narrows BOTH lists —
    // the long "Add devices" list is the painful one, but the
    // favorites list gets the same filter so one query searches
    // the whole sheet. Favorites themselves are untouched.
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Favorites", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search devices") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") } }
                } else null,
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text("Your favorites", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (vm.favorites.isEmpty()) {
                Text("Nothing here yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val visibleFavorites = vm.favorites.filter { id ->
                val entity = states[id]
                favoriteMatchesQuery(query, entity?.friendlyName ?: "Unknown entity: $id", id)
            }
            if (query.isNotBlank() && visibleFavorites.isEmpty() && vm.favorites.isNotEmpty()) {
                Text("No favorites match \"$query\".", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            for (id in visibleFavorites) {
                val entity = states[id]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (entity != null) customTitleFor(sheetCustom.titles, id, entity.friendlyName) else "Unknown entity: $id",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // Expressive-menus round: 44dp squircle buttons
                    // (was bare IconButtons) — same taps, larger targets.
                    SheetIconSquircleButton(Icons.Filled.ArrowUpward, "Move favorite up", onClick = { vm.moveFavorite(id, -1) })
                    Spacer(Modifier.width(4.dp))
                    SheetIconSquircleButton(Icons.Filled.ArrowDownward, "Move favorite down", onClick = { vm.moveFavorite(id, 1) })
                    Spacer(Modifier.width(4.dp))
                    SheetIconSquircleButton(Icons.Filled.Delete, "Remove favorite", onClick = { vm.removeFavorite(id) })
                }
                Spacer(Modifier.height(4.dp))
            }
            // Feedback round, item 6: the standalone "Add devices"
            // list is DELIBERATELY GONE (the user: "I don't even
            // want this individual add devices page") — the
            // Devices section below (search + tap-to-expand
            // dropdown) is the only add path, plus the Ungrouped
            // row after it for entities with no device link.
            // Add/remove/reorder behavior is unchanged.
            // Customization round, item 3: DEVICE-level results
            // alongside entity results. A device row expands inline
            // to its real entities (each with an Add button, and a
            // tap opens that entity's popup) — never a dead row, and
            // never a fake device switch.
            Spacer(Modifier.height(16.dp))
            Text("Devices", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            val deviceGroups = attachDeviceInfo(
                groupEntitiesByDevice(states.values.toList(), entityDevices), deviceInfo,
            ).filter { deviceMatchesQuery(query, it, states) }
                .sortedBy { (it.info?.name ?: it.deviceId).lowercase() }
            if (deviceGroups.isEmpty()) {
                Text(
                    if (query.isNotBlank()) "No devices match \"$query\"." else "No devices found yet — devices appear once the registries load.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (group in deviceGroups) {
                val groupName = deviceDisplayName(group)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { expandedDevice = if (expandedDevice == group.deviceId) null else group.deviceId }) {
                        Text(groupName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(group.info?.manufacturer, group.info?.model).joinToString(" · ").ifEmpty { "${group.entityIds.size} entities" },
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    SheetIconSquircleButton(
                        if (expandedDevice == group.deviceId) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        if (expandedDevice == group.deviceId) "Collapse device" else "Expand device",
                        onClick = { expandedDevice = if (expandedDevice == group.deviceId) null else group.deviceId },
                    )
                }
                if (expandedDevice == group.deviceId) {
                    val groupEntities = group.entityIds.mapNotNull { states[it] }.sortedBy { it.friendlyName }
                    for (member in groupEntities) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                customTitleFor(sheetCustom.titles, member.entityId, member.friendlyName),
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).clickable {
                                    onDismiss()
                                    vm.openPopup(member.entityId)
                                },
                            )
                            if (member.entityId !in vm.favorites) {
                                SheetIconSquircleButton(Icons.Filled.Add, "Add favorite", onClick = { vm.addFavorite(member.entityId) })
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            // Feedback round, item 6 (companion to the removed
            // "Add devices" list) + item 8: entities with NO
            // registry device link — including the ex-"Device
            // null" group, whose literal-"null" device ids are
            // now treated as absent — keep their only add path
            // as ONE expandable row in this same search +
            // tap-to-expand pattern, not a separate page. Hidden
            // entirely when every room entity belongs to a device.
            val allGroupedIds = groupEntitiesByDevice(states.values.toList(), entityDevices)
                .flatMap { it.entityIds }.toSet()
            val ungrouped = states.values
                .filter { isHomeRoomEntity(it) && it.entityId !in vm.favorites && it.entityId !in allGroupedIds && favoriteMatchesQuery(query, it.friendlyName, it.entityId) }
                .sortedBy { it.friendlyName }
            if (ungrouped.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { ungroupedExpanded = !ungroupedExpanded }) {
                        Text("Ungrouped entities", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${ungrouped.size} entities",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    SheetIconSquircleButton(
                        if (ungroupedExpanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        if (ungroupedExpanded) "Collapse ungrouped entities" else "Expand ungrouped entities",
                        onClick = { ungroupedExpanded = !ungroupedExpanded },
                    )
                }
                if (ungroupedExpanded) {
                    for (member in ungrouped) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                customTitleFor(sheetCustom.titles, member.entityId, member.friendlyName),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).clickable {
                                    onDismiss()
                                    vm.openPopup(member.entityId)
                                },
                            )
                            SheetIconSquircleButton(Icons.Filled.Add, "Add favorite", onClick = { vm.addFavorite(member.entityId) })
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
