package com.viettel.appbase.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class CachedUserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val updatedAtEpochMillis: Long,
)
