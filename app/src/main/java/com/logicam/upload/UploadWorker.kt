package com.logicam.upload

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.logicam.util.AppConfig
import com.logicam.util.SecureLogger
import com.logicam.util.StorageUtil
import kotlinx.coroutines.delay
import java.io.File

/**
 * Background worker for uploading recordings with retry logic
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
            val result = uploadFile(file)
            if (result.isFailure) {
                failureCount++
                SecureLogger.w("UploadWorker", "Failed to upload ${file.name}")
            } else {
                // Move to uploaded directory or delete
                file.delete()
                val metadataFile = StorageUtil.getMetadataFile(file)
                metadataFile.delete()
                SecureLogger.i("UploadWorker", "Successfully uploaded ${file.name}")
            }
        }
        
        return when {
            failureCount == 0 -> Result.success()
            failureCount < files.size -> Result.success() // Partial success
            runAttemptCount < MAX_RETRY_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }
    
    private suspend fun uploadFile(file: File): Result<Unit> {
        return try {
            // Simulate upload with delay
            // In production, implement actual upload to your backend
            SecureLogger.i("UploadWorker", "Uploading ${file.name} (${file.length()} bytes)")
            delay(1000) // Simulate network operation
            
            // TODO: Implement actual upload logic here
            // Example: use Retrofit, OkHttp, or other HTTP client
            // val response = uploadApi.upload(file)
            
            SecureLogger.i("UploadWorker", "Upload completed: ${file.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            SecureLogger.e("UploadWorker", "Upload failed for ${file.name}", e)
            Result.failure(e)
        }
    }
}
