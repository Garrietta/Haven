package com.garrietta.haven.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.components.CoolBlue
import com.garrietta.haven.ui.components.HeatOrange
import com.garrietta.haven.ui.components.climateModeColor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stream 1 (design-system sprint): the shape token scale and the
 * single-source state colors. If a token value drifts, every container
 * that reads the token drifts with it — these tests pin the scale.
 */
class HavenShapesTest {

    @Test
    fun shapeScaleIsTheCornerScale() {
        // A step rounder than stock Material at the small end.
        assertEquals(CornerSize(8.dp), HavenShapes.extraSmall.topStart)
        assertEquals(CornerSize(12.dp), HavenShapes.small.topStart)
        assertEquals(CornerSize(16.dp), HavenShapes.medium.topStart)
        assertEquals(CornerSize(20.dp), HavenShapes.large.topStart)
        assertEquals(CornerSize(28.dp), HavenShapes.extraLarge.topStart)
        assertEquals(CornerSize(Corner.xs), HavenShapes.extraSmall.topStart)
        assertEquals(CornerSize(Corner.xl), HavenShapes.extraLarge.topStart)
    }

    @Test
    fun leafCardTurnsItsTightCornerWithTheIndex() {
        fun tight(index: Int) = leafCard(index).let {
            listOf(it.topStart, it.topEnd, it.bottomEnd, it.bottomStart).map { c -> c == CornerSize(Corner.sm) }
        }
        // Exactly one tight corner, and it is a different one for each
        // of four neighbours.
        for (i in 0 until 4) assertEquals(1, tight(i).count { it })
        assertEquals(4, (0 until 4).map { tight(it).indexOf(true) }.toSet().size)
        // The cycle wraps, including negative indices.
        assertEquals(leafCard(0), leafCard(4))
        assertEquals(leafCard(3), leafCard(-1))
    }

    @Test
    fun floatingCornerIsThePopUpMorphEndpoint() {
        assertEquals(CornerSize(32.dp), FloatingCorner.topStart)
    }

    @Test
    fun climateModeColorUsesTheSharedStateColors() {
        assertEquals(HeatOrange, climateModeColor("heat", null))
        assertEquals(CoolBlue, climateModeColor("cool", null))
        assertEquals(HeatOrange, climateModeColor("heat_cool", null))
        assertEquals(HeatOrange, climateModeColor("heat", "heating"))
        assertEquals(CoolBlue, climateModeColor("heat", "cooling"))
    }
}
