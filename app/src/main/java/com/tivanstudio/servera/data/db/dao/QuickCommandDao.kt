package com.tivanstudio.servera.data.db.dao

import androidx.room.*
import com.tivanstudio.servera.data.db.entity.QuickCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickCommandDao {
    @Query("SELECT * FROM quick_commands WHERE serverId = :serverId ORDER BY sortOrder ASC")
    fun getForServer(serverId: Long): Flow<List<QuickCommandEntity>>

    /** Snapshot of the raw rows, ciphertext untouched. Used by the legacy re-encryption pass. */
    @Query("SELECT * FROM quick_commands")
    suspend fun getAllOnce(): List<QuickCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QuickCommandEntity): Long

    @Update
    suspend fun update(entity: QuickCommandEntity)

    @Query("DELETE FROM quick_commands WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quick_commands")
    suspend fun clearAll()
}
