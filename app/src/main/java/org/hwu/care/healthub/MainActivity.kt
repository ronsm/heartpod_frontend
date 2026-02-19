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
import org.hwu.care.healthub.comms.HttpPollingClient
import org.hwu.care.healthub.core.TemiControllerImpl
import org.hwu.care.healthub.ui.screens.*

class MainActivity : ComponentActivity(), OnRobotReadyListener {

    // -------------------------------------------------------------------------
    // Configuration — set BACKEND_URL to the machine running langtest_langgraph.py
    //   Emulator:   http://10.0.2.2:8000   (routes to host loopback)
    //   Real Temi:  http://<host-machine-LAN-IP>:8000  (e.g. 192.168.2.x)
    // -------------------------------------------------------------------------
    companion object {
        const val BACKEND_URL = "http://10.0.2.2:8000"
    }

    // Null on emulator — all calls are guarded with ?.
    private val robot = Robot.getInstance()
    private val temi = TemiControllerImpl()
    private val comms = HttpPollingClient()

    private val appState = mutableStateOf(AppState(pageId = PageId.IDLE))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        robot?.addOnRobotReadyListener(this)

        comms.start(BACKEND_URL) { newState ->
            runOnUiThread { appState.value = newState }
        }

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

    private fun handleUserAction(action: String, data: Map<String, String> = emptyMap()) {
        Log.d("MainActivity", "User action: $action")
        comms.sendAction(action, data)
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            robot?.requestToBeKioskApp()
            robot?.hideTopBar()
            robot?.toggleNavigationBillboard(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        comms.stop()
        robot?.removeOnRobotReadyListener(this)
    }
}

@Composable
fun HealthubApp(state: AppState, onUserAction: (String, Map<String, String>) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.pageId) {
            PageId.IDLE ->
                IdleScreen(
                    onStart = { onUserAction("start", emptyMap()) }
                )

            PageId.WELCOME ->
                WelcomeScreen(
                    data = state.data,
                    onAccept = { onUserAction("confirm", emptyMap()) },
                    onReject = { onUserAction("exit", emptyMap()) }
                )

            PageId.Q1, PageId.Q2, PageId.Q3 ->
                QuestionnaireScreen(
                    data = state.data,
                    onAnswer = { answer -> onUserAction("answer", mapOf("answer" to answer)) },
                    onSkip = { onUserAction("skip", emptyMap()) }
                )

            PageId.MEASURE_INTRO ->
                MeasureIntroScreen(
                    data = state.data,
                    onContinue = { onUserAction("confirm", emptyMap()) }
                )

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
