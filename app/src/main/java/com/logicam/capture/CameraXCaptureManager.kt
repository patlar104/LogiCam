package com.logicam.capture

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.logicam.util.AppConfig
import com.logicam.util.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX-based capture pipeline with fallback support and orientation handling
 */
class CameraXCaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var recording: Recording? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.IDLE)
    val cameraState: StateFlow<CameraState> = _cameraState
    
    // Orientation listener for proper image/video rotation
    private val orientationEventListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            
            val rotation = when (orientation) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
            
            // Update target rotation for image and video capture
            imageCapture?.targetRotation = rotation
            videoCapture?.targetRotation = rotation
        }
    }
    
    sealed class CameraState {
        object IDLE : CameraState()
        object READY : CameraState()
        object RECORDING : CameraState()
        data class ERROR(val message: String) : CameraState()
    }
    
    suspend fun initialize(): Result<Unit> {
        return try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
            
            // Set up video capture with configured quality
            val quality = AppConfig.getVideoQuality(context)
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(quality))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Set up preview
            preview = Preview.Builder().build()
            
            // Set up image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // Set up image analysis for future use
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            // Enable orientation listener for automatic rotation handling
            if (orientationEventListener.canDetectOrientation()) {
                orientationEventListener.enable()
                SecureLogger.i("CameraXCapture", "Orientation listener enabled")
            }
            
            _cameraState.value = CameraState.READY
            SecureLogger.i("CameraXCapture", "Camera initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            _cameraState.value = CameraState.ERROR(e.message ?: "Unknown error")
            SecureLogger.e("CameraXCapture", "Failed to initialize camera", e)
            Result.failure(e)
        }
    }
    
    fun bindToLifecycle(previewSurfaceProvider: Preview.SurfaceProvider): Result<Unit> {
        return try {
            val provider = cameraProvider ?: throw IllegalStateException("Camera not initialized")
            
            provider.unbindAll()
            
            preview?.setSurfaceProvider(previewSurfaceProvider)
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Bind use cases to lifecycle
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
                imageCapture,
                imageAnalysis
            )
            
            SecureLogger.i("CameraXCapture", "Camera bound to lifecycle")
            Result.success(Unit)
        } catch (e: Exception) {
            _cameraState.value = CameraState.ERROR(e.message ?: "Binding failed")
            SecureLogger.e("CameraXCapture", "Failed to bind camera", e)
            Result.failure(e)
        }
    }
    
    fun getPreview(): Preview? = preview
    
    fun getVideoCapture(): VideoCapture<Recorder>? = videoCapture
    
    fun getImageCapture(): ImageCapture? = imageCapture
    
    suspend fun setActiveRecording(recording: Recording?) {
        this.recording = recording
        _cameraState.value = if (recording != null) CameraState.RECORDING else CameraState.READY
    }
    
    fun getActiveRecording(): Recording? = recording
    
    fun shutdown() {
        // Disable orientation listener
        orientationEventListener.disable()
        
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        recording?.stop()
        SecureLogger.i("CameraXCapture", "Camera shutdown complete")
    }
}
