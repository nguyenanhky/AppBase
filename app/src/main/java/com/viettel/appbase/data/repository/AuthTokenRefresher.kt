package com.viettel.appbase.data.repository

import com.viettel.appbase.core.network.TokenRefresher
import com.viettel.appbase.core.utils.suspendRunCatching
import com.viettel.appbase.domain.model.AppError
import com.viettel.appbase.domain.repository.AuthRepository
import timber.log.Timber

/**
 * Ends the session when the token can no longer be renewed. Network failures keep the session,
 * so users are not signed out just for being offline.
 */
class AuthTokenRefresher(private val repository: AuthRepository) : TokenRefresher {
    override suspend fun refreshAccessToken(): String? = repository.refreshToken().getOrElse { error ->
        if (error is AppError.Network) return null
        Timber.w(error, "Session expired, signing out")
        suspendRunCatching { repository.logout() }.onFailure { Timber.e(it, "Unable to clear expired session") }
        null
    }
}
