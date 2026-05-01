package com.qrforge.app.feature.generate

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrforge.app.core.ads.AdManager
import com.qrforge.app.core.billing.SubscriptionRepository
import com.qrforge.app.core.export.ExportManager
import com.qrforge.app.core.export.PdfExporter
import com.qrforge.app.core.export.SvgExporter
import com.qrforge.app.core.qr.QRField
import com.qrforge.app.core.qr.QRGenerator
import com.qrforge.app.core.qr.QRType
import com.qrforge.app.core.qr.LogoOverlayProcessor
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
    val qrBitmap: Bitmap? = null,          // base QR (no logo)
    val finalBitmap: Bitmap? = null,       // QR + logo composite (shown in preview)
    val generatedContent: String = "",
    val logoBitmap: Bitmap? = null,
    val selectedFrameId: String = "none",
    val isGenerating: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveSuccessMessage: String = "",
    val saveError: String? = null,
    val dailyQRCount: Int = 0,
    val dailyLimit: Int = 5,
    val bonusQRs: Int = 0,
    val showRewardedAdOffer: Boolean = false
)

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val repository: QRRepository,
    private val adManager: AdManager,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()
    val isPro = subscriptionRepository.isPro

    private var generateJob: Job? = null
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
                finalBitmap = null,
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

    fun onLogoSelected(bitmap: Bitmap?) {
        _uiState.update { it.copy(logoBitmap = bitmap) }
        // Recomposite with new logo
        val qr = _uiState.value.qrBitmap
        if (qr != null) applyLogoAndFrame(qr)
    }

    fun onFrameSelected(frameId: String) {
        _uiState.update { it.copy(selectedFrameId = frameId) }
        generateQR(frameId)   // pass frameId directly — no state race possible
    }

    fun generateQR(frameOverride: String? = null) {
        val state = _uiState.value
        val content = buildQRContent(state.selectedType, state.fieldValues)
        if (content.isBlank()) return

        _uiState.update { it.copy(isGenerating = true) }

        // Use passed frameId if provided, otherwise read from state
        val frameId = frameOverride ?: state.selectedFrameId

        viewModelScope.launch(Dispatchers.Default) {
            // Values are relative to 1024px bitmap
            // Cell size ≈ 1024 / ~29 modules ≈ 35px per cell
            val cornerRadius = when (frameId) {
                "rounded" -> 14f   // ~40% of cell — clearly rounded squares
                "soft"    -> 20f   // ~57% of cell — very rounded, pill-like
                "circle"  -> 500f  // Canvas clamps to half cell — perfect circles
                else      -> 0f    // sharp squares
            }
            val darkColor = try { Color.parseColor(state.selectedColorHex) }
            catch (e: Exception) { Color.BLACK }

            val qrBitmap = QRGenerator.generate(
                content = content, size = 1024,
                darkColor = darkColor, lightColor = Color.WHITE,
                cornerRadius = cornerRadius
            ) ?: return@launch

            // Apply logo if present
            val finalBitmap = if (state.logoBitmap != null) {
                LogoOverlayProcessor.overlay(qrBitmap, state.logoBitmap)
            } else {
                qrBitmap
            }

            _uiState.update {
                it.copy(
                    qrBitmap = qrBitmap,
                    finalBitmap = finalBitmap,
                    generatedContent = content,
                    isGenerating = false
                )
            }
        }
    }

    private fun applyLogoAndFrame(qrBitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val finalBitmap = if (state.logoBitmap != null) {
                LogoOverlayProcessor.overlay(qrBitmap, state.logoBitmap)
            } else {
                qrBitmap
            }
            _uiState.update { it.copy(finalBitmap = finalBitmap) }
        }
    }

    // ── Save PNG ──────────────────────────────────────────────────────

    fun saveToGallery(context: Context) {
        val state = _uiState.value
        val bitmap = state.finalBitmap ?: return

        viewModelScope.launch(Dispatchers.IO) {
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
                _uiState.update {
                    it.copy(saveSuccess = true, dailyQRCount = newCount,
                        saveSuccessMessage = "QR saved to gallery!")
                }
                if (!userIsPro && sessionSaveCount % 3 == 0 && adManager.isInterstitialReady.value) {
                    (context as? Activity)?.let { adManager.showInterstitial(it) }
                }
            } else {
                _uiState.update { it.copy(saveError = "Failed to save. Check storage permission.") }
            }
        }
    }

    // ── Export SVG (Pro) ──────────────────────────────────────────────

    fun exportSvg(context: Context) {
        val state = _uiState.value
        if (state.generatedContent.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val uri = SvgExporter.export(
                context    = context,
                content    = state.generatedContent,
                fileName   = "QRForge_${state.selectedType.label}",
                darkColor  = state.selectedColorHex
            )
            _uiState.update {
                if (uri != null)
                    it.copy(saveSuccess = true, saveSuccessMessage = "SVG saved to Downloads/QRForge!")
                else
                    it.copy(saveError = "SVG export failed.")
            }
        }
    }

    // ── Export PDF (Pro) ──────────────────────────────────────────────

    fun exportPdf(context: Context) {
        val state = _uiState.value
        val bitmap = state.finalBitmap ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val uri = PdfExporter.export(
                context   = context,
                qrBitmap  = bitmap,
                fileName  = "QRForge_${state.selectedType.label}",
                title     = state.selectedType.label,
                subtitle  = state.generatedContent
            )
            _uiState.update {
                if (uri != null)
                    it.copy(saveSuccess = true, saveSuccessMessage = "PDF saved to Downloads/QRForge!")
                else
                    it.copy(saveError = "PDF export failed.")
            }
        }
    }

    fun watchRewardedAd(activity: Activity) {
        _uiState.update { it.copy(showRewardedAdOffer = false) }
        adManager.showRewarded(activity, onRewarded = { amount ->
            _uiState.update { it.copy(bonusQRs = it.bonusQRs + amount) }
        })
    }

    fun shareQR(context: Context) {
        val bitmap = _uiState.value.finalBitmap ?: return
        ExportManager.shareQR(context, bitmap, _uiState.value.selectedType.label)
    }

    fun dismissRewardedOffer() = _uiState.update { it.copy(showRewardedAdOffer = false) }
    fun dismissSaveSuccess()   = _uiState.update { it.copy(saveSuccess = false) }
    fun dismissSaveError()     = _uiState.update { it.copy(saveError = null) }
}