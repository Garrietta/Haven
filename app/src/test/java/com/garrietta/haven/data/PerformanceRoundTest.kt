package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Performance round (2026-11-28): the rewritten MJPEG parser and WebRTC signalling helpers. */
class PerformanceRoundTest {

    private fun part(payload: ByteArray, boundary: String, withLength: Boolean): ByteArray {
        val header = buildString {
            append("--$boundary\r\nContent-Type: image/jpeg\r\n")
            if (withLength) append("Content-Length: ${payload.size}\r\n")
            append("\r\n")
        }.toByteArray(Charsets.ISO_8859_1)
        return header + payload + "\r\n".toByteArray(Charsets.ISO_8859_1)
    }

    @Test
    fun parser_largeFramesInRandomChunks_allFramesExact() {
        val rnd = Random(42)
        val frames = List(40) { i -> ByteArray(150_000 + i * 1_000) { (it * 31 + i).toByte() } }
        for (withLength in listOf(true, false)) {
            val stream = frames.fold(ByteArray(0)) { acc, f -> acc + part(f, "frame", withLength) } +
                "--frame--\r\n".toByteArray(Charsets.ISO_8859_1)
            val parser = MjpegParser("frame")
            val out = mutableListOf<ByteArray>()
            var pos = 0
            while (pos < stream.size) {
                val n = minOf(stream.size - pos, 1 + rnd.nextInt(20_000))
                out += parser.feed(stream, pos, n)
                pos += n
            }
            assertEquals("withLength=$withLength", frames.size, out.size)
            frames.indices.forEach { assertTrue("frame $it", frames[it].contentEquals(out[it])) }
        }
    }

    @Test
    fun parser_isLinearNotQuadratic() {
        // 120 frames × 200 KB in 16 KB chunks: the old boxed-deque parser took
        // many seconds here; the array parser is well under one.
        val frame = ByteArray(200_000) { it.toByte() }
        val one = part(frame, "b", withLength = true)
        val parser = MjpegParser("b")
        val chunk = 16 * 1024
        val started = System.nanoTime()
        var count = 0
        repeat(120) {
            var pos = 0
            while (pos < one.size) {
                val n = minOf(chunk, one.size - pos)
                count += parser.feed(one, pos, n).size
                pos += n
            }
        }
        val ms = (System.nanoTime() - started) / 1_000_000
        assertEquals(120, count)
        assertTrue("parser took ${ms}ms", ms < 1_500)
    }

    @Test
    fun iceUrls_stringOrArray() {
        assertEquals(listOf("stun:a:3478"), parseIceUrls(JSONObject("""{"urls":"stun:a:3478"}""")))
        assertEquals(
            listOf("turn:b:3478", "turns:b:5349"),
            parseIceUrls(JSONObject("""{"urls":["turn:b:3478","turns:b:5349"],"username":"u","credential":"c"}""")),
        )
        assertEquals(emptyList<String>(), parseIceUrls(JSONObject("""{"username":"u"}""")))
    }
}
