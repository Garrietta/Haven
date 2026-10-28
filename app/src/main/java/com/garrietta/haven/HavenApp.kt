package com.garrietta.haven

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.ui.screens.AreasScreen
import com.garrietta.haven.ui.screens.AreaDetailScreen
import com.garrietta.haven.ui.screens.AutomationBuilderScreen
import com.garrietta.haven.ui.screens.DashboardEditScreen
import com.garrietta.haven.ui.screens.DashboardsScreen
import com.garrietta.haven.ui.screens.EntityDetailScreen
import com.garrietta.haven.ui.screens.HelperBuilderScreen
import com.garrietta.haven.ui.screens.LoginScreen
import com.garrietta.haven.ui.screens.SceneBuilderScreen
import com.garrietta.haven.ui.screens.ScriptBuilderScreen
import com.garrietta.haven.ui.screens.SearchScreen
import com.garrietta.haven.ui.screens.SettingsScreen
import com.garrietta.haven.ui.screens.TodayScreen
import com.garrietta.haven.ui.components.LocalReduceMotion
import com.garrietta.haven.ui.theme.HavenTheme
import com.garrietta.haven.ui.theme.havenSpring

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val TOP_NAV = listOf(
    NavItem(Screen.Today, "Today", Icons.Filled.Home),
    NavItem(Screen.Dashboards, "Boards", Icons.Filled.GridView),
    NavItem(Screen.Areas, "Areas", Icons.Filled.LocationOn),
    NavItem(Screen.Search, "Search", Icons.Filled.Search),
    NavItem(Screen.Settings, "Settings", Icons.Filled.Settings),
)

private fun isTopLevel(screen: Screen): Boolean = when (screen) {
    Screen.Today, Screen.Dashboards, Screen.Areas, Screen.Search, Screen.Settings -> true
    else -> false
}

@Composable
fun HavenApp(vm: HavenViewModel) {
    var themeMode by remember { mutableStateOf(vm.settingsStore.themeMode) }
    var reduceMotion by remember { mutableStateOf(vm.settingsStore.reduceMotion) }
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    HavenTheme(darkTheme = darkTheme) {
        if (!vm.isAuthenticated) {
            LoginScreen(vm)
            return@HavenTheme
        }
        val backstack = remember { mutableStateListOf<Screen>(Screen.Today) }
        val current = backstack.last()
        val navigate: (Screen) -> Unit = { backstack.add(it) }
        val goTop: (Screen) -> Unit = {
            backstack.clear()
            backstack.add(it)
        }
        val goBack: () -> Unit = {
            if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
        }
        BackHandler(enabled = backstack.size > 1) { goBack() }
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        Scaffold(
            bottomBar = {
                if (isTopLevel(current)) {
                    PillNav(
                        current = current,
                        onSelect = goTop,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (val screen = current) {
                    Screen.Today -> TodayScreen(vm, navigate)
                    Screen.Dashboards -> DashboardsScreen(vm, navigate)
                    Screen.Areas -> AreasScreen(vm, navigate)
                    Screen.Search -> SearchScreen(vm, navigate)
                    Screen.Settings -> SettingsScreen(
                        vm = vm,
                        themeMode = themeMode,
                        reduceMotion = reduceMotion,
                        onThemeMode = {
                            themeMode = it
                            vm.settingsStore.themeMode = it
                        },
                        onReduceMotion = {
                            reduceMotion = it
                            vm.settingsStore.reduceMotion = it
                        },
                        onSignOut = {
                            vm.signOut()
                            backstack.clear()
                            backstack.add(Screen.Today)
                        },
                        navigate = navigate,
                    )
                    is Screen.EntityDetail -> EntityDetailScreen(vm, screen.entityId, goBack)
                    is Screen.AreaDetail -> AreaDetailScreen(vm, screen.areaId, goBack, navigate)
                    Screen.DashboardEdit -> DashboardEditScreen(vm, goBack)
                    Screen.AutomationBuilder -> AutomationBuilderScreen(vm, goBack)
                    Screen.SceneBuilder -> SceneBuilderScreen(vm, goBack)
                    Screen.ScriptBuilder -> ScriptBuilderScreen(vm, goBack)
                    Screen.HelperBuilder -> HelperBuilderScreen(vm, goBack)
                }
                // A failed tap used to vanish silently; now it banners here on
                // every screen until dismissed.
                val serviceError by vm.serviceError.collectAsState()
                if (serviceError != null) {
                    ServiceErrorBanner(
                        serviceError!!,
                        onDismiss = vm::dismissServiceError,
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    )
                }
            }
        }
        }
    }
}

/** Dismissible banner for the last service-call failure. */
@Composable
private fun ServiceErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/** The pill-bar bottom navigation: one pill per destination, springy selection. */
@Composable
fun PillNav(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in TOP_NAV) {
                val selected = item.screen == current
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    animationSpec = havenSpring(),
                    label = "pill",
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .clickable { if (!selected) onSelect(item.screen) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/** Back arrow + title row used by every detail screen. */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
