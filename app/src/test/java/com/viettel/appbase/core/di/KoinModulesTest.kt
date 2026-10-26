package com.viettel.appbase.core.di

import android.content.Context
import kotlin.time.Duration
import okhttp3.OkHttpClient
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Fails when a constructor parameter has no Koin definition, e.g. a new ViewModel
 * that was not registered. Catches at test time what would otherwise crash at runtime.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinModulesTest {
    @Test
    fun `all definitions can be resolved`() {
        module { includes(appModules) }.verify(
            // Types created inside definitions or passed explicitly rather than resolved with get().
            extraTypes = listOf(
                Context::class,
                Boolean::class,
                Lazy::class,
                OkHttpClient.Builder::class,
                // MonetizationConfig is built from BuildConfig, not resolved.
                String::class,
                Duration::class,
            ),
        )
    }
}
