package com.duke.orca.android.kotlin.biblelockscreen.store

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

// 광고 제거(한 번 구매)를 파는 Play 결제.
// 산 기록은 DataStore 에 남겨 Play 에 닿지 않아도 광고를 끈 채로 두고, 닿으면 Play 의 구매 내역으로 다시 맞춘다(환불되면 광고가 돌아온다).
object Billing : PurchasesUpdatedListener {
    const val REMOVE_ADS = "remove_ads"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var context: Context
    private lateinit var billingClient: BillingClient

    private var productDetails: ProductDetails? = null

    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    // 스토어에서 받은 가격. 받기 전이면 null.
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    fun init(context: Context) {
        this.context = context.applicationContext
        _isAdFree.value = DataStore.Ad.isAdFree(context)

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    scope.launch {
                        queryProduct()
                        restore()
                    }
                } else {
                    Timber.w("billing setup failed: $billingResult")
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private suspend fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(REMOVE_ADS)
            .setProductType(ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        productDetails = billingClient.queryProductDetails(params).productDetailsList?.firstOrNull()
        _price.value = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    // Play 의 구매 내역으로 광고 제거 여부를 맞춘다. 산 게 있으면 true, 없으면 false, Play 에 닿지 못하면 null.
    suspend fun restore(): Boolean? {
        if (::billingClient.isInitialized.not() || billingClient.isReady.not()) return null

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode != BillingResponseCode.OK) return null

        val purchases = result.purchasesList.filter { it.isRemoveAds }

        purchases.forEach { acknowledge(it) }
        setAdFree(purchases.isNotEmpty())

        return purchases.isNotEmpty()
    }

    // 결제 창을 띄운다. 상품을 아직 못 받았거나 띄우지 못하면 false.
    fun purchase(activity: Activity): Boolean {
        val details = productDetails ?: return false
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> purchases?.filter { it.isRemoveAds }?.forEach {
                setAdFree(true)
                scope.launch { acknowledge(it) }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore() }
            BillingResponseCode.USER_CANCELED -> Unit
            else -> Timber.w("purchase failed: $billingResult")
        }
    }

    // 결제 대기(PENDING)는 끝날 때 다시 onPurchasesUpdated 로 온다.
    private val Purchase.isRemoveAds: Boolean
        get() = REMOVE_ADS in products && purchaseState == Purchase.PurchaseState.PURCHASED

    // 사흘 안에 확인하지 않으면 Play 가 환불한다.
    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = billingClient.acknowledgePurchase(params)

        if (result.responseCode != BillingResponseCode.OK) {
            Timber.w("acknowledge failed: $result")
        }
    }

    private fun setAdFree(value: Boolean) {
        if (_isAdFree.value == value) return

        _isAdFree.value = value
        DataStore.Ad.putAdFree(context, value)
    }
}
