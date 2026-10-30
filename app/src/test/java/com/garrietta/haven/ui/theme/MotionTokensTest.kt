package com.garrietta.haven.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stream 2 (design-system sprint): motion tokens carry the canonical
 * spring values. Tactile targets use MediumBouncy/LowBouncy damping
 * with StiffnessMedium — adopted INTO the tokens, never ad-hoc at a
 * call site. The clamp tests pin the spring-value clamping rule:
 * every animated-progress-derived value stays in [0, 1], including
 * the observed 1.00493 overshoot peak.
 */
class MotionTokensTest {

    @Test
    fun pressBounceIsCanonicalMediumBouncy() {
        val spec = pressBounce<Float>() as SpringSpec<Float>
        assertEquals(Spring.DampingRatioMediumBouncy, spec.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, spec.stiffness, 0.001f)
    }

    @Test
    fun springSnappyIsCanonicalLowBouncy() {
        val spec = springSnappy<Float>() as SpringSpec<Float>
        assertEquals(Spring.DampingRatioLowBouncy, spec.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, spec.stiffness, 0.001f)
    }

    @Test
    fun clampMorphProgressEndpointsAndOvershoot() {
        assertEquals(0f, clampMorphProgress(-1f), 0.001f)
        assertEquals(0f, clampMorphProgress(-0.5f), 0.001f)
        assertEquals(0f, clampMorphProgress(0f), 0.001f)
        assertEquals(0.5f, clampMorphProgress(0.5f), 0.001f)
        assertEquals(1f, clampMorphProgress(1f), 0.001f)
        assertEquals(1f, clampMorphProgress(1.00493f), 0.001f)
        assertEquals(1f, clampMorphProgress(1.5f), 0.001f)
        assertEquals(1f, clampMorphProgress(2f), 0.001f)
    }
}
