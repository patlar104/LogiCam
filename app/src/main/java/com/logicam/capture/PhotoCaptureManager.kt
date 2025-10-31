package com.logicam.capture

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.logicam.util.SecureLogger
import com.logicam.util.StorageUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Manages photo/image capture using CameraX ImageCapture
 */
class PhotoCaptureManager(
    private val context: Context,
    private val cameraManager: CameraXCaptureManager
) {
    
    /**
     * Capture a single photo and save to storage
     * @return Result containing the output File on success
     */
    suspend fun captureImage(): Result<File> {
        return try {
            val imageCapture = cameraManager.getImageCapture()
                ?: return Result.failure(IllegalStateException("ImageCapture not initialized"))
            
            val outputFile = File(
                StorageUtil.getImageOutputDirectory(context),
                StorageUtil.generateImageFileName()
            )
            
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
                .build()
            
            suspendCancellableCoroutine { continuation ->
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            SecureLogger.i("PhotoCapture", "Image saved: ${outputFile.name}")
                            continuation.resume(Result.success(outputFile))
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            SecureLogger.e("PhotoCapture", "Image capture failed", exception)
                            continuation.resume(Result.failure(exception))
                        }
                    }
                )
                
                continuation.invokeOnCancellation {
                    SecureLogger.w("PhotoCapture", "Image capture cancelled")
                }
            }
        } catch (e: Exception) {
            SecureLogger.e("PhotoCapture", "Failed to capture image", e)
            Result.failure(e)
        }
    }
}
