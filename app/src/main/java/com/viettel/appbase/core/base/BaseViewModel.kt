package com.viettel.appbase.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.domain.model.asAppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [S] is the screen state (an immutable data class) rendered by the UI.
 * [E] is a one-shot event (navigation, snackbar...) delivered once; use [Nothing] when a screen has none.
 * Collect [events] in Compose with [ObserveEvents].
 */
abstract class BaseViewModel<S : Any, E : Any>(initialState: S) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val eventChannel = Channel<E>(Channel.BUFFERED)
    val events: Flow<E> = eventChannel.receiveAsFlow()

    protected val currentState: S get() = mutableState.value

    protected fun updateState(reducer: S.() -> S) = mutableState.update(reducer)

    protected fun sendEvent(event: E) {
        eventChannel.trySend(event)
    }

    /** Launches in [viewModelScope]; any failure is converted to [AppError] and passed to [onError]. */
    protected fun launchSafely(
        onError: (AppError) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val error = exception.asAppError()
            if (error is AppError.Unknown) Timber.e(exception, "Unexpected error in ${this@BaseViewModel::class.simpleName}")
            onError(error)
        }
    }
}
