package com.viettel.appbase.core.monetization.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.viettel.appbase.core.monetization.MonetizationConfig
import com.viettel.appbase.core.monetization.billing.BillingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Entry point for ads. Start order matters: restore purchases first (ad-free users never see the
 * consent form or ads), then consent, then the Mobile Ads SDK.
 */
class AdsController(
    private val context: Context,
    private val config: MonetizationConfig,
    private val consentManager: AdsConsentManager,
    private val billingController: BillingController,
    private val interstitials: InterstitialAdController,
    applicationScope: CoroutineScope,
) {
    private val sdkReady = MutableStateFlow(false)
    private val mutablePrivacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = mutablePrivacyOptionsRequired.asStateFlow()

    /** True only when ads are enabled, consented, the SDK is ready and the user has not bought "remove ads". */
    val adsVisible: StateFlow<Boolean> = combine(sdkReady, billingController.isAdFree) { ready, adFree ->
        ready && !adFree
    }.stateIn(applicationScope, SharingStarted.Eagerly, false)

    private val initMutex = Mutex()
    private var initialized = false

    /** Safe to call on every Activity creation; work runs once per process. */
    suspend fun initialize(activity: Activity) {
        if (!config.adsEnabled) return
        initMutex.withLock {
            if (initialized) return
            initialized = startAds(activity)
        }
    }

    fun showInterstitial(activity: Activity): Boolean = adsVisible.value && interstitials.showIfReady(activity)

    fun showPrivacyOptions(activity: Activity) = consentManager.showPrivacyOptionsForm(activity)

    /** @return true when initialization reached a final state (no need to retry). */
    private suspend fun startAds(activity: Activity): Boolean {
        billingController.refresh()
        if (billingController.isAdFree.first()) return true
        consentManager.gatherConsent(activity)
        mutablePrivacyOptionsRequired.value = consentManager.isPrivacyOptionsRequired
        if (!consentManager.canRequestAds) return true
        withContext(Dispatchers.IO) { MobileAds.initialize(context) }
        sdkReady.value = true
        interstitials.preload()
        return true
    }
}
