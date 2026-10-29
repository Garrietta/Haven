package com.garrietta.haven.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Hand-rolled Material 3 Expressive polygons, built on androidx.graphics.shapes.
 * These are the same family of shapes used across Relay/Koda/Pixel Music: big,
 * friendly, animated blobs — never raw rectangles.
 */
enum class ExpressiveKind { SUNNY, FLOWER, BUN, BURST, WAVE, CLOUD }

private fun polygonVertices(n: Int, startDeg: Float = 0f, radiusAt: (Int, Float) -> Float): FloatArray {
    val out = FloatArray(n * 2)
    for (i in 0 until n) {
        val angle = (2 * Math.PI * i / n).toFloat() + Math.toRadians(startDeg.toDouble()).toFloat()
        val r = radiusAt(i, angle)
        out[i * 2] = r * cos(angle)
        out[i * 2 + 1] = r * sin(angle)
    }
    return out
}

fun expressivePolygon(kind: ExpressiveKind): RoundedPolygon = when (kind) {
    // 12-point sun: alternating long/short rays.
    ExpressiveKind.SUNNY -> RoundedPolygon(
        polygonVertices(12) { i, _ -> if (i % 2 == 0) 1f else 0.70f },
        rounding = CornerRounding(0.30f, 0.6f),
    )
    // 6 soft petals.
    ExpressiveKind.FLOWER -> RoundedPolygon(
        polygonVertices(24) { _, a -> 0.80f + 0.20f * cos(6 * a) },
        rounding = CornerRounding(0.30f, 0.6f),
    )
    // One squishy blob.
    ExpressiveKind.BUN -> RoundedPolygon(
        polygonVertices(20) { _, a -> 0.92f + 0.08f * sin(3 * a) },
        rounding = CornerRounding(0.35f, 0.6f),
    )
    // 8-point soft burst.
    ExpressiveKind.BURST -> RoundedPolygon(
        polygonVertices(24) { _, a -> 0.86f + 0.14f * cos(8 * a) },
        rounding = CornerRounding(0.28f, 0.6f),
    )
    // Sound ripple: two broad lobes, for media and fans.
    ExpressiveKind.WAVE -> RoundedPolygon(
        polygonVertices(24) { _, a -> 0.84f + 0.16f * sin(2 * a) },
        rounding = CornerRounding(0.32f, 0.6f),
    )
    // Soft five-lobe cloud, for weather and sensors.
    ExpressiveKind.CLOUD -> RoundedPolygon(
        polygonVertices(24) { _, a -> 0.88f + 0.12f * cos(5 * a) },
        rounding = CornerRounding(0.38f, 0.6f),
    )
}

/** A Compose [Shape] that draws an expressive polygon filling its layout box. */
fun polygonShape(kind: ExpressiveKind): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = expressivePolygon(kind).toPath()
        val scale = min(size.width, size.height) / 2f
        val matrix = android.graphics.Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

/** Same min-dimension scaling as [polygonShape], for state-table shapes
 * (the thermostat pop-out's scallop disc is SCALLOP12, not an
 * ExpressiveKind). Square boxes fit exactly; non-square boxes inset
 * the same way polygonShape does, so disc boxes are kept square. */
fun stateShape(kind: StateShape): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = stateShapePolygon(kind).toPath()
        val scale = min(size.width, size.height) / 2f
        val matrix = android.graphics.Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

/**
 * State-driven polygons for the dynamic badging table: each domain has an
 * idle shape and an active shape, and badges morph between them on state
 * change (see StateShapeBadge). Idle shapes are calm and closed; active
 * shapes bloom, spike, or wave.
 */
enum class StateShape { HEXAGON, SCALLOP12, SQUIRCLE, ARC_WAVE, SHIELD, NOTCHED_DIAMOND, STAR8, OCTAGON, SUNBURST, CLOUD_POLY }

