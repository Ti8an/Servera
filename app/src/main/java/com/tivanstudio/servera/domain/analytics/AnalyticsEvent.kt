package com.tivanstudio.servera.domain.analytics

/**
 * The complete set of events this app is allowed to report.
 *
 * Closed on purpose: a caller cannot hand [Analytics] an arbitrary string, so no host, login,
 * password, command text or server name can reach Firebase by accident. Adding an event means
 * adding a subclass here, which puts every new event in front of a reviewer.
 *
 * [params] may only ever carry values fixed in this file — enum-like constants, flags and
 * counters describing *what kind* of thing happened, never user data. That is why an event that
 * carries a parameter takes a closed type — an enum, a Boolean, a bounded count — rather than a
 * free-form String. Every value is reported as a string, so a count arrives as "2", not as 2.
 *
 * Firebase constrains [name] to <=40 chars, snake_case, starting with a letter.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap()
) {
    // ── Auth ─────────────────────────────────────────────────────────────────

    /** Unlocked the vault by typing the master password. */
    object LoginPassword : AnalyticsEvent("login_password")

    /** Unlocked the vault through the biometric prompt. */
    object LoginBiometric : AnalyticsEvent("login_biometric")

    /** A password attempt was rejected. Carries no attempt count and no input. */
    object LoginFailed : AnalyticsEvent("login_failed")

    // ── Servers ──────────────────────────────────────────────────────────────

    /** A new server was saved. Edits of an existing one do not count. */
    object ServerAdded : AnalyticsEvent("server_added")

    object ServerDeleted : AnalyticsEvent("server_deleted")

    /** An SSH reachability check succeeded. Nothing about *which* server is reported. */
    object ServerConnectSuccess : AnalyticsEvent("server_connect_success")

    /** An SSH reachability check failed. The cause is deliberately not reported. */
    object ServerConnectFail : AnalyticsEvent("server_connect_fail")

    // ── Commands ─────────────────────────────────────────────────────────────

    /** One command run was started, whatever its origin. Never carries the command text. */
    object CommandRun : AnalyticsEvent("command_run")

    /** The run came from a saved command rather than a hand-typed one. */
    object PresetUsed : AnalyticsEvent("preset_used")

    /** A CUSTOM preset was created. Edits of an existing one do not count. */
    object PresetCreated : AnalyticsEvent("preset_created")

    // ── Preset catalog ───────────────────────────────────────────────────────

    /** The built-in catalog was refreshed from Remote Config, successfully. */
    object PresetsUpdated : AnalyticsEvent("presets_updated")

    object PresetCopiedToCustom : AnalyticsEvent("preset_copied_to_custom")

    // ── Security ─────────────────────────────────────────────────────────────

    object BiometricEnabled : AnalyticsEvent("biometric_enabled")

    object BiometricDisabled : AnalyticsEvent("biometric_disabled")

    /** The KDF work factor was changed. [Level] is a mode, not user data. */
    data class SecurityLevelChanged(val level: Level) :
        AnalyticsEvent("security_level_changed", mapOf("level" to level.value)) {

        /** The only values this event can ever carry. */
        enum class Level(val value: String) { MIN("min"), MID("mid"), HIGH("high") }
    }

    // ── Tools ────────────────────────────────────────────────────────────────

    /** A subnet scan was started. The subnet itself is not reported. */
    object NetworkScanRun : AnalyticsEvent("network_scan_run")

    // ── Camera scanner ──────────────────────────────────────────────────────

    /** The camera scanner was opened from the add-server form. */
    object ScannerOpened : AnalyticsEvent("scanner_opened")

    /**
     * Camera access was refused. [permanently] means the system will not show its dialog
     * again, so the app settings page is the only way left to the scanner.
     */
    data class ScannerPermissionDenied(val permanently: Boolean) : AnalyticsEvent(
        "scanner_permission_denied",
        mapOf("permanently" to permanently.toString())
    )

    /**
     * A reading was applied to the form. [fieldsCount] is how many of the three fields it
     * filled (1..3) and [hasPort] whether a port was among them — a port is only ever read
     * off an explicit ssh line, so it says what kind of screen was scanned. Neither carries
     * any part of what was recognized.
     */
    data class ScannerApplied(val fieldsCount: Int, val hasPort: Boolean) : AnalyticsEvent(
        "scanner_applied",
        mapOf("fields_count" to fieldsCount.toString(), "has_port" to hasPort.toString())
    )

    /**
     * The scanner was closed without applying anything. [hadResult] tells "recognized
     * nothing" apart from "recognized something the user did not want".
     */
    data class ScannerCancelled(val hadResult: Boolean) : AnalyticsEvent(
        "scanner_cancelled",
        mapOf("had_result" to hadResult.toString())
    )

    // ── Navigation ───────────────────────────────────────────────────────────

    /**
     * A route was opened. [route] is a route *pattern* out of
     * `presentation.navigation.Screen` — "presets", "servers/{serverId}/console" and so on. The
     * caller is responsible for passing a known pattern and never a filled-in route, since a
     * filled one would carry a server id.
     */
    data class ScreenView(val route: String) :
        AnalyticsEvent("screen_view", mapOf("screen_name" to route))
}
