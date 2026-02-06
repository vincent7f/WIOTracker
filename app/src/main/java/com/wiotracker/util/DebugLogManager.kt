package com.wiotracker.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class DebugLogEntry(
    val timestamp: Long,
    val tag: String,
    val level: LogLevel,
    val message: String,
    val throwable: Throwable? = null
) {
    fun formatTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun formatMessage(): String {
        val timeStr = formatTime()
        val levelStr = when (level) {
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARN -> "W"
            LogLevel.ERROR -> "E"
        }
        val throwableStr = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        return "[$timeStr] $levelStr/$tag: $message$throwableStr"
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

object DebugLogManager {
    private const val MAX_LOG_ENTRIES = 1000
    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = DebugLogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            level = level,
            message = message,
            throwable = throwable
        )
        
        synchronized(_logs) {
            val currentLogs = _logs.value.toMutableList()
            currentLogs.add(entry)
            
            // Keep only the last MAX_LOG_ENTRIES entries
            if (currentLogs.size > MAX_LOG_ENTRIES) {
                currentLogs.removeAt(0)
            }
            
            _logs.value = currentLogs
        }
        
        // Also output to Android Log for compatibility
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, message, throwable)
            LogLevel.INFO -> android.util.Log.i(tag, message, throwable)
            LogLevel.WARN -> android.util.Log.w(tag, message, throwable)
            LogLevel.ERROR -> android.util.Log.e(tag, message, throwable)
        }
    }
    
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
    
    fun getLogsAsText(): String {
        return _logs.value.joinToString("\n") { it.formatMessage() }
    }
}
