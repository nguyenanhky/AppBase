package com.viettel.appbase.core.ai

import com.viettel.appbase.core.network.ApiClient
import com.viettel.appbase.domain.model.AppError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkerAiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: WorkerAiClient
    private val json = Json { ignoreUnknownKeys = true }
    private var sessionError: Throwable? = null
    private val sessionGuard = object : AiSessionGuard {
        override suspend fun ensureSession() {
            sessionError?.let { throw it }
        }
    }

    @Serializable
    data class Result(val name: String)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = ApiClient.create(server.url("/").toString(), OkHttpClient(), json).create(AiApi::class.java)
        client = WorkerAiClient(api, json, sessionGuard)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `run sends task and base64 image, returns typed result and quota`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"result":{"name":"Ladybug"},"quota":{"remaining":4,"limit":5}}"""),
        )

        val result = client.run(
            "identify",
            AiInput(listOf(AiImage(byteArrayOf(1, 2, 3), "image/jpeg"))),
            Result.serializer(),
        )

        assertEquals("Ladybug", result.data.name)
        assertEquals(AiQuota(4, 5), result.quota)
        assertEquals("""{"name":"Ladybug"}""", result.rawJson)
        val request = server.takeRequest()
        assertEquals("/v1/run", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains(""""task":"identify""""))
        assertTrue(body.contains(""""data":"AQID""""))
    }

    @Test
    fun `429 becomes QuotaExceeded with reset time`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(
                429,
            ).setBody("""{"error":"quota_exceeded","resetAt":"2026-10-27T00:00:00Z"}"""),
        )

        val error = failure { client.run("identify", AiInput(), Result.serializer()) }

        assertEquals("2026-10-27T00:00:00Z", (error as AppError.QuotaExceeded).resetAt)
    }

    @Test
    fun `content_blocked becomes ContentBlocked`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":"content_blocked"}"""))

        assertTrue(failure { client.run("identify", AiInput(), Result.serializer()) } is AppError.ContentBlocked)
    }

    @Test
    fun `app_check_failed becomes AppNotVerified`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"app_check_failed"}"""))

        assertTrue(failure { client.run("identify", AiInput(), Result.serializer()) } is AppError.AppNotVerified)
    }

    @Test
    fun `no session and offline fails before calling the Worker`() = runTest {
        sessionError = AppError.Network()

        assertTrue(failure { client.run("identify", AiInput(), Result.serializer()) } is AppError.Network)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `other server errors keep the generic mapping`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(503, (failure { client.run("identify", AiInput(), Result.serializer()) } as AppError.Server).code)
    }

    @Test
    fun `result not matching the schema is a server error`() = runTest {
        server.enqueue(MockResponse().setBody("""{"result":{"unexpected":true}}"""))

        assertTrue(failure { client.run("identify", AiInput(), Result.serializer()) } is AppError.Server)
    }

    @Test
    fun `report posts the raw result`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        client.report("identify", """{"name":"Ladybug"}""", "user_flagged")

        val request = server.takeRequest()
        assertEquals("/v1/report", request.path)
        assertTrue(request.body.readUtf8().contains(""""result":{"name":"Ladybug"}"""))
    }

    private suspend fun failure(block: suspend () -> Unit): Throwable? = runCatching { block() }.exceptionOrNull()
}
