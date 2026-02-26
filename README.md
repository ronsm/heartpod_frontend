# HeartPod Frontend

Android Compose UI for the HeartPod self-screening health check system, designed to run on a [Temi robot](https://www.robotemi.com/). The app displays screens driven by a remote Python backend over WebSocket, and sends user button actions back to the backend.

## Architecture

```
Backend (Python/WebSocket)  ←──WebSocket──→  Android App (Jetpack Compose)
         │                                              │
    page_id + data                              renders screen
    tts utterances                              speaks via Temi
    receives actions                            sends user actions
```

- The backend is the source of truth for which screen to show
- The app maintains a persistent WebSocket connection and re-renders whenever the backend pushes a new `state` message
- User button presses are sent back as `action` messages over the same connection
- The Temi SDK is used for kiosk mode and TTS on real hardware — gracefully degraded on emulator

## Screens

| Page ID | Screen | Description |
|---------|--------|-------------|
| 01 | Idle | Waiting for a user to start |
| 02 | Welcome | Introduction and consent |
| 03–05 | Questionnaire | Lifestyle questions (smoking, exercise, alcohol) |
| 06 | Measure Intro | Transition to measurement phase |
| 07, 10, 13 | Device Instruction | Instructions for oximeter / BP cuff / scale |
| 08, 11, 14 | Loading | "Taking reading..." holding screen |
| 09, 12, 15 | Reading Display | Shows the recorded measurement value |
| 16 | Recap | Session summary |
| 17 | Sorry / Error | Retry prompt on failed reading |

## Project Structure

```
app/src/main/java/org/hwu/care/healthub/
├── MainActivity.kt          # Entry point, WebSocket setup, screen routing, TTS wiring
├── AppState.kt              # Shared UI state (pageId + data map)
├── comms/
│   ├── CommsClient.kt       # Transport interface
│   └── WebSocketClient.kt   # WebSocket connection: state push, TTS, action send
├── core/
│   ├── TemiController.kt    # TTS/navigation interface
│   └── TemiControllerImpl.kt# Temi SDK implementation (Robot.TtsListener)
└── ui/screens/              # One Composable per screen
```

## Running in the Emulator

### Prerequisites

- Android Studio (Hedgehog or newer)
- An AVD configured as a tablet or landscape device (the Temi screen is 1280×800px)
- The Python backend running on the same machine

### 1. Start the backend

```bash
cd heartpod_backend
python main.py --dummy --no-printer --no-listen
```

The WebSocket server starts on port 8000. The `--dummy` flag uses simulated sensor data so no BLE hardware is needed.

### 2. Check the backend URL

In `MainActivity.kt`, confirm the backend URL is set for the emulator:

```kotlin
const val BACKEND_URL = "ws://10.0.2.2:8000"
```

`10.0.2.2` is the Android emulator's alias for the host machine's loopback interface.

### 3. Build and run

Open the project in Android Studio and press **Run** (`Shift+F10`). Select your AVD when prompted.

## Running on a Real Temi Robot

1. Set the backend URL in `MainActivity.kt` to your backend machine's LAN IP:
   ```kotlin
   const val BACKEND_URL = "ws://192.168.2.150:8000"
   ```
2. Connect Temi to the same network as your backend machine.
3. On Temi, find the IP address under **Settings > Developer Tools > ADB**.
4. Install the APK over Wi-Fi:
   ```bash
   adb connect <temi-ip>:5555   # accept the debug prompt on the touchscreen
   adb install -r -t app/build/outputs/apk/debug/app-debug.apk
   adb disconnect
   ```
   The TCP port stays open until closed in **Settings > Developer Tools > ADB**.

To launch the app from the terminal instead of the touchscreen:
```bash
adb shell am start -n org.hwu.care.healthub/.MainActivity
```

To uninstall:
```bash
adb uninstall org.hwu.care.healthub   # add -k to keep data
```

## TTS and Button Locking

When the backend is in `--tts temi` mode it sends `{"type": "tts", "text": "..."}` messages over the WebSocket. The app:

1. Locks all input buttons on the current screen (greyed out, non-interactive)
2. Sends `{"type": "tts_status", "status": "start"}` to the backend
3. Calls `robot.speak()` via the Temi SDK
4. On completion (`Robot.TtsListener.onTtsStatusChanged` → `COMPLETED`), unlocks the buttons and sends `{"type": "tts_status", "status": "stop"}`

When the backend is in `--tts local` mode it sends `{"type": "tts_active", "active": true/false}` messages instead, which lock and unlock the buttons in the same way.

On the emulator, `Robot.getInstance()` returns non-null so `robot.speak()` is called normally, but `onTtsStatusChanged` never fires. In this case `tts_status=stop` is never sent; instead the backend's fallback timer fires `tts_active=false` after an estimated playback duration, which unlocks the buttons via the `onTtsActive` handler.

## WebSocket Protocol

| Direction | Message | Description |
|-----------|---------|-------------|
| Backend → App | `{"type": "state", "page_id": N, "data": {...}}` | Navigate to a screen |
| Backend → App | `{"type": "tts", "text": "..."}` | Speak via Temi (temi mode) |
| Backend → App | `{"type": "tts_active", "active": bool}` | Lock/unlock buttons (local mode) |
| App → Backend | `{"type": "action", "action": "...", "data": {...}}` | Button press |
| App → Backend | `{"type": "tts_status", "status": "start\|stop"}` | Temi speaking start/stop |

## Page IDs Reference

```
 1  IDLE              9  OXIMETER_DONE    14  SCALE_READING
 2  WELCOME          10  BP_INTRO         15  SCALE_DONE
 3  Q1 (smoking)     11  BP_READING       16  RECAP
 4  Q2 (exercise)    12  BP_DONE          17  SORRY
 5  Q3 (alcohol)     13  SCALE_INTRO
 6  MEASURE_INTRO
 7  OXIMETER_INTRO
 8  OXIMETER_READING
```
