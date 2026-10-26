package com.viettel.appbase.domain.repository

import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>

    /** Keeps the current session, or starts an anonymous one. Safe to call often. */
    suspend fun ensureSession(): Result<Unit>

    suspend fun login(email: String, password: String): Result<AuthSession>

    /** While anonymous, upgrades the current user (same id), so quota and history carry over. */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession>
    suspend fun refreshToken(): Result<String>

    /** Ends the session, clears local user data, then starts a fresh anonymous session. */
    suspend fun logout()
}
