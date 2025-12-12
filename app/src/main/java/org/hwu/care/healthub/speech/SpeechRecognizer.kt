package org.hwu.care.healthub.speech

import android.content.Context
import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener

/**
 * Speech Recognizer using Temi's built-in STT
 * 
 * Uses Temi's askQuestion() API for speech recognition
 */
class SpeechRecognizer(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) : OnRobotReadyListener {
    
    companion object {
        private const val TAG = "SpeechRecognizer"
    }
    
    private val robot = Robot.getInstance()
    
    /**
     * Initialize speech recognizer
     */
    fun initialize() {
        robot.addOnRobotReadyListener(this)
        Log.d(TAG, "Speech recognizer initialized")
    }
    
    /**
     * Start listening for speech
     */
    fun startListening() {
        Log.d(TAG, "Ready to listen via askQuestion()")
    }
    
    /**
     * Stop listening for speech
     */
    fun stopListening() {
        Log.d(TAG, "Stopped listening")
    }
    
    /**
     * Ask a question and wait for response
     * The response will be delivered via NLP intents
     */
    fun askQuestion(question: String) {
        robot.askQuestion(question)
        Log.d(TAG, "Asked question: $question")
    }
    
    /**
     * Release resources
     */
    fun release() {
        robot.removeOnRobotReadyListener(this)
        Log.d(TAG, "Released speech recognizer")
    }
    
    // OnRobotReadyListener
    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            Log.d(TAG, "Robot ready for speech recognition")
        }
    }
}
