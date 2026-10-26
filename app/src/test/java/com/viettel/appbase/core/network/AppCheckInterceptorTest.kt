package com.viettel.appbase.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AppCheckInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `adds the App Check token header`() {
        send { "app-check-token" }

        assertEquals("app-check-token", server.takeRequest().getHeader(AppCheckInterceptor.APP_CHECK_HEADER))
    }

    @Test
    fun `sends the request unchanged without a token`() {
        send { null }

        assertNull(server.takeRequest().getHeader(AppCheckInterceptor.APP_CHECK_HEADER))
    }

    private fun send(provider: AppCheckTokenProvider) {
        server.enqueue(MockResponse())
        OkHttpClient.Builder().addInterceptor(AppCheckInterceptor(provider)).build()
            .newCall(Request.Builder().url(server.url("/")).build()).execute().close()
    }
}
