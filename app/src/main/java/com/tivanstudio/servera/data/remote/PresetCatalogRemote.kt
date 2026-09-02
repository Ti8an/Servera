package com.tivanstudio.servera.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.PresetSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Read-only view of the built-in preset catalog published through Firebase Remote Config.
 *
 * Remote Config already persists the last activated value across process restarts, so the "local
 * cache" is its cache -- nothing is mirrored into Room. Room holds CUSTOM rows only.
 *
 * [remoteConfig] is nullable on purpose: without `app/google-services.json` there is no
 * FirebaseApp to attach to, and the app must still start with an empty built-in catalog rather
 * than crash on injection.
 */
@Singleton
class PresetCatalogRemote @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig?
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Parsed form of the last raw value we saw, so a Flow re-collect does not re-parse the JSON. */
    private val parsed = AtomicReference<Pair<String, PresetCatalogDto>?>(null)

    private val _catalogRevision = MutableStateFlow(0)

    /**
     * Bumped whenever a fetch activates a new value. Collectors re-read [getBuiltinGroups] /
     * [getBuiltinPresets] so the merged lists refresh without restarting the app.
     */
    val catalogRevision: StateFlow<Int> = _catalogRevision.asStateFlow()

    init {
        remoteConfig?.let { config ->
            config.setDefaultsAsync(mapOf(KEY_PRESETS_CATALOG to EMPTY_CATALOG_JSON))
            config.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(MINIMUM_FETCH_INTERVAL_SECONDS)
                    .build()
            )
        }
    }

    /**
     * Fetches and activates the catalog, returning the catalog `version` now in effect (0 when
     * Firebase is unavailable or the value is empty).
     *
     * Calls inside [MINIMUM_FETCH_INTERVAL_SECONDS] resolve from the Remote Config cache without
     * hitting the network, which keeps a user-driven refresh button well clear of Firebase quota.
     */
    suspend fun fetchAndActivate(): Result<Int> {
        val config = remoteConfig ?: return Result.success(0)
        return runCatching {
            val activated = awaitTask(config.fetchAndActivate())
            if (activated) _catalogRevision.value += 1
            currentCatalog(config).version
        }.onFailure { Log.w(TAG, "Remote Config fetch failed", it) }
    }

    /** Built-in groups from the currently activated catalog; empty when there is no value yet. */
    fun getBuiltinGroups(): List<PresetGroup> =
        currentCatalog().groups.map { dto ->
            PresetGroup(
                id = builtinGroupId(dto.id),
                name = dto.name,
                colorHex = dto.colorHex,
                sortOrder = dto.sortOrder,
                source = PresetSource.BUILTIN
            )
        }

    /** Built-in presets from the currently activated catalog; empty when there is no value yet. */
    fun getBuiltinPresets(): List<Preset> {
        val catalog = currentCatalog()
        val knownGroups = catalog.groups.mapTo(HashSet()) { it.id }
        return catalog.presets
            // A preset pointing at a group the catalog does not declare would be invisible in the
            // grouped UI, so drop it rather than carry a dangling groupId around.
            .filter { it.groupId in knownGroups }
            .mapIndexed { index, dto ->
                Preset(
                    id = builtinPresetId(dto.groupId, dto.label, index),
                    groupId = builtinGroupId(dto.groupId),
                    label = dto.label,
                    command = dto.command,
                    sortOrder = dto.sortOrder,
                    source = PresetSource.BUILTIN
                )
            }
    }

    private fun currentCatalog(config: FirebaseRemoteConfig? = remoteConfig): PresetCatalogDto {
        val raw = config?.getString(KEY_PRESETS_CATALOG).orEmpty()
        if (raw.isBlank()) return EMPTY_CATALOG

        parsed.get()?.let { (cachedRaw, cached) -> if (cachedRaw == raw) return cached }

        // A malformed catalog must not take the presets screen down with it -- fall back to empty.
        val catalog = runCatching { json.decodeFromString<PresetCatalogDto>(raw) }
            .onFailure { Log.w(TAG, "Malformed $KEY_PRESETS_CATALOG value", it) }
            .getOrDefault(EMPTY_CATALOG)
        parsed.set(raw to catalog)
        return catalog
    }

    private suspend fun <T> awaitTask(task: Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            task.addOnCompleteListener { completed ->
                val error = completed.exception
                if (error != null) continuation.resumeWith(Result.failure(error))
                else continuation.resume(completed.result)
            }
        }

    private companion object {
        const val TAG = "PresetCatalogRemote"
        const val KEY_PRESETS_CATALOG = "presets_catalog"
        const val EMPTY_CATALOG_JSON = """{"version":0,"groups":[],"presets":[]}"""
        const val MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        val EMPTY_CATALOG = PresetCatalogDto()
    }
}
