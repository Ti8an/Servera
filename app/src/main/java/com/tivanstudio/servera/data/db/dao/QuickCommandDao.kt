package com.tivanstudio.servera.data.db.dao

import androidx.room.*
import com.tivanstudio.servera.data.db.entity.QuickCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickCommandDao {
    @Query("SELECT * FROM quick_commands WHERE serverId = :serverId ORDER BY sortOrder ASC")
    fun getForServer(serverId: Long): Flow<List<QuickCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QuickCommandEntity): Long

    @Query("DELETE FROM quick_commands WHERE id = :id")
    suspend fun deleteById(id: Long)
}
