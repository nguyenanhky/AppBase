package com.viettel.appbase.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class LoggingInterceptor(enabled: Boolean) : Interceptor {
    private val delegate = HttpLoggingInterceptor().apply {
        level = if (enabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    override fun intercept(chain: Interceptor.Chain): Response = delegate.intercept(chain)
}
