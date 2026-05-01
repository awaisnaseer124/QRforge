package com.qrforge.app.feature.generate

import android.app.Activity
import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrforge.app.core.ads.AdManager
import com.qrforge.app.core.export.ExportManager
import com.qrforge.app.core.qr.QRField
import com.qrforge.app.core.qr.QRGenerator
import com.qrforge.app.core.qr.QRType
import com.qrforge.app.core.qr.buildQRContent
import com.qrforge.app.core.qr.getFieldsForType
import com.qrforge.app.data.QRRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val QR_COLOR_PALETTE = listOf(
    "#000000", "#00C07A", "#6C63FF", "#FF5C7A",
    "#FFAA40", "#4CA8FF", "#FF6B35", "#8B5CF6"
)

data class GenerateUiState(
    val selectedType: QRType = QRType.URL,
    val fields: List<QRField> = getFieldsForType(QRType.URL),
    val fieldValues: Map<String, String> = emptyMap(),
    val selectedColorHex: String = "#000000",
    val qrBitmap: android.graphics.Bitmap? = null,
    val generatedContent: String = "",
    val isGenerating: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val dailyQRCount: Int = 0,
    val dailyLimit: Int = 5,
    val bonusQRs: Int = 0,              // extra QRs earned via rewarded ads
    val showRewardedAdOffer: Boolean = false  // show "watch ad for +2 QRs" prompt
)

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val repository: QRRepository,
    private val adManager: AdManager,
    private val subscriptionRepository: com.qrforge.app.core.billing.SubscriptionRepository
) : ViewModel() {

    val isPro = subscriptionRepository.isPro

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()
    private var generateJob: Job? = null

    // Track how many QRs saved this session for interstitial trigger
    private var sessionSaveCount = 0

    init {
        viewModelScope.launch {
            val count = repository.getTodayGeneratedCount()
            _uiState.update { it.copy(dailyQRCount = count) }
        }
    }

    fun onTypeSelected(type: QRType) {
        _uiState.update {
            it.copy(
                selectedType = type,
                fields = getFieldsForType(type),
                fieldValues = emptyMap(),
                qrBitmap = null,
                generatedContent = ""
            )
        }
    }

    fun onFieldChanged(key: String, value: String) {
        _uiState.update { state ->
            state.copy(fieldValues = state.fieldValues + (key to value))
        }
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            delay(400)
            generateQR()
        }
    }

    fun onColorSelected(hex: String) {
        _uiState.update { it.copy(selectedColorHex = hex) }
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            delay(100)
            generateQR()
        }
    }

    fun generateQR() {
        val state = _uiState.value
        val content = buildQRContent(state.selectedType, state.fieldValues)
        if (content.isBlank()) return
        _uiState.update { it.copy(isGenerating = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val darkColor = try { Color.parseColor(state.selectedColorHex) }
            catch (e: Exception) { Color.BLACK }
            val bitmap = QRGenerator.generate(
                content = content, size = 1024,
                darkColor = darkColor, lightColor = Color.WHITE, cornerRadius = 8f
            )
            _uiState.update {
                it.copy(qrBitmap = bitmap, isGenerating = false, generatedContent = content)
            }
        }
    }

    fun saveToGallery(context: Context) {
        val state = _uiState.value
        val bitmap = state.qrBitmap ?: return
        val effectiveLimit = state.dailyLimit + state.bonusQRs

        // Check free tier limit
        if (state.dailyQRCount >= effectiveLimit) {
            // Offer rewarded ad if available, otherwise show upgrade prompt
            if (adManager.isRewardedReady.value) {
                _uiState.update { it.copy(showRewardedAdOffer = true) }
            } else {
                _uiState.update { it.copy(saveError = "Daily limit reached! Upgrade to Pro for unlimited QRs.") }
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Pro users — unlimited, no ads, no limit check
            val userIsPro = subscriptionRepository.isPro.first()
            val effectiveLimit = state.dailyLimit + state.bonusQRs

            if (!userIsPro && state.dailyQRCount >= effectiveLimit) {
                if (adManager.isRewardedReady.value) {
                    _uiState.update { it.copy(showRewardedAdOffer = true) }
                } else {
                    _uiState.update { it.copy(saveError = "Daily limit reached! Upgrade to Pro for unlimited QRs.") }
                }
                return@launch
            }

            val uri = ExportManager.savePngToGallery(context, bitmap, "QRForge_${state.selectedType.label}")
            if (uri != null) {
                repository.saveGeneratedQR(state.selectedType, state.generatedContent, state.selectedColorHex)
                val newCount = repository.getTodayGeneratedCount()
                sessionSaveCount++
                _uiState.update { it.copy(saveSuccess = true, dailyQRCount = newCount) }

                // Show interstitial every 3rd save — only for free users
                if (!userIsPro && sessionSaveCount % 3 == 0 && adManager.isInterstitialReady.value) {
                    (context as? Activity)?.let { activity ->
                        adManager.showInterstitial(activity)
                    }
                }
            } else {
                _uiState.update { it.copy(saveError = "Failed to save. Check storage permission.") }
            }
        }
    }

    // Called when user taps "Watch Ad" on the rewarded offer
    fun watchRewardedAd(activity: Activity) {
        _uiState.update { it.copy(showRewardedAdOffer = false) }
        adManager.showRewarded(
            activity = activity,
            onRewarded = { amount ->
                _uiState.update { it.copy(bonusQRs = it.bonusQRs + amount) }
            }
        )
    }

    fun dismissRewardedOffer() {
        _uiState.update { it.copy(showRewardedAdOffer = false) }
    }

    fun shareQR(context: Context) {
        val bitmap = _uiState.value.qrBitmap ?: return
        ExportManager.shareQR(context, bitmap, _uiState.value.selectedType.label)
    }

    fun dismissSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
    fun dismissSaveError()   = _uiState.update { it.copy(saveError = null) }
}