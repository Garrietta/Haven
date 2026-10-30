package com.garrietta.haven.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.ShapePickerTarget
import com.garrietta.haven.ui.components.FloatingCorner
import com.garrietta.haven.ui.components.SHAPE_OPTIONS
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.domainIcon
import com.garrietta.haven.ui.components.shapeForKey
import com.garrietta.haven.ui.components.shapeLabel
import androidx.compose.material3.Icon

/**
 * Shape picker dialog (2026-10-30): a grid of the M3E shapes
 * with live preview, reachable from Settings (global default) and
 * from each entity's sheet (per-entity override). Choices persist
 * in SettingsStore and apply immediately via
 * [HavenViewModel.bumpShapePrefs] (HavenApp re-reads the prefs
 * into LocalShapePrefs). The current domain shapes remain the
 * default unless changed: "Use default" clears a per-entity
 * override back to the global (or domain) shape. Centered dialog
 * panel on the #77 model (0.9 width capped at 480dp, 85% height
 * cap with inner scroll); the scrim tap and back both close.
 */
@Composable
fun ShapePickerDialog(vm: HavenViewModel, target: ShapePickerTarget, onClose: () -> Unit) {
    BackHandler { onClose() }
    val entity = target.entityId?.let { vm.states.value[it] }
    val store = vm.settingsStore
    val currentKey = if (target.forCard) {
        if (target.entityId != null) store.cardShapeOverrides()[target.entityId] else store.globalCardShape
    } else {
        if (target.entityId != null) store.badgeShapeOverrides()[target.entityId] else store.globalBadgeShape
    }
    val accent = if (entity != null) accentFor(entity.entityId) else MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            val panelHeight = (maxHeight * 0.85f)
            Column(
                Modifier.fillMaxWidth(0.9f).widthIn(max = 480.dp)
                    .heightIn(max = panelHeight)
                    .clip(FloatingCorner)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* consume: panel taps must not dismiss */ }
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    if (target.forCard) "Card shape" else "Badge shape",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (target.entityId != null) "Applies immediately to ${entity?.friendlyName ?: "this entity"}."
                    else "Applies immediately everywhere, unless an entity has its own override.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                if (target.entityId != null) {
                    OutlinedButton(
                        onClick = {
                            if (target.forCard) store.setCardShapeOverride(target.entityId, null)
                            else store.setBadgeShapeOverride(target.entityId, null)
                            vm.bumpShapePrefs()
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Use default") }
                    Spacer(Modifier.height(12.dp))
                }
                SHAPE_OPTIONS.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            val selected = option.key == currentKey
                            Column(
                                Modifier.weight(1f).padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        if (target.forCard) {
                                            if (target.entityId != null) store.setCardShapeOverride(target.entityId, option.key)
                                            else store.globalCardShape = option.key
                                        } else {
                                            if (target.entityId != null) store.setBadgeShapeOverride(target.entityId, option.key)
                                            else store.globalBadgeShape = option.key
                                        }
                                        vm.bumpShapePrefs()
                                        onClose()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(56.dp).background(accent, shapeForKey(option.key)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (entity != null) {
                                        Icon(
                                            domainIcon(entity),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    shapeLabel(option.key) ?: option.key,
                                    fontSize = 12.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
