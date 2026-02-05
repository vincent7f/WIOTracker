package com.wiotracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiotracker.data.preferences.AppPreferences
import com.wiotracker.util.WorkManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val targetWifiName: String = "",
    val startHour: Int = 8,
    val endHour: Int = 20,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val wifiNameError: String? = null,
    val timeRangeError: String? = null
)

class SettingsViewModel(
    private val preferences: AppPreferences,
    private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            targetWifiName = preferences.targetWifiName,
            startHour = preferences.scanStartHour,
            endHour = preferences.scanEndHour
        )
    }

    fun updateTargetWifiName(name: String) {
        _uiState.value = _uiState.value.copy(targetWifiName = name)
    }

    fun updateStartHour(hour: Int) {
        _uiState.value = _uiState.value.copy(startHour = hour)
    }

    fun updateEndHour(hour: Int) {
        val newState = _uiState.value.copy(endHour = hour)
        _uiState.value = newState
        // Clear time range error when user changes end hour
        validateTimeRange(newState.startHour, hour)
    }

    private fun validateSettings(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate WiFi name
        val wifiNameError = if (state.targetWifiName.isBlank()) {
            isValid = false
            "WiFi名称不能为空"
        } else {
            null
        }

        // Validate time range
        val timeRangeError = if (state.startHour >= state.endHour) {
            isValid = false
            "开始时间必须小于结束时间"
        } else {
            null
        }

        _uiState.value = state.copy(
            wifiNameError = wifiNameError,
            timeRangeError = timeRangeError,
            errorMessage = null
        )

        return isValid
    }

    private fun validateTimeRange(startHour: Int, endHour: Int) {
        val timeRangeError = if (startHour >= endHour) {
            "开始时间必须小于结束时间"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(timeRangeError = timeRangeError)
    }

    fun saveSettings() {
        // Clear previous errors
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            wifiNameError = null,
            timeRangeError = null
        )

        // Validate settings
        if (!validateSettings()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请检查输入设置"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = false)

            try {
                preferences.targetWifiName = _uiState.value.targetWifiName.trim()
                preferences.scanStartHour = _uiState.value.startHour
                preferences.scanEndHour = _uiState.value.endHour

                // Restart WorkManager with new settings
                WorkManagerHelper.schedulePeriodicScan(context)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            wifiNameError = null,
            timeRangeError = null
        )
    }
}
