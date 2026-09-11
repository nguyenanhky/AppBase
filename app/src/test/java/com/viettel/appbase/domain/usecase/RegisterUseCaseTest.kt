package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthToken
import com.viettel.appbase.domain.model.User
import com.viettel.appbase.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = RegisterUseCase(repository)

    @Test
    fun `blank display name is rejected`() = runTest {
        val result = useCase("user@example.com", "password", " ")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.register(any(), any(), any()) }
    }

    @Test
    fun `valid form is normalized and delegated`() = runTest {
        val session = AuthSession(User("1", "user@example.com", "User"), AuthToken("a", "r"))
        coEvery { repository.register("user@example.com", "password", "User") } returns Result.success(session)

        val result = useCase(" User@Example.com ", "password", " User ")

        assertEquals(session, result.getOrThrow())
        coVerify { repository.register("user@example.com", "password", "User") }
    }
}
