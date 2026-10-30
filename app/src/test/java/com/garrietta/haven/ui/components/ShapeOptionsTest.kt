package com.garrietta.haven.ui.components

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeOptionsTest {

    @Test
    fun everyOptionResolvesToShapeAndPolygon() {
        for (option in SHAPE_OPTIONS) {
            assertNotNull("shapeForKey(${option.key})", shapeForKey(option.key))
            assertNotNull("polygonForKey(${option.key})", polygonForKey(option.key))
            assertNotNull("shapeLabel(${option.key})", shapeLabel(option.key))
        }
    }

    @Test
    fun unknownKeyResolvesToNull() {
        assertNull(shapeForKey("not-a-shape"))
        assertNull(polygonForKey("not-a-shape"))
        assertNull(shapeLabel("not-a-shape"))
    }
}
