package com.garrietta.haven.data

import org.json.JSONObject

/**
 * Reliability round (2026-11-26 QA/QC pass) — the pure rules behind
 * the connection rewrite: which endpoints to try and in what order,
 * how long each attempt may take, how retries back off, when the
 * OAuth access token is renewed, how a token-endpoint answer is
 * classified, which URLs Home Assistant itself reports, and how big
 * a camera snapshot needs to be. No Android types, so every rule is
 * pinned by ConnectionPlannerTest.
 *
 * What this replaces, and why:
 * - The old routing picked ONE URL from "is the phone on Wi-Fi" and
 *   only failed over after the socket died, so a foreign Wi-Fi (or a
 *   home Wi-Fi with the Pi down) cost a full 10s connect timeout, and
 *   once failed over to the cloud it never came back to the LAN.
 *   Now the client walks an ordered candidate list with a short
 *   timeout on LAN addresses, and while it sits on a fallback it
 *   quietly re-probes the preferred endpoint and switches back
 *   make-before-break (see HaClient).
 * - OAuth access tokens live 30 minutes. The socket keeps working
 *   past expiry, but every REST call (camera snapshots, history, HLS
 *   segments) started failing with 401 — and Home Assistant counts
 *   each 401 as a failed login toward its IP ban. Tokens are now
 *   renewed ahead of expiry, and a definitive refusal from the token
 *   endpoint stops retrying immediately instead of hammering it.
 */

/** The active network reduced to what routing needs. */
data class NetworkProfile(
    val available: Boolean,
    val wifi: Boolean = false,
    val ethernet: Boolean = false,
    val vpn: Boolean = false,
    val cellular: Boolean = false,
) {
    /**
     * A transport on which the LAN address can plausibly answer:
     * Wi-Fi, Ethernet (docked tablets), or a VPN (Tailscale /
     * WireGuard back to the house). Cellular alone cannot reach a
     * private address, so the cloud URL goes first there.
     */
    val localCapable: Boolean get() = available && (wifi || ethernet || vpn)

    companion object {
        val OFFLINE = NetworkProfile(available = false)
    }
}

/**
 * Ordered endpoints to try for [mode]. AUTO puts the LAN address
 * first on a local-capable network and the cloud first elsewhere,
 * and always keeps the other side as a fallback. LOCAL / CLOUD are
 * pinned: only their own side, falling back to the other side only
 * when their own is unset (so a pinned mode can never strand the app
 * with no URL at all — same rule as [resolveConnectionTarget]).
 * Blank and duplicate URLs are dropped; trailing slashes trimmed.
 */
fun connectionCandidates(
    mode: ConnectionMode,
    localUrl: String?,
    cloudUrl: String?,
    network: NetworkProfile,
): List<String> {
    val ordered = when (mode) {
        ConnectionMode.LOCAL -> listOf(localUrl ?: cloudUrl)
        ConnectionMode.CLOUD -> listOf(cloudUrl ?: localUrl)
        ConnectionMode.AUTO ->
            if (network.localCapable) listOf(localUrl, cloudUrl) else listOf(cloudUrl, localUrl)
    }
    return ordered
        .mapNotNull { it?.trim()?.trimEnd('/') }
        .filter { it.isNotEmpty() }
        .distinct()
}

/** TCP connect budget for a LAN address: a live Pi answers in milliseconds. */
const val LOCAL_CONNECT_TIMEOUT_MS = 3_000L

/** TCP connect budget for a remote address (Nabu Casa relays add real latency). */
const val REMOTE_CONNECT_TIMEOUT_MS = 10_000L

/** Per-attempt connect timeout for [url]. */
fun endpointConnectTimeoutMs(url: String): Long =
    if (isLocalAddress(url)) LOCAL_CONNECT_TIMEOUT_MS else REMOTE_CONNECT_TIMEOUT_MS

/**
 * WebSocket ping cadence. Without pings a socket whose network
 * vanished (walked out of Wi-Fi range, Doze) stays "connected" until
 * TCP gives up minutes later; OkHttp fails the socket when a pong is
 * missed, which starts the reconnect pass promptly.
 */
