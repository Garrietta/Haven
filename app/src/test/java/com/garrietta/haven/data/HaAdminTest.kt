package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Home Assistant replies Settings reads, in the shapes Home Assistant sends them. */
class HaAdminTest {

    private fun reply(json: String) = JSONObject(json)

    @Test
    fun configEntriesReadWithTheirStateAndNeedForAttention() {
        val entries = parseConfigEntries(resultList(reply("""
            {"success": true, "result": [
              {"entry_id": "a", "domain": "hue", "title": "Hue Bridge", "state": "loaded", "disabled_by": null, "source": "zeroconf"},
              {"entry_id": "b", "domain": "ring", "title": "Ring", "state": "setup_retry", "disabled_by": null},
              {"entry_id": "c", "domain": "cast", "title": "Cast", "state": "not_loaded", "disabled_by": "user"}
            ]}
        """)))
        assertEquals(listOf("cast", "hue", "ring"), entries.map { it.domain })
        assertEquals("Disabled", configEntryStatus(entries[0]))
        assertEquals("Loaded", configEntryStatus(entries[1]))
        assertTrue(configEntryNeedsAttention(entries[2]))
        assertFalse(configEntryNeedsAttention(entries[0]))
    }

    @Test
    fun devicesPreferTheNameTheUserGaveAndDropTheStringNull() {
        val devices = parseDevices(resultList(reply("""
            {"success": true, "result": [
              {"id": "d1", "name": "Hue color lamp 2", "name_by_user": "Floor Lamp", "manufacturer": "Signify", "model": "LCA001", "area_id": "living_room", "config_entries": ["a"]},
              {"id": "d2", "name": "Tapo C110", "name_by_user": "null", "manufacturer": "TP-Link", "area_id": null, "disabled_by": "user"}
            ]}
        """)))
        assertEquals(listOf("Floor Lamp", "Tapo C110"), devices.map { it.displayName })
        assertNull(devices[1].nameByUser)
        assertNull(devices[1].areaId)
        assertEquals("user", devices[1].disabledBy)
        assertEquals(listOf("a"), devices[0].configEntries)
    }

    @Test
    fun entityRegistryKeepsWhatHomeAssistantLetsTheUIChange() {
        val entities = parseEntityRegistry(resultList(reply("""
            {"success": true, "result": [
              {"entity_id": "light.floor", "name": null, "original_name": "Floor", "icon": "mdi:lamp", "device_id": "d1", "hidden_by": "user", "platform": "hue"}
            ]}
        """)))
        val e = entities.single()
        assertNull(e.name)
        assertEquals("Floor", e.originalName)
        assertEquals("mdi:lamp", e.icon)
        assertEquals("user", e.hiddenBy)
        assertEquals("light", e.domain)
    }

    @Test
    fun aFailedReplyIsAnErrorNotAnEmptyList() {
        val failed = reply("""{"success": false, "error": {"code": "unauthorized", "message": "Unauthorized"}}""")
        assertTrue(resultList(failed).isEmpty())
        assertEquals("Unauthorized", commandError(failed))
        assertEquals("Home Assistant did not answer.", commandError(null))
        assertNull(commandError(reply("""{"success": true, "result": null}""")))
    }

    @Test
    fun repairsPutWhatMattersFirst() {
        val issues = parseRepairs(resultList(reply("""
            {"success": true, "result": {"issues": [
              {"domain": "hue", "issue_id": "x", "severity": "warning", "translation_key": "deprecated_yaml", "ignored": false},
              {"domain": "ring", "issue_id": "y", "severity": "error", "translation_key": "reauth", "ignored": false},
              {"domain": "zha", "issue_id": "z", "severity": "critical", "ignored": true}
            ]}}
        """), "issues"))
        assertEquals(listOf("y", "x", "z"), issues.map { it.issueId })
        assertEquals("Ring · Reauth", issues[0].title)
    }

    @Test
    fun logsAreNewestFirstWithTheirSource() {
        val logs = parseLogs(resultList(reply("""
            {"success": true, "result": [
              {"name": "homeassistant.components.ring", "message": ["Timeout"], "level": "ERROR", "source": ["components/ring/__init__.py", 42], "timestamp": 100.0, "count": 3},
              {"name": "homeassistant.core", "message": ["Slow"], "level": "WARNING", "source": ["core.py", 7], "timestamp": 200.0, "count": 1}
            ]}
        """)))
        assertEquals(listOf("Slow", "Timeout"), logs.map { it.message })
        assertEquals("__init__.py:42", logs[1].source)
        assertEquals(3, logs[1].count)
    }

