package com.garrietta.haven.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.garrietta.haven.EXTRA_CONTROL_ENTITY_ID
import com.garrietta.haven.MainActivity
import com.garrietta.haven.R
import com.garrietta.haven.data.ClimateWidgetModel
import com.garrietta.haven.data.ControlTile
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.climateWidgetModel
import com.garrietta.haven.data.controlTiles
import com.garrietta.haven.data.fmtDegrees
import com.garrietta.haven.data.nudgedSetpoint
import com.garrietta.haven.data.securityAlarm
import com.garrietta.haven.data.securitySummary
import com.garrietta.haven.data.SettingsStore
import com.garrietta.haven.ui.theme.StateFamily
import com.garrietta.haven.ui.theme.colors
import com.garrietta.haven.ui.theme.havenDarkColorScheme
import com.garrietta.haven.ui.theme.havenLightColorScheme
import org.json.JSONObject

/*
 * Haven's home screen widgets: Climate (the thermostat, its set point a tap
 * from changing), Controls (the favorite switches) and Security (whether
 * the house is shut). Each draws from the states the app saved last and
 * refreshes its own entities when it updates; a tap acts through Home
 * Assistant's REST API with the app's sign-in (WidgetHa). Colors are the
 * app's: its scheme, and the state families for what a thing is doing.
 */

/** The app's scheme for both day and night, so the widgets wear Haven's colors, not the wallpaper's. */
private fun havenWidgetColors() = ColorProviders(light = havenLightColorScheme(), dark = havenDarkColorScheme())

/** A state family as day and night colors: the container a card wears and the ink and badge on it. */
private class FamilyProviders(family: StateFamily) {
    private val day = family.colors(false)
    private val night = family.colors(true)
    val container = ColorProvider(day.container, night.container)
    val onContainer = ColorProvider(day.onContainer, night.onContainer)
    val accent = ColorProvider(day.accent, night.accent)
    val onAccent = ColorProvider(day.onAccent, night.onAccent)
}

/** A rounded card: a tinted shape drawable, since corner radii are a no-op on Glance below API 31. */
private fun GlanceModifier.card(color: ColorProvider): GlanceModifier =
    this.background(ImageProvider(R.drawable.widget_card), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(color))
        .cornerRadius(24.dp)

/** Opens the app, on [entityId]'s own surface when given (the same deep link Device Controls use). */
private fun openApp(entityId: String? = null) = actionStartActivity<MainActivity>(
    if (entityId != null) actionParametersOf(ActionParameters.Key<String>(EXTRA_CONTROL_ENTITY_ID) to entityId)
    else actionParametersOf(),
)

@Composable
private fun Label(text: String, color: ColorProvider, size: TextUnit, bold: Boolean = false, lines: Int = 1) {
    Text(
        text,
        style = TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal),
        maxLines = lines,
    )
}

/** Brings every Haven widget up to date with the saved states. */
object HavenWidgets {
    suspend fun updateAll(context: Context) {
        ClimateWidget().updateAll(context)
        ControlsWidget().updateAll(context)
        SecurityWidget().updateAll(context)
    }
}

// ---------------------------------------------------------------- Climate

/**
 * Climate: the thermostat at work, in its color (ember heating, frost
 * cooling): what it reads, what it is doing, and its set point with − and
 * + either side. A tap anywhere else opens its dial in the app.
 */
class ClimateWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val before = climateWidgetModel(WidgetHa.cachedStates(context))
        if (before != null) WidgetHa.refresh(context, listOf(before.entityId))
        val model = climateWidgetModel(WidgetHa.cachedStates(context))
        provideContent { GlanceTheme(colors = havenWidgetColors()) { ClimateContent(model) } }
    }
}

@Composable
private fun ClimateContent(model: ClimateWidgetModel?) {
    if (model == null) return EmptyCard("No thermostat yet")
    val fam = FamilyProviders(
        when (model.mood) {
            "heating" -> StateFamily.Heat
            "cooling" -> StateFamily.Cool
            "off" -> StateFamily.Quiet
            else -> StateFamily.On
        },
    )
    Column(
        GlanceModifier.fillMaxSize().card(fam.container).clickable(openApp(model.entityId)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(model.name, fam.onContainer, 13.sp, bold = true)
        Label(model.current?.let { "${fmtDegrees(it)}°" } ?: "—", fam.onContainer, 40.sp, bold = true)
        Label(model.activity, fam.onContainer, 12.sp)
        val setTo = model.target ?: model.low
        if (setTo != null && model.mood != "off") {
            Spacer(GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("−", fam, actionRunCallback<NudgeSetpoint>(actionParametersOf(NudgeSetpoint.UP to false)))
                Spacer(GlanceModifier.width(10.dp))
                Label("${fmtDegrees(setTo)}°", fam.onContainer, 18.sp, bold = true)
                Spacer(GlanceModifier.width(10.dp))
                RoundButton("+", fam, actionRunCallback<NudgeSetpoint>(actionParametersOf(NudgeSetpoint.UP to true)))
            }
        }
    }
}

@Composable
private fun RoundButton(glyph: String, fam: FamilyProviders, action: androidx.glance.action.Action) {
    Box(
        GlanceModifier.size(40.dp)
            .background(ImageProvider(R.drawable.widget_round), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(fam.accent))
            .cornerRadius(20.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Label(glyph, fam.onAccent, 20.sp, bold = true)
    }
}

/** One step of the thermostat's set point; a range moves both ends together. */
class NudgeSetpoint : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val up = parameters[UP] ?: return
        val model = climateWidgetModel(WidgetHa.cachedStates(context)) ?: return
        val next = nudgedSetpoint(model, up) ?: return
        val data = JSONObject().put("entity_id", model.entityId)
        val changes: Map<String, Double> = if (model.target != null) {
            mapOf("temperature" to next)
        } else {
            val low = model.low ?: return
            val high = model.high ?: return
            val delta = next - low
            mapOf("target_temp_low" to low + delta, "target_temp_high" to high + delta)
        }
        changes.forEach { (key, value) -> data.put(key, value) }
        // Shown at once, then whatever Home Assistant reports.
        WidgetHa.cachedStates(context)[model.entityId]?.let { t ->
            WidgetHa.assume(context, t.copy(attributes = t.attributes + changes))
        }
        ClimateWidget().updateAll(context)
        WidgetHa.callService(context, "climate", "set_temperature", data)
        WidgetHa.refresh(context, listOf(model.entityId))
        ClimateWidget().updateAll(context)
    }

    companion object {
        val UP = ActionParameters.Key<Boolean>("up")
    }
}

