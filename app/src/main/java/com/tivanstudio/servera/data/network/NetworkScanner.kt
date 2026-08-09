package com.tivanstudio.servera.data.network

import android.content.Context
import android.net.ConnectivityManager
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
 * Sweeps the current /24 from the phone itself. Without root the picture is partial:
 * devices that drop ICMP and expose no common port stay invisible, and Android 10+
 * hides the ARP table, so MAC addresses are usually null.
 */
@Singleton
class NetworkScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Subnet(val base: String, val selfIp: String, val gatewayIp: String?)

    /** Reads the active network's IPv4 address and default route; null when not on one. */
    fun currentSubnet(): Subnet? = runCatching {
        val cm       = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network  = cm.activeNetwork ?: return null
        val props    = cm.getLinkProperties(network) ?: return null

        val selfIp = props.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
            ?: return null

        val base = selfIp.substringBeforeLast('.') + "."

        val gatewayIp = props.routes
            .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway
            ?.hostAddress

        Subnet(base = base, selfIp = selfIp, gatewayIp = gatewayIp)
    }.getOrNull()

    /**
     * Probes every host in the subnet, reporting (scanned, total) as results come in.
     * Returns the responders sorted by numeric IP.
     */
    suspend fun scan(onProgress: (Int, Int) -> Unit): List<NetworkDevice> =
        withContext(Dispatchers.IO) {
            val subnet = currentSubnet() ?: return@withContext emptyList()
            val total  = TOTAL_HOSTS
            val done   = AtomicInteger(0)
            val gate   = Semaphore(PARALLELISM)

            coroutineScope {
                (1..TOTAL_HOSTS).map { host ->
                    async {
                        val ip = subnet.base + host
                        val alive = gate.withPermit { isReachable(ip) }
                        onProgress(done.incrementAndGet(), total)
                        if (!alive) return@async null

                        NetworkDevice(
                            ip        = ip,
                            hostname  = resolveHostname(ip),
                            mac       = readArpMac(ip),
                            isSelf    = ip == subnet.selfIp,
                            isGateway = ip == subnet.gatewayIp
                        )
                    }
                }.awaitAll()
            }
                .filterNotNull()
                .sortedBy { it.ip.toIpSortKey() }
        }

    /** ICMP first; a TCP handshake on common ports catches hosts that ignore ping. */
    private fun isReachable(ip: String): Boolean {
        val pinged = runCatching {
            InetAddress.getByName(ip).isReachable(PING_TIMEOUT_MS)
        }.getOrDefault(false)
        if (pinged) return true

        return PROBE_PORTS.any { port ->
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
                    true
                }
            }.getOrDefault(false)
        }
    }

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
        const val TOTAL_HOSTS       = 254
        const val PARALLELISM       = 32
        const val PING_TIMEOUT_MS   = 300
        const val CONNECT_TIMEOUT_MS = 200
        const val EMPTY_MAC         = "00:00:00:00:00:00"
        val PROBE_PORTS = listOf(80, 443, 22, 445)
    }
}
