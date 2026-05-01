package com.qrforge.app.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────
// Replace these with your real product IDs from Google Play Console
// ─────────────────────────────────────────────────────────────────────
object ProductIds {
    const val PRO_MONTHLY  = "qrforge_pro_monthly"
    const val PRO_YEARLY   = "qrforge_pro_yearly"
    const val PRO_LIFETIME = "qrforge_pro_lifetime"   // one-time purchase
}

data class ProductInfo(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceMicros: Long,
    val billingPeriod: String   // "monthly", "yearly", "lifetime"
)

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionRepository: SubscriptionRepository
) : PurchasesUpdatedListener {

    private val tag = "BillingManager"

    private val _products    = MutableStateFlow<List<ProductInfo>>(emptyList())
    val products: StateFlow<List<ProductInfo>> = _products.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    // ── Connection ───────────────────────────────────────────────────

    fun connect() {
        if (billingClient.isReady) {
            _isConnected.value = true
            queryProducts()
            queryExistingPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    Log.d(tag, "Billing connected")
                    queryProducts()
                    queryExistingPurchases()
                } else {
                    Log.e(tag, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                Log.w(tag, "Billing disconnected — will retry")
                connect() // auto-reconnect
            }
        })
    }

    // ── Query products ────────────────────────────────────────────────

    private fun queryProducts() {
        // Query subscriptions (monthly + yearly)
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProductIds.PRO_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProductIds.PRO_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(subParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val products = details.mapNotNull { it.toProductInfo() }
                _products.value = products
                Log.d(tag, "Loaded ${products.size} products")
            }
        }

        // Query one-time lifetime purchase
        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProductIds.PRO_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(inappParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val lifetime = details.mapNotNull { it.toProductInfo() }
                _products.value = _products.value + lifetime
            }
        }
    }

    // ── Launch billing flow ───────────────────────────────────────────

    fun launchBillingFlow(activity: Activity, productInfo: ProductInfo) {
        val productDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productInfo.productId)
                        .setProductType(
                            if (productInfo.billingPeriod == "lifetime")
                                BillingClient.ProductType.INAPP
                            else
                                BillingClient.ProductType.SUBS
                        )
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(productDetailsParams) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val detail = details.firstOrNull() ?: return@queryProductDetailsAsync

            val productDetailsParamsList = if (productInfo.billingPeriod == "lifetime") {
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detail)
                        .build()
                )
            } else {
                val offerToken = detail.subscriptionOfferDetails
                    ?.firstOrNull()?.offerToken ?: return@queryProductDetailsAsync
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detail)
                        .setOfferToken(offerToken)
                        .build()
                )
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            activity.runOnUiThread {
                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    // ── Purchase updates ─────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(tag, "User cancelled purchase")
            }
            else -> {
                Log.e(tag, "Purchase error: ${result.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Acknowledge the purchase (required within 3 days or Google refunds)
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "Purchase acknowledged")
                    subscriptionRepository.grantPro(purchase.products.firstOrNull() ?: "")
                }
            }
        } else {
            subscriptionRepository.grantPro(purchase.products.firstOrNull() ?: "")
        }
    }

    // ── Restore purchases ─────────────────────────────────────────────

    fun queryExistingPurchases() {
        // Check active subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val activeSub = purchases.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (activeSub != null) {
                    subscriptionRepository.grantPro(activeSub.products.firstOrNull() ?: "")
                } else {
                    // Check lifetime purchase
                    queryLifetimePurchase()
                }
            }
        }
    }

    private fun queryLifetimePurchase() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val lifetime = purchases.firstOrNull {
                    it.products.contains(ProductIds.PRO_LIFETIME) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (lifetime != null) {
                    subscriptionRepository.grantPro(ProductIds.PRO_LIFETIME)
                } else {
                    subscriptionRepository.revokePro()
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun ProductDetails.toProductInfo(): ProductInfo? {
        return when (productType) {
            BillingClient.ProductType.SUBS -> {
                val offer = subscriptionOfferDetails?.firstOrNull() ?: return null
                val pricing = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return null
                val period = when {
                    pricing.billingPeriod.contains("M") -> "monthly"
                    pricing.billingPeriod.contains("Y") -> "yearly"
                    else -> "monthly"
                }
                ProductInfo(
                    productId     = productId,
                    title         = name,
                    description   = description,
                    formattedPrice = pricing.formattedPrice,
                    priceMicros   = pricing.priceAmountMicros,
                    billingPeriod = period
                )
            }
            BillingClient.ProductType.INAPP -> {
                val price = oneTimePurchaseOfferDetails ?: return null
                ProductInfo(
                    productId     = productId,
                    title         = name,
                    description   = description,
                    formattedPrice = price.formattedPrice,
                    priceMicros   = price.priceAmountMicros,
                    billingPeriod = "lifetime"
                )
            }
            else -> null
        }
    }
}