package com.tivanstudio.servera.data.db.dao

import androidx.room.*
import com.tivanstudio.servera.data.db.entity.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history WHERE serverId = :serverId ORDER BY executedAt DESC")
    fun getHistoryForServer(serverId: Long): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY executedAt DESC")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CommandHistoryEntity)

    @Query("DELETE FROM command_history WHERE serverId = :serverId")
    suspend fun clearByServer(serverId: Long)
}
