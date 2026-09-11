package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthToken
import com.viettel.appbase.domain.model.User
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class FakeAuthRepository(private val appDataStore: AppDataStore) : AuthRepository {
    override val isAuthenticated: Flow<Boolean> = appDataStore.isAuthenticated

    override suspend fun login(email: String, password: String): Result<AuthSession> {
        delay(DEMO_DELAY_MILLIS)
        return saveDemoSession(email, "Demo User")
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession> {
        delay(DEMO_DELAY_MILLIS)
        return saveDemoSession(email, displayName)
    }

    override suspend fun refreshToken(): Result<String> = runCatching {
        val token = "demo-token-refreshed"
        appDataStore.saveSession(token, "demo-refresh-token", "demo-user")
        token
    }

    override suspend fun logout() = appDataStore.clearSession()

    private suspend fun saveDemoSession(email: String, displayName: String): Result<AuthSession> = runCatching {
        val token = AuthToken("demo-token", "demo-refresh-token")
        val user = User("demo-user", email, displayName)
        appDataStore.saveSession(token.accessToken, token.refreshToken, user.id)
        AuthSession(user, token)
    }

    companion object {
        private const val DEMO_DELAY_MILLIS = 350L
    }
}
