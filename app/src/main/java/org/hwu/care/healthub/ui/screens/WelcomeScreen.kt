package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onStart: () -> Unit, onExit: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Exit button in top-right corner
        if (onExit != null) {
            Button(
                onClick = onExit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Exit", fontSize = 18.sp)
            }
        }
        
        // Main content centered
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to HealthHub", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onStart, modifier = Modifier.size(width = 200.dp, height = 60.dp)) {
                Text("Start Screening", fontSize = 24.sp)
            }
        }
    }
}
