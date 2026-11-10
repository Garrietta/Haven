package com.garrietta.haven.data

import androidx.compose.ui.geometry.Rect
import com.garrietta.haven.Screen
import com.garrietta.haven.ui.components.NavItem
import com.garrietta.haven.ui.components.pillBarItems
import com.garrietta.haven.ui.components.selectedNavIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feedback round 16: the round's new pure logic, pinned by tests. */
class ConnectionTargetsTest {

    // Item 1 — aspect-ratio gate.

    @Test fun wideLayout_needs600dpAndWidthAtLeastHeight() {
        assertTrue(isWideLayout(800f, 600f))
        assertTrue(isWideLayout(700f, 700f))
        assertFalse(isWideLayout(707f, 822f))
        assertFalse(isWideLayout(500f, 800f))
        assertFalse(isWideLayout(599f, 400f))
        assertFalse(isWideLayout(600f, 601f))
    }

    // Item 7 — host parsing and local/cloud classification.

    @Test fun connectionHost_stripsSchemePortPathAndBrackets() {
        assertEquals("10.0.0.11", connectionHostOf("http://10.0.0.11:8123"))
        assertEquals("example.ui.nabu.casa", connectionHostOf("https://example.ui.nabu.casa/"))
        assertEquals("::1", connectionHostOf("http://[::1]:8123/api/websocket"))
        assertEquals("myhost.local", connectionHostOf("http://myhost.local:8123"))
        assertNull(connectionHostOf(null))
        assertNull(connectionHostOf(""))
        assertNull(connectionHostOf("http://"))
    }

    @Test fun isLocalAddress_privateLoopbackAndMdnsAreLocal() {
        assertTrue(isLocalAddress("http://10.0.0.11:8123"))
        assertTrue(isLocalAddress("http://192.168.1.5:8123"))
        assertTrue(isLocalAddress("http://172.16.0.9:8123"))
        assertTrue(isLocalAddress("http://172.31.255.1:8123"))
        assertTrue(isLocalAddress("http://localhost:8123"))
        assertTrue(isLocalAddress("http://127.0.0.1:8123"))
        assertTrue(isLocalAddress("http://myhost.local:8123"))
        assertTrue(isLocalAddress("http://[::1]:8123"))
        assertTrue(isLocalAddress("http://169.254.10.20:8123"))
    }

    @Test fun isLocalAddress_publicAndBoundaryHostsAreCloud() {
        assertFalse(isLocalAddress("https://example.ui.nabu.casa"))
        assertFalse(isLocalAddress("https://8.8.8.8:8123"))
        assertFalse(isLocalAddress("http://172.32.0.1:8123"))
        assertFalse(isLocalAddress("http://172.15.0.1:8123"))
        assertFalse(isLocalAddress("http://192.169.1.1:8123"))
        assertFalse(isLocalAddress("http://193.168.1.1:8123"))
        // A hostname merely STARTING with fc/fd is not an IPv6
        // unique-local address — the prefix rule is literals only.
        assertFalse(isLocalAddress("https://fancy.example.com"))
        assertFalse(isLocalAddress(null))
    }

    @Test fun connectionLabel_isLocalOrCloudOnly() {
        assertEquals("Local", connectionLabel("http://10.0.0.11:8123"))
        assertEquals("Cloud", connectionLabel("https://example.ui.nabu.casa"))
        assertEquals("Cloud", connectionLabel(null))
    }

    @Test fun resolveConnectionTarget_modesAndFallbacks() {
        val local = "http://10.0.0.11:8123"
        val cloud = "https://example.ui.nabu.casa"
        assertEquals(local, resolveConnectionTarget(ConnectionMode.AUTO, local, cloud, true))
        assertEquals(cloud, resolveConnectionTarget(ConnectionMode.AUTO, local, cloud, false))
        assertEquals(local, resolveConnectionTarget(ConnectionMode.LOCAL, local, cloud, false))
        assertEquals(cloud, resolveConnectionTarget(ConnectionMode.CLOUD, local, cloud, true))
        // Missing side falls back to the other URL, never null.
        assertEquals(cloud, resolveConnectionTarget(ConnectionMode.AUTO, null, cloud, true))
        assertEquals(local, resolveConnectionTarget(ConnectionMode.AUTO, local, null, false))
        assertEquals(cloud, resolveConnectionTarget(ConnectionMode.LOCAL, null, cloud, true))
        assertEquals(local, resolveConnectionTarget(ConnectionMode.CLOUD, local, null, false))
        assertNull(resolveConnectionTarget(ConnectionMode.AUTO, null, null, true))
    }

    @Test fun connectionMode_parsesLeniently() {
        assertEquals(ConnectionMode.AUTO, connectionModeFromKey(null))
        assertEquals(ConnectionMode.AUTO, connectionModeFromKey("bogus"))
        assertEquals(ConnectionMode.LOCAL, connectionModeFromKey("local"))
        assertEquals(ConnectionMode.CLOUD, connectionModeFromKey("cloud"))
    }

    // Item 4c — captured-at-open beats the live map.

    @Test fun resolveMorphSource_capturedBeatsLiveMap() {
        val captured = Rect(10f, 20f, 110f, 120f)
        val livePoisoned = Rect(300f, 400f, 600f, 900f)
        assertEquals(captured, resolveMorphSource(captured, livePoisoned))
        assertEquals(livePoisoned, resolveMorphSource(null, livePoisoned))
        assertNull(resolveMorphSource(null, null))
    }

    // Item 6 — auto-pin when a slot is free.

    @Test fun shouldAutoPinNewTab_respectsTwoSlotCap() {
        assertTrue(shouldAutoPinNewTab(0))
        assertTrue(shouldAutoPinNewTab(1))
        assertFalse(shouldAutoPinNewTab(2))
        assertFalse(shouldAutoPinNewTab(3))
    }

    // Item 9 — the active index is recomputed from the CURRENT bar
    // list, never carried as a stored index: after a pinned tab is
    // inserted, Settings resolves to its new index, not the stale one.
    @Test fun selectedNavIndex_recomputesAfterTabListChanges() {
        val before: List<NavItem> = pillBarItems(true, true, emptyList())
        assertEquals(3, selectedNavIndex(Screen.Settings, before))
        val climate = UserTab("tab_1", "climate", emptyList(), pinned = true)
        val after: List<NavItem> = pillBarItems(true, true, listOf(climate))
        assertEquals(4, selectedNavIndex(Screen.Settings, after))
        assertEquals(3, selectedNavIndex(Screen.UserTab("tab_1"), after))
        assertNull(selectedNavIndex(Screen.UserTab("tab_missing"), after))
    }
}
