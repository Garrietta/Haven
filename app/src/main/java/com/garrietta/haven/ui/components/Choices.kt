package com.garrietta.haven.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.theme.HavenChrome

/**
 * The one way to pick one of several in Haven: a row of filled pills of
 * equal width, the chosen one in the selected capsule. A thermostat's
 * mode and fan, the alarm, the theme, a vacuum's suction all use it, so
 * a choice looks the same wherever it is made. Options that do not fit
 * one row wrap onto even rows ([choicesPerRow]). [options] are (value, label) pairs in display
 * order; [activeColors] may tint the chosen pill (the thermostat's heat
 * and cool set points do).
 */
@Composable
fun ChoiceRow(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColors: ((String) -> Pair<Color, Color>?)? = null,
) {
    if (options.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
    val perRow = choicesPerRow(options.size, maxWidth.value)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in options.chunked(perRow)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((value, label) in row) {
                    val isSelected = selected == value
                    val active = if (isSelected) activeColors?.invoke(value) else null
                    PressablePillButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        containerColor = active?.first ?: if (isSelected) HavenChrome.capsule else scheme.surfaceContainerHigh,
                        contentColor = active?.second ?: if (isSelected) HavenChrome.onCapsule else scheme.onSurfaceVariant,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        // Four options share a cover-screen row and the longest
                        // label may not fit at full size: step it down first.
                        Text(
                            label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    }
}

/**
 * How many choices share a row in [widthDp]: all of them when each can
 * be at least 76dp wide, otherwise as few rows as fit, split evenly (four
 * that do not fit in one row become two and two, not three and one).
 */
fun choicesPerRow(count: Int, widthDp: Float): Int {
    if (count <= 1) return count.coerceAtLeast(1)
    val fit = ((widthDp + 8f) / (76f + 8f)).toInt().coerceAtLeast(1)
    if (count <= fit) return count
    val rows = (count + fit - 1) / fit
    return (count + rows - 1) / rows
}

/** A labelled choice: the label above, the [ChoiceRow] under it. */
@Composable
fun LabeledChoiceRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        ChoiceRow(options, selected, onSelect)
    }
}
