package org.hwu.care.healthub

sealed class DynamicUiState {
    object Dashboard : DynamicUiState()
    
    data class Question(
        val id: String,
        val text: String,
        val options: List<String> = listOf("Yes", "No")
    ) : DynamicUiState()

    data class Instruction(
        val id: String,
        val text: String,
        val imageUrl: String? = null,
        val videoUrl: String? = null
    ) : DynamicUiState()
}
