package com.logicam.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor

/**
 * Android 15+ Satellite Connectivity awareness
 * Monitors non-terrestrial network (NTN) status for satellite-only connections
 * Updated for Android 16.1 QPR
 */
class SatelliteConnectivityMonitor(private val context: Context) {
    
    private val _isSatelliteOnly = MutableStateFlow(false)
    val isSatelliteOnly: StateFlow<Boolean> = _isSatelliteOnly
    
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    
    /**
     * Check if the device is currently using satellite-only connectivity
     * @return true if connected only to non-terrestrial network
     */
    @SuppressLint("MissingPermission") // Permission check is performed before accessing serviceState
    fun checkSatelliteStatus(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return false
        }
        
        // Check READ_PHONE_STATE permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            SecureLogger.w("SatelliteMonitor", "READ_PHONE_STATE permission not granted")
            return false
        }
        
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val serviceState = telephonyManager?.serviceState
            
            val isSatellite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                serviceState?.isUsingNonTerrestrialNetwork() == true
            } else {
                false
            }
            
            _isSatelliteOnly.value = isSatellite
            
            if (isSatellite) {
                SecureLogger.i("SatelliteMonitor", "Device is using satellite-only connectivity")
            }
            
            isSatellite
        } catch (e: Exception) {
            SecureLogger.e("SatelliteMonitor", "Error checking satellite status", e)
            false
        }
    }
    
    /**
     * Start monitoring satellite connectivity status
     * Requires READ_PHONE_STATE permission
     * @param executor Executor for callback execution
     */
    fun startMonitoring(executor: Executor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            SecureLogger.i("SatelliteMonitor", "Satellite monitoring not available on this Android version")
            return
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            SecureLogger.w("SatelliteMonitor", "Cannot start monitoring: READ_PHONE_STATE permission not granted")
            return
        }
        
        try {
            telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                    override fun onServiceStateChanged(serviceState: ServiceState) {
                        val isSatellite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            serviceState.isUsingNonTerrestrialNetwork()
                        } else {
                            false
                        }
                        
                        _isSatelliteOnly.value = isSatellite
                        
                        if (isSatellite) {
                            SecureLogger.i("SatelliteMonitor", "Switched to satellite-only connectivity")
                        } else {
                            SecureLogger.i("SatelliteMonitor", "Switched to terrestrial connectivity")
                        }
                    }
                }
                
                telephonyManager?.registerTelephonyCallback(executor, telephonyCallback!!)
                SecureLogger.i("SatelliteMonitor", "Started monitoring satellite connectivity")
            }
        } catch (e: Exception) {
            SecureLogger.e("SatelliteMonitor", "Error starting satellite monitoring", e)
        }
    }
    
    /**
     * Stop monitoring satellite connectivity status
     */
    fun stopMonitoring() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { callback ->
                    telephonyManager?.unregisterTelephonyCallback(callback)
                    SecureLogger.i("SatelliteMonitor", "Stopped monitoring satellite connectivity")
                }
            }
            telephonyCallback = null
            telephonyManager = null
        } catch (e: Exception) {
            SecureLogger.e("SatelliteMonitor", "Error stopping satellite monitoring", e)
        }
    }
    
    /**
     * Get user-friendly connectivity status message
     * @return Status message describing current connectivity
     */
    fun getConnectivityStatusMessage(): String {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM -> 
                "Satellite connectivity monitoring requires Android 15+"
            _isSatelliteOnly.value -> 
                "Connected via satellite only. Data services may be limited."
            else -> 
                "Connected via terrestrial network"
        }
    }
}
