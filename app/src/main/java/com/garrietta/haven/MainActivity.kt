package com.garrietta.haven

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.garrietta.haven.data.HavenNotificationManager

class MainActivity : ComponentActivity() {

    private val vm: HavenViewModel by viewModels()

    // Feedback round 19, item 6: the Android 13+ notification
    // permission ask runs ONCE at launch (flag in prefs); a denial
    // is not re-asked — the Settings permission row is the
    // re-grant path via system settings.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> /* Result is re-read from the system on next Settings visit. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge is enforced on Android 15+; opt in explicitly so the
        // app owns its insets (screens consume statusBarsPadding) instead of
        // the system guessing — the old default left headers colliding with
        // the status bar on the Fold8.
        installSplashScreen()
        enableEdgeToEdge()
        // UI rewrite round §6 (exit flash): the navigation bar is
        // fully transparent, never an opaque inset box — this
        // complements (not replaces) the Scaffold/overlay
        // backgrounds painted with the theme color in HavenApp.
        window.navigationBarColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        vm.handleOAuthIntent(intent)
        // Feedback round 3, item 6: a Device Controls tap carries
        // its entity id; the router jumps to that entity's surface
        // once its state has loaded (cold start included).
        vm.handleControlDeepLink(intent.getStringExtra(EXTRA_CONTROL_ENTITY_ID))
        // Feedback round 19, item 6: notification channels exist
        // before any state change can fire, and the Android 13+
        // permission ask runs once (see the launcher above).
        vm.notificationManager.ensureChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED &&
            !vm.notificationManager.permissionAsked()
        ) {
            vm.notificationManager.markPermissionAsked()
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            HavenApp(vm)
        }
    }

    // Front Door live probe (Round 2): backgrounding must never
    // leave the ring-mqtt live-stream switch on — the ViewModel
    // disengages (switch off + watchdog cancel) if a live session
    // is engaged, and no-ops otherwise. onStop also fires on
    // rotation; the panel re-engages on remount if still on Live
    // View, so a rotation costs a live restart, never a stuck switch.
    override fun onStop() {
        super.onStop()
        vm.onAppBackgrounded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.handleOAuthIntent(intent)
        vm.handleControlDeepLink(intent.getStringExtra(EXTRA_CONTROL_ENTITY_ID))
    }
}
