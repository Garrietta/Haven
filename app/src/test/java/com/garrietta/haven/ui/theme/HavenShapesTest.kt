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
    fun shapeScaleMatchesMaterial3Tokens() {
        assertEquals(CornerSize(4.dp), HavenShapes.extraSmall.topStart)
        assertEquals(CornerSize(8.dp), HavenShapes.small.topStart)
        assertEquals(CornerSize(12.dp), HavenShapes.medium.topStart)
        assertEquals(CornerSize(16.dp), HavenShapes.large.topStart)
        assertEquals(CornerSize(28.dp), HavenShapes.extraLarge.topStart)
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
