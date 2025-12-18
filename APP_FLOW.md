# HealthHub Android App - Ideal Interaction Flow

## Overview
The HealthHub app uses a Finite State Machine (FSM) to guide patients through health screenings on the Temi robot.

## Complete User Journey

### 1. Welcome Screen
**What User Sees:**
- "Welcome to HealthHub" message
- "Start Screening" button

**What Happens:**
- App initializes connection to Pi (OpenHAB, MQTT, LangGraph)
- Speech recognition starts listening
- FSM enters `IDLE` state

**User Action:** Tap "Start Screening"

---

### 2. Initial Greeting
**What User Sees:**
- Temi says: "Welcome to HealthHub! Shall we start your health check?"
- Screen shows: "Say 'Yes' or tap to continue"

**What Happens:**
- FSM transitions to `WELCOME` state
- Speech recognizer activates
- LangGraph agent waits for user input

**User Action:** Say "Yes" or tap screen

**Behind the Scenes:**
- Voice input → Speech-to-Text
- Text sent to LangGraph agent at `192.168.2.150:8000`
- Agent extracts intent: `confirm`
- Agent responds: "Great! Let's check your oxygen levels first."
- FSM transitions to `NAVIGATE_TO_DEVICE` state

---

### 3. Navigation to Device (Optional)
**What User Sees:**
- Temi says: "Going to the oximeter station"
- (If locations configured) Temi moves to "oximeter" location

**What Happens:**
- FSM sends MQTT command to Temi: `goto:oximeter`
- Temi SDK navigates to saved location
- When arrived, FSM transitions to `DEVICE_INSTRUCTIONS` state

**User Action:** Wait for Temi to arrive

**Note:** If locations aren't configured, Temi skips navigation and goes straight to instructions.

---

### 4. Device Instructions
**What User Sees:**
- Screen shows:
  - Video placeholder (or animation)
  - Text: "Place your finger in the Pulse Oximeter"
  - "I'm Ready" button

**What Happens:**
- FSM enters `DEVICE_INSTRUCTIONS` state
- App displays instructional content
- Waits for user confirmation

**User Action:** 
1. Turn ON the Pulse Oximeter
2. Place finger in device
3. Tap "I'm Ready"

---

### 5. Measurement in Progress
**What User Sees:**
- Screen shows: "Measuring... Please keep still"
- Progress indicator or animation

**What Happens:**
- FSM transitions to `MEASURING` state
- BLE service on Pi detects Oximeter (MAC: `CB:31:33:32:1F:8F`)
- BLE service reads SpO2 and Heart Rate
- Data pushed to OpenHAB REST API
- App polls OpenHAB every 2 seconds for updated values

**Duration:** 10-15 seconds

**Behind the Scenes:**
```
Pi BLE Service → Detects device
              → Reads: SpO2=98%, HR=72bpm
              → POST to OpenHAB:8080/rest/items/PulseOximeter_SpO2
App          → GET from OpenHAB:8080/rest/items/PulseOximeter_SpO2
              → Displays result
```

---

### 6. Results Display
**What User Sees:**
- Screen shows:
  - "Your Results"
  - SpO2: 98%
  - Heart Rate: 72 bpm
  - Green checkmark (if normal)
- Temi says: "Your oxygen level is 98% and your heart rate is 72. Both are within normal range."

**What Happens:**
- FSM transitions to `RESULTS` state
- App fetches final values from OpenHAB
- LangGraph agent generates natural language summary
- Results displayed with color coding (green=normal, yellow=borderline, red=abnormal)

**User Action:** Review results

---

### 7. Next Steps
**What User Sees:**
- Screen shows:
  - "Would you like to check your blood pressure?"
  - "Yes" / "No" buttons

**What Happens:**
- FSM waits in `RESULTS` state
- User can choose to continue or finish

**User Actions:**
- **If "Yes":** FSM loops back to step 3 (Navigate to BP device)
- **If "No":** FSM transitions to `COMPLETE` state

---

### 8. Completion
**What User Sees:**
- "Thank you! Your results have been saved."
- "Return to Home" button

**What Happens:**
- FSM transitions to `COMPLETE` state
- Data saved to OpenHAB history
- Session log created

**User Action:** Tap "Return to Home" → Returns to step 1

---

## Voice Interaction Examples

### During Any Step:
**User:** "I need help"
- **Agent:** "I can guide you through the measurement. Would you like me to repeat the instructions?"

**User:** "What's my last reading?"
- **Agent:** "Your last oxygen level was 98%, measured 5 minutes ago."

**User:** "Cancel"
- **Agent:** "Okay, stopping the screening. Would you like to start over?"

---

## Error Handling

### Device Not Found
**What User Sees:**
- "Cannot find the Pulse Oximeter. Please make sure it's turned on."
- "Retry" button

**What Happens:**
- BLE service logs: "Device CB:31:33:32:1F:8F not found"
- App shows error after 30-second timeout
- User can retry or cancel

### Connection Lost
**What User Sees:**
- "Lost connection to Health Hub. Reconnecting..."

**What Happens:**
- App detects OpenHAB/MQTT connection failure
- Attempts reconnection (3 retries)
- If fails, shows manual reconnect option

---

## Technical Flow Summary

```
User Tap → FSM Event → State Transition
                     ↓
                Voice Input → Speech-to-Text
                     ↓
                LangGraph Agent (Pi:8000)
                     ↓
                Intent Extraction (GPT/Gemini)
                     ↓
                Action Determination
                     ↓
                FSM receives next_state + speech
                     ↓
                Temi speaks response
                     ↓
                UI updates
                     ↓
                (If measurement) BLE Service reads sensor
                     ↓
                Data → OpenHAB → App displays
```

---

## Current Blockers (As of Dec 18, 2025)

1. **Google API Key Missing:** Voice interaction won't work until key is added to `/home/openhabian/langraph-backend/.env`
2. **Temi Locations Not Configured:** Navigation will be skipped (app still works, just no movement)
3. **SharedPreferences Cache:** Old IP addresses may be cached - clear app data if issues persist

---

## Quick Test Sequence

1. Launch app
2. Tap "Start Screening"
3. Say "Yes" when prompted
4. Turn on Pulse Oximeter
5. Tap "I'm Ready"
6. Wait 15 seconds
7. View results
8. Say "Thank you" to finish
