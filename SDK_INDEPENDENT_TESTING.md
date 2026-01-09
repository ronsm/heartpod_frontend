# Testing Voice Flow Without Temi SDK

## The Real Test: Can Voice Work Without Temi?

**Goal:** Prove that voice capture → AI processing works independently of Temi SDK

## Standalone Voice Test (No Temi SDK)

### What We'll Test
```
Android Mic → SpeechRecognizer → AI Agent → Response
(No Temi SDK involved)
```

### Implementation

**1. Pure Android Voice Capture**
```kotlin
// Uses ONLY Android APIs, zero Temi SDK
class StandaloneVoiceTest(private val context: Context) {
    
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                val text = matches?.get(0) ?: ""
                
                // Got voice input! Now send to AI
                sendToAI(text)
            }
            
            override fun onError(error: Int) {
                Log.e("VoiceTest", "Error: $error")
            }
            
            // Other callbacks...
        })
        
        speechRecognizer.startListening(intent)
    }
    
    private fun sendToAI(text: String) {
        // Call AI agent directly (no Temi involved)
        lifecycleScope.launch {
            val response = apiClient.sendToLangGraph(text)
            Log.d("VoiceTest", "AI Response: $response")
        }
    }
}
```

**2. Test on Regular Android Device**
```bash
# Build APK without Temi SDK dependency
./gradlew assembleDebug

# Install on ANY Android phone (not Temi)
adb install app/build/outputs/apk/debug/app-debug.apk

# Test voice
# 1. Tap button
# 2. Speak
# 3. See if AI responds
```

**3. What This Proves**
- ✅ Android mic works
- ✅ SpeechRecognizer works
- ✅ AI agent receives text
- ✅ AI responds
- ❌ **But won't work on Temi** (mic blocked)

---

## The Real Bottleneck Test

### Test 1: AI Agent (Independent of Voice)
**Can AI process text at all?**

```bash
# Direct API test (no voice, no Temi)
curl -X POST http://192.168.2.150:8000/agent/interpret \
  -H "Content-Type: application/json" \
  -d '{"user_text":"yes I am ready","current_state":"Welcome","session_id":"test"}'
```

**Expected:** AI returns response
**Actual:** HTTP 500 (Gemini error)
**Conclusion:** ❌ AI is broken (not voice)

### Test 2: Voice Capture on Temi (SDK Required)
**Can we capture voice on Temi?**

```kotlin
// This REQUIRES Temi SDK (no alternative)
robot.addAsrListener(this)
robot.toggleWakeup(true)
// User says "Hey Temi, hello"
// onAsrResult("hello") fires
```

**Expected:** onAsrResult() fires
**Actual:** ✅ Works (we saw this in logs Dec 18)
**Conclusion:** ✅ Voice capture works

### Test 3: End-to-End (Voice → AI)
**Can captured voice reach AI?**

```
Voice captured: "yes I'm ready" ✅
Sent to AI agent ✅
AI crashes ❌ (Gemini error)
```

**Conclusion:** Voice works, AI broken

---

## The Actual Bottleneck

**It's NOT the SDK integration!**

The bottleneck is:
1. ❌ **AI Agent (Gemini API broken)** ← THIS IS THE BLOCKER
2. ✅ Voice capture works (proven in logs)
3. ✅ SDK integration works (onAsrResult fires)

**Proof from Dec 18 logs:**
```
12-18 16:43:32 LangGraphClient: Sending to LLM: let's start health hub ✅
12-18 16:43:33 LangGraphClient: HTTP 500 Internal Server Error ❌
```

Voice got to AI, AI crashed.

---

## How to Verify SDK Works (Without Fixing AI)

### Test: Voice Capture Only
```kotlin
override fun onAsrResult(asrText: String, language: SttLanguage) {
    // Don't send to AI, just log it
    Log.d("TEST", "✅ Voice captured: $asrText")
    robot.speak("I heard you say: $asrText")
}
```

**Run on Temi:**
1. Say "Hey Temi, hello world"
2. Check logs for "✅ Voice captured: hello world"
3. Robot speaks back: "I heard you say: hello world"

**This proves SDK works without needing AI!**

---

## Summary

**Can we test without SDK?**
- ❌ Not on Temi (SDK is required for mic access)
- ✅ On regular Android phone (but that's not the target)

**Can we test SDK without AI?**
- ✅ YES! Just log voice input instead of sending to AI
- ✅ Proves voice capture works
- ✅ Isolates the AI bottleneck

**What's the real bottleneck?**
- ❌ AI Agent (Gemini API)
- ✅ NOT voice capture
- ✅ NOT SDK integration

**Next step:**
Fix AI (Groq or Gemini), then everything works.

**Want me to create a simple test that proves SDK voice works without AI?**
