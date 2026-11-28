package com.garrietta.haven.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Network facts the connection planner needs: online at all, and whether
 * the active network can reach the LAN (Wi-Fi / Ethernet / VPN). Emits on
 * [changes] when the default network's transport changes, so the
 * connection re-plans immediately instead of waiting for a timeout.
 */
class NetworkMonitor(context: Context) {
    private val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    @Volatile private var lastKey: String? = null

    init {
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = check(caps)
                override fun onLost(network: Network) {
                    lastKey = "none"
                    _changes.tryEmit(Unit)
                }
            })
        } catch (_: Exception) {
        }
    }

    private fun check(caps: NetworkCapabilities) {
        val key = buildString {
            append(if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "w" else "")
            append(if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) "e" else "")
            append(if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) "v" else "")
            append(if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) "c" else "")
        }
        val previous = lastKey
        lastKey = key
        if (previous != null && previous != key) _changes.tryEmit(Unit)
    }

    private fun caps(): NetworkCapabilities? = try {
        cm.getNetworkCapabilities(cm.activeNetwork)
    } catch (_: Exception) {
        null
    }

    fun isOnline(): Boolean = caps()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true ||
        caps()?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    fun isOnLocalNetwork(): Boolean {
        val c = caps() ?: return false
        return c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
