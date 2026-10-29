package com.garrietta.haven.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RingWebRtcTest {

    @Test
    fun parseAnswerAndSession() {
        val msg = JSONObject(
            """{"id": 1, "type": "result", "success": true,
                "result": {"answer": "v=0\r\n...", "session_id": "abc-123"}}""",
        )
        val parsed = parseWebRtcAnswer(msg)
        assertEquals("v=0\r\n...", parsed.answerSdp)
        assertEquals("abc-123", parsed.sessionId)
        assertNull(parsed.error)
    }

    @Test
    fun parseSdpFallback() {
        val msg = JSONObject(
            """{"id": 1, "type": "result", "success": true,
                "result": {"sdp": "v=0\r\n...", "session_id": "s1"}}""",
        )
        val parsed = parseWebRtcAnswer(msg)
        assertEquals("v=0\r\n...", parsed.answerSdp)
        assertEquals("s1", parsed.sessionId)
    }

    @Test
    fun parseMissingAnswerIsError() {
        val msg = JSONObject(
            """{"id": 1, "type": "result", "success": true,
                "result": {"session_id": "s1"}}""",
        )
        val parsed = parseWebRtcAnswer(msg)
        assertNull(parsed.answerSdp)
        assertEquals("answer missing SDP", parsed.error)
    }

    @Test
    fun parseEmptyResultIsError() {
        val msg = JSONObject("""{"id": 1, "type": "result", "success": true}""")
        val parsed = parseWebRtcAnswer(msg)
        assertNull(parsed.answerSdp)
        assertEquals("empty answer", parsed.error)
    }

    @Test
    fun parseFailureCarriesMessage() {
        val msg = JSONObject(
            """{"id": 1, "type": "result", "success": false,
                "error": {"code": "unknown_command", "message": "Unknown command"}}""",
        )
        val parsed = parseWebRtcAnswer(msg)
        assertNull(parsed.answerSdp)
        assertEquals("Unknown command", parsed.error)
    }

    @Test
    fun parseFailureWithoutMessageFallsBack() {
        val msg = JSONObject("""{"id": 1, "type": "result", "success": false}""")
        val parsed = parseWebRtcAnswer(msg)
        assertEquals("offer rejected", parsed.error)
    }
}
