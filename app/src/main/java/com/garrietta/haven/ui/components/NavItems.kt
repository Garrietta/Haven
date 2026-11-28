package com.garrietta.haven.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garrietta.haven.Screen
import com.garrietta.haven.data.UserTab
import com.garrietta.haven.data.parseCardYaml

/**
 * Stage C (redesign): the navigation model as pure, unit-testable
 * functions. Previously these lived as private functions in HavenApp.kt;
 * they move here unchanged in behavior (plus hidden-tab filtering and
 * icon overrides) so the Settings-reachability invariant and the
 * hidden/visible index math are pinned by tests instead of by eye.
 */

/** Feedback round 15, item 3: the bar holds at most two pinned custom tabs (six tabs total). */
const val MAX_PINNED_TABS = 2

/** One pill-bar / side-rail destination. */
 data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
)

/**
 * Feedback round 15, item 3 — SUPERSEDES the 2026-11-03 four-destination
 * lock: Home and Settings are LOCKED and always present (first and
 * last, so the user can never strand themselves on a screen with no
 * way back to Settings), but Scenes and Energy are now hideable
 * ([showScenes]/[showEnergy] from the Settings store, both on by
 * default). Pinned tabs sit between Energy and Settings. A screen
 * that is not in the bar shows the bar/rail with nothing selected
 * (same as Boards/Areas/Search), except Settings submenus, which
 * map back to Settings via [navActiveScreen] (feedback round 15,
 * item 4).
 */
/**
 * Feedback round 15, item 3, EXTENDED round 19, item 5: the bar/rail
 * model. Home first, Settings last, Scenes/Energy hideable, and
 * up to [MAX_PINNED_TABS] pinned extras between Energy and
 * Settings — where an "extra" is now EITHER a pinned user tab OR
 * a room pinned as a tab. The caller passes ALL pinned tabs and
 * ALL nav-tab rooms (already filtered to their placement); this
 * function caps the COMBINED extras to two, user tabs first so
 * existing bars never reorder when a room is pinned. The 6-total
 * cap holds by construction (Home + Scenes + Energy + 2 extras +
 * Settings). A screen that is not in the bar shows the bar/rail
 * with nothing selected (same as Boards/Areas/Search), except
 * Settings submenus, which map back to Settings via
 * [navActiveScreen] (feedback round 15, item 4).
 */
data class RoomBarItem(
    val areaId: String,
    val title: String,
    val iconKey: String?,
)

