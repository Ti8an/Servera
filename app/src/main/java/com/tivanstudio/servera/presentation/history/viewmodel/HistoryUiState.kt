package com.tivanstudio.servera.presentation.history.viewmodel

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.Server
import java.util.Calendar

sealed class HistoryEvent {
    object NavigateToResult : HistoryEvent()
}

enum class HistoryStatus { ALL, SUCCESS, ERROR }

enum class HistoryPeriod { ALL, TODAY, WEEK }

data class HistoryFilter(
    val serverId: Long? = null,
    val groupName: String? = null,
    val status: HistoryStatus = HistoryStatus.ALL,
    val period: HistoryPeriod = HistoryPeriod.ALL,
    val query: String = ""
) {
    val isActive: Boolean
        get() = serverId != null ||
            groupName != null ||
            status != HistoryStatus.ALL ||
            period != HistoryPeriod.ALL ||
            query.isNotBlank()
}

data class HistoryUiState(
    val allHistory: List<CommandHistory> = emptyList(),
    val servers: List<Server> = emptyList(),
    val groups: List<PresetGroup> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    val showFilterSheet: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /** History narrowed by the active filter, newest first. */
    val filtered: List<CommandHistory>
        get() {
            val now       = System.currentTimeMillis()
            val periodMin = when (filter.period) {
                HistoryPeriod.ALL   -> Long.MIN_VALUE
                HistoryPeriod.TODAY -> startOfToday()
                HistoryPeriod.WEEK  -> now - WEEK_MILLIS
            }
            return allHistory
                .filter { item ->
                    (filter.serverId == null || item.serverId == filter.serverId) &&
                        (filter.groupName == null || item.groupName == filter.groupName) &&
                        when (filter.status) {
                            HistoryStatus.ALL     -> true
                            HistoryStatus.SUCCESS -> item.exitCode == 0
                            HistoryStatus.ERROR   -> item.exitCode != 0
                        } &&
                        item.executedAt >= periodMin &&
                        (filter.query.isBlank() || item.command.contains(filter.query, ignoreCase = true))
                }
                .sortedByDescending { it.executedAt }
        }
}

private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
