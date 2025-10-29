# Quick Reference: Install LogiCam APK

## Download APK from GitHub Actions

1. **Go to GitHub Actions**
   - Visit: https://github.com/patlar104/LogiCam/actions
   - Click on the latest green checkmark workflow run
   - Scroll to "Artifacts" section at the bottom
   - Click "logicam-debug-apk" to download

2. **Extract the ZIP**
   ```bash
   unzip logicam-debug-apk.zip
   # This creates: app-debug.apk
   ```

## Install on Pixel Device

### Option 1: Using ADB (Fastest)

```bash
# 1. Connect device via USB and enable USB debugging
# 2. Verify connection
adb devices

# 3. Install APK
adb install -r app-debug.apk

# 4. Launch app
adb shell am start -n com.logicam/.ui.MainActivity
```

### Option 2: Manual Installation

1. Transfer APK to device:
   ```bash
   adb push app-debug.apk /sdcard/Download/
   ```

2. On device:
   - Open Files or Downloads app
   - Tap on `app-debug.apk`
   - Tap "Install"
   - Grant permissions when prompted

### Option 3: Wireless Installation

```bash
# 1. Enable wireless debugging on device:
#    Settings > Developer options > Wireless debugging

# 2. Connect via IP (shown in wireless debugging settings)
adb connect 192.168.1.XXX:XXXXX

# 3. Install
adb install -r app-debug.apk
```

## Grant Permissions

After installation, the app needs:

### Required Permissions
- **Camera** - For recording video
- **Microphone** - For audio recording
- **Notifications** - For foreground service (Android 13+)

### Grant via ADB (Optional)
```bash
adb shell pm grant com.logicam android.permission.CAMERA
adb shell pm grant com.logicam android.permission.RECORD_AUDIO
adb shell pm grant com.logicam android.permission.POST_NOTIFICATIONS
```

## Test the App

### Basic Test
```bash
# Launch app
adb shell am start -n com.logicam/.ui.MainActivity

# Grant permissions when prompted
# Tap the large red button to start recording
# Tap again to stop

# View logs
adb logcat -s LogiCam
```

### Check Recorded Videos
```bash
# List videos
adb shell ls -lh /sdcard/Android/data/com.logicam/files/Movies/LogiCam/

# Pull video to computer
adb pull /sdcard/Android/data/com.logicam/files/Movies/LogiCam/VID_*.mp4 ./

# Check metadata
adb shell cat /sdcard/Android/data/com.logicam/files/Movies/LogiCam/VID_*_metadata.json
```

## Troubleshooting

### APK Install Fails
```bash
# Uninstall existing version first
adb uninstall com.logicam

# Then reinstall
adb install app-debug.apk
```

### App Crashes on Start
```bash
# View crash logs
adb logcat -s AndroidRuntime

# View app logs
adb logcat -s LogiCam
```

### Permissions Not Working
```bash
# Check current permissions
adb shell dumpsys package com.logicam | grep permission

# Reset app
adb shell pm clear com.logicam

# Reinstall
adb install -r app-debug.apk
```

## Build Locally (Alternative)

If you prefer to build locally:

```bash
# 1. Clone repository
git clone https://github.com/patlar104/LogiCam.git
cd LogiCam

# 2. Set Android SDK
export ANDROID_HOME=/path/to/android-sdk

# 3. Build
./gradlew assembleDebug

# 4. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Useful ADB Commands

```bash
# View app info
adb shell dumpsys package com.logicam

# Clear app data
adb shell pm clear com.logicam

# Force stop
adb shell am force-stop com.logicam

# Uninstall
adb uninstall com.logicam

# View real-time logs
adb logcat -s LogiCam

# Take screenshot
adb exec-out screencap -p > screenshot.png

# Record screen
adb shell screenrecord /sdcard/demo.mp4
```

## App Storage Locations

```bash
# Videos
/sdcard/Android/data/com.logicam/files/Movies/LogiCam/

# Logs
/data/data/com.logicam/files/logicam_log.txt

# Shared preferences
/data/data/com.logicam/shared_prefs/logicam_prefs.xml

# Pending uploads
/data/data/com.logicam/files/pending_uploads/
```

## Support

For issues:
1. Check CI_SETUP.md for detailed documentation
2. View BUILD_AND_TEST.md for testing procedures
3. Check GitHub Issues tab
4. Review app logs with `adb logcat -s LogiCam`

---

**Quick Summary:**
1. Download `logicam-debug-apk.zip` from GitHub Actions
2. Extract to get `app-debug.apk`
3. Run: `adb install -r app-debug.apk`
4. Launch and test!
