package org.hwu.care.healthub

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicUiScreen(viewModel: DynamicUiViewModel, onAnswer: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is DynamicUiState.Dashboard -> DashboardScreen()
            is DynamicUiState.Question -> QuestionScreen(state, onAnswer)
            is DynamicUiState.Instruction -> InstructionScreen(state, onAnswer)
        }
    }
}

@Composable
fun DashboardScreen() {
    Text("HealthHub Dashboard", fontSize = 32.sp)
}

@Composable
fun QuestionScreen(state: DynamicUiState.Question, onAnswer: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state.text, fontSize = 28.sp, modifier = Modifier.padding(bottom = 32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.options.forEach { option ->
                Button(onClick = { onAnswer(option) }) {
                    Text(option, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun InstructionScreen(state: DynamicUiState.Instruction, onDone: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state.text, fontSize = 24.sp, modifier = Modifier.padding(bottom = 32.dp))
        // Image/Video placeholder
        Button(onClick = { onDone("DONE") }) {
            Text("Done", fontSize = 24.sp)
        }
    }
}