const val WS_PING_INTERVAL_MS = 20_000L

/** auth_ok must arrive this long after the socket opened, or the attempt is abandoned. */
const val AUTH_HANDSHAKE_TIMEOUT_MS = 10_000L

/** Application-level ping budget used when the app returns to the foreground. */
const val LIVENESS_PONG_TIMEOUT_MS = 5_000L

/** While connected through a fallback endpoint, the preferred one is re-probed this often. */
const val PREFERRED_ENDPOINT_PROBE_MS = 30_000L

/** A replaced link keeps routing results for this long so in-flight calls still complete. */
const val LINK_DRAIN_MS = 5_000L

/** Network callbacks fire in bursts during a Wi-Fi/cellular handoff; act once it settles. */
const val NETWORK_SETTLE_MS = 750L

/**
 * Backoff between full passes over the candidate list: 1s, 2s, 4s,
 * 8s, 15s, then 30s for every later pass. [jitter] in [0, 1] spreads
 * each delay ±20% so several clients (the app and the Device
 * Controls service) never retry in lockstep. Retries pause entirely
 * while the phone is offline; the network callback resumes them.
 */
fun reconnectBackoffMs(attempt: Int, jitter: Double = 0.5): Long {
    val base = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 30_000L)[attempt.coerceIn(0, 5)]
    val factor = 0.8 + 0.4 * jitter.coerceIn(0.0, 1.0)
    return (base * factor).toLong()
}

/** Renew this long before the access token's expiry. */
const val TOKEN_REFRESH_MARGIN_MS = 5 * 60_000L

/**
 * Delay until the proactive token refresh, or null when the expiry
 * is unknown (long-lived token, or no expires_in reported). Never
 * negative: an already-due refresh runs now.
 */
fun tokenRefreshDelayMs(expiresAtMs: Long, nowMs: Long, marginMs: Long = TOKEN_REFRESH_MARGIN_MS): Long? {
    if (expiresAtMs <= 0L) return null
    return (expiresAtMs - marginMs - nowMs).coerceAtLeast(0L)
}

/** True when a known expiry is within [marginMs] of [nowMs] (or already past). */
fun tokenNeedsRefresh(expiresAtMs: Long, nowMs: Long, marginMs: Long = 60_000L): Boolean =
    expiresAtMs > 0L && expiresAtMs - nowMs <= marginMs

/** What one call to Home Assistant's /auth/token produced. */
enum class TokenRefreshOutcome {
    /** New access token stored. */
    OK,

    /** The instance refused the grant: the refresh token is dead; sign in again. */
    REJECTED,

    /** Network error, timeout, relay hiccup, or 5xx: retry later, possibly via the other URL. */
    UNREACHABLE,
}

/**
 * Classifies a token-endpoint answer. Only an explicit refusal is
 * REJECTED — 401/403, or a 400 carrying an OAuth error code — so a
 * relay error page or a restarting instance never logs the user out.
 * [httpCode] null means no HTTP answer at all.
 */
fun classifyTokenResponse(httpCode: Int?, body: String?): TokenRefreshOutcome {
    if (httpCode == null) return TokenRefreshOutcome.UNREACHABLE
    val json = try {
        JSONObject(body ?: "")
    } catch (e: Exception) {
        null
    }
    if (httpCode in 200..299) {
        val access = json?.optString("access_token").orEmpty()
        return if (access.isNotEmpty() && access != "null") TokenRefreshOutcome.OK else TokenRefreshOutcome.UNREACHABLE
    }
    if (httpCode == 401 || httpCode == 403) return TokenRefreshOutcome.REJECTED
    if (httpCode == 400) {
        val error = json?.optString("error").orEmpty()
        return if (error in OAUTH_REFUSAL_CODES) TokenRefreshOutcome.REJECTED else TokenRefreshOutcome.UNREACHABLE
    }
    return TokenRefreshOutcome.UNREACHABLE
}

