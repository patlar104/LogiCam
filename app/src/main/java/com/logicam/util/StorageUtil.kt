package com.logicam.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for managing storage and file paths
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
    
    fun getMetadataFile(videoFile: File): File {
        return File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_metadata.json")
    }
    
    fun getPendingUploadsDirectory(context: Context): File {
        return File(context.filesDir, "pending_uploads").apply { mkdirs() }
    }
}
