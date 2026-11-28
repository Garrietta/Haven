package com.garrietta.haven.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CardShapesTest {

    @Test
    fun eightVariantCycle() {
        // Variants 0-3: the expressive-menus rule (one 12dp corner, rotating).
        assertEquals(
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 28.dp),
            generalCardShape(0),
        )
        assertEquals(
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
            generalCardShape(1),
        )
        // Variants 4-7: the new sharper/softer/pillow/diagonal edges.
        assertEquals(RoundedCornerShape(8.dp), generalCardShape(4))
        assertEquals(RoundedCornerShape(28.dp), generalCardShape(5))
        assertEquals(RoundedCornerShape(36.dp), generalCardShape(6))
        assertEquals(
            RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 28.dp),
            generalCardShape(7),
        )
        // The cycle wraps, including negative indices.
        assertEquals(generalCardShape(0), generalCardShape(8))
        assertEquals(generalCardShape(7), generalCardShape(-1))
    }

    @Test
    fun cornerPickerKeysResolveToCycleVariants() {
        assertEquals(generalCardShape(4), shapeForKey("corners_4"))
        assertEquals(generalCardShape(0), shapeForKey("corners_0"))
        assertEquals(null, shapeForKey("corners_9"))
        assertEquals("Sharp 8dp", shapeLabel("corners_4"))
    }

    @Test
    fun allEightVariantsAreDistinct() {
        val shapes = (0 until 8).map { generalCardShape(it) }
        assertEquals(8, shapes.toSet().size)
    }
}
