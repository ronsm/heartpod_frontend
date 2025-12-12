package org.hwu.care.healthub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class DynamicUiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DynamicUiState>(DynamicUiState.Dashboard)
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()

    fun handleMqttMessage(topic: String, message: String) {
        if (topic == "temi/command") {
            parseCommand(message)
        }
    }

    private fun parseCommand(json: String) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type")

            when (type) {
                "QUESTION" -> {
                    _uiState.value = DynamicUiState.Question(
                        id = obj.getString("id"),
                        text = obj.getString("text")
                    )
                }
                "INSTRUCTION" -> {
                    _uiState.value = DynamicUiState.Instruction(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        imageUrl = obj.optString("imageUrl"),
                        videoUrl = obj.optString("videoUrl")
                    )
                }
                "DASHBOARD" -> {
                    _uiState.value = DynamicUiState.Dashboard
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
