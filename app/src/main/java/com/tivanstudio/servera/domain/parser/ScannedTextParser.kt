package com.tivanstudio.servera.domain.parser

import com.tivanstudio.servera.domain.entity.ScannedCredentials

/**
 * Pulls SSH connection details out of free-form recognized text.
 *
 * Rules are tried in order of how much they prove: the first one to match decides, and every
 * later rule may only fill in fields still left empty. Anything that fails validation is
 * dropped silently -- a wrong value looks exactly like a result, so the user would save a
 * server that quietly times out instead of seeing that nothing was recognized.
 *
 * A port is only ever taken from an explicit `-p` flag or a `ssh://` URI. Bare numbers are
 * never a port: a hoster panel prints lines like "Closed ports: 587, 3389, 25" right next to
 * the address, and any "number near an IP" heuristic picks one of those.
 *
 * Recognized text is used as-is -- no O/0-style autocorrection, which would break otherwise
 * valid domain names.
 */
object ScannedTextParser {

    fun parse(text: String): ScannedCredentials {
        if (text.isBlank()) return ScannedCredentials()

        var host: String? = null
        var login: String? = null
        var port: Int? = null

        // Rule 1 -- an ssh command. The strongest anchor: fixed syntax, and the port flag may
        // sit on either side of user@host.
        for (match in SSH_COMMAND.findAll(text)) {
            val args = match.groupValues[1]
            // A literal @ always wins; the fallback runs only when the line has none to work from.
            val target = USER_AT_HOST.find(args)?.let { it.groupValues[1] to it.groupValues[2] }
                ?: args.splitOnMisreadAt()
                ?: continue
            login = validLogin(target.first)
            host = validHost(target.second)
            port = PORT_FLAG.find(args)?.let { validPort(it.groupValues[1]) }
            if (login != null || host != null) break
        }

        // Rule 2 -- a ssh:// URI; the user and port parts are optional.
        if (host == null || login == null || port == null) {
            SSH_URI.find(text)?.let { match ->
                if (login == null) login = validLogin(match.groupValues[1])
                if (host == null) host = validHost(match.groupValues[2])
                if (port == null) port = validPort(match.groupValues[3])
            }
        }

        // Rule 3 -- a bare user@host with no ssh keyword around it. Says nothing about a port.
        if (host == null || login == null) {
            for (match in USER_AT_HOST.findAll(text)) {
                // Without a usable host the pair proves nothing -- it is as likely an e-mail address.
                val candidateHost = validHost(match.groupValues[2]) ?: continue
                if (host == null) host = candidateHost
                if (login == null) login = validLogin(match.groupValues[1])
                break
            }
        }

        // Rule 4 -- a standalone IPv4, and only when nothing above produced a host.
        //
        // The address is usually printed more than once on one panel: as its own field, and
        // again inside the ssh line. OCR rarely misreads the same digit the same way every
        // time, so the reading that occurs most often is the one to trust -- refusing outright
        // as soon as two readings differ threw away an address that was there and correct in
        // the majority of its occurrences.
        //
        // A tie is still a refusal: between equally frequent candidates there is nothing to
        // choose, and a wrong address looks exactly like a right one.
        if (host == null) {
            val occurrences = text.split(*TOKEN_SEPARATORS)
                .filter { isIpv4(it) }
                .groupingBy { it }
                .eachCount()
            val mostSeen = occurrences.values.maxOrNull()
            host = occurrences.entries.singleOrNull { it.value == mostSeen }?.key
        }

        return ScannedCredentials(host = host, port = port, login = login)
    }

    /** `ssh` and its arguments, on one line -- a keyword at the end of a line is prose, not a command. */
    private val SSH_COMMAND = Regex("""\bssh[ \t]+([^\r\n]*)""", RegexOption.IGNORE_CASE)

    private val SSH_URI = Regex(
        """\bssh://(?:([A-Za-z0-9._-]{1,32})@)?([A-Za-z0-9.-]+)(?::(\d{1,5}))?""",
        RegexOption.IGNORE_CASE
    )

