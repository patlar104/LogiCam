# Build and Test Guide

## Prerequisites

1. **Android Studio**: Install the latest version (Hedgehog or later recommended)
2. **Android SDK**: API 34 (Android 14) with build tools
3. **Java JDK**: Version 17 or higher
4. **Pixel Device or Emulator**: For testing, preferably running Android 12+ or latest beta

## Building the App

### Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the LogiCam directory
4. Wait for Gradle sync to complete
5. Click "Build > Make Project" or press Ctrl+F9 (Cmd+F9 on Mac)

### Using Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

## Testing Guide

### Manual Testing Checklist

#### 1. Initial Setup
- [ ] Install app on device
- [ ] Grant camera permission when prompted
- [ ] Grant audio recording permission
- [ ] Grant notifications permission (Android 13+)
- [ ] Verify foreground service notification appears

#### 2. Camera Initialization
- [ ] App opens without crash
- [ ] Camera preview displays correctly
- [ ] Preview is responsive and smooth
- [ ] Status text shows "Camera ready"

#### 3. Recording Functionality
- [ ] Tap record button
- [ ] Button changes to "Stop Recording" with red background
- [ ] Status shows "Recording..."
- [ ] Toast notification appears
- [ ] Record for 10+ seconds
- [ ] Tap stop button
- [ ] Recording stops successfully
- [ ] Toast shows file saved confirmation

#### 4. File Management
- [ ] Navigate to device storage: /Android/data/com.logicam/files/Movies/LogiCam/
- [ ] Verify video file exists (VID_YYYYMMDD_HHMMSS.mp4)
- [ ] Verify metadata file exists (VID_YYYYMMDD_HHMMSS_metadata.json)
- [ ] Play video file - confirm audio and video quality
- [ ] Check metadata file contains correct information

#### 5. Session Management
- [ ] Record a video
- [ ] Put app in background (home button)
- [ ] Verify notification persists
- [ ] Return to app
- [ ] Start another recording - verify it works
- [ ] Force close camera in device settings
- [ ] Return to app - verify auto-reconnect works

#### 6. Background Upload
- [ ] Complete a recording
- [ ] Verify WorkManager scheduled upload
- [ ] Check logs for upload activity
- [ ] Confirm upload completes (check logs)

#### 7. Error Handling
- [ ] Deny camera permission - verify graceful error
- [ ] Cover camera lens - verify recording continues
- [ ] Fill device storage - verify error handling
- [ ] Disconnect network during upload - verify retry

#### 8. Multi-Session Testing
- [ ] Record multiple videos back-to-back
- [ ] Verify each saves correctly
- [ ] Check no memory leaks (use Android Profiler)
- [ ] Verify session remains stable

### Testing on Android Beta

1. **Install Beta**: Enroll device in Android Beta Program
2. **Basic Functionality**: Repeat all manual tests above
3. **API Compatibility**: Verify no deprecated API warnings
4. **Permission Model**: Test new permission flows
5. **Foreground Service**: Verify service type restrictions

### Automated Testing (Future)

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Troubleshooting

### Build Issues

**Gradle sync fails**
```bash
# Clear Gradle cache
./gradlew clean
rm -rf .gradle
./gradlew build --refresh-dependencies
```

**SDK not found**
- Set ANDROID_HOME environment variable
- Update local.properties with sdk.dir path

### Runtime Issues

**Camera won't open**
- Check permissions in Settings > Apps > LogiCam
- Verify camera not in use by another app
- Restart device

**Recording fails**
- Check available storage space
- Verify write permissions
- Check logs for specific error

**Upload not working**
- Verify network connection
- Check upload endpoint configuration
- Review WorkManager logs

## Logs

View app logs using:
```bash
# Real-time logs
adb logcat -s LogiCam

# Save to file
adb logcat -s LogiCam > logicam_logs.txt

# View app's internal logs
adb shell run-as com.logicam cat files/logicam_log.txt
```

## Performance Monitoring

Use Android Studio Profiler to monitor:
- CPU usage (should be <30% during recording)
- Memory usage (watch for leaks)
- Network activity (during uploads)
- Battery impact

## Known Limitations

1. Upload endpoint is currently a stub - implement your backend
2. Analysis stream configured but not actively used
3. Only back camera supported (easily extendable)
4. Fixed video quality (FHD 30fps)

## Next Steps

1. Configure actual upload endpoint in UploadWorker
2. Add UI for video quality selection
3. Implement analysis stream processing
4. Add multi-camera support
5. Create comprehensive unit tests
