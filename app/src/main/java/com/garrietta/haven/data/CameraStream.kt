package com.garrietta.haven.data

/**
 * Camera playback: the MJPEG proxy stream for every camera, plus
 * HA's HLS stream path (attempt code retained, not attempted for
 * Ring) and WebRTC (same).
 *
 * Root cause of the still-image bug: the fullscreen viewer fetched ONE frame
 * from `/api/camera_proxy/<entity>` (a single JPEG snapshot) and displayed
 * it until the user hit Refresh. It never opened a stream at all — no silent
 * fallback, just snapshot-only code. The live path is
 * `/api/camera_proxy_stream/<entity>`, which yields a multipart/x-mixed-replace
 * MJPEG stream for every camera HA can serve: native MJPEG cameras stream
 * directly. Run #38 read Ring as the exception: its integration
 * overrides the MJPEG handler and builds the stream from the most
 * recent motion recording (ffmpeg transcodes the recording), so the
 * frames are a clip, not live. That reading was UNDER TEST through
 * the 2026-10-29 MJPEG-live round — and the 2026-10-30 verdict
 * CONFIRMS it: he reports the front-door view "showing a recording
 * now as opposed to the live view" and "bouncing back and forth"
 * between the two. A fresh motion clip looks live-ish (a person
 * moving on the porch); an older clip is obviously a recording;
 * that is the flip. So for Ring the proxy IS the last-recording
 * path, labeled as a recording everywhere it appears (fullscreen
 * connecting text + persistent caption + error copy, hero dialog
 * caption, detail-page button copy), never presented as live, no
 * "Live" badge. The wave test is moot for this path — it cannot
 * pass by construction — and now gates only a FUTURE genuine live
 * path: the frontend's actual network requests for the Ring card
 * (its live transport is still unidentified — WS camera/stream
 * refuses this camera and WebRTC answers empty, so the frontend is
 * not using either), or ring-mqtt's RTSP stream via a Generic
 * Camera entity (whose live-stream switch reverts after ~1s on his
 * instance, cause still unproven). The HLS/WebRTC attempt code
 * remains in the codebase (HaClient.requestHlsUrl, RingWebRtcSession)
 * but is no longer attempted for Ring fullscreen; the HLS diagnostic
 * helpers stay, with their unit tests.
 *
 * [MjpegParser] is pure byte handling (no Android types) so it is unit
 * tested on the JVM; decoding to Bitmap happens in [HaClient.streamCamera].
 */

/**
 * True when this camera is a Ring camera. Routing signal: the
 * fullscreen plays the MJPEG proxy for Ring as the labeled
 * latest-recording path (2026-10-30 verdict confirms #38), and
 * the "Live" badge is never shown for it — a fresh clip also
 * renders moving frames. The "Last recording" presentation
 * applies to the clips-timeline recording entries AND to the
 * Ring fullscreen/dialog, never as a live claim.
 *
 * Two signals, no hardcoded entity id: the `brand` state attribute, and
 * the device-registry manufacturer HaClient attaches to the entity.
 * The brand signal alone is why run #38's label never fired on Garrett's
 * instance: his Ring camera's state attributes carry no `brand` at all,
 * so the fullscreen fell through to generic "Loading camera..." with no
 * badge. The registry manufacturer is the ground truth for who made the
 * device and covers the missing-attribute case. (Deliberately NOT
 * `frontend_stream_type`: web_rtc is not Ring-specific, and using it
 * would risk routing a working Tapo camera onto the Ring path.)
 */
fun isRingCamera(entity: HaEntity): Boolean =
    entity.attrString("brand")?.equals("Ring", ignoreCase = true) == true ||
        entity.manufacturer?.equals("Ring", ignoreCase = true) == true

/** The stream or feed words Home Assistant appends to a camera's name. */
private val CAMERA_FEED_SUFFIX = Regex(
    "\\s+(live view|live stream|live|(hd|sd) stream( \\(direct\\))?|stream|snapshot)$",
    RegexOption.IGNORE_CASE,
)

