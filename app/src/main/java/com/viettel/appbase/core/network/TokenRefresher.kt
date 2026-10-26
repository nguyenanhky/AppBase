package com.viettel.appbase.core.network

interface TokenRefresher {
    /** Returns a renewed access token, or null when the request should not be retried. */
    suspend fun refreshAccessToken(): String?
}
