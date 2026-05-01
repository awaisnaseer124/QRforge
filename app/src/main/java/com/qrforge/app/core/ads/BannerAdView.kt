package com.qrforge.app.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Standard banner height is 50dp (AdSize.BANNER)
// Adaptive banner height varies — we use 60dp as a safe container

@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.loadAd(AdRequest.Builder().build())
        }
    )
}

// Convenience composables for each placement
@Composable
fun HomeBannerAd(modifier: Modifier = Modifier) {
    BannerAd(
        adUnitId = AdUnitIds.BANNER_HOME,
        modifier = modifier
    )
}

@Composable
fun HistoryBannerAd(modifier: Modifier = Modifier) {
    BannerAd(
        adUnitId = AdUnitIds.BANNER_HISTORY,
        modifier = modifier
    )
}