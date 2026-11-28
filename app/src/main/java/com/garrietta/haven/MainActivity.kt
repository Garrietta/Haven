package com.garrietta.haven

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.garrietta.haven.core.auth.OAUTH_REDIRECT_URI
import com.garrietta.haven.ui.HavenRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val graph get() = (application as HavenApplication).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent { HavenRoot(graph) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        // Coming back to the foreground: re-plan the connection right away
        // (the network may have changed while we were away).
        if (graph.auth.credentials != null && !graph.connection.isConnected) graph.connection.reconnectNow()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (!data.toString().startsWith(OAUTH_REDIRECT_URI)) return
        lifecycleScope.launch {
            graph.auth.completeOAuth(data).onFailure {
                Toast.makeText(this@MainActivity, "Sign-in failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
