package com.viettel.appbase.feature.scan

import android.net.Uri
import com.viettel.appbase.core.ai.AiClient
import com.viettel.appbase.core.ai.AiInput
import com.viettel.appbase.core.ai.AiQuota
import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.core.image.ImagePreprocessor
import com.viettel.appbase.data.repository.AiHistoryItem
import com.viettel.appbase.data.repository.AiHistoryRepository
import com.viettel.appbase.domain.model.AppError
import java.io.File
import kotlinx.serialization.json.Json

data class ScanHistoryUi(
    val id: Long,
    val title: String?,
    val imageFile: File?,
    val createdAtEpochMillis: Long,
)

data class ScanUiState(
    val selectedImage: Uri? = null,
    val isScanning: Boolean = false,
    val result: DemoScanResult? = null,
    val quota: AiQuota? = null,
    val reported: Boolean = false,
    val error: AppError? = null,
    val history: List<ScanHistoryUi> = emptyList(),
)

class ScanViewModel(
    private val aiClient: AiClient,
    private val imagePreprocessor: ImagePreprocessor,
    private val history: AiHistoryRepository,
    private val json: Json,
) : BaseViewModel<ScanUiState, Nothing>(ScanUiState()) {
    private var lastRawJson: String? = null
    private var lastHistoryId: Long? = null

    init {
        launchSafely {
            history.observe(DemoScan.TASK).collect { items -> updateState { copy(history = items.map { it.toUi() }) } }
        }
    }

    fun onImagePicked(uri: Uri) {
        if (currentState.isScanning) return
        updateState { copy(selectedImage = uri, isScanning = true, result = null, reported = false, error = null) }
        launchSafely(onError = { error -> updateState { copy(isScanning = false, error = error) } }) {
            val image = imagePreprocessor.prepare(uri)
            val result = aiClient.run(DemoScan.TASK, AiInput(images = listOf(image)), DemoScanResult.serializer())
            lastRawJson = result.rawJson
            lastHistoryId = history.save(DemoScan.TASK, result.rawJson, image)
            updateState { copy(isScanning = false, result = result.data, quota = result.quota ?: quota) }
        }
    }

    fun reportResult() {
        val raw = lastRawJson ?: return
        val historyId = lastHistoryId
        launchSafely(onError = { error -> updateState { copy(error = error) } }) {
            aiClient.report(DemoScan.TASK, raw, REPORT_REASON)
            historyId?.let { history.markReported(it) }
            updateState { copy(reported = true) }
        }
    }

    private fun AiHistoryItem.toUi() = ScanHistoryUi(
        id = id,
        title = runCatching { json.decodeFromString(DemoScanResult.serializer(), resultJson) }.getOrNull()?.name,
        imageFile = imageFile,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    private companion object {
        const val REPORT_REASON = "user_flagged"
    }
}
