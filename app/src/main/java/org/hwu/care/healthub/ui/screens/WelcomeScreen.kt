package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Page 02 — WELCOME
 * Shows the session introduction blurb and asks the user to accept or reject.
 *
 * Expected data keys:
 *   "message" - the introduction blurb spoken by Temi
 */
@Composable
fun WelcomeScreen(
    data: Map<String, String>,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val message = data["message"] ?: ""

    Row(modifier = Modifier.fillMaxSize()) {

        // Left: blurb text
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, fontSize = 32.sp, lineHeight = 32.sp)
        }

        // Right: Accept / Reject buttons
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(72.dp)
            ) {
                Text("Accept", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onReject,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5))
            ) {
                Text("Reject", fontSize = 32.sp)
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        data = mapOf(
            "message" to "I'm Temi, your digital health assistant. I'll guide you step-by-step " +
                "through the self-screening process and provide you with a copy of your results " +
                "to take away.\n\nBefore we start, please take a seat and make yourself comfortable. " +
                "I will ask a few general lifestyle questions to give the clinical team some background. " +
                "You can choose to skip any question.\n\nLet me know if you wish to continue."
        ),
        onAccept = {},
        onReject = {}
    )
}
