@file:OptIn(ExperimentalTextApi::class)

package com.garrietta.haven.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.garrietta.haven.R

/*
 * Haven's voice is one typeface: Google Sans Flex, the open-source
 * Material 3 Expressive face, bundled (res/font, SIL OFL) so it is there
 * on first frame and identical on every phone. It is a variable font and
 * Haven uses three of its axes:
 *
 *   ROND   roundness. Always 100: soft terminals, to match the shapes.
 *   wdth   width. Text runs at 100; display sizes widen to 108, which is
 *          what gives the big numbers and greetings their presence.
 *   wght   weight, remapped below.
 */

private const val ROUND = 100f
private const val TEXT_WIDTH = 100f
private const val DISPLAY_WIDTH = 108f

/**
 * One cut of the variable font. [asked] is the weight a call site asks
 * for; [drawn] is where the weight axis is actually set. They differ on
 * purpose at the heavy end: this face at a true 700 is louder than the
 * interface wants, so Bold draws at 660 and ExtraBold at 760. Every
 * `FontWeight.Bold` in the app is tuned from this one table.
 */
private fun cut(asked: FontWeight, drawn: Int, width: Float, opticalSize: Float) = Font(
    resId = R.font.google_sans_flex,
    weight = asked,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(drawn),
        FontVariation.width(width),
        FontVariation.Setting("ROND", ROUND),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

private fun family(width: Float, opticalSize: Float) = FontFamily(
    cut(FontWeight.Light, 300, width, opticalSize),
    cut(FontWeight.Normal, 400, width, opticalSize),
    cut(FontWeight.Medium, 500, width, opticalSize),
    cut(FontWeight.SemiBold, 590, width, opticalSize),
    cut(FontWeight.Bold, 660, width, opticalSize),
    cut(FontWeight.ExtraBold, 760, width, opticalSize),
    cut(FontWeight.Black, 860, width, opticalSize),
)

/** Reading sizes: names, states, labels, paragraphs. */
val HavenTextFamily: FontFamily = family(TEXT_WIDTH, opticalSize = 16f)

/** Display sizes: greetings, room titles, the big readings. Wider, cut for size. */
val HavenDisplayFamily: FontFamily = family(DISPLAY_WIDTH, opticalSize = 40f)

/*
 * Line heights are in em, not sp, and that is load-bearing. Most text in
 * the app sets its own fontSize at the call site and inherits everything
 * else from the ambient style; an sp line height would hand a 13sp label
 * the 24sp leading of the body style it inherited from (the airy,
 * double-spaced look the old screens had). In em the leading follows
 * whatever size the text ends up at.
 *
 * Tracking is zero or slightly negative throughout: the face carries its
 * own spacing, and Material's default +0.5sp on body text reads as
 * letter-spaced in this typeface.
 */
private fun display(size: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = HavenDisplayFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = 1.14.em,
    letterSpacing = (-0.02).em,
)

private fun title(size: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = HavenTextFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = 1.3.em,
    letterSpacing = (-0.005).em,
)

private fun body(size: Int) = TextStyle(
    fontFamily = HavenTextFamily,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = 1.45.em,
    letterSpacing = 0.em,
)

private fun label(size: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = HavenTextFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = 1.35.em,
    letterSpacing = tracking.em,
)

/**
 * The type scale. The fifteen Material roles, so every stock component
 * picks up the voice, sized for Haven:
 *
 *   display    57 / 45 / 36   the one big thing on a screen
 *   headline   32 / 28 / 24   screen and panel titles, hero readings
 *   title      22 / 17 / 15   section headers, device names
 *   body       16 / 14 / 13   sentences and states
 *   label      14 / 12 / 11   buttons, chips, captions
 *
 * bodySmall is 13, not Material's 12: it is the app's most used size
 * (every device's state line) and 12 is too small to read at a glance
 * from across a room.
 */
val HavenTypography = Typography(
    displayLarge = display(57),
    displayMedium = display(45),
    displaySmall = display(36),
    headlineLarge = display(32),
    headlineMedium = display(28),
    headlineSmall = display(24),
    titleLarge = title(22),
    titleMedium = title(17),
    titleSmall = title(15),
    bodyLarge = body(16),
    bodyMedium = body(14),
    bodySmall = body(13),
    labelLarge = label(14),
    labelMedium = label(12),
    labelSmall = label(11, tracking = 0.02f),
)

/**
 * The wordmark: "Haven" set wide and round. Used wherever the name is
 * the brand rather than a title (sign-in, the Home lockup, About).
 */
val HavenWordmarkStyle = TextStyle(
    fontFamily = FontFamily(
        cut(FontWeight.SemiBold, 620, width = 118f, opticalSize = 48f),
    ),
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.03).em,
    lineHeight = 1.1.em,
)
