package com.garrietta.haven.data
import org.json.JSONObject
data class WebRtcAnswer(val answerSdp: String?, val sessionId: String?, val error: String?)
fun parseWebRtcEventAnswer(msg: JSONObject, knownSessionId: String? = null): WebRtcAnswer? = null
fun parseWebRtcAnswer(msg: JSONObject): WebRtcAnswer = WebRtcAnswer(null, null, null)
