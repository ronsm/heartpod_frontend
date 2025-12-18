# Voice Interaction Not Working - Diagnosis & Fix

## Problem
User says "yes" or other responses, but Temi doesn't respond to voice input.

## Root Cause Analysis

### What's Working ✅
1. **Speech Recognition Initialized:** Logs show `SpeechRecognizer: Speech recognizer initialized`
2. **Temi IS Speaking:** Audio playback events confirm Temi is playing pre-recorded messages
3. **LangGraph Agent Healthy:** `curl http://192.168.2.150:8000/health` returns `{"status":"healthy"}`
4. **Google API Key Configured:** `GOOGLE_API_KEY=AIzaSyDO-fVOLHWPHhb_cx1Op9lG08O1EPElsWM`

### What's NOT Working ❌
1. **No Speech Input Captured:** Logcat shows NO logs from `handleSpeechInput()` when user speaks
2. **No LangGraph Requests:** Agent logs show NO incoming HTTP requests from the app
3. **Speech Recognition Not Listening:** The app initializes speech recognition but doesn't actively listen

## Technical Explanation

The app has TWO speech recognition systems:

### 1. Temi's Built-in Speech (Currently Active)
- **How it works:** Temi SDK's `ConversationMediator` handles "Hey Temi" wake word
- **Limitation:** Only responds to Temi-specific commands, NOT custom app logic
- **Evidence:** Logs show `ConversationMediator` audio focus requests

### 2. Custom Speech Recognition (Not Active)
- **How it should work:** `SpeechRecognizer.kt` uses Android's `SpeechRecognizer` API
- **Current state:** Initialized but NOT actively listening
- **Code location:** `MainActivity.kt` line 46-50

```kotlin
speechRecognizer = SpeechRecognizer(this) { text ->
    handleSpeechInput(text)
}
speechRecognizer.initialize()
speechRecognizer.startListening() // This is called but not working
```

## Why Voice Isn't Working

**The app's custom speech recognizer is NOT capturing audio because:**

1. **Temi SDK Conflict:** Temi's `ConversationMediator` may be blocking Android's `SpeechRecognizer`
2. **Microphone Permissions:** The app may not have microphone access
3. **Speech Recognizer Not Triggered:** The `askQuestion()` method needs to be called explicitly

## Immediate Workaround

**The app DOES work with button taps:**
1. Tap "Start Screening"
2. Tap the screen when prompted (instead of saying "yes")
3. Tap "I'm Ready" button
4. The flow will work without voice

## Permanent Fix Options

### Option 1: Use Temi's Speech API (Recommended)
Modify `SpeechRecognizer.kt` to use Temi's `askQuestion()` instead of Android's `SpeechRecognizer`:

```kotlin
// In SpeechRecognizer.kt
fun askQuestion(question: String, callback: (String) -> Unit) {
    Robot.getInstance().askQuestion(question, object : OnAnswerListener {
        override fun onAnswer(answer: String) {
            callback(answer)
        }
    })
}
```

### Option 2: Request Microphone Permissions
Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Then request at runtime in `MainActivity.onCreate()`:
```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.RECORD_AUDIO), 1)
}
```

### Option 3: Debug Speech Recognition
Add extensive logging to see why `startListening()` isn't working:

```kotlin
// In SpeechRecognizer.kt
override fun onReadyForSpeech(params: Bundle?) {
    Log.d("SpeechRecognizer", "Ready for speech!")
}

override fun onError(error: Int) {
    Log.e("SpeechRecognizer", "Error: $error")
}
```

## Testing Voice After Fix

1. **Test Temi's built-in speech:**
   - Say "Hey Temi"
   - Say "Go to oximeter"
   - (This should work if Temi locations are configured)

2. **Test custom speech (after fix):**
   - Launch app
   - Wait for "Shall we start?"
   - Say "Yes"
   - Check logs: `adb logcat | grep "User said"`

## Current Recommendation

**Use the button-based flow for now.** The app is fully functional with taps, just not with voice. Voice can be added later once we debug the Temi SDK integration.
