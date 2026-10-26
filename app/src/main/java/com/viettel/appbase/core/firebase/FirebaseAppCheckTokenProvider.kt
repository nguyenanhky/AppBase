package com.viettel.appbase.core.firebase

import com.google.android.gms.tasks.Tasks
import com.viettel.appbase.core.network.AppCheckTokenProvider
import java.util.concurrent.TimeUnit
import timber.log.Timber

/** Cached App Check token (refreshed by Firebase). Without Firebase config, requests go out without it. */
class FirebaseAppCheckTokenProvider(private val firebaseProvider: FirebaseProvider) : AppCheckTokenProvider {
    override fun currentToken(): String? {
        val appCheck = firebaseProvider.appCheckOrNull() ?: return null
        return runCatching { Tasks.await(appCheck.getAppCheckToken(false), TIMEOUT_SECONDS, TimeUnit.SECONDS).token }
            .onFailure { Timber.d(it, "App Check token unavailable") }
            .getOrNull()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 10L
    }
}