/**
 * The name a camera goes by: its name without the feed Home Assistant
 * appends to it ("Front Door Live view" is the Front Door, "Outdoor
 * camera HD Stream (Direct)" the Outdoor camera). Whether the picture is
 * live is said by the camera's own label, not by its name. Only the
 * display string changes; Home Assistant's registry is never written.
 */
fun displayCameraName(entity: HaEntity): String {
    val name = entity.friendlyName
    return name.replace(CAMERA_FEED_SUFFIX, "").ifBlank { name }
}

/**
 * Front Door live probe (Round 2, 2026-11-02): the exact entity ids
 * from the user's Home Assistant template export (366 entities). The
 * panel's live path is keyed on these ids only — no guessing, no
 * name matching — so a renamed friendly name can never reroute it.
 */
const val FRONT_DOOR_LIVE_VIEW_ID = "camera.front_door_live_view"
const val FRONT_DOOR_SNAPSHOT_ID = "camera.front_door_snapshot"
const val FRONT_DOOR_LIVE_STREAM_SWITCH_ID = "switch.front_door_live_stream"
const val FRONT_DOOR_BATTERY_SENSOR_ID = "sensor.front_door_battery"

/**
 * The doorbell's second camera entity. It was long taken for ring-mqtt's
 * live camera, and it is not one: its siblings in the entity list
 * (button.front_door_69a1_identify, two switch.front_door_69a1_mute, the
 * event.front_door_69a1 pair) are what the HomeKit Device integration
 * creates, and that integration's cameras serve still snapshots, never a
 * stream. Frames from its proxy were never confirmed live on a phone.
 * It stays as a picture source behind the live stream; the name of the
 * constant is historical.
 */
const val FRONT_DOOR_RING_MQTT_CAMERA_ID = "camera.front_door_69a1"

/**
 * ring-mqtt's RTSP listener on the Home Assistant host (port 8554) and
 * the front door's live feed path. This is the only transport Haven has
 * that is the doorbell's actual live video. The path is the fallback for
 * when [FRONT_DOOR_INFO_SENSOR_ID] does not name one.
 */
const val FRONT_DOOR_RTSP_PORT = 8554
const val FRONT_DOOR_RTSP_PATH = "5c475ecd13a7_live"

/** ring-mqtt's info sensor for the doorbell; its attributes carry the stream's RTSP address. */
const val FRONT_DOOR_INFO_SENSOR_ID = "sensor.front_door_info"

/**
 * The live feed's path as ring-mqtt itself reports it. The info sensor
 * carries the stream address as an attribute, addressed to the add-on's
 * internal hostname (rtsp://03cabcc9-ring-mqtt:8554/<id>_live); only the
 * path is usable from a phone, so only the path is taken. Any attribute
 * holding an rtsp:// address that ends in _live counts, whatever its key
 * is called. Null when there is none, and the caller keeps the default.
 */
fun ringMqttLivePath(attributes: Map<String, Any?>?): String? {
    if (attributes == null) return null
    for (value in attributes.values) {
        val url = (value as? String)?.trim() ?: continue
        if (!url.startsWith("rtsp://", ignoreCase = true) || !url.endsWith("_live")) continue
        val path = url.substringAfter("://").substringAfter('/', "")
        if (path.isNotEmpty() && path.none { it.isWhitespace() }) return path
    }
    return null
}

/**
 * Round 21, item 3: the ring-mqtt RTSP URL for the front door,
 * derived from the HA instance URL's host. RTSP exists only on
 * the LAN Pi, so a non-local host (Nabu Casa remote UI, any
 * public hostname) yields null — no RTSP attempt off-LAN, and no
 * host is ever guessed: the URL comes from the stored instance
 * URL the app already connects through. IPv6 hosts are bracketed.
 * Pure (no Android types) so it is unit tested.
 */
