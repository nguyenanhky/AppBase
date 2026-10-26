package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.utils.suspendRunCatching
import com.viettel.appbase.data.local.dao.UserDao
import com.viettel.appbase.data.local.entity.CachedUserEntity
import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthState
import com.viettel.appbase.domain.model.AuthToken
import com.viettel.appbase.domain.model.User
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class AuthRepositoryImpl(
    private val backend: AuthBackend,
    private val appDataStore: AppDataStore,
    private val userDao: UserDao,
    private val aiHistory: AiHistoryRepository,
) : AuthRepository {
    private val refreshMutex = Mutex()
    private val sessionMutex = Mutex()
    override val authState: Flow<AuthState> = appDataStore.authState

    override suspend fun ensureSession(): Result<Unit> = sessionMutex.withLock {
        suspendRunCatching {
            if (!appDataStore.hasSession()) persist(backend.signInAnonymously())
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthSession> = suspendRunCatching {
        persist(backend.login(email, password))
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession> = suspendRunCatching {
        persist(backend.register(email, password, displayName))
    }

    override suspend fun refreshToken(): Result<String> = refreshMutex.withLock {
        suspendRunCatching { persist(backend.refresh(appDataStore.currentRefreshToken())).token.accessToken }
    }

    override suspend fun logout() {
        backend.logout()
        clearLocalUserData()
        ensureSession().onFailure { Timber.w(it, "No anonymous session after logout") }
    }

    private suspend fun persist(remote: BackendAuthSession): AuthSession {
        val previousUserId = appDataStore.currentUserId()
        // Another account on this device: never show the previous user's data.
        if (previousUserId.isNotBlank() && previousUserId != remote.userId) clearLocalUserData()
        val user = User(
            id = remote.userId,
            email = remote.email,
            displayName = remote.displayName.ifBlank { remote.email.substringBefore('@') },
            isAnonymous = remote.isAnonymous,
        )
        userDao.upsert(
            CachedUserEntity(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        appDataStore.saveSession(remote.accessToken, remote.refreshToken, remote.userId, remote.isAnonymous)
        return AuthSession(user, AuthToken(remote.accessToken, remote.refreshToken))
    }

    private suspend fun clearLocalUserData() {
        appDataStore.clearSession()
        userDao.clear()
        aiHistory.clear()
    }
}
