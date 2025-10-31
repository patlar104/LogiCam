package com.logicam

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.logicam.capture.CameraXCaptureManager
import com.logicam.capture.PhotoCaptureManager
import com.logicam.capture.RecordingManager
import com.logicam.upload.UploadManager
import kotlinx.coroutines.CoroutineScope

/**
 * Manual dependency injection container
 * Provides centralized dependency creation for testing and production
 */
class AppContainer(private val applicationContext: Context) {
    
    /**
     * Provide CameraXCaptureManager instance
     * @param lifecycle LifecycleOwner for camera binding
     */
    fun provideCameraManager(lifecycle: LifecycleOwner): CameraXCaptureManager {
        return CameraXCaptureManager(applicationContext, lifecycle)
    }
    
    /**
     * Provide RecordingManager instance
     * @param cameraManager CameraXCaptureManager dependency
     */
    fun provideRecordingManager(cameraManager: CameraXCaptureManager): RecordingManager {
        return RecordingManager(applicationContext, cameraManager)
    }
    
    /**
     * Provide PhotoCaptureManager instance
     * @param cameraManager CameraXCaptureManager dependency
     */
    fun providePhotoCaptureManager(cameraManager: CameraXCaptureManager): PhotoCaptureManager {
        return PhotoCaptureManager(applicationContext, cameraManager)
    }
    
    /**
     * Provide UploadManager instance
     * @param scope CoroutineScope for upload operations
     */
    fun provideUploadManager(scope: CoroutineScope): UploadManager {
        return UploadManager(applicationContext, scope)
    }
}
