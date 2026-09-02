package com.tivanstudio.servera.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _isSaveCommandsAlways = MutableStateFlow(prefs.getBoolean(KEY_SAVE_COMMANDS_ALWAYS, false))
    val isSaveCommandsAlways: StateFlow<Boolean> = _isSaveCommandsAlways.asStateFlow()

    fun setSaveCommandsAlways(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_COMMANDS_ALWAYS, enabled).apply()
        _isSaveCommandsAlways.value = enabled
    }

    private val _saveResultInHistory = MutableStateFlow(prefs.getBoolean(KEY_SAVE_RESULT_IN_HISTORY, false))
    val saveResultInHistory: StateFlow<Boolean> = _saveResultInHistory.asStateFlow()

    fun setSaveResultInHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_RESULT_IN_HISTORY, enabled).apply()
        _saveResultInHistory.value = enabled
    }

    /** Drops every account-tied preference back to its default. Used by the vault reset. */
    fun clear() {
        prefs.edit().clear().apply()
        _isSaveCommandsAlways.value = false
        _saveResultInHistory.value = false
    }

    companion object {
        private const val KEY_SAVE_COMMANDS_ALWAYS = "save_commands_always"
        private const val KEY_SAVE_RESULT_IN_HISTORY = "save_result_in_history"
    }
}
