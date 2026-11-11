package com.garrietta.haven.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.garrietta.haven.HavenApp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.ui.screens.RoomFilter
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The popup's morph, frame by frame: a tile opening into its popup, and
 * the popup being pulled back down into the tile with the finger held at
 * several distances. A held finger is the hard case, because every
 * in-between state of the morph can be looked at for as long as the user
 * likes, so none of them may be blank.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests '*PopupMorphFramesTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PopupMorphFramesTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun start(dark: Boolean, room: String): HavenViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = HavenViewModel(app)
        vm.settingsStore.themeMode = if (dark) "dark" else "light"
        vm.client.restoreCachedStates(DemoHome.states, DemoHome.areas, DemoHome.entityAreas)
        vm.loginWithToken("http://127.0.0.1:9", "screenshot-token")
        vm.roomFilter = RoomFilter.Area(room)
        RuntimeEnvironment.setQualifiers(FOLDED)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.recreate()
        compose.setContent { HavenApp(vm) }
        compose.mainClock.advanceTimeBy(1_500L)
        return vm
    }

    private fun capture(name: String) =
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/morph/$name.png")

    @Test
    fun pullDown_light() {
        start(dark = false, room = "living_room")
        compose.onNodeWithText("Floor Lamp").performClick()
        compose.mainClock.advanceTimeBy(1_500L)
        capture("pull_0_open")
        val root = compose.onAllNodes(isRoot()).onLast()
        root.performTouchInput { down(Offset(centerX, 300f)) }
        var travelled = 0
        for (step in listOf(80, 200, 320, 440, 560, 680, 800, 1000)) {
            root.performTouchInput { moveBy(Offset(0f, (step - travelled).toFloat())) }
            travelled = step
            compose.mainClock.advanceTimeBy(64L)
            capture("pull_${step.toString().padStart(4, '0')}px_held")
        }
        root.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(1_500L)
        capture("pull_9_released")
    }

    @Test
    fun openAndBackClose_dark() {
        start(dark = true, room = "kitchen")
        compose.onNodeWithText("Pendants").performClick()
        var clock = 0L
        for (ms in listOf(16L, 48L, 80L, 112L, 160L, 240L, 400L, 1_500L)) {
            compose.mainClock.advanceTimeBy(ms - clock)
            clock = ms
            capture("open_${ms.toString().padStart(4, '0')}ms")
        }
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        clock = 0L
        for (ms in listOf(16L, 48L, 80L, 112L, 160L, 240L, 400L, 1_500L)) {
            compose.mainClock.advanceTimeBy(ms - clock)
            clock = ms
            capture("close_${ms.toString().padStart(4, '0')}ms")
        }
    }

    private companion object {
        const val FOLDED = "w384dp-h880dp-xhdpi"
    }
}
