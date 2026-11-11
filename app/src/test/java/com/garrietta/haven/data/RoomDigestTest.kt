package com.garrietta.haven.data

import com.garrietta.haven.ui.theme.StateFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDigestTest {

    private fun entity(id: String, state: String, name: String? = null, attrs: Map<String, Any?> = emptyMap()) =
        HaEntity(id, state, attrs + (name?.let { mapOf("friendly_name" to it) } ?: emptyMap()))

    private val sofa = entity("light.sofa", "on", "Living Room Sofa Lamp")
    private val shelf = entity("light.shelf", "on", "Shelf Light")
    private val tv = entity("media_player.tv", "playing", "TV")
    private val blinds = entity("cover.blinds", "open", "Blinds", mapOf("device_class" to "blind"))
    private val heating = entity(
        "climate.bedroom", "heat", "Thermostat",
        mapOf("hvac_action" to "heating", "temperature" to 20.0, "current_temperature" to 18.5),
    )
    private val frontDoor = entity("lock.front_door", "locked", "Front Door Lock")

    @Test
    fun aLitRoomIsOnAndSaysWhichLights() {
        val digest = roomDigest("Living Room", listOf(sofa, shelf, tv))
        assertEquals(StateFamily.On, digest.family)
        assertEquals("Sofa Lamp and Shelf Light are on, TV is playing", digest.summary)
    }

    @Test
    fun threeLightsAreCountedNotListed() {
        val third = entity("light.floor", "on", "Floor Lamp")
        assertEquals("3 lights on", roomDigest("Living Room", listOf(sofa, shelf, third)).summary)
    }

    @Test
    fun aRoomWithNothingHappeningIsQuietWhateverItHolds() {
        val digest = roomDigest(
            "Kitchen",
            listOf(entity("light.kitchen", "off"), entity("switch.kettle", "off"), entity("sensor.t", "21")),
        )
        assertTrue(digest.quiet)
        assertEquals("Everything is off", digest.summary)
    }

    @Test
    fun heatingColorsARoomOnlyWhileItIsActuallyHeating() {
        val digest = roomDigest("Bedroom", listOf(heating, entity("light.bedside", "off")))
        assertEquals(StateFamily.Heat, digest.family)
        assertEquals("Heating to 20°", digest.summary)
        assertEquals("18.5°", digest.reading)

        val idle = heating.copy(attributes = heating.attributes + ("hvac_action" to "idle"))
        assertTrue(roomDigest("Bedroom", listOf(idle)).quiet)
    }

    @Test
    fun aLampOutranksTheHeatingAndAnOpenDoorOutranksBoth() {
        val lamp = entity("light.bedside", "on", "Bedside Lamp")
        assertEquals(StateFamily.On, roomDigest("Bedroom", listOf(heating, lamp)).family)

        val garage = entity("cover.garage", "open", "Garage Door", mapOf("device_class" to "garage"))
        val digest = roomDigest("Garage", listOf(lamp, garage))
        assertEquals(StateFamily.Alert, digest.family)
        assertEquals("Garage Door is open, Bedside Lamp is on", digest.summary)
    }

    @Test
    fun openBlindsAreNotNews() {
        assertTrue(roomDigest("Bedroom", listOf(blinds)).quiet)
    }

    @Test
    fun aLockedDoorIsCalmAndAnUnlockedOneIsNot() {
        assertEquals(StateFamily.Secure, roomDigest("Entry", listOf(frontDoor)).family)
        assertEquals("Locked", roomDigest("Entry", listOf(frontDoor)).summary)

        val unlocked = frontDoor.copy(state = "unlocked")
        val digest = roomDigest("Entry", listOf(unlocked))
        assertEquals(StateFamily.Alert, digest.family)
        assertEquals("Front Door Lock is unlocked", digest.summary)
    }

    @Test
    fun switchesAreTheOperableThingsLitOnesFirstAndTheRestCounted() {
        val members = listOf(
            entity("light.a", "off"), entity("sensor.temp", "21"), sofa, entity("switch.b", "off"),
            tv, entity("fan.c", "off"), entity("light.gone", "unavailable"),
        )
        val digest = roomDigest("Living Room", members)
        assertEquals(listOf("light.sofa", "media_player.tv", "light.a", "switch.b"), digest.switches)
        assertEquals(1, digest.more)
    }

    @Test
    fun theReadingFallsBackToATemperatureSensor() {
        val sensor = entity("sensor.kitchen_temp", "21.0", attrs = mapOf("device_class" to "temperature"))
        assertEquals("21°", roomDigest("Kitchen", listOf(sensor)).reading)
        assertNull(roomDigest("Kitchen", listOf(entity("sensor.power", "320"))).reading)
    }

    @Test
    fun aNameLosesItsOwnRoomButNotAllOfItself() {
        assertEquals("Sofa Lamp", nameInRoom("Living Room Sofa Lamp", "Living Room"))
        assertEquals("Kitchen", nameInRoom("Kitchen", "Kitchen"))
        assertEquals("TV", nameInRoom("TV", "Living Room"))
        assertEquals("Garage Door", nameInRoom("Garage Door", "Garage"))
    }

    @Test
    fun theHeadlineCountsLightsInWordsAndTheWordTurnsThemOff() {
        assertEquals("All quiet.", homeHeadline(emptyList()).resting)
        assertEquals("All lights off.", homeHeadline(listOf(entity("light.a", "off"))).resting)

        val one = homeHeadline(listOf(sofa))
        assertNull(one.resting)
        assertEquals("One light", one.clauses.single().text)
        assertEquals("on", one.clauses.single().state)
        assertEquals(StateFamily.Light, one.clauses.single().family)

        val two = homeHeadline(listOf(sofa, shelf, entity("light.off", "off"))).clauses.single()
        assertEquals("Two lights", two.text)
        assertEquals(HeadlineAction.TurnOff(listOf("light.sofa", "light.shelf")), two.action)

        val many = (1..14).map { entity("light.l$it", "on") }
        assertEquals("14 lights", homeHeadline(many).clauses.single().text)
    }

    @Test
    fun anOpenDoorIsALineAndItsWordClosesIt() {
        val garage = entity("cover.garage", "open", "Garage Door", mapOf("device_class" to "garage"))
        val headline = homeHeadline(listOf(sofa, garage, frontDoor))
        assertEquals(listOf("on", "open"), headline.clauses.map { it.state })
        val open = headline.clauses[1]
        assertEquals("Garage Door is", open.text)
        assertEquals(StateFamily.Alert, open.family)
        assertEquals(HeadlineAction.Close(listOf("cover.garage")), open.action)
    }

    @Test
    fun anUnlockedDoorLocksOnATapButALockedOneOnlyOpensItsControl() {
        val back = entity("lock.back_door", "unlocked", "Back Door Lock")
        val unlocked = homeHeadline(listOf(frontDoor, back)).clauses.single()
        assertEquals("Back Door is", unlocked.text)
        assertEquals("unlocked", unlocked.state)
        assertEquals(HeadlineAction.Lock(listOf("lock.back_door")), unlocked.action)

        val calm = homeHeadline(listOf(frontDoor)).clauses.single()
        assertEquals("Front Door is", calm.text)
        assertEquals("locked", calm.state)
        assertEquals(StateFamily.Secure, calm.family)
        assertEquals(HeadlineAction.Show("lock.front_door"), calm.action)

        val both = homeHeadline(listOf(frontDoor, back.copy(state = "locked"))).clauses.single()
        assertEquals("Doors are", both.text)
    }

    @Test
    fun aRunningVacuumAndAPlayingTvEachGetALineThatStopsThem() {
        val vacuum = entity("vacuum.robot", "cleaning", "Robot Vacuum")
        val headline = homeHeadline(listOf(vacuum, tv))
        assertNull(headline.resting)
        assertEquals(listOf("Robot Vacuum is", "TV is"), headline.clauses.map { it.text })
        assertEquals(HeadlineAction.Dock(listOf("vacuum.robot")), headline.clauses[0].action)
        assertEquals(HeadlineAction.Pause(listOf("media_player.tv")), headline.clauses[1].action)
    }

    @Test
    fun theHeadlineStopsAtThreeLinesAndKeepsWhatMattersMost() {
        val garage = entity("cover.garage", "open", "Garage Door", mapOf("device_class" to "garage"))
        val back = entity("lock.back_door", "unlocked", "Back Door Lock")
        val vacuum = entity("vacuum.robot", "cleaning", "Robot Vacuum")
        val headline = homeHeadline(listOf(sofa, garage, back, vacuum, tv))
        assertEquals(listOf("on", "open", "unlocked"), headline.clauses.map { it.state })
    }

    @Test
    fun theSublineUsesWhateverIsKnownAndNamesTheThermostat() {
        val weather = entity("weather.home", "cloudy", attrs = mapOf("temperature" to 9.0))
        val both = homeSubline(listOf(heating), weather)!!
        assertEquals("18.5° inside", both.inside)
        assertEquals("climate.bedroom", both.thermostatId)
        assertEquals("9° outside", both.outside)
        assertNull(homeSubline(emptyList(), weather)!!.inside)
        assertNull(homeSubline(emptyList(), null))
    }
}
