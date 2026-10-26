package com.viettel.appbase.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.viettel.appbase.data.local.entity.AiHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiHistoryDao {
    @Insert
    suspend fun insert(entity: AiHistoryEntity): Long

    @Query("SELECT * FROM ai_history WHERE task = :task ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    fun observe(task: String, limit: Int): Flow<List<AiHistoryEntity>>

    @Query("UPDATE ai_history SET reported = 1 WHERE id = :id")
    suspend fun markReported(id: Long)

    @Query("DELETE FROM ai_history")
    suspend fun clear()
}
