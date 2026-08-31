package com.tivanstudio.servera

import com.tivanstudio.servera.presentation.servers.add.validateHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HostValidationTest {

    @Test
    fun `an octet above 255 is rejected`() {
        listOf("152.96.95.485", "256.0.0.1", "10.0.999.1", "1.2.3.4444").forEach { host ->
            assertEquals(host, R.string.invalid_ip, validateHost(host))
        }
    }

    @Test
    fun `well formed addresses pass`() {
        listOf("192.168.1.1", "0.0.0.0", "255.255.255.255", "8.8.8.8").forEach { host ->
            assertNull(host, validateHost(host))
        }
    }

    @Test
    fun `domain names are not validated as addresses`() {
        listOf("example.com", "my-server.local", "srv1.eu-central.hosting.net", "localhost")
            .forEach { host -> assertNull(host, validateHost(host)) }
    }

    @Test
    fun `strings that only resemble an address are left to the resolver`() {
        // Not four groups of digits, so nothing here is a typo this check can be sure about.
        listOf("192.168.1", "1.2.3.4.5", "10.0.0.1a").forEach { host ->
            assertNull(host, validateHost(host))
        }
    }

    @Test
    fun `a blank host is required`() {
        listOf("", "   ").forEach { host ->
            assertEquals(R.string.error_host_required, validateHost(host))
        }
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertNull(validateHost("  192.168.1.1  "))
        assertEquals(R.string.invalid_ip, validateHost(" 152.96.95.485 "))
    }
}
