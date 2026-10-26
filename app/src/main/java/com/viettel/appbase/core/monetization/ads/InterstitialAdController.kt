package com.viettel.appbase.core.monetization.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.viettel.appbase.core.monetization.MonetizationConfig
import timber.log.Timber

/** Keeps one interstitial preloaded and shows it at most as often as [FrequencyCap] allows. Main thread only. */
class InterstitialAdController(private val context: Context, private val config: MonetizationConfig) {
    private val frequencyCap = FrequencyCap(config.interstitialMinInterval, SystemClock.elapsedRealtime())
    private var ad: InterstitialAd? = null
    private var loading = false

    fun preload() {
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            config.interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Timber.d("Interstitial failed to load: %s", error.message)
                }
            },
        )
    }

    /** @return true if an ad was shown. A new ad is preloaded either way. */
    fun showIfReady(activity: Activity): Boolean {
        val now = SystemClock.elapsedRealtime()
        val current = ad
        if (current == null || !frequencyCap.canShow(now)) {
            preload()
            return false
        }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = preload()

            override fun onAdFailedToShowFullScreenContent(error: AdError) = preload()
        }
        current.show(activity)
        frequencyCap.recordShown(now)
        return true
    }
}
