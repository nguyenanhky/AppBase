package com.viettel.appbase.feature.home

import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.domain.usecase.LogoutUseCase

data class HomeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoggingOut: Boolean = false,
)

/** Navigation after logout is handled by the app-wide auth gate in AppNavHost. */
class HomeViewModel(
    private val logout: LogoutUseCase,
    private val appDataStore: AppDataStore,
) : BaseViewModel<HomeUiState, Nothing>(HomeUiState()) {
    init {
        launchSafely {
            appDataStore.themeMode.collect { mode -> updateState { copy(themeMode = mode) } }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        launchSafely { appDataStore.setThemeMode(mode) }
    }

    fun logout() {
        if (currentState.isLoggingOut) return
        launchSafely(onError = { updateState { copy(isLoggingOut = false) } }) {
            updateState { copy(isLoggingOut = true) }
            logout.invoke()
        }
    }
}
