package com.tivanstudio.servera

import com.tivanstudio.servera.presentation.auth.PasswordStrength
import com.tivanstudio.servera.presentation.auth.checkPassword
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {

    @Test
    fun `empty password is weak and cracked instantly`() {
        val check = checkPassword("")
        assertEquals(PasswordStrength.WEAK, check.strength)
        assertEquals(R.string.crack_instant, check.crackTimeRes)
    }

    @Test
    fun `very short passwords are weak and cracked instantly`() {
        listOf("a", "ab1", "abc123").forEach { password ->
            val check = checkPassword(password)
            assertEquals(password, PasswordStrength.WEAK, check.strength)
            assertEquals(password, R.string.crack_instant, check.crackTimeRes)
        }
    }

    @Test
    fun `eight letters and digits is still weak but takes hours`() {
        val check = checkPassword("abcdefg1")
        assertEquals(PasswordStrength.WEAK, check.strength)
        assertEquals(R.string.crack_hours, check.crackTimeRes)
    }

    @Test
    fun `ten letters and digits is medium`() {
        val check = checkPassword("abcdefgh12")
        assertEquals(PasswordStrength.MEDIUM, check.strength)
        assertEquals(R.string.crack_days, check.crackTimeRes)
    }

    @Test
    fun `twelve letters and digits is medium measured in years`() {
        val check = checkPassword("abcdefghij12")
        assertEquals(PasswordStrength.MEDIUM, check.strength)
        assertEquals(R.string.crack_years, check.crackTimeRes)
    }

    @Test
    fun `long password with special characters is strong`() {
        listOf("correct-horse7!", "Str0ng-P@ssw0rd!").forEach { password ->
            val check = checkPassword(password)
            assertEquals(password, PasswordStrength.STRONG, check.strength)
            assertEquals(password, R.string.crack_centuries, check.crackTimeRes)
        }
    }

    @Test
    fun `mixing character classes widens the alphabet`() {
        // Same length, richer alphabet -> a stronger verdict.
        val plain = checkPassword("abcdefghij")
        val mixed = checkPassword("aBcDeF1!j#")
        assertEquals(PasswordStrength.WEAK, plain.strength)
        assertEquals(PasswordStrength.STRONG, mixed.strength)
    }

    @Test
    fun `non-latin letters count towards the alphabet`() {
        val check = checkPassword("парольный1")
        assertEquals(PasswordStrength.MEDIUM, check.strength)
    }
}
