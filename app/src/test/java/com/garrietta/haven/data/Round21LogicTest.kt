package com.garrietta.haven.data

import androidx.compose.ui.graphics.Color
import com.garrietta.haven.Screen
import com.garrietta.haven.ui.components.compositeOver
import com.garrietta.haven.ui.components.contrastRatio
import com.garrietta.haven.ui.components.morphCardAlpha
import com.garrietta.haven.ui.components.topMorphProgress
import com.garrietta.haven.ui.components.topMorphSourceId
import com.garrietta.haven.ui.screens.RoomFilter
import com.garrietta.haven.ui.screens.RoomSection
import com.garrietta.haven.ui.screens.buildHomeChips
import com.garrietta.haven.ui.screens.dialTrackColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors

/**
 * Feedback round 21: the room-chip tap mapping (item 1), the
 * thermostat dial track contrast (item 2), and the Front Door
 * RTSP fallback + badge rules (item 3), the gated detail accent
 * (item 4), and the tile-becomes-popup alpha curve + topmost-
 * source priority (item 8). All pure functions —
 * no Android runtime. Items 5–7 are structural (one popup
 * surface, one tile path, long-press Arrange entry) with no
 * pure logic to unit-test; they are covered by the compile +
 * device checks. The Round 20 resize snap/persistence tests
 * still cover the Arrange behavior itself.
 */
class Round21LogicTest {

    // Item 1: a room chip tap dispatches its room's filter.
    @Test
    fun roomChipTapSwitchesFilter() {
        var filter: RoomFilter? = null
        var navigated: Screen? = null
        val rooms = listOf(
            RoomSection("bedroom", "Bedroom", listOf("light.lamp_socket_1"), null),
            RoomSection("living", "Living Room", listOf("light.standing_lamp_socket_1"), null),
            RoomSection(null, "No room", listOf("sensor.temperature"), null),
        )
        val chips = buildHomeChips(
            rooms, RoomFilter.All, { Placement.ROOM_CHIP }, emptyList(),
            onRoomFilter = { filter = it },
            onNavigateTab = { navigated = it },
        )
        assertEquals(4, chips.size)
        assertTrue(chips[0].selected)
        chips[1].onClick()
        assertEquals(RoomFilter.Area("bedroom"), filter)
        chips[2].onClick()
        assertEquals(RoomFilter.Area("living"), filter)
        chips[3].onClick()
        assertEquals(RoomFilter.Unassigned, filter)
        chips[0].onClick()
        assertEquals(RoomFilter.All, filter)
        assertNull(navigated)
    }

    // Item 1: a hidden room produces no chip; a nav-tab room
    // produces no chip either.
    @Test
    fun nonChipRoomsProduceNoChip() {
        val rooms = listOf(RoomSection("bedroom", "Bedroom", emptyList(), null))
        val hidden = buildHomeChips(
            rooms, RoomFilter.All, { Placement.HIDDEN }, emptyList(),
            onRoomFilter = {}, onNavigateTab = {},
        )
        assertEquals(1, hidden.size)
        val navTab = buildHomeChips(
            rooms, RoomFilter.All, { Placement.NAV_TAB }, emptyList(),
            onRoomFilter = {}, onNavigateTab = {},
        )
        assertEquals(1, navTab.size)
    }

    // Item 1: a tab chip navigates instead of filtering.
    @Test
    fun tabChipTapNavigates() {
        var filter: RoomFilter? = null
        var navigated: Screen? = null
        val tabs = listOf(UserTab("t1", "Lights", listOf("light.lamp_socket_1"), chip = true))
        val chips = buildHomeChips(
            emptyList(), RoomFilter.All, { Placement.ROOM_CHIP }, tabs,
            onRoomFilter = { filter = it },
            onNavigateTab = { navigated = it },
        )
        assertEquals(2, chips.size)
        assertFalse(chips[1].selected)
        chips[1].onClick()
        assertEquals(Screen.UserTab("t1"), navigated)
        assertNull(filter)
    }

