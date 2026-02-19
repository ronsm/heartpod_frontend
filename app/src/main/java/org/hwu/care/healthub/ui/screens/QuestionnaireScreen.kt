package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.json.JSONArray

/**
 * Pages 03, 04, 05 — Q1 / Q2 / Q3
 * Shows a question and a list of answer options, plus a Skip button.
 *
 * Expected data keys:
 *   "question" - the question text
 *   "options"  - JSON array of answer strings, e.g. '["Never","Occasionally","Daily"]'
 */
@Composable
fun QuestionnaireScreen(
    data: Map<String, String>,
    onAnswer: (String) -> Unit,
    onSkip: () -> Unit
) {
    val question = data["question"] ?: ""
    val options = parseOptions(data["options"])

    Row(modifier = Modifier.fillMaxSize()) {

        // Left: question text
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(question, fontSize = 26.sp, lineHeight = 38.sp)
        }

        // Right: option buttons + Skip
        Column(
            modifier = Modifier
                .width(380.dp)
                .fillMaxHeight()
                .padding(48.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            options.forEach { option ->
                Button(
                    onClick = { onAnswer(option) },
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text(option, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5))
            ) {
                Text("Skip", fontSize = 18.sp)
            }
        }
    }
}

private fun parseOptions(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun QuestionnaireScreenPreview() {
    QuestionnaireScreen(
        data = mapOf(
            "question" to "Q1. How frequently do you smoke?",
            "options" to """["I previously smoked but no longer do","I do not and have never smoked","Occasionally (e.g. weekly or monthly)","A few times a day","Many times per day"]"""
        ),
        onAnswer = {},
        onSkip = {}
    )
}
