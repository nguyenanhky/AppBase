package com.viettel.appbase.feature.home

import android.app.Activity
import com.viettel.appbase.core.base.BaseViewModel
import com.viettel.appbase.core.monetization.MonetizationConfig
import com.viettel.appbase.core.monetization.ads.AdsController
import com.viettel.appbase.core.monetization.billing.BillingController
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.combine

data class MonetizationUiState(
    val adsEnabled: Boolean = false,
    val adFree: Boolean = false,
    val removeAdsPrice: String? = null,
    val purchasePending: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
)

data class HomeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoggingOut: Boolean = false,
    val monetization: MonetizationUiState = MonetizationUiState(),
)

/** Navigation after logout is handled by the app-wide auth gate in AppNavHost. */
class HomeViewModel(
    private val logout: LogoutUseCase,
    private val appDataStore: AppDataStore,
    private val billingController: BillingController,
    private val adsController: AdsController,
    config: MonetizationConfig,
) : BaseViewModel<HomeUiState, Nothing>(HomeUiState()) {
    init {
        launchSafely {
            appDataStore.themeMode.collect { mode -> updateState { copy(themeMode = mode) } }
        }
        if (config.adsEnabled) {
            launchSafely {
                combine(
                    billingController.isAdFree,
                    billingController.state,
                    adsController.privacyOptionsRequired,
                ) { adFree, billing, privacyRequired ->
                    MonetizationUiState(
                        adsEnabled = true,
                        adFree = adFree,
                        removeAdsPrice = billing.removeAdsPrice,
                        purchasePending = billing.purchasePending,
                        privacyOptionsRequired = privacyRequired,
                    )
                }.collect { monetization -> updateState { copy(monetization = monetization) } }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        launchSafely { appDataStore.setThemeMode(mode) }
    }

    fun logout() {
        if (currentState.isLoggingOut) return
        launchSafely(onError = { updateState { copy(isLoggingOut = false) } }) {
            updateState { copy(isLoggingOut = true) }
            logout.invoke()
        }
    }

    // The Activity is only used to show Play/UMP UI and is not kept.
    fun removeAds(activity: Activity) {
        launchSafely { billingController.purchaseRemoveAds(activity) }
    }

    fun restorePurchases() {
        launchSafely { billingController.refresh() }
    }

    fun openPrivacyOptions(activity: Activity) = adsController.showPrivacyOptions(activity)
}
