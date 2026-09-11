package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeAuthRepositoryTest {
    private val dataStore = mockk<AppDataStore>()
    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        every { dataStore.isAuthenticated } returns flowOf(false)
        coEvery { dataStore.saveSession(any(), any(), any()) } returns Unit
        coEvery { dataStore.clearSession() } returns Unit
        repository = FakeAuthRepository(dataStore)
    }

    @Test
    fun `login creates deterministic demo session`() = runTest {
        val session = repository.login("user@example.com", "unused").getOrThrow()

        assertEquals("demo-user", session.user.id)
        assertEquals("demo-token", session.token.accessToken)
        coVerify { dataStore.saveSession("demo-token", "demo-refresh-token", "demo-user") }
    }

    @Test
    fun `register preserves provided display name`() = runTest {
        val session = repository.register("user@example.com", "unused", "New User").getOrThrow()

        assertEquals("New User", session.user.displayName)
    }

    @Test
    fun `refresh and logout update local session`() = runTest {
        assertEquals("demo-token-refreshed", repository.refreshToken().getOrThrow())
        repository.logout()

        coVerify { dataStore.saveSession("demo-token-refreshed", "demo-refresh-token", "demo-user") }
        coVerify { dataStore.clearSession() }
    }
}
