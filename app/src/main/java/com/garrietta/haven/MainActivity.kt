package com.garrietta.haven

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val vm: HavenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge is enforced on Android 15+; opt in explicitly so the
        // app owns its insets (screens consume statusBarsPadding) instead of
        // the system guessing — the old default left headers colliding with
        // the status bar on the Fold8.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        vm.handleOAuthIntent(intent)
        setContent {
            HavenApp(vm)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.handleOAuthIntent(intent)
    }
}
