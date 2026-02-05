package com.wiotracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.database.entity.WifiScanRecord
import com.wiotracker.data.repository.WifiScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LogUiState(
    val records: List<WifiScanRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class LogViewModel(
    private val repository: WifiScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(records = records)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            // The flow will automatically update when database changes
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
