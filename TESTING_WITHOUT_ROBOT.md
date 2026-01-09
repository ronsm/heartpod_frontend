# Testing Voice Flow Without Temi Robot

## 3 Ways to Test

### Option 1: Mock Mode (Easiest) ⭐

**What it does:** Simulates Temi robot entirely in software

**Setup:**
1. Enable mock mode in `MainActivity.kt`:
```kotlin
// At top of MainActivity
private val MOCK_MODE = true  // Set to true for testing
private val robot = if (MOCK_MODE) {
    MockTemiRobot()
} else {
    Robot.getInstance()
}
```

2. Run on emulator or any Android device:
```bash
cd /home/kwalker96/Downloads/android-healthub-main
./gradlew installDebug
adb shell am start -n org.hwu.care.healthub/.MainActivity
```

3. Watch logs:
```bash
adb logcat | grep -E "MockTemi|AsrResult|MainActivity"
```

**What happens:**
- Tap "Tap to Speak" button
- Mock robot simulates: "I'm listening..."
- After 2 seconds, simulates voice input: "yes I'm ready"
- `onAsrResult()` fires with mock text
- AI processes it (if working)

**Pros:**
- ✅ No robot needed
- ✅ Works on emulator
- ✅ Fast iteration
- ✅ Full control over inputs

---

### Option 2: Manual Text Input (Simplest)

**What it does:** Type instead of speak

**Add to WelcomeScreen:**
```kotlin
// Add text field for testing
TextField(
    value = testInput,
    onValueChange = { testInput = it },
    label = { Text("Test Voice Input") }
)
Button(onClick = {
    // Simulate voice input
    handleSpeechInput(testInput)
}) {
    Text("Send Test Input")
}
```

**Pros:**
- ✅ Instant testing
- ✅ No voice needed
- ✅ Easy debugging

---

### Option 3: ADB Command Injection

**What it does:** Send mock voice events via command line

**Commands:**
```bash
# Simulate voice input
adb shell am broadcast \
  -a org.hwu.care.healthub.MOCK_VOICE \
  --es text "yes I'm ready to start"

# Simulate button press
adb shell input tap 960 600  # Coordinates of voice button
```

**Pros:**
- ✅ Test from terminal
- ✅ Scriptable
- ✅ No code changes

---

## Recommended Testing Flow

### Step 1: Test with Mock Robot
```bash
# 1. Enable mock mode
# Edit MainActivity.kt: MOCK_MODE = true

# 2. Build and install
cd /home/kwalker96/Downloads/android-healthub-main
./gradlew installDebug

# 3. Run on emulator
emulator -avd Pixel_3a_API_30 &
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Monitor logs
adb logcat -c  # Clear logs
adb logcat | grep -E "MockTemi|AsrResult|LangGraph"
```

### Step 2: Test Voice Flow
```
1. App launches
2. Tap "Tap to Speak" button
3. Mock robot says: "I'm listening. Say 'Hey Temi' followed by your response"
4. After 2 sec, mock simulates: "yes I'm ready"
5. onAsrResult() fires
6. handleSpeechInput("yes I'm ready")
7. Send to AI agent
8. AI responds (if working)
9. Robot speaks response
```

### Step 3: Verify Each Component

**Check 1: Button works**
```
Expected log: "🎤 Wakeup ENABLED"
```

**Check 2: ASR fires**
```
Expected log: "🎙️ Simulating voice input: yes I'm ready"
Expected log: "ASR Result: yes I'm ready"
```

**Check 3: AI called**
```
Expected log: "Sending to LLM: yes I'm ready"
```

**Check 4: Response received**
```
Expected log: "AI Response: [response text]"
```

---

## Quick Start

**1-Minute Test:**
```bash
cd /home/kwalker96/Downloads/android-healthub-main

# Enable mock mode (one-time edit)
sed -i 's/private val MOCK_MODE = false/private val MOCK_MODE = true/' \
  app/src/main/java/org/hwu/care/healthub/MainActivity.kt

# Build and run
./gradlew installDebug
adb shell am start -n org.hwu.care.healthub/.MainActivity

# Watch logs
adb logcat | grep "MockTemi"
```

---

## Troubleshooting

**Issue:** Mock robot not working
- Check: `MOCK_MODE = true` in MainActivity
- Check: MockTemiRobot.kt file exists
- Check: Rebuild app after changes

**Issue:** No logs appearing
- Run: `adb logcat -c` to clear
- Run: `adb logcat | grep -i temi`
- Check: App is actually running

**Issue:** AI still crashes
- This is expected (Gemini API issue)
- Mock mode tests everything EXCEPT AI
- Need to fix AI separately (Groq or Gemini)

---

## Summary

**Yes, you can test the hybrid voice flow without the robot!**

**Easiest method:** Mock mode
- Change 1 line: `MOCK_MODE = true`
- Run on emulator
- Full voice flow simulation

**Want me to implement mock mode now?**
