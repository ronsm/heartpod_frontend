package org.hwu.care.healthub.core

import org.hwu.care.healthub.data.models.Reading

class SessionManager {
    private val readings = mutableListOf<Reading>()
    private val answers = mutableMapOf<String, String>()

    fun addReading(reading: Reading) {
        readings.add(reading)
    }

    fun addAnswer(questionId: String, answer: String) {
        answers[questionId] = answer
    }

    fun getSummary(): String {
        return "Readings: ${readings.size}, Answers: ${answers.size}"
    }

    fun clear() {
        readings.clear()
        answers.clear()
    }
}
