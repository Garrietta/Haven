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
 * Sign-in. One job, so one clear path: the address of the user's Home
 * Assistant (typed, or picked from the instances found on the network),
 * then "Sign in". The long-lived token route does the same job for
 * setups the browser flow cannot reach; it is one tap away behind a
 * disclosure instead of sharing the screen with the main path.
 *
 * The screen paints the scheme background itself, in both themes, so the
 * text on it is always the scheme's text on the scheme's ground.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(vm: HavenViewModel) {
    var url by remember { mutableStateOf(vm.tokenStore.instanceUrl ?: "") }
    var token by remember { mutableStateOf("") }
    var showToken by rememberSaveable { mutableStateOf(false) }
    // The authorize page renders in-app (see OAuthWebViewScreen): the
    // phone's browser cannot always reach a LAN instance.
    var oauthUrl by remember { mutableStateOf<String?>(null) }
    if (oauthUrl != null) {
        BackHandler { oauthUrl = null }
        OAuthWebViewScreen(vm, oauthUrl!!) { oauthUrl = null }
        return
    }
    val discovered by vm.discoveredInstances.collectAsState()
    val conn = vm.connection.collectAsState().value
    val connecting = conn is ConnectionState.Connecting || conn is ConnectionState.Authenticating
    val busy = connecting || vm.probing
    // mDNS discovery runs only while the login screen is visible.
    DisposableEffect(Unit) {
        vm.discovery.startMdns()
        onDispose { vm.discovery.stopMdns() }
    }
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxSize().background(scheme.background).statusBarsPadding().navigationBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            // At least as tall as the window, so a short form sits in
            // the middle of a tall screen instead of hanging from the top.
            Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // Brand
            HavenMark(Modifier.size(88.dp), turning = true)
            Spacer(Modifier.height(20.dp))
            HavenWordmark(fontSize = 44.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Home Assistant, designed.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            // Where is your home?
            Surface(color = scheme.surfaceContainerLow, shape = leafCard(0)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Home Assistant address") },
                        placeholder = { Text("https://home.example.com") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("On your network", Modifier.weight(1f))
                        TextButton(onClick = { vm.scanNetwork() }, enabled = !vm.scanning) {
                            Text(if (vm.scanning) "Scanning…" else "Scan")
                        }
                    }
                    if (discovered.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LoadingIndicator(Modifier.size(20.dp), color = scheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Looking for Home Assistant on your Wi-Fi…",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        for (instance in discovered) {
                            val picked = instance.url == url
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (picked) scheme.primaryContainer else scheme.surfaceContainerHigh)
                                    .clickable {
                                        url = instance.url
                                        vm.preflight(instance.url) {}
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        instance.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (picked) scheme.onPrimaryContainer else scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        instance.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (picked) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    if (instance.source == DiscoverySource.MDNS) "Nearby" else "Scan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (picked) scheme.onPrimaryContainer else scheme.primary,
                                )
                            }
                        }
                    }
                    vm.scanMessage?.let { message ->
                        Spacer(Modifier.height(6.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // The main path
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        // Pre-flight first: a dead address gets an explanation
                        // here instead of a dead page. On success the authorize
                        // page opens in-app (OAuthWebViewScreen).
                        vm.preflight(url) { clean ->
                            oauthUrl = vm.startOAuth(clean).toString()
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Sign in with Home Assistant", style = MaterialTheme.typography.titleSmall) }
            Spacer(Modifier.height(6.dp))
            Text(
                "Opens your instance's sign-in page inside Haven.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // Status: what is happening, or what went wrong, right under the action.
            if (busy) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator(Modifier.size(24.dp), color = scheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (vm.probing) "Checking that address…" else "Connecting…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            vm.probeOk?.let { ok ->
                Spacer(Modifier.height(12.dp))
                Text(ok, style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
            }
            val problem = vm.authError ?: (conn as? ConnectionState.Error)?.message
            if (problem != null) {
                Spacer(Modifier.height(12.dp))
                Surface(color = scheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Text(
                        problem,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // The other path
            Surface(color = scheme.surfaceContainerLow, shape = leafCard(1)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showToken = !showToken }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Use an access token instead",
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onSurface,
                            )
                            Text(
                                "Works when the sign-in page can't be reached.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            if (showToken) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (showToken) "Hide" else "Show",
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                    AnimatedVisibility(visible = showToken) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            OutlinedTextField(
                                value = token,
                                onValueChange = { token = it },
                                label = { Text("Long-lived access token") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = {
                                    if (url.isNotBlank() && token.isNotBlank()) {
                                        vm.preflight(url) { clean -> vm.loginWithToken(clean, token.trim()) }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) { Text("Connect with token") }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Create one in Home Assistant under Profile → Security → Long-lived access tokens.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Haven talks only to your own instance. Tokens are encrypted on this device and never sent anywhere else.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
