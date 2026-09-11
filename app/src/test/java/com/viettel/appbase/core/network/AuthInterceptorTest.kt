package com.viettel.appbase.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `adds bearer token to authenticated request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider("access-token")))
            .build()

        client.newCall(Request.Builder().url(server.url("profile")).build()).execute().close()

        assertEquals("Bearer access-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `skip auth header prevents authorization and is removed`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider("access-token")))
            .build()
        val request = Request.Builder()
            .url(server.url("login"))
            .header(AuthInterceptor.SKIP_AUTH_HEADER, "true")
            .build()

        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals(null, recorded.getHeader("Authorization"))
        assertEquals(null, recorded.getHeader(AuthInterceptor.SKIP_AUTH_HEADER))
    }

    private fun tokenProvider(token: String?) = object : TokenProvider {
        override fun currentAccessToken(): String? = token
    }
}
