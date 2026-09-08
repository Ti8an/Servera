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
    fun `two addresses seen once each are a tie and not guessed between`() {
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

    @Test
    fun `the address read correctly more often wins over a misread one`() {
        // The panel prints the address twice and OCR spoiled one occurrence -- a lowercase l
        // where a 1 belongs, which validation drops. What is left is the address, twice.
        val result = ScannedTextParser.parse(
            """
            IPv4
            147.45.142.41
            Хост 147.45.142.41
            Резерв 147.45.142.4l
            """.trimIndent()
        )

        assertEquals("147.45.142.41", result.host)
    }

    @Test
    fun `the more frequent of two valid addresses wins`() {
        // Both parse as addresses, so nothing but the count separates them.
        val result = ScannedTextParser.parse(
            """
            147.45.142.41
            147.45.142.11
            147.45.142.41
            """.trimIndent()
        )

        assertEquals("147.45.142.41", result.host)
    }

    @Test
    fun `three addresses seen once each are not guessed between`() {
        assertNull(ScannedTextParser.parse("10.0.0.1 10.0.0.2 10.0.0.3").host)
    }

    @Test
    fun `two addresses seen twice each are not guessed between`() {
        assertNull(ScannedTextParser.parse("10.0.0.1 10.0.0.2 10.0.0.1 10.0.0.2").host)
    }

    @Test
    fun `a host the ssh line cannot supply is recovered from the rest of the text`() {
        // Rule 1 half-succeeds: it reads the login off the ssh line, but the address there came
        // back with a digit too many and fails validation. Rule 4 then recovers the address from
        // the occurrence that survived -- the point of ranking occurrences rather than refusing
        // as soon as two readings disagree.
        val result = ScannedTextParser.parse(
            """
            IPv4
            147.45.142.41
            Подключение по SSH
            ssh root@147.45.142.411
            Root-пароль
            ********
            Нода
            kvmnvm-1143
            Закрытые порты
            3389, 25, 2525, 53413, 5060, 465, 587, 389
            """.trimIndent()
        )

        assertEquals("root", result.login)
        assertEquals("147.45.142.41", result.host)
        assertNull(result.port)
    }

    @Test
    fun `domain names are accepted as hosts`() {
        // A numeric label or two is ordinary in a real name; it is three in front that gives a
        // misread address away.
        listOf(
            "example.com",
            "srv1.eu-central.hosting.net",
            "localhost",
            "1.example.com",
            "10.20.example.com"
        ).forEach { host ->
            assertEquals(host, host, ScannedTextParser.parse("admin@$host").host)
        }
    }

    @Test
    fun `an address misread as a domain name is rejected`() {
        // Four valid labels ending in one that is not all digits: the numeric-TLD guard alone
        // lets these through, and each is a dotted quad with a letter picked up in the last
        // octet, never a name anyone would type.
        listOf("147.45.142.4l", "192.168.1.1a", "10.0.0.1x").forEach { host ->
            assertNull(host, ScannedTextParser.parse("admin@$host").host)
        }
    }

    @Test
    fun `an address misread as a domain name in the ssh line is recovered from the rest of the text`() {
        // The defect this guards: the ssh line held a letter in the last octet, that passed as
        // a domain name, rule 4 never ran, and the user saved a server that could only time out.
        val result = ScannedTextParser.parse(
            """
            IPv4
            147.45.142.41
            Подключение по SSH
            ssh root@147.45.142.4l
            Root-пароль
            ********
            Нода
            kvmnvm-1143
            Закрытые порты
            3389, 25, 2525, 53413, 5060, 465, 587, 389
            """.trimIndent()
        )

        assertEquals("root", result.login)
        assertEquals("147.45.142.41", result.host)
        assertNull(result.port)
    }

    @Test
    fun `an at sign misread as a letter still yields the target`() {
        val result = ScannedTextParser.parse("ssh rootO147.45.142.41")

        assertEquals("root", result.login)
        assertEquals("147.45.142.41", result.host)
        assertNull(result.port)
    }

    @Test
    fun `an at sign misread as a zero still yields the target`() {
        val result = ScannedTextParser.parse("ssh root0147.45.142.41")

        assertEquals("root", result.login)
        assertEquals("147.45.142.41", result.host)
        assertNull(result.port)
    }

    @Test
    fun `a misread at sign does not disturb the port flag`() {
        val result = ScannedTextParser.parse("ssh -p 2222 adminO10.0.0.1")

        assertEquals("admin", result.login)
        assertEquals("10.0.0.1", result.host)
        assertEquals(2222, result.port)
    }

    @Test
    fun `a misread at sign is repaired ahead of a domain name too`() {
        // Also pins the character set: were a lowercase o in it, "rootOexample.com" would split
        // validly in four places and the fallback would refuse to choose.
        val result = ScannedTextParser.parse("ssh rootOexample.com")

        assertEquals("root", result.login)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `two possible places for a misread at sign are not guessed between`() {
        // "rootO" then "0example.com", or "root" then "0example.com" -- both split into a valid
        // login and a valid host, and choosing between them would be a guess.
        val result = ScannedTextParser.parse("ssh rootO0example.com")

        assertNull(result.host)
        assertNull(result.login)
    }

    @Test
    fun `a literal at sign is used ahead of the fallback`() {
        val result = ScannedTextParser.parse("ssh root@147.45.142.41")

        assertEquals("root", result.login)
        assertEquals("147.45.142.41", result.host)
    }

    @Test
    fun `a panel block whose ssh line lost its at sign still yields the target`() {
        val result = ScannedTextParser.parse(
            """
            IPv4
            147.45.142.41
            Подключение по SSH
            ssh rootO147.45.142.41
            Root-пароль
            ********
            Нода
            kvmnvm-1143
            Закрытые порты
            3389, 25, 2525, 53413, 5060, 465, 587, 389
            """.trimIndent()
        )

        assertEquals("root", result.login)
        assertEquals("147.45.142.41", result.host)
        assertNull(result.port)
    }
}
