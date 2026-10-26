package com.viettel.appbase.core.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Collects one-shot events while the screen is at least STARTED. */
@Composable
fun <E> ObserveEvents(events: Flow<E>, onEvent: (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(events, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate prevents losing an event between emission and lifecycle stop.
            withContext(Dispatchers.Main.immediate) {
                events.collect { currentOnEvent(it) }
            }
        }
    }
}
