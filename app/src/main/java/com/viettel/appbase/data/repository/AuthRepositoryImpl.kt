package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.data.local.dao.UserDao
import com.viettel.appbase.data.local.entity.CachedUserEntity
import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthToken
import com.viettel.appbase.domain.model.User
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepositoryImpl(
    private val backend: AuthBackend,
    private val appDataStore: AppDataStore,
    private val userDao: UserDao,
) : AuthRepository {
    private val refreshMutex = Mutex()
    override val isAuthenticated: Flow<Boolean> = appDataStore.isAuthenticated

    override suspend fun login(email: String, password: String): Result<AuthSession> = runCatching {
        persist(backend.login(email, password))
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession> = runCatching {
        persist(backend.register(email, password, displayName))
    }

    override suspend fun refreshToken(): Result<String> = refreshMutex.withLock {
        runCatching { persist(backend.refresh()).token.accessToken }
    }

    override suspend fun logout() {
        backend.logout()
        appDataStore.clearSession()
        userDao.clear()
    }

    private suspend fun persist(remote: BackendAuthSession): AuthSession {
        val user = User(
            id = remote.userId,
            email = remote.email,
            displayName = remote.displayName.ifBlank { remote.email.substringBefore('@') },
        )
        userDao.upsert(
            CachedUserEntity(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        appDataStore.saveSession(remote.accessToken, remote.refreshToken, remote.userId)
        return AuthSession(user, AuthToken(remote.accessToken, remote.refreshToken))
    }
}
