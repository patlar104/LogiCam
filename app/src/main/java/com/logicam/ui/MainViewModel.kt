package com.logicam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.logicam.capture.Camera2FallbackManager
import com.logicam.capture.CameraXCaptureManager
import com.logicam.capture.PhotoCaptureManager
import com.logicam.capture.RecordingManager
import com.logicam.upload.UploadManager
import com.logicam.util.AppConfig
import com.logicam.util.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for MainActivity
 * Survives configuration changes and manages all business logic
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    
    private var cameraManager: CameraXCaptureManager? = null
    private var camera2FallbackManager: Camera2FallbackManager? = null
    private var recordingManager: RecordingManager? = null
    private var photoCaptureManager: PhotoCaptureManager? = null
    private val uploadManager = UploadManager(application, viewModelScope)
    
    private var recordingStartTime: Long = 0
    
    sealed class CameraUiState {
        object Idle : CameraUiState()
        object Initializing : CameraUiState()
        object Ready : CameraUiState()
        data class Recording(val duration: Long) : CameraUiState()
        data class RecordingCompleted(val file: File) : CameraUiState()
        data class Error(val message: String, val canRetry: Boolean = true) : CameraUiState()
        data class UsingFallback(val message: String) : CameraUiState()
    }
    
    /**
     * Initialize the camera system
     */
    fun initializeCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Initializing
            
            try {
                // Try CameraX first
                val manager = CameraXCaptureManager(getApplication(), lifecycleOwner)
                val result = manager.initialize()
                
                if (result.isSuccess) {
                    cameraManager = manager
                    recordingManager = RecordingManager(getApplication(), manager)
                    photoCaptureManager = PhotoCaptureManager(getApplication(), manager)
                    
                    _uiState.value = CameraUiState.Ready
                    SecureLogger.i("MainViewModel", "CameraX initialized successfully")
                } else {
                    // Try Camera2 fallback
                    SecureLogger.w("MainViewModel", "CameraX failed, attempting Camera2 fallback")
                    attemptCamera2Fallback()
                }
            } catch (e: Exception) {
                SecureLogger.e("MainViewModel", "Camera initialization failed", e)
                _uiState.value = CameraUiState.Error(
                    e.message ?: "Failed to initialize camera",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * Attempt to use Camera2 as fallback
     */
    private suspend fun attemptCamera2Fallback() {
        try {
            val fallbackManager = Camera2FallbackManager(getApplication())
            val result = fallbackManager.openCamera()
            
            if (result.isSuccess) {
                camera2FallbackManager = fallbackManager
                _uiState.value = CameraUiState.UsingFallback("Camera ready (compatibility mode)")
                SecureLogger.i("MainViewModel", "Camera2 fallback successful")
            } else {
                _uiState.value = CameraUiState.Error(
                    "Failed to initialize camera",
                    canRetry = true
                )
            }
        } catch (e: Exception) {
            SecureLogger.e("MainViewModel", "Camera2 fallback failed", e)
            _uiState.value = CameraUiState.Error(
                "Camera initialization failed",
                canRetry = true
            )
        }
    }
    
    /**
     * Get the camera manager for preview binding
     */
    fun getCameraManager(): CameraXCaptureManager? = cameraManager
    
    /**
     * Start video recording
     */
    fun startRecording() {
        viewModelScope.launch {
            val manager = recordingManager
            if (manager == null) {
                _uiState.value = CameraUiState.Error("Recording manager not initialized", false)
                return@launch
            }
            
            val result = manager.startRecording()
            if (result.isSuccess) {
                recordingStartTime = System.currentTimeMillis()
                _uiState.value = CameraUiState.Recording(0)
                SecureLogger.i("MainViewModel", "Recording started")
            } else {
                _uiState.value = CameraUiState.Error(
                    "Failed to start recording",
                    canRetry = false
                )
            }
        }
    }
    
    /**
     * Stop video recording
     */
    fun stopRecording() {
        viewModelScope.launch {
            val manager = recordingManager
            if (manager == null) {
                _uiState.value = CameraUiState.Error("Recording manager not initialized", false)
                return@launch
            }
            
            val result = manager.stopRecording()
            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    // Save to MediaStore for gallery visibility (Android 10+)
                    val mediaStoreUri = com.logicam.util.StorageUtil.saveVideoToMediaStore(
                        getApplication(),
                        file
                    )
                    
                    if (mediaStoreUri != null) {
                        SecureLogger.i("MainViewModel", "Video saved to MediaStore and visible in gallery")
                    }
                    
                    _uiState.value = CameraUiState.RecordingCompleted(file)
                    
                    // Schedule upload if auto-upload is enabled
                    if (AppConfig.isAutoUploadEnabled(getApplication())) {
                        uploadManager.scheduleUpload(file)
                        SecureLogger.i("MainViewModel", "Upload scheduled for ${file.name}")
                    }
                } else {
                    _uiState.value = CameraUiState.Ready
                }
                SecureLogger.i("MainViewModel", "Recording stopped")
            } else {
                _uiState.value = CameraUiState.Error(
                    "Failed to stop recording",
                    canRetry = false
                )
            }
        }
    }
    
    /**
     * Capture a photo
     */
    fun capturePhoto() {
        viewModelScope.launch {
            val manager = photoCaptureManager
            if (manager == null) {
                _uiState.value = CameraUiState.Error("Photo capture not available", false)
                return@launch
            }
            
            val result = manager.captureImage()
            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    // Save to MediaStore for gallery visibility (Android 10+)
                    val mediaStoreUri = com.logicam.util.StorageUtil.saveImageToMediaStore(
                        getApplication(),
                        file
                    )
                    
                    if (mediaStoreUri != null) {
                        SecureLogger.i("MainViewModel", "Photo saved to MediaStore and visible in gallery")
                    }
                    
                    SecureLogger.i("MainViewModel", "Photo captured: ${file.name}")
                }
            } else {
                SecureLogger.e("MainViewModel", "Photo capture failed")
            }
        }
    }
    
    /**
     * Update recording duration (call this periodically during recording)
     */
    fun updateRecordingDuration() {
        val currentState = _uiState.value
        if (currentState is CameraUiState.Recording) {
            val duration = System.currentTimeMillis() - recordingStartTime
            _uiState.value = CameraUiState.Recording(duration)
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean {
        return _uiState.value is CameraUiState.Recording
    }
    
    /**
     * Retry camera initialization
     */
    fun retryInitialization(lifecycleOwner: LifecycleOwner) {
        initializeCamera(lifecycleOwner)
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        cameraManager?.shutdown()
        camera2FallbackManager?.close()
        SecureLogger.i("MainViewModel", "ViewModel cleared")
    }
}
