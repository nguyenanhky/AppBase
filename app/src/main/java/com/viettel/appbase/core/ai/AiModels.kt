package com.viettel.appbase.core.ai

import kotlinx.serialization.Serializable

/** An image ready to upload: already resized, re-encoded and stripped of EXIF by ImagePreprocessor. */
class AiImage(val bytes: ByteArray, val mimeType: String)

class AiInput(val images: List<AiImage> = emptyList(), val text: String? = null)

@Serializable
data class AiQuota(
    val remaining: Int,
    val limit: Int,
    val resetAt: String? = null,
)

/** [rawJson] is the result exactly as returned, kept for history and content reports. */
data class AiResult<T>(
    val data: T,
    val rawJson: String,
    val quota: AiQuota?,
)
