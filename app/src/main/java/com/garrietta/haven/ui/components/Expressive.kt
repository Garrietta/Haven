package com.garrietta.haven.ui.components

import androidx.compose.animation.core.LinearEasing
import com.garrietta.haven.ui.theme.EmphasizedDecelerate
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.HistoryPoint
import com.garrietta.haven.ui.screens.DialTapZone
import com.garrietta.haven.ui.screens.dialTapZone
import com.garrietta.haven.ui.screens.dialValueFromAngle
import com.garrietta.haven.ui.screens.isHandleGrab
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.MorphPolygon
import com.garrietta.haven.ui.theme.MorphShape
import com.garrietta.haven.ui.theme.havenSpring
import com.garrietta.haven.ui.theme.polygonShape
import com.garrietta.haven.ui.theme.springMorph
import com.garrietta.haven.ui.theme.expressivePolygon
import com.garrietta.haven.ui.theme.stateShape
import com.garrietta.haven.ui.theme.StateShape
import com.garrietta.haven.ui.theme.stateShapePolygon
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Provided by HavenApp from the user's settings. */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * True when the window is wide enough for two-pane layouts (unfolded
 * Fold8). The threshold is 600dp, not Material's 840dp "expanded" bucket:
 * the Fold8 unfolded is ~690dp portrait / ~830dp landscape, so 840dp
 * would miss unfolded landscape entirely. Folded (~360-400dp) stays
 * single-column.
 */
val LocalWideLayout = compositionLocalOf { false }

/** Stable per-entity accent color, hashed from the entity id (same idea as Relay's palettes).
 * Not a scheme role: an identity palette — each entity keeps its accent across launches. */
private val ACCENTS = listOf(
    Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF8B5CF6),
    Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF84CC16), Color(0xFF3B82F6),
    Color(0xFFEAB308), Color(0xFF14B8A6), Color(0xFFF43F5E), Color(0xFFA855F7),
)

fun accentFor(entityId: String): Color {
    val hash = entityId.hashCode()
    val idx = ((hash % ACCENTS.size) + ACCENTS.size) % ACCENTS.size
    return ACCENTS[idx]
}

/**
 * One expressive shape family per domain, shared by tiles, detail pages,
 * sheets, and badges so the whole app reads as one set: lights bloom
 * (SUNNY), climate squishes (BUN), cameras flower (FLOWER), covers burst
 * (BURST), media and fans ripple (WAVE), weather and sensors cloud
 * (CLOUD). Everything else falls back to BUN.
 */
fun shapeForDomain(domain: String): ExpressiveKind = when (domain) {
    "light" -> ExpressiveKind.SUNNY
    "climate" -> ExpressiveKind.BUN
    "camera" -> ExpressiveKind.FLOWER
    "cover" -> ExpressiveKind.BURST
    "switch", "lock", "input_boolean" -> ExpressiveKind.BURST
    "media_player", "fan" -> ExpressiveKind.WAVE
    "weather", "sensor", "binary_sensor" -> ExpressiveKind.CLOUD
    else -> ExpressiveKind.BUN
}

/**
 * Shape picker (2026-10-30 directive: a menu to select the shape of
 * every icon and expressive shape in the app). Every pickable shape
 * has a stable string key so choices persist in SettingsStore: the
 * six ExpressiveKind families plus the ten StateShape polygons.
 * Keys are storage keys, never display strings.
 */
data class ShapeOption(val key: String, val label: String)

val SHAPE_OPTIONS: List<ShapeOption> = listOf(
    ShapeOption("sunny", "Sunny"),
    ShapeOption("flower", "Flower"),
    ShapeOption("bun", "Bun"),
    ShapeOption("burst", "Burst"),
    ShapeOption("wave", "Wave"),
    ShapeOption("cloud", "Cloud"),
    ShapeOption("hexagon", "Hexagon"),
    ShapeOption("scallop12", "Scallop"),
    ShapeOption("squircle", "Squircle"),
    ShapeOption("arc_wave", "Arc wave"),
    ShapeOption("shield", "Shield"),
    ShapeOption("notched_diamond", "Notched diamond"),
    ShapeOption("star8", "Star"),
    ShapeOption("octagon", "Octagon"),
    ShapeOption("sunburst", "Sunburst"),
    ShapeOption("cloud_poly", "Cloud poly"),
)

