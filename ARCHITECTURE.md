# LogiCam Architecture Documentation

## Overview

LogiCam is designed as a production-ready camera recording application with emphasis on stability, reliability, and graceful degradation. The architecture follows clean separation of concerns with distinct layers for different responsibilities.

## Architecture Principles

1. **Modularity**: Each component has a single, well-defined responsibility
2. **Kotlin-First**: Leverages Kotlin features like coroutines, sealed classes, and StateFlow
3. **Reactive State Management**: Uses Kotlin Flow for reactive state updates
4. **Graceful Degradation**: Multiple fallback strategies for failure scenarios
5. **Lifecycle Awareness**: Proper handling of Android lifecycle events

## Layer Architecture

```
┌─────────────────────────────────────────┐
│           UI Layer (ui/)                 │
│  MainActivity - QuickCapture Interface   │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│      Session Layer (session/)            │
│  SessionManagerService - Warm Sessions   │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│      Capture Layer (capture/)            │
│  CameraX + Camera2 + Recording           │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│      Upload Layer (upload/)              │
│  UploadManager + WorkManager             │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│      Utility Layer (util/)               │
│  Logging + Storage Management            │
└─────────────────────────────────────────┘
```

## Component Details

### Session Layer

**SessionManagerService**
- Foreground service with camera service type
- Maintains warm camera session for instant capture
- Auto-reconnection with exponential backoff
- Maximum 5 retry attempts with increasing delays
- State management: IDLE → ACTIVE → RECONNECTING → ERROR

Key Features:
- Persistent notification for user awareness
- Coroutine-based async operations
- Lifecycle-aware cleanup

### Capture Layer

**CameraXCaptureManager**
- Primary camera implementation using CameraX
- Manages camera provider lifecycle
- Configures multi-stream use cases:
  - Preview: Real-time viewfinder
  - VideoCapture: Recording stream
  - ImageAnalysis: Optional frame analysis
- State flow for reactive updates
- Thread-safe with single executor

**Camera2FallbackManager**
- Fallback implementation using Camera2 API
- Used when CameraX unavailable or fails
- Direct MediaRecorder integration
- Lower-level camera control
- Suspending camera open with coroutines

**RecordingManager**
- Coordinates video recording lifecycle
- Generates metadata for each recording
- Handles recording events (start, pause, resume, finalize)
- Automatic metadata JSON creation
- Error handling and recovery

Recording Flow:
1. Generate unique filename with timestamp
2. Create FileOutputOptions
3. Start recording with event listener
4. Monitor recording state
5. On finalize, write metadata to disk
6. Trigger upload scheduling

### Upload Layer

**UploadManager**
- High-level upload coordination
- StateFlow for upload status
- Schedule individual or batch uploads
- Integrates with WorkManager

**UploadWorker**
- Background WorkManager worker
- Network-aware scheduling (WiFi or cellular)
- Retry logic with exponential backoff
- Processes pending uploads directory
- Cleans up after successful upload

Upload Strategy:
- Only runs when network available
- Maximum 3 retry attempts
- Partial success handling
- Secure file cleanup

### Utility Layer

**SecureLogger**
- Dual logging: Logcat + file
- Timestamped entries
- File-based persistence
- Recent log retrieval
- Thread-safe operations

**StorageUtil**
- File path management
- Video output directory creation
- Metadata file association
- Pending uploads directory

## State Management

### Camera States
```kotlin
sealed class CameraState {
    object IDLE      // Not initialized
    object READY     // Ready to capture
    object RECORDING // Currently recording
    data class ERROR(message: String)
}
```

### Recording States
```kotlin
sealed class RecordingState {
    object Idle
    data class Recording(file: File, startTime: Long)
    data class Paused(file: File)
    data class Completed(file: File, duration: Long)
    data class Error(message: String)
}
```

