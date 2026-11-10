package com.garrietta.haven.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import com.garrietta.haven.ui.theme.leafCard

class CardShapesTest {

    @Test
    fun defaultCardShapeIsTheLeafCard() {
        // A grid's cards are leaves: three soft corners and one tight
        // one that turns with the position. No default card is sharp.
        for (index in -2..9) assertEquals(leafCard(index), generalCardShape(index))
        assertEquals(
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 28.dp),
            generalCardShape(0),
        )
        assertEquals(4, (0 until 8).map { generalCardShape(it) }.toSet().size)
    }

    @Test
    fun cornerPickerOffersEightDistinctOutlines() {
        // The numbers are stored with the user's choice, so each keeps
        // its meaning: 0-3 the leaf rotations, then sharp, soft, pillow
        // and the diagonal.
        for (i in 0 until 4) assertEquals(leafCard(i), cardCornerOption(i))
        assertEquals(RoundedCornerShape(8.dp), cardCornerOption(4))
        assertEquals(RoundedCornerShape(28.dp), cardCornerOption(5))
        assertEquals(RoundedCornerShape(36.dp), cardCornerOption(6))
        assertEquals(
            RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 28.dp),
            cardCornerOption(7),
        )
        assertEquals(8, (0 until 8).map { cardCornerOption(it) }.toSet().size)
    }

    @Test
    fun cornerPickerKeysResolveToTheirOutlines() {
        assertEquals(cardCornerOption(4), shapeForKey("corners_4"))
        assertEquals(cardCornerOption(0), shapeForKey("corners_0"))
        assertEquals(null, shapeForKey("corners_9"))
        assertEquals("Sharp 8dp", shapeLabel("corners_4"))
    }
}
