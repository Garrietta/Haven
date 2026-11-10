package com.garrietta.haven.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Haven's shapes come in two layers.
 *
 * Corners (this file) are for containers: cards, fields, sheets, bars.
 * Polygons (Polygons.kt) are for state: the badge behind a device's icon
 * blooms from a calm shape to a lively one when the device turns on.
 * A container is never a polygon and a badge is never a rectangle.
 */

/** The corner radii everything is cut from. Nothing writes a raw dp corner. */
object Corner {
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 28.dp
    /** Things that float above the page: popups, the navigation bar. */
    val float: Dp = 32.dp
}

/**
 * The Material shape scale, a step rounder than stock at the small end
 * (8 and 12 where Material has 4 and 8) so text fields and menus are as
 * soft as the cards beside them. Read it through MaterialTheme.shapes.
 */
val HavenShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.xs),
    small = RoundedCornerShape(Corner.sm),
    medium = RoundedCornerShape(Corner.md),
    large = RoundedCornerShape(Corner.lg),
    extraLarge = RoundedCornerShape(Corner.xl),
)

/**
 * The large corner on top only, square below: for a fill whose bottom
 * edge is the straight edge of whatever it sits in (the garage door).
 */
val LargeTopShape: Shape = HavenShapes.large.copy(
    bottomStart = CornerSize(0.dp),
    bottomEnd = CornerSize(0.dp),
)

/** A floating element at rest. The popup corner morph animates to exactly this. */
val FloatingCorner = RoundedCornerShape(Corner.float)

/**
 * Haven's card: three soft corners and one tight one. The tight corner
 * turns with the card's place in the grid, so four cards together read
 * as a set of leaves rather than a sheet of identical tiles. It is the
 * most recognisable shape in the app after the navigation bar.
 */
fun leafCard(index: Int): RoundedCornerShape = when (Math.floorMod(index, 4)) {
    0 -> RoundedCornerShape(topStart = Corner.xl, topEnd = Corner.xl, bottomEnd = Corner.sm, bottomStart = Corner.xl)
    1 -> RoundedCornerShape(topStart = Corner.xl, topEnd = Corner.xl, bottomEnd = Corner.xl, bottomStart = Corner.sm)
    2 -> RoundedCornerShape(topStart = Corner.xl, topEnd = Corner.sm, bottomEnd = Corner.xl, bottomStart = Corner.xl)
    else -> RoundedCornerShape(topStart = Corner.sm, topEnd = Corner.xl, bottomEnd = Corner.xl, bottomStart = Corner.xl)
}
