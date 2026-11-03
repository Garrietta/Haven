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
    fun parseEventAnswer() {
        // Round 5 Bug B: the answer arrives as a subscription event,
        // correlated by command id, with the SDP in event.answer.
        val msg = JSONObject(
            """{"id": 7, "type": "event",
                "event": {"type": "answer", "answer": "v=0\r\n...", "session_id": "sess-9"}}""",
        )
        val parsed = parseWebRtcEventAnswer(msg)
        assertEquals("v=0\r\n...", parsed?.answerSdp)
        assertEquals("sess-9", parsed?.sessionId)
        assertNull(parsed?.error)
    }

    @Test
    fun parseEventAnswerFallsBackToKnownSessionId() {
        val msg = JSONObject(
            """{"id": 7, "type": "event",
                "event": {"type": "answer", "answer": "v=0\r\n..."}}""",
        )
        val parsed = parseWebRtcEventAnswer(msg, knownSessionId = "from-result")
        assertEquals("from-result", parsed?.sessionId)
    }

    @Test
    fun parseEventCandidateIsNotAnAnswer() {
        // Candidates keep the subscription open: they parse to null,
        // never to an error — the caller only closes on the answer.
        val msg = JSONObject(
            """{"id": 7, "type": "event",
                "event": {"type": "candidate", "candidate": "candidate:..."}}""",
        )
        assertNull(parseWebRtcEventAnswer(msg))
    }

    @Test
    fun parseEventNonAnswerTypesAreIgnored() {
        val msg = JSONObject(
            """{"id": 7, "type": "event",
                "event": {"event_type": "state_changed"}}""",
        )
        assertNull(parseWebRtcEventAnswer(msg))
        assertNull(parseWebRtcEventAnswer(JSONObject("""{"id": 7, "type": "result", "success": true}""")))
    }

    @Test
    fun parseEventAnswerMissingSdpIsError() {
        val msg = JSONObject(
            """{"id": 7, "type": "event",
                "event": {"type": "answer", "session_id": "s1"}}""",
        )
        val parsed = parseWebRtcEventAnswer(msg)
        assertNull(parsed?.answerSdp)
        assertEquals("answer event missing SDP", parsed?.error)
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
