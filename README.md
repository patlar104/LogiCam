# LogiCam

A stable, low-latency camera recording Android app for Pixel devices running Android beta.

## Features

- **Stable CameraX Integration**: Primary camera implementation using CameraX with automatic fallback to Camera2 API when needed
- **SessionManager Service**: Maintains warm camera sessions with auto-reconnection on failure
- **QuickCapture Recording**: Instant recording with minimal UI for fast documentation
- **Multi-Stream Support**: Simultaneous preview, recording, and analysis streams
- **Background Upload**: Automatic upload of recordings with retry logic using WorkManager
- **Secure Metadata Logging**: Comprehensive logging with secure file-based metadata

## Architecture

The app follows a modular, layer-separated architecture:

### Layers

1. **Session Layer** (`session/`)
   - `SessionManagerService`: Foreground service maintaining camera session
   - Auto-reconnection with exponential backoff
   - Persistent notification for user awareness

2. **Capture Layer** (`capture/`)
   - `CameraXCaptureManager`: Primary CameraX implementation
   - `Camera2FallbackManager`: Fallback Camera2 API support
   - `RecordingManager`: Video recording with metadata generation
   - Multi-stream support for preview, recording, and analysis

3. **Upload Layer** (`upload/`)
   - `UploadManager`: Coordinates background uploads
   - `UploadWorker`: WorkManager-based background upload with retry
   - Network-aware scheduling

4. **UI Layer** (`ui/`)
   - `MainActivity`: QuickCapture-style minimal interface
   - Instant recording with large capture button
   - Real-time status updates

5. **Utility Layer** (`util/`)
   - `SecureLogger`: File-based logging with metadata
   - `StorageUtil`: File management and path handling

## Requirements

- Android 12 (API 31) or higher
- Pixel device recommended
- Camera and microphone permissions
- Storage permissions for saving recordings
- Network permissions for uploads

## Permissions

The app requires the following permissions:
- `CAMERA`: For camera access
- `RECORD_AUDIO`: For audio recording
- `FOREGROUND_SERVICE`: For SessionManager service
- `FOREGROUND_SERVICE_CAMERA`: For camera-specific foreground service
- `INTERNET`: For background uploads
- `POST_NOTIFICATIONS`: For upload notifications (Android 13+)

## Build

```bash
./gradlew assembleDebug
```

## Testing

The app is designed for stability across Android beta builds with:
- Graceful fallbacks for camera initialization failures
- Auto-reconnection with configurable retry limits
- Comprehensive error logging
- Partial recording flush to disk on failure

## Key Design Decisions

1. **CameraX Primary, Camera2 Fallback**: CameraX provides better abstraction and lifecycle management, with Camera2 as a fallback for edge cases
2. **Foreground Service for Session**: Ensures camera session persistence even when app is backgrounded
3. **WorkManager for Uploads**: Provides reliable background execution with built-in retry logic
4. **Kotlin Coroutines**: Used throughout for async operations and state management
5. **StateFlow for State Management**: Reactive state updates across components

## Future Enhancements

- Custom upload endpoint configuration
- Video quality settings
- Analysis stream for real-time processing
- Multiple camera support
- Batch upload optimization