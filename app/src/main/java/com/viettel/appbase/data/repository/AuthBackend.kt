package com.viettel.appbase.data.repository

data class BackendAuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String = "",
    val isAnonymous: Boolean = false,
)

/** Implementations throw [com.viettel.appbase.domain.model.AppError] on failure. */
interface AuthBackend {
    /** Returns the backend's current user if any, otherwise creates an anonymous one. */
    suspend fun signInAnonymously(): BackendAuthSession

    suspend fun login(email: String, password: String): BackendAuthSession

    /** When the current user is anonymous, implementations should upgrade it in place (same id). */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): BackendAuthSession
    suspend fun refresh(refreshToken: String): BackendAuthSession
    fun logout()
}
