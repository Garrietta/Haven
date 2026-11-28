package com.garrietta.haven.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.garrietta.haven.HavenApp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaDeviceInfo
import com.garrietta.haven.data.HaEntity
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The app drawn from a snapshot of a real home rather than the demo one,
 * to judge a change on the devices it will actually show. It runs only
 * when a snapshot is present in `app/build/real-home/` (inside the build
 * directory, so it is never committed):
 *
 *   states.json     Home Assistant's /api/states, as served
 *   registry.json   {"entityDevices": {entity: device}, "devices": {id:
 *                   {"name", "model", "service"}}, "auxiliary": [entity]}
 *
 * The pages are drawn in a window as tall as they are, so one image shows
 * a whole page. PNGs land beside the others, prefixed `real_`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RealHomeScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val dir = File("build/real-home")

    private fun realHome(dark: Boolean, style: String = "haven"): HavenViewModel {
        assumeTrue("no real-home snapshot", File(dir, "states.json").isFile && File(dir, "registry.json").isFile)
        val states = JSONArray(File(dir, "states.json").readText()).let { array ->
            (0 until array.length()).map { HaEntity.fromJson(array.getJSONObject(it)) }.associateBy { it.entityId }
        }
        val registry = JSONObject(File(dir, "registry.json").readText())
        val entityDevices = registry.getJSONObject("entityDevices").let { o -> o.keys().asSequence().associateWith { o.getString(it) } }
        val devices = registry.getJSONObject("devices").let { o ->
            o.keys().asSequence().associateWith { id ->
                val d = o.getJSONObject(id)
                HaDeviceInfo(d.optString("name"), d.optString("model").takeIf { it.isNotEmpty() && it != "null" }, null, d.optBoolean("service"))
            }
        }
        val auxiliary = registry.optJSONArray("auxiliary")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty()
        val vm = HavenViewModel(ApplicationProvider.getApplicationContext<Application>())
        vm.settingsStore.themeMode = if (dark) "dark" else "light"
        vm.settingsStore.colorStyleKey = style
        vm.client.restoreCachedStates(states, emptyList(), emptyMap(), entityDevices, devices, auxiliary)
        vm.loginWithToken("http://127.0.0.1:9", "screenshot-token")
        return vm
    }

    private fun shoot(name: String, qualifiers: String, vm: HavenViewModel, then: (() -> Unit)? = null) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.recreate()
        compose.setContent { HavenApp(vm) }
        compose.mainClock.advanceTimeBy(1_500)
        if (then != null) {
            then()
            compose.mainClock.advanceTimeBy(1_500)
        }
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun tapTab(label: String) = compose.onAllNodesWithContentDescription(label).onLast().performClick()

    @Test fun real_home_light() = shoot("real_home_light", TALL, realHome(dark = false))

    @Test fun real_home_dark() = shoot("real_home_dark", TALL, realHome(dark = true))

    @Test fun real_home_home_assistant() = shoot("real_home_home_assistant", TALL, realHome(dark = false, style = "home_assistant"))

    @Test fun real_climate_light() = shoot("real_climate_light", TALL, realHome(dark = false)) { tapTab("Climate") }

    @Test fun real_security_light() = shoot("real_security_light", TALL, realHome(dark = false)) { tapTab("Security") }

    @Test fun real_home_unfolded() = shoot("real_home_unfolded", "w884dp-h1500dp-xhdpi", realHome(dark = false))

    private companion object {
        /** The cover screen's width, tall enough for a whole page. */
        const val TALL = "w384dp-h2600dp-xhdpi"
    }
}
