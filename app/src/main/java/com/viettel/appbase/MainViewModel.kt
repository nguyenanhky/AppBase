package com.viettel.appbase

import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.core.playstore.InAppReviewController
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.flow.combine

/** [isAuthenticated] is null until the stored session has been read. */
data class MainUiState(val themeMode: ThemeMode = ThemeMode.SYSTEM, val isAuthenticated: Boolean? = null)

class MainViewModel(
    authRepository: AuthRepository,
    appDataStore: AppDataStore,
    reviewController: InAppReviewController,
) : BaseViewModel<MainUiState, Nothing>(MainUiState()) {
    init {
        launchSafely { reviewController.onAppLaunched() }
        launchSafely {
            combine(appDataStore.themeMode, authRepository.isAuthenticated) { themeMode, authenticated ->
                MainUiState(themeMode, authenticated)
            }.collect { state -> updateState { state } }
        }
    }
}
