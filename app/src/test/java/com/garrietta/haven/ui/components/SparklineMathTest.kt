package com.garrietta.haven.ui.components

import com.garrietta.haven.data.HistoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SparklineMathTest {

    private fun points(vararg pairs: Pair<Long, Double>) =
        pairs.map { HistoryPoint(it.first, it.second) }

    @Test
    fun segmentsSmoothWithMidpointControlPoints() {
        val segments = sparklineSegments(
            points(0L to 0.0, 1000L to 10.0, 2000L to 5.0),
            width = 100f,
            height = 50f,
        )
        assertEquals(2, segments.size)
        val first = segments[0]
        assertEquals(0f, first.x0, 0.001f)
        assertEquals(50f, first.y0, 0.001f)
        assertEquals(25f, first.c1x, 0.001f)
        assertEquals(50f, first.c1y, 0.001f)
        assertEquals(25f, first.c2x, 0.001f)
        assertEquals(0f, first.c2y, 0.001f)
        assertEquals(50f, first.x1, 0.001f)
        assertEquals(0f, first.y1, 0.001f)
        val second = segments[1]
        assertEquals(100f, second.x1, 0.001f)
        assertEquals(25f, second.y1, 0.001f)
    }

    @Test
    fun degenerateRangeDrawsFlatLineAndShortSeriesAreEmpty() {
        val flat = sparklineSegments(points(0L to 7.0, 1000L to 7.0), 100f, 50f)
        assertEquals(1, flat.size)
        assertEquals(50f, flat[0].y0, 0.001f)
        assertEquals(50f, flat[0].y1, 0.001f)
        assertTrue(sparklineSegments(points(0L to 7.0), 100f, 50f).isEmpty())
        assertTrue(sparklineSegments(emptyList(), 100f, 50f).isEmpty())
    }

    @Test
    fun minCurrentMaxTracksLastAsCurrent() {
        val mm = sparklineMinCurrentMax(points(0L to 0.0, 1000L to 10.0, 2000L to 5.0))!!
        assertEquals(0.0, mm.first, 0.0)
        assertEquals(5.0, mm.second, 0.0)
        assertEquals(10.0, mm.third, 0.0)
        assertNull(sparklineMinCurrentMax(emptyList()))
    }
}
