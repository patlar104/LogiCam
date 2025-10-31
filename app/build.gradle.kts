plugins {
    id("com.android.application")
    // AGP 9.0 has built-in Kotlin support, no separate plugin needed
}

// Updated for Android 16.1 QPR
android {
    namespace = "com.logicam"
    compileSdk = 36  // Updated for Android 16.1 QPR

    defaultConfig {
        applicationId = "com.logicam"
        minSdk = 31  // Pixel devices with Android 12+
        targetSdk = 36  // Updated for Android 16.1 QPR
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // Enable BuildConfig generation (disabled by default in AGP 9.0)
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Core Android - Updated for Kotlin 2.2 compatibility
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // ===== Updated for Android 16.1 QPR =====
    // CameraX - Updated to 1.5.1 for API 36 compatibility
    implementation("androidx.camera:camera-core:1.5.1")
    implementation("androidx.camera:camera-camera2:1.5.1")
    implementation("androidx.camera:camera-lifecycle:1.5.1")
    implementation("androidx.camera:camera-video:1.5.1")
    implementation("androidx.camera:camera-view:1.5.1")
    implementation("androidx.camera:camera-extensions:1.5.1")
    
    // Lifecycle - Updated to 2.9.4 for API 36 compatibility
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-service:2.9.4")
    
    // WorkManager - Updated to 2.11.0 for API 36 compatibility
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    
    // Coroutines - Updated to 1.10.2 (latest stable)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // ===== End Android 16.1 QPR Updates =====
    
    // Permissions
    implementation("androidx.activity:activity-ktx:1.9.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
