package com.garrietta.haven.data

import com.garrietta.haven.ui.components.canPinBarExtra
import com.garrietta.haven.ui.components.nonTabSlideDirection
import com.garrietta.haven.ui.components.shouldApplyFoldedNavClearance
import com.garrietta.haven.ui.components.tabSlideOffsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feedback round 19: direction resolution, the front-door
 * quadrant badge rule, the placement selector mapping, the
 * bar-cap gate, the folded clearance rule, and the notification
 * trigger predicates. All pure functions — no Android runtime.
 */
class Round19LogicTest {

    // Item 2: back-navigation slide direction.
    @Test
    fun pushDirectionIsForward() {
        assertEquals(1, nonTabSlideDirection(isPop = false))
    }

    @Test
    fun popDirectionIsBackward() {
        assertEquals(-1, nonTabSlideDirection(isPop = true))
    }

    @Test
    fun backwardSlideOffsetsAreReversed() {
        // Backward: the incoming page starts left of the content
        // area (negative offset) and the outgoing page exits right.
        val (initial, target) = tabSlideOffsets(-1, 1000, 84)
        assertEquals(-1084, initial)
        assertEquals(1000, target)
    }

    // Item 4: the front-door quadrant badge rule, built literally.
    @Test
    fun quadrantShowsLiveOnlyForHealthyRingSource() {
        assertTrue(frontDoorQuadrantShowsLive(FRONT_DOOR_RING_MQTT_CAMERA_ID, false, rtspProven = true))
        // Being available is not being live: no frame, no badge.
        assertFalse(frontDoorQuadrantShowsLive(FRONT_DOOR_RING_MQTT_CAMERA_ID, false))
    }

    @Test
    fun quadrantShowsRecordingForFallbackFeed() {
        // 69a1 down, fallback feed playing: the live source is
        // what's down, so the badge stays Recording.
        assertFalse(frontDoorQuadrantShowsLive("camera.front_door_live_view", false))
    }

    @Test
    fun quadrantShowsRecordingWhenSourceIsNull() {
        assertFalse(frontDoorQuadrantShowsLive(null, false))
    }

    @Test
    fun quadrantShowsRecordingWhenRingSourceErrors() {
        assertFalse(frontDoorQuadrantShowsLive(FRONT_DOOR_RING_MQTT_CAMERA_ID, true))
    }

    // Item 5: placement selector mapping.
    @Test
    fun placementFromKeyMapsAllKeys() {
        assertEquals(Placement.NAV_TAB, placementFromKey("nav_tab"))
        assertEquals(Placement.HIDDEN, placementFromKey("hidden"))
        assertEquals(Placement.ROOM_CHIP, placementFromKey("room_chip"))
        assertEquals(Placement.ROOM_CHIP, placementFromKey("bogus"))
        assertEquals(Placement.ROOM_CHIP, placementFromKey(null))
    }

    @Test
    fun tabPlacementPinnedWinsOverChip() {
        assertEquals(Placement.NAV_TAB, tabPlacementFor(pinned = true, chip = true, hidden = false))
    }

    @Test
    fun tabPlacementChipBeatsHidden() {
        assertEquals(Placement.ROOM_CHIP, tabPlacementFor(pinned = false, chip = true, hidden = true))
    }

    @Test
    fun tabPlacementHiddenOnly() {
        assertEquals(Placement.HIDDEN, tabPlacementFor(pinned = false, chip = false, hidden = true))
    }

    @Test
    fun tabPlacementNeitherFlagIsHidden() {
        // Historical visibility: browse list only, never bar or
        // chips — so unpinned tabs do not flood the chip row.
        assertEquals(Placement.HIDDEN, tabPlacementFor(pinned = false, chip = false, hidden = false))
    }

    @Test
    fun barCapGateCountsTabsAndRoomsTogether() {
        assertTrue(canPinBarExtra(pinnedTabCount = 1, roomNavTabCount = 0, alreadyOnBar = false))
        assertTrue(canPinBarExtra(pinnedTabCount = 0, roomNavTabCount = 1, alreadyOnBar = false))
        assertFalse(canPinBarExtra(pinnedTabCount = 1, roomNavTabCount = 1, alreadyOnBar = false))
        assertFalse(canPinBarExtra(pinnedTabCount = 2, roomNavTabCount = 0, alreadyOnBar = false))
        // A group already on the bar is never disabled out from
        // under itself, even at the cap.
        assertTrue(canPinBarExtra(pinnedTabCount = 2, roomNavTabCount = 0, alreadyOnBar = true))
    }

