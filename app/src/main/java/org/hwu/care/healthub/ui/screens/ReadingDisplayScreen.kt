package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hwu.care.healthub.data.models.Reading

@Composable
fun ReadingDisplayScreen(reading: Reading, onConfirm: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reading Captured", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("${reading.value} ${reading.unit}", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onRetry) {
                Text("Retry", fontSize = 20.sp)
            }
            Button(onClick = onConfirm) {
                Text("Confirm", fontSize = 20.sp)
            }
        }
    }
}
