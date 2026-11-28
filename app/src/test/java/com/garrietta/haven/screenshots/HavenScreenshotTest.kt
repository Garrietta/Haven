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
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.garrietta.haven.HavenApp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.ui.screens.SettingsPageHost
import com.garrietta.haven.ui.screens.AppearanceControls
import com.garrietta.haven.ui.theme.HavenTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipeUp
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

    private fun tapText(text: String) {
        reveal(text)
        compose.onAllNodesWithText(text).onFirst().performClick()
    }

    /** Taps the status card titled [title] (its heading below has the same word, but is not a button). */
    private fun tapCard(title: String) {
        // Near its left edge: a card at the end of the row can be partly off screen.
        compose.onAllNodes(androidx.compose.ui.test.hasText(title) and androidx.compose.ui.test.hasClickAction()).onFirst()
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(24f, height / 2f)) }
        // A bottom sheet is its own window: Android lays it out on the main
        // looper, and the sheet waits for that size before it slides up, so
        // the looper runs between frames.
        repeat(SHEET_FRAMES) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            frame()
        }
    }

    /** Opens a light, plug or fan's controls from the button in its tile's corner (a tap on the tile switches it). */
    private fun openTile(name: String) {
        reveal(name)
        compose.onAllNodesWithContentDescription("Open $name").onFirst().performClick()
    }

    /**
     * Brings a tile on Home into view when it is not already there: the
     * list composes only what is on screen. Swipes rather than a semantic
     * scroll, which would wait for an idle the always-animating app never
     * reaches; the clock is advanced by hand after each swipe.
     */
    private fun reveal(text: String) {
        repeat(8) {
            if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
            compose.onAllNodes(isRoot()).onFirst().performTouchInput { swipeUp(startY = bottom * 0.8f, endY = bottom * 0.3f) }
            compose.mainClock.advanceTimeBy(SETTLE_MS)
        }
    }

    /** The app on [LivedInHome]: areas half filled in, duplicates, cameras, a greenhouse. */
    private fun livedIn(dark: Boolean): HavenViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = HavenViewModel(app)
        vm.settingsStore.themeMode = if (dark) "dark" else "light"
        vm.client.restoreCachedStates(
            LivedInHome.states, LivedInHome.areas, LivedInHome.entityAreas, LivedInHome.entityDevices,
            LivedInHome.deviceInfo, LivedInHome.auxiliary,
        )
        vm.loginWithToken("http://127.0.0.1:9", "screenshot-token")
        return vm
    }

    @Test fun lived_in_folded_light() = shoot("lived_in_folded_light", FOLDED, livedIn(dark = false))

    @Test fun lived_in_folded_dark() = shoot("lived_in_folded_dark", FOLDED, livedIn(dark = true))

    @Test fun lived_in_unfolded_light() = shoot("lived_in_unfolded_light", UNFOLDED, livedIn(dark = false))

    @Test fun lived_in_unfolded_dark() = shoot("lived_in_unfolded_dark", UNFOLDED, livedIn(dark = true))

    @Test fun login_folded_light() = shoot("login_folded_light", FOLDED, viewModel(signedIn = false, dark = false))

    @Test fun login_folded_dark() = shoot("login_folded_dark", FOLDED, viewModel(signedIn = false, dark = true))

    @Test fun home_folded_light() = shoot("home_folded_light", FOLDED, viewModel(signedIn = true, dark = false))

    @Test fun home_folded_dark() = shoot("home_folded_dark", FOLDED, viewModel(signedIn = true, dark = true))

    @Test fun home_unfolded_light() = shoot("home_unfolded_light", UNFOLDED, viewModel(signedIn = true, dark = false))

    @Test fun home_unfolded_dark() = shoot("home_unfolded_dark", UNFOLDED, viewModel(signedIn = true, dark = true))

    /** The Lights card opens a sheet of every light, each a tile. */
    @Test fun lights_sheet_folded_light() =
        shoot("lights_sheet_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) { tapCard("Lights") }

    @Test fun lights_sheet_unfolded_dark() =
        shoot("lights_sheet_unfolded_dark", UNFOLDED, viewModel(signedIn = true, dark = true)) { tapCard("Lights") }

    @Test fun settings_folded_light() =
        shoot("settings_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) { tapTab("Settings") }

    @Test fun settings_folded_dark() =
        shoot("settings_folded_dark", FOLDED, viewModel(signedIn = true, dark = true)) { tapTab("Settings") }

    @Test fun settings_unfolded_light() =
        shoot("settings_unfolded_light", UNFOLDED, viewModel(signedIn = true, dark = false)) { tapTab("Settings") }



    @Test fun light_popup_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        shoot("light_popup_folded_light", FOLDED, vm) { openTile("Floor Lamp") }
    }

    @Test fun light_popup_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        shoot("light_popup_folded_dark", FOLDED, vm) { openTile("Floor Lamp") }
    }

    /** A switch's pop-up: the power slab, empty while it is off. */
    @Test fun switch_popup_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        shoot("switch_popup_folded_dark", FOLDED, vm) { openTile("Coffee Maker") }
    }

    /** A fan's pop-up: the slab filled to its speed. */
    @Test fun fan_popup_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        shoot("fan_popup_folded_light", FOLDED, vm) { openTile("Ceiling Fan") }
    }

    /**
     * Press and hold a tile: its options sheet, color and icon applied as
     * they are tapped, with Arrange beside them.
     */
    private fun holdTile(name: String) {
        reveal(name)
        // The tile is the first node with the name (the room's line and the bar repeat it).
        compose.onAllNodesWithText(name).onFirst().performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(800)
        compose.onAllNodesWithText(name).onFirst().performTouchInput { up() }
    }

    @Test fun tile_options_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        shoot("tile_options_folded_light", FOLDED, vm) { holdTile("Coffee Maker") }
    }

    @Test fun tile_options_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        shoot("tile_options_folded_dark", FOLDED, vm) { holdTile("Ceiling Fan") }
    }

    /**
     * The whole sheet, every icon group, Haven's own glyphs among them:
     * drawn in a window tall enough to hold it, because a scroll waits
     * on an animation the hand-driven clock never runs.
     */
    @Test fun tile_options_icons_light() {
        val vm = viewModel(signedIn = true, dark = false)
        shoot("tile_options_icons_light", "w384dp-h2700dp-xhdpi", vm) { holdTile("Coffee Maker") }
    }

    /** A lock's pop-up: full and secure while locked. */
    @Test fun lock_popup_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        shoot("lock_popup_folded_light", FOLDED, vm) { tapText("Side Door") }
    }

    /**
     * A settings page on its own, as Settings opens it. Drawn directly
     * rather than reached through Settings, whose rows sit below the
     * fold: a scroll there waits on an animation the hand-driven clock
     * never runs.
     */
    private fun shootSettingsPage(name: String, vm: HavenViewModel, dark: Boolean, route: String, then: (() -> Unit)? = null) {
        RuntimeEnvironment.setQualifiers(FOLDED)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.recreate()
        val controls = AppearanceControls(if (dark) "dark" else "light", false, {}, {}, com.garrietta.haven.ui.theme.ColorStyle.HAVEN, {}, {})
        compose.setContent { HavenTheme(darkTheme = dark) { SettingsPageHost(vm, route, onBack = {}, navigate = {}, appearance = controls) } }
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

    /** Rooms and tabs as rows with grips, one open. */
    @Test fun areas_folded_light() =
        shootSettingsPage("areas_folded_light", viewModel(signedIn = true, dark = false), dark = false, route = "areas/areas") {
            tapText("Kitchen")
        }

    /** Settings' hub: Home Assistant's sections, then Haven's. */
    @Test fun settings_hub_folded_dark() =
        shoot("settings_hub_folded_dark", FOLDED, viewModel(signedIn = true, dark = true)) { tapTab("Settings") }

    /** A finger sliding across a lamp's tile: the tile fills to the level being set. */
    @Test fun tile_slide_folded_light() =
        shoot("tile_slide_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) {
            reveal("Floor Lamp")
            compose.onAllNodesWithText("Floor Lamp").onFirst().performTouchInput {
                down(Offset(width * 0.7f, height / 2f))
                repeat(6) { moveBy(Offset(-25f, 0f)); advanceEventTime(16) }
            }
            frame()
        }

    /** A tile held and carried toward another place in its group. */
    @Test fun tile_carry_folded_light() =
        shoot("tile_carry_folded_light", FOLDED, viewModel(signedIn = true, dark = false)) {
            reveal("Floor Lamp")
            compose.onAllNodesWithText("Floor Lamp").onFirst().performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(700)
            compose.onAllNodesWithText("Floor Lamp").onFirst().performTouchInput {
                repeat(8) { moveBy(Offset(30f, -6f)); advanceEventTime(16) }
            }
            frame()
        }

    /** Appearance: light or dark, the color styles as pictures, motion. */
    @Test fun appearance_folded_light() =
        shootSettingsPage("appearance_folded_light", viewModel(signedIn = true, dark = false), dark = false, route = "appearance")

    @Test fun appearance_folded_dark() =
        shootSettingsPage("appearance_folded_dark", viewModel(signedIn = true, dark = true), dark = true, route = "appearance")

    /** Home in each color style. */
    private fun styled(style: String, dark: Boolean): HavenViewModel =
        viewModel(signedIn = true, dark = dark).also { it.settingsStore.colorStyleKey = style }

    @Test fun style_home_assistant_folded_light() = shoot("style_home_assistant_folded_light", FOLDED, styled("home_assistant", dark = false))

    @Test fun style_home_assistant_folded_dark() = shoot("style_home_assistant_folded_dark", FOLDED, styled("home_assistant", dark = true))

    @Test fun style_tide_folded_light() = shoot("style_tide_folded_light", FOLDED, styled("tide", dark = false))

    @Test fun style_orchid_folded_dark() = shoot("style_orchid_folded_dark", FOLDED, styled("orchid", dark = true))

    @Test fun style_graphite_folded_light() = shoot("style_graphite_folded_light", FOLDED, styled("graphite", dark = false))

    @Test fun style_home_assistant_thermostat_light() {
        val vm = styled("home_assistant", dark = false)
        shoot("style_home_assistant_thermostat_light", FOLDED, vm) { tapText("Thermostat") }
    }

    /** Tiles taken off Home, each with Show to bring it back. */
    @Test fun hidden_from_home_folded_light() {
        val vm = viewModel(signedIn = true, dark = false)
        vm.settingsStore.setHomeLightOverride("switch.coffee_maker", false)
        vm.settingsStore.setHomeLightOverride("light.kitchen_pendants", false)
        shootSettingsPage("hidden_from_home_folded_light", vm, dark = false, route = "home/hidden")
    }

    @Test fun app_updates_folded_light() =
        shootSettingsPage("app_updates_folded_light", viewModel(signedIn = true, dark = false), dark = false, route = "updates")

    @Test fun system_folded_dark() =
        shootSettingsPage("system_folded_dark", viewModel(signedIn = true, dark = true), dark = true, route = "system")

    @Test fun automations_folded_light() =
        shootSettingsPage("automations_folded_light", viewModel(signedIn = true, dark = false), dark = false, route = "automations/scenes")

    /** An entity's page: Home Assistant's settings (unreachable here) and its look in Haven. */
    @Test fun entity_settings_folded_light() =
        shootSettingsPage("entity_settings_folded_light", viewModel(signedIn = true, dark = false), dark = false, route = "entity/light.living_room_lamp")

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

    /** An ecobee and its remote sensor, a greenhouse sensor and an outdoor one, beside the demo's living room. */
    private fun withClimate(vm: HavenViewModel): HavenViewModel {
        fun e(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
            com.garrietta.haven.data.HaEntity(id, state, mapOf<String, Any?>("friendly_name" to name) + attrs)
        val extra = listOf(
            e(
                "climate.thermostat", "heat", "Main Floor",
                "hvac_action" to "heating", "temperature" to 70, "current_temperature" to 67, "current_humidity" to 41,
                "min_temp" to 45, "max_temp" to 95, "hvac_modes" to listOf("heat", "cool", "heat_cool", "off"),
                "preset_modes" to listOf("home", "away", "sleep"), "preset_mode" to "home",
                "fan_modes" to listOf("auto", "on"), "fan_mode" to "auto",
            ),
            e("sensor.bedroom_temperature", "65.8", "Bedroom Temperature", "device_class" to "temperature", "unit_of_measurement" to "°F"),
            e("sensor.bedroom_humidity", "44", "Bedroom Humidity", "device_class" to "humidity", "unit_of_measurement" to "%"),
            e("sensor.greenhouse_temperature", "78.2", "Greenhouse Temperature", "device_class" to "temperature", "unit_of_measurement" to "°F"),
            e("sensor.greenhouse_humidity", "71", "Greenhouse Humidity", "device_class" to "humidity", "unit_of_measurement" to "%"),
            e("sensor.outdoor_temperature", "41.5", "Outdoor Temperature", "device_class" to "temperature", "unit_of_measurement" to "°F"),
        ).associateBy { it.entityId }
        vm.client.restoreCachedStates(
            DemoHome.states + extra,
            DemoHome.areas,
            DemoHome.entityAreas + mapOf("sensor.bedroom_temperature" to "bedroom", "sensor.bedroom_humidity" to "bedroom"),
            mapOf(
                "sensor.bedroom_temperature" to "dev_bedroom", "sensor.bedroom_humidity" to "dev_bedroom",
                "sensor.living_room_temperature" to "dev_living", "sensor.living_room_humidity" to "dev_living",
                "sensor.greenhouse_temperature" to "dev_greenhouse", "sensor.greenhouse_humidity" to "dev_greenhouse",
            ),
        )
        return vm
    }

    /** The Climate tab: the house in one card, the thermostat, each room's air, outside. */
    @Test fun climate_folded_light() =
        shoot("climate_folded_light", FOLDED, withClimate(viewModel(signedIn = true, dark = false))) { tapTab("Climate") }

    @Test fun climate_folded_dark() =
        shoot("climate_folded_dark", FOLDED, withClimate(viewModel(signedIn = true, dark = true))) { tapTab("Climate") }

    /** A thermostat's popup, opened by a tap on its panel on the Climate tab: the panel, its last 24 hours, the menu rows. */
    @Test fun thermostat_popup_folded_light() =
        shoot("thermostat_popup_folded_light", FOLDED, withClimate(viewModel(signedIn = true, dark = false))) {
            tapTab("Climate")
            settle()
            tapText("Main Floor")
        }

    /** A heat-and-cool thermostat at rest: the green panel with Heat to and Cool to. */
    @Test fun thermostat_popup_heat_cool_light() =
        shoot("thermostat_popup_heat_cool_light", FOLDED, livedIn(dark = false)) { tapText("Thermostat") }

    /** Scrolled to each room's air: the comfort bars and the outdoor cards. */
    @Test fun climate_rooms_folded_light() =
        shoot("climate_rooms_folded_light", FOLDED, withClimate(viewModel(signedIn = true, dark = false))) {
            tapTab("Climate")
            settle()
            compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 400) }
            repeat(40) { frame() }
        }

    /** The Climate tab in Edit: each section's items with grips and crosses, and the search to add more. */
    @Test fun climate_edit_folded_light() =
        shoot("climate_edit_folded_light", FOLDED, withClimate(viewModel(signedIn = true, dark = false))) {
            tapTab("Climate")
            settle()
            tapText("Edit")
        }

    /** The Security tab in Edit. */
    @Test fun security_edit_folded_dark() =
        shoot("security_edit_folded_dark", FOLDED, withSecurity(viewModel(signedIn = true, dark = true))) {
            tapTab("Security")
            settle()
            tapText("Edit")
        }

    @Test fun climate_unfolded_light() =
        shoot("climate_unfolded_light", UNFOLDED, withClimate(viewModel(signedIn = true, dark = false))) { tapTab("Climate") }

    /** A reading's popup unfolded: the chart across the whole popup, sized to it. */
    @Test fun reading_popup_unfolded_light() {
        val vm = withClimate(viewModel(signedIn = true, dark = false))
        vm.historyCache["sensor.bedroom_temperature"] = demoHistory(63.8, 66.9)
        shoot("reading_popup_unfolded_light", UNFOLDED, vm) {
            tapTab("Climate")
            frame()
            tapText("Bedroom")
        }
    }

    /** A day of readings between [low] and [high]: a slow swing, a dip and a step up late in the day. */
    private fun demoHistory(low: Double, high: Double): List<com.garrietta.haven.data.HistoryPoint> {
        val now = 1_764_000_000_000L
        return (0..96).map { i ->
            val t = i / 96.0
            val swing = (kotlin.math.sin(t * 6.0) + 1) / 2 * 0.35
            val step = if (t > 0.82) 0.55 else 0.0
            com.garrietta.haven.data.HistoryPoint(now - (96 - i) * 15 * 60_000L, low + (high - low) * (swing + step).coerceAtMost(1.0))
        }
    }

    @Test fun thermostat_popup_folded_dark() {
        val vm = viewModel(signedIn = true, dark = true)
        shoot("thermostat_popup_folded_dark", FOLDED, vm) { tapText("Thermostat") }
    }

    private companion object {
        /** Long enough for every spring in the app to come to rest. */
        const val SETTLE_MS = 1_500L
        /** About a second and a half of frames: long enough for a bottom sheet to finish sliding up. */
        const val SHEET_FRAMES = 90

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
