# Android 15 Feature Implementation Guide

## Overview

LogiCam now supports Android 15 (API 35) features specifically designed for camera and connectivity applications.

## New Features Implemented

### 1. Low Light Boost Mode (Camera Enhancement)

Android 15 introduces a new auto-exposure mode that significantly improves camera performance in low-light conditions.

#### What is Low Light Boost?

Low Light Boost provides:
- **Enhanced continuous preview stream** with brightness boost in low light
- **Superior to Night mode** - works during preview, not just capture
- **Brightened viewfinder** for better framing in dark environments
- **Low-light video recording** with improved quality

#### Implementation

**Class**: `LowLightBoostHelper.kt`

**Check Support**:
```kotlin
val isSupported = LowLightBoostHelper.isLowLightBoostSupported(context, cameraId)
```

**Apply to Capture Request**:
```kotlin
val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
LowLightBoostHelper.applyLowLightBoost(builder, context, cameraId)
```

**Monitor Active State**:
```kotlin
override fun onCaptureCompleted(result: CaptureResult) {
    if (LowLightBoostHelper.isLowLightBoostActive(result)) {
        // Show UI indicator (e.g., moon icon)
    }
}
```

#### Integration with Camera2FallbackManager

To use Low Light Boost with the Camera2 fallback:

```kotlin
// In Camera2FallbackManager.kt, when creating capture request:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
    LowLightBoostHelper.applyLowLightBoost(captureRequestBuilder, context, cameraId)
}
```

#### Fallback for Older Devices

For devices that don't support the native API 35 feature, Google provides a fallback via Google Play Services using `LowLightBoostClient`. This can be added as an optional dependency:

```gradle
implementation("com.google.android.gms:play-services-camera:1.0.0")
```

### 2. Satellite Connectivity Awareness

Android 15 adds platform-level support for satellite connectivity detection.

#### What is Satellite Connectivity?

- Detects when device is connected **only** to Non-Terrestrial Network (NTN)
- Allows apps to adapt behavior when satellite-only connection is active
- No arbitrary IP data over satellite - this is for **awareness**, not communication

#### Implementation

**Class**: `SatelliteConnectivityMonitor.kt`

**Create Monitor**:
```kotlin
val satelliteMonitor = SatelliteConnectivityMonitor(context)
```

**Check Current Status**:
```kotlin
val isSatelliteOnly = satelliteMonitor.checkSatelliteStatus()
if (isSatelliteOnly) {
    // Disable high-bandwidth features
    // Show user notification
}
```

**Monitor Status Changes**:
```kotlin
// Start monitoring with an executor
satelliteMonitor.startMonitoring(ContextCompat.getMainExecutor(context))

// Observe state changes
lifecycleScope.launch {
    satelliteMonitor.isSatelliteOnly.collect { isSatellite ->
        if (isSatellite) {
            // Disable uploads, streaming, etc.
            updateUI("Satellite-only mode: Limited connectivity")
        } else {
            // Re-enable features
            updateUI("Connected to terrestrial network")
        }
    }
}

// Stop monitoring when done
satelliteMonitor.stopMonitoring()
```

**Get Status Message**:
```kotlin
val statusMessage = satelliteMonitor.getConnectivityStatusMessage()
// Shows user-friendly message about connectivity
```

#### Permission Requirements

Satellite connectivity monitoring requires:
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

This permission is already added to the manifest.

#### Recommended App Behavior

When satellite-only connection is detected:
1. **Disable Background Uploads**: `AppConfig.setAutoUploadEnabled(context, false)`
2. **Pause Video Streaming**: If streaming to server
3. **Show User Notification**: Inform user of limited connectivity
4. **Queue Operations**: Save upload tasks for later when terrestrial network returns
5. **Reduce Data Usage**: Switch to low-bandwidth operations only

Example in MainActivity:
```kotlin
private val satelliteMonitor by lazy { SatelliteConnectivityMonitor(this) }

override fun onResume() {
    super.onResume()
    satelliteMonitor.startMonitoring(ContextCompat.getMainExecutor(this))
    
    lifecycleScope.launch {
        satelliteMonitor.isSatelliteOnly.collect { isSatellite ->
            if (isSatellite) {
                // Disable upload
                uploadManager.pauseUploads()
                updateStatus("Satellite mode: Uploads paused")
            } else {
                // Resume normal operation
                uploadManager.resumeUploads()
                updateStatus("Network restored")
            }
        }
    }
}

override fun onPause() {
    super.onPause()
    satelliteMonitor.stopMonitoring()
}
```

## Android 15 Behavioral Changes

### Foreground Service Changes

Android 15 enforces stricter foreground service type restrictions. LogiCam's SessionManagerService is correctly configured:

```xml
<service
    android:name=".session.SessionManagerService"
    android:foregroundServiceType="camera" />
```

This ensures the service can run in the foreground for camera recording.

### Background Activity Launch Restrictions

Starting activities from the background is heavily restricted in Android 15. LogiCam handles this by:
- Using notifications for user interaction
- Launching activities only from user actions
- Not launching activities from services or broadcast receivers

### 16KB Page Size Support

Android 15 introduces 16KB memory page support on some devices. LogiCam doesn't use native code (.so libraries) directly, so this doesn't require changes. However, any future NDK integration must be built with the latest NDK to support 16KB pages.

## Testing Android 15 Features

### Testing Low Light Boost

1. **Check Support**: On Android 15+ Pixel device
2. **Enable Feature**: Use Camera2FallbackManager with Low Light Boost enabled
3. **Test in Low Light**: Cover camera lens partially or test in dark room
4. **Verify Boost**: Check if preview brightens
5. **Monitor State**: Log when boost becomes active

### Testing Satellite Connectivity

1. **Emulator Testing**: Android emulator doesn't support satellite simulation
2. **Device Testing**: Requires actual satellite-capable device
3. **Permission Testing**: Test with/without READ_PHONE_STATE permission
4. **Fallback Testing**: Test on Android 14 and below (should gracefully degrade)

### Testing on Android 16 (API 36)

The build system supports API 36. To test:

```kotlin
android {
    compileSdk = 36
    defaultConfig {
        targetSdk = 36
    }
}
```

**Note**: AGP 9.0.0-alpha11 supports up to API 36.

## Migration Checklist

- [x] Gradle 9.0 installed
- [x] AGP 9.0 configured
- [x] compileSdk = 35
- [x] targetSdk = 35
- [x] Low Light Boost helper implemented
- [x] Satellite connectivity monitor implemented
- [x] READ_PHONE_STATE permission added
- [x] Foreground service type correctly declared
- [ ] Low Light Boost integrated into Camera2FallbackManager (optional)
- [ ] Satellite monitor integrated into MainActivity (optional)
- [ ] Tested on Android 15 device
- [ ] Behavioral changes validated

## References

- [Android 15 Camera Features](https://developer.android.com/about/versions/15/features#camera)
- [Low Light Boost Documentation](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY)
- [Satellite Connectivity](https://developer.android.com/about/versions/15/features#satellite)
- [Android 15 Behavioral Changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Foreground Service Types](https://developer.android.com/about/versions/15/changes/fgs-types-required)

## Next Steps

1. **Optional Integration**: Integrate Low Light Boost and Satellite monitoring into existing managers
2. **UI Updates**: Add UI indicators for Low Light Boost active state
3. **User Notifications**: Show notifications when satellite-only mode is detected
4. **Testing**: Test on Android 15 Pixel devices
5. **Play Store**: Update app listing to highlight Android 15 features
