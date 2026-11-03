package com.garrietta.haven.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.unit.dp
import com.garrietta.haven.Screen
import com.garrietta.haven.data.UserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage C (redesign): the navigation model contract. Home is always
 * first, Settings always last and always present — even with zero tabs
 * or every tab hidden — so the user can never strand themselves on a
 * screen with no way back to Settings.
 */
class NavItemsTest {

    private fun tab(
        id: String,
        title: String = "Tab",
        cards: List<String> = emptyList(),
        icon: String? = null,
        hidden: Boolean = false,
    ) = UserTab(id, title, cards, icon, hidden)

    @Test
    fun barLockedToExactlyFourDestinations() {
        // UI rewrite round: the bar/rail show ONLY Home, Scenes,
        // Energy, Settings — user tabs live under Settings -> Browse
        // and can never push Settings off-screen again.
        val items = pillBarItems()
        assertEquals(listOf("Home", "Scenes", "Energy", "Settings"), items.map { it.label })
        assertEquals(Screen.Today, items[0].screen)
        assertEquals(Screen.Scenes, items[1].screen)
        assertEquals(Screen.Energy, items[2].screen)
        assertEquals(Screen.Settings, items[3].screen)
    }

    @Test
    fun iconOverrideBeatsAutoDerivation() {
        // A camera card would auto-derive Videocam; the picker wins.
        val cameraCard = "type: tile\nentity: camera.tapo_1_live_view"
        val (icon, outlined) = tabIcons(tab("a", "Cameras", listOf(cameraCard), icon = "tv"))
        assertEquals(Icons.Filled.Tv, icon)
        assertEquals(Icons.Outlined.Tv, outlined)
    }

    @Test
    fun autoDerivationFromCardDomainAndTitle() {
        val cameraCard = "type: tile\nentity: camera.tapo_1_live_view"
        assertEquals(
            Icons.Filled.Videocam to Icons.Outlined.Videocam,
            tabIcons(tab("a", "Cameras", listOf(cameraCard))),
        )
        assertEquals(
            Icons.Filled.Home to Icons.Outlined.Home,
            tabIcons(tab("b", "Garage Door")),
        )
        assertEquals(
            Icons.Filled.GridView to Icons.Outlined.GridView,
            tabIcons(tab("c", "Misc")),
        )
    }

    @Test
    fun unknownIconKeyFallsBackToAuto() {
        val (icon, outlined) = tabIcons(tab("a", "Cameras", icon = "bogus"))
        assertEquals(Icons.Filled.Videocam, icon)
        assertEquals(Icons.Outlined.Videocam, outlined)
    }

    @Test
    fun pickerOptionsAreUniqueAndNonEmpty() {
        val keys = TAB_ICON_OPTIONS.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.isNotEmpty())
    }

    @Test
    fun selectedIndexMatchesFixedBarOrder() {
        val items = pillBarItems()
        // Bar order: Home(0), Scenes(1), Energy(2), Settings(3).
        assertEquals(0, selectedNavIndex(Screen.Today, items))
        assertEquals(1, selectedNavIndex(Screen.Scenes, items))
        assertEquals(2, selectedNavIndex(Screen.Energy, items))
        assertEquals(3, selectedNavIndex(Screen.Settings, items))
    }

    @Test
    fun selectedIndexNullForNonBarScreens() {
        val items = pillBarItems()
        assertNull(selectedNavIndex(Screen.UserTab("a"), items))
        assertNull(selectedNavIndex(Screen.Dashboards, items))
        assertNull(selectedNavIndex(Screen.Areas, items))
    }

    @Test
    fun floatingGeometryMatchesCurrentBar() {
        val g = navPillGeometry(NavStyle.FLOATING)
        assertEquals(64.dp, g.barHeight)
        assertEquals(44.dp, g.itemHeight)
        assertEquals(10.dp, g.rowVerticalPadding)
        assertEquals(18.dp, g.rowHorizontalPadding)
        assertEquals(32.dp, g.cornerRadius)
        assertEquals(g.barHeight, g.itemHeight + g.rowVerticalPadding * 2)
    }

    @Test
    fun compactGeometryIsTighterButConsistent() {
        val g = navPillGeometry(NavStyle.COMPACT)
        assertEquals(52.dp, g.barHeight)
        assertEquals(44.dp, g.itemHeight)
        assertEquals(4.dp, g.rowVerticalPadding)
        assertEquals(12.dp, g.rowHorizontalPadding)
        assertEquals(26.dp, g.cornerRadius)
        assertEquals(g.barHeight, g.itemHeight + g.rowVerticalPadding * 2)
        assertTrue(g.barHeight < navPillGeometry(NavStyle.FLOATING).barHeight)
        assertTrue(g.cornerRadius < navPillGeometry(NavStyle.FLOATING).cornerRadius)
    }

    @Test
    fun navStyleKeyResolvesLeniently() {
        assertEquals(NavStyle.FLOATING, navStyleFromKey("floating"))
        assertEquals(NavStyle.COMPACT, navStyleFromKey("compact"))
        assertEquals(NavStyle.FLOATING, navStyleFromKey("bogus"))
        assertEquals(NavStyle.FLOATING, navStyleFromKey(""))
    }
}
