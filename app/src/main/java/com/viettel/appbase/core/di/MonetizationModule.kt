package com.viettel.appbase.core.di

import com.viettel.appbase.core.monetization.MonetizationConfig
import com.viettel.appbase.core.monetization.ads.AdsConsentManager
import com.viettel.appbase.core.monetization.ads.AdsController
import com.viettel.appbase.core.monetization.ads.InterstitialAdController
import com.viettel.appbase.core.monetization.billing.BillingController
import com.viettel.appbase.core.monetization.billing.BillingGateway
import com.viettel.appbase.core.monetization.billing.PlayBillingGateway
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val monetizationModule = module {
    single { MonetizationConfig.fromBuildConfig() }
    single<BillingGateway> { PlayBillingGateway(androidContext()) }
    single { BillingController(get(), get(), get(), get()) }
    single { AdsConsentManager(androidContext()) }
    single { InterstitialAdController(androidContext(), get()) }
    single { AdsController(androidContext(), get(), get(), get(), get(), get()) }
}
