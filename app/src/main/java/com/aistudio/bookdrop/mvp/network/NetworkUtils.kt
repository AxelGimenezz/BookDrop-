package com.aistudio.bookdrop.mvp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

/** Dirección IPv4 alcanzable desde otro equipo de la red local. */
data class LanAddress(
    val ip: String,
    val interfaceName: String,
    val transportName: String
)

object NetworkUtils {

    /**
     * Busca primero una IPv4 perteneciente a una red Wi-Fi o Ethernet conocida por Android.
     * Como respaldo, inspecciona interfaces de hotspot/USB/Ethernet, pero descarta datos
     * móviles, VPN, loopback y direcciones públicas.
     */
    fun getLanAddress(context: Context): LanAddress? {
        findFromConnectivityManager(context)?.let { return it }
        return findFromNetworkInterfaces()
    }

    private fun findFromConnectivityManager(context: Context): LanAddress? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val candidates = mutableListOf<Pair<Int, LanAddress>>()

        for (network in connectivityManager.allNetworks) {
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: continue
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue

            val transport = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> continue
            }

            val linkProperties = connectivityManager.getLinkProperties(network) ?: continue
            val interfaceName = linkProperties.interfaceName ?: transport.lowercase()

            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is Inet4Address && isUsableLanAddress(address)) {
                    val score = when (transport) {
                        "Wi-Fi" -> 200
                        "Ethernet" -> 180
                        else -> 0
                    }
                    candidates += score to LanAddress(
                        ip = address.hostAddress ?: continue,
                        interfaceName = interfaceName,
                        transportName = transport
                    )
                }
            }
        }

        return candidates.maxByOrNull { it.first }?.second
    }

    private fun findFromNetworkInterfaces(): LanAddress? {
        val candidates = mutableListOf<Pair<Int, LanAddress>>()

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val name = networkInterface.name.orEmpty().lowercase()
                val scoreAndTransport = scoreInterface(name) ?: continue
                val (score, transport) = scoreAndTransport

                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (address is Inet4Address && isUsableLanAddress(address)) {
                        candidates += score to LanAddress(
                            ip = address.hostAddress ?: continue,
                            interfaceName = networkInterface.name ?: "LAN",
                            transportName = transport
                        )
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }

        return candidates.maxByOrNull { it.first }?.second
    }

    private fun scoreInterface(name: String): Pair<Int, String>? {
        // Interfaces típicas de red móvil o túneles: nunca deben mostrarse como URL LAN.
        val rejectedPrefixes = listOf(
            "rmnet", "ccmni", "pdp", "wwan", "tun", "tap", "wg", "tailscale", "dummy", "lo"
        )
        if (rejectedPrefixes.any { name.startsWith(it) }) return null

        return when {
            name.startsWith("wlan") || name.startsWith("wifi") -> 150 to "Wi-Fi"
            name.startsWith("ap") || name.startsWith("swlan") -> 145 to "Hotspot Wi-Fi"
            name.startsWith("eth") -> 140 to "Ethernet"
            name.startsWith("rndis") || name.startsWith("usb") -> 130 to "USB LAN"
            name.startsWith("bnep") -> 110 to "Bluetooth LAN"
            else -> null
        }
    }

    private fun isUsableLanAddress(address: Inet4Address): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress) {
            return false
        }

        val bytes = address.address
        val first = bytes[0].toInt() and 0xFF
        val second = bytes[1].toInt() and 0xFF

        return first == 10 ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168)
    }
}
