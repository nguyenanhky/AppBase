package com.viettel.appbase.core.di

import com.viettel.appbase.BuildConfig
import com.viettel.appbase.core.network.ApiClient
import com.viettel.appbase.core.network.AuthInterceptor
import com.viettel.appbase.core.network.ErrorInterceptor
import com.viettel.appbase.core.network.LoggingInterceptor
import com.viettel.appbase.core.network.TokenProvider
import com.viettel.appbase.data.remote.api.AuthApi
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
    single { AuthInterceptor(get<TokenProvider>()) }
    single { LoggingInterceptor(BuildConfig.DEBUG) }
    single { ErrorInterceptor() }
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(get<ErrorInterceptor>())
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<LoggingInterceptor>())
            .build()
    }
    single<Retrofit> { ApiClient.create(BuildConfig.BASE_URL, get(), get()) }
    single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }
}
