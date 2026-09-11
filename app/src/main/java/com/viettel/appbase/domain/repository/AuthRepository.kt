package com.viettel.appbase.domain.repository

import com.viettel.appbase.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    suspend fun login(email: String, password: String): Result<AuthSession>
    suspend fun register(email: String, password: String, displayName: String): Result<AuthSession>
    suspend fun refreshToken(): Result<String>
    suspend fun logout()
}
