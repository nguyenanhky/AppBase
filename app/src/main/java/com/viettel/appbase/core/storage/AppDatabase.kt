package com.viettel.appbase.core.storage

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.viettel.appbase.data.local.dao.AiHistoryDao
import com.viettel.appbase.data.local.dao.UserDao
import com.viettel.appbase.data.local.entity.AiHistoryEntity
import com.viettel.appbase.data.local.entity.CachedUserEntity

/**
 * Changing the schema: bump [VERSION] and build once to export schemas/<VERSION>.json. Simple
 * changes (new table/column) use an AutoMigration below; renames or data rewrites need a manual
 * Migration in [MIGRATIONS]. Either way, extend AppDatabaseMigrationTest.
 */
@Database(
    entities = [CachedUserEntity::class, AiHistoryEntity::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun aiHistoryDao(): AiHistoryDao

    companion object {
        const val VERSION = 2
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
