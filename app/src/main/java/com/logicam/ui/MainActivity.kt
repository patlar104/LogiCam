package com.logicam.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.logicam.R
import com.logicam.capture.CameraXCaptureManager
import com.logicam.capture.RecordingManager
import android.view.LayoutInflater
import android.widget.TextView
import androidx.camera.view.PreviewView
import com.logicam.session.SessionManagerService
import com.logicam.util.AppConfig
import com.logicam.upload.UploadManager
import com.logicam.util.SecureLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main activity with QuickCapture-style instant recording
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var recordButton: MaterialButton
    private lateinit var cameraManager: CameraXCaptureManager
    private lateinit var recordingManager: RecordingManager
    private lateinit var uploadManager: UploadManager
    
    private var sessionService: SessionManagerService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SessionManagerService.SessionBinder
            sessionService = binder.getService()
            isServiceBound = true
            SecureLogger.i("MainActivity", "Service connected")
            observeSessionState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            sessionService = null
            isServiceBound = false
            SecureLogger.i("MainActivity", "Service disconnected")
        }
    }
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeCamera()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)
        recordButton = findViewById(R.id.recordButton)
        
        SecureLogger.i("MainActivity", "Activity created")
        
        // Initialize managers
        cameraManager = CameraXCaptureManager(this, this)
        recordingManager = RecordingManager(this, cameraManager)
        uploadManager = UploadManager(this)
        
        // Check permissions
        if (checkPermissions()) {
            initializeCamera()
        } else {
            requestPermissions()
        }
        
        // Start session manager service
        startSessionService()
        
        // Set up UI
        setupUI()
        
        // Observe states
        observeRecordingState()
        observeCameraState()
    }
    
    private fun checkPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }
    
    private fun initializeCamera() {
        lifecycleScope.launch {
            val result = cameraManager.initialize()
            if (result.isSuccess) {
                val preview = cameraManager.getPreview()
                if (preview != null) {
                    cameraManager.bindToLifecycle(previewView.surfaceProvider)
                }
                updateStatus("Camera ready")
            } else {
                updateStatus("Camera initialization failed")
                Toast.makeText(
                    this@MainActivity,
                    "Failed to initialize camera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupUI() {
        recordButton.setOnClickListener {
            if (recordingManager.isRecording()) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }
    
    private fun startRecording() {
        val result = recordingManager.startRecording()
        if (result.isSuccess) {
            recordButton.apply {
                text = getString(R.string.stop_recording)
                setBackgroundColor(getColor(R.color.recording_red))
            }
            updateStatus("Recording...")
            Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopRecording() {
        val result = recordingManager.stopRecording()
        if (result.isSuccess) {
            recordButton.apply {
                text = getString(R.string.start_recording)
                setBackgroundColor(getColor(R.color.purple_500))
            }
            updateStatus("Recording stopped")
            
            result.getOrNull()?.let { file ->
                // Schedule upload if auto-upload is enabled
                if (AppConfig.isAutoUploadEnabled(this)) {
                    uploadManager.scheduleUpload(file)
                    SecureLogger.i("MainActivity", "Upload scheduled for ${file.name}")
                } else {
                    SecureLogger.i("MainActivity", "Auto-upload disabled, skipping upload")
                }
            }
        }
    }
    
    private fun observeRecordingState() {
        lifecycleScope.launch {
            recordingManager.recordingState.collectLatest { state ->
                when (state) {
                    is RecordingManager.RecordingState.Recording -> {
                        SecureLogger.i("MainActivity", "Recording: ${state.file.name}")
                    }
                    is RecordingManager.RecordingState.Completed -> {
                        SecureLogger.i("MainActivity", "Recording completed: ${state.file.name}, duration: ${state.duration}ms")
                        Toast.makeText(
                            this@MainActivity,
                            "Recording saved: ${state.file.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is RecordingManager.RecordingState.Error -> {
                        SecureLogger.e("MainActivity", "Recording error: ${state.message}")
                        updateStatus("Error: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun observeCameraState() {
        lifecycleScope.launch {
            cameraManager.cameraState.collectLatest { state ->
                when (state) {
                    is CameraXCaptureManager.CameraState.READY -> {
                        updateStatus("Camera ready")
                    }
                    is CameraXCaptureManager.CameraState.ERROR -> {
                        updateStatus("Camera error: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun observeSessionState() {
        lifecycleScope.launch {
            sessionService?.sessionState?.collectLatest { state ->
                when (state) {
                    SessionManagerService.SessionState.ACTIVE -> {
                        SecureLogger.i("MainActivity", "Session active")
                    }
                    SessionManagerService.SessionState.RECONNECTING -> {
                        updateStatus("Reconnecting...")
                    }
                    SessionManagerService.SessionState.ERROR -> {
                        updateStatus("Session error")
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun startSessionService() {
        val intent = Intent(this, SessionManagerService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun updateStatus(status: String) {
        statusText.text = status
        lifecycleScope.launch {
            SecureLogger.logToFile(this@MainActivity, "MainActivity", "Status: $status")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
        cameraManager.shutdown()
        SecureLogger.i("MainActivity", "Activity destroyed")
    }
}
