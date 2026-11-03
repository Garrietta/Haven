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

/** One pill-bar / side-rail destination. */
 data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
)

/**
 * UI rewrite round (2026-11-03): the pill bar and the side rail are
 * LOCKED to exactly four destinations — Home, Scenes, Energy, Settings.
 * Custom user tabs are NOT bar destinations anymore (they used to push
 * Settings off-screen when starter tabs were seeded); they live under
 * Settings -> Browse as User Tabs / Manage Tabs and stay fully
 * functional there. A screen that is not one of the four shows the
 * bar/rail with nothing selected (same as Boards/Areas/Search).
 */
fun pillBarItems(): List<NavItem> = listOf(
    NavItem(Screen.Today, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Screen.Scenes, "Scenes", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavItem(Screen.Energy, "Energy", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    NavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

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
