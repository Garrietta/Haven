package com.garrietta.haven.data

import java.net.InetAddress
import java.net.Inet4Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryTest {

    private fun ipv4(text: String): Inet4Address = InetAddress.getByName(text) as Inet4Address

    @Test
    fun subnetHosts_slash24ExcludesNetworkBroadcastAndSelf() {
        val hosts = subnetHosts(ipv4("192.168.1.50"), 24)
        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertFalse(hosts.contains("192.168.1.0"))
        assertFalse(hosts.contains("192.168.1.255"))
        assertFalse(hosts.contains("192.168.1.50"))
    }

    @Test
    fun subnetHosts_slash25ScansOnlyItsHalf() {
        val hosts = subnetHosts(ipv4("10.0.0.130"), 25)
        // Usable 10.0.0.129..254 (126) minus the device itself = 125.
        assertEquals(125, hosts.size)
        assertEquals("10.0.0.129", hosts.first())
        assertEquals("10.0.0.254", hosts.last())
        assertFalse(hosts.contains("10.0.0.128"))
        assertFalse(hosts.contains("10.0.0.130"))
    }

    @Test
    fun subnetHosts_garrettsSubnetContainsHisInstanceAddress() {
        // His instance lives at 10.0.0.11; a phone on the same /24 must probe it.
        val hosts = subnetHosts(ipv4("10.0.0.42"), 24)
        assertTrue(hosts.contains("10.0.0.11"))
    }
}
