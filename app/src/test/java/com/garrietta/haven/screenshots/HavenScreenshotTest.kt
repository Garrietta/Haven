package com.garrietta.haven.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.garrietta.haven.HavenApp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.ui.screens.RoomFilter
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

    private fun shoot(name: String, qualifiers: String, vm: HavenViewModel, settle: Long = 1_500L) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.recreate()
        compose.setContent { HavenApp(vm) }
        compose.mainClock.advanceTimeBy(settle)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

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

    private companion object {
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
