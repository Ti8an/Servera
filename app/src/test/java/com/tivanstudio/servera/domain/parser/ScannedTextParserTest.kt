package com.tivanstudio.servera.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannedTextParserTest {

    /** A hoster panel screen, verbatim -- the case the scanner exists for. */
    private val hosterPanel = """
        IPv4
        203.0.113.10
        Подключение по SSH
        ssh root@203.0.113.10
        Root-пароль
        ********
        Нода
        sdawada-422
        Закрытые порты
        587, 3389, 2525, 25, 465, 53413, 389
    """.trimIndent()

    @Test
    fun `a hoster panel block yields host and login`() {
        val result = ScannedTextParser.parse(hosterPanel)

        assertEquals("203.0.113.10", result.host)
        assertEquals("root", result.login)
    }

    @Test
    fun `a closed ports list is never mistaken for a port`() {
        // Regression guard for the rule that a port comes only from an explicit -p flag or a
        // ssh:// URI. Any "number near the address" heuristic would pick 587 here, the server
        // would save and never connect, and the user would see nothing but a timeout.
        assertNull(ScannedTextParser.parse(hosterPanel).port)
    }

    @Test
    fun `a port flag before the target is read`() {
        val result = ScannedTextParser.parse("ssh -p 2222 admin@example.com")

        assertEquals("example.com", result.host)
        assertEquals("admin", result.login)
        assertEquals(2222, result.port)
    }

    @Test
    fun `a port flag after the target is read`() {
        val result = ScannedTextParser.parse("ssh admin@example.com -p 2222")

        assertEquals("example.com", result.host)
        assertEquals("admin", result.login)
        assertEquals(2222, result.port)
    }

    @Test
    fun `an ssh command without a port flag leaves the port empty`() {
        val result = ScannedTextParser.parse("ssh admin@example.com")

        assertEquals("example.com", result.host)
        assertEquals("admin", result.login)
        assertNull(result.port)
    }

    @Test
    fun `a ssh URI yields all three fields`() {
        val result = ScannedTextParser.parse("ssh://admin@10.0.0.1:2200")

        assertEquals("10.0.0.1", result.host)
        assertEquals("admin", result.login)
        assertEquals(2200, result.port)
    }

    @Test
    fun `the user and port parts of a URI are optional`() {
        val result = ScannedTextParser.parse("ssh://example.com")

        assertEquals("example.com", result.host)
        assertNull(result.login)
        assertNull(result.port)
    }

    @Test
    fun `a port out of range is dropped without losing the rest`() {
        val result = ScannedTextParser.parse("ssh root@203.0.113.10 -p 70000")

        assertEquals("203.0.113.10", result.host)
        assertEquals("root", result.login)
        assertNull(result.port)
    }

    @Test
    fun `a bare user at host yields host and login but no port`() {
        val result = ScannedTextParser.parse("Доступ: admin@srv1.eu-central.hosting.net")

        assertEquals("srv1.eu-central.hosting.net", result.host)
        assertEquals("admin", result.login)
        assertNull(result.port)
    }

    @Test
    fun `a standalone address fills the host`() {
        val result = ScannedTextParser.parse("IPv4\n203.0.113.10")

        assertEquals("203.0.113.10", result.host)
        assertNull(result.login)
        assertNull(result.port)
    }

    @Test
    fun `the same address repeated is still one address`() {
        assertEquals("10.0.0.1", ScannedTextParser.parse("10.0.0.1\nадрес 10.0.0.1").host)
    }

    @Test
    fun `an octet above 255 is not a host`() {
        assertNull(ScannedTextParser.parse("999.1.1.1").host)
    }

    @Test
    fun `a leading zero is not a host`() {
        // Part of the resolvers read 010 as octal, so this is not the address on the screen.
        assertNull(ScannedTextParser.parse("010.1.1.1").host)
    }

    @Test
    fun `a wrong number of octets is not a host`() {
        listOf("1.2.3", "1.2.3.4.5").forEach { text ->
            assertNull(text, ScannedTextParser.parse(text).host)
        }
    }

    @Test
    fun `two different addresses are not guessed between`() {
        val result = ScannedTextParser.parse("IPv4 203.0.113.10\nШлюз 192.168.1.1")

        assertNull(result.host)
        assertNull(result.port)
    }

    @Test
    fun `an ssh command wins over a second address in the text`() {
        val result = ScannedTextParser.parse("Шлюз 192.168.1.1\nssh root@203.0.113.10")

        assertEquals("203.0.113.10", result.host)
        assertEquals("root", result.login)
    }

    @Test
    fun `a numeric login is dropped`() {
        val result = ScannedTextParser.parse("ssh 12345@203.0.113.10")

        assertEquals("203.0.113.10", result.host)
        assertNull(result.login)
    }

    @Test
    fun `an empty text yields nothing`() {
        assertTrue(ScannedTextParser.parse("").isEmpty)
        assertTrue(ScannedTextParser.parse("   \n  ").isEmpty)
    }

    @Test
    fun `a text without a single match yields nothing`() {
        val result = ScannedTextParser.parse("Тариф VDS-4, оплачено до 01.12.2026, поддержка 24/7")

        assertTrue(result.isEmpty)
        assertEquals(0, result.filledCount)
    }
}
