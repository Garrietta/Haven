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
enum class ExpressiveKind { SUNNY, FLOWER, BUN, BURST }

private fun polygonVertices(n: Int, radiusAt: (Int, Float) -> Float): FloatArray {
    val out = FloatArray(n * 2)
    for (i in 0 until n) {
        val angle = (2 * Math.PI * i / n).toFloat()
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

/**
 * Draws a live morph between two expressive polygons. [progress] 0 = [from], 1 = [to].
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
    Canvas(modifier) {
        val morph = Morph(expressivePolygon(from), expressivePolygon(to))
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
