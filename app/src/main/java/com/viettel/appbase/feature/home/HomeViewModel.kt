package com.viettel.appbase.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(val isLoggingOut: Boolean = false, val loggedOut: Boolean = false)

class HomeViewModel(
    private val logout: LogoutUseCase,
    private val appDataStore: AppDataStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()
    val themeMode: StateFlow<ThemeMode> = appDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appDataStore.setThemeMode(mode) }
    }

    fun logout() {
        if (state.value.isLoggingOut) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoggingOut = true) }
            logout.invoke()
            mutableState.value = HomeUiState(loggedOut = true)
        }
    }
}
