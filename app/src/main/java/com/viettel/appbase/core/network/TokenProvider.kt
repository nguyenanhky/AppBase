package com.viettel.appbase.core.network

interface TokenProvider {
    fun currentAccessToken(): String?
}