    @Test
    fun peopleFromTheUIAndFromYamlAreBothListed() {
        val people = parsePeople(reply("""
            {"success": true, "result": {
              "storage": [{"id": "alex", "name": "Alex", "user_id": "u1", "device_trackers": ["device_tracker.alex_phone"]}],
              "config": [{"id": "sam", "name": "Sam", "device_trackers": []}]
            }}
        """))
        assertEquals(listOf("Alex", "Sam"), people.map { it.name })
        assertFalse(people[0].fromYaml)
        assertTrue(people[1].fromYaml)
        assertEquals(listOf("device_tracker.alex_phone"), people[0].deviceTrackers)
    }

    @Test
    fun systemUsersAreLeftOut() {
        val users = parseUsers(resultList(reply("""
            {"success": true, "result": [
              {"id": "s", "name": "Supervisor", "system_generated": true},
              {"id": "o", "name": "Owner", "is_owner": true, "is_active": true, "group_ids": ["system-admin"]},
              {"id": "k", "name": "Kid", "is_active": true, "group_ids": ["system-users"]}
            ]}
        """)))
        assertEquals(listOf("Owner", "Kid"), users.map { it.name })
        assertTrue(users[0].isAdmin)
        assertFalse(users[1].isAdmin)
    }

    @Test
    fun backupsReadOnNewAndOldVersions() {
        val newer = parseBackups(reply("""
            {"success": true, "result": {"backups": [
              {"backup_id": "b1", "name": "Automatic", "date": "2026-11-12T03:00:00+00:00", "agents": {"backup.local": {"size": 1073741824}}}
            ]}}
        """))
        assertEquals("b1", newer.single().id)
        assertEquals("1.0 GB", formatBytes(newer.single().sizeBytes!!))
        val older = parseBackups(reply("""
            {"success": true, "result": {"backups": [{"slug": "abc", "name": "Full", "date": "2025-01-01T00:00:00", "size": 340.0}]}}
        """))
        assertEquals("340 MB", formatBytes(older.single().sizeBytes!!))
    }

    @Test
    fun coreConfigReadsItsUnits() {
        val config = parseCoreConfig(reply("""
            {"success": true, "result": {"location_name": "Bloomington", "version": "2026.11.1", "time_zone": "America/Indiana/Indianapolis",
              "unit_system": {"temperature": "°F", "length": "mi"}, "currency": "USD", "latitude": 39.1, "longitude": -86.5,
              "elevation": 235, "state": "RUNNING", "components": ["hue", "ring", "zone"]}}
        """))!!
        assertEquals("Bloomington", config.locationName)
        assertEquals("US customary", config.unitSystem)
        assertEquals(3, config.components)
        assertEquals(235, config.elevation)
    }

    @Test
    fun configFlowFormsMenusAndEnds() {
        val form = parseFlowStep(JSONObject("""
            {"type": "form", "flow_id": "f", "step_id": "user", "errors": {"base": "cannot_connect"}, "data_schema": [
              {"name": "host", "required": true, "type": "string"},
              {"name": "password", "required": false, "type": "string"},
              {"name": "port", "type": "integer", "default": 80},
              {"name": "mode", "selector": {"select": {"options": [{"value": "a", "label": "Auto"}, "manual"]}}},
              {"name": "where", "selector": {"location": {}}}
            ]}
        """)) as FlowStep.Form
        assertEquals(listOf(FieldKind.TEXT, FieldKind.PASSWORD, FieldKind.NUMBER, FieldKind.SELECT, FieldKind.UNSUPPORTED), form.fields.map { it.kind })
        assertEquals(80, form.fields[2].default)
        assertEquals(listOf("a" to "Auto", "manual" to "manual"), form.fields[3].options)
        assertEquals("cannot_connect", form.errors["base"])
        assertEquals("Host", form.fields[0].label)

        val menu = parseFlowStep(JSONObject("""{"type": "menu", "flow_id": "f", "step_id": "pick", "menu_options": ["cloud", "local"]}""")) as FlowStep.Menu
        assertEquals(listOf("cloud", "local"), menu.options.map { it.first })
        assertTrue(parseFlowStep(JSONObject("""{"type": "create_entry", "flow_id": "f", "title": "Hue"}""")) is FlowStep.Done)
        assertTrue(parseFlowStep(JSONObject("""{"type": "abort", "flow_id": "f", "reason": "already_configured"}""")) is FlowStep.Aborted)
        assertNull(parseFlowStep(JSONObject("""{"type": "progress", "flow_id": "f"}""")))
    }
}
