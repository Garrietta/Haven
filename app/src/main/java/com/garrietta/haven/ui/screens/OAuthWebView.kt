package com.garrietta.haven.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel

/**
 * The Home Assistant authorize page, rendered in-app.
 *
 * Why not the system browser or a Custom Tab: on Garrett's phone, Chrome's
 * stack cannot load his instance at all (ERR_TOO_MANY_RETRIES even on the bare
 * root URL), while the phone's native HTTP stack reaches it fine — so any
 * Chrome-based flow fails before OAuth even starts. This WebView runs on the
 * app's own network stack (with cleartext permitted for LAN instances), and
 * the haven://auth-callback redirect is intercepted here so the code never
 * leaves the app: it is exchanged natively by [HavenViewModel.handleOAuthRedirect].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OAuthWebViewScreen(vm: HavenViewModel, authorizeUrl: String, onClose: () -> Unit) {
    // Same treatment as LoginScreen: the container paints the scheme
    // background so the top bar and status area follow the system theme
    // (the WebView itself paints HA's own authorize page, untouched).
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Sign in", onClose)
        if (vm.oauthExchanging) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "Finishing sign-in…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        vm.authError?.let { error ->
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url.toString()
                            return if (url.startsWith("haven://auth-callback")) {
                                // Intercept: hand the code to the ViewModel, never
                                // let the WebView try (and fail) to load the scheme.
                                vm.handleOAuthRedirect(request.url)
                                true
                            } else {
                                false
                            }
                        }
                    }
                    loadUrl(authorizeUrl)
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
