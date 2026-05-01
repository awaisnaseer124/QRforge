package com.qrforge.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrforge.app.core.qr.QRType
import com.qrforge.app.data.QRRepository
import com.qrforge.app.data.db.QREntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// null = still loading, empty list = loaded but no items
data class HistoryUiState(
    val items: List<QREntity>? = null,   // null means loading
    val searchQuery: String = "",
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val totalCount: Int = 0,
    val deletedItem: QREntity? = null
) {
    val isLoading get() = items == null
    val isEmpty   get() = items != null && items.isEmpty() && totalCount == 0
}

enum class HistoryFilter(val label: String) {
    ALL("All"),
    GENERATED("Generated"),
    SCANNED("Scanned"),
    URL("URL"),
    WIFI("Wi-Fi"),
    VCARD("Contact"),
    TEXT("Text")
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: QRRepository
) : ViewModel() {

    private val _searchQuery    = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    private val _deletedItem    = MutableStateFlow<QREntity?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        // Debounced search → switches between search and all-QRs flow
        _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) repository.getAllQRs()
                else repository.searchQRs(query)
            },
        _selectedFilter,
        repository.getTotalCount(),
        _deletedItem
    ) { items, filter, total, deleted ->
        val filtered = when (filter) {
            HistoryFilter.ALL       -> items
            HistoryFilter.GENERATED -> items.filter { !it.isScanned }
            HistoryFilter.SCANNED   -> items.filter { it.isScanned }
            HistoryFilter.URL       -> items.filter { it.type == QRType.URL }
            HistoryFilter.WIFI      -> items.filter { it.type == QRType.WIFI }
            HistoryFilter.VCARD     -> items.filter { it.type == QRType.VCARD }
            HistoryFilter.TEXT      -> items.filter { it.type == QRType.TEXT }
        }
        HistoryUiState(
            items         = filtered,   // non-null = Room has responded
            searchQuery   = _searchQuery.value,
            selectedFilter = filter,
            totalCount    = total,
            deletedItem   = deleted
        )
    }.stateIn(
        scope = viewModelScope,
        // Eagerly keeps collecting even when screen is not visible
        // so data is always fresh when you navigate back
        started = SharingStarted.Eagerly,
        initialValue = HistoryUiState()   // items = null → shows loader
    )

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onFilterSelected(filter: HistoryFilter) { _selectedFilter.value = filter }

    fun deleteQR(qr: QREntity) {
        viewModelScope.launch {
            repository.deleteQR(qr)
            _deletedItem.value = qr
        }
    }

    fun undoDelete() {
        val item = _deletedItem.value ?: return
        viewModelScope.launch {
            repository.saveGeneratedQR(item.type, item.content, item.colorHex)
            _deletedItem.value = null
        }
    }

    fun dismissUndo() { _deletedItem.value = null }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}