package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hwu.care.healthub.data.PatientQuestionnaire
import org.hwu.care.healthub.data.SmokingStatus
import org.hwu.care.healthub.ui.components.VoiceButton

@Composable
fun QuestionnaireProgressScreen(
    questionnaire: PatientQuestionnaire,
    currentQuestion: String,
    onVoiceInput: (String) -> Unit,
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
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Progress indicator
            Text(
                text = "Health Screening",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = questionnaire.getCompletionPercentage() / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color(0xFF4CAF50)
            )
            
            Text(
                text = "${questionnaire.getCompletionPercentage()}% Complete",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Current question
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = currentQuestion,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Voice button
            VoiceButton(
                onSpeechResult = onVoiceInput
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Summary of collected data
            if (questionnaire.getCompletionPercentage() > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Collected Information:",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (questionnaire.firstName.isNotBlank()) {
                            DataRow("Name", "${questionnaire.firstName} ${questionnaire.lastName}")
                        }
                        if (questionnaire.dateOfBirth.isNotBlank()) {
                            DataRow("Date of Birth", questionnaire.dateOfBirth)
                        }
                        if (questionnaire.smokingStatus != SmokingStatus.NOT_ANSWERED) {
                            DataRow("Smoking", questionnaire.smokingStatus.toString())
                        }
                        questionnaire.alcoholUnitsPerWeek?.let {
                            DataRow("Alcohol (units/week)", it.toString())
                        }
                        questionnaire.exerciseFrequency?.let {
                            DataRow("Exercise (times/week)", it.toString())
                        }
                        questionnaire.heightCm?.let {
                            DataRow("Height", "$it cm")
                        }
                        questionnaire.weightKg?.let {
                            DataRow("Weight", "$it kg")
                        }
                        questionnaire.bmi?.let {
                            DataRow("BMI", String.format("%.1f", it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "• $label:",
            fontSize = 16.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
