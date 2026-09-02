package com.tivanstudio.servera.data.db.dao

import androidx.room.*
import com.tivanstudio.servera.data.db.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY groupId ASC, sortOrder ASC")
    fun getAll(): Flow<List<PresetEntity>>

    /** Snapshot of the raw rows, ciphertext untouched. Used by the legacy re-encryption pass. */
    @Query("SELECT * FROM presets")
    suspend fun getAllOnce(): List<PresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PresetEntity): Long

    @Update
    suspend fun update(entity: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM presets")
    suspend fun clearAll()
}
