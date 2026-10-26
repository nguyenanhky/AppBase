package com.viettel.appbase.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ai_history", indices = [Index("task")])
data class AiHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task: String,
    val resultJson: String,
    val imagePath: String?,
    val createdAtEpochMillis: Long,
    val reported: Boolean = false,
)
