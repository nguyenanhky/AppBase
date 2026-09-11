package com.viettel.appbase.data.repository

data class BackendAuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String = "",
)

interface AuthBackend {
    suspend fun login(email: String, password: String): BackendAuthSession
    suspend fun register(email: String, password: String, displayName: String): BackendAuthSession
    suspend fun refresh(): BackendAuthSession
    fun logout()
}