    private val USER_AT_HOST = Regex("""([A-Za-z0-9._-]{1,32})@([A-Za-z0-9.-]+)""")

    private val PORT_FLAG = Regex("""(?:^|\s)-p[ \t]*(\d{1,5})\b""")

    private val LABEL = Regex("""[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?""")

    /** Everything an address can be wrapped in. A dot is not here: it is part of the address. */
    private val TOKEN_SEPARATORS = arrayOf(
        " ", "\t", "\r", "\n", ",", ";", ":", "/", "(", ")", "[", "]", "{", "}", "<", ">", "\"", "'", "|", "="
    )

    private fun validHost(value: String): String? =
        if (isIpv4(value) || isHostname(value)) value else null

    /**
     * Characters OCR hands back in place of an @: round, and mostly with something attached.
     *
     * A lowercase o is deliberately not among them. It is round enough to be mistaken for an @,
     * but it is also common inside logins and domain names -- "root", ".com" -- and every
     * occurrence is another place the line could be split. With it in the set,
     * "rootOexample.com" splits validly in four places rather than one, and the fallback
     * refuses to choose in exactly the cases it exists for.
     */
    private val AT_LOOKALIKES = charArrayOf('O', '0', 'Q', '©', '®')

    /**
     * The login and host of an ssh line whose @ was misread as something else.
     *
     * This repairs a structural separator, not the data around it: both halves of every
     * candidate split go through the same validation an ordinary target does, so a wrong guess
     * cannot yield a plausible-looking wrong address. It yields a valid pair or nothing.
     *
     * Whitespace splits first, because a login is one token: a candidate whose left half
     * reaches back across a space is not a login and must not be judged as one.
     *
     * @return the pair when exactly one position gives a valid one. Several valid positions is
     *   a guess between them, and no valid position is nothing to go on; both give null.
     */
    private fun String.splitOnMisreadAt(): Pair<String, String>? =
        split(' ', '\t')
            .flatMap { token -> token.validSplitsAt() }
            .singleOrNull()

    private fun String.validSplitsAt(): List<Pair<String, String>> =
        indices
            .filter { this[it] in AT_LOOKALIKES }
            .mapNotNull { at ->
                val login = validLogin(substring(0, at)) ?: return@mapNotNull null
                val host = validHost(substring(at + 1)) ?: return@mapNotNull null
                login to host
            }

    private fun validLogin(value: String): String? =
        if (LOGIN.matches(value) && !value.all { it.isDigit() }) value else null

    private val LOGIN = Regex("""[A-Za-z0-9._-]{1,32}""")

    private fun validPort(value: String): Int? = value.toIntOrNull()?.takeIf { it in 1..65535 }

    /**
     * Parsed octet by octet rather than by regex, so the awkward cases stay readable.
     * Leading zeros are rejected: some resolvers read `010.1.1.1` as octal.
     */
    private fun isIpv4(value: String): Boolean {
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

    /**
     * RFC 1123 labels, plus two guards that keep a malformed address from passing as a domain
     * name: three fully numeric labels at the front, and a fully numeric TLD.
     *
     * The first guard catches a dotted quad whose last octet came back from OCR with a letter
     * in it. "147.45.142.4l" is four valid labels ending in one that is not all digits, so
     * nothing else here would stop it, and no real domain name begins with three numeric
     * labels. Letting it through costs the user a server that saves and then times out with
     * nothing to explain why. The guard is on the leading labels whatever the total count, so
     * a longer name built on the same misreading is caught too.
     */
    private fun isHostname(value: String): Boolean {
        if (value.isEmpty() || value.length > 253) return false
        val labels = value.split('.')
        if (labels.any { !LABEL.matches(it) }) return false
        if (labels.size >= 3 && labels.take(3).all(::isNumericLabel)) return false
        return !isNumericLabel(labels.last())
    }

    private fun isNumericLabel(label: String): Boolean = label.all { it in '0'..'9' }
}
