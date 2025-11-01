package com.logicam.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for managing storage and file paths
 * Includes MediaStore integration for Android 10+
 */
object StorageUtil {
    
    fun getVideoOutputDirectory(context: Context): File {
        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let {
            File(it, "LogiCam").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }
    
    fun generateVideoFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "VID_$timestamp.mp4"
    }
    
    fun getImageOutputDirectory(context: Context): File {
        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
            File(it, "LogiCam").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }
    
    fun generateImageFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "IMG_$timestamp.jpg"
    }
    
    fun getMetadataFile(videoFile: File): File {
        return File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_metadata.json")
    }
    
    fun getPendingUploadsDirectory(context: Context): File {
        return File(context.filesDir, "pending_uploads").apply { mkdirs() }
    }
    
    /**
     * Save video to MediaStore for gallery visibility (Android 10+)
     * Videos will survive app uninstall and appear in device gallery
     * 
     * @param context Application context
     * @param videoFile The video file to copy to MediaStore
     * @return Uri of the saved video in MediaStore, or null if failed/not supported
     */
    suspend fun saveVideoToMediaStore(context: Context, videoFile: File): Uri? {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/LogiCam")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            videoFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        
                        // Mark as no longer pending
                        contentValues.clear()
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                        
                        SecureLogger.i("StorageUtil", "Video saved to MediaStore: ${videoFile.name}")
                    }
                    
                    uri
                } catch (e: Exception) {
                    SecureLogger.e("StorageUtil", "Failed to save video to MediaStore", e)
                    null
                }
            } else {
                // For older devices, fallback to current implementation
                SecureLogger.i("StorageUtil", "MediaStore not available on Android < 10")
                null
            }
        }
    }
    
    /**
     * Save image to MediaStore for gallery visibility (Android 10+)
     * Images will survive app uninstall and appear in device gallery
     * 
     * @param context Application context
     * @param imageFile The image file to copy to MediaStore
     * @return Uri of the saved image in MediaStore, or null if failed/not supported
     */
    suspend fun saveImageToMediaStore(context: Context, imageFile: File): Uri? {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LogiCam")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            imageFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        
                        // Mark as no longer pending
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                        
                        SecureLogger.i("StorageUtil", "Image saved to MediaStore: ${imageFile.name}")
                    }
                    
                    uri
                } catch (e: Exception) {
                    SecureLogger.e("StorageUtil", "Failed to save image to MediaStore", e)
                    null
                }
            } else {
                // For older devices, fallback to current implementation
                SecureLogger.i("StorageUtil", "MediaStore not available on Android < 10")
                null
            }
        }
    }
}
