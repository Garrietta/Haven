package com.garrietta.haven.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.Screen
import com.garrietta.haven.data.DeviceGroup
import com.garrietta.haven.data.deviceDisplayName
import com.garrietta.haven.ui.controls.SheetIconSquircleButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight

/**
 * Device sheet (customization round, item 3): what a device row
 * leads to. It lists the device's REAL entities (from live state,
 * sorted by name) with taps to their popups — never a dead row, and
 * never a fake master switch. A device with no single switch (the
 * P400M dual outlet) is honestly represented here as its entities:
 * its auto-off switch, turn-off-in number, overheated sensor, and
 * auto-off-at sensor, each opening its own control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSheet(
    vm: HavenViewModel,
    group: DeviceGroup,
    navigate: (Screen) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val states = vm.states.value
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(deviceDisplayName(group), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            val info = group.info
            if (info?.model != null || info?.manufacturer != null) {
                Text(
                    listOfNotNull(info.manufacturer, info.model).joinToString(" · "),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("${group.entityIds.size} entities", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            val entities = group.entityIds.mapNotNull { states[it] }.sortedBy { it.friendlyName }
            if (entities.isEmpty()) {
                Text(
                    "No live entities for this device right now.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (entity in entities) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entity.friendlyName,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            entity.state,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    SheetIconSquircleButton(Icons.Filled.ChevronRight, "Open ${entity.friendlyName}", onClick = {
                        onDismiss()
                        navigate(Screen.EntityDetail(entity.entityId))
                    })
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
