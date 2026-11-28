package com.garrietta.haven.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.toPath
import com.garrietta.haven.ui.theme.BrandGeometry
import com.garrietta.haven.ui.theme.HavenWordmarkStyle
import com.garrietta.haven.ui.theme.Palette

/*
 * The brand, as things a screen can place.
 *
 * The mark is a house with the light on in its doorway and the sun
 * coming up behind the roof. Its geometry is generated (tools/logo.py)
 * and shared with the launcher icon; here it is drawn live so the sun
 * can turn. The sun is MaterialShapes.Sunny, the polygon a light's badge
 * blooms into when it is switched on.
 */

private fun unitPath(data: String): Path = PathParser().parsePathString(data).toPath()

/**
 * The Haven mark.
 *
 * It reads the theme: on paper a leaf-green house with a lime doorway,
 * on black a lime house with the doorway cut through. The sun is gold in
 * both.
 *
 * [turning] lets the sun rotate, one slow revolution a minute. It is a
 * brand moment for screens where the app is waiting on something
 * (sign-in, connecting), not decoration for every header, and it stops
 * when the user has asked for reduced motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HavenMark(modifier: Modifier = Modifier, turning: Boolean = false) {
    val onDark = isDarkBackdrop(MaterialTheme.colorScheme.background)
    val houseColor = if (onDark) Palette.Lime else Palette.Leaf.t40
    val house = remember { unitPath(BrandGeometry.HOUSE) }
    val door = remember { unitPath(BrandGeometry.DOOR) }
    val halo = remember { unitPath(BrandGeometry.HALO) }
    // MaterialShapes polygons are normalised to the unit square; centre
    // the sun on the origin so it can be scaled and turned about itself.
    val sun = remember {
        MaterialShapes.Sunny.toPath().asComposePath().apply {
            transform(Matrix().apply { translate(-0.5f, -0.5f) })
        }
    }
    val angle = if (turning && !LocalReduceMotion.current) {
        val spin by rememberInfiniteTransition(label = "markSun").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
            label = "markSunAngle",
        )
        spin
    } else {
        0f
    }
    Canvas(modifier.aspectRatio(1f).semantics { contentDescription = "Haven" }) {
        val side = size.minDimension
        scale(side, side, pivot = Offset.Zero) {
            // The sun, kept clear of the house by the halo.
            clipPath(halo, clipOp = ClipOp.Difference) {
                withTransform({
                    translate(BrandGeometry.SUN_CX, BrandGeometry.SUN_CY)
                    rotate(BrandGeometry.SUN_ROTATION + angle, pivot = Offset.Zero)
                    scale(BrandGeometry.SUN_SIZE, BrandGeometry.SUN_SIZE, pivot = Offset.Zero)
                }) {
                    drawPath(sun, Palette.Sun)
                }
            }
            drawPath(house, houseColor)
            if (!onDark) drawPath(door, Palette.Lime)
        }
    }
}

/** "Haven", set in the wordmark cut: wide, round, tight. */
@Composable
fun HavenWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text("Haven", modifier = modifier, style = HavenWordmarkStyle, fontSize = fontSize, color = color, maxLines = 1)
}

/**
 * The mark and the wordmark side by side: the small signature at the
 * top of Home. The wordmark is sized from the mark so the pair always
 * keeps its proportions.
 */
@Composable
fun HavenLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(markSize * 0.28f)) {
        HavenMark(Modifier.size(markSize))
        HavenWordmark(fontSize = (markSize.value * 0.72f).sp, color = color)
    }
}
