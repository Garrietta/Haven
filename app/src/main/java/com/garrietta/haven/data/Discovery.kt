package com.garrietta.haven.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

/** One Home Assistant instance found on the local network. */
data class DiscoveredInstance(
    val name: String,
    val url: String,
    val source: DiscoverySource,
)

enum class DiscoverySource { MDNS, SCAN }

/** Outcome of a subnet scan. */
sealed class ScanOutcome {
    /** Scan ran; [found] instances fingerprinted as Home Assistant. */
    data class Done(val found: Int) : ScanOutcome()

    /** Not on Wi-Fi (off, or cellular) — scanning the LAN is meaningless. */
    data object NeedsWifi : ScanOutcome()

    /** No local IPv4 address to derive a subnet from. */
    data object NoAddress : ScanOutcome()
}

/**
 * Finds Home Assistant instances on the local network, two ways:
 *
 * 1. mDNS via Android NSD: Home Assistant advertises `_home-assistant._tcp`, the
 *    same mechanism the official app uses. A found service is resolved to a host
 *    and port, then fingerprinted with the manifest probe before it is listed.
 * 2. Subnet scan fallback: probe port 8123 across the phone's /24 (or smaller
 *    subnet) with short timeouts, and fingerprint each open port the same way.
 *
 * Results from both paths merge into one deduplicated list by URL. Discovery
 * never logs the user in by itself — a tapped result prefills the URL field and
 * feeds the same pre-flight + OAuth/token paths as a typed address.
 */
class HaDiscovery(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _found = MutableStateFlow<List<DiscoveredInstance>>(emptyList())
    val found: StateFlow<List<DiscoveredInstance>> = _found.asStateFlow()

    private var discovering = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onDiscoveryStarted(serviceType: String) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) {
            try {
                nsdManager.resolveService(service, resolveListener)
            } catch (e: Exception) {
                // Resolve can throw if discovery stopped mid-flight; ignore.
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // Keep listed results; a lost advertisement is not proof the host left.
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val host = serviceInfo.host ?: return
            val port = serviceInfo.port
            if (port <= 0) return
            val hostPart = if (host is Inet4Address || !host.hostAddress.orEmpty().contains(":")) {
                host.hostAddress ?: return
            } else {
                "[${host.hostAddress}]"
            }
            val url = "http://$hostPart:$port"
            // Fingerprint before listing: an mDNS advertisement alone is not proof.
            CoroutineScope(Dispatchers.IO).launch {
                if (probeInstance(url, OkHttpClient()) is ProbeResult.Success) {
                    addFound(DiscoveredInstance(serviceInfo.serviceName, url, DiscoverySource.MDNS))
                }
            }
        }
    }

    /** Starts mDNS discovery; safe to call repeatedly (second call is a no-op). */
    fun startMdns() {
        if (discovering) return
        discovering = true
        try {
            nsdManager.discoverServices("_home-assistant._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            discovering = false
        }
    }

    fun stopMdns() {
        if (!discovering) return
        discovering = false
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            // Already stopped.
        }
    }

    /**
     * Scans the phone's local subnet for Home Assistant on port 8123. Runs the
     * port probes concurrently and fingerprints each open port before listing it.
     */
    suspend fun scanSubnet(http: OkHttpClient): ScanOutcome = withContext(Dispatchers.IO) {
        if (!isOnWifi(appContext)) return@withContext ScanOutcome.NeedsWifi
        val local = localIpv4(appContext) ?: return@withContext ScanOutcome.NoAddress
        val hosts = subnetHosts(local.first, local.second)
        val semaphore = Semaphore(32)
        var foundCount = 0
        coroutineScope {
            hosts.map { host ->
                async {
                    semaphore.acquire()
                    try {
                        if (isPortOpen(host, 8123, 400)) {
                            val url = "http://$host:8123"
                            if (probeInstance(url, http) is ProbeResult.Success) {
                                addFound(DiscoveredInstance(host, url, DiscoverySource.SCAN))
                                foundCount++
                            }
                        }
                    } catch (e: Exception) {
                        // One host failing must never abort the scan.
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll()
        }
        ScanOutcome.Done(foundCount)
    }

    private fun addFound(instance: DiscoveredInstance) {
        val current = _found.value
        if (current.any { it.url == instance.url }) return
        _found.value = current + instance
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

/** True when the active network is Wi-Fi (discovery is meaningless on cellular). */
fun isOnWifi(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

/** The phone's local IPv4 address plus prefix length, or null if none. */
fun localIpv4(context: Context): Pair<Inet4Address, Int>? {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null
    val props = manager.getLinkProperties(manager.activeNetwork) ?: return null
    for (addr in props.linkAddresses) {
        val inet = addr.address
        if (inet is Inet4Address && !inet.isLoopbackAddress) {
            return inet to addr.prefixLength
        }
    }
    return null
}

/**
 * Host addresses to probe for a subnet: the device's own /24, or the smaller
 * subnet when the prefix is longer than 24. The network, broadcast, and device
 * addresses are excluded. Pure function, unit-tested.
 */
fun subnetHosts(address: Inet4Address, prefixLength: Int): List<String> {
    val bytes = address.address
    val ip = ((bytes[0].toInt() and 0xFF) shl 24) or
        ((bytes[1].toInt() and 0xFF) shl 16) or
        ((bytes[2].toInt() and 0xFF) shl 8) or
        (bytes[3].toInt() and 0xFF)
    val prefix = prefixLength.coerceIn(24, 30)
    val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
    val network = ip and mask
    val broadcast = network or mask.inv()
    val hosts = mutableListOf<String>()
    var candidate = network + 1
    while (candidate < broadcast) {
        if (candidate != ip) {
            hosts.add(
                "${(candidate ushr 24) and 0xFF}.${(candidate ushr 16) and 0xFF}." +
                    "${(candidate ushr 8) and 0xFF}.${candidate and 0xFF}",
            )
        }
        candidate++
    }
    return hosts
}
