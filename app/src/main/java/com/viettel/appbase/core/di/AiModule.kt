package com.viettel.appbase.core.di

import com.viettel.appbase.BuildConfig
import com.viettel.appbase.core.ai.AiApi
import com.viettel.appbase.core.ai.AiClient
import com.viettel.appbase.core.ai.AiSessionGuard
import com.viettel.appbase.core.ai.FakeAiClient
import com.viettel.appbase.core.ai.WorkerAiClient
import com.viettel.appbase.core.image.ImagePreprocessor
import com.viettel.appbase.core.network.ApiClient
import com.viettel.appbase.data.repository.AiHistoryRepository
import com.viettel.appbase.domain.repository.AuthRepository
import com.viettel.appbase.feature.scan.DemoScan
import com.viettel.appbase.feature.scan.ScanViewModel
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val aiHttpClient = named("aiHttpClient")

val aiModule = module {
    // Same interceptors (ID token, App Check) as the main client, longer timeout for vision models.
    single(aiHttpClient) {
        get<OkHttpClient>().newBuilder().readTimeout(AI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()
    }
    single<AiApi> { ApiClient.create(BuildConfig.AI_BASE_URL, get(aiHttpClient), get()).create(AiApi::class.java) }
    // Each app adds its tasks' fake samples here so the mock flavor works offline.
    single<AiSessionGuard> {
        val authRepository = get<AuthRepository>()
        object : AiSessionGuard {
            override suspend fun ensureSession() = authRepository.ensureSession().getOrThrow()
        }
    }
    single<AiClient> {
        if (BuildConfig.USE_FAKE_AI) FakeAiClient(get(), DemoScan.fakeSamples) else WorkerAiClient(get(), get(), get())
    }
    single { ImagePreprocessor(androidContext()) }
    single { AiHistoryRepository(get(), androidContext()) }
    viewModelOf(::ScanViewModel)
}

private const val AI_READ_TIMEOUT_SECONDS = 60L