fun frontDoorRtspUrl(instanceUrl: String?, path: String = FRONT_DOOR_RTSP_PATH): String? {
    val host = connectionHostOf(instanceUrl) ?: return null
    if (!isLocalAddress(instanceUrl)) return null
    val hostPart = if (host.contains(":")) "[$host]" else host
    return "rtsp://$hostPart:$FRONT_DOOR_RTSP_PORT/$path"
}

/**
 * What the Front Door panel says when it is not showing live video,
 * so a recording is never mistaken for the door right now and the
 * reason is on the screen. [rtspUrl] null means the phone is not on the
 * home network (the stream exists only there); otherwise the stream was
 * tried at that address and did not answer, which is worth naming:
 * the usual cause is the ring-mqtt add-on's RTSP port not being opened
 * to the network.
 */
fun frontDoorNotLiveLine(rtspUrl: String?): String =
    if (rtspUrl == null) "Live view needs your home network · Showing latest recording"
    else "Live stream not answering at ${rtspUrl.substringAfter("://").substringBefore('/')} · Showing latest recording"

/**
 * Home Assistant's own camera for ring-mqtt's live stream: a Generic
 * Camera whose stream source is rtsp://127.0.0.1:8554/<doorbell>_live,
 * ring-mqtt's listener as seen from the Home Assistant host (added
 * 2026-11-23). Home Assistant serves it as HLS over its ordinary HTTP
 * API, so unlike the RTSP port it travels through Nabu Casa: it is the
 * front door's live view away from home. The Generic Camera setup names
 * the entity after the stream's host, hence the id.
 */
const val FRONT_DOOR_RELAY_CAMERA_ID = "camera.127_0_0_1"

/**
 * The camera that relays ring-mqtt's live stream through Home Assistant,
 * or null when there is none or it is down: the known id, else a camera
 * whose name says ring-mqtt (so renaming it in Home Assistant to, say,
 * "Front Door (ring-mqtt)" keeps it found).
 */
fun frontDoorRelayCamera(states: Map<String, HaEntity>): String? {
    fun usable(e: HaEntity?) = e != null && e.state != "unavailable" && e.state != "unknown"
    if (usable(states[FRONT_DOOR_RELAY_CAMERA_ID])) return FRONT_DOOR_RELAY_CAMERA_ID
    return states.values.firstOrNull {
        it.domain == "camera" && usable(it) &&
            it.friendlyName.replace('-', ' ').contains("ring mqtt", ignoreCase = true)
    }?.entityId
}

/**
 * Front-door source resolution (camera fallback round, 2026-11-04):
 * the panel and the hero quadrant share this one rule so they can
 * never disagree about which camera is the front door. The
 * ring-mqtt camera (camera.front_door_69a1) leads whenever it is
 * present and not "unavailable" — it is the only source whose
 * frames may earn the LIVE badge. When it is unavailable (or
 * absent from states), the official camera.front_door_live_view
 * is the fallback, provided it is present and not "unavailable".
 * Both down (or absent) resolves to null: the caller must show an
 * honest unavailable state, never a blank or stale frame. Pure
 * (no Android types) so it is unit tested; callers re-derive it
 * from the live states map on every state change, so a recovered
 * 69a1 automatically becomes the lead source again. Only the
 * literal "unavailable" state triggers the fallback — any other
 * state still tries 69a1 first, and a stream error there keeps
 * the existing error path.
 */
fun resolveFrontDoorCameraSource(states: Map<String, HaEntity>): String? {
    val ringMqtt = states[FRONT_DOOR_RING_MQTT_CAMERA_ID]
    if (ringMqtt != null && ringMqtt.state != "unavailable") return FRONT_DOOR_RING_MQTT_CAMERA_ID
    val liveView = states[FRONT_DOOR_LIVE_VIEW_ID]
    if (liveView != null && liveView.state != "unavailable") return FRONT_DOOR_LIVE_VIEW_ID
    return null
}

