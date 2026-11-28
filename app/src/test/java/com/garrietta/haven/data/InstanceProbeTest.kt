package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstanceProbeTest {

    @Test
    fun normalize_trimsWhitespaceAndTrailingSlash() {
        assertEquals("http://10.0.0.11:8123", normalizeInstanceUrl("  http://10.0.0.11:8123/ "))
    }

    @Test
    fun normalize_bareLanIpGetsHttp() {
        assertEquals("http://10.0.0.11:8123", normalizeInstanceUrl("10.0.0.11:8123"))
    }

    @Test
    fun normalize_localHostnameGetsHttp() {
        assertEquals("http://homeassistant.local:8123", normalizeInstanceUrl("homeassistant.local:8123"))
        assertEquals("http://myserver:8123", normalizeInstanceUrl("myserver:8123"))
    }

    @Test
    fun normalize_publicHostnameGetsHttps() {
        assertEquals("https://home.example.com", normalizeInstanceUrl("home.example.com"))
    }

    @Test
    fun normalize_keepsExplicitScheme() {
        assertEquals("https://home.example.com", normalizeInstanceUrl("https://home.example.com/"))
        assertEquals("http://10.0.0.11:8123", normalizeInstanceUrl("http://10.0.0.11:8123"))
    }

    @Test
    fun manifest_homeAssistantNameMatches() {
        assertTrue(isHomeAssistantManifest(JSONObject("{\"name\": \"Home Assistant\", \"version\": \"2026.10\"}")))
        assertTrue(isHomeAssistantManifest(JSONObject("{\"name\": \"home assistant\"}")))
    }

    @Test
    fun manifest_otherNamesReject() {
        assertFalse(isHomeAssistantManifest(JSONObject("{\"name\": \"Something Else\"}")))
        assertFalse(isHomeAssistantManifest(JSONObject("{}")))
    }

    @Test
    fun failureMessages_arePlainLanguageAndActionable() {
        assertTrue(probeFailureMessage(ProbeResult.Timeout).contains("home Wi-Fi"))
        assertTrue(probeFailureMessage(ProbeResult.ConnectionRefused).contains("8123"))
        assertTrue(probeFailureMessage(ProbeResult.Unreachable).contains("same network"))
        assertTrue(probeFailureMessage(ProbeResult.HttpError(404)).contains("404"))
        assertTrue(probeFailureMessage(ProbeResult.NotHomeAssistant).contains("isn't Home Assistant"))
        assertTrue(probeFailureMessage(ProbeResult.NoNetwork).contains("no network"))
    }
}
