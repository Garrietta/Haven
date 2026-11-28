package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YamlCardTest {

    @Test
    fun parseSimpleCard() {
        val card = parseCardYaml("type: thermostat\nentity: climate.thermostat\n")
        assertEquals("thermostat", cardType(card))
        assertEquals("climate.thermostat", card.optString("entity"))
    }

    @Test
    fun jsonInputParsesToo() {
        val card = parseCardYaml("{\"type\": \"light\", \"entity\": \"light.lamp_socket_1\"}")
        assertEquals("light", cardType(card))
        assertEquals("light.lamp_socket_1", card.optString("entity"))
    }

    @Test
    fun changeTypeRoundTrips() {
        val card = changeCardType(parseCardYaml("type: tile\nentity: cover.garage_door\n"), "button")
        val yaml = cardToYaml(card)
        val reparsed = parseCardYaml(yaml)
        assertEquals("button", cardType(reparsed))
        assertEquals("cover.garage_door", reparsed.optString("entity"))
    }

    @Test
    fun listOfMapsRoundTrips() {
        val yaml = "type: entities\nentities:\n  - entity: light.lamp_socket_1\n    name: Lamp\n  - light.standing_lamp_socket_1\n"
        val reparsed = parseCardYaml(cardToYaml(parseCardYaml(yaml)))
        assertEquals("entities", cardType(reparsed))
        val entities = reparsed.optJSONArray("entities")!!
        assertEquals(2, entities.length())
        assertEquals("light.lamp_socket_1", entities.optJSONObject(0).optString("entity"))
        assertEquals("Lamp", entities.optJSONObject(0).optString("name"))
        assertEquals("light.standing_lamp_socket_1", entities.optString(1))
    }

    @Test
    fun quotedScalarsSurvive() {
        val card = parseCardYaml("type: markdown\ncontent: \"Hello: world\"\n")
        val reparsed = parseCardYaml(cardToYaml(card))
        assertEquals("Hello: world", reparsed.optString("content"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonMappingRejected() {
        parseCardYaml("- just\n- a\n- list\n")
    }

    @Test
    fun brokenYamlThrows() {
        try {
            parseCardYaml("type: [unclosed\n")
            assertTrue("expected a parse exception", false)
        } catch (e: Exception) {
            // Any snakeyaml parse failure is the correct outcome.
            assertTrue(e.message?.isNotEmpty() == true)
        }
    }
}
