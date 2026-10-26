package com.viettel.appbase.feature.scan

import android.net.Uri
import com.viettel.appbase.core.ai.AiClient
import com.viettel.appbase.core.ai.AiImage
import com.viettel.appbase.core.ai.AiQuota
import com.viettel.appbase.core.ai.AiResult
import com.viettel.appbase.core.image.ImagePreprocessor
import com.viettel.appbase.data.repository.AiHistoryRepository
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val aiClient = mockk<AiClient>()
    private val preprocessor = mockk<ImagePreprocessor>()
    private val history = mockk<AiHistoryRepository>(relaxed = true) {
        every { observe(any(), any()) } returns flowOf(emptyList())
    }
    private val uri = mockk<Uri>()
    private val image = AiImage(byteArrayOf(1), "image/jpeg")
    private val result = DemoScanResult(recognized = true, name = "Bọ rùa")

    private fun viewModel() = ScanViewModel(aiClient, preprocessor, history, Json)

    @Test
    fun `successful scan shows the result, quota and saves history`() {
        coEvery { preprocessor.prepare(uri) } returns image
        coEvery { aiClient.run(DemoScan.TASK, any(), DemoScanResult.serializer()) } returns
            AiResult(result, """{"recognized":true}""", AiQuota(4, 5))
        val viewModel = viewModel()

        viewModel.onImagePicked(uri)

        assertEquals(result, viewModel.state.value.result)
        assertEquals(4, viewModel.state.value.quota?.remaining)
        assertFalse(viewModel.state.value.isScanning)
        coVerify { history.save(DemoScan.TASK, """{"recognized":true}""", image, any()) }
    }

    @Test
    fun `unreadable image shows a typed error and calls no AI`() {
        coEvery { preprocessor.prepare(uri) } throws AppError.InvalidImage()
        val viewModel = viewModel()

        viewModel.onImagePicked(uri)

        assertTrue(viewModel.state.value.error is AppError.InvalidImage)
        coVerify(exactly = 0) { aiClient.run(any(), any(), any<kotlinx.serialization.KSerializer<DemoScanResult>>()) }
    }

    @Test
    fun `quota exceeded is surfaced to the user`() {
        coEvery { preprocessor.prepare(uri) } returns image
        coEvery { aiClient.run(DemoScan.TASK, any(), DemoScanResult.serializer()) } throws AppError.QuotaExceeded()
        val viewModel = viewModel()

        viewModel.onImagePicked(uri)

        assertTrue(viewModel.state.value.error is AppError.QuotaExceeded)
    }

    @Test
    fun `report sends the raw result and marks history`() {
        coEvery { preprocessor.prepare(uri) } returns image
        coEvery { aiClient.run(DemoScan.TASK, any(), DemoScanResult.serializer()) } returns AiResult(result, "{}", null)
        coEvery { history.save(any(), any(), any(), any()) } returns 7L
        coEvery { aiClient.report(any(), any(), any()) } returns Unit
        val viewModel = viewModel()
        viewModel.onImagePicked(uri)

        viewModel.reportResult()

        assertTrue(viewModel.state.value.reported)
        coVerify { aiClient.report(DemoScan.TASK, "{}", any()) }
        coVerify { history.markReported(7L) }
    }
}
