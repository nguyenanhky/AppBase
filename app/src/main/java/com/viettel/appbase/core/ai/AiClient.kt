package com.viettel.appbase.core.ai

import kotlinx.serialization.KSerializer

/**
 * Runs a named AI task on the backend. Prompts, models and output schemas live server-side
 * (Worker task config), so they can change without an app release. Throws AppError on failure,
 * including QuotaExceeded and ContentBlocked.
 */
interface AiClient {
    suspend fun <T> run(
        task: String,
        input: AiInput,
        resultSerializer: KSerializer<T>,
    ): AiResult<T>

    /** Google Play requires a way to report offensive AI-generated content. */
    suspend fun report(
        task: String,
        rawResultJson: String,
        reason: String,
    )
}