fun shapeLabel(key: String?): String? =
    SHAPE_OPTIONS.firstOrNull { it.key == key }?.label

/** A pickable key as a Compose [Shape] (for backgrounds and clips), or null for an unknown key. */
fun shapeForKey(key: String): Shape? = when (key) {
    "sunny" -> polygonShape(ExpressiveKind.SUNNY)
    "flower" -> polygonShape(ExpressiveKind.FLOWER)
    "bun" -> polygonShape(ExpressiveKind.BUN)
    "burst" -> polygonShape(ExpressiveKind.BURST)
    "wave" -> polygonShape(ExpressiveKind.WAVE)
    "cloud" -> polygonShape(ExpressiveKind.CLOUD)
    "hexagon" -> stateShape(StateShape.HEXAGON)
    "scallop12" -> stateShape(StateShape.SCALLOP12)
    "squircle" -> stateShape(StateShape.SQUIRCLE)
    "arc_wave" -> stateShape(StateShape.ARC_WAVE)
    "shield" -> stateShape(StateShape.SHIELD)
    "notched_diamond" -> stateShape(StateShape.NOTCHED_DIAMOND)
    "star8" -> stateShape(StateShape.STAR8)
    "octagon" -> stateShape(StateShape.OCTAGON)
    "sunburst" -> stateShape(StateShape.SUNBURST)
    "cloud_poly" -> stateShape(StateShape.CLOUD_POLY)
    else -> null
}

/** A pickable key as a morphable polygon (for [MorphPolygon] paths), or null for an unknown key. */
fun polygonForKey(key: String): RoundedPolygon? = when (key) {
    "sunny" -> expressivePolygon(ExpressiveKind.SUNNY)
    "flower" -> expressivePolygon(ExpressiveKind.FLOWER)
    "bun" -> expressivePolygon(ExpressiveKind.BUN)
    "burst" -> expressivePolygon(ExpressiveKind.BURST)
    "wave" -> expressivePolygon(ExpressiveKind.WAVE)
    "cloud" -> expressivePolygon(ExpressiveKind.CLOUD)
    "hexagon" -> stateShapePolygon(StateShape.HEXAGON)
    "scallop12" -> stateShapePolygon(StateShape.SCALLOP12)
    "squircle" -> stateShapePolygon(StateShape.SQUIRCLE)
    "arc_wave" -> stateShapePolygon(StateShape.ARC_WAVE)
    "shield" -> stateShapePolygon(StateShape.SHIELD)
    "notched_diamond" -> stateShapePolygon(StateShape.NOTCHED_DIAMOND)
    "star8" -> stateShapePolygon(StateShape.STAR8)
    "octagon" -> stateShapePolygon(StateShape.OCTAGON)
    "sunburst" -> stateShapePolygon(StateShape.SUNBURST)
    "cloud_poly" -> stateShapePolygon(StateShape.CLOUD_POLY)
    else -> null
}

/**
 * The resolved shape preferences, provided by HavenApp from
 * SettingsStore (re-read whenever [HavenViewModel.bumpShapePrefs]
 * fires, so a picker choice applies immediately). Per-entity
 * overrides beat the global default; null means "domain default"
 * (badges) or "default corners" (cards). Menu-row badges keep
 * their fixed kinds — they are navigation chrome, not entity
 * shapes — and the camera hero container stays its rectangular
 * 24dp shape by design.
 */
data class ShapePrefs(
    val globalBadge: String?,
    val globalCard: String?,
    val badgeOverrides: Map<String, String>,
    val cardOverrides: Map<String, String>,
) {
    fun badgeShapeKey(entityId: String): String? = badgeOverrides[entityId] ?: globalBadge
    fun cardShapeKey(entityId: String): String? = cardOverrides[entityId] ?: globalCard
    fun badgeShape(entityId: String): Shape? = badgeShapeKey(entityId)?.let { shapeForKey(it) }
    fun badgePolygon(entityId: String): RoundedPolygon? = badgeShapeKey(entityId)?.let { polygonForKey(it) }
    fun cardShape(entityId: String): Shape? = cardShapeKey(entityId)?.let { shapeForKey(it) }
}

val LocalShapePrefs = compositionLocalOf { ShapePrefs(null, null, emptyMap(), emptyMap()) }

