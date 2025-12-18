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
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Exit button
        Button(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Exit", fontSize = 18.sp)
        }
        
        // Error content
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error", fontSize = 32.sp, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text("Try Again", fontSize = 24.sp)
            }
        }
    }
}
