package com.viettel.appbase.core.ai

import com.viettel.appbase.core.network.apiCall
import com.viettel.appbase.domain.model.AppError
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class WorkerAiClient(
    private val api: AiApi,
    private val json: Json,
    private val sessionGuard: AiSessionGuard,
) : AiClient {
    override suspend fun <T> run(
        task: String,
        input: AiInput,
        resultSerializer: KSerializer<T>,
    ): AiResult<T> {
        sessionGuard.ensureSession()
        val request = AiRunRequest(
            task = task,
            locale = Locale.getDefault().toLanguageTag(),
            text = input.text,
            images = input.images.map { AiImagePayload(it.mimeType, Base64.encode(it.bytes)) },
        )
        val response = aiCall { api.run(request) }
        val data = aiCall { json.decodeFromJsonElement(resultSerializer, response.result) }
        return AiResult(data, response.result.toString(), response.quota)
    }

    override suspend fun report(
        task: String,
        rawResultJson: String,
        reason: String,
    ) {
        aiCall { api.report(AiReportRequest(task, json.parseToJsonElement(rawResultJson), reason)) }
    }

    private inline fun <T> aiCall(block: () -> T): T = apiCall {
        try {
            block()
        } catch (exception: HttpException) {
            throw exception.toAiError() ?: exception
        }
    }

    /** Worker-specific errors; anything else falls through to apiCall's generic mapping. */
    private fun HttpException.toAiError(): AppError? {
        val body = response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString(AiErrorBody.serializer(), it) }.getOrNull() }
        return when {
            code() == HTTP_TOO_MANY_REQUESTS || body?.error == "quota_exceeded" -> AppError.QuotaExceeded(
                body?.resetAt,
                this,
            )

            body?.error == "content_blocked" -> AppError.ContentBlocked(this)

            code() == HTTP_FORBIDDEN && body?.error == "app_check_failed" -> AppError.AppNotVerified(this)

            else -> null
        }
    }

    private companion object {
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
