# Healthub Android App - Quick Start Guide


Everything is containerized! You don't need to install Android Studio, SDKs, or any development tools.

### Prerequisites
- Docker installed
- Linux with X11 (for emulator display)

---

## Test the App (Emulator)

Run this single command:

```bash
./run-emulator.sh
```

This will:
1. Build the app (if not already built)
2. Start an Android emulator
3. Install and launch the Healthub app automatically

**First run**: ~5 minutes (downloads emulator)  
**Subsequent runs**: ~30 seconds

---

## Build the App

```bash
docker compose run --rm build
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Install on Temi Robot

### Option 1: USB Cable
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Option 2: WiFi
```bash
adb connect <temi-ip>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Make Changes

1. Edit code in any text editor
2. Rebuild: `docker compose run --rm build`
3. Test: `./run-emulator.sh`
4. Install on Temi

---

## Project Structure

```
android-healthub-main/
├── app/                    # Android app source code
├── Dockerfile              # Build environment
├── docker-compose.yml      # Docker orchestration
├── run-emulator.sh         # One-click emulator
└── README.md              # This file
```

---

## Configuration

Update the OpenHAB server IP in:
`app/src/main/java/org/hwu/care/healthub/DeviceDetailActivity.kt`

```kotlin
private val HARDCODED_OPENHAB_IP = "192.168.2.198"  // Change this
```

Then rebuild.

---

## Troubleshooting

**Emulator won't start:**
- Ensure KVM is enabled: `ls /dev/kvm`
- Run: `xhost +local:docker`

**Build fails:**
- Clean build: `docker compose run --rm build ./gradlew clean assembleDebug`

**App crashes:**
- Check OpenHAB IP is correct
- Ensure OpenHAB server is running
- Check logs: `adb logcat | grep Healthub`
