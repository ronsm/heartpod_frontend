package org.hwu.care.healthub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import org.hwu.care.healthub.core.TemiControllerImpl
import org.hwu.care.healthub.ui.screens.*

class MainActivity : ComponentActivity(), OnRobotReadyListener {

    // Null on emulator — all calls are guarded with ?.
    private val robot = Robot.getInstance()
    private val temi = TemiControllerImpl()

    // Current display state. Updated via onStateReceived() from the comms layer.
    private val appState = mutableStateOf(AppState(pageId = PageId.IDLE))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        robot?.addOnRobotReadyListener(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HealthubApp(
                        state = appState.value,
                        onUserAction = ::handleUserAction
                    )
                }
            }
        }
    }

    /**
     * Called by the comms layer when the backend sends a new state.
     * TODO: Wire this to the agreed comms protocol (WebSocket / REST / MQTT / etc.)
     */
    fun onStateReceived(newState: AppState) {
        appState.value = newState
    }

    /**
     * User interactions (button presses, answers) to send back to the backend.
     * TODO: Wire this to the agreed comms protocol.
     *
     * @param action  e.g. "start", "confirm", "retry", "exit", "answer"
     * @param data    Optional payload, e.g. mapOf("answer" to "yes")
     */
    private fun handleUserAction(action: String, data: Map<String, String> = emptyMap()) {
        Log.d("MainActivity", "User action: $action, data: $data")
        // TODO: Forward action to backend
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            Log.d("MainActivity", "Robot ready")
            robot?.requestToBeKioskApp()
            robot?.hideTopBar()
            robot?.toggleNavigationBillboard(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        robot?.removeOnRobotReadyListener(this)
    }
}

@Composable
fun HealthubApp(state: AppState, onUserAction: (String, Map<String, String>) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.pageId) {
            PageId.IDLE ->
                WelcomeScreen(
                    onStart = { onUserAction("start", emptyMap()) }
                )

            PageId.WELCOME ->
                WelcomeScreen(
                    onStart = { onUserAction("confirm", emptyMap()) }
                )

            PageId.Q1, PageId.Q2, PageId.Q3 ->
                // TODO: Design questionnaire screens once comms protocol is agreed.
                // data["question"] will contain the question text.
                LoadingScreen(message = state.data["question"] ?: "Loading question...")

            PageId.MEASURE_INTRO,
            PageId.OXIMETER_INTRO,
            PageId.BP_INTRO,
            PageId.SCALE_INTRO ->
                DeviceInstructionScreen(
                    deviceId = state.data["device"] ?: "",
                    onReady = { onUserAction("ready", emptyMap()) }
                )

            PageId.OXIMETER_READING,
            PageId.BP_READING,
            PageId.SCALE_READING ->
                LoadingScreen(
                    message = state.data["message"] ?: "Please take your measurement"
                )

            PageId.OXIMETER_DONE,
            PageId.BP_DONE,
            PageId.SCALE_DONE ->
                ReadingDisplayScreen(
                    data = state.data,
                    onAction = { action -> onUserAction(action, emptyMap()) }
                )

            PageId.RECAP ->
                ConfirmSessionScreen(
                    onContinue = { onUserAction("continue", emptyMap()) },
                    onFinish = { onUserAction("finish", emptyMap()) }
                )

            PageId.SORRY ->
                ErrorScreen(
                    message = state.data["message"] ?: "Something went wrong.",
                    onRetry = { onUserAction("retry", emptyMap()) },
                    onExit = { onUserAction("exit", emptyMap()) }
                )

            else ->
                LoadingScreen(message = "Loading...")
        }
    }
}
