package com.garrietta.haven.core.auth

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** OAuth client registered through the GitHub Pages site (it carries the redirect_uri link tag). */
const val OAUTH_CLIENT_ID = "https://garrietta.github.io/Haven/"
const val OAUTH_REDIRECT_URI = "haven://auth-callback"

/** Everything needed to talk to one Home Assistant server. */
data class Credentials(
    /** Ordered base URLs, e.g. LAN first, Nabu Casa second. */
    val localUrl: String?,
    val remoteUrl: String?,
    val accessToken: String,
    /** Null for long-lived tokens. */
    val refreshToken: String? = null,
    /** Epoch millis; 0 for long-lived tokens. */
    val expiresAtMs: Long = 0L,
) {
    val isLongLived: Boolean get() = refreshToken == null

    fun toJson(): String = JSONObject()
        .put("localUrl", localUrl).put("remoteUrl", remoteUrl)
        .put("accessToken", accessToken).put("refreshToken", refreshToken)
        .put("expiresAtMs", expiresAtMs).toString()

    companion object {
        fun fromJson(s: String): Credentials? = try {
            val o = JSONObject(s)
            Credentials(
                localUrl = o.optString("localUrl").ifEmpty { null }?.takeIf { it != "null" },
                remoteUrl = o.optString("remoteUrl").ifEmpty { null }?.takeIf { it != "null" },
                accessToken = o.getString("accessToken"),
                refreshToken = o.optString("refreshToken").ifEmpty { null }?.takeIf { it != "null" },
                expiresAtMs = o.optLong("expiresAtMs", 0L),
            )
        } catch (_: Exception) {
            null
        }
    }
}

/** PKCE (RFC 7636) helpers. Pure, unit tested. */
object Pkce {
    private val rng = SecureRandom()

    fun newVerifier(): String {
        val bytes = ByteArray(48)
        rng.nextBytes(bytes)
        return base64Url(bytes)
    }

