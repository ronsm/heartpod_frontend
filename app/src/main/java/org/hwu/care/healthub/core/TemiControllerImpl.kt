package org.hwu.care.healthub.core

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import org.hwu.care.healthub.data.models.Reading

class TemiControllerImpl : TemiController {
    private val robot = Robot.getInstance()

    override fun speak(text: String) {
        robot.speak(TtsRequest.create(text, isShowOnConversationLayer = false))
    }

    override fun navigateTo(location: String) {
        robot.goTo(location)
    }

    override fun showInstructions(deviceId: String) {
        speak("Please follow the instructions on the screen for the $deviceId.")
    }

    override fun showReading(reading: Reading) {
        speak("I recorded a reading of ${reading.value} ${reading.unit}.")
    }
}
