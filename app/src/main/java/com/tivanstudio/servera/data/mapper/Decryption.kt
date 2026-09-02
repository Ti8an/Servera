package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper

/**
 * Reads one encrypted field that must not take its whole list down with it: ciphertext left by
 * an older key, or a corrupt row, comes back as an empty string and the rest of the list still
 * loads.
 *
 * A locked vault is a different matter -- every field would fail, and the caller is waiting for
 * that signal -- so [IllegalStateException] is rethrown.
 */
internal fun EncryptionHelper.decryptOrEmpty(encoded: String): String =
    runCatching { decrypt(encoded) }
        .onFailure { if (it is IllegalStateException) throw it }
        .getOrDefault("")
