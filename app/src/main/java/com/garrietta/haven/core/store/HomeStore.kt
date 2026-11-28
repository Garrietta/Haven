package com.garrietta.haven.core.store

import android.util.Log
import com.garrietta.haven.core.model.Area
import com.garrietta.haven.core.model.Device
import com.garrietta.haven.core.model.EntityEntry
import com.garrietta.haven.core.model.Floor
import com.garrietta.haven.core.model.HaEntity
import com.garrietta.haven.core.model.ServerConfig
import com.garrietta.haven.core.net.ConnState
import com.garrietta.haven.core.net.HaConnection
import com.garrietta.haven.core.net.HaError
import com.garrietta.haven.core.protocol.CompressedStates
import com.garrietta.haven.core.protocol.Registries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * An optimistic edit: shown immediately, kept until the server confirms it
 * ([confirms] returns true for an incoming entity) or [expiresAtMs] passes.
 * Incoming updates that do NOT confirm it (stale echoes, transitions) are
 * held back while it lives, so the UI never flickers back to the old value.
 */
data class Optimistic(val entity: HaEntity, val expiresAtMs: Long, val confirms: (HaEntity) -> Boolean)

/** Pure overlay rule, unit tested: server entity + live optimistic edits → what the UI shows. */
object OptimisticOverlay {
    fun merge(server: Map<String, HaEntity>, edits: Map<String, Optimistic>, nowMs: Long): Map<String, HaEntity> {
        if (edits.isEmpty()) return server
        val out = HashMap(server)
        for ((id, edit) in edits) {
            if (edit.expiresAtMs <= nowMs) continue
            val s = server[id]
            if (s != null && edit.confirms(s)) continue
            out[id] = edit.entity
        }
        return out
    }
}

/**
 * Everything the UI knows about the home. One source of truth:
 *  - [entities]: live states (subscribe_entities) with optimistic edits applied
 *  - registries: areas / floors / devices / entity entries, re-fetched on connect
 *    and whenever HA reports a registry change
 *  - [call]: service calls, with errors surfaced on [errors]
 */
