package com.garrietta.haven.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Feedback round 15, item 5: domain-default resolution is
 * per-entity override > domain default > global default. The key
 * functions are pure, so the law is pinned here; the store maps
 * and the UI pickers feed them.
 */
class DomainDefaultsTest {

    private val prefs = ShapePrefs(
        globalBadge = "global-badge",
        globalCard = "global-card",
        badgeOverrides = mapOf("light.a" to "ent-badge"),
        cardOverrides = mapOf("light.a" to "ent-card"),
        domainBadge = mapOf("light" to "dom-badge"),
        domainCard = mapOf("light" to "dom-card"),
    )

    @Test
    fun perEntityWinsOverDomainWinsOverGlobal() {
        assertEquals("ent-badge", prefs.badgeShapeKeyFor("light.a", "light"))
        assertEquals("ent-card", prefs.cardShapeKeyFor("light.a", "light"))
        // No per-entity override: the domain default applies, even
        // for a socket-named lamp (domain, not name, is the key).
        assertEquals("dom-badge", prefs.badgeShapeKeyFor("light.lamp_socket_1", "light"))
        assertEquals("dom-card", prefs.cardShapeKeyFor("light.lamp_socket_1", "light"))
        // No domain default: the global default applies.
        assertEquals("global-badge", prefs.badgeShapeKeyFor("switch.x", "switch"))
        assertEquals("global-card", prefs.cardShapeKeyFor("switch.x", "switch"))
        // Unknown domain: the global default applies.
        assertEquals("global-badge", prefs.badgeShapeKeyFor("light.b", null))
        assertEquals("global-card", prefs.cardShapeKeyFor("light.b", null))
    }

    @Test
    fun idOnlyAccessorsKeepPerEntityOverGlobal() {
        // Morph landing fallbacks know no domain; they keep the
        // old behavior (the reported tile shape already carries
        // the domain default when one exists).
        assertEquals("ent-badge", prefs.badgeShapeKey("light.a"))
        assertEquals("global-badge", prefs.badgeShapeKey("light.b"))
        assertEquals("ent-card", prefs.cardShapeKey("light.a"))
        assertEquals("global-card", prefs.cardShapeKey("light.b"))
    }
}
