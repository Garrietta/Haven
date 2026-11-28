package com.garrietta.haven.core.net

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.garrietta.haven.core.auth.AuthManager
import com.garrietta.haven.core.model.HistoryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.json.JSONArray
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime

/** REST side of Home Assistant: snapshots, history, images. Bearer only ever goes to the live instance. */
class HaRest(
    baseHttp: OkHttpClient,
    private val auth: AuthManager,
    private val connection: HaConnection,
) {
    /** 401 → refresh once and retry (OkHttp authenticator runs on a worker thread). */
    val http: OkHttpClient = baseHttp.newBuilder().authenticator(object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.priorResponse != null) return null
            val fresh = runBlocking { auth.accessToken(forceRefresh = true) } ?: return null
            return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
        }
    }).build()

    val streamHttp: OkHttpClient by lazy { http.newBuilder().readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS).build() }

    val baseUrl: String? get() = connection.activeBaseUrl

    suspend fun token(): String? = auth.accessToken()

    fun resolve(path: String): String? =
        if (path.startsWith("http://") || path.startsWith("https://")) path else baseUrl?.let { it + path }

    fun isInstanceUrl(url: String): Boolean = baseUrl?.let { url.startsWith(it) } == true

    suspend fun request(url: String): Request? {
        val builder = Request.Builder().url(url)
        if (isInstanceUrl(url)) builder.header("Authorization", "Bearer ${token() ?: return null}")
        return builder.build()
    }

    /** Camera still, scaled server-side to ~[widthPx] (HA supports ?width=). */
    suspend fun cameraSnapshot(entityId: String, widthPx: Int = 0): ByteArray? = withContext(Dispatchers.IO) {
        val base = baseUrl ?: return@withContext null
        val w = if (widthPx > 0) "width=$widthPx&" else ""
        val req = request("$base/api/camera_proxy/$entityId?${w}t=${System.currentTimeMillis()}") ?: return@withContext null
        try {
            http.newCall(req).execute().use { if (it.isSuccessful) it.body?.bytes() else null }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun image(path: String, maxWidth: Int = 0): Bitmap? = withContext(Dispatchers.IO) {
        val url = resolve(path) ?: return@withContext null
        val req = request(url) ?: return@withContext null
        try {
            http.newCall(req).execute().use { resp ->
                val bytes = if (resp.isSuccessful) resp.body?.bytes() else null
                bytes?.let { decodeScaled(it, maxWidth) }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Numeric history for one entity over [hours] (minimal_response, no attributes). */
    suspend fun history(entityId: String, hours: Int): List<HistoryPoint> = withContext(Dispatchers.IO) {
        val base = baseUrl ?: return@withContext emptyList()
        val start = Instant.now().minusSeconds(hours * 3600L).toString()
        val url = "$base/api/history/period/${URLEncoder.encode(start, "UTF-8")}" +
            "?filter_entity_id=$entityId&minimal_response&no_attributes&significant_changes_only=0"
        val req = request(url) ?: return@withContext emptyList()
        try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                parseHistory(JSONArray(resp.body?.string() ?: "[]"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        /** Pure: HA history JSON → numeric points (non-numeric states skipped). Unit tested. */
        fun parseHistory(root: JSONArray): List<HistoryPoint> {
            val series = root.optJSONArray(0) ?: return emptyList()
            val out = ArrayList<HistoryPoint>(series.length())
            for (i in 0 until series.length()) {
                val o = series.optJSONObject(i) ?: continue
                val v = o.optString("state").toDoubleOrNull() ?: continue
                val ts = o.optString("last_changed").ifEmpty { o.optString("lu") }
                val sec = ts.toDoubleOrNull() ?: try {
                    OffsetDateTime.parse(ts).toInstant().toEpochMilli() / 1000.0
                } catch (_: Exception) {
                    continue
                }
                out += HistoryPoint(sec, v)
            }
            return out
        }

        fun decodeScaled(bytes: ByteArray, maxWidth: Int): Bitmap? {
            if (maxWidth <= 0) return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
