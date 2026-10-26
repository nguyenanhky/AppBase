package com.viettel.appbase.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.viettel.appbase.data.local.dao.UserDao
import com.viettel.appbase.data.local.entity.CachedUserEntity

/**
 * Changing the schema: bump [VERSION], build once to export schemas/<VERSION>.json,
 * add a Migration to [MIGRATIONS] and extend AppDatabaseMigrationTest.
 */
@Database(
    entities = [CachedUserEntity::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val VERSION = 1
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
