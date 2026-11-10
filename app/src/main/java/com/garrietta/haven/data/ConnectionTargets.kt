package com.garrietta.haven.data

import androidx.compose.ui.geometry.Rect

/**
 * Feedback round 16 — pure helpers for the round's new logic, kept
 * free of Android framework types so unit tests pin them exactly.
 * UI code calls these; it never re-implements their rules inline.
 */

/**
 * Item 1 — split-screen rule (aspect ratio). The unfolded/tablet
 * layout engages ONLY once the window is at least 600dp wide AND at
 * least as wide as it is tall. At ~50% split-screen on a folded
 * phone the width is far below the height, so the folded portrait
 * layout stays — the old width-only gate flipped to tablet mode
 * the moment the width crossed 600dp, whatever the height was.
 * Unfolded portrait (e.g. 707x822dp) is folded layout under this
 * rule, by the user's explicit decision.
 */
fun isWideLayout(maxWidthDp: Float, maxHeightDp: Float): Boolean =
    maxWidthDp >= 600f && maxWidthDp >= maxHeightDp

/**
 * Item 7 — connection labels. The host of a URL with scheme, port,
 * path, and (for IPv6) brackets stripped, lowercased. Null or
 * unparseable input yields null (never a guess).
 */
fun connectionHostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val withoutScheme = url.substringAfter("://", url)
    val authority = withoutScheme.substringBefore("/").substringBefore("?")
    val hostPort = authority.substringAfter("@", authority)
    val host = if (hostPort.startsWith("[")) {
        hostPort.substringBefore("]")
            .removePrefix("[")
    } else {
        hostPort.substringBefore(":")
    }
    return host.lowercase().takeIf { it.isNotEmpty() }
}

/**
 * Item 7 — true when the host is a local address: localhost,
 * loopback, mDNS (.local), RFC 1918 private ranges, or IPv6
 * loopback / link-local / unique-local. Anything else — including
 * a Nabu Casa remote-UI hostname — is Cloud. The label rule is
 * exactly this: anything that is not a local address reads Cloud.
 */
fun isLocalAddress(url: String?): Boolean {
    val host = connectionHostOf(url) ?: return false
    if (host == "localhost" || host.endsWith(".local") || host == "::1") return true
    // IPv6 literals only (they contain ':'): link-local and
    // unique-local prefixes. Hostnames never reach this branch.
    if (host.contains(":") &&
        (host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd"))
    ) return true
    val parts = host.split(".")
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull() ?: return false }
    if (octets.any { it !in 0..255 }) return false
    return when (octets[0]) {
        10 -> true
        127 -> true
        172 -> octets[1] in 16..31
        192 -> octets[1] == 168
        169 -> octets[1] == 254
        else -> false
    }
}

/** Item 7 — the Connection row label: Local or Cloud, nothing else. */
fun connectionLabel(url: String?): String = if (isLocalAddress(url)) "Local" else "Cloud"

/** Item 7 — the connection-mode choice stored in prefs. */
enum class ConnectionMode { AUTO, LOCAL, CLOUD }

/** Lenient parse: unknown keys (including legacy values) mean AUTO. */
fun connectionModeFromKey(key: String?): ConnectionMode = when (key) {
    "local" -> ConnectionMode.LOCAL
    "cloud" -> ConnectionMode.CLOUD
    else -> ConnectionMode.AUTO
}

/**
 * Item 7 — which URL to connect to. AUTO prefers the local URL on
 * Wi-Fi and the cloud URL off it; LOCAL/CLOUD pin one side. Every
 * branch falls back to the other URL when its own side is unset,
 * so switching modes can never strand the app with no URL at all.
 * The same HA token works through both URLs (Nabu Casa remote UI
 * forwards to the same instance), so no re-login is needed on a
 * switch.
 */
fun resolveConnectionTarget(
    mode: ConnectionMode,
    localUrl: String?,
    cloudUrl: String?,
    wifiConnected: Boolean,
): String? = when (mode) {
    ConnectionMode.LOCAL -> localUrl ?: cloudUrl
    ConnectionMode.CLOUD -> cloudUrl ?: localUrl
    ConnectionMode.AUTO -> if (wifiConnected) localUrl ?: cloudUrl else cloudUrl ?: localUrl
}

/**
 * Item 4c — morph origin resolution. The rect captured synchronously
 * at tap time ALWAYS wins over the live card-bounds map: while a
 * panel for an entity is open, that panel's own cards report their
 * panel-internal rects under the same entity id, poisoning the map
 * — exit/reopen morphs that read the map first land on the
 * panel-internal rect ("opening out of proper location"). The
 * thermostat popout never showed this because it contains no
 * reporting card, so its tile rect was never overwritten. The live
 * map is only the fallback for anims captured before this rule.
 */
fun resolveMorphSource(captured: Rect?, live: Rect?): Rect? = captured ?: live

/**
 * Item 6 — a newly created tab is auto-pinned to the bar when a pin
 * slot is free (at most two pinned tabs). The user's "climate" tab
 * was created correctly but unpinned, so it appeared nowhere he
 * looked: not on the bar, and the empty tab screen reads as
 * nothing happening. Auto-pin puts it on the bar where he looked;
 * the Pin button still unpins it in one tap.
 */
fun shouldAutoPinNewTab(pinnedCount: Int, maxPinned: Int = 2): Boolean =
    pinnedCount < maxPinned

/**
 * Round 25, item 1: the alternate URL for the current connection
 * target — the other side of the local/cloud pair. AUTO on Wi-Fi
 * targets local, so its alternate is cloud (and vice versa off
 * Wi-Fi); LOCAL's alternate is cloud, CLOUD's is local. When the
 * alternate side is unset the current target is returned (there is
 * nothing to fall back to, and the caller must not switch to null);
 * when neither side is set the result is null.
 */
fun alternateConnectionTarget(
    mode: ConnectionMode,
    localUrl: String?,
    cloudUrl: String?,
    wifiConnected: Boolean,
): String? {
    val current = resolveConnectionTarget(mode, localUrl, cloudUrl, wifiConnected)
    val other = when (current) {
        localUrl -> cloudUrl
        cloudUrl -> localUrl
        // The current target came from a fallback (its own side was
        // unset), so the "other side" is the side the mode prefers.
        else -> if (wifiConnected) cloudUrl else localUrl
    }
    return other ?: current
}

/**
 * Round 25, item 1: the auth-retry loop gives up after this many
 * failed refresh attempts (~5 minutes of backoff) and only then
 * surfaces "Your session expired. Please sign in again." A manual
 * reconnect tap restarts the loop from attempt 0.
 */
const val MAX_AUTH_RETRIES = 8

/**
 * Round 25, item 1: backoff between auth-retry attempts — 2s, 5s,
 * 10s, 30s, then 60s for every later attempt — so a transient
 * drop retries quickly at first and never hammers the instance
 * while it is down. Pure, unit-tested.
 */
fun authRetryDelayMs(attempt: Int): Long =
    longArrayOf(2_000L, 5_000L, 10_000L, 30_000L, 60_000L)[attempt.coerceIn(0, 4)]
