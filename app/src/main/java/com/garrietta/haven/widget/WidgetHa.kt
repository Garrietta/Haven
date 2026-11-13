package com.garrietta.haven.widget

import android.content.Context
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.TokenStore
import com.garrietta.haven.data.parseStateCache
import com.garrietta.haven.data.serializeStateCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Home Assistant for the widgets, which run without the app: the states
 * the app last saved (its cold-start cache, which the widgets read and
 * write too), and REST calls made with the app's own sign-in, refreshing
 * the token when it has run out and trying the local address before the
 * cloud one, as the app does in Auto.
 */
internal object WidgetHa {
    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val CACHE_FILE = "haven_state_cache.json"

    /** The states the app (or a widget) saved last; empty before the first sign-in. */
    fun cachedStates(context: Context): Map<String, HaEntity> =
        runCatching { parseStateCache(File(context.filesDir, CACHE_FILE).readText())?.states }.getOrNull().orEmpty()

    /** Writes [updated] over their entries in the saved states, keeping the rest. */
    private fun storeStates(context: Context, updated: List<HaEntity>) {
        if (updated.isEmpty()) return
        runCatching {
            val file = File(context.filesDir, CACHE_FILE)
            val data = parseStateCache(file.readText()) ?: return
            val states = data.states + updated.associateBy { it.entityId }
            file.writeText(serializeStateCache(states, data.areas, data.entityAreas))
        }
    }

    /** Fetches the given entities' states and saves them; what could not be fetched keeps its saved state. */
    suspend fun refresh(context: Context, entityIds: Collection<String>) = withContext(Dispatchers.IO) {
        val fresh = entityIds.mapNotNull { id ->
            val (code, body) = request(context, "GET", "/api/states/$id", null)
            if (code == 200 && body != null) runCatching { HaEntity.fromJson(JSONObject(body)) }.getOrNull() else null
        }
        storeStates(context, fresh)
    }

    /** Calls a service; true when Home Assistant accepted it. */
    suspend fun callService(context: Context, domain: String, service: String, data: JSONObject): Boolean =
        withContext(Dispatchers.IO) { request(context, "POST", "/api/services/$domain/$service", data).first in 200..299 }

    /** Saves a guessed state at once, so the widget answers a tap before Home Assistant does. */
    fun assume(context: Context, entity: HaEntity) = storeStates(context, listOf(entity))

    private fun request(context: Context, method: String, path: String, body: JSONObject?): Pair<Int, String?> {
        val tokens = TokenStore(context)
        val urls = listOfNotNull(tokens.instanceUrl, tokens.cloudInstanceUrl).filter { it.isNotBlank() }.distinct()
        for (url in urls) {
            freshToken(tokens, url) ?: continue
            val result = send(url, tokens.accessToken ?: continue, method, path, body)
            if (result.first == 401 && refresh(tokens, url)) {
                return send(url, tokens.accessToken ?: return result, method, path, body)
            }
            if (result.first != 0) return result
        }
        return 0 to null
    }

    private fun send(url: String, token: String, method: String, path: String, body: JSONObject?): Pair<Int, String?> =
        try {
            val payload = body?.toString()?.toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(url.trimEnd('/') + path).header("Authorization", "Bearer $token")
                .method(method, payload).build()
            http.newCall(req).execute().use { it.code to it.body?.string() }
        } catch (e: Exception) {
            0 to null
        }

    /** The access token, refreshed first when it has run out or is about to. */
    private fun freshToken(tokens: TokenStore, url: String): String? {
        val expires = tokens.tokenExpiresAtMs
        if (expires > 0 && System.currentTimeMillis() > expires - 60_000) refresh(tokens, url)
        return tokens.accessToken
    }

    private fun refresh(tokens: TokenStore, url: String): Boolean {
        val refreshToken = tokens.refreshToken ?: return false
        return try {
            val form = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", HavenViewModel.OAUTH_CLIENT_ID)
                .build()
            http.newCall(Request.Builder().url(url.trimEnd('/') + "/auth/token").post(form).build()).execute().use { resp ->
                val json = JSONObject(resp.body?.string() ?: return false)
                val access = json.optString("access_token").takeIf { resp.isSuccessful && it.isNotEmpty() } ?: return false
                tokens.accessToken = access
                json.optString("refresh_token").takeIf { it.isNotEmpty() }?.let { tokens.refreshToken = it }
                json.optLong("expires_in", 0L).takeIf { it > 0 }?.let { tokens.tokenExpiresAtMs = System.currentTimeMillis() + it * 1000 }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
