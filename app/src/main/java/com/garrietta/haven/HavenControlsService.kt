package com.garrietta.haven

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.actions.ModeAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.TemperatureControlTemplate
import android.service.controls.templates.ToggleRangeTemplate
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import com.garrietta.haven.data.ClimateMode
import com.garrietta.haven.data.ControlKind
import com.garrietta.haven.data.ControlSpec
import com.garrietta.haven.data.HaClient
import com.garrietta.haven.data.SettingsStore
import com.garrietta.haven.data.TokenStore
import com.garrietta.haven.data.clampSetpoint
import com.garrietta.haven.data.climateBooleanMode
import com.garrietta.haven.data.climateModeFromHa
import com.garrietta.haven.data.climateModeToHa
import com.garrietta.haven.data.defaultControls
import com.garrietta.haven.data.mapEntityToControl
import com.garrietta.haven.data.shiftClimateBand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.Flow

/**
 * Android Device Controls provider (customization round, item 10):
 * Haven appears in Samsung's Device control panel / Manage apps
 * list (SmartThings, Home, Home Assistant — and now Haven) without
 * touching the user's existing selections. Manifest-only reference:
 * no app code ever names this class, so API 26–29 devices never
 * load it ([RequiresApi] R).
 *
 * Control set: the user's Haven favorites first (in favorite
 * order), then every other mappable entity sorted by name — the
 * panel is a quick-control surface, so the user's chosen set leads.
 * Mapping lives in ControlsMapping (pure, tested); this service
 * only renders specs and forwards actions to HaClient.
 *
 * Not logged in: exactly ONE stateless control, "Open Haven to
 * sign in", whose app intent opens MainActivity — an honest
 * launcher, never a fake device, and never a crash. Logged in but
 * no states within ~10s: the one-shot publisher completes EMPTY
 * (never the sign-in tile — that tile for a signed-in user was
 * misleading and lingered as the panel's only Haven tile); the
 * panel re-queries on its next open. Token refresh is deliberately
 * NOT reimplemented here (HaClient has no refresh path); the
 * sign-in control is the graceful state.
 */

/**
 * Feedback round 3, item 6: the app-intent extra carrying the
 * tapped control's entity id, so a Device Controls tap deep-links
 * to that entity's surface instead of landing on Home. MainActivity
 * reads it and stores it on the VM; ControlDeepLinkRouter consumes
 * it. The sign-in control carries "haven_open_app", which matches
 * no entity and clears without routing.
 */
const val EXTRA_CONTROL_ENTITY_ID = "com.garrietta.haven.CONTROL_ENTITY_ID"