// --------------------------------------------------------------- Controls

/**
 * Controls: the favorite switches as tiles, lit in their color when on.
 * A tap switches one; it answers at once and settles to what Home
 * Assistant reports.
 */
class ControlsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val favorites = SettingsStore(context).favoriteEntityIds()
        val before = controlTiles(WidgetHa.cachedStates(context), favorites)
        WidgetHa.refresh(context, before.map { it.entityId })
        val tiles = controlTiles(WidgetHa.cachedStates(context), favorites)
        provideContent { GlanceTheme(colors = havenWidgetColors()) { ControlsContent(tiles) } }
    }
}

@Composable
private fun ControlsContent(tiles: List<ControlTile>) {
    if (tiles.isEmpty()) return EmptyCard("Star devices in Haven to put them here")
    Column(GlanceModifier.fillMaxSize().card(GlanceTheme.colors.surface).padding(8.dp)) {
        for (row in tiles.chunked(3)) {
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                row.forEachIndexed { index, tile ->
                    if (index > 0) Spacer(GlanceModifier.width(6.dp))
                    ControlTileView(tile, GlanceModifier.defaultWeight())
                }
                repeat(3 - row.size) {
                    Spacer(GlanceModifier.width(6.dp))
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
            Spacer(GlanceModifier.height(6.dp))
        }
    }
}

@Composable
private fun ControlTileView(tile: ControlTile, modifier: GlanceModifier) {
    val fam = FamilyProviders(
        when {
            !tile.on -> StateFamily.Quiet
            tile.entityId.startsWith("light.") -> StateFamily.Light
            else -> StateFamily.On
        },
    )
    Column(
        modifier.fillMaxSize().card(fam.container)
            .clickable(actionRunCallback<ToggleEntity>(actionParametersOf(ToggleEntity.ENTITY to tile.entityId)))
            .padding(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Label(tile.name, fam.onContainer, 13.sp, bold = true, lines = 2)
        Label(if (!tile.available) "Unavailable" else if (tile.on) "On" else "Off", fam.onContainer, 11.sp)
    }
}

/** Switches one entity, shows the flip at once, and settles to what Home Assistant reports. */
class ToggleEntity : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ENTITY] ?: return
        WidgetHa.cachedStates(context)[id]?.let { e ->
            WidgetHa.assume(context, e.copy(state = if (e.state == "on") "off" else "on"))
        }
        ControlsWidget().updateAll(context)
        WidgetHa.callService(context, "homeassistant", "toggle", JSONObject().put("entity_id", id))
        WidgetHa.refresh(context, listOf(id))
        ControlsWidget().updateAll(context)
    }

    companion object {
        val ENTITY = ActionParameters.Key<String>("entity")
    }
}

// --------------------------------------------------------------- Security

/** Security: whether the house is shut, and the alarm, in the calm or the alert color. A tap opens the app. */
class SecurityWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val states = WidgetHa.cachedStates(context)
        provideContent { GlanceTheme(colors = havenWidgetColors()) { SecurityContent(states) } }
    }
}

@Composable
private fun SecurityContent(states: Map<String, HaEntity>) {
    if (states.isEmpty()) return EmptyCard("Sign in to Haven")
    val summary = securitySummary(states)
    val alarm = securityAlarm(states)
    val fam = FamilyProviders(if (summary.calm) StateFamily.Secure else StateFamily.Alert)
    Column(
        GlanceModifier.fillMaxSize().card(fam.container).clickable(openApp()).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label("Security", fam.onContainer, 13.sp, bold = true)
        Spacer(GlanceModifier.height(4.dp))
        Label(summary.line, fam.onContainer, 18.sp, bold = true, lines = 3)
        if (alarm != null) {
            Spacer(GlanceModifier.height(6.dp))
            Label("Alarm: " + alarm.state.replace('_', ' ').replaceFirstChar { it.uppercase() }, fam.onContainer, 12.sp)
        }
    }
}

// ------------------------------------------------------------------ shared

@Composable
private fun EmptyCard(message: String) {
    Box(
        GlanceModifier.fillMaxSize().card(GlanceTheme.colors.surface).clickable(openApp()).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(message, GlanceTheme.colors.onSurface, 13.sp, lines = 3)
    }
}

class ClimateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClimateWidget()
}

class ControlsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ControlsWidget()
}

class SecurityWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SecurityWidget()
}
