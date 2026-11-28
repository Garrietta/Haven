package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 3 logic tests (2026-11-02): pure functions only, no Compose. */
class Round3LogicTest {

    private fun entity(id: String, state: String, attrs: Map<String, Any?> = emptyMap()): HaEntity {
        val o = JSONObject()
        o.put("entity_id", id)
        o.put("state", state)
        val ao = JSONObject()
        for ((k, v) in attrs) ao.put(k, v)
        o.put("attributes", ao)
        return HaEntity.fromJson(o)
    }

    @Test fun presenceGroupsGarrettHaleyAndOther() {
        val trackers = listOf(
            entity("device_tracker.garrett_s_z_fold8", "home", mapOf("friendly_name" to "Garrett's Z Fold8")),
            entity("device_tracker.z_fold_8", "home", mapOf("friendly_name" to "Z Fold 8")),
            entity("device_tracker.haleys_iphone", "home", mapOf("friendly_name" to "Haley's iPhone")),
            entity("device_tracker.ipad_46864", "home", mapOf("friendly_name" to "iPad (46864)")),
            entity("device_tracker.vulpo_d2f23f", "unknown", mapOf("friendly_name" to "Vulpo d2f23f")),
        )
        val home = trackers.filter { it.state == "home" }
        val groups = groupPresence(home)
        assertEquals(3, groups.size)
        val garrett = groups.first { it.label == "Garrett" }
        assertEquals(2, garrett.members.size)
        assertFalse(garrett.isOther)
        assertEquals(1, groups.first { it.label == "Haley" }.members.size)
        val other = groups.first { it.label == "Other" }
        assertTrue(other.isOther)
        assertEquals(listOf("device_tracker.ipad_46864"), other.members.map { it.entityId })
        // Vulpo is not home, so it is not a chip — but it must still
        // be findable in the full state map, never silently dropped.
        assertTrue(trackers.any { it.entityId == "device_tracker.vulpo_d2f23f" })
    }

    @Test fun batteryAndLocationLookup() {
        val tracker = entity("device_tracker.garrett_s_z_fold8", "home", mapOf("friendly_name" to "Garrett's Z Fold8"))
        val states = mapOf(
            tracker.entityId to tracker,
            "sensor.garrett_s_z_fold8_garrett_s_z_fold8_battery_level" to
                entity("sensor.garrett_s_z_fold8_garrett_s_z_fold8_battery_level", "35"),
            "sensor.garrett_s_z_fold8_garrett_s_z_fold8_geocoded_location" to
                entity("sensor.garrett_s_z_fold8_garrett_s_z_fold8_geocoded_location", "2710 W Watson Dr, Bloomington, IN 47403, USA"),
            "sensor.haleys_iphone_geocoded_location" to
                entity("sensor.haleys_iphone_geocoded_location", "unavailable"),
        )
        assertEquals(35, batteryForTracker(tracker, states))
        assertEquals(
            "2710 W Watson Dr, Bloomington, IN 47403, USA",
            locationForTracker(tracker, states),
        )
        val haley = entity("device_tracker.haleys_iphone", "home", mapOf("friendly_name" to "Haley's iPhone"))
        assertNull(locationForTracker(haley, states))
        assertNull(batteryForTracker(haley, states))
    }

    @Test fun consumableFractionsMatchExportValues() {
        // Export values against the eufy integration lifetimes.
        assertEquals(334.0 / 360.0, consumableFraction(334.0, 360.0)!!, 0.001)
        assertEquals(154.0 / 180.0, consumableFraction(154.0, 180.0)!!, 0.001)
        assertEquals(34.0 / 60.0, consumableFraction(34.0, 60.0)!!, 0.001)
        assertEquals(4.0 / 30.0, consumableFraction(4.0, 30.0)!!, 0.001)
        assertEquals(1.0, consumableFraction(999.0, 360.0)!!, 0.0)
        assertEquals(0.0, consumableFraction(-5.0, 360.0)!!, 0.0)
        assertNull(consumableFraction(null, 360.0))
    }

    @Test fun vacuumTileLine() {
        assertEquals("Docked · 100%", vacuumTileLine("docked", "100"))
        assertEquals("Cleaning", vacuumTileLine("cleaning", null))
    }

    @Test fun dndRangeStripsSeconds() {
        assertEquals("22:00–08:00", dndRange("22:00:00", "08:00:00"))
        assertNull(dndRange(null, "08:00:00"))
        assertEquals("22:00", shortTime("22:00:00"))
        assertEquals("off", shortTime("off"))
    }

    @Test fun timerFractionFromAttributes() {
        val timer = entity(
            "timer.greenhouse_mist_pulse", "active",
            mapOf("remaining" to "0:15:00", "duration" to "0:30:00"),
        )
        assertEquals(0.5, timerFraction(timer)!!, 0.001)
        val idle = entity("timer.greenhouse_fan_purge", "idle")
        assertNull(timerFraction(idle))
        assertEquals(90.0, parseHaDuration("1:30")!!, 0.0)
        assertEquals(3661.0, parseHaDuration("1:01:01")!!, 0.0)
        assertNull(parseHaDuration("not a duration"))
    }

