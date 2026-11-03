package com.garrietta.haven.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon picker options for per-entity customization (customization
 * round, item 2). Keys are storage keys; every vector here is already
 * used somewhere in-tree (material-icons-extended), so the picker
 * cannot introduce an uncompilable icon. The caller falls back to
 * [domainIcon] whenever the stored key is unknown.
 */
data class CardIconOption(val key: String, val label: String, val icon: ImageVector)

val CARD_ICON_OPTIONS: List<CardIconOption> = listOf(
    CardIconOption("lightbulb", "Bulb", Icons.Filled.Lightbulb),
    CardIconOption("power", "Power", Icons.Filled.Power),
    CardIconOption("home", "Home", Icons.Filled.Home),
    CardIconOption("thermostat", "Thermostat", Icons.Filled.Thermostat),
    CardIconOption("autorenew", "Vacuum", Icons.Filled.Autorenew),
    CardIconOption("cleaning", "Cleaning", Icons.Filled.CleaningServices),
    CardIconOption("tv", "TV", Icons.Filled.Tv),
    CardIconOption("speaker", "Speaker", Icons.Filled.Speaker),
    CardIconOption("music", "Music", Icons.Filled.MusicNote),
    CardIconOption("volume", "Volume", Icons.Filled.VolumeUp),
    CardIconOption("refresh", "Fan", Icons.Filled.Refresh),
    CardIconOption("lock", "Locked", Icons.Filled.Lock),
    CardIconOption("lock_open", "Unlocked", Icons.Filled.LockOpen),
    CardIconOption("garage", "Garage", Icons.Filled.Garage),
    CardIconOption("videocam", "Camera", Icons.Filled.Videocam),
    CardIconOption("camera", "Photo camera", Icons.Filled.Camera),
    CardIconOption("security", "Security", Icons.Filled.Security),
    CardIconOption("visibility", "Motion", Icons.Filled.Visibility),
    CardIconOption("warning", "Warning", Icons.Filled.Warning),
    CardIconOption("water", "Water", Icons.Filled.WaterDrop),
    CardIconOption("sunny", "Sun", Icons.Filled.WbSunny),
    CardIconOption("cloudy", "Cloud", Icons.Filled.WbCloudy),
    CardIconOption("cloud", "Cloud (alt)", Icons.Filled.Cloud),
    CardIconOption("umbrella", "Umbrella", Icons.Filled.Umbrella),
    CardIconOption("battery", "Battery", Icons.Filled.BatteryFull),
    CardIconOption("bolt", "Power (bolt)", Icons.Filled.Bolt),
    CardIconOption("wifi", "Wi-Fi", Icons.Filled.Wifi),
    CardIconOption("spa", "Plant", Icons.Filled.Spa),
    CardIconOption("nightlight", "Night", Icons.Filled.Nightlight),
    CardIconOption("person", "Person", Icons.Filled.Person),
    CardIconOption("schedule", "Schedule", Icons.Filled.Schedule),
    CardIconOption("list", "List", Icons.Filled.List),
    CardIconOption("play", "Play", Icons.Filled.PlayArrow),
    CardIconOption("build", "Tools", Icons.Filled.Build),
    CardIconOption("info", "Info", Icons.Filled.Info),
)

val CARD_ICON_KEYS: Set<String> = CARD_ICON_OPTIONS.map { it.key }.toSet()

/** Picker icon for a key, or null for an unknown key (caller falls back to the domain icon). */
fun cardIconForKey(key: String?): ImageVector? =
    key?.let { k -> CARD_ICON_OPTIONS.firstOrNull { it.key == k }?.icon }
