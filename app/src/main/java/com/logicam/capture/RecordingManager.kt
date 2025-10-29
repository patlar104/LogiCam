package com.logicam.capture

import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.logicam.util.SecureLogger
import com.logicam.util.StorageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages video recording with metadata
 */
class RecordingManager(
    private val context: Context,
    private val cameraManager: CameraXCaptureManager
) {
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState
    
    private var currentVideoFile: File? = null
    
    sealed class RecordingState {
        object Idle : RecordingState()
        data class Recording(val file: File, val startTime: Long) : RecordingState()
        data class Paused(val file: File) : RecordingState()
        data class Completed(val file: File, val duration: Long) : RecordingState()
        data class Error(val message: String) : RecordingState()
    }
    
    suspend fun startRecording(onEvent: (VideoRecordEvent) -> Unit = {}): Result<File> {
        return try {
            val videoCapture = cameraManager.getVideoCapture()
                ?: return Result.failure(IllegalStateException("VideoCapture not initialized"))
            
            val outputDir = StorageUtil.getVideoOutputDirectory(context)
            val videoFile = File(outputDir, StorageUtil.generateVideoFileName())
            currentVideoFile = videoFile
            
            val outputOptions = FileOutputOptions.Builder(videoFile).build()
            
            val recording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    handleRecordingEvent(event)
                    onEvent(event)
                }
            
            cameraManager.setActiveRecording(recording)
            _recordingState.value = RecordingState.Recording(videoFile, System.currentTimeMillis())
            
            SecureLogger.i("RecordingManager", "Recording started: ${videoFile.name}")
            Result.success(videoFile)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            SecureLogger.e("RecordingManager", "Failed to start recording", e)
            Result.failure(e)
        }
    }
    
    suspend fun stopRecording(): Result<File?> {
        return try {
            val recording = cameraManager.getActiveRecording()
            if (recording != null) {
                recording.stop()
                cameraManager.setActiveRecording(null)
                SecureLogger.i("RecordingManager", "Recording stopped")
            }
            Result.success(currentVideoFile)
        } catch (e: Exception) {
            SecureLogger.e("RecordingManager", "Failed to stop recording", e)
            Result.failure(e)
        }
    }
    
    private fun handleRecordingEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                SecureLogger.i("RecordingManager", "Recording event: Start")
            }
            is VideoRecordEvent.Finalize -> {
                if (!event.hasError()) {
                    val file = currentVideoFile
                    if (file != null) {
                        val startTime = (recordingState.value as? RecordingState.Recording)?.startTime ?: 0
                        val duration = System.currentTimeMillis() - startTime
                        _recordingState.value = RecordingState.Completed(file, duration)
                        
                        // Write metadata
                        writeMetadata(file, duration)
                        
                        SecureLogger.i("RecordingManager", "Recording finalized: ${file.name}, duration: ${duration}ms")
                    }
                } else {
                    val error = event.cause?.message ?: "Unknown error"
                    _recordingState.value = RecordingState.Error(error)
                    SecureLogger.e("RecordingManager", "Recording error: $error", event.cause)
                }
            }
            is VideoRecordEvent.Status -> {
                // Recording status update
            }
            is VideoRecordEvent.Pause -> {
                currentVideoFile?.let {
                    _recordingState.value = RecordingState.Paused(it)
                }
                SecureLogger.i("RecordingManager", "Recording paused")
            }
            is VideoRecordEvent.Resume -> {
                currentVideoFile?.let {
                    _recordingState.value = RecordingState.Recording(it, System.currentTimeMillis())
                }
                SecureLogger.i("RecordingManager", "Recording resumed")
            }
        }
    }
    
    private fun writeMetadata(videoFile: File, duration: Long) {
        try {
            val metadataFile = StorageUtil.getMetadataFile(videoFile)
            val metadata = JSONObject().apply {
                put("filename", videoFile.name)
                put("duration_ms", duration)
                put("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                put("size_bytes", videoFile.length())
                put("device_model", android.os.Build.MODEL)
                put("android_version", android.os.Build.VERSION.SDK_INT)
            }
            
            metadataFile.writeText(metadata.toString(2))
            SecureLogger.i("RecordingManager", "Metadata written: ${metadataFile.name}")
        } catch (e: Exception) {
            SecureLogger.e("RecordingManager", "Failed to write metadata", e)
        }
    }
    
    fun isRecording(): Boolean {
        return recordingState.value is RecordingState.Recording
    }
}
