package com.logicam.upload

import android.content.Context
import com.logicam.util.SecureLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manages background uploads with retry logic
 */
class UploadManager(private val context: Context) {
    
    private val uploadScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState
    
    sealed class UploadState {
        object Idle : UploadState()
        data class Uploading(val filename: String, val progress: Int) : UploadState()
        data class Completed(val filename: String) : UploadState()
        data class Failed(val filename: String, val error: String) : UploadState()
    }
    
    fun scheduleUpload(file: File) {
        uploadScope.launch {
            try {
                _uploadState.value = UploadState.Uploading(file.name, 0)
                SecureLogger.i("UploadManager", "Scheduling upload for ${file.name}")
                
                // Schedule background work
                UploadWorker.scheduleUpload(context)
                
                _uploadState.value = UploadState.Completed(file.name)
            } catch (e: Exception) {
                _uploadState.value = UploadState.Failed(file.name, e.message ?: "Unknown error")
                SecureLogger.e("UploadManager", "Failed to schedule upload", e)
            }
        }
    }
    
    fun scheduleAllPendingUploads() {
        uploadScope.launch {
            SecureLogger.i("UploadManager", "Scheduling all pending uploads")
            UploadWorker.scheduleUpload(context)
        }
    }
}
