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
import org.junit.Assert.assertFalse
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
        pinned: Boolean = false,
    ) = UserTab(id, title, cards, icon, hidden, pinned)

    @Test
    fun barDefaultsToFourDestinations() {
        // Feedback round 15, item 3: the DEFAULT bar is still Home,
        // Scenes, Energy, Settings — Home first, Settings last and
        // always present, so the user can never strand themselves
        // on a screen with no way back to Settings. Scenes/Energy
        // hideability and pinned tabs are covered below.
        val items = pillBarItems()
        assertEquals(listOf("Home", "Scenes", "Energy", "Settings"), items.map { it.label })
        assertEquals(Screen.Today, items[0].screen)
        assertEquals(Screen.Scenes, items[1].screen)
        assertEquals(Screen.Energy, items[2].screen)
        assertEquals(Screen.Settings, items[3].screen)
    }

    @Test
    fun hideableBuiltInsDropButHomeAndSettingsNeverDo() {
        assertEquals(
            listOf("Home", "Energy", "Settings"),
            pillBarItems(showScenes = false).map { it.label },
        )
        assertEquals(
            listOf("Home", "Scenes", "Settings"),
            pillBarItems(showEnergy = false).map { it.label },
        )
        assertEquals(
            listOf("Home", "Settings"),
            pillBarItems(showScenes = false, showEnergy = false).map { it.label },
        )
    }

    @Test
    fun pinnedTabsSitBetweenEnergyAndSettingsCappedAtTwo() {
        val a = tab("a", "Cameras", pinned = true)
        val b = tab("b", "Lights", pinned = true)
        val c = tab("c", "Garage", pinned = true)
        // Two pins: Home, Scenes, Energy, pins in user-tab order, Settings.
        val two = pillBarItems(pinnedTabs = listOf(a, b))
        assertEquals(listOf("Home", "Scenes", "Energy", "Cameras", "Lights", "Settings"), two.map { it.label })
        assertEquals(Screen.UserTab("a"), two[3].screen)
        assertEquals(Screen.UserTab("b"), two[4].screen)
        // A third pin is dropped by the cap (the ViewModel setter
        // backstops the same cap; this function trusts the caller
        // and takes the first two).
        val three = pillBarItems(pinnedTabs = listOf(a, b, c))
        assertEquals(listOf("Home", "Scenes", "Energy", "Cameras", "Lights", "Settings"), three.map { it.label })
        // Pins combine with hidden built-ins: six tabs max means the
        // cap, not the built-ins, bounds the bar.
        val hidden = pillBarItems(showScenes = false, showEnergy = false, pinnedTabs = listOf(a, b))
        assertEquals(listOf("Home", "Cameras", "Lights", "Settings"), hidden.map { it.label })
    }

    @Test
    fun settingsSubmenusMapToSettingsForRailHighlight() {
        // Feedback round 15, item 4: every Settings submenu keeps
        // the Settings rail highlight. Feedback round 17,
        // item 3, OVERRIDES the round-15 Browse carve-out for
        // exactly four destinations: Dashboards, Areas, Search,
        // and UserTabs now map to Settings too (Manage home page
        // and Rooms & tabs cleared the indicator on the
        // 2026-11-04 runs).
        for (screen in listOf(
            Screen.HomeEditor, Screen.SettingsPage("areas/areas"),
            Screen.Dashboards, Screen.Areas, Screen.Search, Screen.UserTabs,
            Screen.AutomationBuilder, Screen.SceneBuilder, Screen.ScriptBuilder,
            Screen.HelperBuilder, Screen.UpdatesPanel,
        )) {
            assertEquals(Screen.Settings, navActiveScreen(screen))
        }
        // The remaining browse destinations keep the
        // nothing-selected rule.
        assertEquals(Screen.DashboardEdit, navActiveScreen(Screen.DashboardEdit))
        assertEquals(Screen.AreaDetail("area.bedroom"), navActiveScreen(Screen.AreaDetail("area.bedroom")))
        // Ordinary destinations map to themselves.
        assertEquals(Screen.Today, navActiveScreen(Screen.Today))
        assertEquals(Screen.Scenes, navActiveScreen(Screen.Scenes))
        assertEquals(Screen.Energy, navActiveScreen(Screen.Energy))
        assertEquals(Screen.Settings, navActiveScreen(Screen.Settings))
        assertEquals(Screen.UserTab("a"), navActiveScreen(Screen.UserTab("a")))
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

    @Test
    fun securitySitsBesideHomeAndTheBarStaysAtSix() {
        val a = com.garrietta.haven.data.UserTab("a", "A", emptyList(), pinned = true)
        val b = com.garrietta.haven.data.UserTab("b", "B", emptyList(), pinned = true)
        val bar = pillBarItems(pinnedTabs = listOf(a, b), showSecurity = true)
        assertEquals(listOf("Home", "Security", "Scenes", "Energy", "A", "Settings"), bar.map { it.label })
        assertEquals(6, bar.size)
        // With Scenes off there is room for both pinned tabs again.
        assertEquals(
            listOf("Home", "Security", "Energy", "A", "B", "Settings"),
            pillBarItems(showScenes = false, pinnedTabs = listOf(a, b), showSecurity = true).map { it.label },
        )
    }

    @Test
    fun extraSlotsShrinkAsBuiltInsAreShown() {
        assertEquals(2, barExtraSlots(showSecurity = false, showScenes = true, showEnergy = true))
        assertEquals(1, barExtraSlots(showSecurity = true, showScenes = true, showEnergy = true))
        assertEquals(2, barExtraSlots(showSecurity = true, showScenes = false, showEnergy = true))
        assertEquals(2, barExtraSlots(showSecurity = false, showScenes = false, showEnergy = false))
        assertFalse(canPinBarExtra(1, 0, alreadyOnBar = false, slots = 1))
        assertTrue(canPinBarExtra(1, 0, alreadyOnBar = true, slots = 1))
    }
}
