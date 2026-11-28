package com.garrietta.haven.data

/**
 * Feedback round 19, item 6: real-time Home Assistant event
 * notification rules, pure so they are unit tested on the JVM.
 * The manager ([HavenNotificationManager]) feeds these functions
 * one parsed `state_changed` event at a time; nothing here touches
 * Android types, so the trigger logic is testable without a device.
 *
 * A [StateChangeEvent] carries exactly what the predicates need:
 * the entity, the old and new states (null old = entity just
 * appeared, never a transition), the motion/door device class,
 * the friendly name for the notification text, and the context
 * parent id (non-null = an automation or script caused it).
 */
data class StateChangeEvent(
    val entityId: String,
    val domain: String,
    val oldState: String?,
    val newState: String?,
    val deviceClass: String?,
    val friendlyName: String?,
    val parentId: String?,
) {
    /** Display name for notification text; the entity id is the fallback, never blank. */
    val displayName: String
        get() = friendlyName?.takeIf { it.isNotBlank() } ?: entityId
}

enum class NotificationKind {
    GARAGE_DOOR,
    MOTION,
    LIGHT_SHUTOFF,
}

/**
 * Garage & doors: a cover transitioning TO "open" or "opening"
 * (closing/closed never notify), or a door binary_sensor
 * transitioning off -> on (binary_sensor "on" IS open for doors).
 * Any other domain, or a transition that does not land on the
 * open state, is not a garage/door event.
 */
fun shouldNotifyGarageDoor(event: StateChangeEvent): Boolean {
    val new = event.newState ?: return false
    if (event.oldState == new) return false
    return when (event.domain) {
        "cover" -> new == "open" || new == "opening"
        "binary_sensor" -> event.deviceClass == "door" && event.oldState == "off" && new == "on"
        else -> false
    }
}

/**
 * Camera & motion: a motion binary_sensor transitioning off -> on.
 * on -> on (a re-fire while already on) and on -> off (clearing)
 * never notify; only the off -> on edge is a new detection.
 */
fun shouldNotifyMotion(event: StateChangeEvent): Boolean =
    event.domain == "binary_sensor" &&
        event.deviceClass == "motion" &&
        event.oldState == "off" &&
        event.newState == "on"

/**
 * Automated light shutoffs: a light transitioning on -> off that
 * was NOT the user's own Haven tap. [locallyActuated] is true when
 * Haven itself actuated this entity within the suppression window
 * (see HavenViewModel's actuation tracking) — those confirming
 * events are the user's own action, never news. A non-null parent
 * id means an automation or script caused it even when the timing
 * overlaps a local tap, so it always notifies.
 */
fun shouldNotifyLightShutoff(event: StateChangeEvent, locallyActuated: Boolean): Boolean {
    if (event.domain != "light") return false
    if (event.oldState != "on" || event.newState != "off") return false
    if (event.parentId != null) return true
    return !locallyActuated
}

/**
 * The single trigger decision for one event: garage/door first
 * (a cover can never also be motion or a light, so the order only
 * matters for documentation), then motion, then light shutoff.
 * Null means no notification for this event.
 */
fun resolveNotification(event: StateChangeEvent, locallyActuated: Boolean): NotificationKind? = when {
    shouldNotifyGarageDoor(event) -> NotificationKind.GARAGE_DOOR
    shouldNotifyMotion(event) -> NotificationKind.MOTION
    shouldNotifyLightShutoff(event, locallyActuated) -> NotificationKind.LIGHT_SHUTOFF
    else -> null
}
