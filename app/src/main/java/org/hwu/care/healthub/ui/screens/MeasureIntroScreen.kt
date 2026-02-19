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
 * Page 06 — MEASURE_INTRO
 * Fixed transitional screen shown after the questionnaire.
 * Tells the user what measurements are coming up and asks them to continue.
 *
 * Expected data keys:
 *   "message" - the intro text
 */
@Composable
fun MeasureIntroScreen(
    data: Map<String, String>,
    onContinue: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data["message"] ?: "",
                fontSize = 26.sp,
                lineHeight = 38.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.size(width = 260.dp, height = 68.dp)
            ) {
                Text("Continue", fontSize = 22.sp)
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun MeasureIntroScreenPreview() {
    MeasureIntroScreen(
        data = mapOf(
            "message" to "Great, thank you for answering those questions! Now we'll take " +
                "three quick measurements: an oximeter reading, a blood pressure reading, " +
                "and your weight. Just say 'continue' when you're happy to begin."
        ),
        onContinue = {}
    )
}
