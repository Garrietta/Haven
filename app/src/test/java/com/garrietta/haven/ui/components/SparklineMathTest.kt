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

    // Resampling (visual-bugs round, spec item 6): dense state-change
    // history must bucket into a smooth readable series without
    // inventing or hiding extremes (labels still use raw points).

    @Test
    fun resample_bucketsNoisySeriesToAtMostMaxBuckets() {
        // 240 jittery samples over 24h: alternating 20.0 / 22.0.
        val noisy = (0 until 240).map { i ->
            HistoryPoint(i * 360_000L, if (i % 2 == 0) 20.0 else 22.0)
        }
        val resampled = resampleHistory(noisy, maxBuckets = 72)
        assertTrue(resampled.size <= 72)
        assertTrue(resampled.size >= 2)
        resampled.forEach { p -> assertTrue(p.value in 20.0..22.0) }
    }

    @Test
    fun resample_bucketMeanIsTheAverageOfItsSpan() {
        val twoRuns = (0 until 10).map { i ->
            HistoryPoint(i * 1_000L, if (i < 5) 10.0 else 20.0)
        }
        val resampled = resampleHistory(twoRuns, maxBuckets = 2)
        assertEquals(2, resampled.size)
        assertEquals(10.0, resampled[0].value, 0.001)
        assertEquals(20.0, resampled[1].value, 0.001)
    }

    @Test
    fun resample_shortSeriesPassThroughUnchanged() {
        val single = points(0L to 21.5)
        assertEquals(single, resampleHistory(single))
        assertEquals(emptyList<HistoryPoint>(), resampleHistory(emptyList()))
    }

    @Test
    fun resample_sortsByTimeFirst() {
        val resampled = resampleHistory(
            points(2_000L to 30.0, 0L to 10.0, 1_000L to 20.0),
            maxBuckets = 3,
        )
        assertEquals(3, resampled.size)
        assertEquals(10.0, resampled[0].value, 0.001)
        assertEquals(20.0, resampled[1].value, 0.001)
        assertEquals(30.0, resampled[2].value, 0.001)
    }

    @Test
    fun resample_doesNotHideExtremesFromLabels() {
        // A single spike inside a flat series: the raw min/max must
        // still be what the labels report, even though the drawn line
        // is bucket-averaged.
        val spiked = (0 until 100).map { i ->
            HistoryPoint(i * 1_000L, if (i == 50) 99.0 else 20.0)
        }
        val labels = sparklineMinCurrentMax(spiked)
        assertEquals(20.0, labels!!.first, 0.001)
        assertEquals(99.0, labels.third, 0.001)
        val resampled = resampleHistory(spiked, maxBuckets = 72)
        // No invented values: every bucket mean stays in the raw range,
        // and the spike stays confined to its own bucket instead of
        // smearing across the line.
        assertTrue(resampled.all { it.value in 20.0..99.0 })
        assertTrue(resampled.count { it.value > 21.0 } <= 2)
    }

    @Test
    fun resample_emptySpansAreSkippedNotInvented() {
        // Two clusters far apart: buckets between them hold no samples
        // and must not appear as invented zero/flat points.
        val resampled = resampleHistory(
            points(0L to 10.0, 1_000L to 10.0, 100_000L to 20.0, 101_000L to 20.0),
            maxBuckets = 72,
        )
        assertTrue(resampled.size in 2..4)
        assertTrue(resampled.all { it.value == 10.0 || it.value == 20.0 })
    }
}