    @Test fun updatesSummaryCountsAvailableFirst() {
        val states = mapOf(
            "update.browser_mod_update" to entity("update.browser_mod_update", "on", mapOf("friendly_name" to "browser_mod Update")),
            "update.hacs_update" to entity("update.hacs_update", "off", mapOf("friendly_name" to "HACS Update")),
            "update.elkbledom_update" to entity("update.elkbledom_update", "on", mapOf("friendly_name" to "elkbledom Update")),
        )
        val summary = updatesSummary(states)
        assertEquals(3, summary.total)
        assertEquals(2, summary.available)
        assertTrue(summary.updates.take(2).all { it.state == "on" })
        assertEquals("browser_mod", updateDisplayName(summary.updates.first { it.entityId == "update.browser_mod_update" }))
    }

    @Test fun securityChecklistLabelsByDeviceClass() {
        val sensors = listOf(
            entity("binary_sensor.front_door_69a1", "off", mapOf("friendly_name" to "Front Door 69A1", "device_class" to "door")),
            entity("binary_sensor.front_door_motion", "on", mapOf("friendly_name" to "Front Door Motion", "device_class" to "motion")),
            entity("binary_sensor.remote_ui", "unavailable", mapOf("friendly_name" to "Remote UI")),
        )
        val rows = securityChecklist(sensors)
        assertEquals(2, rows.size)
        assertEquals("Closed", rows.first { it.entityId == "binary_sensor.front_door_69a1" }.statusText)
        val motion = rows.first { it.entityId == "binary_sensor.front_door_motion" }
        assertTrue(motion.active)
        assertEquals("Motion detected", motion.statusText)
    }

    @Test fun alarmModeLabelsAndServices() {
        assertEquals("alarm_arm_home", alarmArmService("home"))
        assertEquals("alarm_arm_night", alarmArmService("night"))
        assertEquals("alarm_disarm", alarmArmService("disarmed"))
        assertEquals("Home", alarmModeLabel("home"))
        assertEquals("Disarmed", alarmModeLabel("disarmed"))
    }

    @Test fun lastTriggeredFormatsWithoutRawIso() {
        val auto = entity(
            "automation.greenhouse_brain", "on",
            mapOf("last_triggered" to "2026-11-01T06:30:00+00:00"),
        )
        val line = lastTriggeredLine(auto)
        assertTrue(line != null)
        assertTrue(line!!.startsWith("Last triggered "))
        assertFalse(line.contains("T06:30"))
        assertNull(lastTriggeredLine(entity("automation.greenhouse_brain", "on")))
        assertNull(lastTriggeredLine(entity("automation.greenhouse_brain", "on", mapOf("last_triggered" to "unknown"))))
    }

    @Test fun countdownFormatting() {
        assertEquals("1 h 1 min", formatCountdown(3661.0))
        assertEquals("15 min", formatCountdown(900.0))
        assertEquals("45 s", formatCountdown(45.0))
    }

    @Test fun prettifyIdName() {
        // Export reality: the timer's friendly name is the bare object id.
        val timer = entity("timer.greenhouse_fan_purge", "idle", mapOf("friendly_name" to "greenhouse_fan_purge"))
        assertEquals("Greenhouse fan purge", prettifyIdName(timer))
        val named = entity("timer.greenhouse_fan_purge", "idle", mapOf("friendly_name" to "Fan Purge Timer"))
        assertEquals("Fan Purge Timer", prettifyIdName(named))
    }

    @Test fun unmappedTrackersIncludeAwayDevices() {
        val states = mapOf(
            "device_tracker.garrett_s_z_fold8" to entity("device_tracker.garrett_s_z_fold8", "home", mapOf("friendly_name" to "Garrett's Z Fold8")),
            "device_tracker.vulpo_d2f23f" to entity("device_tracker.vulpo_d2f23f", "unknown", mapOf("friendly_name" to "Vulpo d2f23f")),
            "device_tracker.ipad_46864" to entity("device_tracker.ipad_46864", "home", mapOf("friendly_name" to "iPad (46864)")),
        )
        val groups = groupPresence(states.values.filter { it.state == "home" })
        val unmapped = unmappedTrackers(states, groups)
        assertEquals(
            listOf("device_tracker.ipad_46864", "device_tracker.vulpo_d2f23f"),
            unmapped.map { it.entityId },
        )
    }

    // Round 4: humanized enum/option labels for Security + vacuum
    // panels — underscores to spaces, title case, acronyms kept.
    @Test fun humanizeOptionLabel_boostIq() {
        assertEquals("Boost IQ", humanizeOptionLabel("Boost_IQ"))
        assertEquals("Boost IQ", humanizeOptionLabel("boost_iq"))
    }

    @Test fun humanizeOptionLabel_underscoresAndCamel() {
        assertEquals("Auto Empty", humanizeOptionLabel("auto_empty"))
        assertEquals("Auto Empty", humanizeOptionLabel("autoEmpty"))
        // LED is a kept acronym, not title-cased to "Led".
        assertEquals("LED Strip", humanizeOptionLabel("led_strip"))
    }

    @Test fun humanizeOptionLabel_blankPassthrough() {
        assertEquals("", humanizeOptionLabel(""))
        assertEquals("   ", humanizeOptionLabel("   "))
    }
}
