package com.viettel.appbase.data.repository

import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthTokenRefresherTest {
    private val repository = mockk<AuthRepository>()
    private val refresher = AuthTokenRefresher(repository)

    @Test
    fun `returns refreshed token`() = runTest {
        coEvery { repository.refreshToken() } returns Result.success("fresh")

        assertEquals("fresh", refresher.refreshAccessToken())
        coVerify(exactly = 0) { repository.logout() }
    }

    @Test
    fun `network failure keeps the session`() = runTest {
        coEvery { repository.refreshToken() } returns Result.failure(AppError.Network())

        assertNull(refresher.refreshAccessToken())
        coVerify(exactly = 0) { repository.logout() }
    }

    @Test
    fun `rejected refresh signs the user out`() = runTest {
        coEvery { repository.refreshToken() } returns Result.failure(AppError.Unauthorized())
        coEvery { repository.logout() } returns Unit

        assertNull(refresher.refreshAccessToken())
        coVerify(exactly = 1) { repository.logout() }
    }
}
