package com.tivanstudio.servera.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tivanstudio.servera.data.db.dao.CommandHistoryDao
import com.tivanstudio.servera.data.db.dao.QuickCommandDao
import com.tivanstudio.servera.data.db.dao.ServerDao
import com.tivanstudio.servera.data.db.entity.CommandHistoryEntity
import com.tivanstudio.servera.data.db.entity.QuickCommandEntity
import com.tivanstudio.servera.data.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, CommandHistoryEntity::class, QuickCommandEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun quickCommandDao(): QuickCommandDao
}
