package org.hwu.care.healthub.core

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest

class TemiControllerImpl : TemiController, Robot.TtsListener {

    // Null on emulator — all calls are guarded with ?.
    private val robot = Robot.getInstance()

    // Callback to invoke when the current TTS utterance finishes.
    private var pendingDone: (() -> Unit)? = null

    override fun onStart() {
        robot?.addTtsListener(this)
    }

    override fun onStop() {
        robot?.removeTtsListener(this)
    }

    override fun speak(text: String, onDone: () -> Unit) {
        if (robot == null) {
            // On emulator there is no robot; treat as immediately done.
            onDone()
            return
        }
        pendingDone = onDone
        robot.speak(TtsRequest.create(text, isShowOnConversationLayer = false))
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        val status = ttsRequest.status
        if (status == TtsRequest.Status.COMPLETED || status == TtsRequest.Status.ERROR) {
            pendingDone?.invoke()
            pendingDone = null
        }
    }

    override fun navigateTo(location: String) {
        robot?.goTo(location)
    }
}
