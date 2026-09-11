package com.viettel.appbase.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.currentAccessToken()
        if (token.isNullOrBlank() || request.header(SKIP_AUTH_HEADER) != null) {
            return chain.proceed(request.newBuilder().removeHeader(SKIP_AUTH_HEADER).build())
        }

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build(),
        )
    }

    companion object {
        const val SKIP_AUTH_HEADER = "X-Skip-Auth"
    }
}
