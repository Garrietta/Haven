package com.garrietta.haven.ui.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class ControlSlabTest {

    @Test
    fun draggingUpTheWholeSlabAddsAHundredPercent() {
        assertEquals(100f, levelAfterDrag(0f, dragPx = -400f, heightPx = 400f), 0.001f)
        assertEquals(75f, levelAfterDrag(50f, dragPx = -100f, heightPx = 400f), 0.001f)
    }

    @Test
    fun draggingDownDimsButNeverToOff() {
        assertEquals(25f, levelAfterDrag(50f, dragPx = 100f, heightPx = 400f), 0.001f)
        // Off is the tap's job: a drag stops at the dimmest level.
        assertEquals(1f, levelAfterDrag(50f, dragPx = 4000f, heightPx = 400f), 0.001f)
        assertEquals(100f, levelAfterDrag(90f, dragPx = -4000f, heightPx = 400f), 0.001f)
    }

    @Test
    fun aSlabWithNoHeightYetKeepsTheLevel() {
        assertEquals(40f, levelAfterDrag(40f, dragPx = -50f, heightPx = 0f), 0.001f)
    }

    @Test
    fun homeAssistantBrightnessReadsAsWholePercent() {
        assertEquals(100, brightnessPercent(255))
        assertEquals(50, brightnessPercent(128))
        assertEquals(0, brightnessPercent(0))
        // A lamp that reports no level is at full.
        assertEquals(100, brightnessPercent(null))
    }

    @Test
    fun theWarmToCoolTrackMapsBothWays() {
        assertEquals(2000, kelvinAt(0f, 2000, 6500))
        assertEquals(6500, kelvinAt(1f, 2000, 6500))
        // Rounded to 50K, as Home Assistant's own picker does.
        assertEquals(4250, kelvinAt(0.5f, 2000, 6500))
        assertEquals(2700, kelvinAt(0.155f, 2000, 6500))
        assertEquals(0.5f, kelvinFraction(4250, 2000, 6500), 0.001f)
        assertEquals(0f, kelvinFraction(1500, 2000, 6500), 0.001f)
        // A lamp that does not say where it is sits in the middle.
        assertEquals(0.5f, kelvinFraction(null, 2000, 6500), 0.001f)
    }

    @Test
    fun aLampWithNoRangeStaysAtItsOnlyKelvin() {
        assertEquals(2700, kelvinAt(0.8f, 2700, 2700))
        assertEquals(0.5f, kelvinFraction(2700, 2700, 2700), 0.001f)
    }

    @Test
    fun wattsReadAsAShortCaption() {
        assertEquals("120 W in use", wattsCaption(119.6))
        assertEquals("4.2 W in use", wattsCaption(4.24))
    }
}
