package org.hwu.care.healthub.core

import org.hwu.care.healthub.data.models.Reading

interface TemiController {
    fun speak(text: String)
    fun navigateTo(location: String)
    fun showInstructions(deviceId: String)
    fun showReading(reading: Reading)
}