    // Item 2: the dial track clears 3:1 against the plain disc in
    // both themes (the round-8 outline track measured ~1.3:1).
    @Test
    fun dialTrackClearsThreeToOneDark() {
        val disc = Color(0xFF2B2B2B) // dark surfaceContainerHigh
        val track = dialTrackColor(disc, Color(0xFFF2F2EF)) // dark onSurface
        assertTrue(contrastRatio(track, disc) >= 3.0)
    }

    @Test
    fun dialTrackClearsThreeToOneLight() {
        val disc = Color(0xFFDDE5D0) // light surfaceContainerHigh
        val track = dialTrackColor(disc, Color(0xFF1E1E1A)) // light onSurface
        assertTrue(contrastRatio(track, disc) >= 3.0)
    }

    // Item 2: the old outline track really was near-invisible, and
    // the new track holds its floor even on the 20% mode-tinted
    // popout disc.
    @Test
    fun dialTrackBeatsTheOldOutlineTrack() {
        val disc = Color(0xFF2B2B2B)
        assertTrue(contrastRatio(Color(0xFF3F3F3A), disc) < 2.0)
        assertTrue(contrastRatio(dialTrackColor(disc, Color(0xFFF2F2EF)), disc) >= 3.0)
    }

    @Test
    fun dialTrackHoldsOnModeTintedDisc() {
        val plain = Color(0xFF2B2B2B)
        val tinted = compositeOver(Color(0xFFFF8F00).copy(alpha = 0.20f), plain)
        val track = dialTrackColor(tinted, Color(0xFFF2F2EF))
        assertTrue(contrastRatio(track, tinted) >= 3.0)
    }

    // Item 3: the RTSP URL derives from the local instance host.
    @Test
    fun rtspUrlFromLocalInstance() {
        assertEquals(
            "rtsp://10.0.0.11:8554/5c475ecd13a7_live",
            frontDoorRtspUrl("http://10.0.0.11:8123"),
        )
        assertEquals(
            "rtsp://192.168.1.5:8554/5c475ecd13a7_live",
            frontDoorRtspUrl("http://192.168.1.5:8123/"),
        )
    }

    // Item 3: no RTSP attempt off-LAN — the feed exists only on
    // the Pi, and no host is ever guessed.
    @Test
    fun rtspUrlNullOffLan() {
        assertNull(frontDoorRtspUrl("https://abc123.ui.nabu.casa"))
        assertNull(frontDoorRtspUrl("https://example.com:8123"))
        assertNull(frontDoorRtspUrl(null))
        assertNull(frontDoorRtspUrl(""))
    }

    // Item 3: RTSP frames earn the LIVE badge on their own, with
    // no switch involved; the old rules are unchanged.
    @Test
    fun rtspFramesEarnLiveBadge() {
        assertTrue(frontDoorShowsLiveBadge(false, false, false, rtspFrames = true))
        assertFalse(frontDoorShowsLiveBadge(false, false, false))
        assertFalse(frontDoorShowsLiveBadge(false, true, false))
        assertTrue(frontDoorShowsLiveBadge(false, true, true))
        assertTrue(frontDoorShowsLiveBadge(true, false, false))
    }

    // Item 3: the quadrant reads LIVE for the fallback only once
    // the RTSP fallback has proven itself streaming this session.
    @Test
    fun quadrantLiveForProvenFallback() {
        assertTrue(
            frontDoorQuadrantShowsLive(FRONT_DOOR_LIVE_VIEW_ID, false, rtspProven = true),
        )
        assertFalse(
            frontDoorQuadrantShowsLive(FRONT_DOOR_LIVE_VIEW_ID, false, rtspProven = false),
        )
        assertFalse(
            frontDoorQuadrantShowsLive(FRONT_DOOR_LIVE_VIEW_ID, true, rtspProven = true),
        )
        // The badge is earned by live frames whichever entity the
        // picture comes from: an available camera with no live frame
        // yet reads Recording.
        assertFalse(
            frontDoorQuadrantShowsLive(FRONT_DOOR_RING_MQTT_CAMERA_ID, false),
        )
        assertTrue(
            frontDoorQuadrantShowsLive(FRONT_DOOR_RING_MQTT_CAMERA_ID, false, rtspProven = true),
        )
        assertFalse(
            frontDoorQuadrantShowsLive(FRONT_DOOR_RING_MQTT_CAMERA_ID, true),
        )
        assertFalse(frontDoorQuadrantShowsLive(null, false, rtspProven = true))
    }

