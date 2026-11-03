package com.garrietta.haven.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues

/**
 * Large expressive action shapes (2026-11-03 round), modeled on the
 * reference music-player screenshot: a big central squircle, large
 * flanking squircles, and pill volume controls — generous targets,
 * bold filled primary action. Every composable here keeps the
 * caller's service call and enabled/disabled semantics identical;
 * only shape, size, and spacing change. Toggles stay [PillSwitch],
 * lock unlock-confirmation is preserved (only its buttons grow),
 * and body-tap-opens-panel behavior is untouched.
 */

/** The squircle used for large square-ish actions (media transport, D-pad, sheet icons). */
val MediaSquircle = RoundedCornerShape(28.dp)

/**
 * Media transport in the reference-player language: two 72dp flanking
 * squircles (previous / next) around one 96dp central squircle
 * (play / pause, filled primaryContainer, icon-only like the
 * reference). Centered as a cluster. No queue button: HA's
 * media_player exposes no queue, so none is invented — the source
 * pills are the real output/source affordance.
 */
@Composable
fun MediaTransportRow(
    playing: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    // Feedback round 5, item 4: the central squircle carries the assigned tile color when a
    // preset is set; flanking squircles stay scheme (their signal is flanking-vs-central).
    pageAccent: androidx.compose.ui.graphics.Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressablePillButton(
            onClick = onPrevious,
            modifier = Modifier.size(72.dp),
            height = 72.dp,
            shape = MediaSquircle,
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        PressablePillButton(
            onClick = onPlayPause,
            modifier = Modifier.size(96.dp),
            height = 96.dp,
            shape = MediaSquircle,
            containerColor = pageAccent ?: scheme.primaryContainer,
            contentColor = if (pageAccent != null) scheme.onSurface else scheme.onPrimaryContainer,
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        PressablePillButton(
            onClick = onNext,
            modifier = Modifier.size(72.dp),
            height = 72.dp,
            shape = MediaSquircle,
        ) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
        }
    }
}

/** Large squircle mute toggle (reference player's mute pill, squared up). */
@Composable
fun MuteSquircleButton(muted: Boolean, onToggleMute: () -> Unit, modifier: Modifier = Modifier) {
    PressablePillButton(
        onClick = onToggleMute,
        modifier = modifier.size(56.dp),
        height = 56.dp,
        shape = MediaSquircle,
    ) {
        Icon(
            if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = if (muted) "Unmute" else "Mute",
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * Volume row in the reference language: the mute squircle pill, then
 * the caller's slider filling the rest. The slider itself is
 * unchanged (same service call, same value label).
 */
@Composable
fun MediaVolumeRow(
    muted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    slider: @Composable () -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        MuteSquircleButton(muted, onToggleMute)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            slider()
        }
    }
}

/**
 * Confirmation actions as large expressive pills: Cancel as a 56dp
 * tonal pill, the confirm action as a 56dp filled primary pill.
 * Used as the single button slot of an AlertDialog (pass it as
 * confirmButton and omit dismissButton) or as a dialog's own row.
 * Labels and callbacks are the caller's, unchanged.
 */
@Composable
fun DialogActionButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    modifier: Modifier = Modifier,
    cancelLabel: String = "Cancel",
) {
    Row(modifier, horizontalArrangement = Arrangement.End) {
        PressablePillButton(
            onClick = onCancel,
            height = 56.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(cancelLabel, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        PressablePillButton(
            onClick = onConfirm,
            height = 56.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(confirmLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 44dp squircle icon button for bottom-sheet rows (Favorites manage/add). */
@Composable
fun SheetIconSquircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressablePillButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        height = 44.dp,
        shape = MediaSquircle,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}
