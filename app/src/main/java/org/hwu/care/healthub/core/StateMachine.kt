package org.hwu.care.healthub.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hwu.care.healthub.data.PiApi
import org.hwu.care.healthub.data.models.Reading

sealed class State {
    object Idle : State()
    object Welcome : State()
    data class NavigateToDevice(val deviceId: String) : State()
    data class ShowInstructions(val deviceId: String) : State()
    data class AwaitUse(val deviceId: String) : State()
    data class ReadingCapture(val deviceId: String) : State()
    data class ShowReading(val reading: Reading) : State()
    object Questionnaire : State()
    
    // Questionnaire states
    object AskPersonalDetails : State()
    object AskSmokingStatus : State()
    object AskAlcoholConsumption : State()
    object AskExerciseFrequency : State()
    object AskHeightWeight : State()
    object ReviewQuestionnaire : State()
    
    object ConfirmSession : State()
    data class ErrorRecover(val message: String) : State()
    object End : State()
}

sealed class Event {
    object Start : Event()
    object UserConfirm : Event()
    object DeviceArrived : Event()
    object ReadingReady : Event()
    object Timeout : Event()
    object Retry : Event()
    object Abort : Event()
    
    // Questionnaire events
    data class PersonalDetailsProvided(val firstName: String, val lastName: String, val dob: String) : Event()
    data class SmokingAnswered(val status: String, val perDay: Int? = null, val quitDate: String? = null) : Event()
    data class AlcoholAnswered(val unitsPerWeek: Int) : Event()
    data class ExerciseAnswered(val timesPerWeek: Int) : Event()
    data class HeightWeightProvided(val heightCm: Float, val weightKg: Float) : Event()
    object QuestionnaireComplete : Event()
}

class StateMachine(
    private val temi: TemiController,
    private val piApi: PiApi,
    private val sessionManager: SessionManager
) {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    suspend fun handleEvent(event: Event) {
        val currentState = _state.value
        
        when (currentState) {
            is State.Idle -> {
                if (event is Event.Start) {
                    val firstDevice = "oximeter" // TODO: Get from config
                    temi.speak("Okay, starting screening. Moving to the $firstDevice station.")
                    temi.navigateTo(firstDevice)
                    _state.value = State.NavigateToDevice(firstDevice)
                }
            }
            is State.Welcome -> {
                if (event is Event.UserConfirm) {
                    val firstDevice = "oximeter" // TODO: Get from config
                    temi.navigateTo(firstDevice)
                    _state.value = State.NavigateToDevice(firstDevice)
                }
            }
            is State.NavigateToDevice -> {
                if (event is Event.DeviceArrived) {
                    temi.showInstructions(currentState.deviceId)
                    _state.value = State.ShowInstructions(currentState.deviceId)
                }
            }
            is State.ShowInstructions -> {
                if (event is Event.UserConfirm) {
                    // Tell Pi to focus/scan
                    piApi.setFocus(currentState.deviceId)
                    _state.value = State.AwaitUse(currentState.deviceId)
                }
            }
            is State.AwaitUse -> {
                if (event is Event.ReadingReady) {
                    _state.value = State.ReadingCapture(currentState.deviceId)
                    captureReading(currentState.deviceId)
                }
            }
            is State.ShowReading -> {
                if (event is Event.UserConfirm) {
                    // Logic for next device or finish
                    _state.value = State.ConfirmSession
                } else if (event is Event.Retry) {
                    // Retry logic
                }
            }
            // ... handle other states
            else -> {
                // Handle global events like Abort
                if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
        }
    }

    private suspend fun captureReading(deviceId: String) {
        try {
            val reading = piApi.getLatestReading(deviceId)
            if (reading != null) {
                sessionManager.addReading(reading)
                temi.showReading(reading)
                _state.value = State.ShowReading(reading)
            } else {
                _state.value = State.ErrorRecover("Could not fetch reading")
                temi.speak("I couldn't get the reading. Please try again.")
            }
        } catch (e: Exception) {
            _state.value = State.ErrorRecover(e.message ?: "Unknown error")
        }
    }
}
