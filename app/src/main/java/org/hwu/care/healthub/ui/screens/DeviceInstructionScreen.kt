package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeviceInstructionScreen(deviceId: String, onReady: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Instructions for $deviceId", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))
        // Placeholder for Video/Image
        Box(modifier = Modifier.size(300.dp).padding(16.dp), contentAlignment = Alignment.Center) {
            Text("[Video Placeholder]")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onReady) {
            Text("I'm Ready", fontSize = 24.sp)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun DeviceInstructionScreenPreview() {
    DeviceInstructionScreen(deviceId = "oximeter", onReady = {})
}
