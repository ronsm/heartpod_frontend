# Android App Rebuild Instructions

## Problem Summary
The Android app on Temi is trying to connect to `192.168.2.198` (old Pi IP) instead of `192.168.2.150` (current Pi IP).

## What Was Fixed in Source Code
Updated 6 hardcoded IP references:
1. `PiApiImpl.kt:23` → `192.168.2.150` (OpenHAB REST API)
2. `DeviceDetailActivity.kt:42` → `192.168.2.150` (Hardcoded fallback)
3. `MainViewModel.kt:85` → `192.168.2.150` (SharedPreferences fallback)
4. `MainActivity.kt:42` → Already correct
5. `MainViewModel.kt:43` → Already correct
6. `MqttClientManager.kt:12` → Already correct

## Build Issue
The `app/build` directory contains root-owned files from a previous build, preventing Gradle from cleaning/rebuilding.

**Error:**
```
Unable to delete directory '/home/kwalker96/Downloads/android-healthub-main/app/build'
```

## Solution

### Option 1: Force Rebuild (Recommended)
```bash
cd /home/kwalker96/Downloads/android-healthub-main
sudo rm -rf app/build .gradle
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleDebug
./deploy_to_temi.sh
```

### Option 2: Use Android Studio
1. Open Android Studio
2. File → Open → `/home/kwalker96/Downloads/android-healthub-main`
3. Build → Clean Project
4. Build → Rebuild Project
5. APK will be in `app/build/outputs/apk/debug/app-debug.apk`
6. Run `./deploy_to_temi.sh`

## Verification After Deployment
```bash
adb logcat | grep "192.168.2"
```
Should show connections to `.150`, not `.198`.
