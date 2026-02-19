package org.hwu.care.healthub.core

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest

class TemiControllerImpl : TemiController {

    // Null on emulator — all calls are guarded with ?.
    private val robot = Robot.getInstance()

    override fun speak(text: String) {
        robot?.speak(TtsRequest.create(text, isShowOnConversationLayer = false))
    }

    override fun navigateTo(location: String) {
        robot?.goTo(location)
    }
}
