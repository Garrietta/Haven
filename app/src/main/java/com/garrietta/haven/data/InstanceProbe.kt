package com.garrietta.haven.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Pre-flight for the login screen. Before Haven ever launches OAuth or opens a
 * WebSocket, it probes the typed instance from inside the app so a dead address
 * produces a precise in-app explanation instead of a dead browser page.
 *
 * The probe is GET <base>/manifest.json: Home Assistant serves its PWA manifest
 * there with no auth, and the manifest names the product, so the probe both
 * proves reachability and fingerprints the server as Home Assistant.
 *
 * Deliberately conservative: this reports only what the probe observed. It never
 * claims the instance is down or its IP changed — a timeout is consistent with
 * the phone being on the wrong network, the host being off, or the IP moving.
 */
fun normalizeInstanceUrl(raw: String): String {
    var url = raw.trim().trimEnd('/')
    if (!url.contains("://")) {
        val host = url.substringBefore(":").substringBefore("/")
        val looksLocal = host.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) ||
            host.endsWith(".local") || host.endsWith(".lan") ||
            host == "localhost" || !host.contains(".")
        url = (if (looksLocal) "http://" else "https://") + url
    }
    return url
}

sealed class ProbeResult {
    /** Reachable, and the manifest identifies it as Home Assistant. */
    data class Success(val name: String?, val version: String?) : ProbeResult()

    /** Host never answered within the timeout (off, wrong network, IP moved). */
    data object Timeout : ProbeResult()

    /** Host answered but the port is closed (host up, Home Assistant down). */
    data object ConnectionRefused : ProbeResult()

    /** No route / DNS failure (bad hostname, no path to the host). */
    data object Unreachable : ProbeResult()

    /** An HTTP server answered, but with an error status. */
    data class HttpError(val code: Int) : ProbeResult()

    /** A server answered, but its manifest is not Home Assistant's. */
    data object NotHomeAssistant : ProbeResult()

    /** No usable network at all (offline, airplane mode). */
    data object NoNetwork : ProbeResult()
}

/** True when the manifest JSON is Home Assistant's (name check, case-insensitive). */
fun isHomeAssistantManifest(json: JSONObject): Boolean =
    json.optString("name").contains("Home Assistant", ignoreCase = true)

/** Plain-language explanation plus the next step, for each failure class. */
fun probeFailureMessage(result: ProbeResult): String = when (result) {
    is ProbeResult.Success -> ""
    ProbeResult.Timeout -> "That address didn't answer. Check you're on your home Wi-Fi " +
        "and that Home Assistant is running — or try finding it on your network below."
    ProbeResult.ConnectionRefused -> "Something answered at that address, but port 8123 is closed. " +
        "Home Assistant may not be running on that machine."
    ProbeResult.Unreachable -> "There's no route to that address from this phone. " +
        "Check the address and that you're on the same network as Home Assistant."
    is ProbeResult.HttpError -> "The server at that address answered with HTTP ${result.code}. " +
        "It may not be a Home Assistant instance."
    ProbeResult.NotHomeAssistant -> "That address answered, but it isn't Home Assistant. " +
        "Check the address and port."
    ProbeResult.NoNetwork -> "This phone has no network connection. Connect to Wi-Fi " +
        "or mobile data and try again."
}

/** True when the phone has a usable network (Wi-Fi or cellular) right now. */
fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

/**
 * Probes [base] (already normalized) and classifies the outcome. Short timeouts
 * so a dead address fails fast instead of hanging the login screen.
 */
suspend fun probeInstance(base: String, http: OkHttpClient): ProbeResult = withContext(Dispatchers.IO) {
    val client = http.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
    try {
        val req = Request.Builder()
            .url("$base/manifest.json")
            .header("Accept", "application/json")
            .build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: ""
        val code = resp.code
        resp.close()
        if (code !in 200..299) return@withContext ProbeResult.HttpError(code)
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            return@withContext ProbeResult.NotHomeAssistant
        }
        if (isHomeAssistantManifest(json)) {
            ProbeResult.Success(
                json.optString("name").ifEmpty { null },
                json.optString("version").ifEmpty { null },
            )
        } else {
            ProbeResult.NotHomeAssistant
        }
    } catch (e: SocketTimeoutException) {
        ProbeResult.Timeout
    } catch (e: ConnectException) {
        ProbeResult.ConnectionRefused
    } catch (e: UnknownHostException) {
        ProbeResult.Unreachable
    } catch (e: IOException) {
        ProbeResult.Unreachable
    }
}
