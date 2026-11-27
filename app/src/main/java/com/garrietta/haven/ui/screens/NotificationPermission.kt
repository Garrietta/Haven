package com.garrietta.haven.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.flow.collectLatest
import com.garrietta.haven.data.cameraSnapshotWidthPx
import com.garrietta.haven.ui.theme.glow
import com.garrietta.haven.ui.theme.grainGlow
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Garage
import androidx.compose.ui.geometry.Offset
import com.garrietta.haven.ui.components.NavStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.layout.RowScope
import android.provider.Settings
import android.os.Build
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import com.garrietta.haven.data.HOME_SECTION_ORDER_DEFAULT
import com.garrietta.haven.data.ConnectionMode
import com.garrietta.haven.data.alertDotColor
import com.garrietta.haven.data.domainCardColors
import com.garrietta.haven.ui.components.PillSwitch
import com.garrietta.haven.data.domainSwitchColors
import com.garrietta.haven.data.hasImportantAlert
import com.garrietta.haven.data.migrateLegacyHiddenSections
import com.garrietta.haven.data.migrateLegacySectionOrder
import com.garrietta.haven.data.resolveOrder
import com.garrietta.haven.data.customTitleFor
import com.garrietta.haven.data.customIconKeyFor
import com.garrietta.haven.data.inactiveCardColors
import com.garrietta.haven.data.presetAppliesForState
import com.garrietta.haven.data.presetBadgeColors
import com.garrietta.haven.data.presetSwitchColors
import com.garrietta.haven.data.tilePresetColor
import com.garrietta.haven.data.tilePresetFor
import com.garrietta.haven.ui.components.CARD_ICON_KEYS
import com.garrietta.haven.ui.components.cardIconForKey
import com.garrietta.haven.ui.controls.GarageDoorVisual
import com.garrietta.haven.ui.components.SensorKind
import com.garrietta.haven.ui.controls.DialogActionButtons
import com.garrietta.haven.ui.controls.MediaSquircle
import com.garrietta.haven.ui.controls.PressablePillButton
import com.garrietta.haven.ui.controls.ServiceSlider
import com.garrietta.haven.ui.components.TAB_ICON_OPTIONS
import com.garrietta.haven.ui.components.tabIconPairForKey
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.data.CardSize
import com.garrietta.haven.data.tallCardHeightDp
import com.garrietta.haven.data.tallPairHeightDp
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.data.UserTab
import com.garrietta.haven.data.groupEntitiesByRoom
import com.garrietta.haven.ui.components.shapeLabel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.HavenViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import com.garrietta.haven.data.UpdateSummary
import com.garrietta.haven.data.updatesSummary
import com.garrietta.haven.ui.components.MenuDivider
import com.garrietta.haven.ui.components.MenuGroup
import com.garrietta.haven.ui.components.MenuRow
import com.garrietta.haven.ui.components.MenuSection
import com.garrietta.haven.ui.components.LocalWideLayout
import com.garrietta.haven.ui.theme.ExpressiveKind
import com.garrietta.haven.ui.theme.springSpatial
import androidx.compose.animation.animateContentSize
import com.garrietta.haven.Screen
import com.garrietta.haven.data.ConnectionState
import com.garrietta.haven.data.DiscoverySource
import com.garrietta.haven.data.isLightShownOnHome
import com.garrietta.haven.isLive
import com.garrietta.haven.data.cleanDeviceName
import com.garrietta.haven.data.deviceCountLabel
import com.garrietta.haven.data.isHomeRoomEntity
import com.garrietta.haven.ui.components.CardTapRoute
import com.garrietta.haven.ui.components.DomainBadge
import com.garrietta.haven.ui.components.accentFor
import com.garrietta.haven.ui.components.cardTapRoute
import com.garrietta.haven.ui.components.tileBadgePair
import com.garrietta.haven.ui.components.CardResizeHandle
import com.garrietta.haven.ui.components.generalCardShape
import com.garrietta.haven.ui.components.reorderDrag
import com.garrietta.haven.ui.components.resizePreviewScale
import com.garrietta.haven.ui.components.resizeSettle
import com.garrietta.haven.ui.components.slotSpringOffset
import com.garrietta.haven.ui.components.weatherConditionColorThemed
import com.garrietta.haven.ui.components.isDarkBackdrop
import com.garrietta.haven.ui.components.legibleSecondaryOn
import com.garrietta.haven.ui.components.reportCardBounds
import com.garrietta.haven.ui.components.reportCardColor
import com.garrietta.haven.ui.components.reportCardShape
import com.garrietta.haven.ui.components.hideWhenMorphSource
import com.garrietta.haven.ui.components.reportHeroBounds
import com.garrietta.haven.ui.components.hideHeroWhenMorphSource
import com.garrietta.haven.ui.controls.dialogHeaderState
import com.garrietta.haven.ui.controls.domainIcon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.garrietta.haven.ui.components.badgeColors
import com.garrietta.haven.ui.components.sensorCardTint
import com.garrietta.haven.ui.components.sensorKind
import com.garrietta.haven.ui.components.climateModeColor
import com.garrietta.haven.ui.components.ensureContrast
import com.garrietta.haven.ui.components.hideBodyWhenMorphSource
import com.garrietta.haven.ui.components.lightBrightnessContainer
import com.garrietta.haven.ui.components.shapeForDomain
import com.garrietta.haven.ui.controls.formatTemp
import com.garrietta.haven.ui.controls.sensorDisplayName
import com.garrietta.haven.ui.controls.shapeTapAction
import com.garrietta.haven.ui.controls.stateText
import com.garrietta.haven.ui.theme.polygonShape
import com.garrietta.haven.ui.theme.pressBounce
import com.garrietta.haven.data.FRONT_DOOR_LIVE_VIEW_ID
import com.garrietta.haven.data.FRONT_DOOR_RING_MQTT_CAMERA_ID
import com.garrietta.haven.data.FRONT_DOOR_SNAPSHOT_ID
import com.garrietta.haven.data.HeadlineAction
import com.garrietta.haven.data.HomeHeadline
import com.garrietta.haven.data.HomeSubline
import com.garrietta.haven.data.Placement
import com.garrietta.haven.data.homeHeadline
import com.garrietta.haven.data.homeSubline
import com.garrietta.haven.data.frontDoorQuadrantShowsLive
import com.garrietta.haven.data.frontDoorResolverStates
import com.garrietta.haven.data.resolveFrontDoorCameraSource
import com.garrietta.haven.data.tabPlacementFor
import com.garrietta.haven.data.displayCameraName
import com.garrietta.haven.data.isRingCamera
import java.util.Calendar
import com.garrietta.haven.ui.theme.Palette
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.HavenLockup
import com.garrietta.haven.ui.theme.HavenChrome
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.garrietta.haven.ui.components.HavenMark
import com.garrietta.haven.ui.components.HavenWordmark
import com.garrietta.haven.ui.theme.leafCard
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import com.garrietta.haven.ui.components.ScreenTitle
import com.garrietta.haven.ui.components.choiceChipColors

/**
 * Feedback round 19, item 6: the notification permission row.
 * The toggles above gate posting per category; this row reports
 * the Android 13+ runtime permission itself, with a button to
 * system settings when it is missing (the launch-time ask runs
 * once in MainActivity — this is the re-grant path). The status
 * is read on each composition; returning from system settings
 * refreshes it on the next visit.
 */
@Composable
internal fun NotificationPermissionRow() {
    val context = LocalContext.current
    val granted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    MenuRow(
        badgeKey = "notif-permission",
        badgeKind = ExpressiveKind.BUN,
        icon = Icons.Filled.Notifications,
        title = "Notification permission",
        subtitle = if (granted) "Granted — alerts can post." else "Not granted — alerts are held until you allow them.",
        onClick = null,
        flat = true,
        trailing = {
            if (!granted) {
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) { Text("Allow") }
            }
        },
    )
}
