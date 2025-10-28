package com.logicam.util

import android.content.Context
import android.content.SharedPreferences
import androidx.camera.video.Quality

/**
 * Configuration manager for app settings
 * Demonstrates extensibility for quality settings, upload configuration, etc.
 */
object AppConfig {
    
    private const val PREFS_NAME = "logicam_prefs"
    private const val KEY_VIDEO_QUALITY = "video_quality"
    private const val KEY_UPLOAD_ONLY_WIFI = "upload_only_wifi"
    private const val KEY_AUTO_UPLOAD = "auto_upload"
    private const val KEY_MAX_RECONNECT_ATTEMPTS = "max_reconnect_attempts"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get configured video quality
     * Default: FHD (1080p)
     */
    fun getVideoQuality(context: Context): Quality {
        val qualityValue = getPrefs(context).getString(KEY_VIDEO_QUALITY, "FHD")
        return when (qualityValue) {
            "UHD" -> Quality.UHD    // 4K
            "FHD" -> Quality.FHD    // 1080p
            "HD" -> Quality.HD      // 720p
            "SD" -> Quality.SD      // 480p
            else -> Quality.FHD
        }
    }
    
    /**
     * Set video quality preference
     */
    fun setVideoQuality(context: Context, quality: Quality) {
        val qualityValue = when (quality) {
            Quality.UHD -> "UHD"
            Quality.FHD -> "FHD"
            Quality.HD -> "HD"
            Quality.SD -> "SD"
            else -> "FHD"
        }
        getPrefs(context).edit().putString(KEY_VIDEO_QUALITY, qualityValue).apply()
    }
    
    /**
     * Check if uploads should only happen on WiFi
     * Default: true
     */
    fun isUploadOnlyWifi(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_UPLOAD_ONLY_WIFI, true)
    }
    
    /**
     * Set WiFi-only upload preference
     */
    fun setUploadOnlyWifi(context: Context, wifiOnly: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UPLOAD_ONLY_WIFI, wifiOnly).apply()
    }
    
    /**
     * Check if auto-upload is enabled
     * Default: true
     */
    fun isAutoUploadEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_UPLOAD, true)
    }
    
    /**
     * Set auto-upload preference
     */
    fun setAutoUploadEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_UPLOAD, enabled).apply()
    }
    
    /**
     * Get maximum reconnect attempts for session manager
     * Default: 5
     */
    fun getMaxReconnectAttempts(context: Context): Int {
        return getPrefs(context).getInt(KEY_MAX_RECONNECT_ATTEMPTS, 5)
    }
    
    /**
     * Set maximum reconnect attempts
     */
    fun setMaxReconnectAttempts(context: Context, attempts: Int) {
        getPrefs(context).edit().putInt(KEY_MAX_RECONNECT_ATTEMPTS, attempts).apply()
    }
}
