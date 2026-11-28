package com.garrietta.haven.ui.screens

/*
 * The shared Dial's touch geometry (ui/components/Expressive.kt), kept
 * free of Compose so it is unit-tested.
 */

/**
 * The dial's 270-degree sweep maps an angle (degrees, 0 = east, clockwise
 * in screen coordinates) to a value: the sweep starts at 135 degrees and
 * runs 270, leaving the gap at the bottom. Null inside the gap, where the
 * caller ignores the touch rather than snapping to an end.
 */
fun dialValueFromAngle(deg: Float, range: ClosedFloatingPointRange<Float>): Float? {
    val rel = (deg - 135f + 360f) % 360f
    if (rel > 270f) return null
    return (range.start + (rel / 270f) * (range.endInclusive - range.start))
        .coerceIn(range)
}

/** Where a tap on a dial landed. */
enum class DialTapZone { INSIDE, RING, OUTSIDE }

/**
 * A tap by its distance from the dial's center: inside the face (clear of
 * the ring by [ringTolerancePx]), on the ring, or beyond it.
 */
fun dialTapZone(
    touchX: Float,
    touchY: Float,
    centerX: Float,
    centerY: Float,
    trackRadiusPx: Float,
    ringTolerancePx: Float,
): DialTapZone {
    val dx = touchX - centerX
    val dy = touchY - centerY
    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
    return when {
        dist < trackRadiusPx - ringTolerancePx -> DialTapZone.INSIDE
        dist <= trackRadiusPx + ringTolerancePx -> DialTapZone.RING
        else -> DialTapZone.OUTSIDE
    }
}

/** True when a drag starts close enough to the handle's center to pick it up. */
fun isHandleGrab(
    touchX: Float,
    touchY: Float,
    handleX: Float,
    handleY: Float,
    grabRadiusPx: Float,
): Boolean {
    val dx = touchX - handleX
    val dy = touchY - handleY
    return kotlin.math.sqrt(dx * dx + dy * dy) <= grabRadiusPx
}
