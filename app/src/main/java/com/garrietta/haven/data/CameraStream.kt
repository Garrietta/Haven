package com.garrietta.haven.data

/**
 * Live camera playback: the MJPEG proxy stream, plus HA's HLS stream path
 * for Ring.
 *
 * Root cause of the still-image bug: the fullscreen viewer fetched ONE frame
 * from `/api/camera_proxy/<entity>` (a single JPEG snapshot) and displayed
 * it until the user hit Refresh. It never opened a stream at all — no silent
 * fallback, just snapshot-only code. The live path is
 * `/api/camera_proxy_stream/<entity>`, which yields a multipart/x-mixed-replace
 * MJPEG stream for every camera HA can serve: native MJPEG cameras stream
 * directly. Ring is the exception: its integration overrides the MJPEG
 * handler and builds the stream from the most recent motion recording
 * (ffmpeg transcodes the recording), so the frames are a clip, not live.
 *
 * The true-live path for Ring through HA is the stream component, not the
 * proxy: WS `camera/stream` returns an HLS playlist, which is what HA's
 * own frontend plays when it shows this entity live (Garrett confirmed the
 * entity serves live, and that it is the SAME entity id Haven uses — the
 * difference was never the entity, it was the path). CameraFullscreen
 * therefore tries HLS first for Ring and falls back to the MJPEG recording
 * with the "Last recording" label when HA cannot stream it. WebRTC remains
 * the fallback if HLS provably cannot deliver, and is NOT built here: a
 * failed HLS attempt must be reported honestly, not silently replaced.
 * One endpoint, all four curated cameras; only the presentation differs.
 *
 * [MjpegParser] is pure byte handling (no Android types) so it is unit
 * tested on the JVM; decoding to Bitmap happens in [HaClient.streamCamera].
 */

/**
 * True when this camera is a Ring camera. Ring's MJPEG proxy stream serves
 * its last motion recording, not live video (see the header note), so the
 * viewer must present it as "Last recording" whenever it is NOT playing
 * the HLS stream — never as live.
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
