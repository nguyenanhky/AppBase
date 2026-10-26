package com.viettel.appbase.core.base

import app.cash.turbine.test
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.testing.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `state changes are exposed to observers`() {
        val viewModel = TestViewModel()

        viewModel.rename("ready")

        assertEquals(TestState(name = "ready"), viewModel.state.value)
    }

    @Test
    fun `events are delivered once`() = runTest {
        val viewModel = TestViewModel()

        viewModel.emit("done")

        viewModel.events.test {
            assertEquals("done", awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `failures are converted to AppError`() {
        val viewModel = TestViewModel()

        viewModel.run { throw IllegalStateException("boom") }

        assertTrue(viewModel.state.value.error is AppError.Unknown)
    }

    @Test
    fun `typed failures are passed through unchanged`() {
        val viewModel = TestViewModel()

        viewModel.run { throw AppError.Network() }

        assertTrue(viewModel.state.value.error is AppError.Network)
    }

    @Test
    fun `cancellation is not reported as an error`() {
        val viewModel = TestViewModel()

        viewModel.run { throw CancellationException("cancelled") }

        assertNull(viewModel.state.value.error)
    }

    private data class TestState(val name: String = "", val error: AppError? = null)

    private class TestViewModel : BaseViewModel<TestState, String>(TestState()) {
        fun rename(value: String) = updateState { copy(name = value) }
        fun emit(event: String) = sendEvent(event)
        fun run(block: suspend () -> Unit) {
            launchSafely(onError = { error -> updateState { copy(error = error) } }) { block() }
        }
    }
}
