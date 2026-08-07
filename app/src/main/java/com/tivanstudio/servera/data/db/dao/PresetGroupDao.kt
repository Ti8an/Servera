package com.tivanstudio.servera.data.db.dao

import androidx.room.*
import com.tivanstudio.servera.data.db.entity.PresetGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetGroupDao {
    @Query("SELECT * FROM preset_groups ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<PresetGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PresetGroupEntity): Long

    @Query("DELETE FROM preset_groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(entity: PresetGroupEntity)
}
