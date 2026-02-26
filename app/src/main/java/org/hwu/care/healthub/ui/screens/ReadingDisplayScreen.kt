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

/**
 * Displays a captured reading value and unit from the backend.
 *
 * Expected data keys:
 *   "value" - the reading value (e.g. "98.6")
 *   "unit"  - the unit label (e.g. "bpm", "mmHg", "°C")
 */
@Composable
fun ReadingDisplayScreen(
    data: Map<String, String>,
    ttsLocked: Boolean = false,
    onAction: (String) -> Unit
) {
    val value = data["value"] ?: "--"
    val unit = data["unit"] ?: ""

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reading Captured", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(48.dp))
        Text("$value $unit", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { onAction("retry") }, enabled = !ttsLocked) {
                Text("Retry", fontSize = 32.sp)
            }
            Button(onClick = { onAction("confirm") }, enabled = !ttsLocked) {
                Text("Confirm", fontSize = 32.sp)
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun ReadingDisplayScreenPreview() {
    ReadingDisplayScreen(
        data = mapOf("value" to "98", "unit" to "bpm"),
        onAction = {}
    )
}
