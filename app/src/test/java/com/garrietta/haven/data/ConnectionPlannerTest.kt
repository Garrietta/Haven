package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reliability round (2026-11-26): the pure rules behind the
 * connection rewrite — endpoint ordering, per-endpoint timeouts,
 * backoff, token renewal timing and classification, self-reported
 * URL parsing, LAN host classification, and snapshot sizing.
 */
class ConnectionPlannerTest {

    private val local = "http://10.0.0.11:8123"
    private val cloud = "https://abc123.ui.nabu.casa"
    private val wifi = NetworkProfile(available = true, wifi = true)
    private val cellular = NetworkProfile(available = true, cellular = true)

    // ---- Candidate ordering ----

    @Test
    fun auto_onWifi_triesLanThenCloud() {
        assertEquals(listOf(local, cloud), connectionCandidates(ConnectionMode.AUTO, local, cloud, wifi))
    }

    @Test
    fun auto_onCellular_triesCloudThenLan() {
        assertEquals(listOf(cloud, local), connectionCandidates(ConnectionMode.AUTO, local, cloud, cellular))
    }

    @Test
    fun auto_ethernetAndVpnAreLocalCapable() {
        val ethernet = NetworkProfile(available = true, ethernet = true)
        val vpnOverCellular = NetworkProfile(available = true, vpn = true, cellular = true)
        assertEquals(local, connectionCandidates(ConnectionMode.AUTO, local, cloud, ethernet).first())
        assertEquals(local, connectionCandidates(ConnectionMode.AUTO, local, cloud, vpnOverCellular).first())
    }

    @Test
    fun auto_offline_prefersCloud() {
        assertEquals(listOf(cloud, local), connectionCandidates(ConnectionMode.AUTO, local, cloud, NetworkProfile.OFFLINE))
        assertFalse(NetworkProfile(available = false, wifi = true).localCapable)
    }

    @Test
    fun pinnedModes_useOnlyTheirSide() {
        assertEquals(listOf(local), connectionCandidates(ConnectionMode.LOCAL, local, cloud, cellular))
        assertEquals(listOf(cloud), connectionCandidates(ConnectionMode.CLOUD, local, cloud, wifi))
    }

    @Test
    fun pinnedModes_fallBackWhenTheirSideIsUnset() {
        assertEquals(listOf(cloud), connectionCandidates(ConnectionMode.LOCAL, null, cloud, wifi))
        assertEquals(listOf(local), connectionCandidates(ConnectionMode.CLOUD, local, null, wifi))
    }

    @Test
    fun candidates_dropBlanksDuplicatesAndTrailingSlashes() {
        assertEquals(listOf(local), connectionCandidates(ConnectionMode.AUTO, "$local/", "$local", wifi))
        assertEquals(listOf(cloud), connectionCandidates(ConnectionMode.AUTO, "  ", cloud, wifi))
        assertTrue(connectionCandidates(ConnectionMode.AUTO, null, null, wifi).isEmpty())
    }

    // ---- Timeouts + backoff ----

    @Test
    fun lanAttemptsFailFast_remoteGetsTheRelayBudget() {
        assertEquals(LOCAL_CONNECT_TIMEOUT_MS, endpointConnectTimeoutMs(local))
        assertEquals(LOCAL_CONNECT_TIMEOUT_MS, endpointConnectTimeoutMs("http://homeassistant.local:8123"))
        assertEquals(REMOTE_CONNECT_TIMEOUT_MS, endpointConnectTimeoutMs(cloud))
        assertTrue(LOCAL_CONNECT_TIMEOUT_MS < REMOTE_CONNECT_TIMEOUT_MS)
    }

    @Test
    fun backoff_growsThenCaps() {
        assertEquals(1_000L, reconnectBackoffMs(0))
        assertEquals(2_000L, reconnectBackoffMs(1))
        assertEquals(4_000L, reconnectBackoffMs(2))
        assertEquals(8_000L, reconnectBackoffMs(3))
        assertEquals(15_000L, reconnectBackoffMs(4))
        assertEquals(30_000L, reconnectBackoffMs(5))
        assertEquals(30_000L, reconnectBackoffMs(50))
        assertEquals(1_000L, reconnectBackoffMs(-3))
    }

