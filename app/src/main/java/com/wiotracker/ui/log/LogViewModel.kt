package com.wiotracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.database.entity.WifiScanRecord
import com.wiotracker.data.repository.WifiScanRepository
import com.wiotracker.util.DebugLogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class LogTab {
    SCAN_RECORDS,
    DEBUG_LOG
}

data class LogUiState(
    val scanSessions: List<ScanSession> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedSession: ScanSession? = null,
    val currentTab: LogTab = LogTab.SCAN_RECORDS
)

class LogViewModel(
    private val repository: WifiScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()
    
    val debugLogs = DebugLogManager.logs

    init {
        loadScanSessions()
    }

    private fun loadScanSessions() {
        viewModelScope.launch {
            // Get all records and group them by scanSessionId
            repository.getAllRecords().collect { records ->
                // Group records by scanSessionId
                val sessionsMap = records.groupBy { it.scanSessionId }
                val sessions = sessionsMap.values.mapNotNull { recordsList ->
                    ScanSession.fromRecords(recordsList)
                }
                // Sort by timestamp descending (newest first)
                val sortedSessions = sessions.sortedByDescending { it.timestamp }
                _uiState.value = _uiState.value.copy(scanSessions = sortedSessions)
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

    fun selectSession(session: ScanSession) {
        _uiState.value = _uiState.value.copy(selectedSession = session)
    }

    fun clearSelectedSession() {
        _uiState.value = _uiState.value.copy(selectedSession = null)
    }
    
    fun setCurrentTab(tab: LogTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }
    
    fun clearDebugLogs() {
        DebugLogManager.clear()
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
