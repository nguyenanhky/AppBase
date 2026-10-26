package com.viettel.appbase.feature.auth.login

import app.cash.turbine.test
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.model.AuthToken
import com.viettel.appbase.domain.model.User
import com.viettel.appbase.domain.usecase.LoginUseCase
import com.viettel.appbase.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val login = mockk<LoginUseCase>()

    @Test
    fun `successful login emits Authenticated`() = runTest {
        val session = AuthSession(User("1", "user@example.com", "User"), AuthToken("a", "r"))
        coEvery { login("user@example.com", "password") } returns Result.success(session)
        val viewModel = LoginViewModel(login)
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password")

        viewModel.submit()

        viewModel.events.test { assertEquals(LoginEvent.Authenticated, awaitItem()) }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `failed login exposes typed error and stops loading`() = runTest {
        coEvery { login(any(), any()) } returns Result.failure(AppError.InvalidCredentials())
        val viewModel = LoginViewModel(login)

        viewModel.submit()

        assertTrue(viewModel.state.value.error is AppError.InvalidCredentials)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.events.test { expectNoEvents() }
    }
}
