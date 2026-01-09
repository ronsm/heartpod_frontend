package org.hwu.care.healthub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Voice button component - "Press to Talk"
 * Captures speech when pressed, sends to AI
 */
@Composable
fun VoiceButton(
    onSpeechResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isListening by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            if (!isListening) {
                isListening = true
                onSpeechResult("") // Trigger voice capture
            }
        },
        modifier = modifier
            .size(width = 200.dp, height = 80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) Color.Red else Color(0xFF2196F3)
        ),
        enabled = !isListening
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎤",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isListening) "Listening..." else "Tap to Speak",
                fontSize = 20.sp
            )
        }
    }
    
    // Auto-reset after 5 seconds
    LaunchedEffect(isListening) {
        if (isListening) {
            kotlinx.coroutines.delay(5000)
            isListening = false
        }
    }
}
