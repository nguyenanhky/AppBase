package com.viettel.appbase.core.base

import org.junit.Assert.assertEquals
import org.junit.Test

class BaseViewModelTest {
    @Test
    fun `state changes are exposed to observers`() {
        val viewModel = TestViewModel()

        viewModel.publish("ready")

        assertEquals(UiState.Success("ready"), viewModel.state.value)
    }

    private class TestViewModel : BaseViewModel<String>() {
        fun publish(value: String) = setState(UiState.Success(value))
    }
}
