# Manual Deployment Guide for Temi Robot

## Problem
The automated deployment script cannot connect to the Temi robot via ADB.

## Solution Options

### Option 1: Enable ADB on Temi Robot (Recommended)

1. On the Temi robot, go to **Settings** → **About**
2. Tap on **Build Number** 7 times to enable Developer Options
3. Go back to **Settings** → **Developer Options**
4. Enable **USB Debugging**
5. Enable **ADB over Network** (if available)
6. Note the IP address shown (should be `192.168.2.115`)

Then try the deployment script again:
```bash
./deploy_to_temi.sh
```

### Option 2: Manual ADB Connection

Try connecting with different commands:

```bash
# Try connecting without port
adb connect 192.168.2.115

# Or try default ADB port
adb connect 192.168.2.115:5555

# Check if device is connected
adb devices

# If connected, install manually
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: USB Installation

1. Connect your laptop to the Temi robot via USB-C cable
2. Enable USB debugging on Temi (see Option 1)
3. Run:
```bash
adb devices  # Should show the device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Option 4: Manual File Transfer

1. Build the APK:
```bash
./gradlew assembleDebug
```

2. The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

3. Transfer the APK to the Temi robot using one of these methods:
   - **USB Drive**: Copy APK to USB drive, plug into Temi, install via File Manager
   - **Cloud**: Upload to Google Drive/Dropbox, download on Temi, install
   - **Email**: Email the APK to yourself, open on Temi, install
   - **HTTP**: Start a local web server and download on Temi:
     ```bash
     cd app/build/outputs/apk/debug
     python3 -m http.server 8080
     # On Temi browser, go to: http://192.168.2.150:8080/app-debug.apk
     ```

4. On the Temi robot, tap the downloaded APK file to install it

### Option 5: Check Temi's ADB Port

The Temi might be using a different port. Try:

```bash
# Scan for open ports on the Temi
nmap -p 5555,5037,5556,5557 192.168.2.115

# Or try connecting to different ports
adb connect 192.168.2.115:5037
adb connect 192.168.2.115:5556
```

## Verify Installation

After installing via any method, verify the app is running the new version by checking the logs:

```bash
# If ADB is working
adb -s 192.168.2.115:5555 logcat | grep "MainActivity"

# Or check the app version in Settings → Apps → HealthHub
```

## Current Issue

The voice dialog opening when clicking "I'm ready" should be fixed in the latest build. Once you successfully deploy the new APK, the issue should be resolved.