fun pillBarItems(
    showScenes: Boolean = true,
    showEnergy: Boolean = true,
    pinnedTabs: List<UserTab> = emptyList(),
    roomTabs: List<RoomBarItem> = emptyList(),
): List<NavItem> {
    val items = mutableListOf(
        NavItem(Screen.Today, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    )
    if (showScenes) items += NavItem(Screen.Scenes, "Scenes", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    if (showEnergy) items += NavItem(Screen.Energy, "Energy", Icons.Filled.Bolt, Icons.Outlined.Bolt)
    val extras = mutableListOf<NavItem>()
    for (tab in pinnedTabs) {
        val (icon, outlined) = tabIcons(tab)
        extras += NavItem(Screen.UserTab(tab.id), tab.title, icon, outlined)
    }
    for (room in roomTabs) {
        val (icon, outlined) = room.iconKey?.let { tabIconPairForKey(it) }
            ?: (Icons.Filled.GridView to Icons.Outlined.GridView)
        extras += NavItem(Screen.RoomTab(room.areaId), room.title, icon, outlined)
    }
    items += extras.take(MAX_PINNED_TABS)
    items += NavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    return items
}

/**
 * Feedback round 15, item 4: the rail/bar active screen for a
 * destination. Settings-owned submenus — the editors and builders
 * reached from Settings — map back to Settings, so the Settings
 * rail icon keeps its active highlight while the user is inside
 * any Settings submenu (the 2026-11-04 video showed it dropping
 * off). Feedback round 17, item 3, EXTENDS the mapping to the
 * Browse destinations reached from Settings (Dashboards, Areas,
 * Search, UserTabs): opening Manage home page or Rooms & areas
 * cleared the green indicator entirely on the 2026-11-04 runs,
 * and those pages are Settings-owned too. Only DashboardEdit,
 * AreaDetail, and CameraClips keep the nothing-selected rule
 * (true detail pages, not Settings submenus). Every other screen
 * maps to itself.
 */
fun navActiveScreen(current: Screen): Screen = when (current) {
    Screen.HomeEditor,
    Screen.RoomsTabs,
    Screen.Dashboards,
    Screen.Areas,
    Screen.Search,
    Screen.UserTabs,
    Screen.AutomationBuilder,
    Screen.SceneBuilder,
    Screen.ScriptBuilder,
    Screen.HelperBuilder,
    Screen.BuilderHub,
    Screen.UpdatesPanel,
    -> Screen.Settings
    else -> current
}

/**
 * Feedback round 18, item 6: the rail/pill tap-back rule, pure so
 * it is unit-testable. Tapping a bar icon navigates whenever the
 * tapped tab's screen differs from the ACTUAL current screen —
 * not the highlighted index. On a sub-screen (Manage home page,
 * Rooms & areas) the highlighted tab is Settings while current
 * is the sub-screen, so tapping the highlighted Settings icon
 * navigates back to Settings instead of being swallowed by the
 * old index-equality guard. Same-tab taps still no-op.
 */
internal fun shouldNavigateOnTabTap(tapped: Screen, current: Screen): Boolean = tapped != current

/**
 * Feedback round 19, item 2: the non-tab slide direction, pure so
 * it is unit tested. Pushing a sub-page (Settings -> UserTabs,
 * Manage home page, Rooms & tabs) slides the new page in from the
 * right and the old page out to the left — direction +1, the same
 * offsets as [tabSlideOffsets](1). Popping (back to Settings or a
 * parent) reverses it: the outgoing sub-page exits to the right
 * and the parent enters from the left — direction -1. The caller
 * (HavenApp) tracks push vs pop from backstack mutations; this
 * function is only the direction mapping, so the transition math
 * stays in one tested place.
 */
fun nonTabSlideDirection(isPop: Boolean): Int = if (isPop) -1 else 1

/**
 * Feedback round 19, item 3: the folded nav clearance gate, pure
 * so it is unit tested. Folded content ALWAYS reserves the
 * navigation-bars inset at the bottom (matching the unfolded
 * clearance rule) — the old isTopLevel gate dropped the reserve
 * the moment a detail panel left the top level, so opening a
 * panel at the bottom of the Home list changed contentPadding
 * and jumped the scroll offset by exactly the inset.
 */
fun shouldApplyFoldedNavClearance(isWideLayout: Boolean): Boolean = !isWideLayout

/**
 * Feedback round 19, item 5: the combined bar-extra cap, pure so
 * it is unit tested. A room or tab may pin to the bar only when
 * it is already there (re-pinning is free) or when pinning it
 * keeps the combined count of pinned user tabs plus nav-tab
 * rooms at or below [MAX_PINNED_TABS] — the 2-cap and the
 * 6-total survive rooms joining the bar. The UI disables the
 * Nav-tab option at the cap; the ViewModel setters are the
 * backstop.
 */
fun canPinBarExtra(pinnedTabCount: Int, roomNavTabCount: Int, alreadyOnBar: Boolean): Boolean =
    alreadyOnBar || pinnedTabCount + roomNavTabCount + 1 <= MAX_PINNED_TABS
fun tabSlideDirection(barScreens: List<Screen>, from: Screen, to: Screen): Int? {
    val fromRank = barScreens.indexOf(from)
    val toRank = barScreens.indexOf(to)
    if (fromRank < 0 || toRank < 0 || fromRank == toRank) return null
    return if (toRank > fromRank) 1 else -1
}

/**
 * Feedback round 3, item 1 (video ba): full-travel tab-slide
 * offsets, pure so they are unit-testable. Returns (enter, exit)
 * pixel offsets for a page of [contentWidthPx], given the tab
 * [direction] from [tabSlideDirection] and the [railWidthPx] the
 * outgoing page must also clear on unfolded (84dp; 0 folded).
 * Forward (direction 1): the new page enters from the right edge
 * (fully off-screen at start) while the old page exits fully past
 * the left edge — content width PLUS rail width, so on unfolded it
 * clears the left screen edge, not just the content boundary.
 * Backward (direction -1): mirrored. Any other direction value
 * (including null's caller-side fallback) behaves as forward.
 * The fade and the non-tab fixed slide are untouched; this helper
 * covers the tab branches only.
 */
fun tabSlideOffsets(direction: Int?, contentWidthPx: Int, railWidthPx: Int): Pair<Int, Int> {
    return when (direction) {
        -1 -> (-(contentWidthPx + railWidthPx)) to contentWidthPx
        else -> contentWidthPx to (-(contentWidthPx + railWidthPx))
    }
}

/** One choosable pill icon: key (stored), label (picker), icon pair. */
 data class TabIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
)

/**
 * The icon picker set. Every pair is verified to exist in
 * material-icons-extended 1.7.8 (both Filled and Outlined); "auto" is
 * not an option here — it is the null icon key, resolved by [tabIcons].
 * Settings is deliberately absent: the Settings tab's icon is fixed.
 */
 val TAB_ICON_OPTIONS: List<TabIconOption> = listOf(
    TabIconOption("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TabIconOption("video", "Video camera", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    TabIconOption("climate", "Climate", Icons.Filled.Thermostat, Icons.Outlined.Thermostat),
    TabIconOption("light", "Light", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
    TabIconOption("grid", "Grid", Icons.Filled.GridView, Icons.Outlined.GridView),
    TabIconOption("tv", "TV", Icons.Filled.Tv, Icons.Outlined.Tv),
    TabIconOption("music", "Music", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    TabIconOption("speaker", "Speaker", Icons.Filled.Speaker, Icons.Outlined.Speaker),
    TabIconOption("bolt", "Energy", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    TabIconOption("photo", "Photo", Icons.Filled.Camera, Icons.Outlined.Camera),
    TabIconOption("garage", "Garage", Icons.Filled.Garage, Icons.Outlined.Garage),
)

/** The stored icon key's pair, or null for auto (null/unknown key). */
 fun tabIconPairForKey(key: String?): Pair<ImageVector, ImageVector>? =
    TAB_ICON_OPTIONS.firstOrNull { it.key == key }?.let { it.icon to it.outlinedIcon }

/**
 * Per-tab pill icons: an explicit picker choice wins; otherwise the
 * first card's entity domain wins (camera, climate, light, cover);
 * title keywords cover entity-less tabs; GridView is the fallback.
 * Only domains with a matching outlined variant get a pair, so the
 * selected pill can still fill its icon like Home and Settings do.
 */
 fun tabIcons(tab: UserTab): Pair<ImageVector, ImageVector> {
    tabIconPairForKey(tab.icon)?.let { return it }
    val domain = tab.cards.firstNotNullOfOrNull { yaml ->
        try {
            parseCardYaml(yaml).optString("entity").ifEmpty { null }?.substringBefore(".")
        } catch (e: Exception) {
            null
        }
    } ?: when {
        tab.title.contains("camera", ignoreCase = true) -> "camera"
        tab.title.contains("thermostat", ignoreCase = true) ||
            tab.title.contains("climate", ignoreCase = true) -> "climate"
        tab.title.contains("garage", ignoreCase = true) -> "cover"
        tab.title.contains("light", ignoreCase = true) ||
            tab.title.contains("lamp", ignoreCase = true) -> "light"
        else -> null
    }
    return when (domain) {
        "camera" -> Icons.Filled.Videocam to Icons.Outlined.Videocam
        "climate" -> Icons.Filled.Thermostat to Icons.Outlined.Thermostat
        "light" -> Icons.Filled.Lightbulb to Icons.Outlined.Lightbulb
        "cover" -> Icons.Filled.Home to Icons.Outlined.Home
        else -> Icons.Filled.GridView to Icons.Outlined.GridView
    }
}

/**
 * Shared nav selection for the pill bar and the vertical rail: the
 * index of the current screen in [barItems], or null when it is not
 * in the bar (a legacy screen, or a hidden tab). A general search —
 * not per-screen branches — so fixed destinations (Scenes, Energy),
 * user tabs, and Settings all resolve by the same rule, and hidden
 * tabs before the current tab cannot shift the index. Both chrome
 * variants read this one function, so they can never disagree about
 * which tab is selected.
 */
 fun selectedNavIndex(
    current: Screen,
    barItems: List<NavItem>,
): Int? = barItems.indexOfFirst { it.screen == current }.takeIf { it >= 0 }

/**
 * Feedback round 22, item 6: the unfolded rail's green indicator
 * NEVER disappears. When the current screen is not a bar tab (a
 * popup, detail panel, or sub-screen is open), [selectedNavIndex]
 * returns null — and the old call site passed that null straight
 * through, so the capsule vanished (the 2026-11-05 runs showed
 * no capsule on any icon while a popup was open and after it
 * closed). The fallback is the last on-tab screen (never null —
 * it starts at Home), resolved fresh against the current bar
 * list; if even that screen is no longer in the bar, index 0
 * (Home) is the final fallback. Pure, unit-tested.
 */
fun railSelectedIndexWithFallback(
    onTabIndex: Int?,
    lastRailTabScreen: Screen,
    barItems: List<NavItem>,
): Int = onTabIndex ?: selectedNavIndex(lastRailTabScreen, barItems) ?: 0

/**
 * Feedback round 17, item 3: the unfolded rail indicator's top
 * offset from its item index — pure, unit-tested. The rail column
 * is 12dp top padding + 48dp rows (no spacedBy, so itemSpacing is
 * 0) and the indicator pill is 32dp tall centered on the 48dp
 * row ((48 − 32) / 2 = 8dp), so the pill top for index i is
 * 12 + i × 48 + 8 = 20 + i × 48 dp. This REPLACES the old
 * boundsInWindow measurement: measured icon rects went stale
 * across bar-list changes and the pill-enter translation, which
 * cleared the indicator or parked it on the wrong tab (the
 * 2026-11-04 runs showed Home displayed with the highlight off
 * Home, and no highlight at all on Rooms & areas). Index math
 * cannot go stale: it is exact whenever the column is not
 * scrolling, which the six-tab cap guarantees (12 + 6 × 48 + 12
 * = 312dp of content, far below any unfolded viewport).
 */
fun railIndicatorTop(index: Int): Dp = 20.dp + 48.dp * index

/**
 * Feedback round 17, item 2: the card/tile tap-routing decision
 * table — pure, unit-tested, shared by HomeRoomCard on every
 * surface (Home, rooms, Favorites, and user tabs since round 21,
 * item 6 unified the tile). Climate opens its dial popout; the
 * eufy vacuum (the single vacuum panel, [VACUUM_ID]) opens the
 * vacuum panel; every other controllable domain opens its full
 * detail page — cameras included (they open the detail page like
 * every Home card now; the detail page's Open live view button
 * reaches the viewer). Long-press enters Arrange mode on every
 * surface (round 21, item 7) — this table is tap-only.
 */
enum class CardTapRoute {
    CLIMATE_POPOUT,
    VACUUM_PANEL,
    ENTITY_DETAIL,
}

fun cardTapRoute(domain: String, entityId: String): CardTapRoute = when {
    domain == "climate" -> CardTapRoute.CLIMATE_POPOUT
    domain == "vacuum" && entityId == com.garrietta.haven.data.VACUUM_ID -> CardTapRoute.VACUUM_PANEL
    else -> CardTapRoute.ENTITY_DETAIL
}

/** Folded pill presentation styles (Stage C nav customization). */
 enum class NavStyle(val key: String) {
    FLOATING("floating"),
    COMPACT("compact"),
}

/** Stored nav-style keys resolve leniently: unknown means floating. */
 fun navStyleFromKey(key: String): NavStyle =
    NavStyle.entries.firstOrNull { it.key == key } ?: NavStyle.FLOATING

/**
 * Folded pill geometry per style, as pure data so tests pin it.
 * barHeight is always itemHeight + 2 * rowVerticalPadding. UI rewrite
 * round: the selected item is a unified 44dp capsule (icon + label),
 * so there is no separate sliding indicator anymore — the style now
 * moves bar height (64dp Floating vs 52dp Compact), outer padding,
 * and corner radius, which is why the old setting read as no visual
 * change: its 8dp height delta rode on an icon-only bar whose
 * indicator barely moved. The 12dp height delta plus the padding
 * and corner deltas below are unmistakable.
 */
data class NavPillGeometry(
    val barHeight: Dp,
    val itemHeight: Dp,
    val rowVerticalPadding: Dp,
    val rowHorizontalPadding: Dp,
    val cornerRadius: Dp,
)

fun navPillGeometry(style: NavStyle): NavPillGeometry = when (style) {
    // Floating: 10 + 44 + 10 = 64dp, generous padding, 32dp corners.
    NavStyle.FLOATING -> NavPillGeometry(
        barHeight = 64.dp,
        itemHeight = 44.dp,
        rowVerticalPadding = 10.dp,
        rowHorizontalPadding = 18.dp,
        cornerRadius = 32.dp,
    )
    // Compact: 4 + 44 + 4 = 52dp, tighter padding, 26dp corners.
    NavStyle.COMPACT -> NavPillGeometry(
        barHeight = 52.dp,
        itemHeight = 44.dp,
        rowVerticalPadding = 4.dp,
        rowHorizontalPadding = 12.dp,
        cornerRadius = 26.dp,
    )
}
