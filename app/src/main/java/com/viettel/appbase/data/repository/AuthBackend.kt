package com.viettel.appbase.data.repository

data class BackendAuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String = "",
)

/** Implementations throw [com.viettel.appbase.domain.model.AppError] on failure. */
interface AuthBackend {
    suspend fun login(email: String, password: String): BackendAuthSession
    suspend fun register(email: String, password: String, displayName: String): BackendAuthSession
    suspend fun refresh(refreshToken: String): BackendAuthSession
    fun logout()
}
