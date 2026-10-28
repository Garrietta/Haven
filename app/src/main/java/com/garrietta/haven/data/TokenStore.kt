package com.garrietta.haven.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the instance URL (plain) and OAuth / long-lived tokens encrypted at rest
 * with an AndroidKeystore AES-256/GCM key. Nothing sensitive is ever written in cleartext.
 */
class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE)

    var instanceUrl: String?
        get() = prefs.getString("instance_url", null)
        set(value) {
            if (value == null) prefs.edit().remove("instance_url").apply()
            else prefs.edit().putString("instance_url", value).apply()
        }

    var accessToken: String?
        get() = readEncrypted("access_token")
        set(value) = writeEncrypted("access_token", value)

    var refreshToken: String?
        get() = readEncrypted("refresh_token")
        set(value) = writeEncrypted("refresh_token", value)

    var tokenExpiresAtMs: Long
        get() = prefs.getLong("expires_at_ms", 0L)
        set(value) = prefs.edit().putLong("expires_at_ms", value).apply()

    /** PKCE verifier + OAuth state for the in-flight sign-in (single-use, plain prefs). */
    var pkceVerifier: String?
        get() = prefs.getString("pkce_verifier", null)
        set(value) {
            if (value == null) prefs.edit().remove("pkce_verifier").apply()
            else prefs.edit().putString("pkce_verifier", value).apply()
        }

    var oauthState: String?
        get() = prefs.getString("oauth_state", null)
        set(value) {
            if (value == null) prefs.edit().remove("oauth_state").apply()
            else prefs.edit().putString("oauth_state", value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun readEncrypted(key: String): String? {
        val cipherText = prefs.getString("${key}_ct", null) ?: return null
        val iv = prefs.getString("${key}_iv", null) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT)))
            String(cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT)), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeEncrypted(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove("${key}_ct").remove("${key}_iv").apply()
            return
        }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val ct = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString("${key}_ct", Base64.encodeToString(ct, Base64.NO_WRAP))
                .putString("${key}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // Encryption failing must never crash login; the token simply won't persist.
        }
    }

    private fun getKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getEntry("haven_key", null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        val spec = KeyGenParameterSpec.Builder(
            "haven_key",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(spec)
        return gen.generateKey()
    }
}
