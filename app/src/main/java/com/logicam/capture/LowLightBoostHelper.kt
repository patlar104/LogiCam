package com.logicam.capture

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import com.logicam.util.SecureLogger

/**
 * Android 15+ Low Light Boost feature support
 * Provides enhanced low-light camera preview and recording capabilities
 * Updated for Android 16.1 QPR
 */
object LowLightBoostHelper {
    
    /**
     * Check if the device supports Low Light Boost mode (Android 15+)
     * @param context Application context
     * @param cameraId Camera ID to check
     * @return true if Low Light Boost is supported
     */
    fun isLowLightBoostSupported(context: Context, cameraId: String = "0"): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return false
        }
        
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val autoExposureModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            
            val supported = autoExposureModes?.contains(
                CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
            ) ?: false
            
            if (supported) {
                SecureLogger.i("LowLightBoost", "Low Light Boost supported on camera $cameraId")
            } else {
                SecureLogger.i("LowLightBoost", "Low Light Boost NOT supported on camera $cameraId")
            }
            
            supported
        } catch (e: Exception) {
            SecureLogger.e("LowLightBoost", "Error checking Low Light Boost support", e)
            false
        }
    }
    
    /**
     * Apply Low Light Boost mode to a capture request builder
     * Only applies if the device supports it (Android 15+)
     * @param builder CaptureRequest.Builder to configure
     * @param context Application context
     * @param cameraId Camera ID being used
     * @return true if Low Light Boost was applied
     */
    fun applyLowLightBoost(
        builder: CaptureRequest.Builder,
        context: Context,
        cameraId: String = "0"
    ): Boolean {
        if (!isLowLightBoostSupported(context, cameraId)) {
            return false
        }
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                builder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
                )
                SecureLogger.i("LowLightBoost", "Low Light Boost mode applied")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            SecureLogger.e("LowLightBoost", "Error applying Low Light Boost", e)
            false
        }
    }
    
    /**
     * Check if Low Light Boost is currently active from a capture result
     * @param result CaptureResult from onCaptureCompleted callback
     * @return true if Low Light Boost is currently active
     */
    fun isLowLightBoostActive(result: CaptureResult): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return false
        }
        
        return try {
            val lowLightState = result.get(CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE)
            val isActive = lowLightState == CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
            
            if (isActive) {
                SecureLogger.d("LowLightBoost", "Low Light Boost is ACTIVE")
            }
            
            isActive
        } catch (e: Exception) {
            SecureLogger.e("LowLightBoost", "Error checking Low Light Boost state", e)
            false
        }
    }
}
