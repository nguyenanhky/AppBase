package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.data.local.dao.UserDao
import com.viettel.appbase.domain.model.AppError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private val backend = mockk<AuthBackend>(relaxed = true)
    private val dataStore = mockk<AppDataStore>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val aiHistory = mockk<AiHistoryRepository>(relaxed = true)
    private val repository = AuthRepositoryImpl(backend, dataStore, userDao, aiHistory)

    @Test
    fun `login persists backend session and maps user`() = runTest {
        coEvery { backend.login("user@example.com", "password") } returns backendSession(displayName = "")

        val result = repository.login("user@example.com", "password").getOrThrow()

        assertEquals("user", result.user.displayName)
        assertEquals("access", result.token.accessToken)
        coVerify { dataStore.saveSession("access", "refresh", "42", false) }
        coVerify { userDao.upsert(match { it.id == "42" && it.email == "user@example.com" }) }
    }

    @Test
    fun `ensureSession starts an anonymous session when there is none`() = runTest {
        coEvery { dataStore.hasSession() } returns false
        coEvery { backend.signInAnonymously() } returns backendSession(userId = "anon", anonymous = true)

        assertTrue(repository.ensureSession().isSuccess)

        coVerify { dataStore.saveSession("access", "refresh", "anon", true) }
    }

    @Test
    fun `ensureSession keeps an existing session`() = runTest {
        coEvery { dataStore.hasSession() } returns true

        repository.ensureSession()

        coVerify(exactly = 0) { backend.signInAnonymously() }
    }

    @Test
    fun `ensureSession failure is returned, not thrown`() = runTest {
        coEvery { dataStore.hasSession() } returns false
        coEvery { backend.signInAnonymously() } throws AppError.Network()

        assertTrue(repository.ensureSession().exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `upgrading the anonymous user keeps its data`() = runTest {
        coEvery { dataStore.currentUserId() } returns "anon"
        coEvery { backend.register(any(), any(), any()) } returns backendSession(userId = "anon")

        val session = repository.register("user@example.com", "password", "User").getOrThrow()

        assertFalse(session.user.isAnonymous)
        coVerify(exactly = 0) { aiHistory.clear() }
        coVerify { dataStore.saveSession("access", "refresh", "anon", false) }
    }

    @Test
    fun `signing into another account clears the previous user's data`() = runTest {
        coEvery { dataStore.currentUserId() } returns "anon"
        coEvery { backend.login(any(), any()) } returns backendSession(userId = "42")

        repository.login("user@example.com", "password")

        coVerify { aiHistory.clear() }
        coVerify { userDao.clear() }
    }

    @Test
    fun `refresh sends stored refresh token and persists replacement`() = runTest {
        coEvery { dataStore.currentRefreshToken() } returns "stored-refresh"
        coEvery { backend.refresh("stored-refresh") } returns backendSession(accessToken = "new-access")

        val token = repository.refreshToken().getOrThrow()

        assertEquals("new-access", token)
        coVerify { dataStore.saveSession("new-access", "refresh", "42", false) }
    }

    @Test
    fun `backend failure is returned without persistence`() = runTest {
        coEvery { backend.login(any(), any()) } throws IllegalStateException("not configured")

        val result = repository.login("user@example.com", "password")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dataStore.saveSession(any(), any(), any(), any()) }
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is propagated instead of wrapped in Result`() = runTest {
        coEvery { backend.login(any(), any()) } throws CancellationException("cancelled")

        repository.login("user@example.com", "password")
    }

    @Test
    fun `logout clears everything then starts a new anonymous session`() = runTest {
        coEvery { dataStore.hasSession() } returns false
        coEvery { backend.signInAnonymously() } returns backendSession(userId = "anon-2", anonymous = true)

        repository.logout()

        verify(exactly = 1) { backend.logout() }
        coVerifyOrder {
            dataStore.clearSession()
            aiHistory.clear()
            backend.signInAnonymously()
            dataStore.saveSession("access", "refresh", "anon-2", true)
        }
    }

    private fun backendSession(
        userId: String = "42",
        displayName: String = "User",
        accessToken: String = "access",
        anonymous: Boolean = false,
    ) = BackendAuthSession(
        userId = userId,
        email = if (anonymous) "" else "user@example.com",
        displayName = displayName,
        accessToken = accessToken,
        refreshToken = "refresh",
        isAnonymous = anonymous,
    )
}
