package com.viettel.appbase.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Proves to your backend (e.g. the AI Worker) that the request comes from the genuine app. */
class AppCheckInterceptor(private val tokenProvider: AppCheckTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.currentToken() ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header(APP_CHECK_HEADER, token).build())
    }

    companion object {
        const val APP_CHECK_HEADER = "X-Firebase-AppCheck"
    }
}

fun interface AppCheckTokenProvider {
    /** Blocking: called on OkHttp's background thread. Null when App Check is unavailable. */
    fun currentToken(): String?
}