### Upload States
```kotlin
sealed class UploadState {
    object Idle
    data class Uploading(filename: String, progress: Int)
    data class Completed(filename: String)
    data class Failed(filename: String, error: String)
}
```

## Data Flow

### Recording Flow
```
User Tap → MainActivity
    ↓
RecordingManager.startRecording()
    ↓
CameraXCaptureManager.getVideoCapture()
    ↓
VideoCapture.start()
    ↓
Recording Events → RecordingManager
    ↓
File Saved + Metadata Written
    ↓
UploadManager.scheduleUpload()
    ↓
WorkManager enqueues UploadWorker
```

### Session Management Flow
```
App Start → MainActivity.onCreate()
    ↓
Start SessionManagerService (Foreground)
    ↓
Service.initializeSession()
    ↓
Monitor session health
    ↓
On Error → attemptReconnect()
    ↓
Retry with backoff (max 5 times)
    ↓
Success → Reset counter | Failure → Show error
```

## Error Handling Strategy

### Camera Initialization Failure
1. Try CameraX initialization
2. If fails, attempt Camera2 fallback
3. Show user-friendly error message
4. Allow retry via session reset

### Recording Failure
1. Catch recording event errors
2. Flush partial recording to disk
3. Log error with full context
4. Reset recording state
5. Notify user

### Upload Failure
1. Keep file in pending directory
2. WorkManager retries automatically
3. Exponential backoff between retries
4. Max 3 attempts before marking failed
5. Maintain failed uploads for manual retry

### Session Disconnection
1. Detect via callback
2. Update state to RECONNECTING
3. Attempt reconnect with delay
4. Increment retry counter
5. Give up after max attempts
6. Maintain notification throughout

## Threading Model

- **Main Thread**: UI updates, lifecycle events
- **IO Dispatcher**: File operations, logging
- **Default Dispatcher**: Background computations, session management
- **Camera Executor**: Single-threaded executor for camera operations

## Memory Management

- Proper cleanup in onDestroy()
- Camera unbind on lifecycle stop
- Service cleanup on destroy
- WorkManager handles worker lifecycle
- No static context references

## Security Considerations

1. **Permissions**: Runtime permission checks before camera access
2. **Foreground Service**: User-visible notification required
3. **File Storage**: Uses scoped storage (app-specific directory)
4. **Metadata**: No PII in metadata files
5. **Logging**: Sensitive data excluded from logs
6. **Upload**: Placeholder for secure HTTPS implementation

## Performance Optimizations

1. **Warm Session**: Reduces capture latency
2. **Single Executor**: Prevents thread contention
3. **StateFlow**: Efficient state propagation
4. **WorkManager**: Battery-efficient background work
5. **Coroutines**: Lightweight async operations

## Testing Strategy

### Unit Tests (Future)
- State transitions
- Error handling logic
- Metadata generation
- File path utilities

### Integration Tests (Future)
- Camera initialization
- Recording lifecycle
- Upload workflow
- Service lifecycle

### Manual Testing (Current)
- End-to-end recording
- Error scenarios
- Multi-session stability
- Background behavior

## Extension Points

1. **Custom Upload Backend**: Implement in UploadWorker
2. **Analysis Stream**: Configure in CameraXCaptureManager
3. **Camera Selection**: Add UI for front/back toggle
4. **Quality Settings**: Add user preferences
5. **Multi-Camera**: Extend for simultaneous streams

## Android Beta Compatibility

- Targets API 34 (Android 14)
- Min SDK 31 (Android 12)
- Uses stable CameraX 1.3.x
- Foreground service types properly declared
- New permission model supported
- Handles API deprecations

## Future Architecture Improvements

1. **Dependency Injection**: Add Hilt/Dagger
2. **Repository Pattern**: Abstract data sources
3. **ViewModel**: Add proper MVVM architecture
4. **Compose UI**: Migrate to Jetpack Compose
5. **Room Database**: Track recording history
6. **Analytics**: Add telemetry for reliability metrics
