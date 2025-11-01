package com.logicam

import android.app.Application
import com.logicam.util.SecureLogger

/**
 * LogiCam Application class
 * Initializes app-wide dependencies and configuration
 */
class LogiCamApplication : Application() {
    
    /**
     * Dependency injection container
     * Available to all components for dependency creation
     */
    lateinit var container: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependency container
        container = AppContainer(applicationContext)
        
        SecureLogger.i("LogiCamApplication", "Application initialized")
    }
}
