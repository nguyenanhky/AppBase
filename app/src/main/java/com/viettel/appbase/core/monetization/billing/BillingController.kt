package com.viettel.appbase.core.monetization.billing

import android.app.Activity
import com.viettel.appbase.core.monetization.MonetizationConfig
import com.viettel.appbase.core.storage.AppDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingState(val removeAdsPrice: String? = null, val purchasePending: Boolean = false)

/**
 * The one-time "remove ads" purchase. The entitlement is cached in DataStore so it survives
 * offline starts, and is only revoked when Play answers authoritatively (refund).
 * Verification is client-side, which is acceptable for a low-value one-time unlock; verify on a
 * server before selling anything valuable.
 */
class BillingController(
    private val gateway: BillingGateway,
    private val appDataStore: AppDataStore,
    private val config: MonetizationConfig,
    applicationScope: CoroutineScope,
) {
    val isAdFree: Flow<Boolean> = appDataStore.preferences.map { it.adFree }.distinctUntilChanged()

    private val mutableState = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = mutableState.asStateFlow()

    init {
        applicationScope.launch {
            gateway.purchaseUpdates.collect { update ->
                when (update) {
                    is PurchaseUpdate.Purchased -> process(update.purchases, authoritative = false)
                    PurchaseUpdate.AlreadyOwned -> refresh()
                }
            }
        }
    }

    /** Restores purchases (reinstall, other device), retries acknowledgement, applies refunds, loads the price. */
    suspend fun refresh() {
        gateway.queryPurchases()?.let { process(it, authoritative = true) }
        gateway.queryProduct(config.removeAdsProductId)?.let { product ->
            mutableState.update { it.copy(removeAdsPrice = product.formattedPrice) }
        }
    }

    suspend fun purchaseRemoveAds(activity: Activity): Boolean =
        gateway.launchPurchase(activity, config.removeAdsProductId)

    private suspend fun process(purchases: List<StorePurchase>, authoritative: Boolean) {
        val owned = purchases.filter { config.removeAdsProductId in it.productIds }
        val completed = owned.filter { it.isPurchased }
        // Unacknowledged purchases are refunded by Play after 3 days; a failed attempt is retried on next refresh.
        completed.filterNot { it.isAcknowledged }.forEach { gateway.acknowledge(it.purchaseToken) }
        mutableState.update { it.copy(purchasePending = owned.any { purchase -> purchase.isPending }) }
        when {
            completed.isNotEmpty() -> appDataStore.setAdFree(true)
            authoritative -> appDataStore.setAdFree(false)
        }
    }
}
