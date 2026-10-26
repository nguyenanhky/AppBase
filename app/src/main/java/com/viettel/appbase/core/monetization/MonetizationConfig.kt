package com.viettel.appbase.core.monetization

import com.viettel.appbase.BuildConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Values come from gradle.properties (ads.enabled, admob.*, billing.*) through BuildConfig. */
data class MonetizationConfig(
    val adsEnabled: Boolean,
    val bannerAdUnitId: String,
    val interstitialAdUnitId: String,
    val removeAdsProductId: String,
    /** Minimum gap between interstitials, also counted from app start. */
    val interstitialMinInterval: Duration = 3.minutes,
) {
    companion object {
        fun fromBuildConfig() = MonetizationConfig(
            adsEnabled = BuildConfig.ADS_ENABLED,
            bannerAdUnitId = BuildConfig.ADMOB_BANNER_ID,
            interstitialAdUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            removeAdsProductId = BuildConfig.REMOVE_ADS_PRODUCT_ID,
        )
    }
}
