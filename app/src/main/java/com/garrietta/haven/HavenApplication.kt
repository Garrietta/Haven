package com.garrietta.haven

import android.app.Application
import android.content.Context
import com.garrietta.haven.core.auth.AuthManager
import com.garrietta.haven.core.auth.AuthState
import com.garrietta.haven.core.auth.isLanUrl
import com.garrietta.haven.core.camera.CameraService
import com.garrietta.haven.core.net.ConnState
import com.garrietta.haven.core.net.HaConnection
import com.garrietta.haven.core.net.HaRest
import com.garrietta.haven.core.net.NetworkMonitor
import com.garrietta.haven.core.store.HomeStore
import com.garrietta.haven.core.store.StateCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class HavenApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

/** The app's object graph: one of everything, created once per process. */
class AppGraph(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    val network = NetworkMonitor(context)
    val auth = AuthManager(context, http)
    val connection = HaConnection(scope, http, auth, network)
    val rest = HaRest(http, auth, connection)
    val store = HomeStore(scope, connection, StateCache(context))
    val cameras = CameraService(context, scope, connection, rest)
    val prefs = Prefs(context)

    init {
        scope.launch {
            auth.state.map { it is AuthState.SignedIn }.distinctUntilChanged().collect { signedIn ->
                if (signedIn) connection.start() else {
                    connection.stop()
                    store.clearOnSignOut()
                }
            }
        }
        // Learn the other side of the connection (LAN ↔ Nabu Casa) from HA itself.
        scope.launch {
            connection.state.filterIsInstance<ConnState.Connected>().collect {
                val (internal, external) = store.instanceUrls()
                auth.updateUrls(
                    local = internal?.takeIf { isLanUrl(it) } ?: auth.credentials?.localUrl,
                    remote = external ?: auth.credentials?.remoteUrl,
                )
            }
        }
    }
}

/** User preferences, observable. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("haven_prefs_v2", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(sp.getString("theme", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(sp.getBoolean("dynamic", false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _favorites = MutableStateFlow(sp.getString("favorites", null)?.split(',')?.filter { it.isNotBlank() })
    /** Null until the user pins something — the Home screen then picks sensible defaults. */
    val favorites: StateFlow<List<String>?> = _favorites.asStateFlow()

    fun setThemeMode(mode: String) { _themeMode.value = mode; sp.edit().putString("theme", mode).apply() }
    fun setDynamicColor(on: Boolean) { _dynamicColor.value = on; sp.edit().putBoolean("dynamic", on).apply() }

    fun setFavorites(ids: List<String>) {
        _favorites.value = ids
        sp.edit().putString("favorites", ids.joinToString(",")).apply()
    }

    fun toggleFavorite(id: String, current: List<String>) {
        setFavorites(if (id in current) current - id else current + id)
    }
}
