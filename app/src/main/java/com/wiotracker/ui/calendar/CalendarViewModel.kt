package com.wiotracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.repository.WifiScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CalendarUiState(
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val dailyStats: Map<String, Pair<Int, Int>> = emptyMap(), // Pair(periodicCount, totalCount)
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val repository: WifiScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.getDailyStatsFlow(state.currentMonth, state.currentYear).collect { stats ->
                _uiState.value = state.copy(dailyStats = stats)
            }
        }
    }

    fun changeMonth(month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentMonth = month,
                currentYear = year
            )
            loadStats()
        }
    }

    fun getMatchCountForDate(date: String): Pair<Int, Int> {
        return _uiState.value.dailyStats[date] ?: Pair(0, 0)
    }

    fun isDateSuccess(date: String): Boolean {
        val (periodicCount, _) = getMatchCountForDate(date)
        return periodicCount >= 3
    }

    fun formatDate(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}
