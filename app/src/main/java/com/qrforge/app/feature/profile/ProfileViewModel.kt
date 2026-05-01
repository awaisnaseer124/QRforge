package com.qrforge.app.feature.profile

import androidx.lifecycle.ViewModel
import com.qrforge.app.core.billing.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val isPro = subscriptionRepository.isPro

    // Dev-only toggle — grants/revokes Pro locally via DataStore
    // Does NOT affect Google Play billing — safe to use during testing
    fun toggleProForTesting(enable: Boolean) {
        if (enable) {
            subscriptionRepository.grantPro("test_pro_mode")
        } else {
            subscriptionRepository.revokePro()
        }
    }
}