    @Test
    fun backoff_jitterStaysWithinTwentyPercent() {
        assertEquals(800L, reconnectBackoffMs(0, 0.0))
        assertEquals(1_200L, reconnectBackoffMs(0, 1.0))
        assertEquals(24_000L, reconnectBackoffMs(9, 0.0))
        assertEquals(36_000L, reconnectBackoffMs(9, 1.0))
        assertEquals(1_200L, reconnectBackoffMs(0, 7.0))
    }

    // ---- Token renewal ----

    @Test
    fun refreshDelay_renewsFiveMinutesEarly() {
        val now = 1_000_000L
        val expires = now + 30 * 60_000L
        assertEquals(25 * 60_000L, tokenRefreshDelayMs(expires, now))
    }

    @Test
    fun refreshDelay_dueOrPastRunsNow_unknownIsNull() {
        val now = 1_000_000L
        assertEquals(0L, tokenRefreshDelayMs(now + 60_000L, now))
        assertEquals(0L, tokenRefreshDelayMs(now - 60_000L, now))
        assertNull(tokenRefreshDelayMs(0L, now))
    }

    @Test
    fun needsRefresh_onlyForKnownExpiryInsideMargin() {
        val now = 1_000_000L
        assertTrue(tokenNeedsRefresh(now + 30_000L, now))
        assertTrue(tokenNeedsRefresh(now - 1L, now))
        assertFalse(tokenNeedsRefresh(now + 10 * 60_000L, now))
        assertFalse(tokenNeedsRefresh(0L, now))
    }

    @Test
    fun tokenResponse_okNeedsAnAccessToken() {
        assertEquals(TokenRefreshOutcome.OK, classifyTokenResponse(200, """{"access_token":"abc","expires_in":1800}"""))
        assertEquals(TokenRefreshOutcome.UNREACHABLE, classifyTokenResponse(200, """{"token_type":"Bearer"}"""))
        assertEquals(TokenRefreshOutcome.UNREACHABLE, classifyTokenResponse(200, "<html>relay</html>"))
    }

    @Test
    fun tokenResponse_onlyExplicitRefusalsAreRejected() {
        assertEquals(TokenRefreshOutcome.REJECTED, classifyTokenResponse(400, """{"error":"invalid_grant"}"""))
        assertEquals(TokenRefreshOutcome.REJECTED, classifyTokenResponse(400, """{"error":"invalid_request","error_description":"x"}"""))
        assertEquals(TokenRefreshOutcome.REJECTED, classifyTokenResponse(401, null))
        assertEquals(TokenRefreshOutcome.REJECTED, classifyTokenResponse(403, "Forbidden"))
        // A relay or proxy error page is not a refusal.
        assertEquals(TokenRefreshOutcome.UNREACHABLE, classifyTokenResponse(400, "<html>Bad Request</html>"))
        assertEquals(TokenRefreshOutcome.UNREACHABLE, classifyTokenResponse(502, null))
        assertEquals(TokenRefreshOutcome.UNREACHABLE, classifyTokenResponse(404, null))
        assertEquals(TokenRefreshOutcome.UNREACHABLE, classifyTokenResponse(null, null))
    }

    // ---- Self-reported URLs ----

    @Test
    fun networkUrls_parseAndDropNulls() {
        val parsed = parseNetworkUrls(
            JSONObject()
                .put("internal", "http://192.168.1.20:8123/")
                .put("external", JSONObject.NULL)
                .put("cloud", "https://abc123.ui.nabu.casa"),
        )!!
        assertEquals("http://192.168.1.20:8123", parsed.internal)
        assertNull(parsed.external)
        assertEquals("https://abc123.ui.nabu.casa", parsed.cloud)
        // Android's org.json spells a JSON null as the string "null".
        assertNull(parseNetworkUrls(JSONObject().put("internal", "null").put("cloud", "")))
        assertNull(parseNetworkUrls(null))
    }

