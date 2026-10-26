package com.viettel.appbase.core.monetization.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayBillingGateway(context: Context) :
    BillingGateway,
    PurchasesUpdatedListener {
    private val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = UPDATE_BUFFER)
    override val purchaseUpdates: Flow<PurchaseUpdate> = updates

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()
    private val connectMutex = Mutex()
    private val productCache = ConcurrentHashMap<String, ProductDetails>()

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> updates.tryEmit(
                PurchaseUpdate.Purchased(
                    purchases.orEmpty().map {
                        it.toStore()
                    },
                ),
            )

            BillingResponseCode.ITEM_ALREADY_OWNED -> updates.tryEmit(PurchaseUpdate.AlreadyOwned)

            BillingResponseCode.USER_CANCELED -> Unit

            else -> Timber.d("Purchase failed (%d): %s", result.responseCode, result.debugMessage)
        }
    }

    override suspend fun queryPurchases(): List<StorePurchase>? {
        if (!ensureConnected()) return null
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(params) { result, purchases ->
                val owned = if (result.responseCode == BillingResponseCode.OK) purchases.map { it.toStore() } else null
                continuation.resume(owned)
            }
        }
    }

    override suspend fun queryProduct(productId: String): StoreProduct? {
        val details = productDetails(productId) ?: return null
        val price = details.oneTimeOffer()?.formattedPrice ?: return null
        return StoreProduct(productId, price)
    }

    override suspend fun acknowledge(purchaseToken: String): Boolean {
        if (!ensureConnected()) return false
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        return suspendCancellableCoroutine { continuation ->
            client.acknowledgePurchase(params) { result ->
                continuation.resume(result.responseCode == BillingResponseCode.OK)
            }
        }
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): Boolean {
        val details = productDetails(productId) ?: return false
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { details.oneTimeOffer()?.offerToken?.let(::setOfferToken) }
            .build()
        val flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
        val result = withContext(Dispatchers.Main) { client.launchBillingFlow(activity, flowParams) }
        return result.responseCode == BillingResponseCode.OK
    }

    private suspend fun productDetails(productId: String): ProductDetails? {
        productCache[productId]?.let { return it }
        if (!ensureConnected()) return null
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        val details = suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                val found = queryResult.productDetailsList.firstOrNull()
                if (found == null) Timber.d("Product %s unavailable: %s", productId, result.debugMessage)
                continuation.resume(found)
            }
        }
        details?.let { productCache[productId] = it }
        return details
    }

    private suspend fun ensureConnected(): Boolean = connectMutex.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    // Billing 8+: one-time products can have several offers; the first is the base price.
    private fun ProductDetails.oneTimeOffer(): ProductDetails.OneTimePurchaseOfferDetails? =
        oneTimePurchaseOfferDetailsList?.firstOrNull() ?: oneTimePurchaseOfferDetails

    private fun Purchase.toStore() = StorePurchase(
        productIds = products,
        purchaseToken = purchaseToken,
        isPurchased = purchaseState == Purchase.PurchaseState.PURCHASED,
        isPending = purchaseState == Purchase.PurchaseState.PENDING,
        isAcknowledged = isAcknowledged,
    )

    private companion object {
        const val UPDATE_BUFFER = 8
    }
}
