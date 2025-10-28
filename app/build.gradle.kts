plugins {
    id("com.android.application")
    // AGP 9.0 has built-in Kotlin support, no separate plugin needed
}

android {
    namespace = "com.logicam"
    compileSdk = 35  // Updated to Android 15

    defaultConfig {
        applicationId = "com.logicam"
        minSdk = 31  // Pixel devices with Android 12+
        targetSdk = 35  // Updated to Android 15
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
    
    // CameraX - Updated to latest versions compatible with API 35
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-video:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.camera:camera-extensions:1.4.0")
    
    // Lifecycle - Updated for compatibility
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    
    // WorkManager - Updated for Kotlin 2.2
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    
    // Coroutines - Updated to version compatible with Kotlin 2.2
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // Permissions
    implementation("androidx.activity:activity-ktx:1.9.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
