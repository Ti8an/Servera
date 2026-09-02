package com.tivanstudio.servera.data.db.dao

import androidx.room.*
import com.tivanstudio.servera.data.db.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY createdAt DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    /** Snapshot of the raw rows, ciphertext untouched. Used by the legacy re-encryption pass. */
    @Query("SELECT * FROM servers")
    suspend fun getAllServersOnce(): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Long): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ServerEntity): Long

    @Update
    suspend fun update(entity: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM servers")
    suspend fun clearAll()
}
