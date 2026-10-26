package com.viettel.appbase.core.network

import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private var currentToken: String? = "expired"
    private val refreshCount = AtomicInteger()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `401 refreshes token and retries once`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val response = client(refreshedToken = "fresh").newCall(request()).execute()

        assertEquals(200, response.code)
        assertEquals("Bearer expired", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh", server.takeRequest().getHeader("Authorization"))
        assertEquals(1, refreshCount.get())
    }

    @Test
    fun `failed refresh returns original 401`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client(refreshedToken = null).newCall(request()).execute()

        assertEquals(401, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `second 401 after retry is not refreshed again`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client(refreshedToken = "fresh").newCall(request()).execute()

        assertEquals(401, response.code)
        assertEquals(1, refreshCount.get())
    }

    @Test
    fun `unauthenticated request is not refreshed`() {
        currentToken = null
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client(refreshedToken = "fresh").newCall(request()).execute()

        assertEquals(401, response.code)
        assertEquals(0, refreshCount.get())
    }

    private fun client(refreshedToken: String?): OkHttpClient {
        val tokenProvider = object : TokenProvider {
            override fun currentAccessToken(): String? = currentToken
        }
        val refresher = object : TokenRefresher {
            override suspend fun refreshAccessToken(): String? {
                refreshCount.incrementAndGet()
                refreshedToken?.let { currentToken = it }
                return refreshedToken
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .authenticator(TokenAuthenticator(tokenProvider, lazyOf(refresher)))
            .build()
    }

    private fun request() = Request.Builder().url(server.url("profile")).build()
}