/**
 * A dialog header name that fits its MEASURED width (2026-10-30
 * verdict round): the old char-count sizing (dialogNameSize)
 * still truncated "Thermostat Current Temperature" (30 chars) in
 * a 90%-width dialog, because 15sp × 30 chars ≈ 248dp exceeds the
 * ~200–224dp a 48dp badge + close leave on a folded phone.
 * This measures the real available width (BoxWithConstraints on
 * the title column) and picks the largest of 20/17/15/13sp that
 * fits on one line; if none fits, the name wraps to two lines at
 * 13sp — the full name always shows, with ellipsis only as the
 * last resort for names longer than two lines at 13sp. The
 * traveling header keeps its char-count interpolation (its end
 * size is dialogNameSize), but names longer than 28 chars travel
 * on two lines, so the handoff to this measured layout is a small
 * size step, never an ellipsis-to-full reflow.
 */
@Composable
fun DialogTitleText(name: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val sizes = listOf(20.sp, 17.sp, 15.sp, 13.sp)
        val chosen = sizes.firstOrNull { size ->
            val style = TextStyle(fontSize = size, fontWeight = FontWeight.Bold)
            measurer.measure(AnnotatedString(name), style).size.width <= maxWidthPx
        }
        if (chosen != null) {
            Text(name, fontSize = chosen, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Domain badge, now state-driven: the polygon morphs between the domain's
 * idle and active shapes on state change (Morph, spring 0.72/320), and
 * alert badges (triggered binary sensors) rotate continuously and pulse.
 * Media badges rotate slowly while playing; a traveling cover's badge
 * lifts gently. The badge fill follows the color-token pipeline (climate
 * containers, light color-temp blend, binary-sensor error, media
 * tertiary); domains with no token mapping keep the accent wash.
 */
@Composable
fun DomainBadge(entity: HaEntity, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val scheme = MaterialTheme.colorScheme
    val domain = entity.domain
    val state = entity.state
    val hvacAction = entity.attrString("hvac_action")
    val progressTarget = badgeMorphProgress(domain, state, hvacAction)
    val progress by animateFloatAsState(
        progressTarget,
        animationSpec = if (reduceMotion) spring(stiffness = 10_000f, dampingRatio = 1f) else springMorph(),
        label = "badgeMorph",
    )
    val alert = badgeAlert(domain, state)
    val playing = domain == "media_player" && state == "playing"
    val moving = domain == "cover" && coverIsMoving(state)
    val infinite = rememberInfiniteTransition(label = "badgeMotion")
    // Continuous rotation stays linear on purpose: an eased rotation
    // would visibly speed up and slow down every revolution. This is not
    // a transition, so the easing audit leaves it alone.
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(if (alert) 12000 else 20000, easing = LinearEasing), RepeatMode.Restart),
        label = "badgeRotation",
    )
    val pulse by infinite.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(900, easing = EmphasizedDecelerate), RepeatMode.Reverse),
        label = "badgePulse",
    )
    val lift by infinite.animateFloat(
        0f, -4f,
        infiniteRepeatable(tween(700, easing = EmphasizedDecelerate), RepeatMode.Reverse),
        label = "badgeLift",
    )
    val idle = stateShapePolygon(badgeIdleShape(domain))
    val active = stateShapePolygon(badgeActiveShape(domain, if (domain == "weather") state else null))
    val (bg, iconTint) = badgeColors(scheme, entity, accent)
    // Shape-picker override (2026-10-30): a picked shape replaces
    // the idle/active morph pair with that one static shape, in
    // the badge's exact colors — the picker's shape IS the badge.
    // Without an override the morph pair below is unchanged.
    val overrideShape = LocalShapePrefs.current.badgeShape(entity.entityId)
    Box(
        modifier.graphicsLayer {
            if (!reduceMotion) {
                if (alert || playing) rotationZ = rotation
                if (alert) {
                    scaleX = pulse
                    scaleY = pulse
                }
                if (moving) translationY = lift
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        if (overrideShape != null) {
            Box(Modifier.fillMaxSize().background(bg, overrideShape))
        } else {
            MorphPolygon(idle, active, progress, bg, Modifier.fillMaxSize())
        }
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * DomainBadge's fill + glyph colors, shared verbatim by
 * [TravelingBadge] so the traveling icon carries exactly the badge's
 * colors at both ends of its travel (no second color table to drift).
 */
fun badgeColors(
    scheme: androidx.compose.material3.ColorScheme,
    entity: HaEntity,
    accent: Color,
): Pair<Color, Color> {
    val domain = entity.domain
    val state = entity.state
    val hvacAction = entity.attrString("hvac_action")
    val alert = badgeAlert(domain, state)
    val playing = domain == "media_player" && state == "playing"
    val kelvin = entity.attrInt("color_temp_kelvin")
    return when (domain) {
        "light" -> {
            val on = state == "on"
            lightContainerColor(scheme, kelvin, on) to
                if (on) lerp(scheme.onSecondaryContainer, scheme.onTertiaryContainer, lightTempBlendFraction(kelvin))
                else scheme.onSurfaceVariant
        }
        "climate" -> when {
            state == "off" -> climateIdleContainer(scheme) to scheme.onSurfaceVariant
            hvacAction == "cooling" || state == "cool" ->
                coolSetpointContainer(scheme) to coolSetpointOnContainer(scheme)
            hvacAction == "heating" || state == "heat" || state == "heat_cool" -> {
                val dual = state == "heat_cool"
                heatSetpointContainer(scheme, dual) to heatSetpointOnContainer(scheme, dual)
            }
            else -> climateIdleContainer(scheme) to scheme.onSurfaceVariant
        }
        "binary_sensor" -> if (alert) scheme.errorContainer to scheme.onErrorContainer
            // Untriggered is an off state: grey like an off light
            // (2026-10-29 verdict — off icons grey everywhere, tiles
            // matching pop-ups), not a dimmed alert color.
            else scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        "media_player" -> if (playing) scheme.tertiaryContainer to scheme.onTertiaryContainer
            // Idle/paused is an off state: same grey rule.
            else scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        "switch", "input_boolean", "fan" -> if (state == "on") accent.copy(alpha = 0.22f) to accent
            // Off greys like an off light (same verdict rule); the
            // on branch is byte-identical to the old else branch.
            else scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        else -> accent.copy(alpha = 0.22f) to accent
    }
}

/**
 * The traveling badge (2026-10-29 verdict-fix round): the icon that
 * moves with the traveling header, MORPHING ITS SHAPE across the
 * morph — not just sliding and scaling a fixed polygon. [progress]
 * 0 = the source card's polygon, 1 = the pop-up header badge's state
 * polygon ([travelBadgeDestinationShape]). Tile sources render the
 * domain's expressive polygon ([shapeForDomain]) while header
 * badges render state polygons, so a tile icon visibly transforms
 * into the header shape as it travels; card sources already render
 * the state polygon, so their morph is correctly the identity
 * (same polygon, only size + position change). The interpolation is
 * the proven Morph + forEachCubic path ([MorphPolygon], the Koda
 * #143 pattern): vertices interpolate, art is never inset inside a
 * mask — the polygon fills its box exactly at every progress, so no
 * crescent gaps appear mid-morph. Colors come from [badgeColors],
 * identical to [DomainBadge]. Ambient badge motion (alert rotation,
 * playing rotation, cover lift) is deliberately NOT carried into
 * travel: the destination badge resumes it at rest, and a spinning
 * icon mid-travel would fight the shape morph.
 */
@Composable
fun TravelingBadge(
    entity: HaEntity,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    progress: Float,
    tileSource: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (bg, iconTint) = badgeColors(scheme, entity, accent)
    val domain = entity.domain
    val state = entity.state
    // Shape-picker override (2026-10-30): the traveler morphs
    // from the picked shape to itself (identity), so a picked
    // badge keeps its shape across the whole travel instead of
    // morphing through the domain/state pair.
    val overridePolygon = LocalShapePrefs.current.badgePolygon(entity.entityId)
    val from = overridePolygon
        ?: if (tileSource) com.garrietta.haven.ui.theme.expressivePolygon(shapeForDomain(domain))
        else stateShapePolygon(travelBadgeDestinationShape(domain, state, entity.attrString("hvac_action"), if (domain == "weather") state else null))
    val to = overridePolygon
        ?: stateShapePolygon(travelBadgeDestinationShape(domain, state, entity.attrString("hvac_action"), if (domain == "weather") state else null))
    Box(modifier, contentAlignment = Alignment.Center) {
        MorphPolygon(from, to, progress.coerceIn(0f, 1f), bg, Modifier.fillMaxSize())
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * Koda Settings menu DNA, adapted to Haven's dynamic colors (the layout /
 * surface / badge / header pattern, never Koda's hardcoded palette):
 * near-black screen background (the dynamic dark scheme already is),
 * dark rounded-rectangle rows, shaped icon badges in muted tones,
 * ALL-CAPS muted section headers, bold title + muted subtitle rows, and
 * a right chevron on rows that navigate. Used by Settings and by menu
 * rows in sheets, so every menu in the app reads as one set.
 */
@Composable
fun KodaMenuSection(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun KodaMenuRow(
    badgeKey: String,
    badgeKind: ExpressiveKind,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    /** Flat rows render background-less, for use inside a [KodaMenuGroup]. */
    flat: Boolean = false,
) {
    val accent = accentFor(badgeKey)
    val row = Modifier
        .fillMaxWidth()
        .then(if (flat) Modifier else Modifier.background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 12.dp)
    Row(row.then(modifier), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.background(accent.copy(alpha = 0.20f), polygonShape(badgeKind)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Inset grouped rounded section (spec item 9): an extraLarge-corner
 * surfaceContainer card holding flat [KodaMenuRow]s separated by
 * [KodaMenuDivider]s. Used by Settings; the sheet Details row keeps its
 * standalone 20dp row.
 */
@Composable
fun KodaMenuGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(content = content)
    }
}

/**
 * A 0.5dp divider indented to the text-start boundary: row padding 12dp
 * + badge 26dp + gap 12dp = 50dp from the card edge.
 */
@Composable
fun KodaMenuDivider(indent: Dp = 50.dp, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = indent),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Low-profile shimmer skeleton layer (spec item 8): an alpha-pulsing
 * horizontal gradient slab, for loading states that should not read as a
 * spinner on black. The caller sizes and clips it.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        0.35f, 0.75f,
        infiniteRepeatable(tween(1100, easing = EmphasizedDecelerate), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .background(
                Brush.horizontalGradient(
                    listOf(base.copy(alpha = 0.6f), base, base.copy(alpha = 0.6f)),
                ),
            ),
    )
}

/**
 * A big round dial: the value changes ONLY by grabbing the radial handle
 * and dragging it around the ring. Taps never change the value — a tap
 * inside the dial face (the center, clear of the ring) reports through
 * [onInsideTap] instead (the thermostat uses it to toggle its heating /
 * cooling selector), and taps on the ring, the handle, or the corners do
 * nothing. Drags that do not start on the handle are ignored, so a stray
 * swipe across the dial cannot move the value.
 *
 * The ring sweeps 270 degrees, leaving the gap at the bottom. Two drag
 * fixes carry over: a touch in the bottom gap is ignored (it used to snap
 * the value to min or max as the finger crossed it), and every computed
 * value is clamped to [range]. When [snapToWhole] is set, drag values
 * snap to whole units (the thermostat's 1-degree intervals). The handle
 * sits exactly on the track's centerline: the canvas is inset by the
 * handle radius, so neither the track's round caps nor the handle are
 * clipped at the edges.
 *
 * [markers] and the current-temperature indicator are display-only:
 * dots on the ring at their values, plus the current label fixed at the
 * top of the ring (Google Home style). They take no input — a tap on a
 * marker lands in the ring zone and does nothing, and a drag starting on
 * one is ignored because it does not start on the handle.
 */

/** A display-only dot on the dial ring at [value], in [color]. */
data class DialMarker(val value: Float, val color: Color)

@Composable
fun Dial(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: (Float) -> Unit = {},
    centerText: String,
    centerSub: String,
    accent: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,
    // Fired by a tap inside the dial face; null means inside taps do
    // nothing. Never changes the value — that is handle-drag only.
    onInsideTap: (() -> Unit)? = null,
    // Snap drag values to whole units (thermostat degrees).
    snapToWhole: Boolean = false,
    // Display-only dots on the ring (the thermostat's heat + cool
    // setpoints). Empty means no markers.
    markers: List<DialMarker> = emptyList(),
    // Current-temperature indicator: a dot on the ring at the current
    // angle plus the small label fixed at the top of the ring. Both a
    // value and a label are required; either missing hides both.
    currentValue: Float? = null,
    currentLabel: String? = null,
    // When set, replaces the default center text with custom content
    // (the thermostat page uses it for its dual current + target numbers).
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    // Read live inside the pointer-input blocks: the blocks only restart
    // when their keys change, so a plain captured fraction would go stale
    // as the value moves.
    val fractionState = rememberUpdatedState(fraction)
    // Last value the user actually set: onFinished reports this, not the
    // composable's value prop, so a drag-end applies the final position
    // even if the parent has not recomposed yet.
    var lastSet by remember { mutableStateOf(value) }
    // Whether the current drag started on the handle; reset on every drag
    // start, so only handle grabs move the value.
    var grabbing by remember { mutableStateOf(false) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val currentDotColor = MaterialTheme.colorScheme.onSurface
    val currentLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .size(diameter)
            // The tap detector is attached ONLY when an inside-tap
            // action exists: detectTapGestures consumes taps in its
            // area even when the callback is null, which would swallow
            // taps meant for an outer card gesture (the Home weather
            // card's tap → full page, whose dial has no inside action).
            // Thermostat dials pass onInsideTap, so they are unaffected.
            .then(
                if (onInsideTap != null) {
                    Modifier.pointerInput(range, snapToWhole) {
                        val handleRadiusPx = 13.dp.toPx()
                        val ringTolerancePx = 23.dp.toPx()
                        detectTapGestures { offset ->
                            val zone = dialTapZone(
                                offset.x, offset.y,
                                size.width / 2f, size.height / 2f,
                                minOf(size.width, size.height) / 2f - handleRadiusPx,
                                ringTolerancePx,
                            )
                            if (zone == DialTapZone.INSIDE) onInsideTap.invoke()
                        }
                    }
                } else Modifier
            )
            .pointerInput(range, snapToWhole) {
                val handleRadiusPx = 13.dp.toPx()
                val grabRadiusPx = handleRadiusPx + 15.dp.toPx()
                detectDragGestures(
                    onDragStart = { down ->
                        val trackRadius = minOf(size.width, size.height) / 2f - handleRadiusPx
                        val angle = Math.toRadians(135.0 + 270.0 * fractionState.value)
                        val hx = size.width / 2f + trackRadius * cos(angle).toFloat()
                        val hy = size.height / 2f + trackRadius * sin(angle).toFloat()
                        grabbing = isHandleGrab(down.x, down.y, hx, hy, grabRadiusPx)
                    },
                    onDragEnd = { if (grabbing) onFinished(lastSet) },
                    onDragCancel = { grabbing = false },
                ) { change, _ ->
                    if (!grabbing) return@detectDragGestures
                    val pos = change.position
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var deg = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
                    if (deg < 0) deg += 360f
                    dialValueFromAngle(deg, range)?.let { raw ->
                        val next = if (snapToWhole) raw.roundToInt().toFloat() else raw
                        lastSet = next
                        onValueChange(next)
                    }
                }
            },
    ) {
        // Inset by the handle radius: the arc's centerline then sits at
        // size.minDimension / 2 of the inset canvas, and the handle drawn
        // at that same radius lands exactly on the track instead of 10dp
        // inside it (the old (minDimension - stroke) / 2 math).
        Canvas(Modifier.size(diameter).padding(13.dp)) {
            val stroke = 20.dp.toPx()
            drawArc(track, 135f, 270f, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            drawArc(accent, 135f, 270f * fraction, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            val radius = size.minDimension / 2f
            val fractionFor: (Float) -> Float = {
                ((it - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            }
            // Display-only ring marks: smaller than the handle, drawn
            // under it, so the handle covers a marker it lands on.
            for (marker in markers) {
                val mAngle = Math.toRadians(135.0 + 270.0 * fractionFor(marker.value))
                drawCircle(
                    marker.color,
                    radius = 6.dp.toPx(),
                    center = Offset(
                        size.width / 2f + radius * cos(mAngle).toFloat(),
                        size.height / 2f + radius * sin(mAngle).toFloat(),
                    ),
                )
            }
            if (currentValue != null && currentLabel != null) {
                val cAngle = Math.toRadians(135.0 + 270.0 * fractionFor(currentValue))
                drawCircle(
                    currentDotColor,
                    radius = 5.dp.toPx(),
                    center = Offset(
                        size.width / 2f + radius * cos(cAngle).toFloat(),
                        size.height / 2f + radius * sin(cAngle).toFloat(),
                    ),
                )
                // The label sits fixed at the top of the ring, inside it
                // (Google Home's small current-temperature number), clear
                // of both the ring and the center content.
                val labelStyle = TextStyle(fontSize = 14.sp, color = currentLabelColor)
                val measured = textMeasurer.measure(currentLabel, labelStyle)
                val labelCenter = Offset(
                    size.width / 2f,
                    size.height / 2f - radius + 30.dp.toPx(),
                )
                drawText(
                    textMeasurer,
                    currentLabel,
                    topLeft = Offset(
                        labelCenter.x - measured.size.width / 2f,
                        labelCenter.y - measured.size.height / 2f,
                    ),
                    style = labelStyle,
                )
            }
            val angle = Math.toRadians((135.0 + 270.0 * fraction))
            drawCircle(
                accent,
                radius = 13.dp.toPx(),
                center = Offset(
                    size.width / 2f + radius * cos(angle).toFloat(),
                    size.height / 2f + radius * sin(angle).toFloat(),
                ),
            )
        }
        if (centerContent != null) {
            Box(Modifier.align(Alignment.Center)) { centerContent() }
        } else {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(centerText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                // Verdict 2026-10-30: the condition subtitle
                // ("Clear-night") washed out at onSurfaceVariant on
                // the tinted dial panel — full onSurface here.
                Text(centerSub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * Smoothed cubic-bezier sparkline with a gradient alpha area fill under
 * the curve (spec item 7). The smoothing math lives in SparklineMath so
 * it is unit-testable; this composable only draws what it returns.
 *
 * The drawn series is bucket-resampled first (see resampleHistory): raw
 * 24-hour state-change history is dense and jittery, and the plain
 * midpoint curve tracked every sample, which read as a thin jagged
 * low-contrast line on detail pages. Resampling de-noises the line;
 * Min/Current/Max labels still come from the raw series. The line itself
 * is drawn heavier than before — a soft under-glow beneath a 4dp core —
 * with a stronger gradient fill, so it holds contrast on light
 * backgrounds without changing its accent hue.
 */
@Composable
fun Sparkline(points: List<HistoryPoint>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val segments = sparklineSegments(resampleHistory(points), size.width, size.height)
        if (segments.isEmpty()) return@Canvas
        val line = Path().apply {
            moveTo(segments.first().x0, segments.first().y0)
            segments.forEach { cubicTo(it.c1x, it.c1y, it.c2x, it.c2y, it.x1, it.y1) }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(segments.last().x1, size.height)
            lineTo(segments.first().x0, size.height)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0f)),
            ),
        )
        // Under-glow first, then the core: presence without a hue change.
        drawPath(line, color.copy(alpha = 0.15f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(line, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * A big pill toggle: a sliding knob on a spring, with a morphing shape behind it.
 * Used for switches and input_booleans where a plain Switch would look like a web form.
 */
@Composable
fun PillToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    onColor: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = havenSpring(),
        label = "pill",
    )
    val progress = if (reduceMotion) (if (checked) 1f else 0f) else animatedProgress
    val track = if (checked) onColor else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(width = 96.dp, height = 56.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable { onToggle() },
    ) {
        MorphShape(
            from = ExpressiveKind.BUN,
            to = ExpressiveKind.SUNNY,
            progress = progress,
            color = if (checked) Color.White.copy(alpha = 0.35f) // switch glyph on a colored track: white by M3 switch spec
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.size(56.dp).align(Alignment.Center),
        )
        Box(
            Modifier
                .padding(start = 6.dp + 40.dp * progress, top = 6.dp)
                .size(44.dp)
                .background(Color.White, RoundedCornerShape(50)), // switch thumb: white by M3 switch spec (50 = 50% pill, not dp)
        )
    }
}

/**
 * Bottom clearance for scrollable tab/Home screens: the pill bar is
 * 64dp tall, floats 20dp above the navigation-bars inset, and content
 * now scrolls full-height beneath it (the scaffold no longer reserves
 * the bottom-bar height — see HavenApp). Every scrollable top-level
 * screen pads its last item by this amount so nothing ends up
 * permanently hidden behind the pill.
 */
@Composable
fun pillClearanceBottom(): Dp {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 64dp pill + 20dp float + 8dp shadow margin: the old exact-fit
    // value let the last item visually touch the pill's shadow
    // (2026-10-30 verdict: the pill "rests directly over" bottom
    // items). The margin guarantees daylight between item and pill.
    return 64.dp + 20.dp + 8.dp + navBottom
}
