package com.tivanstudio.servera.presentation.servers.add

import androidx.annotation.StringRes
import com.tivanstudio.servera.R

/** Four dot-separated groups of digits -- the shape a typo in an IP still keeps. */
private val IPV4_SHAPE = Regex("""\d+\.\d+\.\d+\.\d+""")

/**
 * Guards the host field against typos, not against every unreachable address.
 *
 * Only strings already shaped like an IPv4 address are checked octet by octet; anything else is
 * taken for a domain name and passed through, since the rules a hostname may follow are far
 * looser than this screen should decide on.
 *
 * @return the error to show under the field, or null when the host is acceptable.
 */
@StringRes
fun validateHost(host: String): Int? {
    val trimmed = host.trim()
    return when {
        trimmed.isEmpty()             -> R.string.error_host_required
        !IPV4_SHAPE.matches(trimmed)  -> null
        trimmed.split(".").all { octet -> octet.toIntOrNull()?.let { it in 0..255 } == true } -> null
        else                          -> R.string.invalid_ip
    }
}
