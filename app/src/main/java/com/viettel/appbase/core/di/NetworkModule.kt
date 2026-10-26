package com.viettel.appbase.core.di

import com.viettel.appbase.BuildConfig
import com.viettel.appbase.core.network.ApiClient
import com.viettel.appbase.core.network.AuthInterceptor
import com.viettel.appbase.core.network.LoggingInterceptor
import com.viettel.appbase.core.network.TokenAuthenticator
import com.viettel.appbase.core.network.TokenProvider
import com.viettel.appbase.core.network.TokenRefresher
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
    single { TokenAuthenticator(get<TokenProvider>(), lazy { get<TokenRefresher>() }) }
    single { LoggingInterceptor(BuildConfig.DEBUG) }
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<LoggingInterceptor>())
            .authenticator(get<TokenAuthenticator>())
            .build()
    }
    single<Retrofit> { ApiClient.create(BuildConfig.BASE_URL, get(), get()) }
    single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }
}
