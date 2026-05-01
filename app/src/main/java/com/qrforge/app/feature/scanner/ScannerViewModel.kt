package com.qrforge.app.feature.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrforge.app.core.qr.QRType
import com.qrforge.app.data.QRRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isFlashOn: Boolean = false,
    val scanResult: String? = null,
    val showResultSheet: Boolean = false,
    val isScanning: Boolean = true,
    val copiedToClipboard: Boolean = false,
    val galleryError: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: QRRepository        // ← injected
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun toggleFlash() = _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }

    fun onQRDetected(result: String) {
        if (_uiState.value.showResultSheet) return
        _uiState.update {
            it.copy(scanResult = result, showResultSheet = true,
                isScanning = false, galleryError = null)
        }
        // Auto-save to history
        val type = getResultType(result)
        viewModelScope.launch {
            repository.saveScannedQR(result, type)
        }
    }

    fun onGalleryError(message: String) = _uiState.update { it.copy(galleryError = message) }
    fun dismissGalleryError() = _uiState.update { it.copy(galleryError = null) }

    fun dismissResult() {
        _uiState.update {
            it.copy(showResultSheet = false, scanResult = null,
                isScanning = true, copiedToClipboard = false)
        }
    }

    fun copyToClipboard(context: Context) {
        val result = _uiState.value.scanResult ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", result))
        _uiState.update { it.copy(copiedToClipboard = true) }
    }

    fun openUrl(context: Context) {
        val result = _uiState.value.scanResult ?: return
        try {
            val url = if (result.startsWith("http")) result else "https://$result"
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun shareResult(context: Context) {
        val result = _uiState.value.scanResult ?: return
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, result)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }, "Share QR Content"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun getResultType(result: String): QRType = when {
        result.startsWith("http://") || result.startsWith("https://") -> QRType.URL
        result.startsWith("WIFI:")       -> QRType.WIFI
        result.startsWith("BEGIN:VCARD") -> QRType.VCARD
        result.startsWith("mailto:")     -> QRType.EMAIL
        result.startsWith("tel:")        -> QRType.PHONE
        result.startsWith("sms:")        -> QRType.SMS
        else                             -> QRType.TEXT
    }
}

enum class ScanResultType(val label: String, val emoji: String) {
    URL("Website", "🔗"), WIFI("Wi-Fi Network", "📶"), VCARD("Contact", "👤"),
    EMAIL("Email", "📧"), PHONE("Phone Number", "📞"), SMS("SMS", "💬"), TEXT("Text", "📝")
}

fun ScannerViewModel.getResultTypeDisplay(result: String): ScanResultType = when {
    result.startsWith("http://") || result.startsWith("https://") -> ScanResultType.URL
    result.startsWith("WIFI:")       -> ScanResultType.WIFI
    result.startsWith("BEGIN:VCARD") -> ScanResultType.VCARD
    result.startsWith("mailto:")     -> ScanResultType.EMAIL
    result.startsWith("tel:")        -> ScanResultType.PHONE
    result.startsWith("sms:")        -> ScanResultType.SMS
    else                             -> ScanResultType.TEXT
}