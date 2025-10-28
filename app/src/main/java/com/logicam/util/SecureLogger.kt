package com.logicam.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Secure logger that writes to file with metadata
 */
object SecureLogger {
    private const val TAG = "LogiCam"
    private const val LOG_FILE_NAME = "logicam_log.txt"
    
    private fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE_NAME)
    }
    
    fun d(tag: String, message: String) {
        Log.d(TAG, "[$tag] $message")
    }
    
    fun i(tag: String, message: String) {
        Log.i(TAG, "[$tag] $message")
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[$tag] $message", throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$tag] $message", throwable)
    }
    
    suspend fun logToFile(context: Context, tag: String, message: String) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val logEntry = "[$timestamp] [$tag] $message\n"
                getLogFile(context).appendText(logEntry)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }
    
    suspend fun getRecentLogs(context: Context, lines: Int = 100): String {
        return withContext(Dispatchers.IO) {
            try {
                val logFile = getLogFile(context)
                if (!logFile.exists()) return@withContext ""
                
                logFile.readLines().takeLast(lines).joinToString("\n")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read log file", e)
                ""
            }
        }
    }
}
