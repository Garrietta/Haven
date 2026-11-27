package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The names devices go by in Haven, and what counts as a device on Home. */
class DeviceNamesTest {

    @Test
    fun aPartThatSaysWhatItIsIsTheName() {
        assertEquals("Garage Door", deviceDisplayName("Smart Garage Door Opener Garage Door", "Smart Garage Door Opener"))
    }

    @Test
    fun aSocketStreamOrChannelIsTheDevice() {
        assertEquals("Standing lamp", deviceDisplayName("Standing lamp Socket", "Standing lamp"))
        assertEquals("Lamp", deviceDisplayName("Lamp Socket 1", "Lamp"))
        assertEquals("Outdoor camera", deviceDisplayName("Outdoor camera HD Stream (Direct)", "Outdoor camera"))
        assertEquals("Front Door", deviceDisplayName("Front Door Live view", "Front Door"))
    }

    @Test
    fun aNameNotBuiltOnItsDeviceStays() {
        assertEquals("Greenhouse Fan", deviceDisplayName("Greenhouse Fan", "Unnamed P400M"))
        assertEquals("Ecobee Thermostat", deviceDisplayName("Ecobee Thermostat", null))
        assertEquals("Lamp", deviceDisplayName("Lamp", "Lamp"))
    }

    @Test
    fun aCameraLosesItsFeedWords() {
        fun cam(name: String) = HaEntity("camera.x", "idle", mapOf("friendly_name" to name))
        assertEquals("Front Door", displayCameraName(cam("Front Door Live view")))
        assertEquals("Outdoor camera", displayCameraName(cam("Outdoor camera HD Stream (Direct)")))
        assertEquals("Garage Camera", displayCameraName(cam("Garage Camera")))
        assertEquals("Live", displayCameraName(cam("Live")))
    }

    private val now = java.time.OffsetDateTime.parse("2026-11-27T03:00:00+00:00").toInstant().toEpochMilli()
    private fun e(id: String, state: String, name: String) =
        HaEntity(id, state, mapOf("friendly_name" to name), lastChanged = "2026-11-26T12:00:00+00:00")

    @Test
    fun helpersAndServicesAreNotDevices() {
        val states = listOf(
            e("light.lamp", "on", "Lamp"),
            e("input_boolean.material_you_navbar_home", "on", "Material You Show Navigation Bar Home"),
            e("switch.theme_dark_mode", "on", "Theme Dark mode"),
        ).associateBy { it.entityId }
        val entityDevices = mapOf("light.lamp" to "d_lamp", "switch.theme_dark_mode" to "d_theme")
        val devices = mapOf("d_lamp" to DeviceFacts("Lamp", "Plug"), "d_theme" to DeviceFacts("Material You Theme", null, service = true))
        val shown = homeGroups(states, entityDevices, devices, emptySet(), now).flatMap { it.entityIds }
        assertEquals(listOf("light.lamp"), shown)
    }

    @Test
    fun beforeTheRegistryArrivesEverythingStays() {
        val states = listOf(e("light.lamp", "on", "Lamp"), e("input_boolean.guest_mode", "off", "Guest mode")).associateBy { it.entityId }
        val shown = homeGroups(states, emptyMap(), emptyMap(), emptySet(), now).flatMap { it.entityIds }
        assertTrue("light.lamp" in shown && "input_boolean.guest_mode" in shown)
    }

    @Test
    fun aGreenhouseReadingWithoutADeviceIsNotShown() {
        val states = listOf(
            e("switch.greenhouse_fan", "off", "Greenhouse Fan"),
            HaEntity("sensor.greenhouse_predicted_temp", "0.0", mapOf("friendly_name" to "Greenhouse predicted temp", "device_class" to "temperature")),
        ).associateBy { it.entityId }
        val places = homeGroups(states, mapOf("switch.greenhouse_fan" to "d_outlet"), mapOf("d_outlet" to DeviceFacts("Unnamed P400M", "P400M")), emptySet(), now)
        assertFalse(places.single().readingIds.contains("sensor.greenhouse_predicted_temp"))
    }

    @Test
    fun anOpenEntranceIsAWordOrTwo() {
        val states = listOf(
            HaEntity("cover.garage", "open", mapOf("friendly_name" to "Smart Garage Door Opener Garage Door", "device_class" to "garage"), lastChanged = "2026-11-26T12:00:00+00:00"),
        ).associateBy { it.entityId }
        val security = homeStatus(states, emptyList(), now).first { it.kind == StatusKind.Security }
        assertEquals("Garage open", security.value)
    }
}

/** How choices wrap, and how readings are written. */
class ChoicesAndReadingsTest {
    @Test
    fun choicesShareARowWhenTheyFitAndSplitEvenlyWhenNot() {
        assertEquals(4, com.garrietta.haven.ui.components.choicesPerRow(4, 400f))
        assertEquals(2, com.garrietta.haven.ui.components.choicesPerRow(4, 300f))
        assertEquals(4, com.garrietta.haven.ui.components.choicesPerRow(4, 352f))
        assertEquals(3, com.garrietta.haven.ui.components.choicesPerRow(6, 300f))
        assertEquals(1, com.garrietta.haven.ui.components.choicesPerRow(2, 120f))
        assertEquals(2, com.garrietta.haven.ui.components.choicesPerRow(2, 200f))
        assertEquals(1, com.garrietta.haven.ui.components.choicesPerRow(1, 50f))
    }

    @Test
    fun readingsAreRoundedTheWayTheyAreRead() {
        assertEquals("66.4 °F", formatReading("66.38", "°F", "temperature"))
        assertEquals("66 °F", formatReading("66.0", "°F", "temperature"))
        assertEquals("54%", formatReading("54.2", "%", "humidity"))
        assertEquals("742 W", formatReading("742.4", "W", "power"))
        assertEquals("3.14 kWh", formatReading("3.1415", "kWh", "energy"))
        assertEquals("on", formatReading("on", null, null))
    }
}
