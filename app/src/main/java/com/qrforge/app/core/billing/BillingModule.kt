package com.qrforge.app.core.billing

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @ApplicationContext context: Context
    ): SubscriptionRepository = SubscriptionRepository(context)

    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        subscriptionRepository: SubscriptionRepository
    ): BillingManager = BillingManager(context, subscriptionRepository)
}