package com.tivanstudio.servera.data.remote

/**
 * BUILTIN rows never reach Room, but they still have to share
 * [com.tivanstudio.servera.domain.entity.Preset.groupId] with CUSTOM rows so the UI can keep
 * grouping presets with a single `preset.groupId == group.id` test.
 *
 * Remote Config identifies groups by a human-readable string key ("docker"), so we fold that key
 * into a *negative* Long. Room's `autoGenerate` ids are always >= 1 and 0 means "not persisted
 * yet", so the negative range is free and a BUILTIN id can never collide with a CUSTOM one. The
 * fold is a pure function of the key, so ids stay stable across fetches, activations and process
 * restarts.
 */
internal fun builtinGroupId(groupKey: String): Long = foldToNegative(groupKey)

/** Preset keys carry the index so a catalog with two identically-labelled presets still yields unique ids. */
internal fun builtinPresetId(groupKey: String, label: String, index: Int): Long =
    foldToNegative(groupKey + "/" + label + "/" + index)

private fun foldToNegative(key: String): Long {
    var hash = -0x340d631b7bdddcdbL // FNV-1a 64-bit offset basis
    for (byte in key.encodeToByteArray()) {
        hash = hash xor (byte.toLong() and 0xFF)
        hash *= 0x100000001b3L
    }
    // Keep the magnitude inside 46 bits so the result is comfortably negative, never Long.MIN_VALUE.
    return -(hash and 0x3FFF_FFFF_FFFFL) - 1
}
