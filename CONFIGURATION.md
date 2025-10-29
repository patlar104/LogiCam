# Configuration and Settings

LogiCam includes a configuration system for customizing app behavior. Settings are stored in SharedPreferences and can be extended with a settings UI.

## Available Settings

### Video Quality
**Key**: `video_quality`  
**Type**: String  
**Default**: `FHD` (1080p)  
**Options**:
- `UHD` - 4K (3840x2160)
- `FHD` - Full HD (1920x1080) - Recommended
- `HD` - HD (1280x720)
- `SD` - Standard (720x480)

**Usage**:
```kotlin
// Get current quality
val quality = AppConfig.getVideoQuality(context)

// Set quality
AppConfig.setVideoQuality(context, Quality.FHD)
```

### Upload Only on WiFi
**Key**: `upload_only_wifi`  
**Type**: Boolean  
**Default**: `true`  
**Description**: When enabled, uploads only happen when connected to WiFi. When disabled, uploads can occur on any network connection (mobile data).

**Usage**:
```kotlin
// Check setting
val wifiOnly = AppConfig.isUploadOnlyWifi(context)

// Enable WiFi-only uploads
AppConfig.setUploadOnlyWifi(context, true)
```

### Auto Upload
**Key**: `auto_upload`  
**Type**: Boolean  
**Default**: `true`  
**Description**: When enabled, recordings are automatically scheduled for upload after completion. When disabled, recordings remain local until manually uploaded.

**Usage**:
```kotlin
// Check if auto-upload is enabled
val autoUpload = AppConfig.isAutoUploadEnabled(context)

// Disable auto-upload
AppConfig.setAutoUploadEnabled(context, false)
```

### Max Reconnect Attempts
**Key**: `max_reconnect_attempts`  
**Type**: Int  
**Default**: `5`  
**Description**: Maximum number of times the SessionManager will attempt to reconnect the camera session after a failure.

**Usage**:
```kotlin
// Get max attempts
val maxAttempts = AppConfig.getMaxReconnectAttempts(context)

// Set max attempts
AppConfig.setMaxReconnectAttempts(context, 3)
```

## Future Settings (Extensible)

The configuration system is designed to be easily extended. Consider adding:

### Camera Settings
- Front/back camera selection
- Flash mode (auto, on, off)
- Image stabilization
- HDR mode

### Recording Settings
- Frame rate (24, 30, 60 fps)
- Audio quality
- Video codec (H.264, H.265)
- Maximum recording duration

### Upload Settings
- Upload endpoint URL
- Authentication token
- Batch upload size
- Upload priority

### Storage Settings
- Maximum storage usage
- Auto-delete after upload
- Compression settings
- Local backup location

## Implementing a Settings UI

To add a settings screen, create a new Activity with a PreferenceFragment:

```kotlin
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
```

Create `res/xml/preferences.xml`:
```xml
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
    
    <PreferenceCategory android:title="Video Settings">
        <ListPreference
            android:key="video_quality"
            android:title="Video Quality"
            android:summary="Choose recording quality"
            android:entries="@array/quality_entries"
            android:entryValues="@array/quality_values"
            android:defaultValue="FHD" />
    </PreferenceCategory>
    
    <PreferenceCategory android:title="Upload Settings">
        <SwitchPreference
            android:key="auto_upload"
            android:title="Auto Upload"
            android:summary="Automatically upload recordings"
            android:defaultValue="true" />
        
        <SwitchPreference
            android:key="upload_only_wifi"
            android:title="WiFi Only"
            android:summary="Only upload on WiFi"
            android:defaultValue="true"
            android:dependency="auto_upload" />
    </PreferenceCategory>
    
    <PreferenceCategory android:title="Advanced">
        <SeekBarPreference
            android:key="max_reconnect_attempts"
            android:title="Max Reconnect Attempts"
            android:summary="Maximum camera reconnection attempts"
            android:min="1"
            android:max="10"
            android:defaultValue="5" />
    </PreferenceCategory>
    
</PreferenceScreen>
```

## Configuration Best Practices

1. **Provide Sensible Defaults**: All settings have reasonable defaults for most users
2. **Validate Input**: Ensure configuration values are within acceptable ranges
3. **Apply Immediately**: Configuration changes should take effect without app restart when possible
4. **Document Changes**: Log configuration changes for debugging
5. **Backup Settings**: Consider syncing settings to cloud or including in exports

## Direct SharedPreferences Access

For advanced use cases, you can directly access SharedPreferences:

```kotlin
val prefs = context.getSharedPreferences("logicam_prefs", Context.MODE_PRIVATE)

// Read value
val quality = prefs.getString("video_quality", "FHD")

// Write value
prefs.edit().putString("video_quality", "UHD").apply()

// Listen for changes
prefs.registerOnSharedPreferenceChangeListener { _, key ->
    when (key) {
        "video_quality" -> {
            // Handle quality change
        }
    }
}
```

## Configuration File Location

Settings are stored in:
```
/data/data/com.logicam/shared_prefs/logicam_prefs.xml
```

To export/backup settings:
```bash
adb pull /data/data/com.logicam/shared_prefs/logicam_prefs.xml
```

To import/restore settings:
```bash
adb push logicam_prefs.xml /data/data/com.logicam/shared_prefs/
```

## Testing Configuration

Test different configurations to ensure app stability:

```kotlin
@Test
fun `test video quality configuration`() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    // Test each quality setting
    AppConfig.setVideoQuality(context, Quality.FHD)
    assertEquals(Quality.FHD, AppConfig.getVideoQuality(context))
    
    AppConfig.setVideoQuality(context, Quality.HD)
    assertEquals(Quality.HD, AppConfig.getVideoQuality(context))
}
```
