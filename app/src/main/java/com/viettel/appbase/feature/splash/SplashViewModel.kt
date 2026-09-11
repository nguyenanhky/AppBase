package com.viettel.appbase.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viettel.appbase.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SplashDestination { LOGIN, HOME }

class SplashViewModel(repository: AuthRepository) : ViewModel() {
    val destination: StateFlow<SplashDestination?> = repository.isAuthenticated
        .map { authenticated -> if (authenticated) SplashDestination.HOME else SplashDestination.LOGIN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