@RequiresApi(Build.VERSION_CODES.R)
class HavenControlsService : ControlsProviderService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val tokenStore by lazy { TokenStore(applicationContext) }
    private val settingsStore by lazy { SettingsStore(applicationContext) }
    private val client by lazy { HaClient(serviceScope) }
    private var startedUrl: String? = null
    private var startedToken: String? = null

    override fun onCreate() {
        super.onCreate()
        // Feedback round 2, item 9: connect EAGERLY when the
        // service is created. The panel binds this service before
        // it asks for controls, so a cold panel open used to pay
        // the full WS-connect + state-dump latency inside
        // SystemUI's publish window — the "controls missing, then
        // appearing on their own" transient. Connecting here moves
        // that cost ahead of the first publisher request.
        ensureStarted()
    }

    private fun appIntentFor(entityId: String): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        // Feedback round 3, item 6: carry the tapped entity so the
        // app can deep-link to its surface (previously the intent
        // carried nothing, so every control tap landed on Home).
        // Per-entity requestCode (already) keeps each control's
        // intent distinct under FLAG_UPDATE_CURRENT. SINGLE_TOP
        // routes a tap into the running single-activity instance
        // via onNewIntent instead of stacking a second MainActivity.
        intent.putExtra(EXTRA_CONTROL_ENTITY_ID, entityId)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            applicationContext,
            entityId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureStarted() {
        val url = tokenStore.instanceUrl
        val token = tokenStore.accessToken
        if (url.isNullOrBlank() || token.isNullOrBlank()) {
            // Signed out (or never signed in) after a previous
            // start: drop the stale connection so no controls keep
            // publishing for a signed-out session.
            if (startedUrl != null) {
                client.disconnect()
                startedUrl = null
                startedToken = null
            }
            return
        }
        if (startedUrl == url && startedToken == token) return
        // Token/URL changed since the last start (sign-out/in,
        // token rotation): the old one-shot `started` flag kept
        // the stale session forever. Reconnect on the new pair.
        if (startedUrl != null) client.disconnect()
        startedUrl = url
        startedToken = token
        client.connect(url, token)
    }

    private fun isLoggedIn(): Boolean =
        !tokenStore.instanceUrl.isNullOrBlank() && !tokenStore.accessToken.isNullOrBlank()

    private fun signInControl(): Control =
        Control.StatelessBuilder("haven_open_app", appIntentFor("haven_open_app"))
            .setTitle("Open Haven to sign in")
            .setSubtitle("Sign in to Home Assistant in the app")
            .build()

    private fun controlForSpec(spec: ControlSpec, entityState: String): Control {
        val builder = Control.StatefulBuilder(spec.entityId, appIntentFor(spec.entityId))
            .setTitle(spec.title)
            .setDeviceType(spec.deviceType)
            .setStatus(Control.STATUS_OK)
            .setStatusText(entityState)
        val template = when (spec.kind) {
            ControlKind.TOGGLE -> ToggleTemplate(
                spec.entityId,
                ControlButton(spec.booleanValue == true, "Toggle"),
            )
            ControlKind.RANGE -> RangeTemplate(
                spec.entityId, 0f, 100f, spec.rangeValue ?: 0f, 1f, "%1$.0f%%",
            )
            ControlKind.TOGGLE_RANGE -> ToggleRangeTemplate(
                spec.entityId,
                spec.booleanValue == true,
                "Toggle",
                RangeTemplate(spec.entityId, 0f, 100f, spec.rangeValue ?: 0f, 1f, "%1$.0f%%"),
            )
            ControlKind.TEMPERATURE -> {
                // Feedback round 2, item 8: the temperature panel
                // widget IS the in-panel popup (Android docs'
                // "Open temperature panel widget": tap = panel,
                // long-press = app). Two construction defects
                // could push a panel to the app intent instead:
                // (1) currentActiveMode duplicated currentMode, so
                // a heat_cool system advertised HEAT_COOL as its
                // active mode — a selected mode, not an action;
                // it now comes from hvac_action (heating -> HEAT,
                // cooling -> COOL, idle -> selected mode). (2) A
                // thermostat whose hvac_modes mapped to no flags
                // published an empty mode set, which cannot render
                // a detail panel; it now falls back to the selected
                // mode's flag. The app intent stays as the panel's
                // long-press "open app" affordance only.
                var flags = 0
                for (mode in spec.climateModes) {
                    flags = flags or when (climateModeFromHa(mode)) {
                        ClimateMode.OFF -> TemperatureControlTemplate.FLAG_MODE_OFF
                        ClimateMode.HEAT -> TemperatureControlTemplate.FLAG_MODE_HEAT
                        ClimateMode.COOL -> TemperatureControlTemplate.FLAG_MODE_COOL
                        ClimateMode.HEAT_COOL -> TemperatureControlTemplate.FLAG_MODE_HEAT_COOL
                        ClimateMode.ECO -> TemperatureControlTemplate.FLAG_MODE_ECO
                        null -> 0
                    }
                }
                val currentMode = climateModeFromHa(spec.climateMode ?: "")?.let { modeToTemplate(it) }
                    ?: TemperatureControlTemplate.MODE_UNKNOWN
                if (flags == 0) {
                    flags = when (climateModeFromHa(spec.climateMode ?: "")) {
                        ClimateMode.OFF -> TemperatureControlTemplate.FLAG_MODE_OFF
                        ClimateMode.HEAT -> TemperatureControlTemplate.FLAG_MODE_HEAT
                        ClimateMode.COOL -> TemperatureControlTemplate.FLAG_MODE_COOL
                        ClimateMode.HEAT_COOL -> TemperatureControlTemplate.FLAG_MODE_HEAT_COOL
                        ClimateMode.ECO -> TemperatureControlTemplate.FLAG_MODE_ECO
                        null -> 0
                    }
                }
                val currentActiveMode = spec.climateActiveMode?.let { modeToTemplate(it) } ?: currentMode
                TemperatureControlTemplate(
                    spec.entityId,
                    RangeTemplate(
                        spec.entityId,
                        spec.minTemp ?: 50f,
                        spec.maxTemp ?: 90f,
                        spec.targetTemp ?: spec.currentTemp ?: (spec.minTemp ?: 50f),
                        1f,
                        "%1$.0f°",
                    ),
                    currentMode,
                    currentActiveMode,
                    flags,
                )
            }
        }
        return builder.setControlTemplate(template).build()
    }

    private fun modeToTemplate(mode: ClimateMode): Int = when (mode) {
        ClimateMode.OFF -> TemperatureControlTemplate.MODE_OFF
        ClimateMode.HEAT -> TemperatureControlTemplate.MODE_HEAT
        ClimateMode.COOL -> TemperatureControlTemplate.MODE_COOL
        ClimateMode.HEAT_COOL -> TemperatureControlTemplate.MODE_HEAT_COOL
        ClimateMode.ECO -> TemperatureControlTemplate.MODE_ECO
    }

    private fun modeFromTemplate(mode: Int): ClimateMode? = when (mode) {
        TemperatureControlTemplate.MODE_OFF -> ClimateMode.OFF
        TemperatureControlTemplate.MODE_HEAT -> ClimateMode.HEAT
        TemperatureControlTemplate.MODE_COOL -> ClimateMode.COOL
        TemperatureControlTemplate.MODE_HEAT_COOL -> ClimateMode.HEAT_COOL
        TemperatureControlTemplate.MODE_ECO -> ClimateMode.ECO
        else -> null
    }

    /** Stateful controls for the requested ids from the current snapshot; unknown ids -> STATUS_NOT_FOUND. */
    private fun snapshotControls(ids: List<String>): List<Control> {
        val states = client.states.value
        return ids.map { id ->
            val entity = states[id]
            if (entity == null) {
                Control.StatefulBuilder(id, appIntentFor(id))
                    .setTitle(id)
                    .setStatus(Control.STATUS_NOT_FOUND)
                    .build()
            } else {
                val spec = mapEntityToControl(entity)
                if (spec != null) controlForSpec(spec, entity.state)
                else Control.StatefulBuilder(id, appIntentFor(id))
                    .setTitle(entity.friendlyName)
                    .setStatus(Control.STATUS_UNKNOWN)
                    .build()
            }
        }
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        ensureStarted()
        return object : Flow.Publisher<Control> {
            override fun subscribe(subscriber: Flow.Subscriber<in Control>) {
                subscriber.onSubscribe(object : Flow.Subscription {
                    override fun request(n: Long) {}
                    override fun cancel() {}
                })
                serviceScope.launch {
                    if (!isLoggedIn()) {
                        subscriber.onNext(signInControl())
                        subscriber.onComplete()
                        return@launch
                    }
                    // One-shot list: wait briefly for states, then
                    // publish the default set (favorites first).
                    // Still empty after the wait while LOGGED IN =
                    // a slow/unavailable session, not "signed out":
                    // complete empty (never the sign-in tile — see
                    // the class KDoc). The panel re-queries on its
                    // next open, and the live publisher keeps
                    // updating already-added controls meanwhile.
                    val states = withTimeoutOrNull(10_000) {
                        client.states.filter { it.isNotEmpty() }.first()
                    }
                    if (states == null) {
                        // No emission: see the comment above.
                    } else {
                        val specs = defaultControls(states.values.toList(), settingsStore.favoriteEntityIds())
                        for (spec in specs) {
                            val entity = states[spec.entityId]
                            if (entity != null) subscriber.onNext(controlForSpec(spec, entity.state))
                        }
                    }
                    subscriber.onComplete()
                }
            }
        }
    }

    override fun createPublisherFor(controlIds: List<String>): Flow.Publisher<Control> {
        ensureStarted()
        return object : Flow.Publisher<Control> {
            override fun subscribe(subscriber: Flow.Subscriber<in Control>) {
                var job: kotlinx.coroutines.Job? = null
                subscriber.onSubscribe(object : Flow.Subscription {
                    override fun request(n: Long) {}
                    override fun cancel() { job?.cancel() }
                })
                job = serviceScope.launch {
                    if (!isLoggedIn()) {
                        subscriber.onNext(signInControl())
                        return@launch
                    }
                    client.states.filter { it.isNotEmpty() }.collect { states ->
                        // Re-emit the requested set on every state
                        // change, so toggles in the panel track HA.
                        // (states is non-empty here by the filter.)
                        @Suppress("UNUSED_VARIABLE")
                        val ignored = states
                        for (control in snapshotControls(controlIds)) {
                            subscriber.onNext(control)
                        }
                    }
                }
            }
        }
    }

    override fun performControlAction(
        controlId: String,
        action: ControlAction,
        consumer: java.util.function.Consumer<Int>,
    ) {
        if (controlId == "haven_open_app") {
            consumer.accept(ControlAction.RESPONSE_OK)
            return
        }
        val entity = client.states.value[controlId]
        if (entity == null) {
            consumer.accept(ControlAction.RESPONSE_FAIL)
            return
        }
        val domain = entity.domain
        fun done(ok: Boolean) = consumer.accept(
            if (ok) ControlAction.RESPONSE_OK else ControlAction.RESPONSE_FAIL,
        )
        when (action) {
            is BooleanAction -> {
                when (domain) {
                    "light", "switch", "input_boolean", "fan" -> {
                        val service = if (action.newState) "turn_on" else "turn_off"
                        client.callService(domain, service, controlId, null) { done(it) }
                    }
                    "cover" -> {
                        val service = if (action.newState) "open_cover" else "close_cover"
                        client.callService(domain, service, controlId, null) { done(it) }
                    }
                    "climate" -> {
                        val spec = mapEntityToControl(entity)
                        val mode = spec?.let { climateBooleanMode(it.climateModes, action.newState) }
                        if (mode == null) consumer.accept(ControlAction.RESPONSE_FAIL)
                        else client.callService(domain, "set_hvac_mode", controlId,
                            JSONObject().put("hvac_mode", mode)) { done(it) }
                    }
                    else -> consumer.accept(ControlAction.RESPONSE_UNKNOWN)
                }
            }
            is FloatAction -> {
                when (domain) {
                    "light" -> client.callService(domain, "turn_on", controlId,
                        JSONObject().put("brightness_pct", action.newValue.toInt())) { done(it) }
                    "cover" -> client.callService(domain, "set_cover_position", controlId,
                        JSONObject().put("position", action.newValue.toInt())) { done(it) }
                    "climate" -> {
                        val spec = mapEntityToControl(entity)
                        if (spec == null) {
                            consumer.accept(ControlAction.RESPONSE_FAIL)
                        } else if (spec.climateMode == "heat_cool" && spec.targetHigh != null && spec.targetLow != null) {
                            val band = shiftClimateBand(spec.targetHigh, spec.targetLow, action.newValue, spec.minTemp, spec.maxTemp)
                            if (band == null) consumer.accept(ControlAction.RESPONSE_FAIL)
                            else client.callService(domain, "set_temperature", controlId,
                                JSONObject().put("target_temp_low", band.first.toDouble())
                                    .put("target_temp_high", band.second.toDouble())) { done(it) }
                        } else {
                            val value = clampSetpoint(action.newValue, spec.minTemp, spec.maxTemp)
                            client.callService(domain, "set_temperature", controlId,
                                JSONObject().put("temperature", value.toDouble())) { done(it) }
                        }
                    }
                    else -> consumer.accept(ControlAction.RESPONSE_UNKNOWN)
                }
            }
            is ModeAction -> {
                if (domain != "climate") {
                    consumer.accept(ControlAction.RESPONSE_UNKNOWN)
                    return
                }
                val mode = modeFromTemplate(action.newMode)?.let { climateModeToHa(it) }
                val spec = mapEntityToControl(entity)
                if (mode == null || spec == null || mode !in spec.climateModes) {
                    consumer.accept(ControlAction.RESPONSE_FAIL)
                } else {
                    client.callService(domain, "set_hvac_mode", controlId,
                        JSONObject().put("hvac_mode", mode)) { done(it) }
                }
            }
            else -> consumer.accept(ControlAction.RESPONSE_UNKNOWN)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