    fun challenge(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    fun base64Url(bytes: ByteArray): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

/** Normalizes what a user types into a base URL: adds http://, drops trailing slashes and paths like /lovelace. */
fun normalizeBaseUrl(input: String): String? {
    var s = input.trim()
    if (s.isEmpty()) return null
    if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
    val uri = try { java.net.URI(s) } catch (_: Exception) { return null }
    val host = uri.host ?: return null
    val port = if (uri.port > 0) ":${uri.port}" else if (uri.scheme == "http" && !s.contains("nabu.casa")) ":8123" else ""
    return "${uri.scheme}://$host$port"
}

/** True for RFC1918 / link-local / .local hosts — the "LAN" side of the connection plan. */
fun isLanUrl(url: String?): Boolean {
    val host = try { java.net.URI(url ?: return false).host ?: return false } catch (_: Exception) { return false }
    if (host.endsWith(".local") || host == "localhost" || host == "homeassistant") return true
    val parts = host.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return parts[0] == 10 || (parts[0] == 192 && parts[1] == 168) ||
        (parts[0] == 172 && parts[1] in 16..31) || (parts[0] == 169 && parts[1] == 254) || parts[0] == 127
}

/** AES-256/GCM with a non-exportable AndroidKeystore key. Tokens never touch disk in clear. */
class TokenVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("haven_vault", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    fun save(creds: Credentials) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.doFinal(creds.toJson().toByteArray())
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("data", Base64.encodeToString(data, Base64.NO_WRAP))
            .apply()
    }

    fun load(): Credentials? = try {
        val iv = Base64.decode(prefs.getString("iv", null) ?: return null, Base64.NO_WRAP)
        val data = Base64.decode(prefs.getString("data", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        Credentials.fromJson(String(cipher.doFinal(data)))
    } catch (_: Exception) {
        null
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object { const val ALIAS = "haven_v2_tokens" }
}

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val creds: Credentials) : AuthState
}

/**
 * Owns credentials: OAuth sign-in (PKCE S256), long-lived tokens, and
 * refresh-ahead-of-expiry. [accessToken] is the only thing the network
 * layer asks for; it refreshes under a mutex so concurrent callers share
 * one refresh.
 */
class AuthManager(context: Context, private val http: OkHttpClient) {
    private val vault = TokenVault(context)
    private val mutex = Mutex()
    private val _state = MutableStateFlow<AuthState>(vault.load()?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var pendingVerifier: String? = null
    private var pendingBase: String? = null
    private var pendingState: String? = null

    val credentials: Credentials? get() = (state.value as? AuthState.SignedIn)?.creds

    /** Builds the authorize URL to open in the browser. */
    fun beginOAuth(baseUrl: String): Uri {
        val verifier = Pkce.newVerifier()
        val st = Pkce.newVerifier().take(16)
        pendingVerifier = verifier
        pendingBase = baseUrl
        pendingState = st
        return Uri.parse("$baseUrl/auth/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", OAUTH_CLIENT_ID)
            .appendQueryParameter("redirect_uri", OAUTH_REDIRECT_URI)
            .appendQueryParameter("state", st)
            .appendQueryParameter("code_challenge", Pkce.challenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /** Completes OAuth from the haven://auth-callback intent. */
    suspend fun completeOAuth(callback: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val code = callback.getQueryParameter("code") ?: error("No authorization code returned")
            if (pendingState != null && callback.getQueryParameter("state") != pendingState) error("Sign-in state mismatch")
            val base = pendingBase ?: error("Sign-in was not started from this app session")
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", OAUTH_CLIENT_ID)
                .apply { pendingVerifier?.let { add("code_verifier", it) } }
                .build()
            val json = postToken(base, body)
            val creds = Credentials(
                localUrl = base.takeIf { isLanUrl(it) },
                remoteUrl = base.takeIf { !isLanUrl(it) },
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAtMs = System.currentTimeMillis() + json.optLong("expires_in", 1800) * 1000,
            )
            store(creds)
            pendingVerifier = null; pendingBase = null; pendingState = null
        }
    }

    /** Long-lived token sign-in (verified by the caller with a test connection). */
    fun signInWithToken(baseUrl: String, token: String) {
        store(Credentials(baseUrl.takeIf { isLanUrl(it) }, baseUrl.takeIf { !isLanUrl(it) }, token.trim()))
    }

    /** Fills in the other side (LAN/remote) once HA tells us its URLs. */
    fun updateUrls(local: String?, remote: String?) {
        val c = credentials ?: return
        val next = c.copy(localUrl = local ?: c.localUrl, remoteUrl = remote ?: c.remoteUrl)
        if (next != c) store(next)
    }

    fun signOut() {
        vault.clear()
        _state.value = AuthState.SignedOut
    }

    /** A valid access token, refreshing if it expires within a minute. Null = signed out. */
    suspend fun accessToken(forceRefresh: Boolean = false): String? = mutex.withLock {
        val c = credentials ?: return null
        if (c.isLongLived) return c.accessToken
        if (!forceRefresh && c.expiresAtMs - System.currentTimeMillis() > 60_000) return c.accessToken
        return refresh(c)?.accessToken
    }

    private suspend fun refresh(c: Credentials): Credentials? = withContext(Dispatchers.IO) {
        val bases = listOfNotNull(c.localUrl, c.remoteUrl)
        for (base in bases) {
            try {
                val json = postToken(
                    base,
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", c.refreshToken ?: return@withContext null)
                        .add("client_id", OAUTH_CLIENT_ID)
                        .build(),
                )
                val next = c.copy(
                    accessToken = json.getString("access_token"),
                    expiresAtMs = System.currentTimeMillis() + json.optLong("expires_in", 1800) * 1000,
                )
                store(next)
                return@withContext next
            } catch (e: TokenRejected) {
                signOut()
                return@withContext null
            } catch (_: Exception) {
                // Unreachable endpoint: try the next one; keep the old token.
            }
        }
        null
    }

    private class TokenRejected : Exception()

    private fun postToken(base: String, body: FormBody): JSONObject {
        http.newCall(Request.Builder().url("$base/auth/token").post(body).build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 400 || resp.code == 403) throw TokenRejected()
            if (!resp.isSuccessful) error("Token endpoint HTTP ${resp.code}")
            return JSONObject(text)
        }
    }

    private fun store(c: Credentials) {
        vault.save(c)
        _state.value = AuthState.SignedIn(c)
    }
}
