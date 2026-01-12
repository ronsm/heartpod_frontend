package org.hwu.care.healthub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.hwu.care.healthub.core.*
import org.hwu.care.healthub.data.PiApiImpl
import org.hwu.care.healthub.nlu.Intent
import org.hwu.care.healthub.nlu.IntentParser
import org.hwu.care.healthub.nlu.InterpretResponse
import org.hwu.care.healthub.nlu.LangGraphClient
import org.hwu.care.healthub.speech.SpeechRecognizer
import org.hwu.care.healthub.ui.screens.*
import java.util.UUID
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.NlpResult

class MainActivity : ComponentActivity(), OnRobotReadyListener, Robot.NlpListener, Robot.AsrListener {

    // Set to true to test on emulator without robot
    private val USE_MOCK_ROBOT = false

    private lateinit var stateMachine: StateMachine
    private lateinit var temiController: TemiController
    private lateinit var sessionManager: SessionManager
    private lateinit var intentParser: IntentParser
    private val robot = Robot.getInstance()
    private lateinit var llmClient: LangGraphClient
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListeningForVoice = false
    private var isInConversationMode = false
    private val sessionId = java.util.UUID.randomUUID().toString()
    private lateinit var piApi: org.hwu.care.healthub.data.PiApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Core Components
        temiController = TemiControllerImpl()
        piApi = PiApiImpl()
        sessionManager = SessionManager()
        stateMachine = StateMachine(temiController, piApi, sessionManager)
        
        // 2. Initialize NLU Components
        intentParser = IntentParser()
        llmClient = LangGraphClient(
            baseUrl = "http://192.168.2.150:8000" // LangGraph Agent on Pi 2
        )
        
        // 3. Initialize Speech Recognition
        // (SpeechRecognizer is kept but ignored in favor of Temi SDK for now)
        speechRecognizer = SpeechRecognizer(this) { text ->
            handleSpeechInput(text)
        }
        speechRecognizer.initialize()
        
        // Start listening immediately removed for better UX on home screen
        // startVoiceCapture()
        
        // 4. Register Temi listeners
        robot.addOnRobotReadyListener(this)
        robot.addNlpListener(this)
        robot.addAsrListener(this)  // ASR listener for continuous speech recognition
        Log.d("MainActivity", "Temi listeners registered (ASR + NLP)")
        
