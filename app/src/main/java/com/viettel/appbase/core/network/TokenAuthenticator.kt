package com.viettel.appbase.core.network

import com.viettel.appbase.core.network.AuthInterceptor.Companion.AUTHORIZATION_HEADER
import com.viettel.appbase.core.network.AuthInterceptor.Companion.BEARER_PREFIX
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Refreshes the access token once on HTTP 401 and retries the request.
 * [tokenRefresher] is lazy to break the OkHttp -> repository -> API -> OkHttp dependency cycle.
 */
class TokenAuthenticator(
    private val tokenProvider: TokenProvider,
    tokenRefresher: Lazy<TokenRefresher>,
) : Authenticator {
    private val tokenRefresher by tokenRefresher
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header(AUTHORIZATION_HEADER)?.removePrefix(BEARER_PREFIX) ?: return null
        if (response.priorResponse != null) return null

        synchronized(lock) {
            val currentToken = tokenProvider.currentAccessToken()
            // Another request may have refreshed the token while this one waited for the lock.
            val token = if (currentToken != null && currentToken != failedToken) {
                currentToken
            } else {
                runBlocking { tokenRefresher.refreshAccessToken() } ?: return null
            }
            return response.request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
                .build()
        }
    }
}
