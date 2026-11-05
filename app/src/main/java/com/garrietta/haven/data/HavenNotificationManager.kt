package com.garrietta.haven.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.garrietta.haven.EXTRA_CONTROL_ENTITY_ID
import com.garrietta.haven.MainActivity

/**
 * Feedback round 19, item 6: real-time Home Assistant event
 * notifications. The trigger logic lives in [NotificationRules]
 * (pure, unit tested); this class owns only the Android surface:
 * three high-priority channels (one per toggle, so the user can
 * also tune each category in system settings), the tap
 * PendingIntent, and the permission gate.
 *
 * The tap intent carries [EXTRA_CONTROL_ENTITY_ID], so it flows
 * through MainActivity's existing deep-link path (see
 * ControlDeepLinkRouter): a garage tap opens the garage detail,
 * a motion tap opens the camera detail, a light tap opens the
 * light detail — exactly as if the card had been tapped. The
 * manager never navigates itself and never reads states; the
 * ViewModel feeds it parsed [StateChangeEvent]s from HaClient.
 */
class HavenNotificationManager(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {
    companion object {
        const val CHANNEL_GARAGE = "haven_garage"
        const val CHANNEL_MOTION = "haven_motion"
        const val CHANNEL_LIGHTS = "haven_lights"

        /** SharedPreferences flag: the launch-time permission ask has already run once. */
        const val PREF_PERMISSION_ASKED = "haven_notif_permission_asked"
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** True on Android 13+ only when POST_NOTIFICATIONS is granted; pre-13 needs no runtime grant. */
    fun isPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** True once the launch-time permission ask has run (a denial is never re-asked at launch). */
    fun permissionAsked(): Boolean =
        context.getSharedPreferences("haven_settings", Context.MODE_PRIVATE)
            .getBoolean(PREF_PERMISSION_ASKED, false)

    /** Marks the launch-time permission ask as done. */
    fun markPermissionAsked() {
        context.getSharedPreferences("haven_settings", Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_PERMISSION_ASKED, true).apply()
    }

    /** Creates the three high-priority channels (idempotent — recreating an existing channel is a no-op). */
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channels = listOf(
            NotificationChannel(CHANNEL_GARAGE, "Garage & doors", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Garage door and door openings"
            },
            NotificationChannel(CHANNEL_MOTION, "Motion", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Motion detections"
            },
            NotificationChannel(CHANNEL_LIGHTS, "Automated lights", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Lights turned off by automations or external triggers"
            },
        )
        for (channel in channels) notificationManager.createNotificationChannel(channel)
    }

    /**
     * One parsed `state_changed` event. The category toggle gates
     * first (a disabled category never posts, whatever the event),
     * then the pure trigger rules decide. A denied permission posts
     * nothing — the system would drop it silently, so the explicit
     * gate keeps the behavior honest and testable at the call site.
     */
    fun onStateChanged(event: StateChangeEvent, locallyActuated: Boolean) {
        if (!isPermissionGranted()) return
        val kind = resolveNotification(event, locallyActuated) ?: return
        val enabled = when (kind) {
            NotificationKind.GARAGE_DOOR -> settingsStore.notifyGarage
            NotificationKind.MOTION -> settingsStore.notifyMotion
            NotificationKind.LIGHT_SHUTOFF -> settingsStore.notifyLights
        }
        if (!enabled) return
        ensureChannels()
        val (channelId, title, body) = when (kind) {
            NotificationKind.GARAGE_DOOR -> Triple(
                CHANNEL_GARAGE,
                if (event.newState == "opening") "${event.displayName} opening" else "${event.displayName} opened",
                "Tap to view in Haven.",
            )
            NotificationKind.MOTION -> Triple(
                CHANNEL_MOTION,
                "Motion detected",
                event.displayName,
            )
            NotificationKind.LIGHT_SHUTOFF -> Triple(
                CHANNEL_LIGHTS,
                "${event.displayName} turned off",
                if (event.parentId != null) "Turned off by an automation." else "Turned off externally.",
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CONTROL_ENTITY_ID, event.entityId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pending = PendingIntent.getActivity(
            context,
            (event.entityId + kind.name).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((event.entityId + kind.name).hashCode(), notification)
    }
}
