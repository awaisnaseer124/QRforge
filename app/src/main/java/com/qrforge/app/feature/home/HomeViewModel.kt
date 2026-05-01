package com.qrforge.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrforge.app.data.QRRepository
import com.qrforge.app.data.db.QREntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "Good morning",
    val generatedCount: Int = 0,
    val scannedCount: Int = 0,
    val todayUsed: Int = 0,
    val dailyLimit: Int = 5,
    val recentItems: List<QREntity> = emptyList(),
    val dailyTip: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QRRepository
) : ViewModel() {

    private val tips = listOf(
        "Add your logo to QR codes with Pro for better brand recognition",
        "Dynamic QRs let you change the destination without reprinting",
        "Wi-Fi QRs save guests from typing long passwords",
        "vCard QRs are the fastest way to share contact details",
        "Track scan locations and times with Pro analytics",
        "Bulk create up to 100 QRs at once with a Pro subscription",
        "SVG export gives you infinite resolution for print materials"
    )

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllQRs(),
        repository.getTotalCount()
    ) { allQRs, _ ->
        val generated = allQRs.count { !it.isScanned }
        val scanned   = allQRs.count { it.isScanned }
        val recent    = allQRs.take(3)
        val todayUsed = allQRs.count { !it.isScanned && isToday(it.createdAt) }
        val tipIndex  = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.size)

        HomeUiState(
            greeting       = getGreeting(),
            generatedCount = generated,
            scannedCount   = scanned,
            todayUsed      = todayUsed,
            dailyLimit     = 5,
            recentItems    = recent,
            dailyTip       = tips[tipIndex]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(greeting = getGreeting(), dailyTip = tips[0])
    )

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else      -> "Good night"
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return timestamp >= cal.timeInMillis
    }
}