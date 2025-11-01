package com.logicam.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.logicam.R
import com.logicam.util.SecureLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that maintains a warm camera session
 * Handles auto-reconnection and session management
 */
class SessionManagerService : Service() {
    
    private val binder = SessionBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState
    
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    
    enum class SessionState {
        IDLE,
        ACTIVE,
        RECONNECTING,
        ERROR
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "camera_session_channel"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 2000L
    }
    
    inner class SessionBinder : Binder() {
        fun getService(): SessionManagerService = this@SessionManagerService
    }
    
    override fun onCreate() {
        super.onCreate()
        SecureLogger.i("SessionManager", "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SecureLogger.i("SessionManager", "Service started")
        
        val notification = createNotification(getString(R.string.notification_session_active))
        startForeground(NOTIFICATION_ID, notification)
        
        initializeSession()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    private fun initializeSession() {
        serviceScope.launch {
            try {
                _sessionState.value = SessionState.ACTIVE
                SecureLogger.i("SessionManager", "Session initialized")
                updateNotification(getString(R.string.notification_session_ready))
            } catch (e: Exception) {
                SecureLogger.e("SessionManager", "Failed to initialize session", e)
                handleSessionError()
            }
        }
    }
    
    private fun handleSessionError() {
        _sessionState.value = SessionState.ERROR
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            attemptReconnect()
        } else {
            SecureLogger.e("SessionManager", "Max reconnect attempts reached")
            updateNotification(getString(R.string.notification_session_failed))
        }
    }
    
    private fun attemptReconnect() {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            _sessionState.value = SessionState.RECONNECTING
            reconnectAttempts++
            
            SecureLogger.i("SessionManager", "Reconnecting... Attempt $reconnectAttempts")
            updateNotification(getString(R.string.notification_reconnecting_format, reconnectAttempts, MAX_RECONNECT_ATTEMPTS))
            
            delay(RECONNECT_DELAY_MS * reconnectAttempts)
            
            try {
                initializeSession()
                reconnectAttempts = 0
            } catch (e: Exception) {
                SecureLogger.e("SessionManager", "Reconnect failed", e)
                handleSessionError()
            }
        }
    }
    
    fun resetSession() {
        reconnectAttempts = 0
        initializeSession()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        reconnectJob?.cancel()
        // Cancel the service scope to prevent memory leaks
        serviceScope.coroutineContext[Job]?.cancel()
        SecureLogger.i("SessionManager", "Service destroyed")
    }
}
