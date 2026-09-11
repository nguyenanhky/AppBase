package com.viettel.appbase.core.storage

import androidx.datastore.core.DataStore
import com.viettel.appbase.core.network.TokenProvider
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class AppDataStore(
    private val dataStore: DataStore<AppPreferences>,
    applicationScope: CoroutineScope,
) : TokenProvider {
    private val cachedToken = AtomicReference<String?>(null)

    val preferences: Flow<AppPreferences> = dataStore.data.catch { exception ->
        Timber.e(exception, "Unable to read preferences")
        emit(AppPreferences())
    }
    val isAuthenticated: Flow<Boolean> = preferences.map { it.accessToken.isNotBlank() }
    val themeMode: Flow<ThemeMode> = preferences.map { it.themeMode }

    init {
        applicationScope.launch {
            preferences.collect { cachedToken.set(it.accessToken.ifBlank { null }) }
        }
    }

    override fun currentAccessToken(): String? = cachedToken.get()

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String) {
        cachedToken.set(accessToken)
        dataStore.updateData {
            it.copy(accessToken = accessToken, refreshToken = refreshToken, userId = userId)
        }
    }

    suspend fun clearSession() {
        cachedToken.set(null)
        dataStore.updateData { it.copy(accessToken = "", refreshToken = "", userId = "") }
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.updateData { it.copy(fcmToken = token) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { it.copy(themeMode = mode) }
    }
}
