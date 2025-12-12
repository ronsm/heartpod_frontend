package org.hwu.care.healthub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hwu.care.healthub.core.*
import org.hwu.care.healthub.data.PiApiImpl
import org.hwu.care.healthub.nlu.Intent
import org.hwu.care.healthub.nlu.IntentParser
import org.hwu.care.healthub.nlu.InterpretResponse
import org.hwu.care.healthub.nlu.LangGraphClient
import org.hwu.care.healthub.speech.SpeechRecognizer
import org.hwu.care.healthub.ui.screens.*

class MainActivity : ComponentActivity() {

    private lateinit var stateMachine: StateMachine
    private lateinit var temiController: TemiController
    private lateinit var sessionManager: SessionManager
    private lateinit var intentParser: IntentParser
    private lateinit var llmClient: LangGraphClient
    private lateinit var speechRecognizer: SpeechRecognizer
    private val sessionId = java.util.UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Core Components
        temiController = TemiControllerImpl()
        val piApi = PiApiImpl()
        sessionManager = SessionManager()
        stateMachine = StateMachine(temiController, piApi, sessionManager)
        
        // 2. Initialize NLU Components
        intentParser = IntentParser()
        llmClient = LangGraphClient(
            baseUrl = "http://192.168.2.150:8000"  // LangGraph on Raspberry Pi 2
        )
        
        // 3. Initialize Speech Recognition
        speechRecognizer = SpeechRecognizer(this) { text ->
            handleSpeechInput(text)
        }
        speechRecognizer.initialize()
        speechRecognizer.startListening()


        // 2. Setup UI
        setContent {
            val state by stateMachine.state.collectAsState()

            when (val s = state) {
                is State.Idle -> WelcomeScreen(onStart = { sendEvent(Event.Start) })
                is State.Welcome -> WelcomeScreen(onStart = { sendEvent(Event.UserConfirm) }) // Reusing for now
                is State.NavigateToDevice -> {
                    // Show a "Moving..." screen or just wait for arrival
                    DeviceInstructionScreen(s.deviceId, onReady = { sendEvent(Event.DeviceArrived) }) // Simulating arrival
                }
                is State.ShowInstructions -> DeviceInstructionScreen(s.deviceId, onReady = { sendEvent(Event.UserConfirm) })
                is State.AwaitUse -> {
                    // Show "Waiting for reading..."
                    // In real app, we'd poll or wait for MQTT
                    // Simulating reading ready after delay
                    DeviceInstructionScreen(s.deviceId, onReady = { sendEvent(Event.ReadingReady) }) 
                }
                is State.ReadingCapture -> {
                    // Show "Capturing..."
                }
                is State.ShowReading -> ReadingDisplayScreen(
                    reading = s.reading,
                    onConfirm = { sendEvent(Event.UserConfirm) },
                    onRetry = { sendEvent(Event.Retry) }
                )
                is State.ErrorRecover -> {
                    // Show Error Screen
                }
                else -> {}
            }
        }
    }

    private fun sendEvent(event: Event) {
        lifecycleScope.launch {
            stateMachine.handleEvent(event)
        }
    }
    
    /**
     * Handle speech input from user
     * This will be called by Whisper STT in Phase 3.1
     */
    private fun handleSpeechInput(text: String) {
        Log.d("MainActivity", "User said: $text")
        
        // 1. Try local intent parser first (fast, offline)
        val intent = intentParser.parse(text)
        
        lifecycleScope.launch {
            when (intent) {
                is Intent.Confirm -> sendEvent(Event.UserConfirm)
                is Intent.Cancel -> sendEvent(Event.Abort)
                is Intent.Retry -> sendEvent(Event.Retry)
                is Intent.Help -> {
                    // Repeat current instructions
                    temiController.speak("Let me repeat the instructions.")
                    // Could re-show instructions based on current state
                }
                is Intent.Skip -> {
                    // Skip current step (if allowed)
                    sendEvent(Event.UserConfirm) // Or create Event.Skip
                }
                is Intent.Unknown -> {
                    // 2. Fall back to LLM for complex input
                    handleComplexInput(intent.text)
                }
            }
        }
    }
    
    /**
     * Handle complex input that requires LLM interpretation
     */
    private suspend fun handleComplexInput(text: String) {
        Log.d("MainActivity", "Sending to LLM: $text")
        
        val response = llmClient.interpret(
            text = text,
            currentState = stateMachine.state.value,
            sessionId = sessionId
        )
        
        if (response != null) {
            handleLlmResponse(response)
        } else {
            // LLM unavailable - ask user to repeat
            temiController.speak("I didn't quite understand. Could you please repeat that?")
        }
    }
    
    /**
     * Handle LLM agent response
     */
    private suspend fun handleLlmResponse(response: InterpretResponse) {
        // 1. Speak the LLM's response
        temiController.speak(response.speech)
        
        // 2. Execute any UI actions
        response.action?.let { action ->
            when (action) {
                "show_instructions" -> {
                    // Show instructions UI
                }
                "show_reading" -> {
                    // Show reading UI
                }
                // Add more actions as needed
            }
        }
        
        // 3. Transition to next state if specified
        response.next_state?.let { nextState ->
            // Map string to Event and send
            val event = mapStateToEvent(nextState)
            event?.let { sendEvent(it) }
        }
    }
    
    /**
     * Map state name to appropriate event
     */
    private fun mapStateToEvent(stateName: String): Event? {
        return when (stateName) {
            "Welcome" -> Event.Start
            "NavigateToDevice" -> Event.UserConfirm
            "ShowInstructions" -> Event.DeviceArrived
            "AwaitUse" -> Event.UserConfirm
            "ShowReading" -> Event.ReadingReady
            else -> null
        }
    }
}
