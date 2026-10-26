package com.viettel.appbase.feature.auth.login

import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.domain.usecase.LoginUseCase

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface LoginEvent {
    data object Authenticated : LoginEvent
}

class LoginViewModel(private val login: LoginUseCase) : BaseViewModel<LoginUiState, LoginEvent>(LoginUiState()) {
    fun onEmailChanged(value: String) = updateState { copy(email = value, error = null) }
    fun onPasswordChanged(value: String) = updateState { copy(password = value, error = null) }

    fun submit() {
        val form = currentState
        if (form.isLoading) return
        launchSafely(onError = { error -> updateState { copy(isLoading = false, error = error) } }) {
            updateState { copy(isLoading = true, error = null) }
            login(form.email, form.password).getOrThrow()
            updateState { copy(isLoading = false) }
            sendEvent(LoginEvent.Authenticated)
        }
    }
}
