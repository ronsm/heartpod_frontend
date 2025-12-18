# Wake Word Button Approach for Voice Integration

## Problem Statement

**Current Situation:**
- Temi's `askQuestion()` API forces speech through Temi's NLP first
- Android's `SpeechRecognizer` has audio conflicts with Temi's audio system
- No way to get raw speech-to-text without Temi NLP interference

**User Experience:**
```
User: "Yes, let's start"
→ Temi NLP: "I don't understand how to help with that"
→ Our AI never receives the text
```

---

## Wake Word Button Solution

### Concept

Instead of continuous listening, use a **"Press to Talk" button** that:
1. User taps button when ready to speak
2. App captures audio for 5 seconds
3. Sends audio to speech recognition
4. Routes text to LangGraph/Gemini
5. AI responds via Temi speech

### How It Helps

#### 1. **Avoids Audio Conflicts**
- Only one audio source active at a time
- User controls when microphone is active
- No continuous listening = no conflicts with Temi

#### 2. **Clear User Intent**
- Button press = explicit signal to listen
- No ambiguity about when robot is listening
- Visual feedback (button highlighted while recording)

#### 3. **Bypasses Temi NLP**
- We control the audio capture timing
- Can use Android's `AudioRecord` API directly
- Send raw audio to Google Speech API or Whisper
- Temi's NLP never sees the audio

#### 4. **Better UX**
- User knows exactly when robot is listening
- No "wake word" confusion ("Hey Temi" vs "Hey HealthHub")
- Works reliably every time
- Familiar pattern (like walkie-talkie)

---

## Implementation Approach

### Option A: Simple Button + Android SpeechRecognizer

```kotlin
// In UI
Button(onClick = {
    viewModel.startListening()
}) {
    Text(if (isListening) "🎤 Listening..." else "🎤 Press to Talk")
}

// In ViewModel
fun startListening() {
    isListening = true
    speechRecognizer.startListening()
    
    // Auto-stop after 5 seconds
    lifecycleScope.launch {
        delay(5000)
        speechRecognizer.stopListening()
        isListening = false
    }
}
```

**Pros:**
- Simple implementation
- Uses existing Android APIs
- No external dependencies

**Cons:**
- Still might conflict with Temi audio
- Limited control over audio routing

---

### Option B: Button + Raw Audio Capture + Cloud STT

```kotlin
// Capture raw audio
val audioRecord = AudioRecord(
    MediaRecorder.AudioSource.MIC,
    16000, // Sample rate
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT,
    bufferSize
)

// On button press
fun startRecording() {
    audioRecord.startRecording()
    // Record for 5 seconds
    val audioData = captureAudio(duration = 5000)
    
    // Send to Google Speech API
    val text = googleSpeechAPI.recognize(audioData)
    
    // Route to AI
    handleSpeechInput(text)
}
```

**Pros:**
- Complete control over audio
- Can use any STT service (Google, Whisper, Azure)
- Bypasses all Temi audio systems

**Cons:**
- More complex implementation
- Requires cloud API (costs money)
- Network latency

---

### Option C: Button + Whisper Local STT

```kotlin
// Use OpenAI Whisper running locally on Pi
fun startRecording() {
    val audioFile = recordAudio(duration = 5000)
    
    // Send to Pi's Whisper service
    val text = whisperClient.transcribe(audioFile)
    
    // Route to LangGraph
    llmClient.interpret(text)
}
```

**Pros:**
- No cloud dependency
- Fast (local processing)
- Free (no API costs)
- Complete privacy

**Cons:**
- Need to install Whisper on Pi
- Pi 2 might be too slow
- Requires audio file transfer

---

## Recommended Implementation

### Phase 1: Simple Button (Immediate)

```kotlin
@Composable
fun VoiceButton(onSpeechResult: (String) -> Unit) {
    var isListening by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            if (!isListening) {
                isListening = true
                startSpeechRecognition { text ->
                    onSpeechResult(text)
                    isListening = false
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) Color.Red else Color.Blue
        )
    ) {
        Icon(Icons.Default.Mic)
        Text(if (isListening) "Listening..." else "Tap to Speak")
    }
}
```

**User Flow:**
1. User sees "Tap to Speak" button
2. Taps button
3. Button turns red, shows "Listening..."
4. User speaks (5 seconds max)
5. Text sent to Gemini
6. AI response spoken by Temi
7. Button returns to blue "Tap to Speak"

---

## Why This Works

### 1. **Timing Control**
- We control exactly when audio capture starts/stops
- No continuous listening = no conflicts
- Temi's audio system not active during our capture

### 2. **User Clarity**
- Visual feedback (button color change)
- Clear start/stop of listening
- No confusion about robot state

### 3. **Reliability**
- Button always works (no wake word recognition needed)
- Explicit user action = high intent signal
- Fallback to buttons if voice fails

### 4. **Incremental Enhancement**
- Can start with simple implementation
- Upgrade to better STT later
- Doesn't break existing button flow

---

## Comparison: Continuous vs Button

| Feature | Continuous Listening | Wake Word Button |
|:--------|:--------------------|:-----------------|
| Audio Conflicts | ❌ High | ✅ Low |
| User Clarity | ⚠️ Ambiguous | ✅ Clear |
| Reliability | ❌ Inconsistent | ✅ Consistent |
| Implementation | ❌ Complex | ✅ Simple |
| Battery Impact | ❌ High | ✅ Low |
| Privacy | ⚠️ Always listening | ✅ On-demand |
| UX Familiarity | ⚠️ New pattern | ✅ Known pattern |

---

## Next Steps

### To Implement Wake Word Button:

1. **Add Voice Button to UI** (5 min)
   - Create button component
   - Add to WelcomeScreen and InstructionScreen

2. **Implement Audio Capture** (15 min)
   - Use Android SpeechRecognizer with button trigger
   - Add 5-second timeout
   - Visual feedback during recording

3. **Route to AI** (5 min)
   - Connect to existing `handleSpeechInput()`
   - Already routes to LangGraph/Gemini

4. **Test** (10 min)
   - Verify button triggers recording
   - Confirm text reaches AI
   - Check Temi speaks response

**Total Time:** ~35 minutes

---

## Conclusion

**Wake word button solves the core problem:**
- ✅ Bypasses Temi NLP
- ✅ Avoids audio conflicts
- ✅ Clear user experience
- ✅ Simple to implement
- ✅ Reliable operation

**This is the pragmatic path to voice integration** while we wait for better Temi SDK support or explore more advanced solutions.
