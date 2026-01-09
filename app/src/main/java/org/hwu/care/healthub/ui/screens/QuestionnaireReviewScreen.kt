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

@Composable
fun QuestionnaireReviewScreen(
    questionnaire: PatientQuestionnaire,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Please Review Your Information",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Personal Details Section
            SectionCard("Personal Details") {
                DataRow("Name", "${questionnaire.firstName} ${questionnaire.lastName}")
                DataRow("Date of Birth", questionnaire.dateOfBirth)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lifestyle Section
            SectionCard("Lifestyle Information") {
                DataRow("Smoking Status", when (questionnaire.smokingStatus) {
                    SmokingStatus.NEVER -> "Never smoked"
                    SmokingStatus.CURRENT_SMOKER -> "Current smoker (${questionnaire.cigarettesPerDay}/day)"
                    SmokingStatus.EX_SMOKER -> "Ex-smoker (quit: ${questionnaire.quitSmokingDate})"
                    SmokingStatus.NOT_ANSWERED -> "Not answered"
                })
                DataRow("Alcohol Consumption", "${questionnaire.alcoholUnitsPerWeek} units/week")
                DataRow("Exercise Frequency", "${questionnaire.exerciseFrequency} times/week")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Measurements Section
            SectionCard("Physical Measurements") {
                DataRow("Height", "${questionnaire.heightCm} cm")
                DataRow("Weight", "${questionnaire.weightKg} kg")
                questionnaire.bmi?.let {
                    DataRow("BMI", String.format("%.1f", it))
                }
            }
            
            // Device Measurements (if available)
            if (questionnaire.spo2 != null || questionnaire.systolicBP != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionCard("Device Measurements") {
                    questionnaire.spo2?.let {
                        DataRow("SpO2", "$it%")
                    }
                    questionnaire.pulse?.let {
                        DataRow("Pulse", "$it bpm")
                    }
                    if (questionnaire.systolicBP != null && questionnaire.diastolicBP != null) {
                        DataRow("Blood Pressure", "${questionnaire.systolicBP}/${questionnaire.diastolicBP} mmHg")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Edit", fontSize = 24.sp)
                }
                
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Confirm & Save", fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 18.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
