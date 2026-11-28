package com.garrietta.haven.core.model

/**
 * One Home Assistant entity, as Haven sees it: the live state plus the
 * registry facts that matter for display. Immutable; every update makes a
 * new instance so Compose can compare by equality.
 */
data class HaEntity(
    val entityId: String,
    val state: String,
    val attributes: Map<String, Any?> = emptyMap(),
    /** Seconds since epoch (HA's compressed `lc`). */
    val lastChanged: Double = 0.0,
    /** Seconds since epoch (HA's compressed `lu`, defaults to lastChanged). */
    val lastUpdated: Double = lastChanged,
    val contextId: String? = null,
) {
    val domain: String get() = entityId.substringBefore('.')
    val objectId: String get() = entityId.substringAfter('.')
    val friendlyName: String get() = (attributes["friendly_name"] as? String)?.takeIf { it.isNotBlank() } ?: prettify(objectId)
    val isUnavailable: Boolean get() = state == "unavailable" || state == "unknown"
    val deviceClass: String? get() = attrString("device_class")
    val unit: String? get() = attrString("unit_of_measurement")
    val supportedFeatures: Int get() = attrInt("supported_features") ?: 0

    fun attrString(key: String): String? = attributes[key] as? String
    fun attrDouble(key: String): Double? = (attributes[key] as? Number)?.toDouble()
    fun attrInt(key: String): Int? = (attributes[key] as? Number)?.toInt()
    fun attrBool(key: String): Boolean? = attributes[key] as? Boolean

    @Suppress("UNCHECKED_CAST")
    fun attrList(key: String): List<Any?> = attributes[key] as? List<Any?> ?: emptyList()

    fun attrStrings(key: String): List<String> = attrList(key).mapNotNull { it as? String }

    fun supports(feature: Int): Boolean = supportedFeatures and feature != 0

    companion object {
        fun prettify(objectId: String): String =
            objectId.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

data class Area(
    val id: String,
    val name: String,
    val icon: String? = null,
    val floorId: String? = null,
    val picture: String? = null,
    val temperatureEntityId: String? = null,
    val humidityEntityId: String? = null,
)

data class Floor(val id: String, val name: String, val level: Int? = null)

data class Device(
    val id: String,
    val name: String,
    val areaId: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val isService: Boolean = false,
)

/** `config/entity_registry/list_for_display` entry. */
data class EntityEntry(
    val entityId: String,
    val name: String? = null,
    val areaId: String? = null,
    val deviceId: String? = null,
    val platform: String? = null,
    /** "config" / "diagnostic" / null. */
    val category: String? = null,
    val hidden: Boolean = false,
    val icon: String? = null,
)

data class ServerConfig(
    val locationName: String = "Home",
    val temperatureUnit: String = "°C",
    val version: String = "",
    val timeZone: String? = null,
)

data class HistoryPoint(val timeSec: Double, val value: Double)
