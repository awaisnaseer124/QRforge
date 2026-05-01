package com.qrforge.app.core.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "subscription_prefs")

@Singleton
class SubscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        val KEY_IS_PRO       = booleanPreferencesKey("is_pro")
        val KEY_PRODUCT_ID   = stringPreferencesKey("product_id")
        val KEY_GRANTED_AT   = longPreferencesKey("granted_at")
    }

    // Observe Pro status as a Flow — UI reacts instantly when it changes
    val isPro: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_IS_PRO] ?: false }

    val activeProductId: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_PRODUCT_ID] ?: "" }

    fun grantPro(productId: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_IS_PRO]     = true
                prefs[KEY_PRODUCT_ID] = productId
                prefs[KEY_GRANTED_AT] = System.currentTimeMillis()
            }
        }
    }

    fun revokePro() {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_IS_PRO]     = false
                prefs[KEY_PRODUCT_ID] = ""
            }
        }
    }
}