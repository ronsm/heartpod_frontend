# 📟 End-to-End Data Flow: SDK & API Reference

This document provides a technical map of every function call and network request that happens during a HealthHub session.

---

## 🔬 In-Depth Data Flow Visualization
![In-Depth System Sequence](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/in_depth_system_sequence_diagram_1767892847819.png)

---

## 1. Voice Interaction (The Loop)

When a user speaks, the signal travels through these specific method calls:

| Step | Component | Exact Method / Call | Purpose |
| :--- | :--- | :--- | :--- |
| **1. Audio Capture** | Temi SDK | `robot.askQuestion("Listening")` | Forces mic open (no wake word needed). |
| **2. Transcription** | Temi Hardware | `OnRobotReadyListener.onAsrResult(asrText)` | Robot sends transcribed text to Android. |
| **3. Fast Check** | IntentParser | `intentParser.parse(asrText)` | Local regex check for "Start" or "Yes". |
| **4. AI Request** | Retrofit | `POST http://192.168.2.150:8000/interpret` | Sent to Pi if local check fails. |
| **5. AI Processing** | agent.py | `workflow.invoke(initial_state)` | LangGraph processes the logic via Groq LLM. |
| **6. Robot Answer** | Temi SDK | `robot.speak(TtsRequest.create(text))` | Robot speaks the response. |

---

## 2. Navigation & Movement

How the robot knows where to go and when to stop:

| Step | Component | Exact Method / Call | Purpose |
| :--- | :--- | :--- | :--- |
| **1. Command** | StateMachine | `temiController.navigateTo("oximeter")` | Triggers navigation logic. |
| **2. Movement** | Temi SDK | `robot.goTo("oximeter")` | Instructs the base motors to move to a saved POI. |
| **3. Arrival** | Temi SDK | `OnRobotReadyListener.onGoToLocationStatusChanged` | Callback when "SUCCESS" status is received. |
| **4. Setup** | StateMachine | `temiController.showInstructions("oximeter")` | Triggers the instructional UI layout. |

---

## 3. Medical Data Flow (Oximeter)

The journey of a heart rate reading from the person to the database:

| Step | Component | Exact Method / Call | Purpose |
| :--- | :--- | :--- | :--- |
| **1. Bluetooth** | BLE Service | `BluetoothGattCallback.onCharacteristicChanged` | Raw data received from the Oximeter. |
| **2. Translation** | Android App | `Reading(value, unit, timestamp)` | Wraps raw bytes into a Kotlin Data Object. |
| **3. API Handoff** | Retrofit | `GET http://192.168.2.150:8000/devices/oximeter/latest` | App polls Pi to verify storage. |
| **4. Database** | OpenHAB API | `POST /rest/items/Oximeter_SpO2/state` | Pi saves the final data to the medical record. |

---

## 4. Key Developer Endpoints (Raspberry Pi)

You can test these manually using `curl` to see the data in real-time:

*   **Interpret AI:** `http://192.168.2.150:8000/interpret` (JSON POST)
*   **Latest Reading:** `http://192.168.2.150:8000/devices/oximeter/latest` (JSON GET)
*   **OpenHAB Items:** `http://192.168.2.150:8080/rest/items` (System State)

---

## 5. UI Trigger Logic
Every screen in the app uses this **State Machine** trigger:
```kotlin
// StateMachine.kt
_state.value = NewState // Triggers Compose Recomposition
temiController.speak("...") // Triggers SDK Text-to-Speech
```
