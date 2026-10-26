package com.viettel.appbase.data.repository

import com.viettel.appbase.core.storage.AppDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeAuthRepositoryTest {
    private val dataStore = mockk<AppDataStore>(relaxed = true)
    private val aiHistory = mockk<AiHistoryRepository>(relaxed = true)
    private val repository = FakeAuthRepository(dataStore, aiHistory)

    @Test
    fun `starts an anonymous demo session`() = runTest {
        coEvery { dataStore.hasSession() } returns false

        repository.ensureSession()

        coVerify { dataStore.saveSession("demo-anon-token", "", "demo-anon", true) }
    }

    @Test
    fun `login creates deterministic demo session`() = runTest {
        coEvery { dataStore.currentUserId() } returns ""

        val session = repository.login("user@example.com", "unused").getOrThrow()

        assertEquals("demo-user", session.user.id)
        assertEquals("demo-token", session.token.accessToken)
        coVerify { dataStore.saveSession("demo-token", "demo-refresh-token", "demo-user", false) }
    }

    @Test
    fun `register upgrades the anonymous user in place`() = runTest {
        coEvery { dataStore.currentUserId() } returns "demo-anon"

        val session = repository.register("user@example.com", "unused", "New User").getOrThrow()

        assertEquals("demo-anon", session.user.id)
        assertEquals("New User", session.user.displayName)
        coVerify(exactly = 0) { aiHistory.clear() }
    }

    @Test
    fun `logout clears history and returns to an anonymous session`() = runTest {
        coEvery { dataStore.hasSession() } returns false

        repository.logout()

        coVerify { dataStore.clearSession() }
        coVerify { aiHistory.clear() }
        coVerify { dataStore.saveSession("demo-anon-token", "", "demo-anon", true) }
    }
}
