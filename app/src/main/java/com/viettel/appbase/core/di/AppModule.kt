package com.viettel.appbase.core.di

import com.viettel.appbase.BuildConfig
import com.viettel.appbase.data.repository.AuthRepositoryImpl
import com.viettel.appbase.data.repository.AuthBackend
import com.viettel.appbase.data.repository.FakeAuthRepository
import com.viettel.appbase.data.repository.FirebaseAuthBackend
import com.viettel.appbase.data.repository.FirebaseProvider
import com.viettel.appbase.domain.repository.AuthRepository
import com.viettel.appbase.domain.usecase.LoginUseCase
import com.viettel.appbase.domain.usecase.LogoutUseCase
import com.viettel.appbase.domain.usecase.RefreshTokenUseCase
import com.viettel.appbase.domain.usecase.RegisterUseCase
import com.viettel.appbase.feature.auth.login.LoginViewModel
import com.viettel.appbase.feature.auth.register.RegisterViewModel
import com.viettel.appbase.feature.home.HomeViewModel
import com.viettel.appbase.feature.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { FirebaseProvider(androidContext()) }
    single<AuthBackend> { FirebaseAuthBackend(get()) }
    single<AuthRepository> {
        if (BuildConfig.USE_FAKE_AUTH) FakeAuthRepository(get()) else AuthRepositoryImpl(get(), get(), get())
    }
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { RefreshTokenUseCase(get()) }
    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::HomeViewModel)
}

val appModules = listOf(databaseModule, networkModule, appModule)
