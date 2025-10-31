package com.logicam.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.logicam.R

/**
 * Handles camera and app errors with actionable user dialogs
 */
object ErrorHandler {
    
    /**
     * Sealed class representing specific camera and app errors
     */
    sealed class CameraError {
        /**
         * Camera is being used by another app
         */
        object InUseByOtherApp : CameraError()
        
        /**
         * Device storage is critically low
         */
        data class LowStorage(val availableMB: Long) : CameraError()
        
        /**
         * Camera hardware has failed and needs restart
         */
        object HardwareFailed : CameraError()
        
        /**
         * Generic camera initialization error
         */
        data class InitializationFailed(val message: String) : CameraError()
        
        /**
         * Recording failed due to unknown error
         */
        data class RecordingFailed(val message: String) : CameraError()
    }
    
    /**
     * Check available storage space in MB
     */
    fun getAvailableStorageMB(context: Context): Long {
        val path = context.filesDir
        val stat = StatFs(path.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availableBytes / (1024 * 1024) // Convert to MB
    }
    
    /**
     * Check if device has sufficient storage for recording
     * @param requiredMB Minimum required storage in MB (default 100MB)
     * @return CameraError.LowStorage if insufficient, null if OK
     */
    fun checkStorageSpace(context: Context, requiredMB: Long = 100): CameraError.LowStorage? {
        val availableMB = getAvailableStorageMB(context)
        return if (availableMB < requiredMB) {
            CameraError.LowStorage(availableMB)
        } else {
            null
        }
    }
    
    /**
     * Show error dialog with actionable options
     */
    fun showErrorDialog(context: Context, error: CameraError, onRetry: (() -> Unit)? = null) {
        when (error) {
            is CameraError.InUseByOtherApp -> {
                AlertDialog.Builder(context)
                    .setTitle(R.string.error_camera_in_use_title)
                    .setMessage(R.string.error_camera_in_use_message)
                    .setPositiveButton(R.string.retry) { _, _ ->
                        onRetry?.invoke()
                    }
                    .setNegativeButton(R.string.close_app) { _, _ ->
                        (context as? android.app.Activity)?.finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            
            is CameraError.LowStorage -> {
                AlertDialog.Builder(context)
                    .setTitle(R.string.error_low_storage_title)
                    .setMessage(context.getString(R.string.error_low_storage_message, error.availableMB))
                    .setPositiveButton(R.string.open_settings) { _, _ ->
                        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        context.startActivity(intent)
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            
            is CameraError.HardwareFailed -> {
                AlertDialog.Builder(context)
                    .setTitle(R.string.error_hardware_failed_title)
                    .setMessage(R.string.error_hardware_failed_message)
                    .setPositiveButton(R.string.retry) { _, _ ->
                        onRetry?.invoke()
                    }
                    .setNegativeButton(R.string.report_issue) { _, _ ->
                        sendErrorReport(context, "Camera hardware failure")
                    }
                    .setNeutralButton(R.string.close_app) { _, _ ->
                        (context as? android.app.Activity)?.finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            
            is CameraError.InitializationFailed -> {
                AlertDialog.Builder(context)
                    .setTitle(R.string.error_initialization_title)
                    .setMessage(context.getString(R.string.error_initialization_message, error.message))
                    .setPositiveButton(R.string.retry) { _, _ ->
                        onRetry?.invoke()
                    }
                    .setNegativeButton(R.string.report_issue) { _, _ ->
                        sendErrorReport(context, "Initialization failed: ${error.message}")
                    }
                    .show()
            }
            
            is CameraError.RecordingFailed -> {
                AlertDialog.Builder(context)
                    .setTitle(R.string.error_recording_failed_title)
                    .setMessage(context.getString(R.string.error_recording_failed_message, error.message))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.report_issue) { _, _ ->
                        sendErrorReport(context, "Recording failed: ${error.message}")
                    }
                    .show()
            }
        }
    }
    
    /**
     * Send error report via email or external reporting service
     */
    private fun sendErrorReport(context: Context, errorDetails: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "LogiCam Error Report")
            putExtra(Intent.EXTRA_TEXT, """
                Error Details:
                $errorDetails
                
                Device: ${android.os.Build.MODEL}
                Android: ${android.os.Build.VERSION.RELEASE}
                App Version: ${try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "Unknown" }}
            """.trimIndent())
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_issue)))
        }
    }
}
