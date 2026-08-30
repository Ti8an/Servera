package com.tivanstudio.servera.presentation.auth

import androidx.annotation.StringRes
import com.tivanstudio.servera.R
import kotlin.math.log10

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

/** Advisory only: nothing here blocks a password, it just describes how weak it is. */
data class PasswordCheck(
    val strength: PasswordStrength,
    @StringRes val crackTimeRes: Int
)

/** Guesses per second an attacker is assumed to manage — deliberately generous (1e10). */
private const val LOG10_GUESSES_PER_SECOND = 10.0

/**
 * Estimates how long a brute force over the password's own alphabet would take and maps
 * that to a colour band. Everything is kept in log10 so long passphrases cannot overflow.
 */
fun checkPassword(password: String): PasswordCheck {
    if (password.isEmpty()) return PasswordCheck(PasswordStrength.WEAK, R.string.crack_instant)

    var alphabet = 0
    if (password.any { it.isDigit() }) alphabet += 10
    if (password.any { it.isLowerCase() }) alphabet += 26
    if (password.any { it.isUpperCase() }) alphabet += 26
    if (password.any { !it.isLetterOrDigit() }) alphabet += 33
    if (alphabet == 0) alphabet = 26

    val log10Combos = password.length * log10(alphabet.toDouble())
    val log10Seconds = log10Combos - LOG10_GUESSES_PER_SECOND

    return when {
        log10Seconds < 2 -> PasswordCheck(PasswordStrength.WEAK, R.string.crack_instant)
        log10Seconds < 4.5 -> PasswordCheck(PasswordStrength.WEAK, R.string.crack_hours)
        log10Seconds < 7 -> PasswordCheck(PasswordStrength.MEDIUM, R.string.crack_days)
        log10Seconds < 9.5 -> PasswordCheck(PasswordStrength.MEDIUM, R.string.crack_years)
        else -> PasswordCheck(PasswordStrength.STRONG, R.string.crack_centuries)
    }
}
