package com.viettel.appbase.core.monetization.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** Store-agnostic view of a purchase, so purchase handling can be tested without Google Play. */
data class StorePurchase(
    val productIds: List<String>,
    val purchaseToken: String,
    val isPurchased: Boolean,
    val isPending: Boolean,
    val isAcknowledged: Boolean,
)

data class StoreProduct(val productId: String, val formattedPrice: String)

sealed interface PurchaseUpdate {
    data class Purchased(val purchases: List<StorePurchase>) : PurchaseUpdate

    /** The user already owns the item (e.g. bought on another device): re-query to restore it. */
    data object AlreadyOwned : PurchaseUpdate
}

/** Thin wrapper over the Play Billing Library; one-time (in-app) products only. */
interface BillingGateway {
    val purchaseUpdates: Flow<PurchaseUpdate>

    /** @return owned purchases, or null when Play cannot answer (offline, no Play Store). */
    suspend fun queryPurchases(): List<StorePurchase>?

    suspend fun queryProduct(productId: String): StoreProduct?

    suspend fun acknowledge(purchaseToken: String): Boolean

    /** @return true when the Play purchase sheet was shown. */
    suspend fun launchPurchase(activity: Activity, productId: String): Boolean
}