/**
 * Feedback round 18, item 7: the per-entity map CameraQuadrant
 * feeds the shared resolver above, now that the quadrant collects
 * entity flows instead of the whole states map. Only the
 * front-door trio is ever present (absent entities are simply
 * omitted — the resolver treats a missing entry exactly like an
 * unavailable one, so the fallback behavior is unchanged).
 */
fun frontDoorResolverStates(ringMqtt: HaEntity?, liveView: HaEntity?): Map<String, HaEntity> =
    buildMap {
        ringMqtt?.let { put(FRONT_DOOR_RING_MQTT_CAMERA_ID, it) }
        liveView?.let { put(FRONT_DOOR_LIVE_VIEW_ID, it) }
    }

/** Watchdog cap for a Front Door live session: the switch auto-offs after this long even if the panel stays open. */
const val FRONT_DOOR_LIVE_MAX_MS = 10 * 60 * 1000L

/**
 * The LIVE badge rule, as a pure function so it is unit tested: the
 * badge shows ONLY while frames are actually rendering on a live
 * path — a WebRTC first frame (remote description applied), or
 * frames from the ring-mqtt live camera (camera.front_door_69a1)
 * while the live-stream switch is confirmed on. Round 5 honesty
 * correction (supersedes the #93 rule): the old MJPEG+switch-on
 * rule is WRONG — the live_view proxy serves the last motion
 * recording even with the switch on, so live_view proxy frames
 * must be labeled as recordings and must NEVER feed this
 * function's mjpegFrames argument; the panel feeds it only with
 * 69a1 live-stream frames. Frames with the switch off are the
 * recording path, so they never earn the badge. No frames, no
 * badge, regardless of what was attempted.
 *
 * Round 21, item 3: RTSP frames earn the badge too — the ring-mqtt
 * RTSP feed IS the live doorbell feed (not a recording), so a first
 * frame from it is live proof on its own, with no switch involved
 * (ring-mqtt serves RTSP independently of HA's live-stream switch).
 */
fun frontDoorShowsLiveBadge(
    webrtcFrames: Boolean,
    mjpegFrames: Boolean,
    liveSwitchOn: Boolean,
    rtspFrames: Boolean = false,
): Boolean = webrtcFrames || rtspFrames || (mjpegFrames && liveSwitchOn)

/**
 * Whether the Front Door quadrant on Home may wear the LIVE badge. The
 * quadrant draws snapshots, so it has no frames of its own to go by: it
 * reads LIVE only once the panel's live stream has actually produced a
 * frame this session ([rtspProven], cleared again on a stream error).
 * Which entity the picture comes from does not earn the badge, and
 * neither does an attempt.
 */
fun frontDoorQuadrantShowsLive(resolvedSource: String?, streamError: Boolean, rtspProven: Boolean = false): Boolean =
    resolvedSource != null && rtspProven && !streamError

/**
 * Pulls the HLS playlist URL out of a WS `camera/stream` result message.
 * Pure (JSONObject only) so it is unit tested on the JVM. Returns null
 * when HA reports failure, when the result has no URL, or when the URL
 * is blank — all three mean "HA cannot stream this camera", and the
 * caller falls back to the MJPEG recording instead of guessing.
 */
fun parseHlsStreamUrl(msg: org.json.JSONObject): String? {
    if (!msg.optBoolean("success", false)) return null
    return msg.optJSONObject("result")?.optString("url")?.ifEmpty { null }
}

/**
 * Human-readable diagnosis of a WS `camera/stream` response that did
 * NOT yield a URL (see [parseHlsStreamUrl]). Pure (JSONObject only)
 * so it is unit tested on the JVM. Returns null when the message DID
 * yield a URL: the success path shows nothing, and the URL itself is
 * never printed anywhere (it carries a short-lived token). Otherwise
 * it names what HA actually said: its error code/message when the
 * message carries an `error` object, "success, but result had no url"
 * when success=true with no URL, or the truncated raw JSON for
 * anything else. This closes the diagnostic gap behind the 2026-10-29
 * verdict video's "HLS: no stream URL": the old code discarded HA's
 * error, so a rejected command and a genuinely unstreamable camera
 * looked identical. The fullscreen error UI shows this line (error
 * path only), so the next verdict can pinpoint the HA-side reason.
 */