        // 5. Request microphone permission for speech recognition
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }

        // 6. Setup UI
        setContent {
            val state by stateMachine.state.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content Layer
                when (val s = state) {
                    is State.Idle -> {
                        // No auto-listen on Idle to allow user to see Home screen first
                        WelcomeScreen(
                            onStart = { sendEvent(Event.Start) },
                            onExit = { finish() },
                            onVoiceInput = { text -> startVoiceCapture() }
                        )
                    }
                    is State.Welcome -> {
                        // Removed auto-listen to prevent intrusive Listening window
                        WelcomeScreen(
                            onStart = { sendEvent(Event.UserConfirm) },
                            onExit = { finish() },
                            onVoiceInput = { text ->
                                startVoiceCapture()
                            }
                        )
                    }
                    is State.NavigateToDevice -> {
                        // Show a "Moving..." screen or just wait for arrival
                        DeviceInstructionScreen(s.deviceId, onReady = { sendEvent(Event.DeviceArrived) }) // Simulating arrival
                    }
                    is State.ShowInstructions -> {
                        // Removed auto-listen to prevent intrusive Listening window from blocking instructions
                        DeviceInstructionScreen(s.deviceId, onReady = { sendEvent(Event.UserConfirm) })
                    }
                    is State.AwaitUse -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val deviceText = when (s.deviceId) {
                                "thermometer" -> "Please take your temperature measurement"
                                else -> "Please place your finger in the ${s.deviceId}"
                            }
                            Text(
                                text = deviceText,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                            CircularProgressIndicator() 
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = { sendEvent(Event.ReadingReady) }) {
                                Text("I've taken the measurement", fontSize = 20.sp)
                            }
                        }
                    }
                    is State.ReadingCapture -> {
                        LoadingScreen(message = "Capturing reading...")
                    }
                    is State.ShowReading -> {
                        ReadingDisplayScreen(
                            reading = s.reading,
                            onConfirm = { sendEvent(Event.UserConfirm) },
                            onRetry = { sendEvent(Event.Retry) }
                        )
                    }
                    is State.NavigateToBP -> {
                        // SKIPPED/DEAD CODE
                        LoadingScreen(message = "Moving to blood pressure station...")
                        LaunchedEffect(Unit) {
                            delay(3000) // Simulate travel time / Fallback if nav fails
                            sendEvent(Event.DeviceArrived) 
                        }
                    }
                    is State.ShowBPInstructions -> {
                         BPInstructionsScreen(
                            onConfirm = { sendEvent(Event.UserConfirm) }
                        )
                    }
                    is State.AwaitBPUse -> {
                         BPWaitingScreen(
                            piApi = piApi,
                            onDataReceived = { systolic, diastolic ->
                                sendEvent(Event.BPDataReceived(systolic, diastolic))
                            },
                            onTimeout = { sendEvent(Event.BPTimeout) }
                        )
                    }
                    is State.ShowBPResults -> {
                         BPResultsScreen(
                            systolic = s.systolic,
                            diastolic = s.diastolic,
                            onContinue = { sendEvent(Event.UserConfirm) }
                        )
                    }
                    is State.ShowThermometerInstructions -> {
                        ThermometerInstructionsScreen(
                            onConfirm = { sendEvent(Event.UserConfirm) }
                        )
                    }
                    is State.ShowThermometerResults -> {
                        ThermometerResultsScreen(
                            temperature = s.temperature,
                            onContinue = { sendEvent(Event.UserConfirm) }
                        )
                    }
                    is State.ErrorRecover -> {
                        ErrorScreen(
                            message = s.message,
                            onRetry = { sendEvent(Event.Retry) },
                            onExit = { finish() }
                        )
                    }
                    is State.ConfirmSession -> {
                        ConfirmSessionScreen(
                            onContinue = { sendEvent(Event.UserConfirm) },
                            onFinish = { finish() }
                        )
                    }
                    is State.Questionnaire -> {
                        // Trigger first question
                        LaunchedEffect(Unit) {
                            // Small delay then transition to first question
                             sendEvent(Event.UserConfirm) // This logic might need check in StateMachine...
                             // Actually StateMachine.Questionnaire isn't a View state, it's a Transition state usually?
                             // Let's check logic:
                             // is State.Questionnaire -> { ... _state.value = State.AskPersonalDetails }
                             // So we shouldn't really see this state in UI if it auto-transitions.
                        }
                         LoadingScreen(message = "Preparing questionnaire...")
                    }
                     is State.AskPersonalDetails -> {
                        PersonalDetailsScreen(
                            onDetailsProvided = { first, last, dob ->
                                 sendEvent(Event.PersonalDetailsProvided(first, last, dob))
                            }
                        )
                    }
                    is State.AskSmokingStatus -> {
                        SmokingStatusScreen(
                            onAnswer = { status, perDay, quitDate ->
                                sendEvent(Event.SmokingAnswered(status, perDay, quitDate))
                            }
                        )
                    }
                    is State.AskAlcoholConsumption -> {
                         AlcoholConsumptionScreen(
                            onAnswer = { units ->
                                sendEvent(Event.AlcoholAnswered(units))
                            }
                        )
                    }
                    is State.AskExerciseFrequency -> {
                         ExerciseFrequencyScreen(
                            onAnswer = { times ->
                                sendEvent(Event.ExerciseAnswered(times))
                            }
                        )
                    }
                    is State.AskHeightWeight -> {
                         HeightWeightScreen(
                            onAnswer = { height, weight ->
                                sendEvent(Event.HeightWeightProvided(height, weight))
                            }
                        )
                    }
                    is State.ReviewQuestionnaire -> {
                        // Needs a review screen
                         LoadingScreen(message = "Saving results...")
                         LaunchedEffect(Unit) {
                             delay(1000)
                             sendEvent(Event.QuestionnaireComplete)
                         }
                    }
                    else -> {
                        // Handle other states
                        LoadingScreen(message = "Processing...")
                    }
                }

                // Global Exit Button (Top Right)
                // Only show if NOT in Idle? Or always?
                // Usually valid to exit anytime.
                if (state !is State.Idle && state !is State.Welcome) {
                    IconButton(
                        onClick = { sendEvent(Event.Abort) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.White.copy(alpha = 0.7f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Session",
                            tint = Color.Red
                        )
                    }
                }

        }
    }
}


    private fun sendEvent(event: Event) {
        Log.d("MainActivity", "Sending event: $event")
        stopVoiceCapture() // Ensure voice dialog is closed on any manual action
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
        
        // Show what the app heard on screen for debugging
        runOnUiThread {
            android.widget.Toast.makeText(this, "Heard: $text", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // 1. Try local intent parser first (fast, offline)
        val intent = intentParser.parse(text)
        
        lifecycleScope.launch {
            when (intent) {
                is Intent.Start -> sendEvent(Event.Start)
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
    
    /**
     * Start voice capture using askQuestion
     */
    private fun startVoiceCapture() {
        Log.d("MainActivity", "=== startVoiceCapture() CALLED ===")
        Log.d("MainActivity", "Stack trace:", Exception("Stack trace"))
        // Minimal valid prompt to ensure microphone opens
        robot.askQuestion("Listening") 
        isInConversationMode = true
    }
    
    private fun stopVoiceCapture() {
        Log.d("MainActivity", "=== stopVoiceCapture() CALLED ===")
        Log.d("MainActivity", "isInConversationMode: $isInConversationMode")
        if (isInConversationMode) {
            isInConversationMode = false
            robot.finishConversation()
            Log.d("MainActivity", "Conversation finished")
        } else {
            Log.d("MainActivity", "No active conversation to stop")
        }
    }
    
    /**
     * Temi ASR Listener
     * Captures speech real-time
     */
    override fun onAsrResult(asrText: String, language: SttLanguage) {
        Log.d("MainActivity", "ASR Result: $asrText")
        
        if (asrText.isNotBlank()) {
            val intent = intentParser.parse(asrText)
            
            // If we match a command, interrupt Temi immediately!
            if (intent !is Intent.Unknown) {
                // HACK: KILL THE CONVERSATION INSTANTLY TO PREVENT "I DON'T UNDERSTAND"
                robot.finishConversation() 
                handleSpeechInput(asrText)
            } else {
                 handleSpeechInput(asrText)
            }
        }
    }

    override fun onNlpCompleted(nlpResult: NlpResult) {
        Log.d("MainActivity", "NLP Result: ${nlpResult.action}")
        
        // Double Tap Silence
        temiController.speak(" ") 
        
        val text = nlpResult.action
        if (text.isNotBlank()) {
            // Recurse?
            if (isInConversationMode) {
                 isInConversationMode = false // Don't loop blindly
            }
        } else {
             isInConversationMode = false
        }
    }
    
    /**
     * Temi Robot Ready Listener
     */
    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            Log.d("MainActivity", "Robot is ready")
            try {
                // FORCE KIOSK MODE
                robot.requestToBeKioskApp()
                robot.hideTopBar()
                robot.toggleNavigationBillboard(false)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error configuring robot", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVoiceCapture()  // Stop conversation mode
        robot.removeOnRobotReadyListener(this)
        robot.removeNlpListener(this)
        robot.removeAsrListener(this)
        speechRecognizer.destroy()
    }
    
    @Composable
    private fun BPInstructionsScreen(onConfirm: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Blood Pressure Measurement",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "1. Sit down and relax for a moment",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "2. Place the cuff on your upper arm",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "3. Press START on the device",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "4. After measurement completes, press the Bluetooth/Sync button",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text("Ready", fontSize = 20.sp)
            }
        }
    }
    
    @Composable
    private fun BPWaitingScreen(
        piApi: org.hwu.care.healthub.data.PiApi,
        onDataReceived: (Float, Float) -> Unit,
        onTimeout: () -> Unit
    ) {
        var attempts by remember { mutableStateOf(0) }
        val maxAttempts = 30 // 30 × 2 seconds = 60 seconds
        
        LaunchedEffect(Unit) {
            while (attempts < maxAttempts) {
                try {
                    val systolicState = piApi.getItemState("Omron_Systolic")
                    val diastolicState = piApi.getItemState("Omron_Diastolic")
                    
                    if (systolicState != "NULL" && diastolicState != "NULL") {
                        val systolic = systolicState.toFloatOrNull()
                        val diastolic = diastolicState.toFloatOrNull()
                        
                        if (systolic != null && diastolic != null) {
                            onDataReceived(systolic, diastolic)
                            return@LaunchedEffect
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BPWaitingScreen", "Error polling OpenHAB", e)
                }
                
                delay(2000)
                attempts++
            }
            
            // Timeout
            onTimeout()
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Waiting for blood pressure data...",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please take your measurement and press sync",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Attempt ${attempts + 1} of $maxAttempts",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
    
    @Composable
    private fun BPResultsScreen(
        systolic: Float,
        diastolic: Float,
        onContinue: () -> Unit
    ) {
        // Determine BP category
        val (category, color) = when {
            systolic < 120 && diastolic < 80 -> "Normal" to Color.Green
            systolic < 130 && diastolic < 80 -> "Elevated" to Color(0xFFFFA500) // Orange
            systolic < 140 || diastolic < 90 -> "High (Stage 1)" to Color(0xFFFF6347) // Tomato
            else -> "High (Stage 2)" to Color.Red
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Blood Pressure Results",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Systolic", fontSize = 18.sp, color = Color.Gray)
                    Text(
                        text = "${systolic.toInt()}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("mmHg", fontSize = 16.sp, color = Color.Gray)
                }
                
                Text("/", fontSize = 48.sp, modifier = Modifier.padding(top = 24.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Diastolic", fontSize = 18.sp, color = Color.Gray)
                    Text(
                        text = "${diastolic.toInt()}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("mmHg", fontSize = 16.sp, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = category,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text("Continue", fontSize = 20.sp)
            }
        }
    }

    @Composable
    private fun ThermometerInstructionsScreen(onConfirm: () -> Unit) {
         Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Beurer Thermometer", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Text("1. Turn on the device", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("2. Place on forehead/ear as directed", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("3. Press measurement button", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onConfirm, modifier = Modifier.size(200.dp, 60.dp)) {
                Text("Ready", fontSize = 20.sp)
            }
        }
    }

    @Composable
    private fun ThermometerWaitingScreen(
        piApi: org.hwu.care.healthub.data.PiApi,
        onDataReceived: (Float) -> Unit
    ) {
        var attempts by remember { mutableStateOf(0) }
        val maxAttempts = 60 // 30 seconds wait
        
        LaunchedEffect(Unit) {
            while (attempts < maxAttempts) {
                delay(500) // Poll every 0.5s
                attempts++
                
                try {
                    // Poll OpenHAB via PiApi
                    val reading = piApi.getLatestReading("thermometer_station")
                    if (reading != null) {
                        // value format: "36.5°C"
                        val tempVal = reading.value.replace("°C", "").toFloatOrNull()
                        if (tempVal != null && tempVal > 0) {
                            onDataReceived(tempVal)
                            return@LaunchedEffect
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Timeout or handle elsewhere? 
            // For now, if timeout, maybe stay on waiting or show error?
            // Staying on waiting allows user to try again without navigating away.
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Please take your measurement", 
                fontSize = 18.sp, 
                color = Color(0xFF888888)
            )
        }
    }

    @Composable
    private fun ThermometerResultsScreen(temperature: Float, onContinue: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Temperature Results", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "${temperature}°C",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = if (temperature > 37.5) Color.Red else Color.Green
            )
            Text("Celsius", fontSize = 24.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onContinue, modifier = Modifier.size(200.dp, 60.dp)) {
                Text("Continue", fontSize = 20.sp)
            }
        }
    }

    @Composable
    private fun PersonalDetailsScreen(onDetailsProvided: (String, String, String) -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("1 / 4", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Personal Details", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Text("Please provide your information", fontSize = 20.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { onDetailsProvided("John", "Doe", "01/01/1980") },
                    modifier = Modifier.size(width = 250.dp, height = 60.dp)
                ) {
                    Text("Submit Data", fontSize = 20.sp)
                }
            }
        }
    }

    @Composable
    private fun SmokingStatusScreen(onAnswer: (String, Int?, String?) -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("2 / 4", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Smoking Status", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { onAnswer("Often", null, null) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Often", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnswer("Occasionally", null, null) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Occasionally", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnswer("Never", null, null) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Never", fontSize = 20.sp) }
            }
        }
    }

    @Composable
    private fun AlcoholConsumptionScreen(onAnswer: (Int) -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("3 / 4", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Alcohol Consumption", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { onAnswer(7) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Often", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnswer(3) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Occasionally", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnswer(0) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Never", fontSize = 20.sp) }
            }
        }
    }

    @Composable
    private fun ExerciseFrequencyScreen(onAnswer: (Int) -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("4 / 4", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Exercise Frequency", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { onAnswer(5) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("5 days a week", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnswer(3) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("3 days a week", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnswer(0) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
                ) { Text("Never", fontSize = 20.sp) }
            }
        }
    }

    @Composable
    private fun HeightWeightScreen(onAnswer: (Float, Float) -> Unit) {
         Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
             Text("Height & Weight (Stub)")
             Button(onClick = { onAnswer(180f, 75f) }) { Text("180cm, 75kg") }
        }
    }

    @Composable
    private fun ErrorScreen(message: String, onRetry: () -> Unit, onExit: () -> Unit) {
         Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
             Text("Error: $message", color = Color.Red)
             Button(onClick = onRetry) { Text("Retry") }
             Button(onClick = onExit) { Text("Exit") }
        }
    }

    @Composable
    private fun ConfirmSessionScreen(onContinue: () -> Unit, onFinish: () -> Unit) {
         Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
             Text("Session Complete")
             Button(onClick = onFinish) { Text("Finish") }
        }
    }
}
