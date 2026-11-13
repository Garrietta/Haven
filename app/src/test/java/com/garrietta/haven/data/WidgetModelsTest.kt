package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetModelsTest {

    private fun e(id: String, state: String, name: String, vararg attrs: Pair<String, Any?>) =
        HaEntity(id, state, mapOf<String, Any?>("friendly_name" to name) + attrs)

    @Test
    fun theClimateWidgetShowsTheThermostatAtWork() {
        val states = listOf(
            e("climate.a", "heat", "Attic", "hvac_action" to "idle", "temperature" to 60),
            e("climate.main", "heat", "Main Floor", "hvac_action" to "heating", "temperature" to 70, "current_temperature" to 67, "current_humidity" to 41),
        ).associateBy { it.entityId }
        val model = climateWidgetModel(states)!!
        assertEquals("Main Floor", model.name)
        assertEquals("heating", model.mood)
        assertEquals("Heating to 70°", model.activity)
        assertEquals(71.0, nudgedSetpoint(model, up = true)!!, 0.0)
        assertEquals(69.0, nudgedSetpoint(model, up = false)!!, 0.0)
    }

    @Test
    fun noThermostatNoClimateWidget() {
        assertNull(climateWidgetModel(mapOf("light.a" to e("light.a", "on", "A"))))
    }

    @Test
    fun controlsAreTheSwitchableFavoritesInOrder() {
        val states = listOf(
            e("light.lamp", "on", "Lamp"),
            e("switch.fan", "off", "Fan"),
            e("sensor.temp", "70", "Temp"),
        ).associateBy { it.entityId }
        val tiles = controlTiles(states, favorites = listOf("switch.fan", "sensor.temp", "light.lamp"))
        assertEquals(listOf("Fan", "Lamp"), tiles.map { it.name })
        assertEquals(listOf(false, true), tiles.map { it.on })
    }

    @Test
    fun withNoFavoritesTheLightsThatAreOnStandIn() {
        val states = listOf(e("light.b", "on", "B"), e("light.a", "on", "A"), e("light.c", "off", "C")).associateBy { it.entityId }
        assertEquals(listOf("A", "B"), controlTiles(states, emptyList()).map { it.name })
    }
}