    // Item 4: the panel accent follows the tile — a preset that
    // does not apply for the current state yields null, so the
    // panel falls back to the matrix/scheme colors the tile shows
    // (the closed garage's cooling-blue preset must NOT blue the
    // panel under a green tile).
    @Test
    fun detailAccentGatedByState() {
        val garage = "cover.smart_garage_door_24090408838301610702c4e7ae0ae280_garage"
        val presets = mapOf(garage to "cooling")
        assertNull(
            detailPageAccent(presets, HaEntity(garage, "closed", emptyMap()), true),
        )
        assertEquals(
            StateFamily.Cool.colors(isDark = true).container,
            detailPageAccent(presets, HaEntity(garage, "open", emptyMap()), true),
        )
        assertEquals(
            StateFamily.Cool.colors(isDark = false).container,
            detailPageAccent(presets, HaEntity(garage, "open", emptyMap()), false),
        )
        assertNull(
            detailPageAccent(emptyMap(), HaEntity(garage, "open", emptyMap()), true),
        )
    }

    @Test
    fun detailAccentGatedForLights() {
        val presets = mapOf("light.lamp_socket_1" to "lime")
        assertEquals(
            StateFamily.On.colors(isDark = true).container,
            detailPageAccent(presets, HaEntity("light.lamp_socket_1", "on", emptyMap()), true),
        )
        assertNull(
            detailPageAccent(presets, HaEntity("light.lamp_socket_1", "off", emptyMap()), true),
        )
    }

    // Item 8: the source tile fades out by progress 0.4 on enter
    // and back in over the last 40% of exit — fully visible at
    // progress 0 (before any dismiss clears state), gone at 0.4
    // (before the popup content fades in). Spring overshoot
    // can never push the alpha out of range (clamped).
    @Test
    fun morphCardAlphaCurve() {
        assertEquals(1f, morphCardAlpha(0f), 0.001f)
        assertEquals(0.5f, morphCardAlpha(0.2f), 0.001f)
        assertEquals(0f, morphCardAlpha(0.4f), 0.001f)
        assertEquals(0f, morphCardAlpha(1f), 0.001f)
        assertEquals(1f, morphCardAlpha(-0.5f), 0.001f)
        assertEquals(0f, morphCardAlpha(1.5f), 0.001f)
    }

    // Item 8: the topmost morph source wins — a panel detail
    // stacked over a detail panel hides its own source card.
    @Test
    fun topMorphSourceIdPriority() {
        assertEquals("panel", topMorphSourceId("panel", "detail", "popout"))
        assertEquals("detail", topMorphSourceId(null, "detail", "popout"))
        assertEquals("popout", topMorphSourceId(null, null, "popout"))
        assertEquals("panel", topMorphSourceId("panel", null, null))
        assertNull(topMorphSourceId(null, null, null))
    }

    // Item 8: same priority for the progress the tile fades on;
    // no morph at all reads rest (1f — fully visible).
    @Test
    fun topMorphProgressPriority() {
        assertEquals(0.3f, topMorphProgress(0.3f, 0.6f, 0.9f), 0.001f)
        assertEquals(0.6f, topMorphProgress(null, 0.6f, 0.9f), 0.001f)
        assertEquals(0.9f, topMorphProgress(null, null, 0.9f), 0.001f)
        assertEquals(1f, topMorphProgress(null, null, null), 0.001f)
    }
}
