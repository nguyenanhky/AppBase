package com.viettel.appbase.core.di

import com.viettel.appbase.BuildConfig
import com.viettel.appbase.MainViewModel
import com.viettel.appbase.core.firebase.FirebaseProvider
import com.viettel.appbase.core.network.TokenRefresher
import com.viettel.appbase.data.repository.AuthBackend
import com.viettel.appbase.data.repository.AuthRepositoryImpl
import com.viettel.appbase.data.repository.AuthTokenRefresher
import com.viettel.appbase.data.repository.FakeAuthRepository
import com.viettel.appbase.data.repository.FirebaseAuthBackend
import com.viettel.appbase.domain.repository.AuthRepository
import com.viettel.appbase.domain.usecase.LoginUseCase
import com.viettel.appbase.domain.usecase.LogoutUseCase
import com.viettel.appbase.domain.usecase.RegisterUseCase
import com.viettel.appbase.feature.auth.login.LoginViewModel
import com.viettel.appbase.feature.auth.register.RegisterViewModel
import com.viettel.appbase.feature.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { FirebaseProvider(androidContext()) }
    // Using your own REST API instead of Firebase Auth? Bind RetrofitAuthBackend(get()) here.
    single<AuthBackend> { FirebaseAuthBackend(get()) }
    single<AuthRepository> {
        if (BuildConfig.USE_FAKE_AUTH) {
            FakeAuthRepository(
                get(),
                get(),
            )
        } else {
            AuthRepositoryImpl(get(), get(), get(), get())
        }
    }
    single<TokenRefresher> { AuthTokenRefresher(get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }
    viewModelOf(::MainViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::HomeViewModel)
}

val appModules = listOf(databaseModule, networkModule, playStoreModule, monetizationModule, aiModule, appModule)
