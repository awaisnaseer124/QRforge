package com.qrforge.app.feature.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrforge.app.core.billing.BillingManager
import com.qrforge.app.core.billing.ProductIds
import com.qrforge.app.core.billing.ProductInfo
import com.qrforge.app.core.billing.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProUiState(
    val isPro: Boolean = false,
    val products: List<ProductInfo> = emptyList(),
    val selectedProductId: String = ProductIds.PRO_YEARLY,
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val activeProductId: String = ""
)

@HiltViewModel
class ProViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val uiState: StateFlow<ProUiState> = combine(
        subscriptionRepository.isPro,
        subscriptionRepository.activeProductId,
        billingManager.products,
        billingManager.isConnected
    ) { isPro, activeId, products, connected ->
        ProUiState(
            isPro            = isPro,
            products         = products,
            selectedProductId = ProductIds.PRO_YEARLY,
            isConnected      = connected,
            isLoading        = products.isEmpty() && connected,
            activeProductId  = activeId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProUiState()
    )

    init {
        billingManager.connect()
    }

    fun selectProduct(productId: String) {
        // handled in UI for simplicity
    }

    fun purchase(activity: Activity, productInfo: ProductInfo) {
        billingManager.launchBillingFlow(activity, productInfo)
    }

    fun restorePurchases() {
        billingManager.queryExistingPurchases()
    }

    // Convenience getters for UI
    fun getMonthlyProduct() = uiState.value.products
        .firstOrNull { it.billingPeriod == "monthly" }

    fun getYearlyProduct() = uiState.value.products
        .firstOrNull { it.billingPeriod == "yearly" }

    fun getLifetimeProduct() = uiState.value.products
        .firstOrNull { it.billingPeriod == "lifetime" }
}