fun diagnoseHlsStreamMessage(msg: org.json.JSONObject): String? {
    if (parseHlsStreamUrl(msg) != null) return null
    val error = msg.optJSONObject("error")
    if (error != null) {
        val code = error.optString("code").takeIf { it.isNotEmpty() }
        val message = error.optString("message").takeIf { it.isNotEmpty() }
        return "HA error" + (code?.let { " $it" } ?: "") + (message?.let { ": $it" } ?: "")
    }
    if (msg.optBoolean("success", false)) return "success, but result had no url"
    return "unexpected response: ${msg.toString().take(240)}"
}

/**
 * Whether HA's camera entity advertises stream support: the
 * SUPPORT_STREAM bit (2) of `supported_features`. HA's frontend
 * gates HLS eligibility on it, so it is shown as a hint on the
 * fullscreen error UI — a camera that does not advertise stream
 * support will not get an HLS URL no matter how correct the request
 * is. Missing attribute -> false (no claim made).
 */
fun cameraSupportsStream(entity: HaEntity): Boolean =
    (entity.attrInt("supported_features") ?: 0) and 2 != 0

/** What the fullscreen viewer shows: connecting, live frames, or an explicit failure. */
sealed class CameraStreamState {
    /** No frame yet: the viewer shows the cached last frame dimmed + loading, or a spinner. */
    data object Connecting : CameraStreamState()

    /** A live frame. The viewer displays it and stores it in the last-frame cache. */
    data class Playing(val frame: android.graphics.Bitmap) : CameraStreamState()

    /**
     * The stream could not start or stalled. The viewer must show this as an
     * explicit error + Retry — never a silent still that looks live.
     */
    data class Error(val message: String) : CameraStreamState()
}

/**
 * Last-frame memory cache: entity id -> last live frame. Session-scoped
 * (lives in the ViewModel), capped so four cameras can't grow it without
 * bound. TRANSIENT ONLY: frames are never written to disk, and the viewer
 * shows a cached frame dimmed + "Connecting…" only while a stream is
 * starting or restarting — it yields to [CameraStreamState.Playing] or
 * [CameraStreamState.Error] and never masks a dead stream.
 *
 * Generic over the frame type so the eviction policy is unit tested without
 * Android Bitmaps; the app uses [CameraFrameCache].
 */
class FrameCache<T>(private val maxEntries: Int = 4) {
    private val map = object : LinkedHashMap<String, T>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>?): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun put(key: String, frame: T) {
        map[key] = frame
    }

    @Synchronized
    fun get(key: String): T? = map[key]

    @Synchronized
    fun size(): Int = map.size
}

/** The app's per-camera last-frame cache. */
typealias CameraFrameCache = FrameCache<android.graphics.Bitmap>

/**
 * Incremental multipart/x-mixed-replace parser. Feed raw response bytes in
 * any chunking; complete JPEG frames come back as byte arrays, partial data
 * stays buffered until its closing boundary arrives.
 *
 * Handles both part shapes: with Content-Length (frame ends at the length)
 * and without (frame ends at the next boundary). A boundary token may be
 * given with or without its leading dashes; the stream itself uses
 * `--<token>` delimiters.
 *
 * Performance round (2026-11-28): the previous parser kept every byte as a
 * boxed `Byte` in an ArrayDeque and copied the WHOLE buffer into a fresh
 * array on every extraction attempt — O(n²) work and tens of thousands of
 * allocations per 200 KB frame, which is where the live-view stutter and
 * GC pauses came from. This version keeps one growable [ByteArray] with a
 * read offset, scans in place, and only allocates the frame it returns.
 * Behaviour (and the unit tests) are unchanged.
 */
