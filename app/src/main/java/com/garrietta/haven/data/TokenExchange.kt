package com.garrietta.haven.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Reliability round: one POST to [url]'s /auth/token, shared by the
 * app (sign-in code exchange, refresh) and the Device Controls
 * service (which used to have no refresh path at all, so its session
 * died 30 minutes after every OAuth refresh in the app). The answer is
 * classified by [classifyTokenResponse] — only an explicit refusal is
 * REJECTED — and an OK answer is written to [tokenStore] (access
 * token, refresh token when one is returned, expiry).
 *
 * Uses a client WITHOUT the REST authenticator: a token request must
 * never recurse into another refresh.
 */
suspend fun exchangeTokenAt(
    url: String,
    body: FormBody,
    tokenStore: TokenStore,
    http: OkHttpClient = HaClient.sharedHttp,
): TokenRefreshOutcome = withContext(Dispatchers.IO) {
    try {
        val req = Request.Builder().url("$url/auth/token").post(body).build()
        val (code, text) = http.newCall(req).execute().use { resp -> resp.code to resp.body?.string() }
        val outcome = classifyTokenResponse(code, text)
        if (outcome != TokenRefreshOutcome.OK) return@withContext outcome
        val json = JSONObject(text ?: "")
        tokenStore.accessToken = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        if (refresh.isNotEmpty() && refresh != "null") tokenStore.refreshToken = refresh
        val expiresIn = json.optLong("expires_in", 0L)
        tokenStore.tokenExpiresAtMs = if (expiresIn > 0) System.currentTimeMillis() + expiresIn * 1000L else 0L
        TokenRefreshOutcome.OK
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        TokenRefreshOutcome.UNREACHABLE
    }
}

/**
 * Refresh-token grant against each of [urls] in turn, stopping at the
 * first definitive answer. REJECTED when no refresh token is stored
 * (long-lived-token logins cannot be renewed).
 */
suspend fun refreshTokenVia(
    urls: List<String>,
    tokenStore: TokenStore,
    clientId: String,
    http: OkHttpClient = HaClient.sharedHttp,
): TokenRefreshOutcome {
    val refresh = tokenStore.refreshToken ?: return TokenRefreshOutcome.REJECTED
    var outcome = TokenRefreshOutcome.UNREACHABLE
    for (url in urls.distinct()) {
        outcome = exchangeTokenAt(
            url,
            FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .add("client_id", clientId)
                .build(),
            tokenStore,
            http,
        )
        if (outcome != TokenRefreshOutcome.UNREACHABLE) break
    }
    return outcome
}
