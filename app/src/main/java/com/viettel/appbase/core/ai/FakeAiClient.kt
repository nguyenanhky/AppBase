package com.viettel.appbase.core.ai

import com.viettel.appbase.domain.model.AppError
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Offline stand-in for the mock flavor: returns a canned JSON per task and simulates the daily
 * quota, so UI (results, quota, errors) can be built without a backend or AI costs.
 * Each app registers a realistic sample for each of its tasks.
 */
class FakeAiClient(
    private val json: Json,
    private val samples: Map<String, String>,
    private val dailyLimit: Int = DEFAULT_DAILY_LIMIT,
) : AiClient {
    private val used = AtomicInteger(0)

    override suspend fun <T> run(
        task: String,
        input: AiInput,
        resultSerializer: KSerializer<T>,
    ): AiResult<T> {
        delay(FAKE_LATENCY_MILLIS)
        val raw = samples[task] ?: throw AppError.Server(code = HTTP_NOT_FOUND)
        if (used.get() >= dailyLimit) throw AppError.QuotaExceeded()
        val count = used.incrementAndGet()
        return AiResult(json.decodeFromString(resultSerializer, raw), raw, AiQuota(dailyLimit - count, dailyLimit))
    }

    override suspend fun report(
        task: String,
        rawResultJson: String,
        reason: String,
    ) {
        Timber.d("Fake AI report for %s: %s", task, reason)
    }

    private companion object {
        const val DEFAULT_DAILY_LIMIT = 5
        const val FAKE_LATENCY_MILLIS = 1_200L
        const val HTTP_NOT_FOUND = 404
    }
}
