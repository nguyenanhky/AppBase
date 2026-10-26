package com.viettel.appbase.core.ai

import com.viettel.appbase.domain.model.AppError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAiClientTest {
    @Serializable
    data class Result(val name: String)

    private val client = FakeAiClient(Json, mapOf("identify" to """{"name":"Ladybug"}"""), dailyLimit = 2)

    @Test
    fun `returns the registered sample and counts quota down`() = runTest {
        val first = client.run("identify", AiInput(), Result.serializer())
        val second = client.run("identify", AiInput(), Result.serializer())

        assertEquals("Ladybug", first.data.name)
        assertEquals(1, first.quota?.remaining)
        assertEquals(0, second.quota?.remaining)
    }

    @Test
    fun `exhausted quota throws QuotaExceeded`() = runTest {
        repeat(2) { client.run("identify", AiInput(), Result.serializer()) }

        val error = runCatching { client.run("identify", AiInput(), Result.serializer()) }.exceptionOrNull()

        assertTrue(error is AppError.QuotaExceeded)
    }

    @Test
    fun `unknown task fails like a missing endpoint`() = runTest {
        val error = runCatching { client.run("other", AiInput(), Result.serializer()) }.exceptionOrNull()

        assertTrue(error is AppError.Server)
    }
}
