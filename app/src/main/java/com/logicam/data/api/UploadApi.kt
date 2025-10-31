package com.logicam.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Video metadata for upload
 */
data class VideoMetadata(
    val duration: Long,
    val timestamp: Long,
    val deviceId: String,
    val resolution: String
)

/**
 * Upload response from server
 */
data class UploadResponse(
    val id: String,
    val url: String,
    val status: String = "success"
)

/**
 * API interface for video upload operations
 */
interface UploadApi {
    suspend fun uploadVideo(
        file: File,
        metadata: VideoMetadata
    ): Result<UploadResponse>
}

/**
 * OkHttp implementation of UploadApi
 * Uses multipart upload for video files with metadata
 */
class OkHttpUploadApi(
    private val baseUrl: String,
    private val apiKey: String
) : UploadApi {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "LogiCam-Android/1.0")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .build()
    
    override suspend fun uploadVideo(
        file: File,
        metadata: VideoMetadata
    ): Result<UploadResponse> {
        return try {
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("File does not exist: ${file.path}"))
            }
            
            // Build multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "video",
                    file.name,
                    file.asRequestBody()
                )
                .addFormDataPart("duration", metadata.duration.toString())
                .addFormDataPart("timestamp", metadata.timestamp.toString())
                .addFormDataPart("device_id", metadata.deviceId)
                .addFormDataPart("resolution", metadata.resolution)
                .build()
            
            // Build request
            val request = Request.Builder()
                .url("$baseUrl/api/v1/videos/upload")
                .post(requestBody)
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                // Parse response (simplified - in production use Gson/Jackson)
                val uploadResponse = UploadResponse(
                    id = file.nameWithoutExtension,
                    url = "$baseUrl/videos/${file.name}",
                    status = "success"
                )
                Result.success(uploadResponse)
            } else {
                Result.failure(
                    Exception("Upload failed with code ${response.code}: ${response.message}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Mock implementation for development/testing
 * Returns success without actual upload
 */
class MockUploadApi : UploadApi {
    override suspend fun uploadVideo(
        file: File,
        metadata: VideoMetadata
    ): Result<UploadResponse> {
        // Simulate network delay
        kotlinx.coroutines.delay(500)
        
        return Result.success(
            UploadResponse(
                id = "mock-${file.nameWithoutExtension}",
                url = "mock://videos/${file.name}",
                status = "success"
            )
        )
    }
}
