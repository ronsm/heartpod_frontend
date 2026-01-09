# Temi Voice Integration - Status & Options

## Current Status

### What's Working ✅

**Voice Capture:**
- Temi ASR successfully captures speech
- `onAsrResult()` callback fires correctly
- Speech-to-text conversion works reliably
- Text routing to AI agent works

**Evidence:**
```
Dec 18 logs:
- ASR Result: "let's start health hub" ✅
- Sent to LangGraph agent ✅
- Agent received text ✅
```

**Data Flow:**
- BLE Oximeter → OpenHAB → Android App ✅
- Real-time SpO2 and Pulse display ✅
- Timestamp validation ✅

**UI Components:**
- All screens implemented ✅
- Voice button functional ✅
- Exit buttons working ✅

### What's NOT Working ❌

**AI Agent Response:**
- Gemini API returns 404 errors
- Model name incompatibility with v1beta API
- Agent crashes on every voice input
- No response sent back to user

**Wake Word Requirement:**
- Users must say "Hey Temi" before every interaction
- Breaks natural conversation flow
- Not suitable for elderly/healthcare users

---

## The Core Problem

### Problem 1: Wake Word Requirement

**Technical Constraint:**
- Temi SDK requires wake word detection to activate ASR
- Cannot access microphone directly (system-level block)
- Android SpeechRecognizer blocked by Temi service
- No official API to bypass wake word

**Impact:**
- Unnatural conversation flow
- User frustration
- Not suitable for continuous dialogue

### Problem 2: AI Agent Failure

**Technical Issue:**
- `ChatGoogleGenerativeAI(model="gemini-pro")` returns 404
- Library uses v1beta API, model names incompatible
- Blocks entire voice interaction pipeline

**Impact:**
- Voice capture works but no AI response
- System appears broken to users
- Cannot complete questionnaire flow

---

## Options to Proceed

### Option 1: Smart Conversation Mode (RECOMMENDED) ⭐

**How it works:**
- Use `robot.startConversation()` instead of wake word
- Activates continuous listening for 30 seconds
- Auto-renews on each user interaction
- Only active during questionnaire session

**Implementation:**
```kotlin
fun startQuestionnaire() {
    robot.startConversation()
    // User can speak naturally without wake word
    // Conversation stays active throughout session
}

override fun onConversationTimeout() {
    if (isInQuestionnaire) {
        robot.startConversation()  // Auto-renew
    }
}
```

**User Experience:**
1. Tap "Start Screening" button (once)
2. Natural conversation for entire questionnaire
3. No wake word needed during session
4. Stops when questionnaire complete

**Pros:**
- ✅ Natural conversation flow
- ✅ No wake word after initial start
- ✅ Official Temi SDK (no hacks)
- ✅ Privacy-friendly (only active during session)
- ✅ Healthcare-appropriate
- ✅ No additional hardware

**Cons:**
- ⚠️ Requires initial button press
- ⚠️ 30-second timeout (but auto-renews)

**Effort:** 2-3 hours implementation
**Cost:** £0

---

### Option 2: External USB Microphone

**How it works:**
- Plug USB microphone into Temi
- Bypass Temi's audio system
- Use Android AudioRecord API
- No wake word requirement

**Implementation:**
```kotlin
val audioRecord = AudioRecord(
    MediaRecorder.AudioSource.MIC,  // External USB mic
    16000, // Sample rate
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT,
    bufferSize
)
```

**Pros:**
- ✅ No wake word needed
- ✅ Full control over audio
- ✅ Can use any speech recognition service
- ✅ True continuous listening possible

**Cons:**
- ❌ Requires hardware purchase (~£30)
- ❌ May not work on locked Temi Android (untested)
- ❌ Additional setup complexity
- ❌ Extra cable/hardware on robot
- ❌ Need to implement own STT service

**Effort:** 1 week (including testing and STT integration)
**Cost:** £20-50 for USB microphone

**Risk:** Unknown if Temi allows external USB audio devices

