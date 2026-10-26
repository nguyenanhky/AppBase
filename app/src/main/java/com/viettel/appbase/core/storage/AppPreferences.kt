package com.viettel.appbase.core.storage

import kotlinx.serialization.Serializable

/** ProtoBuf numbers fields by declaration order: only append new fields at the end, never reorder. */
@Serializable
data class AppPreferences(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val launchCount: Int = 0,
    val firstLaunchEpochMillis: Long = 0,
    val lastReviewPromptEpochMillis: Long = 0,
    val adFree: Boolean = false,
    val isAnonymous: Boolean = false,
)

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
