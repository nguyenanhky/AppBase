package com.viettel.appbase.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.viettel.appbase.data.local.entity.CachedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CachedUserEntity?>

    @Upsert
    suspend fun upsert(user: CachedUserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}
