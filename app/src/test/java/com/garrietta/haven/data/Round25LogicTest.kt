package com.garrietta.haven.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.components.reorderSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round 25: reconnect fallback selection + auth-retry backoff
 * (item 1), state-cache round-trip (item 2), Tall pair height
 * math (item 6), reorder center-crossing steps (item 7). All pure
 * functions — no Android runtime. The device checks (cold launch
 * paints cached home + cameras on frame 0, status tap reconnects
 * without signing out, Tall edges aligned, drag reorder floats
 * with a placeholder and persists) are covered by the compile +
 * device checks.
 */
class Round25LogicTest {

    // ---- Item 1: reconnect fallback selection ----

    @Test
    fun alternateTarget_autoWifiFallsBackToCloud() {
        assertEquals(
            "https://cloud.example",
            alternateConnectionTarget(ConnectionMode.AUTO, "http://10.0.0.11:8123", "https://cloud.example", true),
        )
    }

    @Test
    fun alternateTarget_autoCellularFallsBackToLocal() {
        assertEquals(
            "http://10.0.0.11:8123",
            alternateConnectionTarget(ConnectionMode.AUTO, "http://10.0.0.11:8123", "https://cloud.example", false),
        )
    }

    @Test
    fun alternateTarget_pinnedModesSwapSides() {
        assertEquals(
            "https://cloud.example",
            alternateConnectionTarget(ConnectionMode.LOCAL, "http://10.0.0.11:8123", "https://cloud.example", true),
        )
        assertEquals(
            "http://10.0.0.11:8123",
            alternateConnectionTarget(ConnectionMode.CLOUD, "http://10.0.0.11:8123", "https://cloud.example", false),
        )
    }

    @Test
    fun alternateTarget_missingAlternateKeepsCurrent() {
        // Only a local URL stored: no alternate exists, so the
        // current target is returned (the caller must not switch
        // to null).
        assertEquals(
            "http://10.0.0.11:8123",
            alternateConnectionTarget(ConnectionMode.AUTO, "http://10.0.0.11:8123", null, true),
        )
    }

    @Test
    fun alternateTarget_bothUnsetIsNull() {
        assertNull(alternateConnectionTarget(ConnectionMode.AUTO, null, null, true))
    }

    @Test
    fun authRetryDelay_growsThenCapsAtSixtySeconds() {
        assertEquals(2_000L, authRetryDelayMs(0))
        assertEquals(5_000L, authRetryDelayMs(1))
        assertEquals(10_000L, authRetryDelayMs(2))
        assertEquals(30_000L, authRetryDelayMs(3))
        assertEquals(60_000L, authRetryDelayMs(4))
        assertEquals(60_000L, authRetryDelayMs(99))
        assertEquals(2_000L, authRetryDelayMs(-1))
    }

    // ---- Item 2: state-cache round-trip ----

    @Test
    fun stateCache_roundTrip() {
        val states = mapOf(
            "light.lamp" to HaEntity("light.lamp", "on", mapOf("friendly_name" to "Lamp", "brightness" to 200)),
            "sensor.temp" to HaEntity(
                "sensor.temp",
                "69.62",
                mapOf("device_class" to "temperature", "unit_of_measurement" to "°F"),
            ),
            "climate.thermostat" to HaEntity(
                "climate.thermostat",
                "heat",
                mapOf(
                    "friendly_name" to "Ecobee",
                    "nested" to mapOf("a" to 1),
                    "list" to listOf("x", 2),
                ),
                lastChanged = "2026-11-05T10:00:00Z",
            ),
        )
        val areas = listOf(
            HaArea("living_room", "Living Room", "mdi:sofa"),
            HaArea("bedroom", "Bedroom"),
        )
        val entityAreas = mapOf(
            "light.lamp" to "living_room",
            "climate.thermostat" to "bedroom",
        )
        val parsed = parseStateCache(serializeStateCache(states, areas, entityAreas))
        assertEquals(states, parsed?.states)
        assertEquals(areas, parsed?.areas)
        assertEquals(entityAreas, parsed?.entityAreas)
    }

    @Test
    fun stateCache_emptyDocumentParsesToEmpty() {
        val parsed = parseStateCache("{\"states\":[],\"areas\":[],\"entityAreas\":{}}")
        assertEquals(emptyMap<String, HaEntity>(), parsed?.states)
        assertEquals(emptyList<HaArea>(), parsed?.areas)
        assertEquals(emptyMap<String, String>(), parsed?.entityAreas)
    }

    @Test
    fun stateCache_corruptInputIsNull() {
        assertNull(parseStateCache("not json"))
        assertNull(parseStateCache(""))
        assertNull(parseStateCache("{}"))
    }

    // ---- Item 6: Tall pair height math ----

    @Test
    fun tallPairHeight_foldedSpacingMatchesTallCard() {
        assertEquals(284.dp, tallPairHeightDp(12.dp))
        assertEquals(tallCardHeightDp(), tallPairHeightDp(12.dp))
    }

    @Test
    fun tallPairHeight_unfoldedSpacingIs286() {
        assertEquals(286.dp, tallPairHeightDp(14.dp))
    }

    // ---- Item 7: reorder center-crossing steps ----

    @Test
    fun reorderSteps_subThresholdNoStep() {
        val steps = reorderSteps(40f, 20f, 100f, 74f)
        assertEquals(0, steps.dxSteps)
        assertEquals(0, steps.dySteps)
        assertEquals(Offset(40f, 20f), steps.remaining)
    }

    @Test
    fun reorderSteps_halfCellCrossingStepsOnceAndRebases() {
        // Half a cell is 100px; dragging 120px crosses it once
        // and re-bases by a full cell (200px), leaving -80px.
        val steps = reorderSteps(120f, 0f, 100f, 74f)
        assertEquals(1, steps.dxSteps)
        assertEquals(0, steps.dySteps)
        assertEquals(Offset(-80f, 0f), steps.remaining)
    }

    @Test
    fun reorderSteps_negativeCrossingStepsBackwards() {
        val steps = reorderSteps(-120f, -160f, 100f, 74f)
        assertEquals(-1, steps.dxSteps)
        assertEquals(-1, steps.dySteps)
        assertEquals(Offset(80f, -12f), steps.remaining)
    }

    @Test
    fun reorderSteps_longDragWalksMultipleSlots() {
        val steps = reorderSteps(450f, 0f, 100f, 74f)
        assertEquals(2, steps.dxSteps)
        assertEquals(Offset(50f, 0f), steps.remaining)
    }
}
