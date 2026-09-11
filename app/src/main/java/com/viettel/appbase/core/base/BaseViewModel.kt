package com.viettel.appbase.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<T>(initialState: UiState<T> = UiState.Empty) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<UiState<T>> = mutableState.asStateFlow()

    protected fun setState(state: UiState<T>) {
        mutableState.value = state
    }

    protected fun launchSafely(
        onError: (Throwable) -> Unit = { setState(UiState.Error(it.message ?: "Unknown error", it)) },
        block: suspend CoroutineScope.() -> Unit,
    ) = viewModelScope.launch(CoroutineExceptionHandler { _, throwable -> onError(throwable) }, block = block)
}
