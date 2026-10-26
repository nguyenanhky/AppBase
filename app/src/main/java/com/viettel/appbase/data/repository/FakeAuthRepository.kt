package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.utils.suspendRunCatching
import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthState
import com.viettel.appbase.domain.model.AuthToken
import com.viettel.appbase.domain.model.User
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/** Offline auth for the mock flavor: same anonymous → account flow as Firebase, without a backend. */
class FakeAuthRepository(private val appDataStore: AppDataStore, private val aiHistory: AiHistoryRepository) :
    AuthRepository {
    override val authState: Flow<AuthState> = appDataStore.authState

    override suspend fun ensureSession(): Result<Unit> = suspendRunCatching {
        if (!appDataStore.hasSession()) {
            appDataStore.saveSession(ANONYMOUS_TOKEN, "", ANONYMOUS_USER_ID, isAnonymous = true)
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthSession> {
        delay(DEMO_DELAY_MILLIS)
        return saveDemoSession(DEMO_USER_ID, email, "Demo User")
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession> {
        delay(DEMO_DELAY_MILLIS)
        // Like Firebase linking: an anonymous user keeps the same id.
        val userId = appDataStore.currentUserId().ifBlank { DEMO_USER_ID }
        return saveDemoSession(userId, email, displayName)
    }

    override suspend fun refreshToken(): Result<String> = suspendRunCatching {
        val token = "demo-token-refreshed"
        appDataStore.saveSession(token, "demo-refresh-token", appDataStore.currentUserId().ifBlank { DEMO_USER_ID })
        token
    }

    override suspend fun logout() {
        appDataStore.clearSession()
        aiHistory.clear()
        ensureSession()
    }

    private suspend fun saveDemoSession(
        userId: String,
        email: String,
        displayName: String,
    ): Result<AuthSession> = suspendRunCatching {
        val previousUserId = appDataStore.currentUserId()
        if (previousUserId.isNotBlank() && previousUserId != userId) aiHistory.clear()
        val token = AuthToken("demo-token", "demo-refresh-token")
        val user = User(userId, email, displayName)
        appDataStore.saveSession(token.accessToken, token.refreshToken, user.id)
        AuthSession(user, token)
    }

    companion object {
        private const val DEMO_DELAY_MILLIS = 350L
        private const val DEMO_USER_ID = "demo-user"
        private const val ANONYMOUS_USER_ID = "demo-anon"
        private const val ANONYMOUS_TOKEN = "demo-anon-token"
    }
}
