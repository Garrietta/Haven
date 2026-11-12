package com.garrietta.haven.screenshots

import android.app.Application
import androidx.compose.runtime.snapshots.Snapshot
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.garrietta.haven.HavenApp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.ui.screens.RoomFilter
import com.garrietta.haven.ui.screens.RoomsTabsScreen
import com.garrietta.haven.ui.theme.HavenTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real app on the JVM (Robolectric native graphics) with
 * [DemoHome] seeded in, folded and unfolded, light and dark. The PNGs are
 * how a visual change is judged without a phone:
 *
 *   ./gradlew :app:recordRoborazziDebug     writes app/build/outputs/roborazzi
 *
 * In a plain `testDebugUnitTest` run the screens still compose and draw
 * (so a crash on first frame fails the build) but nothing is written.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class HavenScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun viewModel(signedIn: Boolean, dark: Boolean): HavenViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = HavenViewModel(app)
        vm.settingsStore.themeMode = if (dark) "dark" else "light"
        if (signedIn) {
            vm.client.restoreCachedStates(DemoHome.states, DemoHome.areas, DemoHome.entityAreas)
            // An address nothing answers on: the app sits on last-known
            // data, exactly like a cold start away from home.
            vm.loginWithToken("http://127.0.0.1:9", "screenshot-token")
        }
        return vm
    }

    /**
     * Renders the app and captures it. [then] drives the UI first (a tap
     * on a tab or a tile); the clock is advanced by hand around it,
     * because the app always has something animating (a loading
     * indicator, the mark's sun) and would never report itself idle.
     */
    private fun shoot(
        name: String,
        qualifiers: String,
        vm: HavenViewModel,
        then: (() -> Unit)? = null,
    ) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.recreate()
        compose.setContent { HavenApp(vm) }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        if (then != null) {
            then()
            compose.mainClock.advanceTimeBy(SETTLE_MS)
        }
        // onRoot() would be ambiguous while a popup's own window is up.
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    // The navigation bar is drawn last, so its item is the last node
    // carrying the label (a Home card can share it, like "Energy").
    private fun tapTab(label: String) = compose.onAllNodesWithContentDescription(label).onLast().performClick()

    private fun tapText(text: String) = compose.onNodeWithText(text).performClick()

    @Test fun login_folded_light() = shoot("login_folded_light", FOLDED, viewModel(signedIn = false, dark = false))

    @Test fun login_folded_dark() = shoot("login_folded_dark", FOLDED, viewModel(signedIn = false, dark = true))

    @Test fun home_folded_light() = shoot("home_folded_light", FOLDED, viewModel(signedIn = true, dark = false))

    @Test fun home_folded_dark() = shoot("home_folded_dark", FOLDED, viewModel(signedIn = true, dark = true))

    @Test fun home_unfolded_light() = shoot("home_unfolded_light", UNFOLDED, viewModel(signedIn = true, dark = false))

    @Test fun home_unfolded_dark() = shoot("home_unfolded_dark", UNFOLDED, viewModel(signedIn = true, dark = true))

    @Test fun room_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        vm.roomFilter = RoomFilter.Area("living_room")
        shoot("room_folded_light", FOLDED, vm)
    }

    @Test fun room_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        vm.roomFilter = RoomFilter.Area("kitchen")
        shoot("room_folded_dark", FOLDED, vm)
    }

    /** A room card opens its room as a popup over Home. */
    @Test fun room_popup_folded_light() =
        shoot("room_popup_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) { tapText("Living Room") }

    @Test fun room_popup_unfolded_dark() =
        shoot("room_popup_unfolded_dark", UNFOLDED, viewModel(signedIn = true, dark = true)) { tapText("Living Room") }

    @Test fun settings_folded_light() =
        shoot("settings_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) { tapTab("Settings") }

    @Test fun settings_folded_dark() =
        shoot("settings_folded_dark", FOLDED, viewModel(signedIn = true, dark = true)) { tapTab("Settings") }

    @Test fun settings_unfolded_light() =
        shoot("settings_unfolded_light", UNFOLDED, viewModel(signedIn = true, dark = false)) { tapTab("Settings") }

    @Test fun scenes_folded_light() =
        shoot("scenes_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) { tapTab("Scenes") }

    @Test fun energy_folded_dark() =
        shoot("energy_folded_dark", FOLDED, viewModel(signedIn = true, dark = true)) { tapTab("Energy") }

    @Test fun light_popup_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        vm.roomFilter = RoomFilter.Area("living_room")
        shoot("light_popup_folded_light", FOLDED, vm) { tapText("Floor Lamp") }
    }

    @Test fun light_popup_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        vm.roomFilter = RoomFilter.Area("living_room")
        shoot("light_popup_folded_dark", FOLDED, vm) { tapText("Floor Lamp") }
    }

    /** A switch's pop-up: the power slab, empty while it is off. */
    @Test fun switch_popup_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        vm.roomFilter = RoomFilter.Area("kitchen")
        shoot("switch_popup_folded_dark", FOLDED, vm) { tapText("Coffee Maker") }
    }

    /** A fan's pop-up: the slab filled to its speed. */
    @Test fun fan_popup_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        vm.roomFilter = RoomFilter.Area("bedroom")
        shoot("fan_popup_folded_light", FOLDED, vm) { tapText("Ceiling Fan") }
    }

    /** A lock's pop-up: full and secure while locked. */
    @Test fun lock_popup_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        vm.roomFilter = RoomFilter.Area("garage")
        shoot("lock_popup_folded_light", FOLDED, vm) { tapText("Side Door") }
    }

    /**
     * Rooms & tabs on its own, as Settings opens it. Drawn directly rather
     * than reached through Settings, whose row sits below the fold: a
     * scroll there waits on an animation the hand-driven clock never runs.
     */
    private fun shootRoomsTabs(name: String, vm: HavenViewModel, dark: Boolean, then: (() -> Unit)? = null) {
        RuntimeEnvironment.setQualifiers(FOLDED)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.recreate()
        compose.setContent { HavenTheme(darkTheme = dark) { RoomsTabsScreen(vm, onBack = {}, navigate = {}) } }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        if (then != null) {
            then()
            settle()
        }
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /**
     * Hands state written by input to composition, then runs the clock.
     * Nothing on this screen animates at rest, so no frame would otherwise
     * come to carry a tap's write to the screen.
     */
    private fun frame() {
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
    }

    private fun settle() {
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(SETTLE_MS)
    }

    private fun withMediaTab(vm: HavenViewModel): HavenViewModel {
        val id = vm.addTab("Media")
        vm.addCardToTab(id, "type: tile\nentity: media_player.living_room_tv")
        vm.addCardToTab(id, "type: tile\nentity: light.living_room_lamp")
        vm.addCardToTab(id, "type: tile\nentity: climate.thermostat")
        return vm
    }

    /** Rooms and tabs as rows with grips, one open. */
    @Test fun rooms_tabs_folded_light() =
        shootRoomsTabs("rooms_tabs_folded_light", withMediaTab(viewModel(signedIn = true, dark = false)), dark = false) {
            tapText("Kitchen")
        }

    /**
     * A room dragged by its grip: captured mid-drag, lifted over the rows
     * that have stepped aside, then dropped, and the new order is stored.
     */
    @Test fun rooms_tabs_drag_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        shootRoomsTabs("rooms_tabs_drag_folded_dark", vm, dark = true) {
            val grip = compose.onNodeWithContentDescription("Reorder Living Room", useUnmergedTree = true)
            grip.performTouchInput { down(center) }
            frame()
            // Down past Kitchen and Bedroom, a frame at a time, as a finger would.
            repeat(40) {
                grip.performTouchInput { moveBy(Offset(0f, 9f)) }
                frame()
            }
        }
        compose.onNodeWithContentDescription("Reorder Living Room", useUnmergedTree = true).performTouchInput { up() }
        settle()
        assertEquals(listOf("kitchen", "bedroom", "living_room", "garage"), vm.settingsStore.roomOrder())
    }

    private fun withSecurity(vm: HavenViewModel, open: Boolean = false): HavenViewModel {
        val extra = if (open) {
            mapOf("binary_sensor.back_door" to com.garrietta.haven.data.HaEntity(
                "binary_sensor.back_door", "on", mapOf("friendly_name" to "Back Door", "device_class" to "door"),
            ))
        } else emptyMap()
        vm.client.restoreCachedStates(DemoHome.states + DemoHome.security + extra, DemoHome.areas, DemoHome.entityAreas, DemoHome.securityDevices)
        return vm
    }

    /** The Security tab: all shut, the alarm, the cameras and their motion switches. */
    @Test fun security_folded_light() =
        shoot("security_folded_light", FOLDED, withSecurity(viewModel(signedIn = true, dark = false))) { tapTab("Security") }

    /** The same with a door open: the opening line turns to the alert family. */
    @Test fun security_folded_dark() =
        shoot("security_folded_dark", FOLDED, withSecurity(viewModel(signedIn = true, dark = true), open = true)) { tapTab("Security") }

    @Test fun security_unfolded_light() =
        shoot("security_unfolded_light", UNFOLDED, withSecurity(viewModel(signedIn = true, dark = false))) { tapTab("Security") }

    @Test fun thermostat_popup_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        vm.roomFilter = RoomFilter.Area("living_room")
        shoot("thermostat_popup_folded_dark", FOLDED, vm) { tapText("Thermostat") }
    }

    private companion object {
        /** Long enough for every spring in the app to come to rest. */
        const val SETTLE_MS = 1_500L

        /** The Fold's cover screen, in dp. */
        const val FOLDED = "w384dp-h880dp-xhdpi"

        /**
         * The unfolded inner screen in landscape. The two-pane layout needs
         * the window at least 600dp wide and at least as wide as tall
         * (isWideLayout), so unfolded portrait would render the folded layout.
         */
        const val UNFOLDED = "w840dp-h700dp-land-xhdpi"
    }
}
