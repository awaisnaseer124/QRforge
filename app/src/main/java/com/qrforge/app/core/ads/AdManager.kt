package com.qrforge.app.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────
// IMPORTANT: Replace test IDs with your real AdMob IDs before release
// Get real IDs from: https://admob.google.com
// ─────────────────────────────────────────────────────────────────────
object AdUnitIds {
    // Test IDs — safe to use during development
    const val BANNER_HOME        = "ca-app-pub-3940256099942544/6300978111"
    const val BANNER_HISTORY     = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL       = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED           = "ca-app-pub-3940256099942544/5224354917"
}

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "AdManager"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    fun initialize() {
        MobileAds.initialize(context) {
            Log.d(tag, "AdMob initialized")
            loadInterstitial()
            loadRewarded()
        }
    }

    // ── Interstitial ─────────────────────────────────────────────────

    fun loadInterstitial() {
        InterstitialAd.load(
            context,
            AdUnitIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isInterstitialReady.value = true
                    Log.d(tag, "Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    Log.e(tag, "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isInterstitialReady.value = false
                onDismissed()
                loadInterstitial() // pre-load next one immediately
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                _isInterstitialReady.value = false
                onDismissed()
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    // ── Rewarded ─────────────────────────────────────────────────────

    fun loadRewarded() {
        RewardedAd.load(
            context,
            AdUnitIds.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isRewardedReady.value = true
                    Log.d(tag, "Rewarded loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isRewardedReady.value = false
                    Log.e(tag, "Rewarded failed: ${error.message}")
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewarded: (Int) -> Unit,   // passes reward amount
        onDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            loadRewarded()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedReady.value = false
                onDismissed()
                loadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _isRewardedReady.value = false
                onDismissed()
                loadRewarded()
            }
        }
        ad.show(activity) { reward ->
            onRewarded(reward.amount)
        }
    }
}