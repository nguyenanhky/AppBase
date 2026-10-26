package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthSessionUseCasesTest {
    private val repository = mockk<AuthRepository>()

    @Test
    fun `logout delegates to repository`() = runTest {
        coEvery { repository.logout() } returns Unit

        LogoutUseCase(repository).invoke()

        coVerify(exactly = 1) { repository.logout() }
    }
}
