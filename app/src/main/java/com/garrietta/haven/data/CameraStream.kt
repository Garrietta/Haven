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

/**
 * Display name for a camera (Gemini mockup round, 2026-11-01): HA
 * names the Ring camera "Front Door Live view", which reads as a
 * live feed everywhere it appears (tiles, dialog headers, the clips
 * title) while its caption says "Not live" — the naming mismatch
 * from the 2026-11-01 unfolded video. For Ring cameras only, the
 * trailing " Live view" is stripped ("Front Door"), and the
 * recording caption alongside it carries the truth. Native cameras
 * are genuinely live, so their names are untouched. This renames
 * the DISPLAY string only — HA's registry is never written.
 */
fun displayCameraName(entity: HaEntity): String {
    if (!isRingCamera(entity)) return entity.friendlyName
    val name = entity.friendlyName
    val stripped = if (name.endsWith(" live view", ignoreCase = true)) {
        name.dropLast(" live view".length)
    } else {
        name
    }
    return stripped.ifBlank { name }
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
 */
class MjpegParser(boundaryToken: String) {
    private val boundary: ByteArray =
        ("--" + boundaryToken.trimStart('-')).toByteArray(Charsets.ISO_8859_1)
    private val buffer = ArrayDeque<Byte>()

    /** Feed a chunk; returns every complete JPEG frame found in it. */
    fun feed(chunk: ByteArray): List<ByteArray> {
        if (chunk.isEmpty()) return emptyList()
        for (b in chunk) buffer.addLast(b)
        val frames = mutableListOf<ByteArray>()
        while (true) {
            val frame = takeFrame() ?: break
            frames.add(frame)
        }
        return frames
    }

    private fun bytes(): ByteArray {
        val out = ByteArray(buffer.size)
        var i = 0
        for (b in buffer) out[i++] = b
        return out
    }

    private fun indexOf(hay: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty() || hay.size < needle.size) return -1
        outer@ for (i in from..hay.size - needle.size) {
            for (j in needle.indices) {
                if (hay[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    /** Extracts one complete frame, or null when more bytes are needed. */
    private fun takeFrame(): ByteArray? {
        val data = bytes()
        // Skip anything before the first boundary (preamble / garbage).
        val first = indexOf(data, boundary, 0)
        if (first < 0) {
            // Keep a tail that could be the start of a boundary split across chunks.
            trimToTail(data, boundary.size)
            return null
        }
        if (first > 0) {
            drop(first)
            return takeFrame()
        }
        // data starts with the boundary; the part headers follow it.
        var pos = boundary.size
        // A closing delimiter (`--boundary--`) ends the stream: no more frames.
        if (data.size >= pos + 2 && data[pos] == '-'.code.toByte() && data[pos + 1] == '-'.code.toByte()) {
            return null
        }
        // Skip the line break after the boundary.
        if (data.size >= pos + 2 && data[pos] == '\r'.code.toByte() && data[pos + 1] == '\n'.code.toByte()) {
            pos += 2
        } else if (data.size >= pos + 1 && data[pos] == '\n'.code.toByte()) {
            pos += 1
        } else if (data.size <= pos) {
            return null
        }
        // Headers end at a blank line.
        val headerEnd = findHeaderEnd(data, pos) ?: return null
        val headers = String(data, pos, headerEnd - pos, Charsets.ISO_8859_1)
        val bodyStart = headerEnd
        val contentLength = headers.lines()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.toIntOrNull()
        if (contentLength != null) {
            if (data.size < bodyStart + contentLength) return null
            val frame = data.copyOfRange(bodyStart, bodyStart + contentLength)
            drop(bodyStart + contentLength)
            return frame
        }
        // No length: the frame ends at the next boundary.
        val next = indexOf(data, boundary, bodyStart)
        if (next < 0) return null
        var end = next
        // Trim the CRLF that precedes the boundary.
        if (end >= 2 && data[end - 2] == '\r'.code.toByte() && data[end - 1] == '\n'.code.toByte()) {
            end -= 2
        } else if (end >= 1 && data[end - 1] == '\n'.code.toByte()) {
            end -= 1
        }
        val frame = data.copyOfRange(bodyStart, end)
        drop(next)
        return frame
    }

    private fun findHeaderEnd(data: ByteArray, from: Int): Int? {
        var i = from
        while (i + 3 < data.size) {
            if (data[i] == '\r'.code.toByte() && data[i + 1] == '\n'.code.toByte() &&
                data[i + 2] == '\r'.code.toByte() && data[i + 3] == '\n'.code.toByte()
            ) {
                return i + 4
            }
            i++
        }
        // Lone-LF streams: blank line is \n\n.
        i = from
        while (i + 1 < data.size) {
            if (data[i] == '\n'.code.toByte() && data[i + 1] == '\n'.code.toByte()) {
                return i + 2
            }
            i++
        }
        return null
    }

    private fun drop(count: Int) {
        repeat(count) { buffer.removeFirst() }
    }

    /** Keeps only a tail that could hold a boundary split across chunks. */
    private fun trimToTail(data: ByteArray, keep: Int) {
        if (data.size > keep) drop(data.size - keep)
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
