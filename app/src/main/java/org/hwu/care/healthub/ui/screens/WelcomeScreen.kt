package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Page 02 — WELCOME
 * Shows the session introduction blurb centred on screen, with Continue / No Thanks
 * buttons pinned to the bottom.
 *
 * Expected data keys:
 *   "message" - the introduction blurb spoken by Temi
 */
@Composable
fun WelcomeScreen(
    data: Map<String, String>,
    ttsLocked: Boolean = false,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val message = data["message"] ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centre: blurb text
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 36.sp,
                lineHeight = 43.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom: Continue / No Thanks
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onReject,
                enabled = !ttsLocked,
                modifier = Modifier.size(width = 260.dp, height = 72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("No Thanks", fontSize = 32.sp)
            }
            Button(
                onClick = onAccept,
                enabled = !ttsLocked,
                modifier = Modifier.size(width = 260.dp, height = 72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Continue", fontSize = 32.sp)
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        data = mapOf(
            "message" to "I'm HeartPod, your digital health assistant. I'll guide you step-by-step " +
                "through the self-screening process and provide you with a copy of your results " +
                "to take away.\n\nBefore we start, please take a seat and make yourself comfortable. " +
                "I will ask a few general lifestyle questions to give the clinical team some background. " +
                "You can choose to skip any question.\n\nLet me know if you wish to continue."
        ),
        onAccept = {},
        onReject = {}
    )
}
