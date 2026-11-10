package com.garrietta.haven.data

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.components.SensorKind
import com.garrietta.haven.ui.components.chartLineColor
import com.garrietta.haven.ui.components.resolveLandingShape
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.screens.toggleEntitySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.signal
import com.garrietta.haven.ui.components.contrastRatio
import com.garrietta.haven.ui.theme.havenLightColorScheme
import com.garrietta.haven.ui.theme.havenDarkColorScheme

/**
 * Feedback round 20: the shape-stability landing rule, the
 * measurement-keyed chart colors, the multi-select toggle, and
 * the resize snap + persistence mapping. All pure functions —
 * no Android runtime.
 */
class Round20LogicTest {

    // Item 1: the captured open-time shape wins, even when the
    // shared map moved mid-popup (the tab-page exit snap).
    @Test
    fun capturedShapeWinsOverChangedReport() {
        val captured = RoundedCornerShape(28.dp)
        val reported = RoundedCornerShape(12.dp)
        val prefs = RoundedCornerShape(8.dp)
        assertSame(captured, resolveLandingShape(captured, reported, prefs))
    }

    @Test
    fun reportedShapeWinsWhenNothingCaptured() {
        val reported = RoundedCornerShape(12.dp)
        val prefs = RoundedCornerShape(8.dp)
        assertSame(reported, resolveLandingShape(null, reported, prefs))
    }

    @Test
    fun prefsShapeIsLastResort() {
        val prefs = RoundedCornerShape(8.dp)
        assertSame(prefs, resolveLandingShape(null, null, prefs))
    }

    @Test
    fun allNullLandingIsNull() {
        assertNull(resolveLandingShape(null, null, null))
    }

    // Item 2: like entities share one line color.
    @Test
    fun twoTemperatureSensorsShareOneKind() {
        val a = sensorKind("sensor.5179_2551_temperature", "temperature", "5179 2551 Temperature")
        val b = sensorKind("sensor.temperature", "temperature", "Temperature")
        assertEquals(SensorKind.TEMPERATURE, a)
        assertEquals(a, b)
    }

    @Test
    fun aChartLineIsItsMeasurementsFamilySignal() {
        // Keyed by what is measured, never by which entity: two
        // temperature sensors always draw the same line.
        for (isDark in listOf(false, true)) {
            assertEquals(StateFamily.Heat.signal(isDark), chartLineColor(SensorKind.TEMPERATURE, isDark))
            assertEquals(StateFamily.Water.signal(isDark), chartLineColor(SensorKind.HUMIDITY, isDark))
            assertEquals(StateFamily.Power.signal(isDark), chartLineColor(SensorKind.POWER, isDark))
            assertEquals(StateFamily.On.signal(isDark), chartLineColor(SensorKind.BATTERY, isDark))
            assertEquals(StateFamily.Caution.signal(isDark), chartLineColor(SensorKind.LIGHT, isDark))
            assertEquals(StateFamily.Cool.signal(isDark), chartLineColor(SensorKind.PRESSURE, isDark))
        }
    }

    @Test
    fun aChartLineReadsOnThePageInBothThemes() {
        for (kind in SensorKind.entries) {
            assertTrue(contrastRatio(chartLineColor(kind, isDark = false), havenLightColorScheme().background) >= 3f)
            assertTrue(contrastRatio(chartLineColor(kind, isDark = true), havenDarkColorScheme().background) >= 3f)
        }
    }

    @Test
    fun pressureSensorsClassifyAsPressure() {
        assertEquals(
            SensorKind.PRESSURE,
            sensorKind("sensor.barometer_pressure", "pressure", "Barometer"),
        )
    }

    @Test
    fun differentMeasurementsGetDifferentLines() {
        val colors = SensorKind.values().map { chartLineColor(it, isDark = true) }.toSet()
        assertEquals(SensorKind.values().size, colors.size)
    }

    // Item 4: selection survives filtering (it lives outside the
    // filtered list) and toggling never disturbs other members.
    @Test
    fun toggleAddsAndRemoves() {
        assertEquals(setOf("sensor.a"), toggleEntitySelection(emptySet<String>(), "sensor.a"))
        assertEquals(emptySet<String>(), toggleEntitySelection(setOf("sensor.a"), "sensor.a"))
    }

    @Test
    fun togglePreservesOtherMembers() {
        val selected = setOf("sensor.a", "sensor.b")
        assertEquals(setOf("sensor.a", "sensor.b", "sensor.c"), toggleEntitySelection(selected, "sensor.c"))
        assertEquals(setOf("sensor.b"), toggleEntitySelection(selected, "sensor.a"))
    }

    @Test
    fun doubleToggleIsIdentity() {
        val selected = setOf("sensor.a", "sensor.b")
        assertEquals(
            selected,
            toggleEntitySelection(toggleEntitySelection(selected, "sensor.c"), "sensor.c"),
        )
    }

    // Item 5: drag-vector snap.
    @Test
    fun belowThresholdKeepsCurrent() {
        assertEquals(CardSize.WIDE, snapCardSize(CardSize.WIDE, 10f, 5f))
        assertEquals(CardSize.TALL, snapCardSize(CardSize.TALL, 5f, 10f))
        assertEquals(CardSize.NORMAL, snapCardSize(CardSize.NORMAL, 23.9f, 0f))
    }

    @Test
    fun horizontalDragResizesWidth() {
        assertEquals(CardSize.WIDE, snapCardSize(CardSize.NORMAL, 100f, 10f))
        assertEquals(CardSize.NORMAL, snapCardSize(CardSize.WIDE, -100f, 10f))
    }

    @Test
    fun verticalDragResizesHeight() {
        assertEquals(CardSize.TALL, snapCardSize(CardSize.NORMAL, 10f, 100f))
        assertEquals(CardSize.NORMAL, snapCardSize(CardSize.TALL, 10f, -100f))
    }

    @Test
    fun dominantAxisWins() {
        assertEquals(CardSize.WIDE, snapCardSize(CardSize.NORMAL, 80f, 50f))
        assertEquals(CardSize.TALL, snapCardSize(CardSize.NORMAL, 50f, 80f))
    }

    // Item 5: persistence keys are page-scoped.
    @Test
    fun sizeKeysAreScopedPerPage() {
        assertEquals("home:light.lamp", cardSizeKey("home", "light.lamp"))
        assertEquals("room:kitchen:light.lamp", cardSizeKey("room:kitchen", "light.lamp"))
        assertEquals("tab:climate:light.lamp", cardSizeKey("tab:climate", "light.lamp"))
        assertTrue(cardSizeKey("home", "light.lamp") != cardSizeKey("room:kitchen", "light.lamp"))
    }
}
