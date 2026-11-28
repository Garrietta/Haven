package com.garrietta.haven.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max

/*
 * Grain: the one texture Haven has, and it is kept for its moments - a
 * light that is on, a thermostat at work. Everywhere else the chrome is
 * flat. A moment is a soft gradient between two tones of its state's own
 * color, glowing from where the thing is, with fine film grain over it,
 * so a lit lamp reads as light rather than as a colored rectangle.
 *
 * The grain is luminance noise around mid grey laid on with Overlay:
 * mid grey leaves a color as it is, so the noise lightens and darkens
 * the gradient without shifting its hue. It is made once, small, and
 * tiled.
 */

private const val GRAIN_SIZE = 128

/** How strongly the grain shows on a moment. */
const val GRAIN_AMOUNT = 0.16f

/** A tile of film grain: gaussian noise around mid grey, opaque. Texture data, not a color. */
internal val GrainImage: ImageBitmap by lazy {
    val random = java.util.Random(7)
    val pixels = IntArray(GRAIN_SIZE * GRAIN_SIZE) {
        val v = (128 + random.nextGaussian() * 44).toInt().coerceIn(0, 255)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    Bitmap.createBitmap(pixels, GRAIN_SIZE, GRAIN_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * A glow behind the content: [hot] at [from] (fractions of the size,
 * where the thing that glows is) falling off to [ground] across the
 * shape, with grain over it. Clip to the shape first.
 */
fun Modifier.grainGlow(
    hot: Color,
    ground: Color,
    from: Offset = Offset(0.25f, 0.2f),
    reach: Float = 1.1f,
    amount: Float = GRAIN_AMOUNT,
): Modifier = drawWithCache {
    val glow = Brush.radialGradient(
        0f to hot,
        0.55f to androidx.compose.ui.graphics.lerp(hot, ground, 0.6f),
        1f to ground,
        center = Offset(size.width * from.x, size.height * from.y),
        radius = max(size.width, size.height) * reach,
    )
    val grain = ShaderBrush(ImageShader(GrainImage, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind {
        drawRect(ground)
        drawRect(glow)
        drawRect(grain, alpha = amount, blendMode = BlendMode.Overlay)
    }
}

/** The glow's two stops for a size, for callers that draw it themselves. */
internal fun glowRadius(size: Size, reach: Float) = max(size.width, size.height) * reach
