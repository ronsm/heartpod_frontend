# Temi ASR Investigation Results

## Problem
`Robot.AsrListener.onAsrResult()` is NEVER called, even though:
- ✅ Listener is registered
- ✅ Microphone permission granted  
- ✅ Robot is ready
- ❌ No ASR events in logs

## Root Cause
**Temi's ASR listener only activates AFTER wake word detection ("Hey Temi")**

From Temi SDK documentation:
- `AsrListener` receives speech recognition results
- BUT only when Temi's wake word system is active
- Wake word: "Hey Temi" or configured phrase

## The Real Issue
We have a chicken-and-egg problem:
1. User must say "Hey Temi" first
2. Then Temi listens
3. Then `onAsrResult()` fires
4. Then we can route to our AI

But "Hey Temi" triggers Temi's NLP, not ours!

## Solutions

### Option 1: Use Temi's Conversation Mode
```kotlin
robot.startConversation()  // Activates continuous listening
// Now AsrListener will fire without wake word
```
**Pros:** Continuous listening, no wake word needed
**Cons:** May interfere with Temi's built-in features

### Option 2: Custom Wake Word
Configure Temi to use custom wake word via Settings
**Pros:** Dedicated activation
**Cons:** Requires Temi configuration, still goes through Temi NLP first

### Option 3: Button + ASR
Tap button → `robot.startConversation()` → Listen for 10 seconds → Stop
**Pros:** Controlled activation, no wake word confusion
**Cons:** Requires button press

### Option 4: Accept Temi's NLP
Use "Hey Temi" → Temi NLP → Route specific intents to our AI
**Pros:** Works with Temi's design
**Cons:** Limited to Temi's intent recognition

## Recommendation
**Option 3: Button + Conversation Mode**

This gives us the best of both worlds:
- User taps "Tap to Speak" button
- App calls `robot.startConversation()`
- Temi listens continuously for 10 seconds
- `onAsrResult()` fires with recognized text
- We route to Gemini AI
- After timeout, call `robot.stopConversation()`

This is the **proper Temi SDK pattern** for custom voice apps.

## Implementation
```kotlin
private fun startVoiceCapture() {
    robot.startConversation()  // Activates ASR
    isListeningForVoice = true
    
    lifecycleScope.launch {
        delay(10000)  // Listen for 10 seconds
        robot.stopConversation()
        isListeningForVoice = false
    }
}

override fun onAsrResult(asrText: String, language: SttLanguage) {
    // This will now fire!
    handleSpeechInput(asrText)
}
```