class MjpegParser(boundaryToken: String) {
    private val boundary: ByteArray =
        ("--" + boundaryToken.trimStart('-')).toByteArray(Charsets.ISO_8859_1)
    private var buf = ByteArray(64 * 1024)
    private var start = 0 // first unread byte
    private var end = 0 // one past the last buffered byte

    /** Feed a chunk; returns every complete JPEG frame found in it. */
    fun feed(chunk: ByteArray): List<ByteArray> = feed(chunk, 0, chunk.size)

    /** Feed [len] bytes of [chunk] from [off] (lets the reader reuse one read buffer). */
    fun feed(chunk: ByteArray, off: Int, len: Int): List<ByteArray> {
        if (len <= 0) return emptyList()
        append(chunk, off, len)
        var frames: MutableList<ByteArray>? = null
        while (true) {
            val frame = takeFrame() ?: break
            (frames ?: mutableListOf<ByteArray>().also { frames = it }).add(frame)
        }
        return frames ?: emptyList()
    }

    private fun append(src: ByteArray, off: Int, len: Int) {
        if (end + len > buf.size) {
            val live = end - start
            if (live + len <= buf.size && start > 0) {
                // Compact in place.
                System.arraycopy(buf, start, buf, 0, live)
            } else {
                var cap = buf.size
                while (cap < live + len) cap *= 2
                val grown = ByteArray(cap)
                System.arraycopy(buf, start, grown, 0, live)
                buf = grown
            }
            start = 0
            end = live
        }
        System.arraycopy(src, off, buf, end, len)
        end += len
    }

    private fun indexOf(needle: ByteArray, from: Int): Int {
        val last = end - needle.size
        val first = needle[0]
        var i = from
        outer@ while (i <= last) {
            if (buf[i] != first) { i++; continue }
            for (j in 1 until needle.size) {
                if (buf[i + j] != needle[j]) { i++; continue@outer }
            }
            return i
        }
        return -1
    }

    /** Extracts one complete frame, or null when more bytes are needed. */
    private fun takeFrame(): ByteArray? {
        while (true) {
            // Skip anything before the boundary (preamble / garbage).
            val first = indexOf(boundary, start)
            if (first < 0) {
                // Keep a tail that could be the start of a boundary split across chunks.
                val keep = boundary.size
                if (end - start > keep) start = end - keep
                return null
            }
            start = first
            var pos = start + boundary.size
            // A closing delimiter (`--boundary--`) ends the stream: no more frames.
            if (end >= pos + 2 && buf[pos] == DASH && buf[pos + 1] == DASH) return null
            // Skip the line break after the boundary.
            if (end >= pos + 2 && buf[pos] == CR && buf[pos + 1] == LF) {
                pos += 2
            } else if (end >= pos + 1 && buf[pos] == LF) {
                pos += 1
            } else if (end <= pos) {
                return null
            }
            val headerEnd = findHeaderEnd(pos) ?: return null
            val contentLength = parseContentLength(pos, headerEnd)
            if (contentLength != null) {
                if (end < headerEnd + contentLength) return null
                val frame = buf.copyOfRange(headerEnd, headerEnd + contentLength)
                start = headerEnd + contentLength
                return frame
            }
            // No length: the frame ends at the next boundary.
            val next = indexOf(boundary, headerEnd)
            if (next < 0) return null
            var stop = next
            if (stop - 2 >= headerEnd && buf[stop - 2] == CR && buf[stop - 1] == LF) {
                stop -= 2
            } else if (stop - 1 >= headerEnd && buf[stop - 1] == LF) {
                stop -= 1
            }
            val frame = buf.copyOfRange(headerEnd, stop)
            start = next
            return frame
        }
    }

