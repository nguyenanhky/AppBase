package com.viettel.appbase.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

/** Wire contract with the AI Worker (version 1). Keep in sync with worker/ and the README. */
interface AiApi {
    @POST("v1/run")
    suspend fun run(@Body request: AiRunRequest): AiRunResponse

    @POST("v1/report")
    suspend fun report(@Body request: AiReportRequest)
}

@Serializable
data class AiRunRequest(
    val task: String,
    val locale: String,
    val text: String? = null,
    val images: List<AiImagePayload> = emptyList(),
)

/** [data] is base64 without line breaks. */
@Serializable
data class AiImagePayload(val mimeType: String, val data: String)

@Serializable
data class AiRunResponse(val result: JsonElement, val quota: AiQuota? = null)

@Serializable
data class AiReportRequest(
    val task: String,
    val result: JsonElement,
    val reason: String,
)

/** Error body for 4xx: error is "quota_exceeded", "content_blocked", "invalid_request", ... */
@Serializable
data class AiErrorBody(val error: String, val resetAt: String? = null)
