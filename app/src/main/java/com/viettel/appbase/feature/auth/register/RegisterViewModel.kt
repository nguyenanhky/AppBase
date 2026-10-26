package com.viettel.appbase.feature.auth.register

import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.domain.usecase.RegisterUseCase

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface RegisterEvent {
    data object Authenticated : RegisterEvent
}

class RegisterViewModel(
    private val register: RegisterUseCase,
) : BaseViewModel<RegisterUiState, RegisterEvent>(RegisterUiState()) {
    fun onDisplayNameChanged(value: String) = updateState { copy(displayName = value, error = null) }
    fun onEmailChanged(value: String) = updateState { copy(email = value, error = null) }
    fun onPasswordChanged(value: String) = updateState { copy(password = value, error = null) }

    fun submit() {
        val form = currentState
        if (form.isLoading) return
        launchSafely(onError = { error -> updateState { copy(isLoading = false, error = error) } }) {
            updateState { copy(isLoading = true, error = null) }
            register(form.email, form.password, form.displayName).getOrThrow()
            updateState { copy(isLoading = false) }
            sendEvent(RegisterEvent.Authenticated)
        }
    }
}
