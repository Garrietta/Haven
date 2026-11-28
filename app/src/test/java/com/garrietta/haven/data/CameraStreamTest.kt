package com.garrietta.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests: the parser and cache are pure Kotlin with no Android types. */
class CameraStreamTest {

    private fun mjpegFrame(payload: ByteArray, withLength: Boolean = true): ByteArray {
        val header = if (withLength) {
            "Content-Type: image/jpeg\r\nContent-Length: ${payload.size}\r\n\r\n"
        } else {
            "Content-Type: image/jpeg\r\n\r\n"
        }
        return header.toByteArray(Charsets.ISO_8859_1) + payload
    }

    private fun stream(vararg frames: ByteArray, boundary: String = "frame"): ByteArray {
        val out = mutableListOf<Byte>()
        for (f in frames) {
            out.addAll("--$boundary\r\n".toByteArray(Charsets.ISO_8859_1).toList())
            out.addAll(f.toList())
            out.addAll("\r\n".toByteArray(Charsets.ISO_8859_1).toList())
        }
        out.addAll("--$boundary--\r\n".toByteArray(Charsets.ISO_8859_1).toList())
        return out.toByteArray()
    }

    @Test
    fun twoFramesWithLength_parseInOneFeed() {
        val parser = MjpegParser("frame")
        val frames = parser.feed(stream(mjpegFrame(byteArrayOf(1, 2, 3)), mjpegFrame(byteArrayOf(4, 5))))
        assertEquals(2, frames.size)
        assertTrue(frames[0].contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(frames[1].contentEquals(byteArrayOf(4, 5)))
    }

    @Test
    fun splitAcrossChunks_frameHeldUntilComplete() {
        val parser = MjpegParser("frame")
        val bytes = stream(mjpegFrame(byteArrayOf(9, 8, 7, 6)))
        // First half: headers + part of the payload — no complete frame yet.
        val first = parser.feed(bytes.copyOf(bytes.size / 2))
        assertEquals(0, first.size)
        val rest = parser.feed(bytes.copyOfRange(bytes.size / 2, bytes.size))
        assertEquals(1, rest.size)
        assertTrue(rest[0].contentEquals(byteArrayOf(9, 8, 7, 6)))
    }

    @Test
    fun noContentLength_frameEndsAtNextBoundary() {
        val parser = MjpegParser("frame")
        val frames = parser.feed(
            stream(mjpegFrame(byteArrayOf(1, 1), withLength = false), mjpegFrame(byteArrayOf(2, 2), withLength = false)),
        )
        assertEquals(2, frames.size)
        assertTrue(frames[0].contentEquals(byteArrayOf(1, 1)))
        assertTrue(frames[1].contentEquals(byteArrayOf(2, 2)))
    }

    @Test
    fun boundaryWithDashes_tokenNormalized() {
        val parser = MjpegParser("--frame")
        val frames = parser.feed(stream(mjpegFrame(byteArrayOf(7))))
        assertEquals(1, frames.size)
        assertTrue(frames[0].contentEquals(byteArrayOf(7)))
    }

    @Test
    fun preambleBeforeFirstBoundary_skipped() {
        val parser = MjpegParser("frame")
        val garbage = "HTTP/1.1 200 OK\r\n".toByteArray(Charsets.ISO_8859_1)
        val frames = parser.feed(garbage + stream(mjpegFrame(byteArrayOf(3))))
        assertEquals(1, frames.size)
        assertTrue(frames[0].contentEquals(byteArrayOf(3)))
    }

    @Test
    fun cache_holdsPerCameraAndEvictsOldest() {
        val cache = FrameCache<String>(maxEntries = 2)
        cache.put("a", "frame-a")
        cache.put("b", "frame-b")
        assertEquals("frame-a", cache.get("a"))
        assertEquals("frame-b", cache.get("b"))
        cache.put("c", "frame-c")
        // "a" was least recently used after the get("b") above touched b last.
        assertEquals(null, cache.get("a"))
        assertEquals("frame-b", cache.get("b"))
        assertEquals("frame-c", cache.get("c"))
        assertEquals(2, cache.size())
    }

    @Test
    fun cache_overwriteKeepsLatestFrame() {
        val cache = FrameCache<String>()
        cache.put("cam", "old")
        cache.put("cam", "new")
        assertEquals("new", cache.get("cam"))
        assertEquals(1, cache.size())
    }

    @Test
    fun isRingCamera_readsBrandAttributeOnly() {
        // Ring's proxy stream is its last recording, not live — the
        // viewer labels exactly these cameras "Last recording".
        assertTrue(
            isRingCamera(
                HaEntity("camera.front_door_live_view", "idle", mapOf("brand" to "Ring")),
            ),
        )
        assertTrue(
            isRingCamera(
                HaEntity("camera.front_door_live_view", "idle", mapOf("brand" to "ring")),
            ),
        )
        assertFalse(
            isRingCamera(
                HaEntity("camera.tapo_1_live_view", "idle", mapOf("brand" to "Tapo")),
            ),
        )
        assertFalse(
            isRingCamera(HaEntity("camera.front_door_live_view", "idle", emptyMap())),
        )
    }

    // Front-door source resolution (camera fallback round): the
    // ring-mqtt camera leads whenever it is present and not
    // "unavailable"; otherwise the official live-view camera is
    // the fallback; both down resolves to null.
    private fun frontDoorStates(
        ringMqtt: String?,
        liveView: String?,
    ): Map<String, HaEntity> {
        val states = mutableMapOf<String, HaEntity>()
        if (ringMqtt != null) {
            states[FRONT_DOOR_RING_MQTT_CAMERA_ID] =
                HaEntity(FRONT_DOOR_RING_MQTT_CAMERA_ID, ringMqtt, emptyMap())
        }
        if (liveView != null) {
            states[FRONT_DOOR_LIVE_VIEW_ID] =
                HaEntity(FRONT_DOOR_LIVE_VIEW_ID, liveView, emptyMap())
        }
        return states
    }

    @Test
    fun frontDoorSource_prefersRingMqttWhenAvailable() {
        assertEquals(
            FRONT_DOOR_RING_MQTT_CAMERA_ID,
            resolveFrontDoorCameraSource(frontDoorStates("idle", "idle")),
        )
        assertEquals(
            FRONT_DOOR_RING_MQTT_CAMERA_ID,
            resolveFrontDoorCameraSource(frontDoorStates("streaming", "unavailable")),
        )
    }

    @Test
    fun frontDoorSource_fallsBackToLiveViewWhenRingMqttUnavailable() {
        assertEquals(
            FRONT_DOOR_LIVE_VIEW_ID,
            resolveFrontDoorCameraSource(frontDoorStates("unavailable", "idle")),
        )
    }

    @Test
    fun frontDoorSource_fallsBackToLiveViewWhenRingMqttAbsent() {
        assertEquals(
            FRONT_DOOR_LIVE_VIEW_ID,
            resolveFrontDoorCameraSource(frontDoorStates(null, "idle")),
        )
    }

    @Test
    fun frontDoorSource_nullWhenBothUnavailable() {
        assertEquals(
            null,
            resolveFrontDoorCameraSource(frontDoorStates("unavailable", "unavailable")),
        )
    }

    @Test
    fun frontDoorSource_nullWhenBothAbsent() {
        assertEquals(null, resolveFrontDoorCameraSource(emptyMap()))
    }
}