    private fun findHeaderEnd(from: Int): Int? {
        var i = from
        while (i + 3 < end) {
            if (buf[i] == CR && buf[i + 1] == LF && buf[i + 2] == CR && buf[i + 3] == LF) return i + 4
            i++
        }
        // Lone-LF streams: blank line is \n\n.
        i = from
        while (i + 1 < end) {
            if (buf[i] == LF && buf[i + 1] == LF) return i + 2
            i++
        }
        return null
    }

    private fun parseContentLength(from: Int, to: Int): Int? {
        val headers = String(buf, from, to - from, Charsets.ISO_8859_1)
        return headers.lineSequence()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.toIntOrNull()
    }

    private companion object {
        const val CR: Byte = 13
        const val LF: Byte = 10
        const val DASH: Byte = 45
    }
}

/**
 * The last-recording sibling of a live camera entity, for the clips
 * timeline: HA core's Ring integration exposes recordings as a
 * separate `camera.<name>_last_recording` entity (disabled by
 * default), not as a browsable clip list. Match rule, deliberately
 * strict: same id prefix (the live id minus `_live_view`, else minus
 * its last segment) AND "recording" in the id, never the live entity
 * itself. A non-matching camera yields null — a wrong device's
 * recording on this timeline would be worse than none. Deterministic
 * (lowest id wins) when several match.
 */
fun findRecordingSibling(liveId: String, entities: Collection<HaEntity>): HaEntity? {
    val prefix = if (liveId.endsWith("_live_view")) liveId.removeSuffix("_live_view")
        else liveId.substringBeforeLast("_", liveId)
    return entities
        .filter { it.domain == "camera" && it.entityId != liveId }
        .filter { it.entityId.startsWith(prefix) && "recording" in it.entityId }
        .minByOrNull { it.entityId }
}

/**
 * Formats an HA ISO timestamp (last_changed / last_updated) as
 * "Oct 29, 7:23 AM" in [zone]; unparseable or null input yields null
 * (the caller shows no timestamp rather than a raw ISO string).
 */
fun formatHaTimestamp(iso: String?, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String? {
    if (iso.isNullOrEmpty()) return null
    return try {
        val instant = java.time.OffsetDateTime.parse(iso).toInstant()
        java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withZone(zone).format(instant)
    } catch (e: Exception) {
        null
    }
}

/**
 * Whether a camera runs on a battery: something on its own device reports
 * a battery (a battery sensor, or a battery level). Such a camera wakes,
 * and spends charge, every time it is asked for a picture or a stream, so
 * Haven asks it far less often (see [BATTERY_SNAPSHOT_INTERVAL_MS]).
 */
fun cameraHasBattery(cameraId: String, states: Map<String, HaEntity>, entityDevices: Map<String, String>): Boolean {
    if (states[cameraId]?.attributes?.containsKey("battery_level") == true) return true
    val device = entityDevices[cameraId] ?: return false
    return states.values.any { e ->
        entityDevices[e.entityId] == device && e.entityId != cameraId &&
            (e.attrString("device_class") == "battery" || e.attributes.containsKey("battery_level"))
    }
}

/** A battery camera's tile picture is refreshed at most this often. */
const val BATTERY_SNAPSHOT_INTERVAL_MS = 15 * 60_000L

/** A battery camera's live view pauses after this long, until asked to keep going. */
const val BATTERY_LIVE_LIMIT_MS = 2 * 60_000L

/**
 * The live transports a camera offers when Home Assistant has not said
 * (its capabilities did not arrive in time): what its state advertises,
 * WebRTC for a Ring camera (the only live path Ring has), HLS for a
 * camera that can stream at all, nothing otherwise.
 */
fun fallbackStreamTypes(entity: HaEntity): List<String> {
    entity.attrString("frontend_stream_type")?.let { return listOf(it) }
    if (isRingCamera(entity)) return listOf("web_rtc")
    return if (cameraSupportsStream(entity)) listOf("hls") else emptyList()
}
