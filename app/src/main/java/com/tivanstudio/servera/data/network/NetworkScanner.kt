package com.tivanstudio.servera.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import com.tivanstudio.servera.domain.entity.NetworkDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sweeps the local /24 from the phone itself. Without root the picture is partial:
 * hosts that drop ICMP and expose no common port stay invisible, and Android 10+
 * hides the ARP table, so MAC addresses are usually null.
 */
@Singleton
class NetworkScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Subnet(val base: String, val selfIp: String, val gatewayIp: String?)

    /**
     * Address of the real local link. A VPN would otherwise win as the active network
     * and point the sweep at the tunnel instead of the LAN, so it is skipped.
     */
    fun currentSubnet(): Subnet? = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val props = cm.localLinkProperties() ?: return null

        val selfIp = props.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
            ?: return null

        val gatewayIp = props.routes
            .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway
            ?.hostAddress

        Subnet(
            base      = selfIp.substringBeforeLast('.') + ".",
            selfIp    = selfIp,
            gatewayIp = gatewayIp
        )
    }.getOrNull()

    private fun ConnectivityManager.localLinkProperties(): LinkProperties? {
        val physical = allNetworks.firstOrNull { network ->
            val caps = getNetworkCapabilities(network) ?: return@firstOrNull false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                getLinkProperties(network)
                    ?.linkAddresses
                    ?.any { it.address is Inet4Address && !it.address.isLoopbackAddress } == true
        }
        return getLinkProperties(physical ?: activeNetwork ?: return null)
    }

    /**
     * Probes every host in the subnet, reporting (scanned, total) as results come in.
     * Returns only hosts that answered, sorted by numeric IP.
     */
    suspend fun scan(onProgress: (Int, Int) -> Unit): List<NetworkDevice> =
        withContext(Dispatchers.IO) {
            val subnet = currentSubnet() ?: return@withContext emptyList()
            val total  = TOTAL_HOSTS
            val done   = AtomicInteger(0)
            val gate   = Semaphore(PARALLELISM)
            // A VPN or transparent proxy completes every handshake, which would mark the
            // whole range alive. Probe first; if that happens, ICMP is the only evidence left.
            val tcpUsable = tcpProbingIsReliable(subnet.base)

            coroutineScope {
                (1..TOTAL_HOSTS).map { host ->
                    async {
                        val ip = subnet.base + host
                        val alive = gate.withPermit { isReachable(ip, tcpUsable) }
                        onProgress(done.incrementAndGet(), total)

                        if (!alive) {
                            null
                        } else {
                            NetworkDevice(
                                ip        = ip,
                                hostname  = resolveHostname(ip),
                                mac       = readArpMac(ip),
                                isSelf    = ip == subnet.selfIp,
                                isGateway = ip == subnet.gatewayIp
                            )
                        }
                    }
                }.awaitAll()
            }
                .filterNotNull()
                .filter { it.ip.isNotBlank() }
                .sortedBy { it.ip.toIpSortKey() }
        }

    /**
     * Connects to a port nothing realistically listens on, at addresses unlikely to
     * exist. A success means something is answering for the entire range, so TCP
     * carries no information and must not be trusted.
     */
    private fun tcpProbingIsReliable(base: String): Boolean =
        CANARY_HOSTS.none { host -> tcpConnects(base + host, CANARY_PORT) }

    /** ICMP is the trustworthy signal; open ports only add hosts that ignore ping. */
    private fun isReachable(ip: String, allowTcp: Boolean): Boolean {
        if (respondsToPing(ip)) return true
        if (!allowTcp) return false
        return PROBE_PORTS.any { port -> tcpConnects(ip, port) }
    }

    private fun respondsToPing(ip: String): Boolean = runCatching {
        InetAddress.getByName(ip).isReachable(PING_TIMEOUT_MS)
    }.getOrDefault(false)

    /** True only for a completed handshake: a refusal (RST) throws and counts as closed. */
    private fun tcpConnects(ip: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
            socket.isConnected
        }
    }.getOrDefault(false)

    private fun resolveHostname(ip: String): String? = runCatching {
        InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
    }.getOrNull()

    /** Best-effort: /proc/net/arp is unreadable on Android 10+, so this usually returns null. */
    private fun readArpMac(ip: String): String? = runCatching {
        File("/proc/net/arp").useLines { lines ->
            lines.drop(1)
                .map { it.split(Regex("\\s+")) }
                .firstOrNull { it.size >= 4 && it[0] == ip }
                ?.get(3)
                ?.takeIf { it != EMPTY_MAC }
        }
    }.getOrNull()

    private fun String.toIpSortKey(): Long =
        split('.').fold(0L) { acc, part -> acc shl 8 or (part.toLongOrNull() ?: 0L) }

    private companion object {
        const val TOTAL_HOSTS        = 254
        const val PARALLELISM        = 32
        const val PING_TIMEOUT_MS    = 500
        const val CONNECT_TIMEOUT_MS = 250
        const val EMPTY_MAC          = "00:00:00:00:00:00"
        const val CANARY_PORT        = 47156
        val PROBE_PORTS  = listOf(80, 443, 22)
        val CANARY_HOSTS = listOf(251, 252, 253)
    }
}
