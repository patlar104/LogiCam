# CI/CD Setup Guide

## Overview

LogiCam now includes a complete GitHub Actions CI/CD workflow for automated building and testing.

## GitHub Actions Workflow

**Location**: `.github/workflows/android.yml`

The workflow automatically triggers on:
- Push to `main`, `develop`, or `copilot/**` branches
- Pull requests to `main` or `develop`
- Manual dispatch (via Actions tab)

## What the CI Does

### Build Job
1. **Environment Setup**
   - Ubuntu latest runner
   - JDK 17 (Temurin distribution)
   - Android SDK with API 34
   - Gradle 8.2 with caching

2. **Build Process**
   - Accepts Android SDK licenses
   - Installs required SDK components
   - Runs full Gradle build
   - Executes unit tests
   - Builds debug APK
   - Attempts release APK (optional)

3. **Artifacts**
   - Debug APK: `logicam-debug-apk` (30-day retention)
   - Release APK: `logicam-release-apk` (30-day retention, if successful)

### Lint Job
1. **Code Quality**
   - Runs Android Lint checks
   - Generates lint reports
   - Uploads reports as artifacts (7-day retention)

## Downloading APKs

### From GitHub Actions

1. Navigate to your repository on GitHub
2. Click the **Actions** tab
3. Select the latest successful workflow run
4. Scroll to **Artifacts** section
5. Download `logicam-debug-apk.zip`
6. Extract the ZIP file to get `app-debug.apk`

### Installation Methods

#### Method 1: ADB (Recommended)
```bash
# Connect device via USB
adb devices

# Install APK
adb install -r app-debug.apk

# If already installed, use -r to replace
adb install -r app-debug.apk
```

#### Method 2: Manual Transfer
1. Transfer APK to device via:
   - USB cable (file transfer)
   - Cloud storage (Google Drive, Dropbox)
   - Email
2. On device, enable "Install from unknown sources":
   - Settings > Security > Unknown sources
   - Or Settings > Apps > Special app access > Install unknown apps
3. Tap the APK file to install

#### Method 3: Wireless ADB
```bash
# Enable wireless debugging on device
# Settings > Developer options > Wireless debugging

# Connect via IP
adb connect <device-ip>:5555

# Install
adb install -r app-debug.apk
```

## Build Requirements

The CI workflow installs:
- Android SDK Platform 34 (Android 14)
- Build Tools 34.0.0
- Platform Tools
- Android SDK extras

## Build Output

Successful builds produce:
- **Debug APK**: ~15MB, ready for testing
- **Release APK**: Requires signing configuration
- **Lint Reports**: HTML reports for code quality

## Local Build

To build locally (matching CI):

```bash
# Set Android SDK location
export ANDROID_HOME=/path/to/android-sdk

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting CI

### Build Fails

**Check the logs:**
1. Go to Actions tab
2. Click the failed run
3. Expand the failed step
4. Review error messages

**Common issues:**
- Dependency version conflicts
- SDK component missing
- Compilation errors
- Test failures

### APK Not Generated

If the build succeeds but no APK:
1. Check if the `assembleDebug` task completed
2. Verify artifact upload step succeeded
3. Ensure retention period hasn't expired

### Lint Failures

Lint failures don't block APK generation. To view:
1. Download lint-report artifact
2. Open HTML file in browser
3. Fix reported issues

## CI Performance

Typical execution times:
- **Fresh build**: 3-5 minutes
- **Cached build**: 1-2 minutes
- **Lint job**: 1-2 minutes

## Customization

### Change Trigger Branches

Edit `.github/workflows/android.yml`:
```yaml
on:
  push:
    branches: [ your-branch-name ]
```

### Add More SDK Components

```yaml
- name: Install Android SDK components
  run: |
    sdkmanager "platforms;android-34" "build-tools;34.0.0"
    sdkmanager "system-images;android-34;google_apis;x86_64"
```

### Increase Artifact Retention

```yaml
- name: Upload debug APK
  uses: actions/upload-artifact@v4
  with:
    retention-days: 90  # Change from 30 to 90
```

### Add Signing Configuration

For release builds:

1. Generate keystore locally
2. Add secrets to GitHub:
   - Settings > Secrets > Actions
   - Add: KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
3. Update workflow to decode and use keystore

## Monitoring

### Build Status Badge

Add to README.md:
```markdown
![Build Status](https://github.com/patlar104/LogiCam/workflows/Android%20CI/badge.svg)
```

### Email Notifications

GitHub sends notifications on:
- Build failures
- First success after failures
- Configure in Settings > Notifications

## Best Practices

1. **Always test locally first** before pushing
2. **Check build logs** if CI fails
3. **Download APKs promptly** before retention expires
4. **Review lint reports** regularly
5. **Keep dependencies updated** for security

## Advanced Features

### Matrix Builds

Test on multiple API levels:
```yaml
strategy:
  matrix:
    api-level: [31, 32, 33, 34]
```

### Parallel Jobs

Run tests in parallel:
```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
  integration-tests:
    runs-on: ubuntu-latest
```

### Caching

Gradle cache is automatically enabled:
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    cache: gradle  # Caches ~/.gradle
```

## Support

For CI/CD issues:
1. Check GitHub Actions documentation
2. Review workflow logs
3. Verify local build works
4. Check Android SDK versions

## Summary

The CI/CD workflow provides:
- ✅ Automated building on every push
- ✅ APK artifacts ready for download
- ✅ Lint checks for code quality
- ✅ Fast builds with caching
- ✅ 30-day artifact retention
- ✅ Manual workflow dispatch

All builds are reproducible and match the local development environment.
