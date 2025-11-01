package com.logicam

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.logicam.capture.CameraXCaptureManager
import com.logicam.capture.PhotoCaptureManager
import com.logicam.capture.RecordingManager
import com.logicam.data.api.MockUploadApi
import com.logicam.data.api.OkHttpUploadApi
import com.logicam.data.api.UploadApi
import com.logicam.upload.UploadManager
import kotlinx.coroutines.CoroutineScope

/**
 * Manual dependency injection container
 * Provides centralized dependency creation for testing and production
 * Open for testing purposes to allow mocking
 */
open class AppContainer(private val applicationContext: Context) {
    
    /**
     * Provide CameraXCaptureManager instance
     * @param lifecycle LifecycleOwner for camera binding
     */
    open fun provideCameraManager(lifecycle: LifecycleOwner): CameraXCaptureManager {
        return CameraXCaptureManager(applicationContext, lifecycle)
    }
    
    /**
     * Provide RecordingManager instance
     * @param cameraManager CameraXCaptureManager dependency
     */
    open fun provideRecordingManager(cameraManager: CameraXCaptureManager): RecordingManager {
        return RecordingManager(applicationContext, cameraManager)
    }
    
    /**
     * Provide PhotoCaptureManager instance
     * @param cameraManager CameraXCaptureManager dependency
     */
    open fun providePhotoCaptureManager(cameraManager: CameraXCaptureManager): PhotoCaptureManager {
        return PhotoCaptureManager(applicationContext, cameraManager)
    }
    
    /**
     * Provide UploadManager instance
     * @param scope CoroutineScope for upload operations
     */
    open fun provideUploadManager(scope: CoroutineScope): UploadManager {
        return UploadManager(applicationContext, scope)
    }
    
    /**
     * Provide UploadApi instance
     * Uses MockUploadApi by default for development
     * Override in production to use OkHttpUploadApi with real backend URL
     */
    open fun provideUploadApi(): UploadApi {
        // TODO: Replace with OkHttpUploadApi when backend is ready
        // val baseUrl = "https://your-api-server.com"
        // val apiKey = "your-api-key-here"
        // return OkHttpUploadApi(baseUrl, apiKey)
        
        return MockUploadApi()
    }
}