class HomeStore(
    private val scope: CoroutineScope,
    private val conn: HaConnection,
    private val cache: StateCache,
) {
    private val server = MutableStateFlow<Map<String, HaEntity>>(cache.load())
    private val edits = MutableStateFlow<Map<String, Optimistic>>(emptyMap())

    val entities: StateFlow<Map<String, HaEntity>> =
        combine(server, edits) { s, e -> OptimisticOverlay.merge(s, e, System.currentTimeMillis()) }
            .stateIn(scope, SharingStarted.Eagerly, server.value)

    private val _areas = MutableStateFlow<List<Area>>(emptyList())
    val areas: StateFlow<List<Area>> = _areas.asStateFlow()
    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors.asStateFlow()
    private val _devices = MutableStateFlow<Map<String, Device>>(emptyMap())
    val devices: StateFlow<Map<String, Device>> = _devices.asStateFlow()
    private val _entries = MutableStateFlow<Map<String, EntityEntry>>(emptyMap())
    val entries: StateFlow<Map<String, EntityEntry>> = _entries.asStateFlow()
    private val _config = MutableStateFlow(ServerConfig())
    val config: StateFlow<ServerConfig> = _config.asStateFlow()

    /** True once the first full state snapshot of this session arrived. */
    private val _loaded = MutableStateFlow(server.value.isNotEmpty())
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val connection: StateFlow<ConnState> get() = conn.state

    private val entityFlows = ConcurrentHashMap<String, StateFlow<HaEntity?>>()

    /** A per-entity flow so a tile recomposes only when ITS entity changes. */
    fun entity(id: String): StateFlow<HaEntity?> = entityFlows.getOrPut(id) {
        entities.map { it[id] }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(5_000), entities.value[id])
    }

    /** Area of an entity: its own registry area, else its device's. */
    fun areaOf(entityId: String, entries: Map<String, EntityEntry> = _entries.value, devices: Map<String, Device> = _devices.value): String? {
        val e = entries[entityId] ?: return null
        return e.areaId ?: e.deviceId?.let { devices[it]?.areaId }
    }

    /** Display name: registry name, else friendly_name minus a leading device/area name. */
    fun nameOf(entity: HaEntity): String = entity.friendlyName

    private var registryRefresh: Job? = null
    private var sweeper: Job? = null

    init {
        conn.onConnected = { scope.launch { onSessionStart() } }
        sweeper = scope.launch {
            // Expire optimistic edits so the server value takes over if no confirmation came.
            while (true) {
                delay(1_000)
                val now = System.currentTimeMillis()
                if (edits.value.values.any { it.expiresAtMs <= now }) edits.update { m -> m.filterValues { it.expiresAtMs > now } }
            }
        }
        scope.launch(Dispatchers.IO) {
            // Persist a snapshot for instant cold start (debounced to 15s).
            var last = 0L
            server.collect { states ->
                val now = System.currentTimeMillis()
                if (states.isNotEmpty() && now - last > 15_000) {
                    last = now
                    cache.save(states)
                }
            }
        }
    }

    private suspend fun onSessionStart() {
        try {
            var first = true
            conn.subscribe("subscribe_entities") { event ->
                server.update { CompressedStates.apply(if (first && event.has("a")) emptyMap() else it, event) }
                if (first && event.has("a")) {
                    first = false
                    _loaded.value = true
                    pruneConfirmedEdits()
                } else {
                    pruneConfirmedEdits()
                }
            }
        } catch (e: Exception) {
            Log.w("Haven", "subscribe_entities failed: ${e.message}")
        }
        refreshRegistries()
        listOf("area_registry_updated", "device_registry_updated", "entity_registry_updated", "floor_registry_updated").forEach { ev ->
            try {
                conn.subscribe("subscribe_events", { put("event_type", ev) }) { scheduleRegistryRefresh() }
            } catch (_: Exception) {
                // Non-admin users cannot subscribe to registry events; a reconnect still refreshes.
            }
        }
    }

    private fun pruneConfirmedEdits() {
        val s = server.value
        if (edits.value.isEmpty()) return
        edits.update { m -> m.filter { (id, edit) -> s[id]?.let { !edit.confirms(it) } ?: true } }
    }

    private fun scheduleRegistryRefresh() {
        registryRefresh?.cancel()
        registryRefresh = scope.launch {
            delay(1_500)
            refreshRegistries()
        }
    }

    suspend fun refreshRegistries() {
        runCatching { _config.value = Registries.config(conn.call("get_config") as? JSONObject) }
        runCatching { _areas.value = Registries.areas(conn.call("config/area_registry/list") as? JSONArray) }
        runCatching { _floors.value = Registries.floors(conn.call("config/floor_registry/list") as? JSONArray) }
        runCatching { _devices.value = Registries.devices(conn.call("config/device_registry/list") as? JSONArray).associateBy { it.id } }
        runCatching {
            _entries.value = Registries.entitiesForDisplay(conn.call("config/entity_registry/list_for_display") as? JSONObject)
                .associateBy { it.entityId }
        }
    }

    // ---- Actions -----------------------------------------------------------

    /** Calls a service. Returns true on success; failures are reported on [errors]. */
    suspend fun call(domain: String, service: String, entityId: String? = null, data: JSONObject? = null): Boolean {
        return try {
            conn.call("call_service") {
                put("domain", domain)
                put("service", service)
                if (entityId != null) put("target", JSONObject().put("entity_id", entityId))
                if (data != null) put("service_data", data)
            }
            true
        } catch (e: HaError) {
            val msg = if (e.code == HaConnection.NOT_CONNECTED) "Not connected — action not sent" else "$domain.$service: ${e.message}"
            _errors.tryEmit(msg)
            false
        }
    }

    fun fire(domain: String, service: String, entityId: String? = null, data: JSONObject? = null) {
        scope.launch { call(domain, service, entityId, data) }
    }

    /**
     * Optimistic action: shows [optimistic] at once, runs the service call,
     * and rolls back if it fails. [confirms] decides which server update
     * counts as confirmation (default: same state).
     */
    fun actuate(
        id: String,
        optimistic: (HaEntity) -> HaEntity,
        confirms: ((HaEntity, HaEntity) -> Boolean)? = null,
        holdMs: Long = 6_000,
        call: suspend () -> Boolean,
    ) {
        val current = entities.value[id] ?: run { scope.launch { call() }; return }
        val target = optimistic(current)
        val rule: (HaEntity) -> Boolean = { incoming -> confirms?.invoke(target, incoming) ?: (incoming.state == target.state) }
        edits.update { it + (id to Optimistic(target, System.currentTimeMillis() + holdMs, rule)) }
        scope.launch {
            if (!call()) edits.update { it - id }
        }
    }

    // ---- Convenience actions used across the UI -----------------------------

    fun toggle(e: HaEntity) {
        val on = isActive(e)
        when (e.domain) {
            "lock" -> setLocked(e, !on)
            "cover" -> setCoverOpen(e, !on)
            "scene" -> fire("scene", "turn_on", e.entityId)
            "script" -> fire("script", "turn_on", e.entityId)
            "button", "input_button" -> fire(e.domain, "press", e.entityId)
            "automation" -> actuate(e.entityId, { it.copy(state = if (on) "off" else "on") }) {
                call("automation", if (on) "turn_off" else "turn_on", e.entityId)
            }
            else -> actuate(e.entityId, { it.copy(state = if (on) "off" else "on") }) {
                call(if (e.domain in TOGGLE_DOMAINS) e.domain else "homeassistant", if (on) "turn_off" else "turn_on", e.entityId)
            }
        }
    }

    fun setLocked(e: HaEntity, locked: Boolean) =
        actuate(e.entityId, { it.copy(state = if (locked) "locked" else "unlocked") }) {
            call("lock", if (locked) "lock" else "unlock", e.entityId)
        }

    fun setCoverOpen(e: HaEntity, open: Boolean) =
        actuate(e.entityId, { it.copy(state = if (open) "open" else "closed") },
            confirms = { t, s -> s.state == t.state || s.state == (if (open) "opening" else "closing") }, holdMs = 20_000) {
            call("cover", if (open) "open_cover" else "close_cover", e.entityId)
        }

    fun stopCover(e: HaEntity) = fire("cover", "stop_cover", e.entityId)

    fun setCoverPosition(e: HaEntity, pos: Int) =
        actuate(e.entityId, { it.copy(attributes = it.attributes + ("current_position" to pos)) },
            confirms = { _, s -> s.attrInt("current_position") == pos }) {
            call("cover", "set_cover_position", e.entityId, JSONObject().put("position", pos))
        }

    fun setBrightness(e: HaEntity, pct: Int) {
        if (pct <= 0) { actuate(e.entityId, { it.copy(state = "off") }) { call("light", "turn_off", e.entityId) }; return }
        val raw = (pct * 255 + 50) / 100
        actuate(e.entityId, { it.copy(state = "on", attributes = it.attributes + ("brightness" to raw)) },
            confirms = { _, s -> s.state == "on" && kotlin.math.abs((s.attrInt("brightness") ?: -99) - raw) <= 3 }) {
            call("light", "turn_on", e.entityId, JSONObject().put("brightness_pct", pct))
        }
    }

    fun setColorTemp(e: HaEntity, kelvin: Int) =
        actuate(e.entityId, { it.copy(state = "on", attributes = it.attributes + ("color_temp_kelvin" to kelvin)) },
            confirms = { _, s -> kotlin.math.abs((s.attrInt("color_temp_kelvin") ?: 0) - kelvin) < 60 }) {
            call("light", "turn_on", e.entityId, JSONObject().put("color_temp_kelvin", kelvin))
        }

    fun setHs(e: HaEntity, hue: Float, sat: Float) =
        actuate(e.entityId, { it.copy(state = "on", attributes = it.attributes + ("hs_color" to listOf(hue.toDouble(), sat.toDouble()))) },
            confirms = { _, s -> s.state == "on" }, holdMs = 3_000) {
            call("light", "turn_on", e.entityId, JSONObject().put("hs_color", JSONArray().put(hue).put(sat)))
        }

    fun setTemperature(e: HaEntity, value: Double) =
        actuate(e.entityId, { it.copy(attributes = it.attributes + ("temperature" to value)) },
            confirms = { _, s -> s.attrDouble("temperature") == value }) {
            call("climate", "set_temperature", e.entityId, JSONObject().put("temperature", value))
        }

    fun setTemperatureRange(e: HaEntity, low: Double, high: Double) =
        actuate(e.entityId, { it.copy(attributes = it.attributes + mapOf("target_temp_low" to low, "target_temp_high" to high)) },
            confirms = { _, s -> s.attrDouble("target_temp_low") == low && s.attrDouble("target_temp_high") == high }) {
            call("climate", "set_temperature", e.entityId, JSONObject().put("target_temp_low", low).put("target_temp_high", high))
        }

    fun setHvacMode(e: HaEntity, mode: String) =
        actuate(e.entityId, { it.copy(state = mode) }) {
            call("climate", "set_hvac_mode", e.entityId, JSONObject().put("hvac_mode", mode))
        }

    fun setClimateAttr(e: HaEntity, service: String, key: String, value: String) =
        actuate(e.entityId, { it.copy(attributes = it.attributes + (key to value)) }, confirms = { _, s -> s.attrString(key) == value }) {
            call("climate", service, e.entityId, JSONObject().put(key, value))
        }

    fun setFanPercentage(e: HaEntity, pct: Int) =
        actuate(e.entityId, { it.copy(state = if (pct > 0) "on" else "off", attributes = it.attributes + ("percentage" to pct)) },
            confirms = { _, s -> s.attrInt("percentage") == pct }) {
            call("fan", "set_percentage", e.entityId, JSONObject().put("percentage", pct))
        }

    fun setVolume(e: HaEntity, level: Float) =
        actuate(e.entityId, { it.copy(attributes = it.attributes + ("volume_level" to level.toDouble())) },
            confirms = { _, s -> kotlin.math.abs((s.attrDouble("volume_level") ?: -1.0) - level) < 0.02 }) {
            call("media_player", "volume_set", e.entityId, JSONObject().put("volume_level", level.toDouble()))
        }

    fun media(e: HaEntity, service: String) = fire("media_player", service, e.entityId)

    fun vacuum(e: HaEntity, service: String) = fire("vacuum", service, e.entityId)

    fun setNumber(e: HaEntity, value: Double) =
        actuate(e.entityId, { it.copy(state = value.toString()) }, confirms = { _, s -> s.state.toDoubleOrNull() == value }) {
            call(e.domain, "set_value", e.entityId, JSONObject().put("value", value))
        }

    fun selectOption(e: HaEntity, option: String) =
        actuate(e.entityId, { it.copy(state = option) }) {
            call(e.domain, "select_option", e.entityId, JSONObject().put("option", option))
        }

    fun alarm(e: HaEntity, service: String, code: String?) =
        fire("alarm_control_panel", service, e.entityId, code?.takeIf { it.isNotEmpty() }?.let { JSONObject().put("code", it) })

    /** HA's own LAN / remote URLs (admin only); null fields when unknown. */
    suspend fun instanceUrls(): Pair<String?, String?> {
        return try {
            val r = conn.call("network/url") as? JSONObject
            val internal = r?.optString("internal")?.ifEmpty { null }?.takeIf { it != "null" }
            val external = r?.optString("external")?.ifEmpty { null }?.takeIf { it != "null" }
                ?: r?.optString("cloud")?.ifEmpty { null }?.takeIf { it != "null" }
            internal to external
        } catch (_: Exception) {
            null to null
        }
    }

    fun clearOnSignOut() {
        server.value = emptyMap()
        edits.value = emptyMap()
        _loaded.value = false
        cache.clear()
    }

    companion object {
        val TOGGLE_DOMAINS = setOf("light", "switch", "fan", "input_boolean", "media_player", "humidifier", "siren", "climate", "water_heater", "remote")

        /** "On-ish" — what a tile treats as active. Pure, unit tested. */
        fun isActive(e: HaEntity): Boolean = when (e.domain) {
            "lock" -> e.state == "unlocked" || e.state == "open" || e.state == "unlocking"
            "cover" -> e.state == "open" || e.state == "opening"
            "media_player" -> e.state == "playing" || e.state == "on" || e.state == "paused" || e.state == "buffering"
            "climate" -> e.state != "off" && !e.isUnavailable
            "vacuum" -> e.state == "cleaning" || e.state == "returning"
            "alarm_control_panel" -> e.state.startsWith("armed") || e.state == "triggered"
            "person", "device_tracker" -> e.state == "home"
            "binary_sensor", "light", "switch", "fan", "input_boolean", "automation", "siren", "humidifier", "remote" -> e.state == "on"
            else -> e.state == "on"
        }
    }
}
