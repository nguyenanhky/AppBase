package com.viettel.appbase.data.repository

import com.viettel.appbase.core.network.ApiClient
import com.viettel.appbase.data.remote.api.AuthApi
import com.viettel.appbase.domain.model.AppError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RetrofitAuthBackendTest {
    private lateinit var server: MockWebServer
    private lateinit var backend: RetrofitAuthBackend

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val api = ApiClient.create(server.url("/").toString(), OkHttpClient(), json).create(AuthApi::class.java)
        backend = RetrofitAuthBackend(api)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `login maps response to session`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"a","refresh_token":"r","user":{"id":"42","email":"u@example.com"}}""",
            ),
        )

        val session = backend.login("u@example.com", "password")

        assertEquals(BackendAuthSession("42", "u@example.com", "", "a", "r"), session)
        assertEquals("/auth/login", server.takeRequest().path)
    }

    @Test
    fun `login 401 means invalid credentials`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        assertTrue(failure { backend.login("u@example.com", "wrong") } is AppError.InvalidCredentials)
    }

    @Test
    fun `refresh 401 means unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        assertTrue(failure { backend.refresh("expired") } is AppError.Unauthorized)
    }

    @Test
    fun `server error keeps status code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(503, (failure { backend.login("u@example.com", "password") } as AppError.Server).code)
    }

    @Test
    fun `malformed body is a server error`() = runTest {
        server.enqueue(MockResponse().setBody("{}"))

        assertTrue(failure { backend.login("u@example.com", "password") } is AppError.Server)
    }

    @Test
    fun `connection failure is a network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(failure { backend.login("u@example.com", "password") } is AppError.Network)
    }

    private suspend fun failure(block: suspend () -> Unit): Throwable? = runCatching { block() }.exceptionOrNull()
}
