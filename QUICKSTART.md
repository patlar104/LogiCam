# LogiCam Quick Start Guide

Get up and running with LogiCam in 5 minutes.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK with API 34
- JDK 17 or higher
- Pixel device or emulator running Android 12+

## Step 1: Clone and Open Project

```bash
git clone https://github.com/patlar104/LogiCam.git
cd LogiCam
```

Open Android Studio and select "Open an Existing Project", then navigate to the LogiCam directory.

## Step 2: Gradle Sync

Wait for Gradle to sync dependencies. This may take a few minutes on first run.

If sync fails:
- Check your Android SDK installation
- Ensure JDK 17 is configured
- Try: `Tools > SDK Manager` and install API 34

## Step 3: Connect Device

### Option A: Physical Device
1. Enable Developer Options on your Pixel device
2. Enable USB Debugging
3. Connect device via USB
4. Accept debugging prompt on device

### Option B: Emulator
1. Open AVD Manager: `Tools > Device Manager`
2. Create new device: Pixel 7, API 34
3. Start emulator

## Step 4: Run App

1. Select your device from the device dropdown
2. Click the green "Run" button or press Shift+F10
3. App will build, install, and launch

## Step 5: Grant Permissions

When the app opens:
1. Tap "Allow" for camera permission
2. Tap "Allow" for microphone permission
3. Tap "Allow" for notifications (Android 13+)

## Step 6: Record Video

1. You should see camera preview
2. Tap the large circular button
3. Button turns red and shows "Stop Recording"
4. Record for a few seconds
5. Tap button again to stop
6. Toast notification confirms video saved

## Verify Recording

Check that video was saved:

```bash
# Via ADB
adb shell ls -l /sdcard/Android/data/com.logicam/files/Movies/LogiCam/

# Pull file to view
adb pull /sdcard/Android/data/com.logicam/files/Movies/LogiCam/VID_*.mp4
```

## Configuration

### Change Video Quality

```kotlin
// In MainActivity.onCreate() or via settings
AppConfig.setVideoQuality(this, Quality.UHD)  // 4K
```

### Disable Auto-Upload

```kotlin
AppConfig.setAutoUploadEnabled(this, false)
```

### WiFi-Only Uploads

```kotlin
AppConfig.setUploadOnlyWifi(this, true)
```

## Troubleshooting

### Camera won't open
- Check permissions in Settings > Apps > LogiCam
- Ensure no other app is using camera
- Try restarting the app

### Build fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Emulator issues
- Use Pixel 7 with API 33 or 34
- Enable hardware acceleration in AVD settings
- Ensure camera is enabled in AVD config

### Permission denied
- Uninstall and reinstall app
- Check that permissions are declared in manifest
- Grant permissions in Settings manually

## What's Next?

1. **Read Documentation**
   - Architecture: `ARCHITECTURE.md`
   - Testing: `BUILD_AND_TEST.md`
   - Security: `SECURITY.md`

2. **Implement Upload Backend**
   - Edit `upload/UploadWorker.kt`
   - Replace `uploadFile()` placeholder
   - Add your endpoint URL

3. **Customize**
   - Add settings UI
   - Customize colors/theme
   - Add analytics

4. **Test**
   - Follow `BUILD_AND_TEST.md` checklist
   - Test on multiple devices
   - Test error scenarios

## Development Tips

### View Logs
```bash
# Real-time logs
adb logcat -s LogiCam

# App's internal logs
adb shell run-as com.logicam cat files/logicam_log.txt
```

### Debug Recording
Set breakpoints in:
- `RecordingManager.startRecording()`
- `CameraXCaptureManager.initialize()`
- `MainActivity.startRecording()`

### Test Session Recovery
1. Start recording
2. Go to Settings > Apps > LogiCam > Force Stop
3. Reopen app
4. Check notification for reconnection status

### Profile Performance
1. Open Android Profiler: `View > Tool Windows > Profiler`
2. Start recording
3. Monitor CPU, memory, and network
4. Look for memory leaks or CPU spikes

## Key Files to Know

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Main UI and coordination |
| `CameraXCaptureManager.kt` | Camera initialization |
| `RecordingManager.kt` | Recording logic |
| `SessionManagerService.kt` | Background session |
| `UploadWorker.kt` | Upload implementation |
| `AppConfig.kt` | Configuration |

## Support

- **Documentation**: See `README.md` for all docs
- **Issues**: Check existing GitHub issues
- **Architecture**: Questions? See `ARCHITECTURE.md`

## Quick Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean build

# View installed APK info
adb shell pm list packages | grep logicam
adb shell dumpsys package com.logicam
```

## Success Checklist

- [ ] App builds without errors
- [ ] App installs on device
- [ ] Camera preview displays
- [ ] Recording starts and stops
- [ ] Video file is created
- [ ] Metadata JSON is generated
- [ ] Foreground notification appears
- [ ] Logs show no errors

If all checked, you're ready to develop! 🎉

## Next Steps

1. Follow complete testing guide: `BUILD_AND_TEST.md`
2. Review security recommendations: `SECURITY.md`
3. Implement upload backend
4. Add your custom features
5. Deploy to production

---

**Happy Coding!** 📹
