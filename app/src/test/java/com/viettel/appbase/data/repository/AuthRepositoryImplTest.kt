package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.data.local.dao.UserDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {
    private val backend = mockk<AuthBackend>()
    private val dataStore = mockk<AppDataStore>()
    private val userDao = mockk<UserDao>()
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        every { dataStore.isAuthenticated } returns flowOf(false)
        repository = AuthRepositoryImpl(backend, dataStore, userDao)
    }

    @Test
    fun `login persists backend session and maps user`() = runTest {
        val remote = backendSession(displayName = "")
        coEvery { backend.login("user@example.com", "password") } returns remote
        coEvery { userDao.upsert(any()) } returns Unit
        coEvery { dataStore.saveSession(any(), any(), any()) } returns Unit

        val result = repository.login("user@example.com", "password").getOrThrow()

        assertEquals("user", result.user.displayName)
        assertEquals("access", result.token.accessToken)
        coVerify { dataStore.saveSession("access", "refresh", "42") }
        coVerify { userDao.upsert(match { it.id == "42" && it.email == "user@example.com" }) }
    }

    @Test
    fun `register delegates and preserves display name`() = runTest {
        val remote = backendSession(displayName = "Product User")
        coEvery { backend.register(any(), any(), any()) } returns remote
        coEvery { userDao.upsert(any()) } returns Unit
        coEvery { dataStore.saveSession(any(), any(), any()) } returns Unit

        val result = repository.register("user@example.com", "password", "Product User").getOrThrow()

        assertEquals("Product User", result.user.displayName)
        coVerify { backend.register("user@example.com", "password", "Product User") }
    }

    @Test
    fun `refresh persists replacement token`() = runTest {
        coEvery { backend.refresh() } returns backendSession(accessToken = "new-access")
        coEvery { userDao.upsert(any()) } returns Unit
        coEvery { dataStore.saveSession(any(), any(), any()) } returns Unit

        val token = repository.refreshToken().getOrThrow()

        assertEquals("new-access", token)
        coVerify { dataStore.saveSession("new-access", "refresh", "42") }
    }

    @Test
    fun `backend failure is returned without persistence`() = runTest {
        coEvery { backend.login(any(), any()) } throws IllegalStateException("not configured")

        val result = repository.login("user@example.com", "password")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dataStore.saveSession(any(), any(), any()) }
    }

    @Test
    fun `logout clears backend and local state`() = runTest {
        every { backend.logout() } returns Unit
        coEvery { dataStore.clearSession() } returns Unit
        coEvery { userDao.clear() } returns Unit

        repository.logout()

        verify(exactly = 1) { backend.logout() }
        coVerify(exactly = 1) { dataStore.clearSession() }
        coVerify(exactly = 1) { userDao.clear() }
    }

    private fun backendSession(
        displayName: String = "User",
        accessToken: String = "access",
    ) = BackendAuthSession(
        userId = "42",
        email = "user@example.com",
        displayName = displayName,
        accessToken = accessToken,
        refreshToken = "refresh",
    )
}
