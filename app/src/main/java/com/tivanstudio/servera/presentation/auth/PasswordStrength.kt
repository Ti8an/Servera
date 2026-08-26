package com.tivanstudio.servera.presentation.auth

import androidx.annotation.StringRes
import com.tivanstudio.servera.R

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

data class PasswordCheck(
    val valid: Boolean,
    val strength: PasswordStrength,
    @StringRes val errorRes: Int?
)

private const val MIN_LENGTH = 8
private const val STRONG_LENGTH = 12

/**
 * The single rule set behind both the create-password and change-password screens:
 * at least [MIN_LENGTH] characters with a letter and a digit, and a longer passphrase
 * carrying a special character counts as strong.
 */
fun checkPassword(password: String): PasswordCheck = when {
    password.length < MIN_LENGTH ->
        PasswordCheck(false, PasswordStrength.WEAK, R.string.pwd_too_short)

    !password.any { it.isLetter() } || !password.any { it.isDigit() } ->
        PasswordCheck(false, PasswordStrength.WEAK, R.string.pwd_need_letter_digit)

    password.length >= STRONG_LENGTH && password.any { !it.isLetterOrDigit() } ->
        PasswordCheck(true, PasswordStrength.STRONG, null)

    else -> PasswordCheck(true, PasswordStrength.MEDIUM, null)
}
