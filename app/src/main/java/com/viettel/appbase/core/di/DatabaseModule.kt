package com.viettel.appbase.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.viettel.appbase.core.network.TokenProvider
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.AppDatabase
import com.viettel.appbase.core.storage.AppPreferences
import com.viettel.appbase.core.storage.AppPreferencesSerializer
import com.viettel.appbase.core.storage.DataCipher
import com.viettel.appbase.core.storage.KeystoreDataCipher
import com.viettel.appbase.core.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import timber.log.Timber

val databaseModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single<DataCipher> { KeystoreDataCipher(Constants.PREFERENCES_KEY_ALIAS) }
    single<DataStore<AppPreferences>> {
        DataStoreFactory.create(
            serializer = AppPreferencesSerializer(get()),
            // Unreadable file (corruption, lost Keystore key) resets to defaults instead of failing forever.
            corruptionHandler = ReplaceFileCorruptionHandler { exception ->
                Timber.e(exception, "App preferences corrupted, resetting")
                AppPreferences()
            },
            scope = get<CoroutineScope>(),
            produceFile = { androidContext().dataStoreFile(Constants.DATASTORE_FILE_NAME) },
        )
    }
    single { AppDataStore(get(), get()) }
    single<TokenProvider> { get<AppDataStore>() }
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }
    single { get<AppDatabase>().userDao() }
}
