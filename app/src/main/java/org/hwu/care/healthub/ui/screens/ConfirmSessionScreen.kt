package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a full session summary (questionnaire answers + sensor readings)
 * retrieved from the backend via the existing HTTP polling approach.
 *
 * Expected data keys (all provided by the backend recap node):
 *   "q1"       - smoking answer
 *   "q2"       - exercise answer
 *   "q3"       - alcohol answer
 *   "oximeter" - e.g. "72 bpm / 98%"
 *   "bp"       - e.g. "125/82 mmHg"
 *   "weight"   - e.g. "72.4 kg"
 */
@Composable
fun ConfirmSessionScreen(
    data: Map<String, String>,
    onContinue: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Session Summary", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Two-column summary: questionnaire (left) | measurements (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            // Questionnaire answers
            Column(modifier = Modifier.weight(1f)) {
                Text("Questionnaire", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                SummaryRow(label = "Smoking", value = data["q1"] ?: "\u2014")
                SummaryRow(label = "Exercise", value = data["q2"] ?: "\u2014")
                SummaryRow(label = "Alcohol", value = data["q3"] ?: "\u2014")
            }

            // Sensor measurements
            Column(modifier = Modifier.weight(1f)) {
                Text("Measurements", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                SummaryRow(label = "HR / SpO2", value = data["oximeter"] ?: "\u2014")
                SummaryRow(label = "Blood Pressure", value = data["bp"] ?: "\u2014")
                SummaryRow(label = "Weight", value = data["weight"] ?: "\u2014")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Button(
                onClick = onFinish,
                modifier = Modifier.size(width = 200.dp, height = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Finish", fontSize = 24.sp)
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.size(width = 200.dp, height = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Continue", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, fontSize = 16.sp, color = Color.Gray)
        Text(value, fontSize = 20.sp)
    }
    HorizontalDivider()
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun ConfirmSessionScreenPreview() {
    ConfirmSessionScreen(
        data = mapOf(
            "q1" to "I do not and have never smoked",
            "q2" to "Often (3-4 times a week)",
            "q3" to "Occasionally (e.g. once a week)",
            "oximeter" to "72 bpm / 98%",
            "bp" to "125/82 mmHg",
            "weight" to "72.4 kg"
        ),
        onContinue = {},
        onFinish = {}
    )
}
