package com.garrietta.haven.data

/**
 * Camera motion, from real registry data only (expressive-menus
 * round, item 7). A camera's motion sensor is a binary_sensor on
 * the SAME device (entity-registry device link) whose device_class
 * is "motion", falling back to an entity id containing "motion"
 * on that same device. The device link is the association: a
 * same-name guess across devices is never used, and a camera
 * with no motion binary_sensor on its device (both Tapo cameras:
 * their motion controls are enable-switches, not motion events)
 * yields null — the card then shows no motion chip, never a
 * synthesized one. The returned sensor is state-agnostic; the
 * caller shows the chip only while its state is "on".
 *
 * This supersedes the hero comment that claimed no per-camera
 * motion state exists: the state exists for the Ring doorbell
 * (binary_sensor.front_door_motion on the doorbell's device),
 * and the registry link is what makes it attributable.
 *
 * Pure, unit-tested.
 */
fun motionSensorForCamera(
    cameraId: String,
    states: Map<String, HaEntity>,
    entityDevices: Map<String, String>,
): HaEntity? {
    val device = entityDevices[cameraId] ?: return null
    val onDevice = states.values.filter { entity ->
        entity.domain == "binary_sensor" && entityDevices[entity.entityId] == device
    }
    return onDevice.firstOrNull { it.attrString("device_class") == "motion" }
        ?: onDevice.firstOrNull { "motion" in it.entityId }
}
