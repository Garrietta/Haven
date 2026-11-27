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

@Composable
internal fun CameraQuadrant(vm: HavenViewModel, cameraId: String?, modifier: Modifier = Modifier, corners: RoundedCornerShape = RoundedCornerShape(0.dp)) {
    var slotWidthPx by remember { mutableIntStateOf(0) }
    Box(
        modifier.padding(1.dp).clip(corners)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // Morph source (2026-11-01 round 1): the quadrant
            // reports its root rect so the camera panel morphs
            // from — and, crucially, back to — this exact
            // quadrant on both postures. Before this, no quadrant
            // ever reported, so the panel fell back to the
            // full-width shifted rect and its close landed near
            // the top of the screen instead of on the quadrant.
            // It also hides while its panel is open, so the
            // snapshot never doubles the video.
            .then(if (cameraId != null) Modifier.reportCardBounds(cameraId).reportCardShape(cameraId, corners).hideWhenMorphSource(cameraId) else Modifier)
            // Reliability round: the tile's pixel width picks the
            // snapshot size requested from HA (see cameraSnapshotWidthPx).
            .onSizeChanged { slotWidthPx = it.width }
            // Reliability round: a finger landing on the tile starts HA's
            // stream worker (camera/stream) before the tap even completes;
            // observes the down without consuming it, so the click below
            // behaves exactly as before.
            .then(
                if (cameraId != null) {
                    Modifier.pointerInput(cameraId) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            vm.prewarmCameraStream(cameraId)
                        }
                    }
                } else {
                    Modifier
                },
            )
            .then(if (cameraId != null) Modifier.clickable { vm.openCameraDialog(cameraId) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraId == null) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
        } else {
            // Feedback round 18, item 7: per-entity flows instead
            // of the whole states map — a state change on any
            // other entity no longer recomposes this quadrant.
            // The front-door trio feeds the SHARED resolver (same
            // fallback/unavailable behavior as before); the motion
            // sensor arrives on its own flow (see motionSensorFlow).
            // Plain .value reads (not by-delegates) so the entity
            // smart-casts in the overlay block below.
            val entity = vm.entityFlow(cameraId).collectAsState().value
            val ringMqtt = vm.entityFlow(FRONT_DOOR_RING_MQTT_CAMERA_ID).collectAsState().value
            val liveView = vm.entityFlow(FRONT_DOOR_LIVE_VIEW_ID).collectAsState().value
            val snapshotEntity = vm.entityFlow(FRONT_DOOR_SNAPSHOT_ID).collectAsState().value
            val motionSensor = vm.motionSensorFlow(cameraId).collectAsState().value
            // Front Door live probe (Round 2): the Front Door
            // quadrant's static thumbnail comes from the dedicated
            // snapshot camera (camera.front_door_snapshot, "Ring
            // Doorbell") when it exists, instead of the live-view
            // camera's proxy — a still thumbnail for a still card.
            // The tap target stays the live-view panel, and the
            // morph-source / hide keys stay on cameraId, so the
            // panel morph is untouched. Falls back to cameraId when
            // the snapshot entity is absent.
            // Camera fallback round: when BOTH front-door sources
            // are down, the quadrant shows an honest unavailable
            // state instead of a stale cached snapshot. The tap
            // target, morph keys, and cameraId stay on live_view,
            // so the panel (with its own unavailable state) still
            // opens and the dialog routing is untouched.
            val frontDoorStates = frontDoorResolverStates(ringMqtt, liveView)
            val frontDoorDown = cameraId == FRONT_DOOR_LIVE_VIEW_ID &&
                resolveFrontDoorCameraSource(frontDoorStates) == null
            val thumbnailId = if (cameraId == FRONT_DOOR_LIVE_VIEW_ID &&
                snapshotEntity != null
            ) FRONT_DOOR_SNAPSHOT_ID else cameraId
            val snap = vm.snapshotFor(thumbnailId)
            // Cache-first, fetch deferred (2026-11-01 polish round):
            // when a cached image exists it is already on screen,
            // so the fresh fetch waits ~1s instead of racing app
            // open (the old immediate fetch left the top-right
            // quadrant blank for ~5s while four fetches queued).
            // With no cache there is nothing to protect, so the
            // fetch starts at once. refreshSnapshot itself only
            // swaps in a finished image and only when the cached
            // one is stale, so a failed or in-flight fetch never
            // blanks a quadrant.
            // Reliability round: the tile keeps refreshing while it is on
            // screen and the app is in the foreground (10s on the LAN, 20s
            // through the relay) — it used to fetch once per entry, so a
            // screen left open showed stale images. Each fetch asks HA for
            // a tile-sized image.
            val requestWidth = cameraSnapshotWidthPx(slotWidthPx)
            LaunchedEffect(thumbnailId, requestWidth) {
                if (vm.snapshotFor(thumbnailId) != null) kotlinx.coroutines.delay(600)
                snapshotFlow { vm.appInForeground }.collectLatest { foreground ->
                    while (foreground) {
                        vm.refreshSnapshot(thumbnailId, requestWidth)
                        kotlinx.coroutines.delay(vm.snapshotRefreshIntervalMs())
                    }
                }
            }
            if (frontDoorDown) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Unavailable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (snap != null) {
                Image(snap.asImageBitmap(), contentDescription = entity?.friendlyName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
            // Mockup-language overlay (Gemini mockup round,
            // 2026-11-01): the mockup's Front Door card carries its
            // title over the photo with a LIVE badge — each quadrant
            // gets the same treatment: status pill top-left, camera
            // name bottom-left over a bottom scrim. The pill is
            // honest by construction: LIVE only for genuinely live
            // native feeds, Recording for Ring (never a LIVE badge
            // on a recording — standing honesty law). Feedback
            // round 19, item 4, refines the Front Door quadrant:
            // its badge follows the resolved live source (69a1
            // up = LIVE, fallback = Recording), not the brand —
            // see showLive below. The name uses
            // the display name, so the Ring quadrant never reads
            // "Front Door Live view". Expressive-menus round,
            // item 7: a Motion pill joins the status pill ONLY
            // while a motion binary_sensor on the camera's own
            // registry device reports "on" (see
            // motionSensorForCamera) — the mockup's "Motion
            // detected" was demo data, but the Ring doorbell's
            // real motion sensor now backs the same chip. No
            // linked sensor means no chip, never a guess.
            // Camera fallback round: no overlay on the
            // unavailable state — a "Recording" pill over an
            // "Unavailable" quadrant would contradict it.
            if (entity != null && !frontDoorDown) {
                // Feedback round 19, item 4: the Front Door
                // quadrant's badge follows the LIVE source, not
                // the camera brand — a recovered 69a1 earns LIVE
                // here; the fallback feed stays Recording even
                // while it plays (the live source is what is
                // down). Every other quadrant keeps the existing
                // brand rule (LIVE for native feeds, Recording
                // for Ring).
                val showLive = if (cameraId == FRONT_DOOR_LIVE_VIEW_ID) {
                    // Round 21, item 3: the quadrant reads LIVE
                    // for the fallback once the RTSP fallback has
                    // proven itself streaming this session
                    // (vm.frontDoorRtspProven); before any proof
                    // it stays Recording.
                    frontDoorQuadrantShowsLive(resolveFrontDoorCameraSource(frontDoorStates), false, rtspProven = vm.frontDoorRtspProven)
                } else {
                    !isRingCamera(entity)
                }
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().height(56.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                ),
                            ),
                    )
                    // Reliability round: the status pill and the Motion pill
                    // share one Row. The Motion pill used to be a loose Box
                    // child (its Spacer did nothing in a Box), so it painted
                    // straight over the LIVE / Recording pill.
                    Row(
                        Modifier.align(Alignment.TopStart).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showLive) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(Palette.Lime),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Recording", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Motion pill (item 7): only while the linked
                    // sensor is actually "on"; absent or off means
                    // no pill at all.
                    if (motionSensor?.state == "on") {
                        Spacer(Modifier.width(6.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(Palette.Sun),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Motion", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    }
                    Text(
                        displayCameraName(entity),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}
