package org.hwu.care.healthub.data

import java.time.LocalDate

/**
 * Patient questionnaire data model
 * Based on Surgery Pod workflow
 */
data class PatientQuestionnaire(
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Personal Details
    var firstName: String = "",
    var lastName: String = "",
    var dateOfBirth: String = "", // Format: YYYY-MM-DD
    
    // Lifestyle Questions
    var smokingStatus: SmokingStatus = SmokingStatus.NOT_ANSWERED,
    var cigarettesPerDay: Int? = null,
    var quitSmokingDate: String? = null, // Format: YYYY-MM-DD
    var alcoholUnitsPerWeek: Int? = null,
    var exerciseFrequency: Int? = null, // 0-7 times per week
    
    // Physical Measurements
    var heightCm: Float? = null,
    var weightKg: Float? = null,
    var bmi: Float? = null,
    
    // Device Measurements (from BLE devices)
    var systolicBP: Int? = null,
    var diastolicBP: Int? = null,
    var pulse: Int? = null,
    var spo2: Int? = null,
    
    // Status
    var currentStep: QuestionnaireStep = QuestionnaireStep.PERSONAL_DETAILS,
    var confirmed: Boolean = false,
    var savedToRecord: Boolean = false
) {
    /**
     * Calculate BMI from height and weight
     */
    fun calculateBMI(): Float? {
        return if (heightCm != null && weightKg != null && heightCm!! > 0) {
            val heightM = heightCm!! / 100f
            weightKg!! / (heightM * heightM)
        } else null
    }
    
    /**
     * Check if questionnaire is complete
     */
    fun isComplete(): Boolean {
        return firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                dateOfBirth.isNotBlank() &&
                smokingStatus != SmokingStatus.NOT_ANSWERED &&
                alcoholUnitsPerWeek != null &&
                exerciseFrequency != null &&
                heightCm != null &&
                weightKg != null
    }
    
    /**
     * Get completion percentage
     */
    fun getCompletionPercentage(): Int {
        var completed = 0
        var total = 7 // Total required fields
        
        if (firstName.isNotBlank()) completed++
        if (lastName.isNotBlank()) completed++
        if (dateOfBirth.isNotBlank()) completed++
        if (smokingStatus != SmokingStatus.NOT_ANSWERED) completed++
        if (alcoholUnitsPerWeek != null) completed++
        if (exerciseFrequency != null) completed++
        if (heightCm != null && weightKg != null) completed++
        
        return (completed * 100) / total
    }
}

/**
 * Smoking status options
 */
enum class SmokingStatus {
    NOT_ANSWERED,
    NEVER,
    CURRENT_SMOKER,
    EX_SMOKER
}

/**
 * Questionnaire steps/stages
 */
enum class QuestionnaireStep {
    PERSONAL_DETAILS,
    SMOKING,
    ALCOHOL,
    EXERCISE,
    HEIGHT_WEIGHT,
    REVIEW,
    DEVICE_MEASUREMENTS,
    COMPLETE
}