    // Item 3: folded clearance is unconditional on folded layouts.
    @Test
    fun foldedClearanceAppliesOnFoldedOnly() {
        assertTrue(shouldApplyFoldedNavClearance(isWideLayout = false))
        assertFalse(shouldApplyFoldedNavClearance(isWideLayout = true))
    }

    // Item 6: notification trigger predicates.
    private fun event(
        entityId: String,
        domain: String,
        oldState: String?,
        newState: String?,
        deviceClass: String? = null,
        parentId: String? = null,
    ) = StateChangeEvent(
        entityId = entityId,
        domain = domain,
        oldState = oldState,
        newState = newState,
        deviceClass = deviceClass,
        friendlyName = null,
        parentId = parentId,
    )

    @Test
    fun garageCoverOpenAndOpeningNotify() {
        assertTrue(shouldNotifyGarageDoor(event("cover.garage", "cover", "closed", "open")))
        assertTrue(shouldNotifyGarageDoor(event("cover.garage", "cover", "closed", "opening")))
        assertFalse(shouldNotifyGarageDoor(event("cover.garage", "cover", "open", "closing")))
        assertFalse(shouldNotifyGarageDoor(event("cover.garage", "cover", "open", "closed")))
        assertFalse(shouldNotifyGarageDoor(event("cover.garage", "cover", "open", "open")))
    }

    @Test
    fun garageDoorSensorOffToOnNotifies() {
        assertTrue(shouldNotifyGarageDoor(event("binary_sensor.front_door", "binary_sensor", "off", "on", "door")))
        assertFalse(shouldNotifyGarageDoor(event("binary_sensor.front_door", "binary_sensor", "on", "off", "door")))
        assertFalse(shouldNotifyGarageDoor(event("binary_sensor.window", "binary_sensor", "off", "on", "window")))
    }

    @Test
    fun motionOnlyOffToOnNotifies() {
        assertTrue(shouldNotifyMotion(event("binary_sensor.front_door_motion", "binary_sensor", "off", "on", "motion")))
        assertFalse(shouldNotifyMotion(event("binary_sensor.front_door_motion", "binary_sensor", "on", "on", "motion")))
        assertFalse(shouldNotifyMotion(event("binary_sensor.front_door_motion", "binary_sensor", "on", "off", "motion")))
        assertFalse(shouldNotifyMotion(event("binary_sensor.front_door", "binary_sensor", "off", "on", "door")))
    }

    @Test
    fun lightShutoffExternalNotifiesButLocalTapDoesNot() {
        val external = event("light.lamp", "light", "on", "off")
        assertTrue(shouldNotifyLightShutoff(external, locallyActuated = false))
        assertFalse(shouldNotifyLightShutoff(external, locallyActuated = true))
        // An automation parent id always notifies, even inside the
        // local-tap suppression window.
        val automated = event("light.lamp", "light", "on", "off", parentId = "abc")
        assertTrue(shouldNotifyLightShutoff(automated, locallyActuated = true))
        assertFalse(shouldNotifyLightShutoff(event("light.lamp", "light", "off", "on"), locallyActuated = false))
        assertFalse(shouldNotifyLightShutoff(event("switch.lamp", "switch", "on", "off"), locallyActuated = false))
    }

    @Test
    fun resolveNotificationMapsKinds() {
        assertEquals(
            NotificationKind.GARAGE_DOOR,
            resolveNotification(event("cover.garage", "cover", "closed", "open"), locallyActuated = false),
        )
        assertEquals(
            NotificationKind.MOTION,
            resolveNotification(event("binary_sensor.motion", "binary_sensor", "off", "on", "motion"), locallyActuated = false),
        )
        assertEquals(
            NotificationKind.LIGHT_SHUTOFF,
            resolveNotification(event("light.lamp", "light", "on", "off"), locallyActuated = false),
        )
        assertNull(resolveNotification(event("light.lamp", "light", "on", "off"), locallyActuated = true))
        assertNull(resolveNotification(event("sensor.temp", "sensor", "70", "71"), locallyActuated = false))
    }
}
