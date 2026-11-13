package com.garrietta.haven.ui.components

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.garrietta.haven.ui.theme.HavenChrome

/**
 * The heading over a group of things: "Home Status", a room's name,
 * "Last 24 hours". One style and one color everywhere, so a screen's
 * structure reads at a glance: the app's green by day, the lime by
 * night. It is a heading for screen readers too.
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The name of a screen, at the top of it: "Settings", "Scenes", a user
 * tab's title. Home is the one screen without it; its greeting is its
 * title.
 */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Colors for a chip that is one of a set of choices. The chosen one is
 * the lime capsule: the same signal as the selected tab and the selected
 * room, so "this one" looks the same everywhere in the app.
 */
@Composable
fun choiceChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = HavenChrome.capsule,
    selectedLabelColor = HavenChrome.onCapsule,
    selectedLeadingIconColor = HavenChrome.onCapsule,
    selectedTrailingIconColor = HavenChrome.onCapsule,
)
