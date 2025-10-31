package com.logicam.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.logicam.R
import android.widget.TextView
import androidx.camera.view.PreviewView
import com.logicam.session.SessionManagerService
import com.logicam.util.SecureLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main activity with QuickCapture-style instant recording
 * Uses ViewModel for state management and business logic
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var recordButton: MaterialButton
    
    // ViewModel manages all business logic and survives configuration changes
    private val viewModel: MainViewModel by viewModels()
    
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
            viewModel.initializeCamera(this)
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.permissions_required_title)
                .setMessage(R.string.permissions_required_message)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        startActivity(this)
                    }
                }
                .setNegativeButton(R.string.exit_app) { _, _ -> finish() }
                .setCancelable(false)
                .show()
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
        
        // Check permissions
        if (checkPermissions()) {
            viewModel.initializeCamera(this)
        } else {
            requestPermissions()
        }
        
        // Start session manager service
        startSessionService()
        
        // Set up UI
        setupUI()
        
        // Observe ViewModel state
        observeUiState()
        observeSessionState()
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
    
    private fun setupUI() {
        recordButton.setOnClickListener {
            if (viewModel.isRecording()) {
                viewModel.stopRecording()
            } else {
                viewModel.startRecording()
            }
        }
    }
    
    /**
     * Observe ViewModel UI state and update UI accordingly
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is MainViewModel.CameraUiState.Idle -> {
                        updateStatus("Idle")
                        recordButton.isEnabled = false
                    }
                    is MainViewModel.CameraUiState.Initializing -> {
                        updateStatus("Initializing camera...")
                        recordButton.isEnabled = false
                    }
                    is MainViewModel.CameraUiState.Ready -> {
                        updateStatus("Camera ready")
                        recordButton.isEnabled = true
                        recordButton.text = getString(R.string.start_recording)
                        recordButton.setBackgroundColor(getColor(R.color.purple_500))
                        
                        // Bind camera preview
                        val cameraManager = viewModel.getCameraManager()
                        cameraManager?.let {
                            it.getPreview()?.let { preview ->
                                it.bindToLifecycle(previewView.surfaceProvider)
                            }
                        }
                    }
                    is MainViewModel.CameraUiState.Recording -> {
                        val durationSec = state.duration / 1000
                        updateStatus("Recording... ${durationSec}s")
                        recordButton.text = getString(R.string.stop_recording)
                        recordButton.setBackgroundColor(getColor(R.color.recording_red))
                    }
                    is MainViewModel.CameraUiState.RecordingCompleted -> {
                        updateStatus("Recording saved")
                        recordButton.text = getString(R.string.start_recording)
                        recordButton.setBackgroundColor(getColor(R.color.purple_500))
                        Toast.makeText(
                            this@MainActivity,
                            "Recording saved: ${state.file.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is MainViewModel.CameraUiState.Error -> {
                        updateStatus("Error: ${state.message}")
                        recordButton.isEnabled = state.canRetry
                        if (state.canRetry) {
                            showErrorDialog(state.message)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is MainViewModel.CameraUiState.UsingFallback -> {
                        updateStatus(state.message)
                        recordButton.isEnabled = false // Camera2 fallback doesn't support recording yet
                        Toast.makeText(
                            this@MainActivity,
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Camera Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                viewModel.retryInitialization(this)
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
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
        SecureLogger.i("MainActivity", "Activity destroyed")
    }
}