fun stateShapePolygon(shape: StateShape): RoundedPolygon = when (shape) {
    // Smooth hexagon, corner radius 0.35 (lights off, etc.).
    StateShape.HEXAGON -> RoundedPolygon(
        polygonVertices(6) { _, _ -> 1f },
        rounding = CornerRounding(0.35f, 0.6f),
    )
    // 12-lobed scallop (lights on, blooming).
    StateShape.SCALLOP12 -> RoundedPolygon(
        polygonVertices(48) { _, a -> 0.86f + 0.14f * cos(12 * a) },
        rounding = CornerRounding(0.30f, 0.6f),
    )
    // Squircle: a rounded square, corners at 45 degrees, 32% rounding.
    StateShape.SQUIRCLE -> RoundedPolygon(
        polygonVertices(4, 45f) { _, _ -> 1f },
        rounding = CornerRounding(0.32f, 0.6f),
    )
    // Circular arc wave (thermostat active): a wavy ring.
    StateShape.ARC_WAVE -> RoundedPolygon(
        polygonVertices(48) { _, a -> 0.90f + 0.10f * sin(8 * a) },
        rounding = CornerRounding(0.30f, 0.6f),
    )
    // Inverted shield: flat top, point at the bottom (garage closed).
    StateShape.SHIELD -> RoundedPolygon(
        floatArrayOf(
            -0.85f, -0.9f,
            0.85f, -0.9f,
            0.85f, 0.1f,
            0f, 0.95f,
            -0.85f, 0.1f,
        ),
        rounding = CornerRounding(0.25f, 0.6f),
    )
    // Notched diamond (garage open): a diamond with cut-in waists.
    StateShape.NOTCHED_DIAMOND -> RoundedPolygon(
        polygonVertices(8) { i, _ -> if (i % 2 == 0) 1f else 0.55f },
        rounding = CornerRounding(0.25f, 0.6f),
    )
    // 8-point star (media playing): alternating long/short points.
    StateShape.STAR8 -> RoundedPolygon(
        polygonVertices(16) { i, _ -> if (i % 2 == 0) 1f else 0.72f },
        rounding = CornerRounding(0.30f, 0.6f),
    )
    // Rounded octagon (weather/sensor idle).
    StateShape.OCTAGON -> RoundedPolygon(
        polygonVertices(8) { _, _ -> 1f },
        rounding = CornerRounding(0.35f, 0.6f),
    )
    // 12-point sunburst (weather active, clear day) — mirrors SUNNY.
    StateShape.SUNBURST -> RoundedPolygon(
        polygonVertices(12) { i, _ -> if (i % 2 == 0) 1f else 0.70f },
        rounding = CornerRounding(0.30f, 0.6f),
    )
    // Soft five-lobe cloud (weather active, cloudy) — mirrors CLOUD.
    StateShape.CLOUD_POLY -> RoundedPolygon(
        polygonVertices(24) { _, a -> 0.88f + 0.12f * cos(5 * a) },
        rounding = CornerRounding(0.38f, 0.6f),
    )
}

/**
 * Draws a live morph between two state polygons. [progress] 0 = [from], 1 = [to].
 * Used for toggles that change shape with state (e.g. a light bulb blooming into a sun).
 */
@Composable
fun MorphShape(
    from: ExpressiveKind,
    to: ExpressiveKind,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    MorphPolygon(
        from = expressivePolygon(from),
        to = expressivePolygon(to),
        progress = progress,
        color = color,
        modifier = modifier,
    )
}

/** The same live morph, for state polygons directly. */
@Composable
fun MorphPolygon(
    from: RoundedPolygon,
    to: RoundedPolygon,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val morph = Morph(from, to)
        val scale = min(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()
        var first = true
        morph.forEachCubic(progress) { cubic ->
            val ax0 = cx + cubic.anchor0X * scale
            val ay0 = cy + cubic.anchor0Y * scale
            if (first) {
                path.moveTo(ax0, ay0)
                first = false
            }
            path.cubicTo(
                cx + cubic.control0X * scale, cy + cubic.control0Y * scale,
                cx + cubic.control1X * scale, cy + cubic.control1Y * scale,
                cx + cubic.anchor1X * scale, cy + cubic.anchor1Y * scale,
            )
        }
        path.close()
        drawPath(path, color)
    }
}
