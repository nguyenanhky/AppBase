package com.viettel.appbase.core.storage

import androidx.datastore.core.DataStore
import com.viettel.appbase.core.network.TokenProvider
import com.viettel.appbase.domain.model.AuthState
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

// One small accessor per preference group. If it keeps growing, split session data into its own store.
@Suppress("TooManyFunctions")
class AppDataStore(private val dataStore: DataStore<AppPreferences>, applicationScope: CoroutineScope) :
    TokenProvider {
    private val cachedToken = AtomicReference<String?>(null)

    val preferences: Flow<AppPreferences> = dataStore.data.catch { exception ->
        Timber.e(exception, "Unable to read preferences")
        emit(AppPreferences())
    }
    val authState: Flow<AuthState> = preferences.map {
        when {
            it.accessToken.isBlank() -> AuthState.SignedOut
            it.isAnonymous -> AuthState.Anonymous
            else -> AuthState.SignedIn
        }
    }.distinctUntilChanged()
    val themeMode: Flow<ThemeMode> = preferences.map { it.themeMode }

    init {
        applicationScope.launch {
            preferences.collect { cachedToken.set(it.accessToken.ifBlank { null }) }
        }
    }

    override fun currentAccessToken(): String? = cachedToken.get()

    suspend fun currentRefreshToken(): String = preferences.first().refreshToken

    suspend fun currentUserId(): String = preferences.first().userId

    suspend fun hasSession(): Boolean = preferences.first().accessToken.isNotBlank()

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        isAnonymous: Boolean = false,
    ) {
        cachedToken.set(accessToken)
        dataStore.updateData {
            it.copy(accessToken = accessToken, refreshToken = refreshToken, userId = userId, isAnonymous = isAnonymous)
        }
    }

    suspend fun clearSession() {
        cachedToken.set(null)
        dataStore.updateData { it.copy(accessToken = "", refreshToken = "", userId = "", isAnonymous = false) }
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.updateData { it.copy(fcmToken = token) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { it.copy(themeMode = mode) }
    }

    suspend fun recordLaunch(nowEpochMillis: Long) {
        dataStore.updateData {
            it.copy(
                launchCount = it.launchCount + 1,
                firstLaunchEpochMillis = it.firstLaunchEpochMillis.takeIf { first -> first > 0 } ?: nowEpochMillis,
            )
        }
    }

    suspend fun markReviewPrompted(nowEpochMillis: Long) {
        dataStore.updateData { it.copy(lastReviewPromptEpochMillis = nowEpochMillis) }
    }

    suspend fun setAdFree(adFree: Boolean) {
        dataStore.updateData { it.copy(adFree = adFree) }
    }
}
