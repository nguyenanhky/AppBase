package com.viettel.appbase.core.monetization.billing

import android.app.Activity
import com.viettel.appbase.core.monetization.MonetizationConfig
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.AppPreferences
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingControllerTest {
    private val gateway = FakeBillingGateway()
    private val appDataStore = mockk<AppDataStore>(relaxed = true) {
        every { preferences } returns flowOf(AppPreferences())
    }
    private val config = MonetizationConfig(
        adsEnabled = true,
        bannerAdUnitId = "banner",
        interstitialAdUnitId = "interstitial",
        removeAdsProductId = PRODUCT,
    )

    private fun TestScope.controller() = BillingController(gateway, appDataStore, config, backgroundScope)

    @Test
    fun `restored purchase grants ad-free and is acknowledged`() = runTest {
        gateway.purchases = listOf(purchase(acknowledged = false))

        controller().refresh()

        assertEquals(listOf("token"), gateway.acknowledged)
        coVerify { appDataStore.setAdFree(true) }
    }

    @Test
    fun `already acknowledged purchase is not acknowledged again`() = runTest {
        gateway.purchases = listOf(purchase(acknowledged = true))

        controller().refresh()

        assertTrue(gateway.acknowledged.isEmpty())
        coVerify { appDataStore.setAdFree(true) }
    }

    @Test
    fun `refunded purchase revokes ad-free`() = runTest {
        gateway.purchases = emptyList()

        controller().refresh()

        coVerify { appDataStore.setAdFree(false) }
    }

    @Test
    fun `offline refresh keeps the cached entitlement`() = runTest {
        gateway.purchases = null

        controller().refresh()

        coVerify(exactly = 0) { appDataStore.setAdFree(any()) }
    }

    @Test
    fun `pending purchase does not grant ad-free yet`() = runTest {
        gateway.purchases = listOf(purchase(purchased = false, pending = true))
        val controller = controller()

        controller.refresh()

        assertTrue(controller.state.value.purchasePending)
        coVerify(exactly = 0) { appDataStore.setAdFree(true) }
    }

    @Test
    fun `other products are ignored`() = runTest {
        gateway.purchases = listOf(purchase(productId = "coins"))

        controller().refresh()

        assertTrue(gateway.acknowledged.isEmpty())
        coVerify(exactly = 0) { appDataStore.setAdFree(true) }
    }

    @Test
    fun `new purchase from the Play sheet grants ad-free`() = runTest {
        controller()
        runCurrent()

        gateway.updates.emit(PurchaseUpdate.Purchased(listOf(purchase(acknowledged = false))))
        runCurrent()

        assertEquals(listOf("token"), gateway.acknowledged)
        coVerify { appDataStore.setAdFree(true) }
    }

    @Test
    fun `purchase update without the product never revokes`() = runTest {
        controller()
        runCurrent()

        gateway.updates.emit(PurchaseUpdate.Purchased(emptyList()))
        runCurrent()

        coVerify(exactly = 0) { appDataStore.setAdFree(any()) }
    }

    @Test
    fun `already owned triggers a restore`() = runTest {
        controller()
        runCurrent()
        gateway.purchases = listOf(purchase(acknowledged = true))

        gateway.updates.emit(PurchaseUpdate.AlreadyOwned)
        runCurrent()

        coVerify { appDataStore.setAdFree(true) }
    }

    @Test
    fun `price is loaded on refresh`() = runTest {
        gateway.product = StoreProduct(PRODUCT, "29.000 ₫")
        val controller = controller()

        controller.refresh()

        assertEquals("29.000 ₫", controller.state.value.removeAdsPrice)
        assertFalse(controller.state.value.purchasePending)
    }

    private fun purchase(
        productId: String = PRODUCT,
        purchased: Boolean = true,
        pending: Boolean = false,
        acknowledged: Boolean = true,
    ) = StorePurchase(listOf(productId), "token", purchased, pending, acknowledged)

    private class FakeBillingGateway : BillingGateway {
        val updates = MutableSharedFlow<PurchaseUpdate>()
        override val purchaseUpdates = updates
        var purchases: List<StorePurchase>? = emptyList()
        var product: StoreProduct? = null
        val acknowledged = mutableListOf<String>()

        override suspend fun queryPurchases() = purchases

        override suspend fun queryProduct(productId: String) = product

        override suspend fun acknowledge(purchaseToken: String): Boolean {
            acknowledged += purchaseToken
            return true
        }

        override suspend fun launchPurchase(activity: Activity, productId: String) = true
    }

    private companion object {
        const val PRODUCT = "remove_ads"
    }
}
