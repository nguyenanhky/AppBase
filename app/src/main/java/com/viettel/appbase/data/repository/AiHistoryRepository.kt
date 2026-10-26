package com.viettel.appbase.data.repository

import android.content.Context
import com.viettel.appbase.core.ai.AiImage
import com.viettel.appbase.data.local.dao.AiHistoryDao
import com.viettel.appbase.data.local.entity.AiHistoryEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AiHistoryItem(
    val id: Long,
    val resultJson: String,
    val imageFile: File?,
    val createdAtEpochMillis: Long,
    val reported: Boolean,
)

/** Past AI results per task, so users can revisit them without spending quota again. */
class AiHistoryRepository(private val dao: AiHistoryDao, context: Context) {
    private val imageDir = File(context.filesDir, "ai_history")

    fun observe(task: String, limit: Int = DEFAULT_LIMIT): Flow<List<AiHistoryItem>> =
        dao.observe(task, limit).map { entities -> entities.map { it.toItem() } }

    /** Stores the already-downsized upload image as the thumbnail. */
    suspend fun save(
        task: String,
        resultJson: String,
        image: AiImage?,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Long = withContext(Dispatchers.IO) {
        val imagePath = image?.let {
            imageDir.mkdirs()
            File(imageDir, "${UUID.randomUUID()}.jpg").apply { writeBytes(it.bytes) }.absolutePath
        }
        dao.insert(
            AiHistoryEntity(
                task = task,
                resultJson = resultJson,
                imagePath = imagePath,
                createdAtEpochMillis = nowEpochMillis,
            ),
        )
    }

    suspend fun markReported(id: Long) = dao.markReported(id)

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.clear()
        imageDir.deleteRecursively()
    }

    private fun AiHistoryEntity.toItem() = AiHistoryItem(
        id = id,
        resultJson = resultJson,
        imageFile = imagePath?.let(::File),
        createdAtEpochMillis = createdAtEpochMillis,
        reported = reported,
    )

    private companion object {
        const val DEFAULT_LIMIT = 50
    }
}
