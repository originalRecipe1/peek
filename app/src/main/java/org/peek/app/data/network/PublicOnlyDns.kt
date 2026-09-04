package org.peek.app.data.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

class PublicOnlyDns(
    private val delegate: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { !it.isPublicNetworkAddress() }) {
            throw UnsafeNetworkTargetException()
        }
        return addresses
    }
}

class UnsafeNetworkTargetException : UnknownHostException(
    "Blocked a non-public network target",
)

internal fun InetAddress.isPublicNetworkAddress(): Boolean {
    if (
        isAnyLocalAddress ||
        isLoopbackAddress ||
        isLinkLocalAddress ||
        isSiteLocalAddress ||
        isMulticastAddress
    ) {
        return false
    }

    val octets = address.map(Byte::toInt).map { it and 0xff }
    return when (octets.size) {
        4 -> octets.isPublicIpv4Address()
        16 -> octets.isPublicIpv6Address()
        else -> false
    }
}

private fun List<Int>.isPublicIpv4Address(): Boolean {
    val first = this[0]
    val second = this[1]
    val third = this[2]
    return when {
        first == 0 -> false
        first == 10 -> false
        first == 100 && second in 64..127 -> false
        first == 127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 0 && third == 0 -> false
        first == 192 && second == 0 && third == 2 -> false
        first == 192 && second == 88 && third == 99 -> false
        first == 192 && second == 168 -> false
        first == 198 && second in 18..19 -> false
        first == 198 && second == 51 && third == 100 -> false
        first == 203 && second == 0 && third == 113 -> false
        first >= 224 -> false
        else -> true
    }
}

private fun List<Int>.isPublicIpv6Address(): Boolean {
    // Preserve IPv6-only Android connectivity through the well-known NAT64
    // prefix while applying the IPv4 policy to its embedded destination.
    if (take(12) == WELL_KNOWN_NAT64_PREFIX) {
        return takeLast(4).isPublicIpv4Address()
    }

    // Globally routable unicast IPv6 space is currently allocated from 2000::/3.
    if (this[0] and 0xe0 != 0x20) return false

    return when {
        // IETF protocol assignments include Teredo and other transition ranges.
        this[0] == 0x20 && this[1] == 0x01 && this[2] < 0x02 -> false
        // Documentation prefix 2001:db8::/32.
        this[0] == 0x20 && this[1] == 0x01 && this[2] == 0x0d && this[3] == 0xb8 -> false
        // 6to4 can encode an otherwise blocked IPv4 destination.
        this[0] == 0x20 && this[1] == 0x02 -> false
        else -> true
    }
}

private val WELL_KNOWN_NAT64_PREFIX = listOf(
    0x00, 0x64, 0xff, 0x9b,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
)
