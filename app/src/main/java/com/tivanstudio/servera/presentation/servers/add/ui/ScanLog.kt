package com.tivanstudio.servera.presentation.servers.add.ui

import android.util.Log
import com.tivanstudio.servera.BuildConfig
import com.tivanstudio.servera.data.scanner.RecognizedFrame
import com.tivanstudio.servera.data.scanner.RecognizedLine
import com.tivanstudio.servera.domain.entity.ScannedCredentials

/**
 * Scanner diagnostics, debug builds only.
 *
 * Recognized text carries the user's addresses and logins, so none of it may reach a release
 * build. The gate is [BuildConfig.DEBUG] -- a compile-time constant, not a setting -- so there
 * is no switch anywhere that could turn this on for a real user, and the release compiler drops
 * the branch outright.
 *
 * [message] is a lambda rather than a String so nothing is assembled unless it is going to be
 * printed: in release the arguments are never touched.
 */
object ScanLog {
    fun d(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d("ScanDiag", message())
    }
}

/**
 * The separators [com.tivanstudio.servera.domain.parser.ScannedTextParser] splits on, repeated
 * here so the counts below are the ones the parser actually sees.
 *
 * Duplicating them is deliberate: the parser stays plain Kotlin with no diagnostic hooks in it,
 * and this copy is debug-only code that cannot affect what the app does. A dot is not a
 * separator -- it is part of an address.
 */
private val DIAG_SEPARATORS = arrayOf(
    " ", "\t", "\r", "\n", ",", ";", ":", "/", "(", ")", "[", "]", "{", "}", "<", ">", "\"", "'", "|", "="
)

/** Valid IPv4 candidates in [text] and how often each occurs, most frequent first. */
internal fun ipv4Candidates(text: String): Map<String, Int> =
    text.split(*DIAG_SEPARATORS)
        .filter(::isDiagnosticIpv4)
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .associate { it.key to it.value }

/** Mirrors the parser's octet check, leading-zero rule included. */
private fun isDiagnosticIpv4(value: String): Boolean {
    val octets = value.split('.')
    if (octets.size != 4) return false
    return octets.all { octet ->
        octet.isNotEmpty() &&
            octet.length <= 3 &&
            octet.all { it in '0'..'9' } &&
            (octet.length == 1 || octet[0] != '0') &&
            octet.toInt() <= 255
    }
}

/** "147.45.142.41 x2, 147.45.142.4l x1" */
internal fun Map<String, Int>.asCandidateList(): String =
    if (isEmpty()) "none" else entries.joinToString(", ") { "${it.key} x${it.value}" }

/**
 * One frame's diagnostics as a block.
 *
 * Both line lists are printed on purpose. The difference between them is the answer to the
 * question this whole facility exists for: if RAW holds an "ssh root@..." line and AIM is
 * empty, the reticle is throwing away the line and recognition is fine.
 */
internal fun scanDiagnostics(
    frame: RecognizedFrame,
    aimed: List<RecognizedLine>,
    aimedText: String,
    reading: ScannedCredentials,
    votes: String
): String = buildString {
    appendLine("──────────────── frame ────────────────")
    appendLine(
        "buffer ${frame.imageWidth}x${frame.imageHeight} rot=${frame.rotationDegrees} " +
            "upright ${frame.uprightWidth}x${frame.uprightHeight}"
    )
    appendLine("RAW ${frame.lines.size} line(s), before the aim region")
    frame.lines.forEach { appendLine("  ${it.asDiagnosticLine()}") }
    appendLine("AIM ${aimed.size} line(s), after it")
    aimed.forEach { appendLine("  ${it.asDiagnosticLine()}") }
    appendLine("IPv4  ${ipv4Candidates(aimedText).asCandidateList()}")
    appendLine("PARSE host=${reading.host} port=${reading.port} login=${reading.login}")
    append("VOTES $votes")
}

private fun RecognizedLine.asDiagnosticLine(): String =
    "[${box.left},${box.top},${box.right},${box.bottom}] $text"
