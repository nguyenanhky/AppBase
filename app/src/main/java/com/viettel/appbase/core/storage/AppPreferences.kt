package com.viettel.appbase.core.storage

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
