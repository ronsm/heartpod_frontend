# HeartPod Frontend

Android Compose UI for the HeartPod self-screening health check system, designed to run on a [Temi robot](https://www.robotemi.com/). The app displays screens driven by a remote Python/FastAPI backend, polling for page state and sending user actions back.

## Architecture

```
Backend (Python/FastAPI)  <──HTTP polling──>  Android App (Jetpack Compose)
         │                                              │
    page_id + data                              renders screen
    receives actions                            sends user actions
```

- The backend is the source of truth for which screen to show
- The app polls `GET /state` and renders the appropriate Compose screen
- User interactions (button presses) are posted to `POST /action`
- Temi SDK is used for kiosk mode, TTS, and navigation on real hardware — gracefully degraded on emulator

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
├── MainActivity.kt          # Entry point, HTTP client setup, screen routing
├── AppState.kt              # Shared UI state (pageId + data map)
├── comms/
│   └── HttpPollingClient.kt # Polls backend, sends actions
├── core/
│   └── TemiControllerImpl.kt
└── ui/screens/              # One Composable per screen
mock_backend.py              # Standalone mock backend for development
```

## Running in the Emulator

### Prerequisites

- Android Studio (Hedgehog or newer)
- Python 3 (for the mock backend)
- An AVD configured as a tablet or landscape device (the Temi screen is 1280×800px)

### 1. Start the mock backend

```bash
python3 mock_backend.py
```

The server starts on `http://0.0.0.0:8000`. Leave this terminal open — you will type page IDs here to drive the app.

### 2. Check the backend URL

In `MainActivity.kt`, confirm the backend URL is set for the emulator:

```kotlin
const val BACKEND_URL = "http://10.0.2.2:8000"
```

`10.0.2.2` is the Android emulator's alias for the host machine's loopback interface.

### 3. Build and run

Open the project in Android Studio and press **Run** (or `Shift+F10`). Select your AVD when prompted.

### 4. Drive the app from the terminal

With the app running, switch screens by typing a page number in the mock backend terminal:

```
> 1    # Idle screen
> 2    # Welcome screen
> 3    # Smoking question
> 7    # Oximeter instruction
```

User button presses in the app will be printed in the terminal as `[app] action='...'`.

## Running on a Real Temi Robot

1. Set the backend URL in `MainActivity.kt` to your backend machine's LAN IP:
   ```kotlin
   const val BACKEND_URL = "http://192.168.2.150:8000"
   ```
2. Connect the Temi to the same network as your backend machine.
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
