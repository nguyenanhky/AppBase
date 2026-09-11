package com.viettel.appbase.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.viettel.appbase.data.local.dao.UserDao
import com.viettel.appbase.data.local.entity.CachedUserEntity

@Database(
    entities = [CachedUserEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
