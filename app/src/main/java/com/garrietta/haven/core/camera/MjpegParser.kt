package com.garrietta.haven.core.camera

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
