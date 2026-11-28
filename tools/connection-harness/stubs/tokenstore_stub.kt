package com.garrietta.haven.data
class TokenStore {
    var instanceUrl: String? = null
    var cloudInstanceUrl: String? = null
    var connectionModeKey: String? = null
    var accessToken: String? = null
    var refreshToken: String? = null
    var tokenExpiresAtMs: Long = 0L
}
