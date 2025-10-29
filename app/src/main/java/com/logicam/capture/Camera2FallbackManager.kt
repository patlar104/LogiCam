package com.logicam.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Build
import android.view.Surface
import androidx.core.content.ContextCompat
import com.logicam.util.SecureLogger
import com.logicam.util.StorageUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera2 API fallback for when CameraX is unavailable
 */
class Camera2FallbackManager(private val context: Context) {
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    
    suspend fun openCamera(cameraId: String = "0"): Result<CameraDevice> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(SecurityException("Camera permission not granted"))
        }
        
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val device = suspendCancellableCoroutine<CameraDevice> { continuation ->
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        continuation.resume(camera)
                        SecureLogger.i("Camera2Fallback", "Camera opened: $cameraId")
                    }
                    
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                        SecureLogger.w("Camera2Fallback", "Camera disconnected")
                    }
                    
                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                        continuation.resumeWithException(
                            RuntimeException("Camera error: $error")
                        )
                        SecureLogger.e("Camera2Fallback", "Camera error: $error")
                    }
                }, null)
                
                continuation.invokeOnCancellation {
                    cameraDevice?.close()
                }
            }
            Result.success(device)
        } catch (e: Exception) {
            SecureLogger.e("Camera2Fallback", "Failed to open camera", e)
            Result.failure(e)
        }
    }
    
    fun createRecordingSession(previewSurface: Surface): Result<Unit> {
        return try {
            val device = cameraDevice ?: return Result.failure(
                IllegalStateException("Camera device not initialized")
            )
            
            val outputFile = File(
                StorageUtil.getVideoOutputDirectory(context),
                StorageUtil.generateVideoFileName()
            )
            
            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile.absolutePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(1920, 1080)
                setVideoFrameRate(30)
                prepare()
            }
            
            val recordingSurface = mediaRecorder?.surface
                ?: return Result.failure(IllegalStateException("MediaRecorder surface not available"))
            
            device.createCaptureSession(
                listOf(previewSurface, recordingSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startRecording(session, previewSurface, recordingSurface)
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        SecureLogger.e("Camera2Fallback", "Capture session configuration failed")
                    }
                },
                null
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            SecureLogger.e("Camera2Fallback", "Failed to create recording session", e)
            Result.failure(e)
        }
    }
    
    private fun startRecording(
        session: CameraCaptureSession,
        previewSurface: Surface,
        recordingSurface: Surface
    ) {
        try {
            val device = cameraDevice ?: return
            
            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            requestBuilder.addTarget(previewSurface)
            requestBuilder.addTarget(recordingSurface)
            
            session.setRepeatingRequest(
                requestBuilder.build(),
                null,
                null
            )
            
            mediaRecorder?.start()
            SecureLogger.i("Camera2Fallback", "Recording started")
        } catch (e: Exception) {
            SecureLogger.e("Camera2Fallback", "Failed to start recording", e)
        }
    }
    
    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            SecureLogger.i("Camera2Fallback", "Recording stopped")
        } catch (e: Exception) {
            SecureLogger.e("Camera2Fallback", "Failed to stop recording", e)
        }
    }
    
    fun close() {
        stopRecording()
        captureSession?.close()
        cameraDevice?.close()
        captureSession = null
        cameraDevice = null
        SecureLogger.i("Camera2Fallback", "Camera closed")
    }
}
