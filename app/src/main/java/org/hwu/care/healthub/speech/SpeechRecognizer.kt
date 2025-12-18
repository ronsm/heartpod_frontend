package org.hwu.care.healthub.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Speech Recognizer using Android's native STT
 * Bypasses Temi NLP to get raw text for AI processing
 */
class SpeechRecognizer(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit
) : RecognitionListener {

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var isListening = false

    init {
        speechRecognizer.setRecognitionListener(this)
    }

    fun initialize() {
        Log.d(TAG, "Native Android SpeechRecognizer initialized")
    }

    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer.startListening(intent)
            isListening = true
            Log.d(TAG, "Started listening for speech")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }

    fun stopListening() {
        if (!isListening) return
        
        try {
            speechRecognizer.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer.destroy()
            Log.d(TAG, "SpeechRecognizer destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy", e)
        }
    }

    // RecognitionListener callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech started")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio level changed
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "Speech ended")
        isListening = false
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error: $error"
        }
        Log.e(TAG, "Speech recognition error: $errorMessage")
        isListening = false
        
        // Auto-restart on timeout or no match
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.d(TAG, "Auto-restarting listener")
            startListening()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            Log.d(TAG, "Speech recognized: $text")
            onSpeechResult(text)
            
            // Restart listening for continuous recognition
            startListening()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            Log.d(TAG, "Partial result: ${matches[0]}")
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Speech event: $eventType")
    }

    companion object {
        private const val TAG = "SpeechRecognizer"
    }
}
