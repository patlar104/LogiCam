package com.logicam.upload

import android.content.Context
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.logicam.LogiCamApplication
import com.logicam.data.api.VideoMetadata
import com.logicam.util.AppConfig
import com.logicam.util.SecureLogger
import com.logicam.util.StorageUtil
import java.io.File

/**
 * Background worker for uploading recordings with retry logic
 * Now uses real upload API implementation
 */
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val WORK_NAME = "upload_recordings"
        private const val MAX_RETRY_ATTEMPTS = 3
        
        fun scheduleUpload(context: Context) {
            // Check if auto-upload is enabled
            if (!AppConfig.isAutoUploadEnabled(context)) {
                SecureLogger.i("UploadWorker", "Auto-upload is disabled")
                return
            }
            
            // Respect WiFi-only setting
            val networkType = if (AppConfig.isUploadOnlyWifi(context)) {
                NetworkType.UNMETERED  // WiFi only
            } else {
                NetworkType.CONNECTED  // Any network
            }
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()
            
            val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, uploadRequest)
            
            SecureLogger.i("UploadWorker", "Upload work scheduled")
        }
    }
    
    private val uploadApi by lazy {
        (applicationContext as LogiCamApplication).container.provideUploadApi()
    }
    
    override suspend fun doWork(): Result {
        SecureLogger.i("UploadWorker", "Starting upload work")
        
        val pendingDir = StorageUtil.getPendingUploadsDirectory(applicationContext)
        val files = pendingDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
        
        if (files.isEmpty()) {
            SecureLogger.i("UploadWorker", "No files to upload")
            return Result.success()
        }
        
        var failureCount = 0
        
        for (file in files) {
            val success = uploadFile(file)
            if (!success) {
                failureCount++
                SecureLogger.w("UploadWorker", "Failed to upload ${file.name}")
            } else {
                // Only delete after successful upload
                file.delete()
                val metadataFile = StorageUtil.getMetadataFile(file)
                metadataFile.delete()
                SecureLogger.i("UploadWorker", "Successfully uploaded and deleted ${file.name}")
            }
        }
        
        return when {
            failureCount == 0 -> Result.success()
            failureCount < files.size -> Result.success() // Partial success
            runAttemptCount < MAX_RETRY_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }
    
    private suspend fun uploadFile(file: File): Boolean {
        return try {
            SecureLogger.i("UploadWorker", "Uploading ${file.name} (${file.length()} bytes)")
            
            // Get device ID for tracking
            val deviceId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            // Get video quality from app config and convert to string
            val quality = AppConfig.getVideoQuality(applicationContext)
            val resolution = when (quality) {
                androidx.camera.video.Quality.UHD -> "UHD"
                androidx.camera.video.Quality.FHD -> "FHD"
                androidx.camera.video.Quality.HD -> "HD"
                androidx.camera.video.Quality.SD -> "SD"
                else -> "FHD"
            }
            
            // Create metadata
            val metadata = VideoMetadata(
                duration = 0L, // TODO: Extract from video file
                timestamp = file.lastModified(),
                deviceId = deviceId,
                resolution = resolution
            )
            
            // Perform upload using API
            val result = uploadApi.uploadVideo(file, metadata)
            
            if (result.isSuccess) {
                val response = result.getOrNull()
                SecureLogger.i("UploadWorker", "Upload completed: ${file.name} -> ${response?.url}")
                true
            } else {
                val error = result.exceptionOrNull()
                SecureLogger.e("UploadWorker", "Upload failed for ${file.name}", error)
                false
            }
        } catch (e: Exception) {
            SecureLogger.e("UploadWorker", "Upload failed for ${file.name}", e)
            false
        }
    }
}
