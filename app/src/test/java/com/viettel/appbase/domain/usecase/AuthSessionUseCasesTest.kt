package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthSessionUseCasesTest {
    private val repository = mockk<AuthRepository>()

    @Test
    fun `logout delegates to repository`() = runTest {
        coEvery { repository.logout() } returns Unit

        LogoutUseCase(repository).invoke()

        coVerify(exactly = 1) { repository.logout() }
    }

    @Test
    fun `refresh returns repository token`() = runTest {
        coEvery { repository.refreshToken() } returns Result.success("new-token")

        val result = RefreshTokenUseCase(repository).invoke()

        assertEquals("new-token", result.getOrThrow())
    }
}
