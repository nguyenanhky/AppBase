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

class LoginUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = LoginUseCase(repository)

    @Test
    fun `invalid email fails without calling repository`() = runTest {
        val result = useCase("invalid", "password")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `short password fails without calling repository`() = runTest {
        val result = useCase("user@example.com", "123")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `valid form is normalized and delegated`() = runTest {
        val session = AuthSession(User("1", "user@example.com", "User"), AuthToken("a", "r"))
        coEvery { repository.login("user@example.com", "password") } returns Result.success(session)

        val result = useCase(" User@Example.com ", "password")

        assertEquals(session, result.getOrThrow())
        coVerify(exactly = 1) { repository.login("user@example.com", "password") }
    }
}
