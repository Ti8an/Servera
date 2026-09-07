package com.tivanstudio.servera.domain.entity

/** What a single scan managed to pull out of recognized text. */
data class ScannedCredentials(
    val host: String? = null,
    val port: Int? = null,
    val login: String? = null
) {
    val isEmpty: Boolean get() = host == null && port == null && login == null
    val filledCount: Int get() = listOfNotNull(host, port, login).size
}