private val OAUTH_REFUSAL_CODES = setOf("invalid_grant", "invalid_request", "invalid_client", "unauthorized_client")

/** URLs Home Assistant reports for itself (network/url, or cloud/status for the remote domain). */
data class DetectedUrls(val internal: String?, val external: String?, val cloud: String?)

/**
 * Parses the `network/url` result: {internal, external, cloud}, any of
 * which may be null. Android's org.json renders a JSON null as the
 * string "null", so that spelling counts as absent too. Null when
 * nothing usable came back.
 */
fun parseNetworkUrls(result: JSONObject?): DetectedUrls? {
    if (result == null) return null
    fun read(key: String): String? =
        result.optString(key).trim().trimEnd('/').takeIf { it.isNotEmpty() && it != "null" }
    val detected = DetectedUrls(read("internal"), read("external"), read("cloud"))
    return if (detected.internal == null && detected.external == null && detected.cloud == null) null else detected
}

/**
 * The remote-UI URL from a `cloud/status` result: its remote_domain
 * (e.g. "abc123.ui.nabu.casa"), only when remote access is not
 * explicitly disabled. Null when the instance is not on Nabu Casa.
 */
fun parseCloudStatusUrl(result: JSONObject?): String? {
    if (result == null) return null
    if (result.has("remote_enabled") && !result.optBoolean("remote_enabled", true)) return null
    val domain = result.optString("remote_domain").trim().trimEnd('/')
    if (domain.isEmpty() || domain == "null") return null
    return if (domain.contains("://")) domain else "https://$domain"
}

/** The cloud-side URL to adopt from [detected]: Nabu Casa first, then a non-LAN external URL. */
fun detectedCloudUrl(detected: DetectedUrls): String? =
    detected.cloud ?: detected.external?.takeIf { !isLocalAddress(it) }

/** The LAN-side URL to adopt from [detected]: the internal URL, only when it really is a LAN address. */
fun detectedLocalUrl(detected: DetectedUrls): String? =
    detected.internal?.takeIf { isLocalAddress(it) }

/**
 * Width to request from HA's camera proxy (`?width=`) for a slot
 * [slotWidthPx] wide. Rounded UP to a 160px bucket so a few pixels of
 * layout jitter never refetch, clamped to 320..1920. A 1080p Tapo
 * frame is ~250KB; at quadrant size it is ~40KB — that is the
 * difference between instant and multi-second loads through the
 * Nabu Casa relay. Unknown slot width -> 640.
 */
fun cameraSnapshotWidthPx(slotWidthPx: Int): Int {
    if (slotWidthPx <= 0) return 640
    val bucket = ((slotWidthPx + 159) / 160) * 160
    return bucket.coerceIn(320, 1920)
}

/**
 * BitmapFactory inSampleSize: the largest power of two that keeps the
 * decoded width at or above [targetWidth]. Cameras that ignore
 * `?width=` still decode at slot size instead of full resolution.
 */
fun snapshotInSampleSize(srcWidth: Int, srcHeight: Int, targetWidth: Int): Int {
    if (srcWidth <= 0 || srcHeight <= 0 || targetWidth <= 0) return 1
    var sample = 1
    while (srcWidth / (sample * 2) >= targetWidth) sample *= 2
    return sample
}

/**
 * How often a VISIBLE camera tile refreshes, and how old a held
 * snapshot may be before view entry refetches it. LAN images are
 * cheap; remote ones cross the relay, so they refresh half as often.
 */
fun snapshotRefreshIntervalMs(onLocal: Boolean): Long = if (onLocal) 10_000L else 20_000L

/** An HLS playlist URL from `camera/stream` stays reusable this long (prewarm -> open). */
const val HLS_URL_REUSE_MS = 45_000L

/** True when an HLS URL fetched at [fetchedAtMs] may still be handed to a player at [nowMs]. */
fun hlsUrlReusable(fetchedAtMs: Long, nowMs: Long): Boolean {
    val age = nowMs - fetchedAtMs
    return age in 0 until HLS_URL_REUSE_MS
}
