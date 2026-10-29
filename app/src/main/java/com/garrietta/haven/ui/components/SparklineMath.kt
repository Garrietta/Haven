package com.garrietta.haven.ui.components

import com.garrietta.haven.data.HistoryPoint

/**
 * Pure math for the sensor sparkline (spec item 7). Kept dependency-free so
 * it is unit-testable: the composable only draws what these functions return.
 */

/** One cubic-bezier segment of the smoothed sparkline. */
data class CubicSegment(
    val x0: Float,
    val y0: Float,
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float,
    val x1: Float,
    val y1: Float,
)

/**
 * Smooths a history series into cubic-bezier segments. Control points sit at
 * the horizontal midpoint of each segment, on the endpoints' heights — the
 * classic smoothed polyline that keeps every data point exactly on the curve.
 * Returns empty for fewer than 2 points. A degenerate (all-equal) range is
 * drawn as a flat midline rather than dividing by zero.
 */
fun sparklineSegments(
    points: List<HistoryPoint>,
    width: Float,
    height: Float,
): List<CubicSegment> {
    if (points.size < 2) return emptyList()
    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    val t0 = points.first().timeMs.toDouble()
    val t1 = points.last().timeMs.toDouble()
    val span = (t1 - t0).takeIf { it > 0 } ?: 1.0
    val mapped = points.map { p ->
        val x = ((p.timeMs - t0) / span * width).toFloat()
        val y = (height - ((p.value - min) / range * height)).toFloat()
        x to y
    }
    return mapped.zipWithNext { (x0, y0), (x1, y1) ->
        val mid = (x0 + x1) / 2f
        CubicSegment(x0, y0, mid, y0, mid, y1, x1, y1)
    }
}

/** Min / current (last) / max of a history series, or null when empty. */
fun sparklineMinCurrentMax(points: List<HistoryPoint>): Triple<Double, Double, Double>? {
    if (points.isEmpty()) return null
    return Triple(points.minOf { it.value }, points.last().value, points.maxOf { it.value })
}
