package com.tivanstudio.servera.domain.analytics

/**
 * The complete set of events this app is allowed to report.
 *
 * Closed on purpose: a caller cannot hand [Analytics] an arbitrary string, so no host, login,
 * password, command text or server name can reach Firebase by accident. Adding an event means
 * adding a subclass here, which puts every new event in front of a reviewer.
 *
 * [params] may only ever carry values fixed in this file — enum-like constants describing *what
 * kind* of thing happened, never user data.
 *
 * Firebase constrains [name] to <=40 chars, snake_case, starting with a letter.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap()
) {
    /** Unlocked the vault by typing the master password. */
    object LoginPassword : AnalyticsEvent("login_password")

    /** Unlocked the vault through the biometric prompt. */
    object LoginBiometric : AnalyticsEvent("login_biometric")

    /** A password attempt was rejected. Carries no attempt count and no input. */
    object LoginFailed : AnalyticsEvent("login_failed")
}
