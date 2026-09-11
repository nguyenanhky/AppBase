package com.viettel.appbase.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viettel.appbase.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
)

class RegisterViewModel(private val register: RegisterUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = mutableState.asStateFlow()

    fun onDisplayNameChanged(value: String) = mutableState.update { it.copy(displayName = value, errorMessage = null) }
    fun onEmailChanged(value: String) = mutableState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChanged(value: String) = mutableState.update { it.copy(password = value, errorMessage = null) }

    fun submit() {
        val form = state.value
        if (form.isLoading) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            register(form.email, form.password, form.displayName).fold(
                onSuccess = { mutableState.update { state -> state.copy(isLoading = false, isAuthenticated = true) } },
                onFailure = { error ->
                    mutableState.update { state ->
                        state.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.")
                    }
                },
            )
        }
    }
}