    @Test
    fun detectedUrls_pickTheRightSide() {
        val withCloud = DetectedUrls("http://192.168.1.20:8123", "https://home.example.com", cloud)
        assertEquals(cloud, detectedCloudUrl(withCloud))
        assertEquals("http://192.168.1.20:8123", detectedLocalUrl(withCloud))
        val externalOnly = DetectedUrls(null, "https://home.example.com", null)
        assertEquals("https://home.example.com", detectedCloudUrl(externalOnly))
        // An internal URL that is not actually a LAN address is not adopted as local.
        assertNull(detectedLocalUrl(DetectedUrls("https://home.example.com", null, null)))
        // A LAN "external" URL is not a cloud URL.
        assertNull(detectedCloudUrl(DetectedUrls(null, "http://10.0.0.5:8123", null)))
    }

    @Test
    fun cloudStatus_remoteDomainBecomesUrl() {
        assertEquals(
            "https://abc123.ui.nabu.casa",
            parseCloudStatusUrl(JSONObject().put("remote_enabled", true).put("remote_domain", "abc123.ui.nabu.casa")),
        )
        assertNull(parseCloudStatusUrl(JSONObject().put("remote_enabled", false).put("remote_domain", "abc123.ui.nabu.casa")))
        assertNull(parseCloudStatusUrl(JSONObject().put("logged_in", false)))
        assertNull(parseCloudStatusUrl(null))
    }

    // ---- LAN host classification (extends round 16's rule) ----

    @Test
    fun lanSuffixesAndBareHostnamesAreLocal() {
        assertTrue(isLocalAddress("http://homeassistant:8123"))
        assertTrue(isLocalAddress("http://ha.lan:8123"))
        assertTrue(isLocalAddress("http://pi.home.arpa:8123"))
        assertTrue(isLocalAddress("http://ha.internal:8123"))
        assertTrue(isLocalAddress("http://ha.localdomain"))
        assertFalse(isLocalAddress("https://abc123.ui.nabu.casa"))
        assertFalse(isLocalAddress("https://lan.example.com"))
        assertEquals("Local", connectionLabel("http://homeassistant:8123"))
    }

    // ---- Camera sizing ----

    @Test
    fun snapshotWidth_bucketsAndClamps() {
        assertEquals(640, cameraSnapshotWidthPx(0))
        assertEquals(320, cameraSnapshotWidthPx(100))
        assertEquals(640, cameraSnapshotWidthPx(540))
        assertEquals(640, cameraSnapshotWidthPx(640))
        assertEquals(800, cameraSnapshotWidthPx(641))
        assertEquals(1920, cameraSnapshotWidthPx(4000))
    }

    @Test
    fun inSampleSize_keepsAtLeastTargetWidth() {
        assertEquals(1, snapshotInSampleSize(1920, 1080, 1920))
        assertEquals(2, snapshotInSampleSize(1920, 1080, 960))
        assertEquals(2, snapshotInSampleSize(1920, 1080, 640))
        assertEquals(4, snapshotInSampleSize(2560, 1440, 640))
        assertEquals(1, snapshotInSampleSize(0, 0, 640))
        assertEquals(1, snapshotInSampleSize(1920, 1080, 0))
    }

    @Test
    fun refreshCadence_relayIsGentler() {
        assertTrue(snapshotRefreshIntervalMs(onLocal = true) < snapshotRefreshIntervalMs(onLocal = false))
    }

    @Test
    fun hlsUrlReuseWindow() {
        val at = 1_000_000L
        assertTrue(hlsUrlReusable(at, at))
        assertTrue(hlsUrlReusable(at, at + HLS_URL_REUSE_MS - 1))
        assertFalse(hlsUrlReusable(at, at + HLS_URL_REUSE_MS))
        assertFalse(hlsUrlReusable(at, at - 1))
    }
}
