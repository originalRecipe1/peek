package org.peek.app.data.network

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class PublicOnlyDnsTest {
    @Test
    fun `returns public IPv4 and IPv6 addresses`() {
        val expected = listOf(
            address("8.8.8.8"),
            address("2606:4700:4700::1111"),
            address("64:ff9b::808:808"),
        )
        val dns = PublicOnlyDns(dnsReturning(expected))

        assertEquals(expected, dns.lookup("example.com"))
    }

    @Test
    fun `rejects private and special-use IPv4 ranges`() {
        val blocked = listOf(
            "0.0.0.1",
            "10.0.0.1",
            "100.64.0.1",
            "127.0.0.1",
            "169.254.169.254",
            "172.16.0.1",
            "192.0.0.1",
            "192.0.2.1",
            "192.168.1.1",
            "198.18.0.1",
            "198.51.100.1",
            "203.0.113.1",
            "224.0.0.1",
        )

        blocked.forEach { candidate ->
            val dns = PublicOnlyDns(dnsReturning(listOf(address(candidate))))
            assertThrows(candidate, UnsafeNetworkTargetException::class.java) {
                dns.lookup("example.com")
            }
        }
    }

    @Test
    fun `rejects private transition and documentation IPv6 ranges`() {
        val blocked = listOf(
            "::1",
            "fe80::1",
            "fd00::1",
            "2001:db8::1",
            "2002:7f00:1::",
            "64:ff9b::7f00:1",
        )

        blocked.forEach { candidate ->
            val dns = PublicOnlyDns(dnsReturning(listOf(address(candidate))))
            assertThrows(candidate, UnsafeNetworkTargetException::class.java) {
                dns.lookup("example.com")
            }
        }
    }

    @Test
    fun `rejects a mixed public and private DNS response`() {
        val dns = PublicOnlyDns(
            dnsReturning(listOf(address("8.8.8.8"), address("127.0.0.1"))),
        )

        assertThrows(UnsafeNetworkTargetException::class.java) {
            dns.lookup("example.com")
        }
    }

    @Test
    fun `preserves DNS resolution failures`() {
        val expected = UnknownHostException("not found")
        val dns = PublicOnlyDns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = throw expected
        })

        assertEquals(
            expected,
            assertThrows(UnknownHostException::class.java) {
                dns.lookup("example.invalid")
            },
        )
    }

    private fun address(value: String): InetAddress = InetAddress.getByName(value)

    private fun dnsReturning(addresses: List<InetAddress>): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = addresses
    }
}
