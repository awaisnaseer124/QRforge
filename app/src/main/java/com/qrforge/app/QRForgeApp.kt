package com.qrforge.app

import android.app.Application
import com.qrforge.app.core.ads.AdManager
import com.qrforge.app.core.billing.BillingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QRForgeApp : Application() {

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        adManager.initialize()
        billingManager.connect()
    }
}