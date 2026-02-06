package com.wiotracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.database.entity.WifiScanRecord
import com.wiotracker.data.preferences.AppPreferences
import com.wiotracker.data.repository.WifiScanRepository
import com.wiotracker.util.WorkManagerHelper
import com.wiotracker.util.WifiScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val targetWifiName: String = "",
    val startHour: Int = 8,
    val endHour: Int = 20,
    val scanIntervalMinutes: Int = 15,
    val targetTimes: Int = 1,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val errorMessage: String? = null,
    val wifiNameError: String? = null,
    val timeRangeError: String? = null,
    val scanIntervalError: String? = null,
    val targetTimesError: String? = null
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
            endHour = preferences.scanEndHour,
            scanIntervalMinutes = preferences.scanIntervalMinutes,
            targetTimes = preferences.targetTimes
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

    fun updateScanIntervalMinutes(minutes: Int) {
        val error = validateScanInterval(minutes)
        _uiState.value = _uiState.value.copy(
            scanIntervalMinutes = minutes,
            scanIntervalError = error
        )
    }

    fun updateTargetTimes(times: Int) {
        val error = validateTargetTimes(times)
        _uiState.value = _uiState.value.copy(
            targetTimes = times,
            targetTimesError = error
        )
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

        // Validate scan interval
        val scanIntervalError = validateScanInterval(state.scanIntervalMinutes)
        if (scanIntervalError != null) {
            isValid = false
        }

        // Validate target times
        val targetTimesError = validateTargetTimes(state.targetTimes)
        if (targetTimesError != null) {
            isValid = false
        }

        _uiState.value = state.copy(
            wifiNameError = wifiNameError,
            timeRangeError = timeRangeError,
            scanIntervalError = scanIntervalError,
            targetTimesError = targetTimesError,
            errorMessage = null
        )

        return isValid
    }

    private fun validateScanInterval(minutes: Int): String? {
        return when {
            minutes < 15 -> "扫描间隔至少为15分钟（WorkManager限制）"
            minutes % 5 != 0 -> "扫描间隔必须是5分钟的倍数"
            else -> null
        }
    }

    private fun validateTargetTimes(times: Int): String? {
        return if (times < 1) {
            "目标次数必须大于0"
        } else {
            null
        }
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
            timeRangeError = null,
            scanIntervalError = null,
            targetTimesError = null
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
                preferences.scanIntervalMinutes = _uiState.value.scanIntervalMinutes
                preferences.targetTimes = _uiState.value.targetTimes

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
            timeRangeError = null,
            scanIntervalError = null,
            targetTimesError = null
        )
    }

    fun testWifiName() {
        val wifiName = _uiState.value.targetWifiName.trim()
        
        if (wifiName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                testResult = "请输入WiFi名称",
                isTesting = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTesting = true,
                testResult = null
            )

            try {
                val wifiScanner = WifiScanner(context)
                
                // Check if WiFi is enabled
                if (!wifiScanner.isWifiEnabled()) {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        testResult = "WiFi未启用，请先开启WiFi"
                    )
                    return@launch
                }

                // Perform scan
                val matchedWifis = withContext(Dispatchers.IO) {
                    wifiScanner.scanAndMatch(wifiName)
                }

                // Save test results to database if any WiFi found
                if (matchedWifis.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val database = AppDatabase.getDatabase(context)
                        val repository = WifiScanRepository(database.wifiScanDao())
                        val timestamp = System.currentTimeMillis()
                        // Use timestamp as scanSessionId to group records from the same test scan
                        val scanSessionId = timestamp

                        matchedWifis.forEach { matchedWifiName ->
                            repository.insertRecord(
                                WifiScanRecord(
                                    timestamp = timestamp,
                                    wifiName = matchedWifiName,
                                    matchedKeyword = wifiName,
                                    scanSessionId = scanSessionId
                                )
                            )
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = if (matchedWifis.isNotEmpty()) {
                        "找到 ${matchedWifis.size} 个匹配的WiFi:\n${matchedWifis.joinToString("\n")}\n\n结果已保存到日志"
                    } else {
                        "未找到匹配的WiFi网络"
                    }
                )
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "权限不足，请确保已授予位置权限"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "测试失败: ${e.message}"
                )
            }
        }
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }
}