---

### Option 3: Hybrid Voice + Touch Interface

**How it works:**
- Display questions on screen with large buttons
- User can either speak OR tap response
- Voice uses conversation mode (no wake word during session)
- Touch provides fallback

**User Experience:**
```
Screen: "Do you smoke?"
Options: [Yes] [No] [Prefer not to say]

User can:
- Say "No" (voice)
- Tap "No" button (touch)
```

**Pros:**
- ✅ Flexible interaction
- ✅ Works in noisy environments
- ✅ Accessible for hearing impaired
- ✅ Fallback if voice fails
- ✅ No wake word during session

**Cons:**
- ⚠️ More complex UI design
- ⚠️ Not purely voice-based
- ⚠️ Requires reading ability

**Effort:** 1 week (UI design + implementation)
**Cost:** £0

---

### Option 4: Accept Wake Word Limitation

**How it works:**
- Keep current implementation
- Improve UX with clear instructions
- Train users to say "Hey Temi"

**Implementation:**
```kotlin
robot.speak("Please say 'Hey Temi' followed by your answer")
robot.toggleWakeup(true)
```

**Pros:**
- ✅ Already implemented
- ✅ Uses official Temi SDK
- ✅ Reliable ASR
- ✅ No additional development

**Cons:**
- ❌ Unnatural conversation
- ❌ User frustration
- ❌ Not suitable for elderly users
- ❌ Breaks dialogue flow

**Effort:** 0 hours (already done)
**Cost:** £0

---

## Recommendation Matrix

| Option | Natural Conversation | Effort | Cost | Risk | Healthcare Suitability |
|:-------|:---------------------|:-------|:-----|:-----|:----------------------|
| **Smart Conversation Mode** | ✅ High | Low | £0 | Low | ✅ Excellent |
| **External Microphone** | ✅ Highest | High | £30 | High | ⚠️ Good (if works) |
| **Hybrid Voice+Touch** | ⚠️ Medium | Medium | £0 | Low | ✅ Excellent |
| **Accept Wake Word** | ❌ Low | None | £0 | None | ❌ Poor |

---

## Recommended Path Forward

### Phase 1: Immediate (This Week)
**Implement Smart Conversation Mode**
- 2-3 hours development
- Test with questionnaire flow
- Evaluate user experience
- Zero cost, low risk

### Phase 2: If Needed (Next Week)
**Add Hybrid Touch Interface**
- Fallback for voice failures
- Better accessibility
- More robust solution

### Phase 3: Future Enhancement (Optional)
**Test External Microphone**
- Only if Smart Conversation Mode insufficient
- Requires hardware testing first
- Higher risk, higher reward

---

## Next Steps

1. **Fix AI Agent** (Critical blocker)
   - Switch to Groq API (free, works immediately)
   - OR fix Gemini API compatibility
   - OR use Azure OpenAI (requires VPN/campus)

2. **Implement Smart Conversation Mode**
   - Replace wake word with conversation mode
   - Test natural dialogue flow
   - Validate with questionnaire

3. **End-to-End Testing**
   - Complete voice → AI → response flow
   - Test on actual Temi robot
   - User acceptance testing

---

## Timeline Estimate

**With Groq AI + Smart Conversation Mode:**
- AI setup: 30 minutes
- Conversation mode: 2-3 hours
- Testing: 1-2 hours
- **Total: 4-6 hours to working system**

**With External Microphone (if needed):**
- Hardware procurement: 2-3 days
- Testing compatibility: 2-4 hours
- STT integration: 1-2 days
- **Total: 1 week additional**

---

## Decision Required

Choose one approach to proceed:
- **A) Smart Conversation Mode** (recommended, quick, free)
- **B) External Microphone** (higher effort, untested, hardware cost)
- **C) Hybrid Voice+Touch** (balanced, accessible, more work)
- **D) Accept Wake Word** (no change, poor UX)

**Recommendation: Start with Option A (Smart Conversation Mode)**
