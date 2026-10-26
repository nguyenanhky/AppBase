package com.viettel.appbase

import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.core.playstore.InAppReviewController
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.domain.model.AuthState
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.flow.combine
import timber.log.Timber

/** [sessionReady] turns true once the startup session attempt finished (success or offline failure). */
data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val authState: AuthState = AuthState.SignedOut,
    val sessionReady: Boolean = false,
)

class MainViewModel(
    authRepository: AuthRepository,
    appDataStore: AppDataStore,
    reviewController: InAppReviewController,
) : BaseViewModel<MainUiState, Nothing>(MainUiState()) {
    init {
        launchSafely { reviewController.onAppLaunched() }
        launchSafely {
            // Offline at first launch is fine: AI calls retry ensureSession() before each request.
            authRepository.ensureSession().onFailure { Timber.w(it, "Could not start a session") }
            updateState { copy(sessionReady = true) }
        }
        launchSafely {
            combine(appDataStore.themeMode, authRepository.authState, ::Pair).collect { (themeMode, authState) ->
                updateState { copy(themeMode = themeMode, authState = authState) }
            }
        }
    }
}
