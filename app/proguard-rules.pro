# Add project specific ProGuard rules here.

# ========== LogiCam ProGuard Rules ==========

# Keep application class
-keep class com.logicam.LogiCamApplication { *; }

# Keep all Activities, Services, and BroadcastReceivers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# CameraX - Essential for camera functionality
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep camera managers and capture classes
-keep class com.logicam.capture.** { *; }
-keep class com.logicam.session.** { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep StateFlow and Flow classes (Kotlin Coroutines)
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin Serialization (if used in future)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep sealed classes for proper functionality
-keep class com.logicam.ui.MainViewModel$CameraUiState { *; }
-keep class com.logicam.ui.MainViewModel$CameraUiState$* { *; }
-keep class com.logicam.util.ErrorHandler$CameraError { *; }
-keep class com.logicam.util.ErrorHandler$CameraError$* { *; }

# Keep data classes used in UI
-keep class com.logicam.ui.gallery.VideoItem { *; }

# Keep AppContainer for dependency injection
-keep class com.logicam.AppContainer { *; }
-keepclassmembers class com.logicam.AppContainer {
    public <methods>;
}

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Remove logging in release builds (optional - comment out for debugging)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# Coil image loading library
-keep class coil.** { *; }
-dontwarn coil.**

# Preferences
-keep class androidx.preference.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ========== Android Default Rules ==========

# Keep annotations
-keepattributes *Annotation*

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep setters in Views for XML inflation
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# Keep Activity lifecycle methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# R8 full mode compatibility
-allowaccessmodification
-repackageclasses

