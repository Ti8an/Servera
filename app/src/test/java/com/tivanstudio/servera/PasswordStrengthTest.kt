package com.tivanstudio.servera

import com.tivanstudio.servera.presentation.auth.PasswordStrength
import com.tivanstudio.servera.presentation.auth.checkPassword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordStrengthTest {

    @Test
    fun `shorter than eight characters is rejected`() {
        listOf("", "a1", "abc123", "abc1234").forEach { password ->
            val check = checkPassword(password)
            assertFalse(password, check.valid)
            assertEquals(PasswordStrength.WEAK, check.strength)
            assertEquals(R.string.pwd_too_short, check.errorRes)
        }
    }

    @Test
    fun `long enough but missing a letter or a digit is rejected`() {
        listOf("12345678", "abcdefgh", "!@#$%^&*").forEach { password ->
            val check = checkPassword(password)
            assertFalse(password, check.valid)
            assertEquals(R.string.pwd_need_letter_digit, check.errorRes)
        }
    }

    @Test
    fun `letter and digit at the minimum length is medium`() {
        val check = checkPassword("abcdefg1")
        assertTrue(check.valid)
        assertEquals(PasswordStrength.MEDIUM, check.strength)
        assertNull(check.errorRes)
    }

    @Test
    fun `strong needs twelve characters and a special character`() {
        assertEquals(PasswordStrength.MEDIUM, checkPassword("abcdefghijk1").strength)
        assertEquals(PasswordStrength.MEDIUM, checkPassword("abcdef1!").strength)
        assertEquals(PasswordStrength.STRONG, checkPassword("abcdefghijk1!").strength)
    }

    @Test
    fun `non-latin letters count as letters`() {
        val check = checkPassword("парольный1")
        assertTrue(check.valid)
        assertEquals(PasswordStrength.MEDIUM, check.strength)
    }
}
